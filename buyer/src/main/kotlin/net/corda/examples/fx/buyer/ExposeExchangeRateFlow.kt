package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.fx.rate_provider.QueryExchangeRateFlow
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * This flows asks the oracle for the exchange rate between two currencies.
 * It is run by the Buyer so that the RateProvider doesn't have to allow any RPC connection.
 *
 * @param fromCurrency the source [Currency] in the rate.
 * @param toCurrency the destination [Currency] in the rate.
 * @param rateProvider the rate provider oracle [Party].
 *
 * @return the [ExposeExchangeRateResponse] wrapping the rate.
 */
@StartableByRPC
class ExposeExchangeRateFlow(private val fromCurrency: Currency, private val toCurrency: Currency, private val rateProvider: Party) : FlowLogic<ExposeExchangeRateResponse>() {

    private companion object {

        val ASKING_RATE_FROM_PROVIDER = object : ProgressTracker.Step("Asking exchange rate from provider") {}
        val RETURNING_RESPONSE = object : ProgressTracker.Step("Returning response") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_FROM_PROVIDER, RETURNING_RESPONSE)

    @Suspendable
    override fun call(): ExposeExchangeRateResponse {

        logger.info("Asking provider ${rateProvider.name} for exchange rate.")

        val timestamp = Instant.now()
        val exchangeRate = subFlow(QueryExchangeRateFlow(fromCurrency, toCurrency, timestamp, rateProvider))

        logger.info("Exchange rate from ${fromCurrency.currencyCode} to ${toCurrency.currencyCode}: $exchangeRate (provided by $rateProvider.")

        return ExposeExchangeRateResponse(exchangeRate)
    }
}

/**
 * Wraps a [BigDecimal] to make it optional.
 */
@CordaSerializable
data class ExposeExchangeRateResponse(val rate: BigDecimal?)