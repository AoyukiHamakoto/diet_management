<template>
  <div class="admin-plans">
    <el-card shadow="hover">
      <template #header>
        <div class="header-row">
          <span>计划审核</span>
          <el-space>
            <el-select v-model="filters.auditStatus" placeholder="审核状态" clearable style="width: 150px">
              <el-option label="待审核" value="PENDING" />
              <el-option label="自动通过" value="AUTO" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-date-picker
              v-model="filters.planDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="计划日期"
              style="width: 160px"
              clearable
            />
            <el-button type="primary" @click="loadPlans">查询</el-button>
          </el-space>
        </div>
      </template>

      <el-table :data="records" v-loading="loading">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar || ''">{{ (row.nickname || '-').slice(0,1) }}</el-avatar>
              <span>{{ row.nickname || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="planDate" label="计划日期" width="120" />
        <el-table-column prop="target" label="目标" width="130" />
        <el-table-column prop="mealCount" label="餐次数" width="90" />
        <el-table-column prop="totalSuggestedCalories" label="建议总热量" width="130" />
        <el-table-column label="预校验" width="180">
          <template #default="{ row }">
            <el-tag :type="row.precheckPassed ? 'success' : 'danger'">
              {{ row.precheckPassed ? '通过' : '需复核' }}
            </el-tag>
            <span style="margin-left: 8px">分数 {{ row.precheckScore ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.auditStatus)">{{ row.auditStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="success" @click="approve(row.id)">通过</el-button>
            <el-button v-if="row.auditStatus === 'PENDING'" link type="danger" @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :total="pagination.total"
          :page-size="pagination.size"
          :current-page="pagination.page"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="驳回计划" width="460px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入驳回原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPlans, approvePlan, rejectPlan } from '../../api/admin'

const router = useRouter()
const loading = ref(false)
const records = ref([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const filters = reactive({ auditStatus: 'PENDING', planDate: '' })

const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)
const rejecting = ref(false)

async function loadPlans() {
  loading.value = true
  try {
    const res = await listPlans({
      page: pagination.page,
      size: pagination.size,
      auditStatus: filters.auditStatus || undefined,
      planDate: filters.planDate || undefined
    })
    const data = res.data || {}
    records.value = data.records || []
    pagination.total = data.total || 0
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  pagination.page = page
  loadPlans()
}

function viewDetail(id) {
  router.push(`/admin/plans/${id}`)
}

async function approve(id) {
  await approvePlan(id)
  ElMessage.success('审核通过')
  loadPlans()
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  rejecting.value = true
  try {
    await rejectPlan(rejectTarget.value.id, rejectReason.value)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadPlans()
  } finally {
    rejecting.value = false
  }
}

function statusType(status) {
  return {
    PENDING: 'warning',
    AUTO: 'info',
    APPROVED: 'success',
    REJECTED: 'danger'
  }[status] || 'info'
}

onMounted(loadPlans)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
