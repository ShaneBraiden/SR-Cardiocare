# database.py — Firebase Firestore (replaces Oracle Autonomous DB)
#
# The Oracle DB connection pool, DDL schema, and wallet configuration
# have been removed. All data is now stored in Cloud Firestore.
#
# Mobile apps access Firestore directly via Firebase SDKs.
# This module provides admin-level Firestore access for scripts.

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from firebase_admin_setup import init_firebase, get_firestore


def get_db():
    """Return Firestore client (admin access)."""
    init_firebase()
    return get_firestore()


# ── Firestore Collection Schema (reference) ────────────────────────────
#
# /users/{uid}
#   email, firstName, lastName, role, phone, profileImageUrl, createdAt
#   (doctor): speciality, licenseNumber, clinicName
#   (patient): dateOfBirth, gender, injuries[], primaryGoal, assignedDoctorId
#
# /exercises/{id}
#   name, description, category, difficultyLevel, durationSeconds,
#   videoUrl, thumbnailUrl, sets, reps, instructions, createdBy, createdAt
#
# /plans/{id}
#   patientId, doctorId, name, startDate, endDate, isActive, createdAt,
#   exercises: [{exerciseId, order, customSets, customReps, notes}]
#
# /workouts/{id}
#   patientId, planId, startedAt, completedAt,
#   exercisesCompleted, totalExercises
#   └─ /feedback/{id}: painLevel, difficulty, notes, submittedAt
#
# /appointments/{id}
#   patientId, doctorId, dateTime, durationMinutes, type, status, notes, createdAt
#
# /notifications/{id}
#   userId, title, body, type, isRead, createdAt

