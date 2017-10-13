package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.examples.fx.shared.flows.ReceiveSignedTransaction
import net.corda.examples.fx.shared.flows.SendTransactionProposal
import net.corda.examples.fx.shared.flows.generateSpend
import net.corda.finance.contracts.asset.Cash
import java.time.Instant
import java.util.*

@StartableByRPC
class BuyCurrencyFlow(private val buyAmount: Amount<Currency>, private val saleCurrency: Currency, private val rateProvider: Party, private val notary: Party, private val seller: Party) : BuyCurrencyFlowDefinition() {

    private companion object {

        val ASKING_RATE_FROM_PROVIDER = object : ProgressTracker.Step("Asking exchange rate from provider") {}
        val GENERATING_SPEND = object : ProgressTracker.Step("Generating spend to fulfil exchange") {}
        val PROPOSING_EXCHANGE_TO_SELLER = object : ProgressTracker.Step("Proposing currency exchange to seller") {}
        val WAITING_FOR_SELLER_REPLY = object : ProgressTracker.Step("Waiting for seller's reply") {}
        val CHECKING_SELLER_TRANSACTION = object : ProgressTracker.Step("Checking seller's transaction") {}
        val SIGNING_TRANSACTION = object : ProgressTracker.Step("Signing transaction") {}
        val COMMITTING_TRANSACTION = object : ProgressTracker.Step("Committing transaction to the ledger") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_FROM_PROVIDER, GENERATING_SPEND, PROPOSING_EXCHANGE_TO_SELLER, WAITING_FOR_SELLER_REPLY, CHECKING_SELLER_TRANSACTION, SIGNING_TRANSACTION, COMMITTING_TRANSACTION)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = ASKING_RATE_FROM_PROVIDER
        val timestamp = Instant.now()
        val exchangeRate = subFlow(QueryExchangeRateFlow(saleCurrency, buyAmount.token, timestamp, rateProvider)) ?: throw Exception("No exchange rate between $saleCurrency and ${buyAmount.token}.")
        val rate = ExchangeRate(saleCurrency, buyAmount.token, exchangeRate)

        progressTracker.currentStep = GENERATING_SPEND
        val sellAmount = Amount.fromDecimal(buyAmount.toDecimal().multiply(rate.value), saleCurrency)
        val (txBuilder, anonymisedSpendOwnerKeys) = Cash.generateSpend(TransactionBuilder(notary), serviceHub, sellAmount, seller)

        progressTracker.currentStep = PROPOSING_EXCHANGE_TO_SELLER
        val command = ExchangeUsingRate(rate = rate, timestamp = timestamp, buyAmount = buyAmount, sellAmount = sellAmount, rateProviderName = rateProvider.name)
        txBuilder.addCommand(command, rateProvider.owningKey)
        val sellerSession = initiateFlow(seller)
        subFlow(SendTransactionProposal(sellerSession, txBuilder))

        progressTracker.currentStep = WAITING_FOR_SELLER_REPLY
        val signedBySeller = subFlow(ReceiveSignedTransaction(sellerSession))

        progressTracker.currentStep = CHECKING_SELLER_TRANSACTION
        checkTransaction(signedBySeller, command)

        progressTracker.currentStep = SIGNING_TRANSACTION
        val finalTx = anonymisedSpendOwnerKeys.fold(serviceHub.addSignature(signedBySeller), serviceHub::addSignature)

        progressTracker.currentStep = COMMITTING_TRANSACTION
        subFlow(FinalityFlow(finalTx))
    }

    private fun checkTransaction(signedBySeller: SignedTransaction, command: ExchangeUsingRate) {

        signedBySeller.tx.apply {
            val counterPartyIdentities = commands.single { it.value is Cash.Commands.Move }.signers.map { serviceHub.identityService.partyFromKey(it) }.map { serviceHub.identityService.requireWellKnownPartyFromAnonymous(it!!) }
            require(commands.any { it.value is Cash.Commands.Move && seller.owningKey in counterPartyIdentities.map { it.owningKey } }) { "Missing move cash command from buyer." }

            require(outputStates.filterIsInstance<Cash.State>().filter { it.owner == ourIdentity }.singleOrNull { it.amount.toDecimal() == buyAmount.toDecimal() && it.amount.token.product == buyAmount.token } != null) { "Missing bought output state of $buyAmount in transaction signed by seller!" }
            require(commands.any { it.value is Cash.Commands.Move && seller.owningKey in it.signers }) { "Missing move cash command from seller." }
            require(commands.single { it.value is ExchangeUsingRate }.value == command) { "The terms of the exchange have been changed by the seller." }
        }
    }
}