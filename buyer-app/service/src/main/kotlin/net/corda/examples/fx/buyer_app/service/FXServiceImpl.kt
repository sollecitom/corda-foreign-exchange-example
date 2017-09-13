package net.corda.examples.fx.buyer_app.service

import net.corda.examples.fx.buyer_app.domain.Balance
import net.corda.examples.fx.buyer_app.domain.FXAdapter
import net.corda.examples.fx.buyer_app.domain.MoneyAmount
import net.corda.examples.fx.buyer_app.logging.loggerFor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
private class FXServiceImpl @Autowired private constructor(private val adapter: FXAdapter) : FXService {

    companion object {
        private val logger = loggerFor<FXServiceImpl>()
    }

    override fun buyMoneyAmount(amount: MoneyAmount, saleCurrency: Currency): FXService.Result {

        logger.info("Trying to buy $amount.")
        val missingAmount = adapter.exchangeAmount(amount, saleCurrency)
        return FXService.Result(missingAmount)
    }

    override fun selfIssueCash(amount: MoneyAmount) {

        logger.info("Issuing $amount to self.")
        adapter.issueCash(amount)
    }

    override fun balance(): Balance {

        logger.info("Reading cash balance.")
        return adapter.balance()
    }
}