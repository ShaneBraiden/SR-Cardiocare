// RehabRepository.kt - Repository pattern wrapping FirebaseService
// Provides a clean API for ViewModels/Composables to access data.
package com.srcardiocare.data.repository

import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository pattern - all composables/viewmodels access data through here.
 * Delegates to FirebaseService for all Firebase operations.
 */
class RehabRepository {

    private val firebase = FirebaseService

    private val _currentUser = MutableStateFlow<Map<String, Any?>?>(null)
    val currentUser: StateFlow<Map<String, Any?>?> = _currentUser

    // -- Auth --

    suspend fun login(email: String, password: String): Result<Map<String, Any?>> = runCatching {
        val userData = firebase.login(email, password)
        _currentUser.value = userData
        userData
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Result<Map<String, Any?>> = runCatching {
        val userData = firebase.register(email, password, firstName, lastName, role)
        _currentUser.value = userData
        userData
    }

    fun logout() {
        firebase.logout()
        _currentUser.value = null
    }

    // -- Patients --

    suspend fun getPatients(doctorId: String): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        firebase.fetchPatients(doctorId)
    }

    // -- Exercises --

    suspend fun getExercises(category: String? = null): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        firebase.fetchExercises(category)
    }

    suspend fun deleteExercise(exerciseId: String, videoUrl: String?): Result<Unit> = runCatching {
        firebase.deleteExercise(exerciseId, videoUrl)
    }

    // -- Plans --

    suspend fun getPlans(patientId: String): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        firebase.fetchPlans(patientId)
    }

    // -- Workouts --

    suspend fun submitFeedback(
        workoutId: String,
        painLevel: Int,
        difficulty: Int,
        notes: String?
    ): Result<Unit> = runCatching {
        firebase.submitFeedback(workoutId, painLevel, difficulty, notes)
    }

    // -- Appointments --

    suspend fun getAppointments(userId: String, role: String): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        firebase.fetchAppointments(userId, role)
    }

    suspend fun createAppointment(data: Map<String, Any>): Result<String> = runCatching {
        firebase.createAppointment(data)
    }

    // -- Delete Patient --

    suspend fun deletePatient(patientId: String): Result<Unit> = runCatching {
        firebase.deletePatient(patientId)
    }

    // -- Doctors --

    suspend fun getAllDoctors(): Result<List<Pair<String, Map<String, Any?>>>> = runCatching {
        firebase.fetchAllDoctors()
    }
}

