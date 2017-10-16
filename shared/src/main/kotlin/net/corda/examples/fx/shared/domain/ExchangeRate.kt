package net.corda.examples.fx.shared.domain

import net.corda.core.serialization.CordaSerializable
import java.math.BigDecimal
import java.util.*

/**
 * Domain model for an exchange rate.
 *
 * @param from source [Currency].
 * @param to destination [Currency].
 * @param value exchange rate.
 */
@CordaSerializable
data class ExchangeRate(val from: Currency, val to: Currency, val value: BigDecimal) {

    constructor(from: Currency, to: Currency, value: Double) : this(from, to, BigDecimal.valueOf(value))
}