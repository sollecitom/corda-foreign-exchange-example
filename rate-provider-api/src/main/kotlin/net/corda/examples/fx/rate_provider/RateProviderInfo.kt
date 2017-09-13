package net.corda.examples.fx.rate_provider

import net.corda.core.node.services.ServiceType

object RateProviderInfo {

    val serviceName = ServiceType.getServiceType("fx", "rate_provider")
}