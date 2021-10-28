package net.synechron.cordapp.morigin.schema

import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.*

/**
 * The family of schemas for [PropertyValuationSchema].
 */
object PropertyValuationSchema

/**
 * First version of an [PropertyValuationSchema] schema.
 */
object PropertyValuationSchemaV1 : MappedSchema(
        schemaFamily = PropertyValuationSchema.javaClass,
        version = 1, mappedTypes = listOf(PersistedPropertyValuationSchema::class.java)
) {
    @Entity
    @Table(name = "property_valuation", indexes = arrayOf(Index(name = "property_valuation_linearId_idx", columnList = "linearId")))
    class PersistedPropertyValuationSchema(
            @ElementCollection
            @Column(name = "participants")
            @CollectionTable(
                    name = "property_valuation_parts", joinColumns = arrayOf(
                    JoinColumn(name = "output_index", referencedColumnName = "output_index"),
                    JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id")
            )
            )
            var participants: MutableSet<AbstractParty>? = null,

            @Column(name = "linearId")
            val linearId: String,

            @Column(name = "status")
            val status: String,

            @Column(name = "loanRequestId")
            val loanRequestId: String,

            @Column(name = "evolvablePropertyTokenId")
            val evolvablePropertyTokenId: String

    ) : PersistentState() {
        constructor() : this(null, "", "", "", "")
    }
}