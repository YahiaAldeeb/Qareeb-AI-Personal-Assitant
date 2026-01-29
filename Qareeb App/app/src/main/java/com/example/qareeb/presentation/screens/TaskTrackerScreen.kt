package com.example.qareeb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.data.entity.TaskStatus
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.PlanCardTask
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TaskWelcomeBanner
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.presentation.utilis.toLocalDate
import java.time.LocalDate

data class TasksUi(
    val taskId: Long,
    val title: String,
    val dueDate: Long?,
    val status: TaskStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    username: String = "Farida"
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Work", "Sports", "Personal", "Travel")

    var todaysPlans by remember {
        mutableStateOf(
            listOf(
                TasksUi(1, "Meeting at work", System.currentTimeMillis(), TaskStatus.IN_PROGRESS),
                TasksUi(2, "Dinner with Ahmed", System.currentTimeMillis(), TaskStatus.POSTPONED),
                TasksUi(3, "Tennis Training", System.currentTimeMillis(), TaskStatus.COMPLETED)
            )
        )
    }

    var tomorrowsPlans by remember {
        mutableStateOf(
            listOf(
                TasksUi(4, "Meeting at work", System.currentTimeMillis() + 86_400_000, TaskStatus.IN_PROGRESS),
                TasksUi(5, "Dinner with Ahmed", System.currentTimeMillis() + 86_400_000, TaskStatus.COMPLETED),
                TasksUi(6, "Tennis Training", System.currentTimeMillis() + 86_400_000, TaskStatus.POSTPONED)
            )
        )
    }

    val todayDate = selectedDate
    val tomorrowDate = selectedDate.plusDays(1)

    val allPlans = todaysPlans + tomorrowsPlans

    val filteredTodaysPlans = remember(selectedDate, allPlans) {
        allPlans.filter { it.dueDate?.toLocalDate() == todayDate }
    }

    val filteredTomorrowsPlans = remember(selectedDate, allPlans) {
        allPlans.filter { it.dueDate?.toLocalDate() == tomorrowDate }
    }

    val onStatusChange: (TasksUi, TaskStatus) -> Unit = { oldPlan, newStatus ->
        todaysPlans = todaysPlans.map { if (it.taskId == oldPlan.taskId) it.copy(status = newStatus) else it }
        tomorrowsPlans = tomorrowsPlans.map { if (it.taskId == oldPlan.taskId) it.copy(status = newStatus) else it }
    }

    // ✅ NO Scaffold here (MainScaffold already contains bottom nav)
    Box(modifier = Modifier.fillMaxSize()) {

        FancyGradientBackground { Box(modifier = Modifier.fillMaxSize()) }

        Column(modifier = Modifier.fillMaxSize()) {

            TaskWelcomeBanner(username = username)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(0xFFEDF2F7),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                    )
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    item {
                        SearchBarStub()
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item {
                        WeekChipsRow(selectedDate = selectedDate, onSelect = { selectedDate = it })
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                CategoryChip(
                                    text = category,
                                    isSelected = category == selectedCategory,
                                    onClick = { selectedCategory = category }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Today's Plans
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
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
                                )

                                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                    if (filteredTodaysPlans.isEmpty()) {
                                        Text(
                                            text = "No tasks for this day ✅",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            fontFamily = dmSansFamily,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        filteredTodaysPlans.forEach { plan ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 110.dp)
                                            ) {
                                                PlanCardTask(plan = plan, onStatusChange = onStatusChange)
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }

                    // Tomorrow's Plans
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
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
                                )

                                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                    if (filteredTomorrowsPlans.isEmpty()) {
                                        Text(
                                            text = "No tasks for tomorrow ✅",
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            fontFamily = dmSansFamily,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    } else {
                                        filteredTomorrowsPlans.forEach { plan ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(min = 110.dp)
                                            ) {
                                                PlanCardTask(plan = plan, onStatusChange = onStatusChange)
                                            }
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

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyTasksScreenPreview() {
    MyTasksScreen(username = "Farida")
}
