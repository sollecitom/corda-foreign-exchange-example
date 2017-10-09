//package net.corda.examples.fx.rate_provider
//
//import co.paralleluniverse.fibers.Suspendable
//import net.corda.core.crypto.DigitalSignature
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.InitiatingFlow
//import net.corda.core.identity.Party
//import net.corda.core.serialization.CordaSerializable
//import net.corda.core.transactions.FilteredTransaction
//import net.corda.core.transactions.WireTransaction
//import net.corda.core.utilities.unwrap
//
//@InitiatingFlow
//class SignExchangeRateFlow(private val tx: WireTransaction, private val partialMerkleTx: FilteredTransaction, private val oracle: Party) : FlowLogic<DigitalSignature.WithKey>() {
//
//    @Suspendable
//    override fun call(): DigitalSignature.WithKey {
//
//        val resp = sendAndReceive<DigitalSignature.WithKey>(oracle, SignExchangeRateRequest(partialMerkleTx))
//        return resp.unwrap { sig ->
//            check(sig.by == oracle.owningKey)
//            tx.checkSignature(sig)
//            sig
//        }
//    }
//}
//
//@CordaSerializable
//data class SignExchangeRateRequest(val ftx: FilteredTransaction)