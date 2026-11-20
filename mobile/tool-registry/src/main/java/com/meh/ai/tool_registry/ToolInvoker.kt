package com.meh.ai.tool_registry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal class ToolInvoker<TArgs : Any, TResult : Any>(
    private val tool: TypedAgentTool<TArgs, TResult>,
    private val json: Json
) {
    val descriptor: ToolDescriptor get() = tool.descriptor

    suspend fun invoke(rawArgs: JsonObject): JsonElement {
        val typedArgs: TArgs = try {
            json.decodeFromJsonElement(tool.argsSerializer, rawArgs)
        } catch (e: Exception) {
            throw ToolValidationException(
                "Invalid arguments for '${descriptor.name}': ${e.message}"
            )
        }

        val result: TResult = tool.execute(typedArgs)
        return json.encodeToJsonElement(tool.resultSerializer, result)
    }
}

class ToolValidationException(message: String) : IllegalArgumentException(message)