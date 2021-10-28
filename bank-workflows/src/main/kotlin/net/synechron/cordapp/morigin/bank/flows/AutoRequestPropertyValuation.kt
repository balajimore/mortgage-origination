package net.synechron.cordapp.morigin.bank.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.SchedulableFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.seconds
import net.synechron.cordapp.morigin.contract.LoanStateContract
import net.synechron.cordapp.morigin.flows.AbstractAutoRequestPropertyValuation
import net.synechron.cordapp.morigin.state.LoanState
import net.synechron.cordapp.morigin.state.PropertyValuation
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.time.Instant

@SchedulableFlow
class AutoRequestPropertyValuation(
        val loanRequest: LoanState) : AbstractAutoRequestPropertyValuation<Unit>() {

    @Suspendable
    override fun call() {
        val evolvableToken = serviceHub.getStateByLinearId(loanRequest.evolvablePropertyTokenId,
                RealEstateProperty::class.java)

        // Execute flow only for Bank node.
        val accountInfo = serviceHub.accountService.accountInfo(loanRequest.lender.owningKey)
        if(accountInfo == null || ourIdentity != accountInfo.state.data.host)
            return

        val appraiser = getAppraiserNode()
        logger.info("Found Appraiser node identity: $appraiser")
        val propVal = PropertyValuation(
                loanRequest.linearId,
                evolvableToken.state.data,
                ourIdentity,
                appraiser)

        //Build transaction.
        val txb = TransactionBuilder(serviceHub.firstNotary())
                .addCommand(LoanStateContract.Commands.ValuationRequest(), listOf(ourIdentity.owningKey))
                .addOutputState(propVal)
                .setTimeWindow(Instant.now(), 60.seconds)

        // Verify and sign tx.
        txb.verify(serviceHub)
        val stx = serviceHub.signInitialTransaction(txb, ourIdentity.owningKey)

        // Tx finality.
        val session = initiateFlow(appraiser)
        subFlow(FinalityFlow(stx, listOf(session)))
    }

    private fun getAppraiserNode(): Party {
        return serviceHub.networkMapCache.allNodes.filter {
            it.legalIdentities[0].name.toString().contains("Appraiser", true)
        }.first().legalIdentities.first()

    }
}