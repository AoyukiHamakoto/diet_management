<template>
  <PullToRefresh class="plan-page" :on-refresh="loadPlan" :disabled="loading">
    <!-- 提醒横幅 -->
    <div v-if="reminder?.needReminder" class="reminder-banner">
      已经{{ reminder.daysSinceLastPlan }}天没规划饮食了哦
    </div>

    <div ref="swipeRef" :class="{ 'pc-layout': isPC }" class="plan-swipe-area">
      <!-- 原型：未来7天 Calendar 视图 -->
      <div v-if="isPC" class="week-calendar">
        <h4>计划日期（{{ periodDays }} 天）</h4>
        <div
          v-for="day in calendarDays"
          :key="day.date"
          class="week-day"
          :class="{ active: day.date === selectedDate }"
          @click="selectedDate = day.date"
        >
          <span class="day-name">{{ day.name }}</span>
          <span class="day-num">{{ day.day }}</span>
        </div>
      </div>

      <div class="detail-panel">
        <!-- 顶部：生成按钮 + 周期选择 -->
        <div class="plan-header">
          <el-date-picker v-model="selectedDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
          <el-select v-model="periodDays" style="width: 120px" size="default">
            <el-option label="未来7天" :value="7" />
            <el-option label="未来3天" :value="3" />
          </el-select>
          <el-button type="primary" :loading="generating" @click="handleGenerate">生成新计划</el-button>
          <el-button v-if="isPC" type="success" plain :loading="generating" @click="handleGeneratePeriod">
            一键生成{{ periodDays }}天
          </el-button>
        </div>

    <PlanSkeleton v-if="loading" />
    <div v-else-if="generating" class="loading-wrap">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>正在为您计算最佳营养搭配...</p>
    </div>

    <template v-else-if="planData">
      <!-- 原型：进度条 -->
      <el-progress :percentage="completePct" :stroke-width="10" style="margin-bottom: 14px" />

      <!-- 进度环 -->
      <div class="progress-ring-wrap">
        <v-chart ref="ringChartRef" class="ring-chart" :option="progressOption" :init-options="ringChartInitOpts" autoresize />
        <div class="ring-center">
          <span class="consumed">{{ Math.round(planData.totalConsumedCalories || 0) }}</span>
          <span class="slash">/</span>
          <span class="target">{{ Math.round(planData.totalTargetCalories || 2000) }}</span>
          <span class="unit">千卡</span>
        </div>
      </div>
      <div v-if="planData && healthProfile?.exerciseTime === 'EVENING'" class="plan-hint">
        基于您的晚间训练偏好，晚餐已增加蛋白质推荐
      </div>

      <!-- 餐次列表 -->
      <div class="meal-list">
        <div
          v-for="(item, i) in planData.meals"
          :key="item.plan?.id"
          class="meal-card"
          :class="{ completed: item.plan?.status === 'COMPLETED', skipped: item.plan?.status === 'SKIPPED', outEat: item.plan?.status === 'OUT_EAT' }"
          @touchstart="() => startLongPress(item)"
          @touchend="clearLongPress"
          @touchmove="clearLongPress"
        >
          <div class="meal-header">
            <span class="meal-type">{{ mealTypeLabel(item.plan?.mealType) }}</span>
            <div class="meal-actions">
              <el-button
                v-if="item.plan?.status === 'PLANNED' && item.recipe"
                class="complete-meal-btn"
                size="small"
                @click.stop="handleComplete(item.plan)"
              >
                完成
              </el-button>
              <el-tag v-else-if="item.plan?.status === 'COMPLETED'" type="success">已完成</el-tag>
              <el-tag v-else-if="item.plan?.status === 'SKIPPED'" type="info">已跳过</el-tag>
              <el-tag v-else-if="item.plan?.status === 'OUT_EAT'" type="warning">外食</el-tag>
            </div>
          </div>
          <div v-if="item.recipe" class="meal-body" @click="$router.push(`/recipe/${item.recipe.id}`)">
            <div class="meal-img">
              <img :src="item.recipe.coverImage || '/vite.svg'" loading="lazy" />
            </div>
            <div class="meal-info">
              <div class="meal-title">{{ item.recipe.title }}</div>
              <div class="meal-meta">{{ Math.round(item.calories || 0) }} kcal</div>
              <div v-if="item.matchReasons?.length" class="match-tags">
                <el-tag v-for="r in item.matchReasons" :key="r" size="small" type="success" effect="plain">
                  {{ r }}
                </el-tag>
              </div>
              <div v-if="item.plan?.status === 'PLANNED'" class="meal-extra-actions">
                <el-button link type="primary" size="small" @click.stop="handleReplace(item)">换一道</el-button>
                <el-button link type="info" size="small" @click.stop="handleSkip(item.plan)">跳过</el-button>
                <el-button link type="warning" size="small" @click.stop="showOutEatDialog(item.plan)">标记外食</el-button>
              </div>
            </div>
          </div>
          <div v-else-if="item.recipeDeleted || (item.plan?.recipeId && !item.recipe)" class="meal-body deleted-recipe">
            <div class="meal-img">
              <div class="placeholder-img">已下架</div>
            </div>
            <div class="meal-info">
              <div class="meal-title text-muted">菜谱已下架</div>
              <div class="meal-meta">{{ Math.round(item.recordedCalories ?? item.calories ?? 0) }} kcal</div>
            </div>
          </div>
          <div v-else class="meal-body empty">
            <span>暂无推荐菜谱</span>
          </div>
        </div>
      </div>

      <!-- 营养统计图（固定高度容器 + resize，避免 ECharts 在 clientWidth/Height 为 0 时初始化导致空白） -->
      <div v-if="hasNutrition" class="nutrition-section" :class="{ 'in-panel': isPC }">
        <h4>今日营养摄入</h4>
        <div class="nutrition-chart-host">
          <v-chart
            ref="nutritionChartRef"
            class="nutrition-chart"
            :option="nutritionChartOption"
            :init-options="nutritionChartInitOpts"
            :style="{ width: '100%', height: '100%', minHeight: '200px' }"
            autoresize
          />
        </div>
      </div>
    </template>
    <el-empty v-else description="选择日期并生成计划" />

    <el-dialog v-model="outEatDialogVisible" title="标记外食" width="90%" style="max-width: 400px">
      <el-input v-model="outEatNote" type="textarea" placeholder="记录吃了什么（可选）" :rows="3" />
      <template #footer>
        <el-button @click="outEatDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleOutEat">确定</el-button>
      </template>
    </el-dialog>

    <!-- 餐后评价 Bottom Sheet -->
    <el-drawer v-model="feedbackDrawerVisible" direction="btt" :size="isPC ? '50%' : '75%'" title="评价这顿饭">
      <div class="feedback-sheet">
        <div class="feedback-stars">
          <div class="feedback-item">
            <span class="metric-label">口味评分</span>
            <el-rate v-model="feedbackRating" :max="5" size="large" show-score />
          </div>
          <div class="feedback-item">
            <span class="metric-label">饱腹感评分</span>
            <el-rate v-model="feedbackSatietyRating" :max="5" size="large" show-score />
          </div>
          <div class="feedback-item">
            <span class="metric-label">执行难度评分（越高越容易）</span>
            <el-rate v-model="feedbackDifficultyRating" :max="5" size="large" show-score />
          </div>
        </div>
        <div class="feedback-tags">
          <span class="label">快捷标签</span>
          <div class="chips">
            <span
              v-for="t in FEEDBACK_TAGS"
              :key="t.value"
              class="chip"
              :class="{ selected: feedbackTagsMap[t.value] }"
              @click="feedbackTagsMap[t.value] = !feedbackTagsMap[t.value]"
            >{{ t.label }}</span>
          </div>
        </div>
        <el-input v-model="feedbackComment" type="textarea" placeholder="补充说明（可选）" :rows="2" />
        <el-button type="primary" class="submit-btn" @click="submitFeedback">提交评价</el-button>
      </div>
    </el-drawer>

    <el-dialog v-model="proactiveDialogVisible" title="贴心提示" width="90%" style="max-width: 360px">
      <p>{{ proactivePrompt?.message }}</p>
      <template #footer>
        <el-button @click="proactiveDialogVisible = false">暂不</el-button>
        <el-button type="primary" @click="proactiveDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="longPressSheetVisible" direction="btt" size="auto" title="快捷操作">
      <div class="long-press-actions">
        <el-button type="primary" style="width: 100%; margin-bottom: 8px" @click="handleLongPressAction('replace')">
          换一道
        </el-button>
        <el-button type="info" style="width: 100%; margin-bottom: 8px" @click="handleLongPressAction('skip')">
          跳过
        </el-button>
        <el-button type="warning" style="width: 100%; margin-bottom: 8px" @click="handleLongPressAction('outEat')">
          标记外食
        </el-button>
        <el-button style="width: 100%" @click="longPressSheetVisible = false">取消</el-button>
      </div>
    </el-drawer>

    <el-dialog v-model="offerReplaceVisible" title="更换推荐" width="90%" style="max-width: 360px">
      <p>是否需要立即为您更换明天的类似餐品？</p>
      <template #footer>
        <el-button @click="offerReplaceVisible = false">不了</el-button>
        <el-button type="primary" @click="goReplaceTomorrow">去更换</el-button>
      </template>
    </el-dialog>
      </div>
    </div>
  </PullToRefresh>
