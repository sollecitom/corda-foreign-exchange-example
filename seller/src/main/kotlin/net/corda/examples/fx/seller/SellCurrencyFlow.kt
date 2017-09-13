package net.corda.examples.fx.seller

import co.paralleluniverse.fibers.Suspendable
import net.corda.contracts.asset.Cash
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ResolveTransactionsFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.examples.fx.buyer.BuyCurrencyFlowDefinition
import net.corda.examples.fx.rate_provider.RateProviderInfo
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.examples.fx.shared.flows.IssueCashFlow
import java.math.BigDecimal

// TODO use ContractState and Contract to enforce constraints
@InitiatedBy(BuyCurrencyFlowDefinition::class)
class SellCurrencyFlow(private val buyer: Party) : FlowLogic<Unit>() {

    private companion object STEP {

        object STARTING : ProgressTracker.Step("STARTING")
        object GENERATING_SPEND : ProgressTracker.Step("GENERATING_SPEND")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("VERIFYING_TRANSACTION")
        object SIGNING_TRANSACTION : ProgressTracker.Step("SIGNING_TRANSACTION")

        val VALUES: Array<ProgressTracker.Step> = arrayOf(STARTING, GENERATING_SPEND, VERIFYING_TRANSACTION, SIGNING_TRANSACTION)
    }

    override val progressTracker = ProgressTracker(*STEP.VALUES)

    @Suspendable
    override fun call() {

        // TODO perform checks!

        val rateProviderNode = serviceHub.networkMapCache.getNodesWithService(RateProviderInfo.serviceName).single()

        val tx = receive<TransactionBuilder>(buyer).unwrap {

            progressTracker.currentStep = STARTING
            val dependencyTxIDs = it.inputStates().map { it.txhash }.toSet()
            subFlow(ResolveTransactionsFlow(dependencyTxIDs, buyer))
            it
        }

        val commandRaw = tx.commands().single { it.value is ExchangeUsingRate }
        require(rateProviderNode.legalIdentity.owningKey in commandRaw.signers) { "Exchange rate was not signed by rate-provider!" }
        val command = commandRaw.value as ExchangeUsingRate
        command.enforceConstraints()

        val sellAmount = command.sellAmount
        tx.outputStates().filter { it.data is Cash.State }.map { it.data as Cash.State }.filter { it.owner == serviceHub.myInfo.legalIdentity }.singleOrNull { it.amount.toDecimal() == sellAmount.toDecimal() && it.amount.token.product == sellAmount.token } ?: throw Exception("Missing bought output state of $sellAmount in transaction signed by seller!")

        val notary = serviceHub.networkMapCache.getAnyNotary()

        // Self issuing the provided amount of money - obviously just for the sake of the example
        progressTracker.currentStep = GENERATING_SPEND

        subFlow(IssueCashFlow(command.buyAmount, serviceHub.myInfo.legalIdentity))
        val (cashBuilder, _) = serviceHub.vaultService.generateSpend(TransactionBuilder(notary = notary), command.buyAmount, buyer)

        cashBuilder.inputStates().map { tx.addInputState(serviceHub.toStateAndRef<Cash.State>(it)) }
        cashBuilder.outputStates().map { tx.addOutputState(it.data) }

        tx.addCommand(Cash.Commands.Move(), serviceHub.myInfo.legalIdentity.owningKey, buyer.owningKey)

        logger.info("Verifying transaction.")
        progressTracker.currentStep = VERIFYING_TRANSACTION
        tx.verify(serviceHub)

        logger.info("Signing transaction.")
        progressTracker.currentStep = SIGNING_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(tx)

        logger.info("Sending signed transaction to buyer.")
        send(buyer, signedTx)
    }

    private fun ExchangeUsingRate.enforceConstraints() {

        require(areEqual(buyAmount.toDecimal().multiply(rate.value), sellAmount.toDecimal())) { "Command values do not match!" }
        require(buyAmount.token == rate.to) { "Buy amount currency does not match rate from currency!" }
        require(sellAmount.token == rate.from) { "Sell amount currency does not match rate to currency!" }
    }

    private fun areEqual(one: BigDecimal, other: BigDecimal): Boolean {

        logger.info("Comparing $one with $other.")
        return one.compareTo(other) == 0
    }
}