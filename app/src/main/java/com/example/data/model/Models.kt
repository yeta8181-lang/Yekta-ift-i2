package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

const val ADMIN_EMAIL = "yeta8181@gmail.com"

fun isAdminEmail(email: String?): Boolean {
    return email?.trim()?.equals(ADMIN_EMAIL, ignoreCase = true) == true
}

data class CloudLikeRecord(
    val fromUid: String,
    val toUid: String,
    val isSuperLike: Boolean = false,
    val fromName: String = "",
    val fromAvatar: String = "",
    val fromAge: Int = 24,
    val fromCity: String = "İstanbul",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val bio: String,
    val occupation: String,
    val education: String,
    val heightCm: Int,
    val zodiac: String,
    val lookingFor: String,
    val photosJson: String, // comma separated URLs / local URIs
    val interestsJson: String, // comma separated tags
    val lifestyleJson: String, // comma separated lifestyle tags
    val promptQuestion: String,
    val promptAnswer: String,
    val isVerified: Boolean = true,
    val latitude: Double = 41.0082,
    val longitude: Double = 28.9784,
    val distanceKm: Double = 1.2,
    val isOnline: Boolean = true,
    val isMatch: Boolean = false,
    val isLikedByMe: Boolean = false,
    val isSuperLiked: Boolean = false,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isCurrentUser: Boolean = false,
    val isVip: Boolean = false,
    val email: String = "",
    val isProfileSetupCompleted: Boolean = false
) {
    val isAdmin: Boolean
        get() = isAdminEmail(email)

    val photoList: List<String>
        get() = if (photosJson.isBlank()) emptyList() else photosJson.split("|||")

    val interestList: List<String>
        get() = if (interestsJson.isBlank()) emptyList() else interestsJson.split("|||")

    val lifestyleList: List<String>
        get() = if (lifestyleJson.isBlank()) emptyList() else lifestyleJson.split("|||")
}

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val senderId: String, // "me" or user id
    val text: String,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "IMAGE", "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true
)

data class MomentComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVip: Boolean = true
) {
    companion object {
        private const val ITEM_DELIM = "|||"
        private const val FIELD_DELIM = "~~~"

        fun parseList(raw: String): List<MomentComment> {
            if (raw.isBlank()) return emptyList()
            return raw.split(ITEM_DELIM).mapNotNull { entry ->
                val parts = entry.split(FIELD_DELIM)
                if (parts.size < 5) return@mapNotNull null
                MomentComment(
                    id = parts[0],
                    authorId = parts[1],
                    authorName = parts[2],
                    authorAvatar = parts[3],
                    text = parts[4],
                    timestamp = parts.getOrNull(5)?.toLongOrNull() ?: System.currentTimeMillis(),
                    isVip = true
                )
            }
        }

        fun serializeList(comments: List<MomentComment>): String {
            return comments.joinToString(ITEM_DELIM) { c ->
                val safeName = c.authorName.replace(FIELD_DELIM, " ").replace(ITEM_DELIM, " ")
                val safeAvatar = c.authorAvatar.replace(FIELD_DELIM, "").replace(ITEM_DELIM, "")
                val safeText = c.text.replace(FIELD_DELIM, " ").replace(ITEM_DELIM, " ")
                listOf(c.id, c.authorId, safeName, safeAvatar, safeText, c.timestamp.toString())
                    .joinToString(FIELD_DELIM)
            }
        }
    }
}

@Entity(tableName = "moments")
data class MomentEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val mediaUri: String,
    val mediaType: String, // "IMAGE", "VIDEO", or "NONE"
    val caption: String,
    val location: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val commentList: List<MomentComment>
        get() = MomentComment.parseList(commentsJson)
}
