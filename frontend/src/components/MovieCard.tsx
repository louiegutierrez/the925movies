import { Link } from 'react-router-dom'
import { Star, ShoppingCart } from 'lucide-react'
import { motion } from 'framer-motion'
import { Button } from './ui/button'
import { Badge } from './ui/badge'
import type { Movie } from '@/api/types'
import { cn } from '@/lib/utils'

interface MovieCardProps {
  movie: Movie
  onAddToCart?: (movie: Movie) => void
  className?: string
}

function ratingColor(rating: number): string {
  if (rating >= 8) return 'text-emerald-400'
  if (rating >= 6) return 'text-amber-400'
  return 'text-red-400'
}

export function MovieCard({ movie, onAddToCart, className }: MovieCardProps) {
  return (
    <motion.div
      layout
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.2 }}
      className={cn(
        'group relative flex flex-col rounded-xl border border-[var(--color-border)] bg-[var(--color-card)] overflow-hidden hover:border-[var(--color-brand)]/50 hover:shadow-xl hover:shadow-black/30 transition-all duration-200',
        className,
      )}
    >
      {/* Poster placeholder */}
      <div className="relative aspect-[2/3] bg-gradient-to-br from-[var(--color-secondary)] to-[var(--color-accent)] flex items-center justify-center overflow-hidden">
        <span className="text-5xl font-black text-[var(--color-muted-foreground)]/30 select-none">
          {movie.title.charAt(0)}
        </span>
        {/* Hover overlay */}
        <div className="absolute inset-0 bg-[var(--color-brand)]/0 group-hover:bg-[var(--color-brand)]/10 transition-colors" />
      </div>

      <div className="flex flex-col flex-1 p-3 gap-2">
        <Link
          to={`/movie/${movie.id}`}
          className="font-semibold text-sm text-[var(--color-foreground)] hover:text-[var(--color-brand)] transition-colors line-clamp-2 leading-snug"
        >
          {movie.title}
        </Link>

        <div className="flex items-center justify-between text-xs text-[var(--color-muted-foreground)]">
          <span>{movie.year}</span>
          <div className={cn('flex items-center gap-1 font-semibold', ratingColor(movie.rating))}>
            <Star className="h-3 w-3 fill-current" />
            <span>{movie.rating.toFixed(1)}</span>
          </div>
        </div>

        {movie.genres && movie.genres.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {movie.genres.slice(0, 2).map((g) => (
              <Badge key={g.id} variant="secondary" className="text-[10px] px-1.5 py-0">
                {g.name}
              </Badge>
            ))}
          </div>
        )}

        {onAddToCart && (
          <Button
            size="sm"
            variant="brand"
            className="w-full mt-auto h-7 text-xs"
            onClick={(e) => {
              e.preventDefault()
              onAddToCart(movie)
            }}
          >
            <ShoppingCart className="h-3 w-3 mr-1" />
            Add to Cart
          </Button>
        )}
      </div>
    </motion.div>
  )
}
