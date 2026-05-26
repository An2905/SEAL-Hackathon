# Functional Specification: US.09 - Register Team into Track

This document provides the functional requirements, API contracts, and acceptance criteria for **US.09: Register Team into Track**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Update:** Link a Team to a specific Track in an Event.

### Data Dictionary

| Field Name | Data Type | Mandatory | Description                   |
| :--------- | :-------- | :-------- | :---------------------------- |
| `TeamID`   | Integer   | Yes       | The ID of the team.           |
| `TrackID`  | Integer   | Yes       | The ID of the track selected. |

### Business Rules

- **Role Constraint:** Only the Team Leader can perform track registration.
- **Deadline:** Registration must happen before the Event's track registration deadline.
- **Exclusivity:** A team can only compete in ONE track per event.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Track Selection Page:** Cards displaying each Track's name and description.
- **Confirmation Modal:** "Are you sure you want to compete in the AI Track? This cannot be changed later."

### API Contract (BE)

- **Endpoint:** `POST /api/teams/{teamId}/track-registration`
- **Request Payload:**

  ```json
  {
    "trackId": 5
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Successful Track Registration

- **Given:** A Team Leader whose team is not yet registered in a track.
- **When:** They select the "AI" track and confirm.
- **Then:** The team is officially assigned to the AI track.
