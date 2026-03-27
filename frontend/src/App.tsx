import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from '@/context/AuthContext'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { Layout } from '@/components/Layout'
import LoginPage from '@/pages/LoginPage'
import BrowsePage from '@/pages/BrowsePage'
import MovieListPage from '@/pages/MovieListPage'
import SingleMoviePage from '@/pages/SingleMoviePage'
import SingleStarPage from '@/pages/SingleStarPage'
import CartPage from '@/pages/CartPage'
import CheckoutPage from '@/pages/CheckoutPage'
import ConfirmationPage from '@/pages/ConfirmationPage'
import DashboardPage from '@/pages/DashboardPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<LoginPage />} />

          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/browse" element={<BrowsePage />} />
            <Route path="/movies" element={<MovieListPage />} />
            <Route path="/movie/:id" element={<SingleMoviePage />} />
            <Route path="/star/:id" element={<SingleStarPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/confirmation" element={<ConfirmationPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute requireRole="employee">
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
