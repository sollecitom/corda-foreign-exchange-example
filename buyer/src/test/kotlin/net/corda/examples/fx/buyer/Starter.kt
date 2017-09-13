package net.corda.examples.fx.buyer

import net.corda.core.contracts.DOLLARS
import net.corda.core.crypto.entropyToKeyPair
import net.corda.core.getOrThrow
import net.corda.core.identity.Party
import net.corda.core.messaging.startFlow
import net.corda.core.node.services.ServiceInfo
import net.corda.node.services.startFlowPermission
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.testing.driver.driver
import org.bouncycastle.asn1.x500.X500Name
import java.lang.management.ManagementFactory
import java.math.BigInteger
import java.util.*

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to running deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Firstly, run the "Run Example CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports which should be output to the console for each node. They typically start at 5006, 5007,
 *    5008. The "Debug CorDapp" configuration runs with port 5007, which should be "NodeB". In any case, double check
 *    the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
    // No permissions required as we are not invoking flows.
    val user = User("user1", "test", permissions = setOf(startFlowPermission(BuyCurrencyFlow::class.java)))
    driver(isDebug = true, startNodesInProcess = isQuasarAgentSpecified()) {

        startNode(X500Name("CN=Controller,O=R3,OU=corda,L=London,C=UK"), setOf(ServiceInfo(ValidatingNotaryService.type))).getOrThrow()
        val node = startNode(X500Name("CN=NodeA,O=NodeA,L=London,C=UK"), rpcUsers = listOf(user)).getOrThrow()

        node.rpcClientToNode().start(user.username, user.password).use {
            val successful = it.proxy.startFlow(::BuyCurrencyFlow, 10.DOLLARS, Currency.getInstance("GBP"), DUMMY_SELLER).returnValue.getOrThrow()
            println(successful)
        }
        waitForAllNodesToFinish()
    }
}

fun isQuasarAgentSpecified(): Boolean {
    val jvmArgs = ManagementFactory.getRuntimeMXBean().inputArguments
    return jvmArgs.any { it.startsWith("-javaagent:") && it.endsWith("quasar.jar") }
}

val DUMMY_CASH_ISSUER_KEY by lazy { entropyToKeyPair(BigInteger.valueOf(10)) }
/** A dummy, randomly generated issuer party by the name of "Snake Oil Issuer" */
val DUMMY_SELLER by lazy { Party(X500Name("CN=NodeB,O=NodeB,L=London,C=UK"), DUMMY_CASH_ISSUER_KEY.public) }