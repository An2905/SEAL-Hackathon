import { useState } from 'react'
import FormField from '../../../components/common/FormField'
import LoadingButton from '../../../components/common/LoadingButton'
import FormMessage from '../../../components/common/FormMessage'
import { filterEmails } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

export default function StaffFilterEmailPage() {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const [copied, setCopied] = useState(false)
  const [validationError, setValidationError] = useState(null)
  const [form, setForm] = useState({ emailContains: '', nameContains: '' })

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setValidationError(null)
    setResult(null)
    setCopied(false)

    const keyword = form.emailContains.trim() + form.nameContains.trim()
    if (keyword.length < 2) {
      setValidationError('Nhập từ khóa ít nhất 2 ký tự (email hoặc tên).')
      return
    }

    setLoading(true)
    try {
      const data = await filterEmails({
        audiences: 'ALL_IN_EVENT,EXPERT',
        emailContains: form.emailContains.trim() || undefined,
        nameContains: form.nameContains.trim() || undefined,
        includeCopyText: true
      })
      setResult(data)
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setLoading(false)
    }
  }

  const handleCopy = async () => {
    if (!result?.copyText) return
    try {
      await navigator.clipboard.writeText(result.copyText)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      showToast('Không copy được — hãy chọn thủ công', 'error')
    }
  }

  return (
    <>
      <div className='section-title'>
        <h2>Lọc danh sách email</h2>
        <span className='hint'>Tìm kiếm và xuất danh sách email theo bộ lọc</span>
      </div>

      <div className='card'>
        <form className='form' onSubmit={handleSubmit}>
          <FormField label='Lọc email'>
            <input name='emailContains' value={form.emailContains} onChange={handle} placeholder='VD: @fpt.edu.vn' />
          </FormField>
          <FormField label='Lọc tên'>
            <input name='nameContains' value={form.nameContains} onChange={handle} placeholder='VD: Nguyễn' />
          </FormField>

          {validationError && <FormMessage message={validationError} type='error' />}

          <LoadingButton loading={loading} type='submit'>
            Lọc email
          </LoadingButton>
        </form>

        {result && (
          <div style={{ marginTop: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
              <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
                <span className='status-pill status-active'>{result.totalUniqueEmails} email duy nhất</span>
                {result.totalRawMatches !== result.totalUniqueEmails && (
                  <span className='status-pill status-default'>
                    {result.totalRawMatches} kết quả · {result.duplicatesRemoved} trùng đã bỏ
                  </span>
                )}
              </div>
              {result.copyText && (
                <button type='button' className='btn btn-sm btn-outline' onClick={handleCopy}>
                  {copied ? '✓ Đã copy' : 'Copy danh sách'}
                </button>
              )}
            </div>

            {Array.isArray(result.recipients) && result.recipients.length > 0 && (
              <div className='kv-list' style={{ marginTop: 10, maxHeight: 240, overflowY: 'auto' }}>
                {result.recipients.map((r) => (
                  <div className='kv' key={r.userId || r.email} style={{ fontSize: 12 }}>
                    <span style={{ fontWeight: 600 }}>{r.fullName || '—'}</span>
                    <span style={{ color: 'var(--text-dim)' }}>
                      {r.email} · {r.userRole}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </>
  )
}
