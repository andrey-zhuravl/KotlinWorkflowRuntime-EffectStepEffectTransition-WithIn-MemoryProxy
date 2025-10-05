package com.example.workflow.persistence

data class HashChainEntry(
    val sequence: Long,
    val hash: ByteArray,
    val previousHash: ByteArray?,
    val algorithm: String = "SHA-256"
)

interface HashChainValidator {
    fun validate(entries: List<HashChainEntry>): Boolean
}
