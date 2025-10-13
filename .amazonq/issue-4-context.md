# 1行日記アプリ 収益化機能実装 - AI作業指示書

## プロジェクト概要

**アプリ名**: 1行日記アプリ  
**開発言語**: Kotlin Multiplatform (KMP)  
**対象プラットフォーム**: Android / iOS  
**実装する機能**: 広告表示・課金システム

---

## 実装する収益化機能

### 1. バナー広告（AdMob）
- 日記一覧画面（メイン画面）の最下部
- 日記入力画面の最下部
- 設定画面の最下部

### 2. インタースティシャル広告（全画面広告）
- 日記投稿完了時に必須表示
- 広告を最後まで見ないと投稿が完了しない
- プレミアムユーザー（広告非表示購入者）は表示されない

### 3. 買い切り課金
- **広告非表示**: 400円（全ての広告を非表示）
- **Git連携機能**: 600円（既に実装済み、課金で開放するのみ）

### 4. 開発者モード
- デバッグビルド時は自動的に全機能アンロック・広告非表示
- リリースビルドでは無効

---

## 技術スタック

### 広告SDK
- **AdMob**: Google Mobile Ads SDK
- Android: `com.google.android.gms:play-services-ads`
- iOS: Google Mobile Ads SDK

### 課金SDK
- **Android**: Google Play Billing Library 6.x
- **iOS**: StoreKit 2
- バックエンドサーバーは不要（クライアント側で完結）

### 共通コード
- Kotlin Multiplatform (KMP)で課金状態管理
- 広告表示ロジックの共通化

---

## タスク一覧

### Phase 1: 環境セットアップ

#### Android
```gradle
// app/build.gradle.kts
dependencies {
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("com.android.billingclient:billing-ktx:6.2.1")
}
```

#### iOS
```ruby
# Podfile
pod 'Google-Mobile-Ads-SDK'
```

#### AdMobアカウント設定
1. AdMobアカウント作成
2. アプリ登録（Android/iOS）
3. 広告ユニットID取得
    - バナー広告用
    - インタースティシャル広告用
4. `AndroidManifest.xml` / `Info.plist` にAdMob App IDを追加

---

### Phase 2: KMP共通コード実装

#### ファイル: `shared/src/commonMain/.../PurchaseManager.kt`

```kotlin
// 課金状態管理の共通インターフェース
expect class PurchaseManager {
    suspend fun purchaseGitFeature(): PurchaseResult
    suspend fun purchaseRemoveAds(): PurchaseResult
    fun isGitFeaturePurchased(): Boolean
    fun isAdRemovalPurchased(): Boolean
    suspend fun restorePurchases(): RestoreResult
}

sealed class PurchaseResult {
    object Success : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    object Cancelled : PurchaseResult()
}

sealed class RestoreResult {
    data class Success(val restoredItems: List<String>) : RestoreResult()
    object NoItemsToRestore : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}
```

#### ファイル: `shared/src/commonMain/.../AdManager.kt`

```kotlin
// 広告表示制御の共通インターフェース
expect class AdManager {
    fun shouldShowBannerAd(): Boolean
    fun shouldShowInterstitialAd(): Boolean
    fun isAdRemovalActive(): Boolean
}
```

#### ファイル: `shared/src/commonMain/.../DeveloperMode.kt`

```kotlin
// 開発者モード（デバッグビルドのみ有効）
object DeveloperMode {
    fun isEnabled(): Boolean {
        // 各プラットフォームで実装
        return isDebugBuild()
    }
    
    fun unlockAllFeatures(): Boolean = isEnabled()
    fun removeAds(): Boolean = isEnabled()
}

expect fun isDebugBuild(): Boolean
```

---

### Phase 3: Android実装

#### ファイル: `androidMain/.../PurchaseManager.kt`

```kotlin
actual class PurchaseManager(private val activity: Activity) {
    private val billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
        .enablePendingPurchases()
        .build()
    
    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 接続成功
                }
            }
            override fun onBillingServiceDisconnected() {
                // 再接続ロジック
            }
        })
    }
    
    actual suspend fun purchaseGitFeature(): PurchaseResult {
        return launchPurchaseFlow("premium_git_sync")
    }
    
    actual suspend fun purchaseRemoveAds(): PurchaseResult {
        return launchPurchaseFlow("remove_ads")
    }
    
    actual fun isGitFeaturePurchased(): Boolean {
        if (DeveloperMode.unlockAllFeatures()) return true
        return checkPurchase("premium_git_sync")
    }
    
    actual fun isAdRemovalPurchased(): Boolean {
        if (DeveloperMode.removeAds()) return true
        return checkPurchase("remove_ads")
    }
    
    private fun checkPurchase(productId: String): Boolean {
        // SharedPreferences or DataStore から購入状態を確認
        val prefs = activity.getSharedPreferences("purchases", Context.MODE_PRIVATE)
        return prefs.getBoolean(productId, false)
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // 購入状態を保存
            val prefs = activity.getSharedPreferences("purchases", Context.MODE_PRIVATE)
            purchase.products.forEach { productId ->
                prefs.edit().putBoolean(productId, true).apply()
            }
        }
    }
    
    actual suspend fun restorePurchases(): RestoreResult {
        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        
        val restored = mutableListOf<String>()
        purchasesResult.purchasesList.forEach { purchase ->
            handlePurchase(purchase)
            restored.addAll(purchase.products)
        }
        
        return if (restored.isNotEmpty()) {
            RestoreResult.Success(restored)
        } else {
            RestoreResult.NoItemsToRestore
        }
    }
}
```

