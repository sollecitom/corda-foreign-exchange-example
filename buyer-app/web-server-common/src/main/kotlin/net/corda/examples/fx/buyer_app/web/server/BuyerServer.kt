package net.corda.examples.fx.buyer_app.web.server

import com.github.salomonbrys.kotson.double
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import java.math.BigDecimal
import java.util.*

open class BuyerServer {

    protected val parser = JsonParser()

    protected fun fromJson(json: JsonObject): MoneyAmount {

        return MoneyAmount(BigDecimal.valueOf(json["value"].double), parseCurrencyCode(json["currency"].string))
    }

    protected fun toJson(amount: MoneyAmount): JsonObject {

        return jsonObject("value" to amount.value.toDouble(), "currency" to amount.currency.currencyCode)
    }

    protected fun toJson(rate: BigDecimal, from: Currency, to: Currency): JsonObject {

        return jsonObject("rate" to rate.toDouble(), "from" to from.currencyCode, "to" to to.currencyCode)
    }

    protected fun toJson(balance: Balance): JsonObject {

        return jsonObject().apply { balance.byCurrency.forEach { (currency, amount) -> addProperty(currency.currencyCode, amount.value.toDouble()) } }
    }

    protected fun message(text: String): String {

        return jsonObject("message" to text).toString()
    }

    protected fun parseCurrencyCode(it: String): Currency {

        return try {
            Currency.getInstance(it)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid currency code $it.")
        }
    }
}