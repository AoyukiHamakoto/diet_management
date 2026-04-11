<template>
  <div class="plan-skeleton">
    <div class="skeleton-ring">
      <div class="skeleton-circle" />
      <div class="skeleton-center" />
    </div>
    <div class="skeleton-meals">
      <div v-for="i in 4" :key="i" class="skeleton-meal">
        <div class="skeleton-img" />
        <div class="skeleton-info">
          <div class="skeleton-line w80" />
          <div class="skeleton-line w50" />
          <div class="skeleton-line w60" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.plan-skeleton {
  padding: 20px 0;
}

.skeleton-ring {
  position: relative;
  width: 160px;
  height: 160px;
  margin: 0 auto 24px;
}

.skeleton-circle {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #f5f7fa;
}

.skeleton-meals {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-meal {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.skeleton-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line.w80 { width: 80%; }
.skeleton-line.w50 { width: 50%; }
.skeleton-line.w60 { width: 60%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
