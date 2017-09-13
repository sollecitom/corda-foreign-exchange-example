package net.corda.examples.fx.buyer_app.domain

import java.math.BigDecimal
import java.util.*

data class MoneyAmount(val value: BigDecimal, val currency: Currency)

val Double.DOLLARS: MoneyAmount
    get() = MoneyAmount(BigDecimal.valueOf(this), Currency.getInstance("USD"))

val Double.POUNDS: MoneyAmount
    get() = MoneyAmount(BigDecimal.valueOf(this), Currency.getInstance("GBP"))

val Double.EUROS: MoneyAmount
    get() = MoneyAmount(BigDecimal.valueOf(this), Currency.getInstance("EUR"))