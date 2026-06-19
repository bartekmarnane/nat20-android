package au.com.evonet.nat20.patron

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The single source of truth for whether the user has unlocked **Nat20 Patron**
 * — a one-time, non-consumable in-app purchase. Every premium gate in the app
 * keys off [isPatron], so when later perks land (e.g. the cross-table shared
 * journal, A23) they unlock from the same flag with no new plumbing.
 *
 * The Android counterpart of iOS's StoreKit-2 `PatronStore`: this wraps Google
 * Play Billing. Built once and held app-wide by `AppContainer`, alongside the
 * `CharacterStore` and `AppSettings`.
 */
class PatronStore(
    context: Context,
    private val scope: CoroutineScope,
) : PurchasesUpdatedListener {

    /** Whether the Patron unlock is currently owned. Drives every gate. */
    private val _isPatron = MutableStateFlow(false)
    val isPatron: StateFlow<Boolean> = _isPatron.asStateFlow()

    /** The loaded product, used for live price display. `null` until the async
     *  load finishes or if the store is unreachable. */
    private val _product = MutableStateFlow<ProductDetails?>(null)
    val product: StateFlow<ProductDetails?> = _product.asStateFlow()

    /** True while a purchase flow is in flight, so the paywall can show a
     *  spinner and disable its button. */
    private val _purchaseInFlight = MutableStateFlow(false)
    val purchaseInFlight: StateFlow<Boolean> = _purchaseInFlight.asStateFlow()

    /** User-facing copy for the most recent failure, surfaced by the paywall.
     *  Cleared at the start of each new attempt. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** Localised price string (e.g. "$4.99") drawn live from Play, or `null`
     *  while the product is still loading. */
    val priceText: String?
        get() = _product.value?.oneTimePurchaseOfferDetails?.formattedPrice

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    /** In-flight connection attempt, shared so concurrent callers await one. */
    private var connecting: CompletableDeferred<Boolean>? = null

    init {
        scope.launch {
            if (ensureConnected()) {
                loadProduct()
                refreshEntitlement()
            }
        }
    }

    // MARK: - Connection

    /** Suspends until the billing service is connected, reconnecting on demand.
     *  Returns false if the service is unavailable on this device. */
    private suspend fun ensureConnected(): Boolean {
        if (billingClient.connectionState == BillingClient.ConnectionState.CONNECTED) return true
        connecting?.let { return it.await() }

        val deferred = CompletableDeferred<Boolean>()
        connecting = deferred
        billingClient.startConnection(object : com.android.billingclient.api.BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = null
                if (!deferred.isCompleted) {
                    deferred.complete(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }

            override fun onBillingServiceDisconnected() {
                connecting = null
                if (!deferred.isCompleted) deferred.complete(false)
            }
        })
        return deferred.await()
    }

    // MARK: - Loading

    suspend fun loadProduct() {
        if (!ensureConnected()) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _product.value = result.productDetailsList?.firstOrNull()
        } else {
            _lastError.value = "Couldn't reach the store. Check your connection and try again."
        }
    }

    /** Recomputes [isPatron] from the user's current purchases — cheap and
     *  idempotent; call it after any purchase or restore. Also acknowledges any
     *  still-unacknowledged purchase (Play auto-refunds otherwise). */
    suspend fun refreshEntitlement() {
        if (!ensureConnected()) return
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        val result = billingClient.queryPurchasesAsync(params)
        val owned = result.purchasesList.any { purchase ->
            PRODUCT_ID in purchase.products && purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        result.purchasesList.forEach { acknowledgeIfNeeded(it) }
        _isPatron.value = owned
    }

    // MARK: - Purchase & restore

    /** Launches the Play purchase flow. The outcome arrives asynchronously on
     *  [onPurchasesUpdated]. Needs the hosting [Activity] to present the sheet. */
    fun purchase(activity: Activity) {
        val details = _product.value
        if (details == null) {
            _lastError.value = "The Patron offer isn't available right now."
            scope.launch { loadProduct() }
            return
        }
        _lastError.value = null
        _purchaseInFlight.value = true

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build(),
                ),
            )
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseInFlight.value = false
            _lastError.value = "The purchase couldn't be started. Please try again."
        }
    }

    /** Restores a previously-bought unlock — for a reinstall or a new device.
     *  Play ties entitlements to the signed-in account, so this is just a
     *  re-query. */
    fun restore() {
        _lastError.value = null
        scope.launch {
            refreshEntitlement()
            if (!_isPatron.value) {
                _lastError.value = "No previous Patron purchase was found on this account."
            }
        }
    }

    // MARK: - Purchase listener

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _purchaseInFlight.value = false
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch {
                purchases?.forEach { acknowledgeIfNeeded(it) }
                refreshEntitlement()
            }
            // Bought already (e.g. on another device) — just reconcile.
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { refreshEntitlement() }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _lastError.value = "The purchase couldn't be completed. Please try again."
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params)
        }
    }

    companion object {
        /** Non-consumable product ID. Must match the in-app product configured
         *  in the Play Console. Mirrors the iOS StoreKit product id. */
        const val PRODUCT_ID = "au.com.evonet.Nat20.patron"

        /** How many characters a non-Patron may keep. Reaching this turns the
         *  roster's "New Character" CTA into "Upgrade". Existing characters above
         *  the limit (e.g. the debug demo seed) stay fully usable — the gate only
         *  blocks *creating* the next one. */
        const val FREE_CHARACTER_LIMIT = 1

        /** Pure gate predicate (testable without Play): may the user create
         *  another character given the current roster size? */
        fun canCreateMore(characterCount: Int, isPatron: Boolean): Boolean =
            isPatron || characterCount < FREE_CHARACTER_LIMIT
    }
}
