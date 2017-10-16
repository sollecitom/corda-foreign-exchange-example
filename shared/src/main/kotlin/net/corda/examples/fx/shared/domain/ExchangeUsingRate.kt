package net.corda.examples.fx.shared.domain

import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandData
import net.corda.core.identity.CordaX500Name
import java.time.Instant
import java.util.*

/**
 * Command that enforces an [ExchangeRate] to be used as part of an exchange foreign currency transaction.
 *
 * @param rate the [ExchangeRate] mandated.
 * @param timestamp the timestamp at which the [rate] was valid.
 * @param buyAmount the amount to be bought.
 * @param sellAmount the amount to be sold.
 * @param rateProviderName the [CordaX500Name] of the rate provider.
 */
data class ExchangeUsingRate(val rate: ExchangeRate, val timestamp: Instant, val buyAmount: Amount<Currency>, val sellAmount: Amount<Currency>, val rateProviderName: CordaX500Name) : CommandData