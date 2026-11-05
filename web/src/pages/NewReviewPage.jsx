import Editor from '@monaco-editor/react'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { ErrorNote, LANGUAGE_LABELS } from '../components/ui'
import { useTheme } from '../context/ThemeContext'
import { SAMPLES, isSampleOrEmpty, isSampleTitle } from './samples'

const DRAFT_KEY = 'ra.draft'

function loadDraft() {
  try {
    return JSON.parse(localStorage.getItem(DRAFT_KEY)) || null
  } catch {
    return null
  }
}

export default function NewReviewPage() {
  const navigate = useNavigate()
  const { theme } = useTheme()
  const [draft] = useState(loadDraft)
  const [language, setLanguage] = useState(draft?.language || 'javascript')
  const [code, setCode] = useState(draft?.code ?? SAMPLES.javascript.code)
  const [title, setTitle] = useState(draft ? draft.title || '' : SAMPLES.javascript.title)
  const [notes, setNotes] = useState(draft?.notes || '')
  const [meta, setMeta] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.meta().then(setMeta).catch(() => {})
  }, [])

  useEffect(() => {
    try {
      localStorage.setItem(DRAFT_KEY, JSON.stringify({ language, code, title, notes }))
    } catch { /* storage unavailable */ }
  }, [language, code, title, notes])

  const maxChars = meta?.maxCodeChars ?? 20000
  const tooLong = code.length > maxChars
  const lineCount = code ? code.split('\n').length : 0

  async function submit() {
    setBusy(true)
    setError(null)
    try {
      const review = await api.createReview({ language, code, title: title || undefined, notes: notes || undefined })
      try { localStorage.removeItem(DRAFT_KEY) } catch { /* ignore */ }
      navigate(`/reviews/${review.id}`)
    } catch (err) {
      setError(err)
      setBusy(false)
    }
  }

  function applySample(lang) {
    const sample = SAMPLES[lang]
    if (!sample) return
    setCode(sample.code)
    if (isSampleTitle(title)) setTitle(sample.title)
  }

  // Switching language swaps in that language's sample, unless the user has typed their own code.
  function changeLanguage(lang) {
    setLanguage(lang)
    if (isSampleOrEmpty(code)) applySample(lang)
  }

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1>New review</h1>
          <p className="muted">Paste code, pick its language and get a structured review in seconds.</p>
        </div>
        {meta && (
          <span className={`engine-chip ${meta.aiEnabled ? '' : 'engine-demo'}`} title="Active review engine">
            <span className="pulse" />{meta.aiEnabled ? meta.engine : 'Demo mode (no AI key)'}
          </span>
        )}
      </div>

      <div className="editor-layout">
        <div className="card editor-card">
          <div className="editor-toolbar">
            <select value={language} onChange={(e) => changeLanguage(e.target.value)} aria-label="Language">
              {(meta?.languages || Object.keys(LANGUAGE_LABELS)).map((l) => (
                <option key={l} value={l}>{LANGUAGE_LABELS[l] || l}</option>
              ))}
            </select>
            <button className="btn btn-ghost btn-sm" onClick={() => applySample(language)} type="button"
              title={`Load the ${LANGUAGE_LABELS[language] || language} demo snippet`}>Load sample</button>
            <button className="btn btn-ghost btn-sm" type="button"
              onClick={() => { setCode(''); if (isSampleTitle(title)) setTitle('') }}>Clear</button>
            <span className={`counter ${tooLong ? 'counter-over' : ''}`}>
              {lineCount} lines · {code.length.toLocaleString()} / {maxChars.toLocaleString()} chars
            </span>
          </div>
          <div className="editor-frame">
            <Editor
              height="100%"
              language={language === 'shell' ? 'shell' : language}
              value={code}
              onChange={(v) => setCode(v ?? '')}
              theme={theme === 'dark' ? 'vs-dark' : 'light'}
              options={{ minimap: { enabled: false }, fontSize: 14, scrollBeyondLastLine: false, tabSize: 2, automaticLayout: true }}
            />
          </div>
        </div>

        <aside className="card side-card">
          <label className="field">
            <span className="field-label">Title<em>Optional</em></span>
            <input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={120} placeholder="e.g. Login handler" />
          </label>
          <label className="field">
            <span className="field-label">Context for the reviewer<em>Optional</em></span>
            <textarea rows={5} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000}
              placeholder="What does this code do? Anything you're unsure about?" />
          </label>
          <ErrorNote error={error} />
          <button className="btn btn-primary btn-block" onClick={submit} disabled={busy || !code.trim() || tooLong}>
            {busy ? <><span className="spinner" /> Reviewing…</> : 'Run review'}
          </button>
          {tooLong && <p className="field-error">Shorten the code to {maxChars.toLocaleString()} characters or fewer.</p>}
          <p className="muted small">Your draft is saved in this browser until the review runs.</p>
        </aside>
      </div>
    </div>
  )
}
