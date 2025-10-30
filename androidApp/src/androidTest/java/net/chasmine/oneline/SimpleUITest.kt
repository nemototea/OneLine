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
 * シンプルなUIテスト
 * 
 * AIアシスタントが簡単に実行できる基本的なUIテストを提供します。
 * 複雑な設定や依存性は使用せず、純粋なUI動作テストに焦点を当てます。
 */
@RunWith(AndroidJUnit4::class)
class SimpleUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("OneLine へようこそ")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_displaysDescription() {
        // Given
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("毎日の想いを一行で記録する\nシンプルな日記アプリです")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_displaysLocalModeOption() {
        // Given
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("📱 ローカル保存のみ")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun welcomeScreen_displaysGitModeOption() {
        // Given
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { }
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("☁️ Git連携")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun welcomeScreen_localModeClick_triggersCallback() {
        // Given
        var localModeClicked = false
        
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { localModeClicked = true },
                    onGitModeSelected = { }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText("📱 ローカル保存のみ")
            .performClick()

        // Wait for the callback to be processed
        composeTestRule.waitForIdle()

        // Then
        assert(localModeClicked) { "ローカルモードのクリックが検出されること" }
    }

    @Test
    fun welcomeScreen_gitModeClick_triggersCallback() {
        // Given
        var gitModeClicked = false
        
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { gitModeClicked = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText("☁️ Git連携")
            .performClick()

        // Wait for the callback to be processed
        composeTestRule.waitForIdle()

        // Then
        assert(gitModeClicked) { "Gitモードのクリックが検出されること" }
    }

    @Test
    fun welcomeScreen_scrollable() {
        // Given
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { },
                    onGitModeSelected = { }
                )
            }
        }

        // When & Then
        // スクロール可能であることを確認
        composeTestRule
            .onRoot()
            .performTouchInput {
                swipeUp()
            }

        // スクロール後も要素が表示されることを確認
        composeTestRule
            .onNodeWithText("💡 どちらを選べばいい？")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeScreen_multipleClicks_handledCorrectly() {
        // Given
        var clickCount = 0
        
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { clickCount++ },
                    onGitModeSelected = { }
                )
            }
        }

        // When
        repeat(3) {
            composeTestRule
                .onNodeWithText("📱 ローカル保存のみ")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Then
        assert(clickCount == 3) { "複数回のクリックが正しく処理されること" }
    }

    @Test
    fun welcomeScreen_bothOptions_clickable() {
        // Given
        var localClicked = false
        var gitClicked = false
        
        composeTestRule.setContent {
            OneLineTheme {
                WelcomeScreen(
                    onLocalModeSelected = { localClicked = true },
                    onGitModeSelected = { gitClicked = true }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithText("📱 ローカル保存のみ")
            .performClick()
        composeTestRule.waitForIdle()
            
        composeTestRule
            .onNodeWithText("☁️ Git連携")
            .performClick()
        composeTestRule.waitForIdle()

        // Then
        assert(localClicked) { "ローカルモードがクリックされること" }
        assert(gitClicked) { "Gitモードがクリックされること" }
    }
}
