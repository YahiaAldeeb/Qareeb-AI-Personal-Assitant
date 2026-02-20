package com.example.qareeb.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.PlanCardTask
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TaskWelcomeBanner
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.presentation.viewModels.TaskViewModel

@Composable
fun TasksScreen(viewModel: TaskViewModel) {

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val todayTasks by viewModel.todayTasks.collectAsStateWithLifecycle()
    val tomorrowTasks by viewModel.tomorrowTasks.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {

        AppBackground { Box(modifier = Modifier.fillMaxSize()) }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ──
            TaskWelcomeBanner(username = viewModel.username)

            // ── White Sheet ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFEDF2F7),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // ── Search Bar ──
                    item {
                        SearchBarStub()
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Week Chips ──
                    item {
                        WeekChipsRow(
                            selectedDate = selectedDate,
                            onSelect = { viewModel.onDateSelected(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Category Filter Chips ──
                    item {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(viewModel.filters) { filter ->
                                CategoryChip(
                                    text = filter,
                                    isSelected = filter == selectedFilter,
                                    onClick = { viewModel.onFilterSelected(filter) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Today's Plans ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(Color.White, RoundedCornerShape(20.dp))
                        ) {
                            Column {
                                Text(
                                    text = "Today's Plans",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontFamily = dmSansFamily,
                                    modifier = Modifier.padding(
                                        start = 16.dp, top = 16.dp, bottom = 12.dp
                                    )
                                )
                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, bottom = 16.dp
                                    )
                                ) {
                                    if (todayTasks.isEmpty()) {
                                        Text(
                                            text = "No tasks for this day ✅",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            fontFamily = dmSansFamily,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        todayTasks.forEach { task ->
                                            PlanCardTask(
                                                task = task,
                                                onStatusChange = { t, newStatus ->
                                                    viewModel.updateTask(t.copy(status = newStatus))
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // ── Tomorrow's Plans ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(Color.White, RoundedCornerShape(20.dp))
                        ) {
                            Column {
                                Text(
                                    text = "Tomorrow's Plans",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontFamily = dmSansFamily,
                                    modifier = Modifier.padding(
                                        start = 16.dp, top = 16.dp, bottom = 12.dp
                                    )
                                )
                                Column(
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, bottom = 16.dp
                                    )
                                ) {
                                    if (tomorrowTasks.isEmpty()) {
                                        Text(
                                            text = "No tasks for tomorrow ✅",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            fontFamily = dmSansFamily,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        tomorrowTasks.forEach { task ->
                                            PlanCardTask(
                                                task = task,
                                                onStatusChange = { t, newStatus ->
                                                    viewModel.updateTask(t.copy(status = newStatus))
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }
}