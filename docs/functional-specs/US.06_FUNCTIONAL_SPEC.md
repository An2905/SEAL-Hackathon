# Functional Specification: US.06 - Assign Mentors to Tracks

This document provides the functional requirements, API contracts, and acceptance criteria for **US.06: Assign Mentors to Tracks**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Update:** Link a Mentor (Staff/Guest) to a specific Track.

### Data Dictionary

| Field Name | Data Type | Mandatory | Description                |
| :--------- | :-------- | :-------- | :------------------------- |
| `MentorID` | Integer   | Yes       | The user ID of the mentor. |
| `TrackID`  | Integer   | Yes       | The ID of the track.       |

### Business Rules

- **Access Control:** Only `Event Coordinator` can perform assignments.
- **Guidance:** Assigned mentors can view all teams within their track but cannot score them (unless they have dual roles, which is discouraged).
- **Multiple Assignments:** A mentor can be assigned to multiple tracks.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Assignment View:** A drag-and-drop interface or a dual-list selector (Available Mentors vs. Assigned Mentors) for each Track.

### API Contract (BE)

- **Endpoint:** `POST /api/staff/tracks/{trackId}/mentors`
- **Request Payload:**

  ```json
  {
    "mentorIds": [1, 5, 12]
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Successful Mentor Assignment

- **Given:** A track named "AI" and an available mentor "Dr. Smith".
- **When:** Coordinator assigns "Dr. Smith" to the "AI" track.
- **Then:** Dr. Smith gains access to the AI track dashboard and the teams within it.
