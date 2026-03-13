// SceneDelegate.swift
// SR-Cardiocare
//
// Standard SceneDelegate with window setup and root navigation.

import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    // MARK: - Properties

    var window: UIWindow?

    // MARK: - Scene Lifecycle

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let window = UIWindow(windowScene: windowScene)
        window.backgroundColor = DesignTokens.Colors.backgroundLight

        let rootViewController: UIViewController
        if AuthManager.shared.isAuthenticated {
            rootViewController = createMainTabBarController()
        } else {
            rootViewController = UINavigationController(rootViewController: LoginViewController())
        }

        window.rootViewController = rootViewController
        window.makeKeyAndVisible()
        self.window = window
    }

    func sceneDidDisconnect(_ scene: UIScene) {
        // Called when the scene is released by the system.
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        // Called when the scene moves from inactive to active state.
    }

    func sceneWillResignActive(_ scene: UIScene) {
        // Called when the scene will move from active to inactive state.
    }

    func sceneWillEnterForeground(_ scene: UIScene) {
        // Called as the scene transitions from background to foreground.
    }

    func sceneDidEnterBackground(_ scene: UIScene) {
        // Called as the scene transitions from foreground to background.
    }

    // MARK: - Navigation Setup

    private func createMainTabBarController() -> UITabBarController {
        let tabBarController = UITabBarController()

        let homeNav = UINavigationController(rootViewController: PatientHomeViewController())
        homeNav.tabBarItem = UITabBarItem(
            title: NSLocalizedString("tab_home", comment: ""),
            image: UIImage(systemName: "house"),
            selectedImage: UIImage(systemName: "house.fill")
        )

        let scheduleNav = UINavigationController(rootViewController: ScheduleViewController())
        scheduleNav.tabBarItem = UITabBarItem(
            title: NSLocalizedString("tab_schedule", comment: ""),
            image: UIImage(systemName: "calendar"),
            selectedImage: UIImage(systemName: "calendar.circle.fill")
        )

        let exercisesNav = UINavigationController(rootViewController: ExerciseLibraryViewController())
        exercisesNav.tabBarItem = UITabBarItem(
            title: NSLocalizedString("tab_exercises", comment: ""),
            image: UIImage(systemName: "figure.walk"),
            selectedImage: UIImage(systemName: "figure.walk.circle.fill")
        )

        let analyticsNav = UINavigationController(rootViewController: AnalyticsViewController())
        analyticsNav.tabBarItem = UITabBarItem(
            title: NSLocalizedString("tab_analytics", comment: ""),
            image: UIImage(systemName: "chart.bar"),
            selectedImage: UIImage(systemName: "chart.bar.fill")
        )

        tabBarController.viewControllers = [homeNav, scheduleNav, exercisesNav, analyticsNav]
        return tabBarController
    }
}
