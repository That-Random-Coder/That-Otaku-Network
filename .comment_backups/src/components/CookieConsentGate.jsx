import { motion } from 'framer-motion'
import AlertBanner from './AlertBanner.jsx'

function CookieConsentGate({ show, accent, onAllow, onReject, message }) {
  if (!show) return null
  return (
    <motion.div
      className="fixed inset-0 z-[60] flex items-center justify-center px-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.25, ease: 'easeOut' }}
    >
      <div className="absolute inset-0 bg-black/70 backdrop-blur" />
      <motion.div
        className="relative w-full max-w-lg rounded-3xl border border-white/10 bg-white/5 p-8 shadow-2xl"
        style={{ boxShadow: `0 25px 70px rgba(0,0,0,0.5), 0 0 32px ${accent.glow}`, backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}
        initial={{ y: 18, opacity: 0.9 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ type: 'spring', stiffness: 220, damping: 22 }}
      >
        <div className="flex items-center gap-3 text-white">
          <span className="text-2xl" aria-hidden>🍪</span>
          <div>
            <h2 className="text-xl font-semibold">Cookies required</h2>
            <p className="text-sm text-slate-200/90">We use cookies to keep you signed in and secure your account. Please allow cookies to continue.</p>
          </div>
        </div>
        {message && (
          <div className="mt-3">
            <AlertBanner status="error" message={message} />
          </div>
        )}
        <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onReject}
            className="w-full sm:w-auto rounded-2xl border border-white/15 px-4 py-2 text-sm font-semibold text-white/90 bg-white/10 hover:bg-white/15 focus:outline-none focus:ring-2 focus:ring-white/30"
          >
            I do not agree
          </button>
          <button
            type="button"
            onClick={onAllow}
            className="w-full sm:w-auto rounded-2xl px-5 py-2 text-sm font-semibold text-white shadow-lg focus:outline-none focus:ring-2"
            style={{
              backgroundImage: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`,
              boxShadow: `0 10px 30px ${accent.glow}`,
            }}
          >
            Allow cookies
          </button>
        </div>
      </motion.div>
    </motion.div>
  )
}

export default CookieConsentGate
