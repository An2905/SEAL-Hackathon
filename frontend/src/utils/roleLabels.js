export const ROLE_UI_LABELS = {
  COORDINATOR: 'Staff',
  EXPERT: 'Khách',
  EXPERT_INTERNAL: 'Khách',
  EXPERT_EXTERNAL: 'Khách',
  STUDENT_FPT: 'Student',
  STUDENT_EXTERNAL: 'Student',
  MENTOR: 'Mentor',
  JUDGE_INTERNAL: 'Judge',
  JUDGE: 'Judge'
}

export function roleUiLabel(role) {
  if (!role) return ''
  return ROLE_UI_LABELS[role] ?? ''
}

// Vietnamese role label for the unified AccountDropdown header
export const ROLE_VI_LABELS = {
  COORDINATOR: 'Nhân viên',
  EXPERT_INTERNAL: 'Cố vấn / Giám khảo',
  EXPERT_EXTERNAL: 'Cố vấn',
  STUDENT_FPT: 'Sinh viên',
  STUDENT_EXTERNAL: 'Sinh viên'
}

export function vietnameseRoleLabel(role) {
  return ROLE_VI_LABELS[role] || 'Người dùng'
}

export const STUDENT_ROLES = ['STUDENT_FPT', 'STUDENT_EXTERNAL']
