# schemas.py — Data shapes for Firestore documents (reference only)
#
# These are no longer Pydantic models used by FastAPI.
# They serve as documentation for the Firestore document structure.
# The mobile apps define their own Codable/data-class models.

"""
Firestore Document Schemas
===========================

Collection: users/{uid}
{
    "email": str,
    "firstName": str,
    "lastName": str,
    "role": "patient" | "doctor" | "admin",
    "phone": str | null,
    "profileImageUrl": str | null,
    "createdAt": str (ISO 8601),

    # Doctor-only fields:
    "speciality": str | null,
    "licenseNumber": str | null,
    "clinicName": str | null,

    # Patient-only fields:
    "dateOfBirth": str | null,
    "gender": str | null,
    "injuries": [str],
    "primaryGoal": str | null,
    "assignedDoctorId": str | null
}

Collection: exercises/{id}
{
    "id": str,
    "name": str,
    "description": str,
    "category": str,
    "difficultyLevel": "beginner" | "intermediate" | "advanced",
    "durationSeconds": int,
    "videoUrl": str | null,
    "thumbnailUrl": str | null,
    "sets": int,
    "reps": int,
    "instructions": str | null,
    "createdBy": str (uid),
    "createdAt": str (ISO 8601)
}

Collection: plans/{id}
{
    "id": str,
    "patientId": str (uid),
    "doctorId": str (uid),
    "name": str,
    "startDate": str (ISO 8601),
    "endDate": str (ISO 8601) | null,
    "isActive": bool,
    "createdAt": str (ISO 8601),
    "exercises": [
        {
            "exerciseId": str,
            "order": int,
            "customSets": int | null,
            "customReps": int | null,
            "notes": str | null
        }
    ]
}

Collection: workouts/{id}
{
    "id": str,
    "patientId": str (uid),
    "planId": str,
    "startedAt": str (ISO 8601),
    "completedAt": str (ISO 8601) | null,
    "exercisesCompleted": int,
    "totalExercises": int

    Subcollection: feedback/{id}
    {
        "painLevel": int (0-10),
        "difficulty": int (1-5),
        "notes": str | null,
        "submittedAt": str (ISO 8601)
    }
}

Collection: appointments/{id}
{
    "id": str,
    "patientId": str (uid),
    "doctorId": str (uid),
    "dateTime": str (ISO 8601),
    "durationMinutes": int,
    "type": str,
    "status": "scheduled" | "confirmed" | "cancelled" | "completed",
    "notes": str | null,
    "createdAt": str (ISO 8601)
}

Collection: notifications/{id}
{
    "id": str,
    "userId": str (uid),
    "title": str,
    "body": str,
    "type": str,
    "isRead": bool,
    "createdAt": str (ISO 8601)
}
"""
