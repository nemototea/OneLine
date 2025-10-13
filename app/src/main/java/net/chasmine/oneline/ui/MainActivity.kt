package net.chasmine.oneline.ui

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.util.DiaryNotificationManager
import net.chasmine.oneline.util.NotificationInitializer
import net.chasmine.oneline.ui.screens.DiaryEditScreen
import net.chasmine.oneline.ui.screens.DiaryListScreen
import net.chasmine.oneline.ui.screens.MainSettingsScreen
import net.chasmine.oneline.ui.screens.DataStorageSettingsScreen
import net.chasmine.oneline.ui.screens.GitSettingsScreen
import net.chasmine.oneline.ui.screens.NotificationSettingsScreen
import net.chasmine.oneline.ui.screens.AboutScreen
import net.chasmine.oneline.ui.screens.WelcomeScreen
import net.chasmine.oneline.ui.screens.CalendarScreen
import net.chasmine.oneline.ui.theme.OneLineTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var notificationInitializer: NotificationInitializer
    
    // 通知権限リクエスト用のランチャー
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        CoroutineScope(Dispatchers.IO).launch {
            notificationInitializer.handlePermissionResult(isGranted)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ステータスバーをアプリのコンテンツに合わせる
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ウィジェットからの起動かどうかをチェック
        val fromWidget = intent.getBooleanExtra("EXTRA_FROM_WIDGET", false)
        val openNewEntry = intent.getBooleanExtra("EXTRA_OPEN_NEW_ENTRY", false)
        val openSettings = intent?.getBooleanExtra("OPEN_SETTINGS", false) ?: false

        // 通知初期化処理
        initializeNotifications()

        setContent {
            val settingsManager = remember { SettingsManager.getInstance(this@MainActivity) }
            val themeMode by settingsManager.themeMode.collectAsState(initial = net.chasmine.oneline.ui.theme.ThemeMode.SYSTEM)
            
            OneLineTheme(
                darkTheme = when (themeMode) {
                    net.chasmine.oneline.ui.theme.ThemeMode.LIGHT -> false
                    net.chasmine.oneline.ui.theme.ThemeMode.DARK -> true
                    net.chasmine.oneline.ui.theme.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OneLineApp(
                        fromWidget = fromWidget,
                        openNewEntry = openNewEntry,
                        openSettings = openSettings
                    )
                }
            }
        }
    }

    private fun initializeNotifications() {
        notificationInitializer = NotificationInitializer(this)
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = notificationInitializer.initializeOnAppStart()
            
            when (result) {
                is NotificationInitializer.InitializationResult.PermissionRequired -> {
                    // 初回起動時の権限リクエスト
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                is NotificationInitializer.InitializationResult.PermissionPreviouslyDenied -> {
                    // 以前に権限を拒否された場合は、設定画面で案内
                    // ここでは何もしない（設定画面で案内）
                }
                else -> {
                    // その他のケースは既に適切に処理済み
                }
            }
        }
    }
}

