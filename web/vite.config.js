import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development the API runs on :8080; proxying /api avoids CORS entirely.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
