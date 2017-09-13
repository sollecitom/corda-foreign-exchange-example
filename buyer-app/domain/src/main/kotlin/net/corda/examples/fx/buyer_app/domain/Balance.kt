package net.corda.examples.fx.buyer_app.domain

import java.util.*

data class Balance(val byCurrency: Map<Currency, MoneyAmount>)