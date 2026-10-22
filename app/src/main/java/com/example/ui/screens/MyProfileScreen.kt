package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.components.InterestChip
import com.example.ui.components.MediaViewerDialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SafeShield
import com.example.ui.theme.VerifiedBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyProfileScreen(
    user: UserProfileEntity?,
    likedMeUsers: List<UserProfileEntity> = emptyList(),
    isAdmin: Boolean = false,
    allProfiles: List<UserProfileEntity> = emptyList(),
    allMoments: List<MomentEntity> = emptyList(),
    onLikeUser: (UserProfileEntity) -> Unit = {},
    onStartChat: (UserProfileEntity) -> Unit = {},
    onAdminDeleteUser: (UserProfileEntity) -> Unit = {},
    onAdminDeleteMoment: (MomentEntity) -> Unit = {},
    onAddPhoto: (String) -> Unit,
    onUpdateProfile: (
        name: String,
        city: String,
        bio: String,
        occupation: String,
        education: String,
        lookingFor: String,
        interests: List<String>,
        lifestyle: List<String>,
        promptQ: String,
        promptA: String
    ) -> Unit,
    onOpenSafetyCenter: () -> Unit,
    onVerifySelfie: () -> Unit,
    onOpenVipPaywall: () -> Unit,
    onSignOut: () -> Unit = {},
    isCloudActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (user == null) {
        Box(modifier = modifier.fillMaxSize().background(NightSurface), contentAlignment = Alignment.Center) {
            Text("Profil yükleniyor...", color = Color.Gray)
        }
        return
    }

    var isEditingProfile by remember { mutableStateOf(false) }
    var selectedPhotoPreview by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onAddPhoto(it.toString()) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Profilim 👤",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                if (user.isVip || isAdmin) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CupidGold
                    ) {
                        Text(
                            text = if (isAdmin) "YÖNETİCİ 👑" else "VIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Safety button
                Surface(
                    shape = CircleShape,
                    color = NightCard,
                    border = BorderStroke(1.dp, NightCardBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    IconButton(onClick = onOpenSafetyCenter, modifier = Modifier.testTag("profile_safety_center")) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Güvenlik", tint = SafeShield)
                    }
                }

                // Edit Profile Button
                Button(
                    onClick = { isEditingProfile = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Düzenle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // VIP Subscription Management Banner
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (user.isVip || isAdmin) Color(0xFF2E2211) else NightCard
            ),
            border = BorderStroke(1.dp, CupidGold),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenVipPaywall() }
                .testTag("vip_banner_card")
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CupidGold.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isAdmin -> "Yönetici Tam Yetki Aktif 👑"
                            user.isVip -> "VIP Aboneliğin Aktif 👑"
                            else -> "VIP Abonelik Yönetimi 👑"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = CupidGold
                    )
                    Text(
                        text = if (user.isVip || isAdmin) {
                            "Sohbet, Keşfet paylaşımı ve 100 km radar açık • Yönetmek için dokun"
                        } else {
                            "Sohbet etmek ve Keşfette paylaşım yapmak için VIP paketi seç"
                        },
                        fontSize = 11.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CupidGold
                ) {
                    Text(
                        text = if (user.isVip || isAdmin) "Yönet" else "Paketler",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Cloud Firestore Sync Status
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isCloudActive) Color(0xFF143324) else NightCard,
            border = BorderStroke(1.dp, if (isCloudActive) Color(0xFF22C55E) else NightCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCloudActive) Color(0xFF22C55E) else Color(0xFFEAB308),
                        modifier = Modifier.size(8.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCloudActive) "Online Bulut Veritabanı: Aktif (Firestore Canlı)" else "Online Bulut Modu: Yerel Önbellek",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCloudActive) Color(0xFF86EFAC) else Color(0xFFCBD5E1)
                    )
                }

                Text(
                    text = if (isCloudActive) "CANLI" else "YEDEK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCloudActive) Color(0xFF22C55E) else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. "Seni Beğenenler" Section (Synced with Firebase /likes)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NightCard),
            border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("liked_me_section_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = RoseAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seni Beğenenler (${likedMeUsers.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Firebase Canlı",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoseAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Seni beğenen üyeler anında burada listelenir. Beğenerek hemen eşleşebilirsin!",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (likedMeUsers.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF191229),
                        border = BorderStroke(1.dp, NightCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RoseAccent.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Henüz seni beğenen olmadı. Başka bir kullanıcı seni beğendiğinde burada görebilirsin!",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(likedMeUsers, key = { it.id }) { liker ->
                            LikedMeProfileCard(
                                liker = liker,
                                onLikeBack = { onLikeUser(liker) },
                                onStartChat = { onStartChat(liker) }
                            )
                        }
                    }
                }
            }
        }

        // Admin Control Panel (Only visible for yeta8181@gmail.com)
        if (isAdmin) {
            Spacer(modifier = Modifier.height(16.dp))
            AdminManagementPanelCard(
                allProfiles = allProfiles,
                allMoments = allMoments,
                onDeleteUser = onAdminDeleteUser,
                onDeleteMoment = onAdminDeleteMoment
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Photos Gallery Carousel with Add Photo button
        Text(
            text = "Fotoğraflarım (${user.photoList.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(user.photoList) { photoUrl ->
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedPhotoPreview = photoUrl }
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profil Fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NightCard,
                    border = BorderStroke(1.5.dp, RoseAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(width = 110.dp, height = 150.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("add_photo_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Fotoğraf Ekle",
                            tint = RoseAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fotoğraf Ekle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Profile Card (Bio, Interests, Lifestyle)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = NightCard),
            border = BorderStroke(1.dp, NightCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Name, Age, Verification Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${user.name}, ${user.age}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (user.isVerified || isAdmin) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Doğrulanmış",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "${user.occupation} • ${user.city}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1)
                )

                if (!user.isVerified && !isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onVerifySelfie,
                        colors = ButtonDefaults.buttonColors(containerColor = VerifiedBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yüz Doğrulaması Yap (Mavi Tik Al)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bio
                Text(
                    text = "Hakkımda",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoseAccent
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = user.bio,
                    fontSize = 13.5.sp,
                    color = Color.White,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Interests
                Text(
                    text = "İlgi Alanlarım",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CupidGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    user.interestList.forEach { interest ->
                        InterestChip(text = interest)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lifestyle
                Text(
                    text = "Yaşam Tarzı",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    user.lifestyleList.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF261C39),
                            modifier = Modifier.padding(3.dp)
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prompt Q&A
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2243)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = user.promptQuestion,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CupidGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${user.promptAnswer}”",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Details (Looking for, Education, Height, Zodiac)
                Text(
                    text = "🎯 Ne Arıyorum: ${user.lookingFor}",
                    fontSize = 12.5.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "🎓 Eğitim: ${user.education}",
                    fontSize = 12.5.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "📏 Boy & Burç: ${user.heightCm} cm • ${user.zodiac}",
                    fontSize = 12.5.sp,
                    color = Color(0xFFCBD5E1)
                )
                if (user.email.isNotBlank()) {
                    Text(
                        text = "✉️ Hesap: ${user.email}",
                        fontSize = 12.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F1D2B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Text(
                        text = "Hesaptan Çıkış Yap",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5),
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }

    // Edit Profile Modal
    if (isEditingProfile) {
        EditProfileDialog(
            user = user,
            onDismiss = { isEditingProfile = false },
            onSave = { name, city, bio, occupation, education, lookingFor, interests, lifestyle, promptQ, promptA ->
                onUpdateProfile(name, city, bio, occupation, education, lookingFor, interests, lifestyle, promptQ, promptA)
                isEditingProfile = false
            }
        )
    }

    // Photo Preview Dialog
    selectedPhotoPreview?.let { uri ->
        MediaViewerDialog(
            mediaUri = uri,
            mediaType = "IMAGE",
            title = user.name,
            onDismiss = { selectedPhotoPreview = null }
        )
    }
}

@Composable
private fun LikedMeProfileCard(
    liker: UserProfileEntity,
    onLikeBack: () -> Unit,
    onStartChat: () -> Unit
) {
    val photoUrl = liker.photoList.firstOrNull().orEmpty()
    val isAlreadyMatched = liker.isMatch || liker.isLikedByMe

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1630),
        border = BorderStroke(
            1.dp,
            if (liker.isSuperLiked) CupidGold else RoseAccent.copy(alpha = 0.55f)
        ),
        modifier = Modifier
            .width(160.dp)
            .testTag("liked_me_card_${liker.id}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2243)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = liker.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = liker.name,
                        tint = Color.LightGray,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${liker.name}, ${liker.age}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = liker.city,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (liker.isSuperLiked) CupidGold.copy(alpha = 0.2f) else RoseAccent.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (liker.isSuperLiked) Icons.Default.Star else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (liker.isSuperLiked) CupidGold else RoseAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (liker.isSuperLiked) "Süper Beğeni" else "Seni Beğendi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (liker.isSuperLiked) CupidGold else RoseAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isAlreadyMatched) {
                Button(
                    onClick = onStartChat,
                    colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("liked_me_chat_${liker.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sohbet",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            } else {
                Button(
                    onClick = onLikeBack,
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("liked_me_like_back_${liker.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sen de Beğen",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminManagementPanelCard(
    allProfiles: List<UserProfileEntity>,
    allMoments: List<MomentEntity>,
    onDeleteUser: (UserProfileEntity) -> Unit,
    onDeleteMoment: (MomentEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF231535)),
        border = BorderStroke(1.5.dp, CupidGold),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_management_panel")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = CupidGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Yönetici Kontrol Paneli (Tam Yetki) 👑",
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CupidGold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "yeta8181@gmail.com yönetici hesabıyla kullanıcıları ve Keşfet gönderilerini silebilirsiniz.",
                fontSize = 11.5.sp,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Kayıtlı Kullanıcılar (${allProfiles.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (allProfiles.isEmpty()) {
                Text(
                    text = "Henüz listelenecek başka kullanıcı bulunmuyor.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allProfiles.take(15).forEach { targetUser ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF191229),
                            border = BorderStroke(1.dp, NightCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${targetUser.name}, ${targetUser.age} • ${targetUser.city}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (targetUser.email.isNotBlank()) {
                                        Text(
                                            text = targetUser.email,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF3F1D2B),
                                    border = BorderStroke(1.dp, RoseAccent),
                                    modifier = Modifier
                                        .clickable { onDeleteUser(targetUser) }
                                        .testTag("admin_panel_delete_user_${targetUser.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Kullanıcıyı Sil",
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Kullanıcıyı Sil",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Keşfet Gönderileri (${allMoments.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (allMoments.isEmpty()) {
                Text(
                    text = "Henüz Keşfet gönderisi bulunmuyor.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allMoments.take(15).forEach { moment ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF191229),
                            border = BorderStroke(1.dp, NightCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = moment.authorName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CupidGold
                                    )
                                    Text(
                                        text = moment.caption.ifBlank { "(Fotoğraf Paylaşımı)" },
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF3F1D2B),
                                    border = BorderStroke(1.dp, RoseAccent),
                                    modifier = Modifier
                                        .clickable { onDeleteMoment(moment) }
                                        .testTag("admin_panel_delete_moment_${moment.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Gönderiyi Sil",
                                            tint = Color(0xFFFCA5A5),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Gönderiyi Sil",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: UserProfileEntity,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        city: String,
        bio: String,
        occupation: String,
        education: String,
        lookingFor: String,
        interests: List<String>,
        lifestyle: List<String>,
        promptQ: String,
        promptA: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var city by remember { mutableStateOf(user.city) }
    var bio by remember { mutableStateOf(user.bio) }
    var occupation by remember { mutableStateOf(user.occupation) }
    var education by remember { mutableStateOf(user.education) }
    var lookingFor by remember { mutableStateOf(user.lookingFor) }
    var promptQ by remember { mutableStateOf(user.promptQuestion) }
    var promptA by remember { mutableStateOf(user.promptAnswer) }

    // Popular interests pool
    val availableInterests = listOf(
        "Kahve ☕", "Fotoğrafçılık 📷", "Müzik 🎵", "Seyahat ✈️",
        "Kamp ⛺", "Doğa 🌲", "Kitaplar 📚", "Dans 💃", "Sinema 🍿",
        "Gastronomi 🍝", "Yoga 🧘", "Koşu 🏃", "Sanat 🎨", "Kediler 🐱", "Köpekler 🐕"
    )
    var selectedInterests by remember { mutableStateOf(user.interestList.toMutableList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profili Düzenle", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim:") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Şehir / Bölge:") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Meslek:") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Eğitim:") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lookingFor,
                    onValueChange = { lookingFor = it },
                    label = { Text("Ne Arıyorum:") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Hakkımda / Detaylı Biyografi:") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("İlgi Alanlarını Seç:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CupidGold)
                Spacer(modifier = Modifier.height(6.dp))

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    availableInterests.forEach { interest ->
                        val isSelected = selectedInterests.contains(interest)
                        InterestChip(
                            text = interest,
                            isSelected = isSelected,
                            onClick = {
                                val list = selectedInterests.toMutableList()
                                if (isSelected) list.remove(interest) else list.add(interest)
                                selectedInterests = list
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = promptQ,
                    onValueChange = { promptQ = it },
                    label = { Text("Soru Başlığı (Prompt):") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = promptA,
                    onValueChange = { promptA = it },
                    label = { Text("Cevabınız:") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, city, bio, occupation, education, lookingFor, selectedInterests, user.lifestyleList, promptQ, promptA)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Kaydet", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = Color.Gray)
            }
        }
    )
}
