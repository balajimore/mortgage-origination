package net.synechron.cordapp.morigin.commons.utilities

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import java.util.*

@StartableByRPC
@StartableByService
@InitiatingFlow
class FindAccountById(private val accountId: UUID) : FlowLogic<StateAndRef<AccountInfo>>() {
    @Suspendable
    override fun call(): StateAndRef<AccountInfo> {
        val account = accountService.accountInfo(accountId)!!
        return account
    }
}
