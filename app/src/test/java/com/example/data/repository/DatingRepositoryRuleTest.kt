package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.firebase.FirebaseManager
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DatingRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun createRepository(): DatingRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = FirebaseManager(firestore, auth)
        return DatingRepository(context = context, firebaseManager = manager)
    }

    private fun buildSampleProfile(uid: String, name: String, email: String): UserProfileEntity {
        return UserProfileEntity(
            id = uid,
            name = name,
            age = 26,
            city = "İstanbul",
            bio = "Kahve ve fotoğraf tutkunu",
            occupation = "Tasarımcı",
            education = "Boğaziçi Üniversitesi",
            heightCm = 172,
            zodiac = "Terazi ♎",
            lookingFor = "Ciddi İlişki ❤️",
            photosJson = "https://example.com/photo1.jpg",
            interestsJson = "Kahve ☕|||Müzik 🎵",
            lifestyleJson = "Sigara İçmiyor|||Spor Yapıyor",
            promptQuestion = "İdeal ilk buluşma",
            promptAnswer = "Sahilde yürüyüş ve kahve",
            isVerified = true,
            isVip = false,
            email = email,
            isCurrentUser = true,
            isProfileSetupCompleted = true
        )
    }

    @Test
    fun initializeUserProfileOnRegistration_setsBioInterestsAndGeolocation() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = createRepository()
        val expectedInterests = listOf("Kahve ☕", "Müzik 🎵", "Seyahat ✈️")
        val expectedBio = "Yeni yerler keşfetmeyi ve samimi sohbetleri severim."
        val expectedLat = 38.4237
        val expectedLng = 27.1428

        val initResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.initializeUserProfileOnRegistration(
                name = "Alice Yılmaz",
                email = ALICE_EMAIL,
                bio = expectedBio,
                interests = expectedInterests,
                latitude = expectedLat,
                longitude = expectedLng,
                city = "İzmir",
                isProfileSetupCompleted = true
            )
        }
        assertTrue(initResult.isSuccess)
        val createdEntity = initResult.getOrThrow()
        assertEquals(aliceUid, createdEntity.id)
        assertEquals(expectedBio, createdEntity.bio)
        assertEquals(expectedInterests, createdEntity.interestList)
        assertEquals(expectedLat, createdEntity.latitude, 0.0001)
        assertEquals(expectedLng, createdEntity.longitude, 0.0001)

        // Verify private document in /users/{uid} has bio, interests, and geolocation GeoPoint
        val privateSnap = withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(aliceUid).get().await()
        }
        assertTrue(privateSnap.exists())
        assertEquals(expectedBio, privateSnap.getString("bio"))
        assertEquals(expectedInterests, privateSnap.get("interests"))
        val privateGeo = privateSnap.getGeoPoint("geolocation")
        assertNotNull(privateGeo)
        assertEquals(expectedLat, privateGeo!!.latitude, 0.0001)
        assertEquals(expectedLng, privateGeo.longitude, 0.0001)

        // Verify public document in /public_profiles/{uid} via fetchUserProfileFromCloud
        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchUserProfileFromCloud(aliceUid)
        }
        assertNotNull(fetched)
        assertEquals(expectedBio, fetched?.bio)
        assertEquals(expectedInterests, fetched?.interestList)
        assertEquals(expectedLat, fetched?.latitude ?: 0.0, 0.0001)
        assertEquals(expectedLng, fetched?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun saveAndFetchUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = createRepository()
        val profile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfileToCloud(profile)
        }
        assertTrue(saveResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.fetchUserProfileFromCloud(aliceUid)
        }
        assertNotNull(fetched)
        assertEquals("Alice", fetched?.name)
        assertEquals(ALICE_EMAIL, fetched?.email)
    }

    @Test
    fun privateUserRecord_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = createRepository()
        val profile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserProfileToCloud(profile).getOrThrow()
        }

        // Bob signs in and attempts to read Alice's private PII document (/users/{aliceUid})
        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED when reading another user's private PII")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observePublicProfiles_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = createRepository()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePublicProfilesFromCloud("unauth_user").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated public_profiles query")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun createAndObserveMoment_authenticatedUser_emitsMoment() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = createRepository()
        val momentId = "moment_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val moment = MomentEntity(
            id = momentId,
            authorId = aliceUid,
            authorName = "Alice",
            authorAvatar = "https://example.com/avatar.jpg",
            mediaUri = "https://example.com/moment.jpg",
            mediaType = "IMAGE",
            caption = "Boğaz manzarası",
            location = "Bebek, İstanbul",
            likesCount = 0
        )

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMomentInCloud(moment)
        }
        assertTrue(createRes.isSuccess)

        val emittedMoments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMomentsFromCloud().first { list -> list.any { it.id == momentId } }
        }
        assertTrue(emittedMoments.any { it.id == momentId && it.caption == "Boğaz manzarası" })
    }

    @Test
    fun textOnlyMomentWithoutLocation_andToggleMomentLikeInFirebase_persistsLikedState() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val manager = FirebaseManager(firestore, auth)
        val repository = DatingRepository(context = ApplicationProvider.getApplicationContext(), firebaseManager = manager)
        val momentId = "moment_text_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val textOnlyMoment = MomentEntity(
            id = momentId,
            authorId = aliceUid,
            authorName = "Alice",
            authorAvatar = "https://example.com/avatar.jpg",
            mediaUri = "",
            mediaType = "NONE",
            caption = "Fotoğrafsız ve konumsuz paylaşım!",
            location = "",
            likesCount = 0
        )

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createMomentInCloud(textOnlyMoment)
        }
        assertTrue(createRes.isSuccess)

        // Toggle like in Firebase via FirebaseManager
        val likedRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            manager.toggleCloudMomentLike(
                momentId = momentId,
                userId = aliceUid,
                isLiked = true,
                fallbackCount = 1
            )
        }
        assertTrue(likedRes.isSuccess)

        val likedMoments = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMomentsFromCloud().first { list ->
                list.any { it.id == momentId && it.likesCount == 1 && it.isLiked }
            }
        }
        val synced = likedMoments.first { it.id == momentId }
        assertEquals(1, synced.likesCount)
        assertTrue(synced.isLiked)
        assertEquals("", synced.location)
        assertEquals("", synced.mediaUri)
    }

    @Test
    fun saveCloudLike_andListenIncomingLikes_emitsUsersWhoLikedMe() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceManager = FirebaseManager(firestore, auth)
        val bobUid = signInTestUser(BOB_EMAIL)

        // Sign back in as Alice and like Bob in Firebase
        signInTestUser(ALICE_EMAIL)
        val aliceProfile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)
        val savedRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceManager.saveCloudLike(
                myUserId = aliceUid,
                partnerId = bobUid,
                isSuperLike = true,
                senderProfile = aliceProfile
            )
        }
        assertTrue(savedRes.isSuccess)

        // Sign in as Bob and verify incoming like is received from Firebase
        signInTestUser(BOB_EMAIL)
        val bobManager = FirebaseManager(firestore, auth)
        val incomingLikes = withTimeout(FLOW_TIMEOUT_MS) {
            bobManager.listenToIncomingLikes(bobUid).first { list ->
                list.any { it.fromUid == aliceUid }
            }
        }
        val likeFromAlice = incomingLikes.first { it.fromUid == aliceUid }
        assertEquals("Alice", likeFromAlice.fromName)
        assertTrue(likeFromAlice.isSuperLike)
    }

    @Test
    fun adminAccount_yeta8181_canDeleteMomentsAndUsersInFirebase() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = createRepository()
        val aliceProfile = buildSampleProfile(aliceUid, "Alice", ALICE_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveUserProfileToCloud(aliceProfile).getOrThrow()
        }
        val momentId = "moment_del_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createMomentInCloud(
                MomentEntity(
                    id = momentId,
                    authorId = aliceUid,
                    authorName = "Alice",
                    authorAvatar = "https://example.com/avatar.jpg",
                    mediaUri = "",
                    mediaType = "NONE",
                    caption = "Silinecek gönderi",
                    location = ""
                )
            ).getOrThrow()
        }

        // Sign in as Admin (yeta8181@gmail.com)
        signInTestUser(ADMIN_EMAIL)
        val adminManager = FirebaseManager(firestore, auth)
        assertTrue(adminManager.isCurrentAuthUserAdmin())

        val momentDeleted = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminManager.deleteCloudMoment(momentId)
        }
        assertTrue(momentDeleted.isSuccess)

        val userDeleted = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminManager.deleteCloudUser(aliceUid)
        }
        assertTrue(userDeleted.isSuccess)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val ADMIN_EMAIL = "yeta8181@gmail.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
