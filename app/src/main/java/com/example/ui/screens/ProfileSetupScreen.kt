package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfileEntity
import com.example.ui.components.InterestChip
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    initialName: String,
    initialEmail: String,
    initialAvatarUrl: String,
    initialLatitude: Double = 41.0082,
    initialLongitude: Double = 28.9784,
    onRequestLocationUpdate: () -> Unit = {},
    onCompleteSetup: (UserProfileEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Normal name from Google, editable
    var name by remember { mutableStateOf(initialName) }

    // Photos start completely empty - user must upload their own photo
    var photos by remember {
        mutableStateOf<List<String>>(
            if (initialAvatarUrl.isNotBlank()) listOf(initialAvatarUrl)
            else emptyList()
        )
    }

    // All other fields start EMPTY as requested, with selection options
    var ageText by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var lookingFor by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var education by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var zodiac by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var promptQuestion by remember { mutableStateOf("") }
    var promptAnswer by remember { mutableStateOf("") }
    var selectedInterests by remember { mutableStateOf<List<String>>(emptyList()) }
    var latitudeText by remember(initialLatitude) { mutableStateOf("%.4f".format(java.util.Locale.US, initialLatitude)) }
    var longitudeText by remember(initialLongitude) { mutableStateOf("%.4f".format(java.util.Locale.US, initialLongitude)) }

    val cityCoordinates = remember {
        mapOf(
            "İstanbul" to (41.0082 to 28.9784),
            "Ankara" to (39.9334 to 32.8597),
            "İzmir" to (38.4237 to 27.1428),
            "Bursa" to (40.1885 to 29.0610),
            "Antalya" to (36.8969 to 30.7133),
            "Adana" to (37.0000 to 35.3213),
            "Konya" to (37.8746 to 32.4932),
            "Eskişehir" to (39.7667 to 30.5256),
            "Gaziantep" to (37.0662 to 37.3833),
            "Trabzon" to (41.0027 to 39.7168)
        )
    }

    // Predefined selectable options
    val cityOptions = listOf(
        "İstanbul", "Ankara", "İzmir", "Bursa", "Antalya",
        "Adana", "Konya", "Eskişehir", "Gaziantep", "Trabzon"
    )

    val ageOptions = listOf("20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "32", "35")

    val lookingForOptions = listOf(
        "Ciddi İlişki ❤️",
        "Evlilik 💍",
        "Sohbet & Arkadaşlık ☕",
        "Kahve & Buluşma 🍹",
        "Akışına Bırakmak ✨"
    )

    val occupationOptions = listOf(
        "Özel Sektör 💼", "Mühendis 💻", "Doktor / Sağlık 🩺", "Öğretmen 🍎",
        "Girişimci 🚀", "Öğrenci 🎓", "Mimar / Tasarım 🎨", "Kamu / Memur 🏛️",
        "Finans / Bankacı 📊", "Serbest Meslek ✨"
    )

    val educationOptions = listOf(
        "Üniversite Mezunu 🎓",
        "Yüksek Lisans / Doktora 📚",
        "Üniversite Öğrencisi 🎒",
        "Lise Mezunu 🏫"
    )

    val zodiacOptions = listOf(
        "Koç ♈", "Boğa ♉", "İkizler ♊", "Yengeç ♋",
        "Aslan ♌", "Başak ♍", "Terazi ♎", "Akrep ♏",
        "Yay ♐", "Oğlak ♑", "Kova ♒", "Balık ♓"
    )

    val heightOptions = listOf("160", "165", "170", "175", "180", "185", "190")

    val bioSuggestions = listOf(
        "Kahve ve samimi sohbetleri severim ☕",
        "Yeni yerler keşfetmeyi ve seyahati çok severim ✈️",
        "Müzik, konserler ve sanat hayatımın vazgeçilmezi 🎵",
        "Doğada vakit geçirmek ve kamp yapmak favorim 🌲",
        "Kitaplar, sinema ve derin sohbetler 📚",
        "Düzenli spor yapar, sağlıklı yaşama önem veririm 🏋️"
    )

    val promptOptions = listOf(
        "Pazar sabahı ideal rutinim:",
        "Hakkımda en şaşırtıcı şey:",
        "Beni en çok güldüren şey:",
        "İlk buluşmada en sevdiğim aktivite:",
        "Hayat mottom:"
    )

    val availableInterests = listOf(
        "Kahve ☕", "Müzik 🎵", "Seyahat ✈️", "Fotoğrafçılık 📷",
        "Sinema 🍿", "Spor 🏋️", "Kitaplar 📚", "Gastronomi 🍝",
        "Doğa 🌲", "Dans 💃", "Kamp ⛺", "Sanat 🎨",
        "Evcil Hayvan 🐱", "Konserler 🎸", "Yazılım 💻", "Oyun 🎮",
        "Yoga 🧘", "Tiyatro 🎭"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            photos = photos + it.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .background(NightSurface)
            .verticalScroll(rememberScrollState())
            .padding(18.dp)
    ) {
        // Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = RoseAccent.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent)
        ) {
            Text(
                text = "PROFİLİNİ OLUŞTUR",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = RoseAccent,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hoş Geldin ✨",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Text(
            text = "Fotoğrafını yükle ve sana uygun seçenekleri işaretle.",
            fontSize = 13.sp,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // 1. Photos Section - Optional
        Text(
            text = "1. Profil Fotoğrafı (İsteğe Bağlı)",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = CupidGold
        )
        Text(
            text = if (photos.isEmpty()) "Fotoğraf yüklemeden devam edebilirsiniz (varsayılan boş avatar atanır)." else "${photos.size} fotoğraf yüklendi.",
            fontSize = 12.sp,
            color = if (photos.isEmpty()) Color(0xFF94A3B8) else Color(0xFF86EFAC),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(photos) { photoUrl ->
                Box(
                    modifier = Modifier
                        .size(width = 115.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, RoseAccent, RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Fotoğraf",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Remove button
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(24.dp)
                            .clickable {
                                photos = photos.filter { it != photoUrl }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Kaldır", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NightCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (photos.isEmpty()) RoseAccent else CupidGold.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .size(width = 115.dp, height = 150.dp)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("setup_add_photo_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = if (photos.isEmpty()) RoseAccent else CupidGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (photos.isEmpty()) "Fotoğraf Yükle" else "+ Fotoğraf Ekle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Personal Information
        Text(
            text = "2. Temel Bilgiler",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = CupidGold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = NightCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Name Field (Normal name from Google)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ad Soyad") },
                    placeholder = { Text("Adınız ve soyadınız") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_name_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Age selection chips + input
                Text("Yaşınızı Seçin:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ageOptions) { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (ageText == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (ageText == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { ageText = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ageText == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Veya Yaş Girin") },
                    placeholder = { Text("Örn: 24") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_age_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // City selection chips + input
                Text("Şehir Seçin:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cityOptions.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (city == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (city == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable {
                                city = option
                                cityCoordinates[option]?.let { (lat, lng) ->
                                    latitudeText = "%.4f".format(java.util.Locale.US, lat)
                                    longitudeText = "%.4f".format(java.util.Locale.US, lng)
                                }
                            }
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (city == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Şehir / İlçe") },
                    placeholder = { Text("Örn: Kadıköy, İstanbul") },
                    modifier = Modifier.fillMaxWidth().testTag("setup_city_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Enlem (Lat)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("setup_latitude_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Boylam (Lng)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("setup_longitude_field"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RoseAccent.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRequestLocationUpdate() }
                        .testTag("setup_gps_location_button")
                ) {
                    Text(
                        text = "📍 Canlı GPS Jeolokasyonumu Güncelle ($latitudeText, $longitudeText)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Looking For selection chips
                Text("Ne Arıyorsun? (İlişki Amacı):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    lookingForOptions.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (lookingFor == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (lookingFor == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { lookingFor = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (lookingFor == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Occupation chips + input
                Text("Meslek Seçin / Yazın:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    occupationOptions.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (occupation == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (occupation == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { occupation = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (occupation == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = { Text("Meslek") },
                    placeholder = { Text("Mesleğinizi yazın") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Education selection chips
                Text("Eğitim Durumu:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    educationOptions.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (education == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (education == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { education = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (education == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Zodiac selection chips
                Text("Burcunuz:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    zodiacOptions.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (zodiac == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (zodiac == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { zodiac = option }
                        ) {
                            Text(
                                text = option,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (zodiac == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Height selection chips + input
                Text("Boyunuz (cm):", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(heightOptions) { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (heightText == option) RoseAccent else Color(0xFF261D32),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (heightText == option) RoseAccent else NightCardBorder),
                            modifier = Modifier.clickable { heightText = option }
                        ) {
                            Text(
                                text = "$option cm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (heightText == option) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("Boy (cm)") },
                    placeholder = { Text("Örn: 175") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Biography with Selection Suggestions
        Text(
            text = "3. Hakkımda / Biyografi",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = CupidGold
        )
        Text(
            text = "Aşağıdaki hazır cümlelere tıklayarak biyografine ekleyebilir ya da kendi cümleni yazabilirsin:",
            fontSize = 11.5.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bioSuggestions.forEach { suggestion ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF241A30),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NightCardBorder),
                    modifier = Modifier.clickable {
                        bio = if (bio.isBlank()) suggestion else "$bio $suggestion"
                    }
                ) {
                    Text(
                        text = "+ $suggestion",
                        fontSize = 11.5.sp,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            placeholder = { Text("Kendinden, hobilerinden ve beklentilerinden bahset...") },
            modifier = Modifier.fillMaxWidth().testTag("setup_bio_field"),
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Interests (Starts empty, multi-select chips)
        Text(
            text = "4. İlgi Alanların (Seçim Yap)",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = CupidGold
        )
        Text(
            text = "Seni anlatan ilgi alanlarını seç (${selectedInterests.size} seçildi):",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        FlowRow(modifier = Modifier.fillMaxWidth()) {
            availableInterests.forEach { interest ->
                val isSelected = selectedInterests.contains(interest)
                InterestChip(
                    text = interest,
                    isSelected = isSelected,
                    onClick = {
                        selectedInterests = if (isSelected) selectedInterests.filter { it != interest } else selectedInterests + interest
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Prompt Q&A (Selectable question)
        Text(
            text = "5. Profil Sorusu (Buzkıran)",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = CupidGold
        )
        Text(
            text = "Bir soru seç ve yanıtla:",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            promptOptions.forEach { option ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (promptQuestion == option) RoseAccent else Color(0xFF261D32),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (promptQuestion == option) RoseAccent else NightCardBorder),
                    modifier = Modifier.clickable { promptQuestion = option }
                ) {
                    Text(
                        text = option,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (promptQuestion == option) Color.White else Color(0xFFCBD5E1),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = promptQuestion,
            onValueChange = { promptQuestion = it },
            label = { Text("Seçilen Soru") },
            placeholder = { Text("Yukarıdan bir soru seçin") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = promptAnswer,
            onValueChange = { promptAnswer = it },
            label = { Text("Cevabın") },
            placeholder = { Text("Cevabınızı buraya yazın...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Save & Complete Button
        Button(
            onClick = {
                val ageInt = ageText.toIntOrNull() ?: 24
                val heightInt = heightText.toIntOrNull() ?: 175
                val resolvedLat = (latitudeText.replace(",", ".").toDoubleOrNull() ?: initialLatitude).coerceIn(-90.0, 90.0)
                val resolvedLng = (longitudeText.replace(",", ".").toDoubleOrNull() ?: initialLongitude).coerceIn(-180.0, 180.0)
                val stableUserId = if (initialEmail.isNotBlank()) {
                    "usr_" + initialEmail.replace("@", "_").replace(".", "_")
                } else {
                    "usr_" + System.currentTimeMillis()
                }
                val createdUser = UserProfileEntity(
                    id = stableUserId,
                    name = if (name.isBlank()) "Yeni Üye" else name.trim(),
                    age = ageInt,
                    city = if (city.isBlank()) "İstanbul" else city.trim(),
                    bio = bio.trim(),
                    occupation = occupation.trim(),
                    education = education.trim(),
                    heightCm = heightInt,
                    zodiac = if (zodiac.isBlank()) "Aslan ♌" else zodiac.trim(),
                    lookingFor = if (lookingFor.isBlank()) "Ciddi İlişki ❤️" else lookingFor.trim(),
                    photosJson = photos.joinToString("|||"),
                    interestsJson = selectedInterests.joinToString("|||"),
                    lifestyleJson = "🍷 Sosyal İçici|||🚭 Sigara Yok|||🐕 Hayvansever|||🏋️ Düzenli Spor",
                    promptQuestion = promptQuestion.trim(),
                    promptAnswer = promptAnswer.trim(),
                    latitude = resolvedLat,
                    longitude = resolvedLng,
                    isVerified = true,
                    isOnline = true,
                    isCurrentUser = true,
                    email = initialEmail,
                    isProfileSetupCompleted = true
                )
                onCompleteSetup(createdUser)
            },
            colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("complete_profile_setup_button")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Profili Kaydet ve Başla",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
