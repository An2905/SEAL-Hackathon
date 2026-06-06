import { useState } from 'react'
import { CreateEventForm, EventsListSection } from '../StaffDashboard'

export default function StaffEventsPage() {
  const [refreshKey, setRefreshKey] = useState(0)
  const [showCreateModal, setShowCreateModal] = useState(false)

  return (
    <>
      <div className='section-title'>
        <h2>Sự kiện trong hệ thống</h2>
        <span className='hint'>Xem và đổi trạng thái sự kiện</span>
      </div>

      <div style={{ marginBottom: 16 }}>
        <button type='button' className='btn btn-primary' onClick={() => setShowCreateModal(true)}>
          Tạo sự kiện mới
        </button>
      </div>

      <CreateEventForm
        open={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSuccess={() => setRefreshKey((k) => k + 1)}
      />

      <EventsListSection
        refreshKey={refreshKey}
        onStatusChanged={() => setRefreshKey((k) => k + 1)}
      />
    </>
  )
}
