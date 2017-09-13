package net.corda.examples.fx.buyer_app

import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("net.corda.examples.fx.buyer_app")
private open class Starter

internal fun main(args: Array<String>) {

    val application = SpringApplication(Starter::class.java)

    application.setBannerMode(Banner.Mode.OFF)
    application.isWebEnvironment = false

    application.run(*args)
}