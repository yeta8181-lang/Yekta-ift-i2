package com.example.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VipSubscriptionPlan(
    val productId: String,
    val basePlanId: String,
    val productType: String = BillingClient.ProductType.SUBS,
    val title: String,
    val periodLabel: String,
    val formattedPrice: String,
    val badgeText: String? = null,
    val description: String,
    val isPlayStoreVerified: Boolean = false,
    val productDetails: ProductDetails? = null
)

data class ActiveSubscriptionInfo(
    val productId: String,
    val planTitle: String,
    val priceDisplay: String,
    val purchaseTimeMillis: Long,
    val isAutoRenewing: Boolean,
    val isAcknowledged: Boolean,
    val orderId: String = "",
    val purchaseToken: String = ""
)

class GooglePlayBillingManager(
    private val context: Context,
    private val onVipPurchased: (isVip: Boolean) -> Unit
) : PurchasesUpdatedListener {

    companion object {
        const val VIP_WEEKLY_ID = "vipweekly100tl"
        const val VIP_PRODUCT_ID = "vipmonthly400tl"
        const val VIP_YEARLY_ID = "vipyearly900tl"
        const val VIP_PRICE_DISPLAY = "400,00 ₺ / Ay"

        val ALL_PRODUCT_IDS = listOf(
            VIP_WEEKLY_ID,
            VIP_PRODUCT_ID,
            VIP_YEARLY_ID
        )

        val DEFAULT_PLANS = listOf(
            VipSubscriptionPlan(
                productId = VIP_WEEKLY_ID,
                basePlanId = "weeklybase",
                productType = BillingClient.ProductType.SUBS,
                title = "Haftalık VIP Başlangıç",
                periodLabel = "Haftalık",
                formattedPrice = "100,00 ₺ / Hafta",
                badgeText = "Hızlı Deneme",
                description = "7 gün boyunca sınırsız sohbet, keşfette paylaşım ve 100 km radar."
            ),
            VipSubscriptionPlan(
                productId = VIP_PRODUCT_ID,
                basePlanId = "monthlybase",
                productType = BillingClient.ProductType.SUBS,
                title = "Aylık VIP Üyelik",
                periodLabel = "Aylık",
                formattedPrice = "400,00 ₺ / Ay",
                badgeText = "EN POPÜLER",
                description = "Tüm sohbet özellikleri, keşfet/anlar paylaşımı ve sınırsız kaydırma."
            ),
            VipSubscriptionPlan(
                productId = VIP_YEARLY_ID,
                basePlanId = "yearlybase",
                productType = BillingClient.ProductType.SUBS,
                title = "Yıllık VIP Avantaj",
                periodLabel = "Yıllık",
                formattedPrice = "900,00 ₺ / Yıl",
                badgeText = "%81 AVANTAJLI",
                description = "Yıl boyu kesintisiz VIP sohbet, keşfet paylaşımı ve öncelikli profil gösterimi."
            )
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val billingPrefs = context.applicationContext.getSharedPreferences(
        "kalpkalbe_play_billing_prefs",
        Context.MODE_PRIVATE
    )

    private var reconnectDelayMs = 1500L

    private var billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _availablePlans = MutableStateFlow(DEFAULT_PLANS)
    val availablePlans: StateFlow<List<VipSubscriptionPlan>> = _availablePlans.asStateFlow()

    private val _selectedPlanId = MutableStateFlow(VIP_PRODUCT_ID)
    val selectedPlanId: StateFlow<String> = _selectedPlanId.asStateFlow()

    private val _activeSubscription = MutableStateFlow<ActiveSubscriptionInfo?>(null)
    val activeSubscription: StateFlow<ActiveSubscriptionInfo?> = _activeSubscription.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isPendingPurchase = MutableStateFlow(false)
    val isPendingPurchase: StateFlow<Boolean> = _isPendingPurchase.asStateFlow()

    private val _vipProductAvailable = MutableStateFlow(true)
    val vipProductAvailable: StateFlow<Boolean> = _vipProductAvailable.asStateFlow()

    private val _purchaseStatusMessage = MutableStateFlow<String?>(null)
    val purchaseStatusMessage: StateFlow<String?> = _purchaseStatusMessage.asStateFlow()

    init {
        restoreCachedSubscriptionInfo()
        startConnection()
    }

    private fun restoreCachedSubscriptionInfo() {
        val cachedProductId = billingPrefs.getString("active_product_id", null) ?: return
        val cachedTitle = billingPrefs.getString("active_plan_title", "Aylık VIP Üyelik") ?: "Aylık VIP Üyelik"
        val cachedPrice = billingPrefs.getString("active_price_display", VIP_PRICE_DISPLAY) ?: VIP_PRICE_DISPLAY
        val cachedTime = billingPrefs.getLong("active_purchase_time", System.currentTimeMillis())
        val cachedAutoRenew = billingPrefs.getBoolean("active_auto_renew", true)
        val cachedOrderId = billingPrefs.getString("active_order_id", "") ?: ""
        val cachedToken = billingPrefs.getString("active_purchase_token", "") ?: ""

        _activeSubscription.value = ActiveSubscriptionInfo(
            productId = cachedProductId,
            planTitle = cachedTitle,
            priceDisplay = cachedPrice,
            purchaseTimeMillis = cachedTime,
            isAutoRenewing = cachedAutoRenew,
            isAcknowledged = true,
            orderId = cachedOrderId,
            purchaseToken = cachedToken
        )
    }

    private fun saveSubscriptionInfoToPrefs(info: ActiveSubscriptionInfo, fromPlayStore: Boolean) {
        billingPrefs.edit()
            .putString("active_product_id", info.productId)
            .putString("active_plan_title", info.planTitle)
            .putString("active_price_display", info.priceDisplay)
            .putLong("active_purchase_time", info.purchaseTimeMillis)
            .putBoolean("active_auto_renew", info.isAutoRenewing)
            .putString("active_order_id", info.orderId)
            .putString("active_purchase_token", info.purchaseToken)
            .putBoolean("from_play_store", fromPlayStore)
            .apply()
    }

    private fun clearSubscriptionPrefs() {
        billingPrefs.edit()
            .remove("active_product_id")
            .remove("active_plan_title")
            .remove("active_price_display")
            .remove("active_purchase_time")
            .remove("active_auto_renew")
            .remove("active_order_id")
            .remove("active_purchase_token")
            .remove("from_play_store")
            .apply()
    }

    fun selectPlan(productId: String) {
        _selectedPlanId.value = productId
    }

    fun onResumeRefresh() {
        if (billingClient.isReady) {
            queryVipProductDetails()
            queryExistingSubscriptions(silent = true)
        } else {
            startConnection()
        }
    }

    fun endConnection() {
        try {
            mainHandler.removeCallbacksAndMessages(null)
            if (billingClient.isReady) {
                billingClient.endConnection()
            }
            _isConnected.value = false
        } catch (e: Exception) {
            Log.w("GooglePlayBilling", "Error ending BillingClient connection: ${e.message}")
        }
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            _isConnected.value = true
            queryVipProductDetails()
            queryExistingSubscriptions(silent = true)
            onConnected?.invoke()
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _isConnected.value = true
                        reconnectDelayMs = 1500L
                        queryVipProductDetails()
                        queryExistingSubscriptions(silent = true)
                        onConnected?.invoke()
                    } else {
                        Log.w(
                            "GooglePlayBilling",
                            "Billing setup response (${billingResult.responseCode}): ${billingResult.debugMessage}"
                        )
                        _isConnected.value = false
                        onConnected?.invoke()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _isConnected.value = false
                    scheduleReconnect()
                }
            })
        } catch (e: Exception) {
            Log.w("GooglePlayBilling", "Failed to start BillingClient connection: ${e.message}")
            _isConnected.value = false
            onConnected?.invoke()
        }
    }

    private fun scheduleReconnect() {
        mainHandler.postDelayed({
            if (!billingClient.isReady) {
                startConnection()
            }
        }, reconnectDelayMs)
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
    }

    /**
     * Queries BOTH Subscriptions (ProductType.SUBS) and One-Time In-App Products (ProductType.INAPP)
     * from Google Play so whichever product type is configured in Play Console is automatically matched.
     */
    fun queryVipProductDetails(onComplete: (() -> Unit)? = null) {
        if (!billingClient.isReady) {
            onComplete?.invoke()
            return
        }

        val subsProducts = ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(subsProducts)
            .build()

        val inAppProducts = ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(inAppProducts)
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { subsResult, subsDetailsResult ->
            val subsDetailsMap = if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                subsDetailsResult.productDetailsList?.associateBy { it.productId }.orEmpty()
            } else {
                emptyMap()
            }

            billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsResult ->
                val inAppDetailsMap = if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    inAppDetailsResult.productDetailsList?.associateBy { it.productId }.orEmpty()
                } else {
                    emptyMap()
                }

                val combinedMap = inAppDetailsMap + subsDetailsMap
                if (combinedMap.isNotEmpty()) {
                    _vipProductAvailable.value = true
                    _availablePlans.value = DEFAULT_PLANS.map { defaultPlan ->
                        val detail = combinedMap[defaultPlan.productId]
                        if (detail != null) {
                            val livePrice = extractFormattedPrice(detail) ?: defaultPlan.formattedPrice
                            defaultPlan.copy(
                                productType = detail.productType,
                                formattedPrice = livePrice,
                                isPlayStoreVerified = true,
                                productDetails = detail
                            )
                        } else {
                            defaultPlan
                        }
                    }
                }
                onComplete?.invoke()
            }
        }
    }

    private fun extractFormattedPrice(detail: ProductDetails): String? {
        return if (detail.productType == BillingClient.ProductType.SUBS) {
            detail.subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.lastOrNull()
                ?.formattedPrice
        } else {
            detail.oneTimePurchaseOfferDetails?.formattedPrice
        }
    }

    /**
     * Queries active Google Play purchases across both SUBS and INAPP product types.
     */
    fun queryExistingSubscriptions(silent: Boolean = false) {
        if (!billingClient.isReady) {
            if (!silent) {
                _purchaseStatusMessage.value = "Google Play Store bağlantısı kuruluyor..."
                startConnection {
                    if (billingClient.isReady) {
                        queryExistingSubscriptions(silent = false)
                    } else {
                        _purchaseStatusMessage.value =
                            "Google Play Store servisine ulaşılamadı. Lütfen Google Play hesabınızın açık olduğundan emin olun."
                    }
                }
            }
            return
        }

        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        val inAppParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                val allPurchases = buildList {
                    if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        addAll(subsPurchases)
                    }
                    if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        addAll(inAppPurchases)
                    }
                }

                val pendingPurchase = allPurchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PENDING
                }
                _isPendingPurchase.value = pendingPurchase != null

                val activePurchase = allPurchases.firstOrNull {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (activePurchase != null) {
                    handlePurchase(activePurchase, isRestore = !silent)
                } else if (pendingPurchase != null) {
                    if (!silent) {
                        _purchaseStatusMessage.value =
                            "Google Play üzerinde bekleyen bir ödemeniz bulunuyor. Onaylandığında VIP otomatik aktifleşecektir."
                    }
                } else {
                    // If user previously had a Play Store subscription that is no longer active on Google Play, revoke VIP
                    val wasFromPlayStore = billingPrefs.getBoolean("from_play_store", false)
                    if (wasFromPlayStore) {
                        clearSubscriptionPrefs()
                        _activeSubscription.value = null
                        onVipPurchased(false)
                    }
                    if (!silent) {
                        _purchaseStatusMessage.value =
                            "Google Play hesabınızda aktif bir VIP abonelik veya satın alım bulunamadı."
                    }
                }
            }
        }
    }

    /**
     * Launches the official Google Play Billing purchase flow (BillingClient.launchBillingFlow).
     */
    fun launchVipPurchaseFlow(activity: Activity?, planId: String = _selectedPlanId.value) {
        _selectedPlanId.value = planId
        if (activity == null) {
            _purchaseStatusMessage.value = "Satın alma penceresi başlatılamadı (Activity bulunamadı)."
            return
        }

        _isProcessing.value = true

        if (!billingClient.isReady) {
            startConnection {
                if (billingClient.isReady) {
                    queryAndLaunchProduct(activity, planId)
                } else {
                    _isProcessing.value = false
                    _purchaseStatusMessage.value =
                        "Google Play Faturalandırma servisi kullanılamıyor. Lütfen cihazınızda Google Play Store'da oturum açın."
                }
            }
            return
        }

        queryAndLaunchProduct(activity, planId)
    }

    private fun queryAndLaunchProduct(activity: Activity, planId: String) {
        val cachedDetail = _availablePlans.value.find { it.productId == planId }?.productDetails
        if (cachedDetail != null) {
            launchBillingWithDetails(activity, cachedDetail)
            return
        }

        // Query both SUBS and INAPP for the requested planId
        val subsProduct = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(planId)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(subsProduct))
            .build()

        billingClient.queryProductDetailsAsync(subsParams) { subsResult, subsDetailsResult ->
            val subsDetail = subsDetailsResult.productDetailsList?.firstOrNull()
            if (subsResult.responseCode == BillingClient.BillingResponseCode.OK && subsDetail != null) {
                launchBillingWithDetails(activity, subsDetail)
                return@queryProductDetailsAsync
            }

            // Fallback check in INAPP products in case it was created as a one-time product in Play Console
            val inAppProduct = QueryProductDetailsParams.Product.newBuilder()
                .setProductId(planId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            val inAppParams = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(inAppProduct))
                .build()

            billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsResult ->
                val inAppDetail = inAppDetailsResult.productDetailsList?.firstOrNull()
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK && inAppDetail != null) {
                    launchBillingWithDetails(activity, inAppDetail)
                } else {
                    _isProcessing.value = false
                    Log.w(
                        "GooglePlayBilling",
                        "Product $planId not found in Play Console yet (subs=${subsResult.responseCode}, inapp=${inAppResult.responseCode})"
                    )
                    _purchaseStatusMessage.value =
                        "Google Play ödeme ekranı için '$planId' ürününün Play Console > Abonelikler bölümünde 'Aktif' olması gerekmektedir."
                }
            }
        }
    }

    private fun launchBillingWithDetails(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)

        if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerDetails = productDetails.subscriptionOfferDetails
            val selectedOffer = offerDetails?.firstOrNull { offer ->
                offer.basePlanId.isNotBlank()
            } ?: offerDetails?.firstOrNull()

            val offerToken = selectedOffer?.offerToken.orEmpty()
            if (offerToken.isEmpty()) {
                _isProcessing.value = false
                _purchaseStatusMessage.value =
                    "Google Play abonelik teklifi (Offer Token) bulunamadı. Lütfen Play Console'da Temel Plan (Base Plan) ekleyin."
                return
            }
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }

        val flowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParamsBuilder.build()))

        // Attach obfuscated account ID for Google Play fraud protection & attribution if signed in
        val currentUid = try {
            FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            null
        }
        if (!currentUid.isNullOrBlank()) {
            flowParamsBuilder.setObfuscatedAccountId(currentUid)
        }

        // If upgrading/downgrading an active Google Play subscription, pass SubscriptionUpdateParams
        val existingToken = _activeSubscription.value?.purchaseToken
        val existingProductId = _activeSubscription.value?.productId
        if (
            productDetails.productType == BillingClient.ProductType.SUBS &&
            !existingToken.isNullOrBlank() &&
            existingProductId != null &&
            existingProductId != productDetails.productId
        ) {
            val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(existingToken)
                .setSubscriptionReplacementMode(
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                )
                .build()
            flowParamsBuilder.setSubscriptionUpdateParams(updateParams)
        }

        val billingResult = billingClient.launchBillingFlow(activity, flowParamsBuilder.build())
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _isProcessing.value = false
            _purchaseStatusMessage.value =
                "Google Play ödeme ekranı açılamadı (${ mapBillingResponseCode(billingResult.responseCode) })."
        }
    }

    /**
     * Used only for local Robolectric tests or explicit developer verification.
     */
    fun simulateSuccessfulPurchase(planId: String = _selectedPlanId.value) {
        _isProcessing.value = false
        val plan = _availablePlans.value.find { it.productId == planId } ?: DEFAULT_PLANS[1]
        val info = ActiveSubscriptionInfo(
            productId = plan.productId,
            planTitle = plan.title,
            priceDisplay = plan.formattedPrice,
            purchaseTimeMillis = System.currentTimeMillis(),
            isAutoRenewing = plan.productType == BillingClient.ProductType.SUBS,
            isAcknowledged = true,
            orderId = "GPA.TEST-${System.currentTimeMillis()}",
            purchaseToken = "test_token_${plan.productId}"
        )
        _activeSubscription.value = info
        saveSubscriptionInfoToPrefs(info, fromPlayStore = false)
        _purchaseStatusMessage.value = "${plan.title} Aktifleştirildi! Sohbet ve Keşfet Paylaşımı Açıldı 👑"
        onVipPurchased(true)
    }

    fun cancelActiveSubscription() {
        clearSubscriptionPrefs()
        _activeSubscription.value = null
        _isPendingPurchase.value = false
        _purchaseStatusMessage.value = "VIP aboneliğiniz sonlandırıldı. Sohbet ve paylaşım özellikleri kilitlendi."
        onVipPurchased(false)
    }

    fun openPlayStoreSubscriptions(activityContext: Context, productId: String = _selectedPlanId.value) {
        try {
            val uri = Uri.parse(
                "https://play.google.com/store/account/subscriptions?sku=$productId&package=${activityContext.packageName}"
            )
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activityContext.startActivity(intent)
        } catch (e: Exception) {
            _purchaseStatusMessage.value = "Google Play Abonelikler sayfası açılamadı."
        }
    }

    fun clearStatusMessage() {
        _purchaseStatusMessage.value = null
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        _isProcessing.value = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase, isRestore = false)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseStatusMessage.value = "Google Play satın alma işlemi kullanıcı tarafından iptal edildi."
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseStatusMessage.value = "Bu VIP pakete zaten sahipsiniz! Aboneliğiniz geri yükleniyor..."
                queryExistingSubscriptions(silent = false)
            }
            else -> {
                _purchaseStatusMessage.value =
                    "Google Play ödeme hatası (${mapBillingResponseCode(billingResult.responseCode)}): ${billingResult.debugMessage}"
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, isRestore: Boolean = false) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                _isPendingPurchase.value = false
                val productId = purchase.products.firstOrNull() ?: _selectedPlanId.value
                val matchedPlan = _availablePlans.value.find { it.productId == productId } ?: DEFAULT_PLANS[1]

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()

                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            val info = ActiveSubscriptionInfo(
                                productId = matchedPlan.productId,
                                planTitle = matchedPlan.title,
                                priceDisplay = matchedPlan.formattedPrice,
                                purchaseTimeMillis = purchase.purchaseTime,
                                isAutoRenewing = purchase.isAutoRenewing,
                                isAcknowledged = true,
                                orderId = purchase.orderId.orEmpty(),
                                purchaseToken = purchase.purchaseToken
                            )
                            _activeSubscription.value = info
                            saveSubscriptionInfoToPrefs(info, fromPlayStore = true)
                            _purchaseStatusMessage.value =
                                "Google Play ile ${matchedPlan.title} başarıyla onaylandı ve aktifleştirildi! 👑"
                            onVipPurchased(true)
                        } else {
                            _purchaseStatusMessage.value =
                                "Satın alım onaylanırken hata oluştu: ${billingResult.debugMessage}"
                        }
                    }
                } else {
                    val info = ActiveSubscriptionInfo(
                        productId = matchedPlan.productId,
                        planTitle = matchedPlan.title,
                        priceDisplay = matchedPlan.formattedPrice,
                        purchaseTimeMillis = purchase.purchaseTime,
                        isAutoRenewing = purchase.isAutoRenewing,
                        isAcknowledged = true,
                        orderId = purchase.orderId.orEmpty(),
                        purchaseToken = purchase.purchaseToken
                    )
                    _activeSubscription.value = info
                    saveSubscriptionInfoToPrefs(info, fromPlayStore = true)
                    if (isRestore) {
                        _purchaseStatusMessage.value =
                            "Aktif VIP aboneliğiniz (${matchedPlan.title}) Google Play'den geri yüklendi 👑"
                    }
                    onVipPurchased(true)
                }
            }

            Purchase.PurchaseState.PENDING -> {
                _isPendingPurchase.value = true
                _purchaseStatusMessage.value =
                    "Ödemeniz Google Play tarafından işleniyor (Beklemede). Ödeme tamamlandığında VIP üyeliğiniz otomatik olarak açılacaktır."
            }

            else -> {
                _isPendingPurchase.value = false
            }
        }
    }

    private fun mapBillingResponseCode(code: Int): String {
        return when (code) {
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Zaman Aşımı"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Desteklenmeyen Özellik"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Bağlantı Kesildi"
            BillingClient.BillingResponseCode.USER_CANCELED -> "İptal Edildi"
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Servis Kullanılamıyor"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Faturalandırma Kullanılamıyor"
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "Ürün Play Store'da Bulunamadı"
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Yapılandırma Hatası"
            BillingClient.BillingResponseCode.ERROR -> "İşlem Hatası"
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Zaten Satın Alındı"
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Ürün Sahibi Değil"
            BillingClient.BillingResponseCode.NETWORK_ERROR -> "Ağ Bağlantı Hatası"
            else -> "Kod: $code"
        }
    }
}
