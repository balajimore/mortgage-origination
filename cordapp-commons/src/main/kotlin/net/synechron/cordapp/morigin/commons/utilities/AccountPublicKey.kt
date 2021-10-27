package net.synechron.cordapp.morigin.commons.utilities

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.flows.StartableByService
import java.security.PublicKey
import java.util.*


@StartableByRPC
@StartableByService
@InitiatingFlow
class AccountPublicKey(private val accountId: UUID) : FlowLogic<PublicKey>() {
    @Suspendable
    override fun call(): PublicKey {
        val publicKey = accountService.accountKeys(accountId).single()
        return publicKey
    }
}