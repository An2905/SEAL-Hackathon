# GitHub OAuth

Tài liệu giải thích flow liên kết tài khoản GitHub của sinh viên — so sánh với API REST thông thường, response nhận gì ở từng bước, và dữ liệu ghi vào DB thế nào.

---

## 0) OAuth vs API REST bình thường

### API REST (ví dụ đăng nhập, cập nhật profile)

```
FE ── POST /api/auth/login ──► BE
FE ◄── JSON { token } ──────── BE
```

- Gọi xong **trên cùng trang**, nhận JSON, xử lý ngay.
- FE gửi body, BE trả body.

### GitHub OAuth (liên kết tài khoản)

```
FE ── GET /api/auth/github/link-url ──► BE
FE ◄── JSON { authorizeUrl } ───────── BE

FE redirect trình duyệt ──────────────► GitHub (trang ngoài)
User bấm "Authorize" trên GitHub

GitHub redirect trình duyệt ──────────► BE /api/auth/github/callback?code=...&state=...
BE xử lý server-to-server với GitHub, lưu DB

BE redirect trình duyệt (302) ───────► FE /student?github_oauth=success&github_username=...
FE đọc query string trên URL, hiện toast
```

**Khác REST ở đâu:**

| | REST thường | GitHub OAuth |
|---|-------------|--------------|
| Số "chặng" | 1 request → 1 response | Nhiều chặng, có redirect qua GitHub |
| Ai gọi callback? | FE gọi BE bằng `fetch` | **GitHub** redirect trình duyệt về BE |
| Kết quả cuối về FE | JSON trong `fetch` | **Redirect URL** + query string (`?github_oauth=...`) |
| Secret GitHub | Không liên quan | `client_secret` chỉ BE dùng, **không** gửi ra FE |

**Mục đích trong project:** Sinh viên bấm "Liên kết GitHub" → xác nhận trên GitHub → BE lưu `github_username` + `github_id` vào bảng `users` (không phải đăng nhập bằng GitHub).

---

## 1) GitHub OAuth dùng để làm gì?

- Cho phép sinh viên **xác thực** tài khoản GitHub thật (OAuth), thay vì tự gõ username.
- BE lấy từ GitHub: `login` (username) và `id` (số định danh GitHub).
- Lưu vào DB để biết sinh viên nào đã liên kết GitHub nào.

**Không làm:**

- Không dùng GitHub để đăng nhập app (login vẫn bằng email/password).
- Không lưu `access_token` GitHub vào DB (chỉ dùng tạm để gọi API user, rồi bỏ).

**UI hiện tại:** Nút "Liên kết GitHub" trên `StudentDashboard.jsx`.

**Cách khác cập nhật username (không qua OAuth):** `PUT /api/auth/profile` có thể sửa `githubUsername` thủ công — nhưng **không** set `github_id`. Chỉ flow OAuth mới ghi cả hai cột.

---

## 2) Cấu hình môi trường

Backend (`application-dev.properties` / biến môi trường):

```properties
github.client.id=${GITHUB_CLIENT_ID:}
github.client.secret=${GITHUB_CLIENT_SECRET:}
github.redirect.uri=${GITHUB_REDIRECT_URI:http://localhost:8080/api/auth/github/callback}
github.frontend.redirect=${GITHUB_FRONTEND_REDIRECT:http://localhost:5173/student}
```

| Biến | Ai dùng | Ý nghĩa |
|------|---------|---------|
| `GITHUB_CLIENT_ID` | FE (trong URL) + BE | App ID trên GitHub |
| `GITHUB_CLIENT_SECRET` | **Chỉ BE** | Đổi `code` lấy token — không expose ra FE |
| `GITHUB_REDIRECT_URI` | GitHub → BE | URL callback đăng ký trên GitHub OAuth App |
| `GITHUB_FRONTEND_REDIRECT` | BE → FE | Trang FE nhận kết quả sau khi xong |

Trên GitHub OAuth App settings, **Authorization callback URL** phải khớp `github.redirect.uri`.

File chính:

- FE: `frontend/src/api/auth.js` → `getGithubLinkUrl()`, `StudentDashboard.jsx`
- BE: `GitHubOauthController.java`, `GithubOauthService.java`, `UserRepository.java`

---

## 3) Dữ liệu lưu ở đâu?

### Vào DB (lâu dài) — bảng `users`

| Cột | Nguồn | Ví dụ |
|-----|-------|-------|
| `github_username` | GitHub API field `login` | `"nguyenvana"` |
| `github_id` | GitHub API field `id` | `12345678` |

SQL thực tế (`UserRepository.updateGithubProfileIfNotLinked`):

```sql
UPDATE users
SET github_username = ?, github_id = ?
WHERE user_id = ?
```

`user_id` lấy từ JWT lúc bắt đầu flow (mã hóa bên trong mã thông báo `state` dạng JWT, xem bước 4 Flow 1).

### Mã hóa phi trạng thái (tạm, ~5 phút) — Signed JWT State

Mã thông báo `state` gửi sang GitHub là một chuỗi JWT ngắn hạn được ký bằng thuật toán HMAC-SHA256 với khóa `JWT_SECRET_KEY` của ứng dụng.

