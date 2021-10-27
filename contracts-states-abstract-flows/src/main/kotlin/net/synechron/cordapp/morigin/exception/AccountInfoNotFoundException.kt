package net.synechron.cordapp.morigin.exception

import net.corda.core.CordaRuntimeException

class AccountInfoNotFoundException(override val message: String) : CordaRuntimeException(message)