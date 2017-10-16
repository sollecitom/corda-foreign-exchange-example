package net.corda.examples.fx.seller

import net.corda.core.transactions.TransactionBuilder
import net.corda.examples.fx.shared.plugin.FxSerializationWhitelist

/**
 * Custom behaviour for the Seller CorDapp.
 */
class Plugin : FxSerializationWhitelist {

    override val whitelist = super.whitelist + TransactionBuilder::class.java
}