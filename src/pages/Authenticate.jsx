import { Suspense, lazy, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import apiClient from '../lib/apiClient'
import FormField from '../components/FormField.jsx'
import AlertBanner from '../components/AlertBanner.jsx'
import PasswordField from '../components/PasswordField.jsx'
import CodeInputGroup from '../components/CodeInputGroup.jsx'
import WallSkeleton from '../components/WallSkeleton.jsx'
import LoginForm from '../components/LoginForm.jsx'
import RegisterForm from '../components/RegisterForm.jsx'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey, persistAccentKey } from '../theme/accentStorage.js'
import CookieConsentGate from '../components/CookieConsentGate.jsx'
import LoadingScreen from '../components/LoadingScreen.jsx'

const AnimeWall = lazy(() => import('../components/AnimeWall.jsx'))

const usePrefersReducedMotion = () => {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false)

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    const updatePreference = () => setPrefersReducedMotion(mediaQuery.matches)

    updatePreference()
    mediaQuery.addEventListener('change', updatePreference)

    return () => mediaQuery.removeEventListener('change', updatePreference)
  }, [])

  return prefersReducedMotion
}

const useDeviceProfile = () => {
  const [isLowPower, setIsLowPower] = useState(false)

  useEffect(() => {
    const cores = navigator.hardwareConcurrency || 4
    const memory = navigator.deviceMemory || 4
    setIsLowPower(cores <= 4 || memory <= 4)
  }, [])

  return isLowPower
}
const TogglePill = ({ active, label, onClick, motionSafe, accent }) => (
  <motion.button
    type="button"
    onClick={onClick}
    className={`relative rounded-full px-4 py-2 text-sm font-semibold transition-colors ${
      active ? 'text-white' : 'text-slate-200/80'
    }`}
    whileHover={motionSafe ? { scale: 1.04 } : undefined}
    whileTap={motionSafe ? { scale: 0.97 } : undefined}
    aria-pressed={active}
  >
    {active && (
      <motion.span
        layoutId="pill"
        className="absolute inset-0 -z-10 rounded-full"
        style={{
          backgroundImage: `linear-gradient(90deg, ${accent?.mid || '#7c3aed'}, ${accent?.strong || '#5b21b6'})`,
          boxShadow: `0 0 28px ${accent?.glow || 'rgba(124,58,237,0.35)'}`,
        }}
        transition={{ type: 'spring', stiffness: 320, damping: 30 }}
      />
    )}
    <span className="relative z-10">{label}</span>
  </motion.button>
)

