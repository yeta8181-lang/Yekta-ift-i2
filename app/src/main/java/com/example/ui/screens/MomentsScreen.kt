package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.MomentComment
import com.example.data.model.MomentEntity
import com.example.ui.components.AvatarWithBadge
import com.example.ui.components.CommentReportBlockDialog
import com.example.ui.components.MediaViewerDialog
import com.example.ui.theme.CupidGold
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SafeShield
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MomentsScreen(
    moments: List<MomentEntity>,
    currentUserId: String = "",
    isVip: Boolean = false,
    isAdmin: Boolean = false,
    onToggleLike: (MomentEntity) -> Unit,
    onCommentMoment: (MomentEntity, String) -> Unit = { _, _ -> },
    onDeleteComment: (MomentEntity, String) -> Unit = { _, _ -> },
    onDeleteMoment: (MomentEntity) -> Unit = {},
    onReportComment: (MomentEntity, MomentComment, String, Boolean) -> Unit = { _, _, _, _ -> },
    onBlockCommentAuthor: (MomentEntity, MomentComment) -> Unit = { _, _ -> },
    onPublishMoment: (caption: String, mediaUri: String, mediaType: String, location: String) -> Unit,
    onOpenVipPaywall: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val canPostOrComment = isVip || isAdmin
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedMediaPreview by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightSurface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Keşfet Anlar & Paylaşımlar ✨",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Düşüncelerinizi paylaşın, isteğe bağlı fotoğraf ekleyin",
                        fontSize = 12.5.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (canPostOrComment) {
                            showCreateDialog = true
                        } else {
                            onOpenVipPaywall()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPostOrComment) RoseAccent else CupidGold
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("add_moment_top_button")
                ) {
                    Icon(
                        imageVector = if (canPostOrComment) Icons.Default.Add else Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (canPostOrComment) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (canPostOrComment) "Paylaş" else "Paylaş (VIP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (canPostOrComment) Color.White else Color.Black
                    )
                }
            }

            // Non-VIP Restriction Banner
            if (!canPostOrComment) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CupidGold.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onOpenVipPaywall() }
                        .testTag("moments_vip_notice_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Paylaşım ve Yorum Sadece VIP Üyelere Özeldir 👑",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CupidGold
                            )
                            Text(
                                text = "Keşfette gönderi paylaşmak ve paylaşımlara yorum yapmak için VIP üyeliğe geçin.",
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            if (moments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = NightCard,
                            border = BorderStroke(1.dp, NightCardBorder),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = CupidGold,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Henüz Keşfet Paylaşımı Yok",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Keşfette ilk paylaşımı sen yap, üyeler beğensin ve yorum yapsın!",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Moments Feed
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(moments, key = { it.id }) { moment ->
                        MomentFeedCard(
                            moment = moment,
                            currentUserId = currentUserId,
                            isVip = canPostOrComment,
                            isAdmin = isAdmin,
                            onLikeClick = { onToggleLike(moment) },
                            onCommentSubmit = { text -> onCommentMoment(moment, text) },
                            onDeleteComment = { commentId -> onDeleteComment(moment, commentId) },
                            onDeleteMoment = { onDeleteMoment(moment) },
                            onReportComment = { comment, reason, blockAuthor ->
                                onReportComment(moment, comment, reason, blockAuthor)
                            },
                            onBlockCommentAuthor = { comment ->
                                onBlockCommentAuthor(moment, comment)
                            },
                            onOpenVipPaywall = onOpenVipPaywall,
                            onMediaClick = {
                                if (moment.mediaUri.isNotBlank()) {
                                    selectedMediaPreview = Pair(moment.mediaUri, moment.mediaType)
                                }
                            }
                        )
                    }
                }
            }
        }

        // New Moment Dialog (Accessible when VIP or Admin)
        if (showCreateDialog && canPostOrComment) {
            CreateMomentDialog(
                onDismiss = { showCreateDialog = false },
                onPublish = { caption, uri, type, location ->
                    onPublishMoment(caption, uri, type, location)
                    showCreateDialog = false
                }
            )
        }

        // Media Fullscreen Viewer Dialog
        selectedMediaPreview?.let { (uri, type) ->
            MediaViewerDialog(
                mediaUri = uri,
                mediaType = type,
                title = "Paylaşılan Görsel",
                onDismiss = { selectedMediaPreview = null }
            )
        }
    }
}

