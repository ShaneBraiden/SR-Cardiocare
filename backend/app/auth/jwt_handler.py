# jwt_handler.py — REMOVED (Firebase handles authentication)
#
# Firebase Auth replaces custom JWT token management.
# Mobile apps use Firebase Auth SDK for sign-in/sign-up.
# Firebase automatically handles:
#   - ID token creation and verification
#   - Token refresh (1 hour expiry, auto-refreshed by SDK)
#   - Session management
#
# For admin-level token verification, use:
#   from firebase_admin import auth
#   decoded = auth.verify_id_token(id_token)
#   uid = decoded['uid']
