package net.corda.examples.fx.buyer_app.domain

import java.math.BigDecimal
import java.util.*

data class MoneyAmount(val value: BigDecimal, val currency: Currency)