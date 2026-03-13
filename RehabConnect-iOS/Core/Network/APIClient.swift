// APIClient.swift
// SR-Cardiocare
//
// DEPRECATED: This file is kept for reference only.
// All API calls now go through FirebaseService.swift which connects
// directly to Firebase (Auth + Firestore + Cloud Storage).
//
// The URLSession-based REST client, JWT handling, and token refresh
// logic have been replaced by Firebase SDKs.
//
// Migration mapping:
//   APIClient.get()    -> FirebaseService.shared.fetch*()
//   APIClient.post()   -> FirebaseService.shared.create*()
//   APIClient.put()    -> FirebaseService.shared.update*()
//   APIClient.delete() -> Firestore .delete()
//   uploadData()       -> FirebaseService.shared.uploadVideo()
//
// See FirebaseService.swift for the complete replacement.

import Foundation

// MARK: - API Error (kept for backward compatibility)

enum APIError: Error, LocalizedError {
    case invalidURL
    case noData
    case decodingFailed(Error)
    case encodingFailed(Error)
    case unauthorized
    case serverError(Int, String?)
    case networkError(Error)
    case tokenRefreshFailed

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return NSLocalizedString(""error_invalid_url"", comment: """")
        case .noData:
            return NSLocalizedString(""error_no_data"", comment: """")
        case .decodingFailed(let error):
            return ""Decoding failed: \(error.localizedDescription)""
        case .encodingFailed(let error):
            return ""Encoding failed: \(error.localizedDescription)""
        case .unauthorized:
            return NSLocalizedString(""error_unauthorized"", comment: """")
        case .serverError(let code, let message):
            return ""Server error \(code): \(message ?? ""Unknown"")""
        case .networkError(let error):
            return error.localizedDescription
        case .tokenRefreshFailed:
            return NSLocalizedString(""error_token_refresh_failed"", comment: """")
        }
    }
}
