package net.synechron.cordapp.morigin.exception

import net.corda.core.CordaRuntimeException

class NotaryNotFoundException(override val message: String) : CordaRuntimeException(message)