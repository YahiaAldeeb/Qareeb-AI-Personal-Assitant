package com.example.qareeb.presentation.ui.components
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qareeb.R
import com.example.qareeb.screens.CardBackground
import com.example.qareeb.screens.Pill

@Composable
fun BigTasksBanner(
    tasksCount: Int,
    todayLabel: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = shape
            )
            .clip(shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        CardBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            cornerRadius = 18
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Text + pills content (leave space on the right for the image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 90.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pill(text = todayLabel, leading = "📅")
                        Spacer(Modifier.weight(6f))
                        Pill(text = "AI-Report", trailing = "🫐")
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Today's AI Analysis",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 15.sp,
                        //fontFamily = dmSansFamily
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "You Have $tasksCount Tasks For\nToday.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        //fontFamily = dmSansFamily,
                        lineHeight = 26.sp
                    )
                }

                // Camera image under the AI-Report pill (bottom-right)
                Image(
                    painter = painterResource(id = R.drawable.cameragroup),
                    contentDescription = "Camera Illustration",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(140.dp)     // change this if you want bigger/smaller
                        .offset(x = 25.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
