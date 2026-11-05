import Editor, { DiffEditor } from '@monaco-editor/react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import {
  CATEGORY_LABELS, ErrorNote, LANGUAGE_LABELS, SEVERITIES, ScoreRing, SeverityBadge, formatDate,
} from '../components/ui'
import { useTheme } from '../context/ThemeContext'

export default function ReviewPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { theme } = useTheme()
  const [review, setReview] = useState(null)
  const [error, setError] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [view, setView] = useState('code')
  const [activeIndex, setActiveIndex] = useState(null)
  const [busy, setBusy] = useState(false)
  const [copied, setCopied] = useState(false)
  const editorRef = useRef(null)
  const monacoRef = useRef(null)
  const decorationsRef = useRef(null)

  useEffect(() => {
    setReview(null)
    setError(null)
    setActiveIndex(null)
    setView('code')
    api.getReview(id).then(setReview).catch(setError)
  }, [id])

  const counts = useMemo(() => {
    const c = Object.fromEntries(SEVERITIES.map((s) => [s, 0]))
    review?.findings.forEach((f) => { c[f.severity] += 1 })
    return c
  }, [review])

  const visible = useMemo(
    () => (review?.findings || []).map((f, i) => ({ ...f, index: i }))
      .filter((f) => filter === 'ALL' || f.severity === filter),
    [review, filter],
  )

  // Highlight every finding's line in the code view.
  function applyDecorations() {
    const editor = editorRef.current
    const monaco = monacoRef.current
    if (!editor || !monaco || !review) return
    const decorations = review.findings.filter((f) => f.line).map((f) => ({
      range: new monaco.Range(f.line, 1, f.line, 1),
      options: {
        isWholeLine: true,
        className: `line-${f.severity.toLowerCase()}`,
        glyphMarginClassName: `glyph-${f.severity.toLowerCase()}`,
        hoverMessage: { value: `**${f.severity}** · ${f.title}` },
      },
    }))
    decorationsRef.current?.clear()
    decorationsRef.current = editor.createDecorationsCollection(decorations)
  }

  function onEditorMount(editor, monaco) {
    editorRef.current = editor
    monacoRef.current = monaco
    applyDecorations()
  }

  function focusFinding(f) {
    setActiveIndex(f.index)
    if (!f.line) return
    setView('code')
    // The editor may be remounting after a tab switch; wait a frame.
    requestAnimationFrame(() => {
      const editor = editorRef.current
      if (!editor) return
      editor.revealLineInCenter(f.line)
      editor.setSelection({ startLineNumber: f.line, startColumn: 1, endLineNumber: f.line, endColumn: 1 })
    })
  }

  async function rerun() {
    setBusy(true)
    try {
      const fresh = await api.rerunReview(id)
      navigate(`/reviews/${fresh.id}`)
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  async function remove() {
    if (!window.confirm('Delete this review permanently?')) return
    try {
      await api.deleteReview(id)
      navigate('/reviews', { replace: true })
    } catch (err) {
      setError(err)
    }
  }

  async function copyImproved() {
    try {
      await navigator.clipboard.writeText(review.improvedCode)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch { /* clipboard blocked */ }
  }

  if (error && !review) {
    return (
      <div className="page">
        <ErrorNote error={error} />
        <Link to="/reviews" className="btn btn-ghost">← Back to history</Link>
      </div>
    )
  }
  if (!review) return <div className="page-loading">Loading review…</div>

  const monacoTheme = theme === 'dark' ? 'vs-dark' : 'light'
  const editorOptions = {
    readOnly: true, minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false,
    glyphMargin: true, automaticLayout: true, renderLineHighlight: 'none',
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <Link to="/reviews" className="crumb">← History</Link>
          <h1 className="title-break">{review.title}</h1>
          <p className="muted meta-line">
            <span className="lang-tag">{LANGUAGE_LABELS[review.language] || review.language}</span>
            {formatDate(review.createdAt)} · {review.engine} · {(review.durationMs / 1000).toFixed(1)}s
          </p>
        </div>
        <div className="head-actions">
          <button className="btn btn-ghost btn-sm" onClick={rerun} disabled={busy}>{busy ? 'Re-running…' : 'Re-run'}</button>
          <button className="btn btn-danger btn-sm" onClick={remove}>Delete</button>
        </div>
      </div>

      <ErrorNote error={error} />

      <section className="card summary-card">
        <ScoreRing score={review.score} />
        <div className="summary-body">
          <p className="summary-text">{review.summary}</p>
          <div className="sev-counts">
            {SEVERITIES.map((s) => (
              <span key={s} className={`sev-count ${counts[s] ? '' : 'is-zero'}`}>
                <span className={`dot sev-${s.toLowerCase()}-bg`} />{counts[s]} {s.toLowerCase()}
              </span>
            ))}
          </div>
          {review.strengths.length > 0 && (
            <ul className="strengths">
              {review.strengths.map((s, i) => <li key={i}>{s}</li>)}
            </ul>
          )}
        </div>
      </section>

      <div className="review-layout">
        <section className="findings">
          <div className="filter-row" role="tablist" aria-label="Filter by severity">
            {['ALL', ...SEVERITIES].map((s) => (
              <button key={s} role="tab" aria-selected={filter === s}
                className={`chip ${filter === s ? 'chip-on' : ''}`} onClick={() => setFilter(s)}
                disabled={s !== 'ALL' && counts[s] === 0}>
                {s === 'ALL' ? `All ${review.findings.length}` : `${s.toLowerCase()} ${counts[s]}`}
              </button>
            ))}
          </div>

          {review.findings.length === 0 && (
            <div className="card empty">No issues found. Nice work!</div>
          )}

          {visible.map((f) => (
            <article key={f.index} className={`card finding ${activeIndex === f.index ? 'finding-active' : ''}`}
              onClick={() => focusFinding(f)}>
              <header>
                <SeverityBadge severity={f.severity} />
                <span className="cat">{CATEGORY_LABELS[f.category] || f.category}</span>
                {f.line && <span className="line-ref">Line {f.line}</span>}
              </header>
              <h3>{f.title}</h3>
              {f.detail && <p>{f.detail}</p>}
              {f.fix && (
                <div className="fix">
                  <span>Suggested fix</span>
                  <pre>{f.fix}</pre>
                </div>
              )}
            </article>
          ))}
        </section>

        <section className="card code-panel">
          <div className="code-tabs" role="tablist">
            <button role="tab" aria-selected={view === 'code'} className={view === 'code' ? 'tab-on' : ''}
              onClick={() => setView('code')}>Your code</button>
            <button role="tab" aria-selected={view === 'diff'} className={view === 'diff' ? 'tab-on' : ''}
              onClick={() => setView('diff')} disabled={!review.improvedCode}
              title={review.improvedCode ? '' : 'No improved version was suggested'}>
              Suggested version
            </button>
            {view === 'diff' && review.improvedCode && (
              <button className="btn btn-ghost btn-sm push-right" onClick={copyImproved}>{copied ? 'Copied ✓' : 'Copy'}</button>
            )}
          </div>
          <div className="code-frame">
            {view === 'code' ? (
              <Editor height="100%" language={review.language} value={review.code} theme={monacoTheme}
                options={editorOptions} onMount={onEditorMount} />
            ) : (
              <DiffEditor height="100%" language={review.language} original={review.code}
                modified={review.improvedCode} theme={monacoTheme}
                options={{ ...editorOptions, renderSideBySide: false, glyphMargin: false }} />
            )}
          </div>
        </section>
      </div>
    </div>
  )
}
