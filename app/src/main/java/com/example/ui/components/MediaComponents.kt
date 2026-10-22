package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VerifiedBlue

@Composable
fun AvatarWithBadge(
    imageUrl: String,
    size: Dp = 56.dp,
    isVerified: Boolean = false,
    isOnline: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profil Resmi",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(2.dp, RoseAccent.copy(alpha = 0.6f), CircleShape)
            )
        } else {
            Surface(
                shape = CircleShape,
                color = Color(0xFF281938),
                border = androidx.compose.foundation.BorderStroke(2.dp, RoseAccent.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Boş Profil Resmi",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(size * 0.58f)
                    )
                }
            }
        }

        // Online green dot
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(size * 0.26f)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, Color(0xFF1E112A), CircleShape)
            )
        }

        // Verified blue badge
        if (isVerified) {
            Box(
                modifier = Modifier
                    .size(size * 0.32f)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, VerifiedBlue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Doğrulanmış Profil",
                    tint = VerifiedBlue,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun InterestChip(
    text: String,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) RoseAccent else NightCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) RoseAccent else NightCardBorder
        ),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MediaViewerDialog(
    mediaUri: String,
    mediaType: String, // "IMAGE" or "VIDEO"
    title: String = "",
    onDismiss: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .testTag("media_viewer_close")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Media Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )

                if (mediaType == "VIDEO") {
                    Surface(
                        shape = CircleShape,
                        color = if (isPlaying) RoseAccent else Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(72.dp)
                            .clickable { isPlaying = !isPlaying }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Durdur" else "Oynat",
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            if (title.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(20.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}
