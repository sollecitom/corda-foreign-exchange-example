package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
import net.corda.examples.fx.rate_provider.RateProviderInfo
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@InitiatingFlow
@StartableByRPC
class ExposeExchangeRateFlow(private val fromCurrency: Currency, private val toCurrency: Currency) : FlowLogic<ExposeExchangeRateResponse>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): ExposeExchangeRateResponse {

        logger.info("Asking provider for exchange rate.")

        val timestamp = Instant.now()
        val rateProviderNode = serviceHub.networkMapCache.getNodesWithService(RateProviderInfo.serviceName).single()
        val exchangeRate = subFlow(QueryExchangeRateFlow(fromCurrency, toCurrency, timestamp, rateProviderNode.legalIdentity))

        logger.info("Exchange rate from ${fromCurrency.currencyCode} to ${toCurrency.currencyCode}: $exchangeRate (provide by ${rateProviderNode.legalIdentity}.")

        return ExposeExchangeRateResponse(exchangeRate)
    }
}

@CordaSerializable
data class ExposeExchangeRateResponse(val rate: BigDecimal?)