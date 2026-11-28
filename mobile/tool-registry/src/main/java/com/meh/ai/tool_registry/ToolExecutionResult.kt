package com.meh.ai.tool_registry

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Контекст текущего вызова инструмента, передаваемый по цепочке перехватчиков.
 *
 * @property callId Уникальный идентификатор вызова от LLM (для сопоставления запроса и ответа).
 * @property descriptor Паспорт вызываемого инструмента.
 * @property rawArguments Сырые JSON-аргументы, полученные от языковой модели.
 */
data class ToolExecutionContext(
    val callId: String,
    val descriptor: ToolDescriptor,
    val rawArguments: JsonObject
)

/**
 * Стандартизированный результат выполнения инструмента.
 * Гарантирует отсутствие необработанных исключений на выходе из [ToolRegistry.dispatch].
 */
sealed interface ToolExecutionResult {
    /** Идентификатор вызова, совпадающий с входящим `callId`. */
    val callId: String

    /** Имя инструмента, к которому относится результат. */
    val toolName: String

    /**
     * Успешное выполнение инструмента.
     *
     * @property payload Сериализованный JSON-ответ для передачи обратно в LLM.
     */
    data class Success(
        override val callId: String,
        override val toolName: String,
        val payload: JsonElement
    ) : ToolExecutionResult

    /**
     * Контролируемый сбой на любом этапе пайплайна (валидация, права, таймаут, исключение API).
     *
     * @property code Типизированный код причины ошибки.
     * @property message Диагностическое описание ошибки (может передаваться модели для самоисправления).
     */
    data class Failure(
        override val callId: String,
        override val toolName: String,
        val code: ErrorCode,
        val message: String
    ) : ToolExecutionResult

    /** Классификация возможных ошибок при маршрутизации и исполнении инструмента. */
    enum class ErrorCode {
        /** Инструмент с указанным именем не зарегистрирован в реестре. */
        TOOL_NOT_FOUND,

        /** Аргументы от модели не соответствуют JSON-схеме или не прошли десериализацию. */
        INVALID_ARGUMENTS,

        /** У приложения отсутствуют необходимые системные разрешения Android. */
        PERMISSION_DENIED,

        /** Пользователь отклонил выполнение чувствительной операции (`isSensitive = true`). */
        USER_REJECTED,

        /** Превышен лимит времени ожидания выполнения инструмента. */
        EXECUTION_TIMEOUT,

        /** Непредвиденное исключение внутри метода [TypedAgentTool.execute]. */
        INTERNAL_ERROR
    }
}