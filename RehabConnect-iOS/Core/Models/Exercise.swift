// Exercise.swift
// SR-Cardiocare — Exercise, ExercisePlan, PlanExercise models

import Foundation

struct Exercise: Codable, Identifiable {
    let id: String
    let doctorId: String
    let title: String
    let description: String?
    let videoUrl: String?
    let thumbnailUrl: String?
    let durationSecs: Int?
    let category: String?
    let createdAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case doctorId = "doctor_id"
        case title, description
        case videoUrl = "video_url"
        case thumbnailUrl = "thumbnail_url"
        case durationSecs = "duration_secs"
        case category
        case createdAt = "created_at"
    }
}

struct ExercisePlan: Codable, Identifiable {
    let id: String
    let patientId: String
    let doctorId: String
    let title: String
    let startDate: String?
    let endDate: String?
    let status: String?
    let createdAt: Date?

    enum CodingKeys: String, CodingKey {
        case id
        case patientId = "patient_id"
        case doctorId = "doctor_id"
        case title
        case startDate = "start_date"
        case endDate = "end_date"
        case status
        case createdAt = "created_at"
    }
}

struct PlanExercise: Codable, Identifiable {
    let id: String
    let planId: String
    let exerciseId: String
    let scheduledDate: String?
    let orderIndex: Int
    let sets: Int?
    let reps: Int?
    let durationSecs: Int?

    enum CodingKeys: String, CodingKey {
        case id
        case planId = "plan_id"
        case exerciseId = "exercise_id"
        case scheduledDate = "scheduled_date"
        case orderIndex = "order_index"
        case sets, reps
        case durationSecs = "duration_secs"
    }
}
