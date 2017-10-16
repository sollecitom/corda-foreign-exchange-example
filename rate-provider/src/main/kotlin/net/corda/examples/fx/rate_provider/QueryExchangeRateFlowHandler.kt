package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * Handler flow for [QueryExchangeRateFlow].
 */
@InitiatedBy(QueryExchangeRateFlow::class)
class QueryExchangeRateFlowHandler(private val session: FlowSession) : FlowLogic<Unit>() {

    private companion object {

        val RECEIVING_REQUEST = object : ProgressTracker.Step("Receiving query exchange rate request") {}
        val INVOKING_CORDA_SERVICE = object : ProgressTracker.Step("Invoking corda service for the exchange rate") {}
        val RETURNING_RATE = object : ProgressTracker.Step("Generating spend to fulfil exchange") {}
    }

    override val progressTracker = ProgressTracker(RECEIVING_REQUEST, INVOKING_CORDA_SERVICE, RETURNING_RATE)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = RECEIVING_REQUEST
        val (from, to, timestamp) = session.receive<QueryExchangeRateRequest>().unwrap { it }

        progressTracker.currentStep = INVOKING_CORDA_SERVICE
        val oracle = serviceHub.cordaService(RateProvider::class.java)
        val rate = oracle.rateAtTime(from, to, timestamp)

        progressTracker.currentStep = RETURNING_RATE
        session.send(QueryExchangeRateResponse(rate))
    }
}