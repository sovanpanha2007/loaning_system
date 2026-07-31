import type { ApiError } from './types'

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : null
}

export class ApiRequestError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  // CSRF cookie only exists once a request has been made (it's set lazily by the backend's
  // CsrfCookieFilter) — GET requests are exempt from the check so this is only needed here
  // for mutating requests, by which point at least one prior GET has already primed it.
  const csrfToken = readCookie('XSRF-TOKEN')
  if (csrfToken && method !== 'GET') {
    headers['X-XSRF-TOKEN'] = csrfToken
  }

  const response = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') ?? ''
  const data = contentType.includes('application/json') ? await response.json() : undefined

  if (!response.ok) {
    const message = (data as ApiError | undefined)?.error ?? `Request failed (${response.status})`
    throw new ApiRequestError(response.status, message)
  }

  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body ?? {}),
}
