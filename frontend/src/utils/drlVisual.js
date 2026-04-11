/**
 * Lightweight DRL parser for admin UI: extract rule blocks (name / when / then).
 * Display only; does not validate Drools syntax.
 */

export function parseDrlRules(content) {
  if (!content || !String(content).trim()) {
    return { header: '', rules: [] }
  }
  const text = String(content)
  const firstRule = text.search(/^\s*rule\s*"/m)
  const header = firstRule === -1 ? text.trim() : text.slice(0, firstRule).trim()

  const rest = firstRule === -1 ? '' : text.slice(firstRule)
  const rules = []

  const headerRe = /^\s*rule\s*"([^"]+)"\s*$/gm
  const positions = []
  let m
  while ((m = headerRe.exec(rest)) !== null) {
    positions.push({ name: m[1], start: m.index })
  }

  for (let i = 0; i < positions.length; i++) {
    const start = positions[i].start
    const end = i + 1 < positions.length ? positions[i + 1].start : rest.length
    const block = rest.slice(start, end)
    const whenM = block.match(/when\s*\n([\s\S]*?)\n\s*then\s*\n([\s\S]*?)\n\s*end\b/s)
    if (whenM) {
      rules.push({
        name: positions[i].name,
        whenText: whenM[1].trim(),
        thenText: whenM[2].trim()
      })
    } else {
      rules.push({
        name: positions[i].name,
        whenText: '',
        thenText: '',
        parseError: true
      })
    }
  }

  return { header, rules }
}
