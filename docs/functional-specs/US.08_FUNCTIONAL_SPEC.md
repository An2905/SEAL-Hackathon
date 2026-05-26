# Functional Specification: US.08 - Form a Team

This document provides the functional requirements, API contracts, and acceptance criteria for **US.08: Form a Team**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Initialize a new Team.
- **Update:** Add or remove members from the team.
- **Delete:** Disband the team (only if no submissions exist).

### Data Dictionary

| Field Name     | Data Type | Mandatory | Description                      |
| :------------- | :-------- | :-------- | :------------------------------- |
| `TeamName`     | String    | Yes       | Unique name for the team.        |
| `LeaderID`     | Integer   | Yes       | The user who created the team.   |
| `MemberEmails` | List      | Yes       | Emails of the members to invite. |

### Business Rules

- **Team Size:** Minimum 3 members, Maximum 5 members (including the leader).
- **Membership Constraint:** A student can only be a member of ONE active team per event.
- **Invitations:** Members must be registered and "Approved" in the system to join a team.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Creation Form:** Input for Team Name and multiple inputs for Member Emails.
- **Member List:** Display members with status (Pending/Joined).

### API Contract (BE)

- **Endpoint:** `POST /api/teams`
- **Request Payload:**

  ```json
  {
    "teamName": "Super Coders",
    "members": ["student1@fpt.edu.vn", "student2@fpt.edu.vn"]
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Successful Team Creation

- **Given:** A student "Leader L" who is not in a team.
- **When:** They create a team with 2 other valid students.
- **Then:** The team is created with 3 members total, and "Leader L" is assigned as the Team Leader.

### Scenario 2: Team Size Violation

- **Given:** A leader tries to create a team with 6 members.
- **When:** They click "Create".
- **Then:** System returns an error: "Một đội phải có từ 3 đến 5 thành viên".
