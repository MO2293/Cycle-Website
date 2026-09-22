import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // In development the API runs on :8080 and this app on :5173 — different
      // origins, which the browser would normally block. Proxying /api through
      // the dev server makes every request same-origin, so CORS never comes up
      // locally and relative image URLs like /api/items/1/image just work.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
