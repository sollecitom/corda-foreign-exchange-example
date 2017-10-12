package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import java.time.Instant
import java.util.*

@StartableByRPC
class BuyCurrencyFlow(private val buyAmount: Amount<Currency>, private val saleCurrency: Currency, private val rateProvider: Party, private val notary: Party, private val seller: Party) : BuyCurrencyFlowDefinition() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {

        logger.info("Asking provider ${rateProvider.name} for exchange rate.")

        val timestamp = Instant.now()
        val exchangeRate = subFlow(QueryExchangeRateFlow(saleCurrency, buyAmount.token, timestamp, rateProvider)) ?: throw Exception("No exchange rate between $saleCurrency and ${buyAmount.token}.")
        val sellAmount = Amount.fromDecimal(buyAmount.toDecimal().multiply(exchangeRate), saleCurrency)

        logger.info("Generating spend using exchange rate $exchangeRate.")

        val (txBuilder, anonymisedSpendOwnerKeys) = try {
            // TODO maybe replace with the extension function
            Cash.generateSpend(serviceHub, TransactionBuilder(notary = notary), sellAmount, seller)
        } catch (e: InsufficientBalanceException) {
            throw CashException("Insufficient cash for spend: ${e.message}", e)
        }

        val rate = ExchangeRate(saleCurrency, buyAmount.token, exchangeRate)
        txBuilder.addCommand(ExchangeUsingRate(rate = rate, timestamp = timestamp, buyAmount = buyAmount, sellAmount = sellAmount, rateProviderName = rateProvider.name), rateProvider.owningKey)

        logger.info("Sending signed transaction to seller.")
        val sellerSession = initiateFlow(seller)
        // TODO maybe refactor to be automatic
        subFlow(SendStateAndRefFlow(sellerSession, txBuilder.inputStates().map { serviceHub.toStateAndRef<ContractState>(it) }))
        sellerSession.send(txBuilder)
        // TODO maybe refactor to be automatic
        subFlow(FxIdentitySyncFlow.Send(sellerSession, txBuilder))

        // TODO maybe refactor to be automatic
        subFlow(IdentitySyncFlow.Receive(sellerSession))
        val signedBySeller = subFlow(ReceiveTransactionFlow(sellerSession, checkSufficientSignatures = false))

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