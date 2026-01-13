import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Home, Users, PlusCircle, UserRound, LogOut, Search, SaveIcon } from 'lucide-react'
import { useState } from 'react'
import { AnimatePresence } from 'framer-motion'
import LoadingScreen from './LoadingScreen.jsx'

const navItems = [
  { key: 'home', label: 'Home', to: '/', icon: Home, showInMobile: true },
  { key: 'search', label: 'Search', to: '/friends', icon: Search, showInMobile: true },
  { key: 'groups', label: 'Groups', to: '/groups', icon: Users, showInMobile: true },
  { key: 'create', label: 'Create', to: '/create', icon: PlusCircle, showInMobile: true },
  { key: 'profile', label: 'Profile', to: '/profile', icon: UserRound, showInMobile: true, showInDesktop: false },
]


const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const baseClasses = 'flex items-center gap-2 rounded-full px-3 py-2 text-sm font-semibold transition'

function NavigationBar({ accent, variant = 'mobile' }) {
  const location = useLocation()
  const navigate = useNavigate()
  const [loggingOut, setLoggingOut] = useState(false)
  const username = getCookie('username') || 'Otaku'
  const userInitials = username.slice(0, 2).toUpperCase()
  const userId = getCookie('id') || getCookie('userId')

  
  const handleLogout = () => {
    if (loggingOut) return
    import('../lib/session.js').then(({ clearSession }) => clearSession(navigate, setLoggingOut)).catch((e) => console.error(e))
  }

  const renderItem = (item) => {
    const userId = getCookie('id') || getCookie('userId')
    const toPath = item.to === '/profile' ? (userId ? `/profile/${userId}` : '/profile') : item.to
    const isProfileItem = item.to === '/profile'
    const isActive = isProfileItem ? (location.pathname === '/profile' || location.pathname.startsWith('/profile/')) : location.pathname === item.to
    const Icon = item.icon
    const activeStyles = {
      background: `linear-gradient(90deg, ${accent?.mid || '#0ea5e9'}, ${accent?.strong || '#6366f1'})`,
      boxShadow: `0 12px 30px ${accent?.glow || 'rgba(14,165,233,0.3)'}`,
      color: '#fff',
      borderColor: 'rgba(255,255,255,0.18)',
    }

    
  if (variant === 'mobile' && item.showInMobile === false) return null

  return (
      <NavLink
        key={toPath}
        to={toPath}
        className={`${baseClasses} ${isActive ? 'text-white' : 'text-slate-200/80 hover:text-white'} ${
          variant === 'mobile' ? 'flex-1 justify-center border border-white/10' : 'border border-white/10'
        }`}
        style={isActive ? activeStyles : { background: 'rgba(255,255,255,0.04)', borderColor: 'rgba(255,255,255,0.08)' }}
      >
        <Icon className="h-4 w-4" />
        {/* On mobile, hide visible text and keep for screen readers */}
        {variant === 'mobile' ? <span className="sr-only">{item.label}</span> : <span>{item.label}</span>}
      </NavLink>
    )
  }

  if (variant === 'inline') {
    
    return (
      <>
        <aside className="hidden md:flex md:flex-col md:sticky md:top-6 md:self-start md:w-64 md:pt-6 md:pb-4 md:px-4 md:gap-4 md:h-[calc(100vh-4rem)] md:overflow-auto z-30">
          <div className="flex flex-col h-full rounded-3xl bg-black/70 border border-white/10 p-4 shadow-[0_18px_70px_rgba(0,0,0,0.55)] backdrop-blur-xl">
            <div className="flex items-center justify-start gap-2 mb-6 px-2">
              <div className="flex flex-col leading-tight">
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
              </div>
            </div>

            <nav className="flex-1 flex flex-col gap-1 overflow-auto pr-2">
              {navItems.filter((i) => i.showInDesktop !== false).map((item) => {
                const toPath = item.to === '/profile' ? (userId ? `/profile/${userId}` : '/profile') : item.to
                const Icon = item.icon
                const isProfileItem = item.to === '/profile'
                const isActive = isProfileItem ? (location.pathname === '/profile' || location.pathname.startsWith('/profile/')) : location.pathname === item.to
                const activeStyles = isActive
                  ? {
                      background: `linear-gradient(120deg, ${accent?.strong || '#e11d48'}, ${accent?.mid || '#6366f1'})`,
                      color: '#fff',
                      boxShadow: `0 14px 28px ${accent?.glow || 'rgba(99,102,241,0.35)'}`,
                      borderColor: 'rgba(255,255,255,0.08)',
                    }
                  : { background: 'rgba(255,255,255,0.03)', borderColor: 'rgba(255,255,255,0.05)' }

                return (
                  <NavLink
                    key={toPath}
                    to={toPath}
                    className={`flex items-center gap-3 rounded-2xl px-3 py-2.5 text-sm font-semibold transition border ${isActive ? 'text-white' : 'text-slate-200/90 hover:text-white hover:bg-white/8'}`}
                    style={activeStyles}
                  >
                    <Icon className="h-5 w-5" />
                    <span className="flex-1">{item.label}</span>
                    {isActive && <span className="h-2 w-2 rounded-full" style={{ background: accent?.strong || '#e11d48' }} />}
                  </NavLink>
                )
              })}
            </nav>

            <div className="mt-auto flex flex-col gap-6 pb-6">
              <button
                aria-label="View profile"
                type="button"
                onClick={() => navigate(userId ? `/profile/${userId}` : '/profile')}
                className="flex items-center gap-3 rounded-2xl border border-white/10 bg-white/5 px-3 py-2.5 text-left w-full hover:border-white/20 hover:bg-white/10 transition"
              >
                <div className="h-10 w-10 rounded-full bg-white/10 text-white font-semibold flex items-center justify-center border border-white/10">
                  {userInitials}
                </div>
                <div className="flex flex-col leading-tight">
                  <span className="text-sm font-semibold text-white">{username}</span>
                  <span className="text-xs text-slate-300/80">View profile</span>
                </div>
              </button>

              <button
                type="button"
                onClick={handleLogout}
                className={`${baseClasses} w-full justify-start border border-white/10 bg-white/5 text-slate-200/80 hover:text-white ${loggingOut ? 'opacity-70 cursor-not-allowed' : ''}`}
                disabled={loggingOut}
              >
                <LogOut className="h-4 w-4" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </aside>



        <AnimatePresence mode="wait">
          {loggingOut && <LoadingScreen key="logout-loader-inline" accent={accent} message="Logging out" />}
        </AnimatePresence>
      </>
    )
  }

  return (
    <>
      <nav className="fixed inset-x-4 bottom-4 z-30 md:hidden">
      <div className="grid grid-cols-5 items-center gap-2 rounded-2xl border border-white/10 bg-black/60 p-2 shadow-[0_18px_60px_rgba(0,0,0,0.55)] backdrop-blur-xl">
        {/* Mobile-specific ordering: home, search, create, groups, profile */}
        {(
          ['home','search','create','groups','profile']
            .map((k) => navItems.find((i) => i.key === k))
            .filter(Boolean)
        ).map(renderItem)}
      </div>
    </nav>
      <AnimatePresence mode="wait">
        {loggingOut && <LoadingScreen key="logout-loader" accent={accent} message="Logging out" />}
      </AnimatePresence>
    </>
  )
}

export default NavigationBar
