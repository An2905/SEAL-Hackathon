# SEAL Hackathon — Frontend Flow & Architecture Documentation

**Updated:** 2026-06-26
**Source:** `frontend/` (React 18 + Vite SPA)

---

## 1. Architectural Decision Records (ADRs)

### ADR-001: Vite + React 18 SPA (JavaScript, no TypeScript)

- **Status:** Accepted
- **Context:** The team needed a fast dev-server loop and a simple build pipeline for a hackathon-scale SPA.
- **Decision:** `react@18` + `react-dom@18` + `react-router-dom@6` bundled by `vite`. All source is plain `.jsx`/`.js` — no TypeScript, no PropTypes enforcement.
- **Consequences:** Very fast HMR; no compile-time type safety — shape mismatches between BE and FE are caught only by normalizers (`api/normalizers.js`) and runtime errors.

### ADR-002: JWT stored in `localStorage`, decoded client-side

- **Status:** Accepted
- **Context:** The backend issues a JWT on login (`POST /api/auth/login`). The SPA needs to persist the session across reloads and read the user's role to drive routing.
- **Decision:** Store `hh_token`, `hh_email`, `hh_role`, `hh_full_name` in `localStorage` (see `AuthContext.jsx`). The JWT payload is decoded purely client-side via `utils/jwt.js#parseJwt` (base64url → JSON) to recover/refresh the `role` claim when needed.
- **Consequences:** The JWT carries only `email`, `role`, `userId`, and `fullName`. Fields like `university`, `studentId`, `phone`, and `avatarUrl` are **not** in the JWT — they require a separate `GET /api/auth/profile` fetch. The OTP-based register/reset-password flows rely on a server-side `HttpSession` (JSESSIONID cookie), so `apiFetch` always sets `credentials: 'include'` in addition to the Bearer header.

### ADR-003: Centralized `apiFetch` wrapper + response normalizers

- **Status:** Accepted
- **Context:** The backend mixes plain-text success messages, JSON objects, snake_case/camelCase fields, and numeric IDs that can arrive as `bigint`-like floats.
- **Decision:** All REST calls (except `api/criteriaApi.js`, see Gap Analysis) go through `api/client.js#apiFetch`, which injects `Content-Type: application/json`, a `Bearer` token (when `auth !== false`), and `credentials: 'include'`. Raw responses are passed through `api/normalizers.js` helpers (`normalizeId`, `mapAccountRow`, `mapEventRow`, etc.) to produce consistent camelCase shapes with safe ID types before reaching components.
- **Consequences:** Components never parse raw fetch responses; all error strings are funneled through `utils/errors.js#localizeError` for Vietnamese display.

### ADR-004: Role-based routing via `RequireAuth` / `RequireRole` with UI-role aliases

- **Status:** Accepted
- **Context:** The backend's `users.role` enum (`COORDINATOR`, `EXPERT_INTERNAL`, `EXPERT_EXTERNAL`, `STUDENT_FPT`, `STUDENT_EXTERNAL`) doesn't map 1:1 to the FE's dashboard areas (`/staff`, `/student`, `/mentor`, `/judge`).
- **Decision:** `guards/RequireRole.jsx` defines `ROLE_ALIASES` mapping a UI-facing alias (`Staff`, `Student`, `Mentor`, `Judge`, `Expert`) to one or more backend roles. Both `EXPERT_INTERNAL` and `EXPERT_EXTERNAL` satisfy **both** `Mentor` and `Judge` aliases — any "expert" account can open both `/mentor` and `/judge`, regardless of whether they have actual assignments (those dashboards then show empty states if `getAssignedEvents`/`getAssignments` returns nothing).
- **Consequences:** Adding a new role requires updating `ROLE_ALIASES`, `AuthContext.ROLE_PATHS`, and `utils/roleLabels.js` in three places.

### ADR-005: Tab-based dashboard shell with URL-persisted active tab

- **Status:** Accepted
- **Context:** Staff/Student/Mentor/Judge dashboards each have multiple logical sections but should remain on a single route for simplicity and shareable links.
- **Decision:** `components/layout/DashboardLayout.jsx` renders a `TopBar` + `TabNav` + `ModuleContainer`. The active tab key is stored in the URL query string (`?tab=<key>`) via `useSearchParams`, not in route params.
- **Consequences:** Deep-linking to a specific staff tab works (`/staff?tab=assign`), but tab content is not represented as separate routes/guards — `RequireRole` only guards the parent route.

### ADR-006: Realtime chat via STOMP/SockJS layered over REST history

- **Status:** Accepted
- **Context:** Team↔mentor chat needs both message history (for reload) and live delivery.
- **Decision:** REST endpoints (`api/chat.js`) fetch room metadata and the last 200 messages. `hooks/useChatStomp.js` opens a SockJS connection to `${API_BASE}/ws`, authenticates via STOMP CONNECT headers (Bearer token), subscribes to `/topic/chat/{roomId}`, and publishes new messages to `/app/chat.send`.
- **Consequences:** Two parallel code paths must stay in sync (initial REST load + STOMP append); a disconnected socket silently falls back to "no live updates" with no reconnect UI beyond the library defaults.

### ADR-007: Profile data fetched separately from JWT

- **Status:** Accepted
- **Context:** The JWT payload intentionally carries only identity claims (`email`, `role`, `userId`, `fullName`). DB-stored profile fields (`university`, `studentId` for students; `phone`, `avatarUrl` for experts/coordinators) are not encoded in the token to avoid stale data and token bloat.
- **Decision:** `StaffProfilePage` (and any page needing full profile) calls `getProfile()` → `GET /api/auth/profile` on mount. The response is stored in local `profileData` state and passed as a prop to `ProfileModal`.
- **Consequences:** Profile display always reflects DB state, not the cached JWT. A network error on mount silently leaves `profileData` as `null`; the UI falls back to `auth.email` for the email field and shows `'—'` for DB-only fields.

---

## 2. Application Structure & Tech Stack

| Layer              | Technology                                                                                                                                                      |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Build/dev server   | Vite 5.4 (`vite.config.js`)                                                                                                                                     |
| UI framework       | React 18.3 + `react-dom`                                                                                                                                        |
| Routing            | `react-router-dom` 6.26 (lazy routes + `Suspense`)                                                                                                              |
| Realtime           | `@stomp/stompjs` 7.x + `sockjs-client` over `/ws`                                                                                                               |
| Rich text          | `@ckeditor/ckeditor5-build-classic` + `@ckeditor/ckeditor5-react` (dependency present; usage limited to rich-text fields in event/criteria description editors) |
| Linting/formatting | ESLint (`eslint-plugin-react`, `react-hooks`) + Prettier                                                                                                        |
| Captcha            | Google reCAPTCHA (`hooks/useRecaptcha.js` + `components/common/RecaptchaWidget`)                                                                                |

