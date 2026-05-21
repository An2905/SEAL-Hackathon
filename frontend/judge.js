(function () {
    const session = requireRole("Judge");
    if (!session) return;

    mountDashboardHeader("#navHost", { roleLabel: "Judge" });
    mountSiteFooter();
    const modals = mountProfileModals({ showProfileFields: false });

    document.getElementById("welcomeName").textContent = session.email;
    document.getElementById("btnEditProfile").addEventListener("click", modals.openProfile);
    document.getElementById("btnChangePass").addEventListener("click", modals.openPassword);

    const kv = document.getElementById("profileKv");
    kv.innerHTML = `
        <div class="kv"><span>Email</span><span>${escapeHtml(session.email)}</span></div>
        <div class="kv"><span>Vai trò</span><span>Judge (JUDGE_INTERNAL)</span></div>
        <div class="kv"><span>Trạng thái phiên</span><span>Đã đăng nhập</span></div>
    `;

    function escapeHtml(s) {
        return (s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }
})();
