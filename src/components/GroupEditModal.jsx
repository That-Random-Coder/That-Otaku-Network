import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'
import AlertBanner from '../components/AlertBanner.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const GroupEditModal = ({ isOpen, onClose, onUpdated, profile = {}, accent, motionSafe, groupId }) => {
  const [profileFile, setProfileFile] = useState(null)
  const [bgFile, setBgFile] = useState(null)
  const [bio, setBio] = useState(() => profile?.description || '')
  const [loadingImages, setLoadingImages] = useState(false)
  const [loadingBio, setLoadingBio] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')


  const getAccessToken = () => {
    let token = getCookie('AccessToken') || ''
    try { if (!token) token = localStorage.getItem('AccessToken') || '' } catch (e) {}
    token = (token || '').trim()
    const auth = token ? (token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`) : ''
    console.debug('[GroupEditModal] accessToken length:', token ? token.length : 0)
    return { raw: token, auth }
  }

  useEffect(() => {
    if (!isOpen) return
    setProfileFile(null)
    setBgFile(null)
    setBio(profile?.description || '')
    setError('')
  }, [isOpen, profile])

  const uploadImages = async () => {
    if (!profileFile && !bgFile) return setError('Select at least one image to upload')

    try {
      setError('')
      setLoadingImages(true)

      const gid = groupId || profile?.id || ''
      if (!gid) throw new Error('Missing group id.')

      const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/image/update?id=${encodeURIComponent(gid)}`
      const fd = new FormData()
      if (profileFile) fd.append('profileImage', profileFile)
      if (bgFile) fd.append('bgImage', bgFile)

      const token = getAccessToken()
      const headers = { Authorization: token.auth, AccessToken: token.raw }
      const res = await fetch(url, { method: 'PUT', body: fd, headers })
      const bodyRes = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(bodyRes)
        throw new Error(parsed.message || 'Failed to upload images')
      }

      const successMsg = bodyRes?.message || 'Images updated successfully'
      setSuccessMessage(successMsg)

      setTimeout(() => setSuccessMessage(''), 3000)

      try { if (onUpdated) onUpdated(bodyRes?.data || bodyRes || {}) } catch (e) {}
      setProfileFile(null)
      setBgFile(null)

      setTimeout(() => { try { window.location.reload() } catch (e) {} }, 900)
    } catch (err) {
      console.error('uploadImages error', err)
      setError(err?.message || 'Failed to upload images')
    } finally {
      setLoadingImages(false)
    }
  }

  const updateBio = async () => {
    try {
      setError('')
      setLoadingBio(true)
      const gid = groupId || profile?.id || ''
      if (!gid) throw new Error('Missing group id.')

      const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/bio/update`
      const token = getAccessToken()
      const headers = { 'Content-Type': 'application/json', Authorization: token.auth, AccessToken: token.raw }
      const res = await fetch(url, { method: 'PUT', headers, body: JSON.stringify({ id: gid, bio: bio?.trim() || '' }) })
      const bodyRes = await res.json().catch(() => ({}))
      if (!res.ok) {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(bodyRes)
        throw new Error(parsed.message || 'Failed to update bio')
      }

      const successMsg = bodyRes?.message || 'Bio updated successfully'
      setSuccessMessage(successMsg)
      setTimeout(() => setSuccessMessage(''), 3000)
      try { if (onUpdated) onUpdated(bodyRes?.data || { description: bio }) } catch (e) {}
    } catch (err) {
      console.error('updateBio error', err)
      setError(err?.message || 'Failed to update bio')
    } finally {
      setLoadingBio(false)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 backdrop-blur-md p-4" onClick={onClose}>
        <motion.div initial={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : false} animate={motionSafe ? { opacity: 1, scale: 1, y: 0 } : { opacity: 1 }} exit={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : { opacity: 0 }} transition={{ type: 'spring', stiffness: 280, damping: 24 }} className="relative w-full max-w-3xl h-auto overflow-hidden rounded-3xl border backdrop-blur-3xl flex flex-col" style={{ borderColor: 'rgba(255,255,255,0.08)', backgroundColor: accent.surface, boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent.glow}` }} onClick={(e) => e.stopPropagation()}>
          <div className="relative p-6 border-b border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">Update Group Information</h2>
                <p className="text-sm text-slate-300/80 mt-1">Upload images or update the group bio</p>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition"><X className="h-5 w-5" /></button>
            </div>
          </div>

          <div className="p-6 space-y-6">
            {error && <AlertBanner status="error" message={error} />}
            {successMessage && <AlertBanner status="success" message={successMessage} />}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium text-slate-200">Group Image</label>
                <input className="block w-full text-sm text-slate-300 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-white/10 file:text-white hover:file:bg-white/20" type="file" accept="image/*" onChange={(e) => setProfileFile(e.target.files?.[0] || null)} />
                <label className="text-sm font-medium text-slate-200 mt-2">Group Background</label>
                <input className="block w-full text-sm text-slate-300 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-white/10 file:text-white hover:file:bg-white/20" type="file" accept="image/*" onChange={(e) => setBgFile(e.target.files?.[0] || null)} />
                <div className="flex gap-3 mt-3">
                  <button onClick={uploadImages} disabled={loadingImages || (!profileFile && !bgFile)} className="rounded-xl px-4 py-2 text-sm font-semibold text-white" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>{loadingImages ? 'Uploading...' : 'Update Images'}</button>
                  <button onClick={() => { setProfileFile(null); setBgFile(null); }} type="button" className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-white">Clear</button>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-200">Group Bio</label>
              <textarea value={bio} onChange={(e) => setBio(e.target.value)} rows={4} className="w-full rounded-xl border border-white/15 bg-black/30 px-4 py-3 text-base text-slate-200 placeholder:text-slate-400 focus:border-white/30" />
              <div className="flex justify-end">
                <button onClick={updateBio} disabled={loadingBio} className="rounded-xl px-4 py-2 text-sm font-semibold text-white" style={{ backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 8px 24px ${accent.glow}` }}>{loadingBio ? 'Updating...' : 'Update Bio'}</button>
              </div>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

export default GroupEditModal
