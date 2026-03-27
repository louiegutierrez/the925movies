import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { CheckCircle2, Film, Loader2 } from 'lucide-react'
import { getConfirmation } from '@/api/payment'
import type { Confirmation } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'

export default function ConfirmationPage() {
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    getConfirmation()
      .then(setConfirmation)
      .catch(() => setConfirmation(null))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) {
    return (
      <div className="flex justify-center py-32">
        <Loader2 className="h-10 w-10 animate-spin text-[var(--color-muted-foreground)]" />
      </div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.4 }}
      className="max-w-lg mx-auto space-y-6 py-8"
    >
      {/* Success header */}
      <div className="text-center space-y-3">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: 'spring', stiffness: 200, delay: 0.1 }}
          className="w-20 h-20 rounded-full bg-emerald-900/30 border-2 border-emerald-500/50 flex items-center justify-center mx-auto"
        >
          <CheckCircle2 className="h-10 w-10 text-emerald-400" />
        </motion.div>
        <h1 className="text-2xl font-bold text-[var(--color-foreground)]">Order Confirmed!</h1>
        <p className="text-[var(--color-muted-foreground)]">
          Thank you for your purchase. Enjoy your movies!
        </p>
      </div>

      {/* Order summary */}
      {confirmation && (
        <div className="bg-[var(--color-card)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-[var(--color-border)]">
            <h2 className="font-semibold text-[var(--color-foreground)]">Order Summary</h2>
          </div>
          <div className="px-6 py-4 space-y-3">
            {confirmation.sales.map((sale, idx) => (
              <div key={idx} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-8 h-8 rounded-lg bg-[var(--color-secondary)] flex items-center justify-center shrink-0">
                    <Film className="h-4 w-4 text-[var(--color-muted-foreground)]" />
                  </div>
                  <div className="min-w-0">
                    <p className="font-medium text-[var(--color-foreground)] truncate">{sale.title}</p>
                    <p className="text-xs text-[var(--color-muted-foreground)]">
                      {sale.quantity} × ${sale.price.toFixed(2)}
                    </p>
                  </div>
                </div>
                <span className="font-medium text-[var(--color-foreground)] shrink-0 ml-4">
                  ${sale.saleAmount.toFixed(2)}
                </span>
              </div>
            ))}

            <Separator />

            <div className="flex justify-between font-bold text-[var(--color-foreground)]">
              <span>Total Charged</span>
              <span>${confirmation.total.toFixed(2)}</span>
            </div>
          </div>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3">
        <Button variant="brand" size="lg" className="flex-1" asChild>
          <Link to="/browse">Continue Browsing</Link>
        </Button>
        <Button variant="outline" size="lg" className="flex-1" asChild>
          <Link to="/cart">View Cart</Link>
        </Button>
      </div>
    </motion.div>
  )
}
