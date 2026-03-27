import React, { createContext, useContext, useState, useCallback } from 'react'
import { login as apiLogin, logout as apiLogout } from '@/api/auth'
import type { LoginRequest } from '@/api/types'

interface AuthUser {
  role: 'customer' | 'employee'
}

interface AuthContextValue {
  user: AuthUser | null
  isLoading: boolean
  login: (req: LoginRequest) => Promise<{ success: boolean; role?: string; message?: string }>
  logout: () => Promise<void>
  setUser: (user: AuthUser | null) => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function getRoleFromCookie(): 'customer' | 'employee' | null {
  const cookies = document.cookie.split(';')
  for (const cookie of cookies) {
    const [name] = cookie.trim().split('=')
    if (name === 'jwtToken') {
      try {
        const parts = cookie.trim().split('=')[1].split('.')
        if (parts.length === 3) {
          const payload = JSON.parse(atob(parts[1])) as { role?: string }
          if (payload.role === 'employee') return 'employee'
          return 'customer'
        }
      } catch {
        return null
      }
    }
  }
  return null
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const role = getRoleFromCookie()
    return role ? { role } : null
  })
  const [isLoading, setIsLoading] = useState(false)

  const login = useCallback(async (req: LoginRequest) => {
    setIsLoading(true)
    try {
      const res = await apiLogin(req)
      if (res.status === 'success') {
        const role = (res.role ?? 'customer') as 'customer' | 'employee'
        setUser({ role })
        return { success: true, role }
      }
      return { success: false, message: res.message }
    } catch {
      return { success: false, message: 'Network error. Please try again.' }
    } finally {
      setIsLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, setUser }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
