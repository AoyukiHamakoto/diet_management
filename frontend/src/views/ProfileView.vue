<template>
  <div class="profile-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <div v-if="!profile || !hasProfile" class="empty-state">
        <el-empty description="请完善健康档案">
          <el-button type="primary" @click="$router.push('/profile/setup')">去设置</el-button>
        </el-empty>
      </div>
      <div v-else class="profile-content">
        <!-- 健康状态大号显示 -->
        <div class="health-status-card" :class="bmiStatusClass">
          <div class="status-label">当前状态</div>
          <div class="status-value">{{ profile.bmiRating || '--' }}</div>
          <div class="status-bmi">BMI {{ profile.bmi != null ? profile.bmi.toFixed(1) : '--' }}</div>
        </div>

        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-label">TDEE 每日热量</div>
            <div class="stat-value">{{ profile.tdee != null ? profile.tdee : '--' }} <span class="unit">kcal</span></div>
          </div>
          <div class="stat-item">
            <div class="stat-label">身高/体重</div>
            <div class="stat-value">{{ profile.height || '--' }} cm / {{ profile.weight || '--' }} kg</div>
          </div>
        </div>

        <div class="profile-actions">
          <el-button type="primary" size="large" class="profile-action-btn" @click="showWeightDialog = true">
            更新体重
          </el-button>
          <el-button type="default" plain size="large" class="profile-action-btn" @click="$router.push('/profile/setup')">
            编辑健康档案
          </el-button>
        </div>

        <div v-if="exerciseHabitText" class="exercise-habit">
          {{ exerciseHabitText }}
        </div>

        <!-- 饮食标签 -->
        <div v-if="dietTags.length > 0" class="diet-tags">
          <h4>个性化饮食标签</h4>
          <div class="tag-list">
            <el-tag v-for="tag in dietTags" :key="tag.tagName" type="success" effect="plain" class="diet-tag">
              {{ dietTagLabel(tag.tagName) }} ({{ (tag.confidenceScore * 100).toFixed(0) }}%)
            </el-tag>
          </div>
        </div>

        <!-- 体重变化曲线 -->
        <div v-if="chartData.length > 0" class="weight-chart">
          <h4>体重变化</h4>
          <v-chart class="chart" :option="chartOption" autoresize />
        </div>
      </div>
    </template>

    <!-- 更新体重弹窗（底部抽屉，移动端友好） -->
    <el-drawer
      v-model="showWeightDialog"
      title="更新体重"
      direction="btt"
      size="50%"
      :modal="true"
      class="weight-drawer"
    >
      <el-form label-position="top">
        <el-form-item label="体重（kg）">
          <el-input-number
            v-model="newWeight"
            :min="20"
            :max="300"
            :precision="1"
            size="large"
            placeholder="输入体重"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="updating" style="width: 100%" @click="handleUpdateWeight">
            保存
          </el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { getHealthProfile, createBodyLog, getBodyLogChart, getDietTags } from '../api/user'
import { dietTagLabel } from '../utils/dietTagLabels'
use([CanvasRenderer, LineChart, GridComponent, TooltipComponent])

const loading = ref(true)
const profile = ref(null)
const chartData = ref([])
const dietTags = ref([])
const showWeightDialog = ref(false)
const newWeight = ref(null)
const updating = ref(false)

const hasProfile = computed(() => {
  const p = profile.value
  return p && (p.height || p.weight || p.age)
})

const chartOption = computed(() => ({
  xAxis: { type: 'category', data: chartData.value.map((d) => d.date) },
  yAxis: { type: 'value', name: 'kg' },
  series: [{ type: 'line', data: chartData.value.map((d) => d.weight), smooth: true }],
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  tooltip: { trigger: 'axis' }
}))

const bmiStatusClass = computed(() => {
  const r = profile.value?.bmiRating
  if (!r) return ''
  if (r === '正常') return 'normal'
  if (r === '偏瘦') return 'underweight'
  if (r === '偏胖') return 'overweight'
  if (r === '肥胖') return 'obese'
  return ''
})