#### ファイル: `androidMain/.../AdManager.kt`

```kotlin
actual class AdManager(private val context: Context) {
    private val purchaseManager = PurchaseManager(context as Activity)
    
    actual fun shouldShowBannerAd(): Boolean {
        if (DeveloperMode.removeAds()) return false
        return !purchaseManager.isAdRemovalPurchased()
    }
    
    actual fun shouldShowInterstitialAd(): Boolean {
        if (DeveloperMode.removeAds()) return false
        return !purchaseManager.isAdRemovalPurchased()
    }
    
    actual fun isAdRemovalActive(): Boolean {
        return purchaseManager.isAdRemovalPurchased() || DeveloperMode.removeAds()
    }
}
```

#### ファイル: `androidMain/.../DeveloperMode.kt`

```kotlin
actual fun isDebugBuild(): Boolean {
    return BuildConfig.DEBUG
}
```

#### ファイル: `androidMain/.../BannerAdView.kt`

```kotlin
@Composable
fun BannerAdView() {
    val context = LocalContext.current
    val adManager = remember { AdManager(context) }
    
    if (!adManager.shouldShowBannerAd()) return
    
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 本番用ID
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
    )
}
```

#### ファイル: `androidMain/.../InterstitialAdManager.kt`

```kotlin
class InterstitialAdManager(private val activity: Activity) {
    private var interstitialAd: InterstitialAd? = null
    private val adManager = AdManager(activity)
    
    init {
        loadAd()
    }
    
    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            activity,
            "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX", // 本番用ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }
    
    fun showAdBeforeSave(onComplete: () -> Unit) {
        if (!adManager.shouldShowInterstitialAd()) {
            onComplete()
            return
        }
        
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // 広告を閉じた後に投稿完了
                    onComplete()
                    loadAd() // 次回用に再読み込み
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    // 広告表示失敗時も投稿完了
                    onComplete()
                    loadAd()
                }
            }
            ad.show(activity)
        } ?: run {
            // 広告が読み込まれていない場合はそのまま完了
            onComplete()
        }
    }
}
```

---

### Phase 4: iOS実装

#### ファイル: `iosMain/.../PurchaseManager.swift`

```swift
import StoreKit

actual class PurchaseManager {
    private let productIds: Set<String> = ["premium_git_sync", "remove_ads"]
    
    actual func purchaseGitFeature() async -> PurchaseResult {
        return await purchase(productId: "premium_git_sync")
    }
    
    actual func purchaseRemoveAds() async -> PurchaseResult {
        return await purchase(productId: "remove_ads")
    }
    
    actual func isGitFeaturePurchased() -> Bool {
        #if DEBUG
        return true
        #endif
        return checkEntitlement(for: "premium_git_sync")
    }
    
    actual func isAdRemovalPurchased() -> Bool {
        #if DEBUG
        return true
        #endif
        return checkEntitlement(for: "remove_ads")
    }
    
    private func purchase(productId: String) async -> PurchaseResult {
        guard let product = try? await Product.products(for: [productId]).first else {
            return .failure(.productNotFound)
        }
        
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                // 購入成功
                return .success
            case .userCancelled:
                return .cancelled
            case .pending:
                return .pending
            @unknown default:
                return .failure(.unknown)
            }
        } catch {
            return .failure(.error(error.localizedDescription))
        }
    }
    
    private func checkEntitlement(for productId: String) -> Bool {
        var isEntitled = false
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                if transaction.productID == productId {
                    isEntitled = true
                    break
                }
            }
        }
        return isEntitled
    }
    
    actual func restorePurchases() async -> RestoreResult {
        var restoredItems: [String] = []
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result {
                restoredItems.append(transaction.productID)
            }
        }
        
        if restoredItems.isEmpty {
            return .noItemsToRestore
        } else {
            return .success(restoredItems)
        }
    }
}
```

#### ファイル: `iosMain/.../BannerAdView.swift`

