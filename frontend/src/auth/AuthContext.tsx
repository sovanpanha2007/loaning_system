import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, ApiRequestError } from '../api/client'
import type { UserResponse } from '../api/types'

interface AuthContextValue {
  user: UserResponse | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api
      .get<UserResponse>('/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false))
  }, [])

  async function login(username: string, password: string) {
    const loggedInUser = await api.post<UserResponse>('/auth/login', { username, password })
    setUser(loggedInUser)
  }

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch (e) {
      if (!(e instanceof ApiRequestError)) throw e
    }
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
