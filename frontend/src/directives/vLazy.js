/**
 * v-lazy: 图片懒加载指令（IntersectionObserver）
 * 使用: <img v-lazy="imageUrl" alt="..." />
 */
export default {
  mounted(el, binding) {
    const src = binding.value
    if (!src) return
    el.dataset.src = src
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            el.src = el.dataset.src || src
            el.classList.add('lazy-loaded')
            observer.unobserve(el)
          }
        })
      },
      { rootMargin: '100px', threshold: 0.01 }
    )
    observer.observe(el)
  },
  updated(el, binding) {
    const src = binding.value
    if (!src) return
    el.dataset.src = src
    if (el.classList.contains('lazy-loaded')) {
      el.src = src
    }
  }
}
