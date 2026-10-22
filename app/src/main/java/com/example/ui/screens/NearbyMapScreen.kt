package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfileEntity
import com.example.location.GoogleLocationTracker
import com.example.ui.components.AvatarWithBadge
import com.example.ui.components.InterestChip
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SafeShield
import com.example.ui.theme.VerifiedBlue
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun NearbyMapScreen(
    profiles: List<UserProfileEntity>,
    currentUser: UserProfileEntity?,
    distanceFilterKm: Float,
    selectedUser: UserProfileEntity?,
    isVip: Boolean = false,
    isLocationTrackingActive: Boolean = false,
    onDistanceFilterChange: (Float) -> Unit,
    onSelectUser: (UserProfileEntity?) -> Unit,
    onLikeUser: (UserProfileEntity) -> Unit,
    onStartChat: (UserProfileEntity) -> Unit,
    onRequestLocationUpdate: () -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val myLat = currentUser?.latitude ?: 41.0082
    val myLng = currentUser?.longitude ?: 28.9784

    // Compute real-time distance & bearing from current user's GPS coordinates for every profile
    val nearbyProfilesWithGeo = remember(profiles, myLat, myLng, distanceFilterKm) {
        profiles.map { profile ->
            val realDistKm = GoogleLocationTracker.calculateDistanceKm(
                myLat,
                myLng,
                profile.latitude,
                profile.longitude
            )
            val bearingDeg = GoogleLocationTracker.calculateBearingDegrees(
                myLat,
                myLng,
                profile.latitude,
                profile.longitude
            )
            Triple(profile.copy(distanceKm = realDistKm), realDistKm, bearingDeg)
        }.filter { (_, distKm, _) ->
            distKm <= distanceFilterKm
        }.sortedBy { (_, distKm, _) -> distKm }
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadiusFactor by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarWave"
    )
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarBeam"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val radarCenterYOffsetPx = with(density) { (-20).dp.toPx() }
        val maxRadarRadiusPx = (minOf(widthPx, heightPx) * 0.40f).coerceAtLeast(120f)

        // Map / Radar Canvas Background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        onSelectUser(null)
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f + radarCenterYOffsetPx)

            // Stylized Map Grid Lines (Streets & Coordinates)
            val gridStep = 45.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = Color(0x15FFFFFF),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = Color(0x15FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }

            // Crosshair N-S / E-W axes
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x, center.y - maxRadarRadiusPx),
                end = Offset(center.x, center.y + maxRadarRadiusPx),
                strokeWidth = 1f
            )
            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(center.x - maxRadarRadiusPx, center.y),
                end = Offset(center.x + maxRadarRadiusPx, center.y),
                strokeWidth = 1f
            )

            // Radar concentric distance circles
            val rings = listOf(0.3f, 0.6f, 0.85f, 1.0f)
            rings.forEach { ratio ->
                drawCircle(
                    color = Color(0x28E11D48),
                    radius = maxRadarRadiusPx * ratio,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // Expanding Pulse Wave
            drawCircle(
                color = Color(0x40E11D48),
                radius = maxRadarRadiusPx * pulseRadiusFactor,
                center = center,
                style = Stroke(width = 2f)
            )

            // Radar Sweep Beam Line
            val rad = Math.toRadians(radarAngle.toDouble())
            val beamEnd = Offset(
                center.x + (maxRadarRadiusPx * cos(rad)).toFloat(),
                center.y + (maxRadarRadiusPx * sin(rad)).toFloat()
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(RoseAccent.copy(alpha = 0.5f), Color.Transparent),
                    start = center,
                    end = beamEnd
                ),
                start = center,
                end = beamEnd,
                strokeWidth = 3f
            )

            // Center My Location Marker
            drawCircle(
                color = RoseAccent.copy(alpha = 0.25f),
                radius = 18.dp.toPx(),
                center = center
            )
            drawCircle(
                color = RoseAccent,
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = center
            )
        }

        // Overlay Interactive Pins for Nearby Profiles using Real Geographic Bearing & Distance
        Box(modifier = Modifier.fillMaxSize()) {
            nearbyProfilesWithGeo.forEachIndexed { index, (profile, distKm, bearingDeg) ->
                // Convert geographic bearing (0° = North/Up, 90° = East/Right) to screen angle
                // If two users have identical coordinates, offset slightly by index so pins don't overlap
                val effectiveBearing = if (distKm <= 0.1) (index * 60f) % 360f else bearingDeg
                val screenRad = Math.toRadians((effectiveBearing - 90f).toDouble())
                val normalizedRatio = (distKm / distanceFilterKm.coerceAtLeast(1f)).toFloat().coerceIn(0.22f, 0.92f)
                val radiusPx = maxRadarRadiusPx * normalizedRatio

                val xOffsetPx = (radiusPx * cos(screenRad)).roundToInt()
                val yOffsetPx = (radiusPx * sin(screenRad) + radarCenterYOffsetPx).roundToInt()

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(xOffsetPx, yOffsetPx) }
                ) {
                    MapUserPin(
                        profile = profile,
                        isSelected = selectedUser?.id == profile.id,
                        onClick = { onSelectUser(profile) }
                    )
                }
            }
        }

        // Top Filter Bar & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = RoseAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yakındaki Bekarlar",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (nearbyProfilesWithGeo.isEmpty()) {
                            "Canlı arama aktif (${distanceFilterKm.toInt()} km çapında taranıyor...)"
                        } else {
                            "Etrafında ${nearbyProfilesWithGeo.size} aktif üye bulundu"
                        },
                        fontSize = 12.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                // My Location Refresh / Start Tracking Button
                Surface(
                    shape = CircleShape,
                    color = if (isLocationTrackingActive) RoseAccent.copy(alpha = 0.2f) else NightCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLocationTrackingActive) OnlineGreen else NightCardBorder
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onRequestLocationUpdate,
                        modifier = Modifier.testTag("refresh_location_button")
                    ) {
                        Icon(
                            imageVector = if (isLocationTrackingActive) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                            contentDescription = "Konumumu Güncelle",
                            tint = if (isLocationTrackingActive) OnlineGreen else RoseAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Distance Radius Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(1f to "1 km", 5f to "5 km", 15f to "15 km", 50f to "50 km", 100f to "100 km 👑")
                filters.forEach { (km, label) ->
                    FilterChip(
                        selected = (distanceFilterKm == km),
                        onClick = {
                            if (km == 100f && !isVip) {
                                onOpenVipPaywall()
                            } else {
                                onDistanceFilterChange(km)
                            }
                        },
                        label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (km == 100f) CupidGold else RoseAccent,
                            selectedLabelColor = if (km == 100f) Color.Black else Color.White,
                            containerColor = NightCard.copy(alpha = 0.85f),
                            labelColor = if (km == 100f) CupidGold else Color.LightGray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (distanceFilterKm == km) (if (km == 100f) CupidGold else RoseAccent) else NightCardBorder,
                            enabled = true,
                            selected = distanceFilterKm == km
                        )
                    )
                }
            }

            // Google Location API Live Status & Privacy Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isLocationTrackingActive) OnlineGreen.copy(alpha = 0.14f) else SafeShield.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isLocationTrackingActive) OnlineGreen.copy(alpha = 0.45f) else SafeShield.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onRequestLocationUpdate() }
                    .testTag("gps_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (isLocationTrackingActive) Icons.Default.GpsFixed else Icons.Default.LocationSearching,
                            contentDescription = null,
                            tint = if (isLocationTrackingActive) OnlineGreen else SafeShield,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val coordText = String.format(Locale.US, "%.4f, %.4f", myLat, myLng)
                        Text(
                            text = if (isLocationTrackingActive) {
                                "Google Location API Canlı Radar Aktif ($coordText) • Tam adres gizlenir"
                            } else {
                                "Canlı GPS takibini başlatmak için dokun • Yaklaşık mesafe gösterilir"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SafeShield,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Bottom Selected User Mini Profile Sheet
        AnimatedVisibility(
            visible = selectedUser != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
        ) {
            selectedUser?.let { rawUser ->
                val liveDistKm = GoogleLocationTracker.calculateDistanceKm(
                    myLat,
                    myLng,
                    rawUser.latitude,
                    rawUser.longitude
                )
                val user = rawUser.copy(distanceKm = liveDistKm)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = NightCard),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RoseAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nearby_user_preview_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AvatarWithBadge(
                                imageUrl = user.photoList.firstOrNull() ?: "",
                                size = 64.dp,
                                isVerified = user.isVerified,
                                isOnline = user.isOnline
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${user.name}, ${user.age}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (user.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Doğrulanmış",
                                            tint = VerifiedBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${user.occupation} • ${user.distanceKm} km yakında",
                                    fontSize = 12.5.sp,
                                    color = CupidGold,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = user.bio,
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { onSelectUser(null) },
                                modifier = Modifier.testTag("close_preview_card")
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Interests chips
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            user.interestList.take(3).forEach { interest ->
                                InterestChip(text = interest)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Like & Chat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onLikeUser(user) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("map_like_user_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Beğen", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    if (isVip) {
                                        onStartChat(user)
                                    } else {
                                        onOpenVipPaywall()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("map_chat_user_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382952)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Forum, contentDescription = null, tint = CupidGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isVip) "Mesaj At" else "Mesaj At (VIP)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapUserPin(
    profile: UserProfileEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("pin_${profile.id}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(RoseAccent.copy(alpha = 0.4f))
                )
            }

            AsyncImage(
                model = profile.photoList.firstOrNull() ?: "",
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (isSelected) 46.dp else 40.dp)
                    .clip(CircleShape)
                    .border(
                        2.5.dp,
                        if (isSelected) RoseAccent else CupidGold,
                        CircleShape
                    )
            )

            if (profile.isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(OnlineGreen)
                        .border(1.5.dp, Color.Black, CircleShape)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "${profile.name} • ${profile.distanceKm} km",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
