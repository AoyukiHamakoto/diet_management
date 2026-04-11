<template>
  <div class="recipe-create-page">
    <PageBackBar fallback="/recipes" />
    <el-form ref="formRef" :model="form" label-width="100px" class="create-form">
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
          <el-input v-model="ing.name" placeholder="食材名" style="flex:1" />
          <el-input v-model="ing.amount" placeholder="克" style="width: 90px" type="number" min="0" />
          <el-button type="danger" text @click="form.ingredients.splice(i, 1)">删除</el-button>
        </div>
        <el-button type="primary" text @click="form.ingredients.push({ name: '', amount: '' })">+ 添加食材</el-button>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交审核</el-button>
        <el-button @click="$router.push('/recipes')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createRecipe } from '../api/recipe'
import PageBackBar from '../components/PageBackBar.vue'

const router = useRouter()
const submitting = ref(false)
const form = reactive({
  title: '',
  coverImage: '',
  category: 'LUNCH',
  cookingTime: null,
  difficulty: 'EASY',
  ingredients: [{ name: '', amount: '' }],
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

async function handleSubmit() {
  if (!form.title?.trim()) {
    ElMessage.warning('请填写菜谱名称')
    return
  }
  if (!form.category) {
    ElMessage.warning('请选择分类')
    return
  }
  submitting.value = true
  try {
    await createRecipe(buildPayload())
    ElMessage.success('提交成功，等待管理员审核')
    router.push('/my-recipes')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.recipe-create-page {
  padding-bottom: 80px;
}
.create-form {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.ingredient-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
