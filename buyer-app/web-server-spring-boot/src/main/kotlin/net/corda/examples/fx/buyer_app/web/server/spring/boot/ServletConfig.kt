package net.corda.examples.fx.buyer_app.web.server.spring.boot

import net.corda.examples.fx.buyer_app.web.server.configuration.ServerConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
private open class ServletConfig @Autowired constructor(private val configuration: ServerConfiguration) {

    @Bean
    open fun containerCustomizer(): EmbeddedServletContainerCustomizer {

        return EmbeddedServletContainerCustomizer { container -> container.setPort(configuration.httpPort) }
    }
}