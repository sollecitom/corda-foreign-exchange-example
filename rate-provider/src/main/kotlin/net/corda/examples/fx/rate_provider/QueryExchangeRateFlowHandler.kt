//package net.corda.examples.fx.rate_provider
//
//import co.paralleluniverse.fibers.Suspendable
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.InitiatedBy
//import net.corda.core.identity.Party
//import net.corda.core.utilities.unwrap
//
//@InitiatedBy(QueryExchangeRateFlow::class)
//class QueryExchangeRateFlowHandler(private val otherParty: Party) : FlowLogic<Unit>() {
//
//    @Suspendable
//    override fun call() {
//
//        val (from, to, timestamp) = receive<QueryExchangeRateRequest>(otherParty).unwrap { it }
//        val oracle = serviceHub.cordaService(RateProvider::class.java)
//        val rate = oracle.rateAtTime(from, to, timestamp)
//        send(otherParty, QueryExchangeRateResponse(rate))
//    }
//}