package net.synechron.cordapp.morigin.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractAutoRequestPropertyValuation<T>() : FlowLogic<T>(), FlowHelper