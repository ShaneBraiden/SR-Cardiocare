// Models.kt — Shared data classes for SR-Cardiocare Android
package com.srcardiocare.data.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val profileImageUrl: String? = null,
    val phone: String? = null
)

enum class UserRole { PATIENT, DOCTOR, ADMIN }

data class DoctorProfile(
    val userId: String,
    val speciality: String,
    val licenseNumber: String,
    val clinicName: String
)

data class PatientProfile(
    val userId: String,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val injuries: List<String> = emptyList(),
    val primaryGoal: String? = null,
    val assignedDoctorId: String? = null
)

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: String,
    val difficultyLevel: String,
    val durationSeconds: Int,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val sets: Int = 3,
    val reps: Int = 10,
    val instructions: String? = null
)

data class ExercisePlan(
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val doctorId: String,
    val name: String,
    val exercises: List<PlanExercise>,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean = true
)

data class PlanExercise(
    val exerciseId: String,
    val order: Int,
    val customSets: Int? = null,
    val customReps: Int? = null,
    val notes: String? = null,
    val expiryDate: String? = null // ISO date string when this workout expires
)

data class WorkoutSession(
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val planId: String,
    val startedAt: String,
    val completedAt: String? = null,
    val exercisesCompleted: Int = 0,
    val totalExercises: Int = 0
)

data class WorkoutFeedback(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val painLevel: Int,      // 0-10
    val difficulty: Int,      // 1-5
    val notes: String? = null,
    val submittedAt: String? = null
)

data class Appointment(
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val doctorId: String,
    val dateTime: String,
    val durationMinutes: Int = 30,
    val type: String,
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val notes: String? = null
)

enum class AppointmentStatus { SCHEDULED, PENDING, CONFIRMED, CANCELLED, COMPLETED }

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// EXERCISE ASSIGNMENT SYSTEM
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Assignment: Doctor assigns an exercise to a patient with specific dates and frequency.
 * Each assignment has its own lifecycle (active → expired → history).
 */
data class Assignment(
    val id: String = UUID.randomUUID().toString(),
    val patientId: String,
    val doctorId: String,
    val exerciseId: String,
    val exerciseName: String,           // Denormalized for display
    val exerciseVideoUrl: String? = null,
    val exerciseThumbnailUrl: String? = null,
    val exerciseCategory: String? = null,
    val exerciseDifficulty: String? = null,
    val startDate: String,              // ISO date (yyyy-MM-dd) - inclusive
    val endDate: String,                // ISO date (yyyy-MM-dd) - inclusive
    val dailyFrequency: Int = 3,        // How many times per day (doctor sets this)
    val sets: Int = 3,                  // Sets per session
    val reps: Int = 10,                 // Reps per set
    val instructions: String? = null,   // Doctor's notes for patient
    val completionThreshold: Float = 0.8f, // 0.0-1.0, what % = "fully completed"
    val isActive: Boolean = true,       // Soft delete / reassignment
    val createdAt: String? = null       // Server timestamp
)

/** Status of an assignment after it expires (for history) */
enum class AssignmentHistoryStatus {
    FULLY_COMPLETED,    // Met completion threshold
    PARTIALLY_COMPLETED, // Did some but below threshold
    MISSED              // Did nothing
}

/**
 * SessionLog: One entry per exercise session attempt.
 * Patient can do up to dailyFrequency sessions per day.
 */
data class SessionLog(
    val id: String = UUID.randomUUID().toString(),
    val assignmentId: String,
    val patientId: String,
    val sessionDate: String,            // ISO date (yyyy-MM-dd) - which day
    val sessionNumber: Int,             // 1, 2, or 3 for that day
    val startedAt: String? = null,      // ISO timestamp
    val completedAt: String? = null,    // ISO timestamp (null if abandoned)
    val setsCompleted: Int = 0,
    val totalSets: Int = 3,
    val setLogs: List<SetLog> = emptyList(),
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val feedbackId: String? = null      // Link to WorkoutFeedback if submitted
)

enum class SessionStatus {
    IN_PROGRESS,  // Currently doing this session
    COMPLETED,    // Finished all sets and clicked complete
    ABANDONED     // Started but didn't finish (partial)
}

/**
 * SetLog: Tracks individual set completion within a session.
 * Embedded within SessionLog.
 */
data class SetLog(
    val setNumber: Int,                 // 1, 2, 3...
    val startedAt: String? = null,      // ISO timestamp
    val completedAt: String? = null,    // ISO timestamp
    val videoWatchedSeconds: Int = 0,   // How much video they watched
    val repsCompleted: Int? = null      // Optional manual input
)

// ═══════════════════════════════════════════════════════════════════════════════
// UI STATE MODELS (for composables)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Active exercise item for today's list.
 * Computed from Assignment + today's SessionLogs.
 */
data class ActiveExerciseItem(
    val assignment: Assignment,
    val sessionsToday: Int,             // How many completed today
    val dailyTarget: Int,               // = assignment.dailyFrequency
    val isDoneToday: Boolean,           // sessionsToday >= dailyTarget
    val canStartSession: Boolean,       // !isDoneToday && no in-progress session
    val currentSession: SessionLog? = null, // If there's an in-progress session
    val expiryText: String? = null,     // "Expires in 3 days", "Expires today"
    val progressText: String = "0/3"    // "1/3", "2/3", "3/3"
)

/**
 * History item for expired assignments.
 */
data class HistoryExerciseItem(
    val assignment: Assignment,
    val status: AssignmentHistoryStatus,
    val completionRate: Float,          // 0.0-1.0
    val sessionsCompleted: Int,
    val sessionsPossible: Int,          // totalDays * dailyFrequency
    val endDate: String
)
