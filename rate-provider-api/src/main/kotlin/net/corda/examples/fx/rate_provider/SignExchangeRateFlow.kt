package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.unwrap

@InitiatingFlow
class SignExchangeRateFlow(private val tx: WireTransaction, private val partialMerkleTx: FilteredTransaction, private val self: Party) : FlowLogic<TransactionSignature>() {

    @Suspendable
    override fun call(): TransactionSignature {

        val session = initiateFlow(self)
        val resp = session.sendAndReceive<TransactionSignature>(SignExchangeRateRequest(partialMerkleTx))
        return resp.unwrap { sig ->
            check(sig.by == self.owningKey)
            tx.checkSignature(sig)
            sig
        }
    }
}

@CordaSerializable
data class SignExchangeRateRequest(val ftx: FilteredTransaction)