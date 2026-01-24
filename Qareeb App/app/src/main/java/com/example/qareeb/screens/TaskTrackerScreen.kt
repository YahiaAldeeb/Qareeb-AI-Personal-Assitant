package com.example.qareeb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class PlanItem(
    val title: String,
    val time: String,
    val tag: String,
    val tagColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTasksScreen() {
    var selectedDay by remember { mutableStateOf("Mon") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Work", "Sports", "Personal", "Travel")

    val todaysPlans = listOf(
        PlanItem(
            title = "Meeting at work",
            time = "2025-06-01",
            tag = "Work",
            tagColor = Color(0xFF805AD5)
        ),
        PlanItem(
            title = "Dinner with Ahmed",
            time = "2025-08-04",
            tag = "High",
            tagColor = Color(0xFFE53E3E)
        ),
        PlanItem(
            title = "Tennis Training",
            time = "2025-05-01",
            tag = "Sports",
            tagColor = Color(0xFF805AD5)
        )
    )

    val tomorrowsPlans = listOf(
        PlanItem(
            title = "Meeting at work",
            time = "2025-06-01",
            tag = "Work",
            tagColor = Color(0xFF805AD5)
        ),
        PlanItem(
            title = "Dinner with Ahmed",
            time = "2025-08-04",
            tag = "Expense",
            tagColor = Color(0xFFE53E3E)
        ),
        PlanItem(
            title = "Tennis Training",
            time = "2025-05-01",
            tag = "Sports",
            tagColor = Color(0xFF805AD5)
        )
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar()
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background - Using shared component
            FancyGradientBackground {
                Box(modifier = Modifier.fillMaxSize())
            }

            // Content
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
                        // Notification icon
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
                }

                // White rounded box containing search bar and content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(top = 20.dp)
                    ) {
                        // Search Bar - Using shared SearchBarStub component
                        item {
                            SearchBarStub()
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Days of week selector - Using shared WeekChipsRow component
                        item {
                            WeekChipsRow(
                                selected = selectedDay,
                                onSelect = { selectedDay = it }
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

                        items(todaysPlans) { plan ->
                            PlanCard(plan = plan)
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

                        items(tomorrowsPlans) { plan ->
                            PlanCard(plan = plan)
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

// Category Chip Component the all work and those tags
@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF805AD5),
        modifier = Modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = dmSansFamily
        )
    }
}

// PREVIEW FUNCTION - This allows you to see the screen without running the app
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MyTasksScreenPreview() {
    MyTasksScreen()
}