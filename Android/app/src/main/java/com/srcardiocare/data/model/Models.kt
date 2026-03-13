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
    val notes: String? = null
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

enum class AppointmentStatus { SCHEDULED, CONFIRMED, CANCELLED, COMPLETED }

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean = false,
    val createdAt: String
)
