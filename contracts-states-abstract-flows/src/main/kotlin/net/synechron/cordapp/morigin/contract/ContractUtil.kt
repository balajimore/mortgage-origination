package net.synechron.cordapp.morigin.contract

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey

class ContractUtil {
    companion object {
        @JvmStatic
        fun ContractState.participantsKeys(): Set<PublicKey> {
            return this.participants.map {
                it.owningKey
            }.toSet()
        }

        @JvmStatic
        fun <T : ContractState> verifyTxComponents(tx: LedgerTransaction,
                                                   inputs: Int = 0,
                                                   outputs: Int = 0,
                                                   refs: Int = 0,
                                                   inputStateTypeAndCounts: List<Pair<Class<out T>, Int>> = emptyList(),
                                                   outputStateTypeAndCounts: List<Pair<Class<out T>, Int>> = emptyList(),
                                                   refStateTypeAndCounts: List<Pair<Class<out T>, Int>> = emptyList()
        ) = requireThat {
            if (inputs > 0) "Input contract states count must be: $inputs" using (tx.inputStates.size == inputs)
            if (outputs > 0) "Output contract states count must be: $outputs" using (tx.outputStates.size == outputs)
            if (refs > 0) "Reference contract states count must be: $refs" using (tx.referenceStates.size == refs)

            inputStateTypeAndCounts.forEach { inStatesAndCount ->
                val clazz = inStatesAndCount.first
                "The ${clazz.simpleName} input state(s) count must be: ${inStatesAndCount.second}" using (
                        tx.inputsOfType(clazz).size == inStatesAndCount.second)
            }
            outputStateTypeAndCounts.forEach { outStatesAndCount ->
                val clazz = outStatesAndCount.first
                "The ${clazz.simpleName} output state(s) count must be: ${outStatesAndCount.second}" using (
                        tx.outputsOfType(clazz).size == outStatesAndCount.second)
            }

            refStateTypeAndCounts.forEach { refStatesAndCount ->
                val clazz = refStatesAndCount.first
                "The ${clazz.simpleName} reference state(s) count must be: ${refStatesAndCount.second}" using (
                        tx.referenceInputsOfType(clazz).size == refStatesAndCount.second)
            }
        }

        @JvmStatic
        fun <T : ContractState> verifyTxComponents(tx: LedgerTransaction,
                                                   inputs: Int = 0,
                                                   outputs: Int = 0,
                                                   refs: Int = 0,
                                                   inputStateTypeAndCount: Pair<Class<out T>, Int>? = null,
                                                   outputStateTypeAndCount: Pair<Class<out T>, Int>? = null,
                                                   refStateTypeAndCount: Pair<Class<out T>, Int>? = null) {
            verifyTxComponents(tx, inputs, outputs, refs,
                    if (inputStateTypeAndCount == null) emptyList() else listOf(inputStateTypeAndCount),
                    if (outputStateTypeAndCount == null) emptyList() else listOf(outputStateTypeAndCount),
                    if (refStateTypeAndCount == null) emptyList() else listOf(refStateTypeAndCount)
            )
        }
    }
}