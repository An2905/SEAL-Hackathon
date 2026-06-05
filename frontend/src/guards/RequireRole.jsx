import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// FIX: Map từ role alias dùng trong route props sang role thực tế BE trả về trong JWT.
// App.jsx dùng role="Staff" / "Student" / "Mentor" / "Judge" cho dễ đọc,
// còn BE trả về COORDINATOR / STUDENT_* / EXPERT_INTERNAL / EXPERT_EXTERNAL.
const ROLE_ALIASES = {
  Staff: ['COORDINATOR'],
  Student: ['STUDENT_FPT', 'STUDENT_EXTERNAL'],
  Mentor: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL'],
  Judge: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL'],
  Expert: ['EXPERT_INTERNAL', 'EXPERT_EXTERNAL']
}

export default function RequireRole({ role, children }) {
  const { auth, isLoggedIn, pathForRole } = useAuth()

  if (!isLoggedIn) return <Navigate to='/' replace />

  const allowed = ROLE_ALIASES[role] ?? [role]
  if (!allowed.includes(auth.role)) {
    return <Navigate to={pathForRole(auth.role)} replace />
  }

  return children
}
