import { motion } from 'framer-motion'

const LoadingScreen = ({ accent, message }) => {
  const headline = message || 'Loading your anime realm'
  const subline = message === 'Logging out' ? 'Ending session and clearing traces.' : 'Summoning otaku\'s, calibrating grids, syncing feed.'
  const badge = message === 'Logging out' ? 'Processing logout' : 'Booting The Network'

  return (
  <motion.div
    className="fixed inset-0 z-50 flex min-h-screen w-full items-center justify-center overflow-hidden bg-[#050912] px-6"
    initial={{ opacity: 0, scale: 1.02 }}
    animate={{ opacity: 1, scale: 1 }}
    exit={{ opacity: 0, scale: 0.98 }}
    transition={{ duration: 0.55, ease: 'easeInOut' }}
  >
    <div
      className="absolute inset-0 opacity-80"
      style={{
        backgroundImage: `radial-gradient(circle at 18% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 82% 18%, rgba(255,255,255,0.08), transparent 42%), linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})`,
      }}
    />
    <motion.div
      className="absolute inset-0"
      style={{
        backgroundImage: 'linear-gradient(rgba(255,255,255,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.06) 1px, transparent 1px)',
        backgroundSize: '120px 120px, 120px 120px',
        maskImage: 'radial-gradient(circle at 50% 50%, rgba(0,0,0,0.6), transparent 70%)',
      }}
      animate={{ opacity: [0.25, 0.4, 0.25] }}
      transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
    />
    <motion.div
      className="absolute inset-0"
      style={{
        background: `linear-gradient(115deg, transparent 0%, transparent 45%, rgba(255,255,255,0.06) 50%, transparent 55%, transparent 100%)`,
      }}
      animate={{ backgroundPosition: ['0% 0%', '120% 120%'] }}
      transition={{ duration: 2.8, repeat: Infinity, ease: 'linear' }}
    />
    <div className="absolute inset-0 blur-3xl" style={{ background: `radial-gradient(circle at 50% 50%, ${accent.glow}, transparent 45%)` }} />

    <motion.div
      className="relative flex w-full max-w-3xl flex-col items-center gap-8 rounded-3xl border border-white/10 bg-white/5 p-10 backdrop-blur-2xl"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: 'easeOut' }}
      style={{ boxShadow: `0 30px 80px rgba(0,0,0,0.45), 0 0 45px ${accent.glow}` }}
    >
      <div className="relative flex items-center justify-center">
        <motion.div
          className="relative flex h-64 w-64 items-center justify-center rounded-full bg-white/5"
          animate={{ rotate: 360 }}
          transition={{ duration: 10, repeat: Infinity, ease: 'linear' }}
          style={{ boxShadow: `0 0 50px ${accent.glow}` }}
        >
          <motion.div
            className="absolute inset-6 rounded-full border border-white/20"
            animate={{ scale: [1, 1.08, 1], opacity: [0.6, 1, 0.6] }}
            transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
          />
          <motion.div
            className="absolute inset-10 rounded-full border-2 border-white/15"
            animate={{ scale: [1, 1.15, 1], opacity: [0.7, 1, 0.7] }}
            transition={{ duration: 2.8, repeat: Infinity, ease: 'easeInOut' }}
          />
          <motion.div
            className="absolute h-10 w-10 rounded-full"
            style={{ background: `linear-gradient(135deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 0 40px ${accent.glow}` }}
            animate={{ scale: [1, 1.2, 1], opacity: [0.9, 1, 0.9] }}
            transition={{ duration: 1.8, repeat: Infinity, ease: 'easeInOut' }}
          />
        </motion.div>
        <motion.div
          className="absolute left-1/2 top-28 -translate-x-1/2 flex flex-col gap-2 text-sm text-slate-100/90 lg:left-auto lg:right-8 lg:top-2 lg:translate-x-0"
          animate={{ y: [-6, 6, -6] }}
          transition={{ duration: 3.4, repeat: Infinity, ease: 'easeInOut' }}
        >
          <div className="rounded-full bg-white/10 px-3 py-1">Boosting your Nen...</div>
          <div className="rounded-full bg-white/10 px-3 py-1">Setting up the Dattebayo!...</div>
          <div className="rounded-full bg-white/10 px-3 py-1">Syncing the Devil fruits...</div>
        </motion.div>
      </div>

      <div className="flex w-full flex-col gap-3">
        {[0, 1, 2].map((row) => (
          <motion.div
            key={row}
            className="h-3 w-full overflow-hidden rounded-full bg-white/10"
          >
            <motion.div
              className="h-full rounded-full"
              style={{ background: `linear-gradient(90deg, ${accent.mid}, ${accent.strong})`, boxShadow: `0 0 20px ${accent.glow}` }}
              animate={{ x: ['-40%', '110%'] }}
              transition={{ duration: 1.6 + row * 0.2, repeat: Infinity, ease: 'easeInOut' }}
            />
          </motion.div>
        ))}
      </div>

      <div className="flex flex-col items-center gap-1 text-center">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-200/70">{badge}</p>
        <p className="text-2xl font-semibold text-white">{headline}</p>
        <p className="text-sm text-slate-300/80">{subline}</p>
      </div>
    </motion.div>
  </motion.div>
  )
}

export default LoadingScreen
