<template>
  <div class="profile-setup">
    <PageBackBar fallback="/profile" />
    <div class="page-top">
      <div class="page-title">健康档案</div>
      <div class="page-sub">分区填写 + 实时计算 BMI / TDEE</div>
    </div>

    <div class="layout">
      <div class="main">
        <el-form :model="form" label-position="top" class="profile-form">
          <el-card class="section-card" shadow="never">
            <template #header><span class="section-title">基础信息</span></template>
            <div class="grid-2">
              <el-form-item label="身高（cm）">
                <el-input-number v-model="form.height" :min="50" :max="250" :precision="1" size="large" style="width: 100%" />
              </el-form-item>
              <el-form-item label="体重（kg）">
                <el-input-number v-model="form.weight" :min="20" :max="300" :precision="1" size="large" style="width: 100%" />
              </el-form-item>
              <el-form-item label="年龄（岁）">
                <el-input-number v-model="form.age" :min="1" :max="150" size="large" style="width: 100%" />
              </el-form-item>
              <el-form-item label="性别">
                <el-select v-model="form.gender" size="large" style="width: 100%">
                  <el-option label="男" value="MALE" />
                  <el-option label="女" value="FEMALE" />
                </el-select>
              </el-form-item>
            </div>
          </el-card>

          <el-card class="section-card" shadow="never">
            <template #header><span class="section-title">健身目标</span></template>
            <el-radio-group v-model="form.target" size="large" class="radio-row">
              <el-radio-button value="LOSE_WEIGHT">减脂</el-radio-button>
              <el-radio-button value="BUILD_MUSCLE">增肌</el-radio-button>
              <el-radio-button value="MAINTAIN">保持</el-radio-button>
            </el-radio-group>
          </el-card>

          <el-card class="section-card" shadow="never">
            <template #header><span class="section-title">运动习惯</span></template>
            <div class="grid-2">
              <el-form-item label="频率">
                <el-select v-model="form.exerciseFrequency" placeholder="请选择" size="large" style="width: 100%">
                  <el-option label="久坐（办公室）" value="NONE" />
                  <el-option label="轻度（每周1-3次）" value="LIGHT" />
                  <el-option label="中度（每周3-5次）" value="MODERATE" />
                  <el-option label="高强度（每周6-7次）" value="HIGH" />
                </el-select>
              </el-form-item>
              <el-form-item label="时段">
                <el-select v-model="form.exerciseTime" placeholder="请选择" size="large" style="width: 100%">
                  <el-option label="早晨（6:00-12:00）" value="MORNING" />
                  <el-option label="下午（12:00-18:00）" value="AFTERNOON" />
                  <el-option label="晚上（18:00-23:00）" value="EVENING" />
                  <el-option label="不固定/暂无" value="NONE" />
                </el-select>
              </el-form-item>
            </div>
          </el-card>

          <el-card class="section-card" shadow="never">
            <template #header><span class="section-title">饮食禁忌</span></template>
            <el-form-item label="过敏/禁忌标签（可多选）">
              <el-select v-model="form.allergyTags" multiple filterable placeholder="选择过敏食物" size="large" style="width: 100%">
                <el-option label="牛奶" value="牛奶" />
                <el-option label="鸡蛋" value="鸡蛋" />
                <el-option label="海鲜" value="海鲜" />
                <el-option label="花生" value="花生" />
                <el-option label="麸质" value="麸质" />
                <el-option label="大豆" value="大豆" />
                <el-option label="坚果" value="坚果" />
              </el-select>
            </el-form-item>
          </el-card>

          <div class="form-actions">
            <el-button type="info" plain @click="$router.back()">返回</el-button>
            <el-button type="default" :disabled="saving" @click="recalc">重新计算</el-button>
            <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
          </div>
        </el-form>

        <div class="danger-zone">
          <div class="danger-zone-title">危险区域</div>
          <el-button type="danger" plain :loading="clearing" @click="handleClearProfile">清空健康档案</el-button>
        </div>
      </div>

      <div class="aside">
        <el-card class="metrics-card" shadow="never">
          <template #header><span class="section-title">实时指标</span></template>

          <div class="metric">
            <div class="metric-label">BMI</div>
            <div class="metric-value" :class="bmiStatusClass">
              <span v-if="bmiValue != null">{{ bmiValue.toFixed(1) }}</span>
              <span v-else class="muted">--</span>
            </div>
            <div class="metric-sub" :class="bmiStatusClass">{{ bmiValue != null ? bmiRating : '填写身高体重后自动计算' }}</div>
            <el-progress v-if="bmiValue != null" :percentage="bmiProgress" :stroke-width="14" :color="bmiProgressColor" :show-text="false" />
          </div>

          <div class="divider" />

          <div class="metric">
            <div class="metric-label">TDEE（估算）</div>
            <div class="metric-value tdee">
              <span v-if="tdeeValue != null">{{ Math.round(tdeeValue) }}</span>
              <span v-else class="muted">--</span>
              <span class="unit">kcal/天</span>
            </div>
            <div class="metric-sub">基于 Mifflin-St Jeor + 活动系数</div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getHealthProfile, updateHealthProfile, clearHealthProfile } from '../api/user'
import PageBackBar from '../components/PageBackBar.vue'

const router = useRouter()
const clearing = ref(false)
const saving = ref(false)

