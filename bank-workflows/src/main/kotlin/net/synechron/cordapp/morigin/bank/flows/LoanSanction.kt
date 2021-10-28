package net.synechron.cordapp.morigin.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.CordaRuntimeException
import net.corda.core.contracts.Amount
import net.corda.core.flows.CollectSignatureFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.synechron.cordapp.morigin.bank.flows.exception.PropertyValuationNotComplete
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.contract.RealEstatePropertyContract
import net.synechron.cordapp.morigin.flows.AbstractLoanSanction
import net.synechron.cordapp.morigin.schema.PropertyValuationSchemaV1.PersistedPropertyValuationSchema
import net.synechron.cordapp.morigin.state.AppraisalStatus
import net.synechron.cordapp.morigin.state.LoanState
import net.synechron.cordapp.morigin.state.PropertyValuation
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.util.*

// ****************************************************************************
// TODO BUG -- There is bug in Corda library.
//  Failing to distribute reference states of NFT type to counterparty.
// ****************************************************************************

@StartableByRPC
class LoanSanction(
        val loanRequestId: String,
        val sanctionAmount : Amount<Currency>
) : AbstractLoanSanction<String>() {

    @Suspendable
    override fun call(): String {
        // Validate the sanction amount. It should be more than valuation.
        val exLoanRqtId = PersistedPropertyValuationSchema::loanRequestId.equal(loanRequestId)
        val loanRqtCriteria = QueryCriteria.VaultCustomQueryCriteria(exLoanRqtId)
        val propValuation = serviceHub.vaultService.queryBy(PropertyValuation::class.java, loanRqtCriteria).states.first()
        if (propValuation.state.data.status != AppraisalStatus.COMPLETE){
            throw PropertyValuationNotComplete("Property valuation is yet not received from Appraiser node. Please try after some Seconds!")
        } else if (sanctionAmount > propValuation.state.data.valuation!!){
            throw CordaRuntimeException("Loan sanction amount should not greater than property valuation amount!!")
        }

        // Loan states.
        val inLoanRequest = serviceHub.getStateByLinearId(loanRequestId, LoanState::class.java)
        val outLoanRequest = inLoanRequest.state.data.copy(sanctionAmount = sanctionAmount)

        // Get RealEstate Property Evolvable tokens we received previously.
        val inEvolvableToken = serviceHub.getStateByLinearId(outLoanRequest.linearId, RealEstateProperty::class.java)
        val outEvolvableToken = inEvolvableToken.state.data

        // Get participants details.
        val creditAdminDeptAccParty = outLoanRequest.lender
        val borrowerParty = outLoanRequest.owner
        val borrowerAccInfo = serviceHub.accountService.accountInfo(borrowerParty.owningKey) ?:
                throw CordaRuntimeException("Could not find account for borrower / owner of token.")
        val borrowerHost = borrowerAccInfo.state.data.host

        //Build transaction.
        val notary = serviceHub.firstNotary()
        val txb = TransactionBuilder(notary)
                .addOutputState(outEvolvableToken, RealEstatePropertyContract.CONTRACT_ID, notary)
                .addOutputState(outLoanRequest, LoanStateContract.CONTRACT_ID)
                .addInputState(inEvolvableToken)
                .addInputState(inLoanRequest)
                .addCommand(RealEstatePropertyContract.PledgeAsCollateral(), listOf(borrowerParty.owningKey, creditAdminDeptAccParty.owningKey))
                .addCommand(LoanStateContract.Commands.Approve(), listOf(borrowerParty.owningKey, creditAdminDeptAccParty.owningKey))

        // Verify and sign tx.
        txb.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txb, listOf(ourIdentity.owningKey, creditAdminDeptAccParty.owningKey))

        // Collect signatures.
        val session = initiateFlow(borrowerHost)
        val signatureToPledgeCollateral = subFlow(CollectSignatureFlow(stx, session, borrowerParty.owningKey))
        val fstx = stx.withAdditionalSignatures(signatureToPledgeCollateral)

        // Tx finality.
        subFlow(FinalityFlow(fstx, listOf(session).filter { it.counterparty != ourIdentity }))

        return "LoanId: ${outLoanRequest.linearId}"
    }
}