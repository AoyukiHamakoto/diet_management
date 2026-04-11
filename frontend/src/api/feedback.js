import request from './request'

const FEEDBACK_TAGS = [
  { value: 'TASTE_GOOD', label: '口味不错' },
  { value: 'TOO_LIGHT', label: '太淡了' },
  { value: 'TOO_SALTY', label: '太咸' },
  { value: 'TOO_OILY', label: '太油腻' },
  { value: 'TOO_MUCH', label: '量太大' },
  { value: 'TOO_LITTLE', label: '量不够' },
  { value: 'NOT_FRESH', label: '食材不新鲜' },
  { value: 'HARD_TO_COOK', label: '不好做' }
]

export { FEEDBACK_TAGS }

export function submitFeedback(payload) {
  return request.post('/feedback', payload)
}

export function getFeedbackHistory(page = 1, size = 10) {
  return request.get('/feedback/history', { params: { page, size } })
}

/** 简易执行反馈简报（满 3 天执行记录后 eligible=true） */
export function getFeedbackReport() {
  return request.get('/feedback/report')
}
