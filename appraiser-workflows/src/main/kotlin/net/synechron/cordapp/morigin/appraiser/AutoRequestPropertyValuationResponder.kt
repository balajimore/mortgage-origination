package net.synechron.cordapp.morigin.appraiser

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.*
import net.synechron.cordapp.morigin.flows.AbstractAutoRequestPropertyValuation

@InitiatedBy(AbstractAutoRequestPropertyValuation::class)
class AutoRequestPropertyValuationResponder(val counterPartySession : FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Tx finality.
        subFlow(ReceiveFinalityFlow(counterPartySession))
    }
}