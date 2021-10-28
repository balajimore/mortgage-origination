package net.synechron.cordapp.morigin.test

import com.r3.corda.lib.accounts.workflows.internal.accountService
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.Amount
import org.junit.Test

class CreateAccountAndCreateNftTokenTest : AbstractFlowUnitTests() {
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
    fun shareAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)
        val acc1InfoKey = custodianNode.services.accountPublicKey(acc1)

        //Find account else throw error.
        val acc1Info = bankNode.services.accountByName(acc1)
        val acc1InfoByKey = bankNode.services.accountService.accountInfo(acc1InfoKey)!!
        assert(acc1Info == acc1InfoByKey.state.data)
    }


    @Test
    fun issueNFTTokenToAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)

        val nftTokenId = custodianNode.issueNFTTokenTo(
                Amount.parseCurrency("$100000"),
                "1000 SQ.FT.",
                "KP Park, Pune, India",
                acc1
        ).run { this.getId() }

        val states = custodianNode.getStates(NonFungibleToken::class.java)
        assert(states.size == 1)

        val states2 = bankNode.getStates(NonFungibleToken::class.java)
        assert(states2.isEmpty())

        val nftState = custodianNode.services.getStateByLinearId(
                nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState.state.data == states[0])

        val nftState2 = custodianNode.services.getStateByLinearId(
                acc1, nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState2 == nftState)
    }
}