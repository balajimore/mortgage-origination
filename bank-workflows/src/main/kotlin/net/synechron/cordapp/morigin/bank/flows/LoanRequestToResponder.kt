package net.synechron.cordapp.morigin.bank.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.synechron.cordapp.morigin.flows.AbstractLoanRequestTo

@InitiatedBy(AbstractLoanRequestTo::class)
class LoanRequestToResponder(val counterPartySession : FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Tx finality.
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}