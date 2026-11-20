package com.meh.ai.tool_registry

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

data class ToolDescriptor(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject,
    val requiredPermissions: List<String> = emptyList(),
    val isSensitive: Boolean = false
)

interface TypedAgentTool<TArgs : Any, TResult : Any> {
    val descriptor: ToolDescriptor
    val argsSerializer: KSerializer<TArgs>
    val resultSerializer: KSerializer<TResult>

    suspend fun execute(args: TArgs): TResult
}