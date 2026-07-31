import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Proxies /api to the Spring Boot backend so the browser sees same-origin requests during
// dev. This matters beyond convenience: the session cookie defaults to SameSite=Lax, which
// browsers refuse to send on cross-origin fetch()/XHR (unlike curl, which ignores SameSite
// entirely) — going through this proxy is what makes cookie-based auth actually work from a
// real browser tab, not just from curl.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
