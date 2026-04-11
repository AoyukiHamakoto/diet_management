<template>
  <div class="post-detail-page">
    <PageBackBar fallback="/community" />
    <div v-if="loading" class="loading-wrap">
      <el-icon class="loading-icon"><Loading /></el-icon>
    </div>
    <template v-else-if="post">
      <div class="post-head">
        <h1 class="title">{{ post.title }}</h1>
        <div class="meta">
          <span>{{ authorNickname }}</span>
          <span>{{ formatTime(post.createTime) }}</span>
        </div>
        <div class="content">{{ post.content }}</div>
        <div class="actions">
          <el-button :type="liked ? 'primary' : 'default'" @click="onToggleLike">
            {{ liked ? '已赞' : '点赞' }} {{ likeCount }}
          </el-button>
        </div>
      </div>

      <div class="comments-section">
        <h3>评论</h3>
        <div class="composer">
          <el-input v-model="draft" type="textarea" :rows="3" placeholder="写评论…" />
          <el-button type="primary" style="margin-top: 8px" :loading="submitting" @click="submitTop">发送</el-button>
        </div>
        <div v-for="c in roots" :key="c.id" class="comment-thread">
          <div class="comment">
            <div class="c-meta">
              <span class="c-name">{{ c.nickname }}</span>
              <span class="c-time">{{ formatTime(c.createTime) }}</span>
            </div>
            <div class="c-body">{{ c.content }}</div>
            <div class="c-actions">
              <el-button link type="primary" size="small" @click="toggleCommentLike(c)">
                {{ c.liked ? '已赞' : '赞' }} {{ c.likeCount }}
              </el-button>
              <el-button link size="small" @click="toggleReply(c.id)">回复</el-button>
            </div>
            <div v-if="replyTo === c.id" class="reply-box">
              <el-input v-model="replyDraft[c.id]" type="textarea" :rows="2" placeholder="回复…" />
              <el-button size="small" type="primary" style="margin-top: 6px" @click="submitReply(c.id)">发送</el-button>
            </div>
            <div v-for="r in childrenOf(c.id)" :key="r.id" class="comment reply">
              <div class="c-meta">
                <span class="c-name">{{ r.nickname }}</span>
                <span class="c-time">{{ formatTime(r.createTime) }}</span>
              </div>
              <div class="c-body">{{ r.content }}</div>
              <div class="c-actions">
                <el-button link type="primary" size="small" @click="toggleCommentLike(r)">
                  {{ r.liked ? '已赞' : '赞' }} {{ r.likeCount }}
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!comments.length" description="还没有评论" />
      </div>
    </template>
    <el-empty v-else description="帖子不存在" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'

defineOptions({ name: 'PostDetail' })
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageBackBar from '../components/PageBackBar.vue'
import {
  fetchPostDetail,
  togglePostLike,
  fetchPostComments,
  submitPostComment,
  toggleCommentLike as apiToggleCommentLike
} from '../api/post'
import { logError, logInfo } from '../utils/logger'

const route = useRoute()
const loading = ref(true)
const post = ref(null)
const liked = ref(false)
const likeCount = ref(0)
const authorNickname = ref('')
const comments = ref([])
const draft = ref('')
const submitting = ref(false)
const replyTo = ref(null)
const replyDraft = ref({})

const roots = computed(() => comments.value.filter((c) => !c.parentId))

function childrenOf(parentId) {
  return comments.value.filter((c) => c.parentId === parentId)
}

function formatTime(t) {
  if (!t) return ''
  try {
    return new Date(t).toLocaleString('zh-CN')
  } catch {
    return String(t)
  }
}

async function loadDetail() {
  const id = route.params.id
  if (!id) {
    post.value = null
    loading.value = false
    return
  }
  const captured = String(id)
  loading.value = true
  try {
    const res = await fetchPostDetail(id)
    if (String(route.params.id || '') !== captured) return
    const d = res.data || {}
    post.value = d.post || null
    liked.value = !!d.liked
    authorNickname.value = d.authorNickname || '用户'
    likeCount.value = d.post?.likeCount ?? 0
    logInfo('POST', 'detail_ok', { id })
  } catch (e) {
    if (String(route.params.id || '') === captured) {
      post.value = null
      logError('POST', 'detail_fail', e?.message)
    }
  } finally {
    if (String(route.params.id || '') === captured) loading.value = false
  }
}

async function loadComments() {
  const id = route.params.id
  if (!id) {
    comments.value = []
    return
  }
  const captured = String(id)
  try {
    const res = await fetchPostComments(id)
    if (String(route.params.id || '') !== captured) return
    comments.value = Array.isArray(res.data) ? res.data : []
  } catch {
    if (String(route.params.id || '') === captured) comments.value = []
  }
}

async function onToggleLike() {
  const id = route.params.id
  try {
    const res = await togglePostLike(id)
    liked.value = !!res.data?.liked
    likeCount.value = res.data?.likeCount ?? likeCount.value
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function submitTop() {
  const id = route.params.id
  const text = (draft.value || '').trim()
  if (!text) return
  submitting.value = true
  try {
    await submitPostComment(id, { content: text, parentId: null })
    draft.value = ''
    await loadComments()
    ElMessage.success('已发送')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '发送失败')
  } finally {
    submitting.value = false
  }
}

function toggleReply(id) {
  replyTo.value = replyTo.value === id ? null : id
  if (!replyDraft.value[id]) replyDraft.value[id] = ''
}

async function submitReply(parentId) {
  const id = route.params.id
  const text = (replyDraft.value[parentId] || '').trim()
  if (!text) return
  try {
    await submitPostComment(id, { content: text, parentId })
    replyDraft.value[parentId] = ''
    replyTo.value = null
    await loadComments()
    ElMessage.success('已回复')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '发送失败')
  }
}

async function toggleCommentLike(c) {
  try {
    const res = await apiToggleCommentLike(c.id)
    c.liked = !!res.data?.liked
    c.likeCount = res.data?.likeCount ?? c.likeCount
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

watch(() => route.params.id, (id) => {
  if (!id) return
  loadDetail()
  loadComments()
})

onMounted(() => {
  loadDetail()
  loadComments()
})
</script>

<style scoped>
.post-detail-page {
  padding-bottom: 96px;
  max-width: 720px;
  margin: 0 auto;
}
.loading-wrap {
  text-align: center;
  padding: 40px;
}
.loading-icon {
  font-size: 36px;
}
.post-head {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.title {
  font-size: 1.25rem;
  margin: 0 0 12px;
  line-height: 1.4;
}
.meta {
  font-size: 13px;
  color: #909399;
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.content {
  white-space: pre-wrap;
  line-height: 1.7;
  color: #303133;
  font-size: 15px;
}
.actions {
  margin-top: 16px;
}
.comments-section {
  margin-top: 20px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.comments-section h3 {
  margin: 0 0 12px;
  font-size: 1rem;
}
.comment-thread {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  margin-top: 12px;
}
.comment-thread:first-of-type {
  border-top: none;
  padding-top: 0;
  margin-top: 0;
}
.comment.reply {
  margin-left: 16px;
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid #ebeef5;
}
.c-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 10px;
}
.c-name {
  font-weight: 500;
  color: #606266;
}
.c-body {
  margin-top: 6px;
  font-size: 14px;
  line-height: 1.5;
}
.c-actions {
  margin-top: 6px;
}
.reply-box {
  margin-top: 8px;
}
</style>
