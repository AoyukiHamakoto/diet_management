/**
 * 左右滑动切换日期
 * @param {Ref} containerRef - 容器元素
 * @param {Function} onSwipeLeft - 左滑回调（下一天）
 * @param {Function} onSwipeRight - 右滑回调（上一天）
 * @param {Object} options - { minSwipeDistance: 50 }
 */
import { onMounted, onBeforeUnmount } from 'vue'

export function useSwipe(containerRef, onSwipeLeft, onSwipeRight, options = {}) {
  const minDistance = options.minSwipeDistance ?? 50
  let startX = 0
  let startY = 0

  function handleTouchStart(e) {
    startX = e.touches[0].clientX
    startY = e.touches[0].clientY
  }

  function handleTouchEnd(e) {
    const endX = e.changedTouches[0].clientX
    const endY = e.changedTouches[0].clientY
    const dx = endX - startX
    const dy = endY - startY
    if (Math.abs(dx) < minDistance) return
    if (Math.abs(dx) < Math.abs(dy)) return
    if (dx > 0) {
      onSwipeRight?.()
    } else {
      onSwipeLeft?.()
    }
  }

  onMounted(() => {
    const node = containerRef?.value ?? containerRef
    if (node) {
      node.addEventListener('touchstart', handleTouchStart, { passive: true })
      node.addEventListener('touchend', handleTouchEnd, { passive: true })
    }
  })

  onBeforeUnmount(() => {
    const node = containerRef?.value ?? containerRef
    if (node) {
      node.removeEventListener('touchstart', handleTouchStart)
      node.removeEventListener('touchend', handleTouchEnd)
    }
  })
}
