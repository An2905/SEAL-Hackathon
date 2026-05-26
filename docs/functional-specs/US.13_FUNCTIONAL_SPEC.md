# Functional Specification: US.13 - Eliminate Team and Audit Log

This document provides the functional requirements, API contracts, and acceptance criteria for **US.13: Eliminate Team and Audit Log**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Update:** Change Team status to `Eliminated`.
- **Create:** Record the reason in an audit log.

### Data Dictionary

| Field Name      | Data Type | Mandatory | Description                                         |
| :-------------- | :-------- | :-------- | :-------------------------------------------------- |
| `TeamID`        | Integer   | Yes       | The team to eliminate.                              |
| `Reason`        | String    | Yes       | Explanation for the violation (e.g., "Plagiarism"). |
| `CoordinatorID` | Integer   | Yes       | Who performed the action.                           |

### Business Rules

- **Irreversibility:** Elimination is a severe action and should require double confirmation.
- **Transparency:** The reason is stored in a separate `AuditLogs` table and is visible to the Team Leader.
- **Consequence:** Eliminated teams are removed from rankings and cannot advance.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Team Management Row:** An "Eliminate" button.
- **Elimination Modal:** Requires selecting a violation category and entering a detailed reason.
- **Audit View:** A list showing all disciplinary actions taken during the event.

### API Contract (BE)

- **Endpoint:** `POST /api/staff/teams/{teamId}/eliminate`
- **Request Payload:**

  ```json
  {
    "reason": "Sử dụng mã nguồn không hợp lệ",
    "category": "PLAGIARISM"
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Coordinator Eliminates Team

- **Given:** A team "Coders" is in the rankings.
- **When:** Coordinator eliminates them for "Cheating".
- **Then:** Team status becomes `Eliminated`, they disappear from the leaderboard, and an entry is created in the audit log.
