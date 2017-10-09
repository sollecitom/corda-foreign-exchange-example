//package net.corda.examples.fx.buyer
//
//import co.paralleluniverse.fibers.Suspendable
//import net.corda.contracts.asset.Cash
//import net.corda.core.contracts.Amount
//import net.corda.core.contracts.Command
//import net.corda.core.flows.FinalityFlow
//import net.corda.core.flows.ResolveTransactionsFlow
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.identity.Party
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.ProgressTracker
//import net.corda.core.utilities.unwrap
//import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
//import net.corda.examples.fx.rate_provider.RateProviderInfo
//import net.corda.examples.fx.rate_provider.SignExchangeRateFlow
//import net.corda.examples.fx.shared.domain.ExchangeRate
//import net.corda.examples.fx.shared.domain.ExchangeUsingRate
//import java.time.Instant
//import java.util.*
//import java.util.function.Predicate
//
//@StartableByRPC
//class BuyCurrencyFlow(buyAmount: Amount<Currency>, saleCurrency: Currency, seller: Party) : BuyCurrencyFlowDefinition(buyAmount, saleCurrency, seller) {
//
//    override val progressTracker = ProgressTracker()
//
//    @Suspendable
//    override fun call() {
//
//        val notary = serviceHub.networkMapCache.getAnyNotary()
//
//        logger.info("Asking provider for exchange rate.")
//
//        val timestamp = Instant.now()
//        val rateProviderNode = serviceHub.networkMapCache.getNodesWithService(RateProviderInfo.serviceName).single()
//        val exchangeRate = subFlow(QueryExchangeRateFlow(saleCurrency, buyAmount.token, timestamp, rateProviderNode.legalIdentity)) ?: throw Exception("No exchange rate between $saleCurrency and ${buyAmount.token}.")
//        val sellAmount = Amount.fromDecimal(buyAmount.toDecimal().multiply(exchangeRate), saleCurrency)
//
//        logger.info("Generating spend using exchange rate $exchangeRate.")
//
//        val (spendTx, _) = serviceHub.vaultService.generateSpend(tx = TransactionBuilder(notary = notary), amount = sellAmount, to = seller)
//
//        val tx = TransactionBuilder(notary = notary)
//        spendTx.inputStates().map { tx.addInputState(serviceHub.toStateAndRef<Cash.State>(it)) }
//        spendTx.outputStates().map { tx.addOutputState(it.data) }
//
//        val rate = ExchangeRate(saleCurrency, buyAmount.token, exchangeRate)
//
//        logger.info("Asking provider to sign rate $rate.")
//
//        tx.addCommand(ExchangeUsingRate(rate = rate, timestamp = timestamp, buyAmount = buyAmount, sellAmount = sellAmount), rateProviderNode.legalIdentity.owningKey)
//
//        logger.info("Sending signed transaction to seller.")
//        val sellerSignedTx = sendAndReceive<SignedTransaction>(seller, tx).unwrap { it }
//
//        sellerSignedTx.tx.apply {
//            require(commands.singleOrNull { it.value is Cash.Commands.Move } != null) { "Missing Move Cash command from seller." }
//            require(outputStates.filterIsInstance<Cash.State>().filter { it.owner == serviceHub.myInfo.legalIdentity }.singleOrNull { it.amount.toDecimal() == buyAmount.toDecimal() && it.amount.token.product == buyAmount.token } != null) { "Missing bought output state of $buyAmount in transaction signed by seller!" }
//        }
//
//        val dependencyTxIDs = sellerSignedTx.tx.inputs.map { it.txhash }.toSet()
//        subFlow(ResolveTransactionsFlow(dependencyTxIDs, seller))
//
//        val partialMerkleTree = sellerSignedTx.tx.buildFilteredTransaction(Predicate { filtering(it) })
//        val rateProviderSignature = subFlow(SignExchangeRateFlow(sellerSignedTx.tx, partialMerkleTree, rateProviderNode.legalIdentity))
//
//        subFlow(FinalityFlow(serviceHub.addSignature(sellerSignedTx).withAdditionalSignature(rateProviderSignature)))
//    }
//}
//
//@Suspendable
//fun filtering(elem: Any): Boolean = elem is Command<*> && elem.value is ExchangeUsingRate