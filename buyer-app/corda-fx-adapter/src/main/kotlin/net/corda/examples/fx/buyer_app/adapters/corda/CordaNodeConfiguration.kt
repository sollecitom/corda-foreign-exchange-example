package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.nodeapi.User

internal interface CordaNodeConfiguration {

    val nodeAddress: NetworkHostAndPort
    val user: User
    val sellerName: CordaX500Name
    val rateProviderName: CordaX500Name
}