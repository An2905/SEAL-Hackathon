# Functional Specification: US.11 - Scoring Portal for Judges

This document provides the functional requirements, API contracts, and acceptance criteria for **US.11: Scoring Portal for Judges**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Read:** List submissions assigned to the judge.
- **Update:** Enter scores and feedback for a submission.

### Data Dictionary

| Field Name     | Data Type | Mandatory | Description                          |
| :------------- | :-------- | :-------- | :----------------------------------- |
| `SubmissionID` | Integer   | Yes       | The project being scored.            |
| `Scores`       | Object    | Yes       | Key-value pairs (CriteriaID: Score). |
| `Feedback`     | String    | No        | Qualitative comments.                |

### Business Rules

- **Range:** Scores must be between 0.0 and 10.0.
- **Weighting:** Final score is calculated as `SUM(Score * Weight)`.
- **Anonymity:** Judges should ideally see Team IDs or Project Names, not student names (configurable).
- **Finality:** Once "Submitted", a score cannot be changed (unless unlocked by Coordinator).

---

## 2. Interface Specifications

### UI/UX Requirements

- **Judging Dashboard:** List of assigned teams with status (Not Started, In Progress, Completed).
- **Scoring Interface:** Criteria sliders or numeric inputs, links to GitHub/Demo, and a text area for feedback.

### API Contract (BE)

- **Endpoint:** `POST /api/judges/scores`
- **Request Payload:**

  ```json
  {
    "submissionId": 101,
    "scores": {
      "1": 8.5,
      "2": 7.0,
      "3": 9.0
    },
    "feedback": "Excellent technical execution."
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Judge Submits Valid Score

- **Given:** A judge assigned to "Team Alpha".
- **When:** They enter scores for all criteria and click "Submit".
- **Then:** The submission status changes to "Completed" and the team's total score is updated in the database.
