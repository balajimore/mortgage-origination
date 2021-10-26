package net.synechron.cordapp.morigin.custodian.flows

import co.paralleluniverse.fibers.Suspendable
import net.synechron.cordapp.morigin.commons.flows.SignTxFlowVerify
import net.synechron.cordapp.morigin.flows.AbstractIssueObligationFlow
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(AbstractIssueObligationFlow::class)
class IssueObligationResponderFlow(private val otherSideSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        //Swap anonymous confidential identities.
        subFlow(SwapIdentitiesFlow(otherSideSession))
        //Verify and sign the transaction.
        val stx = subFlow(SignTxFlowVerify(otherSideSession))
        //Await to receive the transaction that was signed then will be committed to vault.
        return subFlow(ReceiveFinalityFlow(otherSideSession, stx.id))
    }
}

