# KMP/CMP アーキテクチャ図

## プロジェクト構造

```
┌─────────────────────────────────────────────────────────────────┐
│                         OneLine Project                          │
└─────────────────────────────────────────────────────────────────┘
                                  │
                    ┌─────────────┴──────────────┐
                    │                            │
          ┌─────────▼─────────┐        ┌────────▼────────┐
          │   app (Android)   │        │  iosApp (Swift) │
          │                   │        │                 │
          │  ┌─────────────┐  │        │  ┌───────────┐  │
          │  │ MainActivity│  │        │  │ iOSApp    │  │
          │  │  (Android)  │  │        │  │  .swift   │  │
          │  └─────────────┘  │        │  └───────────┘  │
          │  ┌─────────────┐  │        │  ┌───────────┐  │
          │  │   Widget    │  │        │  │ Content   │  │
          │  │ (Glance)    │  │        │  │  View     │  │
          │  └─────────────┘  │        │  └───────────┘  │
          │  ┌─────────────┐  │        │                 │
          │  │Notification │  │        │                 │
          │  └─────────────┘  │        │                 │
          └─────────┬─────────┘        └────────┬────────┘
                    │                           │
                    │  implementation            │  imports
                    │  project(":shared")        │  shared
                    │                           │
                    └─────────────┬─────────────┘
                                  │
                    ┌─────────────▼──────────────┐
                    │   shared (KMP Module)      │
                    ├────────────────────────────┤
                    │                            │
                    │  ┌──────────────────────┐  │
                    │  │   commonMain         │  │
                    │  │  (All Platforms)     │  │
                    │  │  ┌────────────────┐  │  │
                    │  │  │ DiaryEntry     │  │  │
                    │  │  │ (kotlinx.      │  │  │
                    │  │  │  datetime)     │  │  │
                    │  │  └────────────────┘  │  │
                    │  │  ┌────────────────┐  │  │
                    │  │  │ Repository IF  │  │  │
                    │  │  │ (expect/actual)│  │  │
                    │  │  └────────────────┘  │  │
                    │  │  ┌────────────────┐  │  │
                    │  │  │ UI Theme       │  │  │
                    │  │  │ Color/Type     │  │  │
                    │  │  └────────────────┘  │  │
                    │  └──────────────────────┘  │
                    │            │               │
                    │  ┌─────────┴─────────┐     │
                    │  │                   │     │
                    │  ▼                   ▼     │
                    │  ┌──────────┐  ┌─────────┐│
                    │  │android   │  │  ios    ││
                    │  │  Main    │  │  Main   ││
                    │  │          │  │         ││
                    │  │┌────────┐│  │┌───────┐││
                    │  ││Android ││  ││iOS    │││
                    │  ││Repo    ││  ││Repo   │││
                    │  ││(JGit)  ││  ││(File) │││
                    │  │└────────┘│  │└───────┘││
                    │  │┌────────┐│  │┌───────┐││
                    │  ││Theme   ││  ││Theme  │││
                    │  ││Android ││  ││iOS    │││
                    │  │└────────┘│  │└───────┘││
                    │  └──────────┘  └─────────┘│
                    └────────────────────────────┘
```

## データフロー

### 日記エントリーの保存 (Android)

```
┌──────────────┐
│ DiaryEdit    │
│ Screen       │
│ (Compose)    │
└──────┬───────┘
       │ saveEntry()
       ▼
┌──────────────┐
│ DiaryEdit    │
│ ViewModel    │
└──────┬───────┘
       │ saveEntry()
       ▼
┌──────────────┐
│ Repository   │
│ Manager      │
│ (Android)    │
└──────┬───────┘
       │
       ├─── Local Mode ──┐
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ Local        │  │ Git          │
│ Repository   │  │ Repository   │
│ (File)       │  │ (JGit)       │
└──────┬───────┘  └──────┬───────┘
       │                 │
       ▼                 ▼
┌──────────────┐  ┌──────────────┐
│ Internal     │  │ Git Repo     │
│ Storage      │  │ (GitHub)     │
└──────────────┘  └──────────────┘
```

### 日記エントリーの保存 (iOS - 将来)

```
┌──────────────┐
│ DiaryEdit    │
│ View         │
│ (SwiftUI+    │
│  Compose)    │
└──────┬───────┘
       │ saveEntry()
       ▼
┌──────────────┐
│ DiaryEdit    │
│ ViewModel    │
│ (shared)     │
└──────┬───────┘
       │ saveEntry()
       ▼
┌──────────────┐
│ iOS          │
│ Repository   │
│ (shared/ios) │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│ NSFileManager│
│ Documents/   │
└──────────────┘
```

## expect/actual メカニズム

```
┌─────────────────────────────────────────────────────────┐
│                    commonMain                           │
│  ┌────────────────────────────────────────────────┐     │
│  │ expect class DiaryRepositoryFactory {          │     │
│  │     fun createRepository(): DiaryRepository    │     │
│  │ }                                              │     │
│  └────────────────────────────────────────────────┘     │
└────────────────────┬────────────────────────────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
┌─────────▼────────┐   ┌────────▼──────────┐
│   androidMain    │   │     iosMain       │
│  ┌─────────────┐ │   │  ┌──────────────┐ │
│  │actual class │ │   │  │actual class  │ │
│  │Repository   │ │   │  │Repository    │ │
│  │Factory(ctx) │ │   │  │Factory()     │ │
│  │{            │ │   │  │{             │ │
│  │  create() = │ │   │  │  create() =  │ │
│  │  Android    │ │   │  │  iOS         │ │
│  │  Repository │ │   │  │  Repository  │ │
│  │}            │ │   │  │}             │ │
│  └─────────────┘ │   │  └──────────────┘ │
└──────────────────┘   └───────────────────┘
```

