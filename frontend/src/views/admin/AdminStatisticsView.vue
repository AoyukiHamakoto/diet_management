<template>
  <div class="admin-statistics">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>用户留存率</span>
          </template>
          <div class="retention-stats">
            <div class="retention-item">
              <span class="label">次日留存</span>
              <span class="value">{{ retention.nextDay?.toFixed(1) ?? '-' }}%</span>
            </div>
            <div class="retention-item">
              <span class="label">7日留存</span>
              <span class="value">{{ retention.day7?.toFixed(1) ?? '-' }}%</span>
            </div>
            <div class="retention-item">
              <span class="label">30日留存</span>
              <span class="value">{{ retention.day30?.toFixed(1) ?? '-' }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>计划完成率</span>
          </template>
          <div class="completion-stat">
            <el-progress
              :percentage="Math.round(planCompletion)"
              :color="planCompletion >= 60 ? '#67c23a' : planCompletion >= 30 ? '#e6a23c' : '#f56c6c'"
            />
            <p style="margin-top: 8px; color: #909399">近30天内生成计划中已完成的比例</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <span>热门过敏原统计</span>
          </template>
          <el-table :data="allergies" v-loading="loadingAllergies" style="width: 100%">
            <el-table-column prop="tag" label="过敏原" />
            <el-table-column prop="count" label="用户数" width="120" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRetentionRates, getPlanCompletion, getPopularAllergies } from '../../api/admin'

const retention = ref({})
const planCompletion = ref(0)
const allergies = ref([])
const loadingAllergies = ref(false)

async function load() {
  try {
    const [retRes, compRes, allRes] = await Promise.all([
      getRetentionRates(),
      getPlanCompletion(30),
      getPopularAllergies(10)
    ])
    retention.value = retRes.data || {}
    planCompletion.value = compRes.data?.completionRate ?? 0
    allergies.value = allRes.data || []
  } catch (_) {}
}

onMounted(() => load())
</script>

<style scoped>
.admin-statistics {
  padding: 0;
}

.retention-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.retention-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
}

.retention-item:last-child {
  border-bottom: none;
}

.retention-item .label {
  color: #606266;
}

.retention-item .value {
  font-size: 1.25rem;
  font-weight: 600;
  color: #303133;
}

.completion-stat {
  padding: 20px 0;
}
</style>
