package net.corda.examples.fx.rate_provider

import net.corda.core.contracts.Command
import net.corda.core.crypto.DigitalSignature
import net.corda.core.crypto.MerkleTreeException
import net.corda.core.node.PluginServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.ServiceType
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.utilities.loggerFor
import net.corda.examples.fx.shared.domain.CurrencyValues.DOLLARS
import net.corda.examples.fx.shared.domain.CurrencyValues.EUROS
import net.corda.examples.fx.shared.domain.CurrencyValues.POUNDS
import net.corda.examples.fx.shared.domain.ExchangeRate
import net.corda.examples.fx.shared.domain.ExchangeUsingRate
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@CordaService
class RateProvider(val services: PluginServiceHub) : SingletonSerializeAsToken() {

    companion object {
        @JvmField
        val type = ServiceType.getServiceType("fx", "rate_provider")
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

    fun sign(ftx: FilteredTransaction): DigitalSignature.WithKey {

        logger.info("Asked to sign transaction.")
        // Check the partial Merkle tree is valid.
        if (!ftx.verify()) throw MerkleTreeException("Couldn't verify partial Merkle tree.")

        fun commandValidator(elem: Command<*>): Boolean {
            // This Oracle only cares about commands which have its public key in the signers list.
            // This Oracle also only cares about Prime.Create commands.
            if (!(services.legalIdentityKey in elem.signers && elem.value is ExchangeUsingRate)) {
                throw IllegalArgumentException("Oracle received unknown command (not in signers or not ExchangeUsingRate).")
            }
            val command = elem.value as ExchangeUsingRate
            return command.rate.value == rateAtTime(command.rate.from, command.rate.to, command.timestamp)
        }

        // This function is run for each non-hash leaf of the Merkle tree.
        // We only expect to see commands.
        fun check(elem: Any): Boolean {
            logger.info("GOT ELEMENT!: " + elem)
            return when (elem) {
                is Command<*> -> commandValidator(elem)
                else -> throw IllegalArgumentException("Oracle received data of different type than expected.")
            }
        }

        // Validate the commands.
        val leaves = ftx.filteredLeaves
        if (!leaves.checkWithFun(::check)) throw IllegalArgumentException()

        // Sign over the Merkle root and return the digital signature.
        val signature = services.keyManagementService.sign(ftx.rootHash.bytes, services.legalIdentityKey)
        return DigitalSignature.WithKey(services.legalIdentityKey, signature.bytes)
    }
}