package net.corda.examples.fx.shared.flows

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
import net.corda.core.utilities.UntrustworthyData

// TODO port to Corda or to a TransactionProposal CorDapp
class SendTransactionProposal(private val session: FlowSession, private val tx: TransactionBuilder, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

    companion object {

        object SHARING_INPUT_STATES : ProgressTracker.Step("Sharing input states")
        object SHARING_ANONYMISED_IDENTITIES : ProgressTracker.Step("Sharing anonymised identities")
        object SENDING_TRANSACTION_PROPOSAL : ProgressTracker.Step("Sending transaction proposal")

        fun tracker() = ProgressTracker(SHARING_INPUT_STATES, SHARING_ANONYMISED_IDENTITIES, SENDING_TRANSACTION_PROPOSAL)
    }

    override fun call() {

        progressTracker.currentStep = SHARING_INPUT_STATES
        subFlow(SendStateAndRefFlow(session, tx.inputStates().map { serviceHub.toStateAndRef<ContractState>(it) }))

        progressTracker.currentStep = SHARING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncForBuilderFlow.Send(session, tx))

        progressTracker.currentStep = SHARING_INPUT_STATES
        session.send(tx)
    }
}

// TODO port to Corda or to a TransactionProposal CorDapp
class ReceiveTransactionProposal(private val session: FlowSession, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<UntrustworthyData<TransactionBuilder>>() {

    companion object {

        object RECEIVING_INPUT_STATES : ProgressTracker.Step("Receiving input states")
        object RECEIVING_ANONYMISED_IDENTITIES : ProgressTracker.Step("Receiving anonymised identities")
        object RECEIVING_TRANSACTION_PROPOSAL : ProgressTracker.Step("Receiving transaction proposal")

        fun tracker() = ProgressTracker(RECEIVING_INPUT_STATES, RECEIVING_ANONYMISED_IDENTITIES, RECEIVING_TRANSACTION_PROPOSAL)
    }

    override fun call(): UntrustworthyData<TransactionBuilder> {

        progressTracker.currentStep = RECEIVING_INPUT_STATES
        subFlow(ReceiveStateAndRefFlow<ContractState>(session))

        progressTracker.currentStep = RECEIVING_ANONYMISED_IDENTITIES
        subFlow(IdentitySyncFlow.Receive(session))

        progressTracker.currentStep = RECEIVING_TRANSACTION_PROPOSAL
        return session.receive()
    }
}

// TODO port to Corda or to a TransactionProposal CorDapp
class SendSignedTransaction(private val session: FlowSession, private val signedTx: SignedTransaction, override val progressTracker: ProgressTracker = tracker()) : FlowLogic<Unit>() {

    companion object {

        object SHARING_ANONYMISED_IDENTITIES : ProgressTracker.Step("Sharing anonymised identities")
        object SENDING_SIGNED_TRANSACTION : ProgressTracker.Step("Sending signed transaction")

        fun tracker() = ProgressTracker(SHARING_ANONYMISED_IDENTITIES, SENDING_SIGNED_TRANSACTION)
    }

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

        object RECEIVING_ANONYMISED_IDENTITIES : ProgressTracker.Step("Receiving anonymised identities")
        object RECEIVING_SIGNED_TRANSACTION : ProgressTracker.Step("Receiving signed transaction")

        fun tracker() = ProgressTracker(RECEIVING_ANONYMISED_IDENTITIES, RECEIVING_SIGNED_TRANSACTION)
    }

    override fun call(): SignedTransaction {

        progressTracker.currentStep = RECEIVING_SIGNED_TRANSACTION
        subFlow(IdentitySyncFlow.Receive(session))

        progressTracker.currentStep = RECEIVING_SIGNED_TRANSACTION
        return subFlow(ReceiveTransactionFlow(session, checkSufficientSignatures = false))
    }
}