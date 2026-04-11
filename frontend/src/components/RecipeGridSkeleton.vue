<template>
  <div class="recipe-skeleton" :class="{ 'grid-2': !isPC, 'grid-4': isPC }">
    <div v-for="i in count" :key="i" class="skeleton-card">
      <div class="skeleton-cover" />
      <div class="skeleton-title" />
      <div class="skeleton-meta" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  count: { type: Number, default: 6 },
  isPC: { type: Boolean, default: false }
})
</script>

<style scoped>
.recipe-skeleton {
  display: grid;
  gap: 16px;
}

.recipe-skeleton.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.recipe-skeleton.grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.skeleton-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.skeleton-cover {
  aspect-ratio: 4/3;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-title {
  height: 16px;
  margin: 12px 12px 0;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-meta {
  height: 12px;
  width: 60%;
  margin: 8px 12px 12px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
