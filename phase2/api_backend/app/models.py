from pydantic import BaseModel


class Location(BaseModel):
    building: str
    floor: str
    node: str  # abstract node id in the indoor map
