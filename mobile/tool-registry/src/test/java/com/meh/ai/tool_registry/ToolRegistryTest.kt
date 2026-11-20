package com.meh.ai.tool_registry

import com.meh.ai.tool_registry.interceptors.PermissionInterceptor
import com.meh.ai.tool_registry.interceptors.UserConfirmationInterceptor
import com.meh.ai.tool_registry.schema.SchemaGenerator
import com.meh.ai.tool_registry.schema.ToolParamDescription
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@Serializable
data class MockSmsArgs(
    @ToolParamDescription("Номер телефона получателя")
    val phone: String,
    @ToolParamDescription("Текст сообщения")
    val text: String
)

@Serializable
data class MockSmsResult(val sent: Boolean, val to: String)

class MockSmsTool : TypedAgentTool<MockSmsArgs, MockSmsResult> {
    override val argsSerializer = MockSmsArgs.serializer()
    override val resultSerializer = MockSmsResult.serializer()

    override val descriptor = ToolDescriptor(
        name = "send_sms",
        description = "Отправляет SMS",
        parametersSchema = SchemaGenerator.generateParametersSchema(argsSerializer.descriptor),
        requiredPermissions = listOf("android.permission.SEND_SMS"),
        isSensitive = true
    )

    override suspend fun execute(args: MockSmsArgs): MockSmsResult {
        return MockSmsResult(sent = true, to = args.phone)
    }
}

class ToolRegistryTest {

    @Test
    fun `generates valid JSON schema for LLM`() {
        val registry = ToolRegistry()
        registry.register(MockSmsTool())

        val schema = registry.getToolsJsonSchema()
        println("Generated Schema:\n$schema")

        assertEquals(1, schema.size)
        assertTrue(schema.toString().contains("Номер телефона получателя"))
        assertTrue(schema.toString().contains("\"required\":[\"phone\",\"text\"]"))
    }

    @Test
    fun `dispatches tool successfully when permissions and confirmation granted`() = runTest {
        val registry = ToolRegistry(
            interceptors = listOf(
                PermissionInterceptor { emptyList() },
                UserConfirmationInterceptor { _, _ -> true }
            )
        )
        registry.register(MockSmsTool())

        val rawArgs = buildJsonObject {
            put("phone", "+79991234567")
            put("text", "Привет!")
        }

        val result = registry.dispatch(
            callId = "call_1",
            toolName = "send_sms",
            rawArguments = rawArgs
        )

        assertTrue(result is ToolExecutionResult.Success)
        val payload = (result as ToolExecutionResult.Success).payload.jsonObject
        assertEquals("true", payload["sent"]?.jsonPrimitive?.content)
        assertEquals("+79991234567", payload["to"]?.jsonPrimitive?.content)
    }

    @Test
    fun `returns INVALID_ARGUMENTS when model hallucinates wrong JSON`() = runTest {
        val registry = ToolRegistry()
        registry.register(MockSmsTool())

        val badArgs = buildJsonObject {
            put("phone", "+79991234567")
        }

        val result = registry.dispatch("call_2", "send_sms", badArgs)

        assertTrue(result is ToolExecutionResult.Failure)
        assertEquals(
            ToolExecutionResult.ErrorCode.INVALID_ARGUMENTS,
            (result as ToolExecutionResult.Failure).code
        )
    }

    @Test
    fun `returns PERMISSION_DENIED when Android permission is missing`() = runTest {
        val registry = ToolRegistry(
            interceptors = listOf(
                PermissionInterceptor { required -> required }
            )
        )
        registry.register(MockSmsTool())

        val result = registry.dispatch("call_3", "send_sms", buildJsonObject {})

        assertTrue(result is ToolExecutionResult.Failure)
        assertEquals(
            ToolExecutionResult.ErrorCode.PERMISSION_DENIED,
            (result as ToolExecutionResult.Failure).code
        )
    }

    @Test
    fun `returns USER_REJECTED when user declines sensitive tool`() = runTest {
        val registry = ToolRegistry(
            interceptors = listOf(
                UserConfirmationInterceptor { _, _ -> false }
            )
        )
        registry.register(MockSmsTool())

        val result = registry.dispatch("call_4", "send_sms", buildJsonObject {})

        assertTrue(result is ToolExecutionResult.Failure)
        assertEquals(
            ToolExecutionResult.ErrorCode.USER_REJECTED,
            (result as ToolExecutionResult.Failure).code
        )
    }

    @Test
    fun `returns TOOL_NOT_FOUND for unknown tool name`() = runTest {
        val registry = ToolRegistry()

        val result = registry.dispatch("call_5", "delete_system", buildJsonObject {})

        assertTrue(result is ToolExecutionResult.Failure)
        assertEquals(
            ToolExecutionResult.ErrorCode.TOOL_NOT_FOUND,
            (result as ToolExecutionResult.Failure).code
        )
    }
}