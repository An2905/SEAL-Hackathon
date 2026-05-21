const ERROR_MAP = {
	NETWORK: "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.",
	HTTP_401: "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.",
	HTTP_403: "Bạn không có quyền thực hiện thao tác này.",
	HTTP_404: "Không tìm thấy tài nguyên yêu cầu.",
	HTTP_500: "Lỗi máy chủ. Vui lòng thử lại sau.",
};

export function localizeError(message = "") {
	if (!message) return "Đã xảy ra lỗi không xác định.";
	if (ERROR_MAP[message]) return ERROR_MAP[message];
	// Pass-through Vietnamese messages from the server directly
	if (/[àáâãèéêìíòóôõùúýăđơưạặắẳẵặẹẻẽếệỉịọỏốộờởỡợụủứừ]/i.test(message))
		return message;
	return message;
}
