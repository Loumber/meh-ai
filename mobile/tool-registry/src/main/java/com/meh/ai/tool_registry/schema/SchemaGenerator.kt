package com.meh.ai.tool_registry.schema

import com.meh.ai.tool_registry.ToolDescriptor
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Аннотация для добавления текстового описания поля в генерируемую JSON Schema.
 * Помогает языковой модели точнее понимать формат и назначение параметра.
 *
 * @property description Пояснение для LLM (например, `"Номер телефона в формате +7..."`).
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ToolParamDescription(val description: String)

/**
 * Утилита автогенерации JSON Schema (Draft-07) из [SerialDescriptor] классов `kotlinx.serialization`.
 */
@OptIn(ExperimentalSerializationApi::class)
object SchemaGenerator {

    /**
     * Генерирует JSON Schema входных параметров инструмента на основе дескриптора его `@Serializable` класса.
     * Поля без значения по умолчанию автоматически попадают в массив `required`.
     *
     * @param descriptor Дескриптор сериализатора аргументов (`MyArgs.serializer().descriptor`).
     * @return Готовый [JsonObject] с полями `type`, `properties` и `required`.
     */
    fun generateParametersSchema(descriptor: SerialDescriptor): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            for (i in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(i)
                val elementDesc = descriptor.getElementDescriptor(i)
                val description = descriptor.getElementAnnotations(i)
                    .filterIsInstance<ToolParamDescription>()
                    .firstOrNull()?.description

                put(name, buildPropertySchema(elementDesc, description))
            }
        }
        val requiredFields = (0 until descriptor.elementsCount)
            .filter { !descriptor.isElementOptional(it) }
            .map { descriptor.getElementName(it) }

        if (requiredFields.isNotEmpty()) {
            putJsonArray("required") {
                requiredFields.forEach { add(it) }
            }
        }
    }

    /**
     * Собирает массив всех инструментов в стандартном формате Function Calling (OpenAI / Gemini / Local LLM).
     *
     * @param descriptors Список дескрипторов доступных инструментов.
     * @return [JsonArray], готовый для вставки в поле `tools` API-запроса или системный промпт.
     */
    fun generateToolsJsonArray(descriptors: List<ToolDescriptor>): JsonArray = buildJsonArray {
        descriptors.forEach { tool ->
            addJsonObject {
                put("type", "function")
                putJsonObject("function") {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("parameters", tool.parametersSchema)
                }
            }
        }
    }

    private fun buildPropertySchema(
        descriptor: SerialDescriptor,
        description: String?
    ): JsonObject = buildJsonObject {
        when (descriptor.kind) {
            PrimitiveKind.STRING -> put("type", "string")
            PrimitiveKind.BOOLEAN -> put("type", "boolean")
            PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.LONG -> put(
                "type",
                "integer"
            )

            PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> put("type", "number")
            SerialKind.ENUM -> {
                put("type", "string")
                putJsonArray("enum") {
                    for (i in 0 until descriptor.elementsCount) {
                        add(descriptor.getElementName(i))
                    }
                }
            }

            StructureKind.LIST -> {
                put("type", "array")
                if (descriptor.elementsCount > 0) {
                    put("items", buildPropertySchema(descriptor.getElementDescriptor(0), null))
                }
            }

            StructureKind.CLASS -> {
                val nested = generateParametersSchema(descriptor)
                nested.forEach { (k, v) -> put(k, v) }
            }

            else -> put("type", "object")
        }
        if (description != null) {
            put("description", description)
        }
    }
}