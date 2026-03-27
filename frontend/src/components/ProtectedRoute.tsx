import { Navigate } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'

interface ProtectedRouteProps {
  children: React.ReactNode
  requireRole?: 'employee' | 'customer'
}

export function ProtectedRoute({ children, requireRole }: ProtectedRouteProps) {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/" replace />
  }

  if (requireRole && user.role !== requireRole) {
    return <Navigate to="/browse" replace />
  }

  return <>{children}</>
}
