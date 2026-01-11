export function clearSession(navigate, setLoggingOut) {
  try {
    if (typeof setLoggingOut === 'function') setLoggingOut(true)
    if (typeof document !== 'undefined') {
      [
        'AccessToken',
        'accessToken',
        'requestToken',
        'request_token',
        'RefreshToken',
        'refreshToken',
        'userId',
        'id',
        'username',
        'friendId',
        'sessionActive',
        'profileCompleted',
        'cookieConsent',
      ].forEach((key) => {
        try { document.cookie = `${key}=; path=/; max-age=0; SameSite=Lax` } catch (e) {}
      })
    }

    if (typeof window !== 'undefined') {
      [
        'AccessToken',
        'accessToken',
        'requestToken',
        'RefreshToken',
        'sessionActive',
        'userId',
        'profileCompleted',
        'username',
      ].forEach((k) => {
        try { localStorage.removeItem(k) } catch (e) {}
      })
    }

    setTimeout(() => {
      try { navigate('/authenticate', { replace: true, state: { fromLogout: true } }) } catch (e) {}
    }, 400)
  } catch (e) {
    console.error('clearSession error', e)
  }
}
