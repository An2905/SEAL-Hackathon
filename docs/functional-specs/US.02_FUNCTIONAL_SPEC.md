# Functional Specification: US.02 - Review and Approve Registrations

This document provides the functional requirements, API contracts, and acceptance criteria for **US.02: Review and Approve Registrations**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Read:** List all pending registration requests.
- **Update:** Approve or Reject a specific user registration.

### Data Dictionary (Update Fields)

| Field Name | Data Type | Mandatory | Description                                  |
| :--------- | :-------- | :-------- | :------------------------------------------- |
| `UserID`   | Integer   | Yes       | The ID of the user to be reviewed.           |
| `Status`   | Integer   | Yes       | `1` for Approved (Active), `2` for Rejected. |
| `Reason`   | String    | No        | Optional message for rejection.              |

### Business Rules

- **Access Control:** Only users with the `Event Coordinator` role can access this functionality.
- **Mutual Exclusivity:** A user cannot be both Approved and Rejected at the same time.
- **Notification:** (Optional) System could send an email via Brevo upon approval/rejection.
- **Persistence:** Once approved, the user's `Status` in the `Users` table changes from `0` to `1`.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Dashboard View:** A table showing pending users with columns: Name, Email, Student ID, School, and Actions.
- **Action Buttons:** "Approve" (Green) and "Reject" (Red) buttons for each row.
- **Rejection Modal:** If "Reject" is clicked, a modal appears to enter an optional reason.

### API Contract (BE)

- **Endpoint:** `PATCH /api/staff/registrations/{userId}/status`
- **Auth:** Required (Event Coordinator)
- **Request Payload:**

  ```json
  {
    "status": 1,
    "reason": ""
  }
  ```

- **Success Response (200 OK):**

  ```json
  { "message": "Trạng thái người dùng đã được cập nhật", "status": "success" }
  ```

- **Error Responses:**
  - `403 Forbidden`: If the user is not an Event Coordinator.
  - `404 Not Found`: If `userId` does not exist.

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Coordinator Approves a Student

- **Given:** An Event Coordinator is logged in and viewing the pending registrations list.
- **When:** they click "Approve" for a student named "Nguyen Van A".
- **Then:** The student's status changes to "Active", and they are removed from the pending list.

### Scenario 2: Coordinator Rejects a Student

- **Given:** An Event Coordinator is logged in.
- **When:** they click "Reject" and provide a reason "Hồ sơ không hợp lệ".
- **Then:** The student's status changes to "Rejected", and they cannot log in to the system.
