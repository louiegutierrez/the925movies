import { useEffect, useState, useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { Filter, Loader2, Search, X } from 'lucide-react'
import { searchMovies } from '@/api/movies'
import type { Movie, SearchResult } from '@/api/types'
import { MovieCard } from '@/components/MovieCard'
import { Pagination } from '@/components/Pagination'
import { SortSelector } from '@/components/SortSelector'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useCart } from '@/hooks/useCart'

const DEFAULT_SIZE = 25
const DEFAULT_SORT = 'rating_desc'

export default function MovieListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { addToCart } = useCart()

  const [result, setResult] = useState<SearchResult | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [showFilters, setShowFilters] = useState(false)

  // Derived search state from URL
  const title = searchParams.get('title') ?? ''
  const year = searchParams.get('year') ?? ''
  const director = searchParams.get('director') ?? ''
  const star = searchParams.get('star') ?? ''
  const genre = searchParams.get('genre') ?? ''
  const letter = searchParams.get('letter') ?? ''
  const page = Number(searchParams.get('page') ?? 1)
  const size = Number(searchParams.get('size') ?? DEFAULT_SIZE)
  const sort = searchParams.get('sort') ?? DEFAULT_SORT

  // Filter form local state
  const [localTitle, setLocalTitle] = useState(title)
  const [localYear, setLocalYear] = useState(year)
  const [localDirector, setLocalDirector] = useState(director)
  const [localStar, setLocalStar] = useState(star)

  const fetchMovies = useCallback(async () => {
    setIsLoading(true)
    try {
      const data = await searchMovies({ title, year, director, star, genre, letter, page, size, sort })
      setResult(data)
    } catch {
      setResult(null)
    } finally {
      setIsLoading(false)
    }
  }, [title, year, director, star, genre, letter, page, size, sort])

  useEffect(() => {
    void fetchMovies()
  }, [fetchMovies])

  function updateParams(updates: Record<string, string | number | null>) {
    const next = new URLSearchParams(searchParams)
    for (const [k, v] of Object.entries(updates)) {
      if (v === null || v === '' || v === undefined) {
        next.delete(k)
      } else {
        next.set(k, String(v))
      }
    }
    next.set('page', '1')
    setSearchParams(next)
  }

  function applyFilters(e: React.FormEvent) {
    e.preventDefault()
    updateParams({ title: localTitle, year: localYear, director: localDirector, star: localStar, genre: '', letter: '' })
    setShowFilters(false)
  }

  function clearFilters() {
    setLocalTitle(''); setLocalYear(''); setLocalDirector(''); setLocalStar('')
    updateParams({ title: null, year: null, director: null, star: null, genre: null, letter: null })
  }

  const hasActiveFilters = !!(title || year || director || star || genre || letter)

  const heading = genre
    ? `Genre: ${genre}`
    : letter
    ? `Titles starting with "${letter}"`
    : title
    ? `Search: "${title}"`
    : 'All Movies'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-[var(--color-foreground)]">{heading}</h1>
          {result && (
            <p className="text-sm text-[var(--color-muted-foreground)] mt-0.5">
              {result.total.toLocaleString()} movies found
            </p>
          )}
        </div>

        <div className="flex items-center gap-3">
          {hasActiveFilters && (
            <Button variant="ghost" size="sm" onClick={clearFilters} className="text-xs gap-1">
              <X className="h-3.5 w-3.5" />
              Clear filters
            </Button>
          )}
          <Button
            variant={showFilters ? 'secondary' : 'outline'}
            size="sm"
            onClick={() => setShowFilters((v) => !v)}
            className="gap-2"
          >
            <Filter className="h-4 w-4" />
            Filters
          </Button>
          <SortSelector value={sort} onChange={(v) => updateParams({ sort: v })} />
        </div>
      </div>

      {/* Filter panel */}
      <AnimatePresence>
        {showFilters && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="overflow-hidden"
          >
            <form
              onSubmit={applyFilters}
              className="grid grid-cols-2 md:grid-cols-4 gap-4 p-4 bg-[var(--color-card)] rounded-xl border border-[var(--color-border)]"
            >
              <div className="space-y-1">
                <Label htmlFor="f-title" className="text-xs">Title</Label>
                <Input
                  id="f-title"
                  placeholder="Any title"
                  value={localTitle}
                  onChange={(e) => setLocalTitle(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="f-year" className="text-xs">Year</Label>
                <Input
                  id="f-year"
                  placeholder="e.g. 2000"
                  value={localYear}
                  onChange={(e) => setLocalYear(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="f-director" className="text-xs">Director</Label>
                <Input
                  id="f-director"
                  placeholder="Any director"
                  value={localDirector}
                  onChange={(e) => setLocalDirector(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="f-star" className="text-xs">Star</Label>
                <Input
                  id="f-star"
                  placeholder="Any actor"
                  value={localStar}
                  onChange={(e) => setLocalStar(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div className="col-span-full flex justify-end gap-2">
                <Button type="button" variant="ghost" size="sm" onClick={() => setShowFilters(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="brand" size="sm" className="gap-1">
                  <Search className="h-3.5 w-3.5" />
                  Apply
                </Button>
              </div>
            </form>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Movie grid */}
      {isLoading ? (
        <div className="flex justify-center py-24">
          <Loader2 className="h-10 w-10 animate-spin text-[var(--color-muted-foreground)]" />
        </div>
      ) : !result || result.movies.length === 0 ? (
        <div className="text-center py-24 text-[var(--color-muted-foreground)]">
          <p className="text-xl font-semibold mb-2">No movies found</p>
          <p className="text-sm">Try adjusting your filters or search terms.</p>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
            {result.movies.map((movie: Movie) => (
              <MovieCard
                key={movie.id}
                movie={movie}
                onAddToCart={(m) => void addToCart(m.id, m.title)}
              />
            ))}
          </div>

          <Pagination
            page={result.page}
            totalPages={result.totalPages}
            size={result.size}
            total={result.total}
            onPageChange={(p) => updateParams({ page: p })}
            onSizeChange={(s) => updateParams({ size: s, page: 1 })}
          />
        </>
      )}
    </div>
  )
}
