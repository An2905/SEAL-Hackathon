// =====================================================
// Shared auth utilities used by all pages.
// =====================================================

const API_BASE = "http://localhost:8080";

const RoleHome = {
    Student: "student.html",
    Staff: "staff.html",
    Mentor: "mentor.html",
    Judge: "judge.html",
};

const Auth = {
    save({ token, email, role }) {
        if (token != null) localStorage.setItem("hh_token", token);
        if (email != null) localStorage.setItem("hh_email", email);
        if (role != null) localStorage.setItem("hh_role", role);
    },
    clear() {
        localStorage.removeItem("hh_token");
        localStorage.removeItem("hh_email");
        localStorage.removeItem("hh_role");
    },
    get() {
        return {
            token: localStorage.getItem("hh_token") || "",
            email: localStorage.getItem("hh_email") || "",
            role: localStorage.getItem("hh_role") || "",
        };
    },
    isLoggedIn() {
        return !!localStorage.getItem("hh_token");
    },
    homeForRole(role) {
        return RoleHome[role] || "index.html";
    },
};

/**
 * Guard a dashboard page: only allow visitors whose role matches.
 * Redirects to login (home) or to the user's correct dashboard otherwise.
 */
function requireRole(expectedRole) {
    const { token, role } = Auth.get();
    if (!token) {
        window.location.replace("index.html");
        return null;
    }
    if (role !== expectedRole) {
        window.location.replace(Auth.homeForRole(role));
        return null;
    }
    return Auth.get();
}

/**
 * Plain-text fetch wrapper.
 * BE returns text/plain everywhere, so we parse as text and surface errors clearly.
 */
async function apiFetch(path, { method = "GET", body, auth = true } = {}) {
    const headers = { "Content-Type": "application/json" };
    if (auth) {
        const t = Auth.get().token;
        if (t) headers["Authorization"] = `Bearer ${t}`;
    }

    let response;
    try {
        response = await fetch(`${API_BASE}${path}`, {
            method,
            headers,
            body: body == null ? undefined : JSON.stringify(body),
        });
    } catch (e) {
        throw new Error("NETWORK");
    }

    const text = await response.text();
    if (!response.ok) {
        throw new Error(text || `HTTP_${response.status}`);
    }
    return text;
}

// ============== TOAST ==============
let _toastTimer = null;
function showToast(message, type = "success") {
    let el = document.getElementById("toast");
    if (!el) {
        el = document.createElement("div");
        el.id = "toast";
        el.className = "toast hidden";
        document.body.appendChild(el);
    }
    clearTimeout(_toastTimer);
    el.textContent = message;
    el.classList.remove("hidden", "success", "error");
    el.classList.add(type);
    _toastTimer = setTimeout(() => el.classList.add("hidden"), 3500);
}

// ============== UI HELPERS ==============
function setLoading(btn, loading) {
    if (!btn) return;
    const label = btn.querySelector(".btn-label");
    const spinner = btn.querySelector(".spinner");
    btn.disabled = loading;
    if (label) label.style.opacity = loading ? "0.6" : "1";
    if (spinner) spinner.classList.toggle("hidden", !loading);
}

function setMessage(el, text, type = "error") {
    if (!el) return;
    el.textContent = text || "";
    el.classList.remove("error", "success");
    if (text) el.classList.add(type);
}

