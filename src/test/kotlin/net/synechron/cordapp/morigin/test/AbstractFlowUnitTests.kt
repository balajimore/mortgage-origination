package net.synechron.cordapp.morigin.test

import net.corda.core.contracts.Amount
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.node.*
import net.synechron.cordapp.morigin.bank.flows.LoanRequestToResponder
import net.synechron.cordapp.morigin.flows.FlowHelper
import org.junit.After
import org.junit.Before
import java.util.*

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
                "com.r3.corda.lib.accounts",
                "com.r3.corda.lib.accounts.contracts",
                "com.r3.corda.lib.accounts.workflows",
                "com.r3.corda.lib.tokens.contracts",
                "com.r3.corda.lib.tokens.workflows"
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
        custodian = custodianNode.info.legalIdentities[0]
        bank = bankNode.info.legalIdentities[0]

        listOf(LoanRequestToResponder::class.java).forEach {
            bankNode.registerInitiatedFlow(it)
        }
    }

    @After
    open fun tearDown() {
        network.stopNodes()
    }

    fun StartedMockNode.createAccount(accountName: String) {
        this.startFlow(net.synechron.cordapp.morigin.commons.flows.CreateNewAccount(accountName))
                .get()
        network.waitQuiescent()
    }

    fun StartedMockNode.createLoanRequest(nftPropertyTokenId: String,
                                          creditAdminDeptAccName: String,
                                          loanAmount: Amount<Currency>): String {
        val result = this.startFlow(net.synechron.cordapp.morigin.custodian.flows.LoanRequestTo(
                nftPropertyTokenId,
                creditAdminDeptAccName,
                loanAmount)).get()
        network.waitQuiescent()
        return result;
    }

    fun StartedMockNode.issueNFTTokenTo(propertyValue: Amount<Currency>,
                                        constructionArea: String,
                                        propertyAddress: String,
                                        issueToAccName: String): String {
        val nftTokenId = this.startFlow(
                net.synechron.cordapp.morigin.custodian.flows.IssueNftTokenTo(
                        propertyValue,
                        constructionArea,
                        propertyAddress,
                        issueToAccName
                )
        ).get()
        network.waitQuiescent()

        return nftTokenId
    }

    protected fun <T : ContractState> StartedMockNode.getStates(clazz: Class<T>): List<T> {
        return this.transaction {
            this.services.vaultService.queryBy(clazz).states.map { it.state.data }
        }
    }

    fun <T : ContractState> StartedMockNode.getStates(
            accountName: String,
            clazz: Class<T>,
            stateStatus: Vault.StateStatus = Vault.StateStatus.UNCONSUMED
    ): List<StateAndRef<T>> {
        val serviceHub = this.services
        return this.transaction {
            val accountParty = serviceHub.accountParty(accountName)
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                    listOf(accountParty), null, stateStatus, null
            )
            serviceHub.vaultService.queryBy(clazz, queryCriteria).states
        }
    }

    protected fun String.getId() = this.substring(this.indexOf(": ") + 2)
}