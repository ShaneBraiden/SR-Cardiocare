// APIEndpoints.swift
// SR-Cardiocare
//
// DEPRECATED: REST API endpoints have been replaced by Firestore collections.
// This file is kept as a migration reference only.
//
// Endpoint -> Firestore Collection mapping:
//
//   /auth/login          -> Auth.auth().signIn(withEmail:password:)
//   /auth/register       -> Auth.auth().createUser(withEmail:password:)
//   /auth/refresh         -> Automatic (Firebase SDK)
//   /auth/logout          -> Auth.auth().signOut()
//
//   /users/me             -> Firestore /users/{uid}
//   /patients             -> Firestore /users where role == ""patient""
//   /patients/{id}        -> Firestore /users/{id}
//
//   /exercises            -> Firestore /exercises
//   /exercises/{id}       -> Firestore /exercises/{id}
//   /exercises?category=X -> Firestore /exercises where category == X
//
//   /plans                -> Firestore /plans
//   /plans/{id}           -> Firestore /plans/{id}
//
//   /workouts             -> Firestore /workouts
//   /workouts/{id}        -> Firestore /workouts/{id}
//   /workouts/{id}/feedback -> Firestore /workouts/{id}/feedback
//
//   /appointments         -> Firestore /appointments
//   /appointments/{id}    -> Firestore /appointments/{id}
//
//   /notifications        -> Firestore /notifications
//
//   /videos/upload        -> Firebase Storage: videos/{uid}/{filename}
//
// See FirebaseService.swift for all Firestore query implementations.

import Foundation
