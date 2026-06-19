# WebSocket

Tài liệu giải thích chat realtime trong project — so sánh với API REST thông thường để dễ hình dung.

---

## 0) REST vs WebSocket — khác nhau thế nào?

### API REST (request/response bình thường)

Giống **gọi điện thoại từng cuộc**:

1. FE gửi **1 request** (ví dụ `POST /api/auth/login`).
2. BE xử lý xong, trả **1 response** (JSON).
3. Kết nối **kết thúc**. Muốn biết tin mới → phải gọi lại (hoặc bấm F5).

```
FE  ──request──►  BE
FE  ◄─response──  BE
     (xong, ngắt)
```

Ví dụ trong project:

| Việc | REST |
|------|------|
| Đăng nhập | `POST /api/auth/login` → nhận token |
| Tải lịch sử chat | `GET /api/chat/rooms/{roomId}/messages` → nhận mảng tin nhắn |
| Mở phòng chat | `POST /api/chat/rooms/open` → nhận `roomId` |

Mỗi lần gọi: FE chủ động hỏi, BE trả lời **một lần**, xong.

---

### WebSocket (chat realtime)

Giống **giữ máy** sau khi đã kết nối:

1. FE và BE mở **một đường dây** (kết nối `/ws`), giữ mở lâu.
2. Ai cũng có thể **đẩy dữ liệu** bất cứ lúc nào — không cần FE hỏi trước.
3. Khi mentor gửi tin → BE **tự đẩy** tin đó xuống FE của student (và ngược lại).

```
FE  ═════ kết nối mở ═════  BE
     ◄── tin mới từ BE (push)
     ──► tin mới từ FE (gửi)
```

**Không có** kiểu "gửi request → chờ response rồi đóng" cho từng tin nhắn. Thay vào đó là **luồng hai chiều liên tục**.

---

### Project dùng cả hai — mỗi thứ một việc

| Việc cần làm | Dùng gì? | Vì sao? |
|--------------|----------|---------|
| Mở phòng, tạo phòng, list phòng | **REST** | Chỉ cần làm 1 lần, trả kết quả xong |
| Tải tin nhắn cũ | **REST** | Lấy dữ liệu từ DB, giống API GET bình thường |
| Gửi tin mới | **WebSocket** | Gửi nhanh, không reload |
| Nhận tin người khác vừa gửi | **WebSocket** | BE **chủ động đẩy** xuống FE — REST không làm được kiểu này (trừ khi FE cứ 1 giây gọi GET một lần) |

**Tóm lại:** REST = hỏi–đáp một lần. WebSocket = đường dây mở, ai cũng có thể nói bất cứ lúc nào.

---

## 1) WebSocket trong project dùng để làm gì?

Chat mentor ↔ đội: khi A gửi tin, B thấy ngay trên màn hình.

Dùng ở 3 chỗ:

- Student chat mentor (`ChatPopup` trên dashboard).
- Mentor trả lời đội (`ChatPopup` mode mentor).
- Team leader tạo phòng chat (`TeamChatPanel`).

---

## 2) So sánh cụ thể: một tin nhắn chat

### Nếu chỉ dùng REST (không có WebSocket)

```
Student gửi tin:
  FE → POST /api/chat/messages  { roomId, content }
  FE ← 200 OK  { messageId, content, ... }

Mentor muốn thấy tin mới:
  FE → GET /api/chat/rooms/xxx/messages   (phải gọi lại)
  FE ← 200 OK  [ ..., tin vừa gửi ]
```

Mentor phải **liên tục gọi GET** (polling) mới biết có tin mới → tốn tài nguyên, chậm.

### Cách project đang làm (REST + WebSocket)

**Bước chuẩn bị (REST — giống API bình thường):**

```
FE → GET /api/chat/rooms/abc/messages
FE ← [ tin 1, tin 2, tin 3 ]     ← lịch sử cũ
```

**Bước realtime (WebSocket — khác REST):**

```
FE ═══ CONNECT /ws (gửi JWT, giữ kết nối) ═══ BE

FE ── subscribe ──►  "tôi muốn nghe phòng abc"
     (đăng ký nghe kênh /topic/chat/abc)

Student gửi tin "Hello":
  FE ── publish ──►  /app/chat.send  { roomId: "abc", content: "Hello" }
  BE: lưu DB, rồi broadcast xuống kênh abc
  FE ◄── push ────  { messageId, content: "Hello", ... }   ← cả student lẫn mentor đều nhận
```

