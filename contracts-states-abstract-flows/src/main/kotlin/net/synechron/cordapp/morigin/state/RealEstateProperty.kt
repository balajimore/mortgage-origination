package net.synechron.cordapp.morigin.state

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.synechron.cordapp.morigin.contract.RealEstatePropertyContract
import net.synechron.cordapp.morigin.schema.RealEstatePropertySchemaV1
import java.time.LocalDate
import java.util.*

@BelongsToContract(RealEstatePropertyContract::class)
data class RealEstateProperty(
    // Property Identifier.
    override val linearId: UniqueIdentifier,
    override val maintainers: List<Party>,
    val propertyValue: Amount<Currency>,
    val constructionArea: String,
    val constructedOn: LocalDate,
    val propertyAddress: String,
    val issuer: Party = maintainers.single(),
    override val fractionDigits: Int = 0
) : EvolvableTokenType(), QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is RealEstatePropertySchemaV1 -> RealEstatePropertySchemaV1.PersistentRealEstateProperty(
                maintainers = this.maintainers.toMutableSet(),
                linearId = this.linearId.toString(),
                propertyValue = this.propertyValue.toString(),
                constructionArea = this.constructionArea,
                constructedOn = this.constructedOn,
                propertyAddress = this.propertyAddress,
                issuer = this.issuer,
                fractionDigits = this.fractionDigits
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RealEstatePropertySchemaV1)
}