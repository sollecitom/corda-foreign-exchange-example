package net.corda.examples.fx.shared.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.contracts.ContractState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.ReceiveStateAndRefFlow
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.flows.SendTransactionFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

// TODO port to Corda or to a TransactionProposal CorDapp
class SendTransactionProposal(private val session: FlowSession, private val tx: TransactionBuilder, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

    companion object {

        val SHARING_INPUT_STATES = object : ProgressTracker.Step("Sharing input states") {}
        val SHARING_ANONYMISED_IDENTITIES = object : ProgressTracker.Step("Sharing anonymised identities") {}
        val SENDING_TRANSACTION_PROPOSAL = object : ProgressTracker.Step("Sending transaction proposal") {}

        fun tracker() = ProgressTracker(SHARING_INPUT_STATES, SHARING_ANONYMISED_IDENTITIES, SENDING_TRANSACTION_PROPOSAL)
    }

    @Suspendable
    override fun call() {

        progressTracker.currentStep = SHARING_INPUT_STATES
        subFlow(SendStateAndRefFlow(session, tx.inputStates().map { serviceHub.toStateAndRef<ContractState>(it) }))

        progressTracker.currentStep = SHARING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncForBuilderFlow.Send(session, tx))

        progressTracker.currentStep = SENDING_TRANSACTION_PROPOSAL
        session.send(tx)
    }
}

// TODO port to Corda or to a TransactionProposal CorDapp
class ReceiveTransactionProposal(private val session: FlowSession, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<TransactionBuilder>() {

    companion object {

        val RECEIVING_INPUT_STATES = object : ProgressTracker.Step("Receiving input states") {}
        val RECEIVING_ANONYMISED_IDENTITIES = object : ProgressTracker.Step("Receiving anonymised identities") {}
        val RECEIVING_TRANSACTION_PROPOSAL = object : ProgressTracker.Step("Receiving transaction proposal") {}
        val VERIFYING_TRANSACTION_PROPOSAL = object : ProgressTracker.Step("Verifying transaction proposal") {}

        fun tracker() = ProgressTracker(RECEIVING_INPUT_STATES, RECEIVING_ANONYMISED_IDENTITIES, RECEIVING_TRANSACTION_PROPOSAL, VERIFYING_TRANSACTION_PROPOSAL)
    }

    @Suspendable
    override fun call(): TransactionBuilder {

        progressTracker.currentStep = RECEIVING_INPUT_STATES
        subFlow(ReceiveStateAndRefFlow<ContractState>(session))

        progressTracker.currentStep = RECEIVING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncFlow.Receive(session))

        progressTracker.currentStep = RECEIVING_TRANSACTION_PROPOSAL
        val proposal = session.receive<TransactionBuilder>()

        progressTracker.currentStep = VERIFYING_TRANSACTION_PROPOSAL
        return proposal.unwrap {
            it.verify(serviceHub)
            it
        }
    }
}

// TODO port to Corda or to a TransactionProposal CorDapp
class SendSignedTransaction(private val session: FlowSession, private val signedTx: SignedTransaction, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

    companion object {

        val SHARING_ANONYMISED_IDENTITIES = object : ProgressTracker.Step("Sharing anonymised identities") {}
        val SENDING_SIGNED_TRANSACTION = object : ProgressTracker.Step("Sending signed transaction") {}

        fun tracker() = ProgressTracker(SHARING_ANONYMISED_IDENTITIES, SENDING_SIGNED_TRANSACTION)
    }

    @Suspendable
    override fun call() {

        progressTracker.currentStep = SHARING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncFlow.Send(session, signedTx.tx))

        progressTracker.currentStep = SENDING_SIGNED_TRANSACTION
        subFlow(SendTransactionFlow(session, signedTx))
    }
}

// TODO port to Corda or to a TransactionProposal CorDapp
class ReceiveSignedTransaction(private val session: FlowSession, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<SignedTransaction>() {

    companion object {

        val RECEIVING_ANONYMISED_IDENTITIES = object : ProgressTracker.Step("Receiving anonymised identities") {}
        val RECEIVING_SIGNED_TRANSACTION = object : ProgressTracker.Step("Receiving signed transaction") {}

        fun tracker() = ProgressTracker(RECEIVING_ANONYMISED_IDENTITIES, RECEIVING_SIGNED_TRANSACTION)
    }

    @Suspendable
    override fun call(): SignedTransaction {

        progressTracker.currentStep = RECEIVING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncFlow.Receive(session))

        progressTracker.currentStep = RECEIVING_SIGNED_TRANSACTION
        return subFlow(ReceiveTransactionFlow(session, checkSufficientSignatures = false))
    }
}