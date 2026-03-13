# seed_data.py — Populate Firestore with demo data for RehabConnect
#
# Usage:
#   1. Place your Firebase service-account-key.json in this folder
#   2. pip install -r requirements.txt
#   3. python seed_data.py

from datetime import datetime, timedelta
from firebase_admin_setup import init_firebase, get_firestore, get_auth

# ── Bootstrap ──────────────────────────────────────────────────────────

init_firebase()
db = get_firestore()
fa = get_auth()

print("🔥  Connected to Firebase — seeding demo data …")


# ── Helper ─────────────────────────────────────────────────────────────

def iso(dt: datetime) -> str:
    return dt.isoformat() + "Z"


NOW = datetime.utcnow()


# ── 1. Users ───────────────────────────────────────────────────────────

def seed_users():
    """Create demo users in Firebase Auth + Firestore /users collection."""

    users = [
        {
            "email": "Admin@sr-cardiocare.com",
            "password": "password@123",
            "display_name": "Admin",
            "role": "admin",
            "first_name": "Admin",
            "last_name": "SR-Cardiocare",
            "phone": None,
        },
        {
            "email": "dr.smith@rehabconnect.app",
            "password": "Doctor123!",
            "display_name": "Dr. Smith",
            "role": "doctor",
            "first_name": "John",
            "last_name": "Smith",
            "phone": "+1-555-0100",
            "speciality": "Sports Physiotherapy",
            "license_number": "PT-2024-001",
            "clinic_name": "RehabConnect Clinic",
        },
        {
            "email": "sarah@rehabconnect.app",
            "password": "Patient123!",
            "display_name": "Sarah Wilson",
            "role": "patient",
            "first_name": "Sarah",
            "last_name": "Wilson",
            "phone": "+1-555-0201",
            "date_of_birth": "1995-03-15",
            "gender": "female",
            "injuries": ["ACL Tear — Left Knee"],
            "primary_goal": "Return to running",
        },
        {
            "email": "mike@rehabconnect.app",
            "password": "Patient123!",
            "display_name": "Mike Johnson",
            "role": "patient",
            "first_name": "Mike",
            "last_name": "Johnson",
            "phone": "+1-555-0202",
            "date_of_birth": "1988-07-22",
            "gender": "male",
            "injuries": ["Rotator Cuff — Right Shoulder"],
            "primary_goal": "Regain full ROM",
        },
        {
            "email": "emma@rehabconnect.app",
            "password": "Patient123!",
            "display_name": "Emma Davis",
            "role": "patient",
            "first_name": "Emma",
            "last_name": "Davis",
            "phone": "+1-555-0203",
            "date_of_birth": "2000-11-08",
            "gender": "female",
            "injuries": ["Lower Back Pain"],
            "primary_goal": "Pain-free daily activities",
        },
    ]

    created_uids = {}

    for u in users:
        try:
            # Create in Firebase Auth
            user_record = fa.create_user(
                email=u["email"],
                password=u["password"],
                display_name=u["display_name"],
            )
            uid = user_record.uid
        except fa.EmailAlreadyExistsError:
            user_record = fa.get_user_by_email(u["email"])
            uid = user_record.uid
            print(f"  ⚠  User {u['email']} already exists (uid={uid})")

        # Write user doc to Firestore
        user_doc = {
            "email": u["email"],
            "firstName": u["first_name"],
            "lastName": u["last_name"],
            "role": u["role"],
            "phone": u.get("phone"),
            "profileImageUrl": None,
            "createdAt": iso(NOW),
        }

        # Role-specific fields
        if u["role"] == "doctor":
            user_doc["speciality"] = u.get("speciality")
            user_doc["licenseNumber"] = u.get("license_number")
            user_doc["clinicName"] = u.get("clinic_name")
        elif u["role"] == "patient":
            user_doc["dateOfBirth"] = u.get("date_of_birth")
            user_doc["gender"] = u.get("gender")
            user_doc["injuries"] = u.get("injuries", [])
            user_doc["primaryGoal"] = u.get("primary_goal")
            user_doc["assignedDoctorId"] = None  # Will be set below

        db.collection("users").document(uid).set(user_doc)
        created_uids[u["email"]] = uid
        print(f"  ✔  {u['role'].capitalize()} {u['display_name']} → {uid}")

    # Assign patients to doctor
    doctor_uid = created_uids["dr.smith@rehabconnect.app"]
    for email, uid in created_uids.items():
        if email not in ("dr.smith@rehabconnect.app", "Admin@sr-cardiocare.com"):
            db.collection("users").document(uid).update({"assignedDoctorId": doctor_uid})

    return created_uids


