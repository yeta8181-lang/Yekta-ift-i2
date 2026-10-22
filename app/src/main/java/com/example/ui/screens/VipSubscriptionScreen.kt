package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.ActiveSubscriptionInfo
import com.example.billing.GooglePlayBillingManager
import com.example.billing.VipSubscriptionPlan
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.RoseAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipSubscriptionScreen(
    isVip: Boolean,
    isBillingConnected: Boolean,
    isProcessing: Boolean,
    isPendingPurchase: Boolean = false,
    availablePlans: List<VipSubscriptionPlan>,
    selectedPlanId: String,
    activeSubscription: ActiveSubscriptionInfo?,
    onSelectPlan: (String) -> Unit,
    onPurchaseSelectedPlan: (String) -> Unit,
    onRestorePurchases: () -> Unit,
    onManageInPlayStore: () -> Unit,
    onCancelSubscription: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val selectedPlan = availablePlans.find { it.productId == selectedPlanId }
        ?: availablePlans.firstOrNull()
        ?: GooglePlayBillingManager.DEFAULT_PLANS[1]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
            .testTag("vip_subscription_screen")
    ) {
        // Top AppBar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = CupidGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VIP Abonelik Yönetimi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("vip_dialog_close")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }
            },
            actions = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isBillingConnected) OnlineGreen.copy(alpha = 0.16f) else CupidGold.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (isBillingConnected) OnlineGreen.copy(alpha = 0.6f) else CupidGold.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isBillingConnected) OnlineGreen else CupidGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBillingConnected) "Google Play Bağlı" else "Google Play Hazır",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBillingConnected) OnlineGreen else CupidGold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = NightCard)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pending Purchase Banner if Google Play payment is waiting confirmation
            if (isPendingPurchase) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF33240C)),
                    border = BorderStroke(1.5.dp, CupidGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Play Ödemesi Onay Bekliyor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CupidGold
                            )
                            Text(
                                text = "İşleminiz Google Play tarafından onaylandığında VIP üyeliğiniz otomatik olarak aktifleşecektir.",
                                fontSize = 12.sp,
                                color = Color(0xFFFDE68A),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Hero Banner Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NightCard),
                border = BorderStroke(1.5.dp, CupidGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF3A2410),
                                    Color(0xFF1F142E),
                                    NightCard
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = CupidGold.copy(alpha = 0.18f),
                            border = BorderStroke(2.dp, CupidGold),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "VIP Taç",
                                    tint = CupidGold,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isVip) "Kalp Kalbe VIP Üyeliğiniz Aktif 👑" else "Kalp Kalbe VIP ile Kilidi Aç 👑",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sohbet başlatmak, fotoğraf/video göndermek ve Keşfet akışında paylaşım yapmak sadece VIP üyelere özeldir.",
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Subscription Status Management Card (Shown when user is VIP)
            if (isVip) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val startTimeText = activeSubscription?.purchaseTimeMillis?.let {
                    dateFormat.format(Date(it))
                } ?: "Bugün"
                val orderIdLine = if (!activeSubscription?.orderId.isNullOrBlank()) {
                    "\n• Google Play Sipariş No: ${activeSubscription?.orderId}"
                } else {
                    ""
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF182C25)),
                    border = BorderStroke(1.5.dp, OnlineGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_subscription_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OnlineGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activeSubscription?.planTitle ?: "Aylık VIP Üyelik",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OnlineGreen
                            ) {
                                Text(
                                    text = "AKTİF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• Paket Ücreti: ${activeSubscription?.priceDisplay ?: "400,00 ₺ / Ay"}\n" +
                                "• Başlangıç: $startTimeText$orderIdLine\n" +
                                "• Durum: Sınırsız Sohbet, Keşfet Paylaşımı ve 100 km Radar Açık ✓",
                            fontSize = 12.5.sp,
                            color = Color(0xFFD1FAE5),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onManageInPlayStore,
                                border = BorderStroke(1.dp, CupidGold),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manage_google_play_subs_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = CupidGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Play Store'da Yönet",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CupidGold
                                )
                            }

                            OutlinedButton(
                                onClick = onCancelSubscription,
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cancel_vip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aboneliği İptal Et",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Subscription Plan Selection Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Google Play Abonelik Paketleri",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "İstediğin an iptal et",
                    fontSize = 12.sp,
                    color = CupidGold,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            availablePlans.forEach { plan ->
                val isSelected = plan.productId == selectedPlan.productId
                val isCurrentActivePlan = isVip && activeSubscription?.productId == plan.productId
                SubscriptionPlanCard(
                    plan = plan,
                    isSelected = isSelected,
                    isCurrentActivePlan = isCurrentActivePlan,
                    onClick = { onSelectPlan(plan.productId) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Purchase CTA Button
            Button(
                onClick = { onPurchaseSelectedPlan(selectedPlan.productId) },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CupidGold
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("buy_vip_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(
                    text = if (isVip) {
                        "Paketi Güncelle • ${selectedPlan.formattedPrice}"
                    } else {
                        "Google Play ile Satın Al (${selectedPlan.formattedPrice})"
                    },
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Restore Purchases & Play Store Subscription Center row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onRestorePurchases,
                    border = BorderStroke(1.dp, NightCardBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("restore_purchases_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = CupidGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Satın Alımı Geri Yükle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (!isVip) {
                    OutlinedButton(
                        onClick = onManageInPlayStore,
                        border = BorderStroke(1.dp, NightCardBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("manage_google_play_subs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play Aboneliklerim",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature Comparison Table: Standart vs VIP
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightCard),
                border = BorderStroke(1.dp, NightCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VIP ile Açılan Özellikler",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CupidGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FeatureComparisonRow(
                        icon = Icons.Default.Forum,
                        title = "Sohbet & Mesajlaşma",
                        subtitle = "Eşleşmelerle sınırsız mesaj, fotoğraf ve video gönderimi",
                        standardStatus = "🔒 Kilitli",
                        vipStatus = "✓ Sınırsız Açık"
                    )
                    HorizontalDivider(color = NightCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    FeatureComparisonRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Keşfette Paylaşım & Yorum Yapma",
                        subtitle = "Keşfet akışında fotoğraf/video paylaşma ve paylaşımlara yorum yapma",
                        standardStatus = "🔒 Kilitli",
                        vipStatus = "✓ Sınırsız Açık"
                    )
                    HorizontalDivider(color = NightCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    FeatureComparisonRow(
                        icon = Icons.Default.Radar,
                        title = "Harita & Canlı Radar Menzili",
                        subtitle = "Yakındaki kişileri haritada keşfetme yarıçapı",
                        standardStatus = "50 km",
                        vipStatus = "✓ 100 km 👑"
                    )
                    HorizontalDivider(color = NightCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    FeatureComparisonRow(
                        icon = Icons.Default.AllInclusive,
                        title = "Profil Kaydırma & Geri Alma",
                        subtitle = "Keşfette kartları kaydırma ve son kartı geri alma",
                        standardStatus = "Standart",
                        vipStatus = "✓ Sınırsız + Öncelikli"
                    )
                    HorizontalDivider(color = NightCardBorder, modifier = Modifier.padding(vertical = 8.dp))

                    FeatureComparisonRow(
                        icon = Icons.Default.Star,
                        title = "Süper Beğeni & VIP Taç Rozeti",
                        subtitle = "Profilinde altın VIP rozeti ve günlük 5 Süper Beğeni",
                        standardStatus = "🔒 Kilitli",
                        vipStatus = "✓ Dahil"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ödeme, satın alma onayında Google Play hesabınızdan tahsil edilir. Abonelik, mevcut dönemin bitiminden en az 24 saat önce iptal edilmediği sürece seçilen paket ücretiyle otomatik olarak yenilenir. Aboneliğinizi Google Play Store > Ödemeler ve Abonelikler > Abonelikler bölümünden dilediğiniz zaman yönetebilir veya iptal edebilirsiniz.",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: VipSubscriptionPlan,
    isSelected: Boolean,
    isCurrentActivePlan: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2B1F3A) else NightCard
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CupidGold else NightCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("plan_card_${plan.productId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = CupidGold,
                            unselectedColor = Color.Gray
                        )
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plan.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            if (plan.badgeText != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) CupidGold else RoseAccent.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = plan.badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.Black else CupidGold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plan.description,
                            fontSize = 11.5.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SKU: ${plan.productId} • Plan: ${plan.basePlanId}",
                            fontSize = 10.sp,
                            color = if (plan.isPlayStoreVerified) OnlineGreen else Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) CupidGold else Color(0xFF1E1730)
                    ) {
                        Text(
                            text = plan.formattedPrice,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.Black else CupidGold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    if (isCurrentActivePlan) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mevcut Paket",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnlineGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureComparisonRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    standardStatus: String,
    vipStatus: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = CupidGold.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CupidGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = vipStatus,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = OnlineGreen
            )
            Text(
                text = "Standart: $standardStatus",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
