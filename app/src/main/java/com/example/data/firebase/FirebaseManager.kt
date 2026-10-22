package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CloudLikeRecord
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.isAdminEmail
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE,
    LIST,
    GET,
    WRITE
}

/**
 * Manages real-time online synchronization with Google Cloud Firestore and Firebase Auth.
 * Enforces strict separation between private PII (/users/{userId}) and public profiles (/public_profiles/{userId}).
 */
class FirebaseManager {
    var firestore: FirebaseFirestore? = null
        private set

    var auth: FirebaseAuth? = null
        private set

    private val _isCloudActive = MutableStateFlow(false)
    val isCloudActive: StateFlow<Boolean> = _isCloudActive.asStateFlow()

    constructor(context: Context) {
        initFirebase(context)
    }

    constructor(firestoreInstance: FirebaseFirestore, authInstance: FirebaseAuth) {
        this.firestore = firestoreInstance
        this.auth = authInstance
        this._isCloudActive.value = true
    }

    private fun initFirebase(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }

            if (app != null) {
                val dbId = try {
                    context.getString(R.string.firestore_database_id).trim()
                } catch (e: Exception) {
                    "(default)"
                }
                firestore = if (dbId.isNotEmpty() && dbId != "(default)") {
                    FirebaseFirestore.getInstance(app, dbId)
                } else {
                    FirebaseFirestore.getInstance(app)
                }
                auth = FirebaseAuth.getInstance(app)
                val projectId = app.options.projectId
                _isCloudActive.value = !projectId.isNullOrBlank() && projectId != "remixed-project-id"
            }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Firebase initialization skipped: ${e.message}")
            _isCloudActive.value = false
        }
    }

    fun isConfigured(): Boolean = firestore != null && _isCloudActive.value

    fun getCurrentUid(): String? = auth?.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth?.currentUser?.email

    fun isCurrentAuthUserAdmin(): Boolean = isAdminEmail(auth?.currentUser?.email)

    private fun requireUserId(): String {
        return auth?.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated for Firestore operations")
    }

    private fun handleFirestoreError(error: Exception, operationType: OperationType, path: String?) {
        val currentUser = auth?.currentUser
        val providerInfos = JSONArray()
        currentUser?.providerData?.forEach { profile ->
            val pObj = JSONObject()
            pObj.put("providerId", profile.providerId)
            pObj.put("displayName", profile.displayName)
            pObj.put("email", profile.email)
            pObj.put("photoUrl", profile.photoUrl?.toString())
            providerInfos.put(pObj)
        }

        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("isAnonymous", currentUser?.isAnonymous)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", providerInfos)
        }

        val errInfo = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.name.lowercase())
            put("path", path)
            put("authInfo", authInfo)
        }

        Log.e("FirestoreError", errInfo.toString(), error)
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<String> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("FirebaseAuth not initialized"))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val uid = authResult.user?.uid ?: throw IllegalStateException("Authenticated user UID is null")
            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Google credential sign-in failed", e)
            Result.failure(e)
        }
    }

    fun getConversationChannelId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
    }

    /**
     * Initializes a user profile document in Firestore upon successful registration,
     * including fields for bio, interests, and geolocation (stored both as a Firestore GeoPoint
     * and latitude/longitude numeric coordinates).
     *
     * Writes both `/users/{uid}` (private owner document with PII + bio, interests, geolocation)
     * and `/public_profiles/{uid}` (public discoverable profile with bio, interests, geolocation).
     */
    suspend fun initializeUserProfileInFirestore(
        name: String,
        email: String,
        bio: String = "",
        interests: List<String> = emptyList(),
        latitude: Double = 41.0082,
        longitude: Double = 28.9784,
        avatarUrl: String = "",
        age: Int = 24,
        city: String = "İstanbul",
        occupation: String = "",
        education: String = "",
        heightCm: Int = 170,
        zodiac: String = "Koç ♈",
        lookingFor: String = "Ciddi İlişki ❤️",
        isProfileSetupCompleted: Boolean = false
    ): Result<UserProfileEntity> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = requireUserId()

        val safeName = name.ifBlank { "Yeni Üye" }.trim().take(100)
        val safeEmail = email.ifBlank { auth?.currentUser?.email.orEmpty() }.trim().take(254)
        val isAdminUser = isAdminEmail(safeEmail) || isCurrentAuthUserAdmin()
        val safeBio = bio.trim().take(1000)
        val safeInterests = interests
            .map { it.trim().take(100) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(30)
        val safeInterestsJson = safeInterests.joinToString("|||").take(2000)
        val safeLat = latitude.coerceIn(-90.0, 90.0)
        val safeLng = longitude.coerceIn(-180.0, 180.0)
        val geoPoint = GeoPoint(safeLat, safeLng)
        val safeAge = age.coerceIn(18, 120)
        val safeCity = city.ifBlank { "İstanbul" }.trim().take(100)
        val safeHeight = heightCm.coerceIn(100, 250)
        val safeZodiac = zodiac.ifBlank { "Koç ♈" }.take(50)
        val safeLookingFor = lookingFor.ifBlank { "Ciddi İlişki ❤️" }.take(100)
        val safePhotosJson = avatarUrl.trim().take(4000)

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)

        return try {
            val existingPrivate = privateRef.get().await()
            val basePrivateData = mapOf(
                "userId" to uid,
                "name" to safeName,
                "email" to safeEmail,
                "bio" to safeBio,
                "interests" to safeInterests,
                "geolocation" to geoPoint,
                "latitude" to safeLat,
                "longitude" to safeLng,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existingPrivate.exists()) {
                privateRef.set(
                    basePrivateData + mapOf("createdAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                privateRef.update(basePrivateData).await()
            }

            val existingPublic = publicRef.get().await()
            val basePublicData = mapOf(
                "userId" to uid,
                "name" to safeName,
                "age" to safeAge,
                "city" to safeCity,
                "bio" to safeBio,
                "occupation" to occupation.trim().take(120),
                "education" to education.trim().take(120),
                "heightCm" to safeHeight,
                "zodiac" to safeZodiac,
                "lookingFor" to safeLookingFor,
                "photosJson" to safePhotosJson,
                "interestsJson" to safeInterestsJson,
                "interests" to safeInterests,
                "geolocation" to geoPoint,
                "lifestyleJson" to "Sosyal|||Pozitif",
                "promptQuestion" to "İdeal ilk buluşma",
                "promptAnswer" to "Güzel bir kahve ve samimi sohbet",
                "isVerified" to isAdminUser,
                "isVip" to isAdminUser,
                "latitude" to safeLat,
                "longitude" to safeLng,
                "isOnline" to true,
                "visibility" to "public",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existingPublic.exists()) {
                publicRef.set(
                    basePublicData + mapOf("createdAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                publicRef.update(basePublicData).await()
            }

            val initializedEntity = UserProfileEntity(
                id = uid,
                name = safeName,
                age = safeAge,
                city = safeCity,
                bio = safeBio,
                occupation = occupation.trim().take(120),
                education = education.trim().take(120),
                heightCm = safeHeight,
                zodiac = safeZodiac,
                lookingFor = safeLookingFor,
                photosJson = safePhotosJson,
                interestsJson = safeInterestsJson,
                lifestyleJson = "Sosyal|||Pozitif",
                promptQuestion = "İdeal ilk buluşma",
                promptAnswer = "Güzel bir kahve ve samimi sohbet",
                isVerified = isAdminUser,
                latitude = safeLat,
                longitude = safeLng,
                distanceKm = 0.0,
                isOnline = true,
                isCurrentUser = true,
                isVip = isAdminUser,
                email = safeEmail,
                isProfileSetupCompleted = isProfileSetupCompleted
            )
            Result.success(initializedEntity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, publicRef.path)
            Result.failure(e)
        }
    }

    // 1. Save / Sync Current User Profile to Cloud Firestore (both /users/{uid} and /public_profiles/{uid})
    suspend fun saveUserProfileToCloud(user: UserProfileEntity): Result<String> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = requireUserId()

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)

        val safeName = user.name.ifBlank { "Üye" }.take(100)
        val safeEmail = user.email.ifBlank { auth?.currentUser?.email.orEmpty() }.take(254)
        val isAdminUser = isAdminEmail(safeEmail) || isCurrentAuthUserAdmin()
        val safeBio = user.bio.take(1000)
        val safeInterests = user.interestList
            .map { it.trim().take(100) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(30)
        val safeInterestsJson = safeInterests.joinToString("|||").take(2000)
        val safeLat = user.latitude.coerceIn(-90.0, 90.0)
        val safeLng = user.longitude.coerceIn(-180.0, 180.0)
        val geoPoint = GeoPoint(safeLat, safeLng)

        return try {
            val existingPrivate = privateRef.get().await()
            val basePrivateFields = mapOf(
                "userId" to uid,
                "name" to safeName,
                "email" to safeEmail,
                "bio" to safeBio,
                "interests" to safeInterests,
                "geolocation" to geoPoint,
                "latitude" to safeLat,
                "longitude" to safeLng,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!existingPrivate.exists()) {
                privateRef.set(
                    basePrivateFields + mapOf("createdAt" to FieldValue.serverTimestamp())
                ).await()
            } else {
                privateRef.update(basePrivateFields).await()
            }

            val existingPublic = publicRef.get().await()
            val basePublicFields = mapOf(
                "userId" to uid,
                "name" to safeName,
                "age" to user.age.coerceIn(18, 120),
                "city" to user.city.take(100),
                "bio" to safeBio,
                "occupation" to user.occupation.take(120),
                "education" to user.education.take(120),
                "heightCm" to user.heightCm.coerceIn(100, 250),
                "zodiac" to user.zodiac.take(50),
                "lookingFor" to user.lookingFor.take(100),
                "photosJson" to user.photosJson.take(4000),
                "interestsJson" to safeInterestsJson,
                "interests" to safeInterests,
                "geolocation" to geoPoint,
                "lifestyleJson" to user.lifestyleJson.take(2000),
                "promptQuestion" to user.promptQuestion.take(300),
                "promptAnswer" to user.promptAnswer.take(1000),
                "isVerified" to (user.isVerified || isAdminUser),
                "isVip" to (user.isVip || isAdminUser),
                "latitude" to safeLat,
                "longitude" to safeLng,
                "isOnline" to true,
                "visibility" to "public",
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (!existingPublic.exists()) {
                publicRef.set(
                    basePublicFields + mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                publicRef.update(basePublicFields).await()
            }

            Result.success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, publicRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchUserProfile(userId: String): UserProfileEntity? {
        val db = firestore ?: return null
        val currentUid = auth?.currentUser?.uid
        val publicRef = db.collection("public_profiles").document(userId)

        return try {
            val publicDoc = publicRef.get().await()
            if (!publicDoc.exists()) return null

            var email = ""
            if (currentUid != null && currentUid == userId) {
                val privateDoc = db.collection("users").document(userId).get().await()
                if (privateDoc.exists()) {
                    email = privateDoc.getString("email") ?: ""
                }
            }
            docToUserProfileEntity(publicDoc, email, isCurrentUser = (currentUid == userId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, publicRef.path)
            throw e
        }
    }

    fun syncUserProfile(user: UserProfileEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)

        val safeName = user.name.ifBlank { "Üye" }.take(100)
        val safeEmail = user.email.ifBlank { auth?.currentUser?.email.orEmpty() }.take(254)
        val isAdminUser = isAdminEmail(safeEmail) || isCurrentAuthUserAdmin()
        val safeBio = user.bio.take(1000)
        val safeInterests = user.interestList
            .map { it.trim().take(100) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(30)
        val safeInterestsJson = safeInterests.joinToString("|||").take(2000)
        val safeLat = user.latitude.coerceIn(-90.0, 90.0)
        val safeLng = user.longitude.coerceIn(-180.0, 180.0)
        val geoPoint = GeoPoint(safeLat, safeLng)

        privateRef.get()
            .addOnSuccessListener { existing ->
                val basePrivateFields = mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "email" to safeEmail,
                    "bio" to safeBio,
                    "interests" to safeInterests,
                    "geolocation" to geoPoint,
                    "latitude" to safeLat,
                    "longitude" to safeLng,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val task = if (!existing.exists()) {
                    privateRef.set(basePrivateFields + mapOf("createdAt" to FieldValue.serverTimestamp()))
                } else {
                    privateRef.update(basePrivateFields)
                }
                task.addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, privateRef.path)
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.GET, privateRef.path)
            }

        publicRef.get()
            .addOnSuccessListener { existing ->
                val basePublicFields = mapOf(
                    "name" to safeName,
                    "age" to user.age.coerceIn(18, 120),
                    "city" to user.city.take(100),
                    "bio" to safeBio,
                    "occupation" to user.occupation.take(120),
                    "education" to user.education.take(120),
                    "heightCm" to user.heightCm.coerceIn(100, 250),
                    "zodiac" to user.zodiac.take(50),
                    "lookingFor" to user.lookingFor.take(100),
                    "photosJson" to user.photosJson.take(4000),
                    "interestsJson" to safeInterestsJson,
                    "interests" to safeInterests,
                    "geolocation" to geoPoint,
                    "lifestyleJson" to user.lifestyleJson.take(2000),
                    "promptQuestion" to user.promptQuestion.take(300),
                    "promptAnswer" to user.promptAnswer.take(1000),
                    "isVerified" to (user.isVerified || isAdminUser),
                    "isVip" to (user.isVip || isAdminUser),
                    "latitude" to safeLat,
                    "longitude" to safeLng,
                    "isOnline" to true,
                    "visibility" to "public",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                val task = if (!existing.exists()) {
                    publicRef.set(
                        basePublicFields + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    publicRef.update(basePublicFields)
                }
                task.addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, publicRef.path)
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.GET, publicRef.path)
            }
    }

    // 2. Real-time Listen to Other Public Profiles from Cloud Firestore
    fun listenToCloudUsers(currentUserId: String): Flow<List<UserProfileEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "public_profiles"
        val listener = db.collection(path)
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("userId") ?: doc.id
                        if (id == currentUserId || id == "current_user_me" ||
                            id.startsWith("user_elif") || id.startsWith("user_deniz") ||
                            id.startsWith("user_zeynep") || id.startsWith("user_selin") ||
                            id.startsWith("user_mert") || id.startsWith("user_can") ||
                            id.startsWith("user_aylin") || id.startsWith("demo_") ||
                            id.startsWith("seed_")
                        ) {
                            return@mapNotNull null
                        }
                        docToUserProfileEntity(doc, email = "", isCurrentUser = false)
                    }
                    trySend(users)
                }
            }

        awaitClose { listener.remove() }
    }

    // 3. Send Message to Cloud Firestore
    fun sendCloudMessage(channelId: String, senderId: String, partnerId: String, message: ChatMessageEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: senderId
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val msgRef = db.collection("conversations")
            .document(channelId)
            .collection("messages")
            .document(msgId)

        val rawMap = mapOf<String, Any?>(
            "channelId" to channelId,
            "senderId" to uid,
            "participantIds" to listOf(uid, partnerId).sorted(),
            "text" to message.text.take(2000),
            "mediaUri" to message.mediaUri,
            "mediaType" to message.mediaType,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        msgRef.set(rawMap)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            }
    }

    // 4. Real-time Listen to Chat Messages in a Conversation
    fun listenToCloudMessages(channelId: String, myUserId: String, partnerId: String): Flow<List<ChatMessageEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = auth?.currentUser?.uid ?: myUserId
        val path = "conversations/$channelId/messages"
        val listener = db.collection("conversations")
            .document(channelId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.map { doc ->
                        val remoteSenderId = doc.getString("senderId") ?: "unknown"
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        val stableId = (doc.id.hashCode().toLong() and 0x7FFFFFFF) + 1000L

                        ChatMessageEntity(
                            id = stableId,
                            conversationId = partnerId,
                            senderId = if (remoteSenderId == uid || remoteSenderId == "me") "me" else partnerId,
                            text = doc.getString("text") ?: "",
                            mediaUri = doc.getString("mediaUri"),
                            mediaType = doc.getString("mediaType"),
                            timestamp = timestamp
                        )
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
            }

        awaitClose { listener.remove() }
    }

    // 5. Publish Moment / Story to Cloud Firestore (supports optional photo and no location)
    suspend fun createCloudMoment(moment: MomentEntity): Result<String> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = requireUserId()
        val momentId = moment.id.ifBlank { "moment_${UUID.randomUUID().toString().replace("-", "").take(20)}" }
        val docRef = db.collection("moments").document(momentId)
        val normalizedMediaType = when {
            moment.mediaUri.isBlank() -> "NONE"
            moment.mediaType == "VIDEO" -> "VIDEO"
            else -> "IMAGE"
        }
        val momentMap = mapOf(
            "id" to momentId,
            "authorId" to uid,
            "authorName" to moment.authorName.ifBlank { "Üye" }.take(100),
            "authorAvatar" to moment.authorAvatar.take(2000),
            "mediaUri" to moment.mediaUri.take(2000),
            "mediaType" to normalizedMediaType,
            "caption" to moment.caption.take(1000),
            "location" to moment.location.take(200),
            "likesCount" to moment.likesCount.coerceAtLeast(0),
            "likedUserIds" to if (moment.isLiked) listOf(uid) else emptyList<String>(),
            "commentsJson" to moment.commentsJson.take(4000),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(momentMap).await()
            Result.success(momentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun publishCloudMoment(moment: MomentEntity) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: return
        val momentId = moment.id.ifBlank { "moment_${UUID.randomUUID().toString().replace("-", "").take(20)}" }
        val docRef = db.collection("moments").document(momentId)
        val normalizedMediaType = when {
            moment.mediaUri.isBlank() -> "NONE"
            moment.mediaType == "VIDEO" -> "VIDEO"
            else -> "IMAGE"
        }
        val momentMap = mapOf(
            "id" to momentId,
            "authorId" to uid,
            "authorName" to moment.authorName.ifBlank { "Üye" }.take(100),
            "authorAvatar" to moment.authorAvatar.take(2000),
            "mediaUri" to moment.mediaUri.take(2000),
            "mediaType" to normalizedMediaType,
            "caption" to moment.caption.take(1000),
            "location" to moment.location.take(200),
            "likesCount" to moment.likesCount.coerceAtLeast(0),
            "likedUserIds" to if (moment.isLiked) listOf(uid) else emptyList<String>(),
            "commentsJson" to moment.commentsJson.take(4000),
            "visibility" to "public",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(momentMap)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            }
    }

    // 6. Toggle and Save Moment Like in Cloud Firestore (with likedUserIds + likesCount)
    suspend fun toggleCloudMomentLike(momentId: String, userId: String, isLiked: Boolean, fallbackCount: Int): Result<Int> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val activeUid = auth?.currentUser?.uid ?: userId
        val docRef = db.collection("moments").document(momentId)
        return try {
            val snap = docRef.get().await()
            val existingLikedIds = (snap.get("likedUserIds") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: mutableListOf()

            if (isLiked) {
                if (!existingLikedIds.contains(activeUid)) {
                    existingLikedIds.add(activeUid)
                }
            } else {
                existingLikedIds.remove(activeUid)
            }

            val updatedLikedIds = existingLikedIds.distinct().take(500)
            val resolvedCount = maxOf(updatedLikedIds.size, fallbackCount.coerceAtLeast(0))

            docRef.update(
                mapOf(
                    "likedUserIds" to updatedLikedIds,
                    "likesCount" to resolvedCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(resolvedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun updateMomentLikeCount(momentId: String, newCount: Int, userId: String? = null, isLiked: Boolean? = null) {
        val db = firestore ?: return
        val activeUid = userId ?: auth?.currentUser?.uid
        val docRef = db.collection("moments").document(momentId)

        if (activeUid != null && isLiked != null) {
            docRef.get()
                .addOnSuccessListener { snap ->
                    val existingLikedIds = (snap.get("likedUserIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.filter { it.isNotBlank() }
                        ?.toMutableList()
                        ?: mutableListOf()
                    if (isLiked) {
                        if (!existingLikedIds.contains(activeUid)) existingLikedIds.add(activeUid)
                    } else {
                        existingLikedIds.remove(activeUid)
                    }
                    val updatedLikedIds = existingLikedIds.distinct().take(500)
                    val resolvedCount = maxOf(updatedLikedIds.size, newCount.coerceAtLeast(0))
                    docRef.update(
                        mapOf(
                            "likedUserIds" to updatedLikedIds,
                            "likesCount" to resolvedCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnFailureListener { e ->
                        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                    }
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.GET, docRef.path)
                }
        } else {
            docRef.update(
                mapOf(
                    "likesCount" to newCount.coerceAtLeast(0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            }
        }
    }

    fun updateMomentComments(momentId: String, commentsJson: String) {
        val db = firestore ?: return
        val docRef = db.collection("moments").document(momentId)
        docRef.update(
            mapOf(
                "commentsJson" to commentsJson.take(4000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    // 7. Delete Moment from Cloud Firestore (Admin or Post Author)
    suspend fun deleteCloudMoment(momentId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val docRef = db.collection("moments").document(momentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    fun deleteMomentFromCloud(momentId: String) {
        val db = firestore ?: return
        val docRef = db.collection("moments").document(momentId)
        docRef.delete().addOnFailureListener { e ->
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    // 8. Admin Delete User from Cloud Firestore (/public_profiles/{uid}, /users/{uid}, and user's moments)
    suspend fun deleteCloudUser(targetUserId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        return try {
            db.collection("public_profiles").document(targetUserId).delete().await()
            try {
                db.collection("users").document(targetUserId).delete().await()
            } catch (e: Exception) {
                Log.w("FirebaseManager", "Optional private doc delete skipped: ${e.message}")
            }
            try {
                val authoredMoments = db.collection("moments")
                    .whereEqualTo("authorId", targetUserId)
                    .get()
                    .await()
                for (doc in authoredMoments.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                Log.w("FirebaseManager", "Optional authored moments delete skipped: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "public_profiles/$targetUserId")
            Result.failure(e)
        }
    }

    // 9. Real-time Listen to Moments (restores isLiked from likedUserIds in Firebase)
    fun listenToCloudMoments(currentUserId: String? = null): Flow<List<MomentEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "moments"
        val listener = db.collection(path)
            .whereEqualTo("visibility", "public")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val activeUid = currentUserId?.takeIf { it.isNotBlank() } ?: auth?.currentUser?.uid
                    val moments = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        if (id == "moment_1" || id == "moment_2" ||
                            id == "moment_3" || id == "moment_4" ||
                            id.startsWith("moment_seed")
                        ) {
                            return@mapNotNull null
                        }
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        val likedUserIds = (doc.get("likedUserIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()
                        val isLikedByMe = !activeUid.isNullOrBlank() && likedUserIds.contains(activeUid)
                        val rawLikesCount = (doc.getLong("likesCount") ?: 0L).toInt()
                        val resolvedLikesCount = maxOf(rawLikesCount, likedUserIds.size)

                        MomentEntity(
                            id = id,
                            authorId = doc.getString("authorId") ?: "",
                            authorName = doc.getString("authorName") ?: "",
                            authorAvatar = doc.getString("authorAvatar") ?: "",
                            mediaUri = doc.getString("mediaUri") ?: "",
                            mediaType = doc.getString("mediaType") ?: "IMAGE",
                            caption = doc.getString("caption") ?: "",
                            location = doc.getString("location") ?: "",
                            likesCount = resolvedLikesCount,
                            isLiked = isLikedByMe,
                            commentsJson = doc.getString("commentsJson") ?: "",
                            timestamp = timestamp
                        )
                    }.sortedByDescending { it.timestamp }
                    trySend(moments)
                }
            }

        awaitClose { listener.remove() }
    }

    // 10. Save User Like in Cloud Firestore and check for mutual match
    suspend fun saveCloudLike(
        myUserId: String,
        partnerId: String,
        isSuperLike: Boolean,
        senderProfile: UserProfileEntity? = null
    ): Result<Boolean> {
        val db = firestore ?: return Result.failure(IllegalStateException("Firestore not initialized"))
        val uid = auth?.currentUser?.uid ?: myUserId
        if (uid == partnerId) return Result.failure(IllegalArgumentException("Cannot like self"))
        val likeDocId = "${uid}_likes_${partnerId}"
        val likeRef = db.collection("likes").document(likeDocId)

        return try {
            val existing = likeRef.get().await()
            val metadataFields = mutableMapOf<String, Any>(
                "isSuperLike" to isSuperLike,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            senderProfile?.let { sender ->
                if (sender.name.isNotBlank()) metadataFields["fromName"] = sender.name.take(100)
                val firstPhoto = sender.photoList.firstOrNull().orEmpty()
                if (firstPhoto.isNotBlank()) metadataFields["fromAvatar"] = firstPhoto.take(2000)
                metadataFields["fromAge"] = sender.age.coerceIn(18, 120)
                if (sender.city.isNotBlank()) metadataFields["fromCity"] = sender.city.take(100)
            }

            if (!existing.exists()) {
                val createMap = metadataFields + mapOf(
                    "fromUid" to uid,
                    "toUid" to partnerId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                likeRef.set(createMap).await()
            } else {
                likeRef.update(metadataFields).await()
            }

            val reverseDocId = "${partnerId}_likes_${uid}"
            val reverseSnap = db.collection("likes").document(reverseDocId).get().await()
            Result.success(reverseSnap.exists())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            Result.failure(e)
        }
    }

    fun recordCloudLike(
        myUserId: String,
        partnerId: String,
        isSuperLike: Boolean,
        senderProfile: UserProfileEntity? = null,
        onMutualMatch: () -> Unit
    ) {
        val db = firestore ?: return
        val uid = auth?.currentUser?.uid ?: myUserId
        if (uid == partnerId) return
        val likeDocId = "${uid}_likes_${partnerId}"
        val likeRef = db.collection("likes").document(likeDocId)

        val metadataFields = mutableMapOf<String, Any>(
            "isSuperLike" to isSuperLike,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        senderProfile?.let { sender ->
            if (sender.name.isNotBlank()) metadataFields["fromName"] = sender.name.take(100)
            val firstPhoto = sender.photoList.firstOrNull().orEmpty()
            if (firstPhoto.isNotBlank()) metadataFields["fromAvatar"] = firstPhoto.take(2000)
            metadataFields["fromAge"] = sender.age.coerceIn(18, 120)
            if (sender.city.isNotBlank()) metadataFields["fromCity"] = sender.city.take(100)
        }

        likeRef.get()
            .addOnSuccessListener { existing ->
                val task = if (!existing.exists()) {
                    likeRef.set(
                        metadataFields + mapOf(
                            "fromUid" to uid,
                            "toUid" to partnerId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    likeRef.update(metadataFields)
                }
                task.addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, likeRef.path)
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.GET, likeRef.path)
            }

        db.collection("likes")
            .whereEqualTo("fromUid", partnerId)
            .whereEqualTo("toUid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onMutualMatch()
                }
            }
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.LIST, "likes")
            }
    }

    // 11. Real-time Listen to Incoming Likes ("Seni Beğenenler") where toUid == currentUserId
    fun listenToIncomingLikes(myUserId: String): Flow<List<CloudLikeRecord>> = callbackFlow {
        val db = firestore
        val uid = auth?.currentUser?.uid ?: myUserId
        if (db == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "likes"
        val listener = db.collection(path)
            .whereEqualTo("toUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        val fromUid = doc.getString("fromUid") ?: return@mapNotNull null
                        val toUid = doc.getString("toUid") ?: uid
                        if (fromUid == uid) return@mapNotNull null
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        CloudLikeRecord(
                            fromUid = fromUid,
                            toUid = toUid,
                            isSuperLike = doc.getBoolean("isSuperLike") ?: false,
                            fromName = doc.getString("fromName") ?: "",
                            fromAvatar = doc.getString("fromAvatar") ?: "",
                            fromAge = (doc.getLong("fromAge") ?: 24L).toInt(),
                            fromCity = doc.getString("fromCity") ?: "İstanbul",
                            timestamp = timestamp
                        )
                    }.sortedByDescending { it.timestamp }
                    trySend(records)
                }
            }

        awaitClose { listener.remove() }
    }

    // 12. Real-time Listen to Outgoing Likes where fromUid == currentUserId
    fun listenToOutgoingLikes(myUserId: String): Flow<List<CloudLikeRecord>> = callbackFlow {
        val db = firestore
        val uid = auth?.currentUser?.uid ?: myUserId
        if (db == null || uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val path = "likes"
        val listener = db.collection(path)
            .whereEqualTo("fromUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        val fromUid = doc.getString("fromUid") ?: uid
                        val toUid = doc.getString("toUid") ?: return@mapNotNull null
                        val createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        val timestamp = createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        CloudLikeRecord(
                            fromUid = fromUid,
                            toUid = toUid,
                            isSuperLike = doc.getBoolean("isSuperLike") ?: false,
                            fromName = doc.getString("fromName") ?: "",
                            fromAvatar = doc.getString("fromAvatar") ?: "",
                            fromAge = (doc.getLong("fromAge") ?: 24L).toInt(),
                            fromCity = doc.getString("fromCity") ?: "İstanbul",
                            timestamp = timestamp
                        )
                    }
                    trySend(records)
                }
            }

        awaitClose { listener.remove() }
    }

    private fun docToUserProfileEntity(
        doc: DocumentSnapshot,
        email: String,
        isCurrentUser: Boolean
    ): UserProfileEntity {
        val id = doc.getString("userId") ?: doc.id
        val geoPoint = try {
            doc.getGeoPoint("geolocation")
        } catch (e: Exception) {
            null
        }
        val resolvedLat = geoPoint?.latitude ?: doc.getDouble("latitude") ?: 41.0082
        val resolvedLng = geoPoint?.longitude ?: doc.getDouble("longitude") ?: 28.9784

        val rawInterestsJson = doc.getString("interestsJson").orEmpty()
        val resolvedInterestsJson = if (rawInterestsJson.isNotBlank()) {
            rawInterestsJson
        } else {
            val listFromDoc = (doc.get("interests") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            listFromDoc.joinToString("|||")
        }
        val isAdminUser = isCurrentUser && (isAdminEmail(email) || isCurrentAuthUserAdmin())

        return UserProfileEntity(
            id = id,
            name = doc.getString("name") ?: "Üye",
            age = (doc.getLong("age") ?: 24L).toInt(),
            city = doc.getString("city") ?: "İstanbul",
            bio = doc.getString("bio") ?: "",
            occupation = doc.getString("occupation") ?: "",
            education = doc.getString("education") ?: "",
            heightCm = (doc.getLong("heightCm") ?: 175L).toInt(),
            zodiac = doc.getString("zodiac") ?: "Aslan ♌",
            lookingFor = doc.getString("lookingFor") ?: "Ciddi İlişki ❤️",
            photosJson = doc.getString("photosJson") ?: "",
            interestsJson = resolvedInterestsJson,
            lifestyleJson = doc.getString("lifestyleJson") ?: "",
            promptQuestion = doc.getString("promptQuestion") ?: "",
            promptAnswer = doc.getString("promptAnswer") ?: "",
            isVerified = (doc.getBoolean("isVerified") ?: true) || isAdminUser,
            latitude = resolvedLat,
            longitude = resolvedLng,
            distanceKm = if (isCurrentUser) 0.0 else 2.4,
            isOnline = doc.getBoolean("isOnline") ?: true,
            isVip = (doc.getBoolean("isVip") ?: false) || isAdminUser,
            email = email,
            isCurrentUser = isCurrentUser,
            isProfileSetupCompleted = true
        )
    }
}
