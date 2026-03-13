// Workout.swift
// SR-Cardiocare — WorkoutSession, WorkoutFeedback models

import Foundation

struct WorkoutSession: Codable, Identifiable {
    let id: String
    let patientId: String
    let planExerciseId: String
    let completedAt: Date?
    let status: String

    enum CodingKeys: String, CodingKey {
        case id
        case patientId = "patient_id"
        case planExerciseId = "plan_exercise_id"
        case completedAt = "completed_at"
        case status
    }
}

struct WorkoutFeedback: Codable, Identifiable {
    let id: String
    let sessionId: String
    let patientId: String
    let painLevel: Int        // 1-10
    let difficultyRating: Int // 1-5
    let notes: String?
    let submittedAt: Date?
    let reviewedByDoctor: Bool

    enum CodingKeys: String, CodingKey {
        case id
        case sessionId = "session_id"
        case patientId = "patient_id"
        case painLevel = "pain_level"
        case difficultyRating = "difficulty_rating"
        case notes
        case submittedAt = "submitted_at"
        case reviewedByDoctor = "reviewed_by_doctor"
    }
}
