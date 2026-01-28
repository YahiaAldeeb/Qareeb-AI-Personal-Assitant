package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.example.qareeb.presentation.utilis.buildNext7DaysFromToday
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekChipsRow(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { buildNext7DaysFromToday(today) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(days) { item ->
            DayCard(
                dayLabel = item.label,
                dayNumber = item.date.dayOfMonth,     // ✅ real calendar day number
                selected = item.date == selectedDate, // ✅ purple only when selected
                onClick = { onSelect(item.date) }
            )
        }
    }
}