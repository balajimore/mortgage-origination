package net.synechron.cordapp.morigin.test

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.getOrThrow
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.*
import net.synechron.cordapp.morigin.flows.FlowHelper
import org.junit.After
import org.junit.Before
import org.junit.Test

abstract class AbstractFlowUnitTests : FlowHelper {
    lateinit var network: MockNetwork
    lateinit var custodianNode: StartedMockNode
    lateinit var bankNode: StartedMockNode
    private lateinit var notary: Party
    lateinit var custodian: Party
    lateinit var bank: Party

    @Before
    fun setup() {
        val corDappsForAllNodes = listOf(
            "net.synechron.cordapp.morigin.contract",
            "net.synechron.cordapp.morigin.exception",
            "net.synechron.cordapp.morigin.flows",
            "net.synechron.cordapp.morigin.plugin",
            "net.synechron.cordapp.morigin.schema",
            "net.synechron.cordapp.morigin.state",
            "net.corda.finance",
            "com.r3.corda.lib.accounts"
        )

        network = MockNetwork(
            threadPerNode = true,
            notarySpecs = listOf(
                MockNetworkNotarySpec(
                    name = CordaX500Name(organisation = "Notary", locality = "London", country = "GB"),
                    validating = false
                )
            ),
            cordappPackages = corDappsForAllNodes,
            networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
        )

        custodianNode = network.createNode(
            parameters = MockNodeParameters(
                legalName = CordaX500Name(organisation = "Custodian", locality = "NY", country = "US"),
                additionalCordapps = listOf(
                    TestCordapp.findCordapp(scanPackage = "net.synechron.cordapp.morigin.custodian"),
                    TestCordapp.findCordapp("net.synechron.cordapp.morigin.commons")
                )
            )
        )

        bankNode = network.createNode(
            parameters = MockNodeParameters(
                legalName = CordaX500Name(organisation = "Bank", locality = "NY", country = "US"),
                additionalCordapps = listOf(
                   // TestCordapp.findCordapp(scanPackage = "net.synechron.cordapp.morigin.bank"),
                    TestCordapp.findCordapp("net.synechron.cordapp.morigin.commons")
                )
            )
        )

        notary = network.defaultNotaryIdentity
        custodian = custodianNode.info.chooseIdentity()
        bank = bankNode.info.chooseIdentity()
    }

    @After
    open fun tearDown() {
        network.stopNodes()
    }

    fun StartedMockNode.createAccount(accountName: String) {
        this.startFlow(net.synechron.cordapp.morigin.commons.flows.CreateNewAccount(accountName)).getOrThrow()
    }
}