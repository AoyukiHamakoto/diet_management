<template>
  <div class="admin-dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.todayActiveUsers ?? '-' }}</div>
          <div class="stat-label">今日活跃用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.todayPlansGenerated ?? '-' }}</div>
          <div class="stat-label">今日生成计划数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.pendingRecipes ?? '-' }}</div>
          <div class="stat-label">待审核菜谱</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.avgFeedbackRating ?? '-' }}</div>
          <div class="stat-label">用户反馈平均评分</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>近7天用户增长</span>
          </template>
          <v-chart :option="userGrowthOption" style="height: 280px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>用户目标分布</span>
          </template>
          <v-chart :option="targetDistOption" style="height: 280px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>热门菜谱 TOP10</span>
          </template>
          <v-chart :option="popularRecipesOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboardStats, getUserGrowthChart, getPopularRecipes, getTargetDistribution } from '../../api/admin'

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent
])

const stats = ref({})
const userGrowthData = ref([])
const popularRecipesData = ref([])
const targetDistData = ref({})

const REFRESH_INTERVAL = 5 * 60 * 1000 // 5 minutes
let refreshTimer = null

const userGrowthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: userGrowthData.value.map((d) => d.date)
  },
  yAxis: { type: 'value', name: '新增用户' },
  series: [{ name: '新增用户', type: 'line', data: userGrowthData.value.map((d) => d.count), smooth: true }]
}))

const targetDistOption = computed(() => {
  const labels = { LOSE_WEIGHT: '减脂', BUILD_MUSCLE: '增肌', MAINTAIN: '维持', UNKNOWN: '未设置' }
  const data = Object.entries(targetDistData.value || {}).map(([k, v]) => ({
    name: labels[k] || k,
    value: v
  }))
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ type: 'pie', radius: '60%', data }]
  }
})

const popularRecipesOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: '20%' },
  xAxis: { type: 'value', name: '计划次数' },
  yAxis: {
    type: 'category',
    data: popularRecipesData.value.map((d) => d.title).reverse()
  },
  series: [{
    name: '被选次数',
    type: 'bar',
    data: popularRecipesData.value.map((d) => d.count).reverse()
  }]
}))

async function loadStats() {
  try {
    const res = await getDashboardStats()
    stats.value = res.data || {}
  } catch (_) {}
}

async function loadCharts() {
  try {
    const [growth, recipes, dist] = await Promise.all([
      getUserGrowthChart(7),
      getPopularRecipes(10),
      getTargetDistribution()
    ])
    userGrowthData.value = growth.data || []
    popularRecipesData.value = recipes.data || []
    targetDistData.value = dist.data || {}
  } catch (_) {}
}

function startRefresh() {
  refreshTimer = setInterval(() => {
    loadStats()
    loadCharts()
  }, REFRESH_INTERVAL)
}

onMounted(() => {
  loadStats()
  loadCharts()
  startRefresh()
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.admin-dashboard {
  padding: 0;
}

.stat-cards {
  margin-bottom: 0;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 600;
  color: #303133;
}

.stat-label {
  font-size: 0.875rem;
  color: #909399;
  margin-top: 4px;
}
</style>
