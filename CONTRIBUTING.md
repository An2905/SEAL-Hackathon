# Contributing to SEAL Hackathon

First off, thank you for considering contributing to the SEAL Hackathon project! We welcome and appreciate your efforts to help make this system better for the student community.

To maintain a consistent, high-quality, and maintainable codebase, we ask that you follow these guidelines.

---

## 🚦 Getting Started

Before you start coding, please ensure you have set up your local development environment by following the instructions in the [README.md](README.md).

### Project Structure

This is a monorepo containing both backend and frontend:

- `/backend`: Spring Boot 4.0.6 (Java 17)
- `/frontend`: React 18 + Vite
- `/database`: SQL scripts and schema
- `/docs`: Project rules and documentation

---

## 🐛 Reporting Issues

Before creating a new issue, please **search existing issues** to see if it has already been reported.

When reporting a bug, please include:

- **A clear, descriptive title**.
- **Steps to reproduce**: A detailed list of steps to trigger the behavior.
- **Environment details**: OS, Browser, Node.js version, JDK version, etc.
- **Expected vs. Actual results**: What you thought should happen vs. what actually happened.
- **Screenshots or Logs**: If applicable.

---

## 🛠 Development Workflow

### 1. Git Branching Model

We use **Feature Branching**. Always create a new branch for your work from the `main` branch.

- **Naming Convention**:
  - `feature/short-description` for new features.
  - `fix/short-description` for bug fixes.
  - `docs/short-description` for documentations.
  - `refactor/short-description` for refactoring codebase.
  - `chore/short-description` for updating configs.
- **Workflow**:
  1. `git checkout main`
  2. `git pull origin main`
  3. `git checkout -b feature/your-feature-name`

### 2. Commit Message Convention

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. This helps us generate clear changelogs.

Format: `<type>: <description>`

Common types:

- `feat`: A new feature.
- `fix`: A bug fix.
- `docs`: Documentation only changes.
- `refactor`: A code change that neither fixes a bug nor adds a feature.
- `chore`: Updating build tasks, package manager configs, etc.

_Example: `feat: add team registration endpoint`_

---

## 💻 Coding Standards

### General Principles

- **Clean Code**: Write code that is easy to read and maintain. Use meaningful variable names and follow the Principle of Least Astonishment.
- **DRY (Don't Repeat Yourself)**: Abstract common logic into reusable components or services.

### Frontend (React)

- **Formatting & Linting**: We use **Prettier** for formatting and **ESLint** for linting.
- **Validation**: Run `npm run lint` in the `frontend` directory before submitting.
- Use functional components and hooks.

### Backend (Spring Boot - MANDATORY RULES)

We follow a specific architectural pattern to maintain high performance and low abstraction. **Violating these rules will result in PR rejection.**

1. **No ORM / No JPA**: Strictly **NO** Hibernate, Spring Data JPA, or JdbcTemplate.
2. **Raw JDBC**: Use `javax.sql.DataSource` and `java.sql.Connection` directly.
3. **Security**: Always use `PreparedStatement` to prevent SQL injection.
4. **Manual JSON**: Build JSON responses manually using `StringBuilder`. Do not use Jackson/Gson for response serialization.
5. **Resource Management**: Use try-with-resources for all JDBC resources.

_Reference `TeamService.java` for the standard implementation pattern._

---

## 🚀 Pull Request Process

1. **Single Task**: Each PR should focus on a single task or issue.
2. **Local Testing**: Ensure you have tested your changes locally (UI tests and API tests via Postman).
3. **Linting**: Ensure there are no linting errors.
4. **Description**: Provide a clear summary of your changes, including:
   - What was changed/added?
   - Why was it changed?
   - Any breaking changes?
   - Screenshots (if UI-related).
5. **Review**: At least one maintainer must review and approve your PR before it is merged.

---

## 🧪 Testing Guidelines

- **Success Cases**: Verify the feature works as intended.
- **Edge Cases**: Test for empty inputs, duplicate entries, and invalid data.
- **Security**: Verify that role-based access control (RBAC) is respected.

Happy coding! We look forward to your contributions. 🚀
