package com.example.qareeb.utilis

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
data class DayItem(
    val label: String,     // "Fri"
    val date: LocalDate    // real date
)
fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
}
fun buildNext7DaysFromToday(today: LocalDate): List<DayItem> {
    return (0..6).map { i ->
        val d = today.plusDays(i.toLong())
        DayItem(
            label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), // Fri, Sat...
            date = d
        )
    }
}
fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}