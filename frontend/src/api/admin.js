import request from './request'

export function getDashboardStats() {
  return request.get('/admin/dashboard')
}

export function getUserGrowthChart(days = 7) {
  return request.get('/admin/dashboard/chart/user-growth', { params: { days } })
}

export function getPopularRecipes(limit = 10) {
  return request.get('/admin/dashboard/chart/popular-recipes', { params: { limit } })
}

export function getTargetDistribution() {
  return request.get('/admin/dashboard/chart/target-distribution')
}

export function listUsers(params) {
  return request.get('/admin/users', { params })
}

export function getUserDetail(id) {
  return request.get(`/admin/users/${id}`)
}

export function resetUserPassword(id, newPassword) {
  return request.put(`/admin/users/${id}/reset-password`, { newPassword })
}

export function disableUser(id) {
  return request.put(`/admin/users/${id}/disable`)
}

export function enableUser(id) {
  return request.put(`/admin/users/${id}/enable`)
}

export function getRetentionRates() {
  return request.get('/admin/statistics/retention')
}

export function getPlanCompletion(days = 30) {
  return request.get('/admin/statistics/plan-completion', { params: { days } })
}

export function getPopularAllergies(limit = 10) {
  return request.get('/admin/statistics/popular-allergies', { params: { limit } })
}

export function getRulesList() {
  return request.get('/admin/rules')
}

export function getRuleContent(name) {
  return request.get('/admin/rules/content', { params: { name } })
}

export function saveRule(body) {
  return request.put('/admin/rules', body)
}

export function validateRule(body) {
  return request.post('/admin/rules/validate', body)
}

export function listRuleBackups(name) {
  return request.get('/admin/rules/backups', { params: { name } })
}

export function restoreRule(body) {
  return request.post('/admin/rules/restore', body)
}

export function listPlans(params) {
  return request.get('/admin/plans', { params })
}

export function getPlanDetail(id) {
  return request.get(`/admin/plans/${id}`)
}

export function approvePlan(id) {
  return request.post(`/admin/plans/${id}/approve`)
}

export function rejectPlan(id, reason) {
  return request.post(`/admin/plans/${id}/reject`, { reason })
}

export function listOrders(params) {
  return request.get('/admin/orders', { params })
}

export function updateOrderStatus(id, status) {
  return request.put(`/admin/orders/${id}/status`, { status })
}

export function listPendingPosts(params) {
  return request.get('/admin/post/pending', { params })
}

export function approvePost(id) {
  return request.put(`/admin/post/${id}/approve`, {})
}

export function rejectPost(id, reason) {
  return request.put(`/admin/post/${id}/reject`, { reason })
}
