package net.corda.examples.fx.buyer_app.adapters.corda

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.nodeapi.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val CORDA_USER_NAME = "config.corda.connection.user.name"
const val CORDA_USER_PASSWORD = "config.corda.connection.user.password"
const val CORDA_NODE_HOST = "config.corda.connection.node.host"
const val CORDA_NODE_RPC_PORT = "config.corda.connection.node.ports.rpc"
const val SELLER_NAME = "config.fx.parties.seller.name"
const val RATE_PROVIDER_NAME = "config.fx.parties.rateprovider.name"
const val NOTARY_NAME = "config.fx.parties.notary.name"

@Component
private class ResolvedCordaNodeConfiguration @Autowired private constructor(
        @Value("\${$CORDA_NODE_HOST}") host: String,
        @Value("\${$CORDA_NODE_RPC_PORT}") rpcPort: Int,
        @Value("\${$CORDA_USER_NAME}") username: String,
        @Value("\${$CORDA_USER_PASSWORD}") password: String,
        @Value("\${$SELLER_NAME}") val seller: String,
        @Value("\${$RATE_PROVIDER_NAME}") val rateProvider: String,
        @Value("\${$NOTARY_NAME}") val notary: String) : CordaNodeConfiguration {

    override val user = User(username, password, setOf("ALL"))

    override val nodeAddress = NetworkHostAndPort(host, rpcPort)

    override val sellerName: CordaX500Name = CordaX500Name.parse(seller)

    override val rateProviderName: CordaX500Name = CordaX500Name.parse(rateProvider)

    override val notaryName: CordaX500Name = CordaX500Name.parse(notary)
}