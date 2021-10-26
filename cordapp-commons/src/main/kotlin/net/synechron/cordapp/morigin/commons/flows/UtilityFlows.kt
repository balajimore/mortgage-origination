package net.synechron.cordapp.morigin.commons.flows

import net.corda.core.contracts.requireThat
import net.corda.core.crypto.isFulfilledBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.ReceiveTransactionFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction

class SignTxFlowNoChecking(otherFlow: FlowSession) : SignTransactionFlow(otherFlow) {
    override fun checkTransaction(stx: SignedTransaction) {
        // TODO: Add checking here.
    }
}

class SignTxFlowVerify(otherFlow: FlowSession) : SignTransactionFlow(otherFlow) {
    override fun checkTransaction(stx: SignedTransaction) = requireThat {
        "Must be signed by the initiator." using (stx.sigs.any())
        stx.verify(serviceHub, false)
    }
}

class ReceiveTxSignedByInitiatingPartyFlow
@JvmOverloads constructor(private val otherSideSession: FlowSession,
                          private val checkSufficientSignatures: Boolean = false,
                          private val statesToRecord: StatesToRecord = StatesToRecord.NONE) : FlowLogic<SignedTransaction>() {
    override fun call(): SignedTransaction {
        return subFlow(object : ReceiveTransactionFlow(otherSideSession, checkSufficientSignatures, statesToRecord) {
            override fun checkBeforeRecording(stx: SignedTransaction) {
                val signature = stx.sigs.first()
                require(otherSideSession.counterparty.owningKey.isFulfilledBy(signature.by)) {
                    "Must be signed by transaction initiating party"
                }
            }
        })
    }
}