const frequencyLabels = { NONE: '久坐', LIGHT: '每周1-3次', MODERATE: '每周3-4次', HIGH: '每周5-7次' }
const timeLabels = { MORNING: '早晨', AFTERNOON: '下午', EVENING: '晚间', NONE: '不固定' }

const exerciseHabitText = computed(() => {
  const p = profile.value
  if (!p) return ''
  const freq = p.exerciseFrequency ? frequencyLabels[p.exerciseFrequency] || p.exerciseFrequency : null
  const time = p.exerciseTime && p.exerciseTime !== 'NONE' ? `偏好${timeLabels[p.exerciseTime] || p.exerciseTime}训练` : null
  if (!freq && !time) return ''
  if (freq && time) return `运动习惯：${freq} · ${time}`
  if (freq) return `运动习惯：${freq}`
  return `运动习惯：${time}`
})

async function loadProfile() {
  loading.value = true
  try {
    const [profileRes, chartRes, tagsRes] = await Promise.all([
      getHealthProfile(),
      getBodyLogChart(30),
      getDietTags().catch(() => ({ data: [] }))
    ])
    profile.value = profileRes.data
    newWeight.value = profileRes.data?.weight ?? null
    chartData.value = chartRes.data || []
    dietTags.value = tagsRes.data || []
  } finally {
    loading.value = false
  }
}

async function handleUpdateWeight() {
  if (!newWeight.value || newWeight.value < 20 || newWeight.value > 300) {
    return
  }
  updating.value = true
  try {
    await createBodyLog(newWeight.value)
    profile.value = { ...profile.value, weight: newWeight.value }
    showWeightDialog.value = false
    loadProfile()
  } catch (e) {
    // ElMessage from request interceptor
  } finally {
    updating.value = false
  }
}

onMounted(loadProfile)
// 从编辑页返回时（keep-alive 缓存）重新拉取，保证看到最新档案
onActivated(loadProfile)
</script>

<style scoped>
.profile-page {
  min-height: 200px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.loading-wrap {
  padding: 20px;
}

.empty-state {
  padding: 40px 20px;
}

.profile-content {
  max-width: 400px;
  margin: 0 auto;
}

.health-status-card {
  padding: 28px;
  border-radius: 16px;
  text-align: center;
  margin-bottom: 24px;
}

.health-status-card .status-label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 8px;
}

.health-status-card .status-value {
  font-size: 2rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.health-status-card .status-bmi {
  font-size: 1rem;
  opacity: 0.9;
  margin-top: 4px;
}

.health-status-card.normal { background: linear-gradient(135deg, #67c23a, #85ce61); }
.health-status-card.underweight { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.health-status-card.overweight { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.health-status-card.obese { background: linear-gradient(135deg, #f56c6c, #f78989); }

.stats-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.stat-label {
  font-size: 0.8rem;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 600;
}

.stat-value .unit {
  font-size: 0.875rem;
  font-weight: 400;
  color: #606266;
}

.profile-actions {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.profile-action-btn {
  flex: 1;
  min-width: 0;
}

.exercise-habit {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 10px;
  font-size: 14px;
  color: #606266;
}

.diet-tags {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.diet-tags h4 {
  margin-bottom: 12px;
  font-size: 0.95rem;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.diet-tag {
  font-size: 0.9rem;
}

.weight-chart {
  margin-top: 24px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.weight-chart h4 {
  margin-bottom: 12px;
  font-size: 0.95rem;
}

.weight-chart .chart {
  height: 200px;
  width: 100%;
}

@media (max-width: 480px) {
  .profile-page {
    padding: 16px;
  }
  .health-status-card .status-value {
    font-size: 1.75rem;
  }
  .weight-drawer :deep(.el-drawer) {
    max-height: 70vh;
    border-radius: 16px 16px 0 0;
  }
}
</style>
