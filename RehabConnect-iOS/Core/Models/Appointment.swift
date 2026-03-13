// Appointment.swift
// SR-Cardiocare

import Foundation

struct Appointment: Codable, Identifiable {
    let id: String
    let doctorId: String
    let patientId: String
    let scheduledAt: Date
    let durationMins: Int
    let title: String
    let notes: String?
    let status: AppointmentStatus
    let createdBy: String

    enum CodingKeys: String, CodingKey {
        case id
        case doctorId = "doctor_id"
        case patientId = "patient_id"
        case scheduledAt = "scheduled_at"
        case durationMins = "duration_mins"
        case title, notes, status
        case createdBy = "created_by"
    }
}

enum AppointmentStatus: String, Codable {
    case scheduled
    case confirmed
    case completed
    case cancelled
}
