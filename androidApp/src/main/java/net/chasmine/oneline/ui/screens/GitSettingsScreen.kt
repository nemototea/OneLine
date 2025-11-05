package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel
import net.chasmine.oneline.data.preferences.SettingsManagerFactory
import net.chasmine.oneline.data.repository.RepositoryManager
import net.chasmine.oneline.data.git.GitRepositoryServiceImpl
import net.chasmine.oneline.data.git.ValidationResult
import net.chasmine.oneline.ui.components.MaterialAlertDialog
import net.chasmine.oneline.ui.components.AlertType
import net.chasmine.oneline.ui.components.LottieLoadingIndicator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: (() -> Unit)? = null,
    isInitialSetup: Boolean = false
) {
    val context = LocalContext.current

    // ViewModelの作成（共通化されたSettingsViewModelを使用）
    val viewModel: SettingsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val settingsManager = SettingsManagerFactory.getInstance(context)
                val gitRepositoryService = GitRepositoryServiceImpl.getInstance(context)
                return SettingsViewModel(settingsManager, gitRepositoryService) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManagerFactory.getInstance(context) }
    val repositoryManager = remember { RepositoryManager.getInstance(context) }

    // ローカルモード状態の監視
    val isLocalOnlyMode by settingsManager.isLocalOnlyMode.collectAsState(initial = false)

    var repoUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var commitUserName by remember { mutableStateOf("") }
    var commitUserEmail by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showRepositoryChangeDialog by remember { mutableStateOf(false) }
    var showMigrationOptionsDialog by remember { mutableStateOf(false) }
    var showLocalToGitMigrationDialog by remember { mutableStateOf(false) }
    var migrationInProgress by remember { mutableStateOf(false) }
    var localDiaryCount by remember { mutableStateOf(0) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var showCreateRepoHelpDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<ValidationResult?>(null) }
    var isValidationPassed by remember { mutableStateOf(false) }
    var pendingRepoUrl by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SettingsViewModel.UiState.Loaded -> {
                repoUrl = state.repoUrl
                username = state.username
                token = state.token
                commitUserName = state.commitUserName
                commitUserEmail = state.commitUserEmail
            }
            is SettingsViewModel.UiState.SaveSuccess -> {
                if (isInitialSetup) {
                    // 初回セットアップ時は自動遷移
                    onSetupComplete?.invoke()
                } else {
                    // 設定変更時はダイアログ表示
                    showSuccessDialog = true
                }
            }
            is SettingsViewModel.UiState.Error -> {
                errorMessage = state.message
                showErrorDialog = true
            }
            is SettingsViewModel.UiState.ValidationResult -> {
                validationResult = state.result
                validationMessage = state.message
                isValidationPassed = when (state.result) {
                    ValidationResult.DIARY_REPOSITORY,
                    ValidationResult.LIKELY_DIARY_REPOSITORY,
                    ValidationResult.EMPTY_REPOSITORY -> true
                    else -> false
                }
                showValidationDialog = true
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isInitialSetup) "Git連携の設定" else "データ同期設定") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    if (!isInitialSetup) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "戻る"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateRepoHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = "ヘルプ"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is SettingsViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieLoadingIndicator(
                        size = 150.dp
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Git基本情報
                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = {
                            repoUrl = it
                            isValidationPassed = false
                        },
                        label = { Text("日記リポジトリURL") },
                        placeholder = { Text("https://github.com/username/my-diary.git") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("💡 日記専用のプライベートリポジトリを使用してください")
                        }
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it
                            isValidationPassed = false
                        },
                        label = { Text("ユーザー名") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
                            isValidationPassed = false
                        },
                        label = { Text("アクセストークン") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 検証ボタン（基本情報入力後すぐに検証可能）
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.validateRepository(repoUrl, username, token)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = repoUrl.isNotEmpty() && username.isNotEmpty() && token.isNotEmpty() &&
                                 uiState !is SettingsViewModel.UiState.Validating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isValidationPassed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (uiState is SettingsViewModel.UiState.Validating) {
                            LottieLoadingIndicator(
                                size = 24.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("検証中...")
                        } else if (isValidationPassed) {
                            Text("✓ 検証済み - 再度検証")
                        } else {
                            Text("リポジトリの有効性を検証")
                        }
                    }

                    // コミット情報セクション（Card内にグルーピング）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // セクションタイトル
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🌱",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "GitHubで草を生やそう",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // 説明
                            Text(
                                text = "GitHubやGitLabで使っているユーザー名とメールアドレスを設定すると、日記を書くたびに草（貢献グラフ）が増えます。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "※ メールアドレスはGitのコミット情報として使われるだけで、このアプリでは一切収集しません。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // コミット情報入力フィールド
                            OutlinedTextField(
                                value = commitUserName,
                                onValueChange = {
                                    commitUserName = it
                                },
                                label = { Text("コミット用ユーザー名（必須）") },
                                placeholder = { Text("例: Taro Yamada") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = commitUserName.isBlank() && isValidationPassed,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            OutlinedTextField(
                                value = commitUserEmail,
                                onValueChange = {
                                    commitUserEmail = it
                                },
                                label = { Text("コミット用メールアドレス（必須）") },
                                placeholder = { Text("例: taro@example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = commitUserEmail.isBlank() && isValidationPassed,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    // 保存ボタン（検証成功後のみ表示）
                    if (isValidationPassed) {
                        Button(
                            onClick = {
                                val currentRepoUrl = if (uiState is SettingsViewModel.UiState.Loaded) {
                                    (uiState as SettingsViewModel.UiState.Loaded).repoUrl
                                } else ""

                                if (isLocalOnlyMode) {
                                    // ローカルモードからGit連携への移行
                                    showLocalToGitMigrationDialog = true
                                } else if (currentRepoUrl.isNotEmpty() && currentRepoUrl != repoUrl) {
                                    // 既存のGit設定の変更
                                    pendingRepoUrl = repoUrl
                                    showRepositoryChangeDialog = true
                                } else {
                                    // 通常の保存処理
                                    scope.launch {
                                        viewModel.saveSettings(repoUrl, username, token, commitUserName, commitUserEmail)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = commitUserName.isNotEmpty() && commitUserEmail.isNotEmpty() &&
                                     uiState !is SettingsViewModel.UiState.Saving
                        ) {
                            if (isLocalOnlyMode) {
                                Text("Git連携に移行")
                            } else {
                                Text("Git連携を開始")
                            }
                        }
                    }
                }
            }
        }

        // 各種ダイアログ
        
        // 成功ダイアログ
        if (showSuccessDialog) {
            MaterialAlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = "設定完了",
                message = "Git設定が正常に保存されました。",
                alertType = AlertType.SUCCESS
            )
        }

        // エラーダイアログ
        if (showErrorDialog) {
            MaterialAlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = "エラー",
                message = errorMessage,
                alertType = AlertType.ERROR
            )
        }

        // 検証結果ダイアログ
        if (showValidationDialog) {
            val dialogAlertType = when (validationResult) {
                ValidationResult.DIARY_REPOSITORY,
                ValidationResult.LIKELY_DIARY_REPOSITORY,
                ValidationResult.EMPTY_REPOSITORY -> AlertType.SUCCESS
                ValidationResult.UNKNOWN_REPOSITORY -> AlertType.WARNING
                ValidationResult.SUSPICIOUS_REPOSITORY,
                ValidationResult.DANGEROUS_REPOSITORY,
                ValidationResult.OWNERSHIP_VERIFICATION_FAILED,
                ValidationResult.AUTHENTICATION_FAILED,
                ValidationResult.REPOSITORY_NOT_FOUND,
                ValidationResult.CONNECTION_FAILED,
                ValidationResult.VALIDATION_FAILED -> AlertType.ERROR
                else -> AlertType.INFO
            }
            
            MaterialAlertDialog(
                onDismissRequest = { showValidationDialog = false },
                title = "リポジトリ検証結果",
                message = validationMessage,
                alertType = dialogAlertType
            )
        }

        // ヘルプダイアログ
        if (showCreateRepoHelpDialog) {
            MaterialAlertDialog(
                onDismissRequest = { showCreateRepoHelpDialog = false },
                title = "日記リポジトリの設定ガイド",
                message = "GitHubで日記専用のプライベートリポジトリを作成してください。\n\n" +
                        "1. GitHubにログインし、新しいリポジトリを作成\n" +
                        "2. リポジトリ名を設定（例: my-diary）\n" +
                        "3. プライベートリポジトリに設定\n" +
                        "4. READMEファイルで初期化\n" +
                        "5. Personal Access Tokenを作成",
                alertType = AlertType.INFO,
                confirmText = "閉じる"
            )
        }
        
        // ローカルからGitへの移行確認ダイアログ
        if (showLocalToGitMigrationDialog) {
            AlertDialog(
                onDismissRequest = { showLocalToGitMigrationDialog = false },
                title = { Text("Git連携に移行") },
                text = {
                    Column {
                        Text("ローカル保存からGit連携に移行しますか？")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• 既存のローカルデータはGitリポジトリにコピーされます\n• 今後はGitリポジトリで自動バックアップされます\n• 複数端末での同期が可能になります",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLocalToGitMigrationDialog = false
                            migrationInProgress = true
                            scope.launch {
                                try {
                                    // まずGit設定だけを保存（リポジトリ初期化はmigrateToGitModeで行う）
                                    settingsManager.saveGitSettings(repoUrl, username, token, commitUserName, commitUserEmail)

                                    // ローカルからGitへの移行を実行（内部でリポジトリ初期化も行われる）
                                    val result = repositoryManager.migrateToGitMode()

                                    when (result) {
                                        is RepositoryManager.MigrationResult.Success -> {
                                            if (isInitialSetup) {
                                                // 初回セットアップ時は自動遷移
                                                onSetupComplete?.invoke()
                                            } else {
                                                // 設定変更時はダイアログ表示
                                                showSuccessDialog = true
                                            }
                                        }
                                        else -> {
                                            errorMessage = result.getErrorMessage()
                                            showErrorDialog = true
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "移行中にエラーが発生しました: ${e.message}"
                                    showErrorDialog = true
                                } finally {
                                    migrationInProgress = false
                                }
                            }
                        },
                        enabled = !migrationInProgress
                    ) {
                        Text("移行する")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocalToGitMigrationDialog = false }) {
                        Text("キャンセル")
                    }
                }
            )
        }
        
        // 移行中ダイアログ
        if (migrationInProgress) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("移行中...") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LottieLoadingIndicator(size = 50.dp)
                        Text("ローカルデータをGitリポジトリに移行しています")
                    }
                },
                confirmButton = { }
            )
        }
        
        if (uiState is SettingsViewModel.UiState.Saving) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {}
                
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LottieLoadingIndicator(
                            size = 80.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "保存中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
