![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# Corda Foreign Exchange Example

## Basic information

Currently based upon Corda M16.

This example illustrates how a foreign currency exchange could be implemented using Corda.

While the functionality is stubbed, the structure shows how to decompose behaviour into different Corda nodes, and how
to use Oracles as part of Flows.

This repository is split into four modules:

1. A shared library which holds types that all parties need to use.
2. A buyer CorDApp, representing an entity that buys foreign currency.
3. A buyer Spring Boot application talking to the buyer node. It includes a UI. 
4. A seller CorDApp, representing an entity that sells foreign currency.
5. A rate provider CorDApp, representing an entity that provides currency exchange rates as an Oracle.

## How to run this example

- From terminal inside project root, run `./gradlew deployNodes` and wait for it to finish.
- From terminal inside project root, run `./build/nodes/runnodes` and wait for it to finish.
- Run/Debug Starter.kt inside project 'buyer-app:web-server-ktor' from Intellij.

## What's available

The available endpoints on localhost:8080 are:

- GET /cash -> returns a Map<Currency, Double> with available cash balance. Initially empty.
- POST /cash e.g., payload {"value": 100.0, "currency": "GBP"} -> issues available cash to the buyer node.
- POST /purchases e.g., payload {"amount": {"value": 10.0, "currency": "USD"}, "currency": "GBP"} -> buys given amount ($10) from seller node by accepting exchange rate (GBP to USD) from rate-provider.