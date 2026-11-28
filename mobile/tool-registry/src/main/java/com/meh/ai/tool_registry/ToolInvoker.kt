package com.meh.ai.tool_registry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Внутренняя обёртка стирания типов (Type Erasure) над [TypedAgentTool].
 * Преобразует сырой JSON в [TArgs], вызывает инструмент и сериализует [TResult] обратно в JSON.
 */
internal class ToolInvoker<TArgs : Any, TResult : Any>(
    private val tool: TypedAgentTool<TArgs, TResult>,
    private val json: Json
) {
    val descriptor: ToolDescriptor get() = tool.descriptor

    /**
     * Десериализует [rawArgs], запускает [TypedAgentTool.execute] и возвращает JSON-результат.
     *
     * @param rawArgs Сырые аргументы от модели.
     * @return Сериализованный результат выполнения.
     * @throws ToolValidationException Если [rawArgs] не удалось распарсить в [TArgs].
     * @throws Exception Пробрасывает исключения, возникшие внутри [TypedAgentTool.execute].
     */
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

/**
 * Исключение валидации аргументов, выбрасываемое при несоответствии JSON-объекта от модели
 * ожидаемому Kotlin data-классу инструмента.
 */
class ToolValidationException(message: String) : IllegalArgumentException(message)