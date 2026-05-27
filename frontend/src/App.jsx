import { Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import HomePage from "./pages/HomePage";
import RequireRole from "./guards/RequireRole";
import StudentDashboard from "./pages/dashboards/StudentDashboard";
import StaffDashboard from "./pages/dashboards/StaffDashboard";
import MentorDashboard from "./pages/dashboards/MentorDashboard";
import JudgeDashboard from "./pages/dashboards/JudgeDashboard";
import EventDetailsPage from "./pages/dashboards/EventDetailsPage";

export default function App() {
	return (
		<AuthProvider>
			<ToastProvider>
				<Routes>
					<Route path="/" element={<HomePage />} />

					<Route
						path="/student"
						element={
							<RequireRole role="Student">
								<StudentDashboard />
							</RequireRole>
						}
					/>

					<Route
						path="/staff"
						element={
							<RequireRole role="Staff">
								<StaffDashboard />
							</RequireRole>
						}
					/>

					<Route
						path="/staff/events/:eventId"
						element={
							<RequireRole role="Staff">
								<EventDetailsPage />
							</RequireRole>
						}
					/>

					<Route
						path="/mentor"
						element={
							<RequireRole role="Mentor">
								<MentorDashboard />
							</RequireRole>
						}
					/>

					<Route
						path="/judge"
						element={
							<RequireRole role="Judge">
								<JudgeDashboard />
							</RequireRole>
						}
					/>

					{/* Catch-all */}
					<Route path="*" element={<Navigate to="/" replace />} />
				</Routes>
			</ToastProvider>
		</AuthProvider>
	);
}
