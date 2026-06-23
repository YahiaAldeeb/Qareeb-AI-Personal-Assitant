package com.example.qareeb.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.theme.dmSansFamily
import com.example.qareeb.presentation.ui.components.FullBackground
import com.example.qareeb.security.AppLockManager
import com.example.qareeb.security.BiometricAuthHelper
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    activity: androidx.appcompat.app.AppCompatActivity,
    onSplashFinished: () -> Unit = {}
) {
    var showNoCredentialsWarning by remember { mutableStateOf(false) }
    var authTriggered by remember { mutableStateOf(false) } // ✅ prevent double trigger

    LaunchedEffect(Unit) {
        if (!authTriggered) {
            authTriggered = true  // ✅ mark as triggered before calling
            BiometricAuthHelper.authenticate(
                activity = activity,
                onSuccess = {
                    AppLockManager.unlock()
                    onSplashFinished()
                },
                onFailure = {},
                onNoCredentialsSet = { showNoCredentialsWarning = true }
            )
        }
    }

    SplashContent(
        showNoCredentialsWarning = showNoCredentialsWarning,
        onRetry = {
            BiometricAuthHelper.authenticate(
                activity = activity,
                onSuccess = {
                    AppLockManager.unlock()
                    onSplashFinished()
                },
                onFailure = {},
                onNoCredentialsSet = { showNoCredentialsWarning = true }
            )
        },
        onContinueAnyway = {
            AppLockManager.unlock()
            onSplashFinished()
        }
    )
}

// ── Separated content so preview works ──
@Composable
fun SplashContent(
    showNoCredentialsWarning: Boolean = false,
    onRetry: () -> Unit = {},
    onContinueAnyway: () -> Unit = {}
) {
    FullBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ellipseblue),
                contentDescription = "Ellipse",
                modifier = Modifier.size(400.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.qareeb),
                contentDescription = "Qareeb Logo",
                modifier = Modifier.size(250.dp)
            )

            // ── Slogan below logo ──
            Text(
                text = "Always close, always ready.",
                fontSize = 18.sp,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 300.dp)
            )

            // ── Security warning / retry UI ──
            if (showNoCredentialsWarning) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 420.dp)
                ) {
                    Text(
                        text = "No PIN, pattern, or biometric set up on this device.\nWe recommend securing your device.",
                        fontSize = 12.sp,
                        fontFamily = dmSansFamily,
                        color = Color(0xFFFC8181),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onContinueAnyway,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                    ) {
                        Text("Continue Anyway")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    QareebTheme {
        SplashContent()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenNoCredentialsPreview() {
    QareebTheme {
        SplashContent(showNoCredentialsWarning = true)
    }
}