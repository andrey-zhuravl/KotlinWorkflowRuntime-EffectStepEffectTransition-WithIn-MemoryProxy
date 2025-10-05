package com.example.workflow.transport.kafka

/**
 * Shared Kafka configuration for workflow transport components.
 */
data class KafkaConfig(
    val bootstrapServers: String,
    val clientId: String,
    val commandTopic: String = "workflow.commands.v1",
    val replyTopic: String = "workflow.replies.v1",
    val signalTopic: String = "workflow.signals.v1"
)
