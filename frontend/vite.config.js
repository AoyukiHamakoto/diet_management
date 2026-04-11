import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const loadPwaPlugin = async () => {
  try {
    const { VitePWA } = await import('vite-plugin-pwa')
    return VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: '健身饮食助手',
        short_name: '饮食助手',
        description: '智能健身饮食规划，科学营养搭配',
        theme_color: '#1a5f4a',
        background_color: '#f5f7fa',
        display: 'standalone',
        orientation: 'portrait-primary',
        scope: '/',
        start_url: '/',
        icons: [
          { src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
          { src: '/favicon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'maskable' }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /^https?:\/\/[^/]*\/api\/.*/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              networkTimeoutSeconds: 10,
              expiration: { maxEntries: 100, maxAgeSeconds: 60 * 60 * 24 },
              cacheableResponse: { statuses: [0, 200] }
            }
          }
        ]
      }
    })
  } catch {
    console.log('vite-plugin-pwa not installed, PWA features disabled')
    return null
  }
}

export default defineConfig(async () => {
  const enablePwa = process.env.ENABLE_PWA === 'true'
  const pwaPlugin = enablePwa ? await loadPwaPlugin() : null

  return {
    plugins: [vue(), pwaPlugin].filter(Boolean),
    build: {
      modulePreload: {
        polyfill: false
      }
    },
    server: {
      port: 3000,
      strictPort: true,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
