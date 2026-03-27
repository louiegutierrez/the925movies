import { get, post } from './client'
import type { LoginRequest, LoginResponse } from './types'

export async function login(req: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/login', {
    email: req.email,
    password: req.password,
    ...(req.recaptchaToken ? { 'g-recaptcha-response': req.recaptchaToken } : {}),
  })
}

export async function logout(): Promise<void> {
  await get('/logout')
}

export async function getRecaptchaSiteKey(): Promise<{ siteKey: string }> {
  return get<{ siteKey: string }>('/recaptcha-site-key')
}
