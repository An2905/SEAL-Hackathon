// src/pages/staff/CriteriaManager.jsx
// COORDINATOR — Quản lý tiêu chí chấm điểm cho event
// Dùng trong EventSetupPage hoặc EventDetailsPage, nhận props: eventId

import { useState, useEffect, useCallback } from 'react';
import {
  getCriteriaByEvent,
  createCriteria,
  updateCriteria,
  deleteCriteria,
} from '../../api/criteriaApi';

// ─── Palette màu theo trạng thái weight ─────────────────────────────────────
const weightColor = (pct) => {
  if (pct >= 100) return '#ef4444';
  if (pct >= 80) return '#f59e0b';
  return '#10b981';
};

// ─── Component con: Form thêm / sửa ─────────────────────────────────────────
function CriteriaForm({ eventId, initial, onSave, onCancel, remainingWeight }) {
  const [form, setForm] = useState(
    initial ?? { criterionName: '', weight: '', maxScore: '', description: '' }
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const weight = parseFloat(form.weight);
    const maxScore = parseFloat(form.maxScore);

    if (!form.criterionName.trim()) return setError('Tên tiêu chí là bắt buộc');
    if (isNaN(weight) || weight <= 0 || weight > 100)
      return setError('Trọng số phải từ 0.01 đến 100');
    if (isNaN(maxScore) || maxScore <= 0)
      return setError('Điểm tối đa phải lớn hơn 0');

    setLoading(true);
    try {
      const payload = {
        eventId,
        criterionName: form.criterionName.trim(),
        weight,
        maxScore,
        description: form.description,
      };
      await onSave(payload);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={styles.form}>
      <h3 style={styles.formTitle}>
        {initial ? '✏️ Sửa tiêu chí' : '➕ Thêm tiêu chí mới'}
      </h3>

      {error && <div style={styles.error}>{error}</div>}

      <label style={styles.label}>
        Tên tiêu chí <span style={{ color: '#ef4444' }}>*</span>
        <input
          style={styles.input}
          value={form.criterionName}
          onChange={(e) => set('criterionName', e.target.value)}
          placeholder="Ví dụ: Tính sáng tạo"
          maxLength={100}
        />
      </label>

      <div style={{ display: 'flex', gap: 12 }}>
        <label style={{ ...styles.label, flex: 1 }}>
          Trọng số (%) <span style={{ color: '#ef4444' }}>*</span>
          <input
            style={styles.input}
            type="number"
            min="0.01"
            max="100"
            step="0.01"
            value={form.weight}
            onChange={(e) => set('weight', e.target.value)}
            placeholder="30"
          />
          {remainingWeight !== undefined && (
            <span style={styles.hint}>
              Còn có thể phân bổ: <b>{remainingWeight.toFixed(2)}%</b>
            </span>
          )}
        </label>

        <label style={{ ...styles.label, flex: 1 }}>
          Điểm tối đa <span style={{ color: '#ef4444' }}>*</span>
          <input
            style={styles.input}
            type="number"
            min="0.01"
            step="0.01"
            value={form.maxScore}
            onChange={(e) => set('maxScore', e.target.value)}
            placeholder="10"
          />
        </label>
      </div>

      <label style={styles.label}>
        Mô tả
        <textarea
          style={{ ...styles.input, minHeight: 80, resize: 'vertical' }}
          value={form.description}
          onChange={(e) => set('description', e.target.value)}
          placeholder="Mô tả chi tiết tiêu chí chấm điểm..."
        />
      </label>

      <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
        <button type="button" style={styles.btnSecondary} onClick={onCancel}>
          Huỷ
        </button>
        <button type="submit" style={styles.btnPrimary} disabled={loading}>
          {loading ? 'Đang lưu...' : initial ? 'Cập nhật' : 'Thêm tiêu chí'}
        </button>
      </div>
    </form>
  );
}

// ─── Component con: Thanh tổng weight ───────────────────────────────────────
function WeightBar({ total }) {
  const pct = Math.min(total, 100);
  const color = weightColor(pct);

  return (
    <div style={styles.weightBar}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
        <span style={styles.weightLabel}>Tổng trọng số</span>
        <span style={{ ...styles.weightValue, color }}>
          {total.toFixed(2)}% / 100%
        </span>
      </div>
      <div style={styles.barTrack}>
        <div
          style={{
            ...styles.barFill,
            width: `${pct}%`,
            background: color,
            transition: 'width 0.4s ease, background 0.3s',
          }}
        />
      </div>
      {total > 100 && (
        <p style={{ color: '#ef4444', fontSize: 12, marginTop: 4 }}>
          ⚠️ Tổng trọng số đã vượt 100% — vui lòng điều chỉnh
        </p>
      )}
      {total === 100 && (
        <p style={{ color: '#10b981', fontSize: 12, marginTop: 4 }}>
          ✅ Phân bổ trọng số đầy đủ
        </p>
      )}
    </div>
  );
}

// ─── Component con: Card tiêu chí ───────────────────────────────────────────
function CriteriaCard({ c, onEdit, onDelete }) {
  const [confirmDel, setConfirmDel] = useState(false);

  return (
    <div style={styles.card}>
      <div style={styles.cardHeader}>
        <div>
          <span style={styles.cardName}>{c.criterionName}</span>
          <div style={styles.cardMeta}>
            <span style={styles.badge}>⚖️ {c.weight}%</span>
            <span style={styles.badge}>🎯 Tối đa {c.maxScore} điểm</span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={styles.btnIcon} onClick={() => onEdit(c)} title="Sửa">
            ✏️
          </button>
          {confirmDel ? (
            <>
              <button
                style={{ ...styles.btnIcon, background: '#fee2e2', color: '#ef4444' }}
                onClick={() => { onDelete(c.criteriaId); setConfirmDel(false); }}
              >
                ✓ Xác nhận xóa
              </button>
              <button style={styles.btnIcon} onClick={() => setConfirmDel(false)}>
                ✕
              </button>
            </>
          ) : (
            <button
              style={{ ...styles.btnIcon, background: '#fee2e2', color: '#ef4444' }}
              onClick={() => setConfirmDel(true)}
              title="Xóa"
            >
              🗑️
            </button>
          )}
        </div>
      </div>
      {c.description && (
        <p style={styles.cardDesc}>{c.description}</p>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════════════════════════
// Main Component
// ═══════════════════════════════════════════════════════════════════════════

export default function CriteriaManager({ eventId }) {
  const [data, setData] = useState(null);         // { criteria, totalWeight, totalMaxScore }
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState('');

  const [showForm, setShowForm] = useState(false);
  const [editTarget, setEditTarget] = useState(null); // null = thêm mới, object = sửa

  // ─── Load danh sách ────────────────────────────────────────────────────────

  const load = useCallback(async () => {
    setLoading(true);
    setPageError('');
    try {
      const d = await getCriteriaByEvent(eventId);
      setData(d);
    } catch (err) {
      setPageError(err.message);
    } finally {
      setLoading(false);
    }
  }, [eventId]);

  useEffect(() => { load(); }, [load]);

  // ─── Handlers ──────────────────────────────────────────────────────────────

  const handleAdd = async (payload) => {
    await createCriteria(payload);
    setShowForm(false);
    await load();
  };

  const handleUpdate = async (payload) => {
    await updateCriteria(editTarget.criteriaId, payload);
    setEditTarget(null);
    await load();
  };

  const handleDelete = async (criteriaId) => {
    try {
      await deleteCriteria(criteriaId);
      await load();
    } catch (err) {
      setPageError(err.message);
    }
  };

  const openEdit = (c) => {
    setEditTarget(c);
    setShowForm(false);
  };

  // ─── Render ────────────────────────────────────────────────────────────────

  if (loading) return <div style={styles.center}>⏳ Đang tải tiêu chí...</div>;
  if (pageError) return <div style={styles.error}>{pageError}</div>;

  const totalWeight = data?.totalWeight ?? 0;
  const remaining = Math.max(0, 100 - totalWeight);
  const criteria = data?.criteria ?? [];

  return (
    <div style={styles.container}>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <h2 style={styles.title}>📋 Tiêu chí chấm điểm</h2>
          <p style={styles.subtitle}>
            {criteria.length} tiêu chí · Tổng điểm tối đa: {data?.totalMaxScore ?? 0}
          </p>
        </div>
        <button
          style={styles.btnPrimary}
          onClick={() => { setShowForm(true); setEditTarget(null); }}
          disabled={showForm || !!editTarget}
        >
          ＋ Thêm tiêu chí
        </button>
      </div>

      {/* Weight bar */}
      <WeightBar total={totalWeight} />

      {/* Form thêm mới */}
      {showForm && (
        <CriteriaForm
          eventId={eventId}
          remainingWeight={remaining}
          onSave={handleAdd}
          onCancel={() => setShowForm(false)}
        />
      )}

      {/* Danh sách tiêu chí */}
      {criteria.length === 0 && !showForm ? (
        <div style={styles.empty}>
          <span style={{ fontSize: 40 }}>📭</span>
          <p>Chưa có tiêu chí nào. Hãy thêm tiêu chí đầu tiên!</p>
        </div>
      ) : (
        <div style={styles.list}>
          {criteria.map((c) =>
            editTarget?.criteriaId === c.criteriaId ? (
              <CriteriaForm
                key={c.criteriaId}
                eventId={eventId}
                initial={{
                  criterionName: c.criterionName,
                  weight: c.weight,
                  maxScore: c.maxScore,
                  description: c.description ?? '',
                }}
                remainingWeight={remaining + c.weight}
                onSave={handleUpdate}
                onCancel={() => setEditTarget(null)}
              />
            ) : (
              <CriteriaCard
                key={c.criteriaId}
                c={c}
                onEdit={openEdit}
                onDelete={handleDelete}
              />
            )
          )}
        </div>
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
    marginBottom: 20,
  },
  title: { margin: 0, fontSize: 20, fontWeight: 700, color: '#111827' },
  subtitle: { margin: '4px 0 0', color: '#6b7280', fontSize: 14 },
  weightBar: {
    background: '#f9fafb',
    border: '1px solid #e5e7eb',
    borderRadius: 8,
    padding: '12px 16px',
    marginBottom: 20,
  },
  weightLabel: { fontSize: 13, color: '#6b7280', fontWeight: 500 },
  weightValue: { fontSize: 14, fontWeight: 700 },
  barTrack: { background: '#e5e7eb', borderRadius: 99, height: 8 },
  barFill: { height: '100%', borderRadius: 99 },
  list: { display: 'flex', flexDirection: 'column', gap: 10 },
  card: {
    border: '1px solid #e5e7eb',
    borderRadius: 10,
    padding: '14px 16px',
    background: '#fafafa',
  },
  cardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' },
  cardName: { fontSize: 15, fontWeight: 600, color: '#111827' },
  cardMeta: { display: 'flex', gap: 8, marginTop: 6 },
  cardDesc: { margin: '10px 0 0', fontSize: 13, color: '#6b7280', lineHeight: 1.5 },
  badge: {
    display: 'inline-block',
    background: '#eff6ff',
    color: '#2563eb',
    borderRadius: 6,
    padding: '2px 8px',
    fontSize: 12,
    fontWeight: 500,
  },
  form: {
    background: '#f0f9ff',
    border: '1px solid #bae6fd',
    borderRadius: 10,
    padding: 20,
    marginBottom: 12,
  },
  formTitle: { margin: '0 0 16px', fontSize: 16, fontWeight: 600, color: '#0369a1' },
  label: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    fontSize: 13,
    fontWeight: 500,
    color: '#374151',
    marginBottom: 12,
  },
  input: {
    border: '1px solid #d1d5db',
    borderRadius: 8,
    padding: '8px 12px',
    fontSize: 14,
    color: '#111827',
    outline: 'none',
    background: '#fff',
    width: '100%',
    boxSizing: 'border-box',
  },
  hint: { fontSize: 12, color: '#6b7280', marginTop: 2 },
  btnPrimary: {
    background: '#2563eb',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    padding: '9px 18px',
    fontWeight: 600,
    fontSize: 14,
    cursor: 'pointer',
  },
  btnSecondary: {
    background: '#f3f4f6',
    color: '#374151',
    border: '1px solid #d1d5db',
    borderRadius: 8,
    padding: '9px 18px',
    fontWeight: 500,
    fontSize: 14,
    cursor: 'pointer',
  },
  btnIcon: {
    background: '#f3f4f6',
    border: '1px solid #e5e7eb',
    borderRadius: 6,
    padding: '5px 10px',
    cursor: 'pointer',
    fontSize: 13,
  },
  error: {
    background: '#fee2e2',
    color: '#dc2626',
    border: '1px solid #fecaca',
    borderRadius: 8,
    padding: '10px 14px',
    fontSize: 13,
    marginBottom: 12,
  },
  empty: {
    textAlign: 'center',
    color: '#9ca3af',
    padding: '40px 0',
    fontSize: 14,
  },
  center: { textAlign: 'center', padding: 40, color: '#6b7280' },
};
