![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Foreign Exchange Example

## Basic information

Currently based upon Corda version 1.0

This example illustrates how a foreign currency exchange could be implemented using Corda.

While the functionality is not extremely complicated, the structure shows how to decompose behaviour into different Corda nodes, 
and how to use Oracles as part of Flows.

It also demonstrate how to integrate CORDA into a web application.

This repository is split into seven modules:
    * The _shared_ module holds general types that all parties (buyers, sellers and exchange rate providers) need to use
    * The _buyer_ module holds the flows and plugin run by the buyer of foreign exchange
    * The _seller_ module holds the flow and plugin run by the seller of foreign exchange
    * The _rate-provider_ module holds the flows, service and plugin run by the exchange rate provider
    * The _buyer-api_ module holds the interface the seller of foreign exchange should use as an initiating flow when
      writing a flow to speak to the buyer
    * The _rate-provider_api_ module holds the interfaces the buyer and seller of foreign exchange should use as
      initiating flows when querying the oracle for an exchange rate or a transaction signature
    * The _buyer-app_ module holds two web server implementations: Ktor(buyer_app:web-server-ktor) and
      Spring-Boot(buyer_app:web-server-spring-boot). These implementations are identical in behaviour and only there to
      demonstrate that the client can be arbitrarily chosen
    The purpose of a modular setup is to avoid having parties knowing the implementation code of other peers.

The Spring Boot and Ktor webserver implementations are identical in behaviour and only there to demonstrate that the client can be arbitrarily chosen.

## How to run this example

- From terminal inside project root, run `./gradlew deployNodes` and wait for it to finish.
- From terminal inside project root, run `./build/nodes/runnodes` and wait for it to finish.
- Run/Debug Starter.kt inside project 'buyer-app:web-server-ktor' **or** 'buyer-app:web-server-spring-boot' from Intellij.

## What's available

As for the buyer app, available driving endpoints on localhost:8080 are:

- GET /cash -> returns a map currency to available cash balance. Initially empty.
- GET /exchangeRate?from=<fromCurrencyCode>&to=<toCurrencyCode> e.g., /exchangeRate?from=GBP&to=USD -> returns the exchange rate between specified currencies, or Not Found. Response body is e.g, {"rate": 1.36, "from": "GBP", "to": "USD"}.
- POST /cash e.g., payload {"value": 1000.0, "currency": "GBP"} -> issues available cash to the buyer node.
- POST /purchases e.g., payload {"amount": {"value": 100.0, "currency": "USD"}, "currency": "GBP"} -> buys given amount ($10) from seller node by accepting exchange rate (GBP to USD) from rate-provider.

## Example calls

1. GET /cash -> empty (200 OK).
2. POST /cash with payload {"value": 1000.0, "currency": "GBP"} -> no response body (201 Created).
3. GET /cash -> {"GBP":1000.0} (200 OK).
4. GET /exchangeRate?from=GBP&to=USD -> {"rate":1.36,"from":"GBP","to":"USD"} (200 OK).
5. POST /purchases with payload {"amount": {"value": 100.0, "currency": "USD"}, "currency": "GBP"} -> no response body (201 Created).
6. GET /cash -> {"USD":100.0,"GBP":864.0} (200 OK).
7. As expected, the dollars have been bought and the remaining pounds £864 = £1000 - 1.36 (£/$) * $100.

## Additional remarks

- All currency codes are available to generate cash.
- Only USD, EUR and GBP are available as input/output for currency exchange.