Không ai cần gọi GET lại. BE **đẩy** tin mới xuống mọi người đang "nghe" phòng đó.

---

## 3) Cấu hình

```properties
# FE
VITE_API_BASE=http://localhost:8080

# BE
app.cors.allowed-origins=http://localhost:5173
```

Dev: Vite proxy `/ws` → backend (`frontend/vite.config.js`, cần `ws: true`).

**Ba "địa chỉ" STOMP cần nhớ** (so với REST path):

| STOMP destination | Giống REST kiểu... | Thực tế làm gì |
|-------------------|-------------------|----------------|
| `/ws` | `http://host/api/...` (URL gốc) | Mở kết nối ban đầu |
| `/app/chat.send` | `POST /api/...` (FE gửi lên) | FE gửi tin nhắn mới |
| `/topic/chat/{roomId}` | Không có tương đương REST | BE **push** tin xuống FE — REST không có "subscribe" |

File chính:

- FE: `frontend/src/hooks/useChatStomp.js`, `frontend/src/api/chat.js`
- BE: `WebSocketConfig.java`, `WebSocketAuthInterceptor.java`, `ChatWebSocketController.java`, `ChatService.java`

---

## 4) Frontend làm gì? (nói đơn giản)

### Phần giống REST

Trước khi chat realtime, FE vẫn gọi API bình thường:

- `openChatRoom` / `listChatRooms` / `createChatRoom` → có `roomId`
- `getChatMessages(roomId)` → tải tin cũ (giống `GET` trả JSON)

### Phần khác REST (WebSocket)

1. **Mở đường dây:** `useChatStomp` kết nối `/ws`, gửi JWT (như header `Authorization` của REST).
2. **Đăng ký nghe phòng:** subscribe `/topic/chat/{roomId}` — tương đương "bật loa" để nghe phòng đó. REST không có bước này.
3. **Gửi tin:** `sendMessage(text)` → publish `/app/chat.send` — giống `POST` nhưng qua đường dây đang mở, **không** nhận response trực tiếp cho riêng tin đó.
4. **Nhận tin:** callback `onMessage` — BE **tự gọi về** khi có tin mới (push). REST thì FE phải tự `GET`.
5. **Đóng:** rời phòng / đóng popup → `deactivate()` ngắt kết nối.

---

## 5) Backend làm gì? (nói đơn giản)

### REST (`ChatController` — quen thuộc)

```
Request vào → validate JWT → ChatService → trả JSON → xong
```

Ví dụ: `GET /api/chat/rooms/{roomId}/messages` → trả list tin, **không** push gì thêm.

### WebSocket (khác REST)

**Lúc CONNECT (mở đường dây):**

```
FE gửi CONNECT + Bearer token
→ WebSocketAuthInterceptor kiểm tra JWT (giống middleware REST)
→ OK thì ghi nhớ userId cho session này
```

Chỉ check token **một lần lúc mở**, không check lại từng tin (nhưng `ChatService` vẫn check quyền theo `roomId`).

**Lúc gửi tin (`/app/chat.send`):**

```
FE publish { roomId, content }
→ ChatWebSocketController nhận (không phải @PostMapping — là @MessageMapping)
→ ChatService: check quyền, lưu DB
→ SimpMessagingTemplate broadcast lên /topic/chat/{roomId}
→ Mọi FE đang subscribe phòng đó nhận tin (push)
```

**Điểm khác REST quan trọng:** Sau khi lưu tin, BE **không** trả response cho riêng người gửi qua cùng một "request". Thay vào đó BE **phát** tin lên topic — người gửi cũng nhận lại qua subscription (giống người khác).

---

## 6) Luồng từng use case

### Flow 1: Student mở chat mentor

| Bước | Ai | Giống REST hay WebSocket? | Làm gì |
|------|-----|---------------------------|--------|
| 1 | `ChatPopup.jsx` | — | User bấm chat, mở popup |
| 2 | `chat.js` | **REST** `POST /api/chat/rooms/open` | Xin mở phòng → nhận `roomId` |
| 3 | `ChatController` → `ChatService` | **REST** | Kiểm tra đội, mentor; tạo/tìm phòng |
| 4 | `chat.js` | **REST** `GET .../messages` | Tải tin cũ |
| 5 | `useChatStomp` | **WebSocket** CONNECT | Mở đường dây, gửi JWT |
| 6 | `WebSocketAuthInterceptor` | **WebSocket** | Duyệt token lúc CONNECT |
| 7 | `useChatStomp` | **WebSocket** SUBSCRIBE | Đăng ký nghe `/topic/chat/{roomId}` |

