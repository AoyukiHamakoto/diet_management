<template>
  <div class="admin-recipes">
    <h2>菜谱审核管理</h2>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="待审核" name="pending">
        <el-table :data="pendingList" v-loading="loading" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="菜名" min-width="150" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="cookingTime" label="时长(分)" width="100" />
          <el-table-column prop="createTime" label="提交时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="success" size="small" @click="handleApprove(row)">通过</el-button>
              <el-button type="danger" size="small" @click="showReject(row)">拒绝</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="全部" name="all">
        <el-table :data="allList" v-loading="loading" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="菜名" min-width="150" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectDialogVisible" title="拒绝菜谱" width="400px">
      <el-input v-model="rejectReason" type="textarea" placeholder="请输入拒绝原因" :rows="3" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="handleReject">确定拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getPendingRecipes, approveRecipe, rejectRecipe, getAdminRecipeList } from '../api/recipe'
import { ElMessage } from 'element-plus'

const activeTab = ref('pending')
const pendingList = ref([])
const allList = ref([])
const loading = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)
const rejectTarget = ref(null)

function statusLabel(s) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[s] || s
}

function statusType(s) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info'
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingRecipes()
    pendingList.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  loading.value = true
  try {
    const res = await getAdminRecipeList({ page: 1, size: 50 })
    allList.value = res.data?.records || []
  } finally {
    loading.value = false
  }
}

async function handleApprove(row) {
  try {
    await approveRecipe(row.id)
    ElMessage.success('已通过')
    loadPending()
    loadAll()
  } catch (_) {}
}

function showReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function handleReject() {
  if (!rejectReason.value?.trim()) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  rejecting.value = true
  try {
    await rejectRecipe(rejectTarget.value.id, rejectReason.value)
    ElMessage.success('已拒绝')
    rejectDialogVisible.value = false
    loadPending()
    loadAll()
  } finally {
    rejecting.value = false
  }
}

watch(activeTab, (v) => {
  if (v === 'pending') loadPending()
  else loadAll()
})

onMounted(() => {
  loadPending()
})
</script>

<style scoped>
.admin-recipes {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
}

.admin-recipes h2 {
  margin-bottom: 20px;
  font-size: 1.25rem;
}
</style>
