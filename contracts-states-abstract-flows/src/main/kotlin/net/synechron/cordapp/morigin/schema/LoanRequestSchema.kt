package net.synechron.cordapp.morigin.schema

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.*

/**
 * The family of schemas for [LoanRequestSchema].
 */
object LoanRequestSchema

/**
 * First version of an [LoanRequestSchema] schema.
 */
object LoanRequestSchemaV1 : MappedSchema(
    schemaFamily = LoanRequestSchema.javaClass,
    version = 1, mappedTypes = listOf(PersistedLoanRequestSchema::class.java)
) {
    @Entity
    @Table(name = "loan_request", indexes = arrayOf(Index(name = "loan_request_linearId_idx", columnList = "linearId")))
    class PersistedLoanRequestSchema(
        @ElementCollection
        @Column(name = "participants")
        @CollectionTable(
            name = "loan_request_parts", joinColumns = arrayOf(
                JoinColumn(name = "output_index", referencedColumnName = "output_index"),
                JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id")
            )
        )
        var participants: MutableSet<AbstractParty>? = null,

        @Column(name = "linearId")
        val linearId: String,

        @Column(name = "status")
        val status: String,
        @Column(name = "marketValuation")
        val marketValuation: String,
        @Column(name = "lender")
        val lender: AbstractParty,
        @Column(name = "owner")
        val owner: AbstractParty,
        @Column(name = "loanAmount")
        val loanAmount: String,
        @Column(name = "realEstatePropertyLinearId")
        val nftPropertyTokenId: String,
        @Column(name = "evolvablePropertyTokenId")
        val evolvablePropertyTokenId: String

    ) : PersistentState() {
        constructor() : this(null, "", "", "", NullKeys.NULL_PARTY, NullKeys.NULL_PARTY, "", "", "")
    }
}