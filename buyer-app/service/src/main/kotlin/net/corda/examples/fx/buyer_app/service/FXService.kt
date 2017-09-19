package net.corda.examples.fx.buyer_app.service

import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import java.math.BigDecimal
import java.util.*

interface FXService {

    fun buyMoneyAmount(amount: MoneyAmount, saleCurrency: Currency): Result

    fun selfIssueCash(amount: MoneyAmount)

    fun balance(): Balance

    fun queryRate(from: Currency, to: Currency): BigDecimal?

    data class Result(val missingAmount: MoneyAmount? = null)
}