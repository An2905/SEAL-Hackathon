# Functional Specification: US.03 - Create Guest Judge Accounts

This document provides the functional requirements, API contracts, and acceptance criteria for **US.03: Create Guest Judge Accounts**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Generate a new account with the `Guest Judge` role.
- **Read:** List existing guest judge accounts.

### Data Dictionary

| Field Name     | Data Type | Mandatory | Description                           |
| :------------- | :-------- | :-------- | :------------------------------------ |
| `Email`        | String    | Yes       | Professional email of the judge.      |
| `FullName`     | String    | Yes       | Display name.                         |
| `Organization` | String    | Yes       | Industry company or institution.      |
| `Password`     | String    | Yes       | Auto-generated or set by Coordinator. |
| `Role`         | String    | Yes       | Fixed as `GUEST_JUDGE`.               |

### Business Rules

- **Access Control:** Only `Event Coordinator` can create these accounts.
- **Limited Scope:** Guest judges can only access the scoring portal for assigned tracks/rounds (US.11).
- **Temporary Nature:** Accounts may have an expiration date or be deactivated after the event.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Creation Form:** A modal or page with fields for Email, Full Name, and Organization.
- **Password Handling:** Option to "Auto-generate and email password" to the judge.
- **List View:** A simple list of judges with their current status (Active/Inactive).

### API Contract (BE)

- **Endpoint:** `POST /api/staff/judges`
- **Auth:** Required (Event Coordinator)
- **Request Payload:**

  ```json
  {
    "email": "judge@industry.com",
    "fullName": "Expert X",
    "organization": "Tech Corp",
    "sendEmail": true
  }
  ```

- **Success Response (201 Created):**

  ```json
  { "message": "Tài khoản Giám khảo khách mời đã được tạo", "status": "success" }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Coordinator Creates Guest Judge

- **Given:** An Event Coordinator is on the "Manage Judges" page.
- **When:** They enter valid details for a new judge and click "Create".
- **Then:** The account is created, and the judge receives an email with their credentials.

### Scenario 2: Duplicate Email for Judge

- **Given:** A judge with email `expert@tech.com` already exists.
- **When:** Coordinator tries to create another account with the same email.
- **Then:** System returns a conflict error: "Email này đã được sử dụng".