### Dev proxy (`vite.config.js`)

```js
server: {
  proxy: {
    '/api': { target: 'http://localhost:8080', changeOrigin: true },
    '/ws':  { target: 'http://localhost:8080', ws: true, changeOrigin: true }
  }
}
```

In dev, `VITE_API_BASE` is typically empty so `apiFetch` calls hit relative `/api/...` paths, proxied to the Spring Boot backend on `:8080`. In production, `VITE_API_BASE` points at the deployed API origin.

### Folder layout (`frontend/src/`)

```
api/            REST clients (one file per backend controller area) + normalizers.js + client.js
components/
  common/       Generic UI primitives (Modal, FormField, Pagination, AccountDropdown, LoginModal, ...)
  chat/         ChatPopup, TeamChatPanel
  expert/       ExpertGroupColleaguesBoard, expertDashboardUtils
  judge/        JudgeCriteriaPanel, JudgeScoreModal, SubmissionLinks
  landing/      LandingEventsSection
  layout/       DashboardLayout, TopBar, TabNav, ModuleContainer, DashboardHeader, SiteFooter, HomeNavbar
context/        AuthContext, ToastContext
guards/         RequireAuth, RequireRole
hooks/          useChatStomp, useRecaptcha
pages/
  HomePage.jsx
  dashboards/
    StudentDashboard.jsx, MentorDashboard.jsx, JudgeDashboard.jsx
    DashboardShell.jsx
    staff/      StaffLayout + 9 tab/standalone pages
utils/          errors.js, jwt.js, recaptcha.js, roleLabels.js
```

---

## 3. Routing Map & Guards

### `main.jsx` → `App.jsx`

`main.jsx` wraps `<App />` in `<BrowserRouter>` + `<React.StrictMode>`. `App.jsx` wraps the route tree in `AuthProvider` → `ToastProvider` → `Suspense` (all dashboard pages are `React.lazy`-loaded).

| Path                              | Component                            | Guard                        | Allowed roles (via alias)            |
| --------------------------------- | ------------------------------------ | ---------------------------- | ------------------------------------ |
| `/`                               | `HomePage`                           | none (public)                | —                                    |
| `/student`                        | `StudentDashboard`                   | `RequireRole role='Student'` | `STUDENT_FPT`, `STUDENT_EXTERNAL`    |
| `/staff`                          | `StaffLayout` (5 tabs)               | `RequireRole role='Staff'`   | `COORDINATOR`                        |
| `/staff/events/:eventId`          | `EventDetailsPage`                   | `RequireRole role='Staff'`   | `COORDINATOR`                        |
| `/staff/events/:eventId/check-in` | `StaffCheckInPage`                   | `RequireRole role='Staff'`   | `COORDINATOR`                        |
| `/mentor`                         | `MentorDashboard`                    | `RequireRole role='Mentor'`  | `EXPERT_INTERNAL`, `EXPERT_EXTERNAL` |
| `/judge`                          | `JudgeDashboard`                     | `RequireRole role='Judge'`   | `EXPERT_INTERNAL`, `EXPERT_EXTERNAL` |
| `/profile`                        | `ProfilePage` (= `StaffProfilePage`) | `RequireAuth`                | any authenticated user               |
| `*`                               | —                                    | `Navigate to='/' replace`    | —                                    |

### `guards/RequireAuth.jsx`

Reads `auth.isLoggedIn` from `AuthContext`. If `false`, redirects to `/`.

### `guards/RequireRole.jsx`

```js
const ROLE_ALIASES = {
  Staff: ['COORDINATOR'],
  Student: ['STUDENT_FPT', 'STUDENT_EXTERNAL'],
  Mentor: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL'],
  Judge: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL'],
  Expert: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL']
}
```

1. If not logged in → redirect to `/`.
2. If `auth.role` is not in `ROLE_ALIASES[role]` → redirect to `pathForRole(auth.role)` (bounce the user to their own dashboard).
3. Otherwise render the protected element.

---

## 4. Authentication & Session Model

### Storage (`context/AuthContext.jsx`)

| Key            | Contents                                                    |
| -------------- | ----------------------------------------------------------- |
| `hh_token`     | Raw JWT returned by `/api/auth/login`                       |
| `hh_email`     | User email                                                  |
| `hh_role`      | Backend role enum value (`COORDINATOR`, `STUDENT_FPT`, ...) |
| `hh_full_name` | Display name                                                |

`AuthProvider` hydrates state from `localStorage` on mount and exposes:

- `auth` — `{ token, email, role, fullName, isLoggedIn }`
- `saveAuth({ token, email, role, fullName })` — writes to `localStorage` + state (used after login, registration auto-login, and profile update re-issuing a token)
- `clearAuth()` — wipes all `hh_*` keys (used on logout)
- `pathForRole(role)` — via `ROLE_PATHS`:
  | Role(s) | Path |
  |---------|------|
  | `COORDINATOR` | `/staff` |
  | `EXPERT_INTERNAL`, `EXPERT_EXTERNAL` | `/mentor` |
  | `STUDENT_FPT`, `STUDENT_EXTERNAL` | `/student` |
- `labelForRole(role)` / `pillLabelForRole(role)` — display labels, backed by `utils/roleLabels.js` (`ROLE_UI_LABELS`, `ROLE_VI_LABELS`, `vietnameseRoleLabel`)

### What the JWT does NOT carry

`university`, `studentId`, `phone`, and `avatarUrl` are **not** JWT claims. They must be fetched from the backend via `GET /api/auth/profile` (see `api/auth.js#getProfile`). Components that need these fields should call `getProfile()` on mount and store the result in local state.

### JWT decoding (`utils/jwt.js#parseJwt`)

Pure client-side base64url decode of the JWT payload (no signature verification — the FE never trusts the JWT for authorization, only for display; all real authorization happens server-side per request).

### Error localization (`utils/errors.js#localizeError`)

- Maps known technical codes (`NETWORK`, `HTTP_401`, `HTTP_403`, `HTTP_404`, `HTTP_500`) and known backend error strings (e.g. `"Invalid captcha."`, `"Team name already exists..."`) to Vietnamese.
- Detects technical leakage (SQL fragments, Java stack traces, `org.springframework`, `Caused by:`) and replaces them with a generic "Lỗi máy chủ. Vui lòng thử lại sau." message.
- Vietnamese messages from the server pass through unchanged; short English messages pass through unchanged (assumed already user-safe).

### Login flow (`components/common/LoginModal.jsx`)

