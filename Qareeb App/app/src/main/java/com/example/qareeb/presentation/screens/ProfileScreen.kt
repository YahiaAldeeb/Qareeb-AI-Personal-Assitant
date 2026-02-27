package com.example.qareeb.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.AppBackground
import com.example.qareeb.presentation.viewModels.UserViewModel

@Composable
fun ProfileScreen(
    userViewModel: UserViewModel,
    onLogout: () -> Unit
) {
    val username = userViewModel.username
    val email = userViewModel.email ?: "Not set"
    val userId = userViewModel.userId ?: "Unknown"

    Scaffold(containerColor = Color.Transparent) { padding ->
        AppBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding()
                    )
            ) {
                // Header area similar to Dashboard welcome
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = dmSansFamily
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage your Qareeb account",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = dmSansFamily
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // White sheet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFFECECEC),
                            shape = RoundedCornerShape(topStart = 80.dp, topEnd = 80.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Profile card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF6B46C1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.mingcute_notification_fill),
                                            contentDescription = "Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = username,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D3748),
                                            fontFamily = dmSansFamily
                                        )
                                        Text(
                                            text = email,
                                            fontSize = 14.sp,
                                            color = Color(0xFF718096),
                                            fontFamily = dmSansFamily
                                        )
                                    }
                                }

                                // User ID row
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "User ID",
                                        fontSize = 12.sp,
                                        color = Color(0xFFA0AEC0),
                                        fontFamily = dmSansFamily
                                    )
                                    Text(
                                        text = userId,
                                        fontSize = 14.sp,
                                        color = Color(0xFF4A5568),
                                        fontFamily = dmSansFamily
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Logout button
                        Button(
                            onClick = {
                                userViewModel.logout()
                                onLogout()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE53E3E),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Log out",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = dmSansFamily
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

