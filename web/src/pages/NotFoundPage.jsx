import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <div className="page center-page">
      <h1>Page not found</h1>
      <p className="muted">That page doesn't exist.</p>
      <Link to="/" className="btn btn-primary">Go home</Link>
    </div>
  )
}
