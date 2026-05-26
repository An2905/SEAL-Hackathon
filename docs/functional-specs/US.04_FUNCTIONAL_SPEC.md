# Functional Specification: US.04 - Create Hackathon Event

This document provides the functional requirements, API contracts, and acceptance criteria for **US.04: Create Hackathon Event**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Initialize a new Event with Tracks and Rounds.
- **Read:** View Event details.
- **Update:** Modify Event configuration.

### Data Dictionary

| Field Name    | Data Type | Mandatory | Description                                              |
| :------------ | :-------- | :-------- | :------------------------------------------------------- |
| `EventName`   | String    | Yes       | Name of the hackathon (e.g., "SEAL Hackathon 2026").     |
| `Description` | String    | No        | General info about the event.                            |
| `StartDate`   | DateTime  | Yes       | When registration/competition begins.                    |
| `Tracks`      | List      | Yes       | Categories (e.g., AI, Blockchain, Web3).                 |
| `Rounds`      | List      | Yes       | Sequential stages (e.g., Qualifying, Semi-final, Final). |

### Business Rules

- **Hierarchical Constraint:** An Event must have at least one Track and one Round.
- **Temporal Constraint:** Round dates must be within the Event's date range and must be sequential.
- **Uniqueness:** Event names should ideally be unique within a year.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Wizard-style Form:**
  1. Basic Info (Name, Dates).
  2. Tracks Management (Add/Remove labels).
  3. Rounds Configuration (Name, Dates, Order).

### API Contract (BE)

- **Endpoint:** `POST /api/staff/events`
- **Request Payload:**

  ```json
  {
    "name": "SEAL 2026",
    "description": "Annual Hackathon",
    "tracks": ["AI", "Fintech"],
    "rounds": [
      { "name": "Preliminary", "order": 1 },
      { "name": "Final", "order": 2 }
    ]
  }
  ```

- **Success Response (201 Created):**

  ```json
  { "message": "Sự kiện đã được tạo thành công", "eventId": 123 }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Coordinator Creates Valid Event

- **Given:** An Event Coordinator is on the "Create Event" page.
- **When:** They fill in valid details for Name, Tracks, and Rounds.
- **Then:** The Event is saved and appears in the Event Management dashboard.
