// PostToolUse(Edit|Write): 방금 고친 파일 하나만 빠르게 검사합니다.
// 문제가 있으면 exit 2 로 Claude 에게 알려 바로 고치게 합니다.
import path from 'node:path'
import { exists, has, readInput, ROOT, run, tail } from './lib.mjs'

const input = await readInput()
const file = input.tool_input?.file_path
if (!file) process.exit(0)

const rel = path.relative(ROOT, path.resolve(ROOT, file)).split(path.sep).join('/')

function report(title, output) {
  process.stderr.write(`[lint-file] ${title}: ${rel}\n${tail(output, 40)}\n`)
  process.exit(2)
}

// 프론트엔드 TS/TSX → oxlint (node_modules 가 있을 때만)
if (/^frontend\/src\/.*\.(ts|tsx)$/.test(rel) && exists('frontend', 'node_modules', '.bin')) {
  const res = run('npx', ['--no-install', 'oxlint', path.relative('frontend', rel)], path.join(ROOT, 'frontend'))
  if (!res.ok && !res.missing) report('oxlint 경고/오류', res.output)
}

// FastAPI 파이썬 → ruff format + ruff check (uv 와 .venv 가 있을 때만)
if (/^fastapi\/.*\.py$/.test(rel) && exists('fastapi', '.venv') && has('uv')) {
  const cwd = path.join(ROOT, 'fastapi')
  const target = path.relative('fastapi', rel)
  run('uv', ['run', '--frozen', 'ruff', 'format', target], cwd)
  const res = run('uv', ['run', '--frozen', 'ruff', 'check', target], cwd)
  if (!res.ok && !res.missing) report('ruff 오류', res.output)
}

process.exit(0)
