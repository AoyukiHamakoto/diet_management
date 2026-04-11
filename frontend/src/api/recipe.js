import request from './request'

export function searchRecipes(params) {
  return request.get('/recipe/search', { params })
}

export function getRecommend(category = 'LUNCH') {
  return request.get('/recipe/recommend', { params: { category } })
}

export function getRecipe(id) {
  return request.get(`/recipe/${id}`)
}

export function getMyRecipes(params) {
  return request.get('/recipe/my', { params })
}

export function uploadRecipeImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/recipe/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function createRecipe(data) {
  return request.post('/recipe', data)
}

export function updateRecipe(id, data) {
  return request.put(`/recipe/${id}`, data)
}

export function deleteRecipe(id) {
  return request.delete(`/recipe/${id}`)
}

export function addToPlan(recipeId, mealType, planDate) {
  return request.post(`/recipe/${recipeId}/add-to-plan`, { mealType, planDate })
}

export function getPendingRecipes() {
  return request.get('/admin/recipe/pending')
}

export function approveRecipe(id) {
  return request.put(`/admin/recipe/${id}/approve`)
}

export function rejectRecipe(id, reason) {
  return request.put(`/admin/recipe/${id}/reject`, { reason })
}

export function getAdminRecipeList(params) {
  return request.get('/admin/recipe/list', { params })
}

/** 管理员删除菜谱（逻辑删除），需 ADMIN 角色（使用 POST，避免部分环境拦截 DELETE） */
export function adminDeleteRecipe(id) {
  return request.post(`/admin/recipe/${id}/delete`)
}
