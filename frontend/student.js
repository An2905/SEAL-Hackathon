// Student dashboard logic.
(function () {
    const session = requireRole("Student");
    if (!session) return;

    mountDashboardHeader("#navHost", { roleLabel: "Student" });
    const modals = mountProfileModals({ showProfileFields: true });

    const welcomeName = document.getElementById("welcomeName");
    if (welcomeName) welcomeName.textContent = session.email;

    document.getElementById("btnEditProfile").addEventListener("click", modals.openProfile);
    document.getElementById("btnChangePass").addEventListener("click", modals.openPassword);

    // Section refs
    const teamInfoSection = document.getElementById("teamInfoSection");
    const noTeamSection = document.getElementById("noTeamSection");
    const leaderSection = document.getElementById("leaderSection");
    const loadingState = document.getElementById("teamLoadingState");
    const activityLog = document.getElementById("activityLog");
    const activities = [];

    // ============== TEAM INFO LOADER ==============
    // BE convention: plain text = error / "No team", JSON object = team data.
    async function loadMyTeam() {
        loadingState.classList.remove("hidden");
        teamInfoSection.classList.add("hidden");
        noTeamSection.classList.add("hidden");
        leaderSection.classList.add("hidden");

        try {
            const text = (await apiFetch("/api/team/me", { method: "GET" })).trim();

            if (text.startsWith("{")) {
                const data = JSON.parse(text);
                renderTeamInfo(data);
                teamInfoSection.classList.remove("hidden");
                if (data.isLeader) leaderSection.classList.remove("hidden");
                return;
            }

            if (/^no team$/i.test(text) || text === "") {
                noTeamSection.classList.remove("hidden");
                return;
            }

            // Anything else is treated as an error message from BE
            showToast(localizeError(text), "error");
            noTeamSection.classList.remove("hidden");
        } catch (err) {
            console.error("Failed to load team:", err);
            showToast(localizeError(err.message), "error");
            noTeamSection.classList.remove("hidden");
        } finally {
            loadingState.classList.add("hidden");
        }
    }

    function renderTeamInfo(data) {
        document.getElementById("teamNameDisplay").textContent = data.teamName;
        document.getElementById("teamStatusDisplay").textContent = data.isLeader
            ? "Bạn là leader của đội này"
            : "Bạn đang là thành viên của đội";

        document.getElementById("kvTeamName").textContent = data.teamName;
        document.getElementById("kvEnrollCode").textContent = data.enrollCode;
        document.getElementById("kvLeader").textContent = `${data.leaderName} (${data.leaderEmail})`;
        document.getElementById("kvStatus").textContent = data.status;
        document.getElementById("kvMemberCount").textContent = `${data.memberCount} / 5`;

        const badge = document.getElementById("leaderBadge");
        if (data.isLeader) {
            badge.textContent = "Leader";
            badge.className = "role-pill role-judge";
        } else {
            badge.textContent = "Thành viên";
            badge.className = "role-pill role-student";
        }

        const list = document.getElementById("memberList");
        list.innerHTML = (data.members || [])
            .map((m) => {
                const initial = (m.fullName?.[0] || m.email?.[0] || "U").toUpperCase();
                return `
                    <div class="member-row">
                        <div class="avatar">${escapeHtml(initial)}</div>
                        <div class="member-info">
                            <div class="member-name">${escapeHtml(m.fullName || "(Chưa có tên)")} ${m.isLeader ? '<span class="leader-tag">Leader</span>' : ""}</div>
                            <div class="member-meta">${escapeHtml(m.email || "")}</div>
                        </div>
                        <span class="member-id-chip" title="user_id">#${escapeHtml(m.userId)}</span>
                    </div>
                `;
            })
            .join("");
    }

    // Copy enroll code
    document.getElementById("btnCopyEnroll").addEventListener("click", async () => {
        const code = document.getElementById("kvEnrollCode").textContent.trim();
        if (!code || code === "—") return;
        try {
            await navigator.clipboard.writeText(code);
            showToast("Đã sao chép mã enroll: " + code, "success");
        } catch (_) {
            showToast("Mã enroll: " + code, "success");
        }
    });

    document.getElementById("btnRefreshTeam").addEventListener("click", () => {
        showToast("Đang làm mới...", "success");
        loadMyTeam();
    });

    function logActivity(text) {
        activities.unshift({ text, at: new Date() });
        renderActivity();
    }

    function renderActivity() {
        if (!activities.length) {
            activityLog.className = "empty-state";
            activityLog.textContent = "Chưa có hoạt động nào trong phiên này.";
            return;
        }
        activityLog.className = "kv-list";
        activityLog.innerHTML = activities
            .map((a) => {
                const ts = a.at.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
                return `<div class="kv"><span>${ts}</span><span>${escapeHtml(a.text)}</span></div>`;
            })
            .join("");
    }

    function escapeHtml(s) {
        return (s == null ? "" : String(s))
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function bindForm({ formId, btnId, msgId, request, onSuccess }) {
        const form = document.getElementById(formId);
        if (!form) return;
        const btn = document.getElementById(btnId);
        const msg = document.getElementById(msgId);

        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            setMessage(msg, "");
            setLoading(btn, true);
            try {
                const text = await request(new FormData(form));
                const ok = onSuccess(text, msg, form);
                if (!ok) setMessage(msg, localizeError(text), "error");
            } catch (err) {
                setMessage(msg, localizeError(err.message), "error");
            } finally {
                setLoading(btn, false);
            }
        });
    }

    // ============== CREATE TEAM ==============
    bindForm({
        formId: "createTeamForm",
        btnId: "createTeamSubmit",
        msgId: "createTeamMessage",
        request: (fd) =>
            apiFetch("/api/team/create", {
                method: "PUT",
                body: { teamName: (fd.get("teamName") || "").toString().trim() },
            }),
        onSuccess: (text, msg, form) => {
            const enrollMatch = text.match(/enrollCode:\s*(\S+)/i);
            if (!/^Added Team /i.test(text) && !enrollMatch) return false;
            const code = enrollMatch ? enrollMatch[1] : "(không lấy được)";
            msg.classList.remove("error");
            msg.classList.add("success");
            msg.innerHTML = `Tạo đội thành công! Mã enroll: <code style="background:rgba(99,102,241,0.2);padding:2px 8px;border-radius:6px;">${escapeHtml(code)}</code>`;
            showToast("Đã tạo đội — mã enroll: " + code, "success");
            logActivity(`Tạo đội mới — enrollCode: ${code}`);
            form.reset();
            setTimeout(loadMyTeam, 400);
            return true;
        },
    });

    // ============== JOIN TEAM ==============
    bindForm({
        formId: "joinTeamForm",
        btnId: "joinTeamSubmit",
        msgId: "joinTeamMessage",
        request: (fd) =>
            apiFetch("/api/team/join", {
                method: "PUT",
                body: { enrollCode: (fd.get("enrollCode") || "").toString().trim() },
            }),
        onSuccess: (text, msg, form) => {
            if (!/join team successfully/i.test(text)) return false;
            setMessage(msg, "Đã tham gia đội thành công!", "success");
            showToast("Đã tham gia đội", "success");
            logActivity("Tham gia đội thành công");
            form.reset();
            setTimeout(loadMyTeam, 400);
            return true;
        },
    });

    // ============== JOIN EVENT ==============
    bindForm({
        formId: "joinEventForm",
        btnId: "joinEventSubmit",
        msgId: "joinEventMessage",
        request: (fd) =>
            apiFetch("/api/team/join-event", {
                method: "PUT",
                body: {
                    eventId: (fd.get("eventId") || "").toString().trim(),
                    categoryId: (fd.get("categoryId") || "").toString().trim(),
                },
            }),
        onSuccess: (text, msg, form) => {
            if (!/join event successfully/i.test(text)) return false;
            setMessage(msg, "Đăng ký event thành công!", "success");
            showToast("Đăng ký event thành công", "success");
            logActivity(`Đăng ký event ${form.eventId.value} (category ${form.categoryId.value})`);
            form.reset();
            return true;
        },
    });

    // ============== DELETE MEMBER ==============
    bindForm({
        formId: "deleteMemberForm",
        btnId: "deleteMemberSubmit",
        msgId: "deleteMemberMessage",
        request: (fd) =>
            apiFetch("/api/team/delete-member", {
                method: "DELETE",
                body: { memberId: (fd.get("memberId") || "").toString().trim() },
            }),
        onSuccess: (text, msg, form) => {
            if (!/delete team member successfully/i.test(text)) return false;
            setMessage(msg, "Đã xóa thành viên", "success");
            showToast("Đã xóa thành viên khỏi đội", "success");
            logActivity(`Xóa thành viên ${form.memberId.value}`);
            form.reset();
            setTimeout(loadMyTeam, 400);
            return true;
        },
    });

    // Boot
    loadMyTeam();
})();
