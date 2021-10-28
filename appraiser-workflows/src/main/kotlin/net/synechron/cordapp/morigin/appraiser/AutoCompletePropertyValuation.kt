package net.synechron.cordapp.morigin.appraiser

import co.paralleluniverse.fibers.Suspendable
import io.netty.util.internal.ThreadLocalRandom
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SchedulableFlow
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.flows.AbstractAutoCompletePropertyValuation
import net.synechron.cordapp.morigin.state.AppraisalStatus
import net.synechron.cordapp.morigin.state.PropertyValuation
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@SchedulableFlow
class AutoCompletePropertyValuation(val stateRef: StateRef) : AbstractAutoCompletePropertyValuation<Unit>() {

    @Suspendable
    override fun call() {
        val input = serviceHub.toStateAndRef<PropertyValuation>(stateRef)
        var output = input.state.data

        // Execute flow only for appraiser node.
        if(ourIdentity != output.appraiser)
            return

        output = output.copy(valuation = getValuation(output), status = AppraisalStatus.COMPLETE)

        //Build transaction.
        val txb = TransactionBuilder(serviceHub.firstNotary())
                .addCommand(LoanStateContract.Commands.Valuation(), listOf(ourIdentity.owningKey))
                .addInputState(input)
                .addOutputState(output, LoanStateContract.CONTRACT_ID)
                .setTimeWindow(Instant.now(), 60.seconds)

        // Verify and sign tx.
        txb.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txb, ourIdentity.owningKey)

        // Tx finality.
        val session = initiateFlow(output.bank)
        subFlow(FinalityFlow(stx, listOf(session)))
    }

    private fun getValuation(pv : PropertyValuation): Amount<Currency> {
        val upper = pv.evolvablePropertyToken.propertyValue.toDecimal()
        val lower = upper.multiply(BigDecimal("0.7"))
        val nextDouble = ThreadLocalRandom.current().nextDouble(lower.toDouble(), upper.toDouble())
        return  Amount(nextDouble.toLong(), pv.evolvablePropertyToken.propertyValue.token)
    }
}