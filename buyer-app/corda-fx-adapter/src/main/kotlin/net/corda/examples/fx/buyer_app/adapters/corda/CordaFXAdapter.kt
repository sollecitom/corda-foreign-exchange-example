package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.getOrThrow
import net.corda.core.internal.randomOrNull
import net.corda.core.messaging.startFlow
import net.corda.examples.fx.buyer.BuyCurrencyFlow
import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.FXAdapter
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import net.corda.examples.fx.buyer_app.logging.loggerFor
import net.corda.examples.fx.seller.SellerInfo
import net.corda.examples.fx.shared.flows.IssueCashFlow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
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
                logger.info("Connected to CORDA node!")
                logger.info("Starting flow BuyCurrency")
                val sellerNode = it.proxy.networkMapFeed().snapshot.filter { it.advertisedServices.any { it.info.type.id == SellerInfo.serviceName } }.randomOrNull() ?: throw Exception("No nodes selling cash found.")
                it.proxy.startFlow(::BuyCurrencyFlow, amount.toCorda, saleCurrency, sellerNode.legalIdentity).returnValue.getOrThrow()
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
            logger.info("Connected to CORDA node!")
            logger.info("Starting flow IssueCashFlow with recipient ${it.proxy.nodeIdentity().legalIdentity}")
            it.proxy.startFlow(::IssueCashFlow, amount.toCorda, it.proxy.nodeIdentity().legalIdentity).returnValue.getOrThrow()
        }
    }

    override fun balance(): Balance {

        logger.info("Connecting to CORDA node at address ${configuration.nodeAddress}")
        rpc.start(configuration.user.username, configuration.user.password).use {
            logger.info("Connected to CORDA node!")
            logger.info("Reading cash states from the ledger.")
            val byCurrency = it.proxy.getCashBalances().mapValues { MoneyAmount(it.value.toDecimal(), it.key) }
            return Balance(byCurrency)
        }
    }
}

private val MoneyAmount.toCorda: Amount<Currency>
    get() = Amount.fromDecimal(this.value, this.currency)

private val Amount<*>.toDomain: MoneyAmount
    get() = MoneyAmount(this.toDecimal(), this.token as Currency)