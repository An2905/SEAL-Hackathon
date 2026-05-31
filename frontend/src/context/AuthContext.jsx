import { createContext, useContext, useState, useCallback } from 'react'
import { parseJwt } from '../utils/jwt'

const AuthContext = createContext(null)

const STORAGE_KEYS = {
  token: 'hh_token',
  email: 'hh_email',
  role: 'hh_role',
  fullName: 'hh_full_name'
}

// FIX: Keys khớp với giá trị role thực tế BE trả về trong JWT claim.
// BE dùng: COORDINATOR, MENTOR, JUDGE_INTERNAL, STUDENT_FPT, STUDENT_EXTERNAL
// Code cũ dùng "Staff", "Student"... không khớp → sau login bị redirect về "/" thay vì dashboard đúng.
const ROLE_PATHS = {
  COORDINATOR: '/staff',
  MENTOR: '/mentor',
  JUDGE_INTERNAL: '/judge',
  STUDENT_FPT: '/student',
  STUDENT_EXTERNAL: '/student'
}

// Label hiển thị trên UI theo role
const ROLE_DISPLAY_LABELS = {
  COORDINATOR: 'Trang Staff',
  MENTOR: 'Trang Mentor',
  JUDGE_INTERNAL: 'Trang Giám khảo',
  STUDENT_FPT: 'Trang Sinh viên',
  STUDENT_EXTERNAL: 'Trang Sinh viên'
}

export function AuthProvider({ children }) {
  const [auth, setAuthState] = useState(() => ({
    token: localStorage.getItem(STORAGE_KEYS.token) || '',
    email: localStorage.getItem(STORAGE_KEYS.email) || '',
    role: localStorage.getItem(STORAGE_KEYS.role) || '',
    fullName: localStorage.getItem(STORAGE_KEYS.fullName) || ''
  }))

  const saveAuth = useCallback((patch = {}) => {
    const { token, email, role, fullName } = patch

    // Khi có token mới, ưu tiên lấy fullName/email từ JWT claims
    // (BE nhúng fullName vào JWT — xem JwtUtil.generateToken)
    let derivedFullName = fullName
    let derivedEmail = email
    if (token) {
      const claims = parseJwt(token)
      if (claims) {
        if (derivedFullName == null && claims.fullName) derivedFullName = claims.fullName
        if (derivedEmail == null && claims.sub) derivedEmail = claims.sub
      }
    }

    if (token != null) localStorage.setItem(STORAGE_KEYS.token, token)
    if (derivedEmail != null) localStorage.setItem(STORAGE_KEYS.email, derivedEmail)
    if (role != null) localStorage.setItem(STORAGE_KEYS.role, role)
    if (derivedFullName != null) localStorage.setItem(STORAGE_KEYS.fullName, derivedFullName)

    setAuthState((prev) => ({
      token: token != null ? token : prev.token,
      email: derivedEmail != null ? derivedEmail : prev.email,
      role: role != null ? role : prev.role,
      fullName: derivedFullName != null ? derivedFullName : prev.fullName
    }))
  }, [])

  const clearAuth = useCallback(() => {
    Object.values(STORAGE_KEYS).forEach((k) => localStorage.removeItem(k))
    setAuthState({ token: '', email: '', role: '', fullName: '' })
  }, [])

  const isLoggedIn = !!auth.token

  // Trả về đường dẫn dashboard theo role BE, fallback về "/" nếu role không xác định
  const pathForRole = (role) => ROLE_PATHS[role] || '/'

  // Trả về label hiển thị trên nút "Vào dashboard" trong HomeNavbar
  const labelForRole = (role) => ROLE_DISPLAY_LABELS[role] || 'Vào dashboard'

  return (
    <AuthContext.Provider value={{ auth, saveAuth, clearAuth, isLoggedIn, pathForRole, labelForRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
