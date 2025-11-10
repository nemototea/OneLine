import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // Koinの初期化
        KoinInitializerKt.initKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
