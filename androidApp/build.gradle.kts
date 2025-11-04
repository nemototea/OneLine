plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "net.chasmine.oneline"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.chasmine.oneline"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    packagingOptions {
        resources {
            excludes += listOf(
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/DEPENDENCIES",
                "META-INF/ASL2.0",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

dependencies {
    // Shared module (KMP)
    implementation(project(":shared"))

    // kotlinx-datetime (needed for DiaryEntry from shared module)
    implementation(libs.kotlinx.datetime)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt for DI
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // JGit for Git operations
    implementation(libs.org.eclipse.jgit)

    // Bouncy Castle (暗号化ライブラリ)
    implementation(libs.bcprov.jdk18on)
    implementation(libs.bcpkix.jdk18on)

    // Data Store for settings
    implementation(libs.androidx.datastore.preferences)

    // Glance for Widgets
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Lottie for Animations
    implementation(libs.lottie.compose)

}

// カスタムテストタスク
tasks.register("runAllTests") {
    description = "Run all unit tests and integration tests"
    group = "verification"
    dependsOn("test")
}

tasks.register("runUnitTests") {
    description = "Run only unit tests"
    group = "verification"
    dependsOn("testDebugUnitTest")
}

tasks.register("runIntegrationTests") {
    description = "Run only integration tests"
    group = "verification"
    dependsOn("testDebugUnitTest")
}

tasks.register("runUITests") {
    description = "Run UI tests (requires connected device)"
    group = "verification"
    dependsOn("connectedDebugAndroidTest")
}

tasks.register("testWithCoverage") {
    description = "Run tests with coverage report"
    group = "verification"
    dependsOn("testDebugUnitTest")
    finalizedBy("jacocoTestReport")
}

// テストレポートの設定
tasks.withType<Test> {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}