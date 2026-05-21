import { useEffect, useState } from "react";
import Modal from "../common/Modal";
import FormField from "../common/FormField";
import FormMessage from "../common/FormMessage";
import LoadingButton from "../common/LoadingButton";
import {
	sendRegisterOtp,
	verifyAndRegister,
	login,
} from "../../api/auth";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { useNavigate } from "react-router-dom";
import { localizeError } from "../../utils/errors";

const EMPTY_FORM = {
	fullName: "",
	email: "",
	uni: "",
	studentId: "",
	password: "",
};

export default function RegisterModal({ isOpen, onClose, onSwitchToLogin }) {
	const { saveAuth, pathForRole } = useAuth();
	const { showToast } = useToast();
	const navigate = useNavigate();
	const [step, setStep] = useState("info");
	const [loading, setLoading] = useState(false);
	const [message, setMessage] = useState(null);
	const [form, setForm] = useState(EMPTY_FORM);
	const [otp, setOtp] = useState("");

	useEffect(() => {
		if (!isOpen) {
			setStep("info");
			setMessage(null);
			setForm(EMPTY_FORM);
			setOtp("");
			setLoading(false);
		}
	}, [isOpen]);

	const handleChange = (e) =>
		setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

	const handleSubmitInfo = async (e) => {
		e.preventDefault();
		setMessage(null);
		const { fullName, email, uni, studentId, password } = form;
		if (!fullName || !email || !uni || !studentId || !password) {
			setMessage({ text: "Vui lòng nhập đầy đủ thông tin", type: "error" });
			return;
		}
		if (password.length < 6) {
			setMessage({ text: "Mật khẩu phải có ít nhất 6 ký tự", type: "error" });
			return;
		}
		setLoading(true);
		try {
			await sendRegisterOtp(form);
			setStep("otp");
			setMessage({
				text: "Đã gửi mã OTP tới email. Vui lòng kiểm tra hộp thư.",
				type: "success",
			});
		} catch (err) {
			setMessage({ text: localizeError(err.message), type: "error" });
		} finally {
			setLoading(false);
		}
	};

	const handleSubmitOtp = async (e) => {
		e.preventDefault();
		setMessage(null);
		const code = otp.trim();
		if (!/^\d{6}$/.test(code)) {
			setMessage({ text: "Mã OTP gồm 6 chữ số", type: "error" });
			return;
		}
		setLoading(true);
		try {
			await verifyAndRegister({ email: form.email, otp: code });
			setMessage({
				text: "Tạo tài khoản thành công! Đang đăng nhập...",
				type: "success",
			});
			showToast("Tạo tài khoản thành công!", "success");

			try {
				const result = await login({
					email: form.email,
					password: form.password,
				});
				if (result.ok) {
					saveAuth({
						token: result.token,
						email: form.email,
						role: result.role,
					});
					setTimeout(() => {
						onClose();
						navigate(pathForRole(result.role));
					}, 500);
					return;
				}
			} catch (_) {
				/* fall through to login modal */
			}

			setTimeout(() => {
				onClose();
				onSwitchToLogin();
			}, 800);
		} catch (err) {
			setMessage({ text: localizeError(err.message), type: "error" });
		} finally {
			setLoading(false);
		}
	};

	const handleResend = async () => {
		setMessage(null);
		setLoading(true);
		try {
			await sendRegisterOtp(form);
			setMessage({ text: "Đã gửi lại mã OTP.", type: "success" });
		} catch (err) {
			setMessage({ text: localizeError(err.message), type: "error" });
		} finally {
			setLoading(false);
		}
	};

	return (
		<Modal
			isOpen={isOpen}
			onClose={onClose}
			title={step === "info" ? "Tạo tài khoản" : "Xác thực email"}
			subtitle={
				step === "info"
					? "Tham gia SEAL Hackathon ngay hôm nay."
					: `Nhập mã OTP đã gửi tới ${form.email}.`
			}
		>
			{step === "info" ? (
				<form className="form" onSubmit={handleSubmitInfo} noValidate>
					<FormField label="Họ và tên">
						<input
							type="text"
							name="fullName"
							value={form.fullName}
							onChange={handleChange}
							required
							placeholder="Nguyễn Văn A"
						/>
					</FormField>
					<FormField label="Email">
						<input
							type="email"
							name="email"
							value={form.email}
							onChange={handleChange}
							required
							placeholder="ban@fpt.edu.vn"
							autoComplete="email"
						/>
					</FormField>
					<div className="field-row">
						<FormField label="Trường">
							<input
								type="text"
								name="uni"
								value={form.uni}
								onChange={handleChange}
								required
								placeholder="FPT University"
							/>
						</FormField>
						<FormField label="Mã sinh viên">
							<input
								type="text"
								name="studentId"
								value={form.studentId}
								onChange={handleChange}
								required
								placeholder="SE123456"
							/>
						</FormField>
					</div>
					<FormField label="Mật khẩu">
						<input
							type="password"
							name="password"
							value={form.password}
							onChange={handleChange}
							required
							minLength={6}
							placeholder="Tối thiểu 6 ký tự"
							autoComplete="new-password"
						/>
					</FormField>
					<LoadingButton loading={loading} type="submit">
						Gửi mã OTP
					</LoadingButton>
					<FormMessage message={message?.text} type={message?.type} />
					<p className="form-footer">
						Đã có tài khoản?{" "}
						<a
							href="#"
							onClick={(e) => {
								e.preventDefault();
								onSwitchToLogin();
							}}
						>
							Đăng nhập
						</a>
					</p>
				</form>
			) : (
				<form className="form" onSubmit={handleSubmitOtp} noValidate>
					<FormField label="Mã OTP (6 chữ số)">
						<input
							type="text"
							inputMode="numeric"
							pattern="\d{6}"
							maxLength={6}
							value={otp}
							onChange={(e) =>
								setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
							}
							required
							placeholder="000000"
							autoFocus
						/>
					</FormField>
					<LoadingButton loading={loading} type="submit">
						Xác thực &amp; tạo tài khoản
					</LoadingButton>
					<FormMessage message={message?.text} type={message?.type} />
					<p className="form-footer">
						Không nhận được mã?{" "}
						<a
							href="#"
							onClick={(e) => {
								e.preventDefault();
								if (!loading) handleResend();
							}}
						>
							Gửi lại OTP
						</a>
						{" · "}
						<a
							href="#"
							onClick={(e) => {
								e.preventDefault();
								setStep("info");
								setMessage(null);
							}}
						>
							Sửa thông tin
						</a>
					</p>
				</form>
			)}
		</Modal>
	);
}
