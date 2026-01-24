package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.qareeb.R

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
