import { get, post } from './client'
import type { Cart } from './types'

export async function getCart(): Promise<Cart> {
  return get<Cart>('/cart')
}

export async function updateCart(movieId: string, quantity: number): Promise<Cart> {
  return post<Cart>('/cart', { movieId, quantity })
}
