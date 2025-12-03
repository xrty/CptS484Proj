from typing import Optional
from typing import List
from pydantic import BaseModel, Field


class Location(BaseModel):
    building: str
    floor: str
    node: str  # abstract node id in the indoor map


class Settings(BaseModel):
    share_location: bool = False
    share_health_data: bool = False
    notifications_enabled: bool = True
    theme: str = "system"  # system | light | dark


class User(BaseModel):
    id: int
    name: str
    email: Optional[str] = None
    settings: Settings = Field(default_factory=Settings)

class GuidanceRequest(BaseModel):
    current_location: str
    destination: str

class Step(BaseModel):
    order: int
    instruction: str

class GuidanceResponse(BaseModel):
    summary: str
    steps: List[Step]
