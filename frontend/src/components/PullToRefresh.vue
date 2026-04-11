<template>
  <div
    ref="containerRef"
    class="pull-to-refresh"
    @touchstart.passive="onTouchStart"
    @touchmove="onTouchMove"
    @touchend="onTouchEnd"
  >
    <div class="pull-indicator" :style="{ height: `${Math.max(0, pullDistance)}px` }">
      <div v-if="pullDistance > 0" class="indicator-inner">
        <el-icon v-if="!refreshing" class="pull-icon" :class="{ ready: pullDistance > threshold }">
          <ArrowDown />
        </el-icon>
        <el-icon v-else class="is-loading">
          <Loading />
        </el-icon>
        <span class="pull-text">{{ pullText }}</span>
      </div>
    </div>
    <div class="pull-content">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowDown, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  onRefresh: { type: Function, required: true },
  disabled: { type: Boolean, default: false }
})

const containerRef = ref(null)
const startY = ref(0)
const pullDistance = ref(0)
const refreshing = ref(false)
const threshold = 60

const pullText = computed(() => {
  if (refreshing.value) return '刷新中...'
  if (pullDistance.value > threshold) return '释放刷新'
  return '下拉刷新'
})

function onTouchStart(e) {
  if (props.disabled || refreshing.value) return
  startY.value = e.touches[0].clientY
}

function onTouchMove(e) {
  if (props.disabled || refreshing.value) return
  const y = e.touches[0].clientY
  const scrollEl = document.scrollingElement || document.documentElement
  const scrollTop = scrollEl.scrollTop
  if (scrollTop <= 5 && y > startY.value) {
    const dist = Math.min((y - startY.value) * 0.5, 120)
    pullDistance.value = dist
    e.preventDefault()
  }
}

async function onTouchEnd() {
  if (props.disabled || refreshing.value) return
  if (pullDistance.value >= threshold) {
    refreshing.value = true
    try {
      await props.onRefresh()
    } finally {
      refreshing.value = false
    }
  }
  pullDistance.value = 0
}
</script>

<style scoped>
.pull-to-refresh {
  position: relative;
  min-height: 100%;
}

.pull-indicator {
  position: relative;
  left: 0;
  right: 0;
  min-height: 0;
  overflow: hidden;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  margin-top: -1px;
}

.indicator-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 50px;
  font-size: 14px;
  color: #909399;
}

.pull-icon.ready {
  transform: rotate(180deg);
}

.pull-content {
  min-height: 100%;
}
</style>
