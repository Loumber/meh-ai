package com.meh.ai.tool_registry

import com.meh.ai.tool_registry.interceptors.ToolInterceptor
import com.meh.ai.tool_registry.schema.SchemaGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap

class ToolRegistry(
    private val interceptors: List<ToolInterceptor> = emptyList(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val invokers = ConcurrentHashMap<String, ToolInvoker<*, *>>()

    fun <TArgs : Any, TResult : Any> register(tool: TypedAgentTool<TArgs, TResult>) {
        invokers[tool.descriptor.name] = ToolInvoker(tool, json)
    }

    fun unregister(toolName: String) {
        invokers.remove(toolName)
    }

    fun getDescriptors(filter: (ToolDescriptor) -> Boolean = { true }): List<ToolDescriptor> {
        return invokers.values.map { it.descriptor }.filter(filter)
    }

    fun getToolsJsonSchema(filter: (ToolDescriptor) -> Boolean = { true }): JsonArray {
        return SchemaGenerator.generateToolsJsonArray(getDescriptors(filter))
    }

    suspend fun dispatch(
        callId: String,
        toolName: String,
        rawArguments: JsonObject
    ): ToolExecutionResult = withContext(dispatcher) {
        val invoker = invokers[toolName]
            ?: return@withContext ToolExecutionResult.Failure(
                callId = callId,
                toolName = toolName,
                code = ToolExecutionResult.ErrorCode.TOOL_NOT_FOUND,
                message = "Tool '$toolName' is not registered."
            )

        val initialContext = ToolExecutionContext(
            callId = callId,
            descriptor = invoker.descriptor,
            rawArguments = rawArguments
        )

        val terminalInterceptor = ToolInterceptor { chain ->
            try {
                val output = invoker.invoke(chain.context.rawArguments)
                ToolExecutionResult.Success(
                    callId = chain.context.callId,
                    toolName = chain.context.descriptor.name,
                    payload = output
                )
            } catch (e: ToolValidationException) {
                ToolExecutionResult.Failure(
                    callId = chain.context.callId,
                    toolName = chain.context.descriptor.name,
                    code = ToolExecutionResult.ErrorCode.INVALID_ARGUMENTS,
                    message = e.message ?: "Validation error"
                )
            } catch (e: Exception) {
                ToolExecutionResult.Failure(
                    callId = chain.context.callId,
                    toolName = chain.context.descriptor.name,
                    code = ToolExecutionResult.ErrorCode.INTERNAL_ERROR,
                    message = e.message ?: "Unexpected exception during tool execution"
                )
            }
        }

        val fullChain = interceptors + terminalInterceptor
        RealInterceptorChain(fullChain, index = 0, context = initialContext).proceed(initialContext)
    }
}

private class RealInterceptorChain(
    private val interceptors: List<ToolInterceptor>,
    private val index: Int,
    override val context: ToolExecutionContext
) : ToolInterceptor.Chain {
    override suspend fun proceed(context: ToolExecutionContext): ToolExecutionResult {
        val next = RealInterceptorChain(interceptors, index + 1, context)
        return interceptors[index].intercept(next)
    }
}