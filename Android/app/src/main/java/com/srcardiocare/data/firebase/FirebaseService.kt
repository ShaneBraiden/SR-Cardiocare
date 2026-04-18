// FirebaseService.kt — Central Firebase SDK + YouTube service layer for Android
// Firebase: Authentication + Firestore (free tier)
// YouTube: Video storage via Data API v3 (free quota — unlisted uploads)
package com.srcardiocare.data.firebase

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Exception thrown when a rate limit is exceeded.
 */
class RateLimitExceededException(message: String) : Exception(message)

data class AccessControlSettings(
    val sessionLocksEnabled: Boolean = true,
    val blockAllPatients: Boolean = false,
    val blockAllDoctors: Boolean = false
)

/**
 * Rate limiter for tracking method call frequency.
 * Thread-safe implementation for concurrent access.
 */
private data class RateLimitBucket(
    val timestamps: MutableList<Long> = mutableListOf(),
    val maxCalls: Int,
    val windowMillis: Long
) {
    @Synchronized
    fun allowRequest(): Boolean {
        val now = System.currentTimeMillis()
        // Remove timestamps outside the time window
        timestamps.removeAll { it < now - windowMillis }
        
        return if (timestamps.size < maxCalls) {
            timestamps.add(now)
            true
        } else {
            false
        }
    }
    
    @Synchronized
    fun getNextAllowedTime(): Long {
        if (timestamps.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val oldestTimestamp = timestamps.minOrNull() ?: return 0
        return maxOf(0, (oldestTimestamp + windowMillis) - now)
    }
}

/**
 * Central service for all Firebase operations.
 * Replaces Retrofit ApiService + RehabRepository with direct Firebase SDK calls.
 * Video uploads use YouTube Data API v3 (see YouTubeUploader.kt).
 */
object FirebaseService {

    private const val TAG = "FirebaseService"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    // Rate limiting: 5 logSetCompletion calls per 15 minutes per session
    private val setCompletionRateLimiters = mutableMapOf<String, RateLimitBucket>()
    private const val SET_COMPLETION_MAX_CALLS = 5
    private const val SET_COMPLETION_WINDOW_MS = 15 * 60 * 1000L // 15 minutes
    private const val RATE_LIMITER_CLEANUP_THRESHOLD_MS = 60 * 60 * 1000L // 1 hour
    private var lastRateLimiterCleanup = System.currentTimeMillis()
    
    /**
     * Periodically clean up stale rate limiter buckets to prevent memory leaks.
     * Called before adding new buckets.
     */
    private fun cleanupStaleRateLimiters() {
        val now = System.currentTimeMillis()
        if (now - lastRateLimiterCleanup < RATE_LIMITER_CLEANUP_THRESHOLD_MS) return
        
        synchronized(setCompletionRateLimiters) {
            val staleKeys = setCompletionRateLimiters.entries
                .filter { (_, bucket) -> 
                    bucket.timestamps.isEmpty() || 
                    bucket.timestamps.all { it < now - RATE_LIMITER_CLEANUP_THRESHOLD_MS }
                }
                .map { it.key }
            
            staleKeys.forEach { setCompletionRateLimiters.remove(it) }
            lastRateLimiterCleanup = now
            
            if (staleKeys.isNotEmpty()) {
                Log.d(TAG, "Cleaned up ${staleKeys.size} stale rate limiter buckets")
            }
        }
    }

    // ── Auth State ──────────────────────────────────────────────────────

    val currentUID: String? get() = auth.currentUser?.uid
    val isAuthenticated: Boolean get() = auth.currentUser != null

    // ── Auth ────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Map<String, Any?> {
        var loginHandled = false
        var attemptedUid: String? = null
        val normalizedEmail = email.trim().lowercase()

        try {
            val result = auth.signInWithEmailAndPassword(normalizedEmail, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed: no user")
            attemptedUid = uid

            val userData = fetchUser(uid)
            val role = (userData["role"] as? String ?: "").lowercase()
            val isBlocked = userData["isBlocked"] as? Boolean ?: false

            if (isBlocked) {
                auth.signOut()
                recordLoginLog(
                    userId = uid,
                    email = normalizedEmail,
                    role = role,
                    status = "blocked",
                    message = "Blocked by admin"
                )
                loginHandled = true
                throw Exception("Your account access has been blocked by admin. Please contact support.")
            }

            val accessSettings = fetchAccessControlSettings()
            val roleBlockedByPolicy = (role == "patient" && accessSettings.blockAllPatients) ||
                (role == "doctor" && accessSettings.blockAllDoctors)
            if (roleBlockedByPolicy) {
                auth.signOut()
                recordLoginLog(
                    userId = uid,
                    email = normalizedEmail,
                    role = role,
                    status = "blocked",
                    message = "Blocked by admin policy"
                )
                loginHandled = true
                throw Exception("Your account access is temporarily blocked by admin settings.")
            }

            // Update last seen on login
            updateLastSeen()
            recordLoginLog(
                userId = uid,
                email = normalizedEmail,
                role = role,
                status = "success",
                message = null
            )
            loginHandled = true
            return userData
        } catch (e: Exception) {
            if (!loginHandled) {
                recordLoginLog(
                    userId = attemptedUid,
                    email = normalizedEmail,
                    role = null,
                    status = "failed",
                    message = e.message
                )
            }
            throw e
        }
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
            "isBlocked" to false,
            "apiAccessBlocked" to false,
            "phone" to null,
            "profileImageUrl" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).set(userData).await()
        return userData
    }

    /**
     * Register a new user WITHOUT switching the current auth session.
     * Uses a temporary secondary FirebaseApp so the doctor/admin stays signed in.
     * Returns the new user's UID.
     */
    suspend fun registerOther(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: String
    ): String {
        val defaultApp = FirebaseApp.getInstance()
        // Get or create secondary app
        val secondaryApp = try {
            FirebaseApp.getInstance("accountCreator")
        } catch (_: Exception) {
            FirebaseApp.initializeApp(
                defaultApp.applicationContext,
                defaultApp.options,
                "accountCreator"
            )
        }

        val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
        try {
            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val newUid = result.user?.uid ?: throw Exception("Registration failed")

            // Set display name on the secondary auth user
            result.user?.updateProfile(
                userProfileChangeRequest { displayName = "$firstName $lastName" }
            )?.await()

            // Sign out from secondary immediately
            secondaryAuth.signOut()

            // Write user doc using the PRIMARY Firestore (authenticated as doctor/admin)
            val userData = hashMapOf<String, Any?>(
                "email" to email,
                "firstName" to firstName,
                "lastName" to lastName,
                "role" to role,
                "isBlocked" to false,
                "apiAccessBlocked" to false,
                "phone" to null,
                "profileImageUrl" to null,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(newUid).set(userData).await()
            return newUid
        } catch (e: Exception) {
            secondaryAuth.signOut()
            throw e
        }
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

    /** Update lastSeen timestamp */
    suspend fun updateLastSeen() {
        val uid = currentUID ?: return
        try {
            db.collection("users").document(uid)
                .update("lastSeen", FieldValue.serverTimestamp()).await()
        } catch (_: Exception) { }
    }

    /** Update another user's fields by their ID (for admin/doctor). */
    suspend fun updateUserById(uid: String, fields: Map<String, Any>) {
        db.collection("users").document(uid).update(fields).await()
    }

    /**
     * Block or unblock a user from app/API access.
     * When blocked, login is denied and active sessions are forced out by the doc guard.
     */
    suspend fun setUserAccessBlocked(
        uid: String,
        blocked: Boolean,
        reason: String? = null
    ) {
        val actorId = currentUID
        val updates = mutableMapOf<String, Any?>(
            "isBlocked" to blocked,
            "apiAccessBlocked" to blocked,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (blocked) {
            updates["blockedAt"] = FieldValue.serverTimestamp()
            updates["blockedBy"] = actorId
            updates["blockReason"] = reason?.trim().orEmpty()
        } else {
            updates["blockedAt"] = null
            updates["blockedBy"] = null
            updates["blockReason"] = null
        }

        db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
    }

    /** Delete a user's Firestore document (for admin). Note: Auth account must be deleted via Admin SDK. */
    suspend fun deleteUser(uid: String) {
        db.collection("users").document(uid).delete().await()
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

    /** Fetch ALL users regardless of role (for admin role). */
    suspend fun fetchAllUsers(): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("users").get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /** Fetch recent login logs (admin). */
    suspend fun fetchLoginLogs(limit: Int = 150): List<Pair<String, Map<String, Any?>>> {
        val safeLimit = limit.coerceIn(20, 500).toLong()
        val snapshot = db.collection("loginLogs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(safeLimit)
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
                // Log error but don't fail the exercise deletion
                Log.e(TAG, "Failed to delete storage file: ${e.message}", e)
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

        // Also create a standalone Assignment for the new assignment-based system
        val assignmentData = hashMapOf<String, Any>(
            "patientId" to patientId,
            "doctorId" to doctorId,
            "exerciseId" to (exerciseData["exerciseId"] ?: ""),
            "exerciseName" to (exerciseData["name"] ?: ""),
            "exerciseVideoUrl" to (exerciseData["videoUrl"] ?: ""),
            "exerciseCategory" to (exerciseData["category"] ?: ""),
            "exerciseDifficulty" to (exerciseData["difficulty"] ?: ""),
            "startDate" to java.time.LocalDate.now().toString(),
            "endDate" to java.time.LocalDate.now().plusDays(7).toString(), // Default to 7 days
            "dailyFrequency" to 1,
            "sets" to (exerciseData["customSets"] ?: exerciseData["sets"] ?: 3),
            "reps" to (exerciseData["customReps"] ?: exerciseData["reps"] ?: 10),
            "instructions" to (exerciseData["instructions"] ?: ""),
            "completionThreshold" to 1.0f,
            "isActive" to true
        )
        createAssignment(assignmentData)

        val exerciseName = exerciseData["name"]?.toString()
            ?: exerciseData["title"]?.toString()
            ?: "a new exercise"
        com.srcardiocare.core.push.Notifier.send(
            com.srcardiocare.core.push.NotificationEvent.ExerciseAssigned(
                patientId = patientId,
                exerciseName = exerciseName
            )
        )
    }

    /**
     * Assigns an exercise to a patient with prescription dates.
     * Creates or updates the active plan with expiry information.
     */
    suspend fun assignExerciseToPatientWithPrescription(
        patientId: String,
        exerciseData: Map<String, Any>,
        expiryDays: Int,
        expiryDate: String
    ) {
        val doctorId = currentUID ?: throw Exception("Not authenticated")
        val plans = fetchPlans(patientId)
        val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }

        if (activePlan != null) {
            // Update existing plan with exercise and prescription info
            val planId = activePlan.first
            db.collection("plans").document(planId).update(
                mapOf(
                    "exercises" to FieldValue.arrayUnion(exerciseData),
                    "expiryDays" to expiryDays,
                    "expiryDate" to expiryDate
                )
            ).await()
        } else {
            // Create new active plan with prescription info
            val planData = hashMapOf<String, Any>(
                "patientId" to patientId,
                "doctorId" to doctorId,
                "isActive" to true,
                "exercises" to listOf(exerciseData),
                "expiryDays" to expiryDays,
                "expiryDate" to expiryDate,
                "startDate" to java.time.LocalDate.now().toString()
            )
            createPlan(planData)
        }

        // Also create a standalone Assignment for the new assignment-based system
        val assignmentData = hashMapOf<String, Any>(
            "patientId" to patientId,
            "doctorId" to doctorId,
            "exerciseId" to (exerciseData["exerciseId"] ?: ""),
            "exerciseName" to (exerciseData["name"] ?: ""),
            "exerciseVideoUrl" to (exerciseData["videoUrl"] ?: ""),
            "exerciseCategory" to (exerciseData["category"] ?: ""),
            "exerciseDifficulty" to (exerciseData["difficulty"] ?: ""),
            "startDate" to java.time.LocalDate.now().toString(),
            "endDate" to expiryDate,
            "dailyFrequency" to 1,
            "sets" to (exerciseData["customSets"] ?: exerciseData["sets"] ?: 3),
            "reps" to (exerciseData["customReps"] ?: exerciseData["reps"] ?: 10),
            "instructions" to (exerciseData["instructions"] ?: ""),
            "completionThreshold" to 1.0f,
            "isActive" to true
        )
        createAssignment(assignmentData)

        val exerciseName = exerciseData["name"]?.toString()
            ?: exerciseData["title"]?.toString()
            ?: "a new exercise"
        com.srcardiocare.core.push.Notifier.send(
            com.srcardiocare.core.push.NotificationEvent.PrescriptionUpdated(
                patientId = patientId,
                exerciseName = exerciseName,
                expiryDate = expiryDate
            )
        )
    }

    /**
     * Removes a specific exercise from a patient's active plan.
     */
    suspend fun removeExerciseFromPlan(
        patientId: String,
        exerciseData: Map<String, Any>
    ) {
        val plans = fetchPlans(patientId)
        val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }
        
        if (activePlan != null) {
            val planId = activePlan.first
            db.collection("plans").document(planId).update(
                "exercises", FieldValue.arrayRemove(exerciseData)
            ).await()
        }
    }

    /**
     * Send feedback from doctor to patient.
     */
    suspend fun sendFeedback(patientId: String, message: String) {
        val doctorId = currentUID ?: throw Exception("Not authenticated")
        val ref = db.collection("feedback").document()
        val data = hashMapOf<String, Any>(
            "id" to ref.id,
            "patientId" to patientId,
            "doctorId" to doctorId,
            "message" to message,
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()

        com.srcardiocare.core.push.Notifier.send(
            com.srcardiocare.core.push.NotificationEvent.DoctorFeedback(
                patientId = patientId,
                preview = message.take(120)
            )
        )
    }

    // ── Workouts ────────────────────────────────────────────────────────

    suspend fun fetchWorkouts(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("workouts")
            .whereEqualTo("patientId", patientId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
            .sortedByDescending { (it.second["startedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L }
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

    suspend fun incrementExerciseProgress(patientId: String, planId: String, totalCount: Int): String? {
        val workouts = fetchWorkouts(patientId)
        val todayStart = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond()
        val latest = workouts.firstOrNull { (it.second["startedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0 >= todayStart }
        
        var workoutId = latest?.first
        var comp = (latest?.second?.get("exercisesCompleted") as? Number)?.toInt() ?: 0

        // If no workout today, or the latest one is already finished, start a new one
        if (workoutId == null || comp >= totalCount) {
            workoutId = startWorkout(planId, totalCount)
            comp = 0
        }

        if (comp < totalCount) {
            val newComp = comp + 1
            if (newComp >= totalCount) {
                completeWorkout(workoutId, newComp)
            } else {
                db.collection("workouts").document(workoutId).update("exercisesCompleted", newComp).await()
            }
            return workoutId
        }
        return null
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
        val field = if (role == "doctor" || role == "admin") "doctorId" else "patientId"
        val snapshot = db.collection("appointments")
            .whereEqualTo(field, userId)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /** Fallback when composite index is missing — fetches without ordering. */
    suspend fun fetchAppointmentsUnordered(userId: String, role: String): List<Pair<String, Map<String, Any?>>> {
        val field = if (role == "doctor" || role == "admin") "doctorId" else "patientId"
        val snapshot = db.collection("appointments")
            .whereEqualTo(field, userId)
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

    // ── Workout Completion Tracking ─────────────────────────────────────

    /**
     * Count how many workouts were completed today for a specific patient.
     */
    suspend fun fetchWorkoutCompletionsToday(patientId: String): Int {
        val todayStart = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
        val timestamp = com.google.firebase.Timestamp(todayStart.epochSecond, 0)
        
        return try {
            val snapshot = db.collection("workouts")
                .whereEqualTo("patientId", patientId)
                .whereGreaterThanOrEqualTo("completedAt", timestamp)
                .get().await()
            snapshot.size()
        } catch (_: Exception) {
            // If composite index missing, count manually
            val snapshot = db.collection("workouts")
                .whereEqualTo("patientId", patientId)
                .get().await()
            snapshot.documents.count { doc ->
                val completedAt = doc.getTimestamp("completedAt")
                completedAt != null && completedAt.toDate().toInstant().isAfter(todayStart)
            }
        }
    }

    // ── Notifications ───────────────────────────────────────────────────
    //
    // Schema (collection: "notifications"):
    //   id: string, userId: string (recipient), title: string, body: string,
    //   type: string (category for UI coloring), route: string (deep-link target),
    //   params: map<string,string> (route arguments), isRead: bool,
    //   createdAt: Timestamp (server)
    //
    // Writes are driven by `core.push.Notifier` — do not call [writeNotification]
    // directly from UI; emit a typed event through Notifier so every push has a
    // stable route + params contract.

    /** Persists a notification document. Triggers the Cloud Function FCM fan-out. */
    suspend fun writeNotification(
        userId: String,
        title: String,
        body: String,
        type: String,
        route: String,
        params: Map<String, String> = emptyMap()
    ): String {
        val ref = db.collection("notifications").document()
        val data = mapOf(
            "id" to ref.id,
            "userId" to userId,
            "title" to title,
            "body" to body,
            "type" to type,
            "route" to route,
            "params" to params,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        ref.set(data).await()
        return ref.id
    }

    suspend fun fetchNotifications(userId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .limit(100)
            .get().await()
        return snapshot.documents
            .map { it.id to (it.data ?: emptyMap()) }
            .sortedByDescending { notificationEpochMillis(it.second["createdAt"]) }
            .take(50)
    }

    fun observeNotifications(userId: String): Flow<List<Pair<String, Map<String, Any?>>>> = callbackFlow {
        val registration = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifications = snapshot?.documents
                    ?.map { it.id to (it.data ?: emptyMap()) }
                    ?.sortedByDescending { notificationEpochMillis(it.second["createdAt"]) }
                    ?: emptyList()
                trySend(notifications)
            }
        awaitClose { registration.remove() }
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

    suspend fun markNotificationsReadByType(userId: String, type: String) {
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("type", type)
            .whereEqualTo("isRead", false)
            .get().await()
        if (snapshot.isEmpty) return
        val batch = db.batch()
        for (doc in snapshot.documents) {
            batch.update(doc.reference, "isRead", true)
        }
        batch.commit().await()
    }

    private fun notificationEpochMillis(raw: Any?): Long = when (raw) {
        is com.google.firebase.Timestamp -> raw.toDate().time
        is String -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrDefault(0L)
        else -> 0L
    }

    private suspend fun recordLoginLog(
        userId: String?,
        email: String,
        role: String?,
        status: String,
        message: String?
    ) {
        try {
            val ref = db.collection("loginLogs").document()
            val payload = mutableMapOf<String, Any?>(
                "id" to ref.id,
                "userId" to userId,
                "email" to email,
                "role" to role,
                "status" to status,
                "message" to message,
                "platform" to "android",
                "createdAt" to FieldValue.serverTimestamp()
            )
            ref.set(payload).await()
        } catch (_: Exception) {
            // Logging must never block auth flow.
        }
    }

    private fun isMissingIndexError(e: Exception): Boolean {
        return when (e) {
            is FirebaseFirestoreException -> {
                e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION ||
                    (e.message?.contains("requires an index", ignoreCase = true) == true)
            }
            else -> e.message?.contains("requires an index", ignoreCase = true) == true
        }
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

    // ── Post-Workout Feedback ───────────────────────────────────────────

    /**
     * Submit post-workout feedback to a top-level collection.
     */
    suspend fun submitPostWorkoutFeedback(data: Map<String, Any?>) {
        val ref = db.collection("postWorkoutFeedback").document()
        val mutableData = data.toMutableMap()
        mutableData["id"] = ref.id
        ref.set(mutableData).await()
    }

    suspend fun fetchPatientFeedbacks(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("postWorkoutFeedback")
            .whereEqualTo("patientId", patientId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
            .sortedByDescending { (it.second["submittedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L }
    }

    suspend fun fetchDoctorFeedbacks(doctorId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("postWorkoutFeedback")
            .whereEqualTo("doctorId", doctorId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
            .sortedByDescending { (it.second["submittedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L }
    }

    suspend fun fetchAllFeedbacks(): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("postWorkoutFeedback")
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    // ── Password Change ─────────────────────────────────────────────────

    /**
     * Re-authenticate with old password and update to new password.
     */
    suspend fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser ?: throw Exception("Not authenticated")
        val email = user.email ?: throw Exception("No email on account")

        // Re-authenticate
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).await()

        // Update password
        user.updatePassword(newPassword).await()
    }

    // ── Chat Messaging ──────────────────────────────────────────────────

    suspend fun sendChatMessage(patientId: String, senderId: String, senderName: String, text: String) {
        val msg = mapOf(
            "id" to UUID.randomUUID().toString(),
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        db.collection("chats").document(patientId).collection("messages").add(msg).await()

        val receiverId = if (senderId == patientId) {
            val patientUser = runCatching { fetchUser(patientId) }.getOrNull().orEmpty()
            (patientUser["assignedDoctorId"] as? String)?.takeIf { it.isNotBlank() }
        } else {
            patientId
        }

        if (!receiverId.isNullOrBlank() && receiverId != senderId) {
            val senderRole = runCatching {
                (fetchUser(senderId)["role"] as? String ?: "").lowercase()
            }.getOrDefault("")

            com.srcardiocare.core.push.Notifier.send(
                com.srcardiocare.core.push.NotificationEvent.ChatMessage(
                    recipientId = receiverId,
                    senderName = senderName,
                    preview = text.take(90),
                    chatThreadId = patientId,
                    senderIsClinician = senderRole == "doctor" || senderRole == "admin"
                )
            )
        }
    }

    fun observeChatMessages(patientId: String): kotlinx.coroutines.flow.Flow<List<Map<String, Any?>>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = db.collection("chats").document(patientId).collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val msgs = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    data.toMutableMap().apply { put("docId", doc.id) }
                } ?: emptyList()
                trySend(msgs)
            }
        awaitClose { listener.remove() }
    }

    // ── Global Settings ──────────────────────────────────────────────────

    suspend fun fetchAccessControlSettings(): AccessControlSettings {
        return try {
            val doc = db.collection("settings").document("appSettings").get().await()
            if (!doc.exists()) return AccessControlSettings()

            AccessControlSettings(
                sessionLocksEnabled = doc.getBoolean("sessionLocksEnabled") ?: true,
                blockAllPatients = doc.getBoolean("blockAllPatients") ?: false,
                blockAllDoctors = doc.getBoolean("blockAllDoctors") ?: false
            )
        } catch (_: Exception) {
            AccessControlSettings()
        }
    }

    suspend fun updateAccessControlSettings(
        sessionLocksEnabled: Boolean? = null,
        blockAllPatients: Boolean? = null,
        blockAllDoctors: Boolean? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        sessionLocksEnabled?.let { updates["sessionLocksEnabled"] = it }
        blockAllPatients?.let { updates["blockAllPatients"] = it }
        blockAllDoctors?.let { updates["blockAllDoctors"] = it }
        if (updates.isEmpty()) return

        db.collection("settings").document("appSettings")
            .set(updates, SetOptions.merge())
            .await()
    }

    suspend fun fetchSessionLocksEnabled(): Boolean {
        return fetchAccessControlSettings().sessionLocksEnabled
    }

    suspend fun updateSessionLocksEnabled(enabled: Boolean) {
        updateAccessControlSettings(sessionLocksEnabled = enabled)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXERCISE ASSIGNMENT SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create a new assignment (doctor assigns exercise to patient).
     */
    suspend fun createAssignment(data: Map<String, Any>): String {
        currentUID ?: throw Exception("Not authenticated")
        val ref = db.collection("assignments").document()
        val mutableData = data.toMutableMap()
        mutableData["id"] = ref.id
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        ref.set(mutableData).await()
        return ref.id
    }

    /**
     * Fetch all assignments for a patient.
     */
    suspend fun fetchAssignments(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("assignments")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("isActive", true)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Fetch assignments created by a doctor.
     */
    suspend fun fetchDoctorAssignments(doctorId: String): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("assignments")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("isActive", true)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Update an existing assignment.
     */
    suspend fun updateAssignment(assignmentId: String, updates: Map<String, Any>) {
        db.collection("assignments").document(assignmentId)
            .update(updates).await()
    }

    /**
     * Deactivate an assignment (soft delete).
     */
    suspend fun deactivateAssignment(assignmentId: String) {
        db.collection("assignments").document(assignmentId)
            .update("isActive", false).await()
    }

    // ── Session Logging ─────────────────────────────────────────────────────

    /**
     * Start a new exercise session.
     */
    suspend fun startSession(
        assignmentId: String,
        sessionDate: String,
        sessionNumber: Int,
        totalSets: Int
    ): String {
        val patientId = currentUID ?: throw Exception("Not authenticated")
        val ref = db.collection("sessionLogs").document()
        val data = hashMapOf<String, Any?>(
            "id" to ref.id,
            "assignmentId" to assignmentId,
            "patientId" to patientId,
            "sessionDate" to sessionDate,
            "sessionNumber" to sessionNumber,
            "startedAt" to FieldValue.serverTimestamp(),
            "completedAt" to null,
            "setsCompleted" to 0,
            "totalSets" to totalSets,
            "setLogs" to emptyList<Map<String, Any>>(),
            "status" to "IN_PROGRESS",
            "feedbackId" to null
        )
        ref.set(data).await()
        return ref.id
    }

    /**
     * Log completion of a single set within a session.
     * 
     * **Rate Limited:** 5 calls per 15 minutes per session to prevent abuse.
     * Throws RateLimitExceededException if limit is exceeded.
     */
    suspend fun logSetCompletion(
        sessionId: String,
        setNumber: Int,
        videoWatchedSeconds: Int,
        repsCompleted: Int?
    ) {
        // Input validation
        require(setNumber > 0) { "setNumber must be positive" }
        require(videoWatchedSeconds in 0..86400) { "videoWatchedSeconds must be 0-86400" }
        require(repsCompleted == null || repsCompleted > 0) { "repsCompleted must be positive if provided" }
        
        // Clean up stale rate limiters periodically
        cleanupStaleRateLimiters()
        
        // Rate limiting check
        val bucket = synchronized(setCompletionRateLimiters) {
            setCompletionRateLimiters.getOrPut(sessionId) {
                RateLimitBucket(
                    maxCalls = SET_COMPLETION_MAX_CALLS,
                    windowMillis = SET_COMPLETION_WINDOW_MS
                )
            }
        }
        
        if (!bucket.allowRequest()) {
            val waitTimeMs = bucket.getNextAllowedTime()
            val waitMinutes = (waitTimeMs / 60000.0).toInt()
            throw RateLimitExceededException(
                "Rate limit exceeded for logSetCompletion. " +
                "Maximum $SET_COMPLETION_MAX_CALLS calls per 15 minutes. " +
                "Please wait $waitMinutes minute(s) before trying again."
            )
        }
        
        // Use server timestamp for consistency with other Firestore operations
        val setLog = hashMapOf<String, Any?>(
            "setNumber" to setNumber,
            "completedAt" to FieldValue.serverTimestamp(),
            "videoWatchedSeconds" to videoWatchedSeconds,
            "repsCompleted" to repsCompleted
        )
        db.collection("sessionLogs").document(sessionId).update(
            mapOf(
                "setLogs" to FieldValue.arrayUnion(setLog),
                "setsCompleted" to setNumber
            )
        ).await()
    }

    /**
     * Complete a session (all sets done, user clicked Complete).
     * Cleans up rate limiter for this session.
     */
    suspend fun completeSession(sessionId: String, feedbackId: String? = null) {
        db.collection("sessionLogs").document(sessionId).update(
            mapOf(
                "status" to "COMPLETED",
                "completedAt" to FieldValue.serverTimestamp(),
                "feedbackId" to feedbackId
            )
        ).await()
        
        // Clean up rate limiter for completed session
        cleanupRateLimiter(sessionId)
    }

    /**
     * Mark a session as abandoned (started but not finished).
     * Cleans up rate limiter for this session.
     */
    suspend fun abandonSession(sessionId: String) {
        db.collection("sessionLogs").document(sessionId).update(
            mapOf(
                "status" to "ABANDONED",
                "completedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        
        // Clean up rate limiter for abandoned session
        cleanupRateLimiter(sessionId)
    }
    
    /**
     * Clean up rate limiter bucket for a session to prevent memory leaks.
     */
    private fun cleanupRateLimiter(sessionId: String) {
        synchronized(setCompletionRateLimiters) {
            setCompletionRateLimiters.remove(sessionId)
        }
    }

    /**
     * Fetch sessions for a specific assignment and date.
     */
    suspend fun fetchSessionsForDate(
        assignmentId: String,
        sessionDate: String
    ): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("sessionLogs")
            .whereEqualTo("assignmentId", assignmentId)
            .whereEqualTo("sessionDate", sessionDate)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Fetch all sessions for an assignment (for history/stats).
     */
    suspend fun fetchAllSessionsForAssignment(
        assignmentId: String
    ): List<Pair<String, Map<String, Any?>>> {
        val snapshot = db.collection("sessionLogs")
            .whereEqualTo("assignmentId", assignmentId)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Fetch today's sessions for a patient (across all assignments).
     */
    suspend fun fetchTodaysSessions(patientId: String): List<Pair<String, Map<String, Any?>>> {
        val today = java.time.LocalDate.now().toString()
        val snapshot = db.collection("sessionLogs")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("sessionDate", today)
            .get().await()
        return snapshot.documents.map { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Find any in-progress session for a patient.
     */
    suspend fun findInProgressSession(patientId: String): Pair<String, Map<String, Any?>>? {
        val snapshot = db.collection("sessionLogs")
            .whereEqualTo("patientId", patientId)
            .whereEqualTo("status", "IN_PROGRESS")
            .limit(1)
            .get().await()
        return snapshot.documents.firstOrNull()?.let { it.id to (it.data ?: emptyMap()) }
    }

    /**
     * Get session count for a specific assignment on a specific date.
     */
    suspend fun getCompletedSessionCount(assignmentId: String, sessionDate: String): Int {
        val snapshot = db.collection("sessionLogs")
            .whereEqualTo("assignmentId", assignmentId)
            .whereEqualTo("sessionDate", sessionDate)
            .whereEqualTo("status", "COMPLETED")
            .get().await()
        return snapshot.documents.size
    }
}
