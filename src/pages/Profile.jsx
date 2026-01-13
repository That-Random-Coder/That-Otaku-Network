import { useEffect, useMemo, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { AnimatePresence } from 'framer-motion'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey, persistAccentKey } from '../theme/accentStorage.js'
import NavigationBar from '../components/NavigationBar.jsx'
import LoadingScreen from '../components/LoadingScreen.jsx'
import FollowListModal from '../components/FollowListModal.jsx'
import QRCodeShareModal from '../components/QRCodeShareModal.jsx'
import EditProfileModal from '../components/EditProfileModal.jsx'
import LoadingBar from 'react-top-loading-bar'
import { CheckCircle2, ArrowLeft, Share2 } from 'lucide-react'
import AlertBanner from '../components/AlertBanner.jsx'
import clsx from 'clsx'
import UserPostsGrid from '../components/UserPostsGrid.jsx' 


const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

function Profile() {
  const [accentKey, setAccentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showSettings, setShowSettings] = useState(false)
  const [showAccentLoader, setShowAccentLoader] = useState(false)
  const [pendingAccent, setPendingAccent] = useState(null)

  
  useEffect(() => {
    persistAccentKey(accentKey)
  }, [accentKey])

  const pendingTimerRef = useRef(null)
  const loaderTimerRef = useRef(null)
  const topLoaderRef = useRef(null)

  
  const usePrefersReducedMotion = () => {
    if (typeof window === 'undefined') return false
    return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }
  const motionSafe = !usePrefersReducedMotion()

  
  const [showFollowersModal, setShowFollowersModal] = useState(false)
  const [showFollowingModal, setShowFollowingModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showQRModal, setShowQRModal] = useState(false)
  const [alertBanner, setAlertBanner] = useState({ status: '', message: '' })
  const [loggingOut, setLoggingOut] = useState(false)
  const navigate = useNavigate()

  
  useEffect(() => {
    let timer = null
    const startLoader = () => {
      setShowAccentLoader(true)
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => setShowAccentLoader(false), 2000)
    }

    const onStorage = (e) => {
      if (e.key === 'accentKey') startLoader()
    }
    const onCustom = () => startLoader()

    window.addEventListener('storage', onStorage)
    window.addEventListener('accent:changed', onCustom)
    return () => {
      window.removeEventListener('storage', onStorage)
      window.removeEventListener('accent:changed', onCustom)
      if (timer) clearTimeout(timer)
      if (pendingTimerRef.current) clearTimeout(pendingTimerRef.current)
      if (loaderTimerRef.current) clearTimeout(loaderTimerRef.current)
    }
  }, [])

  
  const handleEditSave = async (payload) => {
    const uid = getCookie('userId') || getCookie('id')
    if (!uid) {
      
      return
    }

    try {
      
      topLoaderRef.current?.continuousStart()

      const tokenRaw = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
      const token = tokenRaw.trim() ? (tokenRaw.toLowerCase().startsWith('bearer ') ? tokenRaw : `Bearer ${tokenRaw}`) : ''

      const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}profile/user/profile/update?id=${encodeURIComponent(uid)}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: token,
        },
        body: JSON.stringify({ id: String(uid), ...payload }),
      })

      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(err)
        throw new Error(parsed.message || 'Failed to update profile')
      }

      const data = await res.json().catch(() => ({}))

      
      topLoaderRef.current?.complete()

      
      setProfile((prev) => prev ? ({ ...prev, ...payload }) : prev)

      
      try { if (payload?.displayName) document.cookie = `displayName=${encodeURIComponent(String(payload.displayName))}; path=/; max-age=604800; SameSite=Lax` } catch (e) {}

      
      setShowEditModal(false)

      
      try { setAlertBanner({ status: 'success', message: 'Profile updated Successfully' }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 5000) } catch (e) {}
    } catch (e) {
      topLoaderRef.current?.complete()
      try { setAlertBanner({ status: 'error', message: e.message || 'Failed to update profile' }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 5000) } catch (err) {}
    }
  }

  const handleAccentSelect = (key) => {
    if (key === accentKey) return
    
    if (pendingTimerRef.current) {
      clearTimeout(pendingTimerRef.current)
      pendingTimerRef.current = null
    }

    console.debug('[profile] accent select clicked', key, Date.now())
    setPendingAccent(key)

    
    setShowAccentLoader(true)
    if (loaderTimerRef.current) clearTimeout(loaderTimerRef.current)
    loaderTimerRef.current = setTimeout(() => setShowAccentLoader(false), 2000)

    
    pendingTimerRef.current = setTimeout(() => {
      console.debug('[profile] applying accent', key, Date.now())
      setAccentKey(key)
      setPendingAccent(null)
      pendingTimerRef.current = null
    }, 250)
  }

  const { userId: userIdParam } = useParams()

  useEffect(() => {
    const userId = userIdParam || getCookie('id') || getCookie('userId')
    if (!userId) {
      setError('User ID not found in URL or cookies.')
      setLoading(false)
      return
    }
    if (document.cookie.includes('friendId=')) {
      document.cookie = 'friendId=; path=/; max-age=0; SameSite=Lax'
    }
    setLoading(true)
    setError('')
    const accessToken = getCookie('AccessToken') || ''
    fetch(`${import.meta.env.VITE_API_BASE_URL}profile/user/profile/get?id=${encodeURIComponent(userId)}&image=true`, {
      headers: {
        Authorization: accessToken.toLowerCase().startsWith('bearer ')
          ? accessToken
          : `Bearer ${accessToken}`,
      },
    })
      .then(async (res) => {
        if (!res.ok) {
          const maybe = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
          throw new Error(parsed.message || 'Failed to fetch profile.')
        }
        return res.json()
      })
      .then((data) => {
        const p = data?.data || data
        setProfile({
          username: p.username || '',
          displayName: p.displayName || '',
          bio: p.bio || '',
          followers: p.followers || 0,
          following: p.following || 0,
          location: p.location || '',
          profileImg: p.profileImg ? `data:image/jpeg;base64,${p.profileImg}` : '',
          isVerified: !!p.isVerified,
        })
        
        try { const dn = p.displayName || ''; if (dn) document.cookie = `displayName=${encodeURIComponent(String(dn))}; path=/; max-age=604800; SameSite=Lax` } catch (e) {}
      })
      .catch((err) => setError(err.message || 'Failed to fetch profile.'))
      .finally(() => setLoading(false))
    
  }, [accentKey])

  const cookieUserId = getCookie('id') || getCookie('userId') || ''
  const profileOwnerId = userIdParam || cookieUserId

  return (
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
        <LoadingBar color={accent.mid} ref={topLoaderRef} height={8} className="-mx-4 sm:-mx-8 lg:-mx-12 xl:-mx-16" style={{ boxShadow: `0 8px 30px ${accent.glow}` }} />
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </div>

          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
            <h1 className="mt-2 text-3xl font-semibold text-white">Profile</h1>
            <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Tune your handle, bio, and preferences. Privacy and notifications live here.</p>
          </div>
          <div>
            <button
              className="rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-white shadow transition hover:bg-white/20"
              onClick={() => setShowSettings((v) => !v)}
            >
              {showSettings ? 'Close Settings' : 'Settings'}
            </button>
          </div>
        </header>
        {/* Mobile-only settings block (shown inside content area) */}
        {showSettings && (
          <div className="mt-8 w-full max-w-xl mx-auto rounded-3xl border border-white/10 bg-white/10 p-6 shadow-lg backdrop-blur-2xl flex flex-col gap-6 md:hidden">
            <div className="flex items-center justify-between">
              <button
                type="button"
                aria-label="Close settings"
                onClick={() => setShowSettings(false)}
                className="rounded-full border border-white/10 bg-white/5 p-2 text-slate-100 hover:bg-white/10 transition"
              >
                <ArrowLeft className="h-4 w-4" />
              </button>
              <h2 className="text-xl font-semibold text-white mb-2">Settings</h2>
              <div className="w-8" />
            </div>

            <div>
              <label className="block text-sm font-semibold text-white mb-2">Accent Color</label>
              <div className="flex flex-wrap gap-3">
                {accentOptions.map((opt) => (
                  <button
                    key={opt.key}
                    type="button"
                    aria-label={`${opt.label} accent`}
                    onClick={() => handleAccentSelect(opt.key)}
                    disabled={pendingAccent && pendingAccent !== opt.key}
                    className={clsx(
                      'h-9 w-9 rounded-full border-2 transition',
                      pendingAccent === opt.key ? 'opacity-90 cursor-wait scale-105' : '',
                      accentKey === opt.key
                        ? 'ring-2 ring-white/80 border-white/70 scale-110'
                        : 'border-white/20 opacity-80 hover:opacity-100 hover:scale-105'
                    )}
                    style={{ background: `radial-gradient(circle at 30% 30%, ${opt.colors.strong}, ${opt.colors.mid})` }}
                  />
                ))}
              </div>
            </div>

            <div className="mt-3">
              <p className="text-xs text-slate-300/80">Your accent color will be used across all pages after login.</p>
              <button onClick={() => { setShowEditModal(true); setShowSettings(false) }} className="mt-3 w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-white hover:bg-white/10 transition md:hidden">Edit Profile</button>
              <button onClick={() => { setShowSettings(false); import('../lib/session.js').then(({ clearSession }) => { clearSession(navigate, setLoggingOut) }).catch(e => console.error(e)) }} disabled={loggingOut} className={`mt-3 w-full rounded-xl border border-white/10 px-4 py-2 text-sm font-semibold transition ${loggingOut ? 'bg-white/10 text-slate-300/60 cursor-wait' : 'bg-white/5 text-white hover:bg-white/10'}`}>
                {loggingOut ? 'Logging out...' : 'Logout'}
              </button>
            </div>

            <AnimatePresence mode="wait">
              {loggingOut && <LoadingScreen key="logout-loader-mobile" accent={accent} message="Logging out" />}
            </AnimatePresence>
          </div>
        )}

        {alertBanner.message && (
          <div className="mt-6 max-w-3xl mx-auto w-full px-4">
            <AlertBanner status={alertBanner.status} message={alertBanner.message} />
          </div>
        )}

            {/* Mount posts grid immediately so it can fetch on page load */}
           
            <AlertBanner status="error" message={error} />
          {profile && !loading && !error && (
            <>
              {/* Mobile — revert to old centered layout */}
              <div className="block md:hidden mt-6">
                <div className="w-full max-w-xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-[0_20px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl flex flex-col items-center gap-6">
                  <div className="relative flex flex-col items-center">
                    <div className="h-32 w-32 rounded-full border-4 border-white/20 bg-black/30 shadow-lg relative overflow-visible">
                      <div className="h-full w-full overflow-hidden rounded-full">
                        {profile.profileImg ? (
                          <img src={profile.profileImg} alt={profile.displayName} className="h-full w-full object-cover" />
                        ) : (
                          <span className="flex h-full w-full items-center justify-center text-5xl font-bold text-slate-200/60 bg-black/40">{profile.username?.slice(0,2)?.toUpperCase() || 'OT'}</span>
                        )}
                      </div>
                    </div>

                    <div className="mt-3 flex items-center gap-2">
                      <span className="text-2xl font-bold text-white">{profile.displayName}</span>
                      {profile.isVerified && (
                        <CheckCircle2 className="h-5 w-5 text-sky-400 drop-shadow-glow" title="Verified" />
                      )}


                    </div>

                    <span className="text-sm text-slate-300/80">@{profile.username}</span>

                    {/* Mobile-only Share button placed under username, above counts */}
                    <div className="mt-2 flex justify-center md:hidden">
                      <button
                        type="button"
                        onClick={() => setShowQRModal(true)}
                        className="rounded-md border border-white/10 bg-white/5 px-3 py-1 text-sm font-semibold text-white hover:bg-white/10 transition flex items-center gap-2"
                        aria-label="Share profile via QR"
                      >
                        <Share2 className="h-4 w-4" />
                        <span>Share</span>
                      </button>
                    </div>
                  </div>

                  <div className="flex w-full items-center justify-center gap-6 mt-4">
                    <button
                      type="button"
                      onClick={() => setShowFollowersModal(true)}
                      className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-3 transition hover:border-white/20 hover:bg-white/8 w-36"
                      aria-label="Open followers list"
                    >
                      <div className="flex flex-col items-center">
                        <span className="text-lg font-bold text-white">{profile.followers}</span>
                        <span className="text-xs text-slate-300/80">Followers</span>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => setShowFollowingModal(true)}
                      className="group relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 p-3 transition hover:border-white/20 hover:bg-white/8 w-36"
                      aria-label="Open following list"
                    >
                      <div className="flex flex-col items-center">
                        <span className="text-lg font-bold text-white">{profile.following}</span>
                        <span className="text-xs text-slate-300/80">Following</span>
                      </div>
                    </button>
                  </div>

                  {profile.bio && <p className="mt-2 text-center text-base text-slate-200/90">{profile.bio}</p>}
                  {profile.location && <p className="text-sm text-slate-300/80">{profile.location}</p>}
                </div>
              </div>
              

              {/* Desktop — Instagram-style layout (unchanged) */}
              <div className="hidden md:block mt-6">
                <div className="w-full max-w-4xl rounded-3xl border border-white/10 bg-white/5 p-6 shadow-[0_20px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl">
                  <div className="flex flex-col sm:flex-row sm:items-start gap-6">
                    {/* Avatar column */}
                    <div className="flex-shrink-0">
                      <div className="h-36 w-36 rounded-full border-4 border-white/20 bg-black/30 overflow-hidden shadow-lg">
                        {profile.profileImg ? (
                          <img src={profile.profileImg} alt={profile.displayName} className="h-full w-full object-cover" />
                        ) : (
                          <span className="flex h-full w-full items-center justify-center text-6xl font-bold text-slate-200/60 bg-black/40">{profile.username?.slice(0,2)?.toUpperCase() || 'OT'}</span>
                        )}
                      </div>
                    </div>
                    

                    {/* Info column */}
                    <div className="flex-1">
                      <div className="flex items-start gap-4 md:gap-12">
                        <div>
                          <div className="flex items-center gap-3">
                            <h2 className="text-2xl font-bold text-white leading-tight">{profile.displayName}</h2>
                            {profile.isVerified && (
                              <CheckCircle2 className="h-5 w-5 text-sky-400 drop-shadow-glow" title="Verified" />
                            )}
                          </div>

                          <div className="mt-1 text-sm text-slate-300/80">@{profile.username}</div>
                        </div>

                        <div className="ml-auto flex items-center gap-3">
                          <button
                            type="button"
                            onClick={() => setShowFollowersModal(true)}
                            className="flex flex-col items-center justify-center rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-center hover:bg-white/8 transition w-28"
                            aria-label="Open followers list"
                          >
                            <span className="text-lg font-semibold text-white">{profile.followers}</span>
                            <span className="text-xs text-slate-300/80">Followers</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => setShowFollowingModal(true)}
                            className="flex flex-col items-center justify-center rounded-lg border border-white/10 bg-white/5 px-4 py-2 text-center hover:bg-white/8 transition w-28"
                            aria-label="Open following list"
                          >
                            <span className="text-lg font-semibold text-white">{profile.following}</span>
                            <span className="text-xs text-slate-300/80">Following</span>
                          </button>

                          <button
                            type="button"
                            onClick={() => setShowQRModal(true)}
                            className="rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-slate-100 hover:bg-white/10 transition flex items-center gap-2 shadow-sm"
                          >
                            <Share2 className="h-4 w-4" />
                            <span className="text-sm font-semibold text-white">Share</span>
                          </button>

                        </div>
                      </div>

                      {/* Bio and location */}
                      {profile.bio && <p className="mt-4 text-base text-slate-200/90">{profile.bio}</p>}
                      {profile.location && <p className="mt-2 text-sm text-slate-300/80">{profile.location}</p>}
                    </div>
                  </div>
                </div>
              </div>
               <div className="mt-6 w-full max-w-4xl mx-auto px-4 md:px-0">
              <UserPostsGrid profileUserId={profileOwnerId} accent={accent} />
            </div>
            </>)}


              {/* Fixed desktop right rail for Settings (matches left nav feel) */}
              <div className={`hidden md:flex md:flex-col md:fixed md:right-6 md:top-6 md:w-64 z-40 ${showSettings ? '' : 'pointer-events-none opacity-0'}`}>
                {showSettings && (
                  <div className="flex flex-col h-full rounded-3xl bg-black/70 border border-white/10 p-4 shadow-[0_18px_70px_rgba(0,0,0,0.55)] backdrop-blur-xl md:sticky md:top-6 md:self-start">
                    <div className="flex items-center justify-between gap-2 mb-4 px-2">
                      <button
                        type="button"
                        aria-label="Close settings"
                        onClick={() => setShowSettings(false)}
                        className="rounded-full border border-white/10 bg-white/5 p-2 text-slate-100 hover:bg-white/10 transition"
                      >
                        <ArrowLeft className="h-4 w-4" />
                      </button>
                    </div>

                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-white mb-3">Appearance</h3>
                      <label className="block text-sm font-semibold text-white mb-2">Accent Color</label>
                      <div className="flex flex-wrap gap-3">
                        {accentOptions.map((opt) => (
                          <button
                            key={opt.key}
                            type="button"
                            aria-label={`${opt.label} accent`}
                            onClick={() => handleAccentSelect(opt.key)}
                            disabled={pendingAccent && pendingAccent !== opt.key}
                            className={clsx(
                              'h-9 w-9 rounded-full border-2 transition',
                              pendingAccent === opt.key ? 'opacity-90 cursor-wait scale-105' : '',
                              accentKey === opt.key
                                ? 'ring-2 ring-white/80 border-white/70 scale-110'
                                : 'border-white/20 opacity-80 hover:opacity-100 hover:scale-105'
                            )}
                            style={{ background: `radial-gradient(circle at 30% 30%, ${opt.colors.strong}, ${opt.colors.mid})` }}
                          />
                        ))}
                      </div>

                      <div className="mt-6">
                        <p className="text-xs text-slate-300/80">Your accent color will be used across all pages after login.</p>
                        <div className="mt-4">
                          <button onClick={() => setShowEditModal(true)} className="w-full rounded-xl border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-white hover:bg-white/10 transition hidden md:block">Edit Profile</button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
        </div>
      </div>
      <FollowListModal isOpen={showFollowersModal} onClose={() => setShowFollowersModal(false)} type="followers" accent={accent} motionSafe={motionSafe} />
      <FollowListModal isOpen={showFollowingModal} onClose={() => setShowFollowingModal(false)} type="following" accent={accent} motionSafe={motionSafe} />
      <QRCodeShareModal isOpen={showQRModal} onClose={() => setShowQRModal(false)} accent={accent} motionSafe={motionSafe} />
      <EditProfileModal isOpen={showEditModal} onClose={() => setShowEditModal(false)} initial={profile || {}} accent={accent} motionSafe={motionSafe} onSave={handleEditSave} />

      <NavigationBar accent={accent} variant="mobile" />

      <AnimatePresence mode="wait">
        {showAccentLoader && <LoadingScreen key="loader-accent" accent={accent} />}
      </AnimatePresence>
    </div>
    </div>
  )
}

export default Profile
