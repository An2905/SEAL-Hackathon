# Functional Specification: US.14 - Calibration Round for Judges

This document provides the functional requirements, API contracts, and acceptance criteria for **US.14: Calibration Round for Judges**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Read:** Access a "Practice" submission.
- **Update:** Submit a calibration score.
- **Read:** View distribution of scores from other judges (Anonymized).

### Data Dictionary

| Field Name           | Data Type | Description                                  |
| :------------------- | :-------- | :------------------------------------------- |
| `SampleSubmissionID` | Integer   | A fixed project used for training.           |
| `JudgeScore`         | Float     | The score given by the current judge.        |
| `MeanScore`          | Float     | Average score of all judges for this sample. |

### Business Rules

- **Non-impact:** Calibration scores do NOT affect the actual team rankings.
- **Visibility:** A judge can only see the score distribution AFTER they have submitted their own score.
- **Goal:** To identify "Hawks" (strict judges) and "Doves" (lenient judges) before the real competition.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Calibration Tab:** Separate from the main judging portal.
- **Histogram/Chart:** Showing how many judges gave which scores (e.g., 5.0, 6.0, 7.0).

### API Contract (BE)

- **Endpoint:** `GET /api/judges/calibration/{sampleId}/results`
- **Response Payload:**

  ```json
  {
    "yourScore": 7.5,
    "average": 7.2,
    "distribution": { "6.0": 1, "7.0": 5, "8.0": 2 }
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Judge views calibration results

- **Given:** A judge has submitted a score of 8.0 for a sample project.
- **When:** They view the results page.
- **Then:** They see a chart showing that most other judges scored it between 7.0 and 7.5.
