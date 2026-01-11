import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'
import Authenticate from './pages/Authenticate.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import Home from './pages/Home.jsx'
import Friends from './pages/Friends.jsx'
import Create from './pages/Create.jsx'
import Groups from './pages/Groups.jsx'
import Saved from './pages/Saved.jsx'
import Profile from './pages/Profile.jsx'
import ProfileSetup from './pages/ProfileSetup.jsx'
import FriendProfile from './pages/FriendProfile.jsx'
import GroupProfile from './pages/GroupProfile.jsx'
import Preferences from './pages/Preferences.jsx'
import Post from './pages/Post.jsx'

const hasSession = () => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return false
  const cookieToken = /(?:^|;\s*)AccessToken=/.test(document.cookie)
  const storedToken = Boolean(localStorage.getItem('AccessToken'))
  return cookieToken || storedToken
}

const profileCompleted = () => {
  if (typeof window === 'undefined') return false
  return localStorage.getItem('profileCompleted') === 'true'
}

function AppRoutes() {
  const location = useLocation()
  const [isAuthenticated, setIsAuthenticated] = useState(hasSession)
  const [isProfileReady, setIsProfileReady] = useState(profileCompleted())

  useEffect(() => {
    setIsAuthenticated(hasSession())
    setIsProfileReady(profileCompleted())
  }, [location.pathname])

  useEffect(() => {
    const syncAuth = () => {
      setIsAuthenticated(hasSession())
      setIsProfileReady(profileCompleted())
    }
    window.addEventListener('storage', syncAuth)
    const interval = setInterval(syncAuth, 1500)
    return () => {
      window.removeEventListener('storage', syncAuth)
      clearInterval(interval)
    }
  }, [])

  return (
    <Routes>
      {isAuthenticated ? (
        isProfileReady ? (
          <>
            <Route path='/' element={<Home />} />
            <Route path='/friends' element={<Friends />} />
            <Route path='/friend-profile/:friendId' element={<FriendProfile />} />
            <Route path='/group/:groupId' element={<GroupProfile />} />
            <Route path='/create' element={<Create />} />
            <Route path='/groups' element={<Groups />} />
            <Route path='/saved' element={<Saved />} />
            <Route path='/profile' element={<Profile />} />
            <Route path='/profile/:userId' element={<Profile />} />
            <Route path='/post/:postId' element={<Post />} />
            <Route path='/profile-setup' element={<ProfileSetup />} />
            <Route path='/authenticate' element={<Navigate to='/' replace />} />
            <Route path='/forgot-password' element={<Navigate to='/' replace />} />
            <Route path='*' element={<Navigate to='/' replace />} />
          </>
        ) : (
          <>
            <Route path='/profile-setup' element={<ProfileSetup />} />
            <Route path='/preferences' element={<Preferences />} />
            <Route path='*' element={<Navigate to='/profile-setup' replace />} />
          </>
        )
      ) : (
        <>
          <Route path='/forgot-password' element={<ForgotPassword />} />
          <Route path='/authenticate' element={<Authenticate />} />
          <Route path='*' element={<Navigate to='/authenticate' replace />} />
        </>
      )}
    </Routes>
  )
}

function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

export default App

