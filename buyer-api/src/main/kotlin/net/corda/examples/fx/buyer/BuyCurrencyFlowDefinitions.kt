package net.corda.examples.fx.buyer

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
// TODO check
//@StartableByRPC
abstract class BuyCurrencyFlowDefinition : FlowLogic<Unit>()