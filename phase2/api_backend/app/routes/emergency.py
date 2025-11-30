from typing import List, Optional

from fastapi import APIRouter, HTTPException, status
from pydantic import BaseModel

from app.models import Location

router = APIRouter(tags=["emergency"])


class EmergencyEvent(BaseModel):
    user_id: Optional[str] = None
    location: Location
    fall_detected: bool = True
    sensor_confidence: Optional[float] = None  # 0-1


class EmergencyAck(BaseModel):
    notified_contacts: List[str]
    ticket_id: str


# Prototype log; swap for persistence later.
_emergency_log: List[EmergencyAck] = []


@router.post("/emergency", response_model=EmergencyAck, status_code=status.HTTP_202_ACCEPTED)
def trigger_emergency(event: EmergencyEvent) -> EmergencyAck:
    if not event.fall_detected:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="No emergency detected")

    # Stub contact notification list; plug in SMS/Push later.
    contacts = ["primary_caregiver", "facility_security"]
    ack = EmergencyAck(notified_contacts=contacts, ticket_id=f"case-{len(_emergency_log) + 1}")
    _emergency_log.append(ack)
    return ack
