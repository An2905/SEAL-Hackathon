const ERROR_MAP = {
  NETWORK: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.',
  HTTP_401: 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.',
  HTTP_403: 'Bạn không có quyền thực hiện thao tác này.',
  HTTP_404: 'Không tìm thấy tài nguyên yêu cầu.',
  HTTP_500: 'Lỗi máy chủ. Vui lòng thử lại sau.',
  'Invalid captcha.': 'Captcha không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.',
  'Team name already exists. Please choose a different name.': 'Tên đội đã tồn tại. Vui lòng chọn tên khác.',
  'Team name cannot be empty.': 'Tên đội không được để trống.',
  'Team name must be at most 100 characters.': 'Tên đội tối đa 100 ký tự.',
  'Only winners from the previous round can be assigned to this round.':
    'Chỉ đội winner vòng trước mới được thêm vào vòng này.'
}

// Các pattern kỹ thuật không được hiển thị trực tiếp ra UI
const TECHNICAL_PATTERNS = [
  /SELECT\s+\w/i, // SQL SELECT query
  /INSERT\s+INTO/i, // SQL INSERT
  /UPDATE\s+\w+\s+SET/i, // SQL UPDATE
  /DELETE\s+FROM/i, // SQL DELETE
  /WHERE\s+\w+\s*=/i, // SQL WHERE clause
  /FROM\s+\w+/i, // SQL FROM
  /NullPointerException/i, // Java NPE
  /SQLException/i, // Java SQL exception
  /at\s+[\w.]+\(\w+\.java/i, // Java stack trace
  /org\.springframework/i, // Spring framework error
  /com\.example\./i, // Java package trace
  /Caused by:/i // Java exception chain
]

function isTechnicalError(message) {
  return TECHNICAL_PATTERNS.some((pattern) => pattern.test(message))
}

export function localizeError(message = '') {
  if (!message) return 'Đã xảy ra lỗi không xác định.'

  // Tra bảng mã lỗi chuẩn
  if (ERROR_MAP[message]) return ERROR_MAP[message]

  // Nếu là lỗi kỹ thuật (SQL, stack trace...) → ẩn đi, hiện message chung
  if (isTechnicalError(message)) return 'Lỗi máy chủ. Vui lòng thử lại sau.'

  // Pass-through message tiếng Việt từ server
  if (/[àáâãèéêìíòóôõùúýăđơưạặắẳẵặẹẻẽếệỉịọỏốộờởỡợụủứừ]/i.test(message)) return message

  // Các HTTP status code dạng số
  if (/^4\d\d$/.test(message.trim())) return 'Yêu cầu không hợp lệ.'
  if (/^5\d\d$/.test(message.trim())) return 'Lỗi máy chủ. Vui lòng thử lại sau.'

  // Message tiếng Anh ngắn gọn từ server — cho qua
  return message
}
