package net.synechron.cordapp.morigin.contract

import net.synechron.cordapp.morigin.state.Obligation
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.utils.sumCash
import java.security.PublicKey

class ObligationContract : Contract {

    companion object {
        @JvmStatic
        val OBLIGATION_CONTRACT_ID = ObligationContract::class.java.name!!
    }

    interface Commands : CommandData {
        class Issue : TypeOnlyCommandData(), Commands
        class Settle : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction): Unit {
        require(tx.timeWindow?.midpoint != null) { "Transaction must be timestamped." }
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Issue -> verifyIssue(tx, setOfSigners)
            is Commands.Settle -> verifySettle(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun keysFromParticipants(obligation: Obligation): Set<PublicKey> {
        return obligation.participants.map {
            it.owningKey
        }.toSet()
    }

    // This only allows one obligation issuance per transaction.
    private fun verifyIssue(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs should be consumed when issuing an obligation." using (tx.inputStates.isEmpty())
        "Only one obligation state should be created when issuing an obligation." using (tx.outputStates.size == 1)
        val obligation = tx.outputsOfType<Obligation>().single()
        "A newly issued obligation must have a positive amount." using (obligation.amount.quantity > 0)
        "The lender and borrower cannot be the same identity." using (obligation.borrower != obligation.lender)
        "Both lender and borrower together only may sign obligation issue transaction." using
                (signers == keysFromParticipants(obligation))
    }

    private fun verifySettle(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        // Check for the presence of an input obligation state.
        val obligationInputs = tx.inputsOfType<Obligation>()
        "There must be one input obligation." using (obligationInputs.size == 1)

        // Check there are output cash states.
        // We don't care about cash inputs, the Cash contract handles those.
        val cash = tx.outputsOfType<Cash.State>()
        "There must be output cash." using (cash.isNotEmpty())

        // Check that the cash is being assigned to us.
        val inputObligation = obligationInputs.single()
        val acceptableCash = cash.filter { it.owner == inputObligation.lender }
        "There must be output cash paid to the recipient." using (acceptableCash.isNotEmpty())

        // Sum the cash being sent to us (we don't care about the issuer).
        val sumAcceptableCash = acceptableCash.sumCash().withoutIssuer()
        val amountOutstanding = inputObligation.amount - inputObligation.paid
        "The amount settled cannot be more than the amount outstanding." using (amountOutstanding >= sumAcceptableCash)

        val obligationOutputs = tx.outputsOfType<Obligation>()

        // Check to see if we need an output obligation or not.
        if (amountOutstanding == sumAcceptableCash) {
            // If the obligation has been fully settled then there should be no obligation output state.
            "There must be no output obligation as it has been fully settled." using (obligationOutputs.isEmpty())
        } else {
            // If the obligation has been partially settled then it should still exist.
            "There must be one output obligation." using (obligationOutputs.size == 1)

            // Check only the paid property changes.
            val outputObligation = obligationOutputs.single()
            "The amount may not change when settling." using (inputObligation.amount == outputObligation.amount)
            "The borrower may not change when settling." using (inputObligation.borrower == outputObligation.borrower)
            "The lender may not change when settling." using (inputObligation.lender == outputObligation.lender)
            "The linearId may not change when settling." using (inputObligation.linearId == outputObligation.linearId)

            // Check the paid property is updated correctly.
            "Paid property incorrectly updated." using (outputObligation.paid == inputObligation.paid + sumAcceptableCash)
        }

        // Checks the required parties have signed.
        "Both lender and borrower together only must sign obligation settle transaction." using
                (signers == keysFromParticipants(inputObligation))
    }
}