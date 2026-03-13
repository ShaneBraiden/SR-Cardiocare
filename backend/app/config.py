# config.py — Firebase + YouTube API configuration
#
# Firebase: Authentication + Firestore (free tier)
# YouTube Data API v3: Video storage (unlisted uploads — free tier)
#
# Mobile apps use GoogleService-Info.plist (iOS) and google-services.json (Android)
# for client-side Firebase configuration.

import os


class FirebaseConfig:
    """Minimal config for admin scripts."""

    SERVICE_ACCOUNT_PATH = os.environ.get(
        "FIREBASE_SERVICE_ACCOUNT",
        os.path.join(os.path.dirname(os.path.dirname(__file__)), "service-account-key.json")
    )


class YouTubeConfig:
    """YouTube Data API v3 configuration for video uploads.

    Free tier limits (per day):
      - ~6 video uploads (10,000 quota units, upload costs ~1,600 units)
      - Unlimited playback via embed/watch URLs
      - Videos uploaded as "unlisted" for privacy

    Setup:
      1. Enable YouTube Data API v3 in Google Cloud Console
      2. Create OAuth 2.0 credentials (same project as Firebase)
      3. Mobile app handles OAuth flow to get access token
    """

    # OAuth scopes required for video upload
    SCOPES = ["https://www.googleapis.com/auth/youtube.upload"]

    # Default video settings
    PRIVACY_STATUS = "unlisted"  # unlisted = accessible via link only
    CATEGORY_ID = "27"           # 27 = Education


firebase_config = FirebaseConfig()
youtube_config = YouTubeConfig()
