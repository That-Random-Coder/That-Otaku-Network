import fs from 'fs'
import path from 'path'

const __filename = decodeURIComponent(new URL(import.meta.url).pathname.replace(/^\//, ''))
const __dirname = path.dirname(__filename)
const ROOT = path.resolve(__dirname, '..')
const BACKUP_DIR = path.join(ROOT, '.comment_backups')
const IGNORED_DIRS = ['.git', 'node_modules', 'dist', '.vite', 'public/build', '.comment_backups']
const TARGET_EXTS = new Set(['.js', '.jsx', '.ts', '.tsx', '.css', '.html', '.json'])

function ensureDir(p) {
  if (!fs.existsSync(p)) fs.mkdirSync(p, { recursive: true })
}

function shouldIgnore(dir) {
  return IGNORED_DIRS.some((d) => dir.includes(path.sep + d + path.sep) || dir.endsWith(path.sep + d))
}

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const e of entries) {
    const full = path.join(dir, e.name)
    if (e.isDirectory()) {
      if (shouldIgnore(full)) continue
      walk(full)
    } else if (e.isFile()) {
      const ext = path.extname(e.name).toLowerCase()
      if (TARGET_EXTS.has(ext)) {
        processFile(full)
      }
    }
  }
}

function backupFile(file) {
  const rel = path.relative(ROOT, file)
  const dest = path.join(BACKUP_DIR, rel)
  ensureDir(path.dirname(dest))
  fs.copyFileSync(file, dest)
}

function processFile(file) {
  try {
    const text = fs.readFileSync(file, 'utf8')
    let out = text


    backupFile(file)


    out = out.replace(/\{\/\*[\s\S]*?\*\/\}/g, '')


    out = out.replace(/\/\*[\s\S]*?\*\//g, '')


    out = out.replace(



    out = out.replace(/(^|[^:\\])\/\/.*$/gm, '$1')


    out = out.replace(/[ \t]+$/gm, '')


    out = out.replace(/\n{3,}/g, '\n\n')

    if (out !== text) fs.writeFileSync(file, out, 'utf8')
    console.log('Processed:', file)
  } catch (e) {
    console.error('Failed to process', file, e)
  }
}

ensureDir(BACKUP_DIR)
walk(ROOT)
console.log('Done. Backups placed in', BACKUP_DIR)
