/** 饮食标签英文名 -> 中文展示（健康档案、评价等统一使用） */
export const DIET_TAG_LABELS = {
  LOW_CALORIE: '低卡',
  HIGH_PROTEIN: '高蛋白',
  HIGH_CALORIE: '高热量',
  DAIRY_FREE: '无乳制品',
  LOW_CARB: '低碳水',
  GLUTEN_FREE: '无麸质',
  BALANCED: '均衡饮食',
  MAINTAIN_CAL: '维持热量',
  LIGHT_MEAL: '清淡/轻食',
  AGGRESSIVE_FAT_LOSS: '强化减脂',
  POST_WORKOUT_MEAL: '练后餐',
  MUSCLE_BUILD: '增肌向',
  SEAFOOD_FREE: '无海鲜',
  NUT_FREE: '无坚果',
  AVOID_OILY: '少油',
  QUICK: '快手菜',
  // 兼容规则/偏好中的内部标签
  AVOID_DAIRY: '无乳制品',
  HIGH_FIBER: '高纤维',
  VEGETARIAN: '素食',
  VEGAN: '纯素',
  KETO: '生酮倾向',
  MEDITERRANEAN: '地中海式',
  ANTI_INFLAMMATORY: '抗炎饮食',
  HEART_HEALTHY: '护心低脂',
  SODIUM_AWARE: '控盐',
  SUGAR_AWARE: '控糖',
  MEAL_PREP: '备餐友好',
  BUDGET_FRIENDLY: '经济实惠',
  KID_FRIENDLY: '儿童友好',
  ELDERLY_FRIENDLY: '易消化',
  SPICY_AWARE: '忌辛辣',
  ALCOHOL_FREE: '无酒精',
  // 餐后反馈标签
  TASTE_GOOD: '口味不错',
  PERFECT: '满意',
  TOO_BLAND: '偏淡',
  TOO_MUCH: '量偏大',
  TOO_LITTLE: '量偏少',
  NOT_FRESH: '不够新鲜',
  HARD_TO_COOK: '制作偏难'
}

export function dietTagLabel(name) {
  if (!name || typeof name !== 'string') return ''
  return DIET_TAG_LABELS[name] || name
}
