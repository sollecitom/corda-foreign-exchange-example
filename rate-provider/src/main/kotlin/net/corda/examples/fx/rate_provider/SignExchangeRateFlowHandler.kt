package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap

@InitiatedBy(SignExchangeRateFlow::class)
class SignExchangeRateFlowHandler(private val session: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        logger.info("Received request to sign exchange rate.")
        val request = session.receive<SignExchangeRateRequest>().unwrap { it }
        val oracle = serviceHub.cordaService(RateProvider::class.java)
        session.send(oracle.sign(request.ftx))
    }
}