function localizeError(raw) {
    const t = (raw || "").trim();
    const l = t.toLowerCase();
    if (l === "network") return "Không kết nối được tới máy chủ. Hãy chắc chắn backend đang chạy ở localhost:8080.";
    if (l.includes("email not found")) return "Email không tồn tại";
    if (l.includes("wrong password")) return "Sai mật khẩu";
    if (l.includes("login denied")) return "Tài khoản chưa được phê duyệt";
    if (l.includes("email already exists")) return "Email đã tồn tại";
    if (l.includes("student id already exists")) return "Mã sinh viên đã tồn tại hoặc không hợp lệ cho trường này";
    if (l.includes("invalid token")) return "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại";
    if (l.includes("old password is incorrect")) return "Mật khẩu cũ không đúng";
    if (l.includes("new password and confirm password do not match")) return "Mật khẩu mới và xác nhận không khớp";
    if (l.includes("all fields are required")) return "Vui lòng nhập đầy đủ các trường";
    if (l.includes("team name cannot be empty")) return "Tên đội không được để trống";
    if (l.includes("team name already exists")) return "Tên đội đã tồn tại, vui lòng chọn tên khác";
    if (l.includes("you have already joined a team")) return "Bạn đã ở trong một đội rồi";
    if (l.includes("only students can")) return "Chỉ sinh viên mới có quyền thực hiện hành động này";
    if (l.includes("enroll code cannot be empty")) return "Mã enroll không được để trống";
    if (l.includes("invalid enroll code")) return "Mã enroll không hợp lệ";
    if (l.includes("only team leaders")) return "Chỉ team leader mới có quyền thực hiện hành động này";
    if (l.includes("leader cannot remove themselves")) return "Leader không thể tự xóa chính mình";
    if (l.includes("delete failed")) return "Xóa thành viên thất bại";
    if (l.includes("event id and category id are required")) return "Vui lòng nhập Event ID và Category ID";
    if (l.includes("you are not in a team")) return "Bạn chưa có đội, hoặc bạn không phải leader";
    if (l.includes("event is not valid")) return "Event không hợp lệ hoặc chưa mở";
    if (l.includes("your team has already joined this event")) return "Đội của bạn đã đăng ký event này rồi";
    if (l.includes("category is not valid")) return "Category không hợp lệ";
    if (l.includes("join event failed")) return "Đăng ký event thất bại";
    if (l.includes("create team failed")) return "Tạo đội thất bại";
    if (l.includes("join team failed")) return "Tham gia đội thất bại";
    if (l.includes("only students can have a team")) return "Chỉ sinh viên mới có đội";
    if (l === "no team") return "Bạn chưa có đội";
    return t || "Có lỗi xảy ra, vui lòng thử lại";
}

// ============== RESPONSE PARSING ==============
function parseLoginResponse(text) {
    const trimmed = (text || "").trim();
    if (!trimmed.toLowerCase().startsWith("login success")) {
        return { ok: false, message: trimmed || "Đăng nhập thất bại" };
    }
    const tokenMatch = trimmed.match(/Token:\s*([^\s]+)/i);
    const roleMatch = trimmed.match(/Role:\s*([^\n\r]+)/i);
    return {
        ok: true,
        message: "Đăng nhập thành công!",
        token: tokenMatch ? tokenMatch[1].trim() : null,
        role: roleMatch ? roleMatch[1].trim() : null,
    };
}

// ============== DASHBOARD HEADER ==============
/**
 * Renders the standard dashboard header (brand, user chip, logout) into a host element.
 */
function mountDashboardHeader(hostSelector, { roleLabel }) {
    const host = document.querySelector(hostSelector);
    if (!host) return;
    const { email, role } = Auth.get();
    const initial = (email[0] || "U").toUpperCase();
    host.innerHTML = `
        <div class="nav-container">
            <a href="index.html" class="brand">
                <img src="assets/images/fpt-logo.png" alt="FPT University" class="brand-logo" />
                <span class="brand-divider"></span>
                <span class="brand-text">
                    <strong>SEAL Hackathon</strong>
                    <small>Spring 2026</small>
                </span>
            </a>
            <div class="nav-links">
                <a href="index.html" class="nav-link">Trang chủ</a>
            </div>
            <div class="nav-user">
                <span class="role-pill role-${roleLabel.toLowerCase()}">${roleLabel}</span>
                <div class="user-chip">
                    <div class="avatar">${initial}</div>
                    <div class="user-meta">
                        <span class="user-email">${email}</span>
                        <span class="user-role">${role}</span>
                    </div>
                </div>
                <button class="btn btn-ghost" id="btnLogout">Đăng xuất</button>
            </div>
        </div>
    `;
    document.getElementById("btnLogout")?.addEventListener("click", () => {
        Auth.clear();
        showToast("Đã đăng xuất", "success");
        setTimeout(() => window.location.replace("index.html"), 400);
    });
}

// ============== SITE FOOTER ==============
/**
 * Inject the standard SEAL Hackathon footer. Call after DOMContentLoaded.
 * Pass a selector ("#footerHost") or omit to append <footer> to <body>.
 */
