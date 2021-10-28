package net.synechron.cordapp.morigin.test

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.Amount
import net.corda.finance.DOLLARS
import net.synechron.cordapp.morigin.state.*
import org.junit.Test

class CreateLoanLifecycleTests : AbstractFlowUnitTests() {

    @Test
    fun issueNFTTokenRequestForLoanAndDoAutoValuation() {
        val creditAdministrationDeptAcc1 = "ABC_Bank_Credit_Administration_dept1"
        val ownerAcc1 = "NFT_Owner1"

        //region Create Accounts for nodes.
        custodianNode.createAccount(ownerAcc1)
        //Find account else throw error.
        custodianNode.services.accountByName(ownerAcc1)

        // Create and verify account on bank's node.
        bankNode.createAccount(creditAdministrationDeptAcc1)
        bankNode.services.accountByName(creditAdministrationDeptAcc1)
        //endregion


        //region Custodian Node issue NFT token.
        val nftTokenId = custodianNode.issueNFTTokenTo(
                Amount.parseCurrency("$100000"),
                "1000 SQ.FT.",
                "KP Park, Pune, India",
                ownerAcc1
        ).run { this.getId() }

        val states = custodianNode.getStates(NonFungibleToken::class.java)
        assert(states.size == 1)

        val states2 = bankNode.getStates(NonFungibleToken::class.java)
        assert(states2.isEmpty())

        val nftState = custodianNode.services.getStateByLinearId(
                nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState.state.data == states[0])
        // Check account for tokens.
        val nftState2 = custodianNode.services.getStateByLinearId(
                ownerAcc1, nftTokenId, NonFungibleToken::class.java
        )
        assert(nftState2 == nftState)

        val evolvableTokens = custodianNode.getStates(RealEstateProperty::class.java)
        assert(evolvableTokens.size == 1)
        val evolvableToken = evolvableTokens[0]
        assert(evolvableToken.linearId.toString() == nftState.state.data.tokenType.tokenIdentifier)
        //endregion

        //region Verify Banks and Appraiser node Vault has not any tokens.
        val bStates = bankNode.getStates(NonFungibleToken::class.java)
        assert(bStates.isEmpty())
        val bStates2 = bankNode.getStates(RealEstateProperty::class.java)
        assert(bStates2.isEmpty())

        val aStates = appraiserNode.getStates(NonFungibleToken::class.java)
        assert(aStates.isEmpty())
        val aStates2 = appraiserNode.getStates(RealEstateProperty::class.java)
        assert(aStates2.isEmpty())
        //endregion

        // Create Loan Request
        val loanId = custodianNode.createLoanRequest(
                nftTokenId, creditAdministrationDeptAcc1, 60000.DOLLARS).run { this.getId() }

        // Verify Vault of Custodian and Bank after loan request.
        val loans = bankNode.getStates(LoanState::class.java)
        assert(loans.size == 1)
        assert(loans[0].linearId.toString() == loanId)
        assert(loans[0].nftPropertyTokenId.toString() == nftTokenId)
        assert(loans[0].evolvablePropertyTokenId == evolvableToken.linearId)
        assert(loans[0].status == LoanStatus.PENDING)

        // Bank NOde.
        val evolvableToken2 = bankNode.getStates(RealEstateProperty::class.java)
        assert(evolvableToken2.size == 1)
        assert(evolvableToken2[0].linearId == evolvableToken.linearId)

        val bLoans = bankNode.getStates(LoanState::class.java)
        assert(bLoans.size == 1)
        assert(bLoans[0].linearId.toString() == loanId)
        assert(bLoans[0].nftPropertyTokenId.toString() == nftTokenId)
        assert(bLoans[0].evolvablePropertyTokenId == evolvableToken2[0].linearId)

        // Appraiser NOde.
        assert(appraiserNode.getStates(LoanState::class.java).isEmpty())
        assert(appraiserNode.getStates(RealEstateProperty::class.java).isEmpty())
        network.waitQuiescent()

        // Wait to complete execution of scheduled flows.
        Thread.sleep(30000)
        network.waitQuiescent()
        // Verify Appraiser node to check auto PropertyValuation request creation.
        val bPropVals = bankNode.getStates(PropertyValuation::class.java)
        if (bPropVals.isEmpty() || bPropVals[0].status != AppraisalStatus.COMPLETE) {
            // Wait to complete execution of scheduled flows.
            Thread.sleep(30000)
            network.waitQuiescent()
        }
        // Has Appraiser completed the valuation.
        val bPropVals2 = bankNode.getStates(PropertyValuation::class.java)
        assert(bPropVals2.size == 1)
        assert(bPropVals[0].status == AppraisalStatus.COMPLETE)

        // Approve the Loan by bank but PLEDGE Token as collateral by locking/making it as encumbered state.
        val sanctionAmount = getLApprovedLoanAmt(bPropVals[0])
        bankNode.loanSanction(loanId, sanctionAmount)
        network.waitQuiescent()

        // Finally Verify the vault.
        val reprop = custodianNode.getStates(RealEstateProperty::class.java).single()
        assert(reprop.linearId == evolvableToken.linearId)
        // Check for encumbered state.

        val loan = custodianNode.getStates(LoanState::class.java).single()
        assert(loan.linearId.toString() == loanId)
        assert(loan.status == LoanStatus.APPROVED)
        assert(loan.sanctionAmount == sanctionAmount)
        assert(loan.nftPropertyTokenId.toString() == nftTokenId)
        assert(loan.evolvablePropertyTokenId == reprop.linearId)


        val bLoan = bankNode.getStates(LoanState::class.java).single()
        assert(bLoan.linearId.toString() == loanId)
        assert(bLoan.status == LoanStatus.APPROVED)
        assert(bLoan.sanctionAmount == sanctionAmount)
        assert(bLoan.nftPropertyTokenId.toString() == nftTokenId)
        assert(bLoan.evolvablePropertyTokenId == reprop.linearId)
    }
}