| Claims trong State JWT | Loại dữ liệu | Mục đích |
|-------------|---------|----------|
| `userId` | `String` | Xác định user thực hiện liên kết tài khoản khi nhận callback |
| `exp` | `Long` (Timestamp) | Đảm bảo mã thông báo tự động hết hạn sau 5 phút |

Sau khi nhận được callback từ GitHub, Backend giải mã và xác thực chữ ký JWT để lấy lại `userId` trực tiếp từ tham số `state` mà không cần lưu giữ trạng thái session trên server.

### Không lưu

| Dữ liệu | Vì sao |
|---------|--------|
| `code` từ GitHub | Dùng 1 lần đổi token, xong bỏ |
| `access_token` GitHub | Chỉ gọi `GET api.github.com/user`, không persist |
| `state` | Chỉ chống CSRF và lưu `userId` phi trạng thái trong lúc redirect |

---

## 4) Response / nhận gì ở từng bước?

So sánh nhanh từng chặng:

| Bước | Gọi ai | Request | Response nhận được |
|------|--------|---------|-------------------|
| 1 | BE | `GET /api/auth/github/link-url` + JWT | JSON `{ "authorizeUrl": "https://github.com/login/oauth/authorize?..." }` |
| 2 | GitHub (trình duyệt) | Mở `authorizeUrl` | Trang GitHub — user bấm Authorize |
| 3 | BE (từ GitHub redirect) | `GET /api/auth/github/callback?code=...&state=...` | **302 redirect** tới FE, không phải JSON |
| 3b | GitHub API (BE gọi ngầm) | `POST .../oauth/access_token` | JSON `{ "access_token": "...", ... }` |
| 3c | GitHub API (BE gọi ngầm) | `GET api.github.com/user` | JSON `{ "login": "...", "id": 123, ... }` |
| 4 | FE | Load URL redirect | Query: `?github_oauth=success&github_username=...` hoặc `?github_oauth=error&message=...` |

**Lưu ý bảo mật phi trạng thái (Stateless):** Vì cơ chế `state` hiện tại sử dụng JWT ký số chứa sẵn `userId`, Backend không cần đọc hoặc ghi session. Điều này giải quyết triệt để lỗi mất session khi redirect chéo tên miền hoặc khi scale ứng dụng trên các cloud server phân tán.

---

## 5) Luồng đầy đủ — từng bước

### Flow 1: Sinh viên bấm "Liên kết GitHub"

**UI:** `StudentDashboard.jsx` → `handleConnectGithub()`

1. **`StudentDashboard.jsx`** — User bấm nút. Gọi `getGithubLinkUrl()`.
2. **`auth.js` → `getGithubLinkUrl()`** — `GET /api/auth/github/link-url` kèm `Authorization: Bearer <JWT>`.
3. **`GitHubOauthController.getGithubLinkUrl(...)`** — Nhận request REST, chuyển xuống `GithubOauthService`.
4. **`GithubOauthService.buildAuthorizeUrl(...)`** — Kiểm tra role sinh viên (`STUDENT_FPT` / `STUDENT_EXTERNAL`).
   - Tạo `state` bằng cách đóng gói `userId` vào JWT ký bằng `JWT_SECRET_KEY`, hết hạn sau 5 phút.
   - Ghép URL GitHub: `client_id`, `redirect_uri`, `scope=read:user`, `state`.
5. **Response về FE** — JSON:

   ```json
   { "authorizeUrl": "https://github.com/login/oauth/authorize?client_id=...&state=..." }
   ```

6. **`StudentDashboard.jsx`** — `window.location.href = authorizeUrl` → **rời app**, sang trang GitHub.

**Chưa ghi DB** ở bước này. Không lưu thông tin tạm nào trên bộ nhớ server.

---

### Flow 2: User xác nhận trên GitHub

**Không qua code project** — user thao tác trên github.com.

- GitHub hiện màn hình "Authorize application".
- User đồng ý → GitHub redirect trình duyệt về:

  ```
  GET {github.redirect.uri}?code=XXXXXXXX&state=YYYYYYYY
  ```

- `code`: mã dùng **một lần**, hết hạn nhanh.
- `state`: phải khớp session đã lưu ở Flow 1.

**FE không tham gia bước này** — trình duyệt gọi thẳng BE.

---

### Flow 3: Backend xử lý callback

**Endpoint:** `GET /api/auth/github/callback`

