const ERROR_MAP = {
  NETWORK: 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.',
  TOKEN_EXPIRED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  HTTP_401: 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.',
  HTTP_403: 'Bạn không có quyền thực hiện thao tác này.',
  HTTP_404: 'Không tìm thấy tài nguyên yêu cầu.',
  HTTP_500: 'Lỗi máy chủ. Vui lòng thử lại sau.',
  'Internal server error': 'Lỗi máy chủ. Vui lòng thử lại sau.',
  'Internal Server Error': 'Lỗi máy chủ. Vui lòng thử lại sau.',
  'Invalid or missing token.': 'Token không hợp lệ hoặc bị thiếu.',
  'Forbidden access.': 'Bạn không có quyền thực hiện thao tác này.',
  'Access Denied: Missing role.': 'Không có quyền truy cập (thiếu vai trò).',
  'Invalid captcha.': 'Captcha không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.',
  'No registration request found. Please try again.':
    'Phiên đăng ký đã hết. Vui lòng quay lại bước 1, gửi lại OTP và nhập mã ngay (trong 5 phút).',
  'No OTP request found. Please request a new OTP.': 'Phiên đặt lại mật khẩu đã hết. Vui lòng gửi lại OTP.',
  'This GitHub account is already linked to another user.': 'Tài khoản GitHub này đã được liên kết với sinh viên khác.',
  'GitHub OAuth is required before joining or creating a team.':
    'Bạn cần liên kết GitHub trước khi tạo hoặc tham gia đội.',
  'GitHub account is already linked.': 'Tài khoản GitHub đã được liên kết.',
  'Team name already exists. Please choose a different name.': 'Tên đội đã tồn tại. Vui lòng chọn tên khác.',
  'Team name cannot be empty.': 'Tên đội không được để trống.',
  'Team name must be at most 100 characters.': 'Tên đội tối đa 100 ký tự.',
  'Only winners from the previous round can be assigned to this round.':
    'Chỉ đội winner vòng trước mới được thêm vào vòng này.',
  'Repository name is required.': 'Tên repository là bắt buộc.',
  'per_page must be between 1 and 100.': 'per_page phải từ 1 đến 100.',
  'page must be at least 1.': 'page phải từ 1 trở lên.',
  'ref is required.': 'Thiếu mã commit (ref).',
  'GitHub App lacks permission to list commits (Contents: Read required).':
    'GitHub App thiếu quyền xem commit (cần Contents: Read).',
  'Failed to generate GitHub JWT': 'Không tạo được GitHub JWT. Kiểm tra cấu hình GITHUB_PRIVATE_KEY.',
  'Invalid email or password.': 'Email hoặc mật khẩu không đúng.',
  'Account is not approved.': 'Tài khoản chưa được duyệt.',
  'Validation failed': 'Dữ liệu không hợp lệ.'
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

  // Tra bảng mã lỗi chuẩn (khớp exact)
  if (ERROR_MAP[message]) return ERROR_MAP[message]

  // Một số message BE kèm suffix động (owner/repo, HTTP status...)
  if (/^Repository not found or GitHub App cannot access it:/i.test(message)) {
    return message.replace(
      /^Repository not found or GitHub App cannot access it:/i,
      'Không tìm thấy repository hoặc GitHub App không có quyền truy cập:'
    )
  }
  if (/^Failed to list commits from GitHub/i.test(message)) {
    return message.replace(
      /^Failed to list commits from GitHub/i,
      'Không lấy được danh sách commit từ GitHub'
    )
  }
  if (/^Failed to generate GitHub JWT/i.test(message)) {
    return 'Không tạo được GitHub JWT. Kiểm tra cấu hình GITHUB_PRIVATE_KEY.'
  }

  // Nếu là lỗi kỹ thuật (SQL, stack trace...) → ẩn đi, hiện message chung
  if (isTechnicalError(message)) return 'Lỗi máy chủ. Vui lòng thử lại sau.'

  // Pass-through message tiếng Việt từ server
  if (/[àáâãèéêìíòóôõùúýăđơưạặắẳẵặẹẻẽếệỉịọỏốộờởỡợụủứừ]/i.test(message)) return message

  // Các HTTP status code dạng số
  if (/^4\d\d$/.test(message.trim())) return 'Yêu cầu không hợp lệ.'
  if (/^5\d\d$/.test(message.trim())) return 'Lỗi máy chủ. Vui lòng thử lại sau.'

  // Message tiếng Anh còn lại — không để lộ nguyên văn ra UI
  return 'Đã xảy ra lỗi. Vui lòng thử lại.'
}
