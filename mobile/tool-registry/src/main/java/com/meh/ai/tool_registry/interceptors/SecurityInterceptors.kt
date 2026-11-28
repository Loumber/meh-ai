package com.meh.ai.tool_registry.interceptors

import com.meh.ai.tool_registry.ToolExecutionResult
import com.meh.ai.tool_registry.security.PermissionChecker
import com.meh.ai.tool_registry.security.UserConfirmationProvider
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Ограничивает максимальное время выполнения инструмента.
 * При превышении [timeoutMs] возвращает [ToolExecutionResult.ErrorCode.EXECUTION_TIMEOUT].
 *
 * @property timeoutMs Лимит времени в миллисекундах (по умолчанию 10 секунд).
 */
class TimeoutInterceptor(
    private val timeoutMs: Long = 10_000L
) : ToolInterceptor {

    override suspend fun intercept(chain: ToolInterceptor.Chain): ToolExecutionResult {
        return try {
            withTimeout(timeoutMs) {
                chain.proceed(chain.context)
            }
        } catch (e: TimeoutCancellationException) {
            ToolExecutionResult.Failure(
                callId = chain.context.callId,
                toolName = chain.context.descriptor.name,
                code = ToolExecutionResult.ErrorCode.EXECUTION_TIMEOUT,
                message = "Tool '${chain.context.descriptor.name}' timed out after ${timeoutMs}ms"
            )
        }
    }
}

/**
 * Проверяет наличие системных разрешений из [com.meh.ai.tool_registry.ToolDescriptor.requiredPermissions].
 * При отсутствии прав прерывает цепочку с кодом [ToolExecutionResult.ErrorCode.PERMISSION_DENIED].
 */
class PermissionInterceptor(
    private val permissionChecker: PermissionChecker
) : ToolInterceptor {

    override suspend fun intercept(chain: ToolInterceptor.Chain): ToolExecutionResult {
        val required = chain.context.descriptor.requiredPermissions
        if (required.isEmpty()) return chain.proceed(chain.context)

        val missing = permissionChecker.getMissingPermissions(required)
        if (missing.isNotEmpty()) {
            return ToolExecutionResult.Failure(
                callId = chain.context.callId,
                toolName = chain.context.descriptor.name,
                code = ToolExecutionResult.ErrorCode.PERMISSION_DENIED,
                message = "Missing permissions: ${missing.joinToString()}"
            )
        }

        return chain.proceed(chain.context)
    }
}

/**
 * Запрашивает подтверждение пользователя для инструментов с флагом `isSensitive = true`.
 * При отказе прерывает цепочку с кодом [ToolExecutionResult.ErrorCode.USER_REJECTED].
 */
class UserConfirmationInterceptor(
    private val confirmationProvider: UserConfirmationProvider
) : ToolInterceptor {

    override suspend fun intercept(chain: ToolInterceptor.Chain): ToolExecutionResult {
        val descriptor = chain.context.descriptor
        if (!descriptor.isSensitive) return chain.proceed(chain.context)

        val isApproved = confirmationProvider.requestConfirmation(
            descriptor = descriptor,
            args = chain.context.rawArguments
        )

        if (!isApproved) {
            return ToolExecutionResult.Failure(
                callId = chain.context.callId,
                toolName = descriptor.name,
                code = ToolExecutionResult.ErrorCode.USER_REJECTED,
                message = "User declined execution of sensitive tool '${descriptor.name}'."
            )
        }

        return chain.proceed(chain.context)
    }
}