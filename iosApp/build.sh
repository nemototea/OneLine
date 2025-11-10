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

echo "Building Shared framework..."
echo "Configuration: $CONFIGURATION"
echo "SDK: $SDK_NAME"
echo "Archs: $ARCHS"

# ビルドタスクを決定
if [ "$SDK_NAME" = "iphoneos" ]; then
    # 実機用
    TASK="linkReleaseFrameworkIosArm64"
elif [ "$SDK_NAME" = "iphonesimulator" ]; then
    # シミュレータ用（Apple Silicon）
    if [ "$ARCHS" = "arm64" ]; then
        TASK="linkDebugFrameworkIosSimulatorArm64"
    else
        # Intel Mac用
        TASK="linkDebugFrameworkIosX64"
    fi
else
    echo "Unknown SDK: $SDK_NAME"
    exit 1
fi

echo "Running Gradle task: $TASK"

# Gradleでフレームワークをビルド
./gradlew :shared:$TASK

echo "✅ Shared framework built successfully"
