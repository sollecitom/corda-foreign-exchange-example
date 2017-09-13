package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.core.utilities.NetworkHostAndPort
import net.corda.nodeapi.User

internal interface CordaNodeConfiguration {

    val nodeAddress: NetworkHostAndPort
    val user: User
}