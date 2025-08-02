package net.chasmine.oneline.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun SettingsScreen(
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
        if (uiState is SettingsViewModel.UiState.Loaded) {
            val loadedState = uiState as SettingsViewModel.UiState.Loaded
            repoUrl = loadedState.repoUrl
            username = loadedState.username
            token = loadedState.token
        } else if (uiState is SettingsViewModel.UiState.SaveSuccess) {
            showSuccessDialog = true
        } else if (uiState is SettingsViewModel.UiState.Error) {
            errorMessage = (uiState as SettingsViewModel.UiState.Error).message
            showErrorDialog = true
        } else if (uiState is SettingsViewModel.UiState.ValidationResult) {
            val validationState = uiState as SettingsViewModel.UiState.ValidationResult
            validationMessage = validationState.message
            validationResult = validationState.result
            
            // 検証通過の判定
            isValidationPassed = when (validationState.result) {
                net.chasmine.oneline.data.git.GitRepository.ValidationResult.DIARY_REPOSITORY,
                net.chasmine.oneline.data.git.GitRepository.ValidationResult.LIKELY_DIARY_REPOSITORY,
                net.chasmine.oneline.data.git.GitRepository.ValidationResult.EMPTY_REPOSITORY -> true
                else -> false
            }
            
            showValidationDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState is SettingsViewModel.UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Git連携設定",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 重要",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "日記専用のリポジトリを使用してください。詳細は右上のヘルプアイコンをご確認ください。",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedTextField(
                        value = repoUrl,
                        onValueChange = { 
                            repoUrl = it
                            // URL変更時は検証をリセット
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
                            // 認証情報変更時は検証をリセット
                            isValidationPassed = false
                        },
                        label = { Text("ユーザー名") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { 
                            token = it
                            // 認証情報変更時は検証をリセット
                            isValidationPassed = false
                        },
                        label = { Text("アクセストークン") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                    Text(
                        text = "※ Gitリポジトリへのアクセスには、GitHubなどのアクセストークンが必要です。",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // 検証状態の表示
                    if (isValidationPassed) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
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

                    Button(
                        onClick = {
                            // リポジトリURL変更チェックは既存のまま
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
                }

                if (uiState is SettingsViewModel.UiState.Saving) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = { Text("設定が保存されました") },
            text = { Text("Gitリポジトリとの連携設定が保存されました。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("エラー") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // リポジトリ変更確認ダイアログ
    if (showRepositoryChangeDialog) {
        AlertDialog(
            onDismissRequest = { showRepositoryChangeDialog = false },
            title = { Text("⚠️ リポジトリ変更の確認") },
            text = {
                Column {
                    Text("リポジトリURLを変更しようとしています。")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "現在: ${if (uiState is SettingsViewModel.UiState.Loaded) (uiState as SettingsViewModel.UiState.Loaded).repoUrl else "未設定"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "新規: $pendingRepoUrl",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // TODO: ローカル日記ファイル数を取得して表示
                    if (localDiaryCount > 0) {
                        Text(
                            text = "⚠️ ローカルに${localDiaryCount}個の日記ファイルがあります。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "データの移行方法を選択してください。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "ローカルに日記データはありません。新しいリポジトリに切り替えます。",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (localDiaryCount > 0) {
                    TextButton(
                        onClick = {
                            showRepositoryChangeDialog = false
                            showMigrationOptionsDialog = true
                        }
                    ) {
                        Text("移行方法を選択")
                    }
                } else {
                    TextButton(
                        onClick = {
                            showRepositoryChangeDialog = false
                            scope.launch {
                                viewModel.saveSettings(pendingRepoUrl, username, token)
                            }
                        }
                    ) {
                        Text("変更する")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRepositoryChangeDialog = false
                        // 元のURLに戻す
                        if (uiState is SettingsViewModel.UiState.Loaded) {
                            repoUrl = (uiState as SettingsViewModel.UiState.Loaded).repoUrl
                        }
                    }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 移行オプション選択ダイアログ
    if (showMigrationOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showMigrationOptionsDialog = false },
            title = { Text("📦 データ移行方法の選択") },
            text = {
                Column {
                    Text(
                        text = "既存の${localDiaryCount}個の日記データをどのように処理しますか？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 移行オプション1: データ移行（推奨）
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showMigrationOptionsDialog = false
                                scope.launch {
                                    viewModel.migrateRepository(pendingRepoUrl, username, token, "MIGRATE_DATA")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📤 データを移行する（推奨）",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "既存の日記データを新しいリポジトリにコピーします。",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 同じ日付のファイルがある場合は、より新しい内容を優先\n• データの損失なし\n• 継続して日記を記述可能",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 移行オプション2: 破棄して切り替え
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showMigrationOptionsDialog = false
                                scope.launch {
                                    viewModel.migrateRepository(pendingRepoUrl, username, token, "DISCARD_AND_SWITCH")
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🗑️ データを破棄して切り替え",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "既存の日記データを削除して新しいリポジトリに切り替えます。",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 既存データは完全に削除されます\n• 新しいリポジトリで新規開始\n• データの復元はできません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showMigrationOptionsDialog = false }
                ) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 検証結果ダイアログ
    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = { Text("リポジトリ検証結果") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(validationMessage)
                    
                    if (validationResult == net.chasmine.oneline.data.git.GitRepository.ValidationResult.SUSPICIOUS_REPOSITORY ||
                        validationResult == net.chasmine.oneline.data.git.GitRepository.ValidationResult.DANGEROUS_REPOSITORY ||
                        validationResult == net.chasmine.oneline.data.git.GitRepository.ValidationResult.OWNERSHIP_VERIFICATION_FAILED ||
                        validationResult == net.chasmine.oneline.data.git.GitRepository.ValidationResult.UNKNOWN_REPOSITORY) {
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "🔍 検証基準（参考）",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "✅ 検証をパスするリポジトリ:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• 空の新しいリポジトリ\n• 名前に「diary」「journal」「note」「obsidian」「vault」「daily」「log」を含む\n• 日記形式のMarkdownファイル（YYYY-MM-DD.md）を含む",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                        )
                        
                        Text(
                            text = "❌ 検証で警告されるリポジトリ:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "• 名前に開発用キーワードを含む\n• コードファイル（.kt, .java, .gradle等）を含む",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    if (isValidationPassed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✅ 検証に合格しました。設定を保存できます。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showValidationDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }

    // リポジトリ作成ヘルプダイアログ
    if (showCreateRepoHelpDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRepoHelpDialog = false },
            title = { Text("📖 日記リポジトリの設定ガイド") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // 検証基準セクション
                    Text(
                        text = "🔍 検証基準",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "✅ 検証をパスするリポジトリ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• 空の新しいリポジトリ\n• 名前に「diary」「journal」「note」「obsidian」「vault」「daily」「log」を含む\n• 日記形式のMarkdownファイル（YYYY-MM-DD.md）を含む",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    
                    Text(
                        text = "❌ 検証で警告されるリポジトリ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "• 名前に開発用キーワード（oneline、app、android、source、code等）を含む\n• コードファイル（.kt, .java, .gradle等）を含む",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
                    )
                    
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // リポジトリ作成手順セクション
                    Text(
                        text = "📝 新しいリポジトリの作成手順",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "1️⃣ GitHub.comにアクセス",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• github.com にログイン\n• 右上の「+」→「New repository」をクリック",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    
                    Text(
                        text = "2️⃣ リポジトリ名を入力",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "推奨名:\n• my-diary-2025\n• daily-journal\n• personal-notes\n• obsidian-vault",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    
                    Text(
                        text = "3️⃣ 設定",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• 「Private」を選択（推奨）\n• 「Add a README file」はチェックしない\n• 「Create repository」をクリック",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    
                    Text(
                        text = "4️⃣ URLをコピー",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• 作成されたリポジトリのURLをコピー\n• 設定画面のフィールドに貼り付け",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showCreateRepoHelpDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
}