1. User enters email/password, completes reCAPTCHA (`useRecaptcha().RecaptchaField`).
2. `login({ email, password, captchaToken })` → `POST /api/auth/login`.
3. `parseLoginResponse(text)` (in `api/client.js`) handles both the new JSON `{message, token}` shape and legacy plain-text, decoding `role`/`email` from the JWT via `parseJwt`.
4. On success: `saveAuth({ token, email, role, fullName })`, toast "Chào mừng …", `navigate(pathForRole(role))`.
5. On failure: `localizeError` + `resetCaptcha()`.

### Registration flow (`components/common/RegisterModal.jsx`) — 2-step, session-based OTP

1. **Info step:** full name, email, student ID, university (populated via `getAllUniversities()`), password + reCAPTCHA → `sendRegisterOtp(...)` → `POST /api/auth/register/otp`. Backend stores the pending registration in `HttpSession` and either emails an OTP (production) or prints it to the server console (when `email.dev-bypass=true`).
2. **OTP step:** user enters the 6-digit code → `verifyAndRegister({ email, otp })` → `POST /api/auth/register`. Backend validates the session OTP, inserts `users` (+`studentprofile`), assigning `STUDENT_FPT` or `STUDENT_EXTERNAL` based on email/university pattern.
3. A separate `useRecaptcha()` instance powers the "resend OTP" action.

### Password reset flow (`components/common/ResetPasswordModal.jsx`) — 2-step, session-based OTP

1. **Email step:** `sendResetPasswordOtp({ email })` → `POST /api/auth/password/reset-otp`.
2. **OTP + new password step:** `verifyAndResetPassword({ email, otp, newPassword })` → `POST /api/auth/password/reset`, with a confirm-password client-side check and a resend-OTP option.

---

## 5. API Client Layer

### `api/client.js`

- `API_BASE` — from `import.meta.env.VITE_API_BASE`, trailing slash stripped (empty in dev → relative paths via Vite proxy).
- `resolveAssetUrl(path)` — prefixes `API_BASE` for server-hosted assets (e.g. avatar images).
- `apiFetch(path, { method='GET', body, auth=true })`:
  - Sets `Content-Type: application/json`.
  - If `auth !== false` and a token exists in `localStorage`, sets `Authorization: Bearer <token>`.
  - Always sets `credentials: 'include'` (for `HttpSession`/JSESSIONID-based OTP flows).
  - Throws `Error('NETWORK')` if `fetch` itself rejects.
  - Throws the raw response text as an `Error` on non-2xx responses (later passed through `localizeError`).
- `parseLoginResponse(text)` — normalizes the login endpoint's response shape (JSON or legacy plain text) and decodes the JWT.

### `api/normalizers.js`

Shared helpers used across multiple API modules:

- `normalizeId(value)` (+ aliases `normalizeEventId`, `normalizeAccountUserId`, `normalizeRegistrationId`) — coerces numeric/bigint-as-float IDs into stable strings.
- `mapCount`, `mapList(value, mapper)` — defensive array/number coercion for BE payloads that may omit fields.
- `countPendingTeams(teams)` — counts `team_registrations` with `status === 'PENDING'` for the Staff "pending teams" badge.
- `mapAccountRow`, `mapEventRow`, `mapEventDetailRow`, `mapMentorAssignmentRow`, `mapMentorAssignedTeamRow`, `mapGroupColleagueRow`, `mapGroupColleaguesResponse`, `mapCriteriaResponse`, `mapJudgeScoreRow`, `mapTeamToScoreRow` — per-entity field mapping from BE snake_case/camelCase variants to a single FE shape.

### API modules → Backend endpoints

