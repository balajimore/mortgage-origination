package net.synechron.cordapp.morigin.exception

import net.corda.core.CordaRuntimeException

class TooManyStatesFoundException(override val message: String) : CordaRuntimeException(message)