package com.example.workflow.security

data class Subject(val id: String, val roles: Set<String>)

data class Action(val verb: String, val resourceType: String)

data class Resource(val type: String, val id: String)

enum class Decision { ALLOW, DENY, DEFER }

fun interface PolicyEngine {
    fun authorize(subject: Subject, action: Action, resource: Resource): Decision
}

data class AuditRecord(
    val subject: Subject,
    val action: Action,
    val resource: Resource,
    val decision: Decision,
    val reason: String? = null
)

@Target(AnnotationTarget.PROPERTY)
annotation class Sensitive(val field: String)

fun interface Redactor {
    fun redact(json: String): String
}

data class CryptoEnvelope(
    val keyId: String,
    val initializationVector: String,
    val cipherText: String
)
