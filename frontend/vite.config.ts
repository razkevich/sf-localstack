import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/',
  server: {
    proxy: {
      '/services': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/reset': 'http://localhost:8080',
    }
  },
})
