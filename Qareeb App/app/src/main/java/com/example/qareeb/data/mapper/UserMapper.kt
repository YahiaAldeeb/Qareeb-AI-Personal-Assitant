package com.example.qareeb.data.mapper

// data/mapper/UserMapper.kt
import com.example.qareeb.data.entity.User          // Room entity
import com.example.qareeb.domain.model.UserDomain    // domain model

fun User.toDomain(): UserDomain {
    return UserDomain(
        userId = userId,
        name = name,
        email = email,
        password = password,
        createdAt = createdAt,
        lastLogin = lastLogin,
        voiceEmbedding = voiceEmbedding
    )
}

fun UserDomain.toEntity(): User {
    return User(
        userId = userId,
        name = name,
        email = email,
        password = password,
        createdAt = createdAt,
        lastLogin = lastLogin,
        voiceEmbedding = voiceEmbedding
    )
}