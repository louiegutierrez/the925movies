import { useState, useCallback } from 'react'
import { getCart, updateCart } from '@/api/cart'
import type { Cart } from '@/api/types'
import { toast } from '@/hooks/use-toast'

export function useCart() {
  const [cart, setCart] = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const fetchCart = useCallback(async () => {
    setIsLoading(true)
    try {
      const data = await getCart()
      setCart(data)
    } catch {
      // ignore
    } finally {
      setIsLoading(false)
    }
  }, [])

  const addToCart = useCallback(async (movieId: string, title: string, quantity = 1) => {
    try {
      const data = await updateCart(movieId, quantity)
      setCart(data)
      toast({ title: 'Added to cart', description: `${title} ×${quantity}` })
    } catch {
      toast({ title: 'Error', description: 'Failed to update cart', variant: 'destructive' })
    }
  }, [])

  const removeFromCart = useCallback(async (movieId: string, quantity: number) => {
    try {
      const data = await updateCart(movieId, -quantity)
      setCart(data)
    } catch {
      toast({ title: 'Error', description: 'Failed to update cart', variant: 'destructive' })
    }
  }, [])

  const itemCount = cart?.items.reduce((s, i) => s + i.quantity, 0) ?? 0

  return { cart, isLoading, fetchCart, addToCart, removeFromCart, itemCount }
}
