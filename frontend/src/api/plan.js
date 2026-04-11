import request from './request'

export function getDailyPlan(date) {
  return request.get('/plan/daily', { params: { date } })
}

export function getReminder() {
  return request.get('/plan/reminder')
}

export function getProactivePrompt() {
  return request.get('/plan/proactive-prompt')
}

export function generatePlan(date) {
  return request.post('/plan/generate', null, {
    params: { date },
    timeout: 60000
  })
}

export function generateWeekPlan(startDate) {
  return request.post('/plan/generate-week', null, {
    params: { startDate },
    timeout: 60000
  })
}

export function completePlan(id) {
  return request.put(`/plan/${id}/complete`)
}

export function skipPlan(id) {
  return request.put(`/plan/${id}/skip`)
}

export function replacePlan(id) {
  return request.put(`/plan/${id}/replace`)
}

export function markOutEat(id, note) {
  return request.put(`/plan/${id}/out-eat`, { note })
}

export function getWeekSummary(startDate) {
  return request.get('/plan/week', { params: { startDate } })
}
