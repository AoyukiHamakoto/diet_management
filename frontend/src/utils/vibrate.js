/**
 * 震动反馈（完成打卡等场景）
 */
export function vibrate(pattern = 50) {
  if (typeof navigator !== 'undefined' && navigator.vibrate) {
    navigator.vibrate(pattern)
  }
}
