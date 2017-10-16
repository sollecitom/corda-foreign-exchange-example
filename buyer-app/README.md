![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Buyer APP

## Basic information

This module is a **not** a CorDapp. It is a standalone web application, managed by the Buyer.

### Submodules

#### web-server-ktor

A web server based on Ktor, JetBrain's Kotlin web server.

#### web-server-spring-boot

A web server based on the Spring Web Framework (Tomcat).

#### web-server-common

Common logic shared by the two parallel server implementations.

#### service

Traditional service layer in a web application. In includes methods available in the web servers to access behaviour.

#### domain

Domain objects and interface to ledger operations.

#### logging

Thin logging facade for Kotlin.

#### corda-fx-adapter

Corda RPC based implementation of the ledger operations interface defined in `domain`.