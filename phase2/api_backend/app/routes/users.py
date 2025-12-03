from fastapi import APIRouter

from app.models import Contact, Settings, User

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
        contacts=[
            Contact(
                name="Alice Anderson",
                phone="+1-555-0101",
                relation="sister",
                preferred_channel="sms",
            ),
            Contact(
                name="Bob Brown",
                phone="+1-555-0202",
                relation="neighbor",
                preferred_channel="call",
            ),
            Contact(
                name="Carmen Carter",
                phone="+1-555-0303",
                relation="caregiver",
                preferred_channel="sms",
            ),
        ],
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
