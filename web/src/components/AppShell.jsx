import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import Logo from './Logo'

export default function AppShell() {
  const { user, logout } = useAuth()
  const { theme, toggle } = useTheme()

  return (
    <div className="shell">
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <Logo />
            <span>Review<b>Assist</b></span>
          </NavLink>
          <nav className="nav">
            <NavLink to="/" end>New review</NavLink>
            <NavLink to="/reviews">History</NavLink>
            <NavLink to="/insights">Insights</NavLink>
          </nav>
          <div className="topbar-actions">
            <button className="icon-btn" onClick={toggle} aria-label="Toggle colour theme" title="Toggle theme">
              {theme === 'dark' ? '☀' : '☾'}
            </button>
            <span className="avatar" title={user.email}>{user.displayName.slice(0, 1).toUpperCase()}</span>
            <button className="btn btn-ghost btn-sm" onClick={logout}>Sign out</button>
          </div>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
