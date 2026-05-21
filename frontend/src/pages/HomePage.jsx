import { useState } from "react";
import HomeNavbar from "../components/layout/HomeNavbar";
import SiteFooter from "../components/layout/SiteFooter";
import LoginModal from "../components/common/LoginModal";
import RegisterModal from "../components/common/RegisterModal";
import ResetPasswordModal from "../components/common/ResetPasswordModal";
import { useAuth } from "../context/AuthContext";

import posterImg from "../assets/images/poster.jpg";
import eventImg from "../assets/images/event.jpg";
import speakerImg from "../assets/images/speaker.jpg";

export default function HomePage() {
	const { isLoggedIn } = useAuth();
	const [modal, setModal] = useState(null);

	return (
		<>
			<HomeNavbar
				onOpenLogin={() => setModal("login")}
				onOpenRegister={() => setModal("register")}
			/>

			<main>
				{/* HERO */}
				<section className="hero">
					<div className="hero-grid">
						<div className="hero-content">
							<span className="badge accent">SEAL Hackathon · Spring 2026</span>

							<h1 className="hero-title">
								<span>Mastering</span>
								<span className="grad-text">Domain-Specific AI</span>
								<span>RAG Systems</span>
							</h1>

							<p className="lead">
								Sân chơi học thuật dành cho sinh viên đam mê công nghệ — nơi bạn
								xây dựng các hệ thống AI RAG có khả năng truy xuất tri thức
								chuyên ngành và tạo ra giải pháp công nghệ thực tiễn.
							</p>

							{!isLoggedIn && (
								<div className="hero-cta">
									<button
										className="btn btn-primary btn-lg"
										onClick={() => setModal("register")}
									>
										Đăng ký tham gia
									</button>

									<button
										className="btn btn-outline btn-lg"
										onClick={() => setModal("login")}
									>
										Tôi đã có tài khoản
									</button>
								</div>
							)}

							<div className="stats">
								<div className="stat">
									<strong>3–5</strong>
									<span>Members / Team</span>
								</div>

								<div className="stat">
									<strong>48h</strong>
									<span>Hackathon</span>
								</div>

								<div className="stat">
									<strong>FPT HCM</strong>
									<span>Campus</span>
								</div>
							</div>
						</div>

						<div className="hero-poster">
							<img src={posterImg} alt="SEAL Hackathon Spring 2026 Poster" />

							<div className="poster-overlay">
								<span>AI • RAG • Innovation</span>
							</div>
						</div>
					</div>
				</section>

				{/* ABOUT */}
				<section className="section" id="about">
					<div className="section-head">
						<h2>Về SEAL Hackathon</h2>
						<p>
							Mùa hai của hành trình đào tạo và trải nghiệm AI thực chiến dành
							cho sinh viên Công nghệ thông tin.
						</p>
					</div>

					<div className="features-grid">
						{[
							{
								title: "Bài toán AI ứng dụng",
								desc: "Thử sức với những bài toán dữ liệu và AI mang tính ứng dụng cao theo từng domain chuyên sâu.",
							},
							{
								title: "Xây dựng hệ thống RAG",
								desc: "Rèn luyện tư duy thiết kế hệ thống Retrieval-Augmented Generation và tối ưu độ chính xác của mô hình.",
							},
							{
								title: "Mentor trực tiếp",
								desc: "Nhận hướng dẫn từ giảng viên Bộ môn Kỹ thuật phần mềm — Trường Đại học FPT.",
							},
							{
								title: "Cộng đồng builder",
								desc: "Kết nối với cộng đồng sinh viên công nghệ và cùng đội nhóm tạo nên sản phẩm đột phá.",
							},
						].map((f) => (
							<div className="feature-card" key={f.title}>
								<h3>{f.title}</h3>
								<p>{f.desc}</p>
							</div>
						))}
					</div>
				</section>

				{/* SCHEDULE */}
				<section className="section" id="schedule">
					<div className="section-head">
						<h2>Lịch trình cuộc thi</h2>
						<p>
							3 mốc quan trọng bạn không thể bỏ lỡ — từ workshop đến đêm trao
							giải.
						</p>
					</div>

					<div className="schedule">
						<div className="schedule-item">
							<span className="date">09 / 04 / 2026</span>
							<h3>Workshop: The RAG Revolution</h3>
							<p>
								"Transforming Complex Data into Actionable Domain Insights" —
								nền tảng cho hành trình hackathon.
							</p>
							<div className="meta">20:00 – 21:30 · Online</div>
						</div>

						<div className="schedule-item">
							<span className="date">11 / 04 / 2026</span>
							<h3>Khai mạc &amp; Chia track</h3>
							<p>
								Lễ khai mạc chính thức, công bố track thi đấu và buổi gặp gỡ
								giữa các đội.
							</p>
							<div className="meta">Offline · FPT University HCM</div>
						</div>

						<div className="schedule-item">
							<span className="date">12 / 04 / 2026</span>
							<h3>Hackathon &amp; Bế mạc</h3>
							<p>
								Ngày thi đấu chính thức, demo sản phẩm, chấm điểm và lễ trao
								giải.
							</p>
							<div className="meta">Offline · FPT University HCM</div>
						</div>
					</div>

					<div className="location-card">
						<div className="loc-info">
							<div className="loc-text">
								<strong>Địa điểm — Trường Đại học FPT TP.HCM</strong>

								<span>
									Lô E2a-7, đường D1, Khu CNC, phường Tăng Nhơn Phú, TP.HCM
								</span>
							</div>
						</div>
					</div>
				</section>

				{/* GALLERY */}
				<section className="section" id="gallery">
					<div className="section-head">
						<h2>Khoảnh khắc SEAL Hackathon</h2>

						<p>
							Hình ảnh từ mùa Fall 2025 — sân chơi nơi ý tưởng trở thành sản
							phẩm.
						</p>
					</div>

					<div className="gallery">
						<div className="gallery-item large">
							<img src={eventImg} alt="SEAL Hackathon - khán giả và sân khấu" />
						</div>

						<div className="gallery-item">
							<img src={speakerImg} alt="SEAL Hackathon - diễn giả phát biểu" />
						</div>
					</div>
				</section>
			</main>

			<SiteFooter />

			<LoginModal
				isOpen={modal === "login"}
				onClose={() => setModal(null)}
				onSwitchToRegister={() => setModal("register")}
				onSwitchToReset={() => setModal("reset")}
			/>

			<RegisterModal
				isOpen={modal === "register"}
				onClose={() => setModal(null)}
				onSwitchToLogin={() => setModal("login")}
			/>

			<ResetPasswordModal
				isOpen={modal === "reset"}
				onClose={() => setModal(null)}
				onSwitchToLogin={() => setModal("login")}
			/>
		</>
	);
}
