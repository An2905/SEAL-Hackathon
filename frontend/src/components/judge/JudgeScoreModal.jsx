import { useEffect, useMemo, useState } from 'react'
import Modal from '../common/Modal'
import FormField from '../common/FormField'
import FormMessage from '../common/FormMessage'
import LoadingButton from '../common/LoadingButton'
import { getCriteriaForJudge, getScoreBySubmission, submitScore, updateScore } from '../../api/judge'
import { localizeError } from '../../utils/errors'
import SubmissionLinks from './SubmissionLinks'

function buildEmptyForm(criteria) {
  const form = {}
  for (const c of criteria) {
    form[c.criteriaId] = { score: '', feedback: '' }
  }
  return form
}

function formFromScore(criteria, score) {
  const form = buildEmptyForm(criteria)
  for (const d of score.details ?? []) {
    if (form[d.criteriaId]) {
      form[d.criteriaId] = {
        score: d.score != null ? String(d.score) : '',
        feedback: d.feedback ?? ''
      }
    }
  }
  return form
}

function calcPreviewTotal(criteria, form) {
  let total = 0
  for (const c of criteria) {
    const raw = form[c.criteriaId]?.score
    const val = parseFloat(raw)
    if (!Number.isFinite(val)) continue
    const maxScore = Number(c.maxScore)
    if (!Number.isFinite(maxScore) || maxScore <= 0) continue
    total += (val / maxScore) * (Number(c.weight) || 0)
  }
  return Math.round(total * 100) / 100
}

export default function JudgeScoreModal({ isOpen, assignment, team, onClose, onSaved }) {
  const [criteriaData, setCriteriaData] = useState(null)
  const [form, setForm] = useState({})
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const isEdit = Boolean(team?.scored && team?.scoreId)
  const criteria = useMemo(() => criteriaData?.criteria ?? [], [criteriaData])

  const previewTotal = useMemo(() => calcPreviewTotal(criteria, form), [criteria, form])

  useEffect(() => {
    if (!isOpen || !assignment?.roundId || !team) return
    let cancelled = false

    ;(async () => {
      setLoading(true)
      setError('')
      try {
        const crit = await getCriteriaForJudge(assignment.roundId)
        if (cancelled) return
        setCriteriaData(crit)

        if (team.scored && team.submissionId) {
          const existing = await getScoreBySubmission(team.submissionId)
          if (cancelled) return
          setForm(formFromScore(crit.criteria, existing))
        } else {
          setForm(buildEmptyForm(crit.criteria))
        }
      } catch (err) {
        if (!cancelled) {
          setError(localizeError(err?.message))
          setCriteriaData(null)
          setForm({})
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()

    return () => {
      cancelled = true
    }
  }, [isOpen, assignment?.roundId, team])

  const setField = (criteriaId, key, value) => {
    setForm((prev) => ({
      ...prev,
      [criteriaId]: { ...prev[criteriaId], [key]: value }
    }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    if (!team?.submissionId) {
      setError('Đội chưa nộp bài — không thể chấm điểm.')
      return
    }

    const details = []
    for (const c of criteria) {
      const entry = form[c.criteriaId] ?? {}
      const score = parseFloat(entry.score)
      if (!Number.isFinite(score) || score < 0 || score > c.maxScore) {
        setError(`Điểm cho "${c.criterionName}" phải từ 0 đến ${c.maxScore}.`)
        return
      }
      details.push({
        criteriaId: c.criteriaId,
        score,
        feedback: entry.feedback?.trim() || null
      })
    }

    const payload = {
      eventId: assignment.eventId,
      roundId: assignment.roundId,
      groupId: assignment.groupId,
      submissionId: team.submissionId,
      details
    }

    setSaving(true)
    try {
      if (isEdit) {
        await updateScore(team.scoreId, payload)
      } else {
        await submitScore(payload)
      }
      onSaved?.()
      onClose()
    } catch (err) {
      setError(localizeError(err?.message))
    } finally {
      setSaving(false)
    }
  }

  if (!team) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={isEdit ? 'Sửa điểm' : 'Chấm điểm'}
      subtitle={team.teamName || '—'}
      className='modal-wide'
    >
      {loading && <div className='empty-state'>Đang tải form chấm điểm…</div>}

      {!loading && (
        <form className='form' onSubmit={handleSubmit}>
          <FormMessage message={error} type='error' />

          {team.submissionId ? <SubmissionLinks team={team} /> : null}

          {!team.submissionId && (
            <div className='empty-state' style={{ marginBottom: 12 }}>
              Đội chưa nộp bài cho vòng này.
            </div>
          )}

          {criteria.length > 0 && (
            <div className='criteria-list'>
              {criteria.map((c) => (
                <div key={c.criteriaId} className='criteria-card'>
                  <div className='criteria-card-head'>
                    <div>
                      <div className='criteria-card-name'>{c.criterionName}</div>
                      <div className='criteria-card-meta'>
                        <span className='criteria-badge'>⚖️ {c.weight}%</span>
                        <span className='criteria-badge'>Tối đa {c.maxScore}</span>
                      </div>
                    </div>
                  </div>
                  {c.description ? <p className='criteria-card-desc'>{c.description}</p> : null}
                  <div className='criteria-form-row' style={{ marginTop: 12 }}>
                    <FormField label={`Điểm (0 – ${c.maxScore}) *`}>
                      <input
                        type='number'
                        min='0'
                        max={c.maxScore}
                        step='0.1'
                        value={form[c.criteriaId]?.score ?? ''}
                        onChange={(e) => setField(c.criteriaId, 'score', e.target.value)}
                        disabled={saving || !team.submissionId}
                        required
                      />
                    </FormField>
                    <FormField label='Nhận xét'>
                      <textarea
                        rows={2}
                        value={form[c.criteriaId]?.feedback ?? ''}
                        onChange={(e) => setField(c.criteriaId, 'feedback', e.target.value)}
                        disabled={saving || !team.submissionId}
                        placeholder='Ghi chú cho tiêu chí này (tuỳ chọn)'
                      />
                    </FormField>
                  </div>
                </div>
              ))}
            </div>
          )}

          {criteria.length > 0 && team.submissionId && (
            <div className='criteria-weight-bar' style={{ marginTop: 16 }}>
              <div className='criteria-weight-bar-head'>
                <span>Điểm tổng dự kiến</span>
                <span style={{ fontWeight: 700 }}>{previewTotal}</span>
              </div>
            </div>
          )}

          <div className='criteria-form-actions' style={{ marginTop: 16 }}>
            <button type='button' className='btn btn-ghost' onClick={onClose} disabled={saving}>
              Huỷ
            </button>
            <LoadingButton
              type='submit'
              loading={saving}
              className='btn btn-primary'
              disabled={!team.submissionId || criteria.length === 0}
            >
              {isEdit ? 'Cập nhật điểm' : 'Nộp điểm'}
            </LoadingButton>
          </div>
        </form>
      )}
    </Modal>
  )
}
