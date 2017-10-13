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

@InitiatingFlow
class QueryExchangeRateFlow(private val from: Currency, private val to: Currency, private val timestamp: Instant, private val self: Party) : FlowLogic<BigDecimal?>() {

    private companion object {

        val ASKING_RATE_TO_SERVICE = object : ProgressTracker.Step("Asking query exchange rate to Corda service") {}
        val RETURNING_RATE = object : ProgressTracker.Step("Returning exchange rate") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_TO_SERVICE, RETURNING_RATE)

    @Suspendable
    override fun call(): BigDecimal? {

        progressTracker.currentStep = ASKING_RATE_TO_SERVICE
        val session = initiateFlow(self)
        val resp = session.sendAndReceive<QueryExchangeRateResponse>(QueryExchangeRateRequest(from, to, timestamp))

        progressTracker.currentStep = RETURNING_RATE
        return resp.unwrap { it.rate }
    }
}

@CordaSerializable
data class QueryExchangeRateRequest(val from: Currency, val to: Currency, val timestamp: Instant)

@CordaSerializable
data class QueryExchangeRateResponse(val rate: BigDecimal?)