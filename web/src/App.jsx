import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import AppShell from './components/AppShell'
import { useAuth } from './context/AuthContext'
import AuthPage from './pages/AuthPage'
import HistoryPage from './pages/HistoryPage'
import InsightsPage from './pages/InsightsPage'
import NewReviewPage from './pages/NewReviewPage'
import NotFoundPage from './pages/NotFoundPage'
import ReviewPage from './pages/ReviewPage'

function RequireAuth({ children }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="page-loading">Loading…</div>
  if (!user) return <Navigate to="/signin" replace state={{ from: location.pathname }} />
  return children
}

function GuestOnly({ children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="page-loading">Loading…</div>
  return user ? <Navigate to="/" replace /> : children
}

export default function App() {
  return (
    <Routes>
      <Route path="/signin" element={<GuestOnly><AuthPage mode="signin" /></GuestOnly>} />
      <Route path="/signup" element={<GuestOnly><AuthPage mode="signup" /></GuestOnly>} />
      <Route element={<RequireAuth><AppShell /></RequireAuth>}>
        <Route index element={<NewReviewPage />} />
        <Route path="reviews" element={<HistoryPage />} />
        <Route path="reviews/:id" element={<ReviewPage />} />
        <Route path="insights" element={<InsightsPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
