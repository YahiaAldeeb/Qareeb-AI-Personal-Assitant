package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.presentation.ui.components.FancyGradientBackground
import com.example.qareeb.presentation.ui.components.SearchBarStub
import com.example.qareeb.ui.theme.dmSansFamily

data class FinanceTask(
    val id: String,
    val title: String,
    val date: String,
    val category: String,
    val status: String,
    val price: String? = null,
    val statusColor: Color,
    val categoryColor: Color,
    val icon: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinanceScreen() {
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
        FinanceTask(
            id = "TASK-001",
            title = "Meeting at work",
            date = "2025-06-01",
            category = "Work",
            status = "In Progress",
            statusColor = Color(0xFF9F7AEA),
            categoryColor = Color(0xFF805AD5)
        ),
        FinanceTask(
            id = "PRJ-002",
            title = "Dinner with Ahmed",
            date = "2025-08-04",
            category = "Expense",
            status = "Complete",
            price = "$150.00",
            statusColor = Color(0xFF48BB78),
            categoryColor = Color(0xFF38B2AC)
        ),
        FinanceTask(
            id = "TASK-003",
            title = "Tennis Training",
            date = "2025-05-01",
            category = "Sports",
            status = "Postponed",
            statusColor = Color(0xFFFC8181),
            categoryColor = Color(0xFF805AD5)
        )
    )

    val tomorrowsTasks = listOf(
        FinanceTask(
            id = "TASK-001",
            title = "Meeting at work",
            date = "2025-06-01",
            category = "Work",
            status = "In Progress",
            statusColor = Color(0xFF9F7AEA),
            categoryColor = Color(0xFF805AD5)
        ),
        FinanceTask(
            id = "FIN-002",
            title = "Dinner with Ahmed",
            date = "2025-08-04",
            category = "Expense",
            status = "Complete",
            price = "$150.00",
            statusColor = Color(0xFF48BB78),
            categoryColor = Color(0xFF38B2AC)
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar()
        }
    ) { paddingValues ->
        FancyGradientBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Section with Profile
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 50.dp)
                    ) {
                        // Profile header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Profile image placeholder
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "My Finance",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = dmSansFamily
                                    )
                                    Text(
                                        text = "All your spendings and savings in one place!",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = dmSansFamily
                                    )
                                }
                            }
                            // Notification icon placeholder
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔔", fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Summary Card
                        CardBackground(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
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
                                                .background(Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "8 JUNE 2025",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = dmSansFamily
                                        )
                                    }
                                    Pill(text = "Report")
                                }

                                Column {
                                    Text(
                                        text = "Today's AI Analysis",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontFamily = dmSansFamily
                                    )
                                    Text(
                                        text = "You Have 8 Tasks For",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = dmSansFamily
                                    )
                                    Text(
                                        text = "Today.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = dmSansFamily
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Search Bar
                        SearchBarStub()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Days of week selector
                        LazyRow(
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

                        // Category filters
                        LazyRow(
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
                    FinanceTaskCard(task = task)
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
                    FinanceTaskCard(task = task)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun FinanceTaskCard(task: FinanceTask) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(task.categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(task.categoryColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(task.categoryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.id,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = dmSansFamily
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = dmSansFamily
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = task.date,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = dmSansFamily
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (task.status.isNotEmpty()) {
                    Text(
                        text = task.status,
                        fontSize = 11.sp,
                        color = task.statusColor,
                        fontWeight = FontWeight.Medium,
                        fontFamily = dmSansFamily
                    )
                }

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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
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

@Composable
fun BottomNavigationBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(icon = Icons.Default.Home, label = "Home", isSelected = true)
            BottomNavItem(icon = Icons.Default.Search, label = "Tasks", isSelected = false)

            // Floating Action Button in center
            FloatingActionButton(
                onClick = { },
                containerColor = Color(0xFF805AD5),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            BottomNavItem(icon = Icons.Default.Search, label = "History", isSelected = false)
            BottomNavItem(icon = Icons.Default.Person, label = "Profile", isSelected = false)
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF805AD5) else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF805AD5) else Color.Gray,
            fontFamily = dmSansFamily
        )
    }
}

// PREVIEW FUNCTION
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyFinanceScreenPreview() {
    MyFinanceScreen()
}