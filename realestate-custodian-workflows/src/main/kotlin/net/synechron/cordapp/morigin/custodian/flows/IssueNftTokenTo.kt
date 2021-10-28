package net.synechron.cordapp.morigin.custodian.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.synechron.cordapp.morigin.flows.FlowHelper
import net.synechron.cordapp.morigin.state.RealEstateProperty
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * Flow to create the user account.
 */
@StartableByRPC
@InitiatingFlow
class IssueNftTokenTo(
    val propertyValue: Amount<Currency>,
    val constructionArea: String,
    val propertyAddress: String,
    val issueToAccName: String
) : FlowLogic<String>(), FlowHelper {

    @Suspendable
    override fun call(): String {
        val issuer = ourIdentity
        val constructedOn = constructedDate()
        val holder = serviceHub.accountParty(issueToAccName)

        val propertyToken = RealEstateProperty(
            UniqueIdentifier(),
            listOf(issuer),
            propertyValue,
            constructionArea,
            constructedOn,
            propertyAddress
        )

        // Create the real estate property NFT on ledger.
        val notary = serviceHub.firstNotary()
        val transactionState = TransactionState(data = propertyToken, notary = notary)
        subFlow(CreateEvolvableTokens(transactionState))

        // Create NFT.
        val issuedPropertyToken = IssuedTokenType(
            issuer,
            propertyToken.toPointer(propertyToken.javaClass)
        )
        val nftPropertyToken = NonFungibleToken(issuedPropertyToken, holder, UniqueIdentifier())

        // Issue the real estate property NFT token.
        subFlow(IssueTokens(listOf(nftPropertyToken)))

        return  nftPropertyToken.linearId.toString()
    }

    private fun constructedDate(): LocalDate {
        val nextInt = ThreadLocalRandom.current().nextInt(2, 10)
        return LocalDate.now().minusMonths(nextInt.toLong())
    }
}