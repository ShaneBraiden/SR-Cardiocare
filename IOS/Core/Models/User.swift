// User.swift
// SR-Cardiocare — User, DoctorProfile, PatientProfile models

import Foundation

struct User: Codable, Identifiable {
    let id: String
    let email: String
    let role: UserRole
    let createdAt: Date?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id, email, role
        case createdAt = "created_at"
        case isActive = "is_active"
    }
}

struct DoctorProfile: Codable, Identifiable {
    let id: String
    let userId: String
    let fullName: String
    let specialization: String?
    let licenseNumber: String?
    let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case fullName = "full_name"
        case specialization
        case licenseNumber = "license_number"
        case avatarUrl = "avatar_url"
    }
}

struct PatientProfile: Codable, Identifiable {
    let id: String
    let userId: String
    let doctorId: String
    let fullName: String
    let dob: String?
    let injuryType: String?
    let conditionNotes: String?
    let goals: String?
    let onboardingCompleted: Bool
    let avatarUrl: String?

    enum CodingKeys: String, CodingKey {
        case id
        case userId = "user_id"
        case doctorId = "doctor_id"
        case fullName = "full_name"
        case dob
        case injuryType = "injury_type"
        case conditionNotes = "condition_notes"
        case goals
        case onboardingCompleted = "onboarding_completed"
        case avatarUrl = "avatar_url"
    }
}
