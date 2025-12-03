from app.models import Contact, User


def get_contacts(user: User) -> list[Contact]:
    """
    Return the contact list for a given user.
    """
    return list(user.contacts)


def send_notification(contact: Contact) -> dict:
    """
    Dummy notification sender
    """
    print(
        f"Sending {contact.preferred_channel} notification to {contact.name} at {contact.phone}"
    )
    return {
        "contact": contact.name,
        "channel": contact.preferred_channel,
        "status": "sent",
    }


def send_notifications_to_all_contacts(user: User) -> list[dict]:
    """
    Controller that iterates through contacts and dispatches notifications.
    """
    contacts = get_contacts(user)
    return [send_notification(contact) for contact in contacts]
