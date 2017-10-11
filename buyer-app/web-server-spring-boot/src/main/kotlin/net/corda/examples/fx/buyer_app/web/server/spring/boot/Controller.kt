package net.corda.examples.fx.buyer_app.web.server.spring.boot

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import net.corda.core.utilities.loggerFor
import net.corda.examples.fx.buyer_app.service.FXService
import net.corda.examples.fx.buyer_app.web.server.BuyerServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
private class CustomerController @Autowired constructor(private val service: FXService) : BuyerServer() {

    companion object {
        private val logger = loggerFor<CustomerController>()
    }

    @RequestMapping(value = "/exchangeRate", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json; charset=utf-8"))
    fun readExchangeRate(@RequestParam("from") fromParam: String?, @RequestParam("to") toParam: String?): ResponseEntity<String> {

        logger.info("Received read exchange rate request.")

        val from = fromParam?.let { Currency.getInstance(it) }
        val to = toParam?.let { Currency.getInstance(it) }
        if (from == null || to == null) {
            return ResponseEntity.badRequest().body(message("Unspecified 'from' and 'to' currency codes query parameters."))
        }
        return try {
            val rate = service.queryRate(from = from, to = to)
            if (rate == null) {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(message("Not exchange rate found."))
            } else {
                ResponseEntity.ok(toJson(rate, from, to).toString())
            }
        } catch (e: Throwable) {
            logger.error("Error while trying to retrieve exchange rate.", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message("Unknown error."))
        }
    }

    @RequestMapping(value = "/cash", method = arrayOf(RequestMethod.GET), produces = arrayOf("application/json; charset=utf-8"))
    fun readCashBalance(): ResponseEntity<String> {

        logger.info("Received read cash balance request.")
        return try {
            val balance = service.balance()
            ResponseEntity.ok(toJson(balance).toString())
        } catch (e: Throwable) {
            logger.error("Error while trying to read balance.", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message("Unknown error."))
        }
    }

    @RequestMapping(value = "/cash", method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json; charset=utf-8"), consumes = arrayOf("application/json; charset=utf-8"))
    fun selfIssueCash(@RequestBody body: String?): ResponseEntity<String> {

        body ?: return ResponseEntity.badRequest().body(message("Missing mandatory request body."))
        val json = parser.parse(body) as JsonObject
        logger.info("Received cash issuance request with payload: $json")
        val amount = fromJson(json)
        return try {
            service.selfIssueCash(amount)
            ResponseEntity.status(HttpStatus.CREATED).build()
        } catch (e: Throwable) {
            logger.error("Error while trying to self issue cash.", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message("Unknown error."))
        }
    }

    @RequestMapping(value = "/purchases", method = arrayOf(RequestMethod.POST), produces = arrayOf("application/json; charset=utf-8"), consumes = arrayOf("application/json; charset=utf-8"))
    fun attemptToBuyCash(@RequestBody body: String?): ResponseEntity<String> {

        body ?: return ResponseEntity.badRequest().body(message("Missing mandatory request body."))
        val json = parser.parse(body) as JsonObject
        logger.info("Received cash acquisition request with payload: $json")
        val amount = fromJson(json["amount"].asJsonObject)
        val currency = Currency.getInstance(json["currency"].string)
        return try {
            val result = service.buyMoneyAmount(amount, currency)
            if (result.missingAmount == null) {
                ResponseEntity.status(HttpStatus.CREATED).build()
            } else {
                ResponseEntity.unprocessableEntity().body(jsonObject("message" to "Insufficient funds to buy given money amount. Missing ${result.missingAmount}.", "missing" to toJson(result.missingAmount!!)).toString())
            }
        } catch (e: Throwable) {
            logger.error("Error while trying to buy money.", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message("Unknown error."))
        }
    }
}