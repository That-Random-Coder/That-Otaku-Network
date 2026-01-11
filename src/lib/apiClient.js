import axios from 'axios'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:11111/api',
  timeout: 12000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Helper to read and write cookies locally
const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const setCookie = (name, value, maxAgeSeconds) => {
  if (typeof document === 'undefined') return
  const parts = [`${name}=${encodeURIComponent(String(value))}`, 'path=/', `SameSite=Lax`]
  if (typeof maxAgeSeconds === 'number') parts.push(`max-age=${Number(maxAgeSeconds)}`)
  document.cookie = parts.join('; ')
}

// IST offset (UTC+5:30) in milliseconds
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000 // 19,800,000
let isRefreshing = false
let refreshPromise = null

const refreshAccessToken = async () => {
  if (isRefreshing) {
    console.info('[apiClient] Token refresh already in progress, returning pending promise')
    return refreshPromise
  }
  isRefreshing = true
  refreshPromise = (async () => {
    try {
      const id = getCookie('userId') || getCookie('id') || ''
      const refreshToken = getCookie('RefreshToken') || getCookie('refreshToken') || ''
      if (!id || !refreshToken) throw new Error('Missing id or refresh token')
      const url = `${apiClient.defaults.baseURL || ''}/auth/public/access/token`
      console.info('[apiClient] Refreshing access token', { url, id, time: new Date().toISOString() })
      let res
      try {
        res = await axios.post(url, { id, token: refreshToken })
        console.info('[apiClient] Token refresh response', res?.data)
      } catch (err) {
        console.error('[apiClient] Token refresh failed', err?.response?.data || err?.message || err)
        throw err
      }

      const token = res?.data?.token || res?.data?.accessToken || ''
      const expireAt = res?.data?.expireAt || res?.data?.expiresAt || res?.data?.timestamp || ''

      // Normalize expireAt into milliseconds-since-epoch when possible
      let expireMs = null
      if (expireAt) {
        if (typeof expireAt === 'number') {
          expireMs = Number(expireAt)
          if (expireMs < 1e12) expireMs = expireMs * 1000 // likely seconds -> convert to ms
        } else {
          const asNum = Number(expireAt)
          if (!Number.isNaN(asNum)) {
            expireMs = asNum
            if (expireMs < 1e12) expireMs = expireMs * 1000
          } else {
            const parsed = Date.parse(String(expireAt))
            if (!Number.isNaN(parsed)) expireMs = parsed
          }
        }
      }

      if (!token) {
        console.error('[apiClient] Token refresh response missing token', res?.data)
        throw new Error('Token refresh returned no token')
      }

      // Save AccessToken and TimeStampAccessToken (ms since epoch where possible)
      setCookie('AccessToken', token, 7 * 24 * 3600)
      try { localStorage.setItem('AccessToken', String(token)) } catch {}
      console.info('[apiClient] New AccessToken saved (first 8 chars):', String(token).slice(0, 8) + '...')

      if (expireMs) {
        // Convert expireMs (UTC ms since epoch) into IST ms-epoch before saving
        const expireMsIST = expireMs + IST_OFFSET_MS
        // set max-age to remaining lifetime (minimum 60s)
        const remaining = Math.max(60, Math.floor((expireMsIST - Date.now()) / 1000))
        setCookie('TimeStampAccessToken', String(expireMsIST), remaining)
        console.info('[apiClient] TimeStampAccessToken (IST ms) set to:', expireMsIST, 'originalUTC(ms):', expireMs, 'remainingSeconds:', remaining)
      } else if (expireAt) {
        // If we only have a raw expireAt string, save it as-is (convert to IST if parseable)
        const parsed = Date.parse(String(expireAt))
        if (!Number.isNaN(parsed)) {
          const expireMsIST = parsed + IST_OFFSET_MS
          const remaining = Math.max(60, Math.floor((expireMsIST - Date.now()) / 1000))
          setCookie('TimeStampAccessToken', String(expireMsIST), remaining)
          console.info('[apiClient] TimeStampAccessToken (from raw, IST ms):', expireMsIST, 'originalRaw:', expireAt)
        } else {
          setCookie('TimeStampAccessToken', String(expireAt), 7 * 24 * 3600)
          console.info('[apiClient] TimeStampAccessToken set (raw):', expireAt)
        }
      }

      // Notify app that the access token was refreshed so components can reload state
      try {
        if (typeof window !== 'undefined' && window && typeof window.dispatchEvent === 'function') {
          try {
            window.dispatchEvent(new CustomEvent('accessToken:refreshed', { detail: { token, expireAt: expireMs || expireAt } }))
            console.info('[apiClient] Dispatched accessToken:refreshed event')
          } catch (e) {
            // ignore dispatch failure
          }
        }
      } catch (e) {
        // ignore
      }

      return token
    } finally {
      isRefreshing = false
      refreshPromise = null
    }
  })()
  return refreshPromise
}

// Request interceptor: refresh token if access timestamp expired and attach Authorization header
apiClient.interceptors.request.use(async (config) => {
  try {
    let tsRaw = getCookie('TimeStampAccessToken') || 0
    let ts = Number(tsRaw) || 0
    // If TimeStampAccessToken stored is in IST ms (we write IST ms), compare using an IST-aware clock
    const nowIst = Date.now() + IST_OFFSET_MS
    if (ts && nowIst >= ts) {
      // Attempt refresh
      try {
        await refreshAccessToken()
      } catch (e) {
        // Ignore - let request proceed; backend will respond 401 if still unauthorized
      }
    }

    const access = getCookie('AccessToken') || localStorage.getItem('AccessToken') || ''
    const token = access.trim()
    if (token && !config.headers?.Authorization) {
      config.headers = config.headers || {}
      config.headers.Authorization = token.toLowerCase().startsWith('bearer ') ? token : `Bearer ${token}`
    }
  } catch (e) {
    // silence.
  }
  return config
}, (error) => Promise.reject(error))

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.code === 'ECONNABORTED') {
      return Promise.reject(new Error('Request timed out. Please retry.'))
    }
    if (!navigator.onLine) {
      return Promise.reject(new Error('You appear to be offline. Reconnect and try again.'))
    }
    return Promise.reject(error)
  },
)

export default apiClient

// Export helper for other modules to trigger a refresh when needed
export { refreshAccessToken }
