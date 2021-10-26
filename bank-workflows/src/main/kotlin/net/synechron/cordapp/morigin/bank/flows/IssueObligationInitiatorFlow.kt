package net.synechron.cordapp.morigin.bank.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.confidential.SwapIdentitiesFlow
import net.corda.core.contracts.Amount
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowSession
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.utilities.seconds
import net.synechron.cordapp.morigin.commons.exception.GenerateConfidentialIdentityException
import net.synechron.cordapp.morigin.contract.ObligationContract
import net.synechron.cordapp.morigin.flows.AbstractIssueObligationFlow
import net.synechron.cordapp.morigin.state.Obligation
import java.util.*

@StartableByRPC
class IssueObligationInitiatorFlow(
    private val amount: Amount<Currency>,
    private val lender: Party
) : AbstractIssueObligationFlow<SignedTransaction>() {

    companion object {
        object INITIALISING : Step("Performing initial steps.")
        object BUILDING : Step("Building and verifying transaction.")
        object SIGNING : Step("Signing transaction.")
        object COLLECTING : Step("Collecting counterparty signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING : Step("Finalising transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING)
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        logger.info("Start IssueObligationInitiatorFlow.")

        // Step 1. Initialisation.
        val lenderSession = initiateFlow(lender)
        progressTracker.currentStep = INITIALISING
        val obligation = createAnonymousObligation(lenderSession)
        val ourSigningKey = obligation.borrower.owningKey

        // Step 2. Building.
        progressTracker.currentStep = BUILDING
        val utx = TransactionBuilder(serviceHub.firstNotary())
            .addOutputState(obligation, ObligationContract.OBLIGATION_CONTRACT_ID)
            .addCommand(ObligationContract.Commands.Issue(), obligation.participants.map { it.owningKey })
            .setTimeWindow(serviceHub.clock.instant(), 30.seconds)

        // Step 3. Sign the transaction.
        progressTracker.currentStep = SIGNING
        val ptx = serviceHub.signInitialTransaction(utx, ourSigningKey)

        // Step 4. Get the counter-party signature.
        progressTracker.currentStep = COLLECTING
        val stx = subFlow(
            CollectSignaturesFlow(
                ptx,
                setOf(lenderSession),
                listOf(ourSigningKey),
                COLLECTING.childProgressTracker()
            )
        )

        // Step 5. Finalise the transaction.
        progressTracker.currentStep = FINALISING
        val ftx = subFlow(FinalityFlow(stx, listOf(lenderSession), FINALISING.childProgressTracker()))

        logger.info("End IssueObligationInitiatorFlow.")
        return ftx
    }

    @Suspendable
    private fun createAnonymousObligation(lenderSession: FlowSession): Obligation {
        //Swap anonymous confidential identities.
        val txKeys = subFlow(SwapIdentitiesFlow(lenderSession))
        check(txKeys.size == 2) { throw GenerateConfidentialIdentityException("Something went wrong when generating confidential identities.") }

        val anonymousMe = txKeys[ourIdentity]
            ?: throw GenerateConfidentialIdentityException("Couldn't create our conf. identity.")
        val anonymousLender = txKeys[lender]
            ?: throw GenerateConfidentialIdentityException("Couldn't create lender's conf. identity.")

        return Obligation(amount, anonymousLender, anonymousMe)
    }
}