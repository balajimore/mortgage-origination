package net.synechron.cordapp.morigin.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

@InitiatingFlow
abstract class AbstractEnquiryForLoan<T>() : FlowLogic<T>(), FlowHelper