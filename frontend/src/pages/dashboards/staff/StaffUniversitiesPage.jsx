import { useEffect, useState } from "react";
import { useToast } from "../../../context/ToastContext";
import { localizeError } from "../../../utils/errors";
import FormField from "../../../components/common/FormField";
import FormMessage from "../../../components/common/FormMessage";
import LoadingButton from "../../../components/common/LoadingButton";
import Modal from "../../../components/common/Modal";
import {
  getStaffUniversities,
  createUniversity,
  updateUniversity,
  getDeleteUniversityPreview,
  deleteUniversity,
} from "../../../api/staffUniversity";

export default function StaffUniversitiesPage() {
  const { showToast } = useToast();
  const [universities, setUniversities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  // Create Form State
  const [createName, setCreateName] = useState("");
  const [createMessage, setCreateMessage] = useState(null);

  // Edit Modal State
  const [editModal, setEditModal] = useState({ isOpen: false, university: null });
  const [editFormName, setEditFormName] = useState("");
  const [editMessage, setEditMessage] = useState(null);

  // Delete Modal State
  const [deleteModal, setDeleteModal] = useState({
    isOpen: false,
    universityId: "",
    universityName: "",
    linkedUserCount: 0,
    canDeleteDirectly: true,
    requiresUserHandling: false,
    message: "",
  });
  const [deleteOption, setDeleteOption] = useState("reassign"); // "reassign" | "clear"
  const [deleteReplacement, setDeleteReplacement] = useState("");
  const [deleteMessage, setDeleteMessage] = useState(null);

  useEffect(() => {
    fetchList();
  }, []);

  const fetchList = async () => {
    setLoading(true);
    try {
      const list = await getStaffUniversities();
      setUniversities(list);
      setError(null);
    } catch (err) {
      setError(localizeError(err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSubmit = async (e) => {
    e.preventDefault();
    setCreateMessage(null);
    const trimmed = createName.trim();
    if (!trimmed) {
      setCreateMessage({ text: "Vui lòng nhập tên trường đại học", type: "error" });
      return;
    }
    setSubmitting(true);
    try {
      await createUniversity({ universityName: trimmed });
      showToast("Đã tạo trường đại học thành công", "success");
      setCreateName("");
      setCreateMessage({ text: "Tạo trường đại học thành công", type: "success" });
      await fetchList();
    } catch (err) {
      setCreateMessage({ text: localizeError(err.message), type: "error" });
    } finally {
      setSubmitting(false);
    }
  };

  const openEditModal = (uni) => {
    setEditModal({ isOpen: true, university: uni });
    setEditFormName(uni.universityName);
    setEditMessage(null);
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditMessage(null);
    const trimmed = editFormName.trim();
    if (!trimmed) {
      setEditMessage({ text: "Tên trường không được để trống", type: "error" });
      return;
    }
    setSubmitting(true);
    try {
      await updateUniversity({
        universityId: editModal.university.universityId,
        universityName: trimmed,
      });
      showToast("Đã cập nhật tên trường thành công", "success");
      setEditModal({ isOpen: false, university: null });
      await fetchList();
    } catch (err) {
      setEditMessage({ text: localizeError(err.message), type: "error" });
    } finally {
      setSubmitting(false);
    }
  };

  const openDeleteModal = async (uni) => {
    setDeleteMessage(null);
    setDeleteReplacement("");
    setDeleteOption("reassign");
    try {
      const preview = await getDeleteUniversityPreview(uni.universityId);
      setDeleteModal({
        isOpen: true,
        universityId: preview.universityId,
        universityName: preview.universityName,
        linkedUserCount: preview.linkedUserCount,
        canDeleteDirectly: preview.canDeleteDirectly,
        requiresUserHandling: preview.requiresUserHandling,
        message: preview.message,
      });
    } catch (err) {
      showToast(localizeError(err.message), "error");
    }
  };

  const handleDeleteSubmit = async (e) => {
    e.preventDefault();
    setDeleteMessage(null);

    const requiresHandling = deleteModal.linkedUserCount > 0;
    const isReassign = deleteOption === "reassign";

    if (requiresHandling && isReassign && !deleteReplacement) {
      setDeleteMessage({ text: "Vui lòng chọn một trường thay thế hợp lệ.", type: "error" });
      return;
    }

    setSubmitting(true);
    try {
      await deleteUniversity({
        universityId: deleteModal.universityId,
        replacementUniversityName: (requiresHandling && isReassign) ? deleteReplacement : undefined,
        clearLinkedUsers: (requiresHandling && !isReassign) ? true : undefined,
      });
      showToast("Xóa trường đại học thành công", "success");
      setDeleteModal({ ...deleteModal, isOpen: false });
      await fetchList();
    } catch (err) {
      setDeleteMessage({ text: localizeError(err.message), type: "error" });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="section-title">
        <h2>Quản lý Trường Đại Học</h2>
        <span className="hint">Thêm, sửa đổi tên trường, và giải quyết liên kết khi xóa</span>
      </div>

      <div className="dashboard-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 24, alignItems: "start" }}>
        {/* Card Thêm Trường */}
        <div className="card">
          <div className="card-head">
            <div className="card-title">Thêm trường mới</div>
          </div>
          <form className="form" onSubmit={handleCreateSubmit}>
            <FormField label="Tên trường đại học *">
              <input
                type="text"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
                placeholder="Nhập tên trường, ví dụ: FPT University"
                disabled={submitting}
              />
            </FormField>
            <LoadingButton loading={submitting} type="submit">
              Thêm trường
            </LoadingButton>
            <FormMessage message={createMessage?.text} type={createMessage?.type} />
          </form>
        </div>

        {/* Card Danh Sách Trường */}
        <div className="card" style={{ gridColumn: "span 2" }}>
          <div className="card-head">
            <div className="card-title">Danh sách trường đại học ({universities.length})</div>
          </div>

          {error && <FormMessage message={error} type="error" />}
          {loading && <div className="empty-state">Đang tải danh sách trường...</div>}
          {!loading && universities.length === 0 && !error && (
            <div className="empty-state">Chưa có trường đại học nào trong hệ thống.</div>
          )}

          {!loading && universities.length > 0 && (
            <div className="kv-list">
              {universities.map((uni) => (
                <div className="kv" key={uni.universityId} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 8px" }}>
                  <span style={{ minWidth: 0, flex: 1, textAlign: "left" }}>
                    <div style={{ fontWeight: 600, color: "var(--text)" }}>{uni.universityName}</div>
                    <div style={{ fontSize: 11, color: "var(--text-mute)", marginTop: 2 }}>ID: {uni.universityId}</div>
                  </span>
                  <span style={{ display: "flex", gap: 12, alignItems: "center" }}>
                    <span className="card-badge" style={{
                      backgroundColor: uni.linkedUserCount > 0 ? "rgba(245, 158, 11, 0.1)" : "var(--bg-card)",
                      color: uni.linkedUserCount > 0 ? "var(--warning-fg, #d97706)" : "var(--text-dim)",
                      border: uni.linkedUserCount > 0 ? "1px solid rgba(245, 158, 11, 0.2)" : "none",
                      padding: "4px 8px",
                      borderRadius: "4px"
                    }}>
                      {uni.linkedUserCount} SV liên kết
                    </span>
                    <button className="btn btn-secondary btn-sm" onClick={() => openEditModal(uni)} disabled={submitting}>
                      Sửa
                    </button>
                    <button className="btn btn-danger btn-sm" onClick={() => openDeleteModal(uni)} disabled={submitting}>
                      Xóa
                    </button>
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Modal Sửa Tên Trường */}
      <Modal isOpen={editModal.isOpen} onClose={() => setEditModal({ isOpen: false, university: null })} title="Sửa tên trường đại học" subtitle="Sinh viên đang liên kết với tên cũ sẽ được tự động cập nhật.">
        <form className="form" onSubmit={handleEditSubmit} style={{ marginTop: 16 }}>
          <FormField label="Tên trường đại học mới *">
            <input
              type="text"
              value={editFormName}
              onChange={(e) => setEditFormName(e.target.value)}
              required
              disabled={submitting}
            />
          </FormField>
          <div style={{ display: "flex", gap: 12, justifyContent: "flex-end", marginTop: 20 }}>
            <button type="button" className="btn btn-secondary" onClick={() => setEditModal({ isOpen: false, university: null })} disabled={submitting}>
              Hủy
            </button>
            <LoadingButton loading={submitting} type="submit" className="btn btn-primary">
              Lưu thay đổi
            </LoadingButton>
          </div>
          <FormMessage message={editMessage?.text} type={editMessage?.type} />
        </form>
      </Modal>

      {/* Modal Xóa Trường (Có đếm linked users) */}
      <Modal isOpen={deleteModal.isOpen} onClose={() => setDeleteModal({ ...deleteModal, isOpen: false })} title="Xóa trường đại học" className="modal-delete">
        <form className="form" onSubmit={handleDeleteSubmit} style={{ marginTop: 16 }}>
          <p style={{ color: "var(--text)" }}>
            Bạn đang yêu cầu xóa trường: <strong>{deleteModal.universityName}</strong>
          </p>

          {deleteModal.linkedUserCount === 0 ? (
            <div className="alert alert-info" style={{ margin: "16px 0", padding: "12px", borderRadius: "6px", backgroundColor: "rgba(59, 130, 246, 0.1)", color: "var(--primary)" }}>
              Trường này hiện không có sinh viên nào liên kết. Bạn có thể xóa trực tiếp và an toàn.
            </div>
          ) : (
            <div style={{ marginTop: 16, border: "1px solid rgba(239, 68, 68, 0.2)", borderRadius: "6px", padding: "16px", backgroundColor: "rgba(239, 68, 68, 0.05)" }}>
              <p style={{ fontWeight: 600, color: "var(--error-fg, #ef4444)", marginBottom: 8 }}>
                ⚠️ Cảnh báo: Có {deleteModal.linkedUserCount} sinh viên đang liên kết với trường này!
              </p>
              <p style={{ fontSize: 13, color: "var(--text-dim)", marginBottom: 16 }}>
                Bạn phải chọn phương án xử lý dữ liệu sinh viên trước khi xóa:
              </p>

              <div className="form-group" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                <label style={{ display: "flex", gap: 8, alignItems: "center", cursor: "pointer", fontWeight: 500 }}>
                  <input
                    type="radio"
                    name="deleteOption"
                    value="reassign"
                    checked={deleteOption === "reassign"}
                    onChange={() => setDeleteOption("reassign")}
                  />
                  Chuyển sinh viên sang trường đại học khác (Khuyên dùng)
                </label>

                {deleteOption === "reassign" && (
                  <div style={{ marginLeft: 24, marginTop: 4 }}>
                    <select
                      className="form-select"
                      value={deleteReplacement}
                      onChange={(e) => setDeleteReplacement(e.target.value)}
                      style={{ width: "100%", padding: "8px", borderRadius: "4px", border: "1px solid var(--border)", backgroundColor: "var(--bg-card)", color: "var(--text)" }}
                    >
                      <option value="">-- Chọn trường thay thế --</option>
                      {universities
                        .filter((u) => u.universityId !== deleteModal.universityId)
                        .map((u) => (
                          <option key={u.universityId} value={u.universityName}>
                            {u.universityName}
                          </option>
                        ))}
                    </select>
                  </div>
                )}

                <label style={{ display: "flex", gap: 8, alignItems: "center", cursor: "pointer", fontWeight: 500, marginTop: 8 }}>
                  <input
                    type="radio"
                    name="deleteOption"
                    value="clear"
                    checked={deleteOption === "clear"}
                    onChange={() => setDeleteOption("clear")}
                  />
                  Không chuyển — Để trống tên trường của sinh viên (đặt thành NULL)
                </label>
              </div>
            </div>
          )}

          <div style={{ display: "flex", gap: 12, justifyContent: "flex-end", marginTop: 24 }}>
            <button type="button" className="btn btn-secondary" onClick={() => setDeleteModal({ ...deleteModal, isOpen: false })} disabled={submitting}>
              Hủy
            </button>
            <LoadingButton
              loading={submitting}
              type="submit"
              className="btn btn-danger"
              disabled={deleteModal.linkedUserCount > 0 && deleteOption === "reassign" && !deleteReplacement}
            >
              Xác nhận xóa
            </LoadingButton>
          </div>
          <FormMessage message={deleteMessage?.text} type={deleteMessage?.type} />
        </form>
      </Modal>
    </>
  );
}