@Composable
fun OneLineApp(
    fromWidget: Boolean = false,
    openNewEntry: Boolean = false,
    openSettings: Boolean = false
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val gitRepository = GitRepository.getInstance(context.applicationContext as Application)
    val repositoryManager = remember { RepositoryManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    var showGitConfigDialog by remember { mutableStateOf(false) }
    
    // 初回起動判定
    var isFirstLaunch by remember { mutableStateOf(true) }
    var hasValidSettings by remember { mutableStateOf(false) }
    
    // ボトムナビゲーション用の状態
    var selectedTab by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        hasValidSettings = repositoryManager.hasValidSettings()
        isFirstLaunch = !hasValidSettings
    }

    // Git設定チェック関数
    fun checkGitConfigAndNavigate(onConfigured: () -> Unit) {
        scope.launch {
            val repositoryManager = RepositoryManager.getInstance(context)
            val hasValidSettings = repositoryManager.hasValidSettings()
            if (hasValidSettings) {
                onConfigured()
            } else {
                showGitConfigDialog = true
            }
        }
    }

    // Git設定確認ダイアログ
    if (showGitConfigDialog) {
        AlertDialog(
            onDismissRequest = { showGitConfigDialog = false },
            title = { Text("データ保存方法を選択してください") },
            text = {
                Column {
                    Text("日記を投稿するには、データの保存方法を選択する必要があります。")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "📱 ローカル保存のみ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• 端末内にのみ保存\n• 設定不要ですぐに使用可能\n• バックアップや同期は手動",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )
                    
                    Text(
                        text = "☁️ Git連携",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• クラウドで自動バックアップ\n• 複数端末での同期が可能\n• GitHubなどの設定が必要",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val settingsManager = SettingsManager.getInstance(context)
                                settingsManager.setLocalOnlyMode(true)
                                showGitConfigDialog = false
                                // ローカルモードで新規作成画面に遷移
                                navController.navigate("diary_edit/new")
                            }
                        }
                    ) {
                        Text("📱 ローカル保存")
                    }
                    
                    TextButton(
                        onClick = {
                            showGitConfigDialog = false
                            navController.navigate("git_settings")
                        }
                    ) {
                        Text("☁️ Git設定")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitConfigDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // ウィジェットから起動された場合の画面遷移
    LaunchedEffect(fromWidget, openNewEntry, openSettings) {
        if (fromWidget) {
            if (openNewEntry) {
                // ウィジェットからの新規作成もGit設定をチェック
                checkGitConfigAndNavigate {
                    navController.navigate("diary_edit/new")
                }
            } else if (openSettings) {
                navController.navigate("settings")
            } else {
                selectedTab = 0
                navController.navigate("diary_list")
            }
        }
    }

    // 現在のルートに基づいてタブ状態を更新
    val currentRoute by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentRoute?.destination?.route) {
        when (currentRoute?.destination?.route) {
            "diary_list" -> selectedTab = 0
            "calendar" -> selectedTab = 1
        }
    }

    Scaffold(
        bottomBar = {
            // ウェルカム画面と設定画面以外でボトムナビゲーションを表示
            val currentRoute by navController.currentBackStackEntryAsState()
            val route = currentRoute?.destination?.route
            if (route != "welcome" && 
                route?.startsWith("settings") != true && 
                route?.startsWith("data_storage_settings") != true &&
                route?.startsWith("git_settings") != true &&
                route?.startsWith("notification_settings") != true &&
                route?.startsWith("about") != true &&
                route?.startsWith("diary_edit") != true) {
                
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "日記一覧") },
                        label = { Text("日記") },
                        selected = selectedTab == 0,
                        onClick = { 
                            selectedTab = 0
                            navController.navigate("diary_list") {
                                popUpTo("diary_list") { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "カレンダー") },
                        label = { Text("カレンダー") },
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            navController.navigate("calendar") {
                                popUpTo("diary_list") { saveState = true }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController, 
            startDestination = if (isFirstLaunch && !fromWidget) "welcome" else "diary_list",
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it / 3 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
        composable("welcome") {
            WelcomeScreen(
                onLocalModeSelected = {
                    navController.navigate("diary_list") {
                        popUpTo("welcome") { inclusive = true }
                    }
                },
                onGitModeSelected = {
                    navController.navigate("git_settings") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        
        composable("diary_list") {
            DiaryListScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToEdit = { date ->
                    navController.navigate("diary_edit/$date")
                },
                onNavigateToNewEntry = {
                    checkGitConfigAndNavigate {
                        navController.navigate("diary_edit/new")
                    }
                }
            )
        }

        composable(
            route = "diary_edit/{date}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: "new"
            DiaryEditScreen(
                date = date,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            MainSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDataStorage = { navController.navigate("data_storage_settings") },
                onNavigateToGitSettings = { navController.navigate("git_settings") },
                onNavigateToNotificationSettings = { navController.navigate("notification_settings") },
                onNavigateToAbout = { navController.navigate("about") }
            )
        }

        composable("data_storage_settings") {
            DataStorageSettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGitSettings = { navController.navigate("git_settings") }
            )
        }

        composable("git_settings") {
            GitSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("notification_settings") {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("calendar") {
            CalendarScreen(
                onNavigateToEdit = { date ->
                    navController.navigate("diary_edit/$date")
                }
            )
        }
        }
    }
}

