/**
 * 长按触发
 * @param {Ref} elRef - 元素 ref
 * @param {Function} onLongPress - 长按回调
 * @param {Object} options - { delay: 500 }
 */
import { onMounted, onBeforeUnmount } from 'vue'

export function useLongPress(elRef, onLongPress, options = {}) {
  const delay = options.delay ?? 500
  let timer = null

  function clear() {
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  function handleTouchStart() {
    clear()
    timer = setTimeout(() => {
      onLongPress?.()
      timer = null
    }, delay)
  }

  function handleTouchEnd() {
    clear()
  }

  function handleTouchMove() {
    clear()
  }

  onMounted(() => {
    const node = elRef?.value ?? elRef
    if (node) {
      node.addEventListener('touchstart', handleTouchStart, { passive: true })
      node.addEventListener('touchend', handleTouchEnd, { passive: true })
      node.addEventListener('touchmove', handleTouchMove, { passive: true })
    }
  })

  onBeforeUnmount(() => {
    clear()
    const node = elRef?.value ?? elRef
    if (node) {
      node.removeEventListener('touchstart', handleTouchStart)
      node.removeEventListener('touchend', handleTouchEnd)
      node.removeEventListener('touchmove', handleTouchMove)
    }
  })
}
