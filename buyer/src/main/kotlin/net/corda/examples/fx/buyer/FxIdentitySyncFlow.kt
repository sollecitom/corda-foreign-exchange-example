package net.corda.examples.fx.buyer

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

object FxIdentitySyncFlow {

    class Send(private val otherSide: FlowSession, private val tx: TransactionBuilder, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

        companion object {

            object SYNCING_IDENTITIES : ProgressTracker.Step("Syncing identities")

            fun tracker() = ProgressTracker(SYNCING_IDENTITIES)
        }

        @Suspendable
        override fun call() {
            progressTracker.currentStep = SYNCING_IDENTITIES
            val states: List<ContractState> = (tx.inputStates().map { serviceHub.loadState(it) }.requireNoNulls().map { it.data } + tx.outputStates().map { it.data })
            val identities: Set<AbstractParty> = states.flatMap { it.participants }.toSet()
            // Filter participants down to the set of those not in the network map (are not well known)
            val confidentialIdentities = identities
                    .filter { serviceHub.networkMapCache.getNodesByLegalIdentityKey(it.owningKey).isEmpty() }
                    .toList()
            val identityCertificates: Map<AbstractParty, PartyAndCertificate?> = identities
                    .map { Pair(it, serviceHub.identityService.certificateFromKey(it.owningKey)) }.toMap()

            val requestedIdentities: List<AbstractParty> = otherSide.sendAndReceive<List<AbstractParty>>(confidentialIdentities).unwrap { req ->
                require(req.all { it in identityCertificates.keys }) { "${otherSide.counterparty} requested a confidential identity not part of transaction." }
                req
            }
            val sendIdentities: List<PartyAndCertificate?> = requestedIdentities.map {
                val identityCertificate = identityCertificates[it]
                if (identityCertificate != null)
                    identityCertificate
                else
                    throw IllegalStateException("Counterparty requested a confidential identity for which we do not have the certificate path.")
            }
            otherSide.send(sendIdentities)
        }
    }
}