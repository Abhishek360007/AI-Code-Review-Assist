// Thin fetch wrapper: attaches the JWT, parses JSON and turns problem responses into Error objects.

const TOKEN_KEY = 'ra.token'
const API_BASE = import.meta.env.VITE_API_BASE || ''

export class ApiError extends Error {
  constructor(status, message, fieldErrors) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors || {}
  }
}

export const tokenStore = {
  get: () => {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set: (token) => {
    try { localStorage.setItem(TOKEN_KEY, token) } catch { /* storage unavailable */ }
  },
  clear: () => {
    try { localStorage.removeItem(TOKEN_KEY) } catch { /* storage unavailable */ }
  },
}

let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

async function request(method, path, body) {
  const headers = { Accept: 'application/json' }
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let res
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the API running?')
  }

  if (res.status === 204) return null
  const text = await res.text()
  const data = text ? safeJson(text) : null

  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    const message = data?.detail || data?.message || defaultMessage(res.status)
    throw new ApiError(res.status, message, data?.fieldErrors)
  }
  return data
}

function safeJson(text) {
  try { return JSON.parse(text) } catch { return null }
}

function defaultMessage(status) {
  if (status === 401) return 'Please sign in again.'
  if (status === 404) return 'Not found.'
  if (status >= 500) return 'The server had a problem. Please try again.'
  return `Request failed (${status}).`
}

export const api = {
  meta: () => request('GET', '/api/meta'),

  register: (payload) => request('POST', '/api/auth/register', payload),
  login: (login, password) => request('POST', '/api/auth/login', { login, password }),
  me: () => request('GET', '/api/auth/me'),

  createReview: (payload) => request('POST', '/api/reviews', payload),
  listReviews: ({ page = 0, size = 10, language = '', q = '' } = {}) => {
    const params = new URLSearchParams({ page, size })
    if (language) params.set('language', language)
    if (q) params.set('q', q)
    return request('GET', `/api/reviews?${params}`)
  },
  getReview: (id) => request('GET', `/api/reviews/${id}`),
  rerunReview: (id) => request('POST', `/api/reviews/${id}/rerun`),
  deleteReview: (id) => request('DELETE', `/api/reviews/${id}`),
  stats: () => request('GET', '/api/reviews/stats'),
}
