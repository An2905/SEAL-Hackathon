import DashboardLayout from '../../components/layout/DashboardLayout'

/**
 * DashboardShell
 *
 * Compatibility wrapper around DashboardLayout for standalone full-page
 * routes (no tabs) — maps the legacy title/subtitle props to
 * moduleTitle/moduleSubtitle. Chrome comes from MainLayout.
 */
export default function DashboardShell({ title, subtitle, children }) {
  return (
    <DashboardLayout moduleTitle={title} moduleSubtitle={subtitle}>
      {children}
    </DashboardLayout>
  )
}
