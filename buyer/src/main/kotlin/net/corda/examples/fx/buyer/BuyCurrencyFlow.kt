package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.internal.ResolveTransactionsFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
import net.corda.examples.fx.rate_provider.SignExchangeRateFlow
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import java.time.Instant
import java.util.*
import java.util.function.Predicate

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
            Cash.generateSpend(serviceHub, TransactionBuilder(notary = notary), sellAmount, ourIdentity)
        } catch (e: InsufficientBalanceException) {
            throw CashException("Insufficient cash for spend: ${e.message}", e)
        }

        val rate = ExchangeRate(saleCurrency, buyAmount.token, exchangeRate)
        txBuilder.addCommand(ExchangeUsingRate(rate = rate, timestamp = timestamp, buyAmount = buyAmount, sellAmount = sellAmount), rateProvider.owningKey)

        logger.info("Sending signed transaction to seller.")
        val sellerSession = initiateFlow(seller)
        val signedBySeller = sellerSession.sendAndReceive<SignedTransaction>(txBuilder).unwrap { it }

        signedBySeller.tx.apply {
            require(commands.singleOrNull { it.value is Cash.Commands.Move } != null) { "Missing move cash command from seller." }
            require(outputStates.filterIsInstance<Cash.State>().filter { it.owner == ourIdentity }.singleOrNull { it.amount.toDecimal() == buyAmount.toDecimal() && it.amount.token.product == buyAmount.token } != null) { "Missing bought output state of $buyAmount in transaction signed by seller!" }
        }

        val dependencyTxIDs = signedBySeller.tx.inputs.map { it.txhash }.toSet()
        subFlow(ResolveTransactionsFlow(dependencyTxIDs, sellerSession))

        val partialMerkleTree = signedBySeller.tx.buildFilteredTransaction(Predicate { filtering(it) })
        logger.info("Asking provider to sign rate $rate.")
        val rateProviderSignature = subFlow(SignExchangeRateFlow(signedBySeller.tx, partialMerkleTree, rateProvider))

        val finalTx = anonymisedSpendOwnerKeys.fold(serviceHub.addSignature(signedBySeller).withAdditionalSignature(rateProviderSignature), serviceHub::addSignature)
        subFlow(FinalityFlow(finalTx))
    }
}

@Suspendable
fun filtering(elem: Any): Boolean = elem is Command<*> && elem.value is ExchangeUsingRate