// AppNotification.swift
// SR-Cardiocare

import Foundation

struct AppNotification: Codable, Identifiable {
    let id: String
    let userId: String
    let type: String
    let title: String
    let body: String
    let isRead: Bool
    let createdAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case type, title, body
        case isRead = "is_read"
        case createdAt = "created_at"
    }
}
