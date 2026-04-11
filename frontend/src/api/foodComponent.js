import request from './request'

export function searchFoodComponents(params) {
  return request.get('/food-components/search', { params })
}

export function matchFoodComponents(ingredientName) {
  return request.get('/food-components/match', { params: { ingredientName: ingredientName || '' } })
}

export function getFoodComponent(id) {
  return request.get(`/food-components/${id}`)
}

export function calculateNutrition(ingredients) {
  return request.post('/food-components/calculate-nutrition', { ingredients })
}
