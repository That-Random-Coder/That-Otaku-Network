import { useEffect, useMemo, useRef, useState } from 'react'
import anime from 'animejs/lib/anime.es.js'
import AlertBanner from '../components/AlertBanner.jsx' 

const shuffleArray = (arr) => arr
  .map((item) => ({ value: item, sort: Math.random() }))
  .sort((a, b) => a.sort - b.sort)
  .map((item) => item.value)

function AnimeWall({ reduceMotion, isLowPower, onLoadingChange }) {
  const [animeList, setAnimeList] = useState([])
  const [error, setError] = useState('')
  const containerRef = useRef(null)
  const columnRefs = useRef([])
  const columnInnerRefs = useRef([])
  const timelinesRef = useRef([])
  const repeatCount = isLowPower ? 3 : 4

  useEffect(() => {
    const controller = new AbortController()

    const load = async () => {
      onLoadingChange?.(true)
      try {
        setError('')
        const response = await fetch('https://api.jikan.moe/v4/top/anime?limit=25', {
          signal: controller.signal,
        })
        if (!response.ok) {
          const body = await response.json().catch(() => ({}))
          const parsed = (await import('../lib/parseApiError.js')).parseApiErrorBody(body)
          throw new Error(parsed.message || `Failed to load anime: ${response.status}`)
        }
        const payload = await response.json()
        const limited = shuffleArray(payload?.data || [])
          .filter((item) => item?.images?.jpg?.large_image_url)
          .slice(0, isLowPower ? 12 : 18)
          .map((item) => ({
            id: item.mal_id,
            title: item.title,
            image:
              item.images?.jpg?.large_image_url ||
              item.images?.jpg?.image_url ||
              item.images?.jpg?.small_image_url,
          }))
        setAnimeList(limited)
      } catch (err) {
        if (err.name !== 'AbortError') {
          setAnimeList([])
          setError(err.message || 'Failed to load anime wall')
        }
      } finally {
        onLoadingChange?.(false)
      }
    }

    load()
    return () => controller.abort()
  }, [isLowPower, onLoadingChange])

  useEffect(() => {
    timelinesRef.current.forEach((timeline) => timeline.pause())
    timelinesRef.current = []

    const node = containerRef.current
    if (!node || reduceMotion || animeList.length === 0) return undefined

    const columns = columnRefs.current
      .map((col, idx) => ({ col, inner: columnInnerRefs.current[idx] }))
      .filter(({ col, inner }) => col && inner)

    columns.forEach(({ inner }, idx) => {
      const segmentHeight = inner.scrollHeight / repeatCount || 1
      const directionUp = idx % 2 === 0
      const bias = idx === 1 ? -segmentHeight * 1.15 : idx === 0 ? 0 : segmentHeight * 0.18
      const startOffset = directionUp ? -bias : bias
      const distance = directionUp ? -segmentHeight : segmentHeight
      const duration = (isLowPower ? 23000 : 34000) + idx * (isLowPower ? 2200 : 3000)

      anime.set(inner, { translateY: startOffset })

      const timeline = anime({
        targets: inner,
        translateY: [startOffset, startOffset + distance],
        duration,
        easing: 'linear',
        loop: true,
        autoplay: true,
      })

      timelinesRef.current.push(timeline)
    })

    return () => {
      timelinesRef.current.forEach((timeline) => timeline.pause())
      timelinesRef.current = []
    }
  }, [animeList, reduceMotion, isLowPower])

  const columns = useMemo(() => {
    const columnCount = 3
    const buckets = Array.from({ length: columnCount }, () => [])
    animeList.forEach((item, idx) => {
      buckets[idx % columnCount].push(item)
    })
    return buckets
  }, [animeList])

  return (
    <div className="relative h-full w-full overflow-hidden flex justify-start" ref={containerRef} aria-hidden>
      {error && (
        <div className="absolute inset-x-0 top-6 z-40 flex justify-center px-4">
          <AlertBanner status="error" message={error} />
        </div>
      )}
      <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-[#161024]/85 via-[#0f0b1a]/88 to-[#05060d]" />
      <div className="relative w-full max-w-4xl mx-auto grid grid-cols-3 gap-4 p-3 pt-8 max-h-screen">
        {columns.map((col, idx) => (
          <div
            key={`col-${idx}`}
            ref={(node) => {
              columnRefs.current[idx] = node
            }}
            className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 p-3 shadow-glow backdrop-blur-lg h-full max-h-[calc(100vh-3rem)]"
          >
            <div
              ref={(node) => {
                columnInnerRefs.current[idx] = node
              }}
              className="flex flex-col gap-4 will-change-transform"
            >
              {Array.from({ length: repeatCount }).flatMap((_, dupIdx) =>
                col.map((animeItem, innerIdx) => (
                  <div
                    key={`${animeItem.id}-${dupIdx}-${innerIdx}`}
                    className="overflow-hidden rounded-2xl border border-white/10 bg-white/5 h-72 w-full max-w-[240px] mx-auto"
                  >
                    <img
                      loading="lazy"
                      decoding="async"
                      fetchPriority="high"
                      src={animeItem.image}
                      alt={animeItem.title}
                      className="h-72 w-full object-cover"
                    />
                  </div>
                )),
              )}
            </div>
          </div>
        ))}
      </div>
      <div className="pointer-events-none absolute inset-x-0 top-0 h-44 bg-gradient-to-b from-[#03040a]/95 via-[#03040a]/62 to-transparent z-10" />
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-44 bg-gradient-to-t from-[#03040a]/95 via-[#03040a]/62 to-transparent z-10" />
    </div>
  )
}

export default AnimeWall
