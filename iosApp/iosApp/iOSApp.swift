import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        IosQrInterop().registerLaunchScanner {
            QrBridge.shared.startQrFlow()
        }

        let bleInterop = IosBleInterop()
        BleBridge.shared.registerIntoKotlin(interop: bleInterop)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}