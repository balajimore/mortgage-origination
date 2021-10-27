package net.synechron.cordapp.morigin.schema

import net.corda.core.contracts.Amount
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.finance.USD
import java.time.LocalDate
import java.util.*
import javax.persistence.*

/**
 * The family of schemas for [RealEstatePropertySchema].
 */
object RealEstatePropertySchema

/**
 * First version of an [RealEstatePropertySchema] schema.
 */
object RealEstatePropertySchemaV1 : MappedSchema(
    schemaFamily = RealEstatePropertySchema.javaClass,
    version = 1, mappedTypes = listOf(PersistentRealEstateProperty::class.java)
) {
    @Entity
    @Table(name = "real_estate_property", indexes = arrayOf(Index(name = "real_estate_property_linearId_idx", columnList = "linearId")))
    class PersistentRealEstateProperty(
        @ElementCollection
        @Column(name = "maintainers")
        @CollectionTable(
            name = "real_estate_property_maintainers", joinColumns = arrayOf(
                JoinColumn(name = "output_index", referencedColumnName = "output_index"),
                JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id")
            )
        )
        var maintainers: MutableSet<Party>? = null,

        @Column(name = "linearId")
        val linearId: String,
        @Column(name = "propertyValue")
        val propertyValue: String,
        @Column(name = "constructionArea")
        val constructionArea: String,
        @Column(name = "constructedOn")
        val constructedOn: LocalDate,
        @Column(name = "propertyAddress")
        val propertyAddress: String,
        @Column(name = "issuer")
        val issuer: Party?,
        @Column(name = "fractionDigits")
        val fractionDigits: Int

    ) : PersistentState() {
        constructor() : this(null, "", "",
        "", LocalDate.now(), "",   null, 0)
    }
}