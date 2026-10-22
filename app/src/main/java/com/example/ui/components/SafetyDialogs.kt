package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MomentComment
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SafeShield
import com.example.ui.theme.VerifiedBlue

@Composable
fun SafetyCenterDialog(
    isVerified: Boolean,
    onDismiss: () -> Unit,
    onRequestVerification: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SafeShield.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = SafeShield,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Güvenlik Merkezi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("safety_dialog_close")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Verification Status Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVerified) VerifiedBlue.copy(alpha = 0.15f) else Color(0xFF261E38)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Face,
                            contentDescription = null,
                            tint = if (isVerified) VerifiedBlue else RoseAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isVerified) "Profiliniz Doğrulandı ✓" else "Yüz Doğrulaması Yap",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isVerified) "Mavi tikli güvenilir profil rozetiniz aktif."
                                else "Selfie çekerek gerçek kişi olduğunuzu kanıtlayın ve güven kazanın.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                    }
                    if (!isVerified) {
                        Button(
                            onClick = onRequestVerification,
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("verify_selfie_button")
                        ) {
                            Text("Hemen Doğrula (Mavi Tik Al)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Güvenli Buluşma & Topluluk Moderasyonu",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                SafetyTipItem(
                    icon = Icons.Default.Place,
                    title = "Halka Açık Yerlerde Buluşun",
                    desc = "İlk buluşmalarınız için kalabalık kafeleri, restoranları ve açık alanları tercih edin."
                )

                SafetyTipItem(
                    icon = Icons.Default.Lock,
                    title = "Finansal Bilgilerinizi Paylaşmayın",
                    desc = "Kimseyle IBAN, kredi kartı, şifre veya para transferi gibi bilgileri paylaşmayın."
                )

                SafetyTipItem(
                    icon = Icons.Default.VerifiedUser,
                    title = "Planınızı Bir Yakınınızla Paylaşın",
                    desc = "Buluşmaya gitmeden önce kime gittiğinizi ve konumu güvendiğiniz bir arkadaşınıza haber verin."
                )

                SafetyTipItem(
                    icon = Icons.Default.Flag,
                    title = "Paylaşım ve Yorumları Modere Edin",
                    desc = "Paylaşımlarınıza gelen yorumları silebilir, rahatsız edici yorumları bildirebilir ve kullanıcıyı anında engelleyebilirsiniz."
                )

                SafetyTipItem(
                    icon = Icons.Default.Report,
                    title = "Şüpheli Davranışları Bildirin",
                    desc = "Rahatsızlık duyduğunuz herhangi bir profili anında 'Bildir ve Engelle' seçeneğiyle bize iletin."
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NightCard)
                ) {
                    Text("Anladım", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SafetyTipItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF28203E),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = RoseAccent, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
            Text(text = desc, fontSize = 11.5.sp, color = Color(0xFF94A3B8), lineHeight = 16.sp)
        }
    }
}

@Composable
fun ReportBlockDialog(
    user: UserProfileEntity,
    onDismiss: () -> Unit,
    onBlockConfirmed: () -> Unit,
    onReportConfirmed: (reason: String) -> Unit
) {
    val reportReasons = listOf(
        "Sahte Profil / Bot",
        "Uygunsuz Fotoğraf veya Video",
        "Taciz veya Rahatsız Edici Mesajlar",
        "Spam veya Reklam",
        "Diğer Sebepler"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RoseAccent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${user.name} Profilini Bildir / Engelle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Bu kullanıcıyı engellerseniz aranızdaki eşleşme ve sohbet silinir. Bildirirseniz moderasyon ekibimiz inceleyecektir.",
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Şikayet Nedeni Seçin:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                reportReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = (selectedReason == reason),
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = RoseAccent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = reason, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReportConfirmed(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                modifier = Modifier.testTag("report_confirm_button")
            ) {
                Text("Bildir ve Engelle", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBlockConfirmed,
                    modifier = Modifier.testTag("block_only_button")
                ) {
                    Text("Sadece Engelle", color = CupidGold, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Vazgeç", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun CommentReportBlockDialog(
    comment: MomentComment,
    moment: MomentEntity,
    isPostOwner: Boolean,
    isCommentAuthor: Boolean,
    onDismiss: () -> Unit,
    onDeleteComment: () -> Unit,
    onBlockAuthorOnly: () -> Unit,
    onReportComment: (reason: String, alsoBlockAuthor: Boolean) -> Unit
) {
    val reportReasons = listOf(
        "Taciz, Zorbalık veya Hakaret",
        "Uygunsuz veya Müstehcen Yorum",
        "Spam, Reklam veya Dolandırıcılık",
        "Nefret Söylemi veya Saldırgan Dil",
        "Sahte Hesap / Bot Aktivitesi"
    )
    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    var alsoBlockAuthor by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = NightSurface),
            border = BorderStroke(1.dp, NightCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("comment_moderation_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RoseAccent.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = RoseAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yorumu Yönet / Bildir",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPostOwner) "Paylaşım Sahibi Moderasyonu" else "Topluluk Güvenliği",
                                fontSize = 11.5.sp,
                                color = CupidGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("comment_moderation_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selected Comment Preview Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NightCard,
                    border = BorderStroke(1.dp, NightCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${comment.authorName} kullanıcısının yorumu:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CupidGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${comment.text}”",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFFE2E8F0),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Direct Delete option for Post Owner or Comment Author
                if (isPostOwner || isCommentAuthor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDeleteComment,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CupidGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCommentAuthor) "Kendi Yorumumu Sil" else "Yorumu Paylaşımdan Kaldır",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (!isCommentAuthor) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = NightCardBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Şikayet Nedeni Seçin:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    reportReasons.forEachIndexed { index, reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 3.dp)
                                .testTag("comment_report_reason_$index")
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RoseAccent,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                fontSize = 13.sp,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Block Author Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF231834),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alsoBlockAuthor = !alsoBlockAuthor }
                            .testTag("comment_block_author_checkbox")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = alsoBlockAuthor,
                                onCheckedChange = { alsoBlockAuthor = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = RoseAccent,
                                    uncheckedColor = Color.Gray,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${comment.authorName} kullanıcısını engelle",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Kullanıcının tüm yorumları ve paylaşımları akışınızdan gizlenir.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Report & Remove Button
                    Button(
                        onClick = { onReportComment(selectedReason, alsoBlockAuthor) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_report_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (alsoBlockAuthor) "Yorumu Bildir ve Engelle" else "Yorumu Bildir ve Kaldır",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Block Author Only Button
                    OutlinedButton(
                        onClick = onBlockAuthorOnly,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("comment_block_only_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sadece ${comment.authorName} Kullanıcısını Engelle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Vazgeç", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
