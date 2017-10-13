package net.corda.examples.fx.seller

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.fx.buyer.BuyCurrencyFlowDefinition
import net.corda.examples.fx.rate_provider.SignExchangeRateFlow
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.examples.fx.shared.flows.IssueCashFlow
import net.corda.examples.fx.shared.flows.ReceiveTransactionProposal
import net.corda.examples.fx.shared.flows.SendSignedTransaction
import net.corda.examples.fx.shared.flows.generateSpend
import net.corda.finance.contracts.asset.Cash
import java.math.BigDecimal
import java.security.PublicKey
import java.util.function.Predicate

@InitiatedBy(BuyCurrencyFlowDefinition::class)
class SellCurrencyFlow(private val session: FlowSession) : FlowLogic<Unit>() {

    private companion object {

        val RATE_PROVIDERS_WHITELIST = setOf(CordaX500Name(organisation = "Rate-Provider", locality = "Austin", country = "US"))

        val RECEIVING_PROPOSAL = object : ProgressTracker.Step("Receiving buyer's proposal") {}
        val CHECKING_PROPOSAL = object : ProgressTracker.Step("Checking buyer's proposal") {}
        val GENERATING_SPEND = object : ProgressTracker.Step("Generating spend to fulfil exchange") {}
        val CONFIRMING_RATE_WITH_PROVIDER = object : ProgressTracker.Step("Confirming exchange rate with provider") {}
        val SIGNING_TRANSACTION = object : ProgressTracker.Step("Signing buyer's proposal") {}
        val SENDING_BACK_TRANSACTION = object : ProgressTracker.Step("Sending signed transaction back to buyer") {}
    }

    override val progressTracker = ProgressTracker(RECEIVING_PROPOSAL, CHECKING_PROPOSAL, GENERATING_SPEND, CONFIRMING_RATE_WITH_PROVIDER, SIGNING_TRANSACTION, SENDING_BACK_TRANSACTION)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = RECEIVING_PROPOSAL
        val buyerProposal = subFlow(ReceiveTransactionProposal(session))

        progressTracker.currentStep = CHECKING_PROPOSAL
        val (rateProvider, exchangeRate) = checkProposal(buyerProposal)

        progressTracker.currentStep = GENERATING_SPEND
        // TODO self issuing the amount necessary for the trade - obviously just for the sake of the example.
        subFlow(IssueCashFlow(exchangeRate.buyAmount, buyerProposal.notary!!))
        val (tx, anonymisedSpendOwnerKeys) = Cash.generateSpend(buyerProposal, serviceHub, exchangeRate.buyAmount, session.counterparty)

        progressTracker.currentStep = CONFIRMING_RATE_WITH_PROVIDER
        val signedTx = confirmRate(tx, rateProvider, anonymisedSpendOwnerKeys)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val finalTx = serviceHub.addSignature(signedTx)

        progressTracker.currentStep = SENDING_BACK_TRANSACTION
        subFlow(SendSignedTransaction(session, finalTx))
    }

    @Suspendable
    private fun confirmRate(builder: TransactionBuilder, rateProvider: Party, anonymisedSpendOwnerKeys: List<PublicKey>): SignedTransaction {

        var transaction = serviceHub.signInitialTransaction(builder, anonymisedSpendOwnerKeys[0])
        when {
            anonymisedSpendOwnerKeys.size > 1 -> transaction = anonymisedSpendOwnerKeys.slice(1 until anonymisedSpendOwnerKeys.size).fold(transaction, serviceHub::addSignature)
        }
        val partialMerkleTree = transaction.buildFilteredTransaction(Predicate { filtering(it) })
        val rateProviderSignature = subFlow(SignExchangeRateFlow(transaction.tx, partialMerkleTree, rateProvider))
        return transaction.withAdditionalSignature(rateProviderSignature)
    }

    private fun checkProposal(proposal: TransactionBuilder): CheckProposalResult {

        val command = proposal.commands().single { it.value is ExchangeUsingRate }
        val exchangeRate = command.value as ExchangeUsingRate

        val rateProvider = serviceHub.networkMapCache.getPeerByLegalName(exchangeRate.rateProviderName) ?: throw Exception("Cannot find specified rate provider ${exchangeRate.rateProviderName}.")
        exchangeRate.enforceConstraints(rateProvider, command.signers)

        val sellAmount = exchangeRate.sellAmount
        proposal.outputStates().filter { it.data is Cash.State }.map { it.data as Cash.State }.filter { it.owner == ourIdentity }.singleOrNull { it.amount.toDecimal() == sellAmount.toDecimal() && it.amount.token.product == sellAmount.token } ?: throw Exception("Missing bought output state of $sellAmount signed from buyer!")

        // TODO identityService could do with a method that identifies a well known party from a key, whether anonymised or not.
        val counterPartyIdentities = proposal.commands().single { it.value is Cash.Commands.Move }.signers.map { serviceHub.identityService.partyFromKey(it) }.map { serviceHub.identityService.requireWellKnownPartyFromAnonymous(it!!) }
        require(proposal.commands().any { it.value is Cash.Commands.Move && session.counterparty.owningKey in counterPartyIdentities.map { it.owningKey } }) { "Missing move cash command from buyer." }
        return CheckProposalResult(rateProvider, exchangeRate)
    }

    private data class CheckProposalResult(val rateProvider: Party, val exchangeRate: ExchangeUsingRate)

    private fun ExchangeUsingRate.enforceConstraints(rateProvider: Party, signers: List<PublicKey>) {

        require(areEqual(buyAmount.toDecimal().multiply(rate.value), sellAmount.toDecimal())) { "Command values do not match!" }
        require(buyAmount.token == rate.to) { "Buy amount currency does not match rate from currency!" }
        require(sellAmount.token == rate.from) { "Sell amount currency does not match rate to currency!" }

        require(rateProvider.owningKey in signers) { "Exchange rate was not signed by rate-provider!" }
        require(rateProvider.name in RATE_PROVIDERS_WHITELIST) { "Rate provider ${rateProvider.name} is not admissible." }
    }

    private fun areEqual(one: BigDecimal, other: BigDecimal): Boolean {

        return one.compareTo(other) == 0
    }

    @Suspendable
    private fun filtering(elem: Any): Boolean = elem is Command<*> && elem.value is ExchangeUsingRate
}