package net.synechron.cordapp.morigin.custodian.flows

import co.paralleluniverse.fibers.Suspendable
import net.synechron.cordapp.morigin.commons.flows.SignTxFlowVerify
import net.synechron.cordapp.morigin.flows.AbstractSettleObligationFlow
import net.corda.confidential.IdentitySyncFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.transactions.SignedTransaction

@InitiatedBy(AbstractSettleObligationFlow::class)
class SettleObligationResponderFlow(private val otherSideSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        subFlow(IdentitySyncFlow.Receive(otherSideSession))
        val stx = subFlow(SignTxFlowVerify(otherSideSession))
        return subFlow(ReceiveFinalityFlow(otherSideSession, stx.id))
    }
}