## モジュール依存グラフ

```
                    External Dependencies
                    ┌────────────────────┐
                    │ kotlinx.datetime   │
                    │ kotlinx.coroutines │
                    │ compose.material3  │
                    └─────────┬──────────┘
                              │
                    ┌─────────▼──────────┐
                    │      shared        │
                    │    (commonMain)    │
                    └─────────┬──────────┘
                              │
                    ┌─────────┴──────────┐
                    │                    │
          ┌─────────▼─────────┐  ┌───────▼────────┐
          │ shared/androidMain│  │ shared/iosMain │
          └─────────┬─────────┘  └───────┬────────┘
                    │                    │
          ┌─────────▼─────────┐  ┌───────▼────────┐
          │        app         │  │    iosApp      │
          │    (Android)       │  │    (Swift)     │
          └────────────────────┘  └────────────────┘

Legend:
  ──▶  : depends on / imports
```

## ファイル配置マップ

```
OneLine/
│
├── shared/                              ← KMP Module
│   ├── src/
│   │   ├── commonMain/                  ← すべてのプラットフォームで共有
│   │   │   └── kotlin/
│   │   │       └── net/chasmine/oneline/
│   │   │           ├── data/
│   │   │           │   ├── model/
│   │   │           │   │   └── DiaryEntry.kt          [✅]
│   │   │           │   └── repository/
│   │   │           │       └── DiaryRepository.kt     [✅]
│   │   │           └── ui/
│   │   │               └── theme/
│   │   │                   ├── Color.kt               [✅]
│   │   │                   ├── Theme.kt               [✅]
│   │   │                   └── Type.kt                [✅]
│   │   │
│   │   ├── androidMain/                 ← Android固有実装
│   │   │   └── kotlin/
│   │   │       └── net/chasmine/oneline/
│   │   │           ├── data/repository/
│   │   │           │   └── DiaryRepository.android.kt [✅]
│   │   │           └── ui/theme/
│   │   │               └── Theme.android.kt           [✅]
│   │   │
│   │   └── iosMain/                     ← iOS固有実装
│   │       └── kotlin/
│   │           └── net/chasmine/oneline/
│   │               ├── data/repository/
│   │               │   └── DiaryRepository.ios.kt     [✅]
│   │               └── ui/theme/
│   │                   └── Theme.ios.kt               [✅]
│   │
│   └── build.gradle.kts                 ← KMP設定 [✅]
│
├── app/                                 ← Android App
│   ├── src/main/
│   │   ├── java/net/chasmine/oneline/
│   │   │   ├── widget/                  [Android専用]
│   │   │   ├── util/                    [Android専用]
│   │   │   └── MainActivity.kt          [Android専用]
│   │   └── res/                         [Android専用]
│   └── build.gradle.kts                 [更新済み]
│
├── iosApp/                              ← iOS App [✅]
│   └── iosApp/
│       ├── iOSApp.swift                 [✅]
│       └── ContentView.swift            [✅]
│
└── docs/                                ← Documentation
    ├── KMP_MIGRATION_GUIDE.md           [✅]
    ├── KMP_IMPLEMENTATION_GUIDE.md      [✅]
    ├── PHASE1_COMPLETION_REPORT.md      [✅]
    ├── PHASE2_MIGRATION_GUIDE.md        [✅]
    └── README_KMP_MIGRATION.md          [✅]

Legend:
  [✅] - 実装済み
  [Android専用] - プラットフォーム固有機能として残す
```

## ビルドフロー

### Android Build

```
./gradlew :app:assembleDebug
       │
       ├─── :shared:compileKotlinAndroid
       │         │
       │         ├─── commonMain sources
       │         └─── androidMain sources
       │
       └─── :app:compileDebugKotlin
             │
             └─── app sources + shared classes
```

### iOS Build (将来)

```
./gradlew :shared:linkDebugFrameworkIosArm64
       │
       └─── :shared:compileKotlinIosArm64
             │
             ├─── commonMain sources
             └─── iosMain sources
                   │
                   └─── shared.framework (Objective-C)
                         │
                         └─── Xcode Project
                               │
                               └─── iosApp.app
```

## Phase 進捗マップ

```
Phase 1: 基盤構築 [✅ 完了]
  ├─ shared module setup
  ├─ commonMain code
  ├─ androidMain/iosMain stubs
  └─ documentation

Phase 2: Android移行 [🚧 次]
  ├─ app → shared dependency
  ├─ data layer migration
  ├─ ViewModel logic extraction
  └─ Android app verification

Phase 3: 共通UI [⏳ 未着手]
  ├─ Compose Multiplatform screens
  ├─ Common navigation
  └─ Shared ViewModels

Phase 4: iOS完成 [⏳ 未着手]
  ├─ Xcode project
  ├─ Framework integration
  └─ iOS app completion

Phase 5: 品質向上 [⏳ 未着手]
  ├─ Tests
  ├─ CI/CD
  └─ Performance

Legend:
  [✅] - 完了
  [🚧] - 進行中
  [⏳] - 未着手
```
