// src/pages/judge/JudgeCriteriaView.jsx
// JUDGE — Xem tiêu chí chấm điểm của round được assign
// Dùng trong JudgeDashboard, nhận props: roundId (string), roundName (string optional)

import { useState, useEffect } from 'react';
import { getCriteriaForJudge } from '../../../api/judge';

// ─── Thanh trọng số mini trong mỗi card ─────────────────────────────────────
function MiniWeightBar({ weight, total }) {
  const pct = total > 0 ? (weight / total) * 100 : 0;
  return (
    <div style={miniBar.track}>
      <div style={{ ...miniBar.fill, width: `${pct}%` }} title={`${weight}% / ${total}%`} />
    </div>
  );
}

const miniBar = {
  track: { background: '#e0e7ff', borderRadius: 99, height: 6, marginTop: 6 },
  fill: { height: '100%', borderRadius: 99, background: '#4f46e5', transition: 'width 0.4s' },
};

// ─── Skeleton loader ─────────────────────────────────────────────────────────
function Skeleton() {
  return (
    <div style={styles.skeletonWrap}>
      {[1, 2, 3].map((n) => (
        <div key={n} style={styles.skeletonCard}>
          <div style={{ ...styles.skeletonLine, width: '60%', height: 16 }} />
          <div style={{ ...styles.skeletonLine, width: '40%', height: 12, marginTop: 8 }} />
        </div>
      ))}
    </div>
  );
}

