package net.corda.examples.fx.buyer_app.service

import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import java.util.*

interface FXService {

    fun buyMoneyAmount(amount: MoneyAmount, saleCurrency: Currency): Result

    fun selfIssueCash(amount: MoneyAmount)

    fun balance(): Balance

    data class Result(val missingAmount: MoneyAmount? = null)
}