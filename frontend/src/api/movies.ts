import { get } from './client'
import type { Genre, Movie, SearchParams, SearchResult } from './types'

export async function getGenres(): Promise<Genre[]> {
  return get<Genre[]>('/genres')
}

export async function getMovie(id: string): Promise<Movie> {
  return get<Movie>('/movie', { id })
}

export async function searchMovies(params: SearchParams): Promise<SearchResult> {
  return get<SearchResult>('/search', {
    title: params.title,
    year: params.year,
    director: params.director,
    star: params.star,
    genre: params.genre,
    letter: params.letter,
    page: params.page,
    size: params.size,
    sort: params.sort,
  })
}