# ── 2. Exercises ───────────────────────────────────────────────────────

def seed_exercises(doctor_uid: str) -> list[str]:
    exercises = [
        {
            "name": "Wall Slides",
            "description": "Stand with back against wall, slowly slide down to 45° knee bend.",
            "category": "Knee",
            "difficultyLevel": "beginner",
            "durationSeconds": 120,
            "sets": 3,
            "reps": 12,
            "instructions": "Keep back flat against wall. Lower slowly for 3 seconds, hold 2 seconds, rise for 3 seconds.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
        {
            "name": "Straight Leg Raise",
            "description": "Lying on back, raise straight leg to 45 degrees and hold.",
            "category": "Knee",
            "difficultyLevel": "beginner",
            "durationSeconds": 90,
            "sets": 3,
            "reps": 10,
            "instructions": "Keep knee locked. Raise leg slowly over 2 seconds, hold 5 seconds at top.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
        {
            "name": "Hamstring Curl",
            "description": "Standing hamstring curl with resistance band.",
            "category": "Knee",
            "difficultyLevel": "intermediate",
            "durationSeconds": 120,
            "sets": 3,
            "reps": 15,
            "instructions": "Anchor band low. Flex knee to 90°. Control the eccentric phase.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
        {
            "name": "Shoulder External Rotation",
            "description": "External rotation with resistance band at 90° abduction.",
            "category": "Shoulder",
            "difficultyLevel": "intermediate",
            "durationSeconds": 90,
            "sets": 3,
            "reps": 12,
            "instructions": "Elbow stays at side. Rotate forearm outward slowly.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
        {
            "name": "Cat-Cow Stretch",
            "description": "Alternate between arching and rounding the spine on all fours.",
            "category": "Back",
            "difficultyLevel": "beginner",
            "durationSeconds": 60,
            "sets": 3,
            "reps": 10,
            "instructions": "Inhale to arch, exhale to round. Move slowly and deliberately.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
        {
            "name": "Bird Dog",
            "description": "Extend opposite arm and leg from all-fours position.",
            "category": "Back",
            "difficultyLevel": "intermediate",
            "durationSeconds": 120,
            "sets": 3,
            "reps": 10,
            "instructions": "Keep hips level. Hold extended position for 3 seconds each side.",
            "createdBy": doctor_uid,
            "createdAt": iso(NOW),
        },
    ]

    exercise_ids = []
    for ex in exercises:
        ref = db.collection("exercises").document()
        ex["id"] = ref.id
        ref.set(ex)
        exercise_ids.append(ref.id)
        print(f"  ✔  Exercise: {ex['name']}")

    return exercise_ids


# ── 3. Plans ───────────────────────────────────────────────────────────

def seed_plans(doctor_uid: str, patient_uid: str, exercise_ids: list[str]):
    plan_ref = db.collection("plans").document()
    plan = {
        "id": plan_ref.id,
        "patientId": patient_uid,
        "doctorId": doctor_uid,
        "name": "ACL Recovery — Phase 1",
        "startDate": iso(NOW - timedelta(days=21)),
        "endDate": iso(NOW + timedelta(days=42)),
        "isActive": True,
        "createdAt": iso(NOW - timedelta(days=21)),
        "exercises": [
            {"exerciseId": exercise_ids[0], "order": 1, "customSets": 3, "customReps": 12, "notes": "Focus on form"},
            {"exerciseId": exercise_ids[1], "order": 2, "customSets": 3, "customReps": 10, "notes": None},
            {"exerciseId": exercise_ids[2], "order": 3, "customSets": 3, "customReps": 15, "notes": "Use light band"},
            {"exerciseId": exercise_ids[4], "order": 4, "customSets": 3, "customReps": 10, "notes": "Warm-up exercise"},
            {"exerciseId": exercise_ids[5], "order": 5, "customSets": 3, "customReps": 10, "notes": "Core stability"},
        ],
    }
    plan_ref.set(plan)
    print(f"  ✔  Plan: {plan['name']} ({plan_ref.id})")
    return plan_ref.id


# ── 4. Workouts ────────────────────────────────────────────────────────

def seed_workouts(patient_uid: str, plan_id: str):
    for i in range(3):
        dt = NOW - timedelta(days=3 - i)
        session_ref = db.collection("workouts").document()
        completed = i < 2  # 2 completed, 1 in progress
        session = {
            "id": session_ref.id,
            "patientId": patient_uid,
            "planId": plan_id,
            "startedAt": iso(dt),
            "completedAt": iso(dt + timedelta(minutes=25)) if completed else None,
            "exercisesCompleted": 5 if completed else 3,
            "totalExercises": 5,
        }
        session_ref.set(session)

        if completed:
            fb_ref = db.collection("workouts").document(session_ref.id).collection("feedback").document()
            fb_ref.set({
                "painLevel": 3 if i == 0 else 2,
                "difficulty": 4 if i == 0 else 3,
                "notes": "Felt good, slight tightness in knee" if i == 0 else "Easier today",
                "submittedAt": iso(dt + timedelta(minutes=26)),
            })

        status = "completed" if completed else "in-progress"
        print(f"  ✔  Workout session {i+1}: {status}")


# ── 5. Appointments ───────────────────────────────────────────────────

def seed_appointments(doctor_uid: str, patient_uids: dict):
    appts = [
        {"patient": "sarah@rehabconnect.app", "offset_days": 1, "hour": 9, "type": "Follow-up", "status": "confirmed", "duration": 30},
        {"patient": "mike@rehabconnect.app", "offset_days": 1, "hour": 10, "type": "Initial Assessment", "status": "scheduled", "duration": 45},
        {"patient": "emma@rehabconnect.app", "offset_days": 2, "hour": 14, "type": "Progress Review", "status": "scheduled", "duration": 30},
        {"patient": "sarah@rehabconnect.app", "offset_days": 3, "hour": 11, "type": "Exercise Review", "status": "scheduled", "duration": 30},
        {"patient": "sarah@rehabconnect.app", "offset_days": -2, "hour": 9, "type": "Check-up", "status": "completed", "duration": 30},
        {"patient": "mike@rehabconnect.app", "offset_days": -5, "hour": 15, "type": "Initial Consult", "status": "completed", "duration": 60},
    ]

    for a in appts:
        dt = NOW.replace(hour=a["hour"], minute=0, second=0) + timedelta(days=a["offset_days"])
        ref = db.collection("appointments").document()
        ref.set({
            "id": ref.id,
            "patientId": patient_uids[a["patient"]],
            "doctorId": doctor_uid,
            "dateTime": iso(dt),
            "durationMinutes": a["duration"],
            "type": a["type"],
            "status": a["status"],
            "notes": None,
            "createdAt": iso(NOW),
        })
        print(f"  ✔  Appointment: {a['type']} ({a['status']})")


# ── 6. Notifications ──────────────────────────────────────────────────

def seed_notifications(patient_uid: str):
    notifs = [
        {"title": "Workout Reminder", "body": "Time for your afternoon exercises!", "type": "reminder"},
        {"title": "New Plan Assigned", "body": "Dr. Smith has updated your exercise plan.", "type": "plan"},
        {"title": "Great Progress!", "body": "You've completed 3 workouts this week.", "type": "achievement"},
        {"title": "Appointment Tomorrow", "body": "Follow-up with Dr. Smith at 9:00 AM.", "type": "appointment"},
    ]

    for i, n in enumerate(notifs):
        ref = db.collection("notifications").document()
        ref.set({
            "id": ref.id,
            "userId": patient_uid,
            "title": n["title"],
            "body": n["body"],
            "type": n["type"],
            "isRead": i >= 2,  # First 2 unread
            "createdAt": iso(NOW - timedelta(hours=i * 3)),
        })
        print(f"  ✔  Notification: {n['title']}")


# ── Run ────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    print("\n── Seeding Users ─────────────────────")
    uids = seed_users()

    doctor_uid = uids["dr.smith@rehabconnect.app"]
    sarah_uid = uids["sarah@rehabconnect.app"]

    print("\n── Seeding Exercises ─────────────────")
    exercise_ids = seed_exercises(doctor_uid)

    print("\n── Seeding Plans ────────────────────")
    plan_id = seed_plans(doctor_uid, sarah_uid, exercise_ids)

    print("\n── Seeding Workouts ─────────────────")
    seed_workouts(sarah_uid, plan_id)

    print("\n── Seeding Appointments ─────────────")
    seed_appointments(doctor_uid, uids)

    print("\n── Seeding Notifications ────────────")
    seed_notifications(sarah_uid)

    print("\n✅  Demo data seeded successfully!")
    print(f"   Admin login:   Admin@sr-cardiocare.com   / password@123")
    print(f"   Doctor login:  dr.smith@rehabconnect.app / Doctor123!")
    print(f"   Patient login: sarah@rehabconnect.app    / Patient123!")