</template>

<script setup>
import { ref, computed, watch, onMounted, onActivated, nextTick } from 'vue'
import VChart from 'vue-echarts'
import PullToRefresh from '../components/PullToRefresh.vue'
import PlanSkeleton from '../components/PlanSkeleton.vue'
import { useSwipe } from '../composables/useSwipe'
import { vibrate } from '../utils/vibrate'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { Loading } from '@element-plus/icons-vue'
import {
  getDailyPlan,
  getReminder,
  getProactivePrompt,
  generatePlan,
  completePlan,
  skipPlan,
  replacePlan,
  markOutEat
} from '../api/plan'
import { submitFeedback as apiSubmitFeedback, FEEDBACK_TAGS } from '../api/feedback'
import { getHealthProfile } from '../api/user'
import { ElMessage } from 'element-plus'
import { logError, logInfo, logWarn } from '../utils/logger'

use([CanvasRenderer, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

const isPC = ref(window.innerWidth >= 768)
const selectedDate = ref(new Date().toISOString().slice(0, 10))
const periodDays = ref(7)
const planData = ref(null)
const healthProfile = ref(null)
const reminder = ref(null)
const loading = ref(false)
const generating = ref(false)
const outEatDialogVisible = ref(false)
const outEatNote = ref('')
const outEatPlan = ref(null)
const feedbackDrawerVisible = ref(false)
const feedbackPlan = ref(null)
const feedbackRating = ref(5)
const feedbackSatietyRating = ref(5)
const feedbackDifficultyRating = ref(5)
const feedbackComment = ref('')
const feedbackTagsMap = ref(Object.fromEntries(FEEDBACK_TAGS.map(t => [t.value, false])))
const offerReplaceVisible = ref(false)
const proactivePrompt = ref(null)
const proactiveDialogVisible = ref(false)
const swipeRef = ref(null)
const longPressTimer = ref(null)
const longPressTarget = ref(null)
const longPressSheetVisible = ref(false)
const ringChartRef = ref(null)
const nutritionChartRef = ref(null)

/** 首次 init 时若容器尚未参与布局，传入像素尺寸可避免 clientWidth/Height 为 0 */
const ringChartInitOpts = { width: 160, height: 160 }
const nutritionChartInitOpts = { height: 240 }

/** keep-alive / 异步布局下容器可能一度为 0×0，需延后 resize 才能正常绘图 */
function resizePlanCharts() {
  const run = () => {
    ringChartRef.value?.resize?.()
    nutritionChartRef.value?.resize?.()
  }
  nextTick(() => {
    requestAnimationFrame(run)
    setTimeout(run, 50)
  })
}

useSwipe(swipeRef, swipeNextDay, swipePrevDay)

function swipeNextDay() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() + 1)
  selectedDate.value = d.toISOString().slice(0, 10)
}

