import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { readFileSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

/** Load frontend/.env.properties (same idea as backend). */
function loadEnvProperties() {
  const file = resolve(__dirname, '.env.properties')
  if (!existsSync(file)) return {}
  const vars = {}
  for (const line of readFileSync(file, 'utf8').split(/\r?\n/)) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue
    const eq = trimmed.indexOf('=')
    if (eq === -1) continue
    vars[trimmed.slice(0, eq).trim()] = trimmed.slice(eq + 1).trim()
  }
  return vars
}

export default defineConfig(({ mode }) => {
  const fromProperties = loadEnvProperties()
  const fromFiles = loadEnv(mode, __dirname, 'VITE_')
  const env = { ...fromFiles, ...fromProperties }

  // Inject only VITE_* from .env.properties; keep import.meta.env.DEV/MODE/etc.
  const envDefine = Object.fromEntries(
    Object.entries(env)
      .filter(([key]) => key.startsWith('VITE_'))
      .map(([key, value]) => [`import.meta.env.${key}`, JSON.stringify(value)])
  )

  return {
    plugins: [react()],
    define: {
      ...envDefine,
      global: 'globalThis',
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ws': {
          target: 'http://localhost:8080',
          ws: true,
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'node',
      include: ['src/**/*.test.js'],
    },
  }
})
