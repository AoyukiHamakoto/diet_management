<template>
  <div class="feedback-history-page">
    <PageBackBar fallback="/mine" />
    <div v-if="loading" class="loading-wrap">
      <el-icon class="loading-icon"><Loading /></el-icon>
    </div>
    <div v-else-if="records?.length" class="timeline">
      <div v-for="(item, idx) in records" :key="item.feedback?.id ?? `fb-${idx}`" class="timeline-item">
        <div class="date">{{ formatDate(item.planDate) }}</div>
        <div class="content">
          <div class="dish">{{ item.dishName || '未知菜品' }}</div>
          <el-rate :model-value="item.feedback?.rating" disabled size="small" />
          <div class="metrics">
            <span>饱腹感：{{ item.feedback?.satietyRating ?? '-' }}</span>
            <span>难度：{{ item.feedback?.difficultyRating ?? '-' }}</span>
            <span>综合满意度：{{ item.feedback?.satisfactionScore ?? '-' }}</span>
          </div>
          <div v-if="item.feedback?.feedbackTags?.length" class="tags">
            <el-tag v-for="t in item.feedback.feedbackTags" :key="t" size="small">{{ tagLabel(t) }}</el-tag>
          </div>
          <div v-if="item.feedback?.systemAction" class="action">
            系统改进：{{ item.feedback.systemAction }}
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂无评价记录" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFeedbackHistory } from '../api/feedback'
import { dietTagLabel } from '../utils/dietTagLabels'
import PageBackBar from '../components/PageBackBar.vue'

const loading = ref(true)
const records = ref([])

const tagLabel = (t) => dietTagLabel(t)

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN')
}

async function load() {
  loading.value = true
  try {
    const res = await getFeedbackHistory(1, 20)
    records.value = res.data?.records || []
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '加载失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.feedback-history-page {
  padding-bottom: 80px;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.timeline-item {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.timeline-item .date {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.timeline-item .dish {
  font-weight: 500;
  margin-bottom: 8px;
}
.timeline-item .tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.timeline-item .action {
  margin-top: 8px;
  font-size: 13px;
  color: #67c23a;
}
.timeline-item .metrics {
  margin-top: 6px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}
.loading-wrap { text-align: center; padding: 40px; }
.loading-icon { font-size: 32px; }
</style>
