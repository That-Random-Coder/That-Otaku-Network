import { useEffect, useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import AlertBanner from '../components/AlertBanner.jsx'
import accentOptions from '../theme/accentOptions.js'
import NavigationBar from '../components/NavigationBar.jsx'
import { CheckCircle2, Share2 } from 'lucide-react'
import QRCodeShareModal from '../components/QRCodeShareModal.jsx'
import clsx from 'clsx'


const toastSuccessOptions = {
  position: 'top-center',
  autoClose: 2500,
  hideProgressBar: false,
  closeOnClick: true,
  pauseOnHover: true,
  draggable: true,
  progress: undefined,
  style: {
    background: 'linear-gradient(90deg, rgba(16,185,129,0.12), rgba(16,185,129,0.18))',
    border: '1px solid rgba(16,185,129,0.18)',
    color: '#e6fffa',
    backdropFilter: 'blur(6px)',
    boxShadow: '0 10px 30px rgba(16,185,129,0.08)'
  },
}

const toastErrorOptions = {
  position: 'top-center',
  autoClose: 3500,
  hideProgressBar: false,
  closeOnClick: true,
  pauseOnHover: true,
  draggable: true,
  progress: undefined,
  style: {
    background: 'linear-gradient(90deg, rgba(244,63,94,0.08), rgba(244,63,94,0.12))',
    border: '1px solid rgba(244,63,94,0.12)',
    color: '#ffeef0',
    backdropFilter: 'blur(6px)',
    boxShadow: '0 10px 30px rgba(244,63,94,0.06)'
  },
}

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

function FriendProfile() {
  const { friendId: friendIdParam } = useParams()
  const [accentKey] = useState(() => {
    try {
      const stored = localStorage.getItem('accentKey')
      return stored || 'crimson-night'
    } catch {
      return 'crimson-night'
    }
  })
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [isFollowing, setIsFollowing] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [showQRModal, setShowQRModal] = useState(false)
  const [alertBanner, setAlertBanner] = useState({ status: '', message: '' })

  const usePrefersReducedMotion = () => {
    if (typeof window === 'undefined') return false
    return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  }
  const motionSafe = !usePrefersReducedMotion()

  useEffect(() => {
    const friendId = friendIdParam
    if (!friendId) {
      setError('No friend selected.')
      setLoading(false)
      return
    }
    const accessToken = getCookie('AccessToken') || ''
    setLoading(true)
    setError('')
    fetch(`${import.meta.env.VITE_API_BASE_URL}profile/user/profile/get?id=${encodeURIComponent(friendId)}&image=true`, {
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
        console.log('Friend profile API response:', data)
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

        setIsFollowing(Boolean(p.isFollow ?? p.isFollowing))
      })
      .catch((err) => setError(err.message || 'Failed to fetch profile.'))
      .finally(() => setLoading(false))
  }, [friendIdParam])
  const handleToggleFollow = async () => {
    if (actionLoading) return
    setActionLoading(true)
    const id = getCookie('id') || getCookie('userId')
    const friendId = friendIdParam
    const accessToken = getCookie('AccessToken') || ''

    try {
      if (!isFollowing) {

        const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}profile/user/${encodeURIComponent(id)}/follow/${encodeURIComponent(friendId)}`, {
          method: 'POST',
          headers: {
            Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}`,
          },
        })
        if (!res.ok) {
          const maybe = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
          throw new Error(parsed.message || 'Failed to follow user')
        }
        setIsFollowing(true)
        setProfile((prev) => prev ? ({ ...prev, followers: (prev.followers || 0) + 1 }) : prev)
        try { setAlertBanner({ status: 'success', message: 'Became their Nakama' }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500) } catch (e) {}
      } else {

        const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}profile/user/${encodeURIComponent(id)}/follow/${encodeURIComponent(friendId)}`, {
          method: 'DELETE',
          headers: {
            Authorization: accessToken.toLowerCase().startsWith('bearer ') ? accessToken : `Bearer ${accessToken}`,
          },
        })
        if (!res.ok) {
          const maybe = await res.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(maybe)
          throw new Error(parsed.message || 'Failed to unfollow user')
        }
        setIsFollowing(false)
        setProfile((prev) => prev ? ({ ...prev, followers: Math.max(0, (prev.followers || 0) - 1) }) : prev)
        try { setAlertBanner({ status: 'error', message: 'Betrayed a Nakama' }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 2500) } catch (e) {}
      }
    } catch (e) {

      try { setAlertBanner({ status: 'error', message: e?.message || (isFollowing ? 'Failed to unfollow' : 'Failed to follow') }); setTimeout(() => setAlertBanner({ status: '', message: '' }), 3500) } catch (err) {}
    } finally {
      setActionLoading(false)
    }
  }

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
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </div>
          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
            <h1 className="mt-2 text-3xl font-semibold text-white">Friend Profile</h1>
            <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">See your friend's public profile and stats.</p>
          </div>
        </header>

        {alertBanner.message && (
          <div className="mt-6 max-w-3xl mx-auto w-full px-4">
            <AlertBanner status={alertBanner.status} message={alertBanner.message} />
          </div>
        )}

        <div className="mt-10 flex flex-col items-center justify-center">
          {loading && <div className="text-lg text-slate-200/80">Loading profile...</div>}
          {error && (
            <div className="w-full max-w-xl mx-auto px-4 mb-4">
              <AlertBanner status="error" message={error} />
            </div>
          )}
          {profile && !loading && !error && (
            <div className="w-full max-w-xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-[0_20px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl flex flex-col items-center gap-6">

              <div className="relative flex flex-col items-center">
                <div className="h-32 w-32 rounded-full border-4 border-white/20 bg-black/30 overflow-hidden shadow-lg">
                  {profile.profileImg ? (
                    <img src={profile.profileImg} alt={profile.displayName} className="h-full w-full object-cover" />
                  ) : (
                    <span className="flex h-full w-full items-center justify-center text-5xl font-bold text-slate-200/60 bg-black/40">{profile.username?.slice(0,2)?.toUpperCase() || 'OT'}</span>
                  )}
                </div>
                <div className="mt-4 flex items-center justify-between w-full gap-3">
                  <div className="flex items-center gap-2">
                    <span className="text-2xl font-bold text-white">{profile.displayName}</span>
                    {profile.isVerified && (
                      <CheckCircle2 className="h-6 w-6 text-sky-400 drop-shadow-glow" title="Verified" />
                    )}
                  </div>


                  <div className="flex items-center gap-3">
                    {getCookie('id') !== friendIdParam && (
                      <button
                        className={`rounded-full border px-3 py-1.5 text-sm font-semibold transition ${isFollowing ? 'border-white/30 bg-white/15 text-white' : 'border-white/10 bg-white/5 text-slate-200/80 hover:text-white'}`}
                        onClick={handleToggleFollow}
                        disabled={actionLoading}
                      >
                        {isFollowing ? 'Unfollow' : 'Follow'}
                      </button>
                    )}

                    <button
                      type="button"
                      onClick={() => setShowQRModal(true)}
                      className="rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-slate-100 hover:bg-white/10 transition flex items-center gap-2"
                      aria-label="Share friend profile via QR"
                    >
                      <Share2 className="h-4 w-4" />
                      <span className="text-sm font-semibold text-white">Share</span>
                    </button>
                  </div>

                </div>
                <span className="text-base text-slate-300/90">@{profile.username}</span>
              </div>
              <div className="flex w-full items-center justify-center gap-8 mt-4">
                <div className="flex flex-col items-center">
                  <span className="text-lg font-bold text-white">{profile.followers}</span>
                  <span className="text-xs text-slate-300/80">Followers</span>
                </div>
                <div className="flex flex-col items-center">
                  <span className="text-lg font-bold text-white">{profile.following}</span>
                  <span className="text-xs text-slate-300/80">Following</span>
                </div>
              </div>
              {profile.bio && <p className="mt-2 text-center text-base text-slate-200/90">{profile.bio}</p>}
              {profile.location && <p className="text-sm text-slate-300/80">{profile.location}</p>}
            </div>
          )}
        </div>
      </div>
    </div>
  </div>

      <NavigationBar accent={accent} variant="mobile" />
      <QRCodeShareModal isOpen={showQRModal} onClose={() => setShowQRModal(false)} accent={accent} motionSafe={motionSafe} targetId={getCookie('friendId') || friendIdParam} />
    </div>
  )
}

export default FriendProfile
