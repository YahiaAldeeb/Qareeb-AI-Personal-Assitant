package com.example.qareeb.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SearchBarStub() {
    OutlinedTextField(

        value = "",
        onValueChange = {},
        placeholder = { Text("Search for Plans", color = Color.Gray) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),

        shape = RoundedCornerShape(12.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,

        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFD6D6D6),     // 👈 outline when not focused
            focusedBorderColor = Color(0xFFD6D6D6),
            unfocusedContainerColor = Color(0xFFFAF5FF),// 👈 outline when not focused
            focusedContainerColor = Color(0xFFFAF5FF),// 👈 outline when focused
            cursorColor = Color(0xFFD6D6D6),
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}