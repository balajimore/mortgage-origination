package net.synechron.cordapp.morigin.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.synechron.cordapp.morigin.state.*
import java.security.PublicKey

class LoanStateContract : Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = LoanStateContract::class.java.name!!
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class ValuationRequest : TypeOnlyCommandData(), Commands
        class Valuation : TypeOnlyCommandData(), Commands
        class Approve : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction): Unit {
        require(tx.timeWindow?.midpoint != null) { "Transaction must be timestamped." }
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx, setOfSigners)
            is Commands.ValuationRequest -> verifyValuationRequest(tx, setOfSigners)
            is Commands.Valuation -> verifyValuation(tx, setOfSigners)
            is Commands.Approve -> verifyApprove(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyValuation(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        ContractUtil.verifyTxComponents(tx, outputs = 1, inputs = 1,
                inputStateTypeAndCount = PropertyValuation::class.java to 1,
                outputStateTypeAndCount = PropertyValuation::class.java to 1)

        val output = tx.outputsOfType<PropertyValuation>().first()
        "Invalid status." using (output.status == AppraisalStatus.COMPLETE)
        "Invalid valuation value." using (output.valuation != null
                && output.valuation > Amount.zero(output.valuation.token))
        "Appraiser only may sign create valuation transaction." using (signers.contains(output.bank.owningKey))
    }

    private fun verifyValuationRequest(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        ContractUtil.verifyTxComponents(tx, outputs = 1, outputStateTypeAndCount = PropertyValuation::class.java to 1)

        val output = tx.outputsOfType<PropertyValuation>().first()
        "Invalid status." using (output.status == AppraisalStatus.PENDING)
        "Invalid valuation value." using (output.valuation == null)
        "Bank only may sign create valuation request transaction." using (signers.contains(output.bank.owningKey))
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        ContractUtil.verifyTxComponents(tx, outputs = 1, refStateTypeAndCounts = listOf(
                RealEstateProperty::class.java to 1
        ))

        val loanRequest = tx.outputsOfType<LoanState>().single()
        // val nftToken = tx.referenceInputRefsOfType<NonFungibleToken>().single()
        val outEvolvableToken = tx.referenceInputRefsOfType<RealEstateProperty>().single()

        "Invalid loan amount." using (loanRequest.loanAmount.quantity > 0)
        //"Invalid NFT property token Id." using (loanRequest.nftPropertyTokenId == nftToken.state.data.linearId)
        "Invalid Evolvable property token Id." using (loanRequest.evolvablePropertyTokenId == outEvolvableToken.state.data.linearId)
        "Invalid Evolvable property token StateRef." using (loanRequest.evolvablePropertyTokenSateRef == outEvolvableToken.ref)
        // "LoanRequest has invalid token holder identity." using (loanRequest.owner == nftToken.state.data.holder)
        "LoanRequest should not be assigned value to sanction amount while creation time." using (loanRequest.sanctionAmount == null)
        "LoanRequest status should be PENDING." using (loanRequest.status == LoanStatus.PENDING)

        "Token holder only may sign create loan request transaction." using (signers.contains(loanRequest.owner.owningKey))
    }

    private fun verifyApprove(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
            // Most of all rules covered in RealEstatePropertyContract.
    }
}