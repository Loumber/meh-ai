package com.meh.ai.tool_registry

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject

/**
 * Статический паспорт инструмента, описывающий его контракт для LLM и системы безопасности.
 *
 * @property name Уникальное имя функции (например, `"send_sms"`), по которому модель вызывает инструмент.
 * @property description Описание назначения инструмента для системного промпта / Function Calling API.
 * @property parametersSchema JSON Schema (Draft-07) входных аргументов, сгенерированная через [com.meh.ai.tool_registry.schema.SchemaGenerator].
 * @property requiredPermissions Список системных разрешений Android (например, `Manifest.permission.SEND_SMS`), необходимых для запуска.
 * @property isSensitive Флаг критичности: если `true`, перед выполнением требуется подтверждение пользователя.
 */
data class ToolDescriptor(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject,
    val requiredPermissions: List<String> = emptyList(),
    val isSensitive: Boolean = false
)

/**
 * Строго типизированный контракт системного инструмента.
 * Изолирует бизнес-логику (Android API) от парсинга JSON, проверки прав и обработки ошибок.
 *
 * @param TArgs Тип `@Serializable` data-класса входных аргументов.
 * @param TResult Тип `@Serializable` data-класса возвращаемого результата.
 */
interface TypedAgentTool<TArgs : Any, TResult : Any> {
    /** Метаданные и JSON-схема инструмента. */
    val descriptor: ToolDescriptor

    /** Сериализатор для десериализации входящего JSON от модели в [TArgs]. */
    val argsSerializer: KSerializer<TArgs>

    /** Сериализатор для упаковки результата [TResult] обратно в JSON. */
    val resultSerializer: KSerializer<TResult>

    /**
     * Выполняет основную логику инструмента с уже провалидированными аргументами.
     *
     * @param args Десериализованные аргументы вызова.
     * @return Результат выполнения операции.
     * @throws Exception Любое исключение Android API будет перехвачено [ToolRegistry] и преобразовано в [ToolExecutionResult.Failure] с кодом [ToolExecutionResult.ErrorCode.INTERNAL_ERROR].
     */
    suspend fun execute(args: TArgs): TResult
}