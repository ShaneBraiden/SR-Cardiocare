// AuthManager.swift
// SR-Cardiocare — Firebase Auth with Firestore user profiles.
// Firebase handles token management automatically (1hr ID tokens, auto-refresh).
// Passwords: min 8 chars (uppercase + lowercase + number) — validated client-side.

import Foundation
import FirebaseAuth
import FirebaseFirestore

enum UserRole: String, Codable {
    case doctor
    case patient
    case admin
}

final class AuthManager {

    static let shared = AuthManager()
    private let db = Firestore.firestore()

    private init() {}

    // MARK: - State

    var isAuthenticated: Bool {
        return Auth.auth().currentUser != nil
    }

    var currentUserId: String? {
        return Auth.auth().currentUser?.uid
    }

    var currentUserRole: UserRole? {
        // Cached from last login/fetch — stored in UserDefaults for quick access
        guard let roleString = UserDefaults.standard.string(forKey: "currentUserRole") else { return nil }
        return UserRole(rawValue: roleString)
    }

    // MARK: - Login

    func login(email: String, password: String) async throws -> [String: Any] {
        // Validate password format client-side before sending
        guard isValidPassword(password) else {
            throw AuthError.invalidPassword
        }

        // Sign in with Firebase Auth
        let result = try await Auth.auth().signIn(withEmail: email, password: password)
        let uid = result.user.uid

        // Fetch user profile from Firestore
        let doc = try await db.collection("users").document(uid).getDocument()
        guard let userData = doc.data() else {
            throw AuthError.userNotFound
        }

        // Cache role locally
        if let role = userData["role"] as? String {
            UserDefaults.standard.set(role, forKey: "currentUserRole")
        }

        return userData
    }

    // MARK: - Register

    func register(email: String, password: String, firstName: String, lastName: String, role: UserRole) async throws -> [String: Any] {
        guard isValidPassword(password) else {
            throw AuthError.invalidPassword
        }

        // Create user in Firebase Auth
        let result = try await Auth.auth().createUser(withEmail: email, password: password)
        let uid = result.user.uid

        // Update display name
        let changeRequest = result.user.createProfileChangeRequest()
        changeRequest.displayName = "\(firstName) \(lastName)"
        try await changeRequest.commitChanges()

        // Create user document in Firestore
        let userData: [String: Any] = [
            "email": email,
            "firstName": firstName,
            "lastName": lastName,
            "role": role.rawValue,
            "phone": NSNull(),
            "profileImageUrl": NSNull(),
            "createdAt": FieldValue.serverTimestamp(),
        ]
        try await db.collection("users").document(uid).setData(userData)

        UserDefaults.standard.set(role.rawValue, forKey: "currentUserRole")

        return userData
    }

    // MARK: - Logout

    func logout() {
        do {
            try Auth.auth().signOut()
        } catch {
            // Best-effort sign out
        }
        UserDefaults.standard.removeObject(forKey: "currentUserRole")
    }

    // MARK: - Auth State Listener

    func addAuthStateListener(_ listener: @escaping (Bool) -> Void) -> AuthStateDidChangeListenerHandle {
        return Auth.auth().addStateDidChangeListener { _, user in
            listener(user != nil)
        }
    }

    // MARK: - Password Validation

    /// Min 8 chars, at least 1 uppercase, 1 lowercase, 1 digit
    func isValidPassword(_ password: String) -> Bool {
        guard password.count >= 8 else { return false }
        let hasUpper = password.range(of: "[A-Z]", options: .regularExpression) != nil
        let hasLower = password.range(of: "[a-z]", options: .regularExpression) != nil
        let hasDigit = password.range(of: "[0-9]", options: .regularExpression) != nil
        return hasUpper && hasLower && hasDigit
    }
}

// MARK: - Auth Error

enum AuthError: LocalizedError {
    case invalidPassword
    case userNotFound
    case sessionExpired

    var errorDescription: String? {
        switch self {
        case .invalidPassword:
            return NSLocalizedString("auth_error_invalid_password", comment: "")
        case .userNotFound:
            return NSLocalizedString("auth_error_user_not_found", comment: "")
        case .sessionExpired:
            return NSLocalizedString("auth_error_session_expired", comment: "")
        }
    }
}
