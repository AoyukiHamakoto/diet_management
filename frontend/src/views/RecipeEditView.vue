<template>
  <div class="recipe-edit-page" v-loading="loading">
    <PageBackBar fallback="/my-recipes" />
    <template v-if="recipe">
      <div v-if="recipe.status === 'APPROVED'" class="not-allowed">
        <el-alert type="warning" title="已通过审核的菜谱不能修改" show-icon />
        <el-button type="primary" style="margin-top: 16px" @click="$router.push('/my-recipes')">返回我的菜谱</el-button>
      </div>
      <el-form v-else ref="formRef" :model="form" label-width="100px" class="edit-form">
        <el-form-item label="菜谱名称" required>
          <el-input v-model="form.title" placeholder="请输入菜谱名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="封面图URL">
          <el-input v-model="form.coverImage" placeholder="图片地址或留空" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option label="早餐" value="BREAKFAST" />
            <el-option label="午餐" value="LUNCH" />
            <el-option label="晚餐" value="DINNER" />
            <el-option label="加餐" value="SNACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="烹饪时间(分钟)">
          <el-input-number v-model="form.cookingTime" :min="1" :max="300" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" placeholder="请选择" style="width: 100%">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(kcal)">
          <el-input-number v-model="nutritionCalories" :min="0" :max="2000" placeholder="单份热量" />
        </el-form-item>
        <el-form-item label="标签(逗号分隔)">
          <el-input v-model="tagsStr" placeholder="如：低卡,高蛋白" />
        </el-form-item>
        <el-form-item label="匹配标签(逗号分隔)">
          <el-input v-model="matchTagsStr" placeholder="如：HIGH_PROTEIN,LOW_CALORIE" />
        </el-form-item>
        <el-form-item label="食材">
          <div v-for="(ing, i) in form.ingredients" :key="i" class="ingredient-row">
            <el-autocomplete
              v-model="ing.name"
              placeholder="食材名（可搜索标准库）"
              :fetch-suggestions="(q, cb) => fetchMatchSuggestions(q, cb, i)"
              style="flex:1"
              clearable
              @select="(item) => onSelectFood(item, i)"
            >
              <template #default="{ item }">
                <span>{{ item.foodName }}</span>
                <span class="suggest-meta">{{ item.foodCategory }} · {{ item.energyKcal || 0 }}kcal/100g</span>
              </template>
            </el-autocomplete>
            <el-input v-model="ing.amount" placeholder="克" style="width: 90px" type="number" min="0" />
            <el-button type="danger" text @click="form.ingredients.splice(i, 1)">删除</el-button>
          </div>
          <el-button type="primary" text @click="form.ingredients.push({ name: '', amount: '', foodComponentId: null })">+ 添加食材</el-button>
          <div v-if="calculatedNutrition" class="nutrition-result">
            <h4>根据标准库计算（已匹配食材）</h4>
            <div class="nutrition-grid">
              <span>热量 {{ calculatedNutrition.totalCalories }} kcal</span>
              <span>蛋白质 {{ calculatedNutrition.totalProtein }}g</span>
              <span>脂肪 {{ calculatedNutrition.totalFat }}g</span>
              <span>碳水 {{ calculatedNutrition.totalCarbs }}g</span>
            </div>
          </div>
          <el-button type="success" plain style="margin-top: 8px" :loading="calculating" @click="handleCalculateNutrition">
            计算营养
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">保存修改</el-button>
          <el-button @click="$router.push('/my-recipes')">取消</el-button>
        </el-form-item>
      </el-form>
    </template>
    <el-empty v-else-if="!loading && loadError" :description="loadError" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecipe, updateRecipe } from '../api/recipe'
import PageBackBar from '../components/PageBackBar.vue'
import { matchFoodComponents, calculateNutrition } from '../api/foodComponent'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const submitting = ref(false)
const loadError = ref('')
const recipe = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '',
  coverImage: '',
  category: '',
  cookingTime: null,
  difficulty: '',
  ingredients: [],
  nutritionInfo: {},
  tags: [],
  matchTags: [],
  caloriesPer100g: null,
  proteinCarbFatRatio: ''
})

const nutritionCalories = computed({
  get() {
    const v = form.nutritionInfo?.calories
    return v != null ? (typeof v === 'number' ? v : Number(v)) : null
  },
  set(val) {
    if (!form.nutritionInfo) form.nutritionInfo = {}
    form.nutritionInfo.calories = val
  }
})

const tagsStr = computed({
  get() { return (form.tags || []).join(', ') },
  set(s) { form.tags = s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : [] }
})

const matchTagsStr = computed({
  get() { return (form.matchTags || []).join(', ') },
  set(s) { form.matchTags = s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : [] }
})

const wasRejected = computed(() => recipe.value?.status === 'REJECTED')
const calculating = ref(false)
const calculatedNutrition = ref(null)