const form = reactive({
  gender: 'MALE',
  age: 25,
  height: null,
  weight: null,
  target: 'MAINTAIN',
  exerciseFrequency: 'NONE',
  exerciseTime: 'NONE',
  allergyTags: []
})

const bmiValue = ref(null)
const bmiRating = ref('')
const bmiProgress = ref(0)
const bmiProgressColor = ref('#67c23a')
const bmiStatusClass = ref('')

function calcBmi() {
  const h = form.height
  const w = form.weight
  if (h && w && h > 0 && w > 0) {
    const heightM = h / 100
    bmiValue.value = w / (heightM * heightM)
    if (bmiValue.value < 18.5) {
      bmiRating.value = '偏瘦'
      bmiStatusClass.value = 'underweight'
      bmiProgress.value = Math.min(100, (bmiValue.value / 18.5) * 50)
      bmiProgressColor.value = '#e6a23c'
    } else if (bmiValue.value < 24) {
      bmiRating.value = '正常'
      bmiStatusClass.value = 'normal'
      bmiProgress.value = 50 + ((bmiValue.value - 18.5) / (24 - 18.5)) * 30
      bmiProgressColor.value = '#67c23a'
    } else if (bmiValue.value < 28) {
      bmiRating.value = '偏胖'
      bmiStatusClass.value = 'overweight'
      bmiProgress.value = 80 + ((bmiValue.value - 24) / (28 - 24)) * 15
      bmiProgressColor.value = '#e6a23c'
    } else {
      bmiRating.value = '肥胖'
      bmiStatusClass.value = 'obese'
      bmiProgress.value = Math.min(100, 95 + (bmiValue.value - 28) / 10)
      bmiProgressColor.value = '#f56c6c'
    }
  } else {
    bmiValue.value = null
  }
}

const tdeeValue = computed(() => {
  const w = Number(form.weight)
  const h = Number(form.height)
  const age = Number(form.age)
  if (!w || !h || !age) return null
  const bmr = form.gender === 'FEMALE'
    ? (10 * w + 6.25 * h - 5 * age - 161)
    : (10 * w + 6.25 * h - 5 * age + 5)
  const activity = { NONE: 1.2, LIGHT: 1.375, MODERATE: 1.55, HIGH: 1.725 }[form.exerciseFrequency] ?? 1.2
  return bmr * activity
})

watch(() => [form.height, form.weight], calcBmi, { immediate: true })

onMounted(async () => {
  try {
    const res = await getHealthProfile()
    const p = res.data
    if (p?.height) form.height = p.height
    if (p?.weight) form.weight = p.weight
    if (p?.gender) form.gender = p.gender
    if (p?.age) form.age = p.age
    if (p?.target) form.target = p.target
    if (p?.exerciseFrequency) form.exerciseFrequency = p.exerciseFrequency
    if (p?.exerciseTime) form.exerciseTime = p.exerciseTime
    if (p?.allergyTags?.length) form.allergyTags = [...p.allergyTags]
    calcBmi()
  } catch (_) {}
})

async function handleSubmit() {
  saving.value = true
  try {
    await updateHealthProfile({
      gender: form.gender,
      age: form.age,
      height: form.height,
      weight: form.weight,
      target: form.target,
      exerciseFrequency: form.exerciseFrequency,
      exerciseTime: form.exerciseTime,
      allergyTags: form.allergyTags
    })
    ElMessage.success('健康档案已保存')
    router.push('/profile')
  } finally {
    saving.value = false
  }
}

function recalc() {
  calcBmi()
  ElMessage.success('已重新计算指标')
}

async function handleClearProfile() {
  try {
    await ElMessageBox.confirm(
      '此操作将清空您的身高、体重、健身目标等所有健康数据，已生成的饮食计划历史将保留，但您需要重新填写档案才能生成新计划。此操作不可恢复。',
      '确认清空健康档案？',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  clearing.value = true
  try {
    await clearHealthProfile()
    ElMessage.success('健康档案已清空')
    router.push('/profile')
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '清空失败')
  } finally {
    clearing.value = false
  }
}
</script>

<style scoped>
.profile-setup {
  padding-bottom: 80px;
}

.page-top {
  margin-bottom: 16px;
}

.page-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #303133;
}

.page-sub {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

.main {
  min-width: 0;
}

.aside {
  position: static;
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-card {
  border-radius: 12px;
}

.section-title {
  font-weight: 600;
  color: #303133;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.radio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

.danger-zone {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.danger-zone-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.metrics-card {
  border-radius: 12px;
}

.metric {
  margin-bottom: 12px;
}

.metric-label {
  font-size: 12px;
  color: #909399;
}

.metric-value {
  margin-top: 6px;
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1;
}

.metric-value.tdee {
  font-size: 1.5rem;
}

.metric-value .unit {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
  font-weight: 600;
}

.metric-sub {
  margin: 6px 0 10px;
  font-size: 12px;
  color: #909399;
}

.muted {
  color: #c0c4cc;
}

.divider {
  height: 1px;
  background: #f0f0f0;
  margin: 12px 0;
}

.normal { color: #67c23a; }
.underweight { color: #e6a23c; }
.overweight { color: #e6a23c; }
.obese { color: #f56c6c; }

@media (min-width: 992px) {
  .layout {
    grid-template-columns: 1fr 320px;
    align-items: start;
  }
  .aside {
    position: sticky;
    top: 16px;
  }
  .grid-2 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
