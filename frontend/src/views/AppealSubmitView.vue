<template>
  <div class="appeal-submit-page">
    <PageBackBar fallback="/mine" />
    <div v-if="loading" class="loading-wrap"><el-icon class="is-loading"><Loading /></el-icon></div>
    <template v-else-if="targetSummary">
      <h3 class="section-title">申诉对象</h3>
      <div class="target-card">
        <div class="target-type">{{ targetTypeLabel }}</div>
        <div class="target-info">{{ targetSummary }}</div>
        <div v-if="rejectReason" class="reject-reason">拒绝原因：{{ rejectReason }}</div>
      </div>
      <el-form label-position="top" class="form">
        <el-form-item label="申诉理由（10-500字）" required>
          <el-input
            v-model="reason"
            type="textarea"
            :rows="5"
            placeholder="请说明您的申诉理由"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-button type="primary" size="large" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
          提交申诉
        </el-button>
      </el-form>
    </template>
    <el-empty v-else description="无效的申诉对象或无权申诉" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { submitAppeal } from '../api/appeal'
import { getRecipe } from '../api/recipe'
import { getDailyPlan } from '../api/plan'
import { fetchPostDetail } from '../api/post'
import { useUserStore } from '../stores/user'
import PageBackBar from '../components/PageBackBar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const submitting = ref(false)
const targetSummary = ref('')
const rejectReason = ref('')
const reason = ref('')

const targetType = computed(() => route.query.type || '')
const targetId = computed(() => (route.query.id ? Number(route.query.id) : null))

const targetTypeLabel = computed(() => {
  if (targetType.value === 'MEAL_PLAN') return '饮食计划'
  if (targetType.value === 'RECIPE') return '菜谱'
  if (targetType.value === 'POST') return '帖子'
  return ''
})

const canSubmit = computed(() => reason.value && reason.value.trim().length >= 10 && reason.value.trim().length <= 500)

async function loadTarget() {
  if (!targetType.value || !targetId.value) {
    loading.value = false
    return
  }
  try {
    if (targetType.value === 'RECIPE') {
      const res = await getRecipe(targetId.value)
      const r = res.data
      if (r && String(r.creatorId) === String(userStore.userId) && r.status === 'REJECTED') {
        targetSummary.value = r.title || '菜谱'
        rejectReason.value = r.rejectReason || ''
      }
    } else if (targetType.value === 'MEAL_PLAN') {
      const res = await getDailyPlan(new Date().toISOString().slice(0, 10))
      const meals = res.data?.meals || []
      const plan = meals.find(m => m.plan?.id === targetId.value)?.plan
      if (plan && String(plan.userId) === String(userStore.userId) && plan.auditStatus === 'REJECTED') {
        targetSummary.value = `计划 ${plan.planDate} ${plan.mealType || ''}`
        rejectReason.value = plan.auditComment || ''
      } else {
        targetSummary.value = '计划（ID: ' + targetId.value + '）'
        rejectReason.value = ''
      }
    } else if (targetType.value === 'POST') {
      const res = await fetchPostDetail(targetId.value)
      const p = res.data?.post
      if (p && String(p.userId) === String(userStore.userId) && p.status === 'REJECTED') {
        targetSummary.value = p.title || '帖子'
        rejectReason.value = p.rejectReason || ''
      }
    }
  } catch (_) {
    targetSummary.value = ''
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value) return
  submitting.value = true
  try {
    await submitAppeal({
      targetType: targetType.value,
      targetId: targetId.value,
      reason: reason.value.trim()
    })
    ElMessage.success('申诉已提交，请等待处理')
    router.push('/my-appeals')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(loadTarget)
</script>

<style scoped>
.appeal-submit-page {
  padding: 16px;
  max-width: 480px;
  margin: 0 auto;
}

.loading-wrap {
  text-align: center;
  padding: 40px;
}

.section-title {
  margin-bottom: 12px;
  font-size: 1rem;
}

.target-card {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
  margin-bottom: 24px;
}

.target-type {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.target-info {
  font-weight: 500;
}

.reject-reason {
  margin-top: 8px;
  font-size: 13px;
  color: #f56c6c;
}

.form {
  margin-top: 16px;
}

.form .el-button {
  width: 100%;
  margin-top: 16px;
}
</style>