| Module                                         | Function                                                       | Endpoint                                                                                                                                                |
| ---------------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `auth.js`                                      | `login`                                                        | `POST /api/auth/login`                                                                                                                                  |
|                                                | `sendRegisterOtp`                                              | `POST /api/auth/register/otp`                                                                                                                           |
|                                                | `verifyAndRegister`                                            | `POST /api/auth/register`                                                                                                                               |
|                                                | `sendResetPasswordOtp`                                         | `POST /api/auth/password/reset-otp`                                                                                                                     |
|                                                | `verifyAndResetPassword`                                       | `POST /api/auth/password/reset`                                                                                                                         |
|                                                | `getProfile`                                                   | `GET /api/auth/profile` — returns `{fullName, email, role, university, studentId, phone, avatarUrl}`                                                   |
|                                                | `updateProfile`                                                | `PUT /api/auth/profile` — returns JSON `{message, newToken}`                                                                                            |
|                                                | `updatePassword`                                               | `PUT /api/auth/password`                                                                                                                                |
|                                                | `getGithubLinkUrl`                                             | `GET /api/auth/github/link-url` — returns `{authorizeUrl}`                                                                                              |
|                                                | `getGithubLinkStatus`                                          | `GET /api/auth/github/status` — returns `{githubLinked, githubUsername}`                                                                                |
| `team.js`                                      | `getMyTeam`                                                    | `GET /api/team/me`                                                                                                                                      |
|                                                | `createTeam`                                                   | `PUT /api/team/create`                                                                                                                                  |
|                                                | `joinTeam`                                                     | `PUT /api/team/join`                                                                                                                                    |
|                                                | `joinEvent`                                                    | `PUT /api/team/join-event`                                                                                                                              |
|                                                | `deleteMember`                                                 | `DELETE /api/team/delete-member`                                                                                                                        |
|                                                | `getTeamRegistrations`                                         | `GET /api/team/registrations`                                                                                                                           |
|                                                | `getTeamTrackMentors`                                          | `GET /api/team/mentors`                                                                                                                                 |
|                                                | `getTeamRounds`                                                | `GET /api/team/rounds`                                                                                                                                  |
|                                                | _(none)_                                                       | ⚠️ `PUT /api/team/submit-project`, `GET /api/team/submissions` — **not called anywhere in FE** (see §10)                                                |
| `chat.js`                                      | `openChatRoom`                                                 | `POST /api/chat/rooms/open`                                                                                                                             |
|                                                | `createChatRoom`                                               | `POST /api/chat/rooms`                                                                                                                                  |
|                                                | `listChatRooms`                                                | `GET /api/chat/rooms`                                                                                                                                   |
|                                                | `getChatRoom`                                                  | `GET /api/chat/rooms/{roomId}`                                                                                                                          |
|                                                | `getChatMessages`                                              | `GET /api/chat/rooms/{roomId}/messages`                                                                                                                 |
|                                                | `getWebSocketUrl`                                              | `WS /ws` → STOMP `/topic/chat/{roomId}`, `/app/chat.send`                                                                                               |
| `checkIn.js`                                   | `getCheckInPage`                                               | `GET /api/staff/check-in`                                                                                                                               |
|                                                | `setTeamCheckIn`                                               | `PUT /api/staff/check-in/team`                                                                                                                          |
|                                                | `setMemberCheckIn`                                             | `PUT /api/staff/check-in/member`                                                                                                                        |
| `criteriaApi.js` (own `fetch`, not `apiFetch`) | `getCriteriaByRound` (+ deprecated alias `getCriteriaByEvent`) | `GET /api/staff/criteria`                                                                                                                               |
|                                                | `getCriteriaDetail`                                            | `GET /api/staff/criteria/detail`                                                                                                                        |
|                                                | `createCriteria`                                               | `POST /api/staff/criteria`                                                                                                                              |
|                                                | `updateCriteria`                                               | `PUT /api/staff/criteria`                                                                                                                               |
|                                                | `deleteCriteria`                                               | `DELETE /api/staff/criteria`                                                                                                                            |
|                                                | `getCriteriaForJudge` (`@deprecated`, duplicate)               | `GET /api/judge/criteria`                                                                                                                               |
| `event.js`                                     | `getEventDetail`                                               | `GET /api/staff/events/detail`                                                                                                                          |
|                                                | `getAllEvents`                                                 | `GET /api/staff/events`                                                                                                                                 |
|                                                | `attachPendingTeamsToEvents`                                   | composed (per-event `getEventDetail` + `countPendingTeams`)                                                                                             |
| `eventService.js`                              | `createEventGroup`                                             | `POST /api/staff/events/groups`                                                                                                                         |
|                                                | `createEventRound`                                             | `POST /api/staff/events/rounds`                                                                                                                         |
|                                                | `deleteEventGroup`                                             | `DELETE /api/staff/events/groups`                                                                                                                       |
|                                                | `deleteEventRound`                                             | `DELETE /api/staff/events/rounds`                                                                                                                       |
|                                                | `getEventGroupTeams`                                           | `GET /api/staff/events/groups/teams`                                                                                                                    |
|                                                | `assignTeamToGroup`                                            | `POST /api/staff/events/groups/teams`                                                                                                                   |
|                                                | `removeTeamFromGroup`                                          | `DELETE /api/staff/events/groups/teams`                                                                                                                 |
|                                                | `updateEventGroup`                                             | `PUT /api/staff/events/groups`                                                                                                                          |
|                                                | `getEventRoundDetail`                                          | `GET /api/staff/events/rounds/detail`                                                                                                                   |
|                                                | `updateEventRound`                                             | `PUT /api/staff/events/rounds`                                                                                                                          |
|                                                | `updateEvent`                                                  | `PUT /api/staff/events`                                                                                                                                 |
|                                                | `createEventAward` / `updateEventAward` / `deleteEventAward`   | `POST` / `PUT` / `DELETE /api/staff/events/awards`                                                                                                      |
| `judge.js`                                     | `getAssignedEvents`                                            | `GET /api/judge/events`                                                                                                                                 |
|                                                | `getAssignedCurrentRounds`                                     | `GET /api/judge/events/current-rounds`                                                                                                                  |
|                                                | `getAssignments` (alias `getJudgeAssignments`)                 | `GET /api/judge/assignments`                                                                                                                            |
|                                                | `getGroupColleagues`                                           | `GET /api/judge/colleagues`                                                                                                                             |
|                                                | `getCriteriaForJudge`                                          | `GET /api/judge/criteria`                                                                                                                               |
|                                                | `getTeamsToScore`                                              | `GET /api/judge/teams-to-score`                                                                                                                         |
|                                                | `getScoreBySubmission`                                         | `GET /api/judge/scores`                                                                                                                                 |
|                                                | `submitScore`                                                  | `POST /api/judge/scores`                                                                                                                                |
|                                                | `updateScore`                                                  | `PUT /api/judge/scores`                                                                                                                                 |
| `mentor.js`                                    | `getAssignedEvents`                                            | `GET /api/mentor/events`                                                                                                                                |
|                                                | `getAssignedCurrentRounds`                                     | `GET /api/mentor/events/current-rounds`                                                                                                                 |
|                                                | `getMentorAssignments`                                         | `GET /api/mentor/assignments`                                                                                                                           |
|                                                | `getGroupColleagues`                                           | `GET /api/mentor/colleagues`                                                                                                                            |
|                                                | `getAssignedTeams`                                             | `GET /api/mentor/teams`                                                                                                                                 |
| `publicEvent.js`                               | `getPublicEvents`                                              | `GET /api/events` (`auth: false`)                                                                                                                       |
| `staff.js`                                     | `createStaffAccount`                                           | `POST /api/staff/register`                                                                                                                              |
|                                                | `createEvent`                                                  | `POST /api/staff/events` — accepts optional `githubTemplateRepo`                                                                                        |
|                                                | `changeEventStatus`                                            | `PUT /api/staff/events/status`                                                                                                                          |
|                                                | `getAllAccounts`                                               | `GET /api/staff/accounts`                                                                                                                               |
|                                                | `changeAccountStatus`                                          | `PUT /api/staff/change-status`                                                                                                                          |
|                                                | `changeTeamRegistrationStatus`                                 | `PUT /api/staff/team-registration/status`                                                                                                               |
|                                                | `assignJudge`                                                  | `POST /api/staff/assign/judge`                                                                                                                          |
|                                                | `assignMentor`                                                 | `POST /api/staff/assign/mentor`                                                                                                                         |
|                                                | `exportEventsExcel`                                            | `GET /api/staff/events/export` (raw `fetch`, returns `Blob`)                                                                                            |
|                                                | `retryGitHubProvisioning`                                      | `POST /api/github/registrations/{id}/retry`                                                                                                             |
|                                                | `updateEventRepoAccess`                                        | `PUT /api/staff/events/{id}/github-access?grant=true\|false`                                                                                            |
|                                                | _(none)_                                                       | ⚠️ `sendAnnouncementToAll`, `sendAnnouncementToParticipants` — **imported by `StaffAnnouncementsPage.jsx` but not exported from this module** (see §10) |
| `staffAssignment.js`                           | `deleteMentorAssignment` / `updateMentorAssignment`            | `DELETE` / `PUT /api/staff/assign/mentor`                                                                                                               |
|                                                | `deleteJudgeAssignment` / `updateJudgeAssignment`              | `DELETE` / `PUT /api/staff/assign/judge`                                                                                                                |
| `staffUniversity.js`                           | `getStaffUniversities`                                         | `GET /api/staff/universities`                                                                                                                           |
|                                                | `createUniversity`                                             | `POST /api/staff/universities`                                                                                                                          |
|                                                | `updateUniversity`                                             | `PUT /api/staff/universities`                                                                                                                           |
|                                                | `getDeleteUniversityPreview`                                   | `GET /api/staff/universities/delete-preview`                                                                                                            |
|                                                | `deleteUniversity`                                             | `DELETE /api/staff/universities`                                                                                                                        |
| `university.js`                                | `getAllUniversities`                                           | `GET /api/universities/all` (public)                                                                                                                    |

---

## 6. Shared UI Shell & Components

