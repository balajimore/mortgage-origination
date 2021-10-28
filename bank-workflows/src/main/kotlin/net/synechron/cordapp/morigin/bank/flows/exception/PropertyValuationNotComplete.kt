package net.synechron.cordapp.morigin.bank.flows.exception

import net.corda.core.CordaRuntimeException

class PropertyValuationNotComplete(override val message: String) : CordaRuntimeException(message)