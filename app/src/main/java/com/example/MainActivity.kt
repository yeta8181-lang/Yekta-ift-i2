package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.billing.GooglePlayBillingManager
import com.example.ui.components.ReportBlockDialog
import com.example.ui.components.SafeMatchCelebrationDialog
import com.example.ui.components.SafetyCenterDialog
import com.example.ui.components.VipPaywallDialog
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MatchesAndChatScreen
import com.example.ui.screens.MomentsScreen
import com.example.ui.screens.MyProfileScreen
import com.example.ui.screens.NearbyMapScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.VipSubscriptionScreen
import com.example.ui.theme.CupidGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NightCard
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightSurface
import com.example.ui.theme.RoseAccent
import com.example.ui.viewmodel.DatingViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    DISCOVER("Keşfet", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    MAP("Harita", Icons.Filled.Radar, Icons.Outlined.Radar),
    MOMENTS("Anlar", Icons.Filled.PhotoCamera, Icons.Outlined.PhotoCamera),
    CHAT("Mesajlar", Icons.Filled.Forum, Icons.Outlined.Forum),
    PROFILE("Profilim", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    private val viewModel: DatingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DatingAppMain(viewModel = viewModel)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DatingAppMain(viewModel: DatingViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(MainTab.DISCOVER) }

    // Google Play Billing Manager (VIP Subscription Management)
    val billingManager = remember(context) {
        GooglePlayBillingManager(context) { isVip ->
            viewModel.setVipStatus(isVip)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, billingManager) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                billingManager.onResumeRefresh()
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                billingManager.endConnection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isBillingConnected by billingManager.isConnected.collectAsStateWithLifecycle()
    val availablePlans by billingManager.availablePlans.collectAsStateWithLifecycle()
    val selectedPlanId by billingManager.selectedPlanId.collectAsStateWithLifecycle()
    val activeSubscription by billingManager.activeSubscription.collectAsStateWithLifecycle()
    val isProcessingPurchase by billingManager.isProcessing.collectAsStateWithLifecycle()
    val isPendingPurchase by billingManager.isPendingPurchase.collectAsStateWithLifecycle()
    val purchaseMessage by billingManager.purchaseStatusMessage.collectAsStateWithLifecycle()
    LaunchedEffect(purchaseMessage) {
        purchaseMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            billingManager.clearStatusMessage()
        }
    }

    // State from ViewModel
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val likedMeProfiles by viewModel.likedMeProfiles.collectAsStateWithLifecycle()
    val moments by viewModel.moments.collectAsStateWithLifecycle()
    val distanceFilter by viewModel.distanceFilterKm.collectAsStateWithLifecycle()
    val discoverFilter by viewModel.discoverFilter.collectAsStateWithLifecycle()
    val selectedMapUser by viewModel.selectedMapUser.collectAsStateWithLifecycle()
    val activeConversationUser by viewModel.activeConversationUser.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val matchCelebration by viewModel.matchCelebration.collectAsStateWithLifecycle()
    val safetyDialogOpen by viewModel.safetyDialogOpen.collectAsStateWithLifecycle()
    val vipDialogOpen by viewModel.vipDialogOpen.collectAsStateWithLifecycle()
    val reportBlockUser by viewModel.reportBlockDialogUser.collectAsStateWithLifecycle()
    val verificationNotice by viewModel.verificationSuccessNotice.collectAsStateWithLifecycle()

    // Auth & Setup states
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val isSettingUpProfile by viewModel.isSettingUpProfile.collectAsStateWithLifecycle()
    val googleUserInfo by viewModel.googleUserInfo.collectAsStateWithLifecycle()
    val isCloudActive by viewModel.isCloudActive.collectAsStateWithLifecycle()
    val isLocationTrackingActive by viewModel.isLocationTrackingActive.collectAsStateWithLifecycle()

    val isUserAdmin = currentUser?.isAdmin == true || viewModel.isCurrentUserAdmin()
    val isUserVip = currentUser?.isVip == true || isUserAdmin

    // Google Location API permission & continuous tracking launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            viewModel.refreshLocationNow { updated ->
                val message = if (updated) {
                    "Canlı GPS konumu güncellendi! Yakındaki kişiler haritada sıralandı 📡"
                } else {
                    "Canlı konum takibi başlatıldı 📡"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Yakındaki kişileri görmek için konum izni gereklidir.", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestLocation() {
        val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshLocationNow { updated ->
                val msg = if (updated) "Konum güncellendi! 📍" else "Canlı GPS taraması aktif 📍"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // Automatically start or request location tracking when entering the Map tab
    LaunchedEffect(currentTab, isSignedIn) {
        if (isSignedIn && currentTab == MainTab.MAP) {
            val fineCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarseCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fineCheck == PackageManager.PERMISSION_GRANTED || coarseCheck == PackageManager.PERMISSION_GRANTED) {
                viewModel.startContinuousLocationTracking()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }
    }

    // Show verification success notice
    LaunchedEffect(verificationNotice) {
        if (verificationNotice) {
            snackbarHostState.showSnackbar("Tebrikler! Yüz doğrulamanız tamamlandı ve Mavi Tik rozetiniz eklendi ✓")
            viewModel.dismissVerificationNotice()
        }
    }

    // Step 1: If not signed in with Google/Email, display Login screen
    if (!isSignedIn) {
        LoginScreen(
            onGoogleSignInWithToken = { idToken, name, email, avatarUrl, onError ->
                viewModel.onGoogleSignInWithToken(
                    idToken = idToken,
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    onError = onError
                )
            },
            onRegisterWithEmail = { name, email, password, onError ->
                viewModel.onRegisterWithEmail(
                    name = name,
                    email = email,
                    password = password,
                    onError = onError
                )
            },
            onLoginWithEmail = { email, password, onError ->
                viewModel.onLoginWithEmail(
                    email = email,
                    password = password,
                    onError = onError
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        )
        return
    }

    // Step 2: If signed in with Google but hasn't completed profile details & photo upload, display ProfileSetupScreen
    if (isSettingUpProfile || (currentUser != null && !currentUser!!.isProfileSetupCompleted)) {
        val (name, email, avatar) = googleUserInfo ?: Triple(
            currentUser?.name ?: "Yeni Üye",
            currentUser?.email ?: "uye@gmail.com",
            currentUser?.photoList?.firstOrNull() ?: ""
        )
        ProfileSetupScreen(
            initialName = name,
            initialEmail = email,
            initialAvatarUrl = avatar,
            initialLatitude = currentUser?.latitude ?: 41.0082,
            initialLongitude = currentUser?.longitude ?: 28.9784,
            onRequestLocationUpdate = { requestLocation() },
            onCompleteSetup = { profile ->
                viewModel.completeProfileSetup(profile)
                Toast.makeText(context, "Profiliniz başarıyla oluşturuldu! Hoş geldiniz ❤️", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        )
        return
    }

    // Step 3: Main Dating App Interface
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Hide bottom navigation when inside a chat conversation or VIP subscription screen
            if (activeConversationUser == null && !vipDialogOpen) {
                NavigationBar(
                    containerColor = NightCard,
                    contentColor = Color.White,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_bottom_nav")
                ) {
                    MainTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                if (tab == MainTab.CHAT && matches.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${matches.size}") } }) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else if (tab == MainTab.PROFILE && likedMeProfiles.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${likedMeProfiles.size}") } }) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = { Text(tab.title, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = RoseAccent,
                                indicatorColor = RoseAccent,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .background(NightSurface)
        ) {
            if (vipDialogOpen) {
                // Dedicated Google Play Subscription Management Screen
                VipSubscriptionScreen(
                    isVip = isUserVip,
                    isBillingConnected = isBillingConnected,
                    isProcessing = isProcessingPurchase,
                    isPendingPurchase = isPendingPurchase,
                    availablePlans = availablePlans,
                    selectedPlanId = selectedPlanId,
                    activeSubscription = activeSubscription,
                    onSelectPlan = { planId -> billingManager.selectPlan(planId) },
                    onPurchaseSelectedPlan = { planId ->
                        billingManager.launchVipPurchaseFlow(activity, planId)
                    },
                    onRestorePurchases = {
                        billingManager.queryExistingSubscriptions(silent = false)
                    },
                    onManageInPlayStore = {
                        billingManager.openPlayStoreSubscriptions(context, selectedPlanId)
                    },
                    onCancelSubscription = {
                        billingManager.cancelActiveSubscription()
                    },
                    onBack = { viewModel.closeVipDialog() }
                )
            } else if (activeConversationUser != null) {
                // Active Conversation Detail View
                ChatDetailScreen(
                    user = activeConversationUser!!,
                    messages = activeMessages,
                    isVip = isUserVip,
                    onSendMessage = { text, uri, type ->
                        viewModel.sendMessage(text, uri, type)
                    },
                    onBack = { viewModel.closeConversation() },
                    onReportOrBlock = { user -> viewModel.openReportBlockDialog(user) },
                    onOpenVipPaywall = { viewModel.openVipDialog() }
                )
            } else {
                // Main Tabs
                when (currentTab) {
                    MainTab.DISCOVER -> {
                        DiscoverScreen(
                            profiles = allProfiles,
                            currentUser = currentUser,
                            moments = moments,
                            isVip = isUserVip,
                            isAdmin = isUserAdmin,
                            onLike = { profile, isSuperLike ->
                                viewModel.likeProfile(profile, isSuperLike)
                            },
                            onPass = { profile ->
                                viewModel.passProfile(profile)
                            },
                            onOpenSafetyCenter = { viewModel.openSafetyDialog() },
                            onOpenReportDialog = { profile -> viewModel.openReportBlockDialog(profile) },
                            onOpenVipPaywall = { viewModel.openVipDialog() },
                            onPublishMoment = { caption, uri, type, location ->
                                if (viewModel.shareMoment(caption, uri, type, location)) {
                                    Toast.makeText(context, "Keşfette paylaşımınız yayınlandı! ✨", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onToggleMomentLike = { viewModel.toggleMomentLike(it) },
                            onCommentMoment = { moment, commentText ->
                                if (viewModel.commentOnMoment(moment, commentText)) {
                                    Toast.makeText(context, "VIP yorumunuz paylaşıldı! 💬", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteMomentComment = { moment, commentId ->
                                viewModel.deleteCommentOnMoment(moment, commentId)
                                Toast.makeText(context, "Yorum paylaşımdan kaldırıldı.", Toast.LENGTH_SHORT).show()
                            },
                            onReportMomentComment = { moment, comment, reason, blockAuthor ->
                                viewModel.reportCommentOnMoment(moment, comment, reason, blockAuthor)
                                val msg = if (blockAuthor) {
                                    "Yorum bildirildi ve ${comment.authorName} engellendi."
                                } else {
                                    "Yorum bildirildi ve paylaşımdan kaldırıldı."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onBlockMomentCommentAuthor = { moment, comment ->
                                viewModel.blockCommentAuthorOnMoment(moment, comment)
                                Toast.makeText(context, "${comment.authorName} engellendi ve yorumları kaldırıldı.", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteMoment = { moment ->
                                viewModel.deleteMoment(moment)
                                Toast.makeText(context, "Gönderi silindi.", Toast.LENGTH_SHORT).show()
                            },
                            onAdminDeleteUser = { profile ->
                                viewModel.adminDeleteUser(profile.id)
                                Toast.makeText(context, "${profile.name} kullanıcısı yönetici tarafından silindi.", Toast.LENGTH_SHORT).show()
                            },
                            discoverFilter = discoverFilter,
                            onUpdateDiscoverFilter = { newFilter ->
                                viewModel.updateDiscoverFilter(newFilter)
                            }
                        )
                    }

                    MainTab.MAP -> {
                        NearbyMapScreen(
                            profiles = allProfiles,
                            currentUser = currentUser,
                            distanceFilterKm = distanceFilter,
                            selectedUser = selectedMapUser,
                            isVip = isUserVip,
                            isLocationTrackingActive = isLocationTrackingActive,
                            onDistanceFilterChange = { viewModel.setDistanceFilter(it) },
                            onSelectUser = { viewModel.selectMapUser(it) },
                            onLikeUser = { profile -> viewModel.likeProfile(profile) },
                            onStartChat = { profile ->
                                viewModel.selectMapUser(null)
                                viewModel.openConversation(profile)
                            },
                            onRequestLocationUpdate = { requestLocation() },
                            onOpenVipPaywall = { viewModel.openVipDialog() }
                        )
                    }

                    MainTab.MOMENTS -> {
                        MomentsScreen(
                            moments = moments,
                            currentUserId = currentUser?.id.orEmpty(),
                            isVip = isUserVip,
                            isAdmin = isUserAdmin,
                            onToggleLike = { viewModel.toggleMomentLike(it) },
                            onCommentMoment = { moment, commentText ->
                                if (viewModel.commentOnMoment(moment, commentText)) {
                                    Toast.makeText(context, "VIP yorumunuz paylaşıldı! 💬", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteComment = { moment, commentId ->
                                viewModel.deleteCommentOnMoment(moment, commentId)
                                Toast.makeText(context, "Yorum paylaşımdan kaldırıldı.", Toast.LENGTH_SHORT).show()
                            },
                            onReportComment = { moment, comment, reason, blockAuthor ->
                                viewModel.reportCommentOnMoment(moment, comment, reason, blockAuthor)
                                val msg = if (blockAuthor) {
                                    "Yorum bildirildi ve ${comment.authorName} engellendi."
                                } else {
                                    "Yorum bildirildi ve paylaşımdan kaldırıldı."
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onBlockCommentAuthor = { moment, comment ->
                                viewModel.blockCommentAuthorOnMoment(moment, comment)
                                Toast.makeText(context, "${comment.authorName} engellendi ve yorumları kaldırıldı.", Toast.LENGTH_SHORT).show()
                            },
                            onPublishMoment = { caption, uri, type, location ->
                                if (viewModel.shareMoment(caption, uri, type, location)) {
                                    Toast.makeText(context, "Anınız paylaşıldı!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteMoment = { moment ->
                                viewModel.deleteMoment(moment)
                                Toast.makeText(context, "Gönderi silindi.", Toast.LENGTH_SHORT).show()
                            },
                            onOpenVipPaywall = { viewModel.openVipDialog() }
                        )
                    }

                    MainTab.CHAT -> {
                        MatchesAndChatScreen(
                            matches = matches,
                            allProfiles = allProfiles,
                            isVip = isUserVip,
                            onSelectConversation = { user -> viewModel.openConversation(user) },
                            onOpenVipPaywall = { viewModel.openVipDialog() }
                        )
                    }

                    MainTab.PROFILE -> {
                        val credentialManager = remember(context) {
                            androidx.credentials.CredentialManager.create(context)
                        }
                        MyProfileScreen(
                            user = currentUser,
                            likedMeUsers = likedMeProfiles,
                            isAdmin = isUserAdmin,
                            allProfiles = allProfiles,
                            allMoments = moments,
                            onLikeUser = { profile ->
                                viewModel.likeProfile(profile)
                                Toast.makeText(context, "${profile.name} beğenildi! ❤️", Toast.LENGTH_SHORT).show()
                            },
                            onStartChat = { profile ->
                                viewModel.openConversation(profile)
                            },
                            onAdminDeleteUser = { profile ->
                                viewModel.adminDeleteUser(profile.id)
                                Toast.makeText(context, "${profile.name} kullanıcısı yönetici tarafından silindi.", Toast.LENGTH_SHORT).show()
                            },
                            onAdminDeleteMoment = { moment ->
                                viewModel.deleteMoment(moment)
                                Toast.makeText(context, "Gönderi yönetici tarafından silindi.", Toast.LENGTH_SHORT).show()
                            },
                            onAddPhoto = { uri ->
                                viewModel.addPhotoToCurrentUser(uri)
                                Toast.makeText(context, "Fotoğraf eklendi!", Toast.LENGTH_SHORT).show()
                            },
                            onUpdateProfile = { name, city, bio, occupation, education, lookingFor, interests, lifestyle, promptQ, promptA ->
                                viewModel.updateMyProfile(name, city, bio, occupation, education, lookingFor, interests, lifestyle, promptQ, promptA)
                                Toast.makeText(context, "Profil güncellendi!", Toast.LENGTH_SHORT).show()
                            },
                            onOpenSafetyCenter = { viewModel.openSafetyDialog() },
                            onVerifySelfie = { viewModel.verifySelfie() },
                            onOpenVipPaywall = { viewModel.openVipDialog() },
                            onSignOut = {
                                com.example.ui.screens.signOut(
                                    credentialManager = credentialManager,
                                    onSignOutComplete = {
                                        viewModel.onSignOutComplete()
                                        Toast.makeText(context, "Çıkış yapıldı.", Toast.LENGTH_SHORT).show()
                                    },
                                    scope = scope
                                )
                            },
                            isCloudActive = isCloudActive
                        )
                    }
                }
            }

            // Global Safe Match Celebration Overlay
            matchCelebration?.let { matchedUser ->
                SafeMatchCelebrationDialog(
                    matchedUser = matchedUser,
                    currentUser = currentUser,
                    onStartChat = {
                        viewModel.dismissMatchCelebration()
                        viewModel.openConversation(matchedUser)
                    },
                    onKeepBrowsing = {
                        viewModel.dismissMatchCelebration()
                    }
                )
            }

            // Global Safety Center Dialog
            if (safetyDialogOpen) {
                SafetyCenterDialog(
                    isVerified = currentUser?.isVerified ?: false,
                    onDismiss = { viewModel.closeSafetyDialog() },
                    onRequestVerification = {
                        viewModel.verifySelfie()
                        viewModel.closeSafetyDialog()
                    }
                )
            }

            // Global Report / Block Dialog
            reportBlockUser?.let { targetUser ->
                ReportBlockDialog(
                    user = targetUser,
                    onDismiss = { viewModel.closeReportBlockDialog() },
                    onBlockConfirmed = {
                        viewModel.blockUser(targetUser.id)
                        Toast.makeText(context, "${targetUser.name} engellendi.", Toast.LENGTH_SHORT).show()
                    },
                    onReportConfirmed = { reason ->
                        viewModel.reportUser(targetUser.id, reason)
                        Toast.makeText(context, "Şikayetiniz alındı. Kullanıcı engellendi.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