function swipePrevDay() {
  const d = new Date(selectedDate.value)
  d.setDate(d.getDate() - 1)
  selectedDate.value = d.toISOString().slice(0, 10)
}

function startLongPress(item) {
  clearLongPress()
  if (item.plan?.status !== 'PLANNED' || !item.recipe) return
  longPressTimer.value = setTimeout(() => {
    longPressTarget.value = item
    longPressSheetVisible.value = true
    longPressTimer.value = null
  }, 500)
}

function clearLongPress() {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

function handleLongPressAction(action) {
  const item = longPressTarget.value
  if (!item) return
  if (action === 'replace') handleReplace(item)
  else if (action === 'skip') handleSkip(item.plan)
  else if (action === 'outEat') showOutEatDialog(item.plan)
  longPressSheetVisible.value = false
  longPressTarget.value = null
}

const mealTypeLabel = (t) => ({ BREAKFAST: '早餐', LUNCH: '午餐', DINNER: '晚餐', SNACK: '加餐' }[t] || t)

const weekDays = computed(() => {
  const d = new Date()
  const monday = new Date(d)
  monday.setDate(d.getDate() - d.getDay() + 1)
  const names = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  return Array.from({ length: 7 }, (_, i) => {
    const day = new Date(monday)
    day.setDate(monday.getDate() + i)
    return {
      date: day.toISOString().slice(0, 10),
      day: day.getDate(),
      name: names[i]
    }
  })
})

function toYmd(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 与日期选择器、周期下拉联动：最上多一天便于回到昨日；其后为从已选日期起连续 periodDays 天 */
const calendarDays = computed(() => {
  const n = Math.min(Math.max(periodDays.value, 1), 14)
  const start = selectedDate.value
    ? new Date(selectedDate.value + 'T12:00:00')
    : new Date()
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const prev = new Date(start)
  prev.setDate(start.getDate() - 1)
  const days = [{
    date: toYmd(prev),
    day: prev.getDate(),
    name: names[prev.getDay()]
  }]
  for (let i = 0; i < n; i++) {
    const day = new Date(start)
    day.setDate(start.getDate() + i)
    days.push({
      date: toYmd(day),
      day: day.getDate(),
      name: names[day.getDay()]
    })
  }
  return days
})

const completePct = computed(() => {
  const meals = planData.value?.meals || []
  const total = meals.length || 0
  const done = meals.filter(m => m.plan?.status === 'COMPLETED').length
  return total > 0 ? Math.round((done / total) * 100) : 0
})

const progressOption = computed(() => {
  const consumed = planData.value?.totalConsumedCalories || 0
  const target = planData.value?.totalTargetCalories || 2000
  const pct = target > 0 ? Math.min(100, (consumed / target) * 100) : 0
  return {
    series: [{
      type: 'pie',
      radius: ['70%', '90%'],
      startAngle: 90,
      data: [
        { value: pct, itemStyle: { color: '#67c23a' } },
        { value: 100 - pct, itemStyle: { color: '#f0f0f0' } }
      ],
      label: { show: false }
    }]
  }
})

// 只要有当日计划就展示；已摄入可为 0（尚未完成任何一餐时），否则整块被 v-if 隐藏且无任何报错
const hasNutrition = computed(() => !!planData.value)

const nutritionChartOption = computed(() => {
  const d = planData.value || {}
  const consumed = [d.consumedProtein || 0, d.consumedCarb || 0, d.consumedFat || 0]
  const target = [d.targetProtein || 50, d.targetCarb || 250, d.targetFat || 65]
  const colors = consumed.map((c, i) => (c > target[i] && target[i] > 0 ? '#f56c6c' : '#67c23a'))
  const maxVal = Math.max(1, ...consumed, ...target)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['已摄入', '目标'] },
    grid: { left: -95, right: '2%', bottom: '4%', top: 28, containLabel: true },
    xAxis: {
      type: 'category',
      data: ['蛋白质(g)', '碳水(g)', '脂肪(g)'],
      axisLabel: { interval: 0, hideOverlap: true }
    },
    yAxis: { type: 'value', min: 0, max: maxVal * 1.12 },
    series: [
      {
        name: '已摄入',
        type: 'bar',
        data: consumed.map((v, i) => ({ value: v, itemStyle: { color: colors[i] } })),
        barMaxWidth: 40,
        barGap: '12%'
      },
      {
        name: '目标',
        type: 'bar',
        data: target,
        barMaxWidth: 40,
        itemStyle: { color: '#dcdfe6' }
      }
    ]
  }
})

