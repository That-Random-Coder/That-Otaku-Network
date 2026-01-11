import accentOptions from './accentOptions.js'

const DEFAULT_ACCENT = 'crimson-night'

const isValidAccentKey = (key) => accentOptions.some((opt) => opt.key === key)

const readAccentFromCookie = () => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(/(?:^|; )accentKey=([^;]*)/)
  if (!match) return ''
  const value = decodeURIComponent(match[1])
  return isValidAccentKey(value) ? value : ''
}

export const getSavedAccentKey = (fallback = DEFAULT_ACCENT) => {
  const safeFallback = isValidAccentKey(fallback) ? fallback : DEFAULT_ACCENT
  if (typeof window === 'undefined') return safeFallback
  try {
    const stored = localStorage.getItem('accentKey')
    if (stored && isValidAccentKey(stored)) return stored
  } catch (_) {

  }
  const fromCookie = readAccentFromCookie()
  if (fromCookie) return fromCookie
  return safeFallback
}

export const persistAccentKey = (key) => {
  if (typeof window === 'undefined') return
  const nextKey = isValidAccentKey(key) ? key : DEFAULT_ACCENT
  try {
    localStorage.setItem('accentKey', nextKey)

    try { console.info('[accentStorage] persisted accentKey', nextKey, Date.now()) } catch (_) {}
  } catch (_) {

  }
  if (typeof document !== 'undefined') {
    document.cookie = `accentKey=${encodeURIComponent(nextKey)}; path=/; max-age=31536000; SameSite=Lax`
  }


  try {
    const ev = new CustomEvent('accent:changed', { detail: { key: nextKey } })
    window.dispatchEvent(ev)
    try { console.info('[accentStorage] dispatched accent:changed', nextKey, Date.now()) } catch (_) {}
  } catch (_) {

  }
}

export default { getSavedAccentKey, persistAccentKey }
