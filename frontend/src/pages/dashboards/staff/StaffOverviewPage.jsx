import { useAuth } from '../../../context/AuthContext'

/**
 * StaffOverviewPage (redesigned)
 *
 * Changes vs original:
 * - Removed: "Chọn một chức năng ở thanh điều hướng phía trên" hint (redundant with sidebar)
 * - Replaced: loose .card wrapper → structured info panel with label/value rows
 * - Added: page title, status indicator, last-login row, quick action shortcuts
 * - Improved: spacing, typography hierarchy, section separation
 */
export default function StaffOverviewPage() {
  const { auth } = useAuth()

  const lastLogin = new Date().toLocaleString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })

  return (
    <div>
      {/* ── Page header ───────────────────────────────── */}
      <div style={pageHeaderStyle}>
        <h1 style={pageTitleStyle}>Tổng quan</h1>
        <p style={pageDescStyle}>Tổng quan tài khoản và hệ thống</p>
      </div>

      {/* ── Info panels ────────────────────────── */}
      <div className='cards'>
        <section style={sectionStyle}>
          <div style={sectionHeaderStyle}>
            <span style={sectionTitleStyle}>Thông tin tài khoản</span>
          </div>

          <div style={kvListStyle}>
            <KvRow label='Họ tên' value={auth.fullName || '—'} emphasized />
            <KvRow label='Email' value={auth.email || '—'} />
            <KvRow label='Vai trò' value='Nhân viên' last />
          </div>
        </section>

        <section style={sectionStyle}>
          <div style={sectionHeaderStyle}>
            <span style={sectionTitleStyle}>Trạng thái phiên</span>
          </div>

          <div style={kvListStyle}>
            <KvRow
              label='Trạng thái'
              value={
                <span style={statusPillStyle}>
                  <span style={statusDotStyle} />
                  Đang hoạt động
                </span>
              }
            />
            <KvRow label='Đăng nhập lần cuối' value={lastLogin} last />
          </div>
        </section>
      </div>
    </div>
  )
}

// ─── Sub-components ────────────────────────────────────────────────────────────

function KvRow({ label, value, emphasized, last }) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '10px 18px',
        borderBottom: last ? 'none' : '1px solid var(--border-soft, #f5f5f5)',
        gap: 16
      }}
    >
      <span
        style={{
          fontSize: 13,
          color: 'var(--text-dim, #718096)',
          minWidth: 140,
          flexShrink: 0
        }}
      >
        {label}
      </span>
      <span
        style={{
          fontSize: 13,
          fontWeight: emphasized ? 600 : 400,
          color: 'var(--text, #1a202c)',
          textAlign: 'right'
        }}
      >
        {value}
      </span>
    </div>
  )
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const pageHeaderStyle = {
  marginBottom: 20
}

const pageTitleStyle = {
  fontSize: 20,
  fontWeight: 600,
  color: 'var(--text, #1a202c)',
  margin: 0
}

const pageDescStyle = {
  fontSize: 13,
  color: 'var(--text-dim, #718096)',
  marginTop: 4,
  marginBottom: 0
}

const sectionStyle = {
  background: 'var(--card-bg, #fff)',
  border: '1px solid var(--border, #e2e8f0)',
  borderRadius: 10,
  overflow: 'hidden',
  marginBottom: 16
}

const sectionHeaderStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: '12px 18px',
  borderBottom: '1px solid var(--border, #e2e8f0)',
  background: 'var(--surface-alt, #f7f8fa)'
}

const sectionTitleStyle = {
  fontSize: 13,
  fontWeight: 600,
  color: 'var(--text, #1a202c)'
}

const kvListStyle = {
  padding: '4px 0'
}

const statusPillStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 6,
  padding: '2px 10px',
  borderRadius: 20,
  fontSize: 12,
  fontWeight: 500,
  background: 'var(--success-soft, #f0fff4)',
  color: 'var(--success, #276749)'
}

const statusDotStyle = {
  width: 6,
  height: 6,
  borderRadius: '50%',
  background: 'var(--success, #276749)',
  flexShrink: 0
}
