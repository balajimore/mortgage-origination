package net.synechron.cordapp.morigin.state

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.schema.LoanStateSchemaV1
import java.time.Instant
import java.util.*

/**
 * This state act as loan agreement between Bank and Borrower.
 * This is collateral backed loan lending.
 */

@BelongsToContract(LoanStateContract::class)
data class LoanState(
        val nftPropertyTokenId: UniqueIdentifier,
        val evolvablePropertyTokenId: UniqueIdentifier,
        val evolvablePropertyTokenSateRef: StateRef,
        val loanAmount: Amount<Currency>,
        // Token holder / owner / borrower.
        val owner: AbstractParty,
        val lender: AbstractParty,
        val ownerAccName: String,
        val sanctionAmount: Amount<Currency>? = null,
        val status: LoanStatus = LoanStatus.PENDING,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<AbstractParty> = listOf(owner, lender)
) : LinearState, QueryableState, SchedulableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LoanStateSchemaV1 -> LoanStateSchemaV1.PersistedLoanStateSchema(
                    participants = this.participants.toMutableSet(),
                    linearId = this.linearId.toString(),
                    status = this.status.toString(),
                    sanctionAmountAmount = this.sanctionAmount.toString(),
                    lender = this.lender,
                    owner = this.owner,
                    // TODO  ownerAccName,
                    loanAmount = this.loanAmount.toString(),
                    nftPropertyTokenId = this.nftPropertyTokenId.toString(),
                    evolvablePropertyTokenId = this.evolvablePropertyTokenId.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LoanStateSchemaV1)

    override fun nextScheduledActivity(thisStateRef: net.corda.core.contracts.StateRef,
                                       flowLogicRefFactory: net.corda.core.flows.FlowLogicRefFactory)
            : net.corda.core.contracts.ScheduledActivity? {
        return if (this.status == LoanStatus.PENDING) {
            try {
                ScheduledActivity(
                        flowLogicRefFactory.create(
                                "net.synechron.cordapp.morigin.bank.flows.AutoRequestPropertyValuation",
                                this),
                        Instant.now())
            } catch (ex: Throwable) {
                // This would be true on non-Bank Vault.
                null
            }
        } else null
    }

}


@CordaSerializable
enum class LoanStatus {
    PENDING, APPROVED, REJECTED
}
