package com.meh.ai.tool_registry

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class ToolExecutionContext(
    val callId: String,
    val descriptor: ToolDescriptor,
    val rawArguments: JsonObject
)

sealed interface ToolExecutionResult {
    val callId: String
    val toolName: String

    data class Success(
        override val callId: String,
        override val toolName: String,
        val payload: JsonElement
    ) : ToolExecutionResult

    data class Failure(
        override val callId: String,
        override val toolName: String,
        val code: ErrorCode,
        val message: String
    ) : ToolExecutionResult

    enum class ErrorCode {
        TOOL_NOT_FOUND,
        INVALID_ARGUMENTS,
        PERMISSION_DENIED,
        USER_REJECTED,
        EXECUTION_TIMEOUT,
        INTERNAL_ERROR
    }
}