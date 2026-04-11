import request from './request'

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}

export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/user/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getHealthProfile() {
  return request.get('/user/health-profile')
}

export function updateHealthProfile(data) {
  return request.put('/user/health-profile', data)
}

export function clearHealthProfile() {
  return request.post('/user/health-profile/clear', { confirm: true })
}

export function createBodyLog(weightOrPayload, logDate) {
  // Backward-compatible: support both createBodyLog(65.2, '2025-02-01')
  // and createBodyLog({ weight: 65.2, logDate: '2025-02-01' }).
  if (weightOrPayload && typeof weightOrPayload === 'object' && !Array.isArray(weightOrPayload)) {
    return request.post('/user/body-log', weightOrPayload)
  }
  return request.post('/user/body-log', { weight: weightOrPayload, logDate })
}

export function getBodyLogChart(days = 30) {
  return request.get('/user/body-log/chart', { params: { days } })
}

export function getDietTags() {
  return request.get('/user/diet-tags')
}

export function getPreferences() {
  return request.get('/user/preferences')
}

export function resetPreferences() {
  return request.post('/user/preferences/reset')
}

export function getNotifications() {
  return request.get('/user/notifications')
}

export function markNotificationRead(id) {
  return request.put(`/user/notifications/${id}/read`)
}
