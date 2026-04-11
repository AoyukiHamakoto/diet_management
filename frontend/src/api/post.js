import request from './request'

export function fetchPublicPosts(page = 1, size = 10, keyword) {
  return request.get('/post/public', { params: { page, size, keyword } })
}

export function fetchMyPosts(params = {}) {
  // 社区页辅助数据：失败时不弹全局错误（避免与主列表抢提示）
  return request.get('/post/my', { params, silent: true })
}

export function createPost(payload) {
  return request.post('/post', payload)
}

export function fetchPostDetail(id) {
  return request.get(`/post/${id}`)
}

export function togglePostLike(id) {
  return request.post(`/post/${id}/like`)
}

export function fetchPostComments(id) {
  return request.get(`/post/${id}/comments`)
}

export function submitPostComment(id, payload) {
  return request.post(`/post/${id}/comments`, payload)
}

export function toggleCommentLike(commentId) {
  return request.post(`/post/comment/${commentId}/like`)
}
