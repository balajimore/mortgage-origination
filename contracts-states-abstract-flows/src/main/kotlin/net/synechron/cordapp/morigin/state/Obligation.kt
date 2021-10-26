package net.synechron.cordapp.morigin.state

import net.synechron.cordapp.morigin.contract.ObligationContract
import net.synechron.cordapp.morigin.schema.ObligationSchemaV1
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.utilities.toBase58String
import java.util.*

/***
 * Obligation state object to be stored on vault.
 */

@BelongsToContract(ObligationContract::class)
data class Obligation(val amount: Amount<Currency>,
                      val lender: AbstractParty,
                      val borrower: AbstractParty,
                      val paid: Amount<Currency> = Amount(0, amount.token),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(lender, borrower)

    fun pay(amountToPay: Amount<Currency>) = copy(paid = paid + amountToPay)
    fun withNewLender(newLender: AbstractParty) = copy(lender = newLender)
    fun withoutLender() = copy(lender = NullKeys.NULL_PARTY)

    override fun toString(): String {
        val lenderString = (lender as? Party)?.name?.organisation ?: lender.owningKey.toBase58String()
        val borrowerString = (borrower as? Party)?.name?.organisation ?: borrower.owningKey.toBase58String()
        return "Obligation($linearId): $borrowerString owes $lenderString $amount and has paid $paid so far."
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ObligationSchemaV1 -> ObligationSchemaV1.PersistentObligation(
                    participants = this.participants.toMutableSet(),
                    linearId = this.linearId.toString(),
                    amount = this.amount.toString(),
                    lender = this.lender,
                    borrower = this.borrower,
                    paid = this.paid.toString()
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ObligationSchemaV1)
}