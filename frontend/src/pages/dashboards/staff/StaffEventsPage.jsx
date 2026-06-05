import { useState } from 'react'
import { CreateEventForm, EventsListSection } from '../StaffDashboard'
import LoadingButton from '../../../components/common/LoadingButton'
import { exportEventsExcel } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

export default function StaffEventsPage() {
  const { showToast } = useToast()
  const [exporting, setExporting] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  const handleExport = async () => {
    setExporting(true)
    try {
      const blob = await exportEventsExcel()

      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'events.xlsx'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(url)

      showToast('Xuất Excel thành công', 'success')
    } catch (err) {
      showToast(localizeError(err.message), 'error')
    } finally {
      setExporting(false)
    }
  }

  return (
    <>
      <div className='section-title'>
        <h2>Tạo sự kiện</h2>
        <span className='hint'>Tạo hackathon mới — mặc định trạng thái BUILDING</span>
      </div>

      <CreateEventForm onSuccess={() => setRefreshKey((k) => k + 1)} />

      <div className='section-title' style={{ marginTop: 24 }}>
        <h2>Sự kiện trong hệ thống</h2>
        <span className='hint'>Xem và đổi trạng thái sự kiện</span>
      </div>

      <div style={{ marginBottom: 16 }}>
        <LoadingButton loading={exporting} className='btn btn-success' onClick={handleExport} type='button'>
          Xuất Excel
        </LoadingButton>
      </div>

      <EventsListSection
        refreshKey={refreshKey}
        onStatusChanged={() => setRefreshKey((k) => k + 1)}
      />
    </>
  )
}
