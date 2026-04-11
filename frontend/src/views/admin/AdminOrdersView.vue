<template>
  <div class="toolbar">
    <el-input v-model="keyword" placeholder="订单号/商品名" clearable style="max-width: 260px" />
    <el-select v-model="status" clearable placeholder="状态" style="width: 140px">
      <el-option label="待支付" value="PENDING" />
      <el-option label="已支付" value="PAID" />
      <el-option label="已退款" value="REFUNDED" />
    </el-select>
    <el-button type="primary" @click="load">查询</el-button>
  </div>

  <el-table v-if="rows.length" :data="rows" style="width: 100%" border>
    <el-table-column prop="id" label="订单ID" width="100" />
    <el-table-column prop="orderNo" label="订单号" width="180" />
    <el-table-column prop="userId" label="用户ID" width="100" />
    <el-table-column prop="itemName" label="内容" />
    <el-table-column prop="amount" label="金额" width="100" />
    <el-table-column label="状态" width="160">
      <template #default="{ row }">
        <el-select :model-value="row.status" size="small" @change="(v) => changeStatus(row.id, v)">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column prop="createTime" label="创建时间" width="180" />
  </el-table>

  <el-empty v-else description="暂无订单数据" />
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listOrders, updateOrderStatus } from '../../api/admin'
import { logInfo } from '../../utils/logger'

const rows = ref([])
const status = ref('')
const keyword = ref('')

async function load() {
  const params = { page: 1, size: 50, status: status.value || undefined, keyword: keyword.value || undefined }
  logInfo('ADMIN_ORDERS', 'load_orders', params)
  const res = await listOrders(params)
  rows.value = res.data?.records || []
}

async function changeStatus(id, v) {
  await updateOrderStatus(id, v)
  ElMessage.success('状态已更新')
  logInfo('ADMIN_ORDERS', 'change_status', { id, status: v })
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
</style>

