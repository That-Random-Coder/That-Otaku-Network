import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X } from 'lucide-react'
import QRCodeStyling from 'qr-code-styling'
import AlertBanner from '../components/AlertBanner.jsx'

const getCookie = (name) => {
  if (typeof document === 'undefined') return ''
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : ''
}

const QRCodeShareModal = ({ isOpen, onClose, accent, motionSafe, targetId, targetType }) => {
  const wrapperRef = useRef(null)
  const qrRef = useRef(null)
  const qrInstanceRef = useRef(null)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!isOpen) return

    const cookieUserId = getCookie('userId') || getCookie('id')
    const effectiveId = targetId || cookieUserId
    const basePath = targetType === 'group' ? '/group/' : '/friend-profile/'
    const url = `http://localhost:5173${basePath}${encodeURIComponent(effectiveId || '')}`

    const color = accent?.mid || '#7c3aed'

    // create a small SVG to embed text in the center of the QR
    // Create a white SVG center with slate-colored text (larger) for contrast inside the QR
    // Increase text size but keep overall QR size unchanged; use accent color for center text
    const svgTextColor = accent?.mid || color
    const svg = `
      <svg xmlns='http://www.w3.org/2000/svg' width='420' height='140' viewBox='0 0 420 140'>
        <rect x='0' y='0' width='420' height='140' rx='16' fill='#ffffff'/>
        <text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' font-family='Inter, system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial' font-size='34' fill='${svgTextColor}' font-weight='800'>ThatOtakuNetwork</text>
      </svg>`

    const svgDataUrl = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`

    // Build gradient stops from the accent colors
    const gradientStops = [
      { offset: 0, color: accent?.start || color },
      { offset: 0.5, color: accent?.mid || color },
      { offset: 1, color: accent?.end || color },
    ]

    // create QR instance with white background and accent gradient for dots and accent-colored corners
    qrInstanceRef.current = new QRCodeStyling({
      width: 380,
      height: 380,
      data: url,
      image: svgDataUrl,
      dotsOptions: {
        type: 'rounded',
        gradient: {
          type: 'linear',
          rotation: 0,
          colorStops: gradientStops,
        },
      },
      cornersSquareOptions: {
        type: 'extra-rounded',
        color: accent?.mid || color,
      },
      cornersDotOptions: {
        type: 'dot',
        color: accent?.mid || color,
      },
      backgroundOptions: {
        color: '#ffffff',
      },
      imageOptions: {
        crossOrigin: 'anonymous',
        margin: 0,
        imageSize: 0.36,
      },
    })

    // append to the container
    if (qrRef.current) {
      qrRef.current.innerHTML = ''
      qrInstanceRef.current.append(qrRef.current)
    }

    return () => {
      // cleanup
      if (qrRef.current) qrRef.current.innerHTML = ''
      qrInstanceRef.current = null
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, accent])

  const handleDownload = async () => {
    try {
      setError('')
      await qrInstanceRef.current?.download({ extension: 'png' })
    } catch (e) {
      console.error('Failed to download QR', e)
      setError(e?.message || 'Failed to download QR. Please try again.')
      setTimeout(() => setError(''), 3500)
    }
  }

  if (!isOpen) return null

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-md p-4"
        onClick={onClose}
      >
        <motion.div
          initial={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : false}
          animate={motionSafe ? { opacity: 1, scale: 1, y: 0 } : { opacity: 1 }}
          exit={motionSafe ? { opacity: 0, scale: 0.95, y: 18 } : { opacity: 0 }}
          transition={{ type: 'spring', stiffness: 280, damping: 24 }}
          className="relative w-full max-w-2xl h-auto overflow-hidden rounded-3xl border backdrop-blur-3xl flex flex-col"
          style={{
            borderColor: 'rgba(255,255,255,0.08)',
            backgroundColor: accent?.surface || 'rgba(0,0,0,0.5)',
            boxShadow: `0 40px 100px rgba(0,0,0,0.6), 0 0 50px ${accent?.glow || 'transparent'}`,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="pointer-events-none absolute -left-24 -top-24 h-72 w-72 rounded-full bg-purple-500/20 blur-3xl" />
          <div className="pointer-events-none absolute -bottom-24 -right-24 h-80 w-80 rounded-full bg-indigo-500/25 blur-3xl" />

          {/* Header */}
          <div className="relative p-6 border-b border-white/10">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">Share Profile</h2>
                <p className="text-sm text-slate-300/80 mt-1">Scan the QR to share</p>
              </div>
              <button onClick={onClose} className="rounded-full p-2 text-slate-300 hover:bg-white/10 hover:text-white transition">
                <X className="h-6 w-6" />
              </button>
            </div>
            {error && (
              <div className="mt-3 px-2">
                <AlertBanner status="error" message={error} />
              </div>
            )}
          </div>

          {/* QR Area */}
          <div className="relative flex-1 p-6 flex flex-col items-center gap-6">
            <div className="relative rounded-3xl p-1" style={{ background: `linear-gradient(135deg, ${accent.start}, ${accent.end})` }}>
              <div className="rounded-2xl bg-white p-6 flex items-center justify-center" style={{ boxShadow: `0 30px 80px rgba(0,0,0,0.45), 0 0 30px ${accent.glow}` }}>
                <div ref={qrRef} className="flex items-center justify-center" />
              </div>
            </div>

            <div className="text-sm text-slate-300/80">Scan the QR to share</div>

            <div className="flex gap-3">
              <button onClick={handleDownload} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10">Download</button>
              <button onClick={onClose} className="rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-white/10">Close</button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}

export default QRCodeShareModal
