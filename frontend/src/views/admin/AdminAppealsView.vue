<template>
  <div class="admin-appeals">
    <div class="toolbar">
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 140px" @change="loadData">
        <el-option label="待处理" value="PENDING" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已通过" value="RESOLVED" />
        <el-option label="已拒绝" value="REJECTED" />
      </el-select>
    </div>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ targetTypeLabel(row.targetType) }}</template>
      </el-table-column>
      <el-table-column label="对象摘要" min-width="160">
        <template #default="{ row }">{{ row.targetSummary || '—' }}</template>
      </el-table-column>
      <el-table-column label="申诉理由" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ (row.reason || '').slice(0, 80) }}{{ (row.reason || '').length > 80 ? '…' : '' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/admin/appeals/${row.id}`)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :current-page="page"
        :page-size="size"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminAppeals } from '../../api/appeal'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterStatus = ref('PENDING')

function statusLabel(s) {
  return { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已通过', REJECTED: '已拒绝' }[s] || s
}

function statusType(s) {
  return { PENDING: 'warning', PROCESSING: 'info', RESOLVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

function formatTime(str) {
  if (!str) return '—'
  try {
    return new Date(str).toLocaleString('zh-CN')
  } catch {
    return str
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminAppeals({ page: page.value, size: size.value, status: filterStatus.value })
    list.value = res.data?.records || []
    total.value = res.data?.total ?? 0
  } finally {
    loading.value = false
  }
}

function targetTypeLabel(t) {
  return { MEAL_PLAN: '计划', RECIPE: '菜谱', POST: '帖子' }[t] || t || '—'
}

function onPageChange(p) {
  page.value = p
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.admin-appeals {
  padding: 16px;
}

.toolbar {
  margin-bottom: 16px;
}

.pager {
  margin-top: 16px;
}
</style>
