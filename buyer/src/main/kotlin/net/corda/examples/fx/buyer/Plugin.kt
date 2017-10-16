package net.corda.examples.fx.buyer

import net.corda.core.transactions.TransactionBuilder
import net.corda.examples.fx.shared.plugin.FxSerializationWhitelist

/**
 * Custom behaviour for the Buyer CorDapp.
 */
class Plugin : FxSerializationWhitelist {

    override val whitelist = super.whitelist + TransactionBuilder::class.java
}