import {
	CreateStaffAccountForm,
	AccountsListSection,
} from "../StaffDashboard";

export default function StaffAccountsPage() {
	return (
		<>
			<div className="section-title">
				<h2>Tạo tài khoản</h2>
				<span className="hint">Tạo tài khoản Judge / Mentor</span>
			</div>
			<div className="cards">
				<CreateStaffAccountForm />
			</div>

			<div className="section-title" style={{ marginTop: 24 }}>
				<h2>Danh sách tài khoản</h2>
				<span className="hint">Lọc theo vai trò, tìm kiếm, duyệt trạng thái</span>
			</div>
			<AccountsListSection />
		</>
	);
}
