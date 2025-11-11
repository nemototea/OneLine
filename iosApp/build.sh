#!/bin/bash

# iOS Shared フレームワークのビルドスクリプト
# Xcodeビルド時に自動でSharedフレームワークをビルドします

set -e

# プロジェクトルートに移動
cd "$(dirname "$0")/.."

# 設定
CONFIGURATION=${CONFIGURATION:-Debug}
SDK_NAME=${SDK_NAME:-iphonesimulator}
ARCHS=${ARCHS:-arm64}
CLEAN_BUILD=${CLEAN_BUILD:-false}

echo "======================================"
echo "Building Shared framework..."
echo "======================================"
echo "Configuration: $CONFIGURATION"
echo "SDK: $SDK_NAME"
echo "Archs: $ARCHS"
echo "Clean build: $CLEAN_BUILD"
echo ""

# ビルドタスクを決定
if [ "$SDK_NAME" = "iphoneos" ]; then
    # 実機用
    TASK="linkReleaseFrameworkIosArm64"
    FRAMEWORK_PATH="shared/build/bin/iosArm64/releaseFramework/Shared.framework"
elif [ "$SDK_NAME" = "iphonesimulator" ]; then
    # シミュレータ用（Apple Silicon）
    if [ "$ARCHS" = "arm64" ]; then
        TASK="linkDebugFrameworkIosSimulatorArm64"
        FRAMEWORK_PATH="shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework"
    else
        # Intel Mac用
        TASK="linkDebugFrameworkIosX64"
        FRAMEWORK_PATH="shared/build/bin/iosX64/debugFramework/Shared.framework"
    fi
else
    echo "❌ Error: Unknown SDK: $SDK_NAME"
    exit 1
fi

echo "Gradle task: $TASK"
echo "Framework path: $FRAMEWORK_PATH"
echo ""

# クリーンビルドの場合
if [ "$CLEAN_BUILD" = "true" ]; then
    echo "Cleaning previous build..."
    ./gradlew :shared:clean
    echo ""
fi

# Gradleでフレームワークをビルド
echo "Running Gradle build..."
if ./gradlew :shared:$TASK --info; then
    echo ""
    echo "======================================"
    echo "✅ Build succeeded!"
    echo "======================================"
    echo "Framework location: $FRAMEWORK_PATH"

    # フレームワークの存在確認
    if [ -d "$FRAMEWORK_PATH" ]; then
        echo "Framework size: $(du -sh "$FRAMEWORK_PATH" | cut -f1)"
    else
        echo "⚠️ Warning: Framework not found at expected location"
    fi
else
    echo ""
    echo "======================================"
    echo "❌ Build failed!"
    echo "======================================"
    exit 1
fi
