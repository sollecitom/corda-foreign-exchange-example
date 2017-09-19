package net.corda.examples.fx.buyer_app.web.server.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val HTTP_PORT = "config.http.port"

@Component
private class ResolvedConfiguration @Autowired private constructor(
        @Value("\${$HTTP_PORT}") override val httpPort: Int) : ServerConfiguration {
}