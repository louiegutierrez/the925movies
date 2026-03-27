import { get } from './client'
import type { Star } from './types'

export async function getStar(id: string): Promise<Star> {
  return get<Star>('/star', { id })
}
