package net.corda.examples.fx.rate_provider

import net.corda.core.node.CordaPluginRegistry
import net.corda.core.serialization.SerializationCustomization
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.loggerFor
import java.math.BigDecimal

class Plugin : CordaPluginRegistry() {

    companion object {
        private val logger = loggerFor<Plugin>()
    }

    override fun customizeSerialization(custom: SerializationCustomization): Boolean {

        logger.info("Registering custom serialization.")

        custom.addToWhitelist(BigDecimal::class.java)
        custom.addToWhitelist(TransactionBuilder::class.java)
        return super.customizeSerialization(custom)
    }
}