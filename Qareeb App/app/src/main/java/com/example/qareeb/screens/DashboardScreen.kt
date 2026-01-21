package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.ui.theme.QareebTheme
import com.example.qareeb.R


// -------------------------
// Models
// -------------------------
data class PlanItem(
    val title: String,
    val time: String,
    val tag: String,
    val tagColor: Color
)

data class ExpenseItem(
    val title: String,
    val subtitle: String,
    val status: String,
    val amount: String,
    val statusColor: Color
)

// -------------------------
// Screen
// -------------------------
@Composable
fun DashboardScreen(
    username: String = "Manar",
    todayLabel: String = "08 June 2025",
    tasksCount: Int = 8
) {
    val plans = remember {
        listOf(
            PlanItem("Meeting at work", "10:00–6:00", "In Progress", Color(0xFF7C3AED)),
            PlanItem("Dinner with Ahmed", "20:45–0:01", "Completed", Color(0xFF16A34A)),
            PlanItem("Tennis Training", "20:45–0:01", "Postponed", Color(0xFFF97316)),
        )
    }

    val expenses = remember {
        listOf(
            ExpenseItem("Expert Consultation", "12/01/2024", "Completed", "$150.00", Color(0xFF16A34A)),
            ExpenseItem("Office Supplies", "12/01/2024", "Declined", "$45.00", Color(0xFFEF4444)),
            ExpenseItem("Website Redesign", "12/01/2024", "In Progress", "$2,500.00", Color(0xFF7C3AED)),
        )
    }

    var selectedDay by remember { mutableStateOf("Mon") }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO */ },
                shape = CircleShape,
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White
            ) { Icon(Icons.Default.Add, contentDescription = "Add") }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->

        FancyGradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Header (on gradient)
                WelcomeBanner(username = username)

                Spacer(Modifier.height(20.dp))
                // White background sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFECECEC),
                            shape = RoundedCornerShape(topStart = 80.dp, topEnd = 80.dp)
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item {
                            // ✅ BIG banner: date + AI-Report + tasks in ONE card
                            BigTasksBanner(
                                tasksCount = tasksCount,
                                todayLabel = todayLabel
                            )

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MiniCardPriority(modifier = Modifier.weight(1f))
                                MiniCardCompleted(modifier = Modifier.weight(1f))
                            }

                            Spacer(Modifier.height(18.dp))
                            SearchBarStub()
                            Spacer(Modifier.height(14.dp))

                            WeekChipsRow(
                                selected = selectedDay,
                                onSelect = { selectedDay = it }
                            )

                            Spacer(Modifier.height(18.dp))
                            SectionTitle(title = "Today's Plans", actionText = "View All")
                            Spacer(Modifier.height(10.dp))
                        }

                        items(plans) { plan ->
                            PlanCard(plan = plan)
                            Spacer(Modifier.height(8.dp))
                        }

                        item {
                            Spacer(Modifier.height(18.dp))
                            SectionTitle(title = "Recent Expenses", actionText = "View All")
                            Spacer(Modifier.height(10.dp))
                        }

                        items(expenses) { ex ->
                            ExpenseRow(item = ex)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------
// Components
// -------------------------



@Composable
fun ExpenseRow(item: ExpenseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(item.subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(item.amount, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(item.status, color = item.statusColor, fontSize = 11.sp)
        }
    }
}

@Composable
fun BottomNavBar() {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            label = { Text("Plans") }
        )

        Spacer(Modifier.width(48.dp)) // Space for FAB

        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
            label = { Text("Wallet") }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") }
        )
    }
}

@Composable
private fun SectionTitle(title: String, actionText: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        TextButton(onClick = {}) { Text(actionText, color = Color(0xFF7C3AED)) }
    }
}

/**
 * ✅ ONE big card:
 * - date pill
 * - AI-Report pill
 * - "Today's AI Analysis"
 * - "You have X tasks..."
 */
@Composable
fun BigTasksBanner(
    tasksCount: Int,
    todayLabel: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = shape
            )
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            cornerRadius = 18
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Text + pills content (leave space on the right for the image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 90.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pill(text = todayLabel, leading = "📅")
                        Spacer(Modifier.weight(6f))
                        Pill(text = "AI-Report", trailing = "🫐")
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Today's AI Analysis",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        //fontFamily = dmSansFamily
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "You Have $tasksCount Tasks For\nToday.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        //fontFamily = dmSansFamily,
                        lineHeight = 26.sp
                    )
                }

                // Camera image under the AI-Report pill (bottom-right)
                Image(
                    painter = painterResource(id = R.drawable.cameragroup),
                    contentDescription = "Camera Illustration",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(140.dp)     // change this if you want bigger/smaller
                        .offset(x=25.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun MiniCardPriority(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp)
    ) {
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            cornerRadius = 18
        ){
            Column(Modifier.padding(14.dp)) {
                Text("Priority Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                PriorityItem("Meeting", true)
                PriorityItem("Email", false)
            }
        }
    }
}

@Composable
private fun PriorityItem(label: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (checked) Color(0xFF7C3AED) else Color.LightGray,
                    CircleShape
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
private fun MiniCardCompleted(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(16.dp)
    ) {
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            cornerRadius = 18
        ){
            Column(Modifier.padding(14.dp)) {
                Text("Completed Tasks", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("11 Tasks Done", color = Color.White, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))

            }
        }

    }
}





// -------------------------
// Preview
// -------------------------
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardPreview() {
    QareebTheme {
        DashboardScreen()
    }
}
