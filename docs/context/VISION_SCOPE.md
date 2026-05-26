# Vision and Scope: SEAL Hackathon Management System

## 1. Business Requirements

The SEAL Hackathon Management System (SEAL-HMS) aims to transform the current manual and fragmented academic competition process into a unified, transparent, and data-driven digital platform.

### 1.1 Vision Statement

For the Software Engineering Department at FPT University, the SEAL-HMS is a management and research platform that automates the hackathon lifecycle. Unlike the current manual Excel-based process, this system ensures data integrity, provides transparent audit trails, and supports academic research into evaluation consistency.

### 1.2 Business Objectives

- **Automation:** Reduce the administrative overhead of managing three annual hackathons by 70% through automated registration, team formation, and round advancement.
- **Data Integrity:** Eliminate manual data entry errors in scoring and ranking by providing a direct digital interface for judges.
- **Transparency:** Establish a 100% audit trail for all scoring decisions and team eliminations to ensure fairness and contestability.
- **Research Support:** Enable Research-Based Learning (RBL) by collecting granular, non-aggregated scoring data to analyze inter-rater reliability among judges.

### 1.3 Success Metrics

- Reduction in time from "Competition End" to "Official Ranking Announcement".
- Zero discrepancy between judge inputs and final ranking outputs.
- Completion of a research-ready anonymized dataset after each event.

## 2. Project Scope

The scope of this phase focuses on the core engine required to run a multi-track, multi-round hackathon event.

### 2.1 In-Scope Features

- **User & Identity Management:** JWT-based authentication with a manual approval workflow for internal (FPT) and external students.
- **Event Orchestration:** Dynamic creation of hackathons with multiple tracks (categories) and rounds (stages), including configurable advancement rules (e.g., Top N).
- **Scoring Engine:** Template-based criteria management and individual judge assessment portals.
- **Team & Submission Management:** Self-service team formation (3-5 members) and multi-format submission (URLs/Metadata).
- **Ranking & Results:** Automated ranking calculations based on weighted criteria and exportable reports (CSV/Excel).
- **Research Module (RBL):** Judge calibration tools, variance dashboards, and anonymized data export.

### 2.2 Out-of-Scope (Future Phases)

- Automated certificate generation.
- In-platform real-time chat/communication.
- Advanced Git API integration (beyond basic metadata fetch).
- Sponsor management and branding modules.

## 3. Limitations

- **Manual Verification:** Verification of external student status remains a manual process for organizers based on provided IDs.
- **Dependency on Coordinator:** All accounts require manual approval by an Event Coordinator before participation is enabled.
- **Fixed Team Size:** The system strictly enforces a team size of 3-5 members as per current regulation.
- **Guest Judge Restrictions:** Guest judges are limited to scoring specific assigned rounds and do not have broader system access.
- **Technological Constraint:** The system is designed for a specific multi-round structure and may require refactoring for drastically different competition formats.
