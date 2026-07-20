# Báo cáo thay đổi commit

**Branch:** `feature/github-app`  
**Ngày:** 20/07/2026  
**Commit message đề xuất:**

```
feat: automate round lifecycle, security fixes, and FE integration hardening
```

> Tài liệu này mô tả **cụ thể các file đã sửa/thêm** trong commit hiện tại để team review nhanh.  
> Không phải báo cáo đánh giá tổng thể — xem riêng `docs/CODE_REVIEW_REPORT.md` nếu cần.

---

## 1. Tóm tắt nhanh

| Nhóm | Mục đích |
|------|----------|
| **Round lifecycle (mới)** | Scheduler tự động xử lý 3 mốc vòng: bắt đầu → hết hạn nộp bài → kết thúc vòng |
| **GitHub & judging** | Mở/khóa quyền repo theo vòng; tạo submission; phân judge round-robin; chọn winner |
| **Bảo mật** | JWT filter toàn cục; kiểm tra quyền subscribe WebSocket chat; OTP rate-limit |
| **FE integration** | Sửa bug assign judge, export Excel prod, parsing response mutation |
| **FE UX** | `pendingTeams` trong list API; xem commit trong modal chi tiết đội |

---

## 2. Luồng tự động vòng đấu (mới)

Scheduler `GitHubRepoAccessScheduler` (mỗi 60s) gọi `RoundLifecycleService.processDueMilestones()`.

```
start_date đến hạn          → STARTED
submission_deadline đến hạn → SUBMISSION_CLOSED
end_date đến hạn            → ENDED
```

Mỗi mốc chỉ chạy **một lần** (ghi vào bảng `round_lifecycle_milestones`).

### Khi vòng BẮT ĐẦU (`STARTED`)

1. `EventService.autoFillRoundGroupsForLifecycle()` — tự gán đội vào bảng (chỉ từ scheduler, không chạy khi mở trang FE).
2. `StaffService.updateRoundTeamRepoAccess(roundId, grant=true)` — cấp quyền ghi GitHub cho thành viên đội trong vòng.

### Khi HẾT HẠN NỘP BÀI (`SUBMISSION_CLOSED`)

1. `updateRoundTeamRepoAccess(roundId, grant=false, keepReadOnly=true)` — chuyển repo sang **read-only**.
2. `SubmissionRepository.createSubmissionsForRound()` — tạo bản ghi `submissions` (status `SUBMITTED`) từ `github_repo_url`.
3. `JudgeRepositoryProvisioningService.provisionRoundForJudging()` — phân judge round-robin theo group, cấp read-only repo, lưu `judge_team_assignments`.

### Khi vòng KẾT THÚC (`ENDED`)

1. `JudgeRepositoryProvisioningService.revokeJudgesFromRound()` — thu hồi quyền judge trên repo.
2. `RoundWinnerRepository.finalizeWinnersForRound()` — xếp hạng theo điểm TB judge, ghi `round_winners`, đẩy winner sang vòng tiếp theo (nếu có).

---

## 3. Backend — File mới

| File | Mô tả |
|------|--------|
| `service/RoundLifecycleService.java` | Orchestrator 3 mốc vòng (start / submission close / end) |
| `repository/RoundLifecycleRepository.java` | Query vòng đến hạn; đánh dấu milestone; lấy team trong vòng |
| `repository/SubmissionRepository.java` | Tạo submission snapshot khi hết hạn nộp bài |
| `repository/RoundWinnerRepository.java` | Tính ranking & insert `round_winners` |
| `security/JwtAuthenticationFilter.java` | Parse Bearer JWT, set Spring Security context |
| `database/migrations/20260720_round_lifecycle_milestones.sql` | Migration bảng `round_lifecycle_milestones` |

---

## 4. Backend — File đã sửa

| File | Thay đổi chính |
|------|----------------|
| `service/GitHubRepoAccessScheduler.java` | **Thay logic cũ** (scan round completed + grant repo) bằng gọi `RoundLifecycleService` |
| `service/StaffService.java` | Thêm `updateRoundTeamRepoAccess()` — grant/revoke/read-only repo theo **round** (dùng bởi lifecycle) |
| `service/JudgeRepositoryProvisioningService.java` | Thêm `provisionRoundForJudging()`, `revokeJudgesFromRound()` — phân judge + GitHub read-only |
| `service/EventService.java` | Thêm `autoFillRoundGroupsForLifecycle()`; bỏ `syncAutoEventStatuses()` trên `getAllEvents` / `getEventDetail` |
| `repository/JudgeTeamAssignmentRepository.java` | Query team chưa có judge; tạo assignment; revoke theo round |
| `repository/EventRepository.java` | `findAllByStatus` JOIN `team_registrations` → trả thêm `pending_teams` |
| `model/dto/response/EventSummaryResponse.java` | Field mới `pendingTeams` |
| `model/mapper/EventMapper.java` | Map `pending_teams` từ ResultSet → DTO |
| `config/SecurityConfig.java` | **Trước:** `permitAll()`. **Sau:** JWT filter + `/api/**` yêu cầu authenticated; whitelist login/register/public |
| `config/WebSocketAuthInterceptor.java` | Thêm kiểm tra **SUBSCRIBE** `/topic/chat/{roomId}` — phải là member phòng |
| `service/AuthService.java` | Re-check account `APPROVED` mỗi `validateRole()`; OTP rate-limit (3 lần/15 phút); max 5 lần nhập sai OTP; reset password không tiết lộ email |
| `exception/GlobalExceptionHandler.java` | Lỗi 500 không còn trả `ex.getMessage()` ra client |
| `database/scripts/schema.sql` | Thêm định nghĩa bảng `round_lifecycle_milestones` |

