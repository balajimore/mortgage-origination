package net.synechron.cordapp.morigin.commons.exception

import net.corda.core.CordaRuntimeException

class InvalidFlowInitiatorException(override val message: String) : CordaRuntimeException(message)