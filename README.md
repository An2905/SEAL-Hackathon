# SEAL Hackathon Management System (SEAL-HMS)

[![Backend CI](https://github.com/nmt2103/SEAL-Hackathon/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/nmt2103/SEAL-Hackathon/actions/workflows/backend-ci.yml)
[![Frontend CI](https://github.com/nmt2103/SEAL-Hackathon/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/nmt2103/SEAL-Hackathon/actions/workflows/frontend-ci.yml)

A comprehensive, production-ready monorepo platform designed to automate event coordination, team registration, mentoring, real-time communication, and academic research-based grading for the annual Software Engineering Agile League (SEAL) hackathon.

***

## 📖 Project Overview

"Software Engineering Agile League (SEAL)" is an academic hackathon organized by the Software Engineering Department at FPT University Ho Chi Minh City. **SEAL-HMS** transforms the traditional manual and fragmented event coordination process into a unified, transparent, and data-driven digital platform.

The system serves a dual purpose:

1. **Competition Management:** Seamlessly administers user profiles, event configurations, group stages, project submissions, and real-time chat.
2. **Academic Research Support:** Collects granular scoring data to enable Research-Based Learning (RBL) focusing on inter-rater reliability among judges.

***

## 🛠 Tech Stack & Tools

### Backend (API Server)

* **Framework:** Spring Boot 4.0.6 (Java 17)
* **Database:** MySQL
* **Real-Time Communication:** Spring Boot WebSockets + STOMP (with JWT channel authentication)
* **Architecture:** Direct JDBC (`Connection`, `PreparedStatement`) for maximum database control and low-level performance optimization.
* **Email Dispatch:** Brevo API integration for automated invitations and OTPs.

### Frontend (SPA Client)

* **Framework:** React 18
* **Build Engine:** Vite (with server-side API routing proxy)
* **Routing:** React Router DOM
* **Linting & Code Quality:** ESLint & Prettier quality rules

***

## 🏗 System Architecture & Design Decisions

```
[ Frontend Client: React 18 (Vite) ]
              │
              ▼ (REST APIs & STOMP WebSockets via Proxy)
[ API Gateway / Spring Security Filter (JWT Authentication) ]
              │
              ▼
    [ Spring Controllers ]
              │
              ▼
     [ Service Layer ]
              │
              ▼ (Direct JDBC Access)
  [ Spring JDBC Repositories ]
              │
              ▼
     [ MySQL Database ]
```

### Architectural Highlights:

* **Zero ORM Overhead:** Direct SQL executions using direct JDBC connections for peak query efficiency, using parameterized statements to eliminate SQL injection risks.
* **Stateless Authentication:** JWT token authentication with role-based validation (`COORDINATOR`, `EXPERT_INTERNAL`, `EXPERT_EXTERNAL`, `STUDENT_FPT`, `STUDENT_EXTERNAL`).
* **Standardized Error Handling:** High-integrity Global Exception Handler returning consistent JSON error payloads matching frontend expectation DTOs.

***

## 🚀 Getting Started & Local Setup

### Prerequisites

* **Java JDK 17** (Temurin or similar distribution)
* **Node.js** v20.x or later
* **MySQL Server** (v8.0+)
* **Git**

### 1. Database Initialization

1. Ensure your local MySQL instance is running and create a database named `hackathon`:
   ```sql
   CREATE DATABASE hackathon CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Navigate to `database/scripts/` and run `schema.sql` to generate tables, followed by `seeding.sql` to populate initial users, universities, and event details.

### 2. Environment Variables Configuration

We manage configurations via a single example template. Refer to [.env.example](file:///C:/Users/Ngo%20Minh%20Thuan/Documents/SU26/SEAL-Hackathon/.env.example) at the repository root for details.

1. **Backend:** Copy `.env.example` to `backend/.env.properties`:
   ```bash
   cp .env.example backend/.env.properties
   ```
   Fill in your local MySQL details:
   ```properties
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=hackathon
   DB_USERNAME=root
   DB_PASSWORD=your_database_password
   JWT_SECRET_KEY=your_very_secret_and_long_jwt_key_here
   ```
2. **Frontend:** Copy `.env.example` to `frontend/.env.local` to override environment configurations if needed.

### 3. Running the Applications Locally

#### Start the Spring Boot Backend:

From the `backend/` directory, use the Maven wrapper:

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts at `http://localhost:8080` (API endpoints available at `/api/*`).

#### Start the React Frontend:

From the `frontend/` directory, install dependencies and launch Vite:

```bash
cd frontend
npm install
npm run dev
```

The frontend starts at `http://localhost:5173`. Vite will proxy API and WebSocket connections to the local backend.

***

## 📁 Repository Structure

```text
SEAL-Hackathon/
├── .github/                  # GitHub Actions Workflows & Templates
│   ├── ISSUE_TEMPLATE/       # Templates for Bug & Feature issues
│   └── workflows/            # CI/CD pipelines (Backend, Frontend, Security)
├── backend/                  # Spring Boot backend application
│   ├── src/                  # Application source code & properties
│   └── pom.xml               # Maven configuration & dependencies
├── database/                 # Database initialization scripts
│   └── scripts/              # SQL schema & seeding scripts
├── docs/                     # Technical specifications & flow guides
│   ├── DEPLOY.md             # Multi-environment deployment guide
│   └── FLOW_DOCUMENTATION.md # End-to-end system sequence breakdown
└── frontend/                 # React Single Page Application (Vite)
    ├── src/                  # Components, context, router & utility code
    └── package.json          # Dependency mappings & tool scripts
```

***

## 🛠 Tooling & Quality Gates (CI/CD)

The project includes structured automation checks executed on every Pull Request:

* **Backend Quality Gate:** Validates compilation and packaging via JDK 17 on Ubuntu.
* **Frontend Quality Gate:** Executes `npm run lint` and `npm run format:check` to enforce strict formatting standards and clean JS compilation.
* **Security Scans:** Triggers dependency vulnerability audits (`npm audit` and Maven dependency scans) weekly and on PR merges.
* **PR Automations:** Includes size checks (labeling PRs from `size/XS` to `size/XL`), stale-issue closers, and auto-labelers.

***

## 🤝 Contributing Guidelines

We welcome contributions! Please review the [CONTRIBUTING.md](CONTRIBUTING.md) guide and read our [DEVELOPMENT\_RULES.md](docs\DEVELOPMENT_RULES.md) documentation prior to submitting a Pull Request.

***

## 📄 License

This repository is private and licensed for academic use within the Software Engineering Department at FPT University Ho Chi Minh City. All rights reserved.
