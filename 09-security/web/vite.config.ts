import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发代理：后端三组前缀 /auth /api /admin 都要转发到 8009，漏一条就会拿到 index.html
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8009',
      '/api': 'http://localhost:8009',
      '/admin': 'http://localhost:8009'
    }
  }
})
