import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Film, Search, ShoppingCart, LogOut, LayoutDashboard, X, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/context/AuthContext'
import { cn } from '@/lib/utils'

interface NavbarProps {
  cartItemCount?: number
}

export function Navbar({ cartItemCount = 0 }: NavbarProps) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchQuery, setSearchQuery] = useState('')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [suggestions, setSuggestions] = useState<string[]>([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const suggestRef = useRef<HTMLDivElement>(null)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Close suggestions on outside click
  useEffect(() => {
    function handler(e: MouseEvent) {
      if (suggestRef.current && !suggestRef.current.contains(e.target as Node)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  // Debounced autocomplete fetch
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (searchQuery.trim().length < 2) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await fetch(`/api/search?title=${encodeURIComponent(searchQuery)}&size=5`, {
          credentials: 'include',
        })
        const data = await res.json() as { movies?: Array<{ title: string }> }
        const titles = (data.movies ?? []).map((m) => m.title)
        setSuggestions(titles)
        setShowSuggestions(titles.length > 0)
      } catch {
        setSuggestions([])
      }
    }, 250)
  }, [searchQuery])

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!searchQuery.trim()) return
    navigate(`/movies?title=${encodeURIComponent(searchQuery)}`)
    setShowSuggestions(false)
  }

  function pickSuggestion(title: string) {
    navigate(`/movies?title=${encodeURIComponent(title)}`)
    setSearchQuery('')
    setShowSuggestions(false)
  }

  async function handleLogout() {
    await logout()
    navigate('/', { replace: true })
  }

  const isActive = (path: string) => location.pathname === path

  return (
    <nav className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-background)]/95 backdrop-blur supports-[backdrop-filter]:bg-[var(--color-background)]/60">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 gap-4">
          {/* Logo */}
          <Link
            to="/browse"
            className="flex items-center gap-2 text-[var(--color-foreground)] shrink-0"
          >
            <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-[var(--color-brand)]">
              <Film className="w-4 h-4 text-white" />
            </div>
            <span className="font-bold text-lg hidden sm:block">FabFlix</span>
          </Link>

          {/* Nav links */}
          <div className="hidden md:flex items-center gap-1">
            <Link
              to="/browse"
              className={cn(
                'px-3 py-1.5 rounded-md text-sm font-medium transition-colors',
                isActive('/browse')
                  ? 'bg-[var(--color-accent)] text-[var(--color-foreground)]'
                  : 'text-[var(--color-muted-foreground)] hover:text-[var(--color-foreground)] hover:bg-[var(--color-accent)]',
              )}
            >
              Browse
            </Link>
            {user?.role === 'employee' && (
              <Link
                to="/dashboard"
                className={cn(
                  'px-3 py-1.5 rounded-md text-sm font-medium transition-colors',
                  isActive('/dashboard')
                    ? 'bg-[var(--color-accent)] text-[var(--color-foreground)]'
                    : 'text-[var(--color-muted-foreground)] hover:text-[var(--color-foreground)] hover:bg-[var(--color-accent)]',
                )}
              >
                Dashboard
              </Link>
            )}
          </div>

          {/* Search */}
          <div className="flex-1 max-w-sm relative" ref={suggestRef}>
            <form onSubmit={handleSearch} className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--color-muted-foreground)] pointer-events-none" />
              <Input
                placeholder="Search movies…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
                className="pl-9 pr-8 h-9 bg-[var(--color-secondary)] border-transparent focus:border-[var(--color-brand)] text-sm"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => { setSearchQuery(''); setShowSuggestions(false) }}
                  className="absolute right-2 top-1/2 -translate-y-1/2 text-[var(--color-muted-foreground)] hover:text-[var(--color-foreground)]"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              )}
            </form>
            {showSuggestions && (
              <div className="absolute top-full left-0 right-0 mt-1 bg-[var(--color-popover)] border border-[var(--color-border)] rounded-lg shadow-xl overflow-hidden z-50">
                {suggestions.map((title) => (
                  <button
                    key={title}
                    onMouseDown={() => pickSuggestion(title)}
                    className="w-full text-left px-4 py-2.5 text-sm hover:bg-[var(--color-accent)] transition-colors"
                  >
                    <Search className="inline h-3.5 w-3.5 mr-2 text-[var(--color-muted-foreground)]" />
                    {title}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Right actions */}
          <div className="flex items-center gap-2 shrink-0">
            <Link to="/cart">
              <Button variant="ghost" size="icon" className="relative">
                <ShoppingCart className="h-5 w-5" />
                {cartItemCount > 0 && (
                  <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-[var(--color-brand)] text-white text-[10px] font-bold flex items-center justify-center">
                    {cartItemCount > 9 ? '9+' : cartItemCount}
                  </span>
                )}
              </Button>
            </Link>

            {user?.role === 'employee' && (
              <Link to="/dashboard" className="hidden md:block">
                <Button variant="ghost" size="icon">
                  <LayoutDashboard className="h-5 w-5" />
                </Button>
              </Link>
            )}

            <Button
              variant="ghost"
              size="icon"
              onClick={() => void handleLogout()}
              title="Sign out"
            >
              <LogOut className="h-4 w-4" />
            </Button>

            {/* Mobile menu toggle */}
            <Button
              variant="ghost"
              size="icon"
              className="md:hidden"
              onClick={() => setMobileOpen((v) => !v)}
            >
              <Menu className="h-5 w-5" />
            </Button>
          </div>
        </div>

        {/* Mobile nav */}
        {mobileOpen && (
          <div className="md:hidden border-t border-[var(--color-border)] py-3 flex flex-col gap-1">
            <Link
              to="/browse"
              onClick={() => setMobileOpen(false)}
              className="px-3 py-2 rounded-md text-sm font-medium text-[var(--color-foreground)] hover:bg-[var(--color-accent)]"
            >
              Browse
            </Link>
            {user?.role === 'employee' && (
              <Link
                to="/dashboard"
                onClick={() => setMobileOpen(false)}
                className="px-3 py-2 rounded-md text-sm font-medium text-[var(--color-foreground)] hover:bg-[var(--color-accent)]"
              >
                Dashboard
              </Link>
            )}
          </div>
        )}
      </div>
    </nav>
  )
}
