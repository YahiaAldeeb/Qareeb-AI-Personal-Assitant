package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.qareeb.R
import com.example.qareeb.presentation.navigation.Routes

@Composable
fun BottomNavBar(navController: NavHostController) {

    // ✅ Know which route is currently open (to highlight selected tab)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            // ✅ Avoid building a huge backstack when switching tabs
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            containerColor = Color.White,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = currentRoute == Routes.DASHBOARD,
                onClick = { navigateTo(Routes.DASHBOARD) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                label = { Text("Home") }
            )

            NavigationBarItem(
                selected = currentRoute == Routes.TASKS,
                onClick = { navigateTo(Routes.TASKS) },
                icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = "Tasks") },
                label = { Text("Tasks") }
            )

            // Empty slot in the middle so spacing stays correct
            Spacer(modifier = Modifier.weight(1f))

            NavigationBarItem(
                selected = currentRoute == Routes.FINANCE,
                onClick = { navigateTo(Routes.FINANCE) },
                icon = { Icon(Icons.Default.AccessTime, contentDescription = "History") },
                label = { Text("History") }
            )

            NavigationBarItem(
                selected = currentRoute == "profile", // change when you add profile route
                onClick = { /* TODO: add profile route later */ },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") }
            )
        }

        // ✅ Center logo navigates to Chatbot
        FloatingActionButton(
            onClick = { navigateTo(Routes.CHATBOT) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-23).dp)
                .zIndex(10f),
            containerColor = Color(0xFF582FFF),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Image(
                painter = painterResource(id = R.drawable.qareeb),
                contentDescription = "ChatBot",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
