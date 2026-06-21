package com.example.qareeb.security

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.qareeb.NetworkModule
import com.example.qareeb.TextMessageRequest
import com.example.qareeb.data.remote.LoginRequest
import com.example.qareeb.data.remote.RetrofitInstance
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.utilis.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceAuthFallbackActivity : FragmentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager.getInstance(this)

        val commandText = intent.getStringExtra("command_text") ?: ""
        val userId = intent.getStringExtra("userID") ?: ""

        if (commandText.isEmpty() || userId.isEmpty()) {
            finish()
            return
        }

        // Check if biometrics can be used
        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt(commandText, userId)
        } else {
            showPasswordFallbackScreen(commandText, userId)
        }
    }

    private fun showBiometricPrompt(commandText: String, userId: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(this@VoiceAuthFallbackActivity, "Identity verified! Executing...", Toast.LENGTH_SHORT).show()
                    executeCommandBypass(commandText, userId)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(this@VoiceAuthFallbackActivity, "Verification error: $errString. Please use password.", Toast.LENGTH_LONG).show()
                        showPasswordFallbackScreen(commandText, userId)
                    } else {
                        finish()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirm Voice Command")
            .setSubtitle("Verify identity to run: \"$commandText\"")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showPasswordFallbackScreen(commandText: String, userId: String) {
        setContent {
            QareebTheme {
                var password by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf("") }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2B55))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Security Verification",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Text(
                                text = "Verify your account password to execute command:\n\"$commandText\"",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password", color = Color.White.copy(alpha = 0.6f)) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF14B8A6),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (errorMessage.isNotEmpty()) {
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFFC8181),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { finish() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        if (password.isBlank()) {
                                            errorMessage = "Password cannot be blank"
                                            return@Button
                                        }
                                        isLoading = true
                                        errorMessage = ""

                                        lifecycleScope.launch {
                                            val email = sessionManager.getUserEmail() ?: ""
                                            val verified = withContext(Dispatchers.IO) {
                                                try {
                                                    val res = RetrofitInstance.api.login(
                                                        LoginRequest(email = email, password = password)
                                                    )
                                                    res.userID.isNotEmpty()
                                                } catch (e: Exception) {
                                                    false
                                                }
                                            }

                                            if (verified) {
                                                Toast.makeText(this@VoiceAuthFallbackActivity, "Identity verified!", Toast.LENGTH_SHORT).show()
                                                executeCommandBypass(commandText, userId)
                                            } else {
                                                isLoading = false
                                                errorMessage = "Incorrect password"
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Text("Verify")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun executeCommandBypass(commandText: String, userId: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    NetworkModule.api.sendTextMessage(
                        TextMessageRequest(text = commandText, userID = userId)
                    )
                }

                if (response.status == "success" || response.status == "accepted") {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VoiceAuthFallbackActivity, "Command executed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    // Trigger DB Sync in background
                    withContext(Dispatchers.IO) {
                        try {
                            val db = com.example.qareeb.data.AppDatabase.getDatabase(this@VoiceAuthFallbackActivity)
                            val syncRepository = com.example.qareeb.data.remote.SyncRepository(
                                taskDao = db.taskDao(),
                                userDao = db.userDao(),
                                transactionDao = db.transactionDao(),
                                promptDao = db.promptDao(),
                                memoryDao = db.memoryDao(),
                                api = RetrofitInstance.syncApi,
                                prefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
                            )
                            syncRepository.sync(userId)
                        } catch (e: Exception) {
                            Log.e("VoiceAuthFallback", "Sync failed: ${e.message}")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@VoiceAuthFallbackActivity, "Execution failed: ${response.status}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VoiceAuthFallbackActivity, "Error executing: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }
}
