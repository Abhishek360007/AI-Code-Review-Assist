import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import Logo from '../components/Logo'
import { ErrorNote } from '../components/ui'
import { useAuth } from '../context/AuthContext'

const EMPTY = { login: '', username: '', email: '', displayName: '', password: '' }

export default function AuthPage({ mode }) {
  const isSignup = mode === 'signup'
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [form, setForm] = useState(EMPTY)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }))
  const fieldError = (key) => error?.fieldErrors?.[key]

  async function onSubmit(e) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (isSignup) {
        await register({
          username: form.username, email: form.email, password: form.password,
          displayName: form.displayName || undefined,
        })
      } else {
        await login(form.login, form.password)
      }
      navigate(location.state?.from || '/', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-layout">
      <section className="auth-hero">
        <div className="brand brand-lg"><Logo size={36} /><span>Review<b>Assist</b></span></div>
        <h1>Ship cleaner code with a reviewer that never gets tired.</h1>
        <ul className="hero-points">
          <li><span className="dot sev-critical-bg" />Findings ranked by severity, from critical to info</li>
          <li><span className="dot sev-medium-bg" />Exact line numbers, with a concrete fix for every issue</li>
          <li><span className="dot sev-low-bg" />A full corrected version of your code, shown as a diff</li>
          <li><span className="dot brand-bg" />History and insights to track your progress over time</li>
        </ul>
      </section>

      <section className="auth-panel">
        <form className="card auth-card" onSubmit={onSubmit} noValidate>
          <h2>{isSignup ? 'Create your account' : 'Welcome back'}</h2>
          <p className="muted">{isSignup ? 'It takes less than a minute.' : 'Sign in to run and review your code reviews.'}</p>

          {isSignup ? (
            <>
              <Field label="Username" error={fieldError('username')}>
                <input value={form.username} onChange={set('username')} autoComplete="username" required minLength={3} />
              </Field>
              <Field label="Email" error={fieldError('email')}>
                <input type="email" value={form.email} onChange={set('email')} autoComplete="email" required />
              </Field>
              <Field label="Display name" hint="Optional" error={fieldError('displayName')}>
                <input value={form.displayName} onChange={set('displayName')} autoComplete="name" />
              </Field>
            </>
          ) : (
            <Field label="Username or email">
              <input value={form.login} onChange={set('login')} autoComplete="username" required autoFocus />
            </Field>
          )}

          <Field label="Password" hint={isSignup ? 'At least 8 characters' : undefined} error={fieldError('password')}>
            <input type="password" value={form.password} onChange={set('password')}
              autoComplete={isSignup ? 'new-password' : 'current-password'} required />
          </Field>

          {!error?.fieldErrors || Object.keys(error.fieldErrors).length === 0 ? <ErrorNote error={error} /> : null}

          <button className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Please wait…' : isSignup ? 'Create account' : 'Sign in'}
          </button>

          <p className="muted center">
            {isSignup ? <>Already have an account? <Link to="/signin">Sign in</Link></>
              : <>New here? <Link to="/signup">Create an account</Link></>}
          </p>
        </form>
      </section>
    </div>
  )
}

function Field({ label, hint, error, children }) {
  return (
    <label className={`field ${error ? 'field-invalid' : ''}`}>
      <span className="field-label">{label}{hint && <em>{hint}</em>}</span>
      {children}
      {error && <span className="field-error">{error}</span>}
    </label>
  )
}
