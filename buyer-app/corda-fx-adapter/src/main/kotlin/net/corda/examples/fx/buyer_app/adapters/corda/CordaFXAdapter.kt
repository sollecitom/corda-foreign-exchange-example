package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.internal.randomOrNull
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.getOrThrow
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
        return null
//        return try {
//            rpc.start(configuration.user.username, configuration.user.password).use {
//                logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
//                logger.info("Starting flow BuyCurrency")
//                val sellerNode = findSeller(it) ?: throw Exception("No nodes selling cash found.")
//                it.proxy.startFlow(::BuyCurrencyFlow, amount.toCorda, saleCurrency, sellerNode.legalIdentities.single()).returnValue.getOrThrow()
//                null
//            }
//        } catch (e: InsufficientBalanceException) {
//            logger.info("Insufficient founds to buy $amount.", e)
//            e.amountMissing.toDomain
//        }
    }

    override fun issueCash(amount: MoneyAmount) {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        rpc.start(configuration.user.username, configuration.user.password).use {
            logger.info("Connected to CORDA node ${it.proxy.nodeInfo().legalIdentities[0]}!")
            val notary = it.proxy.notaryIdentities().randomOrNull() ?: throw Exception("No notary found.")
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
            val rateProvider = it.proxy.wellKnownPartyFromX500Name(configuration.rateProviderName) ?: throw Exception("No exchange rate provider found.")
            it.proxy.registeredFlows().forEach { println(it) }
            val response = it.proxy.startFlow(::ExposeExchangeRateFlow, from, to, rateProvider).returnValue.getOrThrow()
            response.rate
        }
    }
}

private val MoneyAmount.toCorda: Amount<Currency>
    get() = Amount.fromDecimal(this.value, this.currency)

private val Amount<*>.toDomain: MoneyAmount
    get() = MoneyAmount(this.toDecimal(), this.token as Currency)