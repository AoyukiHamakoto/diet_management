<template>
  <div class="admin-plan-detail" v-loading="loading">
    <el-card shadow="hover" v-if="detail">
      <template #header>
        <div class="header-row">
          <span>计划详情</span>
          <el-space>
            <el-button @click="goBack">返回</el-button>
            <el-button
              v-if="detail.auditStatus === 'PENDING'"
              type="success"
              @click="approve"
            >
              通过
            </el-button>
            <el-button
              v-if="detail.auditStatus === 'PENDING'"
              type="danger"
              @click="rejectDialogVisible = true"
            >
              驳回
            </el-button>
          </el-space>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户">{{ detail.user?.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划日期">{{ detail.planDate }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">
          <el-tag :type="statusType(detail.auditStatus)">{{ detail.auditStatus }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标">{{ detail.healthProfile?.target || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建议总热量">{{ detail.totalSuggestedCalories }}</el-descriptions-item>
        <el-descriptions-item label="预校验结果">
          <el-tag :type="detail.precheckPassed ? 'success' : 'danger'">
            {{ detail.precheckPassed ? '通过' : '需复核' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="预校验分数">{{ detail.precheckScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核备注">{{ detail.auditComment || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="Array.isArray(detail.precheckIssues) && detail.precheckIssues.length"
        :title="`预校验提示：${detail.precheckIssues.join('；')}`"
        type="warning"
        :closable="false"
        style="margin-top: 12px"
      />

      <el-divider />

      <el-table :data="detail.meals || []">
        <el-table-column label="餐次" width="120">
          <template #default="{ row }">
            {{ row.plan?.mealType || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="菜谱" min-width="180">
          <template #default="{ row }">
            {{ row.recipe?.title || '未匹配' }}
          </template>
        </el-table-column>
        <el-table-column label="建议热量" width="120">
          <template #default="{ row }">
            {{ row.plan?.suggestedCalories || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="计划状态" width="120">
          <template #default="{ row }">
            {{ row.plan?.status || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectDialogVisible" title="驳回计划" width="460px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="reject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPlanDetail, approvePlan, rejectPlan } from '../../api/admin'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref(null)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)

async function loadDetail() {
  loading.value = true
  try {
    const res = await getPlanDetail(route.params.id)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/plans')
}

async function approve() {
  await approvePlan(route.params.id)
  ElMessage.success('审核通过')
  loadDetail()
}

async function reject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  rejecting.value = true
  try {
    await rejectPlan(route.params.id, rejectReason.value)
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadDetail()
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

onMounted(loadDetail)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
