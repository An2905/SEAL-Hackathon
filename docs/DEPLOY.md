# Deploy

| Môi trường | URL / Ghi chú |
|------------|----------------|
| Frontend | https://sealhackathon.vercel.app/ (Vercel) |
| Backend | Railway + profile `prod` |
| DB | Railway MySQL |

## Local (mặc định)

- `application.properties` — MySQL `localhost:3306/hackathon`, port `8080`
- `backend/.env.properties` — `JWT_SECRET_KEY`, `RECAPTCHA_SECRET`, `BREVO_API_KEY`
- `mvnw spring-boot:run` trong `backend/`
- FE: `npm run dev` trong `frontend/` (proxy `/api` → 8080)

## Railway Backend

1. Service **Root Directory**: `backend`
2. Thêm MySQL, link variables: `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`
3. Variables:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `JWT_SECRET_KEY`, `RECAPTCHA_SECRET`, `BREVO_API_KEY` (optional)
4. Import `database/scripts/schema.sql` + `seeding.sql`
5. Generate domain → test `GET /api/universities/all`

Config: `application-prod.properties` + `CorsConfig` (profile `prod`, CORS cho Vercel).

## Vercel Frontend

Environment (Production):

- `VITE_API_BASE` = URL Railway backend (không có `/` cuối)
- `VITE_RECAPTCHA_SITE_KEY` = site key

Redeploy sau khi đổi env.

## reCAPTCHA domains

- `sealhackathon.vercel.app`
- `localhost`
