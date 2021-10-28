package net.synechron.cordapp.morigin.state

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.synechron.cordapp.morigin.contract.RealEstatePropertyContract
import net.synechron.cordapp.morigin.schema.LoanRequestSchemaV1
import net.synechron.cordapp.morigin.schema.RealEstatePropertySchemaV1
import java.util.*

@BelongsToContract(RealEstatePropertyContract::class)
data class LoanRequest(
    val nftPropertyTokenId: UniqueIdentifier,
    val evolvablePropertyTokenId: UniqueIdentifier,
    val evolvablePropertyTokenSateRef: StateRef,
    val loanAmount: Amount<Currency>,
    // Token holder / owner.
    val owner: AbstractParty,
    val lender: AbstractParty,
    val marketValuation: Amount<Currency>? = null,
    val status: LoanRequestStatus = LoanRequestStatus.PENDING,
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val participants: List<AbstractParty> = listOf(owner, lender)
) : LinearState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LoanRequestSchemaV1 -> LoanRequestSchemaV1.PersistedLoanRequestSchema(
                participants = this.participants.toMutableSet(),
                linearId = this.linearId.toString(),
                status = this.status.toString(),
                marketValuation = this.marketValuation.toString(),
                lender = this.lender,
                owner = this.owner,
                loanAmount = this.loanAmount.toString(),
                nftPropertyTokenId = this.nftPropertyTokenId.toString(),
                evolvablePropertyTokenId = this.evolvablePropertyTokenId.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RealEstatePropertySchemaV1)
}

enum class LoanRequestStatus {
    PENDING, APPROVED, REJECTED
}
