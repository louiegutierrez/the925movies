import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowLeft, CreditCard, Loader2, Lock } from 'lucide-react'
import { checkout } from '@/api/payment'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Separator } from '@/components/ui/separator'
export default function CheckoutPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    cardNumber: '',
    expirationDate: '',
  })
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  function update(field: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
    setError('')
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setIsLoading(true)
    setError('')
    try {
      const res = await checkout(form)
      if (res.status === 'success') {
        navigate('/confirmation')
      } else {
        setError(res.message || 'Payment failed. Please check your card details.')
      }
    } catch {
      setError('Network error. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="max-w-lg mx-auto space-y-6"
    >
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="gap-2">
        <ArrowLeft className="h-4 w-4" /> Back to Cart
      </Button>

      <div className="bg-[var(--color-card)] border border-[var(--color-border)] rounded-2xl overflow-hidden">
        {/* Header */}
        <div className="px-8 pt-8 pb-6">
          <div className="flex items-center gap-3 mb-1">
            <div className="w-10 h-10 rounded-lg bg-[var(--color-brand)] flex items-center justify-center">
              <CreditCard className="h-5 w-5 text-white" />
            </div>
            <h1 className="text-2xl font-bold text-[var(--color-foreground)]">Checkout</h1>
          </div>
          <p className="text-sm text-[var(--color-muted-foreground)]">
            Complete your purchase securely
          </p>
        </div>

        <Separator />

        <form onSubmit={(e) => void handleSubmit(e)} className="px-8 py-6 space-y-5">
          <div>
            <h2 className="text-sm font-semibold text-[var(--color-muted-foreground)] uppercase tracking-wider mb-4">
              Cardholder Information
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="firstName">First Name</Label>
                <Input
                  id="firstName"
                  placeholder="John"
                  value={form.firstName}
                  onChange={(e) => update('firstName', e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="lastName">Last Name</Label>
                <Input
                  id="lastName"
                  placeholder="Doe"
                  value={form.lastName}
                  onChange={(e) => update('lastName', e.target.value)}
                  required
                />
              </div>
            </div>
          </div>

          <div>
            <h2 className="text-sm font-semibold text-[var(--color-muted-foreground)] uppercase tracking-wider mb-4">
              Card Details
            </h2>
            <div className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="cardNumber">Card Number</Label>
                <Input
                  id="cardNumber"
                  placeholder="1234 5678 9012 3456"
                  value={form.cardNumber}
                  onChange={(e) => update('cardNumber', e.target.value)}
                  required
                  maxLength={19}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="expDate">Expiration Date</Label>
                <Input
                  id="expDate"
                  placeholder="MM/YYYY"
                  value={form.expirationDate}
                  onChange={(e) => update('expirationDate', e.target.value)}
                  required
                />
              </div>
            </div>
          </div>

          {error && (
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-sm text-red-400 bg-red-900/20 border border-red-800/40 rounded-lg px-3 py-2"
            >
              {error}
            </motion.p>
          )}

          <Button type="submit" variant="brand" size="lg" className="w-full gap-2" disabled={isLoading}>
            {isLoading ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Processing…
              </>
            ) : (
              <>
                <Lock className="h-4 w-4" />
                Place Order
              </>
            )}
          </Button>

          <p className="text-center text-xs text-[var(--color-muted-foreground)]">
            Your payment information is processed securely
          </p>
        </form>
      </div>
    </motion.div>
  )
}
