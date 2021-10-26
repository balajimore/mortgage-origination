package net.synechron.cordapp.morigin.commons.exception

import net.corda.core.CordaRuntimeException

class TooManyStatesFoundException(override val message: String) : CordaRuntimeException(message)