Sau bước 7: UI hiện "Đã kết nối". Từ đây tin mới đi qua WebSocket, không qua REST.

---

### Flow 2: Mentor vào chat

Giống Flow 1, chỉ khác bước mở phòng:

| Bước | Khác Flow 1 |
|------|-------------|
| 2 | **REST** `GET /api/chat/rooms` — list phòng mentor tham gia (không gọi `open`) |
| 5–7 | WebSocket giống hệt |

---

### Flow 3: Team leader tạo phòng

| Bước | Ai | Loại | Làm gì |
|------|-----|------|--------|
| 1 | `TeamChatPanel.jsx` | — | Chọn event, round, mentor, bấm tạo |
| 2 | `chat.js` | **REST** `POST /api/chat/rooms` | Tạo phòng mới |
| 3 | `ChatService.createRoom` | **REST** | Chỉ leader; thêm member đội + mentor |
| 4 | `getChatMessages` | **REST** | Tải lịch sử (thường rỗng) |
| 5–7 | `useChatStomp` | **WebSocket** | CONNECT + subscribe |

---

### Flow 4: Gửi tin (so sánh trực tiếp REST)

**REST (project không dùng cho gửi tin):**

```
POST /api/chat/messages
Body: { roomId, content }
Response: { messageId, content, senderId, ... }   ← chỉ người gửi nhận
```

**WebSocket (project đang dùng):**

| Bước | Làm gì |
|------|--------|
| 1 | User bấm Gửi → `sendMessage(text)` |
| 2 | FE publish `/app/chat.send` + `{ roomId, content }` — **không** chờ response kiểu HTTP |
| 3 | `ChatWebSocketController` nhận, lấy `userId` từ session CONNECT |
| 4 | `ChatService.sendMessage` — validate, lưu DB |
| 5 | BE broadcast lên `/topic/chat/{roomId}` — **mọi** client đang nghe đều nhận |
| 6 | FE `onMessage` → thêm tin vào UI |

Người gửi **không** nhận tin qua "response của publish", mà nhận qua **cùng kênh push** với người khác.

---

### Flow 5: Nhận tin từ người khác

**REST:** FE phải tự `GET /messages` lặp lại.

**WebSocket:** Không làm gì thêm — đã subscribe từ Flow 1–3 thì BE tự push:

1. Người kia gửi tin (Flow 4).
2. BE broadcast.
3. `useChatStomp` bắt tin → `handleIncoming` → UI cập nhật.

---

## 7) Auth: REST vs WebSocket

| | REST | WebSocket |
|---|------|-----------|
| Khi nào gửi token? | **Mỗi** request (`Authorization` header) | Chủ yếu lúc **CONNECT** |
| Token sai | 401, request fail | CONNECT fail, không vào được |
| Sau khi vào | Mỗi API check lại | Session nhớ user; mỗi tin `ChatService` vẫn check member phòng |

---

## 8) Mở rộng tính năng realtime mới

**Backend:**

- REST: thêm `@PostMapping` / `@GetMapping` → trả JSON.
- WebSocket: thêm `@MessageMapping` (nhận từ FE) + `convertAndSend` (push xuống FE).

**Frontend:**

- REST: `apiFetch('/api/...')`.
- WebSocket: `client.publish(...)` gửi + `client.subscribe(...)` nhận push.

---

## 9) Checklist nhanh

- Việc **một lần** (tạo, list, tải cũ) → REST.
- Việc **liên tục** (gửi/nhận tin mới) → WebSocket.
- Mở phòng xong phải `getChatMessages` (REST) trước khi subscribe (WS).
- Đổi phòng / đóng UI → ngắt WebSocket (`deactivate`).
- Phòng `CLOSED`: REST vẫn xem lịch sử; WebSocket chặn gửi mới.

---

## 10) Lưu ý dễ nhầm

1. **WebSocket không thay REST** — chỉ thay phần "ai gửi tin thì người kia thấy ngay".
2. **Gửi tin WS không có response 1-1** như `POST` — tin quay về qua kênh subscribe.
3. **Tin cũ = REST, tin mới = WebSocket** — mở chat lần đầu vẫn thấy lịch sử vì đã `GET messages`.
4. **Token hết hạn** — kết nối WS có thể rớt; cần đăng nhập lại (hook có reconnect nhưng không tự refresh token).
5. **Dev** — proxy `/ws` phải bật `ws: true` trong Vite.
