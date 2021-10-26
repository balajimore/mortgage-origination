# Mortgage Origination CorDapp

It is very simple usecase for loan origination process for real estate property.

## Nodes and Network Parties
![mortgage-origination-network-parties](mortgage-origination-network-parties.PNG)

* **RealEstateAsset Custodian**: A `custodian` who issues the real estate property `NFT tokens` on a ledger. It is also host `customer accounts` who are actual `owner` of these properties.
* **Bank**: It is acting as `lender`, who offers the loans for again real estate collaterals. 
* **Appraiser Company**: has expertise in `property valuation` and respond to valuation request from bank(lender). 
* **Notary**: attestation party on network, provide the UTXO algorithm to avoid double spend and integrity of ledger. 

## Sequence diagrams  
![Sequence Diagram](sequence-diagram.png)

## Software versions
You will need the following installed on your machine before you can start:

* **Java 8**
* **Corda 4.8** 
* Platform version 10
* H2 DB

## Getting Set Up

To get started, clone this repository with:

     git clone https://github.com/corda/cordapp-template-kotlin.git

And change directories to the newly cloned repo:

     cd cordapp-template-kotlin

## Building the CorDapp template:

**Unix:** 

     ./gradlew deployNodes

**Windows:**

     gradlew.bat deployNodes

Note: You'll need to re-run this build step after making any changes to
the template for these to take effect on the node.

## Running the Nodes

Once the build finishes, change directories to the folder where the newly
built nodes are located:

     cd build/nodes

The Gradle build script will have created a folder for each node. You'll
see three folders, one for each node and a `runnodes` script. You can
run the nodes with:

**Unix:**

     ./runnodes --log-to-console --logging-level=DEBUG

**Windows:**

    runnodes.bat --log-to-console --logging-level=DEBUG

You should now have three Corda nodes running on your machine serving 
the template.

When the nodes have booted up, you should see a message like the following 
in the console: 

     Node started up and registered in 5.007 sec

## Interacting with the CorDapp via HTTP

The CorDapp defines a couple of HTTP API end-points and also serves some
static web content. Initially, these return generic template responses.

The nodes can be found using the following port numbers, defined in 
`build.gradle`, as well as the `node.conf` file for each node found
under `build/nodes/partyX`:

     PartyA: localhost:10007
     PartyB: localhost:10010

As the nodes start up, they should tell you which host and port their
embedded web server is running on. The API endpoints served are:

     /api/template/templateGetEndpoint

And the static web content is served from:

     /web/template

## Using the Example RPC Client

The `ExampleClient.kt` file is a simple utility which uses the client
RPC library to connect to a node and log its transaction activity.
It will log any existing states and listen for any future states. To build 
the client use the following Gradle task:

     ./gradlew runTemplateClient

To run the client:

**Via IntelliJ:**

Select the 'Run Template RPC Client'
run configuration which, by default, connect to PartyA (RPC port 10006). Click the
Green Arrow to run the client.

**Via the command line:**

Run the following Gradle task:

     ./gradlew runTemplateClient
     
Note that the template RPC client won't output anything to the console as no state
objects are contained in either PartyA's or PartyB's vault.

## Running the Nodes Across Multiple Machines

See https://docs.corda.net/tutorial-cordapp.html#running-nodes-across-machines.

## Corda Shell Flow Testing
```
flow start com.synechron.cordapp.obligation.borrower.flows.IssueObligationInitiatorFlow amount: $1000, lender: "O=LenderA,L=New York,C=US"

run vaultQuery contractStateType: com.synechron.cordapp.obligation.state.Obligation

flow start net.corda.finance.flows.CashIssueFlow amount: $5000, issuerBankPartyRef: 1234, notary: "O=Notary,L=London,C=GB"

run vaultQuery contractStateType: net.corda.finance.contracts.asset.Cash$State

flow start com.synechron.cordapp.obligation.borrower.flows.SettleObligationInitiatorFlow linearId: "<linearId>", amount: $1000, anonymous: false
```