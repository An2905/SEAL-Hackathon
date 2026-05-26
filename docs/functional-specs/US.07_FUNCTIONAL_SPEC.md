# Functional Specification: US.07 - Manage Scoring Criteria Templates

This document provides the functional requirements, API contracts, and acceptance criteria for **US.07: Manage Scoring Criteria Templates**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Create:** Define a new criteria template (e.g., "Standard Round Scoring").
- **Update:** Apply a template to a specific round.
- **Read:** List available templates.

### Data Dictionary

| Field Name     | Data Type | Mandatory | Description                                |
| :------------- | :-------- | :-------- | :----------------------------------------- |
| `TemplateName` | String    | Yes       | Name of the template.                      |
| `CriteriaList` | List      | Yes       | Objects containing `Label` and `Weight`.   |
| `Label`        | String    | Yes       | E.g., "Innovation", "Technical Depth".     |
| `Weight`       | Integer   | Yes       | Percentage (must total 100% per template). |

### Business Rules

- **Access Control:** Only `Event Coordinator`.
- **Validation:** Total weight of all criteria in a template must exactly equal 100.
- **Immutability:** Once a template is applied to a round and scoring has started, it cannot be modified for that round.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Template Builder:** A dynamic list where users can add criteria rows, enter labels, and adjust weights.
- **Real-time Sum:** Display the total weight as the user types (show warning if not 100).

### API Contract (BE)

- **Endpoint:** `POST /api/staff/scoring-templates`
- **Request Payload:**

  ```json
  {
    "name": "Final Round Format",
    "criteria": [
      { "label": "Innovation", "weight": 40 },
      { "label": "Pitch", "weight": 30 },
      { "label": "Impact", "weight": 30 }
    ]
  }
  ```

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Creating Valid Template

- **Given:** Coordinator enters 3 criteria with weights 40, 30, and 30.
- **When:** They click "Save Template".
- **Then:** The template is saved and becomes available for selection in Rounds.

### Scenario 2: Invalid Weight Sum

- **Given:** Coordinator enters criteria totaling 110%.
- **When:** They try to save.
- **Then:** System displays error: "Tổng trọng số phải bằng 100%".
