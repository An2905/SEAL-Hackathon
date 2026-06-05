export const ROLE_UI_LABELS = {
  COORDINATOR: 'Staff',
  EXPERT: 'Khách',
  EXPERT_INTERNAL: 'Khách (INTERNAL)',
  EXPERT_EXTERNAL: 'Khách (EXTERNAL)',
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
