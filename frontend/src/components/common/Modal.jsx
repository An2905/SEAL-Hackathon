import { useEffect } from "react";

export default function Modal({ isOpen, onClose, title, subtitle, children }) {
	useEffect(() => {
		if (!isOpen) return;
		document.body.style.overflow = "hidden";
		return () => {
			document.body.style.overflow = "";
		};
	}, [isOpen]);

	useEffect(() => {
		const handler = (e) => {
			if (e.key === "Escape") onClose();
		};
		document.addEventListener("keydown", handler);
		return () => document.removeEventListener("keydown", handler);
	}, [onClose]);

	if (!isOpen) return null;

	return (
		<div
			className="modal-overlay"
			onClick={(e) => {
				if (e.target === e.currentTarget) onClose();
			}}
		>
			<div className="modal">
				<button className="modal-close" onClick={onClose} aria-label="Đóng">
					&times;
				</button>
				<h2>{title}</h2>
				{subtitle && <p className="modal-sub">{subtitle}</p>}
				{children}
			</div>
		</div>
	);
}
