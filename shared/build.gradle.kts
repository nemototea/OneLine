plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true

            // 依存関係をフレームワークに含める
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            export(libs.kotlinx.datetime)
            export(libs.koin.core)

            // リリースビルドの最適化
            if (buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.RELEASE) {
                // リリースビルドではデバッグシンボルを除去して最適化
                freeCompilerArgs = freeCompilerArgs + listOf(
                    "-Xdisable-phases=VerifyBitcode"
                )
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Kotlin Coroutines (API - iOSフレームワークにexport)
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

            // kotlinx-datetime (API - iOSフレームワークにexport)
            api(libs.kotlinx.datetime)

            // Koin for Dependency Injection (API - iOSフレームワークにexport)
            api(libs.koin.core)

            // kotlinx-serialization (Multiplatform serialization)
            implementation(libs.kotlinx.serialization.json)

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.materialIconsExtended)

            // Lifecycle ViewModel (Compose Multiplatform)
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

            // Koin (Compose統合 - implementation)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Compottie (Compose Multiplatform Lottie)
            implementation(libs.compottie)
            implementation(libs.compottie.resources)
        }

        androidMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

            // DataStore Preferences (設定管理)
            implementation(libs.androidx.datastore.preferences)

            // JGit (Git操作ライブラリ)
            implementation(libs.org.eclipse.jgit)

            // Bouncy Castle (暗号化ライブラリ - JGitの依存関係)
            implementation(libs.bcprov.jdk18on)
            implementation(libs.bcpkix.jdk18on)

            // AndroidX Core (WindowInsetsControllerCompatなど)
            implementation("androidx.core:core-ktx:1.15.0")
        }

        iosMain.dependencies {
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

android {
    namespace = "net.chasmine.oneline.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
