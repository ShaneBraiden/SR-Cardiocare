# RehabConnect Backend — Firebase Administration

> **Architecture Change**: The backend no longer runs a REST API server.
> Mobile apps (iOS/Android) connect **directly** to Firebase using native SDKs.
> This folder contains admin utilities only.

## Architecture

```
Mobile Apps (iOS / Android)
  |  Firebase SDKs (direct connection - no server in between)
Firebase Auth  <->  Cloud Firestore  <->  Cloud Storage
```

## Files

| File | Purpose |
|------|---------|
| `firebase_admin_setup.py` | Firebase Admin SDK initialisation |
| `seed_data.py` | Seed demo data into Firestore |
| `requirements.txt` | Python dependencies |

## Quick Start

```bash
# 1  Download service-account key from Firebase Console
#    Project Settings -> Service Accounts -> Generate New Private Key
#    Save as  backend/service-account-key.json

# 2  Install
pip install -r requirements.txt

# 3  Seed demo data
python seed_data.py
```

## Previous Backend (removed)

The original backend used FastAPI + Oracle Autonomous DB + Oracle Object Storage.
All REST routes (`/auth`, `/patients`, `/exercises`, etc.) have been removed.
Authentication and data access are now handled by Firebase SDKs inside each mobile app.
