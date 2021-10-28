package net.synechron.cordapp.morigin.bank.flows

import co.paralleluniverse.fibers.Suspendable
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
import net.synechron.cordapp.morigin.state.*
import java.util.*

/** ****************************************************************************
 * Approve loan to borrower / NFT token holder by locking/ pledging the its token
 * as collateral (a security in case default)
 ****************************************************************************
 */

@StartableByRPC
class LoanSanction(
        val loanId: String,
        val sanctionAmount: Amount<Currency>
) : AbstractLoanSanction<String>() {

    @Suspendable
    override fun call(): String {
        // Validate the sanction amount. It should be more than valuation.
        val exLoanRqtId = PersistedPropertyValuationSchema::loanRequestId.equal(loanId)
        val loanRqtCriteria = QueryCriteria.VaultCustomQueryCriteria(exLoanRqtId)
        val propValuation = serviceHub.vaultService.queryBy(PropertyValuation::class.java, loanRqtCriteria)
                .states.firstOrNull()
                ?: throw PropertyValuationNotComplete("Property valuation info not found. You may try after some seconds.")
        if (propValuation.state.data.status != AppraisalStatus.COMPLETE) {
            throw PropertyValuationNotComplete("Property valuation is yet not received from Appraiser node. Please try after some Seconds!")
        } else if (sanctionAmount > propValuation.state.data.valuation!!) {
            throw CordaRuntimeException("Loan sanction amount should not greater than property valuation amount!!")
        }

        // Build Loan states.
        val inLoan = serviceHub.getStateByLinearId(loanId, LoanState::class.java)
        var outLoan = inLoan.state.data
        val actualSanctionAmt = if (outLoan.loanAmount > sanctionAmount) sanctionAmount else outLoan.loanAmount
        outLoan = inLoan.state.data.copy(
                sanctionAmount = actualSanctionAmt,
                status = LoanStatus.APPROVED
        )

        // Get RealEstate Property Evolvable tokens we received previously.
        val inEvolvableToken = serviceHub.getStateByLinearId(outLoan.evolvablePropertyTokenId, RealEstateProperty::class.java)
        val outEvolvableToken = inEvolvableToken.state.data

        // Get participants details.
        val creditAdminDeptAccParty = outLoan.lender
        val borrowerParty = outLoan.owner
        val borrowerHost = serviceHub.accountByName(outLoan.ownerAccName).host

        //Build transaction.
        val notary = serviceHub.firstNotary()
        val txb = TransactionBuilder(notary)
                .addOutputState(outEvolvableToken, RealEstatePropertyContract.CONTRACT_ID, notary, 1)
                .addOutputState(outLoan, LoanStateContract.CONTRACT_ID, notary, 0)
                .addInputState(inEvolvableToken)
                .addInputState(inLoan)
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

        return "LoanId: ${outLoan.linearId}"
    }
}