1. **`GitHubOauthController.githubCallback(...)`** — Nhận `code`, `state` từ query string (caller là **trình duyệt** sau redirect GitHub).
2. **`GithubOauthService.processCallback(code, state)`** — Xử lý tuần tự:

   **3a. Validate và giải mã `state`**
   - Đọc và verify chữ ký JWT của tham số `state` bằng `JWT_SECRET_KEY`.
   - Nếu chữ ký không đúng hoặc đã quá hạn 5 phút → Báo lỗi (Nếu lỗi do hết hạn, vẫn cố gắng đọc `userId` từ token hết hạn để tìm đúng origin redirect của Dashboard).
   - Lấy `userId` trực tiếp từ claim `"userId"` của `state`.

   **3b. Đổi `code` lấy token** (`exchangeCodeForAccessToken`)
   - BE → GitHub: `POST https://github.com/login/oauth/access_token`
   - Body: `client_id`, `client_secret`, `code`, `redirect_uri`
   - GitHub trả JSON:

     ```json
     { "access_token": "gho_...", "token_type": "bearer", "scope": "read:user" }
     ```

   - BE chỉ lấy `access_token`, **không lưu DB**.

   **3c. Lấy thông tin user GitHub** (`fetchGithubUser`)
   - BE → GitHub: `GET https://api.github.com/user` + `Authorization: Bearer {access_token}`
   - GitHub trả JSON (rút gọn):

     ```json
     { "login": "nguyenvana", "id": 12345678, ... }
     ```

   - BE map thành `GithubUser(username, githubId)`.

   **3d. Ghi DB**
   - `UserRepository.updateGithubProfileIfNotLinked(userId, username, githubId)`
   - UPDATE `users` set `github_username`, `github_id` cho đúng `user_id`.
   - Không có row / update fail → lỗi.

3. **Response về trình duyệt** — **Không phải JSON**. BE trả **HTTP 302** + header `Location`:

   **Thành công:**
   ```
   http://localhost:5173/student?github_oauth=success&github_username=nguyenvana
   ```

   **Lỗi:**
   ```
   http://localhost:5173/student?github_oauth=error&message=...
   ```

So với REST: thay vì `{ "success": true }`, OAuth redirect dùng **query string trên URL** để FE biết kết quả.

---

### Flow 4: Frontend nhận kết quả

**UI:** `StudentDashboard.jsx` — `useEffect` đọc `location.search`

1. Trình duyệt load `/student?github_oauth=success&github_username=...` (sau redirect BE).
2. **`useEffect`** — Parse query:
   - `github_oauth=success` → toast "Đã liên kết GitHub: {username}"
   - `github_oauth=error` → toast lỗi từ `message`
3. **`navigate('/student', { replace: true })`** — Xóa query khỏi URL (tránh refresh hiện toast lại).

**FE không gọi thêm API** để lấy kết quả — thông tin đã nằm trên URL redirect. Dữ liệu chính thức đã nằm trong DB từ Flow 3.

---

## 6) Sơ đồ tổng thể

```
[StudentDashboard]
       │
       │ ① GET /api/auth/github/link-url  (JWT)
       ▼
[GitHubOauthController] → [GithubOauthService] → Sinh state = JWT(userId)
       │
       │ ② JSON { authorizeUrl }
       ▼
[Trình duyệt] ──redirect──► [GitHub] user Authorize
       │
       │ ③ GET /callback?code&state
       ▼
[GithubOauthService] → Giải mã state JWT để lấy userId
       ├── POST GitHub /access_token  → access_token (tạm)
       ├── GET GitHub /user           → login, id
       ├── UPDATE users      → github_username, github_id
       └── 302 → FE ?github_oauth=...
       │
       ▼
[StudentDashboard] đọc query → toast → xóa query
```

---

## 7) So sánh: cập nhật GitHub qua Profile API

| | OAuth (nút Liên kết GitHub) | `PUT /api/auth/profile` |
|---|------------------------------|-------------------------|
| Cách gọi | Redirect nhiều bước | 1 request REST |
| `github_username` | Từ GitHub API (`login`) | User gõ trong form |
| `github_id` | Có, từ GitHub API (`id`) | **Không** cập nhật |
| Xác thực GitHub | Có (user login GitHub) | Không |

---

## 8) Checklist khi sửa / debug OAuth

- `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` đã set trên BE.
- Callback URL trên GitHub App khớp `github.redirect.uri`.
- `github.frontend.redirect` trỏ đúng trang FE (dev: `http://localhost:5173/student`).
- FE gọi `link-url` kèm header `Authorization: Bearer <JWT>`.
- User là sinh viên (`STUDENT_FPT` / `STUDENT_EXTERNAL`).
- Đã có row `users` cho `user_id` (tạo lúc đăng ký).
- `state` (JWT) hết hạn 5 phút → phải bấm "Liên kết GitHub" lại từ đầu.

---

## 9) Lưu ý dễ nhầm

1. **OAuth ≠ đăng nhập** — JWT app vẫn là email/password; OAuth chỉ **liên kết** GitHub vào profile.
2. **Callback về BE, không về FE** — GitHub gọi `localhost:8080/.../callback`, không phải `5173`.
3. **Kết quả cuối không phải JSON** — FE đọc `?github_oauth=` trên URL, giống redirect sau thanh toán, khác `fetch` REST.
4. **`client_secret` chỉ ở BE** — FE chỉ nhận `authorizeUrl` (có `client_id` công khai).
5. **Token GitHub không lưu** — Mỗi lần liên kết lại phải qua OAuth; app không giữ quyền truy cập GitHub lâu dài.
6. **Scope `read:user`** — Chỉ đọc thông tin public profile (`login`, `id`), không đụng repo.
