# Functional Specification: US.16 - Export Anonymized Scoring Datasets

This document provides the functional requirements, API contracts, and acceptance criteria for **US.16: Export Anonymized Scoring Datasets**.

---

## 1. Functional & Business Logic

### Granular Operations

- **Export:** Generate a data dump for research.

### Data Dictionary (Anonymized)

| Original Field | Anonymized Field | Method             |
| :------------- | :--------------- | :----------------- |
| `JudgeName`    | `Judge_ID`       | Hash / Random UUID |
| `TeamName`     | `Team_ID`        | Hash / Random UUID |
| `StudentName`  | Removed          | Complete omission  |
| `Score`        | `Score`          | Preserved          |

### Business Rules

- **Privacy:** All Personally Identifiable Information (PII) must be removed.
- **Linkability:** The same Judge_ID should be used for all scores by that judge within a single export to allow for correlation analysis.
- **Consent:** (Optional) Only include data from participants who agreed to research.

---

## 2. Interface Specifications

### UI/UX Requirements

- **Research Export Panel:** Options to select Event, Round, and specific data fields (Scores, Comments, Criteria).
- **Format Selection:** CSV, JSON.

### API Contract (BE)

- **Endpoint:** `GET /api/staff/research/export?anonymize=true`
- **Auth:** Required (Event Coordinator)

---

## 3. Acceptance Criteria (BDD)

### Scenario 1: Generating Anonymized CSV

- **Given:** A hackathon with 100 teams and 20 judges.
- **When:** Coordinator selects "Anonymized Research Export".
- **Then:** The resulting file contains scores for 100 teams, but no names, emails, or student IDs are present.
