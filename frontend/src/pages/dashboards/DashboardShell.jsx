import DashboardLayout from '../../components/layout/DashboardLayout'

/**
 * DashboardShell
 *
 * Compatibility wrapper around DashboardLayout for standalone full-page
 * routes (no tabs) — maps the legacy title/subtitle props to
 * moduleTitle/moduleSubtitle.
 */
export default function DashboardShell({
  roleLabel,
  title,
  subtitle,
  showStudentFields = false,
  showStaffFields = false,
  children
}) {
  return (
    <DashboardLayout
      roleLabel={roleLabel}
      moduleTitle={title}
      moduleSubtitle={subtitle}
      showStudentFields={showStudentFields}
      showStaffFields={showStaffFields}
    >
      {children}
    </DashboardLayout>
  )
}
