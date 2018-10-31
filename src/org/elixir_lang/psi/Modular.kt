package org.elixir_lang.psi

import org.elixir_lang.Arity
import org.elixir_lang.ArityRange
import org.elixir_lang.Name
import org.elixir_lang.psi.call.Call
import org.elixir_lang.psi.impl.call.macroChildCallSequence
import org.elixir_lang.psi.impl.call.macroChildCalls

data class AccumulatorContinue<out R>(val accumulator: R, val `continue`: Boolean)

object Modular {
    @JvmStatic
    fun callDefinitionClauseCallSequence(modular: Call): Sequence<Call> =
            modular.macroChildCallSequence().filter { CallDefinitionClause.`is`(it) }

    @JvmStatic
    fun callDefinitionClauseCallWhile(modular: Call, function: (Call) -> Boolean): Boolean {
        val childCalls = modular.macroChildCalls()
        var keepProcessing = true

        for (childCall in childCalls) {
            if (CallDefinitionClause.`is`(childCall) && !function(childCall)) {
                keepProcessing = false

                break
            }
        }

        return keepProcessing
    }

    @JvmStatic
    inline fun <R> callDefinitionClauseCallFoldWhile(
            modular: Call,
            initial: R,
            foldWhile: (Call, R) -> AccumulatorContinue<R>
    ): AccumulatorContinue<R> {
        val childCalls = modular.macroChildCalls()
        var accumulatorContinue = AccumulatorContinue(initial, true)

        for (childChild in childCalls) {
            if (CallDefinitionClause.`is`(childChild)) {
                accumulatorContinue = foldWhile(childChild, accumulatorContinue.accumulator)

                if (!accumulatorContinue.`continue`) {
                    break
                }
            }
        }

        return accumulatorContinue
    }

    inline fun <R> callDefinitionClauseCallFoldWhile(
            modular: Call,
            functionName: Name,
            initial: R,
            foldWhile: (Call, Name, ArityRange,  R) -> AccumulatorContinue<R>
    ): AccumulatorContinue<R> =
            callDefinitionClauseCallFoldWhile(modular, initial) { callDefinitionClauseCall, acc ->
                CallDefinitionClause.nameArityRange(callDefinitionClauseCall)?.let { (name, arityRange) ->
                    if (name == functionName) {
                            foldWhile(callDefinitionClauseCall, name, arityRange, acc)
                    } else {
                        null
                    }
                } ?:
                AccumulatorContinue(acc, true)
            }

    @JvmStatic
    inline fun <R> callDefinitionClauseCallFoldWhile(
            modular: Call,
            functionName: Name,
            resolvedFinalArity: Arity,
            initial: R,
            foldWhile: (Call, String, IntRange, R) -> AccumulatorContinue<R>
    ): AccumulatorContinue<R> =
            callDefinitionClauseCallFoldWhile(
                    modular,
                    functionName,
                    initial
            ) { callDefinitionClauseCall, name, arityRange, acc ->
                if (arityRange.contains(resolvedFinalArity)) {
                    foldWhile(callDefinitionClauseCall, name, arityRange, acc)
                } else {
                    AccumulatorContinue(acc, true)
                }
            }
}