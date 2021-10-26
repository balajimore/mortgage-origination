package net.synechron.cordapp.morigin.flows

import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow

/**
 * An abstract FlowLogic class that is used to implement Node specific Initiator and Responder classes.
 * It also provides helper methods.
 */
@InitiatingFlow
abstract class AbstractIssueObligationFlow<out T> : FlowLogic<T>(), FlowHelper