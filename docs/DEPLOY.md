# Deployment Guide

| Environment | URL / Notes |
|-------------|-------------|
| Frontend | https://sealhackathon.vercel.app/ (Vercel) |
| Backend | Railway + profile `prod` |
| Database | Railway MySQL |

## Local Environment (Default)

- **Configuration:** `application.properties` connects to MySQL at `localhost:3306/hackathon` on port `8080`.
- **Environment variables:** Configured in `backend/.env.properties` (local only, not committed):
  - `JWT_SECRET_KEY`
  - `RECAPTCHA_SECRET`
  - `BREVO_API_KEY`
- **Backend Startup:** Run `./mvnw spring-boot:run` in the `backend/` directory.
- **Frontend Startup:** Run `npm run dev` in the `frontend/` directory. (Vite API proxy will route `/api` request to port `8080`).

## Railway Deployment (Backend)

1. Create a Railway service with **Root Directory** set to `backend`.
2. Provision a MySQL database and link variables: `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`.
3. Add environment variables:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `JWT_SECRET_KEY`, `RECAPTCHA_SECRET`, `BREVO_API_KEY` (optional)
4. Initialize the database by importing `database/scripts/schema.sql` followed by `seeding.sql`.
5. Generate a public domain and verify connection by testing `GET /api/universities/all`.

*Note:* Configuration details are managed in `application-prod.properties` and `CorsConfig.java` (enabled under `prod` profile to configure CORS origins for Vercel).

## Vercel Deployment (Frontend)

Configure the following environment variables on Vercel:

- `VITE_API_BASE` = URL of your Railway backend service (no trailing slash `/`).
- `VITE_RECAPTCHA_SITE_KEY` = ReCAPTCHA public site key.

*Note:* Trigger a redeploy after updating environment variables on Vercel.

## reCAPTCHA Domains

Configure reCAPTCHA to allow validation for:
- `sealhackathon.vercel.app`
- `localhost`
