import Foundation
import UIKit
import AVFoundation
import ComposeApp

@objcMembers
final class QrBridge: NSObject, AVCaptureMetadataOutputObjectsDelegate {
    static let shared = QrBridge()

    private let sessionQueue = DispatchQueue(label: "ru.aokruan.qr.session")
    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private weak var presentedController: UIViewController?
    private var completion: ((String?) -> Void)?
    private var isRunningFlow = false
    private var didFinish = false

    func startQrFlow() {
        print("QrBridge.startQrFlow")

        guard !isRunningFlow else {
            print("QrBridge: flow already running")
            return
        }
        isRunningFlow = true

        ensureCameraAccess { [weak self] granted in
            guard let self else { return }

            print("QrBridge.ensureCameraAccess granted=\(granted)")

            if !granted {
                let interop = IosQrInterop()
                interop.cancelScan()
                self.resetState()
                return
            }

            self.scanQr { result in
                let interop = IosQrInterop()
                if let result {
                    print("QrBridge.scanQr finished result=\(result)")
                    interop.completeScan(result: result)
                } else {
                    print("QrBridge.scanQr finished with nil")
                    interop.cancelScan()
                }
                self.resetState()
            }
        }
    }

    private func ensureCameraAccess(completion: @escaping (Bool) -> Void) {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            completion(true)

        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                DispatchQueue.main.async {
                    completion(granted)
                }
            }

        case .denied, .restricted:
            completion(false)

        @unknown default:
            completion(false)
        }
    }

    private func scanQr(completion: @escaping (String?) -> Void) {
        self.completion = completion
        self.didFinish = false

        let controller = UIViewController()
        controller.view.backgroundColor = .black

        guard let top = topViewController() else {
            print("QrBridge: topViewController is nil")
            completion(nil)
            return
        }

        DispatchQueue.main.async {
            print("QrBridge: presenting scanner controller")
            top.present(controller, animated: true) { [weak self] in
                guard let self else { return }
                self.presentedController = controller
                self.configureAndStartSession(in: controller)
            }
        }
    }

    private func configureAndStartSession(in controller: UIViewController) {
        sessionQueue.async { [weak self] in
            guard let self else { return }
            guard self.captureSession == nil else {
                print("QrBridge: captureSession already exists")
                return
            }

            print("QrBridge: configuring session on sessionQueue")

            let session = AVCaptureSession()
            session.beginConfiguration()

            guard let device = AVCaptureDevice.default(for: .video) else {
                print("QrBridge: camera device is nil")
                session.commitConfiguration()
                self.finish(result: nil)
                return
            }

            guard let input = try? AVCaptureDeviceInput(device: device), session.canAddInput(input) else {
                print("QrBridge: failed to create/add camera input")
                session.commitConfiguration()
                self.finish(result: nil)
                return
            }
            session.addInput(input)

            let output = AVCaptureMetadataOutput()
            guard session.canAddOutput(output) else {
                print("QrBridge: failed to add metadata output")
                session.commitConfiguration()
                self.finish(result: nil)
                return
            }
            session.addOutput(output)

            output.setMetadataObjectsDelegate(self, queue: .main)
            output.metadataObjectTypes = [.qr]

            session.commitConfiguration()

            let preview = AVCaptureVideoPreviewLayer(session: session)
            preview.videoGravity = .resizeAspectFill

            DispatchQueue.main.async {
                preview.frame = controller.view.bounds
                controller.view.layer.addSublayer(preview)
                self.previewLayer = preview
                print("QrBridge: preview layer added")
            }

            self.captureSession = session

            if !session.isRunning {
                print("QrBridge: startRunning")
                session.startRunning()
                print("QrBridge: startRunning completed")
            }
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        guard !didFinish else { return }

        guard
            let readable = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
            let value = readable.stringValue
        else { return }

        print("QrBridge: QR detected value=\(value)")
        didFinish = true
        finish(result: value)
    }

    private func finish(result: String?) {
        let session = captureSession
        captureSession = nil

        sessionQueue.async { [weak self] in
            guard let self else { return }

            if let session, session.isRunning {
                print("QrBridge: stopRunning")
                session.stopRunning()
                print("QrBridge: stopRunning completed")
            }

            DispatchQueue.main.async {
                self.previewLayer?.removeFromSuperlayer()
                self.previewLayer = nil

                if let controller = self.presentedController {
                    print("QrBridge: dismiss scanner controller")
                    controller.dismiss(animated: true) {
                        self.presentedController = nil
                        self.completion?(result)
                        self.completion = nil
                    }
                } else {
                    self.completion?(result)
                    self.completion = nil
                }
            }
        }
    }

    private func resetState() {
        DispatchQueue.main.async {
            self.isRunningFlow = false
            self.didFinish = false
        }
    }

    private func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first,
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else {
            return nil
        }

        var current = root
        while let presented = current.presentedViewController {
            current = presented
        }
        return current
    }
}