async function loadPlan() {
  if (!selectedDate.value) return
  logInfo('PLAN', 'load_daily_plan_start', { date: selectedDate.value })
  loading.value = true
  try {
    const res = await getDailyPlan(selectedDate.value)
    planData.value = res.data
    if (!healthProfile.value) {
      try {
        const hp = await getHealthProfile()
        healthProfile.value = hp.data
      } catch (_) {}
    }
    logInfo('PLAN', 'load_daily_plan_ok', { date: selectedDate.value, meals: planData.value?.meals?.length })
  } catch (_) {
    planData.value = null
    logWarn('PLAN', 'load_daily_plan_empty_or_error', { date: selectedDate.value })
  } finally {
    loading.value = false
  }
}

async function loadReminder() {
  try {
    const res = await getReminder()
    reminder.value = res.data
    logInfo('PLAN', 'load_reminder_ok', reminder.value)
  } catch (_) {}
}

async function loadProactivePrompt() {
  try {
    const res = await getProactivePrompt()
    if (res.data?.show) {
      proactivePrompt.value = res.data
      proactiveDialogVisible.value = true
    }
  } catch (_) {}
}

async function handleGenerate() {
  if (!selectedDate.value) return
  logInfo('PLAN', 'generate_one_start', { date: selectedDate.value })
  generating.value = true
  try {
    await generatePlan(selectedDate.value)
    ElMessage.success('计划已生成')
    loadPlan()
    loadReminder()
    logInfo('PLAN', 'generate_one_ok', { date: selectedDate.value })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '生成失败')
    logError('PLAN', 'generate_one_failed', { date: selectedDate.value, message: e?.response?.data?.message || e?.message })
  } finally {
    generating.value = false
  }
}

