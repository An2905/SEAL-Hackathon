import { useState } from 'react'
import FormField from '../../../components/common/FormField'
import LoadingButton from '../../../components/common/LoadingButton'
import Pagination from '../../../components/common/Pagination'
import { createEventAward, deleteEventAward, updateEventAward } from '../../../api/eventService'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

const PAGE_SIZE = 5

const greenOutlineBtnStyle = {
  borderColor: '#22c55e',
  color: '#16a34a',
  padding: '6px 12px',
  height: 'auto',
  minHeight: 0
}

function AwardForm({ initial, saving, onSubmit, onCancel, submitLabel }) {
  const [form, setForm] = useState(
    initial ?? {
      title: '',
      rank: ''
    }
  )

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = (e) => {
    e.preventDefault()
    onSubmit(form)
  }

  return (
    <form className='form criteria-form-panel awards-form-panel' onSubmit={handleSubmit}>
      <h4 className='section-subtitle'>{initial ? 'Sửa giải thưởng' : 'Thêm giải thưởng mới'}</h4>
      <FormField label='Tên giải thưởng *'>
        <input name='title' value={form.title} onChange={handleChange} maxLength={100} disabled={saving} required />
      </FormField>
      <FormField label='Hạng'>
        <input
          type='number'
          name='rank'
          value={form.rank}
          onChange={handleChange}
          min={1}
          disabled={saving}
          placeholder='Để trống nếu không có hạng'
        />
      </FormField>
      <div className='criteria-form-actions'>
        <button type='button' className='btn btn-ghost' onClick={onCancel} disabled={saving}>
          Huỷ
        </button>
        <LoadingButton loading={saving} type='submit' className='btn btn-primary'>
          {submitLabel}
        </LoadingButton>
      </div>
    </form>
  )
}

function AwardCard({ award, onEdit, onDelete }) {
  const [confirmDel, setConfirmDel] = useState(false)

  return (
    <div className='criteria-card'>
      <div className='criteria-card-head'>
        <div>
          <div className='criteria-card-name'>{award.title || '—'}</div>
          <div className='criteria-card-meta'>
            {award.rank != null ? <span className='criteria-badge'>🏆 Hạng #{award.rank}</span> : null}
          </div>
        </div>
        <div className='criteria-card-actions'>
          <button type='button' className='btn btn-ghost btn-sm' onClick={() => onEdit(award)}>
            Sửa
          </button>
          {confirmDel ? (
            <>
              <button type='button' className='btn btn-danger btn-sm' onClick={() => onDelete(award)}>
                Xác nhận
              </button>
              <button type='button' className='btn btn-ghost btn-sm' onClick={() => setConfirmDel(false)}>
                Huỷ
              </button>
            </>
          ) : (
            <button type='button' className='btn btn-danger btn-sm' onClick={() => setConfirmDel(true)}>
              Xóa
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

export default function AwardsManager({
  eventId,
  awards = [],
  onAwardCreated,
  onAwardUpdated,
  onAwardDeleted
}) {
  const { showToast } = useToast()
  const [page, setPage] = useState(1)
  const [showAddForm, setShowAddForm] = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [saving, setSaving] = useState(false)

  const sortedAwards = [...awards].sort((a, b) => {
    const rankA = a.rank == null ? Number.MAX_SAFE_INTEGER : Number(a.rank)
    const rankB = b.rank == null ? Number.MAX_SAFE_INTEGER : Number(b.rank)
    if (rankA !== rankB) return rankA - rankB
    return String(a.title ?? '').localeCompare(String(b.title ?? ''), 'vi')
  })

  const paginatedAwards = sortedAwards.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  const handleCreate = async (form) => {
    setSaving(true)
    try {
      const created = await createEventAward({
        eventId,
        title: form.title,
        rank: form.rank
      })
      onAwardCreated?.(created)
      showToast('Đã thêm giải thưởng', 'success')
      setShowAddForm(false)
      setPage(1)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleUpdate = async (form) => {
    if (!editTarget) return
    setSaving(true)
    try {
      const updated = await updateEventAward({
        eventId,
        awardId: editTarget.awardId,
        title: form.title,
        rank: form.rank
      })
      onAwardUpdated?.(updated)
      showToast('Đã cập nhật giải thưởng', 'success')
      setEditTarget(null)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (award) => {
    try {
      await deleteEventAward({
        eventId,
        awardId: award.awardId
      })
      onAwardDeleted?.(award.awardId)
      showToast('Đã xóa giải thưởng', 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    }
  }

  return (
    <section className='awards-manager'>
      <div className='awards-manager-head'>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className='awards-manager-title-row'>
            <h3 className='section-title' style={{ margin: 0 }}>
              Giải thưởng
            </h3>
            {!showAddForm && !editTarget ? (
              <button
                type='button'
                className='btn btn-outline btn-sm'
                style={greenOutlineBtnStyle}
                onClick={() => {
                  setShowAddForm(true)
                  setEditTarget(null)
                }}
              >
                ＋ Thêm giải thưởng
              </button>
            ) : null}
          </div>
          <p className='muted' style={{ margin: '4px 0 0' }}>
            Thiết lập các giải thưởng của sự kiện — bắt buộc trước khi chuyển sang sắp diễn ra
          </p>
        </div>
      </div>

      {showAddForm ? (
        <AwardForm saving={saving} onSubmit={handleCreate} onCancel={() => setShowAddForm(false)} submitLabel='Thêm giải thưởng' />
      ) : null}

      {sortedAwards.length === 0 && !showAddForm ? (
        <div className='empty-state'>Chưa có giải thưởng nào.</div>
      ) : (
        <div className='awards-list'>
          {paginatedAwards.map((award) =>
            editTarget?.awardId === award.awardId ? (
              <AwardForm
                key={award.awardId}
                initial={{
                  title: award.title || '',
                  rank: award.rank == null ? '' : String(award.rank)
                }}
                saving={saving}
                onSubmit={handleUpdate}
                onCancel={() => setEditTarget(null)}
                submitLabel='Cập nhật'
              />
            ) : (
              <AwardCard
                key={award.awardId}
                award={award}
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

      {sortedAwards.length > PAGE_SIZE ? (
        <Pagination total={sortedAwards.length} pageSize={PAGE_SIZE} currentPage={page} onChange={setPage} />
      ) : null}
    </section>
  )
}
