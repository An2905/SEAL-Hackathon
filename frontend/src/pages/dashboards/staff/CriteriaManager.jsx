// COORDINATOR — Quản lý tiêu chí chấm điểm theo từng vòng thi

import { useState, useEffect, useCallback, useMemo } from 'react'
import Modal from '../../../components/common/Modal'
import FormField from '../../../components/common/FormField'
import LoadingButton from '../../../components/common/LoadingButton'
import FormMessage from '../../../components/common/FormMessage'
import { getCriteriaByRound, createCriteria, updateCriteria, deleteCriteria } from '../../../api/criteriaApi'

const weightColor = (pct) => {
  if (pct >= 100) return '#ef4444'
  if (pct >= 80) return '#f59e0b'
  return '#10b981'
}

function roundLabel(round) {
  const name = String(round?.name ?? round?.roundName ?? '').trim()
  if (name) return name
  const order = round?.roundOrder
  return order ? `Vòng ${order}` : 'Vòng'
}

function WeightBar({ total }) {
  const pct = Math.min(total, 100)
  const color = weightColor(pct)

  return (
    <div className='criteria-weight-bar'>
      <div className='criteria-weight-bar-head'>
        <span>Tổng trọng số</span>
        <span style={{ color, fontWeight: 700 }}>{total.toFixed(2)}% / 100%</span>
      </div>
      <div className='criteria-weight-track'>
        <div className='criteria-weight-fill' style={{ width: `${pct}%`, background: color }} />
      </div>
      {total > 100 && <p className='criteria-weight-warn'>⚠️ Tổng trọng số đã vượt 100% — vui lòng điều chỉnh</p>}
      {total === 100 && <p className='criteria-weight-ok'>✅ Phân bổ trọng số đầy đủ</p>}
    </div>
  )
}

