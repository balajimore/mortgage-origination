package net.synechron.cordapp.morigin.custodian.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.synechron.cordapp.morigin.commons.flows.SignTxFlowVerify
import net.synechron.cordapp.morigin.flows.AbstractLoanRequestTo
import net.synechron.cordapp.morigin.flows.AbstractLoanSanction

@InitiatedBy(AbstractLoanSanction::class)
class LoanSanctionResponder(val counterPartySession : FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {

        class SignTxFlowVerify(otherFlow: FlowSession) : SignTransactionFlow(otherFlow) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                "Must be signed by the initiator." using (stx.sigs.any())
                
                stx.verify(serviceHub, false)
            }
        }
        // Sign transaction.
        subFlow(SignTxFlowVerify(counterPartySession))

        // Tx finality.
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}