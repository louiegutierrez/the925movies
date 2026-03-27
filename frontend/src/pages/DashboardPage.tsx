import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { UserPlus, Film, Database, CheckCircle2, XCircle, Loader2, ChevronDown, ChevronRight } from 'lucide-react'
import { insertStar, addMovie, getMetadata } from '@/api/dashboard'
import type { MetadataTable } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

interface FormResult {
  status: 'success' | 'error'
  message: string
  id?: string
}

function ResultBadge({ result }: { result: FormResult }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      className={`flex items-start gap-2 rounded-lg px-3 py-2.5 text-sm ${
        result.status === 'success'
          ? 'bg-emerald-900/20 border border-emerald-700/40 text-emerald-300'
          : 'bg-red-900/20 border border-red-700/40 text-red-300'
      }`}
    >
      {result.status === 'success' ? (
        <CheckCircle2 className="h-4 w-4 mt-0.5 shrink-0" />
      ) : (
        <XCircle className="h-4 w-4 mt-0.5 shrink-0" />
      )}
      <span>
        {result.message}
        {result.id && (
          <span className="ml-1 font-mono text-xs opacity-70">(ID: {result.id})</span>
        )}
      </span>
    </motion.div>
  )
}

function InsertStarForm() {
  const [name, setName] = useState('')
  const [birthYear, setBirthYear] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState<FormResult | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setIsLoading(true)
    setResult(null)
    try {
      const res = await insertStar({
        starName: name,
        birthYear: birthYear ? Number(birthYear) : undefined,
      })
      setResult({
        status: res.status === 'success' ? 'success' : 'error',
        message: res.message,
        id: res.starId,
      })
      if (res.status === 'success') {
        setName(''); setBirthYear('')
      }
    } catch {
      setResult({ status: 'error', message: 'Failed to insert star' })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-violet-900/50 border border-violet-700/40 flex items-center justify-center">
            <UserPlus className="h-4 w-4 text-violet-300" />
          </div>
          <div>
            <CardTitle>Insert Star</CardTitle>
            <CardDescription>Add a new actor/actress to the database</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 space-y-2">
              <Label htmlFor="star-name">Name *</Label>
              <Input
                id="star-name"
                placeholder="e.g. Keanu Reeves"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="birth-year">Birth Year (optional)</Label>
              <Input
                id="birth-year"
                type="number"
                placeholder="e.g. 1964"
                value={birthYear}
                onChange={(e) => setBirthYear(e.target.value)}
                min={1850}
                max={new Date().getFullYear()}
              />
            </div>
          </div>
          {result && <ResultBadge result={result} />}
          <Button type="submit" variant="brand" disabled={isLoading} className="gap-2">
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <UserPlus className="h-4 w-4" />}
            Insert Star
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function AddMovieForm() {
  const [form, setForm] = useState({
    title: '', year: '', director: '', starName: '', genreName: '',
  })
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState<FormResult | null>(null)

  function update(field: keyof typeof form, value: string) {
    setForm((p) => ({ ...p, [field]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setIsLoading(true)
    setResult(null)
    try {
      const res = await addMovie({
        title: form.title,
        year: Number(form.year),
        director: form.director,
        starName: form.starName,
        genreName: form.genreName,
      })
      setResult({
        status: res.status === 'success' ? 'success' : 'error',
        message: res.message,
        id: res.movieId,
      })
      if (res.status === 'success') {
        setForm({ title: '', year: '', director: '', starName: '', genreName: '' })
      }
    } catch {
      setResult({ status: 'error', message: 'Failed to add movie' })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-blue-900/50 border border-blue-700/40 flex items-center justify-center">
            <Film className="h-4 w-4 text-blue-300" />
          </div>
          <div>
            <CardTitle>Add Movie</CardTitle>
            <CardDescription>Add a new movie via stored procedure</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <form onSubmit={(e) => void handleSubmit(e)} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2 space-y-2">
              <Label htmlFor="movie-title">Title *</Label>
              <Input
                id="movie-title"
                placeholder="e.g. The Matrix"
                value={form.title}
                onChange={(e) => update('title', e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="movie-year">Year *</Label>
              <Input
                id="movie-year"
                type="number"
                placeholder="e.g. 1999"
                value={form.year}
                onChange={(e) => update('year', e.target.value)}
                required
                min={1888}
                max={new Date().getFullYear() + 5}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="movie-director">Director *</Label>
              <Input
                id="movie-director"
                placeholder="e.g. Lana Wachowski"
                value={form.director}
                onChange={(e) => update('director', e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="movie-star">Star Name *</Label>
              <Input
                id="movie-star"
                placeholder="e.g. Keanu Reeves"
                value={form.starName}
                onChange={(e) => update('starName', e.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="movie-genre">Genre *</Label>
              <Input
                id="movie-genre"
                placeholder="e.g. Action"
                value={form.genreName}
                onChange={(e) => update('genreName', e.target.value)}
                required
              />
            </div>
          </div>
          {result && <ResultBadge result={result} />}
          <Button type="submit" variant="brand" disabled={isLoading} className="gap-2">
            {isLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Film className="h-4 w-4" />}
            Add Movie
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function MetadataSection() {
  const [tables, setTables] = useState<MetadataTable[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [expanded, setExpanded] = useState<Set<string>>(new Set())

  useEffect(() => {
    getMetadata()
      .then(setTables)
      .catch(() => setTables([]))
      .finally(() => setIsLoading(false))
  }, [])

  function toggle(name: string) {
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(name)) next.delete(name)
      else next.add(name)
      return next
    })
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-emerald-900/50 border border-emerald-700/40 flex items-center justify-center">
            <Database className="h-4 w-4 text-emerald-300" />
          </div>
          <div>
            <CardTitle>Database Metadata</CardTitle>
            <CardDescription>Schema for all tables in the fabflix database</CardDescription>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin text-[var(--color-muted-foreground)]" />
          </div>
        ) : tables.length === 0 ? (
          <p className="text-sm text-[var(--color-muted-foreground)]">No metadata available</p>
        ) : (
          <div className="space-y-2">
            {tables.map((table) => (
              <div key={table.name} className="rounded-lg border border-[var(--color-border)] overflow-hidden">
                <button
                  onClick={() => toggle(table.name)}
                  className="w-full flex items-center justify-between px-4 py-3 text-sm font-medium text-[var(--color-foreground)] hover:bg-[var(--color-accent)] transition-colors"
                >
                  <span className="font-mono">{table.name}</span>
                  <div className="flex items-center gap-2 text-[var(--color-muted-foreground)]">
                    <span className="text-xs">{table.columns.length} columns</span>
                    {expanded.has(table.name) ? (
                      <ChevronDown className="h-4 w-4" />
                    ) : (
                      <ChevronRight className="h-4 w-4" />
                    )}
                  </div>
                </button>
                {expanded.has(table.name) && (
                  <div className="border-t border-[var(--color-border)]">
                    <table className="w-full text-xs">
                      <thead>
                        <tr className="bg-[var(--color-secondary)]">
                          <th className="text-left px-4 py-2 font-semibold text-[var(--color-muted-foreground)]">
                            Column
                          </th>
                          <th className="text-left px-4 py-2 font-semibold text-[var(--color-muted-foreground)]">
                            Type
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {table.columns.map((col, idx) => (
                          <tr
                            key={col.name}
                            className={idx % 2 === 0 ? '' : 'bg-[var(--color-secondary)]/30'}
                          >
                            <td className="px-4 py-2 font-mono text-[var(--color-foreground)]">
                              {col.name}
                            </td>
                            <td className="px-4 py-2 text-[var(--color-muted-foreground)]">
                              {col.type}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

export default function DashboardPage() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-3xl font-bold text-[var(--color-foreground)]">Employee Dashboard</h1>
        <p className="text-[var(--color-muted-foreground)] mt-1">
          Manage the FabFlix database
        </p>
      </div>

      <Separator />

      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ staggerChildren: 0.1 }}
        className="grid md:grid-cols-2 gap-6"
      >
        <InsertStarForm />
        <AddMovieForm />
      </motion.div>

      <MetadataSection />
    </div>
  )
}
