package net.synechron.cordapp.morigin.flows


import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import net.synechron.cordapp.morigin.exception.*
import java.security.PublicKey
import java.util.*
import javax.management.Query

interface FlowHelper {
    //Default functions definition.
    fun ServiceHub.firstNotary(): Party {
        return this.networkMapCache.notaryIdentities.firstOrNull()
            ?: throw NotaryNotFoundException("No available notary.")
    }

    // Account helper function
    fun ServiceHub.accountPublicKey(accountId: UUID): PublicKey {
        return accountService.accountKeys(accountId).getOrNull(0)
            ?: throw AccountPublicKeyNotFoundException("Public key not found for Id: $accountId")
    }

    fun ServiceHub.accountPublicKey(accountName: String): PublicKey {
        return this.accountPublicKey(accountByName(accountName).identifier.id)
    }

    fun ServiceHub.accountParty(accountName: String): AnonymousParty {
        return AnonymousParty(this.accountPublicKey(accountByName(accountName).identifier.id))
    }

    fun ServiceHub.accountById(accountId: UUID): AccountInfo {
        val state = accountService.accountInfo(accountId)
            ?: throw AccountInfoNotFoundException("Account Info not found for accountId: $accountId")
        return state.state.data
    }

    fun ServiceHub.accountById(accountId: String): AccountInfo {
        val accountId2 = accountId.toUUID()
        return this.accountById(accountId2)
    }

    fun ServiceHub.accountStateAndRefById(accountId: UUID): StateAndRef<AccountInfo> {
        return accountService.accountInfo(accountId)
            ?: throw AccountInfoNotFoundException("Account Info not found for accountId: $accountId")
    }

    fun ServiceHub.accountStateAndRefById(accountId: String): StateAndRef<AccountInfo> {
        return accountService.accountInfo(accountId.toUUID())
            ?: throw AccountInfoNotFoundException("Account Info not found for accountId: $accountId")
    }

    fun ServiceHub.accountByName(accountName: String): AccountInfo {
        val accountInfo = accountService.accountInfo(accountName).firstOrNull()
            ?: throw AccountInfoNotFoundException("Account Info not found for accountName: $accountName")
        return accountInfo.state.data
    }

    fun ServiceHub.accountParty(accountId: UUID): AnonymousParty {
        val publicKey = this.accountPublicKey(accountId)
        return AnonymousParty(publicKey)
    }

    fun String?.toUUID(): UUID {
        try {
            // Parse string to UUID.
            return if (this == null) throw IllegalArgumentException("Invalid UUID string: $this")
            else UUID.fromString(this)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UUID string: $this")
        }
    }

    fun String?.toUniqueIdentifier(): UniqueIdentifier {
        val str = this
        if (str == null) {
            throw IllegalArgumentException("Invalid UniqueIdentifier string: $str!")
        }
        try {
            // Check if externalId and UUID may be separated by underscore.
            if (str.contains("_")) {
                val ids = str.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                // Create UUID object from string.
                val uuid = UUID.fromString(ids[1])
                // Create UniqueIdentifier object using externalId and UUID.
                return UniqueIdentifier(ids[0], uuid)
            }

            // Any other string used as id (i.e. UUID).
            return UniqueIdentifier(null, UUID.fromString(str))
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid UniqueIdentifier string: $exception")
        }
    }

    fun accountCriteria(accountParty: AnonymousParty) =
        QueryCriteria.VaultQueryCriteria().withParticipants(listOf(accountParty))

    fun ServiceHub.accountCriteria(accountName: String): QueryCriteria.VaultQueryCriteria {
        val accountParty = this.accountParty(accountName)
        return accountCriteria(accountParty)
    }

    fun <T : ContractState> ServiceHub.getStateByLinearId(
        accountName: String,
        linearId: UniqueIdentifier,
        clazz: Class<T>,
        stateStatus: StateStatus = StateStatus.UNCONSUMED
    ): StateAndRef<T> {
        val accountCriteria = accountCriteria(accountName)
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            null,
            listOf(linearId), stateStatus, null
        )
        val finalCriteria = queryCriteria.and(accountCriteria)
        return this.vaultService.queryBy(clazz, finalCriteria).states.let { states ->
            if (states.isEmpty()) throw StateNotFoundOnVaultException("State with id $linearId not found.")
            else if (states.size > 1) throw TooManyStatesFoundException("Too many states found with linearId: $linearId.")
            states[0]
        }
    }

    fun <T : ContractState> ServiceHub.getStateByLinearId(
        accountName: String,
        linearId: String,
        clazz: Class<T>,
        stateStatus: StateStatus = StateStatus.UNCONSUMED
    ): StateAndRef<T> {
        val linearId2 = linearId.toUniqueIdentifier()
        return this.getStateByLinearId(accountName, linearId2, clazz, stateStatus)
    }

    fun <T : ContractState> ServiceHub.getStatesByLinearId(
        linearId: UniqueIdentifier,
        clazz: Class<T>,
        stateStatus: StateStatus = StateStatus.UNCONSUMED
    ): List<StateAndRef<T>> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, listOf(linearId), stateStatus, null)
        return this.vaultService.queryBy(clazz, queryCriteria).states
    }

    fun <T : ContractState> ServiceHub.getStateByLinearId(
        linearId: UniqueIdentifier,
        clazz: Class<T>,
        stateStatus: StateStatus = StateStatus.UNCONSUMED
    ): StateAndRef<T> {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
            null,
            listOf(linearId), stateStatus, null
        )
        return this.vaultService.queryBy(clazz, queryCriteria).states.let { states ->
            if (states.isEmpty()) throw StateNotFoundOnVaultException("State with id $linearId not found.")
            else if (states.size > 1) throw TooManyStatesFoundException("Too many states found with linearId: $linearId.")
            states[0]
        }
    }

    fun <T : ContractState> ServiceHub.getStateByLinearId(
        linearId: String,
        clazz: Class<T>,
        stateStatus: StateStatus = StateStatus.UNCONSUMED
    ): StateAndRef<T> {
        val linearId2 = linearId.toUniqueIdentifier()
        return this.getStateByLinearId(linearId2, clazz, stateStatus)
    }
}