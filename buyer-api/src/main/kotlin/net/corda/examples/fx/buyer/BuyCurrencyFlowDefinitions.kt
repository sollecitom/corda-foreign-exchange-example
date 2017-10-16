package net.corda.examples.fx.buyer

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

/**
 * Definition of the flow that initiates the foreign currency exchange.
 * Useful to avoid exposing Buyer's logic to the Seller.
 */
@InitiatingFlow
abstract class BuyCurrencyFlowDefinition : FlowLogic<Unit>()