package net.synechron.cordapp.morigin.commons.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.utilities.ProgressTracker
import net.synechron.cordapp.morigin.commons.utilities.NewKeyForAccount
import net.synechron.cordapp.morigin.commons.utilities.ShareAccountTo
import net.synechron.cordapp.morigin.flows.FlowHelper

/**
 * Flow to create the user account. Share account to counterparties by default.
 */
@StartableByRPC
@InitiatingFlow
class CreateNewAccount constructor(
    private val accountName: String,
    private val shareToCounterParty: Boolean
) : FlowLogic<String>(), FlowHelper {
    constructor(accountName: String) : this(accountName, true)

    companion object {
        object CREATE_ACCOUNT : ProgressTracker.Step("Creating user's account.")
        object KEY_REQUEST : ProgressTracker.Step("Requesting for account public key.")
        object SHARE_ACCOUNT_TO : ProgressTracker.Step("Share account to counter parties.")

        fun tracker() = ProgressTracker(
            CREATE_ACCOUNT,
            KEY_REQUEST,
            SHARE_ACCOUNT_TO
        )
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): String {
        progressTracker.currentStep = CREATE_ACCOUNT
        val accountInfoRef: StateAndRef<AccountInfo> = subFlow(CreateAccount(accountName))

        progressTracker.currentStep = KEY_REQUEST
        val account = accountInfoRef.state.data
        subFlow(NewKeyForAccount(account.identifier.id)).owningKey

        // Share Account to CounterParty.
        if (shareToCounterParty) {
            //TODO Need refactor. It is not good to share account to all counter-parties.
            val counterParties = getCounterParties()
            counterParties.forEach { shareTo ->
                subFlow(ShareAccountTo(accountName, shareTo))
            }
        }
        return "${account.name} created with Id: ${account.identifier}"
    }

    private fun getCounterParties(): List<Party> {
        val allParties = serviceHub.networkMapCache.allNodes.map { it.legalIdentities[0] }
        val allNotaries = serviceHub.networkMapCache.notaryIdentities

        return allParties - allNotaries - ourIdentity
    }

}