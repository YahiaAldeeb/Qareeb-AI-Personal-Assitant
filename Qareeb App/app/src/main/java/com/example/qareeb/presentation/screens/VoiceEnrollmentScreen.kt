package com.example.qareeb.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.domain.model.UserDomain
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.theme.interFamily
import com.example.qareeb.presentation.ui.components.FullBackground
import com.example.qareeb.presentation.viewModels.VoiceEnrollmentViewModel

@Composable
fun VoiceEnrollmentScreen(
    viewModel: VoiceEnrollmentViewModel,
    user: UserDomain,
    onEnrollmentSuccess: () -> Unit,
    onSkipClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording
    val isLoading by viewModel.isLoading
    val isEnrollmentFinished by viewModel.isEnrollmentFinished
    
    val currentStep by viewModel.currentStep
    val samplesRecorded by viewModel.samplesRecorded
    val statusMessage by viewModel.statusMessage
    val currentPhrase = viewModel.phrases.getOrElse(currentStep) { "" }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startRecording(context)
        else viewModel.setStatus("Microphone permission is required")
    }

    VoiceEnrollmentContent(
        isRecording = isRecording,
        isLoading = isLoading,
        isEnrollmentFinished = isEnrollmentFinished,
        currentStep = currentStep,
        currentPhrase = currentPhrase,
        samplesRecorded = samplesRecorded,
        statusMessage = statusMessage,
        onMicClick = {
            if (isRecording) viewModel.stopRecording(context)
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onResetClick = {
            viewModel.resetRecordings()
        },
        onConfirmClick = {
            viewModel.uploadVoice(context, user, onEnrollmentSuccess)
        },
        onSkipClick = onSkipClick,
        onLoginClick = onLoginClick
    )
}

@Composable
fun VoiceEnrollmentContent(
    isRecording: Boolean,
    isLoading: Boolean,
    isEnrollmentFinished: Boolean,
    currentStep: Int,
    currentPhrase: String,
    samplesRecorded: List<Boolean>,
    statusMessage: String,
    onMicClick: () -> Unit,
    onResetClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onSkipClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val allSamplesRecorded = samplesRecorded.all { it }

    FullBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Logo ──
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.ellipseblue),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.qareeb),
                    contentDescription = "Qareeb Logo",
                    modifier = Modifier.size(120.dp)
                )
            }

            Text(
                text = "Secure your Assistant",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = interFamily,
                color = Color.White
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "Record your voice to secure your assistant ⚡",
                fontSize = 13.sp,
                fontFamily = interFamily,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFE9D8FD)
            )

            Spacer(Modifier.height(10.dp))

            // ── Card ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF2D2B55).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Step progress header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Voice Verification",
                            fontSize = 14.sp,
                            fontFamily = interFamily,
                            color = Color(0xFF14B8A6)
                        )
                        
                        Text(
                            text = "Step ${if (allSamplesRecorded) 3 else (currentStep + 1)} of 3",
                            fontSize = 14.sp,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF14B8A6)
                        )
                    }

                    Spacer(Modifier.height(25.dp))

                    Text(
                        text = "Say the following phrase clearly:",
                        fontSize = 14.sp,
                        fontFamily = interFamily,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Phrase Box ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(
                                color = Color(0xFF1E1B3A),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allSamplesRecorded) "\"All phrases recorded successfully!\"" else currentPhrase,
                            fontSize = 15.sp,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Voice Image ──
                    Image(
                        painter = painterResource(id = R.drawable.voice),
                        contentDescription = "Voice",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentScale = ContentScale.Fit
                    )

                    // ── Mic Button ──
                    VoiceMicButton(
                        isRecording = isRecording,
                        isLoading = isLoading,
                        isSuccess = allSamplesRecorded || isEnrollmentFinished,
                        onClick = onMicClick
                    )

                    Spacer(Modifier.height(5.dp))

                    // ── Status Message ──
                    Text(
                        text = statusMessage,
                        fontSize = 13.sp,
                        fontFamily = interFamily,
                        color = when {
                            isEnrollmentFinished -> Color(0xFF4ADE80)
                            isRecording -> Color(0xFFFC8181)
                            else -> Color.White.copy(alpha = 0.7f)
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // ── Reset Button if recording index > 0 ──
                    if ((currentStep > 0 || allSamplesRecorded) && !isEnrollmentFinished && !isRecording && !isLoading) {
                        TextButton(onClick = onResetClick) {
                            Text(
                                "Reset & Record Again",
                                color = Color(0xFFFC8181),
                                fontSize = 12.sp,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(Modifier.height(36.dp)) // Maintain layout height consistency
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Confirm Button ──
                    Button(
                        onClick = onConfirmClick,
                        enabled = allSamplesRecorded && !isLoading && !isRecording && !isEnrollmentFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8C25F4),
                            disabledContainerColor = Color(0xFFB066FF).copy(alpha = 0.4f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Confirm & Secure →",
                                fontSize = 16.sp,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Skip Button ──
                    if (!isEnrollmentFinished && !isLoading) {
                        TextButton(
                            onClick = onSkipClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Skip for now",
                                fontSize = 14.sp,
                                fontFamily = interFamily,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Or Divider ──
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            "  Or  ",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = interFamily
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Login Link ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Do you have an account? ",
                            fontSize = 13.sp,
                            fontFamily = interFamily,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        TextButton(onClick = onLoginClick) {
                            Text(
                                "Log In Now!",
                                fontSize = 13.sp,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun VoiceMicButton(
    isRecording: Boolean,
    isLoading: Boolean,
    isSuccess: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(110.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .background(
                        color = Color(0xFFFC8181).copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale * 0.9f)
                    .background(
                        color = Color(0xFFFC8181).copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            )
        }

        IconButton(
            onClick = onClick,
            enabled = !isLoading && !isSuccess,
            modifier = Modifier
                .size(70.dp)
                .background(
                    color = when {
                        isSuccess -> Color(0xFF4ADE80)
                        isRecording -> Color(0xFFE53E3E)
                        else -> Color(0xFF7C3AED)
                    },
                    shape = CircleShape
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}