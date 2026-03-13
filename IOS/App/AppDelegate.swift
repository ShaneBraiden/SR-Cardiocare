// AppDelegate.swift
// SR-Cardiocare
//
// Standard UIKit AppDelegate with Firebase initialization and push notification registration.

import UIKit
import UserNotifications
import FirebaseCore

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    // MARK: - Properties

    var window: UIWindow?

    // MARK: - Application Lifecycle

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize Firebase SDK — must be called before any Firebase service is used.
        // Reads configuration from GoogleService-Info.plist
        FirebaseApp.configure()

        configureAppearance()
        registerForPushNotifications(application)
        return true
    }

    // MARK: - UISceneSession Lifecycle

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }

    func application(
        _ application: UIApplication,
        didDiscardSceneSessions sceneSessions: Set<UISceneSession>
    ) {
        // Called when the user discards a scene session.
    }

    // MARK: - Push Notification Registration

    private func registerForPushNotifications(_ application: UIApplication) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if let error = error {
                print("Push notification authorization error: \(error.localizedDescription)")
                return
            }
            guard granted else { return }
            DispatchQueue.main.async {
                application.registerForRemoteNotifications()
            }
        }
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("Device push token: \(tokenString)")
        // TODO: Send token to backend API
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Failed to register for push notifications: \(error.localizedDescription)")
    }

    // MARK: - Appearance Configuration

    private func configureAppearance() {
        let navBarAppearance = UINavigationBarAppearance()
        navBarAppearance.configureWithOpaqueBackground()
        navBarAppearance.backgroundColor = DesignTokens.Colors.surfaceLight
        navBarAppearance.titleTextAttributes = [
            .foregroundColor: DesignTokens.Colors.textMain,
            .font: DesignTokens.Typography.inter(DesignTokens.Typography.headline, weight: DesignTokens.Typography.Weight.semibold)
        ]
        navBarAppearance.largeTitleTextAttributes = [
            .foregroundColor: DesignTokens.Colors.textMain,
            .font: DesignTokens.Typography.lexend(DesignTokens.Typography.largeTitle, weight: DesignTokens.Typography.Weight.bold)
        ]

        UINavigationBar.appearance().standardAppearance = navBarAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navBarAppearance
        UINavigationBar.appearance().tintColor = DesignTokens.Colors.primary

        let tabBarAppearance = UITabBarAppearance()
        tabBarAppearance.configureWithOpaqueBackground()
        tabBarAppearance.backgroundColor = DesignTokens.Colors.surfaceLight
        UITabBar.appearance().standardAppearance = tabBarAppearance
        UITabBar.appearance().scrollEdgeAppearance = tabBarAppearance
        UITabBar.appearance().tintColor = DesignTokens.Colors.primary
    }
}
