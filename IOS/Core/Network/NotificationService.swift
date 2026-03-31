// NotificationService.swift
// SR-Cardiocare — Local and Push Notification Management
// Handles: workout reminders (30min, 1hr, 2hr), activity notifications, badge dots

import UIKit
import UserNotifications

final class NotificationService {

    static let shared = NotificationService()

    private init() {}

    // MARK: - Notification Categories

    enum NotificationCategory: String {
        case workoutReminder = "WORKOUT_REMINDER"
        case doctorActivity = "DOCTOR_ACTIVITY"
        case patientActivity = "PATIENT_ACTIVITY"
        case message = "MESSAGE"
        case achievement = "ACHIEVEMENT"
    }

    // MARK: - Authorization

    func requestAuthorization(completion: @escaping (Bool) -> Void) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            DispatchQueue.main.async {
                if let error = error {
                    print("Notification authorization error: \(error)")
                }
                completion(granted)
            }
        }
    }

    func checkAuthorizationStatus(completion: @escaping (UNAuthorizationStatus) -> Void) {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                completion(settings.authorizationStatus)
            }
        }
    }

    // MARK: - Workout Reminders

    /// Schedules workout reminder notifications at 30min, 1hr, and 2hr before scheduled time
    func scheduleWorkoutReminders(workoutId: String, workoutName: String, scheduledTime: Date) {
        let reminderOffsets: [(minutes: Int, title: String)] = [
            (30, "Workout in 30 minutes"),
            (60, "Workout in 1 hour"),
            (120, "Workout in 2 hours")
        ]

        for offset in reminderOffsets {
            let triggerDate = scheduledTime.addingTimeInterval(TimeInterval(-offset.minutes * 60))

            // Skip if trigger time is in the past
            guard triggerDate > Date() else { continue }

            let content = UNMutableNotificationContent()
            content.title = offset.title
            content.body = "Don't miss your \(workoutName) session!"
            content.sound = .default
            content.categoryIdentifier = NotificationCategory.workoutReminder.rawValue
            content.userInfo = [
                "workoutId": workoutId,
                "minutesBefore": offset.minutes
            ]

            let triggerComponents = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: triggerDate)
            let trigger = UNCalendarNotificationTrigger(dateMatching: triggerComponents, repeats: false)

            let identifier = "workout_\(workoutId)_\(offset.minutes)min"
            let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

            UNUserNotificationCenter.current().add(request) { error in
                if let error = error {
                    print("Failed to schedule workout reminder: \(error)")
                }
            }
        }
    }

    /// Cancels all reminders for a specific workout
    func cancelWorkoutReminders(workoutId: String) {
        let identifiers = [30, 60, 120].map { "workout_\(workoutId)_\($0)min" }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: identifiers)
    }

    // MARK: - Activity Notifications

    /// Sends notification when doctor performs an action
    func notifyDoctorActivity(patientId: String, doctorName: String, action: String) {
        let content = UNMutableNotificationContent()
        content.title = "Dr. \(doctorName)"
        content.body = action
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.doctorActivity.rawValue
        content.userInfo = ["patientId": patientId]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let identifier = "doctor_activity_\(UUID().uuidString)"
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request)
    }

    /// Sends notification when patient performs an action
    func notifyPatientActivity(doctorId: String, patientName: String, action: String) {
        let content = UNMutableNotificationContent()
        content.title = patientName
        content.body = action
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.patientActivity.rawValue
        content.userInfo = ["doctorId": doctorId]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let identifier = "patient_activity_\(UUID().uuidString)"
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Message Notifications

    func notifyNewMessage(fromName: String, messagePreview: String, chatId: String) {
        let content = UNMutableNotificationContent()
        content.title = fromName
        content.body = messagePreview
        content.sound = .default
        content.categoryIdentifier = NotificationCategory.message.rawValue
        content.userInfo = ["chatId": chatId]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let identifier = "message_\(UUID().uuidString)"
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)

        UNUserNotificationCenter.current().add(request)
    }

    // MARK: - Badge Management

    func updateBadgeCount(_ count: Int) {
        DispatchQueue.main.async {
            UIApplication.shared.applicationIconBadgeNumber = count
        }
    }

    func clearBadge() {
        updateBadgeCount(0)
    }

    // MARK: - Clear All

    func clearAllPendingNotifications() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }

    func clearAllDeliveredNotifications() {
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
    }
}

// MARK: - In-App Notification State

final class InAppNotificationState: ObservableObject {
    static let shared = InAppNotificationState()

    @Published var hasUnreadChat: Bool = false
    @Published var hasUnreadNotifications: Bool = false
    @Published var unreadChatCount: Int = 0
    @Published var unreadNotificationCount: Int = 0

    private init() {}

    func markChatAsRead() {
        hasUnreadChat = false
        unreadChatCount = 0
    }

    func incrementChatUnread() {
        hasUnreadChat = true
        unreadChatCount += 1
    }

    func setNotificationDot(visible: Bool, count: Int = 0) {
        hasUnreadNotifications = visible
        unreadNotificationCount = count
    }
}
