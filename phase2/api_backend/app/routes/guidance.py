from typing import List, Optional

from fastapi import APIRouter
from pydantic import BaseModel

from app.models import Location

router = APIRouter(tags=["guidance"])


class GuidanceRequest(BaseModel):
    user_id: Optional[str] = None
    current: Location
    destination: Location


class GuidanceResponse(BaseModel):
    steps: List[str]
    distance_m: Optional[float] = None


@router.post("/guidance", response_model=GuidanceResponse)
def get_guidance(payload: GuidanceRequest) -> GuidanceResponse:
    # Simplest possible route: return canned steps; in reality run a pathfinder.
    steps = [
        f"Start at {payload.current.node} on floor {payload.current.floor}",
        "Proceed forward 10 meters",
        "Turn right",
        f"Continue to {payload.destination.node} on floor {payload.destination.floor}",
        "Arrived",
    ]
    return GuidanceResponse(steps=steps, distance_m=25.0)
