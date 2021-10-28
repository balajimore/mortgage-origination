package net.synechron.cordapp.morigin.contract

import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.synechron.cordapp.morigin.state.LoanRequest
import net.synechron.cordapp.morigin.state.LoanRequestStatus
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.security.PublicKey

class LoanRequestContract : Contract {
    companion object {
        @JvmStatic
        val LOAN_REQUEST_CONTRACT_ID = LoanRequestContract::class.java.name!!
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Approve : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction): Unit {
        require(tx.timeWindow?.midpoint != null) { "Transaction must be timestamped." }
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.Create -> verifyCreate(tx, setOfSigners)
            is Commands.Approve -> verifySettle(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun keysFromParticipants(contractState: ContractState): Set<PublicKey> {
        return contractState.participants.map {
            it.owningKey
        }.toSet()
    }

    // This only allows one obligation issuance per transaction.
    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        ContractUtil.verifyTxComponents(tx, outputs = 1, refStateTypeAndCounts = listOf(
                 RealEstateProperty::class.java to 1
        ))

        val loanRequest = tx.outputsOfType<LoanRequest>().single()
       // val nftToken = tx.referenceInputRefsOfType<NonFungibleToken>().single()
        val outEvolvableToken = tx.referenceInputRefsOfType<RealEstateProperty>().single()

        "Invalid loan amount." using (loanRequest.loanAmount.quantity > 0)
        //"Invalid NFT property token Id." using (loanRequest.nftPropertyTokenId == nftToken.state.data.linearId)
        "Invalid Evolvable property token Id." using (loanRequest.evolvablePropertyTokenId == outEvolvableToken.state.data.linearId)
        "Invalid Evolvable property token StateRef." using (loanRequest.evolvablePropertyTokenSateRef == outEvolvableToken.ref)
       // "LoanRequest has invalid token holder identity." using (loanRequest.owner == nftToken.state.data.holder)
        "LoanRequest should not be assigned at creation time." using (loanRequest.marketValuation == null)
        "LoanRequest status should be PENDING." using (loanRequest.status == LoanRequestStatus.PENDING)

        "Token holder only may sign create loan request transaction." using (signers.contains(loanRequest.owner.owningKey))
    }

    private fun verifySettle(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {

    }
}