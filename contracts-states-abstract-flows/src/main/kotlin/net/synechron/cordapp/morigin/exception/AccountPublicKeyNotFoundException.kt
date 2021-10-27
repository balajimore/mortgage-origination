package net.synechron.cordapp.morigin.exception

import net.corda.core.CordaRuntimeException

class AccountPublicKeyNotFoundException(override val message: String) : CordaRuntimeException(message)