package net.corda.examples.fx.shared.plugin

import net.corda.core.serialization.SerializationWhitelist
import java.math.BigDecimal

interface FxSerializationWhitelist : SerializationWhitelist {
    override val whitelist: List<Class<*>>
        get() = listOf(BigDecimal::class.java)
}