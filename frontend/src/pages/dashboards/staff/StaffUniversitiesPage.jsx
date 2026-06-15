import { useCallback, useEffect, useMemo, useState } from 'react'
import FormField from '../../../components/common/FormField'
import FormMessage from '../../../components/common/FormMessage'
import LoadingButton from '../../../components/common/LoadingButton'
import Modal from '../../../components/common/Modal'
import Pagination from '../../../components/common/Pagination'
import LoadingState from '../../../components/common/LoadingState'
import {
  createUniversity,
  deleteUniversity,
  getDeleteUniversityPreview,
  getStaffUniversities,
  updateUniversity
} from '../../../api/staffUniversity'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

function CreateUniversityForm({ onCreated }) {
  const { showToast } = useToast()
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)
  const [name, setName] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage(null)
    setLoading(true)
    try {
      await createUniversity({ universityName: name })
      showToast('Đã thêm trường đại học', 'success')
      setName('')
      onCreated?.()
    } catch (err) {
      setMessage({ text: localizeError(err.message), type: 'error' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className='card'>
      <div className='card-head'>
        <div className='card-title'>Thêm trường đại học</div>
      </div>
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tên trường'>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            maxLength={255}
            placeholder='VD: FPT University'
          />
        </FormField>
        <LoadingButton loading={loading} type='submit'>
          Thêm trường
        </LoadingButton>
        <FormMessage message={message?.text} type={message?.type} />
      </form>
    </div>
  )
}

function EditUniversityModal({ university, onClose, onUpdated }) {
  const { showToast } = useToast()
  const [name, setName] = useState(university?.universityName ?? '')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setName(university?.universityName ?? '')
    setError('')
  }, [university])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await updateUniversity({
        universityId: university.universityId,
        universityName: name
      })
      showToast('Đã cập nhật tên trường', 'success')
      onUpdated?.()
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      isOpen={!!university}
      onClose={onClose}
      title='Sửa trường đại học'
      subtitle='Sinh viên đang dùng tên cũ sẽ được cập nhật theo tên mới.'
    >
      <form className='form' onSubmit={handleSubmit}>
        <FormField label='Tên trường'>
          <input value={name} onChange={(e) => setName(e.target.value)} required maxLength={255} />
        </FormField>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
          <button type='button' className='btn btn-outline' onClick={onClose} disabled={loading}>
            Hủy
          </button>
          <LoadingButton loading={loading} type='submit'>
            Lưu
          </LoadingButton>
        </div>
        <FormMessage message={error} type='error' />
      </form>
    </Modal>
  )
}

function DeleteUniversityModal({ university, allUniversities, onClose, onDeleted }) {
  const { showToast } = useToast()
  const [preview, setPreview] = useState(null)
  const [loadingPreview, setLoadingPreview] = useState(false)
  const [loadingDelete, setLoadingDelete] = useState(false)
  const [error, setError] = useState('')
  const [replacement, setReplacement] = useState('')

  useEffect(() => {
    if (!university) return
    setPreview(null)
    setError('')
    setReplacement('')
    setLoadingPreview(true)
    getDeleteUniversityPreview(university.universityId)
      .then(setPreview)
      .catch((err) => setError(localizeError(err.message)))
      .finally(() => setLoadingPreview(false))
  }, [university])

  const linkedCount = Number(preview?.linkedUserCount ?? 0)
  const needsHandling = linkedCount > 0
  const replacementOptions = allUniversities.filter((u) => u.universityId !== university?.universityId)

  const canSubmit = !loadingPreview && preview && (!needsHandling || replacement)

  const handleDelete = async () => {
    if (!canSubmit) return
    setError('')
    setLoadingDelete(true)
    try {
      await deleteUniversity({
        universityId: university.universityId,
        replacementUniversityName: replacement || undefined
      })
      showToast('Đã xóa trường đại học', 'success')
      onDeleted?.()
      onClose()
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoadingDelete(false)
    }
  }

  return (
    <Modal
      isOpen={!!university}
      onClose={onClose}
      title='Xóa trường đại học'
      subtitle={university?.universityName}
      className='modal-wide'
    >
      {loadingPreview && <p className='hint'>Đang kiểm tra sinh viên liên kết...</p>}
      {preview && (
        <div className='form'>
          <p style={{ margin: '0 0 12px', fontSize: 14 }}>{preview.message}</p>
          {needsHandling && (
            <>
              <div
                style={{
                  padding: '10px 12px',
                  borderRadius: 'var(--radius)',
                  background: 'rgba(234, 179, 8, 0.12)',
                  border: '1px solid rgba(234, 179, 8, 0.35)',
                  fontSize: 13,
                  marginBottom: 12
                }}
              >
                Còn <strong>{linkedCount}</strong> sinh viên đang gắn trường này.
              </div>
              <FormField label='Chuyển sinh viên sang trường'>
                <select value={replacement} onChange={(e) => setReplacement(e.target.value)}>
                  <option value=''>— Chọn trường thay thế —</option>
                  {replacementOptions.map((u) => (
                    <option key={u.universityId} value={u.universityName}>
                      {u.universityName}
                    </option>
                  ))}
                </select>
              </FormField>
            </>
          )}
        </div>
      )}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <button type='button' className='btn btn-outline' onClick={onClose} disabled={loadingDelete}>
          Hủy
        </button>
        <button
          type='button'
          className='btn'
          style={{ background: 'var(--danger, #dc2626)', borderColor: 'var(--danger, #dc2626)', color: '#fff' }}
          onClick={handleDelete}
          disabled={!canSubmit || loadingDelete}
        >
          {loadingDelete ? 'Đang xóa...' : 'Xóa'}
        </button>
      </div>
      <FormMessage message={error} type='error' />
    </Modal>
  )
}