function mountSiteFooter(hostSelector) {
    const html = `
        <footer class="site-footer">
            <div class="footer-inner">
                <div class="footer-col">
                    <a href="index.html" class="footer-brand">
                        <img src="assets/images/fpt-logo.png" alt="FPT University" class="brand-logo" />
                        <span class="brand-text">
                            <strong>SEAL Hackathon</strong>
                            <small>Software Engineering · Agile League</small>
                        </span>
                    </a>
                    <p class="footer-about">
                        Sân chơi học thuật và trải nghiệm công nghệ dành cho sinh viên ngành CNTT
                        tại trường Đại học FPT và các trường trên địa bàn TP.HCM. Mỗi năm tổ chức
                        03 mùa Hackathon: Spring – Summer – Fall.
                    </p>
                </div>

                <div class="footer-col">
                    <h4>Cuộc thi</h4>
                    <ul class="footer-list">
                        <li><a href="index.html#about">Giới thiệu</a></li>
                        <li><a href="index.html#schedule">Lịch trình</a></li>
                        <li><a href="index.html#gallery">Khoảnh khắc</a></li>
                    </ul>
                </div>

                <div class="footer-col">
                    <h4>Đối tượng</h4>
                    <ul class="footer-list">
                        <li>Sinh viên Đại học FPT TP.HCM</li>
                        <li>Sinh viên các trường ĐH tại TP.HCM</li>
                        <li>Mỗi đội: 3 – 5 thành viên</li>
                    </ul>
                </div>

                <div class="footer-col">
                    <h4>Liên hệ</h4>
                    <ul class="footer-list">
                        <li>
                            <strong>Thầy Trương Long</strong>
                            <a class="mail" href="mailto:longt5@fe.edu.vn">longt5@fe.edu.vn</a>
                        </li>
                        <li>
                            <strong>Cô Lê Thị Diệu Ân</strong>
                            <a class="mail" href="mailto:anltd3@fe.edu.vn">anltd3@fe.edu.vn</a>
                        </li>
                    </ul>
                </div>
            </div>

            <div class="footer-bottom">
                <span>© 2026 SEAL Hackathon — Bộ môn Kỹ thuật phần mềm, Trường Đại học FPT.</span>
                <span>Made with care by the SEAL team.</span>
            </div>
        </footer>
    `;
    const host = hostSelector ? document.querySelector(hostSelector) : null;
    if (host) {
        host.outerHTML = html;
    } else {
        document.body.insertAdjacentHTML("beforeend", html);
    }
}

// ============== SHARED PROFILE / PASSWORD MODALS ==============
/**
 * Inject reusable "Update profile" and "Change password" modals into the page,
 * wire their submit handlers to BE, and expose openers.
 */
