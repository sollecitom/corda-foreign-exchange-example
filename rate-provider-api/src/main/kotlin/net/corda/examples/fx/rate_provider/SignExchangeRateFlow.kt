package net.corda.examples.fx.rate_provider

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.crypto.TransactionSignature
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.FilteredTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

/**
 * Initiating flow to sign a foreign currency exchange transaction.
 * Useful to avoid exposing Rate Provider's logic to clients.
 *
 * @param tx the [WireTransaction] for the Rate Provider to sign.
 * @param partialMerkleTx the [FilteredTransaction] the Rate Provider will have access to.
 * @param self the Rate Provider [Party].
 *
 * @return the [TransactionSignature] from the Rate Provider.
 */
@InitiatingFlow
class SignExchangeRateFlow(private val tx: WireTransaction, private val partialMerkleTx: FilteredTransaction, private val self: Party) : FlowLogic<TransactionSignature>() {

    private companion object {

        val SIGNING_REQUEST = object : ProgressTracker.Step("Inspecting and signing the transaction") {}
        val RETURNING_SIGNATURE = object : ProgressTracker.Step("Returning transaction signature") {}
    }

    override val progressTracker = ProgressTracker(SIGNING_REQUEST, RETURNING_SIGNATURE)

    @Suspendable
    override fun call(): TransactionSignature {

        progressTracker.currentStep = SIGNING_REQUEST
        val session = initiateFlow(self)
        val resp = session.sendAndReceive<TransactionSignature>(SignExchangeRateRequest(partialMerkleTx))

        progressTracker.currentStep = RETURNING_SIGNATURE
        return resp.unwrap { sig ->
            check(sig.by == self.owningKey)
            tx.checkSignature(sig)
            sig
        }
    }
}

/**
 * Request object for [SignExchangeRateFlow].
 */
@CordaSerializable
data class SignExchangeRateRequest(val ftx: FilteredTransaction)