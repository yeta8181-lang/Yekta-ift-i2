package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.CupidGold
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.RosePrimary

@Composable
fun SafeMatchCelebrationDialog(
    matchedUser: UserProfileEntity,
    currentUser: UserProfileEntity?,
    onStartChat: () -> Unit,
    onKeepBrowsing: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartPulse"
    )

    Dialog(
        onDismissRequest = onKeepBrowsing,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xEE1E0B36),
                            Color(0xFF0F071B)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RosePrimary.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent)
                ) {
                    Text(
                        text = "✨ GÜVENLİ KARŞILIKLI EŞLEŞME ✨",
                        color = CupidGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Kalp Kalbe Karşı!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Sen ve ${matchedUser.name} birbirinizi beğendiniz.",
                    fontSize = 15.sp,
                    color = Color(0xFFE2E8F0),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Overlapping Avatars
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(150.dp)
                ) {
                    // Left avatar (current user)
                    val myPhoto = currentUser?.photoList?.firstOrNull() ?: ""
                    AsyncImage(
                        model = myPhoto,
                        contentDescription = "Benim Profilim",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = (-46).dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, RoseAccent, CircleShape)
                    )

                    // Right avatar (match user)
                    val matchPhoto = matchedUser.photoList.firstOrNull() ?: ""
                    AsyncImage(
                        model = matchPhoto,
                        contentDescription = matchedUser.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .offset(x = 46.dp)
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, CupidGold, CircleShape)
                    )

                    // Center Pulsing Heart
                    Surface(
                        shape = CircleShape,
                        color = RoseAccent,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .scale(heartScale)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Eşleşme Kalbi",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Actions
                val isVip = currentUser?.isVip == true
                Button(
                    onClick = onStartChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("match_start_chat_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVip) RoseAccent else CupidGold
                    ),
                    shape = RoundedCornerShape(27.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = if (isVip) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (isVip) {
                            "${matchedUser.name} ile Sohbete Başla"
                        } else {
                            "${matchedUser.name} ile Sohbet Et (VIP 👑)"
                        },
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVip) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onKeepBrowsing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("match_keep_browsing_button"),
                    shape = RoundedCornerShape(25.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64748B))
                ) {
                    Text(
                        text = "Keşfetmeye Devam Et",
                        fontSize = 15.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}
