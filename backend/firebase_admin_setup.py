# firebase_admin_setup.py — Initialise Firebase Admin SDK
# Used for server-side operations like seeding data, bulk imports, etc.
# Note: Video storage uses YouTube Data API v3 (unlisted uploads)

import firebase_admin
from firebase_admin import credentials, firestore, auth

_app = None


def init_firebase(service_account_path: str = "service-account-key.json"):
    """Initialise Firebase Admin SDK with service account credentials."""
    global _app
    if _app is not None:
        return _app

    cred = credentials.Certificate(service_account_path)
    _app = firebase_admin.initialize_app(cred)
    return _app


def get_firestore():
    """Return Firestore client."""
    if _app is None:
        init_firebase()
    return firestore.client()


def get_auth():
    """Return Firebase Auth client."""
    if _app is None:
        init_firebase()
    return auth
