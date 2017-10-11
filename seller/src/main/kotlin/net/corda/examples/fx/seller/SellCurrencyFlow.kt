package net.corda.examples.fx.seller

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveStateAndRefFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.examples.fx.buyer.BuyCurrencyFlowDefinition
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.examples.fx.shared.flows.IssueCashFlow
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import java.math.BigDecimal
import java.security.PublicKey
import java.util.*

// TODO use ContractState and Contract to enforce constraints
@InitiatedBy(BuyCurrencyFlowDefinition::class)
class SellCurrencyFlow(private val session: FlowSession) : FlowLogic<Unit>() {

    private companion object {

        val RATE_PROVIDERS_WHITELIST = setOf(CordaX500Name(organisation = "Rate-Provider", locality = "Austin", country = "US"))

        val STARTING = object : ProgressTracker.Step("STARTING") {}
        val GENERATING_SPEND = object : ProgressTracker.Step("GENERATING_SPEND") {}
        val VERIFYING_TRANSACTION = object : ProgressTracker.Step("VERIFYING_TRANSACTION") {}
        val SIGNING_TRANSACTION = object : ProgressTracker.Step("SIGNING_TRANSACTION") {}

        val STEPS: Array<ProgressTracker.Step> = arrayOf(STARTING, GENERATING_SPEND, VERIFYING_TRANSACTION, SIGNING_TRANSACTION)
    }

    override val progressTracker = ProgressTracker(*STEPS)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = STARTING

        subFlow(ReceiveStateAndRefFlow<ContractState>(session))
        val buyerTx = session.receive<TransactionBuilder>().unwrap { it }

        val command = buyerTx.commands().single { it.value is ExchangeUsingRate }
        val exchangeRate = command.value as ExchangeUsingRate
        exchangeRate.enforceConstraints(command.signers)

        val sellAmount = exchangeRate.sellAmount
        buyerTx.outputStates().filter { it.data is Cash.State }.map { it.data as Cash.State }.filter { it.owner == ourIdentity }.singleOrNull { it.amount.toDecimal() == sellAmount.toDecimal() && it.amount.token.product == sellAmount.token } ?: throw Exception("Missing bought output state of $sellAmount signed from buyer!")
        require(buyerTx.commands().any { it.value is Cash.Commands.Move && session.counterparty.owningKey in it.signers }) { "Missing move cash command from buyer." }

        progressTracker.currentStep = GENERATING_SPEND

        // TODO Shall the seller use the same notary specified by the buyer? Any danger?
        // Not sure how to get a notary without plainly hardcoding it
        // val notary = serviceHub.networkMapCache.notaryIdentities.randomOrNull()

        // Self issuing the amount necessary for the trade - obviously just for the sake of the example.
        subFlow(IssueCashFlow(exchangeRate.buyAmount, buyerTx.notary!!))

        val (tx, anonymisedSpendOwnerKeys) = try {
            Cash.generateSpend(buyerTx, serviceHub, exchangeRate.buyAmount, session.counterparty)
        } catch (e: InsufficientBalanceException) {
            throw CashException("Insufficient cash for spend: ${e.message}", e)
        }

        logger.info("Verifying transaction.")
        progressTracker.currentStep = VERIFYING_TRANSACTION
        // TODO remove this TODO: fails here!
        tx.verify(serviceHub)

        logger.info("Signing transaction.")
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = anonymisedSpendOwnerKeys.fold(serviceHub.signInitialTransaction(tx), serviceHub::addSignature)

        logger.info("Sending signed transaction to buyer.")
        subFlow(SendTransactionFlow(session, signedTx))
    }

    private fun ExchangeUsingRate.enforceConstraints(signers: List<PublicKey>) {

        require(areEqual(buyAmount.toDecimal().multiply(rate.value), sellAmount.toDecimal())) { "Command values do not match!" }
        require(buyAmount.token == rate.to) { "Buy amount currency does not match rate from currency!" }
        require(sellAmount.token == rate.from) { "Sell amount currency does not match rate to currency!" }

        val rateProvider = serviceHub.networkMapCache.getPeerByLegalName(rateProviderName) ?: throw Exception("Cannot find specified rate provider $rateProviderName.")
        require(rateProvider.owningKey in signers) { "Exchange rate was not signed by rate-provider!" }
        require(rateProvider.name in RATE_PROVIDERS_WHITELIST) { "Rate provider ${rateProvider.name} is not admissible." }
    }

    private fun areEqual(one: BigDecimal, other: BigDecimal): Boolean {

        logger.info("Comparing $one with $other.")
        return one.compareTo(other) == 0
    }
}

private fun Cash.Companion.generateSpend(builder: TransactionBuilder, serviceHub: ServiceHub, amount: Amount<Currency>, to: AbstractParty, onlyFromParties: Set<AbstractParty> = emptySet()): Pair<TransactionBuilder, List<PublicKey>> {

    val tmpBuilder = TransactionBuilder(builder.notary!!)
    val (_, anonymisedSpendOwnerKeys) = try {
        generateSpend(serviceHub, tmpBuilder, amount, to)
    } catch (e: InsufficientBalanceException) {
        throw CashException("Insufficient cash for spend: ${e.message}", e)
    }
    val resultBuilder = TransactionBuilder(builder.notary!!)
    builder.copyTo(resultBuilder, serviceHub, filterCommands = { it.value !is Cash.Commands.Move })
    tmpBuilder.copyTo(resultBuilder, serviceHub, filterCommands = { it.value !is Cash.Commands.Move })
    resultBuilder.addCommand(Cash.Commands.Move(), builder.commands().filter { it.value is Cash.Commands.Move }.flatMap { it.signers } + tmpBuilder.commands().filter { it.value is Cash.Commands.Move }.flatMap { it.signers })
    return resultBuilder to anonymisedSpendOwnerKeys
}

// TODO remove after solving tmpBuilder need - or perhaps create a similar function in TransactionBuilder
private fun TransactionBuilder.copyTo(
        other: TransactionBuilder,
        serviceHub: ServiceHub,
        filterInputStates: (input: StateAndRef<ContractState>) -> Boolean = { true },
        filterOutputStates: (output: TransactionState<ContractState>) -> Boolean = { true },
        filterCommands: (command: Command<*>) -> Boolean = { true },
        filterAttachments: (attachment: SecureHash) -> Boolean = { true }
) {
    inputStates().map { serviceHub.toStateAndRef<ContractState>(it) }.filter(filterInputStates).forEach { other.addInputState(it) }
    outputStates().filter(filterOutputStates).map { other.addOutputState(it) }
    commands().filter(filterCommands).forEach { other.addCommand(it) }
    attachments().filter(filterAttachments).forEach { other.addAttachment(it) }
}