### `components/layout/DashboardLayout.jsx`

The shell used by every authenticated dashboard (`StudentDashboard`, `StaffLayout`, `MentorDashboard`, `JudgeDashboard`, `EventDetailsPage`/`StaffCheckInPage` via `DashboardShell`):

- `TopBar` — FPT logo + `AccountDropdown`.
- `TabNav` — renders the `tabs` prop (if provided) as a horizontal nav; active tab synced to `?tab=` query param via `useSearchParams`.
- `ModuleContainer` — wraps `DashboardHeader` (title/subtitle) + the active tab's `content`, or `children` directly for non-tabbed pages.
- `SiteFooter` — page footer.
- Props like `roleLabel`, `showStaffFields`, `moduleTitle`, `moduleSubtitle` configure the header text and which extra profile fields `ProfileModal` exposes.

### `pages/dashboards/DashboardShell.jsx`

Thin compatibility wrapper: maps `title`/`subtitle` props to `moduleTitle`/`moduleSubtitle` for `DashboardLayout`, used by the standalone `EventDetailsPage` and `StaffCheckInPage` (which are not part of the `StaffLayout` tab set but still want the same shell).

### `components/common/AccountDropdown.jsx`

Unified avatar + name + role-pill trigger, used in both `TopBar` (dashboards) and `HomeNavbar` (landing). Menu items:

- **Trang làm việc** → `navigate(pathForRole(auth.role))`
- **Hồ sơ của tôi** → profile tab (if current page has one) or `/profile`
- **Thông báo** → disabled, "Sắp ra mắt" (Coming Soon) badge
- **Đăng xuất** → `clearAuth()` + `navigate('/')`

### `components/common/ProfileModals.jsx`

#### `ProfileModal`

Props: `isOpen`, `onClose`, `showStudentFields`, `showStaffFields`, `profileData`, `onProfileUpdated`.

- **Pre-fill:** a `useEffect` on `[isOpen, profileData, auth.fullName, auth.email]` resets the form when the modal opens, pulling `university`/`studentId`/`phone` from `profileData` (the object returned by `getProfile()`).
- **Fields shown:**
  - All roles: `fullName` (editable), `email` (editable — leave blank to keep current).
  - Students (`showStudentFields`): `university` + `studentId` editable inputs.
  - Experts/Coordinators (`showStaffFields`): `phone` editable input.
- **Submit:** calls `updateProfile(form)` → `PUT /api/auth/profile`. The backend returns JSON `{message, newToken}`. The frontend JSON-parses the response and reads `parsed.newToken`. On success, calls `saveAuth({ token: newToken, fullName })` and fires `onProfileUpdated` with the updated local profile state.

#### `PasswordModal`

`oldPassword` / `newPassword` / `confirmPassword` → `updatePassword` → `PUT /api/auth/password`. Client-side confirm-match check before submit.

### `context/ToastContext.jsx`

`ToastProvider` exposes `showToast(message, type='success')`. Renders a single auto-dismissing (3.5s) toast banner positioned below the navbar. `type` drives a `toast--<type>` CSS class (`success` / `error`).

### `hooks/useRecaptcha.js` + `components/common/RecaptchaWidget`

`useRecaptcha()` returns `{ getCaptchaToken, resetCaptcha, RecaptchaField }`. `RecaptchaField` is a memoized component wrapping `RecaptchaWidget` (forwarded ref exposing `getToken`/`reset`). Used by `LoginModal`, `RegisterModal` (2 independent instances for submit vs. resend), and implicitly by `ResetPasswordModal`.

### Other shared primitives (presentational, used pervasively)

`Pagination` (default page size `5`), `Modal`, `FormField`, `FormMessage`, `LoadingButton`, `LoadingState`, `ConfirmModal`, `Avatar`, `AccordionCard`, `FullWidthSearchBar`, `PendingTeamsBadge`, `ComingSoonCards`, `TabNav`, `ModuleContainer`, `DashboardHeader`, `SiteFooter`.

---

## 7. Domain Flows by Page

### 7.1 Public Landing — `/` (`pages/HomePage.jsx`)

- **Auth scope:** Public. `HomeNavbar` shows Login/Register buttons if logged out, or `AccountDropdown` if logged in.
- **Sections:** Hero, `LandingEventsSection`, About, Schedule, Gallery (mostly static content).
- **`LandingEventsSection`:**
  - Calls `getPublicEvents()` → `GET /api/events` (no auth).
  - Groups results by `PUBLIC_STATUS_ORDER` (`UPCOMING` → `ONGOING` → `COMPLETED`) with Vietnamese status labels/pills.
  - `FullWidthSearchBar` filters by title/description/status (client-side).
  - If any non-`COMPLETED` event exists and `onOpenRegister` is provided, shows a "Đăng ký tham gia ngay" CTA that opens `RegisterModal`.
- **Modal state:** `HomePage` owns a `modal` state (`null | 'login' | 'register' | 'reset'`) toggled by `HomeNavbar`/CTA buttons, rendering `LoginModal`/`RegisterModal`/`ResetPasswordModal`.

### 7.2 Authentication Modals

Covered in detail in §4 (Login / Register / Reset Password flows).

### 7.3 Student Dashboard — `/student` (`pages/dashboards/StudentDashboard.jsx`)

- **Role:** `STUDENT_FPT`, `STUDENT_EXTERNAL`.
- **Team state machine** (`teamState ∈ {'loading','no-team','has-team'}`), driven by `getMyTeam()` (`GET /api/team/me`):
  - **`no-team`:**
    - `CreateTeamForm` → `createTeam({ teamName })` → `PUT /api/team/create`.
    - `JoinTeamForm` → `joinTeam({ enrollCode })` → `PUT /api/team/join`.
  - **`has-team`:**
    - `TeamInfoCard` — team name, `enrollCode`, member list with leader badge.
    - `DeleteMemberForm` (leader-only) → `deleteMember({ memberId })` → `DELETE /api/team/delete-member`.
    - `TeamEventsPanel` / `TeamEventsList` — `getTeamRegistrations()` → `GET /api/team/registrations`; shows each event's registration status (PENDING/APPROVED/REJECTED).
      - `EventMentorsBlock` — for `APPROVED` registrations, calls `getTeamTrackMentors(eventId)` → `GET /api/team/mentors` to show assigned mentors for that event/round/group.
    - `JoinEventForm` → `joinEvent({ eventId })` → `PUT /api/team/join-event` (registers the team, status `PENDING`).
    - `ActivityLog` — client-side-only running log of actions performed in this session (not persisted server-side).
- **Chat:** Floating `ChatPopup` (`mode='student'`) — for a chosen event/mentor pair, `openChatRoom({eventId, mentorId})` → `POST /api/chat/rooms/open`, then loads history via `getChatMessages` and live-updates via `useChatStomp`.
- **⚠️ Not present:** No UI calls `PUT /api/team/submit-project` or `GET /api/team/submissions` — see §10.

