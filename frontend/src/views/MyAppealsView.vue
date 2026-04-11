<template>
  <div class="my-appeals-page">
    <PageBackBar fallback="/mine" />
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="4" /></div>
    <template v-else>
      <div v-if="list.length === 0" class="empty-wrap">
        <el-empty description="暂无申诉记录" />
      </div>
      <div v-else class="appeal-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="appeal-card"
          @click="openDetail(item.id)"
        >
          <div class="card-header">
            <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
            <span class="time">{{ formatTime(item.createTime) }}</span>
          </div>
          <div class="card-body">
            <div class="target-type">{{ targetTypeLabel(item.targetType) }}</div>
            <div class="target-summary">{{ targetSummaryText(item) }}</div>
            <div class="reason-preview">{{ (item.reason || '').slice(0, 60) }}{{ (item.reason || '').length > 60 ? '…' : '' }}</div>
          </div>
        </div>
      </div>
    <el-dialog v-model="detailVisible" title="申诉详情" width="90%" style="max-width: 480px">
      <div v-if="detail">
        <p><strong>申诉对象：</strong>{{ targetSummaryText(detail) }}</p>
        <p><strong>申诉理由：</strong>{{ detail.reason }}</p>
        <p><strong>状态：</strong><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></p>
        <p v-if="detail.adminComment"><strong>处理意见：</strong>{{ detail.adminComment }}</p>
      </div>
    </el-dialog>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="size"
          :total="total"
          @current-change="onPageChange"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyAppeals, getAppealDetail } from '../api/appeal'
import PageBackBar from '../components/PageBackBar.vue'

const loading = ref(true)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const detailVisible = ref(false)
const detail = ref(null)

async function openDetail(id) {
  try {
    const res = await getAppealDetail(id)
    detail.value = res.data
    detailVisible.value = true
  } catch (_) {}
}

function statusLabel(s) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已通过', REJECTED: '已拒绝' }[s] || s || '—'
}

function statusType(s) {
  return { PENDING: 'warning', PROCESSING: 'info', RESOLVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

function targetTypeLabel(t) {
  return { MEAL_PLAN: '计划', RECIPE: '菜谱', POST: '帖子' }[t] || t || '—'
}

function targetSummaryText(item) {
  const t = item.targetSummary
  if (!t) return '—'
  if (item.targetType === 'RECIPE' && t.title) return t.title
  if (item.targetType === 'MEAL_PLAN' && t.planDate) return `计划 ${t.planDate} ${t.mealType || ''}`
  if (item.targetType === 'POST' && t.title) return t.title
  return '—'
}

function formatTime(str) {
  if (!str) return '—'
  try {
    return new Date(str).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return str
  }
}

function onPageChange(p) {
  page.value = p
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMyAppeals({ page: page.value, size: size.value })
    list.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.my-appeals-page {
  padding: 16px;
}

.loading-wrap,
.empty-wrap {
  padding: 20px;
}

.appeal-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.appeal-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.time {
  font-size: 12px;
  color: #909399;
}

.target-type {
  font-size: 12px;
  color: #909399;
}

.target-summary {
  font-weight: 500;
  margin: 4px 0;
}

.reason-preview {
  font-size: 13px;
  color: #606266;
}

.pager {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
