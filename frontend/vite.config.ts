import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // The SPA and the API are different origins in development. Proxying keeps the app talking to
    // same-origin /api paths, so no CORS configuration exists to drift from production.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