const AuthCard = ({
  mode,
  setMode,
  motionSafe,
  accent,
  onAccentChange,
  authStage,
  formValues,
  onFormChange,
  onLoginSubmit,
  onRegisterSubmit,
  onCodeSubmit,
  onCodeChange,
  onCodeKeyDown,
  codeDigits,
  codeRefs,
  formErrors,
  apiMessage,
  apiStatus,
  isSubmitting,
  passwordVisibility,
  onTogglePassword,
}) => (
  <motion.div
    initial={motionSafe ? { opacity: 0, y: 20, scale: 0.98 } : false}
    animate={motionSafe ? { opacity: 1, y: 0, scale: 1 } : { opacity: 1 }}
    transition={{ type: 'spring', stiffness: 140, damping: 18 }}
    className="relative w-full max-w-2xl overflow-hidden rounded-3xl border backdrop-blur-3xl"
    style={{
      borderColor: 'rgba(255,255,255,0.08)',
      backgroundColor: accent.surface,
      boxShadow: `0 30px 90px rgba(0,0,0,0.45), 0 0 35px ${accent.glow}`,
    }}
  >
    <div className="pointer-events-none absolute -left-20 -top-24 h-64 w-64 rounded-full bg-purple-500/20 blur-3xl" />
    <div className="pointer-events-none absolute -bottom-24 -right-20 h-72 w-72 rounded-full bg-indigo-500/25 blur-3xl" />

    <div className="relative p-8 sm:p-10">
      <div className="flex flex-col items-center gap-4 text-center sm:flex-row sm:items-start sm:justify-between sm:text-left">
        <div className="flex flex-col items-center sm:items-start">
          <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
          <h1 className="mt-2 text-3xl font-semibold text-white">
            {authStage === 'welcome'
              ? 'Welcome'
              : authStage === 'verify'
                ? 'Verify Code'
                : mode === 'login'
                  ? 'Sign In'
                  : 'Sign Up'}
          </h1>
          <p className="mt-1 text-sm text-slate-300 max-w-xl">
            Chat, review, like, and dislike your favorite anime together.
          </p>
        </div>
        <div className="flex flex-col items-center gap-3 sm:items-end">
          <div className="flex flex-wrap items-center justify-center gap-2 rounded-full border border-white/10 bg-white/5 p-1 lg:flex-nowrap">
            <TogglePill
              label="Login"
              active={mode === 'login'}
              onClick={() => (authStage === 'verify' || authStage === 'welcome' ? undefined : setMode('login'))}
              motionSafe={motionSafe}
              accent={accent}
            />
            <TogglePill
              label="Register"
              active={mode === 'register'}
              onClick={() => (authStage === 'verify' || authStage === 'welcome' ? undefined : setMode('register'))}
              motionSafe={motionSafe}
              accent={accent}
            />
          </div>
          <div className="flex flex-wrap items-center justify-center gap-2 lg:flex-nowrap">
            {accentOptions.map((opt) => (
              <div key={opt.key} className="relative group">
                <button
                  type="button"
                  aria-label={`${opt.label} accent`}
                  onClick={() => onAccentChange(opt.key)}
                  className={`h-7 w-7 rounded-full border border-white/20 transition ${
                    accent.key === opt.key ? 'ring-2 ring-white/70 scale-105' : 'opacity-75 hover:opacity-100 hover:scale-[1.08]'
                  }`}
                  style={{ background: `radial-gradient(circle at 30% 30%, ${opt.colors.strong}, ${opt.colors.mid})` }}
                />
                <span
                  className="pointer-events-none absolute left-1/2 top-9 -translate-x-1/2 whitespace-nowrap rounded-full border border-white/10 bg-black/70 px-3 py-1 text-xs font-semibold text-white opacity-0 backdrop-blur-lg shadow-glow transition duration-200 ease-out group-hover:opacity-100 group-hover:-translate-y-1 group-hover:scale-100 scale-95"
                  style={{ boxShadow: `0 14px 36px ${opt.colors.glow}` }}
                >
                  {opt.label}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-8">
        <AlertBanner status={apiStatus} message={apiMessage} />
        <AnimatePresence mode="wait">
          {authStage === 'welcome' ? (
            <motion.div
              key="welcome"
              initial={motionSafe ? { opacity: 0, y: 10 } : false}
              animate={motionSafe ? { opacity: 1, y: 0 } : { opacity: 1 }}
              exit={motionSafe ? { opacity: 0, y: -10 } : { opacity: 0 }}
              transition={{ type: 'spring', stiffness: 160, damping: 18 }}
              className="space-y-4 text-center"
            >
              <h2 className="text-2xl font-semibold text-white">Welcome aboard!</h2>
              <p className="text-sm text-slate-200/90">You are now verified and ready to enter ThatOtakuNetwork.</p>
            </motion.div>
          ) : authStage === 'verify' ? (
            <motion.form
              key="verify"
              onSubmit={onCodeSubmit}
              initial={motionSafe ? { opacity: 0, x: 12 } : false}
              animate={motionSafe ? { opacity: 1, x: 0 } : { opacity: 1 }}
              exit={motionSafe ? { opacity: 0, x: -12 } : { opacity: 0 }}
              transition={{ type: 'spring', stiffness: 160, damping: 18 }}
              className="space-y-6"
            >
              <p className="text-sm text-slate-200/90 text-center">
                Enter the 6-digit code sent to your email.
              </p>
              <CodeInputGroup codeDigits={codeDigits} onChange={onCodeChange} onKeyDown={onCodeKeyDown} codeRefs={codeRefs} />
              <motion.button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-2xl border border-white/10 px-5 py-3 text-base font-semibold text-white shadow-lg focus:outline-none focus:ring-2 disabled:opacity-60"
                style={{
                  backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
                  boxShadow: `0 10px 30px ${accent.glow}`,
                }}
                whileHover={motionSafe && !isSubmitting ? { scale: 1.02, boxShadow: '0 10px 30px rgba(126, 87, 194, 0.35)' } : undefined}
                whileTap={motionSafe && !isSubmitting ? { scale: 0.98 } : undefined}
              >
                {isSubmitting ? 'Verifying...' : 'Verify code'}
              </motion.button>
            </motion.form>
          ) : mode === 'login' ? (
            <motion.div
              key="login"
              initial={motionSafe ? { opacity: 0, x: -16 } : false}
              animate={motionSafe ? { opacity: 1, x: 0 } : { opacity: 1 }}
              exit={motionSafe ? { opacity: 0, x: 16 } : { opacity: 0 }}
              transition={{ type: 'spring', stiffness: 160, damping: 18 }}
            >
              <LoginForm
                formValues={formValues}
                formErrors={formErrors}
                onFormChange={onFormChange}
                onSubmit={onLoginSubmit}
                passwordVisibility={passwordVisibility}
                onTogglePassword={onTogglePassword}
                motionSafe={motionSafe}
                accent={accent}
                isSubmitting={isSubmitting}
              />
            </motion.div>
          ) : (
            <motion.div
              key="register"
              initial={motionSafe ? { opacity: 0, x: 16 } : false}
              animate={motionSafe ? { opacity: 1, x: 0 } : { opacity: 1 }}
              exit={motionSafe ? { opacity: 0, x: -16 } : { opacity: 0 }}
              transition={{ type: 'spring', stiffness: 160, damping: 18 }}
            >
              <RegisterForm
                formValues={formValues}
                formErrors={formErrors}
                onFormChange={onFormChange}
                onSubmit={onRegisterSubmit}
                passwordVisibility={passwordVisibility}
                onTogglePassword={onTogglePassword}
                motionSafe={motionSafe}
                accent={accent}
                isSubmitting={isSubmitting}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  </motion.div>
)

function Authenticate() {
  const [mode, setMode] = useState('login')
  const location = useLocation()
  const navigate = useNavigate()
  const [accentKey, setAccentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const [wallLoading, setWallLoading] = useState(true)
  const [splashTimerDone, setSplashTimerDone] = useState(false)
  const [authStage, setAuthStage] = useState('auth')
  const [postAuthRedirect, setPostAuthRedirect] = useState('/')
  const [formValues, setFormValues] = useState({ username: '', email: '', password: '', confirmPassword: '' })
  const [formErrors, setFormErrors] = useState({})
  const [apiMessage, setApiMessage] = useState('')
  const [apiStatus, setApiStatus] = useState('info')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [codeDigits, setCodeDigits] = useState(Array(6).fill(''))
  const [passwordVisibility, setPasswordVisibility] = useState({ login: false, register: false, confirm: false })
  const [cookiesAllowed, setCookiesAllowed] = useState(false)
  const [cookiePromptMessage, setCookiePromptMessage] = useState('')
  const codeRefs = useRef([])
  const reduceMotion = usePrefersReducedMotion()
  const isLowPower = useDeviceProfile()
  const motionSafe = !reduceMotion
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])

  const storeSession = (data) => {
    if (!data || typeof window === 'undefined' || typeof document === 'undefined') return
    Object.entries(data).forEach(([key, val]) => {
      if (val === undefined || val === null) return
      const encoded = typeof val === 'object' ? encodeURIComponent(JSON.stringify(val)) : encodeURIComponent(String(val))
      const lowerKey = key.toLowerCase()
      const normalizedKey = lowerKey === 'refreshtoken' ? 'RefreshToken' : lowerKey === 'accesstoken' ? 'AccessToken' : key
      document.cookie = `${normalizedKey}=${encoded}; path=/; max-age=604800; SameSite=Lax`
      if (lowerKey === 'refreshtoken') {
        localStorage.setItem('RefreshToken', String(val))
      } else if (lowerKey === 'accesstoken') {
        localStorage.setItem('AccessToken', String(val))
        document.cookie = `RefreshToken=${encoded}; path=/; max-age=604800; SameSite=Lax`
        localStorage.setItem('RefreshToken', String(val))
      }
    })
    localStorage.setItem('sessionActive', 'true')
  }

  const setApiFeedback = (status, message) => {
    setApiStatus(status)
    setApiMessage(message)
  }

  const togglePasswordVisibility = (key) => {
    setPasswordVisibility((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleAllowCookies = () => {
    setCookiesAllowed(true)
    setCookiePromptMessage('')
    if (typeof window !== 'undefined') {
      localStorage.setItem('cookieConsent', 'allowed')
      document.cookie = 'cookieConsent=allowed; path=/; max-age=31536000; SameSite=Lax'
    }
  }

  const handleRejectCookies = () => {
    setCookiePromptMessage('Cookies are required to continue. Please allow them to keep your session secure.')
  }

  useEffect(() => {
    persistAccentKey(accentKey)
  }, [accentKey])

  const handleFormChange = (event) => {
    const { name, value } = event.target
    setFormValues((prev) => {
      const nextValues = { ...prev, [name]: value }
      setFormErrors((prevErr) => {
        const nextErr = { ...prevErr }
        const pwd = nextValues.password
        const conf = nextValues.confirmPassword
        if (mode === 'register' && authStage === 'auth') {
          if (conf && pwd && conf !== pwd) {
            nextErr.confirmPassword = 'Passwords must match'
          } else {
            delete nextErr.confirmPassword
          }
        } else {
          delete nextErr.confirmPassword
        }
        return nextErr
      })
      return nextValues
    })
  }

  const validateRegister = () => {
    const errors = {}
    const username = formValues.username.trim()
    const email = formValues.email.trim()
    const password = formValues.password
    const confirmPassword = formValues.confirmPassword
    if (!username) {
      errors.username = 'Username must not be blank'
    } else {
      if (username.length < 6 || username.length > 50) errors.username = 'Username length must be between 6 and 50 characters'
      if (!/^\w+$/.test(username)) errors.username = 'Username can only contain letters, digits, and underscores'
    }
    if (!email) {
      errors.email = 'Email must not be blank'
    } else {
      if (email.length > 100) errors.email = 'Email length must not exceed 100 characters'
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(email)) errors.email = 'Invalid email format'
    }
    if (!password) {
      errors.password = 'Password must not be blank'
    } else if (password.length < 6 || password.length > 60) {
      errors.password = 'Password length must be between 6 and 60 characters'
    }
    if (!confirmPassword) {
      errors.confirmPassword = 'Confirm password must not be blank'
    } else if (confirmPassword !== password) {
      errors.confirmPassword = 'Passwords must match'
    }
    return errors
  }

  const handleRegisterSubmit = async (event) => {
    event.preventDefault()
    const errors = validateRegister()
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) {
      setApiFeedback('error', 'Please resolve the highlighted fields.')
      return
    }
    setIsSubmitting(true)
    setApiFeedback('info', '')
    try {
      const response = await apiClient.post('/auth/public/sign-up', {
        username: formValues.username.trim(),
        email: formValues.email.trim(),
        password: formValues.password,
      })
      const userId = response?.data?.id
      if (userId) {
        document.cookie = `userId=${userId}; path=/; max-age=604800; SameSite=Lax`
      }
      if (formValues.username.trim()) {
        const uname = `${formValues.username.trim()}_`
        document.cookie = `username=${encodeURIComponent(uname)}; path=/; max-age=604800; SameSite=Lax`
      }
      setAuthStage('verify')
      setApiFeedback('info', 'Check your email for the 6-digit code')
      setCodeDigits(Array(6).fill(''))
    } catch (error) {
      // Prefer structured API validation errors array, map to form fields
      const body = error?.response?.data
      if (Array.isArray(body) && body.length) {
        const map = {}
        const msgs = []
        body.forEach((it) => {
          const key = it?.keyError || it?.field || 'error'
          const msg = it?.valueError || it?.message || JSON.stringify(it)
          map[key] = msg
          msgs.push(msg)
        })
        setFormErrors((prev) => ({ ...prev, ...map }))
        setApiFeedback('error', msgs.join(' • '))
      } else {
        const message = body?.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Registration failed. Please try again.')
        setApiFeedback('error', message)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLoginSubmit = (event) => {
    event.preventDefault()
    const errors = {}
    const identifier = formValues.email.trim()
    if (!identifier) errors.email = 'Email / Username must not be blank'
    const password = formValues.password
    if (!password) errors.password = 'Password must not be blank'
    setFormErrors(errors)
    setApiFeedback('info', '')
    if (Object.keys(errors).length > 0) {
      setApiFeedback('error', 'Please provide both identifier and password.')
      return
    }
    setIsSubmitting(true)
    apiClient
      .post('/auth/public/login', { identifier, password })
      .then((response) => {
        storeSession(response?.data || {})
        // Set displayName cookie if available in response (defensive)
        try {
          const d = response?.data?.displayName || response?.data?.displayname || response?.data?.user?.displayName || response?.data?.userName || ''
          if (d) document.cookie = `displayName=${encodeURIComponent(String(d))}; path=/; max-age=604800; SameSite=Lax`
        } catch (e) {}
        localStorage.setItem('profileCompleted', 'true')
        setPostAuthRedirect('/')
        setApiFeedback('success', 'Login successful')
        setAuthStage('welcome')
        navigate('/', { replace: true })
      })
      .catch(async (error) => {
      const body = error?.response?.data
      try {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        setApiFeedback('error', parsed.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Login failed. Please try again.'))
      } catch (err) {
        const message = body?.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Login failed. Please try again.')
        setApiFeedback('error', message)
      }
      })
      .finally(() => setIsSubmitting(false))
  }

  const handleCodeChange = (index, value) => {
    if (!/^[0-9]?$/.test(value)) return
    setCodeDigits((prev) => {
      const next = [...prev]
      next[index] = value
      return next
    })
    if (value && codeRefs.current[index + 1]) {
      codeRefs.current[index + 1].focus()
    }
  }

  const handleCodeKeyDown = (index, event) => {
    if (event.key === 'Backspace' && !codeDigits[index] && codeRefs.current[index - 1]) {
      codeRefs.current[index - 1].focus()
    }
  }

  const handleCodeSubmit = (event) => {
    event.preventDefault()
    const code = codeDigits.join('')
    if (code.length !== 6) {
      setApiFeedback('error', 'Please enter all 6 digits')
      return
    }
    const userId = getCookie('userId')
    if (!userId) {
      setApiFeedback('error', 'Session missing. Please register again.')
      return
    }
    setIsSubmitting(true)
    setApiFeedback('info', 'Verifying code...')
    apiClient
      .post('/auth/public/verify-code', { id: userId, code })
      .then((response) => {
        const accessToken = response?.data?.AccessToken || response?.data?.accessToken
        const requestToken = response?.data?.requestToken
        if (accessToken) {
          document.cookie = `AccessToken=${accessToken}; path=/; max-age=604800; SameSite=Lax`
          localStorage.setItem('AccessToken', accessToken)
        }
        if (requestToken) {
          document.cookie = `requestToken=${requestToken}; path=/; max-age=604800; SameSite=Lax`
        }
        localStorage.setItem('sessionActive', 'true')
        localStorage.setItem('profileCompleted', 'false')
        setPostAuthRedirect('/profile-setup')
        setAuthStage('welcome')
        setApiFeedback('success', 'Welcome to ThatOtakuNetwork! Finish your profile to continue.')
        navigate('/profile-setup', { replace: true })
      })
      .catch(async (error) => {
        const body = error?.response?.data
        try {
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          setApiFeedback('error', parsed.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Verification failed. Please try again.'))
        } catch (err) {
          const message = body?.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Verification failed. Please try again.')
          setApiFeedback('error', message)
        }
      })
      .finally(() => setIsSubmitting(false))
  }

  useEffect(() => {
    document.documentElement.style.setProperty('--accent-strong', accent.strong)
  }, [accent.strong])

  useEffect(() => {
    if (location?.state?.fromLogout) {
      setWallLoading(false)
      setSplashTimerDone(true)
      return
    }
    const timer = setTimeout(() => setSplashTimerDone(true), 6000)
    return () => clearTimeout(timer)
  }, [location?.state?.fromLogout])

  useEffect(() => {
    if (typeof window === 'undefined') return
    const stored = localStorage.getItem('cookieConsent')
    if (stored === 'allowed') {
      setCookiesAllowed(true)
    }
  }, [])

  const showLoading = wallLoading || !splashTimerDone
  const getCookie = (name) => {
    const match = typeof document !== 'undefined' ? document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`)) : null
    return match ? decodeURIComponent(match[1]) : ''
  }

  useEffect(() => {
    const token = getCookie('AccessToken') || localStorage.getItem('AccessToken')
    if (token) {
      navigate('/', { replace: true })
    }
  }, [navigate])

  useEffect(() => {
    if (authStage === 'welcome' && (getCookie('AccessToken') || localStorage.getItem('AccessToken'))) {
      navigate(postAuthRedirect, { replace: true })
    }
  }, [authStage, navigate, postAuthRedirect])

  return (
    <div
      className="relative min-h-screen overflow-hidden text-slate-50"
      style={{
        backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})`,
      }}
    >
      <CookieConsentGate
        show={!cookiesAllowed}
        accent={{ ...accent, key: accentKey }}
        onAllow={handleAllowCookies}
        onReject={handleRejectCookies}
        message={cookiePromptMessage}
      />
      <style>{`
        ::selection { background: var(--accent-strong, ${accent.strong}); color: #f8fafc; }
        input::selection, textarea::selection { background: var(--accent-strong, ${accent.strong}); color: #f8fafc; }
      `}</style>
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />

      <div className={`relative flex min-h-screen w-full max-w-none flex-col items-center justify-center gap-8 px-0 sm:px-0 lg:flex-row lg:items-stretch lg:justify-between lg:gap-0 lg:px-0 ${showLoading ? 'opacity-0 pointer-events-none' : 'opacity-100 transition-opacity duration-500'}`}>
        <div className="flex w-full max-w-xl flex-col items-center justify-center px-4 py-10 sm:px-8 lg:w-2/5 lg:items-start lg:justify-center lg:px-10 xl:px-14">
          <AuthCard
            mode={mode}
            setMode={setMode}
            motionSafe={motionSafe}
            accent={{ ...accent, key: accentKey }}
            onAccentChange={setAccentKey}
            authStage={authStage}
            formValues={formValues}
            onFormChange={handleFormChange}
            onLoginSubmit={handleLoginSubmit}
            onRegisterSubmit={handleRegisterSubmit}
            onCodeSubmit={handleCodeSubmit}
            onCodeChange={handleCodeChange}
            onCodeKeyDown={handleCodeKeyDown}
            codeDigits={codeDigits}
            codeRefs={codeRefs}
            formErrors={formErrors}
            apiMessage={apiMessage}
            apiStatus={apiStatus}
            isSubmitting={isSubmitting}
            passwordVisibility={passwordVisibility}
            onTogglePassword={togglePasswordVisibility}
          />
        </div>

        <div className="hidden w-full items-stretch lg:flex lg:w-[62%] relative">
          <Suspense fallback={<WallSkeleton />}>
            <AnimeWall reduceMotion={reduceMotion} isLowPower={isLowPower} onLoadingChange={setWallLoading} />
          </Suspense>
        </div>
      </div>

      <AnimatePresence mode="wait">
        {showLoading && <LoadingScreen key="loader" accent={accent} />}
      </AnimatePresence>
    </div>
  )
}

export default Authenticate