### 7.4 Mentor Dashboard — `/mentor` (`pages/dashboards/MentorDashboard.jsx`)

- **Role:** `EXPERT_INTERNAL`, `EXPERT_EXTERNAL`.
- **Initial load** (parallel via `Promise.allSettled`):
  - `getAssignedEvents()` → `GET /api/mentor/events` — paginated (5/page) list with status pills.
  - `getAssignedCurrentRounds()` → `GET /api/mentor/events/current-rounds` — currently-active rounds, paginated.
  - `getMentorAssignments()` → `GET /api/mentor/assignments` — populates assignment-selector buttons (auto-selects the first one).
- **On assignment selection** (`eventId-roundId-groupId` key):
  - `getAssignedTeams({eventId, roundId, groupId, registrationStatus})` → `GET /api/mentor/teams`, filterable via `TEAM_STATUS_FILTERS` (`APPROVED` default / `PENDING` / `ALL`). Shows team name, leader, members, `enrollCode`, registration-status pill.
  - `getGroupColleagues({eventId, roundId, groupId})` → `GET /api/mentor/colleagues`, rendered by `ExpertGroupColleaguesBoard` as two chip rows (Mentors / Judges), highlighting the current user ("Bạn").
- **Account info panel:** static display of `auth.fullName`/`auth.email`/role/session status.
- **Cross-link:** "Chuyển sang khu Judge" button → `/judge`.
- **Chat:** Floating `ChatPopup` (`mode='mentor'`) — `listChatRooms()` to pick an existing room, then history + STOMP live updates.

### 7.5 Judge Dashboard — `/judge` (`pages/dashboards/JudgeDashboard.jsx`)