async function handleGeneratePeriod() {
  const base = selectedDate.value
    ? new Date(selectedDate.value + 'T12:00:00')
    : new Date()
  generating.value = true
  try {
    logInfo('PLAN', 'generate_period_start', { days: periodDays.value })
    for (let i = 0; i < periodDays.value; i++) {
      const day = new Date(base)
      day.setDate(base.getDate() + i)
      const dayStr = toYmd(day)
      // eslint-disable-next-line no-await-in-loop
      await generatePlan(dayStr)
    }
    ElMessage.success(`已生成未来${periodDays.value}天计划`)
    await loadPlan()
    await loadReminder()
    logInfo('PLAN', 'generate_period_ok', { days: periodDays.value })
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '计划生成失败')
    logError('PLAN', 'generate_period_failed', { days: periodDays.value, message: e?.response?.data?.message || e?.message })
  } finally {
    generating.value = false
  }
}

async function handleComplete(plan) {
  try {
    logInfo('PLAN', 'complete_start', { planId: plan.id })
    await completePlan(plan.id)
    vibrate(50)
    feedbackPlan.value = plan
    feedbackRating.value = 5
    feedbackSatietyRating.value = 5
    feedbackDifficultyRating.value = 5
    feedbackComment.value = ''
    feedbackTagsMap.value = Object.fromEntries(FEEDBACK_TAGS.map(t => [t.value, false]))
    feedbackDrawerVisible.value = true
    loadPlan()
    logInfo('PLAN', 'complete_ok', { planId: plan.id })
  } catch (_) {}
}

