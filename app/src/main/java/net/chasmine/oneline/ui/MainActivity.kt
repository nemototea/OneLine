package net.chasmine.oneline.ui

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.chasmine.oneline.data.git.GitRepository
import net.chasmine.oneline.data.preferences.NotificationPreferences
import net.chasmine.oneline.util.DiaryNotificationManager
import net.chasmine.oneline.util.NotificationInitializer
import net.chasmine.oneline.ui.screens.DiaryEditScreen
import net.chasmine.oneline.ui.screens.DiaryListScreen
import net.chasmine.oneline.ui.screens.MainSettingsScreen
import net.chasmine.oneline.ui.screens.GitSettingsScreen
import net.chasmine.oneline.ui.screens.NotificationSettingsScreen
import net.chasmine.oneline.ui.screens.AboutScreen
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

        // ウィジェットからの起動かどうかをチェック
        val fromWidget = intent.getBooleanExtra("EXTRA_FROM_WIDGET", false)
        val openNewEntry = intent.getBooleanExtra("EXTRA_OPEN_NEW_ENTRY", false)
        val openSettings = intent?.getBooleanExtra("OPEN_SETTINGS", false) ?: false

        // 通知初期化処理
        initializeNotifications()

        setContent {
            OneLineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
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
    var showGitConfigDialog by remember { mutableStateOf(false) }

    // Git設定チェック関数
    fun checkGitConfigAndNavigate(onConfigured: () -> Unit) {
        if (gitRepository.isConfigValid()) {
            onConfigured()
        } else {
            showGitConfigDialog = true
        }
    }

    // Git設定確認ダイアログ
    if (showGitConfigDialog) {
        AlertDialog(
            onDismissRequest = { showGitConfigDialog = false },
            title = { Text("Git設定が必要です") },
            text = {
                Column {
                    Text("日記を投稿するには、まずGitリポジトリの設定が必要です。")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "設定画面でGitHubリポジトリの情報を入力してください。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showGitConfigDialog = false
                        navController.navigate("settings")
                    }
                ) {
                    Text("設定画面へ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGitConfigDialog = false }
                ) {
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
                navController.navigate("diary_list")
            }
        }
    }

    NavHost(navController = navController, startDestination = "diary_list") {
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
                onNavigateToGitSettings = { navController.navigate("git_settings") },
                onNavigateToNotificationSettings = { navController.navigate("notification_settings") },
                onNavigateToAbout = { navController.navigate("about") }
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
    }
}

