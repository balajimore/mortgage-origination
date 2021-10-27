package net.synechron.cordapp.morigin.contract

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.synechron.cordapp.morigin.state.RealEstateProperty

class RealEstatePropertyContract : EvolvableTokenContract(), Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID = RealEstatePropertyContract::class.java.name!!
    }

    override fun additionalCreateChecks(tx: LedgerTransaction) = requireThat {
        val outputState = tx.getOutput(0) as RealEstateProperty
        "Valuation cannot be zero" using (outputState.propertyValue.quantity > 0)
        //"Constructed date should not be in future." - need to customize flow that adds time-window to tx.
        "Invalid construction area" using (outputState.propertyAddress.isNotBlank())
        "Invalid construction area" using (outputState.constructionArea.isNotBlank())
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as RealEstateProperty
        val inputState = tx.getInput(0) as RealEstateProperty
        "Construction date should be changed." using (outputState.constructedOn == inputState.constructedOn)
        "Construction date should be changed." using (outputState.fractionDigits == inputState.fractionDigits)
    }
}