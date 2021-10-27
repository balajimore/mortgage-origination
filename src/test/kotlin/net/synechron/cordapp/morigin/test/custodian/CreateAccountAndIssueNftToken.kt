package net.synechron.cordapp.morigin.test.custodian

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.finance.DOLLARS
import net.synechron.cordapp.morigin.test.AbstractFlowUnitTests
import org.junit.Test

class CreateAccountAndIssueNftToken : AbstractFlowUnitTests() {
    @Test
    fun createAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)

        val acc2 = "BankCreditAdminDept1"
        bankNode.createAccount(acc2)
        //Find account else throw error.
        bankNode.services.accountByName(acc2)
    }

    @Test
    fun issueNFTTokenToAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)

        val nftTokenId = custodianNode.issueNFTTokenTo(
            100000.DOLLARS,
            "1000 SQ.FT.",
            "KP Park, Pune, India",
            acc1
        )

        val states = custodianNode.getStates(NonFungibleToken::class.java)
        assert(states.size == 1)

        val states2 = bankNode.getStates(NonFungibleToken::class.java)
        assert(states2.isEmpty())

        val nftState = custodianNode.services.getStateByLinearId(
            nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState.state.data === states[0])

        val nftState2 = custodianNode.services.getStateByLinearId(
            acc1, nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState2 == nftState)
    }
}