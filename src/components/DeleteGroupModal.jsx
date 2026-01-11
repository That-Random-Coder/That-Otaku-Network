import React, { useState } from 'react'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const DeleteGroupModal = ({ isOpen, onClose, onSuccess, groupId, leaderIdProp, accent }) => {
  const [isDeleting, setIsDeleting] = useState(false)
  const [error, setError] = useState('')

  if (!isOpen) return null

  const handleDelete = async () => {
    if (isDeleting) return
    setIsDeleting(true)
    setError('')
    try {
      const leaderId = leaderIdProp || String(getCookie('id') || getCookie('userId') || '')
      if (!leaderId) throw new Error('Missing leader id. Please log in again.')
      if (!groupId) throw new Error('Missing group id.')

      const accessToken = getCookie('AccessToken') || ''
      const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/delete?groupId=${encodeURIComponent(groupId)}&leaderId=${encodeURIComponent(leaderId)}`
      const res = await fetch(url, { method: 'DELETE', headers: { Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}` } })
      const body = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        throw new Error(parsed.message || 'Failed to delete group')
      }

      // success: call onSuccess if present, else redirect
      try { if (onSuccess) onSuccess({ message: 'The Group has been deleted' }) } catch (e) {}
      // small delay for UX
      setTimeout(() => { window.location.href = '/friends' }, 750)
    } catch (err) {
      console.error('DeleteGroupModal error:', err)
      setError(err?.message || 'Failed to delete group')
      setIsDeleting(false)
    }
  }

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 backdrop-blur-md p-4" onClick={onClose}>
      <div className="relative w-full max-w-lg h-auto overflow-hidden rounded-3xl border backdrop-blur-3xl flex flex-col" style={{ borderColor: 'rgba(255,255,255,0.08)', backgroundColor: accent?.surface || 'rgba(0,0,0,0.6)', boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent?.glow || 'rgba(124,58,237,0.5)'}` }} onClick={(e) => e.stopPropagation()}>
        <div className="relative p-6 border-b border-white/10">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-2xl font-semibold text-white">Delete Group</h2>
              <p className="text-sm text-slate-300/80 mt-1">Are you sure you want to delete this group? This action cannot be undone.</p>
            </div>
            <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition">✕</button>
          </div>
        </div>

        <div className="p-6">
          {error && <div className="mb-3 text-sm text-rose-300">{error}</div>}
          <div className="text-sm text-slate-300/80 mb-4">Confirm deletion to permanently remove this group and all related data.</div>
          <div className="flex flex-col sm:flex-row items-center justify-end gap-3">
            <button onClick={onClose} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10 w-full sm:w-auto">Cancel</button>
            <button
              onClick={handleDelete}
              disabled={isDeleting}
              className="rounded-xl px-5 py-2.5 text-sm font-semibold text-white transition w-full sm:w-auto disabled:opacity-50"
              style={{
                backgroundImage: `linear-gradient(90deg, ${accent?.mid || '#f97316'}, ${accent?.strong || '#ef4444'})`,
                boxShadow: `0 8px 24px ${accent?.glow || 'rgba(0,0,0,0.25)'}`,
              }}
            >{isDeleting ? 'Deleting...' : 'Delete'}</button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default DeleteGroupModal