// ─── Card tiêu chí (read-only) ───────────────────────────────────────────────
function CriteriaCard({ c, index, totalWeight }) {
  const [open, setOpen] = useState(false);

  return (
    <div style={styles.card}>
      <div style={styles.cardTop}>
        {/* Index */}
        <div style={styles.indexBadge}>{index + 1}</div>

        <div style={{ flex: 1 }}>
          <div style={styles.cardName}>{c.criterionName}</div>
          <div style={styles.cardStats}>
            <span style={styles.chip}>⚖️ Trọng số: <b>{c.weight}%</b></span>
            <span style={styles.chip}>🎯 Tối đa: <b>{c.maxScore} điểm</b></span>
          </div>
          <MiniWeightBar weight={c.weight} total={totalWeight} />
        </div>

        {c.description && (
          <button style={styles.toggleBtn} onClick={() => setOpen((o) => !o)}>
            {open ? '▲' : '▼'}
          </button>
        )}
      </div>

      {open && c.description && (
        <div style={styles.desc}>{c.description}</div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// Main Component
// ═══════════════════════════════════════════════════════════════════════════

export default function JudgeCriteriaView({ roundId, roundName }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!roundId) return;
    setLoading(true);
    setError('');
    getCriteriaForJudge(roundId)
      .then(setData)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [roundId]);

  if (!roundId)
    return <div style={styles.empty}>Vui lòng chọn vòng thi để xem tiêu chí.</div>;

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <h2 style={styles.title}>📐 Tiêu chí chấm điểm</h2>
          {roundName && <p style={styles.subtitle}>Vòng thi: <b>{roundName}</b></p>}
        </div>

        {data && (
          <div style={styles.summaryBox}>
            <div style={styles.summaryRow}>
              <span style={styles.summaryLabel}>Số tiêu chí</span>
              <span style={styles.summaryValue}>{data.count}</span>
            </div>
            <div style={styles.summaryRow}>
              <span style={styles.summaryLabel}>Tổng trọng số</span>
              <span style={{
                ...styles.summaryValue,
                color: data.totalWeight === 100 ? '#059669' : '#d97706',
              }}>
                {data.totalWeight}%
              </span>
            </div>
            <div style={styles.summaryRow}>
              <span style={styles.summaryLabel}>Điểm tối đa</span>
              <span style={styles.summaryValue}>{data.totalMaxScore}</span>
            </div>
          </div>
        )}
      </div>

      {/* States */}
      {loading && <Skeleton />}

      {error && (
        <div style={styles.errorBox}>
          <span>⚠️</span> {error}
        </div>
      )}

      {!loading && !error && data?.criteria?.length === 0 && (
        <div style={styles.empty}>
          <div style={{ fontSize: 40, marginBottom: 8 }}>📭</div>
          <p>Vòng thi này chưa có tiêu chí chấm điểm.</p>
          <p style={{ fontSize: 12, color: '#9ca3af' }}>Ban tổ chức sẽ cập nhật sớm.</p>
        </div>
      )}

      {!loading && !error && data?.criteria?.length > 0 && (
        <>
          {/* Note nếu tổng weight != 100 */}
          {data.totalWeight !== 100 && (
            <div style={styles.warningBox}>
              ⚠️ Tổng trọng số đang là <b>{data.totalWeight}%</b>, chưa đủ 100% —
              ban tổ chức có thể điều chỉnh thêm.
            </div>
          )}

          {/* Danh sách */}
          <div style={styles.list}>
            {data.criteria.map((c, i) => (
              <CriteriaCard
                key={c.criteriaId}
                c={c}
                index={i}
                totalWeight={data.totalWeight}
              />
            ))}
          </div>

          {/* Footer tổng kết */}
          <div style={styles.footer}>
            <span>Tổng trọng số: <b>{data.totalWeight}%</b></span>
            <span>Điểm tối đa tổng cộng: <b>{data.totalMaxScore} điểm</b></span>
          </div>
        </>
      )}
    </div>
  );
}

// ─── Styles ──────────────────────────────────────────────────────────────────

const styles = {
  container: {
    background: '#fff',
    borderRadius: 12,
    padding: 24,
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    flexWrap: 'wrap',
    gap: 16,
    marginBottom: 20,
  },
  title: { margin: 0, fontSize: 20, fontWeight: 700, color: '#111827' },
  subtitle: { margin: '4px 0 0', fontSize: 14, color: '#6b7280' },
  summaryBox: {
    background: '#f5f3ff',
    border: '1px solid #ddd6fe',
    borderRadius: 10,
    padding: '12px 20px',
    display: 'flex',
    gap: 24,
  },
  summaryRow: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 },
  summaryLabel: { fontSize: 11, color: '#7c3aed', fontWeight: 600, textTransform: 'uppercase' },
  summaryValue: { fontSize: 20, fontWeight: 700, color: '#4f46e5' },
  list: { display: 'flex', flexDirection: 'column', gap: 10 },
  card: {
    border: '1px solid #e5e7eb',
    borderRadius: 10,
    padding: '14px 16px',
    background: '#fafafa',
    transition: 'box-shadow 0.2s',
  },
  cardTop: { display: 'flex', alignItems: 'flex-start', gap: 12 },
  indexBadge: {
    minWidth: 28,
    height: 28,
    borderRadius: '50%',
    background: '#4f46e5',
    color: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 13,
    fontWeight: 700,
    flexShrink: 0,
    marginTop: 2,
  },
  cardName: { fontSize: 15, fontWeight: 600, color: '#111827' },
  cardStats: { display: 'flex', gap: 8, marginTop: 6, flexWrap: 'wrap' },
  chip: {
    display: 'inline-block',
    background: '#ede9fe',
    color: '#5b21b6',
    borderRadius: 6,
    padding: '2px 8px',
    fontSize: 12,
    fontWeight: 500,
  },
  toggleBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    color: '#6b7280',
    fontSize: 12,
    padding: 4,
    flexShrink: 0,
  },
  desc: {
    marginTop: 10,
    paddingTop: 10,
    borderTop: '1px dashed #e5e7eb',
    fontSize: 13,
    color: '#4b5563',
    lineHeight: 1.6,
  },
  footer: {
    marginTop: 16,
    paddingTop: 14,
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: 13,
    color: '#6b7280',
  },
  errorBox: {
    background: '#fef2f2',
    border: '1px solid #fecaca',
    borderRadius: 8,
    padding: '12px 16px',
    color: '#dc2626',
    fontSize: 14,
  },
  warningBox: {
    background: '#fffbeb',
    border: '1px solid #fde68a',
    borderRadius: 8,
    padding: '10px 14px',
    color: '#92400e',
    fontSize: 13,
    marginBottom: 12,
  },
  empty: {
    textAlign: 'center',
    padding: '40px 0',
    color: '#9ca3af',
    fontSize: 14,
  },
  skeletonWrap: { display: 'flex', flexDirection: 'column', gap: 10 },
  skeletonCard: {
    border: '1px solid #f3f4f6',
    borderRadius: 10,
    padding: 16,
    background: '#f9fafb',
  },
  skeletonLine: {
    background: 'linear-gradient(90deg, #e5e7eb 25%, #f3f4f6 50%, #e5e7eb 75%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s infinite',
    borderRadius: 4,
  },
};
