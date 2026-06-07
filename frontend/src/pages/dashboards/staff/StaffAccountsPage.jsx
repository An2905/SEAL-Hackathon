import { useState } from "react";
import {
	CreateStaffAccountForm,
	AccountsListSection,
} from "../StaffDashboard";

export default function StaffAccountsPage() {
	const [showCreateModal, setShowCreateModal] = useState(false);
	const [refreshKey, setRefreshKey] = useState(0);

	return (
		<>
			<div className="section-title">
				<h2>Tạo tài khoản</h2>
				<span className="hint">Tạo tài khoản Judge / Mentor</span>
			</div>
			<div style={{ marginBottom: 16 }}>
				<button
					type="button"
					className="btn btn-primary"
					onClick={() => setShowCreateModal(true)}
				>
					Tạo tài khoản Khách
				</button>
			</div>
			<CreateStaffAccountForm
				open={showCreateModal}
				onClose={() => setShowCreateModal(false)}
				onSuccess={() => setRefreshKey((k) => k + 1)}
			/>

			<div className="section-title" style={{ marginTop: 24 }}>
				<h2>Danh sách tài khoản</h2>
				<span className="hint">Lọc theo vai trò, tìm kiếm, duyệt trạng thái</span>
			</div>
			<AccountsListSection refreshKey={refreshKey} />
		</>
	);
}