- **Role:** `EXPERT_INTERNAL`, `EXPERT_EXTERNAL`.
- **Initial load** (same `Promise.allSettled` pattern): `getAssignedEvents()`, `getAssignedCurrentRounds()`, `getAssignments()`/`getJudgeAssignments()` — all `/api/judge/...` analogues of the mentor calls.
- **`JudgeCriteriaPanel`** (`roundId`, `roundName`): `getCriteriaForJudge(roundId)` → `GET /api/judge/criteria`; renders a weight bar (`totalWeight` vs 100%, color-coded green/amber/red) and read-only criteria cards (`criterionName`, `weight`, `maxScore`, `description`).
- **Teams-to-score list:** `getTeamsToScore({eventId, roundId, groupId})` → `GET /api/judge/teams-to-score`. Each row shows `SubmissionLinks` (GitHub/Demo/Báo cáo/Slide, only rendered if the URL field is non-empty) and a score button (label depends on `team.scored`).
- **`JudgeScoreModal`** (`assignment`, `team`):
  - On open: `getCriteriaForJudge(assignment.roundId)`; if `team.scored && team.submissionId`, also `getScoreBySubmission(team.submissionId)` → `GET /api/judge/scores` to pre-fill the form.
  - Per-criteria inputs: `score` (0..`maxScore`, validated client-side) + optional `feedback`.
  - `calcPreviewTotal` — live-computed weighted total (`Σ score × weight/100`, rounded to 2 decimals).
  - Submit: `submitScore(payload)` → `POST /api/judge/scores` (new) or `updateScore(scoreId, payload)` → `PUT /api/judge/scores` (edit), where `payload = { eventId, roundId, groupId, submissionId, details: [{criteriaId, score, feedback}] }`.
  - If `team.submissionId` is absent (team hasn't submitted), the form is disabled with "Đội chưa nộp bài cho vòng này."
- **Cross-link:** "Chuyển sang khu Mentor" → `/mentor`.

### 7.6 Staff / Coordinator Area — `/staff` (`pages/dashboards/staff/StaffLayout.jsx`)

**Role:** `COORDINATOR`. `StaffLayout` renders `DashboardLayout` with `roleLabel='Nhân viên'`, `showStaffFields`, and 5 tabs:

| Tab key        | Label     | Component               | Summary                                                                            |
| -------------- | --------- | ----------------------- | ---------------------------------------------------------------------------------- |
| `overview`     | Tổng quan | `StaffOverviewPage`     | Static account/session info (`auth.fullName`, `auth.email`, last-login timestamp). |
| `events`       | Sự kiện   | `StaffEventsPage`       | List/create/manage events; navigates to `EventDetailsPage` and `StaffCheckInPage`. |
| `accounts`     | Tài khoản | `StaffAccountsPage`     | Manage Expert (Mentor/Judge) and Student accounts.                                 |
| `assign`       | Phân công | `StaffAssignPage`       | Assign/remove mentors & judges per round/group.                                    |
| `universities` | Trường ĐH | `StaffUniversitiesPage` | CRUD universities list.                                                            |

#### `StaffEventsPage`

- `getAllEvents(status)` → `GET /api/staff/events` (status filter: `ALL`/`BUILDING`/`UPCOMING`/`ONGOING`/`COMPLETED`).
- `attachPendingTeamsToEvents(events)` — enriches each event with a pending-registration count (via `countPendingTeams`) shown by `PendingTeamsBadge`.
- `createEvent({title, description, startDate, endDate, maxTeams, numRounds, githubTemplateRepo})` → `POST /api/staff/events` (new events start as `BUILDING`). `githubTemplateRepo` is an optional GitHub URL stored in `github_template_repo` column; used to provision team repositories when a registration is approved.
- `changeEventStatus({eventId, newStatus})` → `PUT /api/staff/events/status` — lifecycle `BUILDING → UPCOMING → ONGOING → COMPLETED`.
- `exportEventsExcel()` → `GET /api/staff/events/export` — downloads an `.xlsx` via `Blob`.
- Per-event navigation: "Chi tiết" → `/staff/events/:eventId`; "Check-in" → `/staff/events/:eventId/check-in`.

#### `StaffAccountsPage`

- `getAllAccounts(role, input)` → `GET /api/staff/accounts` — filter by role (`ALL`/`EXPERT_INTERNAL`/`EXPERT_EXTERNAL`/`STUDENT_FPT`/`STUDENT_EXTERNAL`) and free-text search.
- `createStaffAccount({email, fullName, role})` → `POST /api/staff/register` — creates Mentor/Judge (`EXPERT_INTERNAL`/`EXPERT_EXTERNAL`) accounts; backend emails an invitation.
- `changeAccountStatus({userId, status})` → `PUT /api/staff/change-status` — approve/reject accounts (`PENDING`/`APPROVED`/`REJECTED`); cannot demote another `COORDINATOR`.

#### `StaffAssignPage`

- Assign: `assignMentor({userId, roundId, groupId})` / `assignJudge({judgeId, roundId, groupId})` (from `staff.js`) → `POST /api/staff/assign/mentor` / `judge`.
- Edit/remove existing assignments: `updateMentorAssignment` / `deleteMentorAssignment` / `updateJudgeAssignment` / `deleteJudgeAssignment` (from `staffAssignment.js`) → `PUT`/`DELETE /api/staff/assign/mentor|judge`.

#### `StaffUniversitiesPage`

- `getStaffUniversities()` → `GET /api/staff/universities`.
- `createUniversity` / `updateUniversity` → `POST`/`PUT /api/staff/universities`.
- `getDeleteUniversityPreview(universityId)` → `GET /api/staff/universities/delete-preview` — shows affected student count/list before deletion.
- `deleteUniversity({universityId, replacementUniversityName, clearLinkedUsers})` → `DELETE /api/staff/universities` — supports reassigning affected students to a replacement university or clearing the link.

#### `EventDetailsPage` — `/staff/events/:eventId` (standalone route, via `DashboardShell`)

The most complex page in the app. Combines:

- **Event metadata edit:** `getEventDetail(eventId)` → `GET /api/staff/events/detail`; `updateEvent({...})` → `PUT /api/staff/events`.
- **Rounds & groups board:**
  - `createEventRound` / `updateEventRound` / `deleteEventRound` / `getEventRoundDetail` → `/api/staff/events/rounds*`.
  - `createEventGroup` / `updateEventGroup` / `deleteEventGroup` → `/api/staff/events/groups*`.
- **Team ↔ group assignment:** `getEventGroupTeams` → `GET /api/staff/events/groups/teams`; `assignTeamToGroup` / `removeTeamFromGroup` → `POST`/`DELETE /api/staff/events/groups/teams` (only `APPROVED` registrations are eligible).
- **Registration approvals:** `changeTeamRegistrationStatus({registrationId, status})` → `PUT /api/staff/team-registration/status`. The registration list does not expose internal registration/team IDs to the UI.
- **GitHub provisioning:** `retryGitHubProvisioning(registrationId)` → `POST /api/github/registrations/{id}/retry`; `updateEventRepoAccess({eventId, grant})` → `PUT /api/staff/events/{id}/github-access`.
- **Mentor/judge assignment editing:** reuses `staffAssignment.js` functions (same as `StaffAssignPage`), scoped to this event's rounds/groups.
- **Awards CRUD:** `createEventAward` / `updateEventAward` / `deleteEventAward` → `/api/staff/events/awards*`.
- **Criteria management:** embeds `CriteriaManager` (see below) per round.

#### `CriteriaManager` (embedded in `EventDetailsPage`)

- `getCriteriaByRound(roundId)` → `GET /api/staff/criteria` (via `criteriaApi.js`'s standalone `request()` helper, not `apiFetch`).
- `createCriteria` / `updateCriteria` / `deleteCriteria` → `POST`/`PUT`/`DELETE /api/staff/criteria`.
- Client-side validation: total criteria weight for a round must not exceed 100% (mirrors backend's `1.00` constraint), shown via a weight bar identical in style to `JudgeCriteriaPanel`'s.

#### `StaffCheckInPage` — `/staff/events/:eventId/check-in` (standalone route, via `DashboardShell`)

- `getCheckInPage(eventId)` → `GET /api/staff/check-in` — loads the roster of teams/members for the event with their current check-in state.
- `setTeamCheckIn({eventId, teamId, checked})` → `PUT /api/staff/check-in/team` — bulk check-in/out for a whole team.
- `setMemberCheckIn({eventId, teamId, userId, checked})` → `PUT /api/staff/check-in/member` — per-member check-in/out.

#### `StaffProfilePage` — `/profile` (lazy-loaded as `ProfilePage`)

Available to **any authenticated role** (`RequireAuth`, not `RequireRole`).

- **On mount:** calls `getProfile()` → `GET /api/auth/profile`. Stores the result as `profileData` state (`{ fullName, email, role, university, studentId, phone, avatarUrl }`).
- **Display logic (read-only section):**
  - All roles: email from `profileData?.email` (falls back to `auth.email`).
  - Students (`STUDENT_FPT`/`STUDENT_EXTERNAL`): `profileData?.university` (Trường) and `profileData?.studentId` (Mã sinh viên).
  - Non-students (Experts, Coordinator): `profileData?.phone` (Số điện thoại). No "Khoa / Phòng" field exists.
- **Edit:** "Chỉnh sửa hồ sơ" button opens `ProfileModal` with `profileData` prop and `onProfileUpdated={setProfileData}` — so the display refreshes immediately after a successful save without another network round-trip.
- **Password change:** "Đổi mật khẩu" button opens `PasswordModal`.

#### ⚠️ `StaffAnnouncementsPage` — defined but **not wired up**

Implements two forms — "Gửi toàn hệ thống" (broadcast) and "Gửi theo sự kiện & vai trò" (targeted by event + recipient role) — calling `sendAnnouncementToAll` / `sendAnnouncementToParticipants` from `api/staff.js`. **It is not included in `StaffLayout`'s `TABS` array and is not routed anywhere**, and the two functions it imports do not exist in `api/staff.js` (see §10).

---

## 8. Chat Subsystem (cross-cutting)

- **REST (`api/chat.js`):**
  - `openChatRoom({eventId, mentorId})` → `POST /api/chat/rooms/open` (get-or-create, student-side).
  - `createChatRoom({eventId, roundId, mentorId})` → `POST /api/chat/rooms` (explicit creation, team-leader only per backend).
  - `listChatRooms({eventId, roundId})` → `GET /api/chat/rooms` (mentor-side room list).
  - `getChatRoom(roomId)` → `GET /api/chat/rooms/{roomId}`.
  - `getChatMessages(roomId)` → `GET /api/chat/rooms/{roomId}/messages` (last 200 messages).
- **Realtime (`hooks/useChatStomp.js`):** SockJS connection to `getWebSocketUrl()` (`${API_BASE}/ws`), STOMP CONNECT with `Authorization: Bearer <token>` header, subscribes to `/topic/chat/{roomId}`, `sendMessage(content)` publishes to `/app/chat.send`.
- **UI surfaces:**
  - `components/chat/ChatPopup.jsx` — the **active** floating chat widget, used by both `StudentDashboard` (`mode='student'`) and `MentorDashboard`/`JudgeDashboard` (`mode='mentor'`). Renders message bubbles with auto-linkified URLs; input is disabled if the room status is `CLOSED`.
  - `components/chat/TeamChatPanel.jsx` — an alternate, fully-built team-side chat panel. **Not imported anywhere** — see §10.

---

## 9. End-to-End User Journeys

### A. Student Registration → Team → Event Participation

1. `/` → `RegisterModal` (2-step OTP, §4) → account created as `STUDENT_FPT`/`STUDENT_EXTERNAL`.
2. `/` → `LoginModal` → `saveAuth` → redirect to `/student`.
3. `teamState === 'no-team'` → `CreateTeamForm` (`createTeam`) or `JoinTeamForm` (`joinTeam`).
4. `JoinEventForm` → `joinEvent({eventId})` → registration created with status `PENDING`.
5. _(Staff side)_ Coordinator approves via `StaffEventsPage`/`EventDetailsPage` → `changeTeamRegistrationStatus` → `APPROVED`, then assigns the team to a round group (`assignTeamToGroup`) and assigns mentors/judges to that group.
6. Back on `/student`, `TeamEventsPanel` now shows `APPROVED`, and `EventMentorsBlock` (`getTeamTrackMentors`) lists the assigned mentor(s).
7. Student opens `ChatPopup` (`mode='student'`) → `openChatRoom({eventId, mentorId})` → chats with the mentor in real time.

### B. Coordinator Event Setup Journey

1. `StaffEventsPage` → `createEvent` (status `BUILDING`). Optional: enter a GitHub Template Repository URL to auto-provision team repos on approval.
2. Navigate to `EventDetailsPage` (`/staff/events/:eventId`) → `createEventRound`, `createEventGroup`.
3. `CriteriaManager` → `createCriteria` per round (weights summing to ≤ 100%).
4. `StaffAssignPage` or `EventDetailsPage` → `assignMentor` / `assignJudge` per round/group.
5. After registrations are `APPROVED`, `EventDetailsPage` → `assignTeamToGroup` to place teams into round groups.
6. `StaffEventsPage` → `changeEventStatus` to advance `BUILDING → UPCOMING → ONGOING → COMPLETED`.
7. On event day, `StaffCheckInPage` (`/staff/events/:eventId/check-in`) → `setTeamCheckIn` / `setMemberCheckIn`.

### C. Judge Scoring Journey

1. Login (`EXPERT_INTERNAL`/`EXPERT_EXTERNAL`) → `/judge`.
2. Pick an assignment (event/round/group) → `JudgeCriteriaPanel` shows the rubric (`getCriteriaForJudge`).
3. `getTeamsToScore` lists teams with submission links (`SubmissionLinks`) and scored/unscored state.
4. Open `JudgeScoreModal` → fill per-criteria scores/feedback → live weighted-total preview → `submitScore` (first time) or `updateScore` (edit).
5. Optionally cross-link to `/mentor` (same `EXPERT_*` account may also be a mentor for the same or other groups).

### D. Mentor Support Journey

1. Login (`EXPERT_INTERNAL`/`EXPERT_EXTERNAL`) → `/mentor`.
2. Pick an assignment → view assigned teams (`getAssignedTeams`, default filter `APPROVED`) and colleagues (`ExpertGroupColleaguesBoard`).
3. Open `ChatPopup` (`mode='mentor'`) → `listChatRooms` → select a room → respond to student messages in real time.
4. Optionally cross-link to `/judge` if also assigned as a judge.

### E. Profile Update Journey (any role)

1. Any authenticated user → navigate to `/profile` (or click "Hồ sơ của tôi" in `AccountDropdown`).
2. `StaffProfilePage` mounts → `getProfile()` fires → `profileData` state populated with DB values.
3. Display shows email, and role-specific fields (university/studentId for students; phone for others).
4. Click "Chỉnh sửa hồ sơ" → `ProfileModal` opens, pre-filled from `profileData` via `useEffect`.
5. User edits fields, submits → `updateProfile(form)` → `PUT /api/auth/profile`.
6. Backend re-issues a JWT with updated claims and returns `{message, newToken}`.
7. Frontend JSON-parses the response, calls `saveAuth({ token: newToken, fullName })`, fires `onProfileUpdated` to update `profileData` in the parent, closes modal.

---

## 10. Gap Analysis & Known Issues (Frontend)

| Priority | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **P0**   | `StaffAnnouncementsPage.jsx` is fully implemented (two forms for broadcast / targeted announcements, matching backend endpoints `POST /api/staff/announcements/send-all` and `/send-participant`) but (a) is **not added to `StaffLayout`'s `TABS`**, so it is unreachable in the UI, and (b) imports `sendAnnouncementToAll`/`sendAnnouncementToParticipants` from `api/staff.js`, **which does not export them** — the page would throw at runtime if ever rendered. |
| **P1**   | No FE implementation for `PUT /api/team/submit-project` (submit GitHub/Demo/Report/Slide links) or `GET /api/team/submissions` (submission history) on the Student side. Judges _read_ these via `SubmissionLinks`/`JudgeScoreModal`, but students have no UI to write them — submission data must currently be seeded/entered another way for judging to be testable end-to-end.                                                                                      |
| **P2**   | `components/chat/TeamChatPanel.jsx` is a complete alternate chat UI (room creation by team leader via event/round/mentor pickers) but is **never imported** — `StudentDashboard` uses `ChatPopup` instead. Likely superseded/dead code, or a pending integration.                                                                                                                                                                                                      |
| **P2**   | `api/criteriaApi.js` bypasses the shared `apiFetch` wrapper with its own `request()` helper, and re-implements `getCriteriaForJudge`/`getCriteriaByEvent` as `@deprecated` duplicates of functions already in `api/judge.js` (which is what the judge UI actually uses). Candidate for cleanup/consolidation.                                                                                                                                                          |
| **P3**   | The "Thông báo" (Notifications) item in `AccountDropdown` is permanently disabled with a "Sắp ra mắt" (Coming Soon) badge — no notification feature exists yet on either side.                                                                                                                                                                                                                                                                                         |

---

## 11. Frontend ↔ Backend Quick Reference

| UI Area                                      | Route                             | Primary API modules                                                   |
| -------------------------------------------- | --------------------------------- | --------------------------------------------------------------------- |
| Landing / Auth modals                        | `/`                               | `publicEvent.js`, `auth.js`, `university.js`                          |
| Student Dashboard                            | `/student`                        | `team.js`, `chat.js`                                                  |
| Mentor Dashboard                             | `/mentor`                         | `mentor.js`, `chat.js`                                                |
| Judge Dashboard                              | `/judge`                          | `judge.js`                                                            |
| Staff: Overview/Accounts/Assign/Universities | `/staff?tab=...`                  | `staff.js`, `staffAssignment.js`, `staffUniversity.js`                |
| Staff: Events list                           | `/staff?tab=events`               | `event.js`, `staff.js`                                                |
| Event Details                                | `/staff/events/:eventId`          | `event.js`, `eventService.js`, `staffAssignment.js`, `criteriaApi.js` |
| Check-in                                     | `/staff/events/:eventId/check-in` | `checkIn.js`                                                          |
| Profile (any role)                           | `/profile`                        | `auth.js`                                                             |
