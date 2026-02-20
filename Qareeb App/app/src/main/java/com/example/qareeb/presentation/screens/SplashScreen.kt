package com.example.qareeb.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit = {}) {

    LaunchedEffect(Unit) {
        delay(2000L)
        onSplashFinished()
    }

    SplashContent()
}

// ── Separated content so preview works ──
@Composable
fun SplashContent() {
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
            Spacer(modifier = Modifier.size(30.dp))
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
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    QareebTheme {
        SplashContent()  // ← no LaunchedEffect, no navigation, safe to preview
    }
}