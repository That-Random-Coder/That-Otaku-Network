import { Suspense, lazy, useEffect, useMemo, useRef, useState } from 'react'
import AlertBanner from '../components/AlertBanner.jsx'
import FormField from '../components/FormField.jsx'
import PasswordField from '../components/PasswordField.jsx'
import CodeInputGroup from '../components/CodeInputGroup.jsx'
import WallSkeleton from '../components/WallSkeleton.jsx'
import accentOptions from '../theme/accentOptions.js'
import apiClient from '../lib/apiClient'

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

const ForgotPassword = () => {
  const [formValues, setFormValues] = useState({ email: '', password: '', confirmPassword: '' })
  const [codeDigits, setCodeDigits] = useState(Array(6).fill(''))
  const [formErrors, setFormErrors] = useState({})
  const [apiMessage, setApiMessage] = useState('')
  const [apiStatus, setApiStatus] = useState('info')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isSendingCode, setIsSendingCode] = useState(false)
  const [passwordVisibility, setPasswordVisibility] = useState({ reset: false, confirm: false })
  const codeRefs = useRef([])
  const accentKey = 'crimson-night'
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const reduceMotion = usePrefersReducedMotion()
  const isLowPower = useDeviceProfile()

  const togglePasswordVisibility = (key) => {
    setPasswordVisibility((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const handleFormChange = (event) => {
    const { name, value } = event.target
    setFormValues((prev) => ({ ...prev, [name]: value }))
  }

  const handleSendCode = async () => {
    const email = formValues.email.trim()
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!email || !emailRegex.test(email)) {
      setFormErrors((prev) => ({ ...prev, email: !email ? 'Email must not be blank' : 'Invalid email format' }))
      setApiStatus('error')
      setApiMessage('Please enter a valid email to receive the code.')
      return
    }
    setIsSendingCode(true)
    setApiStatus('info')
    setApiMessage('Sending code to your email...')
    try {
      await apiClient.post('/auth/public/password/send-code', { email })
      setApiStatus('success')
      setApiMessage('Code sent. Check your inbox for the 6-digit code.')
    } catch (error) {
      const body = error?.response?.data
      try {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        setApiStatus('error')
        setApiMessage(parsed.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Failed to send code. Please retry.'))
      } catch (err) {
        const message = body?.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Failed to send code. Please retry.')
        setApiStatus('error')
        setApiMessage(message)
      }
    } finally {
      setIsSendingCode(false)
    }
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

  const validate = () => {
    const errors = {}
    const email = formValues.email.trim()
    const password = formValues.password
    const confirmPassword = formValues.confirmPassword
    if (!email) errors.email = 'Email must not be blank'
    else {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
      if (!emailRegex.test(email)) errors.email = 'Invalid email format'
    }
    if (!password) errors.password = 'Password must not be blank'
    else if (password.length < 6 || password.length > 60) errors.password = 'Password length must be between 6 and 60 characters'
    if (confirmPassword && confirmPassword !== password) errors.confirmPassword = 'Passwords must match'
    if (codeDigits.join('').length !== 6) errors.code = 'Enter all 6 digits'
    return errors
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const errors = validate()
    setFormErrors(errors)
    if (Object.keys(errors).length > 0) {
      setApiStatus('error')
      setApiMessage('Please resolve the highlighted fields.')
      return
    }
    setApiStatus('info')
    setApiMessage('Submitting reset request...')
    setIsSubmitting(true)
    try {
      await apiClient.post(import.meta.env.VITE_API_BASE_URL + 'auth/public/password/reset', {
        email: formValues.email.trim(),
        code: codeDigits.join(''),
        password: formValues.password,
      })
      setApiStatus('success')
      setApiMessage('Password reset successfully. You can now sign in.')
    } catch (error) {
      const body = error?.response?.data
      try {
        const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
        setApiStatus('error')
        setApiMessage(parsed.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Reset failed. Please retry.'))
      } catch (err) {
        const message = body?.message || (!navigator.onLine ? 'You appear to be offline. Reconnect and try again.' : 'Reset failed. Please retry.')
        setApiStatus('error')
        setApiMessage(message)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div
      className="relative min-h-screen overflow-hidden text-slate-50"
      style={{
        backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})`,
      }}
    >
      <style>{`
        ::selection { background: ${accent.strong}; color: #f8fafc; }
        input::selection, textarea::selection { background: ${accent.strong}; color: #f8fafc; }
      `}</style>
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />

      <div className="relative flex min-h-screen w-full flex-col items-center justify-center gap-8 px-0 sm:px-0 lg:flex-row lg:items-stretch lg:justify-between lg:gap-0 lg:px-0">
        <div className="flex w-full max-w-xl flex-col items-center justify-center px-4 py-10 sm:px-8 lg:w-2/5 lg:items-start lg:justify-center lg:px-10 xl:px-14">
          <div className="relative w-full overflow-hidden rounded-3xl border backdrop-blur-3xl" style={{ borderColor: 'rgba(255,255,255,0.08)', backgroundColor: accent.surface, boxShadow: `0 30px 90px rgba(0,0,0,0.45), 0 0 35px ${accent.glow}` }}>
            <div className="relative p-8 sm:p-10 space-y-6">
              <div className="flex flex-col gap-1 text-left">
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">ThatOtakuNetwork</p>
                <h1 className="mt-1 text-3xl font-semibold text-white">Reset Password</h1>
                <p className="text-sm text-slate-300">Send a 6-digit code to your email, then set a new password.</p>
              </div>

              <AlertBanner status={apiStatus} message={apiMessage} />

              <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                  <FormField
                    id="email"
                    label="Email"
                    type="email"
                    value={formValues.email}
                    autoComplete="email"
                    onChange={handleFormChange}
                  />
                  {formErrors.email && <p className="text-xs text-rose-300/90">{formErrors.email}</p>}
                  <button
                    type="button"
                    onClick={handleSendCode}
                    disabled={isSendingCode}
                    className="w-full rounded-2xl border border-white/10 px-5 py-3 text-sm font-semibold text-white shadow-lg focus:outline-none focus:ring-2 disabled:opacity-60"
                    style={{
                      backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
                      boxShadow: `0 10px 30px ${accent.glow}`,
                    }}
                  >
                    {isSendingCode ? 'Sending code...' : 'Send code to email'}
                  </button>
                </div>

                <div className="space-y-2">
                  <CodeInputGroup codeDigits={codeDigits} onChange={handleCodeChange} onKeyDown={handleCodeKeyDown} codeRefs={codeRefs} />
                  {formErrors.code && <p className="text-xs text-rose-300/90 text-center">{formErrors.code}</p>}
                </div>

                <PasswordField
                  id="password"
                  label="New Password"
                  value={formValues.password}
                  autoComplete="new-password"
                  onChange={handleFormChange}
                  visible={passwordVisibility.reset}
                  onToggle={() => togglePasswordVisibility('reset')}
                  ariaLabel={passwordVisibility.reset ? 'Hide password' : 'Show password'}
                />
                {formErrors.password && <p className="text-xs text-rose-300/90">{formErrors.password}</p>}

                <PasswordField
                  id="confirmPassword"
                  label="Confirm Password"
                  value={formValues.confirmPassword}
                  autoComplete="new-password"
                  onChange={handleFormChange}
                  visible={passwordVisibility.confirm}
                  onToggle={() => togglePasswordVisibility('confirm')}
                  ariaLabel={passwordVisibility.confirm ? 'Hide confirm password' : 'Show confirm password'}
                />
                {formErrors.confirmPassword && <p className="text-xs text-rose-300/90">{formErrors.confirmPassword}</p>}

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full rounded-2xl border border-white/10 px-5 py-3 text-base font-semibold text-white shadow-lg focus:outline-none focus:ring-2 disabled:opacity-60"
                  style={{
                    backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
                    boxShadow: `0 10px 30px ${accent.glow}`,
                  }}
                >
                  {isSubmitting ? 'Submitting...' : 'Reset password'}
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="hidden w-full items-stretch lg:flex lg:w-[62%] relative">
          <Suspense fallback={<WallSkeleton />}>
            <AnimeWall reduceMotion={reduceMotion} isLowPower={isLowPower} />
          </Suspense>
        </div>
      </div>
    </div>
  )
}

export default ForgotPassword
