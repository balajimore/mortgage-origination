package net.synechron.cordapp.morigin.commons.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.synechron.cordapp.morigin.commons.utilities.NewKeyForAccount
import net.synechron.cordapp.morigin.flows.FlowHelper

/**
 * Flow to create the user account.
 */
@StartableByRPC
@InitiatingFlow
class CreateNewAccount(private val accountName: String) : FlowLogic<String>(), FlowHelper {
    companion object {
        object CREATE_ACCOUNT : ProgressTracker.Step("Creating user's account.")
        object KEY_REQUEST : ProgressTracker.Step("Requesting for account public key.")

        fun tracker() = ProgressTracker(
            CREATE_ACCOUNT,
            KEY_REQUEST
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

        return "${account.name} created with Id: ${account.identifier}"
    }
}