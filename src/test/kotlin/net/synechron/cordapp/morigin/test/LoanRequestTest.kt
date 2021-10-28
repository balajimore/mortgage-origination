package net.synechron.cordapp.morigin.test

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.Amount
import net.corda.finance.DOLLARS
import net.synechron.cordapp.morigin.state.LoanState
import net.synechron.cordapp.morigin.state.RealEstateProperty
import org.junit.Test

class LoanRequestTest : AbstractFlowUnitTests() {

    @Test
    fun issueNFTTokenToAccount() {
        val acc1 = "PropertyOwner1"
        custodianNode.createAccount(acc1)
        //Find account else throw error.
        custodianNode.services.accountByName(acc1)

        val bankAcc = "BankCreditAdminDept1"
        bankNode.createAccount(bankAcc)
        //Find account else throw error.
        bankNode.services.accountByName(bankAcc)

        val nftTokenId = custodianNode.issueNFTTokenTo(
                Amount.parseCurrency("$100000"),
                "1000 SQ.FT.",
                "KP Park, Pune, India",
                acc1
        ).run { this.getId() }

        val states = custodianNode.getStates(NonFungibleToken::class.java)
        assert(states.size == 1)

        // Loan Request - Custodian.
        val loanRequestId = custodianNode.createLoanRequest(
                nftTokenId,
                bankAcc,
                60000.DOLLARS).run { this.getId() }

        val loanRqts = bankNode.getStates(LoanState::class.java)
        assert(loanRqts.size ==1)
        assert(loanRqts[0].linearId.toString() == loanRequestId)
        assert(loanRqts[0].nftPropertyTokenId.toString() == nftTokenId)


        // Bank NOde.
        /* val nftToken2 = bankNode.getStates(NonFungibleToken::class.java)
         assert(nftToken2.size == 1)
         assert(nftToken2[0].linearId.toString() == nftTokenId)*/

        val evolvableToken2 = bankNode.getStates(RealEstateProperty::class.java)
        assert(evolvableToken2.size == 1)
        assert(loanRqts[0].evolvablePropertyTokenId == evolvableToken2[0].linearId)

        val loanRqt2 = bankNode.getStates(LoanState::class.java).single()
        assert(loanRqt2.linearId.toString() == loanRequestId)
        assert(loanRqt2.nftPropertyTokenId.toString() == nftTokenId)


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