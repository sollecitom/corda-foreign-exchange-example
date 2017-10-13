package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.identity.Party
import net.corda.core.internal.randomOrNull
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
import net.corda.examples.fx.buyer.BuyCurrencyFlow
import net.corda.examples.fx.buyer.ExposeExchangeRateFlow
import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.FXAdapter
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import net.corda.examples.fx.buyer_app.logging.loggerFor
import net.corda.examples.fx.shared.flows.IssueCashFlow
import net.corda.finance.contracts.getCashBalances
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

@Component
private class CordaFXAdapter @Autowired private constructor(private val configuration: CordaNodeConfiguration) : FXAdapter {

    private val rpc = CordaRPCClient(configuration.nodeAddress)

    companion object {

        private val logger = loggerFor<CordaFXAdapter>()
    }

    override fun exchangeAmount(amount: MoneyAmount, saleCurrency: Currency): MoneyAmount? {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        return try {
            rpc.start(configuration.user.username, configuration.user.password).use {
                logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
                logger.info("Starting flow BuyCurrency")
                val notary = configuration.notary(it)
                val rateProvider = configuration.rateProvider(it)
                val sellerNode = configuration.seller(it)
                it.proxy.startFlow(::BuyCurrencyFlow, amount.toCorda, saleCurrency, rateProvider, notary, sellerNode).returnValue.getOrThrow()
                null
            }
        } catch (e: InsufficientBalanceException) {
            logger.info("Insufficient founds to buy $amount.", e)
            e.amountMissing.toDomain
        }
    }

    override fun issueCash(amount: MoneyAmount) {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        rpc.start(configuration.user.username, configuration.user.password).use {
            logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
            val notary = configuration.notary(it)
            logger.info("Starting flow IssueCashFlow with notary $notary")
            it.proxy.startFlow(::IssueCashFlow, amount.toCorda, notary).returnValue.getOrThrow()
        }
    }

    override fun balance(): Balance {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        rpc.start(configuration.user.username, configuration.user.password).use {
            logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
            logger.info("Reading cash states from the ledger.")
            val byCurrency = it.proxy.getCashBalances().mapValues { MoneyAmount(it.value.toDecimal(), it.key) }
            return Balance(byCurrency)
        }
    }

    override fun queryRate(from: Currency, to: Currency): BigDecimal? {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        return rpc.start(configuration.user.username, configuration.user.password).use {
            logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
            val rateProvider = configuration.rateProvider(it)
            val response = it.proxy.startFlow(::ExposeExchangeRateFlow, from, to, rateProvider).returnValue.getOrThrow()
            response.rate
        }
    }
}

private val MoneyAmount.toCorda: Amount<Currency>
    get() = Amount.fromDecimal(this.value, this.currency)

private val Amount<*>.toDomain: MoneyAmount
    get() = MoneyAmount(this.toDecimal(), this.token as Currency)

private fun CordaNodeConfiguration.notary(connection: CordaRPCConnection): Party {

    return connection.proxy.wellKnownPartyFromX500Name(notaryName) ?: throw Exception("No notary with name $notaryName found.")
}

private fun CordaNodeConfiguration.rateProvider(connection: CordaRPCConnection): Party {

    return connection.proxy.wellKnownPartyFromX500Name(rateProviderName) ?: throw Exception("No rate provider with name $rateProviderName found.")
}

private fun CordaNodeConfiguration.seller(connection: CordaRPCConnection): Party {

    return connection.proxy.wellKnownPartyFromX500Name(sellerName) ?: throw Exception("No seller with name $sellerName found.")
}