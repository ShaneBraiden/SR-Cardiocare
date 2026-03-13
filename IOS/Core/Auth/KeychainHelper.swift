// KeychainHelper.swift
// SR-Cardiocare — Secure token storage. Never use UserDefaults for tokens.

import Foundation
import Security

final class KeychainHelper {

    static let shared = KeychainHelper()
    private init() {}

    private let service = "com.srcardiocare.auth"

    enum Key: String {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case userId = "user_id"
        case userRole = "user_role"
    }

    // MARK: - Save

    @discardableResult
    func save(_ data: Data, for key: Key) -> Bool {
        delete(key)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]

        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }

    @discardableResult
    func save(_ string: String, for key: Key) -> Bool {
        guard let data = string.data(using: .utf8) else { return false }
        return save(data, for: key)
    }

    // MARK: - Read

    func read(key: Key) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]

        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        guard status == errSecSuccess else { return nil }
        return result as? Data
    }

    func readString(key: Key) -> String? {
        guard let data = read(key: key) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    // MARK: - Delete

    @discardableResult
    func delete(_ key: Key) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue
        ]

        let status = SecItemDelete(query as CFDictionary)
        return status == errSecSuccess || status == errSecItemNotFound
    }

    // MARK: - Clear All

    func clearAll() {
        Key.allCases.forEach { delete($0) }
    }
}

extension KeychainHelper.Key: CaseIterable {}
