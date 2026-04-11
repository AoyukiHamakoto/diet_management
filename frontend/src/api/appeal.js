import request from './request'

export function submitAppeal(data) {
  return request.post('/appeals', data)
}

export function getMyAppeals(params, axiosConfig = {}) {
  return request.get('/appeals/my', { params, ...axiosConfig })
}

export function getAppealDetail(id) {
  return request.get(`/appeals/${id}`)
}

export function getAdminAppeals(params) {
  return request.get('/admin/appeals', { params })
}

export function getAdminAppealDetail(id) {
  return request.get(`/admin/appeals/${id}`)
}

export function resolveAppeal(id, data) {
  return request.post(`/admin/appeals/${id}/resolve`, data || {})
}

export function rejectAppeal(id, data) {
  return request.post(`/admin/appeals/${id}/reject`, data)
}
