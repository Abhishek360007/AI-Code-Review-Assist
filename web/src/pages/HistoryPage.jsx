import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorNote, LANGUAGE_LABELS, ScorePill, SeverityBadge, timeAgo } from '../components/ui'

const PAGE_SIZE = 10

export default function HistoryPage() {
  const [page, setPage] = useState(0)
  const [language, setLanguage] = useState('')
  const [query, setQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query), 300)
    return () => clearTimeout(t)
  }, [query])

  useEffect(() => {
    setPage(0)
  }, [language, debouncedQuery])

  useEffect(() => {
    let cancelled = false
    api.listReviews({ page, size: PAGE_SIZE, language, q: debouncedQuery })
      .then((d) => { if (!cancelled) { setData(d); setError(null) } })
      .catch((e) => { if (!cancelled) setError(e) })
    return () => { cancelled = true }
  }, [page, language, debouncedQuery])

  const filtering = Boolean(language || debouncedQuery)

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>History</h1>
          <p className="muted">{data ? `${data.totalItems} review${data.totalItems === 1 ? '' : 's'}` : ' '}</p>
        </div>
        <Link to="/" className="btn btn-primary btn-sm">New review</Link>
      </div>

      <div className="toolbar">
        <input className="search" type="search" placeholder="Search by title…" value={query}
          onChange={(e) => setQuery(e.target.value)} aria-label="Search reviews" />
        <select value={language} onChange={(e) => setLanguage(e.target.value)} aria-label="Filter by language">
          <option value="">All languages</option>
          {Object.entries(LANGUAGE_LABELS).map(([id, label]) => <option key={id} value={id}>{label}</option>)}
        </select>
      </div>

      <ErrorNote error={error} />

      {data && data.items.length === 0 && (
        <div className="card empty">
          {filtering ? 'No reviews match these filters.' : <>No reviews yet. <Link to="/">Run your first one →</Link></>}
        </div>
      )}

      {data && data.items.length > 0 && (
        <div className="card table-card">
          <table className="table">
            <thead>
              <tr>
                <th>Review</th>
                <th>Language</th>
                <th className="num">Score</th>
                <th>Worst issue</th>
                <th className="num">Issues</th>
                <th className="hide-sm">When</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((r) => (
                <tr key={r.id}>
                  <td>
                    <div className="title-cell">
                      <Link to={`/reviews/${r.id}`}>{r.title}</Link>
                      <span className="muted small">{r.lineCount} lines</span>
                    </div>
                  </td>
                  <td><span className="lang-tag">{LANGUAGE_LABELS[r.language] || r.language}</span></td>
                  <td className="num"><ScorePill score={r.score} /></td>
                  <td><SeverityBadge severity={r.worstSeverity} /></td>
                  <td className="num">{r.issueCount}</td>
                  <td className="muted hide-sm">{timeAgo(r.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {data && data.totalPages > 1 && (
        <div className="pager">
          <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>← Newer</button>
          <span className="muted">Page {data.page + 1} of {data.totalPages}</span>
          <button className="btn btn-ghost btn-sm" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Older →</button>
        </div>
      )}
    </div>
  )
}
