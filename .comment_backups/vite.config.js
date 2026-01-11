import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { visualizer } from 'rollup-plugin-visualizer'

const enableAnalyze = process.env.ANALYZE === 'true'

export default defineConfig({
  plugins: [react(), enableAnalyze && visualizer({ filename: 'dist/stats.html', template: 'treemap', gzipSize: true, brotliSize: true })].filter(Boolean),
  server: {
    allowedHosts: true,
  },
  build: {
    target: 'es2019',
    sourcemap: false,
    cssCodeSplit: true,
    minify: 'esbuild',
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom', 'framer-motion', 'animejs', 'axios'],
        },
      },
    },
  },
  optimizeDeps: {
    include: ['react', 'react-dom', 'framer-motion', 'animejs', 'axios'],
  },
})