async function submitFeedback() {
  if (!feedbackPlan.value) return
  try {
    logInfo('FEEDBACK', 'submit_start', { planId: feedbackPlan.value.id })
    const tags = Object.entries(feedbackTagsMap.value).filter(([, v]) => v).map(([k]) => k)
    const res = await apiSubmitFeedback({
      planId: feedbackPlan.value.id,
      rating: feedbackRating.value,
      satietyRating: feedbackSatietyRating.value,
      difficultyRating: feedbackDifficultyRating.value,
      tags,
      comment: feedbackComment.value
    })
    feedbackDrawerVisible.value = false
    ElMessage.success('感谢评价，我们会根据您的反馈优化推荐')
    logInfo('FEEDBACK', 'submit_ok', { planId: feedbackPlan.value.id, offerReplace: !!res.data?.offerReplace })
    if (res.data?.offerReplace) {
      offerReplaceVisible.value = true
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
    logError('FEEDBACK', 'submit_failed', { planId: feedbackPlan.value?.id, message: e?.response?.data?.message || e?.message })
  }
}

function goReplaceTomorrow() {
  offerReplaceVisible.value = false
  selectedDate.value = new Date(Date.now() + 86400000).toISOString().slice(0, 10)
}

async function handleSkip(plan) {
  try {
    logInfo('PLAN', 'skip_start', { planId: plan.id })
    await skipPlan(plan.id)
    loadPlan()
    logInfo('PLAN', 'skip_ok', { planId: plan.id })
  } catch (_) {}
}

function showOutEatDialog(plan) {
  outEatPlan.value = plan
  outEatNote.value = ''
  outEatDialogVisible.value = true
}

async function handleOutEat() {
  if (!outEatPlan.value) return
  try {
    logInfo('PLAN', 'out_eat_start', { planId: outEatPlan.value.id })
    await markOutEat(outEatPlan.value.id, outEatNote.value)
    ElMessage.success('已标记')
    outEatDialogVisible.value = false
    loadPlan()
    logInfo('PLAN', 'out_eat_ok', { planId: outEatPlan.value.id })
  } catch (_) {}
}

async function handleReplace(item) {
  try {
    logInfo('PLAN', 'replace_start', { planId: item.plan.id })
    const res = await replacePlan(item.plan.id)
    const d = res.data || {}
    if (d.needCompensationAdvice && d.compensationAdvice) {
      ElMessage.warning(`已替换。营养偏差率 ${(Number(d.nutritionDeviationRate || 0) * 100).toFixed(1)}%，建议：${d.compensationAdvice}`)
    } else {
      ElMessage.success('已替换')
    }
    loadPlan()
    logInfo('PLAN', 'replace_ok', { planId: item.plan.id, deviationRate: d.nutritionDeviationRate })
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '替换失败')
    logError('PLAN', 'replace_failed', { planId: item.plan?.id, message: e?.response?.data?.message || e?.message })
  }
}

watch(selectedDate, loadPlan)

watch(planData, () => resizePlanCharts())

watch(loading, (v) => {
  if (!v) resizePlanCharts()
})

onMounted(() => {
  loadPlan()
  loadReminder()
  loadProactivePrompt()
  resizePlanCharts()
  window.addEventListener('resize', () => {
    isPC.value = window.innerWidth >= 768
    resizePlanCharts()
  })
})

onActivated(() => {
  resizePlanCharts()
})
</script>

<style scoped>
.plan-page {
  padding-bottom: 80px;
}

.reminder-banner {
  background: #fff3e0;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 16px;
  text-align: center;
}

