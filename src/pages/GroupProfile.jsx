import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import AlertBanner from '../components/AlertBanner.jsx'
import accentOptions from '../theme/accentOptions.js'
import NavigationBar from '../components/NavigationBar.jsx'
import { Users, Share2 } from 'lucide-react'
import QRCodeShareModal from '../components/QRCodeShareModal.jsx'
import DeleteGroupModal from '../components/DeleteGroupModal.jsx'
import GroupEditModal from '../components/GroupEditModal.jsx'
import GroupPostsGrid from '../components/GroupPostsGrid.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

function GroupProfile() {
  const { groupId: groupIdParam } = useParams()
  const [accentKey] = useState(() => {
    try { return localStorage.getItem('accentKey') || 'crimson-night' } catch { return 'crimson-night' }
  })
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])

  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [isMember, setIsMember] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [alertBanner, setAlertBanner] = useState({ status: '', message: '' })
  const [showQRModal, setShowQRModal] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [showEditGroupModal, setShowEditGroupModal] = useState(false)

  useEffect(() => {
    const groupId = groupIdParam
    if (!groupId) {
      setError('No group selected.')
      setLoading(false)
      return
    }
    const accessToken = getCookie('AccessToken') || ''
    setLoading(true)
    setError('')

    fetch(`${import.meta.env.VITE_API_BASE_URL}profile/group/get?id=${encodeURIComponent(groupId)}&image=true`, {
      headers: {
        Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}`,
      },
    })
      .then(async (res) => {

        try {
          console.log('Group profile API response object:', res)
          console.log('Group profile response headers:', Array.from(res.headers.entries()))
          const rawBody = await res.clone().text()
          console.log('Group profile response body (raw):', rawBody)
        } catch (logErr) {
          console.log('Group profile response logging error:', logErr)
        }

        if (!res.ok) {
          const maybe = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
          throw new Error(parsed.message || 'Failed to fetch group.')
        }
        return res.json()
      })
      .then((data) => {
        console.log('Group profile API response:', data)
        const p = data?.data || data || {}

        const mappedProfile = {
          id: p.id || p.groupId || groupId,
          name: p.name || p.groupName || '',
          description: p.description || p.groupBio || p.bio || '',
          members: p.members || p.memberCount || p.totalMembers || 0,
          activity: p.activity || p.activityFrequency || '',
          leaderId: p.leaderId || p.leader?.id || p.ownerId || '',
          leaderName: p.leaderUsername || 'The leader was just your genjutsu',
          image: p.profileImage ? `data:image/jpeg;base64,${p.profileImage}` : (p.icon || p.image || p.imageUrl || ''),
          backgroundImage: p.bgImage ? `data:image/jpeg;base64,${p.bgImage}` : (p.backgroundImage || p.bg || ''),
          createdAt: p.createdAt || p.created || null,
          raw: p,
        }
        setProfile(mappedProfile)

        setIsMember(Boolean(p.isMember ?? p.isJoined ?? p.joined ?? false))


        ;(async () => {
          try {
            const userId = getCookie('id') || getCookie('userId') || ''
            if (!userId) return
            const gid = mappedProfile.id || ''
            if (!gid) return

            const accessToken = getCookie('AccessToken') || ''
            const headers = accessToken ? { Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}` } : {}
            const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/user/${encodeURIComponent(userId)}`
            const res = await fetch(url, { headers })
            const body = await res.json().catch(() => ({}))
            if (!res.ok) {

              console.warn('Background fetch groups failed:', body)
              return
            }

            const arr = Array.isArray(body?.data) ? body.data : (Array.isArray(body) ? body : (body.groups || []))
            if (!Array.isArray(arr)) return
            const found = arr.some((g) => (g.id || g.groupId || g._id || '') === String(gid))
            if (found) setIsMember(true)
          } catch (e) {
            console.log('Background user groups check failed', e)
          }
        })()
      })
      .catch((err) => setError(err.message || 'Failed to fetch group.'))
      .finally(() => setLoading(false))
  }, [groupIdParam])

  const handleToggleMember = async () => {
    if (actionLoading) return
    setActionLoading(true)
    const userId = getCookie('id') || getCookie('userId') || ''
    const groupId = groupIdParam
    const accessToken = getCookie('AccessToken') || ''

    try {
      if (!userId) throw new Error('Missing user id. Please log in again.')

      if (!isMember) {

        console.log('Joining group', groupId, 'as', userId)
        const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/join?groupId=${encodeURIComponent(groupId)}&userId=${encodeURIComponent(userId)}`
        const res = await fetch(url, {
          method: 'POST',
          headers: {
            Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}`,
          },
        })
        console.log('Join response status:', res.status)
        if (!res.ok) {
          const maybe = await res.json().catch(() => ({}))
          console.error('Join response body:', maybe)
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
          throw new Error(parsed.message || 'Failed to join group')
        }

        setIsMember(true)
        setProfile((prev) => prev ? ({ ...prev, members: (prev.members || 0) + 1 }) : prev)
        setAlertBanner({ status: 'success', message: "You're a part of the troop!" })
        setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500)
      } else {


        const currentUserId = String(getCookie('id') || getCookie('userId') || '')
        if (String(profile?.leaderId || '') === currentUserId) {
          throw new Error('Group leader cannot leave; delete the group instead.')
        }

        console.log('Leaving group', groupId, 'as', userId)

        const leaveUrls = [
          `${import.meta.env.VITE_API_BASE_URL}search/group/leave?groupId=${encodeURIComponent(groupId)}&userId=${encodeURIComponent(userId)}`,
          `${import.meta.env.VITE_API_BASE_URL}profile/group/leave?groupId=${encodeURIComponent(groupId)}&userId=${encodeURIComponent(userId)}`,
          `${import.meta.env.VITE_API_BASE_URL}profile/group/join?groupId=${encodeURIComponent(groupId)}&userId=${encodeURIComponent(userId)}`,
        ]
        let left = false
        for (const u of leaveUrls) {
          try {
            const method = 'DELETE'
            const res = await fetch(u, { method, headers: { Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}` } })
            console.log('Tried leave url', u, 'status', res.status)
            if (res.ok) { left = true; break }

            if (res.status === 405) {
              const res2 = await fetch(u, { method: 'POST', headers: { Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}` } })
              console.log('Tried fallback POST for', u, 'status', res2.status)
              if (res2.ok) { left = true; break }
            }
          } catch (e) {
            console.error('Error trying leave url', u, e)

          }
        }

        if (!left) {
          throw new Error('Failed to leave group')
        }

        setIsMember(false)
        setProfile((prev) => prev ? ({ ...prev, members: Math.max(0, (prev.members || 0) - 1) }) : prev)
        setAlertBanner({ status: 'error', message: 'You left the troop.' })
        setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500)
      }
    } catch (err) {
      console.error('handleToggleMember error:', err)
      try { setAlertBanner({ status: 'error', message: err?.message || (isMember ? 'Failed to leave group' : 'Failed to join group') }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 3500) } catch (e) {}
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeleteGroup = async () => {
    if (isDeleting) return
    setIsDeleting(true)
    try {
      const leaderId = String(getCookie('id') || getCookie('userId') || '')
      const groupId = profile?.id || groupIdParam
      if (!leaderId) throw new Error('Missing leader id. Please log in again.')

      const url = `${import.meta.env.VITE_API_BASE_URL}profile/group/delete?groupId=${encodeURIComponent(groupId)}&leaderId=${encodeURIComponent(leaderId)}`
      console.log('Deleting group:', url)
      const accessToken = getCookie('AccessToken') || ''
      const res = await fetch(url, { method: 'DELETE', headers: { Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}` } })
      console.log('Delete status', res.status)
      const maybe = await res.json().catch(() => ({}))
      if (!res.ok) {
        console.error('Delete body', maybe)
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
        throw new Error(parsed.message || 'Failed to delete group')
      }

      setAlertBanner({ status: 'error', message: 'The Group has been deleted' })
      setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500)

      setTimeout(() => { window.location.href = '/friends' }, 900)
    } catch (err) {
      console.error('Delete error:', err)
      setAlertBanner({ status: 'error', message: err?.message || 'Failed to delete group' })
      setTimeout(() => setAlertBanner({ status: '', message: '' }), 3500)
      setIsDeleting(false)
      setShowDeleteModal(false)
    }
  }

  return (
    <>
    <DeleteGroupModal isOpen={showDeleteModal} onClose={() => setShowDeleteModal(false)} onSuccess={(info) => { try { setAlertBanner({ status: 'error', message: info?.message || 'The Group has been deleted' }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500) } catch(e){} }} groupId={profile?.id || groupIdParam} accent={accent} />
    <GroupEditModal isOpen={showEditGroupModal} onClose={() => setShowEditGroupModal(false)} onUpdated={(data) => { try { setProfile((p) => ({ ...p, ...data })) } catch (e) {} }} profile={profile} groupId={profile?.id || groupIdParam} accent={accent} motionSafe={true} />
    <div
      className="relative min-h-screen overflow-visible text-slate-50"
      style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}
    >
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />

      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </div>
          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">Communities</p>
                <h1 className="mt-2 text-3xl font-semibold text-white">Group Profile</h1>
                <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Group details and members.</p>
              </div>


              <div>
                {String(getCookie('id') || getCookie('userId') || '') === String(profile?.leaderId || groupIdParam) && (
                  <button onClick={() => setShowEditGroupModal(true)} className="rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-white shadow transition hover:bg-white/20">Update Group Information</button>
                )}
              </div>
            </header>

            {alertBanner.message && (
              <div className="mt-6 max-w-3xl mx-auto w-full px-4">
                <AlertBanner status={alertBanner.status} message={alertBanner.message} />
              </div>
            )}

            <div className="mt-10 flex flex-col items-center justify-center">
              {loading && <div className="text-lg text-slate-200/80">Loading group...</div>}
              {error && (
                <div className="w-full max-w-xl mx-auto px-4 mb-4">
                  <AlertBanner status="error" message={error} />
                </div>
              )}

              {profile && !loading && !error && (
                <div className="w-full">
                <div className="block md:hidden w-full max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-[0_20px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl flex flex-col items-center gap-6">




                  <div className="relative w-full">
                    <div className="h-36 sm:h-48 w-full overflow-hidden rounded-2xl bg-black/20" style={{ backgroundImage: profile.backgroundImage ? `url(${profile.backgroundImage})` : undefined, backgroundSize: 'cover', backgroundPosition: 'center' }} />
                    <div className="-mt-16 flex flex-col items-center w-full">
                      <div className="h-32 w-32 rounded-full border-4 border-white/20 bg-black/30 shadow-lg relative overflow-hidden">
                        {profile.image ? (
                          <img src={profile.image} alt={profile.name} className="h-full w-full object-cover rounded-full" />
                        ) : (
                          <span className="flex h-full w-full items-center justify-center text-5xl font-bold text-slate-200/60 bg-black/40">{(profile.name || '').slice(0,2).toUpperCase()}</span>
                        )}
                      </div>

                      <div className="mt-3 flex items-center gap-2">
                        <span className="text-2xl font-bold text-white">{profile.name}</span>
                      </div>


                      <div className="mt-2 flex justify-center md:hidden">
                        <button
                          type="button"
                          onClick={() => setShowQRModal(true)}
                          className="rounded-md border border-white/10 bg-white/5 px-3 py-1 text-sm font-semibold text-white hover:bg-white/10 transition flex items-center gap-2"
                          aria-label="Share group via QR"
                        >
                          <Share2 className="h-4 w-4" />
                          <span>Share</span>
                        </button>
                      </div>

                    </div>
                  </div>

                  <div className="flex w-full items-center justify-center gap-6 mt-4">
                    <div className="flex flex-col items-center">
                      <span className="text-lg font-bold text-white">{profile.members}</span>
                      <span className="text-xs text-slate-300/80">Members</span>
                    </div>
                  </div>

                  {profile.description && <p className="mt-2 text-center text-base text-slate-200/90">{profile.description}</p>}
                  {profile.createdAt && <p className="text-sm text-slate-300/80">Created: {new Date(profile.createdAt).toLocaleString()}</p>}

                  <div className="w-full mt-3 flex gap-3">
                    {String(getCookie('id') || getCookie('userId') || '') !== String(profile.leaderId) && (
                      <button className={`rounded-full border px-3 py-1.5 text-sm font-semibold transition w-full ${isMember ? 'border-white/30 bg-white/15 text-white' : 'border-white/10 bg-white/5 text-slate-200/80 hover:text-white'}`} disabled={actionLoading} onClick={handleToggleMember}>
                        {isMember ? 'Leave' : 'Join'}
                      </button>
                    )}

                    {String(profile.leaderId || '').length && (String(getCookie('id') || getCookie('userId') || '') === String(profile.leaderId)) && (
                      <button type="button" onClick={() => setShowDeleteModal(true)} className="rounded-full border border-rose-400 bg-rose-600/5 text-rose-300 px-3 py-1.5 text-sm font-semibold hover:bg-rose-600/10 transition w-full">Delete Group</button>
                    )}

                  </div>
                </div>


                <div className="hidden md:block">
                  <div className="w-full max-w-4xl rounded-3xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl">

                    <div className="h-36 w-full rounded-xl overflow-hidden mb-4 bg-black/20" style={{ backgroundImage: profile.backgroundImage ? `url(${profile.backgroundImage})` : undefined, backgroundSize: 'cover', backgroundPosition: 'center' }} />
                    <div className="flex flex-col sm:flex-row sm:items-start gap-6">
                      <div className="flex-shrink-0">
                        <div className="h-36 w-36 rounded-xl border-4 border-white/20 bg-black/30 overflow-hidden shadow-lg">
                          {profile.image ? (
                            <img src={profile.image} alt={profile.name} className="h-full w-full object-cover" />
                          ) : (
                            <div className="flex h-full w-full items-center justify-center text-6xl font-bold text-slate-200/60 bg-black/40">{(profile.name || '').slice(0,2).toUpperCase()}</div>
                          )}
                        </div>
                      </div>

                      <div className="flex-1">
                        <div className="flex items-start gap-4 md:gap-12">
                          <div className="min-w-0">
                            <div className="flex items-center gap-3">
                              <h2 className="text-2xl font-bold text-white leading-tight">{profile.name}</h2>

                            </div>

                            <div className="mt-1 text-sm text-slate-300/80">Leader: {profile.leaderName || '—'}</div>
                            {profile.createdAt && <div className="text-xs text-slate-300/80">Created: {new Date(profile.createdAt).toLocaleString()}</div>}

                            <p className="mt-4 text-sm text-slate-200/90">{profile.description}</p>
                          </div>

                          <div className="ml-auto flex items-center gap-6">
                            <div className="text-center">
                              <div className="text-lg font-bold text-white">{profile.members}</div>
                              <div className="text-xs text-slate-300/80">Members</div>
                            </div>

                            <div className="flex items-center gap-3">
                              {String(getCookie('id') || getCookie('userId') || '') !== String(profile.leaderId) && (
                                <button className={`rounded-full border px-4 py-2 text-sm font-semibold transition ${isMember ? 'border-white/30 bg-white/15 text-white' : 'border-white/10 bg-white/5 text-slate-200/80 hover:text-white'}`} disabled={actionLoading} onClick={handleToggleMember}>
                                  {isMember ? 'Leave' : 'Join'}
                                </button>
                              )}

                              <button type="button" onClick={() => setShowQRModal(true)} className="rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm text-slate-100 hover:bg-white/10 transition">Share</button>

                              {String(profile.leaderId || '').length && (String(getCookie('id') || getCookie('userId') || '') === String(profile.leaderId)) && (
                                <button type="button" onClick={() => setShowDeleteModal(true)} className="rounded-full border border-rose-400 bg-rose-600/5 text-rose-300 px-4 py-2 text-sm font-semibold hover:bg-rose-600/10 transition">Delete Group</button>
                              )}
                            </div>
                          </div>

                        </div>
                      </div>
                    </div>
                  </div>
                </div>


                <div className="mt-8 w-full">
                  <GroupPostsGrid groupId={profile?.id || groupIdParam} accent={accent} />
                </div>

              </div>
            )}
        </div>
      </div>
    </div>
    </div>
    </div>
      <NavigationBar accent={accent} variant="mobile" />
      <QRCodeShareModal isOpen={showQRModal} onClose={() => setShowQRModal(false)} accent={accent} targetId={groupIdParam} targetType="group" />
    </>
  )
}

export default GroupProfile
