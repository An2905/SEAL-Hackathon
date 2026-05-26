# Functional Specification: US.10 - Submit Project Links

This document provides the functional requirements, API contracts, and acceptance criteria for **US.10: Submit Project Links**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Submit project URLs for a specific round.
- **Read:** View current submission.
- **Update:** Modify submission before the deadline.

### Data Dictionary

| Field Name         | Data Type | Mandatory | Description                    |
| :----------------- | :-------- | :-------- | :----------------------------- |
| `RoundID`          | Integer   | Yes       | The current competition round. |
| `GitHubURL`        | String    | Yes       | Link to the code repository.   |
| `DemoURL`          | String    | No        | Link to live demo or video.    |
| `DocumentationURL` | String    | No        | Link to project docs/slides.   |
| `SubmissionTime`   | DateTime  | Auto      | Timestamp of the request.      |

### Business Rules

- **Eligibility:** Only teams that have advanced to the current round can submit.
- **Deadline:** Submission is blocked after the Round's closing time.
- **Role Constraint:** Only the Team Leader can submit.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Submission Form:** Input fields for URLs with validation (must be valid HTTP/HTTPS links).
- **Countdown Timer:** Showing time remaining until the round deadline.

### API Contract (BE)

- **Endpoint:** `POST /api/teams/submissions`
- **Request Payload:**

  ```json
  {
    "roundId": 2,
    "githubUrl": "https://github.com/team/project",
    "demoUrl": "https://demo.com"
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: On-time Submission

- **Given:** A Team Leader in Round 1, 2 hours before the deadline.
- **When:** They submit valid GitHub and Demo links.
- **Then:** The submission is saved, and a success message is displayed.

### Scenario 2: Late Submission Attempt

- **Given:** A Team Leader in Round 1, 5 minutes AFTER the deadline.
- **When:** They try to submit.
- **Then:** System returns error: "Thời gian nộp bài đã kết thúc".
