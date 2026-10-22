package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent

@Composable
fun VipPaywallDialog(
    isAlreadyVip: Boolean,
    onDismiss: () -> Unit,
    onPurchaseVip: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CupidGold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CupidGold.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CupidGold)
                    ) {
                        Text(
                            text = "GOOGLE PLAY ABONELİĞİ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CupidGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("vip_dialog_close")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Crown Badge
                Surface(
                    shape = CircleShape,
                    color = CupidGold.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(2.dp, CupidGold),
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
                    text = "Kalp Kalbe VIP 👑",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Aşkı bulma şansını 5 katına çıkar ve tüm ayrıcalıkların kilidini aç!",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Price Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NightCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CupidGold.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Aylık VIP Üyelik",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Google Play güvencesiyle",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CupidGold
                        ) {
                            Text(
                                text = "400 TL / Ay",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Benefits List
                VipBenefitItem(
                    icon = Icons.Default.LockOpen,
                    title = "Sınırsız Sohbet & Mesajlaşma (VIP Özel)",
                    desc = "Sadece VIP üyeler eşleştiği ve çevresindeki kişilerle sohbet edebilir, fotoğraf ve video gönderebilir."
                )

                VipBenefitItem(
                    icon = Icons.Default.WorkspacePremium,
                    title = "Keşfette Paylaşım & Yorum Yapma (VIP Özel)",
                    desc = "Sadece VIP üyeler Keşfet ve Anlar akışında fotoğraf/video paylaşabilir ve paylaşımlara yorum yapabilir."
                )

                VipBenefitItem(
                    icon = Icons.Default.AllInclusive,
                    title = "Sınırsız Beğeni & Geri Alma",
                    desc = "Hiçbir beğeni sınırı olmadan istediğin kadar profili kaydır ve yanlışlıkla geçtiğin kişileri geri al."
                )

                VipBenefitItem(
                    icon = Icons.Default.Visibility,
                    title = "Seni Beğenenleri Gör",
                    desc = "Sen kaydırmadan önce profilini kimlerin beğendiğini anında gör ve direkt eşleş."
                )

                VipBenefitItem(
                    icon = Icons.Default.Radar,
                    title = "100 km Radar Menzili",
                    desc = "Standart menzili 100 kilometreye kadar genişleterek daha çok şehre ve insana ulaş."
                )

                VipBenefitItem(
                    icon = Icons.Default.Star,
                    title = "Günlük 5 Süper Beğeni & Boost",
                    desc = "Her gün 5 kişiye dikkat çekici Süper Beğeni gönder ve profilini keşfette zirveye taşı."
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (isAlreadyVip) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CupidGold.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CupidGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✓ VIP ÜYELİĞİNİZ ŞU AN AKTİF!",
                            color = CupidGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onPurchaseVip,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CupidGold
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("buy_vip_button")
                    ) {
                        Text(
                            text = "Google Play ile Satın Al (400 TL)",
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Haftalık 100 TL, Aylık 400 TL veya Yıllık 900 TL seçenekleriyle Google Play hesabınız üzerinden tahsil edilir. İstediğiniz an Google Play > Abonelikler bölümünden iptal edebilirsiniz.",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun VipBenefitItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = CupidGold.copy(alpha = 0.15f),
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = CupidGold, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = desc, fontSize = 11.5.sp, color = Color(0xFFCBD5E1), lineHeight = 16.sp)
        }
    }
}
