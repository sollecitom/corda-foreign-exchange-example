package net.corda.examples.fx.buyer_app.web.server.ktor

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import net.corda.core.utilities.loggerFor
import net.corda.examples.fx.buyer_app.service.FXService
import net.corda.examples.fx.buyer_app.web.server.BuyerServer
import net.corda.examples.fx.buyer_app.web.server.configuration.ServerConfiguration
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.features.Compression
import org.jetbrains.ktor.features.DefaultHeaders
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.request.receiveText
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.post
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.PostConstruct

@Component
private class Server @Autowired internal constructor(private val service: FXService, private val configuration: ServerConfiguration) : BuyerServer() {

    companion object {
        private val logger = loggerFor<Server>()
    }

    @PostConstruct
    internal fun init() {

        logger.info("Initialising server on port ${configuration.httpPort}")
        val server = embeddedServer(Netty, configuration.httpPort) {
            install(DefaultHeaders)
            install(Compression)
            routing {
                route("/exchangeRate") {
                    get("") {
                        readExchangeRate(call)
                    }
                }
                route("/cash") {
                    post("") {
                        selfIssueCash(call)
                    }
                    get("") {
                        readCashBalance(call)
                    }
                }
                route("/purchases") {
                    post("") {
                        attemptToBuyCash(call)
                    }
                }
            }
        }
        server.start(wait = true)
    }

    private suspend fun readExchangeRate(call: ApplicationCall) {

        logger.info("Received read exchange rate request.")
        try {
            val from = call.request.queryParameters["from"]?.let(this::parseCurrencyCode)
            val to = call.request.queryParameters["to"]?.let(this::parseCurrencyCode)
            if (from == null || to == null) {
                call.respondText(text = message("Unspecified 'from' and 'to' currency codes query parameters."), contentType = ContentType.Application.Json, status = HttpStatusCode.BadRequest)
                return
            }

            val rate = service.queryRate(from = from, to = to)
            if (rate == null) {
                call.respondText(text = message("No exchange rate found."), contentType = ContentType.Application.Json, status = HttpStatusCode.NotFound)
            } else {
                call.respondText(text = toJson(rate, from, to).toString(), contentType = ContentType.Application.Json, status = HttpStatusCode.OK)
            }
        } catch (e: IllegalArgumentException) {
            call.respondText(text = message(e.localizedMessage), contentType = ContentType.Application.Json, status = HttpStatusCode.BadRequest)
        } catch (e: Throwable) {
            logger.error("Error while trying to retrieve exchange rate.", e)
            call.respondText(text = message("Unknown error."), contentType = ContentType.Application.Json, status = HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun attemptToBuyCash(call: ApplicationCall) {

        val json = parser.parse(call.receiveText()) as JsonObject
        logger.info("Received cash acquisition request with payload: $json")
        try {
            val amount = fromJson(json["amount"].asJsonObject)
            val currency = parseCurrencyCode(json["currency"].string)
            val result = service.buyMoneyAmount(amount, currency)
            if (result.missingAmount == null) {
                call.respondText(text = "", contentType = ContentType.Application.Json, status = HttpStatusCode.Created)
            } else {
                call.respondText(text = jsonObject("message" to "Insufficient funds to buy given money amount. Missing ${result.missingAmount}.", "missing" to toJson(result.missingAmount!!)).toString(), contentType = ContentType.Application.Json, status = HttpStatusCode(422, "Unprocessable Entity"))
            }
        } catch (e: IllegalArgumentException) {
            call.respondText(text = message(e.localizedMessage), contentType = ContentType.Application.Json, status = HttpStatusCode.BadRequest)
        } catch (e: Throwable) {
            logger.error("Error while trying to buy money.", e)
            call.respondText(text = message("Unknown error."), contentType = ContentType.Application.Json, status = HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun selfIssueCash(call: ApplicationCall) {

        val json = parser.parse(call.receiveText()) as JsonObject
        logger.info("Received cash issuance request with payload: $json")
        try {
            val amount = fromJson(json)
            service.selfIssueCash(amount)
            call.respondText(text = "", contentType = ContentType.Application.Json, status = HttpStatusCode.Created)
        } catch (e: IllegalArgumentException) {
            call.respondText(text = message(e.localizedMessage), contentType = ContentType.Application.Json, status = HttpStatusCode.BadRequest)
        } catch (e: Throwable) {
            logger.error("Error while trying to self issue cash.", e)
            call.respondText(text = message("Unknown error."), contentType = ContentType.Application.Json, status = HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun readCashBalance(call: ApplicationCall) {

        logger.info("Received read cash balance request.")
        try {
            val balance = service.balance()
            call.respondText(text = toJson(balance).toString(), contentType = ContentType.Application.Json, status = HttpStatusCode.OK)
        } catch (e: Throwable) {
            logger.error("Error while trying to read balance.", e)
            call.respondText(text = message("Unknown error."), contentType = ContentType.Application.Json, status = HttpStatusCode.InternalServerError)
        }
    }
}