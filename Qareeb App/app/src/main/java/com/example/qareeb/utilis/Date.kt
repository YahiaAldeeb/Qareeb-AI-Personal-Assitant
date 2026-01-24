package com.example.qareeb.utilis

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}