function mountProfileModals({ showProfileFields = true } = {}) {
    const html = `
        <div class="modal-overlay hidden" id="profileModal">
            <div class="modal">
                <button class="modal-close" data-close="profileModal" aria-label="Đóng">&times;</button>
                <h2>Cập nhật hồ sơ</h2>
                <p class="modal-sub">Cập nhật thông tin cá nhân của bạn.</p>
                <form id="profileForm" class="form" novalidate>
                    <label class="field">
                        <span>Họ và tên</span>
                        <input name="fullName" required />
                    </label>
                    <label class="field">
                        <span>Email</span>
                        <input name="email" type="email" required />
                    </label>
                    <div class="field-row ${showProfileFields ? "" : "hidden"}">
                        <label class="field">
                            <span>Trường</span>
                            <input name="Uni" />
                        </label>
                        <label class="field">
                            <span>Mã sinh viên</span>
                            <input name="studentId" />
                        </label>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block" id="profileSubmit">
                        <span class="btn-label">Cập nhật</span>
                        <span class="spinner hidden"></span>
                    </button>
                    <div class="form-message" id="profileMessage"></div>
                </form>
            </div>
        </div>

        <div class="modal-overlay hidden" id="passwordModal">
            <div class="modal">
                <button class="modal-close" data-close="passwordModal" aria-label="Đóng">&times;</button>
                <h2>Đổi mật khẩu</h2>
                <p class="modal-sub">Để bảo mật, bạn cần nhập mật khẩu hiện tại.</p>
                <form id="passwordForm" class="form" novalidate>
                    <label class="field">
                        <span>Mật khẩu cũ</span>
                        <input name="oldPassword" type="password" required />
                    </label>
                    <label class="field">
                        <span>Mật khẩu mới</span>
                        <input name="newPassword" type="password" required minlength="6" />
                    </label>
                    <label class="field">
                        <span>Xác nhận mật khẩu mới</span>
                        <input name="confirmPassword" type="password" required minlength="6" />
                    </label>
                    <button type="submit" class="btn btn-primary btn-block" id="passwordSubmit">
                        <span class="btn-label">Cập nhật</span>
                        <span class="spinner hidden"></span>
                    </button>
                    <div class="form-message" id="passwordMessage"></div>
                </form>
            </div>
        </div>
    `;
    const wrap = document.createElement("div");
    wrap.innerHTML = html;
    document.body.appendChild(wrap);

    const profileModal = document.getElementById("profileModal");
    const passwordModal = document.getElementById("passwordModal");
    const profileForm = document.getElementById("profileForm");
    const passwordForm = document.getElementById("passwordForm");
    const profileMessage = document.getElementById("profileMessage");
    const passwordMessage = document.getElementById("passwordMessage");
    const profileSubmit = document.getElementById("profileSubmit");
    const passwordSubmit = document.getElementById("passwordSubmit");

    // Close buttons + overlay click + ESC
    document.querySelectorAll("[data-close]").forEach((b) => {
        b.addEventListener("click", () => closeModal(b.dataset.close));
    });
    [profileModal, passwordModal].forEach((o) =>
        o.addEventListener("click", (e) => {
            if (e.target === o) closeModal(o.id);
        })
    );
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeModal("profileModal");
            closeModal("passwordModal");
        }
    });

    // Profile submit
    profileForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        setMessage(profileMessage, "");
        const fd = new FormData(profileForm);
        const payload = {
            fullName: (fd.get("fullName") || "").toString().trim(),
            email: (fd.get("email") || "").toString().trim(),
            Uni: (fd.get("Uni") || "").toString().trim(),
            studentId: (fd.get("studentId") || "").toString().trim(),
        };
        setLoading(profileSubmit, true);
        try {
            const text = await apiFetch("/api/auth/updateprofile", { method: "PUT", body: payload });
            if (!/profile updated successfully/i.test(text)) {
                setMessage(profileMessage, localizeError(text), "error");
                return;
            }
            // Capture new token if returned
            const tokenMatch = text.match(/New Token:\s*([^\s]+)/i);
            if (tokenMatch) Auth.save({ token: tokenMatch[1].trim() });
            Auth.save({ email: payload.email });

            setMessage(profileMessage, "Cập nhật hồ sơ thành công!", "success");
            showToast("Đã cập nhật hồ sơ", "success");
            setTimeout(() => {
                closeModal("profileModal");
                // Refresh header bits
                document.querySelectorAll(".user-email").forEach((n) => (n.textContent = payload.email));
                document.querySelectorAll(".avatar").forEach((n) => (n.textContent = (payload.email[0] || "U").toUpperCase()));
            }, 600);
        } catch (err) {
            setMessage(profileMessage, localizeError(err.message), "error");
        } finally {
            setLoading(profileSubmit, false);
        }
    });

    // Password submit
    passwordForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        setMessage(passwordMessage, "");
        const fd = new FormData(passwordForm);
        const payload = {
            oldPassword: (fd.get("oldPassword") || "").toString(),
            newPassword: (fd.get("newPassword") || "").toString(),
            confirmPassword: (fd.get("confirmPassword") || "").toString(),
        };
        if (payload.newPassword !== payload.confirmPassword) {
            setMessage(passwordMessage, "Mật khẩu mới và xác nhận không khớp", "error");
            return;
        }
        setLoading(passwordSubmit, true);
        try {
            const text = await apiFetch("/api/auth/updatepassword", { method: "PUT", body: payload });
            if (!/password updated successfully/i.test(text)) {
                setMessage(passwordMessage, localizeError(text), "error");
                return;
            }
            setMessage(passwordMessage, "Đổi mật khẩu thành công!", "success");
            showToast("Đã đổi mật khẩu", "success");
            setTimeout(() => {
                closeModal("passwordModal");
                passwordForm.reset();
            }, 600);
        } catch (err) {
            setMessage(passwordMessage, localizeError(err.message), "error");
        } finally {
            setLoading(passwordSubmit, false);
        }
    });

    return {
        openProfile() {
            // Pre-fill known data
            const { email } = Auth.get();
            profileForm.querySelector('input[name="email"]').value = email || "";
            openModal("profileModal");
        },
        openPassword() {
            passwordForm.reset();
            openModal("passwordModal");
        },
    };
}

function openModal(id) {
    const m = document.getElementById(id);
    if (!m) return;
    m.classList.remove("hidden");
    document.body.style.overflow = "hidden";
    const first = m.querySelector("input");
    if (first) setTimeout(() => first.focus(), 80);
}
function closeModal(id) {
    const m = document.getElementById(id);
    if (!m) return;
    m.classList.add("hidden");
    document.body.style.overflow = "";
}
