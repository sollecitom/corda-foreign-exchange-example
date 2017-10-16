package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Initiating flow to query an exchange rate.
 * Useful to avoid exposing Rate Provider's logic to clients.
 *
 * @param from the source [Currency] for the exchange.
 * @param to the destination [Currency] for the exchange.
 * @param timestamp the point in time this information is required at.
 * @param self the rate provider [Party].
 *
 * @return a [BigDecimal] exchange rate, or `null` if no rate is known between the two currencies.
 */
@InitiatingFlow
class QueryExchangeRateFlow(private val from: Currency, private val to: Currency, private val timestamp: Instant, private val oracle: Party) : FlowLogic<BigDecimal?>() {

    private companion object {

        val ASKING_RATE_TO_SERVICE = object : ProgressTracker.Step("Asking query exchange rate to Corda service") {}
        val RETURNING_RATE = object : ProgressTracker.Step("Returning exchange rate") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_TO_SERVICE, RETURNING_RATE)

    @Suspendable
    override fun call(): BigDecimal? {

        progressTracker.currentStep = ASKING_RATE_TO_SERVICE
        val session = initiateFlow(oracle)
        val resp = session.sendAndReceive<QueryExchangeRateResponse>(QueryExchangeRateRequest(from, to, timestamp))

        progressTracker.currentStep = RETURNING_RATE
        return resp.unwrap { it.rate }
    }
}

/**
 * Request object for [QueryExchangeRateFlow].
 */
@CordaSerializable
data class QueryExchangeRateRequest(val from: Currency, val to: Currency, val timestamp: Instant)

/**
 * Response object for [QueryExchangeRateFlow].
 */
@CordaSerializable
data class QueryExchangeRateResponse(val rate: BigDecimal?)