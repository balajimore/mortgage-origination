package net.synechron.cordapp.morigin.flows

import net.synechron.cordapp.morigin.exception.NotaryNotFoundException
import net.synechron.cordapp.morigin.exception.StateNotFoundOnVaultException
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

/**
 * It offers helper methods to get Notary from network, resolve [AbstractParty] to well-known identity,
 * query vault for [ContractState] by it's [UniqueIdentifier] linearId.
 */
interface FlowHelper {
    //Default functions definition.
    fun ServiceHub.firstNotary(): Party {
        return this.networkMapCache.notaryIdentities.firstOrNull()
                ?: throw NotaryNotFoundException("No available notary.")
    }

    fun <T : ContractState> ServiceHub.getStateByLinearId(linearId: UniqueIdentifier, clazz: Class<T>): StateAndRef<T> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null,
                listOf(linearId), Vault.StateStatus.UNCONSUMED, null)
        return this.vaultService.queryBy(clazz, queryCriteria).states.singleOrNull()
                ?: throw StateNotFoundOnVaultException("State with id $linearId not found.")
    }

    fun ServiceHub.resolveIdentity(abstractParty: AbstractParty): Party {
        return this.identityService.requireWellKnownPartyFromAnonymous(abstractParty)
    }
}