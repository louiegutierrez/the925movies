import { get, post } from './client'
import type { MetadataTable } from './types'

export interface InsertStarRequest {
  starName: string
  birthYear?: number
}

export interface AddMovieRequest {
  title: string
  year: number
  director: string
  starName: string
  genreName: string
}

export async function insertStar(
  req: InsertStarRequest,
): Promise<{ status: string; message: string; starId?: string }> {
  return post('/insert_star', {
    star_name: req.starName,
    ...(req.birthYear ? { birth_year: req.birthYear } : {}),
  })
}

export async function addMovie(
  req: AddMovieRequest,
): Promise<{ status: string; message: string; movieId?: string }> {
  return post('/add_movie', {
    title: req.title,
    year: req.year,
    director: req.director,
    star_name: req.starName,
    genre_name: req.genreName,
  })
}

export async function getMetadata(): Promise<MetadataTable[]> {
  return get<MetadataTable[]>('/get_metadata')
}
