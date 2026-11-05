// Small presentational pieces shared across pages.

export const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']

export const CATEGORY_LABELS = {
  BUG: 'Bug',
  SECURITY: 'Security',
  PERFORMANCE: 'Performance',
  MAINTAINABILITY: 'Maintainability',
  STYLE: 'Style',
  TESTING: 'Testing',
}

export const LANGUAGE_LABELS = {
  java: 'Java', python: 'Python', javascript: 'JavaScript', typescript: 'TypeScript', cpp: 'C++', c: 'C',
  csharp: 'C#', go: 'Go', rust: 'Rust', kotlin: 'Kotlin', php: 'PHP', ruby: 'Ruby', swift: 'Swift',
  sql: 'SQL', shell: 'Shell', plaintext: 'Other',
}

export function SeverityBadge({ severity }) {
  if (!severity) return <span className="badge badge-clean">Clean</span>
  return <span className={`badge sev-${severity.toLowerCase()}`}>{severity.toLowerCase()}</span>
}

export function scoreTone(score) {
  if (score >= 85) return 'good'
  if (score >= 60) return 'fair'
  return 'poor'
}

export function ScoreRing({ score, size = 112 }) {
  const stroke = 10
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  const offset = c * (1 - score / 100)
  return (
    <div className={`score-ring tone-${scoreTone(score)}`} style={{ width: size, height: size }}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-label={`Score ${score} out of 100`}>
        <circle cx={size / 2} cy={size / 2} r={r} className="ring-track" strokeWidth={stroke} fill="none" />
        <circle cx={size / 2} cy={size / 2} r={r} className="ring-value" strokeWidth={stroke} fill="none"
          strokeDasharray={c} strokeDashoffset={offset} strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`} />
      </svg>
      <div className="score-ring-label">
        <strong>{score}</strong>
        <span>/ 100</span>
      </div>
    </div>
  )
}

export function ScorePill({ score }) {
  return <span className={`score-pill tone-${scoreTone(score)}`}>{score}</span>
}

export function ErrorNote({ error }) {
  if (!error) return null
  return <div className="note note-error" role="alert">{error.message || String(error)}</div>
}

export function formatDate(iso) {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
}

export function timeAgo(iso) {
  const seconds = Math.round((Date.now() - new Date(iso).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.round(hours / 24)
  if (days < 30) return `${days}d ago`
  return new Date(iso).toLocaleDateString()
}