@Composable
internal fun MomentFeedCard(
    moment: MomentEntity,
    currentUserId: String = "",
    isVip: Boolean,
    isAdmin: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentSubmit: (String) -> Unit,
    onDeleteComment: (String) -> Unit = {},
    onDeleteMoment: () -> Unit = {},
    onReportComment: (MomentComment, String, Boolean) -> Unit = { _, _, _ -> },
    onBlockCommentAuthor: (MomentComment) -> Unit = {},
    onOpenVipPaywall: () -> Unit,
    onMediaClick: () -> Unit
) {
    val comments = moment.commentList
    val isPostOwner = currentUserId.isNotBlank() && moment.authorId == currentUserId
    val canDeletePost = isAdmin || isPostOwner
    var commentDraft by remember(moment.id) { mutableStateOf("") }
    var showCommentsSection by remember(moment.id) { mutableStateOf(true) }
    var commentToModerate by remember(moment.id) { mutableStateOf<MomentComment?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()) }
    val timeString = remember(moment.timestamp) { dateFormat.format(Date(moment.timestamp)) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        border = BorderStroke(1.dp, NightCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_card_${moment.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Author Avatar, Name, Timestamp & Admin/Owner Delete Button (No Location)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AvatarWithBadge(
                        imageUrl = moment.authorAvatar,
                        size = 44.dp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = moment.authorName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CupidGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "VIP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CupidGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = timeString,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (canDeletePost) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF3F1D2B),
                        border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable { onDeleteMoment() }
                            .testTag("delete_moment_${moment.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Gönderiyi Sil",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAdmin && !isPostOwner) "Yönetici Sil" else "Sil",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption / Text Content
            if (moment.caption.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF191229),
                    border = BorderStroke(1.dp, NightCardBorder.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = moment.caption,
                        fontSize = 14.5.sp,
                        color = Color.White,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Optional Media (Only rendered if user attached a photo/video)
            if (moment.mediaUri.isNotBlank() && moment.mediaType != "NONE") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onMediaClick() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = moment.mediaUri,
                        contentDescription = moment.caption,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (moment.mediaType == "VIDEO") {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video Oynat",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Actions: Like button & count (saved to Firebase) + Comment toggle & count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.testTag("moment_like_${moment.id}")
                    ) {
                        Icon(
                            imageVector = if (moment.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (moment.isLiked) RoseAccent else Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "${moment.likesCount} beğeni",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (moment.isLiked) RoseAccent else Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCommentsSection = !showCommentsSection }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("moment_comments_toggle_${moment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Yorumlar",
                            tint = CupidGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${comments.size} yorum",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CupidGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = CupidGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "VIP Yorum",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CupidGold
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showCommentsSection) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = NightCardBorder,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Community Moderation & Post Owner Info Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SafeShield.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, SafeShield.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SafeShield,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isAdmin -> "Yönetici Modu Aktif: Tüm gönderileri ve yorumları silebilirsiniz."
                                    isPostOwner -> "Paylaşım Sahibi Moderasyonu: Yorumları silebilir, bildirebilir veya kullanıcıyı engelleyebilirsiniz."
                                    else -> "Güvenli Topluluk: Rahatsız edici yorumları 'Bildir / Engelle' ile bize iletebilirsiniz."
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Existing Comments List
                    if (comments.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            comments.forEach { comment ->
                                val isCommentAuthor = currentUserId.isNotBlank() && comment.authorId == currentUserId
                                MomentCommentRow(
                                    comment = comment,
                                    isPostOwner = isPostOwner || isAdmin,
                                    isCommentAuthor = isCommentAuthor,
                                    onModerateClick = { commentToModerate = comment },
                                    onQuickDeleteClick = { onDeleteComment(comment.id) }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Henüz yorum yapılmamış. İlk VIP yorumunu sen bırak!",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // VIP-Gated Comment Input Area
                    if (isVip || isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentDraft,
                                onValueChange = { commentDraft = it },
                                placeholder = {
                                    Text(
                                        text = "Yorum yaz...",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CupidGold,
                                    unfocusedBorderColor = NightCardBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF191229),
                                    unfocusedContainerColor = Color(0xFF191229)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("moment_comment_input_${moment.id}")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = if (commentDraft.isNotBlank()) CupidGold else NightCardBorder,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clickable(enabled = commentDraft.isNotBlank()) {
                                        val clean = commentDraft.trim()
                                        if (clean.isNotEmpty()) {
                                            onCommentSubmit(clean)
                                            commentDraft = ""
                                        }
                                    }
                                    .testTag("moment_comment_send_${moment.id}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Yorum Gönder",
                                        tint = if (commentDraft.isNotBlank()) Color.Black else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Locked Comment Bar for Non-VIP Users
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF231834),
                            border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVipPaywall() }
                                .testTag("moment_comment_vip_lock_${moment.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "VIP Kilit",
                                        tint = CupidGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Paylaşımlara sadece VIP üyeler yorum yapabilir",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = CupidGold
                                ) {
                                    Text(
                                        text = "VIP Ol 👑",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    commentToModerate?.let { targetComment ->
        val isCommentAuthor = currentUserId.isNotBlank() && targetComment.authorId == currentUserId
        CommentReportBlockDialog(
            comment = targetComment,
            moment = moment,
            isPostOwner = isPostOwner || isAdmin,
            isCommentAuthor = isCommentAuthor,
            onDismiss = { commentToModerate = null },
            onDeleteComment = {
                onDeleteComment(targetComment.id)
                commentToModerate = null
            },
            onBlockAuthorOnly = {
                onBlockCommentAuthor(targetComment)
                commentToModerate = null
            },
            onReportComment = { reason, alsoBlock ->
                onReportComment(targetComment, reason, alsoBlock)
                commentToModerate = null
            }
        )
    }
}

@Composable
private fun MomentCommentRow(
    comment: MomentComment,
    isPostOwner: Boolean,
    isCommentAuthor: Boolean,
    onModerateClick: () -> Unit,
    onQuickDeleteClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeText = remember(comment.timestamp) { timeFormat.format(Date(comment.timestamp)) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF191229),
        border = BorderStroke(1.dp, NightCardBorder.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("moment_comment_row_${comment.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            AvatarWithBadge(
                imageUrl = comment.authorAvatar,
                size = 30.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = comment.authorName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CupidGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "VIP",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = CupidGold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = timeText,
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isPostOwner || isCommentAuthor) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF281C3D),
                                border = BorderStroke(1.dp, CupidGold.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clickable { onQuickDeleteClick() }
                                    .testTag("comment_quick_delete_${comment.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Yorumu Sil",
                                        tint = CupidGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Sil",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CupidGold
                                    )
                                }
                            }
                        }

                        if (!isCommentAuthor) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = RoseAccent.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, RoseAccent.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clickable { onModerateClick() }
                                    .testTag("comment_moderate_${comment.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = "Yorumu Bildir veya Engelle",
                                        tint = RoseAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Bildir / Engelle",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDA4AF)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = comment.text,
                    fontSize = 12.5.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
internal fun CreateMomentDialog(
    onDismiss: () -> Unit,
    onPublish: (caption: String, uri: String, type: String, location: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    // Photo starts null by default (no mandatory photo and no location)
    var selectedUri by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it.toString()
        }
    }

    val canSubmit = caption.trim().isNotEmpty() || !selectedUri.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    text = "Keşfette Yeni Paylaşım Yap",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Paylaşım metniniz / düşünceniz:") },
                    placeholder = { Text("Ne düşünüyorsun? Buraya yaz...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("moment_caption_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Fotoğraf (İsteğe Bağlı):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF372C54)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_optional_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, tint = CupidGold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedUri == null) "İsteğe Bağlı Fotoğraf Ekle" else "Fotoğrafı Değiştir",
                            fontSize = 12.sp
                        )
                    }

                    if (selectedUri != null) {
                        IconButton(
                            onClick = { selectedUri = null },
                            modifier = Modifier.testTag("remove_optional_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fotoğrafı Kaldır",
                                tint = RoseAccent
                            )
                        }
                    }
                }

                // Only show preview if the user explicitly chose a photo
                selectedUri?.let { chosenUri ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = chosenUri,
                            contentDescription = "Seçilen Fotoğraf",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalUri = selectedUri.orEmpty()
                    val finalType = if (finalUri.isNotBlank()) "IMAGE" else "NONE"
                    onPublish(caption.trim(), finalUri, finalType, "")
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = RoseAccent),
                modifier = Modifier.testTag("publish_moment_confirm_button")
            ) {
                Text("Paylaş", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç", color = Color.Gray)
            }
        }
    )
}
