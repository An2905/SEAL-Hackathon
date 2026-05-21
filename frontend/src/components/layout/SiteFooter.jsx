export default function SiteFooter() {
	return (
		<footer className="site-footer" id="contact">
			<div className="footer-inner">
				<div className="footer-col">
					<a href="/" className="footer-brand">
						<img
							src="/assets/images/fpt-logo.png"
							alt="FPT University"
							className="brand-logo"
						/>
						<span className="brand-text">
							<strong>SEAL Hackathon</strong>
							<small>Software Engineering · Agile League</small>
						</span>
					</a>
					<p className="footer-about">
						Sân chơi học thuật và trải nghiệm công nghệ dành cho sinh viên ngành
						CNTT tại trường Đại học FPT và các trường trên địa bàn TP.HCM.
					</p>
				</div>

				<div className="footer-col">
					<h4>Cuộc thi</h4>
					<ul className="footer-list">
						<li>
							<a href="/#about">Giới thiệu</a>
						</li>
						<li>
							<a href="/#schedule">Lịch trình</a>
						</li>
						<li>
							<a href="/#gallery">Khoảnh khắc</a>
						</li>
					</ul>
				</div>

				<div className="footer-col">
					<h4>Đối tượng</h4>
					<ul className="footer-list">
						<li>Sinh viên Đại học FPT TP.HCM</li>
						<li>Sinh viên các trường ĐH tại TP.HCM</li>
						<li>Mỗi đội: 3 – 5 thành viên</li>
					</ul>
				</div>

				<div className="footer-col">
					<h4>Liên hệ</h4>
					<ul className="footer-list">
						<li>
							<strong>Thầy Trương Long</strong>
							<a className="mail" href="mailto:longt5@fe.edu.vn">
								longt5@fe.edu.vn
							</a>
						</li>
						<li>
							<strong>Cô Lê Thị Diệu Ân</strong>
							<a className="mail" href="mailto:anltd3@fe.edu.vn">
								anltd3@fe.edu.vn
							</a>
						</li>
					</ul>
				</div>
			</div>

			<div className="footer-bottom">
				<span>
					© 2026 SEAL Hackathon — Bộ môn Kỹ thuật phần mềm, Trường Đại học FPT.
				</span>
				<span>Made with care by the SEAL team.</span>
			</div>
		</footer>
	);
}
