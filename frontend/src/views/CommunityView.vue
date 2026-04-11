<template>
  <div class="community-page">
    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索帖子" style="max-width: 280px" @clear="load" @keyup.enter="load" />
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button type="success" @click="openCreate">发布帖子</el-button>
    </div>
    <div v-if="myRejected.length" class="reject-list">
      <el-alert type="warning" show-icon :closable="false" title="你有被驳回的帖子，可发起申诉" />
      <div v-for="p in myRejected" :key="`reject-${p.id}`" class="reject-item">
        <div class="reject-title">{{ p.title }}</div>
        <div class="reject-reason">驳回原因：{{ p.rejectReason || '内容不符合规范' }}</div>
        <el-button type="danger" link @click="$router.push(`/appeal/submit?type=POST&id=${p.id}`)">提交申诉</el-button>
      </div>
    </div>
    <div v-loading="loading" class="post-list">
      <div
        v-for="row in rows"
        :key="row.post?.id"
        class="post-card"
        @click="goDetail(row.post?.id)"
      >
        <div class="title">{{ row.post?.title }}</div>
        <div class="excerpt">{{ excerpt(row.post?.content) }}</div>
        <div class="meta">
          <span>{{ row.authorNickname || '用户' }}</span>
          <span>{{ formatTime(row.post?.createTime) }}</span>
          <span>赞 {{ row.post?.likeCount ?? 0 }}</span>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && !rows.length" description="暂无帖子" />
    <div v-if="total > pageSize" class="pager-wrap">
      <el-pagination
        v-model:current-page="page"
        background
        layout="prev, pager, next, total"
        :page-size="pageSize"
        :total="total"
        @current-change="load"
      />
    </div>
    <el-dialog v-model="createVisible" title="发布帖子" width="90%" style="max-width: 520px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="createForm.title" maxlength="120" show-word-limit placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="createForm.content" type="textarea" :rows="6" maxlength="4000" show-word-limit placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPost, fetchMyPosts, fetchPublicPosts } from '../api/post'
import { getMyAppeals } from '../api/appeal'
import { logError, logInfo } from '../utils/logger'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const createVisible = ref(false)
const submitting = ref(false)
const createForm = ref({ title: '', content: '' })
const myRejected = ref([])

function excerpt(text) {
  if (!text) return ''
  const t = text.replace(/\s+/g, ' ').trim()
  return t.length > 120 ? `${t.slice(0, 120)}…` : t
}

function formatTime(t) {
  if (!t) return ''
  try {
    return new Date(t).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return String(t)
  }
}

function goDetail(id) {
  if (!id) return
  router.push(`/community/${id}`)
}

async function load() {
  loading.value = true
  try {
    const res = await fetchPublicPosts(page.value, pageSize.value, keyword.value || undefined)
    const raw = res.data?.records || []
    rows.value = raw
    total.value = res.data?.total || 0
    logInfo('COMMUNITY', 'list_ok', { count: rows.value.length })
  } catch (e) {
    logError('COMMUNITY', 'list_failed', e?.message)
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function loadMyRejected() {
  try {
    const res = await fetchMyPosts({ page: 1, size: 50 })
    const rows = res.data?.records || []
    /** 管理员已驳回申诉的帖子不再展示「可申诉」区块（帖子仍可能为 REJECTED） */
    let appealRejectedPostIds = new Set()
    try {
      const ar = await getMyAppeals({ page: 1, size: 100 }, { silent: true })
      const appeals = ar.data?.records || []
      appealRejectedPostIds = new Set(
        appeals
          .filter((a) => a.targetType === 'POST' && a.status === 'REJECTED')
          .map((a) => Number(a.targetId))
      )
    } catch {
      /* 申诉列表失败时不影响被驳回帖子提示 */
    }
    myRejected.value = rows.filter(
      (p) => p?.status === 'REJECTED' && !appealRejectedPostIds.has(Number(p.id))
    )
  } catch {
    myRejected.value = []
  }
}

function openCreate() {
  createForm.value = { title: '', content: '' }
  createVisible.value = true
}

async function submitCreate() {
  if (!createForm.value.title?.trim() || !createForm.value.content?.trim()) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  submitting.value = true
  try {
    await createPost({
      title: createForm.value.title.trim(),
      content: createForm.value.content.trim()
    })
    ElMessage.success('帖子已提交，等待管理员审核')
    createVisible.value = false
    await loadMyRejected()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function refreshAll() {
  await load()
  await loadMyRejected()
}

/** keep-alive 下首次进入与从详情返回都会触发，避免重复 onMounted+onActivated */
onActivated(refreshAll)
</script>

<style scoped>
.community-page {
  padding-bottom: 88px;
  max-width: 720px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}
.reject-list {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.reject-item {
  background: #fff8f8;
  border: 1px solid #fde2e2;
  border-radius: 10px;
  padding: 10px 12px;
}
.reject-title {
  font-weight: 600;
  color: #303133;
}
.reject-reason {
  margin-top: 4px;
  font-size: 13px;
  color: #f56c6c;
}
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.post-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.15s;
}
.post-card:hover {
  transform: translateY(-1px);
}
.title {
  font-weight: 600;
  font-size: 1rem;
  margin-bottom: 8px;
  color: #303133;
}
.excerpt {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}
.meta {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.pager-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
