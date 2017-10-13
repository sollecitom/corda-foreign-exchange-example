package net.corda.examples.fx.rate_provider

import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import net.corda.examples.fx.shared.domain.CurrencyValues.DOLLARS
import net.corda.examples.fx.shared.domain.CurrencyValues.EUROS
import net.corda.examples.fx.shared.domain.CurrencyValues.POUNDS
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import org.slf4j.Logger
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@CordaService
class RateProvider(override val services: ServiceHub) : SingletonSerializeAsToken(), OracleService {

    companion object {

        private val logger = loggerFor<RateProvider>()
    }

    private val exchangeRates: List<ExchangeRate> = listOf(
            ExchangeRate(DOLLARS, POUNDS, 0.74),
            ExchangeRate(DOLLARS, EUROS, 0.84),
            ExchangeRate(POUNDS, DOLLARS, 1.36),
            ExchangeRate(POUNDS, EUROS, 1.14),
            ExchangeRate(EUROS, DOLLARS, 1.20),
            ExchangeRate(EUROS, POUNDS, 0.88)
    )

    fun rateAtTime(from: Currency, to: Currency, timestamp: Instant): BigDecimal? {

        val rate = exchangeRates.singleOrNull { it.from == from && it.to == to }?.value
        logger.info("Asked to provide $from to $to exchange rate at time $timestamp. Was: $rate.")
        return rate
    }

    private fun validateExchangeRate(command: ExchangeUsingRate): Boolean {

        return command.rateProviderName == services.myInfo.legalIdentities[0].name && command.rate.value == rateAtTime(command.rate.from, command.rate.to, command.timestamp)
    }

    override val validatingFunctions = setOf(ExchangeUsingRate::class using this::validateExchangeRate)

    override val logger: Logger
        get() = RateProvider.logger
}