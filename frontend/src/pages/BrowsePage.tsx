import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Tag, AlignLeft, Loader2 } from 'lucide-react'
import { getGenres } from '@/api/movies'
import type { Genre } from '@/api/types'
import { cn } from '@/lib/utils'

const LETTERS = '#ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

const GENRE_COLORS = [
  'from-violet-900/40 to-violet-800/20 border-violet-700/30',
  'from-blue-900/40 to-blue-800/20 border-blue-700/30',
  'from-emerald-900/40 to-emerald-800/20 border-emerald-700/30',
  'from-amber-900/40 to-amber-800/20 border-amber-700/30',
  'from-rose-900/40 to-rose-800/20 border-rose-700/30',
  'from-cyan-900/40 to-cyan-800/20 border-cyan-700/30',
  'from-purple-900/40 to-purple-800/20 border-purple-700/30',
  'from-teal-900/40 to-teal-800/20 border-teal-700/30',
]

const container = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.04 },
  },
}

const item = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3 } },
}

export default function BrowsePage() {
  const navigate = useNavigate()
  const [genres, setGenres] = useState<Genre[]>([])
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    getGenres()
      .then(setGenres)
      .catch(() => setGenres([]))
      .finally(() => setIsLoading(false))
  }, [])

  return (
    <div className="space-y-12">
      {/* Hero */}
      <div className="text-center py-8">
        <h1 className="text-4xl font-bold text-[var(--color-foreground)] mb-3">
          Discover Movies
        </h1>
        <p className="text-[var(--color-muted-foreground)] text-lg max-w-xl mx-auto">
          Browse by genre or find titles by their first letter
        </p>
      </div>

      {/* Browse by Genre */}
      <section>
        <div className="flex items-center gap-2 mb-6">
          <Tag className="h-5 w-5 text-[var(--color-brand)]" />
          <h2 className="text-xl font-semibold text-[var(--color-foreground)]">Browse by Genre</h2>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-16">
            <Loader2 className="h-8 w-8 animate-spin text-[var(--color-muted-foreground)]" />
          </div>
        ) : (
          <motion.div
            variants={container}
            initial="hidden"
            animate="show"
            className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3"
          >
            {genres.map((genre, idx) => (
              <motion.button
                key={genre.id}
                variants={item}
                onClick={() => navigate(`/movies?genre=${encodeURIComponent(genre.name)}`)}
                className={cn(
                  'group relative overflow-hidden rounded-xl border bg-gradient-to-br p-4 text-left transition-all hover:scale-[1.03] hover:shadow-lg hover:shadow-black/30 active:scale-[0.98]',
                  GENRE_COLORS[idx % GENRE_COLORS.length],
                )}
              >
                <div className="absolute inset-0 bg-white/0 group-hover:bg-white/5 transition-colors" />
                <span className="relative text-sm font-semibold text-[var(--color-foreground)]">
                  {genre.name}
                </span>
              </motion.button>
            ))}
          </motion.div>
        )}
      </section>

      {/* Browse by Letter */}
      <section>
        <div className="flex items-center gap-2 mb-6">
          <AlignLeft className="h-5 w-5 text-[var(--color-brand)]" />
          <h2 className="text-xl font-semibold text-[var(--color-foreground)]">Browse by Title</h2>
        </div>

        <div className="flex flex-wrap gap-2">
          {LETTERS.map((letter) => (
            <button
              key={letter}
              onClick={() => navigate(`/movies?letter=${encodeURIComponent(letter)}`)}
              className="w-10 h-10 rounded-lg border border-[var(--color-border)] bg-[var(--color-card)] text-sm font-bold text-[var(--color-muted-foreground)] hover:bg-[var(--color-brand)] hover:text-white hover:border-[var(--color-brand)] transition-all hover:scale-105 active:scale-95"
            >
              {letter}
            </button>
          ))}
        </div>
      </section>
    </div>
  )
}
