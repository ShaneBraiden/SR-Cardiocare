# videos.py — Cloudinary video upload helpers
#
# Videos are uploaded directly from mobile apps to Cloudinary using
# unsigned upload preset (no server proxy needed).
#
# Mobile upload URL:
#   POST https://api.cloudinary.com/v1_1/{cloud_name}/video/upload
#   Body (multipart/form-data):
#     file: <video data>
#     upload_preset: sr_cardiocare_unsigned
#     folder: sr-cardiocare/videos/{uid}
#
# This route module provides optional server-side helpers:
#   GET /videos/upload-config  — returns Cloudinary cloud_name + preset for the app
#   DELETE /videos/{public_id}  — delete a video (doctor only)

from fastapi import APIRouter
from ..config import cloudinary_config

router = APIRouter(prefix="/videos", tags=["videos"])


@router.get("/upload-config")
async def get_upload_config():
    """Return Cloudinary config needed by mobile apps for direct upload."""
    return {
        "cloud_name": cloudinary_config.CLOUD_NAME,
        "upload_preset": cloudinary_config.UPLOAD_PRESET,
        "video_folder": "sr-cardiocare/videos",
        "thumbnail_folder": "sr-cardiocare/thumbnails",
    }
