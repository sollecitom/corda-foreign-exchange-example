package net.corda.examples.fx.buyer_app.domain

import java.math.BigDecimal
import java.util.*

interface FXAdapter {

    fun exchangeAmount(amount: MoneyAmount, saleCurrency: Currency): MoneyAmount?

    fun issueCash(amount: MoneyAmount)

    fun balance(): Balance

    fun queryRate(from: Currency, to: Currency): BigDecimal?
}