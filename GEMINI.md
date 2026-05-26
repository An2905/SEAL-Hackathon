# SEAL Hackathon Project Instructions

This project is organized as a monorepo with separate backend and frontend modules.

## Project Structure

- **backend/**: Spring Boot 4.0.6 (Java 17) backend.
- **frontend/**: React 18 + Vite frontend.
- **database/**: SQL scripts and schema definitions.
- **docs/**: Project rules and documentation.

## Architecture & Development Conventions

### 1. Mandatory Raw JDBC

- **NO JPA, Hibernate, or Spring Data JPA**.
- Use `javax.sql.DataSource` and `java.sql.Connection` directly.
- Use `PreparedStatement` for all database operations.
- Always `close()` resources properly.
- For SQL Server, use `OUTPUT inserted.<id_column>` to retrieve generated IDs.

### 2. DTO & JSON Usage

- Create DTOs **only for requests** in `backend/src/main/java/com/hackathon/hackathon/dto`.
- **NO Response DTOs**: Method return types should be `String`.
- Build JSON strings manually using `StringBuilder`.

### 3. Layers & Regions

- Business logic in `Service` classes, organized with `//region <NAME>` comments.
- Controllers should be thin, delegating to services.

## Key Commands

### Backend

Navigate to the `backend/` directory first:

- **Run Application**: `./mvnw spring-boot:run`
- **Build**: `./mvnw clean package`

### Frontend

Navigate to the `frontend/` directory first:

- **Install Dependencies**: `npm install`
- **Run Dev Server**: `npm run dev`

## Environment Setup

- **Database**: Run `database/scripts/SQL4.sql`.
- **Configuration**: Backend loads config from `backend/src/main/resources/application.properties` and `backend/.env.properties`.
