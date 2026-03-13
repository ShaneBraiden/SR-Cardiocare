// FirebaseService.swift
// SR-Cardiocare — Central Firebase SDK service layer.
//
// Firebase: Authentication + Firestore (free tier)
// YouTube Data API v3: Video storage (unlisted uploads — free tier)
// Mobile app connects directly to Firebase for data, YouTube for video.

import Foundation
import FirebaseAuth
import FirebaseFirestore

// MARK: - Firebase Error

enum FirebaseServiceError: Error, LocalizedError {
    case notAuthenticated
    case userNotFound
    case documentNotFound(String)
    case firestoreError(Error)
    case storageError(Error)
    case invalidData

    var errorDescription: String? {
        switch self {
        case .notAuthenticated:
            return "User is not authenticated."
        case .userNotFound:
            return "User document not found in Firestore."
        case .documentNotFound(let id):
            return "Document not found: \(id)"
        case .firestoreError(let error):
            return "Firestore error: \(error.localizedDescription)"
        case .storageError(let error):
            return "Storage error: \(error.localizedDescription)"
        case .invalidData:
            return "Invalid data format."
        }
    }
}

// MARK: - FirebaseService

final class FirebaseService {

    static let shared = FirebaseService()

    private let db = Firestore.firestore()

    // YouTube Data API v3 — video uploads as unlisted
    // OAuth access token with youtube.upload scope is required (obtained via Google Sign-In)
    private let youtubeUploadEndpoint = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status"
    private let youtubeVideoCategoryId = "27"  // 27 = Education

    private init() {}

    // MARK: - Current User

    var currentUID: String? {
        return Auth.auth().currentUser?.uid
    }

    var isAuthenticated: Bool {
        return Auth.auth().currentUser != nil
    }

    // MARK: - Users Collection

    func fetchCurrentUser() async throws -> [String: Any] {
        guard let uid = currentUID else { throw FirebaseServiceError.notAuthenticated }
        let doc = try await db.collection("users").document(uid).getDocument()
        guard let data = doc.data() else { throw FirebaseServiceError.userNotFound }
        return data
    }

    func updateUser(fields: [String: Any]) async throws {
        guard let uid = currentUID else { throw FirebaseServiceError.notAuthenticated }
        try await db.collection("users").document(uid).updateData(fields)
    }

    // MARK: - Patients

