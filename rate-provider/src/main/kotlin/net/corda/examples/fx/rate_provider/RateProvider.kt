package net.corda.examples.fx.rate_provider

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import net.corda.finance.EUR
import net.corda.finance.GBP
import net.corda.finance.USD
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Rate Provider [CordaService] able to provide data to handler flows.
 */
@CordaService
class RateProvider(override val services: ServiceHub) : SingletonSerializeAsToken(), OracleService {

    companion object {

        private val logger = loggerFor<RateProvider>()
    }

    private val exchangeRates: List<ExchangeRate> = listOf(
            ExchangeRate(USD, GBP, 0.74),
            ExchangeRate(USD, EUR, 0.84),
            ExchangeRate(GBP, USD, 1.36),
            ExchangeRate(GBP, EUR, 1.14),
            ExchangeRate(EUR, USD, 1.20),
            ExchangeRate(EUR, GBP, 0.88)
    )

    /**
     * Provides the exchange rate between two currency at a point in time.
     *
     * @param from the source [Currency].
     * @param to the destination [Currency].
     * @param timestamp the [Instant] at which the rate is required.
     *
     * @return a [BigDecimal] representing the exchange rate, or `null` if no exchange rate is known.
     */
    fun rateAtTime(from: Currency, to: Currency, timestamp: Instant): BigDecimal? {

        val rate = exchangeRates.singleOrNull { it.from == from && it.to == to }?.value
        logger.info("Asked to provide $from to $to exchange rate at time $timestamp. Was: $rate.")
        return rate
    }

    private fun validateExchangeRate(command: ExchangeUsingRate): Boolean {

        return command.rateProviderName == ourIdentity.name && command.rate.value == rateAtTime(command.rate.from, command.rate.to, command.timestamp)
    }

    override val validatingFunctions = setOf(ExchangeUsingRate::class using this::validateExchangeRate)

    override val logger: Logger
        get() = RateProvider.logger
}