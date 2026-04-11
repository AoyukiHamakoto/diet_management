<template>
  <div class="admin-appeal-detail">
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="5" /></div>
    <template v-else-if="detail">
      <el-descriptions title="申诉信息" :column="1" border>
        <el-descriptions-item label="申诉ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="对象类型">{{ targetTypeLabel(detail.targetType) }}</el-descriptions-item>
        <el-descriptions-item label="对象ID">{{ detail.targetId }}</el-descriptions-item>
        <el-descriptions-item label="申诉理由"><pre class="reason-pre">{{ detail.reason }}</pre></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.adminComment" label="处理意见">{{ detail.adminComment }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.targetDetail" class="target-detail">
        <h4>被申诉对象信息</h4>
        <el-descriptions :column="1" border>
          <el-descriptions-item v-for="(v, k) in detail.targetDetail" :key="k" :label="String(k)">{{ v }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div v-if="canHandle" class="actions">
        <el-input v-model="adminComment" type="textarea" :rows="3" placeholder="处理意见（拒绝时必填）" />
        <div class="btn-row">
          <el-button type="success" :loading="resolving" @click="handleResolve">通过申诉（恢复对象）</el-button>
          <el-button type="danger" :loading="rejecting" @click="handleReject">拒绝申诉</el-button>
        </div>
      </div>

      <el-button style="margin-top: 16px" @click="$router.push('/admin/appeals')">返回列表</el-button>
    </template>
    <el-empty v-else description="申诉不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminAppealDetail, resolveAppeal, rejectAppeal } from '../../api/appeal'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const detail = ref(null)
const adminComment = ref('')
const resolving = ref(false)
const rejecting = ref(false)

const canHandle = computed(() => {
  const s = detail.value?.status
  return s === 'PENDING' || s === 'PROCESSING'
})

function statusLabel(s) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已通过', REJECTED: '已拒绝' }[s] || s
}

function statusType(s) {
  return { PENDING: 'warning', PROCESSING: 'info', RESOLVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

function targetTypeLabel(t) {
  return { MEAL_PLAN: '计划', RECIPE: '菜谱', POST: '帖子' }[t] || t || '—'
}

async function loadDetail() {
  loading.value = true
  try {
    const res = await getAdminAppealDetail(route.params.id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleResolve() {
  resolving.value = true
  try {
    await resolveAppeal(route.params.id, { adminComment: adminComment.value || undefined })
    ElMessage.success('已通过申诉，相关对象已恢复')
    loadDetail()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    resolving.value = false
  }
}

async function handleReject() {
  if (!adminComment.value?.trim()) {
    ElMessage.warning('拒绝申诉必须填写处理意见')
    return
  }
  try {
    await ElMessageBox.confirm('确认拒绝该申诉？', '提示', { type: 'warning' })
  } catch {
    return
  }
  rejecting.value = true
  try {
    await rejectAppeal(route.params.id, { adminComment: adminComment.value })
    ElMessage.success('已拒绝申诉')
    loadDetail()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    rejecting.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.admin-appeal-detail {
  padding: 16px;
  max-width: 720px;
}

.loading-wrap {
  padding: 20px;
}

.reason-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.target-detail {
  margin-top: 24px;
}

.target-detail h4 {
  margin-bottom: 12px;
}

.actions {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.btn-row {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}
</style>
