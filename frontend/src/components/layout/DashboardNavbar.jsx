import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";

export default function DashboardNavbar({ roleLabel }) {
	const { auth, clearAuth } = useAuth();
	const { showToast } = useToast();
	const navigate = useNavigate();

	const handleLogout = () => {
		clearAuth();
		showToast("Đã đăng xuất", "success");
		navigate("/");
	};

	// FIX: optional chaining to prevent crash when email is empty/undefined
	const initial = (auth.email?.[0] || "U").toUpperCase();

	return (
		<nav className="navbar">
			<div className="nav-container">
				<a
					onClick={() => navigate("/")}
					className="brand"
					style={{ cursor: "pointer" }}
				>
					<img
						src="/assets/images/fpt-logo.png"
						alt="FPT University"
						className="brand-logo"
					/>
					<span className="brand-divider" />
					<span className="brand-text">
						<strong>SEAL Hackathon</strong>
						<small>Spring 2026</small>
					</span>
				</a>

				<div className="nav-user">
					<span className={`role-pill role-${roleLabel.toLowerCase()}`}>
						{roleLabel}
					</span>
					<div className="user-chip">
						<div className="avatar">{initial}</div>
						<div className="user-meta">
							<span className="user-email">{auth.email}</span>
							<span className="user-role">{auth.role}</span>
						</div>
					</div>
					<button className="btn btn-ghost" onClick={handleLogout}>
						Đăng xuất
					</button>
				</div>
			</div>
		</nav>
	);
}
