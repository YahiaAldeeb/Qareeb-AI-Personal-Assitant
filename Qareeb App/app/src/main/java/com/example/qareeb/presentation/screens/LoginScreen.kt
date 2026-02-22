package com.example.qareeb.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.theme.interFamily
import com.example.qareeb.presentation.ui.components.FullBackground
import com.example.qareeb.presentation.viewModels.LoginState
import com.example.qareeb.presentation.viewModels.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // ── Navigate on Success ──
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    FullBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(60.dp))

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
                    modifier = Modifier.size(150.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Welcome Text ──
            Text(
                text = "Welcome Back! 👋",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = interFamily,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Log in to your AI workspace.",
                fontSize = 16.sp,
                fontFamily = interFamily,
                fontWeight = FontWeight.Light,
                color = Color(0xFFE9D8FD)
            )

            Spacer(Modifier.height(32.dp))

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
                Column {

                    // ── Email Field ──
                    Text(
                        text = "Email",
                        fontSize = 14.sp,
                        fontFamily = interFamily,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = {
                            Text(
                                "m@example.com",
                                color = Color.Gray,
                                fontFamily = interFamily
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Password Field ──
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        fontFamily = interFamily,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = {
                            Text(
                                "••••••••",
                                color = Color.Gray,
                                fontFamily = interFamily
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (passwordVisible)
                                            R.drawable.eyeoff
                                        else
                                            R.drawable.eyeon
                                    ),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    // ── Error Message ──
                    if (loginState is LoginState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = (loginState as LoginState.Error).message,
                            color = Color(0xFFFC8181),
                            fontSize = 12.sp,
                            fontFamily = interFamily,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // ── Forgot Password ──
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onForgotPasswordClick,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Text(
                                text = "Forgot your password?",
                                fontSize = 12.sp,
                                fontFamily = interFamily,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Login Button ──
                    Button(
                        onClick = { viewModel.login(email, password) }, // ← calls ViewModel
                        enabled = loginState !is LoginState.Loading,    // ← disabled while loading
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED),
                            disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.5f)
                        )
                    ) {
                        if (loginState is LoginState.Loading) {
                            CircularProgressIndicator(  // ← shows spinner while loading
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Login →",
                                fontSize = 16.sp,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

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
                            text = "  Or  ",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = interFamily
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Register ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't Have an account? ",
                            fontSize = 13.sp,
                            fontFamily = interFamily,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        TextButton(onClick = onRegisterClick) {
                            Text(
                                text = "Join Us Now!",
                                fontSize = 13.sp,
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginPreview() {
    QareebTheme {
        // preview can't use real ViewModel so pass fake lambdas
        LoginScreen(
            viewModel = TODO("use fake ViewModel for preview"),
            onLoginSuccess = {},
            onForgotPasswordClick = {},
            onRegisterClick = {}
        )
    }
}