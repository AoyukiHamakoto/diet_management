<template>
  <div class="preferences-page">
    <PageBackBar fallback="/mine" />
    <div v-if="loading" class="loading-wrap">
      <el-icon class="loading-icon"><Loading /></el-icon>
    </div>
    <template v-else>
      <div class="pref-card">
        <h3>系统已根据您的反馈做了 {{ pref?.adjustmentCount || 0 }} 次调整</h3>
      </div>
      <div class="pref-card">
        <h4>学习进度</h4>
        <div class="progress-wrap">
          <el-progress :percentage="pref?.learningProgress || 0" :stroke-width="12" />
          <p>系统已了解您的口味进度：{{ pref?.learningProgress || 0 }}%</p>
        </div>
      </div>
      <div class="pref-card">
        <h4>当前偏好标签</h4>
        <div v-if="pref?.profileTags?.length" class="tag-list">
          <div v-for="t in pref.profileTags" :key="t.tag" class="tag-item">
            <span class="tag-name">{{ dietTagLabel(t.tag) }}</span>
            <span class="tag-conf">置信度 {{ t.confidence }}%</span>
          </div>
        </div>
        <el-empty v-else description="完善健康档案后生成" />
      </div>
      <div class="pref-card notifications">
        <h4>系统消息</h4>
        <div v-if="notifications?.length" class="noti-list">
          <div v-for="n in notifications" :key="n.id" class="noti-item">
            <span>{{ n.message }}</span>
            <el-button link size="small" @click="markRead(n.id)">已读</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无系统消息" />
      </div>
      <el-button type="warning" plain class="reset-btn" @click="handleReset">重置推荐偏好</el-button>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getPreferences, resetPreferences, getNotifications, markNotificationRead } from '../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dietTagLabel } from '../utils/dietTagLabels'
import PageBackBar from '../components/PageBackBar.vue'

const loading = ref(true)
const pref = ref(null)
const notifications = ref([])

async function load() {
  loading.value = true
  try {
    const [pRes, nRes] = await Promise.all([getPreferences(), getNotifications()])
    pref.value = pRes.data
    notifications.value = nRes.data
  } catch (_) {}
  finally {
    loading.value = false
  }
}

async function markRead(id) {
  try {
    await markNotificationRead(id)
    load()
  } catch (_) {}
}

async function handleReset() {
  try {
    await ElMessageBox.confirm('重置后推荐将回到初始状态，确定吗？', '提示')
    await resetPreferences()
    ElMessage.success('已重置')
    load()
  } catch (_) {}
}

onMounted(load)
</script>

<style scoped>
.preferences-page {
  padding-bottom: 80px;
}
.pref-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.pref-card h3, .pref-card h4 {
  margin: 0 0 16px;
}
.progress-wrap p {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
.tag-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.tag-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.tag-name { font-weight: 500; min-width: 80px; }
.tag-conf { font-size: 13px; color: #909399; }
.noti-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.noti-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.reset-btn {
  width: 100%;
  margin-top: 24px;
}
.loading-wrap { text-align: center; padding: 40px; }
.loading-icon { font-size: 32px; }
</style>
