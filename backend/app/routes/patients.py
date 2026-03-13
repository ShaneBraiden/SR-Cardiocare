# patients.py - REMOVED (Firestore /users collection with role=patient)
#
# Patient data lives in /users/{uid} where role == "patient"
# Doctor queries patients assigned to them:
#   .whereField("assignedDoctorId", isEqualTo: doctorUid)
#   .whereField("role", isEqualTo: "patient")
