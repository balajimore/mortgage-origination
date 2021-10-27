package net.synechron.cordapp.morigin.commons.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import net.synechron.cordapp.morigin.flows.FlowHelper

// *********
// * Flows *
// *********
@StartableByRPC
class IssueFiatCurrency(
    val currency: String,
    val amount: Long,
    val recipientAccName: String
) : FlowLogic<String>(), FlowHelper {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {
        /* Create an instance of the fiat currency token */
        val token = FiatCurrency.getInstance(currency)

        /* Create an instance of IssuedTokenType for the fiat currency */
        val issuedTokenType = IssuedTokenType(ourIdentity, token)

        /* Create an instance of FungibleToken for the fiat currency to be issued */
        val recipient = serviceHub.accountParty(recipientAccName)
        val fungibleToken = FungibleToken(Amount(amount, issuedTokenType), recipient)

        //val stx = subFlow(IssueTokens(listOf(fungibleToken), listOf<Party>(recipient)))
        subFlow(IssueTokens(listOf(fungibleToken)))
        return "Issued $amount $currency token(s) to $recipientAccName"
    }
}
