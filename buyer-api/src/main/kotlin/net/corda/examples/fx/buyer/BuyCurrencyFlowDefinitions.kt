package net.corda.examples.fx.buyer

import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import java.util.*

@InitiatingFlow
@StartableByRPC
abstract class BuyCurrencyFlowDefinition(val buyAmount: Amount<Currency>, val saleCurrency: Currency, val seller: Party) : FlowLogic<Unit>()