const UNIVERSITIES_PAGE_SIZE = 5

export default function StaffUniversitiesPage() {
  const [universities, setUniversities] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [page, setPage] = useState(1)

  const filteredUniversities = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return universities
    return universities.filter((u) => (u.universityName ?? '').toLowerCase().includes(q))
  }, [universities, search])

  const loadUniversities = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setUniversities(await getStaffUniversities())
    } catch (err) {
      setError(localizeError(err.message))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadUniversities()
  }, [loadUniversities])

  useEffect(() => {
    setPage(1)
  }, [search])

  return (
    <>
      <div className='section-title'>
        <h2>Trường đại học</h2>
        <span className='hint'>Quản lý danh sách trường cho đăng ký sinh viên</span>
      </div>

      <div className='cards'>
        <CreateUniversityForm onCreated={loadUniversities} />
      </div>

      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Danh sách trường</h2>
        <span className='hint'>
          {search.trim()
            ? `${filteredUniversities.length} / ${universities.length} trường`
            : `${universities.length} trường`}
        </span>
      </div>

      <div className='card'>
        <FormField label='Tìm theo tên trường'>
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder='VD: FPT, HCM, RMIT...'
            disabled={loading}
          />
        </FormField>

        {loading && <LoadingState text='Đang tải...' className='hint' />}
        <FormMessage message={error} type='error' />
        {!loading && !error && universities.length === 0 && <p className='hint'>Chưa có trường nào.</p>}
        {!loading && !error && universities.length > 0 && filteredUniversities.length === 0 && (
          <p className='hint'>Không tìm thấy trường khớp với &quot;{search.trim()}&quot;.</p>
        )}
        {!loading && filteredUniversities.length > 0 && (
          <>
            <div className='kv-list'>
              {filteredUniversities
                .slice((page - 1) * UNIVERSITIES_PAGE_SIZE, page * UNIVERSITIES_PAGE_SIZE)
                .map((u) => {
                  const linked = Number(u.linkedUserCount) || 0
                  return (
                    <div className='kv' key={u.universityId}>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: 600 }}>{u.universityName}</div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <span
                          style={{
                            fontSize: 12,
                            padding: '3px 8px',
                            borderRadius: 'var(--radius)',
                            background: linked > 0 ? 'rgba(234, 179, 8, 0.15)' : 'var(--bg)',
                            border: '1px solid var(--border)',
                            color: linked > 0 ? '#b45309' : 'var(--text-mute)'
                          }}
                        >
                          {linked} SV
                        </span>
                        <button
                          type='button'
                          className='btn btn-outline'
                          style={{ fontSize: 12, padding: '4px 10px' }}
                          onClick={() => setEditing(u)}
                        >
                          Sửa
                        </button>
                        <button
                          type='button'
                          className='btn btn-outline'
                          style={{
                            fontSize: 12,
                            padding: '4px 10px',
                            color: 'var(--danger, #dc2626)',
                            borderColor: 'var(--danger, #dc2626)'
                          }}
                          onClick={() => setDeleting(u)}
                        >
                          Xóa
                        </button>
                      </div>
                    </div>
                  )
                })}
            </div>
            <Pagination
              total={filteredUniversities.length}
              pageSize={UNIVERSITIES_PAGE_SIZE}
              currentPage={page}
              onChange={setPage}
            />
          </>
        )}
      </div>

      <EditUniversityModal university={editing} onClose={() => setEditing(null)} onUpdated={loadUniversities} />
      <DeleteUniversityModal
        university={deleting}
        allUniversities={universities}
        onClose={() => setDeleting(null)}
        onDeleted={loadUniversities}
      />
    </>
  )
}