async function fetchMatchSuggestions(queryString, cb, rowIndex) {
  if (!queryString || queryString.length < 1) {
    cb([])
    return
  }
  try {
    const res = await matchFoodComponents(queryString)
    const list = res.data || []
    cb(list.map(f => ({ ...f, value: f.foodName })))
  } catch {
    cb([])
  }
}

function onSelectFood(item, rowIndex) {
  const ing = form.ingredients[rowIndex]
  if (ing) {
    ing.foodComponentId = item.id
    ing.name = item.foodName
  }
}

async function handleCalculateNutrition() {
  const list = form.ingredients
    .filter(i => i.foodComponentId && i.amount && Number(i.amount) > 0)
    .map(i => ({ foodComponentId: i.foodComponentId, amount: Number(i.amount) }))
  if (list.length === 0) {
    ElMessage.warning('请先从标准库选择食材并填写用量（克）')
    return
  }
  calculating.value = true
  calculatedNutrition.value = null
  try {
    const res = await calculateNutrition(list)
    const d = res.data
    calculatedNutrition.value = {
      totalCalories: d.totalCalories ?? 0,
      totalProtein: (d.totalProtein != null ? Number(d.totalProtein) : 0).toFixed(1),
      totalFat: (d.totalFat != null ? Number(d.totalFat) : 0).toFixed(1),
      totalCarbs: (d.totalCarbs != null ? Number(d.totalCarbs) : 0).toFixed(1)
    }
  } catch {
    ElMessage.error('计算失败')
  } finally {
    calculating.value = false
  }
}

function mapRecipeToForm(r) {
  form.title = r.title || ''
  form.coverImage = r.coverImage || ''
  form.category = r.category || ''
  form.cookingTime = r.cookingTime ?? null
  form.difficulty = r.difficulty || ''
  form.ingredients = Array.isArray(r.ingredients) && r.ingredients.length
    ? r.ingredients.map(x => ({ name: x.name || '', amount: x.amount || '', foodComponentId: x.foodComponentId ?? null }))
    : [{ name: '', amount: '', foodComponentId: null }]
  form.nutritionInfo = r.nutritionInfo && typeof r.nutritionInfo === 'object' ? { ...r.nutritionInfo } : {}
  form.tags = Array.isArray(r.tags) ? [...r.tags] : []
  form.matchTags = Array.isArray(r.matchTags) ? [...r.matchTags] : []
  form.caloriesPer100g = r.caloriesPer100g != null ? r.caloriesPer100g : null
  form.proteinCarbFatRatio = r.proteinCarbFatRatio || ''
}

function buildPayload() {
  const payload = {
    title: form.title?.trim() || undefined,
    coverImage: form.coverImage || undefined,
    category: form.category || undefined,
    cookingTime: form.cookingTime ?? undefined,
    difficulty: form.difficulty || undefined,
    ingredients: form.ingredients.filter(i => i.name?.trim()).length ? form.ingredients : undefined,
    nutritionInfo: Object.keys(form.nutritionInfo || {}).length ? form.nutritionInfo : undefined,
    tags: form.tags?.length ? form.tags : undefined,
    matchTags: form.matchTags?.length ? form.matchTags : undefined,
    caloriesPer100g: form.caloriesPer100g ?? undefined,
    proteinCarbFatRatio: form.proteinCarbFatRatio || undefined
  }
  return Object.fromEntries(Object.entries(payload).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

async function loadRecipe() {
  loading.value = true
  loadError.value = ''
  recipe.value = null
  try {
    const res = await getRecipe(route.params.id)
    recipe.value = res.data
    if (recipe.value.status === 'APPROVED') {
      return
    }
    mapRecipeToForm(recipe.value)
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '加载失败'
    const code = e.response?.status
    if (code === 403) loadError.value = '无权修改该菜谱'
    else if (code === 409) loadError.value = '已通过审核的菜谱不能修改'
    else if (code === 404) loadError.value = '菜谱不存在'
    else loadError.value = msg
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!form.title?.trim()) {
    ElMessage.warning('请输入菜谱名称')
    return
  }
  if (wasRejected.value) {
    try {
      await ElMessageBox.confirm('修改后将重新提交审核，是否继续？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })
    } catch {
      return
    }
  }
  const payload = buildPayload()
  if (Object.keys(payload).length === 0) {
    ElMessage.warning('请至少修改一项内容')
    return
  }
  submitting.value = true
  try {
    await updateRecipe(route.params.id, payload)
    ElMessage.success('保存成功，将重新进入审核')
    router.push('/my-recipes')
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '保存失败'
    if (e.response?.status === 409) {
      ElMessage.warning('菜谱状态已变更，请刷新后重试')
      loadRecipe()
    } else {
      ElMessage.error(msg)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(loadRecipe)
</script>

<style scoped>
.recipe-edit-page {
  padding-bottom: 80px;
}
.not-allowed {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
}
.edit-form {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.ingredient-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
.suggest-meta {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
.nutrition-result {
  margin-top: 12px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 8px;
}
.nutrition-result h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #67c23a;
}
.nutrition-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
}
</style>
