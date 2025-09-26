import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8100',
        changeOrigin: true,
        secure: false,
      },
      '/deployurl': {
        target: 'http://localhost:80',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/deployurl/, ''),
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            // 移除可能阻止iframe加载的安全头
            delete proxyRes.headers['content-security-policy']
            delete proxyRes.headers['x-frame-options']
            delete proxyRes.headers['cross-origin-embedder-policy']
          })
        },
      },
    },
  },
})
