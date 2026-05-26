# Functional Specification: US.01 - Student Registration

This document provides the functional requirements, API contracts, and acceptance criteria for **US.01: Student Registration**. It serves as the source of truth for implementation and quality assurance.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Submit a new registration request.
- **Read:** Verify email uniqueness.

### Data Dictionary

| Field Name   | Data Type | Mandatory   | Description                                  |
| :----------- | :-------- | :---------- | :------------------------------------------- |
| `Email`      | String    | Yes         | Unique identifier, must follow email format. |
| `Password`   | String    | Yes         | Minimum 8 characters, hashed using BCrypt.   |
| `FullName`   | String    | Yes         | User's full name (Vietnamese support).       |
| `UserType`   | Integer   | Yes         | `1`: FPT Student, `2`: External Student.     |
| `StudentID`  | String    | Yes         | Unique identifier within the school.         |
| `SchoolName` | String    | Conditional | Required only if `UserType = 2`.             |

### Business Rules

- **Duplicate Prevention:** Email must be unique.
- **Status Workflow:** New registrations default to `Status = 0` (Pending Approval).
- **Classification:** If `UserType = 1`, `SchoolName` defaults to "FPT University".

---

## 2. Interface Specifications

### UI/UX Requirements

- **Form:** Dynamic rendering for `SchoolName` based on `UserType`.
- **Validation:** Real-time feedback for email format and password strength.
- **States:** Loading spinner during submission; Success/Error toast notifications.

### API Contract (BE)

- **Endpoint:** `POST /api/auth/register`
- **Request Payload:**

  ```json
  {
    "email": "student@example.com",
    "password": "plain_password",
    "fullName": "Nguyen Van A",
    "userType": 1,
    "studentId": "SE123456",
    "schoolName": "FPT University"
  }
  ```

- **Success Response (201 Created):**

  ```json
  { "message": "Đăng ký thành công", "status": "success" }
  ```

- **Error Responses:**
  - `409 Conflict`: `{"error": "Email này đã được sử dụng"}`
  - `400 Bad Request`: `{"error": "Vui lòng nhập mã số sinh viên"}`

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Successful FPT Student Registration

- **Given:** A guest is on the registration page.
- **When:** They select "FPT Student", enter valid details, and click "Register".
- **Then:** System saves the record with `Status = 0` and redirects to the "Pending" page.

### Scenario 2: Successful External Student Registration

- **Given:** A guest is on the registration page.
- **When:** They select "External Student", provide a "School Name", and enter valid details.
- **Then:** System saves the record including the school name.

### Scenario 3: Registration with Duplicate Email

- **Given:** A user with email `test@fpt.edu.vn` already exists.
- **When:** A guest tries to register with the same email.
- **Then:** System returns `409 Conflict` and UI displays "Email này đã được sử dụng".
