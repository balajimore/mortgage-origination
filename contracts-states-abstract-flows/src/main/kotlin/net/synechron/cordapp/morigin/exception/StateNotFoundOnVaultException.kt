package net.synechron.cordapp.morigin.exception

import net.corda.core.CordaRuntimeException

class StateNotFoundOnVaultException(override val message: String) : CordaRuntimeException(message)