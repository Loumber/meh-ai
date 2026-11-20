package com.meh.ai.tool_registry.security

import com.meh.ai.tool_registry.ToolDescriptor
import kotlinx.serialization.json.JsonObject

fun interface PermissionChecker {
    suspend fun getMissingPermissions(permissions: List<String>): List<String>
}

fun interface UserConfirmationProvider {
    suspend fun requestConfirmation(descriptor: ToolDescriptor, args: JsonObject): Boolean
}