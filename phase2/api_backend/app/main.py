from fastapi import FastAPI

from app.routes import building_map, emergency, guidance, users

app = FastAPI(
    title="Prototype API",
    version="0.1.0",
    description="Minimal FastAPI starter for Android clients.",
)

app.include_router(guidance.router)
app.include_router(emergency.router)
app.include_router(building_map.router)
app.include_router(users.router)


@app.get("/health", tags=["system"])
def health() -> dict:
    return {"status": "ok"}


@app.get("/", tags=["system"])
def root() -> dict:
    return {"message": "Prototype API is running"}
