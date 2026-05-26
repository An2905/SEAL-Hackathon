# SEAL Hackathon Management System (SEAL-HMS)

> A comprehensive Hackathon Management System designed to facilitate event coordination, student participation, and judging for the Software Engineering Agile League (SEAL).

## 📖 Overview

"Software Engineering Agile League (SEAL)" is an annual academic hackathon organized by the Software Engineering Department in collaboration with PDP at FPT University Ho Chi Minh City. SEAL-HMS transforms the traditional manual and fragmented event management process into a unified, transparent, and data-driven digital platform.

The system serves a dual purpose: a robust competition management platform and a data collection tool for Research-Based Learning (RBL) focused on inter-rater reliability in software engineering evaluation.

## 🎯 Vision & Business Objectives

- **Automation:** Reduce the administrative overhead of managing annual hackathons by 70% through automated registration, team formation, and round advancement.
- **Data Integrity:** Eliminate manual data entry errors in scoring and ranking by providing a direct digital interface for judges.
- **Transparency:** Establish a 100% audit trail for all scoring decisions and team eliminations to ensure fairness and contestability.
- **Research Support:** Enable Research-Based Learning (RBL) by collecting granular, non-aggregated scoring data to analyze inter-rater reliability among judges.

## ✨ Key Features

| Module                           | Features                                                                                                                                            |
| :------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------- |
| **User & Identity Management**   | JWT-based authentication, manual approval workflow for internal/external students, and temporary Guest Judge accounts.                              |
| **Event & Track Management**     | Dynamic creation of multi-track, multi-round events, customizable scoring criteria templates, and configurable advancement rules (e.g., Top N).     |
| **Team & Submission Management** | Self-service team formation (3-5 members), Track registration, and multi-format submissions (URLs/repository links).                                |
| **Assessment & Ranking**         | Dedicated scoring portal for judges, automated ranking calculations based on weighted criteria, real-time leaderboards, and elimination audit logs. |
| **Research Module (RBL)**        | Judge calibration tools, score variance dashboards, and exportable anonymized scoring datasets for academic research.                               |

## 🛠 Tech Stack

### Backend

- **Framework:** Spring Boot 4.0.6
- **Language:** Java 17
- **Database:** SQL Server (Microsoft SQL Server)
- **Authentication:** JWT (JSON Web Token)
- **Email Service:** Brevo (formerly Sendinblue)
- **Architecture:** Direct JDBC (`Connection`, `PreparedStatement`) for high performance and low abstraction overhead.

### Frontend

- **Framework:** React 18
- **Build Tool:** Vite
- **Routing:** React Router DOM
- **API Communication:** Fetch API with Vite proxy configuration

---

## 🚀 Environment Setup

### Prerequisites

- **JDK 17**: Ensure Java 17 is installed and `JAVA_HOME` is set.
- **Node.js**: v18.x or later is recommended.
- **SQL Server**: A local instance of Microsoft SQL Server or an Azure SQL Database.
- **Maven**: (Optional) Use the included `./mvnw` wrapper.

### 1. Database Setup

1. Install and start your SQL Server instance.
2. Create a new database named `Hackathon`.
3. Locate the SQL script at `database/scripts/SQL4.sql`.
4. Run the script against the `Hackathon` database to initialize the schema and seed initial data (including users and roles).

### 2. Backend Configuration

The backend uses environment variables for sensitive configuration. You can provide these via a `.env.properties` file in the `backend/` directory.

1. Create a file named `.env.properties` in the `backend/` directory.
2. Add the following required variables:

   ```properties
   # Email service API key
   BREVO_API_KEY=your_brevo_api_key_here

   # Secret key for JWT signing (minimum 32 characters)
   JWT_SECRET_KEY=your_very_secret_and_long_jwt_key_here
   ```

3. **Optional Database Overrides**: If your local SQL Server instance does not use the default credentials (`sa` / `12345`) or port, add:

   ```properties
   SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=Hackathon;encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true
   SPRING_DATASOURCE_USERNAME=your_username
   SPRING_DATASOURCE_PASSWORD=your_password
   ```

### 3. Frontend Setup

1. Navigate to the `frontend` directory:

   ```bash
   cd frontend
   ```

2. Install dependencies:

   ```bash
   npm install
   ```

---

## 🏃‍♂️ How to Run

### Start the Backend

From the `backend` directory:

```bash
./mvnw spring-boot:run
```

The server will start at `http://localhost:8080`.

### Start the Frontend

From the `frontend` directory:

```bash
npm run dev
```

The application will be accessible at `http://localhost:5173` (or the port indicated by Vite).

---

## 📜 Project Structure & Rules

- **Backend Rules**: Refer to `docs/rules/Rules.txt` and `docs/rules/AI Rules.txt` for specific coding standards, including the mandatory use of direct JDBC and manual JSON construction.
- **API Proxies**: All frontend requests to `/api/*` are automatically proxied to the backend at `http://localhost:8080` via the Vite configuration.
