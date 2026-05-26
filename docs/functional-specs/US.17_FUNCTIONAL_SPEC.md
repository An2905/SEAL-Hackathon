# Functional Specification: US.17 - Assign and Announce Prizes

This document provides the functional requirements, API contracts, and acceptance criteria for **US.17: Assign and Announce Prizes**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Define Prize categories (e.g., "First Prize", "Best Innovation").
- **Update:** Link a Team to a specific Prize.
- **Read:** Announce winners on a public leaderboard.

### Data Dictionary

| Field Name  | Data Type | Mandatory | Description                                   |
| :---------- | :-------- | :-------- | :-------------------------------------------- |
| `PrizeName` | String    | Yes       | E.g., "Giải Nhất".                            |
| `Value`     | String    | No        | Prize details (e.g., "$1000", "Certificate"). |
| `TeamID`    | Integer   | Yes       | The winning team.                             |

### Business Rules

- **Eligibility:** Prizes are assigned AFTER the final round is closed and rankings are validated.
- **Limit:** A team can potentially win multiple prizes (e.g., First Prize + Best Presentation).
- **Public Visibility:** Winners are only visible to the public once the Coordinator clicks "Announce Winners".

---

## 2. Interface Specifications

### UI/UX Requirements

- **Awards Dashboard:** A list of prizes with dropdowns to select the winning team from the top-ranked participants.
- **Winner Announcement Card:** A visually distinct section on the Home Page showing winning teams and their photos/projects.

### API Contract (BE)

- **Endpoint:** `POST /api/staff/awards`
- **Request Payload:**

  ```json
  {
    "prizeId": 1,
    "teamId": 25,
    "isPublic": true
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Assigning a Prize

- **Given:** The Final Round has ended and rankings are finalized.
- **When:** Coordinator assigns "First Prize" to the Rank 1 team.
- **Then:** The team is notified via the app/email, and their profile displays the "First Prize" badge.
