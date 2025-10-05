package com.example.workflow.transport.kafka

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Simple serialization helpers built on kotlinx.serialization.
 */
object Serde {
    private val json = Json { ignoreUnknownKeys = true }

    fun <T> serialize(serializer: KSerializer<T>, value: T): String = json.encodeToString(serializer, value)
    fun <T> deserialize(serializer: KSerializer<T>, value: String): T = json.decodeFromString(serializer, value)
}
