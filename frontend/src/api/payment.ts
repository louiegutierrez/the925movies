import { get, post } from './client'
import type { Confirmation } from './types'

export interface CheckoutRequest {
  firstName: string
  lastName: string
  cardNumber: string
  expirationDate: string
}

export async function checkout(req: CheckoutRequest): Promise<{ status: string; message: string }> {
  return post('/payment', {
    first_name: req.firstName,
    last_name: req.lastName,
    card_number: req.cardNumber,
    expiration_date: req.expirationDate,
  })
}

export async function getConfirmation(): Promise<Confirmation> {
  return get<Confirmation>('/payment')
}
