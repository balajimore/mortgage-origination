package net.synechron.cordapp.morigin.flows


import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.internal.accountService
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.getOrThrow
import net.synechron.cordapp.morigin.exception.AccountInfoNotFoundException
import net.synechron.cordapp.morigin.exception.AccountPublicKeyNotFoundException
import net.synechron.cordapp.morigin.exception.NotaryNotFoundException
import java.security.PublicKey
import java.util.*

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
}