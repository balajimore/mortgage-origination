package net.synechron.cordapp.morigin.custodian.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.flows.AbstractLoanRequestTo
import net.synechron.cordapp.morigin.state.LoanState
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.time.Instant
import java.util.*

// ****************************************************************************
// TODO BUG -- There is bug in Corda libarary.
//  Failing to distribute reference states of NFT type to counterparty.
// ****************************************************************************

@StartableByRPC
class LoanRequestTo(
        val nftPropertyTokenId: String,
        val creditAdminDeptAccName: String,
        val loanAmount: Amount<Currency>
) : AbstractLoanRequestTo<String>() {

    @Suspendable
    override fun call(): String {
        // Get RealEstate Property NFT and Evolvable tokens.
        val nftPropertyToken = serviceHub.getStateByLinearId(nftPropertyTokenId, NonFungibleToken::class.java)
        val evolvableTokenId = nftPropertyToken.state.data.tokenType.tokenIdentifier
        val evolvableToken = serviceHub.getStateByLinearId(evolvableTokenId, RealEstateProperty::class.java)

        // Counterparty Bank's credit administration department account Name.
        val creditAdminDeptAccInfo = serviceHub.accountByName(creditAdminDeptAccName)
        val creditAdminDeptAccParty = subFlow(RequestKeyForAccount(creditAdminDeptAccInfo))
        val tokenHolder = nftPropertyToken.state.data.holder
        val outEnquiry = LoanState(
                nftPropertyToken.state.data.linearId,
                evolvableToken.state.data.linearId,
                evolvableToken.ref,
                loanAmount,
                tokenHolder,
                creditAdminDeptAccParty
        )

        //Build transaction.
        val txb = TransactionBuilder(serviceHub.firstNotary())
                .addCommand(LoanStateContract.Commands.Create(), listOf(tokenHolder.owningKey))
                .addReferenceState(evolvableToken.referenced())
                //.addReferenceState(nftPropertyToken.referenced())
                .addOutputState(outEnquiry)
                .setTimeWindow(Instant.now(), 30.seconds)

        // Verify and sign tx.
        txb.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txb, tokenHolder.owningKey)

        // Tx finality.
        val session = initiateFlow(creditAdminDeptAccInfo.host)
        subFlow(FinalityFlow(stx, listOf(session)))

        return "LoanRequestId: ${outEnquiry.linearId}"
    }
}