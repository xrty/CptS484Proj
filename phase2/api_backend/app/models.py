from typing import List, Literal, Optional

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


class Contact(BaseModel):
    name: str
    phone: str
    relation: Optional[str] = None
    preferred_channel: str = "sms"


class User(BaseModel):
    id: int
    name: str
    email: Optional[str] = None
    settings: Settings = Field(default_factory=Settings)
    contacts: List[Contact] = Field(default_factory=list)


class GuidanceRequest(BaseModel):
    current_location: str
    destination: str


class Step(BaseModel):
    order: int
    instruction: str


class GuidanceResponse(BaseModel):
    summary: str
    steps: List[Step]


class Hallway(BaseModel):
    id: int
    name: str
    status: Literal["available", "under_construction"] = "available"
    description: Optional[str] = None
