package net.corda.examples.fx.shared.flows

import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.InsufficientBalanceException
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionState
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashException
import java.security.PublicKey
import java.util.*

// TODO I believe this should be the standard behaviour of Cash.generateSpend(), rather than appending a Cash.Commands.Move() without checks
// TODO port this to Corda
fun Cash.Companion.generateSpend(builder: TransactionBuilder, serviceHub: ServiceHub, amount: Amount<Currency>, to: AbstractParty, onlyFromParties: Set<AbstractParty> = emptySet()): Pair<TransactionBuilder, List<PublicKey>> {

    val tmpBuilder = TransactionBuilder(builder.notary!!)
    val (_, anonymisedSpendOwnerKeys) = try {
        generateSpend(serviceHub, tmpBuilder, amount, to, onlyFromParties)
    } catch (e: InsufficientBalanceException) {
        throw CashException("Insufficient cash for spend: ${e.message}", e)
    }
    val resultBuilder = TransactionBuilder(builder.notary!!)
    builder.copyTo(resultBuilder, serviceHub, filterCommands = { it.value !is Cash.Commands.Move })
    tmpBuilder.copyTo(resultBuilder, serviceHub, filterCommands = { it.value !is Cash.Commands.Move })
    resultBuilder.addCommand(Cash.Commands.Move(), builder.commands().filter { it.value is Cash.Commands.Move }.flatMap { it.signers } + tmpBuilder.commands().filter { it.value is Cash.Commands.Move }.flatMap { it.signers })
    return resultBuilder to anonymisedSpendOwnerKeys
}

// TODO remove after solving tmpBuilder need - or perhaps port this into Corda's TransactionBuilder
fun TransactionBuilder.copyTo(
        other: TransactionBuilder,
        serviceHub: ServiceHub,
        filterInputStates: (input: StateAndRef<ContractState>) -> Boolean = { true },
        filterOutputStates: (output: TransactionState<ContractState>) -> Boolean = { true },
        filterCommands: (command: Command<*>) -> Boolean = { true },
        filterAttachments: (attachment: SecureHash) -> Boolean = { true }
) {

    // TODO perhaps we won't need this if we add this function to TransactionBuilder
    inputStates().map { serviceHub.toStateAndRef<ContractState>(it) }.filter(filterInputStates).forEach { other.addInputState(it) }
    outputStates().filter(filterOutputStates).map { other.addOutputState(it) }
    commands().filter(filterCommands).forEach { other.addCommand(it) }
    attachments().filter(filterAttachments).forEach { other.addAttachment(it) }
}