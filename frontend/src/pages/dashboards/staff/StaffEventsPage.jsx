import { useState } from 'react'
import { EventsListSection } from '../StaffDashboard'
import LoadingButton from '../../../components/common/LoadingButton'
import { exportEventsExcel } from '../../../api/staff'
import { useToast } from '../../../context/ToastContext'
import { localizeError } from '../../../utils/errors'

export default function StaffEventsPage() {
  const { showToast } = useToast()
  const [exporting, setExporting] = useState(false)

  const handleExport = async () => {
    setExporting(true)
    try {
      const blob = await exportEventsExcel()

      // Tạo link download tạm, click, rồi xóa
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
        <h2>Sự kiện trong hệ thống</h2>
        <span className='hint'>Xem và đổi trạng thái sự kiện</span>
      </div>

      {/* Nút Xuất Excel */}
      <div style={{ marginBottom: 16 }}>
        <LoadingButton loading={exporting} className='btn btn-outline' onClick={handleExport} type='button'>
          Xuất Excel
        </LoadingButton>
      </div>

      <EventsListSection />
    </>
  )
}
