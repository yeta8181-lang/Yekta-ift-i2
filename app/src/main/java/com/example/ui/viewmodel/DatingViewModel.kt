package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessageEntity
import com.example.data.model.MomentComment
import com.example.data.model.MomentEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.DatingRepository
import com.example.location.GoogleLocationTracker
import com.example.ui.components.DiscoverFilterState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DatingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DatingRepository(application)
    val locationTracker = GoogleLocationTracker(application)
    private var locationTrackingJob: Job? = null

    val currentUser: StateFlow<UserProfileEntity?> = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allProfiles: StateFlow<List<UserProfileEntity>> = repository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<UserProfileEntity>> = repository.getMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val likedMeProfiles: StateFlow<List<UserProfileEntity>> = repository.getUsersWhoLikedMe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moments: StateFlow<List<MomentEntity>> = repository.getAllMoments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _distanceFilterKm = MutableStateFlow(25f)
    val distanceFilterKm: StateFlow<Float> = _distanceFilterKm.asStateFlow()

    private val _discoverFilter = MutableStateFlow(DiscoverFilterState())
    val discoverFilter: StateFlow<DiscoverFilterState> = _discoverFilter.asStateFlow()

    private val _selectedMapUser = MutableStateFlow<UserProfileEntity?>(null)
    val selectedMapUser: StateFlow<UserProfileEntity?> = _selectedMapUser.asStateFlow()

    private val _activeConversationUser = MutableStateFlow<UserProfileEntity?>(null)
    val activeConversationUser: StateFlow<UserProfileEntity?> = _activeConversationUser.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<ChatMessageEntity>> = _activeConversationUser
        .flatMapLatest { user ->
            if (user != null) repository.getMessagesForConversation(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _matchCelebration = MutableStateFlow<UserProfileEntity?>(null)
    val matchCelebration: StateFlow<UserProfileEntity?> = _matchCelebration.asStateFlow()

    private val _safetyDialogOpen = MutableStateFlow(false)
    val safetyDialogOpen: StateFlow<Boolean> = _safetyDialogOpen.asStateFlow()

    private val _vipDialogOpen = MutableStateFlow(false)
    val vipDialogOpen: StateFlow<Boolean> = _vipDialogOpen.asStateFlow()

    private val _reportBlockDialogUser = MutableStateFlow<UserProfileEntity?>(null)
    val reportBlockDialogUser: StateFlow<UserProfileEntity?> = _reportBlockDialogUser.asStateFlow()

    private val _verificationSuccessNotice = MutableStateFlow(false)
    val verificationSuccessNotice: StateFlow<Boolean> = _verificationSuccessNotice.asStateFlow()

    // Google Location API live tracking states
    private val _isLocationTrackingActive = MutableStateFlow(false)
    val isLocationTrackingActive: StateFlow<Boolean> = _isLocationTrackingActive.asStateFlow()

    // Authentication & Onboarding states
    private val _isSignedIn = MutableStateFlow(repository.hasActiveSession())
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _googleUserInfo = MutableStateFlow<Triple<String, String, String>?>(null)
    val googleUserInfo: StateFlow<Triple<String, String, String>?> = _googleUserInfo.asStateFlow()

    private val _isSettingUpProfile = MutableStateFlow(false)
    val isSettingUpProfile: StateFlow<Boolean> = _isSettingUpProfile.asStateFlow()

    val isCloudActive: StateFlow<Boolean> = repository.firebaseManager.isCloudActive

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _isSignedIn.value = true
            repository.startAuthenticatedCloudSync()
            if (locationTracker.hasLocationPermission()) {
                startContinuousLocationTracking()
            }
        } else if (repository.getActiveEmailUid().isNullOrBlank()) {
            _isSignedIn.value = false
            stopContinuousLocationTracking()
            repository.stopCloudSync()
        }
    }

    init {
        repository.firebaseManager.auth?.addAuthStateListener(authStateListener)

        viewModelScope.launch {
            repository.matchCelebrationEvent.collect { matchedUser ->
                _matchCelebration.value = matchedUser
            }
        }

        viewModelScope.launch {
            repository.getCurrentUser().collect { user ->
                if (user != null && repository.hasActiveSession()) {
                    _isSignedIn.value = true
                    if (user.isProfileSetupCompleted) {
                        _isSettingUpProfile.value = false
                    } else {
                        _googleUserInfo.value = Triple(user.name, user.email, user.photoList.firstOrNull().orEmpty())
                        _isSettingUpProfile.value = true
                    }
                }
            }
        }

        if (locationTracker.hasLocationPermission()) {
            startContinuousLocationTracking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopContinuousLocationTracking()
        repository.firebaseManager.auth?.removeAuthStateListener(authStateListener)
    }

    fun isCurrentUserAdmin(): Boolean {
        return repository.isCurrentUserAdmin(currentUser.value)
    }

    fun hasVipOrAdminPrivileges(): Boolean {
        val user = currentUser.value
        return user?.isVip == true || user?.isAdmin == true || repository.firebaseManager.isCurrentAuthUserAdmin()
    }

    fun startContinuousLocationTracking() {
        if (!locationTracker.hasLocationPermission()) {
            _isLocationTrackingActive.value = false
            return
        }
        if (locationTrackingJob?.isActive == true) return

        _isLocationTrackingActive.value = true
        locationTrackingJob = viewModelScope.launch {
            locationTracker.observeLocationUpdates()
                .catch { e ->
                    Log.w("DatingViewModel", "Location tracking stream error: ${e.message}")
                    _isLocationTrackingActive.value = false
                }
                .collect { location ->
                    _isLocationTrackingActive.value = true
                    repository.updateLocationAndRecalculateDistances(
                        location.latitude,
                        location.longitude
                    )
                }
        }
    }

    fun stopContinuousLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
        _isLocationTrackingActive.value = false
    }

    fun refreshLocationNow(onResult: (Boolean) -> Unit = {}) {
        if (!locationTracker.hasLocationPermission()) {
            _isLocationTrackingActive.value = false
            onResult(false)
            return
        }
        startContinuousLocationTracking()
        viewModelScope.launch {
            val loc = locationTracker.fetchSingleHighAccuracyLocation()
            if (loc != null) {
                repository.updateLocationAndRecalculateDistances(loc.latitude, loc.longitude)
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun onGoogleSignInWithToken(
        idToken: String,
        name: String,
        email: String,
        avatarUrl: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val loc = if (locationTracker.hasLocationPermission()) {
                locationTracker.fetchSingleHighAccuracyLocation()
            } else {
                null
            }
            val result = repository.signInWithGoogleCredential(
                idToken = idToken,
                name = name,
                email = email,
                avatarUrl = avatarUrl,
                latitude = loc?.latitude ?: 41.0082,
                longitude = loc?.longitude ?: 28.9784
            )
            result.onSuccess { existingProfile ->
                _isSignedIn.value = true
                if (existingProfile != null && existingProfile.isProfileSetupCompleted) {
                    _isSettingUpProfile.value = false
                } else {
                    _googleUserInfo.value = Triple(name, email, avatarUrl)
                    _isSettingUpProfile.value = true
                }
            }.onFailure { e ->
                onError(e.localizedMessage ?: "Google ile giriş başarısız oldu")
            }
        }
    }

    fun initializeUserProfileOnRegistration(
        name: String,
        email: String,
        bio: String,
        interests: List<String>,
        latitude: Double,
        longitude: Double,
        avatarUrl: String = "",
        age: Int = 24,
        city: String = "İstanbul",
        isProfileSetupCompleted: Boolean = true,
        onComplete: (Result<UserProfileEntity>) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.initializeUserProfileOnRegistration(
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
            if (result.isSuccess && isProfileSetupCompleted) {
                _isSettingUpProfile.value = false
                _isSignedIn.value = true
            }
            onComplete(result)
        }
    }

    fun onRegisterWithEmail(
        name: String,
        email: String,
        password: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val loc = if (locationTracker.hasLocationPermission()) {
                locationTracker.fetchSingleHighAccuracyLocation()
            } else {
                null
            }
            val result = repository.registerWithEmailAccount(
                name = name,
                email = email,
                password = password,
                latitude = loc?.latitude ?: 41.0082,
                longitude = loc?.longitude ?: 28.9784
            )
            result.onSuccess { profile ->
                _googleUserInfo.value = Triple(profile.name, profile.email, "")
                _isSettingUpProfile.value = true
                _isSignedIn.value = true
            }.onFailure { e ->
                onError(e.localizedMessage ?: "E-posta ile kayıt başarısız oldu")
            }
        }
    }

    fun onLoginWithEmail(
        email: String,
        password: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.loginWithEmailAccount(email, password)
            result.onSuccess { profile ->
                _isSignedIn.value = true
                if (profile.isProfileSetupCompleted) {
                    _isSettingUpProfile.value = false
                } else {
                    _googleUserInfo.value = Triple(profile.name, profile.email, profile.photoList.firstOrNull().orEmpty())
                    _isSettingUpProfile.value = true
                }
            }.onFailure { e ->
                onError(e.localizedMessage ?: "E-posta ile giriş başarısız oldu")
            }
        }
    }

    fun onSignOutComplete() {
        viewModelScope.launch {
            repository.clearLocalAuthSession()
            _isSignedIn.value = false
            _isSettingUpProfile.value = false
            _activeConversationUser.value = null
            stopContinuousLocationTracking()
            repository.stopCloudSync()
        }
    }

    fun completeProfileSetup(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            _isSettingUpProfile.value = false
            _isSignedIn.value = true
            if (locationTracker.hasLocationPermission()) {
                startContinuousLocationTracking()
            }
        }
    }

    fun openVipDialog() {
        _vipDialogOpen.value = true
    }

    fun closeVipDialog() {
        _vipDialogOpen.value = false
    }

    fun setVipStatus(isVip: Boolean) {
        viewModelScope.launch {
            repository.updateVipStatus(isVip)
        }
    }

    fun setDistanceFilter(km: Float) {
        _distanceFilterKm.value = km
    }

    fun selectMapUser(user: UserProfileEntity?) {
        _selectedMapUser.value = user
    }

    fun openConversation(user: UserProfileEntity) {
        if (!hasVipOrAdminPrivileges()) {
            _vipDialogOpen.value = true
            return
        }
        _activeConversationUser.value = user
    }

    fun closeConversation() {
        _activeConversationUser.value = null
    }

    fun dismissMatchCelebration() {
        _matchCelebration.value = null
    }

    fun openSafetyDialog() {
        _safetyDialogOpen.value = true
    }

    fun closeSafetyDialog() {
        _safetyDialogOpen.value = false
    }

    fun openReportBlockDialog(user: UserProfileEntity) {
        _reportBlockDialogUser.value = user
    }

    fun closeReportBlockDialog() {
        _reportBlockDialogUser.value = null
    }

    fun dismissVerificationNotice() {
        _verificationSuccessNotice.value = false
    }

    fun likeProfile(profile: UserProfileEntity, isSuperLike: Boolean = false) {
        viewModelScope.launch {
            repository.likeProfile(profile, isSuperLike)
        }
    }

    fun passProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.passProfile(profile)
        }
    }

    fun sendMessage(text: String, mediaUri: String? = null, mediaType: String? = null) {
        val activeUser = _activeConversationUser.value ?: return
        if (text.isBlank() && mediaUri == null) return
        viewModelScope.launch {
            repository.sendMessage(activeUser.id, text, mediaUri, mediaType)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            repository.blockUser(userId)
            if (_activeConversationUser.value?.id == userId) {
                _activeConversationUser.value = null
            }
            if (_selectedMapUser.value?.id == userId) {
                _selectedMapUser.value = null
            }
            _reportBlockDialogUser.value = null
        }
    }

    fun reportUser(userId: String, reason: String) {
        viewModelScope.launch {
            repository.reportUser(userId, reason)
            if (_activeConversationUser.value?.id == userId) {
                _activeConversationUser.value = null
            }
            if (_selectedMapUser.value?.id == userId) {
                _selectedMapUser.value = null
            }
            _reportBlockDialogUser.value = null
        }
    }

    fun adminDeleteUser(userId: String) {
        if (!isCurrentUserAdmin()) return
        viewModelScope.launch {
            repository.adminDeleteUser(userId)
            if (_activeConversationUser.value?.id == userId) {
                _activeConversationUser.value = null
            }
            if (_selectedMapUser.value?.id == userId) {
                _selectedMapUser.value = null
            }
            _reportBlockDialogUser.value = null
        }
    }

    fun deleteMoment(moment: MomentEntity) {
        val user = currentUser.value
        val canDelete = isCurrentUserAdmin() || (user != null && moment.authorId == user.id)
        if (!canDelete) return
        viewModelScope.launch {
            repository.deleteMoment(moment)
        }
    }

    fun verifySelfie() {
        viewModelScope.launch {
            repository.verifyCurrentUser()
            _verificationSuccessNotice.value = true
        }
    }

    fun shareMoment(
        caption: String,
        mediaUri: String = "",
        mediaType: String = "NONE",
        location: String = ""
    ): Boolean {
        val user = currentUser.value
        if (user == null || !hasVipOrAdminPrivileges()) {
            _vipDialogOpen.value = true
            return false
        }
        if (caption.isBlank() && mediaUri.isBlank()) return false
        viewModelScope.launch {
            repository.addMoment(user, caption, mediaUri, mediaType, location)
        }
        return true
    }

    fun toggleMomentLike(moment: MomentEntity) {
        viewModelScope.launch {
            repository.toggleMomentLike(moment)
        }
    }

    fun commentOnMoment(moment: MomentEntity, text: String): Boolean {
        val user = currentUser.value
        if (user == null || !hasVipOrAdminPrivileges()) {
            _vipDialogOpen.value = true
            return false
        }
        val cleanText = text.trim()
        if (cleanText.isBlank()) return false
        viewModelScope.launch {
            repository.addCommentToMoment(moment, user, cleanText)
        }
        return true
    }

    fun deleteCommentOnMoment(moment: MomentEntity, commentId: String) {
        viewModelScope.launch {
            repository.deleteCommentFromMoment(moment, commentId)
        }
    }

    fun reportCommentOnMoment(
        moment: MomentEntity,
        comment: MomentComment,
        reason: String,
        blockAuthor: Boolean
    ) {
        viewModelScope.launch {
            repository.reportComment(moment, comment, reason, blockAuthor)
            if (blockAuthor) {
                if (_activeConversationUser.value?.id == comment.authorId) {
                    _activeConversationUser.value = null
                }
                if (_selectedMapUser.value?.id == comment.authorId) {
                    _selectedMapUser.value = null
                }
            }
        }
    }

    fun blockCommentAuthorOnMoment(moment: MomentEntity, comment: MomentComment) {
        viewModelScope.launch {
            repository.blockCommentAuthor(moment, comment)
            if (_activeConversationUser.value?.id == comment.authorId) {
                _activeConversationUser.value = null
            }
            if (_selectedMapUser.value?.id == comment.authorId) {
                _selectedMapUser.value = null
            }
        }
    }

    fun addPhotoToCurrentUser(newPhotoUri: String) {
        val user = currentUser.value ?: return
        val currentPhotos = user.photoList.toMutableList()
        currentPhotos.add(newPhotoUri)
        val updated = user.copy(photosJson = currentPhotos.joinToString("|||"))
        viewModelScope.launch {
            repository.updateCurrentUser(updated)
        }
    }

    fun updateMyProfile(
        name: String,
        city: String,
        bio: String,
        occupation: String,
        education: String,
        lookingFor: String,
        interests: List<String>,
        lifestyle: List<String>,
        promptQ: String,
        promptA: String
    ) {
        val user = currentUser.value ?: return
        val updated = user.copy(
            name = name,
            city = city,
            bio = bio,
            occupation = occupation,
            education = education,
            lookingFor = lookingFor,
            interestsJson = interests.joinToString("|||"),
            lifestyleJson = lifestyle.joinToString("|||"),
            promptQuestion = promptQ,
            promptAnswer = promptA
        )
        viewModelScope.launch {
            repository.updateCurrentUser(updated)
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.updateLocationAndRecalculateDistances(lat, lng)
        }
    }

    fun updateDiscoverFilter(filter: DiscoverFilterState) {
        _discoverFilter.value = filter
    }

    fun resetDiscoverFilter() {
        _discoverFilter.value = DiscoverFilterState()
    }
}
