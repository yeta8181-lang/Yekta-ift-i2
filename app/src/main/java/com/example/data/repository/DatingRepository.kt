package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CloudLikeRecord
import com.example.data.model.MomentComment
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.isAdminEmail
import com.example.location.GoogleLocationTracker
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DatingRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    val firebaseManager: FirebaseManager = FirebaseManager(context)
) {
    private val userDao = db.userDao()
    private val messageDao = db.messageDao()
    private val momentDao = db.momentDao()

    private val authPrefs = context.applicationContext.getSharedPreferences(
        "kalpkalbe_email_auth",
        Context.MODE_PRIVATE
    )

    private val moderationPrefs = context.applicationContext.getSharedPreferences(
        "kalpkalbe_moderation_prefs",
        Context.MODE_PRIVATE
    )

    private val _blockedUserIds = MutableStateFlow<Set<String>>(
        moderationPrefs.getStringSet("blocked_user_ids", emptySet())?.toSet() ?: emptySet()
    )
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _reportedCommentIds = MutableStateFlow<Set<String>>(
        moderationPrefs.getStringSet("reported_comment_ids", emptySet())?.toSet() ?: emptySet()
    )
    val reportedCommentIds: StateFlow<Set<String>> = _reportedCommentIds.asStateFlow()

    private val _likedMomentIds = MutableStateFlow<Set<String>>(
        moderationPrefs.getStringSet("liked_moment_ids", emptySet())?.toSet() ?: emptySet()
    )

    private val _incomingLikes = MutableStateFlow<List<CloudLikeRecord>>(emptyList())
    val incomingLikes: StateFlow<List<CloudLikeRecord>> = _incomingLikes.asStateFlow()

    private val _outgoingLikedIds = MutableStateFlow<Set<String>>(emptySet())

    private val repoScope = CoroutineScope(Dispatchers.IO)
    private var usersSyncJob: Job? = null
    private var momentsSyncJob: Job? = null
    private var incomingLikesSyncJob: Job? = null
    private var outgoingLikesSyncJob: Job? = null

    private val _matchCelebrationEvent = MutableSharedFlow<UserProfileEntity>(extraBufferCapacity = 1)
    val matchCelebrationEvent = _matchCelebrationEvent.asSharedFlow()

    init {
        repoScope.launch {
            cleanupLegacyFakeData()
            // Auth-gate Firestore listeners: only attach listeners when a user is signed in with Firebase
            if (firebaseManager.getCurrentUid() != null) {
                startAuthenticatedCloudSync()
            }
        }
    }

    fun getActiveEmailUid(): String? = authPrefs.getString("active_email_uid", null)

    fun hasActiveSession(): Boolean {
        return firebaseManager.getCurrentUid() != null || !getActiveEmailUid().isNullOrBlank()
    }

    fun isCurrentUserAdmin(currentUser: UserProfileEntity?): Boolean {
        return currentUser?.isAdmin == true || firebaseManager.isCurrentAuthUserAdmin()
    }

    private fun hashCredential(email: String, password: String): String {
        val input = "${email.trim().lowercase()}::kalpkalbe_salt::$password"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun deterministicEmailUid(email: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(20)
        return "email_$hex"
    }

    suspend fun registerWithEmailAccount(
        name: String,
        email: String,
        password: String,
        bio: String = "",
        interests: List<String> = listOf("Kahve ☕", "Müzik 🎵", "Seyahat ✈️"),
        latitude: Double = 41.0082,
        longitude: Double = 28.9784
    ): Result<UserProfileEntity> {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir ad soyad girin (en az 2 karakter)."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Şifreniz en az 6 karakter olmalıdır."))
        }

        val existingHash = authPrefs.getString("pwd_$cleanEmail", null)
        val existingDbUser = userDao.getUserByEmailDirect(cleanEmail)
        if (existingHash != null || (existingDbUser != null && existingDbUser.isProfileSetupCompleted)) {
            return Result.failure(
                IllegalArgumentException("Bu e-posta adresi zaten kayıtlı. Lütfen Giriş Yap sekmesini kullanın.")
            )
        }

        val uid = firebaseManager.getCurrentUid() ?: existingDbUser?.id ?: deterministicEmailUid(cleanEmail)
        val pwdHash = hashCredential(cleanEmail, password)

        authPrefs.edit()
            .putString("pwd_$cleanEmail", pwdHash)
            .putString("name_$cleanEmail", cleanName)
            .putString("uid_$cleanEmail", uid)
            .putString("active_email_uid", uid)
            .apply()

        userDao.clearCurrentUserFlags()

        val safeLat = latitude.coerceIn(-90.0, 90.0)
        val safeLng = longitude.coerceIn(-180.0, 180.0)
        val safeInterests = interests.map { it.trim() }.filter { it.isNotBlank() }
        val isAdminUser = isAdminEmail(cleanEmail)

        val newProfile = UserProfileEntity(
            id = uid,
            name = cleanName,
            age = 24,
            city = "İstanbul",
            bio = bio.trim(),
            occupation = if (isAdminUser) "Sistem Yöneticisi" else "",
            education = "",
            heightCm = 170,
            zodiac = "Koç ♈",
            lookingFor = "Ciddi İlişki ❤️",
            photosJson = "",
            interestsJson = safeInterests.joinToString("|||"),
            lifestyleJson = "Sosyal|||Pozitif",
            promptQuestion = "İdeal ilk buluşma",
            promptAnswer = "Güzel bir kahve ve samimi sohbet",
            latitude = safeLat,
            longitude = safeLng,
            isVerified = isAdminUser,
            isCurrentUser = true,
            isVip = isAdminUser,
            email = cleanEmail,
            isProfileSetupCompleted = false
        )

        userDao.insertUser(newProfile)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.initializeUserProfileInFirestore(
                name = cleanName,
                email = cleanEmail,
                bio = bio.trim(),
                interests = safeInterests,
                latitude = safeLat,
                longitude = safeLng,
                isProfileSetupCompleted = false
            )
        }
        return Result.success(newProfile)
    }

    suspend fun loginWithEmailAccount(
        email: String,
        password: String
    ): Result<UserProfileEntity> {
        val cleanEmail = email.trim().lowercase()
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Lütfen geçerli bir e-posta adresi girin."))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Lütfen şifrenizi girin."))
        }

        val isAdminUser = isAdminEmail(cleanEmail)
        val storedHash = authPrefs.getString("pwd_$cleanEmail", null)
        val existingDbUser = userDao.getUserByEmailDirect(cleanEmail)

        if (storedHash == null && existingDbUser == null && !isAdminUser) {
            return Result.failure(
                IllegalArgumentException("Bu e-posta ile kayıtlı hesap bulunamadı. Lütfen önce Kayıt Ol sekmesinden kayıt olun.")
            )
        }

        val providedHash = hashCredential(cleanEmail, password)
        if (storedHash != null && storedHash != providedHash) {
            return Result.failure(IllegalArgumentException("E-posta veya şifre hatalı. Lütfen tekrar deneyin."))
        }

        val uid = existingDbUser?.id
            ?: authPrefs.getString("uid_$cleanEmail", null)
            ?: deterministicEmailUid(cleanEmail)

        if (storedHash == null) {
            authPrefs.edit()
                .putString("pwd_$cleanEmail", providedHash)
                .putString("uid_$cleanEmail", uid)
                .apply()
        }

        authPrefs.edit()
            .putString("active_email_uid", uid)
            .apply()

        userDao.clearCurrentUserFlags()

        val activeProfile = if (existingDbUser != null) {
            val updated = existingDbUser.copy(
                isCurrentUser = true,
                isVip = existingDbUser.isVip || isAdminUser,
                isVerified = existingDbUser.isVerified || isAdminUser
            )
            userDao.insertUser(updated)
            updated
        } else {
            val savedName = authPrefs.getString("name_$cleanEmail", if (isAdminUser) "Yönetici" else "Üye")
                ?: if (isAdminUser) "Yönetici" else "Üye"
            val created = UserProfileEntity(
                id = uid,
                name = savedName,
                age = 24,
                city = "İstanbul",
                bio = if (isAdminUser) "Kalp Kalbe Resmi Yönetici Hesabı" else "",
                occupation = if (isAdminUser) "Yönetici" else "",
                education = "",
                heightCm = 175,
                zodiac = "Koç ♈",
                lookingFor = "Ciddi İlişki ❤️",
                photosJson = "",
                interestsJson = "Kahve ☕|||Müzik 🎵|||Seyahat ✈️",
                lifestyleJson = "Sosyal|||Pozitif",
                promptQuestion = "İdeal ilk buluşma",
                promptAnswer = "Güzel bir kahve ve samimi sohbet",
                isVerified = isAdminUser,
                isCurrentUser = true,
                isVip = isAdminUser,
                email = cleanEmail,
                isProfileSetupCompleted = isAdminUser
            )
            userDao.insertUser(created)
            created
        }

        return Result.success(activeProfile)
    }

    suspend fun clearLocalAuthSession() {
        authPrefs.edit().remove("active_email_uid").apply()
        userDao.clearCurrentUserFlags()
    }

    fun startAuthenticatedCloudSync() {
        val currentUid = firebaseManager.getCurrentUid() ?: return
        if (!firebaseManager.isConfigured()) return

        if (usersSyncJob?.isActive != true) {
            usersSyncJob = repoScope.launch {
                firebaseManager.listenToCloudUsers(currentUid)
                    .catch { e -> Log.w("DatingRepository", "Users stream error: ${e.message}") }
                    .collect { onlineUsers ->
                        userDao.deleteFakeProfiles()
                        if (onlineUsers.isNotEmpty()) {
                            val me = userDao.getCurrentUserDirect()
                            val myLat = me?.latitude ?: 41.0082
                            val myLng = me?.longitude ?: 28.9784
                            val existingMap = userDao.getAllOtherProfilesDirect().associateBy { it.id }
                            val outgoingIds = _outgoingLikedIds.value
                            val incomingIds = _incomingLikes.value.map { it.fromUid }.toSet()

                            val enrichedUsers = onlineUsers
                                .filter { it.id !in _blockedUserIds.value }
                                .map { cloudUser ->
                                    val prev = existingMap[cloudUser.id]
                                    val distKm = GoogleLocationTracker.calculateDistanceKm(
                                        myLat,
                                        myLng,
                                        cloudUser.latitude,
                                        cloudUser.longitude
                                    )
                                    val likedByMe = (prev?.isLikedByMe ?: cloudUser.isLikedByMe) || (cloudUser.id in outgoingIds)
                                    val isMutual = (prev?.isMatch ?: cloudUser.isMatch) || (likedByMe && cloudUser.id in incomingIds)
                                    cloudUser.copy(
                                        distanceKm = distKm,
                                        isMatch = isMutual,
                                        isLikedByMe = likedByMe,
                                        isSuperLiked = prev?.isSuperLiked ?: cloudUser.isSuperLiked,
                                        isBlocked = (prev?.isBlocked ?: false) || _blockedUserIds.value.contains(cloudUser.id),
                                        isReported = prev?.isReported ?: false
                                    )
                                }
                            userDao.insertAll(enrichedUsers)
                        }
                    }
            }
        }

        if (momentsSyncJob?.isActive != true) {
            momentsSyncJob = repoScope.launch {
                firebaseManager.listenToCloudMoments(currentUid)
                    .catch { e -> Log.w("DatingRepository", "Moments stream error: ${e.message}") }
                    .collect { onlineMoments ->
                        momentDao.deleteFakeMoments()
                        val localLikedIds = _likedMomentIds.value
                        val mergedMoments = onlineMoments.map { m ->
                            val isLiked = m.isLiked || (m.id in localLikedIds)
                            val count = if (isLiked && m.likesCount == 0) 1 else m.likesCount
                            m.copy(isLiked = isLiked, likesCount = count)
                        }
                        if (mergedMoments.isNotEmpty()) {
                            momentDao.insertAll(mergedMoments)
                        }
                    }
            }
        }

        if (incomingLikesSyncJob?.isActive != true) {
            incomingLikesSyncJob = repoScope.launch {
                firebaseManager.listenToIncomingLikes(currentUid)
                    .catch { e -> Log.w("DatingRepository", "Incoming likes stream error: ${e.message}") }
                    .collect { incoming ->
                        _incomingLikes.value = incoming
                        val outgoingIds = _outgoingLikedIds.value
                        incoming.forEach { likeRecord ->
                            val existing = userDao.getUserByIdDirect(likeRecord.fromUid)
                            if (existing == null && likeRecord.fromUid !in _blockedUserIds.value) {
                                val fetched = try {
                                    firebaseManager.fetchUserProfile(likeRecord.fromUid)
                                } catch (e: Exception) {
                                    null
                                }
                                if (fetched != null) {
                                    val isMutual = likeRecord.fromUid in outgoingIds
                                    userDao.insertUser(
                                        fetched.copy(
                                            isCurrentUser = false,
                                            isLikedByMe = isMutual,
                                            isMatch = isMutual
                                        )
                                    )
                                }
                            } else if (existing != null && likeRecord.fromUid in outgoingIds && !existing.isMatch) {
                                userDao.setLiked(existing.id, isMatch = true, isSuperLike = existing.isSuperLiked)
                            }
                        }
                    }
            }
        }

        if (outgoingLikesSyncJob?.isActive != true) {
            outgoingLikesSyncJob = repoScope.launch {
                firebaseManager.listenToOutgoingLikes(currentUid)
                    .catch { e -> Log.w("DatingRepository", "Outgoing likes stream error: ${e.message}") }
                    .collect { outgoing ->
                        val outSet = outgoing.map { it.toUid }.toSet()
                        _outgoingLikedIds.value = outSet
                        val incomingSet = _incomingLikes.value.map { it.fromUid }.toSet()
                        outgoing.forEach { rec ->
                            val isMutual = rec.toUid in incomingSet
                            userDao.setLiked(rec.toUid, isMatch = isMutual, isSuperLike = rec.isSuperLike)
                        }
                    }
            }
        }
    }

    fun stopCloudSync() {
        usersSyncJob?.cancel()
        usersSyncJob = null
        momentsSyncJob?.cancel()
        momentsSyncJob = null
        incomingLikesSyncJob?.cancel()
        incomingLikesSyncJob = null
        outgoingLikesSyncJob?.cancel()
        outgoingLikesSyncJob = null
    }

    private suspend fun cleanupLegacyFakeData() {
        userDao.deleteFakeProfiles()
        momentDao.deleteFakeMoments()
        messageDao.deleteFakeMessages()

        val existingCurrent = userDao.getCurrentUserDirect()
        if (existingCurrent != null && existingCurrent.name == "Kaan" && existingCurrent.email.isBlank()) {
            userDao.deleteAllOtherUsers()
        }
    }

    fun getAllProfiles(): Flow<List<UserProfileEntity>> = userDao.getAllOtherProfiles()
    fun getCurrentUser(): Flow<UserProfileEntity?> = userDao.getCurrentUser()
    fun getMatches(): Flow<List<UserProfileEntity>> = userDao.getMatches()
    fun getUserById(id: String): Flow<UserProfileEntity?> = userDao.getUserById(id)

    /**
     * Returns the real-time list of users who liked the current user ("Seni Beğenenler") from Firebase.
     */
    fun getUsersWhoLikedMe(): Flow<List<UserProfileEntity>> = combine(
        _incomingLikes,
        userDao.getAllOtherProfiles(),
        _outgoingLikedIds,
        _blockedUserIds
    ) { incomingRecords, otherProfiles, outgoingSet, blockedSet ->
        val profileMap = otherProfiles.associateBy { it.id }
        incomingRecords
            .filter { it.fromUid !in blockedSet }
            .map { record ->
                val existing = profileMap[record.fromUid]
                val isLikedBack = (existing?.isLikedByMe == true) || (record.fromUid in outgoingSet)
                val isMatched = (existing?.isMatch == true) || isLikedBack
                if (existing != null) {
                    existing.copy(
                        isSuperLiked = record.isSuperLike || existing.isSuperLiked,
                        isLikedByMe = isLikedBack,
                        isMatch = isMatched
                    )
                } else {
                    UserProfileEntity(
                        id = record.fromUid,
                        name = record.fromName.ifBlank { "Seni Beğenen Üye" },
                        age = record.fromAge.coerceIn(18, 99),
                        city = record.fromCity.ifBlank { "İstanbul" },
                        bio = if (record.isSuperLike) "Sana Süper Beğeni ⭐ gönderdi!" else "Profilini beğendi ❤️",
                        occupation = "",
                        education = "",
                        heightCm = 172,
                        zodiac = "Terazi ♎",
                        lookingFor = "Ciddi İlişki ❤️",
                        photosJson = record.fromAvatar,
                        interestsJson = "Kahve ☕|||Müzik 🎵",
                        lifestyleJson = "Sosyal",
                        promptQuestion = "İdeal ilk buluşma",
                        promptAnswer = "Samimi bir sohbet",
                        isVerified = true,
                        isLikedByMe = isLikedBack,
                        isMatch = isMatched,
                        isSuperLiked = record.isSuperLike,
                        isCurrentUser = false
                    )
                }
            }
    }

    fun getMessagesForConversation(convId: String): Flow<List<ChatMessageEntity>> {
        val myUid = firebaseManager.getCurrentUid()
        if (firebaseManager.isConfigured() && myUid != null) {
            repoScope.launch {
                val channelId = firebaseManager.getConversationChannelId(myUid, convId)
                firebaseManager.listenToCloudMessages(channelId, myUid, convId)
                    .catch { e -> Log.w("DatingRepository", "Messages stream error: ${e.message}") }
                    .collect { cloudMsgs ->
                        if (cloudMsgs.isNotEmpty()) {
                            messageDao.insertAll(cloudMsgs)
                        }
                    }
            }
        }
        return messageDao.getMessagesForConversation(convId)
    }

    fun getAllMoments(): Flow<List<MomentEntity>> = combine(
        momentDao.getAllMoments(),
        _blockedUserIds,
        _reportedCommentIds
    ) { momentsList, blockedIds, reportedCmtIds ->
        momentsList
            .filter { moment -> moment.authorId !in blockedIds }
            .map { moment ->
                val filteredComments = moment.commentList.filter { comment ->
                    comment.id !in reportedCmtIds && comment.authorId !in blockedIds
                }
                if (filteredComments.size == moment.commentList.size) {
                    moment
                } else {
                    moment.copy(commentsJson = MomentComment.serializeList(filteredComments))
                }
            }
    }

    // Direct Cloud Firestore operations (used by both production sync and Robolectric RuleTests)
    /**
     * Initializes a user profile document in Firestore upon successful registration,
     * including fields for bio, interests, and geolocation, and caches it locally in Room.
     */
    suspend fun initializeUserProfileOnRegistration(
        name: String,
        email: String,
        bio: String = "",
        interests: List<String> = emptyList(),
        latitude: Double = 41.0082,
        longitude: Double = 28.9784,
        avatarUrl: String = "",
        age: Int = 24,
        city: String = "İstanbul",
        isProfileSetupCompleted: Boolean = false
    ): Result<UserProfileEntity> {
        val result = firebaseManager.initializeUserProfileInFirestore(
            name = name,
            email = email,
            bio = bio,
            interests = interests,
            latitude = latitude,
            longitude = longitude,
            avatarUrl = avatarUrl,
            age = age,
            city = city,
            isProfileSetupCompleted = isProfileSetupCompleted
        )
        result.onSuccess { initializedProfile ->
            userDao.clearCurrentUserFlags()
            userDao.insertUser(initializedProfile.copy(isCurrentUser = true))
            startAuthenticatedCloudSync()
        }
        return result
    }

    suspend fun saveUserProfileToCloud(profile: UserProfileEntity): Result<String> {
        return firebaseManager.saveUserProfileToCloud(profile)
    }

    suspend fun fetchUserProfileFromCloud(userId: String): UserProfileEntity? {
        return firebaseManager.fetchUserProfile(userId)
    }

    fun observePublicProfilesFromCloud(currentUserId: String): Flow<List<UserProfileEntity>> {
        return firebaseManager.listenToCloudUsers(currentUserId)
    }

    suspend fun createMomentInCloud(moment: MomentEntity): Result<String> {
        return firebaseManager.createCloudMoment(moment)
    }

    fun observeMomentsFromCloud(): Flow<List<MomentEntity>> {
        return firebaseManager.listenToCloudMoments()
    }

    suspend fun saveUserLikeToCloud(
        partnerProfile: UserProfileEntity,
        isSuperLike: Boolean = false
    ): Result<Boolean> {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"
        return firebaseManager.saveCloudLike(
            myUserId = myId,
            partnerId = partnerProfile.id,
            isSuperLike = isSuperLike,
            senderProfile = currentUser
        )
    }

    fun observeIncomingLikesFromCloud(myUserId: String): Flow<List<CloudLikeRecord>> {
        return firebaseManager.listenToIncomingLikes(myUserId)
    }

    suspend fun likeProfile(profile: UserProfileEntity, isSuperLike: Boolean = false): Boolean {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"
        val likedMeAlready = _incomingLikes.value.any { it.fromUid == profile.id }

        val isMutualMatch = isSuperLike || profile.isMatch || likedMeAlready
        _outgoingLikedIds.value = _outgoingLikedIds.value + profile.id
        userDao.insertUser(
            profile.copy(
                isCurrentUser = false,
                isLikedByMe = true,
                isMatch = isMutualMatch,
                isSuperLiked = isSuperLike
            )
        )
        userDao.setLiked(profile.id, isMatch = isMutualMatch, isSuperLike = isSuperLike)

        if (isMutualMatch) {
            val updatedProfile = profile.copy(isMatch = true, isLikedByMe = true, isSuperLiked = isSuperLike)
            _matchCelebrationEvent.tryEmit(updatedProfile)
        }

        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.recordCloudLike(
                myUserId = myId,
                partnerId = profile.id,
                isSuperLike = isSuperLike,
                senderProfile = currentUser
            ) {
                repoScope.launch {
                    userDao.setLiked(profile.id, isMatch = true, isSuperLike = isSuperLike)
                    _matchCelebrationEvent.tryEmit(profile.copy(isMatch = true, isLikedByMe = true))
                }
            }
        }

        return isMutualMatch
    }

    suspend fun passProfile(profile: UserProfileEntity) {
        // Passed without mock side-effects
    }

    suspend fun blockUser(userId: String) {
        if (userId.isBlank()) return
        val updatedBlocked = _blockedUserIds.value + userId
        _blockedUserIds.value = updatedBlocked
        moderationPrefs.edit().putStringSet("blocked_user_ids", updatedBlocked).apply()

        userDao.blockUser(userId)
        momentDao.deleteMomentsByAuthor(userId)
        removeCommentsByAuthorAcrossAllMoments(userId)
    }

    suspend fun reportUser(userId: String, reason: String) {
        if (userId.isBlank()) return
        val updatedBlocked = _blockedUserIds.value + userId
        _blockedUserIds.value = updatedBlocked
        moderationPrefs.edit()
            .putStringSet("blocked_user_ids", updatedBlocked)
            .putString("user_report_${userId}_${System.currentTimeMillis()}", reason)
            .apply()

        userDao.reportUser(userId)
        momentDao.deleteMomentsByAuthor(userId)
        removeCommentsByAuthorAcrossAllMoments(userId)
    }

    /**
     * Admin (`yeta8181@gmail.com`) full-privilege action: deletes a user profile locally and from Firestore.
     */
    suspend fun adminDeleteUser(userId: String) {
        if (userId.isBlank()) return
        val updatedBlocked = _blockedUserIds.value + userId
        _blockedUserIds.value = updatedBlocked
        moderationPrefs.edit().putStringSet("blocked_user_ids", updatedBlocked).apply()

        userDao.deleteUserById(userId)
        momentDao.deleteMomentsByAuthor(userId)
        removeCommentsByAuthorAcrossAllMoments(userId)

        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.deleteCloudUser(userId)
        }
    }

    /**
     * Deletes a Discover / Moment post locally and from Firestore (accessible by Admin `yeta8181@gmail.com` or post author).
     */
    suspend fun deleteMoment(moment: MomentEntity) {
        momentDao.deleteMomentById(moment.id)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.deleteCloudMoment(moment.id)
        }
    }

    private suspend fun removeCommentsByAuthorAcrossAllMoments(authorId: String) {
        val allMoments = momentDao.getAllMomentsDirect()
        allMoments.forEach { moment ->
            val currentComments = moment.commentList
            val remaining = currentComments.filterNot { it.authorId == authorId }
            if (remaining.size != currentComments.size) {
                val newCommentsJson = MomentComment.serializeList(remaining)
                momentDao.updateComments(moment.id, newCommentsJson)
                if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
                    firebaseManager.updateMomentComments(moment.id, newCommentsJson)
                }
            }
        }
    }

    suspend fun verifyCurrentUser() {
        userDao.verifyCurrentUser()
    }

    suspend fun updateCurrentUser(updated: UserProfileEntity) {
        userDao.updateUser(updated)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.saveUserProfileToCloud(updated)
        }
    }

    suspend fun updateVipStatus(isVip: Boolean) {
        val me = userDao.getCurrentUserDirect()
        val effectiveVip = isVip || (me?.isAdmin == true) || firebaseManager.isCurrentAuthUserAdmin()
        userDao.updateVipStatus(effectiveVip)
        val user = userDao.getCurrentUserDirect()
        if (user != null && firebaseManager.getCurrentUid() != null) {
            firebaseManager.saveUserProfileToCloud(user.copy(isVip = effectiveVip))
        }
    }

    suspend fun signInWithGoogleCredential(
        idToken: String,
        name: String = "",
        email: String = "",
        avatarUrl: String = "",
        bio: String = "",
        interests: List<String> = emptyList(),
        latitude: Double = 41.0082,
        longitude: Double = 28.9784
    ): Result<UserProfileEntity?> {
        val authResult = firebaseManager.signInWithGoogleCredential(idToken)
        return authResult.mapCatching { uid ->
            authPrefs.edit().remove("active_email_uid").apply()
            userDao.clearCurrentUserFlags()
            startAuthenticatedCloudSync()
            val resolvedEmail = email.ifBlank { firebaseManager.auth?.currentUser?.email.orEmpty() }
            val isAdminUser = isAdminEmail(resolvedEmail)
            val cloudProfile = try {
                firebaseManager.fetchUserProfile(uid)
            } catch (e: Exception) {
                null
            }
            if (cloudProfile != null) {
                val enriched = cloudProfile.copy(
                    isCurrentUser = true,
                    email = cloudProfile.email.ifBlank { resolvedEmail },
                    isVip = cloudProfile.isVip || isAdminUser,
                    isVerified = cloudProfile.isVerified || isAdminUser
                )
                userDao.insertUser(enriched)
                if (isAdminUser && (!cloudProfile.isVip || !cloudProfile.isVerified)) {
                    firebaseManager.saveUserProfileToCloud(enriched)
                }
                enriched
            } else {
                val resolvedName = name.ifBlank { firebaseManager.auth?.currentUser?.displayName ?: "Yeni Üye" }
                val initResult = initializeUserProfileOnRegistration(
                    name = resolvedName,
                    email = resolvedEmail,
                    bio = bio,
                    interests = interests,
                    latitude = latitude,
                    longitude = longitude,
                    avatarUrl = avatarUrl,
                    isProfileSetupCompleted = isAdminUser
                )
                initResult.getOrNull()
            }
        }
    }

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        val currentUid = firebaseManager.getCurrentUid()
        val emailUid = getActiveEmailUid()
        val effectiveId = when {
            !currentUid.isNullOrBlank() -> currentUid
            !emailUid.isNullOrBlank() -> emailUid
            else -> profile.id
        }
        val isAdminUser = profile.isAdmin || firebaseManager.isCurrentAuthUserAdmin()
        val userToSave = profile.copy(
            id = effectiveId,
            isCurrentUser = true,
            isVip = profile.isVip || isAdminUser,
            isVerified = profile.isVerified || isAdminUser,
            isProfileSetupCompleted = true
        )
        userDao.clearCurrentUserFlags()
        userDao.insertUser(userToSave)
        if (currentUid != null) {
            firebaseManager.saveUserProfileToCloud(userToSave)
            startAuthenticatedCloudSync()
        }
    }

    suspend fun sendMessage(conversationId: String, text: String, mediaUri: String? = null, mediaType: String? = null) {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"

        val msg = ChatMessageEntity(
            conversationId = conversationId,
            senderId = "me",
            text = text,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(msg)

        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            val channelId = firebaseManager.getConversationChannelId(myId, conversationId)
            firebaseManager.sendCloudMessage(channelId, myId, conversationId, msg)
        }
    }

    suspend fun addMoment(
        author: UserProfileEntity,
        caption: String,
        mediaUri: String = "",
        mediaType: String = "NONE",
        location: String = ""
    ) {
        val uid = firebaseManager.getCurrentUid() ?: author.id
        val resolvedType = when {
            mediaUri.isBlank() -> "NONE"
            mediaType == "VIDEO" -> "VIDEO"
            else -> "IMAGE"
        }
        val moment = MomentEntity(
            id = "moment_${System.currentTimeMillis()}",
            authorId = uid,
            authorName = author.name,
            authorAvatar = author.photoList.firstOrNull() ?: "",
            mediaUri = mediaUri.trim(),
            mediaType = resolvedType,
            caption = caption.trim(),
            location = location.trim(),
            likesCount = 0,
            isLiked = false,
            timestamp = System.currentTimeMillis()
        )
        momentDao.insertMoment(moment)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.createCloudMoment(moment)
        }
    }

    suspend fun toggleMomentLike(moment: MomentEntity) {
        val currentUser = userDao.getCurrentUserDirect()
        val myId = firebaseManager.getCurrentUid() ?: currentUser?.id ?: "me"
        val newLiked = !moment.isLiked
        val newCount = if (newLiked) moment.likesCount + 1 else (moment.likesCount - 1).coerceAtLeast(0)

        val updatedSet = if (newLiked) _likedMomentIds.value + moment.id else _likedMomentIds.value - moment.id
        _likedMomentIds.value = updatedSet
        moderationPrefs.edit().putStringSet("liked_moment_ids", updatedSet).apply()

        momentDao.updateLike(moment.id, newLiked, newCount)
        if (firebaseManager.getCurrentUid() != null) {
            firebaseManager.toggleCloudMomentLike(
                momentId = moment.id,
                userId = myId,
                isLiked = newLiked,
                fallbackCount = newCount
            )
        }
    }

    suspend fun addCommentToMoment(moment: MomentEntity, author: UserProfileEntity, text: String) {
        val uid = firebaseManager.getCurrentUid() ?: author.id
        val newComment = MomentComment(
            id = "cmt_${System.currentTimeMillis()}",
            authorId = uid,
            authorName = author.name.ifBlank { "VIP Üye" },
            authorAvatar = author.photoList.firstOrNull().orEmpty(),
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            isVip = true
        )
        val updatedComments = moment.commentList + newComment
        val newCommentsJson = MomentComment.serializeList(updatedComments)
        momentDao.updateComments(moment.id, newCommentsJson)
        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.updateMomentComments(moment.id, newCommentsJson)
        }
    }

    suspend fun deleteCommentFromMoment(moment: MomentEntity, commentId: String) {
        val updatedComments = moment.commentList.filterNot { it.id == commentId }
        val newCommentsJson = MomentComment.serializeList(updatedComments)
        momentDao.updateComments(moment.id, newCommentsJson)
        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.updateMomentComments(moment.id, newCommentsJson)
        }
    }

    suspend fun reportComment(
        moment: MomentEntity,
        comment: MomentComment,
        reason: String,
        blockAuthor: Boolean
    ) {
        val updatedReported = _reportedCommentIds.value + comment.id
        _reportedCommentIds.value = updatedReported
        moderationPrefs.edit()
            .putStringSet("reported_comment_ids", updatedReported)
            .putString(
                "comment_report_${comment.id}",
                "${moment.id}|${comment.authorId}|$reason|${System.currentTimeMillis()}"
            )
            .apply()

        val updatedComments = moment.commentList.filterNot {
            it.id == comment.id || (blockAuthor && it.authorId == comment.authorId)
        }
        val newCommentsJson = MomentComment.serializeList(updatedComments)
        momentDao.updateComments(moment.id, newCommentsJson)
        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.updateMomentComments(moment.id, newCommentsJson)
        }

        if (blockAuthor && comment.authorId.isNotBlank()) {
            reportUser(comment.authorId, "Yorum Şikayeti: $reason")
        }
    }

    suspend fun blockCommentAuthor(moment: MomentEntity, comment: MomentComment) {
        val updatedComments = moment.commentList.filterNot { it.authorId == comment.authorId }
        val newCommentsJson = MomentComment.serializeList(updatedComments)
        momentDao.updateComments(moment.id, newCommentsJson)
        if (firebaseManager.isConfigured() && firebaseManager.getCurrentUid() != null) {
            firebaseManager.updateMomentComments(moment.id, newCommentsJson)
        }
        if (comment.authorId.isNotBlank()) {
            blockUser(comment.authorId)
        }
    }

    suspend fun updateLocationAndRecalculateDistances(userLat: Double, userLng: Double) {
        val currentUser = userDao.getCurrentUserDirect()
        if (currentUser != null) {
            val updated = currentUser.copy(latitude = userLat, longitude = userLng, distanceKm = 0.0)
            userDao.updateUser(updated)
            if (firebaseManager.getCurrentUid() != null) {
                firebaseManager.saveUserProfileToCloud(updated)
            }
        }

        val otherUsers = userDao.getAllOtherProfilesDirect()
        if (otherUsers.isNotEmpty()) {
            val recalculated = otherUsers.map { profile ->
                val distKm = GoogleLocationTracker.calculateDistanceKm(
                    userLat,
                    userLng,
                    profile.latitude,
                    profile.longitude
                )
                profile.copy(distanceKm = distKm)
            }
            userDao.insertAll(recalculated)
        }
    }
}
