package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfileEntity
import com.example.ui.screens.calculateCompatibilityPercent
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.VerifiedBlue
import kotlin.math.roundToInt

enum class DiscoverSortOption(val label: String) {
    RECOMMENDED("Önerilen"),
    HIGHEST_COMPATIBILITY("En Yüksek Uyum (%)"),
    NEAREST_DISTANCE("En Yakın Mesafe"),
    YOUNGEST_FIRST("Yaş (Gençten →)")
}

data class DiscoverFilterState(
    val minAge: Int = DEFAULT_MIN_AGE,
    val maxAge: Int = DEFAULT_MAX_AGE,
    val maxDistanceKm: Int = DEFAULT_MAX_DISTANCE_KM, // 500 means unlimited / all distances
    val minHeightCm: Int = DEFAULT_MIN_HEIGHT_CM,
    val maxHeightCm: Int = DEFAULT_MAX_HEIGHT_CM,
    val selectedCity: String? = null,
    val selectedLookingFor: Set<String> = emptySet(),
    val selectedZodiacs: Set<String> = emptySet(),
    val selectedInterests: Set<String> = emptySet(),
    val minCompatibilityPercent: Int = 0,
    val onlyVerified: Boolean = false,
    val onlyOnline: Boolean = false,
    val onlyWithPhoto: Boolean = false,
    val sortOption: DiscoverSortOption = DiscoverSortOption.RECOMMENDED
) {
    companion object {
        const val DEFAULT_MIN_AGE = 18
        const val DEFAULT_MAX_AGE = 60
        const val DEFAULT_MAX_DISTANCE_KM = 500
        const val DEFAULT_MIN_HEIGHT_CM = 150
        const val DEFAULT_MAX_HEIGHT_CM = 210

        val ALL_ZODIACS = listOf(
            "Koç", "Boğa", "İkizler", "Yengeç", "Aslan", "Başak",
            "Terazi", "Akrep", "Yay", "Oğlak", "Kova", "Balık"
        )

        val DEFAULT_LOOKING_FOR_OPTIONS = listOf(
            "Uzun Süreli İlişki",
            "Ciddi İlişki",
            "Kahve & Sohbet",
            "Yeni Arkadaşlıklar",
            "Romantik Buluşma",
            "Seyahat Arkadaşı"
        )
    }

    val activeFilterCount: Int
        get() {
            var count = 0
            if (minAge > DEFAULT_MIN_AGE || maxAge < DEFAULT_MAX_AGE) count++
            if (maxDistanceKm < DEFAULT_MAX_DISTANCE_KM) count++
            if (minHeightCm > DEFAULT_MIN_HEIGHT_CM || maxHeightCm < DEFAULT_MAX_HEIGHT_CM) count++
            if (!selectedCity.isNullOrBlank()) count++
            if (selectedLookingFor.isNotEmpty()) count++
            if (selectedZodiacs.isNotEmpty()) count++
            if (selectedInterests.isNotEmpty()) count++
            if (minCompatibilityPercent > 0) count++
            if (onlyVerified) count++
            if (onlyOnline) count++
            if (onlyWithPhoto) count++
            if (sortOption != DiscoverSortOption.RECOMMENDED) count++
            return count
        }

    val isDefault: Boolean
        get() = activeFilterCount == 0

    fun matches(profile: UserProfileEntity, currentUser: UserProfileEntity? = null): Boolean {
        if (profile.age < minAge || profile.age > maxAge) return false

        if (maxDistanceKm < DEFAULT_MAX_DISTANCE_KM && profile.distanceKm > maxDistanceKm.toDouble()) {
            return false
        }

        if (profile.heightCm > 0 && (profile.heightCm < minHeightCm || profile.heightCm > maxHeightCm)) {
            return false
        }

        if (!selectedCity.isNullOrBlank() && !profile.city.equals(selectedCity, ignoreCase = true)) {
            return false
        }

        if (selectedLookingFor.isNotEmpty()) {
            val matchesGoal = selectedLookingFor.any { goal ->
                profile.lookingFor.contains(goal, ignoreCase = true) ||
                    goal.contains(profile.lookingFor, ignoreCase = true)
            }
            if (!matchesGoal) return false
        }

        if (selectedZodiacs.isNotEmpty()) {
            val matchesZodiac = selectedZodiacs.any { z ->
                profile.zodiac.equals(z, ignoreCase = true)
            }
            if (!matchesZodiac) return false
        }

        if (selectedInterests.isNotEmpty()) {
            val profileInterestsLower = profile.interestList.map { it.lowercase() }
            val matchesInterest = selectedInterests.any { wanted ->
                val w = wanted.lowercase()
                profileInterestsLower.any { it == w || it.contains(w) } ||
                    profile.bio.lowercase().contains(w)
            }
            if (!matchesInterest) return false
        }

        if (minCompatibilityPercent > 0) {
            val compat = calculateCompatibilityPercent(currentUser, profile)
            if (compat < minCompatibilityPercent) return false
        }

        if (onlyVerified && !profile.isVerified) return false
        if (onlyOnline && !profile.isOnline) return false
        if (onlyWithPhoto && profile.photoList.isEmpty()) return false

        return true
    }

    fun filterAndSort(
        profiles: List<UserProfileEntity>,
        currentUser: UserProfileEntity? = null
    ): List<UserProfileEntity> {
        val filtered = profiles.filter { matches(it, currentUser) }
        return when (sortOption) {
            DiscoverSortOption.RECOMMENDED -> filtered
            DiscoverSortOption.HIGHEST_COMPATIBILITY ->
                filtered.sortedByDescending { calculateCompatibilityPercent(currentUser, it) }
            DiscoverSortOption.NEAREST_DISTANCE ->
                filtered.sortedBy { it.distanceKm }
            DiscoverSortOption.YOUNGEST_FIRST ->
                filtered.sortedBy { it.age }
        }
    }

    fun activeSummaryBadges(): List<String> {
        val badges = mutableListOf<String>()
        if (minAge > DEFAULT_MIN_AGE || maxAge < DEFAULT_MAX_AGE) {
            badges.add("Yaş: $minAge-$maxAge")
        }
        if (maxDistanceKm < DEFAULT_MAX_DISTANCE_KM) {
            badges.add("≤ $maxDistanceKm km")
        }
        if (!selectedCity.isNullOrBlank()) {
            badges.add(selectedCity)
        }
        if (minHeightCm > DEFAULT_MIN_HEIGHT_CM || maxHeightCm < DEFAULT_MAX_HEIGHT_CM) {
            badges.add("$minHeightCm-$maxHeightCm cm")
        }
        if (onlyVerified) badges.add("Mavi Tikli ✓")
        if (onlyOnline) badges.add("Çevrimiçi ●")
        if (onlyWithPhoto) badges.add("Fotoğraflı")
        if (minCompatibilityPercent > 0) badges.add("%$minCompatibilityPercent+ Uyum")
        selectedLookingFor.forEach { badges.add(it) }
        selectedZodiacs.forEach { badges.add("✨ $it") }
        selectedInterests.forEach { badges.add("#$it") }
        if (sortOption != DiscoverSortOption.RECOMMENDED) {
            badges.add("Sırala: ${sortOption.label}")
        }
        return badges
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedDiscoverFilterDialog(
    initialFilter: DiscoverFilterState,
    allProfiles: List<UserProfileEntity>,
    currentUser: UserProfileEntity?,
    availableInterests: List<String>,
    onApply: (DiscoverFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initialFilter) { mutableStateOf(initialFilter) }

    val availableCities by remember(allProfiles, currentUser) {
        derivedStateOf {
            val defaultCities = listOf("İstanbul", "Ankara", "İzmir", "Antalya", "Bursa", "Adana")
            val fromProfiles = allProfiles.map { it.city.trim() }.filter { it.isNotEmpty() }
            val myCity = listOfNotNull(currentUser?.city?.trim()?.takeIf { it.isNotEmpty() })
            (myCity + fromProfiles + defaultCities).distinct()
        }
    }

    val lookingForOptions by remember(allProfiles) {
        derivedStateOf {
            val fromProfiles = allProfiles.map { it.lookingFor.trim() }.filter { it.isNotEmpty() }
            (DiscoverFilterState.DEFAULT_LOOKING_FOR_OPTIONS + fromProfiles).distinct()
        }
    }

    val matchingCount by remember(draft, allProfiles, currentUser) {
        derivedStateOf {
            draft.filterAndSort(allProfiles, currentUser).size
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NightSurface,
            border = BorderStroke(1.dp, NightCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("advanced_discover_filter_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = NightCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RoseAccent.copy(alpha = 0.18f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = RoseAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Gelişmiş Keşfet Filtreleri",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (draft.activeFilterCount > 0) {
                                        "${draft.activeFilterCount} filtre aktif • $matchingCount profil uygun"
                                    } else {
                                        "Aradığın kriterlere en uygun kişileri keşfet"
                                    },
                                    fontSize = 12.sp,
                                    color = if (draft.activeFilterCount > 0) CupidGold else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!draft.isDefault) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF231834),
                                    border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { draft = DiscoverFilterState() }
                                        .testTag("reset_advanced_filters_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sıfırla",
                                            tint = RoseAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Sıfırla",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("close_advanced_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = NightCardBorder)

                // Scrollable Filter Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Sort Option
                    FilterSectionCard(
                        title = "Sıralama Ölçütü",
                        subtitle = draft.sortOption.label,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = null,
                                tint = CupidGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiscoverSortOption.values().forEach { option ->
                                val selected = draft.sortOption == option
                                FilterChip(
                                    selected = selected,
                                    onClick = { draft = draft.copy(sortOption = option) },
                                    label = {
                                        Text(
                                            text = option.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CupidGold,
                                        selectedLabelColor = Color.Black,
                                        selectedLeadingIconColor = Color.Black,
                                        containerColor = NightSurface,
                                        labelColor = Color(0xFFE2E8F0)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = NightCardBorder,
                                        selectedBorderColor = CupidGold
                                    ),
                                    modifier = Modifier.testTag("sort_chip_${option.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 2. Age Range Slider
                    FilterSectionCard(
                        title = "Yaş Aralığı",
                        subtitle = "${draft.minAge} - ${draft.maxAge} yaş",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        RangeSlider(
                            value = draft.minAge.toFloat()..draft.maxAge.toFloat(),
                            onValueChange = { range ->
                                val newMin = range.start.roundToInt().coerceIn(
                                    DiscoverFilterState.DEFAULT_MIN_AGE,
                                    DiscoverFilterState.DEFAULT_MAX_AGE - 1
                                )
                                val newMax = range.endInclusive.roundToInt().coerceIn(
                                    newMin + 1,
                                    DiscoverFilterState.DEFAULT_MAX_AGE
                                )
                                draft = draft.copy(minAge = newMin, maxAge = newMax)
                            },
                            valueRange = DiscoverFilterState.DEFAULT_MIN_AGE.toFloat()..DiscoverFilterState.DEFAULT_MAX_AGE.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = RoseAccent,
                                activeTrackColor = RoseAccent,
                                inactiveTrackColor = NightCardBorder
                            ),
                            modifier = Modifier.testTag("age_range_slider")
                        )
                        // Quick Age Presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                Triple("Tümü (18-60)", 18, 60),
                                Triple("18-24", 18, 24),
                                Triple("25-32", 25, 32),
                                Triple("30-40", 30, 40),
                                Triple("35+", 35, 60)
                            )
                            presets.forEach { (label, minA, maxA) ->
                                val isPresetSelected = draft.minAge == minA && draft.maxAge == maxA
                                QuickPresetPill(
                                    label = label,
                                    selected = isPresetSelected,
                                    onClick = { draft = draft.copy(minAge = minA, maxAge = maxA) },
                                    testTag = "age_preset_${minA}_${maxA}"
                                )
                            }
                        }
                    }

                    // 3. Max Distance Slider & City Selection
                    FilterSectionCard(
                        title = "Maksimum Mesafe & Şehir",
                        subtitle = if (draft.maxDistanceKm >= DiscoverFilterState.DEFAULT_MAX_DISTANCE_KM) {
                            "Tüm Mesafeler${draft.selectedCity?.let { " • $it" } ?: ""}"
                        } else {
                            "En fazla ${draft.maxDistanceKm} km${draft.selectedCity?.let { " • $it" } ?: ""}"
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = OnlineGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Slider(
                            value = draft.maxDistanceKm.toFloat(),
                            onValueChange = { value ->
                                draft = draft.copy(maxDistanceKm = value.roundToInt().coerceIn(2, 500))
                            },
                            valueRange = 2f..DiscoverFilterState.DEFAULT_MAX_DISTANCE_KM.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = OnlineGreen,
                                activeTrackColor = OnlineGreen,
                                inactiveTrackColor = NightCardBorder
                            ),
                            modifier = Modifier.testTag("distance_slider")
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val distPresets = listOf(
                                10 to "10 km",
                                25 to "25 km",
                                50 to "50 km",
                                100 to "100 km",
                                500 to "Tüm Türkiye"
                            )
                            distPresets.forEach { (km, label) ->
                                QuickPresetPill(
                                    label = label,
                                    selected = draft.maxDistanceKm == km,
                                    onClick = { draft = draft.copy(maxDistanceKm = km) },
                                    testTag = "distance_preset_$km"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Şehir Seçimi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickPresetPill(
                                label = "Tüm Şehirler",
                                selected = draft.selectedCity == null,
                                onClick = { draft = draft.copy(selectedCity = null) },
                                testTag = "city_chip_all"
                            )
                            availableCities.forEach { city ->
                                val selected = draft.selectedCity.equals(city, ignoreCase = true)
                                QuickPresetPill(
                                    label = city,
                                    selected = selected,
                                    onClick = {
                                        draft = draft.copy(selectedCity = if (selected) null else city)
                                    },
                                    testTag = "city_chip_$city"
                                )
                            }
                        }
                    }

                    // 4. Quick Profile Status Switches (Verified, Online, With Photo)
                    FilterSectionCard(
                        title = "Profil Güvencesi & Durum",
                        subtitle = "Doğrulanmış ve aktif üyeler",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = VerifiedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterToggleRow(
                                title = "Sadece Mavi Tikli (Doğrulanmış)",
                                subtitle = "Yüz doğrulaması tamamlanmış gerçek profiller",
                                checked = draft.onlyVerified,
                                accentColor = VerifiedBlue,
                                onCheckedChange = { draft = draft.copy(onlyVerified = it) },
                                testTag = "filter_switch_verified"
                            )
                            HorizontalDivider(color = NightCardBorder.copy(alpha = 0.5f))
                            FilterToggleRow(
                                title = "Sadece Şu An Çevrimiçi Olanlar",
                                subtitle = "Uygulamada aktif olan kullanıcıları öne çıkar",
                                checked = draft.onlyOnline,
                                accentColor = OnlineGreen,
                                onCheckedChange = { draft = draft.copy(onlyOnline = it) },
                                testTag = "filter_switch_online"
                            )
                            HorizontalDivider(color = NightCardBorder.copy(alpha = 0.5f))
                            FilterToggleRow(
                                title = "Sadece Fotoğraflı Profiller",
                                subtitle = "En az bir profil fotoğrafı yüklemiş kişiler",
                                checked = draft.onlyWithPhoto,
                                accentColor = RoseAccent,
                                onCheckedChange = { draft = draft.copy(onlyWithPhoto = it) },
                                testTag = "filter_switch_photo"
                            )
                        }
                    }

                    // 5. Minimum Compatibility Score
                    FilterSectionCard(
                        title = "Minimum Uyumluluk Skoru",
                        subtitle = if (draft.minCompatibilityPercent == 0) "Tümü" else "En az %${draft.minCompatibilityPercent} Uyum",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CupidGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val scoreOptions = listOf(0, 75, 80, 85, 90)
                            scoreOptions.forEach { score ->
                                val isSel = draft.minCompatibilityPercent == score
                                QuickPresetPill(
                                    label = if (score == 0) "Tümü" else "%$score+ Uyum",
                                    selected = isSel,
                                    onClick = { draft = draft.copy(minCompatibilityPercent = score) },
                                    testTag = "compat_preset_$score"
                                )
                            }
                        }
                    }

                    // 6. Looking For (Relationship Expectations)
                    FilterSectionCard(
                        title = "İlişki Beklentisi",
                        subtitle = if (draft.selectedLookingFor.isEmpty()) "Tümü" else "${draft.selectedLookingFor.size} seçili",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = RoseAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lookingForOptions.forEach { goal ->
                                val isSelected = goal in draft.selectedLookingFor
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = if (isSelected) {
                                            draft.selectedLookingFor - goal
                                        } else {
                                            draft.selectedLookingFor + goal
                                        }
                                        draft = draft.copy(selectedLookingFor = updated)
                                    },
                                    label = {
                                        Text(
                                            text = goal,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoseAccent,
                                        selectedLabelColor = Color.White,
                                        containerColor = NightSurface,
                                        labelColor = Color(0xFFE2E8F0)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = NightCardBorder,
                                        selectedBorderColor = RoseAccent
                                    ),
                                    modifier = Modifier.testTag("goal_chip_$goal")
                                )
                            }
                        }
                    }

                    // 7. Height Range Slider
                    FilterSectionCard(
                        title = "Boy Aralığı (cm)",
                        subtitle = "${draft.minHeightCm} cm - ${draft.maxHeightCm} cm",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Height,
                                contentDescription = null,
                                tint = CupidGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        RangeSlider(
                            value = draft.minHeightCm.toFloat()..draft.maxHeightCm.toFloat(),
                            onValueChange = { range ->
                                val newMin = range.start.roundToInt().coerceIn(
                                    DiscoverFilterState.DEFAULT_MIN_HEIGHT_CM,
                                    DiscoverFilterState.DEFAULT_MAX_HEIGHT_CM - 5
                                )
                                val newMax = range.endInclusive.roundToInt().coerceIn(
                                    newMin + 5,
                                    DiscoverFilterState.DEFAULT_MAX_HEIGHT_CM
                                )
                                draft = draft.copy(minHeightCm = newMin, maxHeightCm = newMax)
                            },
                            valueRange = DiscoverFilterState.DEFAULT_MIN_HEIGHT_CM.toFloat()..DiscoverFilterState.DEFAULT_MAX_HEIGHT_CM.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = CupidGold,
                                activeTrackColor = CupidGold,
                                inactiveTrackColor = NightCardBorder
                            ),
                            modifier = Modifier.testTag("height_range_slider")
                        )
                    }

                    // 8. Zodiac Signs
                    FilterSectionCard(
                        title = "Burç Filtresi",
                        subtitle = if (draft.selectedZodiacs.isEmpty()) "Tüm Burçlar" else draft.selectedZodiacs.joinToString(", "),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CupidGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiscoverFilterState.ALL_ZODIACS.forEach { zodiac ->
                                val isSelected = zodiac in draft.selectedZodiacs
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val updated = if (isSelected) {
                                            draft.selectedZodiacs - zodiac
                                        } else {
                                            draft.selectedZodiacs + zodiac
                                        }
                                        draft = draft.copy(selectedZodiacs = updated)
                                    },
                                    label = {
                                        Text(
                                            text = zodiac,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CupidGold,
                                        selectedLabelColor = Color.Black,
                                        containerColor = NightSurface,
                                        labelColor = Color(0xFFE2E8F0)
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = NightCardBorder,
                                        selectedBorderColor = CupidGold
                                    ),
                                    modifier = Modifier.testTag("zodiac_chip_$zodiac")
                                )
                            }
                        }
                    }

                    // 9. Multi-select Interests
                    if (availableInterests.isNotEmpty()) {
                        FilterSectionCard(
                            title = "Ortak İlgi Alanları",
                            subtitle = if (draft.selectedInterests.isEmpty()) "Tüm İlgi Alanları" else "${draft.selectedInterests.size} ilgi alanı seçili",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    tint = RoseAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableInterests.forEach { interest ->
                                    val isSelected = interest in draft.selectedInterests
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected) {
                                                draft.selectedInterests - interest
                                            } else {
                                                draft.selectedInterests + interest
                                            }
                                            draft = draft.copy(selectedInterests = updated)
                                        },
                                        label = {
                                            Text(
                                                text = interest,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RoseAccent,
                                            selectedLabelColor = Color.White,
                                            containerColor = NightSurface,
                                            labelColor = Color(0xFFE2E8F0)
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = NightCardBorder,
                                            selectedBorderColor = RoseAccent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = NightCardBorder)

                // Footer Action Bar
                Surface(
                    color = NightCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { draft = DiscoverFilterState() },
                            border = BorderStroke(1.dp, NightCardBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(0.38f)
                        ) {
                            Text(
                                text = "Temizle",
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { onApply(draft) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(0.62f)
                                .testTag("apply_advanced_filters_button")
                        ) {
                            Text(
                                text = "Uygula ($matchingCount Kişi)",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = NightCard,
        border = BorderStroke(1.dp, NightCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CupidGold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun QuickPresetPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) RoseAccent else NightSurface,
        border = BorderStroke(1.dp, if (selected) RoseAccent else NightCardBorder),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFFCBD5E1),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FilterToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = NightSurface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
