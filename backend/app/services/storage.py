# storage.py — YouTube Data API v3 Video Storage
#
# Video storage uses YouTube Data API v3 with unlisted uploads.
# This approach provides free video hosting with no storage limits.
#
# Upload approach:
#   - Mobile apps upload directly to YouTube using OAuth2 access tokens
#   - Videos are uploaded as "unlisted" (accessible only via direct link)
#   - YouTube auto-generates thumbnails
#
# YouTube URL formats:
#   Watch:     https://www.youtube.com/watch?v={videoId}
#   Embed:     https://www.youtube.com/embed/{videoId}
#   Thumbnail: https://img.youtube.com/vi/{videoId}/hqdefault.jpg
#
# YouTube quota (free tier — 10,000 units/day):
#   - Video upload: ~1,600 units each → ~6 uploads/day
#   - Playback: unlimited (no quota cost)
#
# Setup:
#   1. Enable YouTube Data API v3 in Google Cloud Console
#   2. Create OAuth 2.0 credentials (same project as Firebase)
#   3. Add youtube.upload scope to OAuth consent screen
#   4. Mobile apps handle OAuth flow via Google Sign-In
#
# iOS:     FirebaseService.uploadVideo() → YouTube resumable upload
# Android: YouTubeUploader.uploadVideo() → YouTube resumable upload


def youtube_watch_url(video_id: str) -> str:
    """Construct YouTube watch URL from video ID."""
    return f"https://www.youtube.com/watch?v={video_id}"


def youtube_embed_url(video_id: str) -> str:
    """Construct YouTube embed URL from video ID."""
    return f"https://www.youtube.com/embed/{video_id}"


def youtube_thumbnail_url(video_id: str, quality: str = "hqdefault") -> str:
    """Construct YouTube thumbnail URL from video ID.
    
    Quality options: default, mqdefault, hqdefault, sddefault, maxresdefault
    """
    return f"https://img.youtube.com/vi/{video_id}/{quality}.jpg"


def extract_video_id(url: str) -> str | None:
    """Extract YouTube video ID from various URL formats."""
    import re
    pattern = r"(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/embed/)([a-zA-Z0-9_-]{11})"
    match = re.search(pattern, url)
    return match.group(1) if match else None
