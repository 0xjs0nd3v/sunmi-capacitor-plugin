// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SunmiCapacitorPlugin",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "SunmiCapacitorPlugin",
            targets: ["SunmiPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "SunmiPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/SunmiPluginPlugin"),
        .testTarget(
            name: "SunmiPluginPluginTests",
            dependencies: ["SunmiPluginPlugin"],
            path: "ios/Tests/SunmiPluginPluginTests")
    ]
)