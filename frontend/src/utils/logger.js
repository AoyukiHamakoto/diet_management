const STORAGE_KEY = 'diet_app_logs_v1'
const MAX_LOGS = 500

function safeJsonParse(s, fallback) {
  try {
    return JSON.parse(s)
  } catch {
    return fallback
  }
}

function nowIso() {
  return new Date().toISOString()
}

function readLogs() {
  if (typeof localStorage === 'undefined') return []
  return safeJsonParse(localStorage.getItem(STORAGE_KEY) || '[]', [])
}

function writeLogs(rows) {
  if (typeof localStorage === 'undefined') return
  localStorage.setItem(STORAGE_KEY, JSON.stringify(rows.slice(-MAX_LOGS)))
}

export function clearAppLogs() {
  writeLogs([])
}

export function getAppLogs() {
  return readLogs()
}

export function logEvent(level, module, message, data) {
  const row = {
    time: nowIso(),
    level: String(level || 'INFO').toUpperCase(),
    module: String(module || 'APP').toUpperCase(),
    message: String(message || ''),
    data: data ?? null
  }

  // Console for dev
  const line = `[${row.module}] ${row.message}`
  if (row.level === 'ERROR') console.error(line, row.data)
  else if (row.level === 'WARN') console.warn(line, row.data)
  else console.log(line, row.data)

  // Persist for later debugging (AdminLogsView reads it)
  const rows = readLogs()
  rows.push(row)
  writeLogs(rows)
}

export const logInfo = (module, message, data) => logEvent('INFO', module, message, data)
export const logWarn = (module, message, data) => logEvent('WARN', module, message, data)
export const logError = (module, message, data) => logEvent('ERROR', module, message, data)

