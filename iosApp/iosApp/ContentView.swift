import SwiftUI
import shared

struct ContentView: View {
    var body: some View {
        VStack {
            Text("OneLine")
                .font(.largeTitle)
                .padding()
            
            Text("日記アプリ - iOS版")
                .font(.title2)
                .padding()
            
            // TODO: Compose UIの統合
            // ComposeViewを使用してKotlinで書かれたUIを表示
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
