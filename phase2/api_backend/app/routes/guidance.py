from typing import List, Optional
from fastapi import APIRouter
from pydantic import BaseModel
from app.models import Location
from app.models import GuidanceRequest, Step, GuidanceResponse

router = APIRouter(prefix="/guidance", tags=["guidance"])

@router.post("/route", response_model=GuidanceResponse)
def get_guidance_route(req: GuidanceRequest) -> GuidanceResponse:
    """
    Prototype simulator for THEIA guidance.

    For demo purposes, it always returns:
    - walk ahead 10 steps
    - then turn left
    - then continue to destination
    """
    steps: List[Step] = [
        Step(order=1, instruction="Walk ahead 10 steps."),
        Step(order=2, instruction="Then turn left."),
        Step(order=3, instruction="Continue straight until you reach your destination."),
    ]

    summary = (
        f"From {req.current_location} to {req.destination}, "
        f"walk ahead 10 steps, then turn left, then continue straight."
    )

    return GuidanceResponse(summary=summary, steps=steps)


@router.get("/demo", response_model=GuidanceResponse)
def demo_guidance() -> GuidanceResponse:
    """
    Simple GET endpoint you can hit in a browser or Postman
    to show the TO-BE scenario without sending a body.
    """
    req = GuidanceRequest(
        current_location="Current hallway",
        destination="Next classroom",
    )
    return get_guidance_route(req)
