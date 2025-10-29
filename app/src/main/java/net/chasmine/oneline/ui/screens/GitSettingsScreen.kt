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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel
import net.chasmine.oneline.data.preferences.SettingsManager
import net.chasmine.oneline.data.repository.RepositoryManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onNavigateBack: () -> Unit,
    onSetupComplete: (() -> Unit)? = null,
    isInitialSetup: Boolean = false,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager.getInstance(context) }
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
    var validationResult by remember { mutableStateOf<net.chasmine.oneline.data.git.GitRepository.ValidationResult?>(null) }
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
                    net.chasmine.oneline.data.git.GitRepository.ValidationResult.DIARY_REPOSITORY,
                    net.chasmine.oneline.data.git.GitRepository.ValidationResult.LIKELY_DIARY_REPOSITORY,
                    net.chasmine.oneline.data.git.GitRepository.ValidationResult.EMPTY_REPOSITORY -> true
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
                    CircularProgressIndicator()
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
                    // 説明カード
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "💡",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "日記専用のリポジトリを使用してください",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Git設定フォーム
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
                            Text("日記データを保存するGitHubリポジトリのURLを入力してください")
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

                    OutlinedTextField(
                        value = commitUserName,
                        onValueChange = {
                            commitUserName = it
                            isValidationPassed = false
                        },
                        label = { Text("コミット用ユーザー名（必須）") },
                        placeholder = { Text("例: Taro Yamada") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = commitUserName.isBlank() && repoUrl.isNotBlank()
                    )

                    OutlinedTextField(
                        value = commitUserEmail,
                        onValueChange = {
                            commitUserEmail = it
                            isValidationPassed = false
                        },
                        label = { Text("コミット用メールアドレス（必須）") },
                        placeholder = { Text("例: taro@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = commitUserEmail.isBlank() && repoUrl.isNotBlank()
                    )

                    // 検証ボタン
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.validateRepository(repoUrl, username, token)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = repoUrl.isNotEmpty() && username.isNotEmpty() && token.isNotEmpty() &&
                                 commitUserName.isNotEmpty() && commitUserEmail.isNotEmpty() &&
                                 uiState !is SettingsViewModel.UiState.Validating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isValidationPassed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (uiState is SettingsViewModel.UiState.Validating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("検証中...")
                        } else if (isValidationPassed) {
                            Text("✅ 検証済み - 再検証")
                        } else {
                            Text("🔍 リポジトリを検証")
                        }
                    }

                    // 保存ボタン
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
                        enabled = isValidationPassed && uiState !is SettingsViewModel.UiState.Saving
                    ) {
                        if (isValidationPassed) {
                            if (isLocalOnlyMode) {
                                Text("✅ Git連携に移行して保存")
                            } else {
                                Text("✅ 設定を保存")
                            }
                        } else {
                            Text("リポジトリを検証")
                        }
                    }
                }
            }
        }

        // 各種ダイアログ
        
        // 成功ダイアログ
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = { Text("✅ 設定完了") },
                text = { Text("Git設定が正常に保存されました。") },
                confirmButton = {
                    TextButton(onClick = { showSuccessDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // エラーダイアログ
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("❌ エラー") },
                text = { Text(errorMessage) },
                confirmButton = {
                    TextButton(onClick = { showErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // 検証結果ダイアログ
        if (showValidationDialog) {
            AlertDialog(
                onDismissRequest = { showValidationDialog = false },
                title = { Text("リポジトリ検証結果") },
                text = { Text(validationMessage) },
                confirmButton = {
                    TextButton(onClick = { showValidationDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // ヘルプダイアログ
        if (showCreateRepoHelpDialog) {
            AlertDialog(
                onDismissRequest = { showCreateRepoHelpDialog = false },
                title = { Text("📖 日記リポジトリの設定ガイド") },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "GitHubで日記専用のプライベートリポジトリを作成してください。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. GitHubにログインし、新しいリポジトリを作成\n2. リポジトリ名を設定（例: my-diary）\n3. プライベートリポジトリに設定\n4. READMEファイルで初期化\n5. Personal Access Tokenを作成",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreateRepoHelpDialog = false }) {
                        Text("閉じる")
                    }
                }
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 4.dp
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