function CriteriaForm({ roundId, initial, onSave, onCancel, remainingWeight }) {
  const [form, setForm] = useState(initial ?? { criterionName: '', weight: '', maxScore: '', description: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    const weight = parseFloat(form.weight)
    const maxScore = parseFloat(form.maxScore)

    if (!form.criterionName.trim()) return setError('Tên tiêu chí là bắt buộc')
    if (isNaN(weight) || weight <= 0 || weight > 100) return setError('Trọng số phải từ 0.01 đến 100')
    if (isNaN(maxScore) || maxScore <= 0) return setError('Điểm tối đa phải lớn hơn 0')

    setLoading(true)
    try {
      await onSave({
        roundId,
        criterionName: form.criterionName.trim(),
        weight,
        maxScore,
        description: form.description
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className='form criteria-form-panel' onSubmit={handleSubmit}>
      <h4 className='section-subtitle'>{initial ? 'Sửa tiêu chí' : 'Thêm tiêu chí mới'}</h4>
      <FormMessage message={error} type='error' />

      <FormField label='Tên tiêu chí *'>
        <input
          value={form.criterionName}
          onChange={(e) => set('criterionName', e.target.value)}
          placeholder='Ví dụ: Tính sáng tạo'
          maxLength={100}
          disabled={loading}
          required
        />
      </FormField>

      <div className='criteria-form-row'>
        <FormField label='Trọng số (%) *'>
          <input
            type='number'
            min='0.01'
            max='100'
            step='0.01'
            value={form.weight}
            onChange={(e) => set('weight', e.target.value)}
            placeholder='30'
            disabled={loading}
            required
          />
          {remainingWeight !== undefined && (
            <span className='criteria-form-hint'>
              Còn có thể phân bổ: <b>{remainingWeight.toFixed(2)}%</b>
            </span>
          )}
        </FormField>

        <FormField label='Điểm tối đa *'>
          <input
            type='number'
            min='0.01'
            step='0.01'
            value={form.maxScore}
            onChange={(e) => set('maxScore', e.target.value)}
            placeholder='10'
            disabled={loading}
            required
          />
        </FormField>
      </div>

      <FormField label='Mô tả'>
        <textarea
          value={form.description}
          onChange={(e) => set('description', e.target.value)}
          placeholder='Mô tả chi tiết tiêu chí chấm điểm...'
          rows={3}
          disabled={loading}
        />
      </FormField>

      <div className='criteria-form-actions'>
        <button type='button' className='btn btn-ghost' onClick={onCancel} disabled={loading}>
          Huỷ
        </button>
        <LoadingButton loading={loading} type='submit' className='btn btn-primary'>
          {initial ? 'Cập nhật' : 'Thêm tiêu chí'}
        </LoadingButton>
      </div>
    </form>
  )
}

function CriteriaCard({ c, onEdit, onDelete }) {
  const [confirmDel, setConfirmDel] = useState(false)

  return (
    <div className='criteria-card'>
      <div className='criteria-card-head'>
        <div>
          <div className='criteria-card-name'>{c.criterionName}</div>
          <div className='criteria-card-meta'>
            <span className='criteria-badge'>⚖️ {c.weight}%</span>
            <span className='criteria-badge'>🎯 Tối đa {c.maxScore} điểm</span>
          </div>
        </div>
        <div className='criteria-card-actions'>
          <button type='button' className='btn btn-ghost btn-sm' onClick={() => onEdit(c)} title='Sửa'>
            Sửa
          </button>
          {confirmDel ? (
            <>
              <button
                type='button'
                className='btn btn-danger btn-sm'
                onClick={() => {
                  onDelete(c.criteriaId)
                  setConfirmDel(false)
                }}
              >
                Xác nhận
              </button>
              <button type='button' className='btn btn-ghost btn-sm' onClick={() => setConfirmDel(false)}>
                Huỷ
              </button>
            </>
          ) : (
            <button type='button' className='btn btn-danger btn-sm' onClick={() => setConfirmDel(true)} title='Xóa'>
              Xóa
            </button>
          )}
        </div>
      </div>
      {c.description ? <p className='criteria-card-desc'>{c.description}</p> : null}
    </div>
  )
}

function RoundCriteriaModal({ round, isOpen, onClose }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [pageError, setPageError] = useState('')
  const [showAddForm, setShowAddForm] = useState(false)
  const [editTarget, setEditTarget] = useState(null)

  const load = useCallback(async () => {
    if (!round?.roundId) {
      setData(null)
      return
    }
    setLoading(true)
    setPageError('')
    setShowAddForm(false)
    setEditTarget(null)
    try {
      setData(await getCriteriaByRound(round.roundId))
    } catch (err) {
      setPageError(err.message)
      setData(null)
    } finally {
      setLoading(false)
    }
  }, [round?.roundId])

  useEffect(() => {
    if (isOpen && round) load()
  }, [isOpen, round, load])

  const handleClose = () => {
    setShowAddForm(false)
    setEditTarget(null)
    onClose()
  }

  const handleAdd = async (payload) => {
    await createCriteria(payload)
    setShowAddForm(false)
    await load()
  }

  const handleUpdate = async (payload) => {
    await updateCriteria(editTarget.criteriaId, payload)
    setEditTarget(null)
    await load()
  }

  const handleDelete = async (criteriaId) => {
    try {
      await deleteCriteria(criteriaId)
      await load()
    } catch (err) {
      setPageError(err.message)
    }
  }

  const totalWeight = data?.totalWeight ?? 0
  const remaining = Math.max(0, 100 - totalWeight)
  const criteria = data?.criteria ?? []
  const busyForm = showAddForm || !!editTarget

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title='Tiêu chí chấm điểm'
      subtitle={round ? roundLabel(round) : undefined}
      className='modal-wide'
    >
      {pageError ? <FormMessage message={pageError} type='error' /> : null}

      {loading ? (
        <div className='empty-state'>Đang tải tiêu chí…</div>
      ) : (
        <>
          <WeightBar total={totalWeight} />

          <div className='criteria-modal-toolbar'>
            <p className='muted' style={{ margin: 0 }}>
              {criteria.length} tiêu chí · Tổng trọng số mỗi vòng = 100%
            </p>
            <button
              type='button'
              className='btn btn-primary btn-sm'
              onClick={() => {
                setShowAddForm(true)
                setEditTarget(null)
              }}
              disabled={busyForm}
            >
              ＋ Thêm tiêu chí
            </button>
          </div>

          {showAddForm ? (
            <CriteriaForm
              roundId={round.roundId}
              remainingWeight={remaining}
              onSave={handleAdd}
              onCancel={() => setShowAddForm(false)}
            />
          ) : null}

          {criteria.length === 0 && !showAddForm ? (
            <div className='empty-state'>Chưa có tiêu chí cho vòng này.</div>
          ) : (
            <div className='criteria-list'>
              {criteria.map((c) =>
                editTarget?.criteriaId === c.criteriaId ? (
                  <CriteriaForm
                    key={c.criteriaId}
                    roundId={round.roundId}
                    initial={{
                      criterionName: c.criterionName,
                      weight: c.weight,
                      maxScore: c.maxScore,
                      description: c.description ?? ''
                    }}
                    remainingWeight={remaining + c.weight}
                    onSave={handleUpdate}
                    onCancel={() => setEditTarget(null)}
                  />
                ) : (
                  <CriteriaCard
                    key={c.criteriaId}
                    c={c}
                    onEdit={(item) => {
                      setEditTarget(item)
                      setShowAddForm(false)
                    }}
                    onDelete={handleDelete}
                  />
                )
              )}
            </div>
          )}

          <div style={{ marginTop: 16 }}>
            <button type='button' className='btn btn-ghost' onClick={handleClose}>
              Đóng
            </button>
          </div>
        </>
      )}
    </Modal>
  )
}

export default function CriteriaManager({ rounds = [] }) {
  const sortedRounds = useMemo(() => [...rounds].sort((a, b) => Number(a.roundOrder) - Number(b.roundOrder)), [rounds])

  const [activeRound, setActiveRound] = useState(null)

  if (!sortedRounds.length) {
    return (
      <section className='criteria-manager'>
        <h3 className='section-title'>Tiêu chí chấm điểm theo vòng</h3>
        <div className='empty-state'>Chưa có vòng thi nào. Hãy tạo vòng thi trước khi thiết lập tiêu chí.</div>
      </section>
    )
  }

  return (
    <section className='criteria-manager'>
      <div className='criteria-manager-head'>
        <div>
          <h3 className='section-title'>Tiêu chí chấm điểm theo vòng</h3>
          <p className='muted' style={{ margin: '4px 0 0' }}>
            Mỗi vòng có bộ tiêu chí riêng — nhấn vào vòng để quản lý
          </p>
        </div>
      </div>

      <div className='criteria-round-list'>
        {sortedRounds.map((round) => (
          <button
            key={round.roundId}
            type='button'
            className='criteria-round-row'
            onClick={() => setActiveRound(round)}
          >
            <span className='criteria-round-row-main'>
              <span className='criteria-round-row-name'>{roundLabel(round)}</span>
              <span className='criteria-round-row-meta'>Vòng {round.roundOrder ?? '—'}</span>
            </span>
            <span className='criteria-round-row-action'>Tiêu chí ›</span>
          </button>
        ))}
      </div>

      <RoundCriteriaModal round={activeRound} isOpen={!!activeRound} onClose={() => setActiveRound(null)} />
    </section>
  )
}
