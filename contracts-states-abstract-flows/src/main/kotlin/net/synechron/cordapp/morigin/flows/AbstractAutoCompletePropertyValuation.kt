package net.synechron.cordapp.morigin.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractAutoCompletePropertyValuation<T>() : FlowLogic<T>(), FlowHelper