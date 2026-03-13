// FirebaseService.kt — Central Firebase SDK + YouTube service layer for Android
// Firebase: Authentication + Firestore (free tier)
// YouTube: Video storage via Data API v3 (free quota — unlisted uploads)
package com.srcardiocare.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Central service for all Firebase operations.
 * Replaces Retrofit ApiService + RehabRepository with direct Firebase SDK calls.
 * Video uploads use YouTube Data API v3 (see YouTubeUploader.kt).
 */
object FirebaseService {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ── Auth State ──────────────────────────────────────────────────────

    val currentUID: String? get() = auth.currentUser?.uid
    val isAuthenticated: Boolean get() = auth.currentUser != null

    // ── Auth ────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Map<String, Any?> {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Login failed: no user")
        return fetchUser(uid)
    }

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): Map<String, Any?> {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw Exception("Registration failed")

        // Update display name
        result.user?.updateProfile(
            userProfileChangeRequest { displayName = "$firstName $lastName" }
        )?.await()

        // Create user doc in Firestore
        val userData = hashMapOf<String, Any?>(
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "role" to role,
            "phone" to null,
            "profileImageUrl" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).set(userData).await()
        return userData
    }

    fun logout() {
        auth.signOut()
    }

    // ── Users ───────────────────────────────────────────────────────────

    suspend fun fetchUser(uid: String): Map<String, Any?> {
        val doc = db.collection("users").document(uid).get().await()
        return doc.data ?: throw Exception("User not found: $uid")
    }

    suspend fun fetchCurrentUser(): Map<String, Any?> {
        val uid = currentUID ?: throw Exception("Not authenticated")
        return fetchUser(uid)
    }

    suspend fun updateUser(fields: Map<String, Any>) {
        val uid = currentUID ?: throw Exception("Not authenticated")
        db.collection("users").document(uid).update(fields).await()
    }

    // ── Patients ────────────────────────────────────────────────────────

    /** Fetch patients assigned to a specific doctor (for doctor role). */
    suspend fun fetchPatients(doctorId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "patient")
            .whereEqualTo("assignedDoctorId", doctorId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /** Fetch ALL patients regardless of assigned doctor (for admin role). */
    suspend fun fetchAllPatients(): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "patient")
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    // ── Exercises ───────────────────────────────────────────────────────

    suspend fun fetchExercises(category: String? = null): List<Pair<String, Map<String, Any?>>> {
        var query: Query = db.collection("exercises")
        if (category != null) {
            query = query.whereEqualTo("category", category)
        }
        val snapshot = query.get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    suspend fun createExercise(data: Map<String, Any>): String {
        val ref = db.collection("exercises").document()
        val mutableData = data.toMutableMap()
        mutableData["id"] = ref.id
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        ref.set(mutableData).await()
        return ref.id
    }

    suspend fun deleteExercise(exerciseId: String, videoUrl: String?) {
        // Delete Firestore document
        db.collection("exercises").document(exerciseId).delete().await()
        
        // Delete Storage file if it exists
        if (!videoUrl.isNullOrBlank()) {
            try {
                // Get a reference from the URL
                val storageRef = storage.getReferenceFromUrl(videoUrl)
                storageRef.delete().await()
            } catch (e: Exception) {
                // Log or ignore failure to delete storage if it doesn't exist
                e.printStackTrace()
            }
        }
    }

    // ── Plans ───────────────────────────────────────────────────────────

    suspend fun fetchPlans(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("plans")
            .whereEqualTo("patientId", patientId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    suspend fun createPlan(data: Map<String, Any>): String {
        val ref = db.collection("plans").document()
        val mutableData = data.toMutableMap()
        mutableData["id"] = ref.id
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        ref.set(mutableData).await()
        return ref.id
    }

    /**
     * Assigns an exercise to a patient by adding it to their active plan.
     * If no active plan exists, creates one first.
     */
    suspend fun assignExerciseToPatient(
        patientId: String,
        exerciseData: Map<String, Any>
    ) {
        val doctorId = currentUID ?: throw Exception("Not authenticated")
        val plans = fetchPlans(patientId)
        val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }

        if (activePlan != null) {
            // Append exercise to existing plan
            val planId = activePlan.first
            db.collection("plans").document(planId).update(
                "exercises", FieldValue.arrayUnion(exerciseData)
            ).await()
        } else {
            // Create new active plan with this exercise
            val planData = hashMapOf<String, Any>(
                "patientId" to patientId,
                "doctorId" to doctorId,
                "isActive" to true,
                "exercises" to listOf(exerciseData)
            )
            createPlan(planData)
        }
    }

    // ── Workouts ────────────────────────────────────────────────────────

    suspend fun fetchWorkouts(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("workouts")
            .whereEqualTo("patientId", patientId)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    suspend fun startWorkout(planId: String, totalExercises: Int): String {
        val uid = currentUID ?: throw Exception("Not authenticated")
        val ref = db.collection("workouts").document()
        val data = hashMapOf<String, Any?>(
            "id" to ref.id,
            "patientId" to uid,
            "planId" to planId,
            "startedAt" to FieldValue.serverTimestamp(),
            "completedAt" to null,
            "exercisesCompleted" to 0,
            "totalExercises" to totalExercises
        )
        ref.set(data).await()
        return ref.id
    }

    suspend fun completeWorkout(id: String, exercisesCompleted: Int) {
        db.collection("workouts").document(id).update(
            mapOf(
                "completedAt" to FieldValue.serverTimestamp(),
                "exercisesCompleted" to exercisesCompleted
            )
        ).await()
    }

    suspend fun submitFeedback(workoutId: String, painLevel: Int, difficulty: Int, notes: String?) {
        val ref = db.collection("workouts").document(workoutId)
            .collection("feedback").document()
        val data = hashMapOf<String, Any?>(
            "painLevel" to painLevel,
            "difficulty" to difficulty,
            "notes" to notes,
            "submittedAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
    }

    // ── Appointments ────────────────────────────────────────────────────

    suspend fun fetchAppointments(userId: String, role: String): List<Pair<String, Map<String, Any?>>> {
        val field = if (role == "doctor") "doctorId" else "patientId"
        val snapshot = db.collection("appointments")
            .whereEqualTo(field, userId)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    suspend fun createAppointment(data: Map<String, Any>): String {
        val ref = db.collection("appointments").document()
        val mutableData = data.toMutableMap()
        mutableData["id"] = ref.id
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        ref.set(mutableData).await()
        return ref.id
    }

    suspend fun updateAppointment(id: String, fields: Map<String, Any>) {
        db.collection("appointments").document(id).update(fields).await()
    }

    // ── Notifications ───────────────────────────────────────────────────

    suspend fun fetchNotifications(userId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    suspend fun markNotificationRead(id: String) {
        db.collection("notifications").document(id)
            .update("isRead", true).await()
    }

    suspend fun markAllNotificationsRead(userId: String) {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get().await()
        val batch = db.batch()
        for (doc in snapshot.documents) {
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    // ── Delete Patient ──────────────────────────────────────────────────

    /**
     * Deletes a patient from Firestore: removes user doc + related plans,
     * workouts, and appointments. Firebase client SDK cannot delete
     * another user's Auth account, so only Firestore data is removed.
     */
    suspend fun deletePatient(patientId: String) {
        val batch = db.batch()

        // Delete related plans
        val plans = db.collection("plans")
            .whereEqualTo("patientId", patientId).get().await()
        for (doc in plans.documents) batch.delete(doc.reference)

        // Delete related workouts
        val workouts = db.collection("workouts")
            .whereEqualTo("patientId", patientId).get().await()
        for (doc in workouts.documents) batch.delete(doc.reference)

        // Delete related appointments
        val appointments = db.collection("appointments")
            .whereEqualTo("patientId", patientId).get().await()
        for (doc in appointments.documents) batch.delete(doc.reference)

        // Delete related notifications
        val notifications = db.collection("notifications")
            .whereEqualTo("userId", patientId).get().await()
        for (doc in notifications.documents) batch.delete(doc.reference)

        // Delete the user document
        batch.delete(db.collection("users").document(patientId))

        batch.commit().await()
    }

    // ── Doctors ─────────────────────────────────────────────────────────

    /** Fetch ALL doctors (for admin role). */
    suspend fun fetchAllDoctors(): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("users")
            .whereEqualTo("role", "doctor")
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    // ── Video Upload (YouTube Data API v3 — free tier) ──────────────────

    /**
     * Uploads a video to Firebase Storage and returns the download URL.
     */
    suspend fun uploadVideo(
        data: ByteArray,
        mimeType: String = "video/mp4"
    ): String {
        currentUID ?: throw Exception("Not authenticated")
        val storageRef = storage.reference
        val videoRef = storageRef.child("videos/${UUID.randomUUID()}.mp4")
        
        val metadata = storageMetadata {
            contentType = mimeType
        }
        
        // Upload the file
        videoRef.putBytes(data, metadata).await()
        
        // Get the download URL
        return videoRef.downloadUrl.await().toString()
    }
}
