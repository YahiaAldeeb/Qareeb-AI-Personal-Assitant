package com.example.qareeb.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.presentation.theme.QareebTheme
import com.example.qareeb.presentation.theme.interFamily
import com.example.qareeb.presentation.ui.components.FullBackground
import com.example.qareeb.presentation.viewModels.SignUpState
import com.example.qareeb.presentation.viewModels.SignUpViewModel

// ── Real Screen with ViewModel ──
@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel,
    onSignUpSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val signUpState by viewModel.signUpState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(signUpState) {
        if (signUpState is SignUpState.Success) onSignUpSuccess()
    }

    SignUpContent(
        fullName = fullName,
        onFullNameChange = { fullName = it },
        email = email,
        onEmailChange = { email = it },
        phoneNumber = phoneNumber,
        onPhoneNumberChange = { phoneNumber = it },
        password = password,
        onPasswordChange = { password = it },
        confirmPassword = confirmPassword,
        onConfirmPasswordChange = { confirmPassword = it },
        passwordVisible = passwordVisible,
        onPasswordVisibleChange = { passwordVisible = !passwordVisible },
        confirmPasswordVisible = confirmPasswordVisible,
        onConfirmPasswordVisibleChange = { confirmPasswordVisible = !confirmPasswordVisible },
        errorMessage = (signUpState as? SignUpState.Error)?.message,
        isLoading = signUpState is SignUpState.Loading,
        onNextClick = {
            viewModel.signUp(fullName, email, phoneNumber, password, confirmPassword)
        },
        onLoginClick = onLoginClick
    )
}

// ── Pure UI Composable ──
@Composable
fun SignUpContent(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    onNextClick: () -> Unit,
    onLoginClick: () -> Unit
) {
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
                    modifier = Modifier.size(135.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.qareeb),
                    contentDescription = "Qareeb Logo",
                    modifier = Modifier.size(130.dp)
                )
            }

            Spacer(Modifier.height(0.dp))

            Text(
                text = "Secure Your Assistant",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = interFamily,
                color = Color.White
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "Register Your Data For a personalized Formula ⚡",
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
                Column {

                    // ── Full Name ──
                    Text("Full Name", fontSize = 14.sp, fontFamily = interFamily, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = onFullNameChange,
                        placeholder = { Text("Your Name", color = Color.Gray, fontFamily = interFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Email ──
                    Text("Email", fontSize = 14.sp, fontFamily = interFamily, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = { Text("m@gmail.com", color = Color.Gray, fontFamily = interFamily) },
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

                    // ── Phone Number ──
                    Text("Phone Number", fontSize = 14.sp, fontFamily = interFamily, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneNumberChange,
                        placeholder = { Text("example:01212324521", color = Color.Gray, fontFamily = interFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Password ──
                    Text("Password", fontSize = 14.sp, fontFamily = interFamily, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = { Text("••••••••", color = Color.Gray, fontFamily = interFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = onPasswordVisibleChange) {
                                Icon(
                                    painter = painterResource(id = if (passwordVisible) R.drawable.eyeoff else R.drawable.eyeon),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Confirm Password ──
                    Text("Confirm Password", fontSize = 14.sp, fontFamily = interFamily, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        placeholder = { Text("••••••••", color = Color.Gray, fontFamily = interFamily) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmPassword.isNotEmpty() && confirmPassword != password) Color.Red else Color(0xFF6B46C1),
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = onConfirmPasswordVisibleChange) {
                                Icon(
                                    painter = painterResource(id = if (confirmPasswordVisible) R.drawable.eyeoff else R.drawable.eyeon),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )

                    // ── Password Mismatch ──
                    if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                        Spacer(Modifier.height(4.dp))
                        Text("Passwords do not match", color = Color(0xFFFC8181), fontSize = 12.sp, fontFamily = interFamily)
                    }

                    // ── API Error ──
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = Color(0xFFFC8181), fontSize = 12.sp, fontFamily = interFamily)
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Next Button ──
                    Button(
                        onClick = onNextClick,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED),
                            disabledContainerColor = Color(0xFF7C3AED).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Next →", fontSize = 16.sp, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Or Divider ──
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
                        Text("  Or  ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontFamily = interFamily)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Login Link ──
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Do you have an account? ", fontSize = 13.sp, fontFamily = interFamily, color = Color.White.copy(alpha = 0.7f))
                        TextButton(onClick = onLoginClick) {
                            Text("Log In Now!", fontSize = 13.sp, fontFamily = interFamily, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Preview uses SignUpContent directly ──
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpPreview() {
    QareebTheme {
        SignUpContent(
            fullName = "",
            onFullNameChange = {},
            email = "",
            onEmailChange = {},
            phoneNumber = "",
            onPhoneNumberChange = {},
            password = "",
            onPasswordChange = {},
            confirmPassword = "",
            onConfirmPasswordChange = {},
            passwordVisible = false,
            onPasswordVisibleChange = {},
            confirmPasswordVisible = false,
            onConfirmPasswordVisibleChange = {},
            errorMessage = null,
            isLoading = false,
            onNextClick = {},
            onLoginClick = {}
        )
    }
}