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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chasmine.oneline.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var repoUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showRepositoryChangeDialog by remember { mutableStateOf(false) }
    var showMigrationOptionsDialog by remember { mutableStateOf(false) }
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
            }
            is SettingsViewModel.UiState.SaveSuccess -> {
                showSuccessDialog = true
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
                title = { Text("データ同期設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "💡 重要",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "日記専用のリポジトリを使用してください。既存のプロジェクトリポジトリは使用しないでください。",
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

                    // 検証ボタン
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

                    // 検証状態の表示
                    if (isValidationPassed) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✅",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "リポジトリの検証が完了しました。設定を保存できます。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 保存ボタン
                    Button(
                        onClick = {
                            val currentRepoUrl = if (uiState is SettingsViewModel.UiState.Loaded) {
                                (uiState as SettingsViewModel.UiState.Loaded).repoUrl
                            } else ""
                            
                            if (currentRepoUrl.isNotEmpty() && currentRepoUrl != repoUrl) {
                                pendingRepoUrl = repoUrl
                                showRepositoryChangeDialog = true
                            } else {
                                scope.launch {
                                    viewModel.saveSettings(repoUrl, username, token)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isValidationPassed && uiState !is SettingsViewModel.UiState.Saving
                    ) {
                        if (isValidationPassed) {
                            Text("設定を保存する")
                        } else {
                            Text("まずリポジトリを検証してください")
                        }
                    }

                    Text(
                        text = "※ Gitリポジトリへのアクセスには、GitHubなどのアクセストークンが必要です。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
