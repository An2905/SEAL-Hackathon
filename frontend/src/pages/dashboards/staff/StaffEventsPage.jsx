import { EventsListSection } from "../StaffDashboard";

export default function StaffEventsPage() {
	return (
		<>
			<div className="section-title">
				<h2>Sự kiện trong hệ thống</h2>
				<span className="hint">Xem và đổi trạng thái sự kiện</span>
			</div>
			<EventsListSection />
		</>
	);
}
