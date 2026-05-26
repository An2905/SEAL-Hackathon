# Functional Specification: US.12 - Rankings and Result Export

This document provides the functional requirements, API contracts, and acceptance criteria for **US.12: Rankings and Result Export**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Read:** View dynamic leaderboard.
- **Export:** Download CSV/Excel reports.

### Data Dictionary (Export)

| Field Name   | Data Type | Description             |
| :----------- | :-------- | :---------------------- |
| `Rank`       | Integer   | Calculated position.    |
| `TeamName`   | String    | -                       |
| `Track`      | String    | Category.               |
| `TotalScore` | Float     | Final weighted average. |
| `Status`     | String    | Advanced/Eliminated.    |

### Business Rules

- **Ranking Logic:** Teams are sorted by `TotalScore` descending.
- **Tie-breaking:** Secondary sorting by specific criteria (e.g., "Technical Depth") if specified in Event rules.
- **Real-time:** Rankings update as soon as a judge submits a score.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Leaderboard View:** Filterable by Event, Track, and Round.
- **Export Buttons:** "Download CSV" and "Download Excel".

### API Contract (BE)

- **Endpoint:** `GET /api/staff/rankings?roundId=2&trackId=5`
- **Success Response:**

  ```json
  [
    { "rank": 1, "teamName": "A", "score": 9.5 },
    { "rank": 2, "teamName": "B", "score": 9.2 }
  ]
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Exporting Results

- **Given:** A round has ended with 20 teams scored.
- **When:** Coordinator clicks "Export CSV".
- **Then:** A file is downloaded containing all 20 teams ranked correctly with their scores.
