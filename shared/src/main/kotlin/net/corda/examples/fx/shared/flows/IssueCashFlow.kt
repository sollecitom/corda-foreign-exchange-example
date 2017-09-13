package net.corda.examples.fx.shared.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import net.corda.flows.CashIssueFlow
import java.util.*

@InitiatingFlow
@StartableByRPC
class IssueCashFlow(private val amount: Amount<Currency>, private val recipient: Party) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val notary = serviceHub.networkMapCache.getAnyNotary()!!
        subFlow(CashIssueFlow(amount, OpaqueBytes.of(0), recipient, notary, false))
    }
}