```swift
import GoogleMobileAds
import SwiftUI

struct BannerAdView: UIViewRepresentable {
    let adManager: AdManager
    
    func makeUIView(context: Context) -> GADBannerView {
        let banner = GADBannerView(adSize: GADAdSizeBanner)
        banner.adUnitID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX" // 本番用ID
        banner.rootViewController = UIApplication.shared.windows.first?.rootViewController
        
        if adManager.shouldShowBannerAd() {
            banner.load(GADRequest())
        }
        
        return banner
    }
    
    func updateUIView(_ uiView: GADBannerView, context: Context) {}
}
```

#### ファイル: `iosMain/.../InterstitialAdManager.swift`

```swift
import GoogleMobileAds

class InterstitialAdManager: NSObject, GADFullScreenContentDelegate {
    private var interstitial: GADInterstitialAd?
    private let adManager: AdManager
    private var completionHandler: (() -> Void)?
    
    init(adManager: AdManager) {
        self.adManager = adManager
        super.init()
        loadAd()
    }
    
    private func loadAd() {
        GADInterstitialAd.load(
            withAdUnitID: "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX", // 本番用ID
            request: GADRequest()
        ) { [weak self] ad, error in
            self?.interstitial = ad
            self?.interstitial?.fullScreenContentDelegate = self
        }
    }
    
    func showAdBeforeSave(onComplete: @escaping () -> Void) {
        guard adManager.shouldShowInterstitialAd() else {
            onComplete()
            return
        }
        
        completionHandler = onComplete
        
        if let ad = interstitial,
           let rootViewController = UIApplication.shared.windows.first?.rootViewController {
            ad.present(fromRootViewController: rootViewController)
        } else {
            onComplete()
        }
    }
    
    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) {
        completionHandler?()
        loadAd()
    }
    
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        completionHandler?()
        loadAd()
    }
}
```

---

### Phase 5: UI統合

#### 日記一覧画面（メイン画面）

```kotlin
@Composable
fun DiaryListScreen() {
    Scaffold(
        bottomBar = {
            BannerAdView() // 最下部に広告
        }
    ) { padding ->
        // 日記一覧のコンテンツ
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            // ...
        }
    }
}
```

#### 日記入力画面

```kotlin
@Composable
fun DiaryInputScreen() {
    val interstitialAdManager = remember { InterstitialAdManager(LocalContext.current as Activity) }
    
    Scaffold(
        bottomBar = {
            Column {
                BannerAdView() // 最下部に広告
                Button(
                    onClick = {
                        // 広告表示後に保存
                        interstitialAdManager.showAdBeforeSave {
                            saveDiary()
                        }
                    }
                ) {
                    Text("保存")
                }
            }
        }
    ) { padding ->
        // 入力フォーム
    }
}
```

#### 設定画面

```kotlin
@Composable
fun SettingsScreen() {
    val purchaseManager = remember { PurchaseManager(LocalContext.current as Activity) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        bottomBar = {
            BannerAdView() // 最下部に広告
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding)
        ) {
            // 開発者モード表示
            if (DeveloperMode.isEnabled()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Yellow.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "🔧 開発者モード: 有効",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // プレミアム機能
            item {
                Text(
                    text = "プレミアム機能",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // 広告非表示
            item {
                PurchaseItem(
                    title = "広告非表示",
                    description = "全ての広告を非表示にします",
                    price = "¥400",
                    isPurchased = purchaseManager.isAdRemovalPurchased(),
                    onPurchase = {
                        coroutineScope.launch {
                            purchaseManager.purchaseRemoveAds()
                        }
                    }
                )
            }
            
            // Git連携機能
            item {
                PurchaseItem(
                    title = "Git連携機能",
                    description = "GitHub/GitLabと連携して日記をバックアップ",
                    price = "¥600",
                    isPurchased = purchaseManager.isGitFeaturePurchased(),
                    onPurchase = {
                        coroutineScope.launch {
                            purchaseManager.purchaseGitFeature()
                        }
                    }
                )
            }
            
            // 購入の復元
            item {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = purchaseManager.restorePurchases()
                            // 結果を表示
                        }
                    }
                ) {
                    Text("購入の復元")
                }
            }
        }
    }
}

@Composable
fun PurchaseItem(
    title: String,
    description: String,
    price: String,
    isPurchased: Boolean,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            
            if (isPurchased) {
                Text(
                    text = "✓ 購入済み",
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Button(onClick = onPurchase) {
                    Text(price)
                }
            }
        }
    }
}
```

---

### Phase 6: 設定ファイル

#### Android: `AndroidManifest.xml`

```xml
<manifest>
    <application>
        <!-- AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"/>
    </application>
</manifest>
```

#### iOS: `Info.plist`

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX</string>

