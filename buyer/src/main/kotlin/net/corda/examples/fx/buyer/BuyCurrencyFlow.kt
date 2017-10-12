package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
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

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {

        // TODO introduce progressTracker

        logger.info("Asking provider ${rateProvider.name} for exchange rate.")
        val timestamp = Instant.now()
        val exchangeRate = subFlow(QueryExchangeRateFlow(saleCurrency, buyAmount.token, timestamp, rateProvider)) ?: throw Exception("No exchange rate between $saleCurrency and ${buyAmount.token}.")

        logger.info("Generating spend using exchange rate $exchangeRate.")
        val sellAmount = Amount.fromDecimal(buyAmount.toDecimal().multiply(exchangeRate), saleCurrency)
        val (txBuilder, anonymisedSpendOwnerKeys) = Cash.generateSpend(TransactionBuilder(notary), serviceHub, sellAmount, seller)

        logger.info("Sending transaction proposal to the seller.")
        val rate = ExchangeRate(saleCurrency, buyAmount.token, exchangeRate)
        txBuilder.addCommand(ExchangeUsingRate(rate = rate, timestamp = timestamp, buyAmount = buyAmount, sellAmount = sellAmount, rateProviderName = rateProvider.name), rateProvider.owningKey)

        val sellerSession = initiateFlow(seller)
        subFlow(SendTransactionProposal(sellerSession, txBuilder))

        logger.info("Receiving signed transaction from the seller.")
        val signedBySeller = subFlow(ReceiveSignedTransaction(sellerSession))

        logger.info("Checking transaction sent by the seller.")
        signedBySeller.tx.apply {
            val counterPartyIdentities = commands.single { it.value is Cash.Commands.Move }.signers.map { serviceHub.identityService.partyFromKey(it) }.map { serviceHub.identityService.requireWellKnownPartyFromAnonymous(it!!) }
            require(commands.any { it.value is Cash.Commands.Move && seller.owningKey in counterPartyIdentities.map { it.owningKey } }) { "Missing move cash command from buyer." }

            require(outputStates.filterIsInstance<Cash.State>().filter { it.owner == ourIdentity }.singleOrNull { it.amount.toDecimal() == buyAmount.toDecimal() && it.amount.token.product == buyAmount.token } != null) { "Missing bought output state of $buyAmount in transaction signed by seller!" }
            require(commands.any { it.value is Cash.Commands.Move && seller.owningKey in it.signers }) { "Missing move cash command from seller." }
        }

        val finalTx = anonymisedSpendOwnerKeys.fold(serviceHub.addSignature(signedBySeller), serviceHub::addSignature)
        subFlow(FinalityFlow(finalTx))
    }
}