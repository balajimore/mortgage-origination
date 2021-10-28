package net.synechron.cordapp.morigin.state

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.schema.PropertyValuationSchemaV1
import java.time.Instant
import java.util.*

@BelongsToContract(LoanStateContract::class)
data class PropertyValuation(
        val loanRequestId: UniqueIdentifier,
        val evolvablePropertyToken: RealEstateProperty,
        val bank: AbstractParty,
        val appraiser: AbstractParty,
        val valuation: Amount<Currency>? = null,
        val status: AppraisalStatus = AppraisalStatus.PENDING,
        override val linearId: UniqueIdentifier = UniqueIdentifier(),
        override val participants: List<AbstractParty> = listOf(bank, appraiser)
) : LinearState, QueryableState, SchedulableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PropertyValuationSchemaV1 -> PropertyValuationSchemaV1.PersistedPropertyValuationSchema(
                    participants = this.participants.toMutableSet(),
                    linearId = this.linearId.toString(),
                    status = this.status.toString(),
                    loanRequestId = this.loanRequestId.toString(),
                    evolvablePropertyTokenId = this.evolvablePropertyToken.linearId.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PropertyValuationSchemaV1)

    override fun nextScheduledActivity(thisStateRef: net.corda.core.contracts.StateRef,
                                       flowLogicRefFactory: net.corda.core.flows.FlowLogicRefFactory)
            : net.corda.core.contracts.ScheduledActivity? {
        return if (this.status == AppraisalStatus.PENDING) {
            try {
                ScheduledActivity(
                        flowLogicRefFactory.create(
                                "net.synechron.cordapp.morigin.appraiser.AutoCompletePropertyValuation",
                                thisStateRef),
                        Instant.now())
            } catch (thw: Throwable) {
                // This would be true on non-AppraiserNode Vault.
                null
            }
        } else null
    }
}

@CordaSerializable
enum class AppraisalStatus {
    PENDING, COMPLETE
}
