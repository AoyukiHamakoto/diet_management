<template>
  <div class="recipe-detail" v-loading="loading">
    <PageBackBar fallback="/recipes" />
    <template v-if="recipe">
      <div class="detail-header">
        <div class="cover-wrap">
          <img :src="recipe.coverImage || '/vite.svg'" :alt="recipe.title" class="cover-img" />
        </div>
        <h1 class="title">{{ recipe.title }}</h1>
        <div class="meta">
          {{ getCalories() }}kcal · {{ recipe.cookingTime || '--' }}分钟 · {{ difficultyLabel }}
        </div>
      </div>

      <!-- 营养素饼图 -->
      <div v-if="hasNutrition" class="nutrition-chart">
        <h4>营养素占比</h4>
        <v-chart class="chart" :option="chartOption" autoresize />
      </div>

      <!-- 食材清单 -->
      <div class="ingredients-section">
        <h4>食材清单</h4>
        <div class="ingredient-list">
          <div
            v-for="(ing, i) in (recipe.ingredients || [])"
            :key="i"
            class="ingredient-item"
          >
            <el-checkbox v-model="ingredientChecked[i]">
              {{ ing.name }} {{ ing.amount || '' }}
            </el-checkbox>
          </div>
        </div>
      </div>

      <!-- 加入计划 + 分享 -->
      <div class="add-plan-section">
        <el-button type="primary" size="large" @click="showPlanDialog = true">
          加入今日计划
        </el-button>
        <el-button
          v-if="canShare"
          type="default"
          size="large"
          style="margin-top: 12px"
          @click="handleShare"
        >
          分享菜谱
        </el-button>
        <el-button
          v-if="showAppealEntry"
          type="danger"
          plain
          size="large"
          style="margin-top: 12px"
          @click="$router.push(`/appeal/submit?type=RECIPE&id=${recipe.id}`)"
        >
          对审核结果有异议？申诉
        </el-button>
      </div>

      <el-drawer v-model="showPlanDialog" title="选择餐次" direction="btt" size="40%">
        <el-radio-group v-model="selectedMeal" style="display: flex; flex-direction: column; gap: 12px">
          <el-radio label="BREAKFAST">早餐</el-radio>
          <el-radio label="LUNCH">午餐</el-radio>
          <el-radio label="DINNER">晚餐</el-radio>
          <el-radio label="SNACK">加餐</el-radio>
        </el-radio-group>
        <el-date-picker v-model="planDate" type="date" placeholder="选择日期" style="margin-top: 16px; width: 100%" />
        <el-button type="primary" style="margin-top: 16px; width: 100%" :loading="adding" @click="handleAddToPlan">
          确定
        </el-button>
      </el-drawer>
    </template>
    <el-empty v-else-if="!loading" description="菜谱不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent } from 'echarts/components'
import { getRecipe, addToPlan } from '../api/recipe'
import PageBackBar from '../components/PageBackBar.vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent])

const route = useRoute()
const userStore = useUserStore()
const showAppealEntry = computed(() => {
  const r = recipe.value
  return r?.status === 'REJECTED' && r?.creatorId != null && String(r.creatorId) === String(userStore.userId)
})
const recipe = ref(null)
const loading = ref(true)
const showPlanDialog = ref(false)
const selectedMeal = ref('LUNCH')
const planDate = ref(new Date())
const adding = ref(false)
const ingredientChecked = ref({})
const canShare = ref(typeof navigator !== 'undefined' && !!navigator.share)

async function handleShare() {
  if (!navigator.share || !recipe.value) return
  try {
    await navigator.share({
      title: recipe.value.title,
      text: `${recipe.value.title} - 来自健身饮食助手`,
      url: window.location.href
    })
    ElMessage.success('分享成功')
  } catch (e) {
    if (e.name !== 'AbortError') {
      ElMessage.warning('分享失败')
    }
  }
}

const difficultyLabel = computed(() => {
  const d = recipe.value?.difficulty
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d] || d || '--'
})

const hasNutrition = computed(() => {
  const ni = recipe.value?.nutritionInfo
  if (!ni) return false
  const p = ni.protein ?? ni.protein
  const c = ni.carb ?? ni.carbohydrate
  const f = ni.fat
  return (p || c || f) != null
})

const chartOption = computed(() => {
  const ni = recipe.value?.nutritionInfo || {}
  const p = Number(ni.protein) || 0
  const c = Number(ni.carb ?? ni.carbohydrate) || 0
  const f = Number(ni.fat) || 0
  const total = p + c + f
  if (total === 0) return {}
  return {
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { value: p, name: '蛋白质', itemStyle: { color: '#67c23a' } },
        { value: c, name: '碳水', itemStyle: { color: '#409eff' } },
        { value: f, name: '脂肪', itemStyle: { color: '#e6a23c' } }
      ]
    }]
  }
})

function getCalories() {
  const r = recipe.value
  const ni = r?.nutritionInfo
  if (ni?.calories != null) return Math.round(ni.calories)
  if (r?.caloriesPer100g != null) return Math.round(r.caloriesPer100g * 2)
  return '--'
}

async function loadRecipe() {
  loading.value = true
  try {
    const res = await getRecipe(route.params.id)
    recipe.value = res.data
    const ings = recipe.value?.ingredients || []
    ingredientChecked.value = Object.fromEntries(ings.map((_, i) => [i, false]))
  } finally {
    loading.value = false
  }
}

async function handleAddToPlan() {
  adding.value = true
  try {
    const d = planDate.value
    const dateStr = d ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}` : new Date().toISOString().slice(0, 10)
    await addToPlan(recipe.value.id, selectedMeal.value, dateStr)
    ElMessage.success('已加入计划')
    showPlanDialog.value = false
  } finally {
    adding.value = false
  }
}

onMounted(loadRecipe)
</script>

<style scoped>
.recipe-detail {
  min-height: 200px;
  padding-bottom: 80px;
}

.detail-header {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 20px;
}

.cover-wrap {
  aspect-ratio: 16/9;
  background: #f5f7fa;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.title {
  font-size: 1.25rem;
  padding: 16px;
  margin: 0;
}

.meta {
  padding: 0 16px 16px;
  color: #909399;
  font-size: 14px;
}

.nutrition-chart {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.nutrition-chart h4, .ingredients-section h4 {
  margin-bottom: 12px;
  font-size: 1rem;
}

.chart {
  height: 200px;
}

.ingredients-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.ingredient-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.ingredient-item:last-child {
  border-bottom: none;
}

.add-plan-section {
  padding: 16px;
}

.add-plan-section .el-button {
  width: 100%;
}
</style>
