package com.example.qareeb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.ui.components.BottomNavBar
import com.example.qareeb.presentation.ui.components.CategoryChip
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.PlanCard
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.presentation.ui.components.TaskWelcomeBanner
import com.example.qareeb.presentation.ui.components.WeekChipsRow
import com.example.qareeb.ui.theme.dmSansFamily
import com.example.qareeb.utilis.toLocalDate
import java.time.LocalDate

data class TasksUi(
    val taskId: Long,
    val title: String,
    val dueDate: Long?,
    val status: com.example.qareeb.data.entity.TaskStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen(
    username: String = "Farida"
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Work", "Sports", "Personal", "Travel")

    val todaysPlans = remember {
        listOf(
            TaskUi(
                taskId = 1,
                title = "Meeting at work",
                status = com.example.qareeb.data.entity.TaskStatus.IN_PROGRESS,
                dueDate = System.currentTimeMillis()
            ),
            TaskUi(
                taskId = 2,
                title = "Dinner with Ahmed",
                status = com.example.qareeb.data.entity.TaskStatus.POSTPONED,
                dueDate = System.currentTimeMillis()
            ),
            TaskUi(
                taskId = 3,
                title = "Tennis Training",
                status = com.example.qareeb.data.entity.TaskStatus.COMPLETED,
                dueDate = System.currentTimeMillis()
            )
        )
    }

    val tomorrowsPlans = remember {
        listOf(
            TaskUi(
                taskId = 4,
                title = "Meeting at work",
                status = com.example.qareeb.data.entity.TaskStatus.IN_PROGRESS,
                dueDate = System.currentTimeMillis() + 86_400_000
            ),
            TaskUi(
                taskId = 5,
                title = "Dinner with Ahmed",
                status = com.example.qareeb.data.entity.TaskStatus.COMPLETED,
                dueDate = System.currentTimeMillis() + 86_400_000
            ),
            TaskUi(
                taskId = 6,
                title = "Tennis Training",
                status = com.example.qareeb.data.entity.TaskStatus.POSTPONED,
                dueDate = System.currentTimeMillis() + 86_400_000
            )
        )
    }

    // ✅ This is the logic you want:
    // - Today's box shows tasks for selectedDate
    // - Tomorrow's box shows tasks for selectedDate + 1
    val todayDate = selectedDate
    val tomorrowDate = selectedDate.plusDays(1)

    val filteredTodaysPlans = remember(selectedDate, todaysPlans, tomorrowsPlans) {
        (todaysPlans + tomorrowsPlans).filter { plan ->
            plan.dueDate?.toLocalDate() == todayDate
        }
    }

    val filteredTomorrowsPlans = remember(selectedDate, todaysPlans, tomorrowsPlans) {
        (todaysPlans + tomorrowsPlans).filter { plan ->
            plan.dueDate?.toLocalDate() == tomorrowDate
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavBar() }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            // Gradient Background
            FancyGradientBackground {
                Box(modifier = Modifier.fillMaxSize())
            }

            // Content
            Column(modifier = Modifier.fillMaxSize()) {

                // Header
                TaskWelcomeBanner(username = username)

                // Main container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFEDF2F7),
                            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Search Bar
                        item {
                            SearchBarStub()
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Week chips
                        item {
                            WeekChipsRow(
                                selectedDate = selectedDate,
                                onSelect = { selectedDate = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Category filters
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

                        // ✅ Today's Plans Box (selectedDate)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column {
                                    Text(
                                        text = "Today's Plans",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = dmSansFamily,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            top = 16.dp,
                                            bottom = 12.dp
                                        )
                                    )

                                    Column(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                        )
                                    ) {
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
                                                    PlanCard(plan = plan)
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Space between the two boxes
                        item { Spacer(modifier = Modifier.height(24.dp)) }

                        // ✅ Tomorrow's Plans Box (selectedDate + 1)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Column {
                                    Text(
                                        text = "Tomorrow's Plans",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        fontFamily = dmSansFamily,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            top = 16.dp,
                                            bottom = 12.dp
                                        )
                                    )

                                    Column(
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                        )
                                    ) {
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
                                                    PlanCard(plan = plan)
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
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
