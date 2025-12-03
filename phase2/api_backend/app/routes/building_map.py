from typing import List, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.models import Hallway, Location  # Imported for potential reuse/consistency

router = APIRouter(tags=["map"])

class Login(BaseModel):
    username: str
    password: str
    
MANAGER_USERNAME = "managername"
MANAGER_PASSWORD = "111111"

@router.post("/manager/login")
def manager_login(login: Login):
    if login.username == MANAGER_USERNAME and login.password == MANAGER_PASSWORD:
        return {"success": True}
    raise HTTPException(status_code=404, detail="account not found")

class MapUpdate(BaseModel):
    building: str
    version: str
    blocked_nodes: List[str] = []
    notes: Optional[str] = None


class MapStatus(BaseModel):
    building: str
    version: str
    blocked_nodes: List[str]
    notes: Optional[str] = None


_map_state: MapStatus = MapStatus(building="default", version="v1", blocked_nodes=[], notes=None)


# ----- Hallway status (prototype) -----

class HallwayUpdate(BaseModel):
    status: str  # "available" | "under_construction"


_hallways: List[Hallway] = [
    Hallway(id=1, name="North Connector", status="available"),
    Hallway(id=2, name="Atrium Passage", status="under_construction"),
    Hallway(id=3, name="South Wing Link", status="available"),
]


@router.put("/building-map", response_model=MapStatus)
def update_map(update: MapUpdate) -> MapStatus:
    global _map_state
    if update.building != _map_state.building:
        # Prototype only supports a single building profile.
        raise HTTPException(status_code=400, detail="Unknown building")

    _map_state = MapStatus(
        building=update.building,
        version=update.version,
        blocked_nodes=update.blocked_nodes,
        notes=update.notes,
    )
    return _map_state


@router.get("/building-map", response_model=MapStatus)
def get_map() -> MapStatus:
    return _map_state


@router.get("/building-map/hallways", response_model=List[Hallway])
def list_hallways() -> List[Hallway]:
    return _hallways


@router.put("/building-map/hallways/{hallway_id}", response_model=Hallway)
def update_hallway(hallway_id: int, payload: HallwayUpdate) -> Hallway:
    status_value = payload.status.lower()
    if status_value not in ("available", "under_construction"):
        raise HTTPException(status_code=400, detail="status must be 'available' or 'under_construction'")

    for idx, hallway in enumerate(_hallways):
        if hallway.id == hallway_id:
            updated = hallway.copy(update={"status": status_value})
            _hallways[idx] = updated
            return updated

    raise HTTPException(status_code=404, detail="hallway not found")
