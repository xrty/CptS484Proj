from fastapi import APIRouter

from app.models import Settings, User

router = APIRouter(tags=["users"])


def create_user_dummy_logic() -> User:
    """
    Stub user factory that returns a dummy user with default settings.
    Replace with real persistence later.
    """
    return User(
        id=1,
        name="Test User",
        email="test@example.com",
        settings=Settings(),
    )


# Initialized at import so other endpoints can reference an existing user.
default_user: User = create_user_dummy_logic()


def user_payload(user: User | None = None) -> dict:
    obj = user or default_user
    return {"user": obj.model_dump()}


@router.post("/users/create")
def create_user_dummy() -> dict:
    # returns the already-initialized dummy user for now
    return user_payload()
