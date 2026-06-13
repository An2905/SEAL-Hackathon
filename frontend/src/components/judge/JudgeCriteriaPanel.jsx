import { useEffect, useState } from 'react'
import { getCriteriaForJudge } from '../../api/judge'

function WeightBar({ total }) {
  const pct = Math.min(total, 100)
  const color = pct >= 100 ? '#ef4444' : pct >= 80 ? '#f59e0b' : '#10b981'
  return (
    <div className='criteria-weight-bar'>
      <div className='criteria-weight-bar-head'>
        <span>Tổng trọng số</span>
        <span style={{ color, fontWeight: 700 }}>{total.toFixed(2)}% / 100%</span>
      </div>
      <div className='criteria-weight-track'>
        <div className='criteria-weight-fill' style={{ width: `${pct}%`, background: color }} />
      </div>
    </div>
  )
}

function CriteriaReadCard({ c }) {
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
      </div>
      {c.description ? <p className='criteria-card-desc'>{c.description}</p> : null}
    </div>
  )
}

export default function JudgeCriteriaPanel({ roundId, roundName }) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!roundId) {
      setData(null)
      return
    }
    let cancelled = false
    setLoading(true)
    setError('')
    getCriteriaForJudge(roundId)
      .then((res) => {
        if (!cancelled) setData(res)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err?.message || 'Không tải được tiêu chí')
          setData(null)
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [roundId])

  if (!roundId) {
    return <div className='empty-state'>Chọn phân công để xem tiêu chí chấm điểm.</div>
  }

  return (
    <div className='criteria-manager'>
      <div className='criteria-manager-head'>
        <div>
          <h3 className='section-subtitle' style={{ margin: 0 }}>
            Tiêu chí chấm điểm
          </h3>
          {roundName ? (
            <p className='muted' style={{ margin: '4px 0 0', fontSize: 13 }}>
              Vòng: {roundName}
            </p>
          ) : null}
        </div>
        {data ? (
          <div style={{ fontSize: 13, color: 'var(--text-dim)' }}>
            {data.count} tiêu chí · Tối đa {data.totalMaxScore} điểm
          </div>
        ) : null}
      </div>

      {loading && <div className='empty-state'>Đang tải tiêu chí…</div>}
      {!loading && error && <div className='empty-state'>{error}</div>}
      {!loading && !error && data?.criteria?.length === 0 && (
        <div className='empty-state'>Vòng này chưa có tiêu chí. Liên hệ ban tổ chức.</div>
      )}
      {!loading && !error && data?.criteria?.length > 0 && (
        <>
          <WeightBar total={data.totalWeight} />
          <div className='criteria-list' style={{ marginTop: 16 }}>
            {data.criteria.map((c) => (
              <CriteriaReadCard key={c.criteriaId} c={c} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}