    func fetchPatients(forDoctor doctorId: String) async throws -> [(id: String, data: [String: Any])] {
        let snapshot = try await db.collection("users")
            .whereField("role", isEqualTo: "patient")
            .whereField("assignedDoctorId", isEqualTo: doctorId)
            .getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func fetchPatient(id: String) async throws -> [String: Any] {
        let doc = try await db.collection("users").document(id).getDocument()
        guard let data = doc.data() else { throw FirebaseServiceError.documentNotFound(id) }
        return data
    }

    // MARK: - Exercises

    func fetchExercises(category: String? = nil) async throws -> [(id: String, data: [String: Any])] {
        var query: Query = db.collection("exercises")
        if let category = category {
            query = query.whereField("category", isEqualTo: category)
        }
        let snapshot = try await query.getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func createExercise(_ data: [String: Any]) async throws -> String {
        let ref = db.collection("exercises").document()
        var mutableData = data
        mutableData["id"] = ref.documentID
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        try await ref.setData(mutableData)
        return ref.documentID
    }

    // MARK: - Plans

    func fetchPlans(forPatient patientId: String) async throws -> [(id: String, data: [String: Any])] {
        let snapshot = try await db.collection("plans")
            .whereField("patientId", isEqualTo: patientId)
            .getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func createPlan(_ data: [String: Any]) async throws -> String {
        let ref = db.collection("plans").document()
        var mutableData = data
        mutableData["id"] = ref.documentID
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        try await ref.setData(mutableData)
        return ref.documentID
    }

    // MARK: - Workouts

    func fetchWorkouts(forPatient patientId: String) async throws -> [(id: String, data: [String: Any])] {
        let snapshot = try await db.collection("workouts")
            .whereField("patientId", isEqualTo: patientId)
            .order(by: "startedAt", descending: true)
            .getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func startWorkout(planId: String, totalExercises: Int) async throws -> String {
        guard let uid = currentUID else { throw FirebaseServiceError.notAuthenticated }
        let ref = db.collection("workouts").document()
        let data: [String: Any] = [
            "id": ref.documentID,
            "patientId": uid,
            "planId": planId,
            "startedAt": FieldValue.serverTimestamp(),
            "completedAt": NSNull(),
            "exercisesCompleted": 0,
            "totalExercises": totalExercises,
        ]
        try await ref.setData(data)
        return ref.documentID
    }

    func completeWorkout(id: String, exercisesCompleted: Int) async throws {
        try await db.collection("workouts").document(id).updateData([
            "completedAt": FieldValue.serverTimestamp(),
            "exercisesCompleted": exercisesCompleted,
        ])
    }

    func submitFeedback(workoutId: String, painLevel: Int, difficulty: Int, notes: String?) async throws {
        let ref = db.collection("workouts").document(workoutId).collection("feedback").document()
        var data: [String: Any] = [
            "painLevel": painLevel,
            "difficulty": difficulty,
            "submittedAt": FieldValue.serverTimestamp(),
        ]
        if let notes = notes { data["notes"] = notes }
        try await ref.setData(data)
    }

    // MARK: - Appointments

    func fetchAppointments(forUser userId: String, role: String) async throws -> [(id: String, data: [String: Any])] {
        let field = role == "doctor" ? "doctorId" : "patientId"
        let snapshot = try await db.collection("appointments")
            .whereField(field, isEqualTo: userId)
            .order(by: "dateTime", descending: false)
            .getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func createAppointment(_ data: [String: Any]) async throws -> String {
        let ref = db.collection("appointments").document()
        var mutableData = data
        mutableData["id"] = ref.documentID
        mutableData["createdAt"] = FieldValue.serverTimestamp()
        try await ref.setData(mutableData)
        return ref.documentID
    }

    func updateAppointment(id: String, fields: [String: Any]) async throws {
        try await db.collection("appointments").document(id).updateData(fields)
    }

    // MARK: - Notifications

    func fetchNotifications(forUser userId: String) async throws -> [(id: String, data: [String: Any])] {
        let snapshot = try await db.collection("notifications")
            .whereField("userId", isEqualTo: userId)
            .order(by: "createdAt", descending: true)
            .limit(to: 50)
            .getDocuments()
        return snapshot.documents.map { ($0.documentID, $0.data()) }
    }

    func markNotificationRead(id: String) async throws {
        try await db.collection("notifications").document(id).updateData([
            "isRead": true
        ])
    }

    func markAllNotificationsRead(forUser userId: String) async throws {
        let snapshot = try await db.collection("notifications")
            .whereField("userId", isEqualTo: userId)
            .whereField("isRead", isEqualTo: false)
            .getDocuments()
        let batch = db.batch()
        for doc in snapshot.documents {
            batch.updateData(["isRead": true], forDocument: doc.reference)
        }
        try await batch.commit()
    }

    // MARK: - Video Upload (YouTube Data API v3 — free tier)

    /// Uploads a video to YouTube as unlisted.
    /// - Parameters:
    ///   - accessToken: OAuth2 access token with youtube.upload scope
    ///   - data: Video data bytes
    ///   - title: Video title
    ///   - description: Video description
    /// - Returns: YouTube watch URL (e.g., https://www.youtube.com/watch?v=VIDEO_ID)
    func uploadVideo(accessToken: String, data: Data, title: String, description: String = "") async throws -> String {
        guard currentUID != nil else { throw FirebaseServiceError.notAuthenticated }
        
        let videoId = try await uploadToYouTube(accessToken: accessToken, data: data, title: title, description: description, mimeType: "video/mp4")
        return "https://www.youtube.com/watch?v=\(videoId)"
    }

    /// Returns YouTube thumbnail URL from video ID.
    func youtubeThumbnailUrl(videoId: String) -> String {
        return "https://img.youtube.com/vi/\(videoId)/hqdefault.jpg"
    }

    // MARK: - YouTube Resumable Upload

    /// Uploads a video to YouTube using resumable upload protocol.
    /// Returns the YouTube video ID on success.
    private func uploadToYouTube(accessToken: String, data: Data, title: String, description: String, mimeType: String) async throws -> String {
        guard let initUrl = URL(string: youtubeUploadEndpoint) else {
            throw FirebaseServiceError.invalidData
        }

        // Step 1: Initiate resumable upload session
        let metadata: [String: Any] = [
            "snippet": [
                "title": title,
                "description": description,
                "categoryId": youtubeVideoCategoryId
            ],
            "status": [
                "privacyStatus": "unlisted"
            ]
        ]

        var initRequest = URLRequest(url: initUrl)
        initRequest.httpMethod = "POST"
        initRequest.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        initRequest.setValue("application/json; charset=UTF-8", forHTTPHeaderField: "Content-Type")
        initRequest.setValue(mimeType, forHTTPHeaderField: "X-Upload-Content-Type")
        initRequest.setValue("\(data.count)", forHTTPHeaderField: "X-Upload-Content-Length")
        initRequest.httpBody = try JSONSerialization.data(withJSONObject: metadata)

        let (_, initResponse) = try await URLSession.shared.data(for: initRequest)

        guard let httpInitResponse = initResponse as? HTTPURLResponse,
              httpInitResponse.statusCode == 200,
              let uploadUrlString = httpInitResponse.value(forHTTPHeaderField: "Location"),
              let uploadUrl = URL(string: uploadUrlString) else {
            throw FirebaseServiceError.storageError(
                NSError(domain: "YouTube", code: (initResponse as? HTTPURLResponse)?.statusCode ?? 0,
                        userInfo: [NSLocalizedDescriptionKey: "YouTube upload initialization failed"])
            )
        }

        // Step 2: Upload video bytes to resumable session URL
        var uploadRequest = URLRequest(url: uploadUrl)
        uploadRequest.httpMethod = "PUT"
        uploadRequest.setValue(mimeType, forHTTPHeaderField: "Content-Type")
        uploadRequest.setValue("\(data.count)", forHTTPHeaderField: "Content-Length")
        uploadRequest.httpBody = data

        let (responseData, uploadResponse) = try await URLSession.shared.data(for: uploadRequest)

        guard let httpUploadResponse = uploadResponse as? HTTPURLResponse, httpUploadResponse.statusCode == 200 else {
            throw FirebaseServiceError.storageError(
                NSError(domain: "YouTube", code: (uploadResponse as? HTTPURLResponse)?.statusCode ?? 0,
                        userInfo: [NSLocalizedDescriptionKey: "YouTube video upload failed"])
            )
        }

        guard let json = try? JSONSerialization.jsonObject(with: responseData) as? [String: Any],
              let videoId = json["id"] as? String else {
            throw FirebaseServiceError.invalidData
        }

        return videoId
    }
}