<key>SKAdNetworkItems</key>
<array>
    <dict>
        <key>SKAdNetworkIdentifier</key>
        <string>cstr6suwn9.skadnetwork</string>
    </dict>
</array>
```

#### Google Play Console / App Store Connect

**商品ID登録**
- `remove_ads` - 広告非表示 - ¥400
- `premium_git_sync` - Git連携機能 - ¥600

---

## テスト手順

### 1. 開発者モードのテスト（デバッグビルド）
- [ ] デバッグビルドで起動
- [ ] 設定画面に「開発者モード: 有効」が表示されるか確認
- [ ] 広告が表示されないことを確認
- [ ] Git連携機能が使えることを確認
- [ ] 広告非表示機能が有効になっていることを確認

### 2. 広告表示のテスト（リリースビルド）
- [ ] AdMobテストデバイスを登録
- [ ] 日記一覧画面でバナー広告が表示されるか
- [ ] 日記入力画面でバナー広告が表示されるか
- [ ] 設定画面でバナー広告が表示されるか
- [ ] 日記投稿時にインタースティシャル広告が表示されるか
- [ ] 広告を閉じた後に投稿が完了するか

### 3. 課金機能のテスト
- [ ] Google Play / App Store のテストアカウントで購入
- [ ] 広告非表示を購入 → 広告が非表示になるか
- [ ] Git連携機能を購入 → 機能が使えるようになるか
- [ ] アプリを再起動しても購入状態が保持されるか
- [ ] 購入の復元が正しく動作するか

### 4. エッジケースのテスト
- [ ] 広告読み込み失敗時に投稿が完了するか
- [ ] 課金処理をキャンセルした場合の挙動
- [ ] ネットワークオフライン時の挙動
- [ ] 機種変更後の購入復元

---

## 注意事項

### セキュリティ
- **ProGuard/R8を有効化**（Android）して難読化
- 課金処理のコードを難読化対象から除外しない
- デバッグログは本番ビルドで出力しない

### プライバシーポリシー
- 広告SDKによるデータ収集について明記
- AdMobのプライバシー要件に準拠
- Google Play / App Storeの審査に必要

### 広告ポリシー
- 子供向けアプリの場合はファミリー向け広告設定が必要
- 広告の誤タップを誘発しないUI設計
- 広告とコンテンツの明確な区別

### 課金テスト
- 本番環境で課金テストする前に、必ずテスト環境を使用
- テスト購入は24時間以内にキャンセルされる
- 実際の課金は少額でテスト

---

## デバッグ用のログ出力

```kotlin
// デバッグビルドのみログ出力
if (BuildConfig.DEBUG) {
    Log.d("PurchaseManager", "isGitFeaturePurchased: ${isGitFeaturePurchased()}")
    Log.d("PurchaseManager", "isAdRemovalPurchased: ${isAdRemovalPurchased()}")
    Log.d("AdManager", "shouldShowBannerAd: ${shouldShowBannerAd()}")
    Log.d("DeveloperMode", "isEnabled: ${DeveloperMode.isEnabled()}")
}
```

---

## トラブルシューティング

### 広告が表示されない
1. AdMob App IDが正しく設定されているか確認
2. 広告ユニットIDが正しいか確認
3. テストデバイスとして登録されているか確認
4. ネットワーク接続を確認

### 課金が動作しない
1. 商品IDがコンソールに登録されているか確認
2. アプリのバージョンが公開されているか確認（少なくともクローズドテスト）
3. テストアカウントが正しく設定されているか確認
4. Billing Library / StoreKitのバージョンを確認

### 開発者モードが動作しない
1. `BuildConfig.DEBUG` が true になっているか確認
2. デバッグビルドでビルドされているか確認
3. リリースビルドで動作していないか確認（セキュリティリスク）

---

## 完成後のチェックリスト

- [ ] デバッグビルドで開発者モードが動作する
- [ ] リリースビルドで開発者モードが無効化される
- [ ] 全ての画面でバナー広告が表示される（プレミアムユーザー以外）
- [ ] 日記投稿時にインタースティシャル広告が表示される
- [ ] 広告非表示の購入が動作する
- [ ] Git連携機能の購入が動作する
- [ ] 購入の復元が動作する
- [ ] プライバシーポリシーが更新されている
- [ ] AdMobアカウントがアプリにリンクされている
- [ ] Google Play Console / App Store Connectに商品が登録されている

---

## 参考資料

- [Google Mobile Ads SDK - Android](https://developers.google.com/admob/android/quick-start)
- [Google Mobile Ads SDK - iOS](https://developers.google.com/admob/ios/quick-start)
- [Google Play Billing Library](https://developer.android.com/google/play/billing)
- [StoreKit 2](https://developer.apple.com/documentation/storekit)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)