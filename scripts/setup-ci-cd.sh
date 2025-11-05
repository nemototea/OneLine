#!/bin/bash

# CI/CD セットアップスクリプト
# このスクリプトは、CI/CD環境のセットアップをサポートします

set -e

echo "🚀 OneLine CI/CD Setup Script"
echo "=============================="
echo ""

# カラー定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 必要なツールのチェック
check_dependencies() {
    echo "📦 依存関係チェック..."
    
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}❌ GitHub CLI (gh) がインストールされていません${NC}"
        echo "インストール: https://cli.github.com/"
        exit 1
    fi
    
    if ! command -v base64 &> /dev/null; then
        echo -e "${RED}❌ base64 コマンドが見つかりません${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ 依存関係OK${NC}"
    echo ""
}

# Keystoreのエンコード
encode_keystore() {
    echo "🔑 Keystore のエンコード"
    echo "------------------------"
    
    read -p "Keystoreファイルのパス: " keystore_path
    
    if [ ! -f "$keystore_path" ]; then
        echo -e "${RED}❌ ファイルが見つかりません: $keystore_path${NC}"
        exit 1
    fi
    
    echo ""
    echo "Base64エンコード中..."
    encoded=$(base64 -i "$keystore_path" 2>/dev/null || base64 -w 0 "$keystore_path")
    
    echo -e "${GREEN}✅ エンコード完了${NC}"
    echo ""
    echo "このBase64文字列をGitHubシークレット KEYSTORE_BASE64 に設定してください:"
    echo ""
    echo "$encoded"
    echo ""
}

# GitHubシークレットの設定確認
check_secrets() {
    echo "🔐 GitHub Secrets の確認"
    echo "------------------------"
    
    echo "以下のシークレットが設定されている必要があります:"
    echo ""
    echo "1. KEYSTORE_BASE64              - エンコードされたkeystore"
    echo "2. KEYSTORE_PASSWORD            - keystoreのパスワード"
    echo "3. KEY_ALIAS                    - キーエイリアス"
    echo "4. KEY_PASSWORD                 - キーのパスワード"
    echo "5. GOOGLE_PLAY_SERVICE_ACCOUNT_JSON - サービスアカウントJSON"
    echo ""
    
    read -p "GitHubシークレットの設定を確認しますか？ (y/n): " confirm
    
    if [ "$confirm" != "y" ]; then
        echo "スキップしました"
        return
    fi
    
    echo ""
    echo "GitHub Secrets 一覧:"
    gh secret list || echo -e "${YELLOW}⚠️  GitHub CLIでログインしてください: gh auth login${NC}"
    echo ""
}

# ブランチ保護設定の確認
check_branch_protection() {
    echo "🛡️  ブランチ保護の確認"
    echo "--------------------"
    
    echo "mainブランチの保護設定を確認してください:"
    echo ""
    echo "Settings > Branches > Branch protection rules"
    echo ""
    echo "必要な設定:"
    echo "- ✅ Require a pull request before merging"
    echo "- ✅ Require status checks to pass before merging"
    echo "- ✅ Require conversation resolution before merging"
    echo ""
    
    read -p "Enterキーで続行..."
    echo ""
}

# メタデータファイルの確認
check_metadata() {
    echo "📝 メタデータファイルの確認"
    echo "--------------------------"
    
    required_files=(
        "metadata/ja-JP/title.txt"
        "metadata/ja-JP/short_description.txt"
        "metadata/ja-JP/full_description.txt"
        "metadata/ja-JP/changelogs/1.txt"
    )
    
    all_exists=true
    
    for file in "${required_files[@]}"; do
        if [ -f "$file" ]; then
            echo -e "${GREEN}✅ $file${NC}"
        else
            echo -e "${RED}❌ $file (見つかりません)${NC}"
            all_exists=false
        fi
    done
    
    echo ""
    
    if [ "$all_exists" = true ]; then
        echo -e "${GREEN}✅ すべての必須ファイルが存在します${NC}"
    else
        echo -e "${YELLOW}⚠️  一部のファイルが見つかりません${NC}"
    fi
    
    echo ""
}

# ワークフローファイルの確認
check_workflows() {
    echo "⚙️  GitHub Actions ワークフローの確認"
    echo "-----------------------------------"
    
    workflows=(
        ".github/workflows/pr-check.yml"
        ".github/workflows/release.yml"
    )
    
    all_exists=true
    
    for workflow in "${workflows[@]}"; do
        if [ -f "$workflow" ]; then
            echo -e "${GREEN}✅ $workflow${NC}"
        else
            echo -e "${RED}❌ $workflow (見つかりません)${NC}"
            all_exists=false
        fi
    done
    
    echo ""
    
    if [ "$all_exists" = true ]; then
        echo -e "${GREEN}✅ すべてのワークフローファイルが存在します${NC}"
    else
        echo -e "${YELLOW}⚠️  一部のワークフローファイルが見つかりません${NC}"
    fi
    
    echo ""
}

# メインメニュー
show_menu() {
    echo "🎯 何をしますか？"
    echo ""
    echo "1) Keystoreをエンコード"
    echo "2) GitHubシークレットの確認"
    echo "3) ブランチ保護設定の確認"
    echo "4) メタデータファイルの確認"
    echo "5) ワークフローファイルの確認"
    echo "6) すべてをチェック"
    echo "0) 終了"
    echo ""
    read -p "選択 [0-6]: " choice
    
    case $choice in
        1) encode_keystore ;;
        2) check_secrets ;;
        3) check_branch_protection ;;
        4) check_metadata ;;
        5) check_workflows ;;
        6) 
            check_metadata
            check_workflows
            check_secrets
            check_branch_protection
            ;;
        0) 
            echo "👋 終了します"
            exit 0
            ;;
        *)
            echo -e "${RED}❌ 無効な選択です${NC}"
            ;;
    esac
    
    echo ""
    read -p "Enterキーでメニューに戻る..."
    echo ""
}

# メイン処理
main() {
    check_dependencies
    
    while true; do
        show_menu
    done
}

main
