package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

@InitiatedBy(SignExchangeRateFlow::class)
class SignExchangeRateFlowHandler(private val session: FlowSession) : FlowLogic<Unit>() {

    private companion object {

        val RECEIVING_REQUEST = object : ProgressTracker.Step("Receiving exchange rate signing request") {}
        val INVOKING_CORDA_SERVICE = object : ProgressTracker.Step("Invoking Corda service to check and verify the rate") {}
        val SIGNING_TRANSACTION = object : ProgressTracker.Step("Signing and returning transaction") {}
    }

    override val progressTracker = ProgressTracker(RECEIVING_REQUEST, INVOKING_CORDA_SERVICE, SIGNING_TRANSACTION)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = RECEIVING_REQUEST
        val request = session.receive<SignExchangeRateRequest>().unwrap { it }

        progressTracker.currentStep = INVOKING_CORDA_SERVICE
        val oracle = serviceHub.cordaService(RateProvider::class.java)

        progressTracker.currentStep = SIGNING_TRANSACTION
        session.send(oracle.sign(request.ftx))
    }
}