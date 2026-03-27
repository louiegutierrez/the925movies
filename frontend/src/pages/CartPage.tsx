import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { ShoppingCart, Trash2, Plus, Minus, ArrowLeft, ArrowRight, Loader2 } from 'lucide-react'
import { getCart, updateCart } from '@/api/cart'
import type { Cart, CartItem } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { toast } from '@/hooks/use-toast'

export default function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<Cart | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [updating, setUpdating] = useState<string | null>(null)

  useEffect(() => {
    getCart()
      .then(setCart)
      .catch(() => setCart(null))
      .finally(() => setIsLoading(false))
  }, [])

  async function changeQty(item: CartItem, delta: number) {
    setUpdating(item.movieId)
    try {
      const updated = await updateCart(item.movieId, delta)
      setCart(updated)
    } catch {
      toast({ title: 'Error', description: 'Failed to update cart', variant: 'destructive' })
    } finally {
      setUpdating(null)
    }
  }

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="h-10 w-10 animate-spin text-[var(--color-muted-foreground)]" />
      </div>
    )
  }

  const isEmpty = !cart || cart.items.length === 0

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="gap-2">
          <ArrowLeft className="h-4 w-4" /> Back
        </Button>
        <h1 className="text-2xl font-bold text-[var(--color-foreground)]">Your Cart</h1>
      </div>

      {isEmpty ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center py-24 space-y-4"
        >
          <div className="w-20 h-20 rounded-full bg-[var(--color-card)] border border-[var(--color-border)] flex items-center justify-center mx-auto">
            <ShoppingCart className="h-10 w-10 text-[var(--color-muted-foreground)]" />
          </div>
          <p className="text-xl font-semibold text-[var(--color-foreground)]">Your cart is empty</p>
          <p className="text-[var(--color-muted-foreground)] text-sm">
            Browse movies and add them to your cart
          </p>
          <Button variant="brand" asChild>
            <Link to="/browse">Browse Movies</Link>
          </Button>
        </motion.div>
      ) : (
        <div className="grid md:grid-cols-[1fr_280px] gap-6">
          {/* Items */}
          <div className="space-y-3">
            <AnimatePresence mode="popLayout">
              {cart.items.map((item) => (
                <motion.div
                  key={item.movieId}
                  layout
                  initial={{ opacity: 0, x: -16 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 16, height: 0 }}
                  transition={{ duration: 0.2 }}
                  className="flex items-center gap-4 p-4 bg-[var(--color-card)] border border-[var(--color-border)] rounded-xl"
                >
                  {/* Poster mini */}
                  <div className="w-12 h-16 rounded-lg bg-gradient-to-br from-[var(--color-secondary)] to-[var(--color-accent)] flex items-center justify-center shrink-0">
                    <span className="text-lg font-black text-[var(--color-muted-foreground)]/40">
                      {item.title.charAt(0)}
                    </span>
                  </div>

                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-[var(--color-foreground)] truncate">{item.title}</p>
                    <p className="text-sm text-[var(--color-muted-foreground)]">
                      ${item.price.toFixed(2)} each
                    </p>
                  </div>

                  {/* Quantity controls */}
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={updating === item.movieId}
                      onClick={() => void changeQty(item, -1)}
                    >
                      {item.quantity <= 1 ? (
                        <Trash2 className="h-3.5 w-3.5 text-red-400" />
                      ) : (
                        <Minus className="h-3.5 w-3.5" />
                      )}
                    </Button>
                    <span className="w-6 text-center text-sm font-semibold">
                      {updating === item.movieId ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin mx-auto" />
                      ) : (
                        item.quantity
                      )}
                    </span>
                    <Button
                      variant="outline"
                      size="icon"
                      className="h-7 w-7"
                      disabled={updating === item.movieId}
                      onClick={() => void changeQty(item, 1)}
                    >
                      <Plus className="h-3.5 w-3.5" />
                    </Button>
                  </div>

                  <div className="text-right shrink-0">
                    <p className="font-semibold text-[var(--color-foreground)]">
                      ${(item.price * item.quantity).toFixed(2)}
                    </p>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>

          {/* Summary */}
          <div className="bg-[var(--color-card)] border border-[var(--color-border)] rounded-xl p-6 space-y-4 h-fit">
            <h2 className="font-semibold text-[var(--color-foreground)]">Order Summary</h2>
            <Separator />

            <div className="space-y-2 text-sm">
              {cart.items.map((item) => (
                <div key={item.movieId} className="flex justify-between text-[var(--color-muted-foreground)]">
                  <span className="truncate mr-2">{item.title} ×{item.quantity}</span>
                  <span className="shrink-0">${(item.price * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>

            <Separator />

            <div className="flex justify-between font-semibold text-[var(--color-foreground)]">
              <span>Total</span>
              <span>${cart.total.toFixed(2)}</span>
            </div>

            <Button
              variant="brand"
              size="lg"
              className="w-full gap-2"
              onClick={() => navigate('/checkout')}
            >
              Proceed to Checkout
              <ArrowRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
