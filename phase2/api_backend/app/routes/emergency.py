from fastapi import APIRouter
from pydantic import BaseModel

from app.routes.users import default_user
from app.services.notification import send_notifications_to_all_contacts

router = APIRouter(tags=["emergency"])


class FallAlert(BaseModel):
    user_id: int
    latitude: float
    longitude: float
    source: str = "android_app"


@router.get("/ping")
def ping() -> dict:
    return {"message": "pong"}




@router.post("/alerts/fall")
def receive_fall(alert: FallAlert) -> dict:
    """
    The single "real" endpoint: logs and acknowledges a fall alert.
    Later you can plug in DB lookups + notifications.
    """
    print("=== FALL ALERT RECEIVED ===")
    print(f"User ID: {alert.user_id}")
    print(f"Location: ({alert.latitude}, {alert.longitude})")
    print(f"Source: {alert.source}")
    print("===========================")

    notification_results = send_notifications_to_all_contacts(default_user)

    return {
        "status": "ok",
        "message": "Fall alert received. Contacts notified.",
        "notifications": notification_results,
    }
