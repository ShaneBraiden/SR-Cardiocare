# main.py — Firebase Administration Entry Point
#
# The REST API server (FastAPI) has been removed.
# Mobile apps now connect directly to Firebase using native SDKs.
#
# This file is kept as a reference and provides a simple health check
# that can verify Firebase Admin SDK connectivity.

import sys
import os

# Add parent dir to path so we can import firebase_admin_setup
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from firebase_admin_setup import init_firebase, get_firestore


def check_firebase():
    """Verify Firebase Admin SDK can connect."""
    try:
        init_firebase()
        db = get_firestore()
        # Try a simple read
        collections = [c.id for c in db.collections()]
        print("✅  Firebase connection OK")
        print(f"   Collections: {collections or '(empty — run seed_data.py)'}")
        return True
    except Exception as e:
        print(f"❌  Firebase connection failed: {e}")
        return False


if __name__ == "__main__":
    check_firebase()