---

## 5. Database

### Bảng mới: `round_lifecycle_milestones`

| Cột | Ý nghĩa |
|-----|---------|
| `round_id` | FK → `rounds` |
| `milestone` | `STARTED` \| `SUBMISSION_CLOSED` \| `ENDED` |
| `processed_at` | Thời điểm xử lý |

**Deploy:** chạy `database/migrations/20260720_round_lifecycle_milestones.sql` trên DB đã có, hoặc init mới từ `schema.sql`.

---

## 6. Frontend — File đã sửa

| File | Thay đổi chính |
|------|----------------|
| `api/staff.js` | **`assignJudge`:** body gửi `userId` (khớp BE) thay vì `judgeId`. Bỏ regex check message EN sau mutation — tin HTTP 2xx. **`exportEventsExcel`:** dùng `apiFetchBlob()` + `VITE_API_BASE` (fix Vercel prod) |
| `api/staffAssignment.js` | Bỏ regex check message EN khi xóa assignment mentor/judge |
| `api/client.js` | Thêm `parseMessageResponse()`, `apiFetchBlob()`; logic JWT/expiry giữ nguyên |
| `api/event.js` | `attachPendingTeamsToEvents()` không còn gọi N× `getEventDetail` — dùng `pendingTeams` từ list API |
| `api/normalizers.js` | `mapEventRow()` map thêm `pendingTeams` / `pending_teams` |
| `pages/dashboards/EventDetailsPage.jsx` | Nút **Xem Commits** chuyển vào modal **Chi tiết đội**; bỏ auto-fill khi load trang; xóa dead code `commitModals` / bulk picker |
| `hooks/useChatStomp.js` | Kiểm tra JWT hết hạn trước khi connect STOMP |
| `package.json` | Scripts `test`, `test:watch`; devDependency `vitest` |
| `vite.config.js` | Cấu hình Vitest (`environment: node`) |

---

## 7. Hành vi người dùng thấy khác

| Khu vực | Trước | Sau |
|---------|-------|-----|
| Phân công giám khảo (Staff) | Có thể fail im lặng (sai field `judgeId`) | Gửi đúng `userId`, API thành công |
| Export Excel (Staff, prod) | Gọi sai origin trên Vercel | Gọi đúng backend qua `VITE_API_BASE` |
| Danh sách sự kiện (Staff) | Load chậm (N request detail để đếm pending) | `pendingTeams` có sẵn trong list |
| Chi tiết sự kiện | Auto-fill bảng mỗi lần mở trang | Chỉ auto-fill qua scheduler khi vòng bắt đầu |
| Xem commit GitHub | Nút ở header danh sách đội | Nút trong modal **Chi tiết ›** từng đội |
| API không có token | Vẫn vào được endpoint (BE permitAll) | `/api/**` trả 401 nếu thiếu/sai JWT |
| Chat WebSocket | Subscribe phòng bất kỳ nếu biết `roomId` | Phải là member mới subscribe được |
| Vòng đấu | Staff thao tác thủ công nhiều bước | Tự động: repo access → submission → judge → winners |

---

## 8. Checklist review cho team

- [ ] Đã chạy migration `20260720_round_lifecycle_milestones.sql` trên DB staging/prod
- [ ] Scheduler chạy đúng timezone (`VietnamTime.nowForDatabase()`)
- [ ] Test flow: tạo vòng có `start_date` / `submission_deadline` / `end_date` → quan sát milestone + GitHub access
- [ ] Staff: phân công judge, export Excel, đổi trạng thái đội
- [ ] Mở chi tiết đội → **Xem Commits** (cần GitHub SUCCESS + repo URL)
- [ ] `cd frontend && npm test` pass
- [ ] `cd frontend && npm run build` pass

---

*Cập nhật: 20/07/2026 — đồng bộ với working tree trên branch `feature/github-app`.*
