package net.synechron.cordapp.morigin.contract

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.r3.corda.lib.tokens.contracts.commands.EvolvableTokenTypeCommand
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.transactions.LedgerTransaction
import net.synechron.cordapp.morigin.contract.ContractUtil.Companion.participantsKeys
import net.synechron.cordapp.morigin.state.LoanState
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.security.PublicKey

class RealEstatePropertyContract : EvolvableTokenContract(), Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = RealEstatePropertyContract::class.java.name!!
    }

    /** Used when creating encumbered state to lock as collateral used by Bank from Borrower. */
    class PledgeAsCollateral : EvolvableTokenTypeCommand, TypeOnlyCommandData()

    /** Used when releasing encumbered state which was pledge as collateral to Bank by Borrower. */
    class ReleaseCollateral : EvolvableTokenTypeCommand, TypeOnlyCommandData()

    override fun verify(tx: LedgerTransaction) {
        super.verify(tx)

        val command = tx.commands.requireSingleCommand<EvolvableTokenTypeCommand>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is PledgeAsCollateral -> verifyPledgeAsCollateral(tx, setOfSigners)
            is ReleaseCollateral -> verifyReleaseCollateral(tx, setOfSigners)
        }
    }

    private fun verifyPledgeAsCollateral(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        ContractUtil.verifyTxComponents(tx = tx, outputs = 2, inputs = 2,
                inputStateTypeAndCounts = listOf(RealEstateProperty::class.java to 1, LoanState::class.java to 1 ),
                outputStateTypeAndCounts = listOf(RealEstateProperty::class.java to 1, LoanState::class.java to 1 ))

        val outEvolvableTokenType = tx.outputsOfType<RealEstateProperty>().first()
        val outLoan = tx.outputsOfType<LoanState>().first()

        // Check for loan collateral
        val txoutEvolvableTokenType = tx.outputs.first { it.data is RealEstateProperty }
        val isIncuberanceAtRightPosition = tx.outputStates[1] is LoanState
        "You must pledge collateral by specifying encumbrance number. " +
                "We must make RealEstateProperty Evolvable Token as encumbered." using (
                txoutEvolvableTokenType.encumbrance != null && txoutEvolvableTokenType.encumbrance == 1)
        "Encumbrance state is at invalid position. Make sure Loan output state is at index: 1" using(
                isIncuberanceAtRightPosition)

        "Invalid property Token." using (outEvolvableTokenType.linearId == outLoan.evolvablePropertyTokenId)
        "Both bank's credit administration department and borrower must sign the transaction." using (
                signers.containsAll(outLoan.participantsKeys()))
    }

    private fun verifyReleaseCollateral(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {  }

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as RealEstateProperty
        "Valuation cannot be zero" using (outputState.propertyValue.quantity > 0)
        //"Constructed date should not be in future." - need to customize flow that adds time-window to tx.
        "Invalid property address" using (outputState.propertyAddress.isNotBlank())
        "Invalid construction area" using (outputState.constructionArea.isNotBlank())
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as RealEstateProperty
        val inputState = tx.getInput(0) as RealEstateProperty
        "Construction date should be changed." using (outputState.constructedOn == inputState.constructedOn)
        "Construction date should be changed." using (outputState.fractionDigits == inputState.fractionDigits)
    }
}