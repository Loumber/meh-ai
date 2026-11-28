package com.meh.ai.tool_registry.security

import com.meh.ai.tool_registry.ToolDescriptor
import kotlinx.serialization.json.JsonObject

/**
 * Платформенный контракт проверки системных разрешений (реализуется в Android-слое).
 */
fun interface PermissionChecker {
    /**
     * Проверяет наличие запрошенных разрешений у приложения.
     *
     * @param permissions Список разрешений из [ToolDescriptor.requiredPermissions].
     * @return Список недостающих разрешений. Пустой список означает, что все права выданы.
     */
    suspend fun getMissingPermissions(permissions: List<String>): List<String>
}

/**
 * Платформенный контракт запроса подтверждения у пользователя (Human-in-the-loop).
 */
fun interface UserConfirmationProvider {
    /**
     * Приостанавливает выполнение инструмента до получения ответа пользователя в UI.
     *
     * @param descriptor Метаданные вызываемого инструмента.
     * @param args Аргументы, которые модель собирается передать инструменту.
     * @return `true`, если пользователь подтвердил операцию; `false`, если отклонил.
     */
    suspend fun requestConfirmation(descriptor: ToolDescriptor, args: JsonObject): Boolean
}