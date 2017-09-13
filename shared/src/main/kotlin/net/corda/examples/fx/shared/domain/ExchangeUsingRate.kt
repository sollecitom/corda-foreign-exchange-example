package net.corda.examples.fx.shared.domain

import net.corda.core.contracts.Amount
import net.corda.core.contracts.CommandData
import java.time.Instant
import java.util.*

data class ExchangeUsingRate(val rate: ExchangeRate, val timestamp: Instant, val buyAmount: Amount<Currency>, val sellAmount: Amount<Currency>) : CommandData