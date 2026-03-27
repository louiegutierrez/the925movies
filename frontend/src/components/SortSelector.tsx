import { ArrowUpDown } from 'lucide-react'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select'

interface SortSelectorProps {
  value: string
  onChange: (value: string) => void
}

const SORT_OPTIONS = [
  { value: 'rating_desc', label: 'Rating: High → Low' },
  { value: 'rating_asc', label: 'Rating: Low → High' },
  { value: 'title_asc', label: 'Title: A → Z' },
  { value: 'title_desc', label: 'Title: Z → A' },
  { value: 'year_desc', label: 'Year: Newest' },
  { value: 'year_asc', label: 'Year: Oldest' },
]

export function SortSelector({ value, onChange }: SortSelectorProps) {
  return (
    <div className="flex items-center gap-2">
      <ArrowUpDown className="h-4 w-4 text-[var(--color-muted-foreground)]" />
      <Select value={value} onValueChange={onChange}>
        <SelectTrigger className="h-8 w-48 text-xs">
          <SelectValue placeholder="Sort by…" />
        </SelectTrigger>
        <SelectContent>
          {SORT_OPTIONS.map((opt) => (
            <SelectItem key={opt.value} value={opt.value}>
              {opt.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
