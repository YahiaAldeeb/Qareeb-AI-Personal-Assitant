package com.example.qareeb.presentation.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.qareeb.R

@Composable
fun FullBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Diagonal gradient from top-left to bottom-right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF6B46C1),  // purple at 0%
                            0.6f to Color(0xFF7B5CC1),  // purple at 60%
                            0.85f to Color(0xFF4A7A7B), // transition at 85%
                            1.0f to Color(0xFF2C7A7B)   // teal only at the very end
                        ),
                        start = Offset(0f, 0f),           // top-left
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // bottom-right
                    )
                )
        )

        // 2. Stars covering the full screen
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds  // ← stretches to fill entire screen
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Image(
            painter = painterResource(id = R.drawable.stars),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // 3. Content on top
        content()
    }
}