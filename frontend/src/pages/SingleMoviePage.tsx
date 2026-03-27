import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Star, ArrowLeft, ShoppingCart, User, Tag, Calendar, Video, Loader2 } from 'lucide-react'
import { getMovie } from '@/api/movies'
import type { Movie } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { useCart } from '@/hooks/useCart'
import { cn } from '@/lib/utils'

function ratingColor(rating: number): string {
  if (rating >= 8) return 'text-emerald-400'
  if (rating >= 6) return 'text-amber-400'
  return 'text-red-400'
}

export default function SingleMoviePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { addToCart } = useCart()
  const [movie, setMovie] = useState<Movie | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    setIsLoading(true)
    getMovie(id)
      .then(setMovie)
      .catch(() => setMovie(null))
      .finally(() => setIsLoading(false))
  }, [id])

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="h-10 w-10 animate-spin text-[var(--color-muted-foreground)]" />
      </div>
    )
  }

  if (!movie) {
    return (
      <div className="text-center py-32">
        <p className="text-xl font-semibold text-[var(--color-muted-foreground)]">Movie not found</p>
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

      {/* Hero card */}
      <div className="bg-[var(--color-card)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
        <div className="flex flex-col sm:flex-row gap-0">
          {/* Poster */}
          <div className="sm:w-48 shrink-0 aspect-[2/3] sm:aspect-auto bg-gradient-to-br from-[var(--color-secondary)] to-[var(--color-accent)] flex items-center justify-center">
            <span className="text-7xl font-black text-[var(--color-muted-foreground)]/30 select-none">
              {movie.title.charAt(0)}
            </span>
          </div>

          {/* Info */}
          <div className="flex-1 p-6 sm:p-8 space-y-4">
            <div>
              <h1 className="text-3xl font-bold text-[var(--color-foreground)] leading-tight">
                {movie.title}
              </h1>
              <div className="flex items-center gap-4 mt-2 text-sm text-[var(--color-muted-foreground)]">
                <div className="flex items-center gap-1">
                  <Calendar className="h-4 w-4" />
                  <span>{movie.year}</span>
                </div>
                <div className="flex items-center gap-1">
                  <Video className="h-4 w-4" />
                  <span>{movie.director}</span>
                </div>
                <div className={cn('flex items-center gap-1 font-semibold', ratingColor(movie.rating))}>
                  <Star className="h-4 w-4 fill-current" />
                  <span>{movie.rating.toFixed(1)}</span>
                  {movie.numVotes && (
                    <span className="text-[var(--color-muted-foreground)] font-normal">
                      ({movie.numVotes.toLocaleString()} votes)
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Genres */}
            {movie.genres && movie.genres.length > 0 && (
              <div className="flex items-center gap-2 flex-wrap">
                <Tag className="h-4 w-4 text-[var(--color-muted-foreground)]" />
                {movie.genres.map((g) => (
                  <Link key={g.id} to={`/movies?genre=${encodeURIComponent(g.name)}`}>
                    <Badge variant="secondary" className="hover:bg-[var(--color-brand)] hover:text-white transition-colors cursor-pointer">
                      {g.name}
                    </Badge>
                  </Link>
                ))}
              </div>
            )}

            <Separator />

            {/* Add to cart */}
            <Button
              variant="brand"
              size="lg"
              className="gap-2"
              onClick={() => void addToCart(movie.id, movie.title)}
            >
              <ShoppingCart className="h-5 w-5" />
              Add to Cart
            </Button>
          </div>
        </div>
      </div>

      {/* Stars */}
      {movie.stars && movie.stars.length > 0 && (
        <section>
          <div className="flex items-center gap-2 mb-4">
            <User className="h-5 w-5 text-[var(--color-brand)]" />
            <h2 className="text-xl font-semibold">Cast</h2>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            {movie.stars.map((star) => (
              <Link
                key={star.id}
                to={`/star/${star.id}`}
                className="flex items-center gap-3 p-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-card)] hover:border-[var(--color-brand)]/50 hover:bg-[var(--color-accent)] transition-all group"
              >
                <div className="w-10 h-10 rounded-full bg-[var(--color-secondary)] flex items-center justify-center shrink-0">
                  <User className="h-5 w-5 text-[var(--color-muted-foreground)]" />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-[var(--color-foreground)] group-hover:text-[var(--color-brand)] transition-colors truncate">
                    {star.name}
                  </p>
                  {star.birthYear && (
                    <p className="text-xs text-[var(--color-muted-foreground)]">b. {star.birthYear}</p>
                  )}
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}
    </motion.div>
  )
}
