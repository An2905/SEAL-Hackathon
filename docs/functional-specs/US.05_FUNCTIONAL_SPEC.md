# Functional Specification: US.05 - Define Advancement Rules

This document provides the functional requirements, API contracts, and acceptance criteria for **US.05: Define Advancement Rules**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Update:** Set rules for how teams move between rounds.

### Data Dictionary

| Field Name         | Data Type | Mandatory | Description                                     |
| :----------------- | :-------- | :-------- | :---------------------------------------------- |
| `RoundID`          | Integer   | Yes       | The round being configured.                     |
| `AdvancementType`  | Integer   | Yes       | `1`: Top N teams, `2`: Minimum Score threshold. |
| `AdvancementValue` | Float     | Yes       | The N value (e.g., 10) or Score (e.g., 7.5).    |

### Business Rules

- **Automation:** When a round is "Closed", the system automatically flags teams for the next round based on these rules.
- **Tie-breaking:** If two teams have the same score at the cutoff, both advance (or manual intervention required).

---

## 2. Interface Specifications

### UI/UX Requirements

- **Configuration Panel:** Inside the Round management view.
- **Select Dropdown:** "Advancement Criteria" (Top N, Min Score).
- **Numeric Input:** For the value.

### API Contract (BE)

- **Endpoint:** `PUT /api/staff/rounds/{roundId}/advancement-rules`
- **Request Payload:**

  ```json
  {
    "type": 1,
    "value": 15
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Setting "Top N" Rule

- **Given:** A round with 50 teams.
- **When:** Coordinator sets advancement rule to "Top 10 teams".
- **Then:** When the round results are finalized, only the top 10 ranked teams are assigned to the next round.
