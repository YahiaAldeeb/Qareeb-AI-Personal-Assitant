package com.example.qareeb.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.ui.theme.QareebTheme
import com.example.qareeb.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.qareeb.utilis.toLocalDate
import androidx.compose.ui.zIndex



// -------------------------
// Models
// -------------------------
data class PlanItem(
    val title: String,
    val taskId: Long,
    val time: String,
    val tag: PlanStatus,
    val dueDate: Long = System.currentTimeMillis()
)

data class ExpenseItem(
    val title: String,
    val status: TransactionStatus,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val source: String? = null,
    val description: String? = null,
    val income: Boolean
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
            PlanItem(
                title = "Meeting at work",
                time = "10:00 – 6:00",
                tag = PlanStatus.IN_PROGRESS,
                dueDate = System.currentTimeMillis(),
                taskId = 1

            ),
            PlanItem(
                title = "Dinner with Ahmed",
                time = "20:45 – 00:01",
                tag = PlanStatus.COMPLETED,
                dueDate = System.currentTimeMillis() + 86_400_000,
                taskId = 2
            ),
            PlanItem(
                title = "Tennis Training",
                time = "20:45 – 00:01",
                tag = PlanStatus.POSTPONED,
                dueDate = System.currentTimeMillis() + 2 * 86_400_000,
                taskId = 3
            )
        )
    }



    val expenses = remember {
        listOf(
            ExpenseItem(
                title = "Expert Consultation",
                status = TransactionStatus.COMPLETED,
                amount = 150.00,
                date = 1704067200000L,// 01 Jan 2024 (example timestamp),
                income=false
            ),
            ExpenseItem(
                title = "Office Supplies",
                status = TransactionStatus.DECLINED,

                amount = 45.00,
                date = 1704067200000L,
                income=true
            ),
            ExpenseItem(
                title = "Website Redesign",
                status = TransactionStatus.IN_PROGRESS,

                amount = 2500.00,
                date = 1704067200000L,
                income = true
            )
        )
    }



    var selectedDate by remember { mutableStateOf(LocalDate.now()) }


    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavBar() },
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
                                selectedDate = selectedDate,
                                onSelect = { selectedDate = it }
                            )

                            Spacer(Modifier.height(24.dp))
                        }

                        // ================= TODAY'S PLANS (WHITE CARD) =================
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(
                                        title = selectedDate.dayOfWeek.toString() + " PLANS",
                                    )

                                    Spacer(Modifier.height(15.dp))

                                    plans.forEach { plan ->
                                        if (plan.dueDate.toLocalDate() == selectedDate) {
                                            PlanCard(plan = plan)
                                            Spacer(Modifier.height(8.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(15.dp))
                                    Box(Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .background(Color.White, shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, color = Color(0xFFECECEC))
                                    ){
                                        TextButton(onClick = {}) { Text("View All Plans", color = Color.Black, fontWeight = FontWeight.Bold,textAlign= TextAlign.Center) }
                                    }
                                }
                            }
                        }

                        // ================= TODAY'S EXPENSES (WHITE CARD) =================
                        item {
                            Spacer(Modifier.height(20.dp))

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(vertical = 16.dp)
                            ) {
                                Column {
                                    SectionTitle(
                                        title = "Recent Expenses",
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    expenses.forEach { ex ->
                                        ExpenseRow(item = ex)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    TextButton(onClick = {}) { Text("View All Expenses", color = Color.Black, fontWeight = FontWeight.Bold,textAlign= TextAlign.Center) }
                                }
                            }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEFT: title + date
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                fontFamily = interFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatDate(item.date),
                fontFamily = interFamily,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Light
            )
        }

        // RIGHT: status + amount
        Row(
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                border = BorderStroke(1.dp, item.status.color),
                color = item.status.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = item.status.name.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = item.status.color,
                    fontSize = 12.sp,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${item.amount}$",
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (item.income) Color(0xFF00A63E) else Color(0xFFA62700)
            )
        }
    }
}


@Composable
fun BottomNavBar(
    onCenterClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // The actual Navigation Bar
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),               // a bit taller like your screenshot
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
                icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                label = { Text("Tasks") }
            )

            // Empty slot in the middle so spacing stays correct
            Spacer(modifier = Modifier.weight(1f))

            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                label = { Text("History") }
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text("Profile") }
            )
        }

        // Center purple circle ABOVE the bar (won't be clipped)
        FloatingActionButton(
            onClick = onCenterClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-23).dp)  // lift it up
                .zIndex(10f),          // force it in front
            containerColor = Color(0xFF582FFF),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Image(painter = painterResource(id = R.drawable.qareeb),contentDescription = "Add",Modifier.size(30.dp))
        }
    }
}



@Composable
private fun SectionTitle(title: String) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        fontFamily = dmSansFamily
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "You Have $tasksCount Tasks For\nToday.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = dmSansFamily,
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
                        .offset(x = 25.dp),
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
