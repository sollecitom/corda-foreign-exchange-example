package net.corda.examples.fx.shared.domain

import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandData
import net.corda.core.identity.CordaX500Name
import java.time.Instant
import java.util.*

data class ExchangeUsingRate(val rate: ExchangeRate, val timestamp: Instant, val buyAmount: Amount<Currency>, val sellAmount: Amount<Currency>, val rateProviderName: CordaX500Name) : CommandData