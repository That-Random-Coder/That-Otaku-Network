const WallSkeleton = () => (
  <div className="relative h-full w-full overflow-hidden flex justify-start" aria-hidden>
    <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-[#161024]/85 via-[#0f0b1a]/88 to-[#05060d]" />
    <div className="relative w-full max-w-4xl mx-auto grid grid-cols-3 gap-4 p-3 pt-8 max-h-screen">
      {Array.from({ length: 3 }).map((_, colIdx) => (
        <div key={`skeleton-col-${colIdx}`} className="overflow-hidden rounded-3xl border border-white/10 bg-white/5 p-3 shadow-glow backdrop-blur-lg h-full max-h-[calc(100vh-3rem)]">
          <div className="flex flex-col gap-4">
            {Array.from({ length: 4 }).map((__, cardIdx) => (
              <div key={`skeleton-card-${colIdx}-${cardIdx}`} className="overflow-hidden rounded-2xl border border-white/10 bg-white/5 h-72 w-full max-w-[240px] mx-auto animate-pulse" />
            ))}
          </div>
        </div>
      ))}
    </div>
    <div className="pointer-events-none absolute inset-x-0 top-0 h-44 bg-gradient-to-b from-[#03040a]/95 via-[#03040a]/62 to-transparent z-10" />
    <div className="pointer-events-none absolute inset-x-0 bottom-0 h-44 bg-gradient-to-t from-[#03040a]/95 via-[#03040a]/62 to-transparent z-10" />
  </div>
)

export default WallSkeleton
