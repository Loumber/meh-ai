package com.meh.ai.tool_registry.interceptors

import com.meh.ai.tool_registry.ToolExecutionContext
import com.meh.ai.tool_registry.ToolExecutionResult

fun interface ToolInterceptor {
    suspend fun intercept(chain: Chain): ToolExecutionResult

    interface Chain {
        val context: ToolExecutionContext
        suspend fun proceed(context: ToolExecutionContext): ToolExecutionResult
    }
}