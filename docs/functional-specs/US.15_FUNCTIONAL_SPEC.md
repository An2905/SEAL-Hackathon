# Functional Specification: US.15 - Judge Variance Dashboard

This document provides the functional requirements, API contracts, and acceptance criteria for **US.15: Judge Variance Dashboard**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Read:** View statistical analysis of judging consistency.

### Data Dictionary

| Field Name          | Data Type | Description                                                     |
| :------------------ | :-------- | :-------------------------------------------------------------- |
| `JudgeID`           | Integer   | -                                                               |
| `StandardDeviation` | Float     | Measurement of how much a judge's scores vary from the average. |
| `Correlation`       | Float     | Correlation with the final ranking.                             |

### Business Rules

- **Inter-Rater Reliability:** The system calculates the variance between two or more judges who scored the same submission.
- **Outlier Detection:** Highlights judges whose scores are significantly higher or lower than the group average.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Heatmap:** Showing submissions vs. judges with color-coded score differences.
- **Judge Profile Summary:** Showing "Strictness" and "Consistency" metrics.

### API Contract (BE)

- **Endpoint:** `GET /api/staff/research/variance`
- **Response Payload:**

  ```json
  [
    { "judgeName": "Judge A", "avgVariance": 0.8, "status": "Stable" },
    { "judgeName": "Judge B", "avgVariance": 2.5, "status": "Outlier" }
  ]
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Identifying Inconsistent Judging

- **Given:** Judge A gives a 9.0 and Judge B gives a 4.0 to the same submission.
- **When:** Coordinator views the Variance Dashboard.
- **Then:** That specific submission is highlighted as "High Variance", alerting the Coordinator to review.
