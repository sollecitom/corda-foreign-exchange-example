package net.corda.examples.fx.shared.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.flows.AbstractCashFlow
import net.corda.finance.flows.CashIssueFlow
import java.util.*

// TODO this is just to avoid the ugly OpaqueBytes leaking in the flows.
@InitiatingFlow
@StartableByRPC
class IssueCashFlow(private val amount: Amount<Currency>, private val notary: Party) : FlowLogic<AbstractCashFlow.Result>() {

    @Suspendable
    override fun call(): AbstractCashFlow.Result {

        return subFlow(CashIssueFlow(amount, OpaqueBytes.of(0), notary))
    }
}