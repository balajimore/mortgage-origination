package net.synechron.cordapp.morigin.schema

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.*

/**
 * The family of schemas for [ObligationSchema].
 */
object ObligationSchema

/**
 * First version of an [ObligationSchema] schema.
 */
object ObligationSchemaV1 : MappedSchema(schemaFamily = ObligationSchema.javaClass,
        version = 1, mappedTypes = listOf(PersistentObligation::class.java)) {
    @Entity
    @Table(name = "obligation", indexes = arrayOf(Index(name = "linearId_idx", columnList = "linearId")))
    class PersistentObligation(
            @ElementCollection
            @Column(name = "participants")
            @CollectionTable(name = "obligation_participants", joinColumns = arrayOf(
                    JoinColumn(name = "output_index", referencedColumnName = "output_index"),
                    JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id")))
            var participants: MutableSet<AbstractParty>? = null,

            @Column(name = "linearId")
            val linearId: String,

            @Column(name = "amount")
            val amount: String,

            @Column(name = "lender")
            val lender: AbstractParty,

            @Column(name = "borrower")
            val borrower: AbstractParty,

            @Column(name = "paid")
            val paid: String
    ) : PersistentState() {
        constructor() : this(null, "", "", NullKeys.NULL_PARTY, NullKeys.NULL_PARTY, "")
    }
}