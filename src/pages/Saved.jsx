import { useEffect, useMemo, useState } from 'react'
import accentOptions from '../theme/accentOptions.js'
import { getSavedAccentKey } from '../theme/accentStorage.js'
import NavigationBar from '../components/NavigationBar.jsx'
import { Bookmark, ImageIcon, Video, Music } from 'lucide-react'

const seedSaved = [
  { id: 's1', title: 'Top Animation Scenes', type: 'Article', description: 'A longform look at modern sakuga', image: '' },
  { id: 's2', title: 'Fan Art: Aki', type: 'Image', description: 'Stunning watercolor reinterpretations', image: '' },
  { id: 's3', title: 'AMV: Requiem', type: 'Video', description: 'Perfectly edited AMV for vibes', image: '' },
  { id: 's4', title: 'Soundtrack: Mood', type: 'Music', description: 'Chill soundtrack worth re-listening', image: '' },
]

function Saved() {
  const [accentKey] = useState(() => getSavedAccentKey('crimson-night'))
  const accent = useMemo(() => accentOptions.find((opt) => opt.key === accentKey)?.colors || accentOptions[0].colors, [accentKey])
  const [items] = useState(seedSaved)

  useEffect(() => {
    document.title = 'Saved - ThatOtakuNetwork'
  }, [])

  return (
    <div
      className="relative min-h-screen overflow-visible text-slate-50"
      style={{ backgroundImage: `linear-gradient(135deg, ${accent.start}, ${accent.mid}, ${accent.end})` }}
    >
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          backgroundImage: `radial-gradient(circle at 45% 20%, ${accent.glow}, transparent 38%), radial-gradient(circle at 78% 30%, rgba(255,255,255,0.06), transparent 30%), radial-gradient(circle at 55% 78%, rgba(255,255,255,0.04), transparent 33%)`,
          mixBlendMode: 'screen',
        }}
      />

      <div className="relative z-10 px-4 pb-20 pt-14 sm:px-8 lg:px-12 xl:px-16">
        <div className="md:grid md:grid-cols-[20rem_1fr] md:gap-8">
          <div className="hidden md:block">
            <NavigationBar accent={accent} variant="inline" />
          </div>
          <div className="min-w-0">
            <header className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <p className="text-xs uppercase tracking-[0.25em] text-slate-300/80">Your Library</p>
                <h1 className="mt-2 text-3xl font-semibold text-white">Saved</h1>
                <p className="mt-2 text-sm text-slate-200/85 max-w-2xl">Things you saved for later: articles, art, videos, and more.</p>
              </div>
              <div>
                <button className="rounded-full border border-white/15 bg-white/10 px-4 py-2 text-sm font-semibold text-white shadow transition hover:bg-white/20">Manage</button>
              </div>
            </header>

            <div className="mt-8 grid gap-6">
              {items.map((it) => (
                <div key={it.id} className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-[0_18px_60px_rgba(0,0,0,0.35)] backdrop-blur-2xl">
                  <div className="flex items-center justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div className="h-14 w-14 rounded-xl bg-white/8 flex items-center justify-center text-xl font-semibold text-white">{it.type.slice(0,1)}</div>
                      <div>
                        <h3 className="text-lg font-semibold text-white">{it.title}</h3>
                        <p className="text-sm text-slate-300/80">{it.description}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <button className="rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-sm font-semibold text-slate-200/85 hover:text-white">Open</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <NavigationBar accent={accent} variant="mobile" />
    </div>
  )
}

export default Saved