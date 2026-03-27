export interface Genre {
  id: number
  name: string
}

export interface Star {
  id: string
  name: string
  birthYear?: number
  movies?: Movie[]
}

export interface Movie {
  id: string
  title: string
  year: number
  director: string
  rating: number
  numVotes?: number
  genres?: Genre[]
  stars?: Star[]
}

export interface SearchParams {
  title?: string
  year?: string
  director?: string
  star?: string
  genre?: string
  letter?: string
  page?: number
  size?: number
  sort?: string
}

export interface SearchResult {
  movies: Movie[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface CartItem {
  movieId: string
  title: string
  quantity: number
  price: number
}

export interface Cart {
  items: CartItem[]
  total: number
}

export interface SaleItem {
  title: string
  quantity: number
  price: number
  saleAmount: number
}

export interface Confirmation {
  sales: SaleItem[]
  total: number
}

export interface LoginRequest {
  email: string
  password: string
  recaptchaToken?: string
}

export interface LoginResponse {
  status: string
  message: string
  role?: string
}

export interface MetadataColumn {
  name: string
  type: string
}

export interface MetadataTable {
  name: string
  columns: MetadataColumn[]
}

export interface ApiError {
  status: string
  message: string
}
