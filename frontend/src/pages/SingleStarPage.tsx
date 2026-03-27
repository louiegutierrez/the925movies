import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowLeft, User, Film, Star as StarIcon, Calendar, Loader2 } from 'lucide-react'
import { getStar } from '@/api/stars'
import type { Star } from '@/api/types'
import { Button } from '@/components/ui/button'
import { useCart } from '@/hooks/useCart'
import { cn } from '@/lib/utils'

function ratingColor(rating: number): string {
  if (rating >= 8) return 'text-emerald-400'
  if (rating >= 6) return 'text-amber-400'
  return 'text-red-400'
}

export default function SingleStarPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToCart } = useCart()
  const [star, setStar] = useState<Star | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    setIsLoading(true)
    getStar(id)
      .then(setStar)
      .catch(() => setStar(null))
      .finally(() => setIsLoading(false))
  }, [id])

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="h-10 w-10 animate-spin text-[var(--color-muted-foreground)]" />
      </div>
    )
  }

  if (!star) {
    return (
      <div className="text-center py-32">
        <p className="text-xl font-semibold text-[var(--color-muted-foreground)]">Star not found</p>
        <Button variant="ghost" className="mt-4" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-2" /> Go back
        </Button>
      </div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="max-w-4xl mx-auto space-y-8"
    >
      {/* Back */}
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" /> Back
      </Button>

      {/* Hero */}
      <div className="bg-[var(--color-card)] border border-[var(--color-border)] rounded-2xl p-8 flex flex-col sm:flex-row items-center sm:items-start gap-6">
        <div className="w-24 h-24 rounded-full bg-gradient-to-br from-[var(--color-secondary)] to-[var(--color-accent)] flex items-center justify-center shrink-0">
          <User className="h-12 w-12 text-[var(--color-muted-foreground)]/50" />
        </div>
        <div className="space-y-3 text-center sm:text-left">
          <h1 className="text-3xl font-bold text-[var(--color-foreground)]">{star.name}</h1>
          {star.birthYear && (
            <div className="flex items-center justify-center sm:justify-start gap-2 text-sm text-[var(--color-muted-foreground)]">
              <Calendar className="h-4 w-4" />
              <span>Born {star.birthYear}</span>
            </div>
          )}
          {star.movies && (
            <p className="text-sm text-[var(--color-muted-foreground)]">
              {star.movies.length} {star.movies.length === 1 ? 'film' : 'films'}
            </p>
          )}
        </div>
      </div>

      {/* Filmography */}
      {star.movies && star.movies.length > 0 && (
        <section>
          <div className="flex items-center gap-2 mb-4">
            <Film className="h-5 w-5 text-[var(--color-brand)]" />
            <h2 className="text-xl font-semibold">Filmography</h2>
          </div>
          <div className="space-y-2">
            {star.movies.map((movie) => (
              <div
                key={movie.id}
                className="flex items-center justify-between gap-4 p-4 rounded-xl border border-[var(--color-border)] bg-[var(--color-card)] hover:border-[var(--color-brand)]/40 transition-colors group"
              >
                <div className="flex items-center gap-4 min-w-0">
                  <div className="text-sm text-[var(--color-muted-foreground)] w-12 shrink-0 text-right font-mono">
                    {movie.year}
                  </div>
                  <div className="min-w-0">
                    <Link
                      to={`/movie/${movie.id}`}
                      className="font-medium text-[var(--color-foreground)] group-hover:text-[var(--color-brand)] transition-colors truncate block"
                    >
                      {movie.title}
                    </Link>
                    {movie.director && (
                      <p className="text-xs text-[var(--color-muted-foreground)] mt-0.5">
                        dir. {movie.director}
                      </p>
                    )}
                  </div>
                </div>
                <div className="flex items-center gap-3 shrink-0">
                  <div className={cn('flex items-center gap-1 text-sm font-semibold', ratingColor(movie.rating))}>
                    <StarIcon className="h-3.5 w-3.5 fill-current" />
                    <span>{movie.rating.toFixed(1)}</span>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 text-xs gap-1 hidden sm:flex"
                    onClick={() => void addToCart(movie.id, movie.title)}
                  >
                    Add to Cart
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </motion.div>
  )
}
