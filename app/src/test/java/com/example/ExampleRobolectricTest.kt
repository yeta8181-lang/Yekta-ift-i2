package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.MomentComment
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.DatingRepository
import com.example.ui.components.DiscoverFilterState
import com.example.ui.components.DiscoverSortOption
import com.example.ui.screens.formatDurationSeconds
import com.example.ui.screens.parseVoiceDurationSeconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kalp Kalbe", appName)
    }

    @Test
    fun `report and block comment removes comment and blocks author`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = DatingRepository(context, db)

        val comment1 = MomentComment(
            id = "cmt_1",
            authorId = "spammer_user",
            authorName = "Spammer",
            authorAvatar = "",
            text = "Uygunsuz reklam yorumu",
            timestamp = 1000L
        )
        val comment2 = MomentComment(
            id = "cmt_2",
            authorId = "friend_user",
            authorName = "Zeynep",
            authorAvatar = "",
            text = "Harika paylaşım!",
            timestamp = 2000L
        )

        val moment = MomentEntity(
            id = "moment_test_1",
            authorId = "owner_user",
            authorName = "Elif",
            authorAvatar = "",
            mediaUri = "https://example.com/photo.jpg",
            mediaType = "IMAGE",
            caption = "Güzel bir gün",
            location = "İstanbul",
            commentsJson = MomentComment.serializeList(listOf(comment1, comment2))
        )
        db.momentDao().insertMoment(moment)

        // Report comment1 and block its author
        repository.reportComment(
            moment = moment,
            comment = comment1,
            reason = "Spam, Reklam veya Dolandırıcılık",
            blockAuthor = true
        )

        val updatedMoments = repository.getAllMoments().first()
        val updatedMoment = updatedMoments.first { it.id == "moment_test_1" }
        assertEquals(1, updatedMoment.commentList.size)
        assertEquals("cmt_2", updatedMoment.commentList.first().id)
        assertTrue(repository.blockedUserIds.value.contains("spammer_user"))
        assertTrue(repository.reportedCommentIds.value.contains("cmt_1"))

        // Post owner deletes remaining comment
        repository.deleteCommentFromMoment(updatedMoment, "cmt_2")
        val finalMoments = repository.getAllMoments().first()
        val finalMoment = finalMoments.first { it.id == "moment_test_1" }
        assertTrue(finalMoment.commentList.isEmpty())

        db.close()
    }

    @Test
    fun `advanced discover filter filters and sorts profiles accurately`() {
        val p1 = UserProfileEntity(
            id = "u1",
            name = "Selin",
            age = 24,
            city = "İstanbul",
            bio = "Kahve ve müzik tutkunu",
            occupation = "Mimar",
            education = "İTÜ",
            heightCm = 168,
            zodiac = "Terazi",
            lookingFor = "Uzun Süreli İlişki",
            photosJson = "https://example.com/1.jpg",
            interestsJson = "Kahve|||Müzik|||Seyahat",
            lifestyleJson = "Sosyal",
            promptQuestion = "En sevdiğim pazar:",
            promptAnswer = "Boğaz yürüyüşü",
            isVerified = true,
            distanceKm = 5.0,
            isOnline = true
        )
        val p2 = UserProfileEntity(
            id = "u2",
            name = "Merve",
            age = 31,
            city = "Ankara",
            bio = "Kitap ve sinema",
            occupation = "Avukat",
            education = "Ankara Üni",
            heightCm = 175,
            zodiac = "Aslan",
            lookingFor = "Kahve & Sohbet",
            photosJson = "",
            interestsJson = "Sinema|||Kitap",
            lifestyleJson = "Sakin",
            promptQuestion = "Hayalim:",
            promptAnswer = "Dünya turu",
            isVerified = false,
            distanceKm = 40.0,
            isOnline = false
        )

        val filter = DiscoverFilterState(
            minAge = 20,
            maxAge = 27,
            maxDistanceKm = 15,
            selectedCity = "İstanbul",
            selectedZodiacs = setOf("Terazi"),
            onlyVerified = true,
            onlyOnline = true,
            onlyWithPhoto = true,
            sortOption = DiscoverSortOption.NEAREST_DISTANCE
        )

        val result = filter.filterAndSort(listOf(p1, p2), null)
        assertEquals(1, result.size)
        assertEquals("Selin", result.first().name)
        assertTrue(filter.activeFilterCount >= 6)
    }

    @Test
    fun `chat supports sending voice messages and gallery photos`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = DatingRepository(context, db)

        val conversationId = "user_partner_1"

        // Send a gallery photo message with caption
        repository.sendMessage(
            conversationId = conversationId,
            text = "Sahilden bir kare 📸",
            mediaUri = "file:///data/user/0/com.example/files/chat_media/gallery_photo_1.jpg",
            mediaType = "IMAGE"
        )

        // Send a voice message with duration metadata
        repository.sendMessage(
            conversationId = conversationId,
            text = "🎤 Sesli Mesaj (0:08)",
            mediaUri = "file:///data/user/0/com.example/files/chat_media/voice_1.m4a?duration=8",
            mediaType = "AUDIO"
        )

        val messages = repository.getMessagesForConversation(conversationId).first()
        assertEquals(2, messages.size)

        val photoMsg = messages[0]
        assertEquals("IMAGE", photoMsg.mediaType)
        assertEquals("Sahilden bir kare 📸", photoMsg.text)

        val voiceMsg = messages[1]
        assertEquals("AUDIO", voiceMsg.mediaType)
        assertEquals(8, parseVoiceDurationSeconds(voiceMsg.mediaUri, voiceMsg.text))
        assertEquals("0:08", formatDurationSeconds(8))

        db.close()
    }
}
