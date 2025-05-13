package net.chasmine.oneline.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.chasmine.oneline.ui.screens.DiaryEditScreen
import net.chasmine.oneline.ui.screens.DiaryListScreen
import net.chasmine.oneline.ui.screens.SettingsScreen
import net.chasmine.oneline.ui.theme.OneLineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ウィジェットからの起動かどうかをチェック
        val fromWidget = intent.getBooleanExtra("EXTRA_FROM_WIDGET", false)
        val openNewEntry = intent.getBooleanExtra("EXTRA_OPEN_NEW_ENTRY", false)

        setContent {
            OneLineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OneLineApp(
                        fromWidget = fromWidget,
                        openNewEntry = openNewEntry
                    )
                }
            }
        }
    }
}

@Composable
fun OneLineApp(
    fromWidget: Boolean = false,
    openNewEntry: Boolean = false
) {
    val navController = rememberNavController()

    // ウィジェットから起動された場合、新規エントリー画面に遷移
    LaunchedEffect(fromWidget, openNewEntry) {
        if (fromWidget && openNewEntry) {
            navController.navigate("journal_edit/new")
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
                    navController.navigate("diary_edit/new")
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
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

