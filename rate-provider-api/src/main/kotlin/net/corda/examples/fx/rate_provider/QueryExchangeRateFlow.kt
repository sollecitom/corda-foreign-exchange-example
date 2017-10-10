package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.unwrap
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@InitiatingFlow
class QueryExchangeRateFlow(private val from: Currency, private val to: Currency, private val timestamp: Instant, private val self: Party) : FlowLogic<BigDecimal?>() {

    @Suspendable
    override fun call(): BigDecimal? {

        logger.info("Asking back-end for rate exchange.")
        val session = initiateFlow(self)
        val resp = session.sendAndReceive<QueryExchangeRateResponse>(QueryExchangeRateRequest(from, to, timestamp))
        return resp.unwrap { it.rate }
    }
}

@CordaSerializable
data class QueryExchangeRateRequest(val from: Currency, val to: Currency, val timestamp: Instant)

@CordaSerializable
data class QueryExchangeRateResponse(val rate: BigDecimal?)