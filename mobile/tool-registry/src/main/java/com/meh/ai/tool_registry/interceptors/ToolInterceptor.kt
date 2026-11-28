package com.meh.ai.tool_registry.interceptors

import com.meh.ai.tool_registry.ToolExecutionContext
import com.meh.ai.tool_registry.ToolExecutionResult

/**
 * Перехватчик выполнения инструмента (паттерн Chain of Responsibility).
 * Позволяет добавлять сквозную логику (права, подтверждения, таймауты, логирование) до и после вызова.
 */
fun interface ToolInterceptor {
    /**
     * Обрабатывает вызов инструмента. Может прервать цепочку, вернув [ToolExecutionResult.Failure],
     * или передать управление дальше через [Chain.proceed].
     */
    suspend fun intercept(chain: Chain): ToolExecutionResult

    /** Звено цепочки перехватчиков. */
    interface Chain {
        /** Текущий контекст вызова инструмента. */
        val context: ToolExecutionContext

        /** Передаёт выполнение следующему перехватчику или терминальному вызову инструмента. */
        suspend fun proceed(context: ToolExecutionContext): ToolExecutionResult
    }
}