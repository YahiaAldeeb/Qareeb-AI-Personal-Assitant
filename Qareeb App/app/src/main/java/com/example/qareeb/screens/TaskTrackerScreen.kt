package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.ui.theme.dmSansFamily

data class Task(
    val id: String,
    val title: String,
    val date: String,
    val category: String,
    val status: String,
    val price: String? = null,
    val statusColor: Color,
    val categoryColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen() {
    var selectedDay by remember { mutableStateOf("Mon") }
    var selectedCategory by remember { mutableStateOf("All") }

    val days = listOf(
        "Fri" to "5",
        "Sat" to "6",
        "Sun" to "7",
        "Mon" to "8",
        "Tue" to "9",
        "Wed" to "10",
        "Thu" to "11"
    )

    val categories = listOf("All", "Work", "Sports", "Personal", "Travel")

    val todaysTasks = listOf(
        Task(
            id = "TASK-001",
            title = "Meeting at work",
            date = "2025-06-01",
            category = "Work",
            status = "In Progress",
            statusColor = Color(0xFF9F7AEA),
            categoryColor = Color(0xFF805AD5)
        ),
        Task(
            id = "PRJ-002",
            title = "Dinner with Ahmed",
            date = "2025-08-04",
            category = "Expense",
            status = "Postponed",
            price = "$150.00",
            statusColor = Color(0xFFFC8181),
            categoryColor = Color(0xFFE53E3E)
        ),
        Task(
            id = "TASK-003",
            title = "Tennis Training",
            date = "2025-05-01",
            category = "Sports",
            status = "",
            statusColor = Color(0xFF9F7AEA),
            categoryColor = Color(0xFF805AD5)
        )
    )

    val tomorrowsTasks = listOf(
        Task(
            id = "TASK-001",
            title = "Meeting at work",
            date = "2025-06-01",
            category = "Work",
            status = "In Progress",
            statusColor = Color(0xFF9F7AEA),
            categoryColor = Color(0xFF805AD5)
        ),
        Task(
            id = "PRJ-002",
            title = "Dinner with Ahmed",
            date = "2025-08-04",
            category = "Expense",
            status = "",
            price = "$150.00",
            statusColor = Color(0xFFFC8181),
            categoryColor = Color(0xFFE53E3E)
        ),
        Task(
            id = "TASK-003",
            title = "Tennis Training",
            date = "2025-05-01",
            category = "Sports",
            status = "Postponed",
            statusColor = Color(0xFFFC8181),
            categoryColor = Color(0xFF805AD5)
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar()
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background
            FancyGradientBackground {
                Box(modifier = Modifier.fillMaxSize())
            }

            // White rounded box for content
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Section (on gradient)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Tasks",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = dmSansFamily
                            )
                            Text(
                                text = "All your plans and tasks in one place!",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontFamily = dmSansFamily
                            )
                        }
                        // Notification icon - uncomment when you have the icon
                        /*Icon(
                            painter = painterResource(id = R.drawable.mingcute_notification_fill),
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )*/
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = {
                            Text(
                                "Search for Plans",
                                color = Color.Gray,
                                fontFamily = dmSansFamily
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = Color.Gray
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // White rounded box containing the rest of the content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 80.dp, topEnd = 80.dp)
                        )
                        //azbat ma3 el serach
                        .height(200.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(top = 16.dp)
                    ) {
                        // Days of week selector
                        item {
                            LazyRow(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(days) { (day, date) ->
                                    DayChip(
                                        day = day,
                                        date = date,
                                        isSelected = day == selectedDay,
                                        onClick = { selectedDay = day }
                                    )
                                }
                            }
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

                        // Today's Plans Section
                        item {
                            Text(
                                text = "Today's Plans",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = dmSansFamily,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(todaysTasks) { task ->
                            TaskCard(task = task)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Tomorrow's Plans Section
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tomorrow's Plans",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = dmSansFamily,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(tomorrowsTasks) { task ->
                            TaskCard(task = task)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayChip(
    day: String,
    date: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF805AD5) else Color(0xFF2D3748),
        modifier = Modifier.size(47.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontFamily = dmSansFamily
            )
            Text(
                text = date,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = dmSansFamily
            )
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFF805AD5) else Color(0xFFF0F0F0),
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = dmSansFamily
        )
    }
}

@Composable
fun TaskCard(task: Task) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Task ID and Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(task.categoryColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = task.id,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontFamily = dmSansFamily
                    )
                }

                if (task.status.isNotEmpty()) {
                    Text(
                        text = task.status,
                        fontSize = 12.sp,
                        color = task.statusColor,
                        fontWeight = FontWeight.Medium,
                        fontFamily = dmSansFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Task title
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontFamily = dmSansFamily
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Date and category/price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.date,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontFamily = dmSansFamily
                )

                if (task.price != null) {
                    Text(
                        text = task.price,
                        fontSize = 14.sp,
                        color = Color(0xFFE53E3E),
                        fontWeight = FontWeight.Bold,
                        fontFamily = dmSansFamily
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = task.categoryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = task.category,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = task.categoryColor,
                            fontWeight = FontWeight.Medium,
                            fontFamily = dmSansFamily
                        )
                    }
                }
            }
        }
    }
}

// PREVIEW FUNCTION - This allows you to see the screen without running the app
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyTasksScreenPreview() {
    MyTasksScreen()
}