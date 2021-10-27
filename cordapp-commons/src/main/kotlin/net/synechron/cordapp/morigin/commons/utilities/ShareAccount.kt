package net.synechron.cordapp.morigin.commons.utilities


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.synechron.cordapp.morigin.commons.utilities.FindAccountByName


@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareAccountTo(
        private val acctNameShared: String,
        private val shareTo: Party
        ) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {
        //Create a new account
        val accountId = subFlow(FindAccountByName(acctNameShared)).identifier.id

        accountService.shareAccountInfoWithParty(accountId,shareTo)
        return "Shared " + acctNameShared + " with " + shareTo.name.organisation
    }
}