.plan-header {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.loading-wrap {
  text-align: center;
  padding: 40px;
}

.loading-icon {
  font-size: 48px;
  color: #1a5f4a;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.progress-ring-wrap {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 24px;
}

.ring-chart {
  width: 100%;
  height: 100%;
}

.ring-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.consumed { font-size: 1.5rem; font-weight: 700; color: #1a5f4a; }
.target { font-size: 1rem; color: #909399; }
.unit { font-size: 0.8rem; color: #909399; }

.plan-hint {
  text-align: center;
  font-size: 13px;
  color: #67c23a;
  margin: -8px auto 16px;
}

.meal-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.meal-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.meal-card.completed { opacity: 0.85; }
.meal-card.skipped { opacity: 0.6; }

.meal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f5f7fa;
}

.meal-type {
  font-weight: 600;
}

.meal-body {
  display: flex;
  gap: 16px;
  padding: 16px;
  cursor: pointer;
}

.meal-body.empty {
  justify-content: center;
  color: #909399;
}

.meal-body.deleted-recipe {
  cursor: default;
  opacity: 0.75;
}
.meal-body.deleted-recipe .meal-img .placeholder-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #909399;
  background: #e8e8e8;
}
.meal-body.deleted-recipe .meal-title.text-muted {
  color: #909399;
}

.meal-img {
  width: 80px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
}

.meal-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.meal-info {
  flex: 1;
  min-width: 0;
}

.meal-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.meal-meta {
  font-size: 13px;
  color: #909399;
}

.match-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.meal-extra-actions {
  margin-top: 8px;
}

.nutrition-section {
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.nutrition-section h4 {
  margin-bottom: 12px;
}

.nutrition-chart-host {
  width: 100%;
  height: 240px;
  min-height: 200px;
  position: relative;
  box-sizing: border-box;
}

.nutrition-chart {
  display: block;
  width: 100%;
  height: 100%;
  margin: 0 auto;
}

.complete-meal-btn {
  background: #fff !important;
  color: #1a5f4a !important;
  border: 1px solid #1a5f4a !important;
}

@media (max-width: 470px) {
  .nutrition-chart-host {
    height: min(260px, 52vw);
    min-height: 200px;
  }
}

@media (min-width: 768px) {
  .nutrition-chart-host {
    height: min(280px, 32vh);
    min-height: 220px;
  }
}

.pc-layout {
  display: flex;
  gap: 24px;
}

.week-calendar {
  flex-shrink: 0;
  width: 120px;
  padding: 12px 16px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  align-self: flex-start;
  max-height: calc(100vh - 24px);
  overflow-y: auto;
}

.week-calendar h4 {
  margin-bottom: 12px;
  font-size: 0.9rem;
}

.week-day {
  padding: 8px 0;
  text-align: center;
  cursor: pointer;
  border-radius: 8px;
  margin-bottom: 4px;
}

.week-day:hover {
  background: #f5f7fa;
}

.week-day.active {
  background: #1a5f4a;
  color: #fff;
}

.day-name { display: block; font-size: 12px; }
.day-num { font-size: 1.2rem; font-weight: 600; }

.detail-panel {
  flex: 1;
  min-width: 0;
}

@media (min-width: 768px) {
  .plan-swipe-area.pc-layout {
    align-items: flex-start;
  }
  .detail-panel {
    min-height: 0;
    max-height: calc(100vh - 100px);
    overflow-y: auto;
  }
}

.feedback-sheet {
  padding: 0 16px 24px;
}
.feedback-stars {
  margin-bottom: 20px;
}
.feedback-item {
  margin-bottom: 10px;
}
.metric-label {
  display: inline-block;
  margin-bottom: 4px;
  color: #606266;
  font-size: 13px;
}
.feedback-tags .label {
  font-size: 14px;
  color: #606266;
  display: block;
  margin-bottom: 8px;
}
.feedback-tags .chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.feedback-tags .chip {
  padding: 6px 12px;
  border-radius: 16px;
  background: #f0f0f0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.feedback-tags .chip.selected {
  background: #1a5f4a;
  color: #fff;
}
.feedback-sheet .submit-btn {
  width: 100%;
  margin-top: 16px;
}
</style>
