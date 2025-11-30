from typing import List, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from app.models import Location  # Imported for potential reuse/consistency

router = APIRouter(tags=["map"])


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
