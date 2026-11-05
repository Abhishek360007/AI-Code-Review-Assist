import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { CATEGORY_LABELS, ErrorNote, LANGUAGE_LABELS, SEVERITIES, scoreTone } from '../components/ui'

export default function InsightsPage() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.stats().then(setStats).catch(setError)
  }, [])

  if (error) return <div className="page"><ErrorNote error={error} /></div>
  if (!stats) return <div className="page-loading">Loading insights…</div>

  if (stats.totalReviews === 0) {
    return (
      <div className="page">
        <h1>Insights</h1>
        <div className="card empty">Insights appear after your first review. <Link to="/">Run one →</Link></div>
      </div>
    )
  }

  const categories = Object.entries(stats.findingsByCategory).sort((a, b) => b[1] - a[1])
  const maxCategory = Math.max(1, ...categories.map(([, n]) => n))
  const scores = stats.recentScores.map((p) => p.score)
  const first = scores[0]
  const last = scores[scores.length - 1]
  const trend = scores.length > 1 ? last - first : 0

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>Insights</h1>
          <p className="muted">How your code quality looks across every review you've run.</p>
        </div>
      </div>

      <div className="kpis">
        <Kpi label="Reviews" value={stats.totalReviews} />
        <Kpi label="Average score" value={stats.averageScore} tone={scoreTone(stats.averageScore)} />
        <Kpi label="Findings" value={stats.totalFindings} />
        <Kpi label="Critical + high" value={stats.findingsBySeverity.CRITICAL + stats.findingsBySeverity.HIGH} />
      </div>

      <div className="insight-grid">
        <section className="card">
          <h2 className="card-title">Score trend <span className="muted small">last {scores.length} reviews</span></h2>
          {scores.length > 1 ? (
            <>
              <Sparkline points={stats.recentScores} />
              <p className="muted small">
                {trend > 0 ? `Up ${trend} points` : trend < 0 ? `Down ${-trend} points` : 'Flat'} since the oldest review shown.
              </p>
            </>
          ) : <p className="muted">Run a few more reviews to see a trend.</p>}
        </section>

        <section className="card">
          <h2 className="card-title">Findings by severity</h2>
          <SeverityBar counts={stats.findingsBySeverity} total={stats.totalFindings} />
          <ul className="legend">
            {SEVERITIES.map((s) => (
              <li key={s}><span className={`dot sev-${s.toLowerCase()}-bg`} />{s.toLowerCase()}<b>{stats.findingsBySeverity[s]}</b></li>
            ))}
          </ul>
        </section>

        <section className="card">
          <h2 className="card-title">Findings by category</h2>
          <ul className="bars">
            {categories.map(([cat, n]) => (
              <li key={cat}>
                <span className="bar-label">{CATEGORY_LABELS[cat] || cat}</span>
                <span className="bar-track"><span className="bar-fill" style={{ width: `${(n / maxCategory) * 100}%` }} /></span>
                <span className="bar-value">{n}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="card">
          <h2 className="card-title">By language</h2>
          <table className="table compact">
            <thead><tr><th>Language</th><th className="num">Reviews</th><th className="num">Avg score</th></tr></thead>
            <tbody>
              {stats.languages.map((l) => (
                <tr key={l.language}>
                  <td>{LANGUAGE_LABELS[l.language] || l.language}</td>
                  <td className="num">{l.reviews}</td>
                  <td className="num"><span className={`tone-text tone-${scoreTone(l.averageScore)}`}>{l.averageScore}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>
    </div>
  )
}

function Kpi({ label, value, tone }) {
  return (
    <div className="card kpi">
      <span className="kpi-label">{label}</span>
      <span className={`kpi-value ${tone ? `tone-text tone-${tone}` : ''}`}>{value}</span>
    </div>
  )
}

function SeverityBar({ counts, total }) {
  if (total === 0) return <p className="muted">No findings yet.</p>
  return (
    <div className="stack-bar" role="img" aria-label="Findings split by severity">
      {SEVERITIES.filter((s) => counts[s] > 0).map((s) => (
        <span key={s} className={`sev-${s.toLowerCase()}-bg`} style={{ flexGrow: counts[s] }}
          title={`${s.toLowerCase()}: ${counts[s]}`} />
      ))}
    </div>
  )
}

function Sparkline({ points }) {
  const width = 480
  const height = 140
  const pad = 12
  const step = (width - pad * 2) / Math.max(1, points.length - 1)
  const y = (score) => pad + (1 - score / 100) * (height - pad * 2)
  const coords = points.map((p, i) => [pad + i * step, y(p.score)])
  const line = coords.map(([x, yy], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${yy.toFixed(1)}`).join(' ')
  const area = `${line} L${coords[coords.length - 1][0].toFixed(1)},${height - pad} L${pad},${height - pad} Z`

  return (
    <svg className="sparkline" viewBox={`0 0 ${width} ${height}`} role="img"
      aria-label="Review score over time">
      {[25, 50, 75].map((g) => <line key={g} x1={pad} x2={width - pad} y1={y(g)} y2={y(g)} className="grid" />)}
      <path d={area} className="area" />
      <path d={line} className="line" />
      {coords.map(([x, yy], i) => (
        <Link key={points[i].reviewId} to={`/reviews/${points[i].reviewId}`}>
          <circle cx={x} cy={yy} r="4" className="point"><title>{points[i].score}</title></circle>
        </Link>
      ))}
    </svg>
  )
}
