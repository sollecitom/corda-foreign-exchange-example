//package net.corda.examples.fx.rate_provider
//
//import co.paralleluniverse.fibers.Suspendable
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.InitiatingFlow
//import net.corda.core.identity.Party
//import net.corda.core.serialization.CordaSerializable
//import net.corda.core.utilities.unwrap
//import java.math.BigDecimal
//import java.time.Instant
//import java.util.*
//
//@InitiatingFlow
//class QueryExchangeRateFlow(private val from: Currency, private val to: Currency, private val timestamp: Instant, private val oracle: Party) : FlowLogic<BigDecimal?>() {
//
//    @Suspendable
//    override fun call(): BigDecimal? {
//
//        val resp = sendAndReceive<QueryExchangeRateResponse>(oracle, QueryExchangeRateRequest(from, to, timestamp))
//        return resp.unwrap { it.rate }
//    }
//}
//
//@CordaSerializable
//data class QueryExchangeRateRequest(val from: Currency, val to: Currency, val timestamp: Instant)
//
//@CordaSerializable
//data class QueryExchangeRateResponse(val rate: BigDecimal?)