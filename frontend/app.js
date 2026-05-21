// =====================================================
// HomePage logic: login / register modals + role-based redirect.
// Depends on auth.js (must be loaded first).
// =====================================================

const els = {
    btnOpenLogin: document.getElementById("btnOpenLogin"),
    btnOpenRegister: document.getElementById("btnOpenRegister"),
    btnHeroLogin: document.getElementById("btnHeroLogin"),
    btnHeroRegister: document.getElementById("btnHeroRegister"),

    loginModal: document.getElementById("loginModal"),
    registerModal: document.getElementById("registerModal"),
    loginForm: document.getElementById("loginForm"),
    registerForm: document.getElementById("registerForm"),
    loginMessage: document.getElementById("loginMessage"),
    registerMessage: document.getElementById("registerMessage"),
    loginSubmit: document.getElementById("loginSubmit"),
    registerSubmit: document.getElementById("registerSubmit"),

    switchToRegister: document.getElementById("switchToRegister"),
    switchToLogin: document.getElementById("switchToLogin"),

    navActions: document.getElementById("navActions"),
    navUser: document.getElementById("navUser"),
    userAvatar: document.getElementById("userAvatar"),
    userEmail: document.getElementById("userEmail"),
    userRole: document.getElementById("userRole"),
    btnLogout: document.getElementById("btnLogout"),
};

function refreshHomeAuthUI() {
    const { token, email, role } = Auth.get();
    if (token && email) {
        els.navActions.classList.add("hidden");
        els.navUser.classList.remove("hidden");
        els.userEmail.textContent = email;
        els.userRole.textContent = role || "USER";
        els.userAvatar.textContent = (email[0] || "U").toUpperCase();
    } else {
        els.navActions.classList.remove("hidden");
        els.navUser.classList.add("hidden");
    }
}

function redirectToDashboard(role) {
    const url = Auth.homeForRole(role);
    if (url && url !== "index.html") {
        window.location.assign(url);
    }
}

async function handleLogin(e) {
    e.preventDefault();
    setMessage(els.loginMessage, "");
    const fd = new FormData(els.loginForm);
    const payload = {
        email: (fd.get("email") || "").toString().trim(),
        password: (fd.get("password") || "").toString(),
    };
    if (!payload.email || !payload.password) {
        setMessage(els.loginMessage, "Vui lòng nhập đầy đủ email và mật khẩu", "error");
        return;
    }

    setLoading(els.loginSubmit, true);
    try {
        const text = await apiFetch("/api/auth/login", { method: "POST", body: payload, auth: false });
        const result = parseLoginResponse(text);
        if (!result.ok) {
            setMessage(els.loginMessage, localizeError(result.message), "error");
            return;
        }
        Auth.save({ token: result.token, email: payload.email, role: result.role });
        setMessage(els.loginMessage, "Đăng nhập thành công, đang chuyển hướng...", "success");
        showToast(`Chào mừng ${payload.email}!`, "success");
        setTimeout(() => redirectToDashboard(result.role), 500);
    } catch (err) {
        setMessage(els.loginMessage, localizeError(err.message), "error");
    } finally {
        setLoading(els.loginSubmit, false);
    }
}

async function handleRegister(e) {
    e.preventDefault();
    setMessage(els.registerMessage, "");
    const fd = new FormData(els.registerForm);
    const payload = {
        fullName: (fd.get("fullName") || "").toString().trim(),
        email: (fd.get("email") || "").toString().trim(),
        Uni: (fd.get("Uni") || "").toString().trim(),
        studentId: (fd.get("studentId") || "").toString().trim(),
        password: (fd.get("password") || "").toString(),
    };
    if (!payload.fullName || !payload.email || !payload.Uni || !payload.studentId || !payload.password) {
        setMessage(els.registerMessage, "Vui lòng nhập đầy đủ thông tin", "error");
        return;
    }
    if (payload.password.length < 6) {
        setMessage(els.registerMessage, "Mật khẩu phải có ít nhất 6 ký tự", "error");
        return;
    }

    setLoading(els.registerSubmit, true);
    try {
        const text = await apiFetch("/api/auth/register", { method: "POST", body: payload, auth: false });
        if (!/registration successful/i.test(text)) {
            setMessage(els.registerMessage, localizeError(text), "error");
            return;
        }
        setMessage(els.registerMessage, "Tạo tài khoản thành công! Đang đăng nhập...", "success");
        showToast("Tạo tài khoản thành công!", "success");

        // Auto-login
        try {
            const loginText = await apiFetch("/api/auth/login", {
                method: "POST",
                body: { email: payload.email, password: payload.password },
                auth: false,
            });
            const result = parseLoginResponse(loginText);
            if (result.ok) {
                Auth.save({ token: result.token, email: payload.email, role: result.role });
                setTimeout(() => redirectToDashboard(result.role), 500);
                return;
            }
        } catch (_) {
            // Fall through to manual login.
        }

        setTimeout(() => {
            closeModal("registerModal");
            openModal("loginModal");
        }, 800);
    } catch (err) {
        setMessage(els.registerMessage, localizeError(err.message), "error");
    } finally {
        setLoading(els.registerSubmit, false);
    }
}

function init() {
    // If already logged in, jump straight to dashboard
    const { token, role } = Auth.get();
    if (token && role) {
        redirectToDashboard(role);
        return;
    }

    els.btnOpenLogin?.addEventListener("click", () => openModal("loginModal"));
    els.btnOpenRegister?.addEventListener("click", () => openModal("registerModal"));
    els.btnHeroLogin?.addEventListener("click", () => openModal("loginModal"));
    els.btnHeroRegister?.addEventListener("click", () => openModal("registerModal"));

    document.querySelectorAll("[data-close]").forEach((b) => {
        b.addEventListener("click", () => closeModal(b.dataset.close));
    });
    [els.loginModal, els.registerModal].forEach((o) =>
        o?.addEventListener("click", (e) => {
            if (e.target === o) closeModal(o.id);
        })
    );
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            closeModal("loginModal");
            closeModal("registerModal");
        }
    });

    els.switchToRegister?.addEventListener("click", (e) => {
        e.preventDefault();
        closeModal("loginModal");
        openModal("registerModal");
    });
    els.switchToLogin?.addEventListener("click", (e) => {
        e.preventDefault();
        closeModal("registerModal");
        openModal("loginModal");
    });

    els.loginForm?.addEventListener("submit", handleLogin);
    els.registerForm?.addEventListener("submit", handleRegister);

    els.btnLogout?.addEventListener("click", () => {
        Auth.clear();
        refreshHomeAuthUI();
        showToast("Đã đăng xuất", "success");
    });

    refreshHomeAuthUI();
}

document.addEventListener("DOMContentLoaded", init);
