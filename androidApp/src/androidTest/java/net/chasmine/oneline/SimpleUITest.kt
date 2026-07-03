package net.chasmine.oneline

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chasmine.oneline.ui.screens.WelcomeScreen
import net.chasmine.oneline.ui.theme.OneLineTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * オンボーディング画面（WelcomeScreen）の基本UIテスト
 *
 * 構成（issue #70）:
 * 1-2. 機能紹介ページ
 * 3.   リマインダー設定ページ
 * 4.   開始ページ（ローカルモードで開始し、最初の日記へ誘導）
 */
@RunWith(AndroidJUnit4::class)
class SimpleUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysFirstTutorialPage() {
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(onStartFirstEntry = { })
            }
        }

        composeTestRule
            .onNodeWithText("シンプルな日記")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("スキップ")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("次へ")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_nextButtonAdvancesToCalendarPage() {
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(onStartFirstEntry = { })
            }
        }

        composeTestRule
            .onNodeWithText("次へ")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("カレンダー表示")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_skipNavigatesToStartPage() {
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(onStartFirstEntry = { })
            }
        }

        composeTestRule
            .onNodeWithText("スキップ")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("準備ができました！")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("今日の日記を書いてみる")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun welcomeScreen_startButtonTriggersCallback() {
        var started = false

        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(onStartFirstEntry = { started = true })
            }
        }

        composeTestRule
            .onNodeWithText("スキップ")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("今日の日記を書いてみる")
            .performClick()

        // CTAはローカルモード設定（suspend）後にコールバックを呼ぶため待機する
        composeTestRule.waitUntil(timeoutMillis = 5_000) { started }
    }

    @Test
    fun welcomeScreen_startPageShowsGitGuidance() {
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(onStartFirstEntry = { })
            }
        }

        composeTestRule
            .onNodeWithText("スキップ")
            .performClick()

        composeTestRule.waitForIdle()

        // Git連携はオンボーディングでは選択させず、設定から可能なことを案内する
        composeTestRule
            .onNodeWithText("クラウド同期もできます")
            .assertIsDisplayed()
    }
}
