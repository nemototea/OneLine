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
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Kotlin Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

            // kotlinx-datetime (Multiplatform date/time library)
            implementation(libs.kotlinx.datetime)

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
