package com.example.qareeb.domain.model

data class UserDomain(
    val userId: String,
    val name: String,
    val email: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null,
    val voiceEmbedding: String? = null
)