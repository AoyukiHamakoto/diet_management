/** Matches backend RuleFileService logical names. */
export const RULE_FILE_META = {
  'diet-label-rules.drl': {
    title: '饮食标签规则',
    subtitle: '用户档案 → 个性化标签',
    description:
      '根据 BMI、健身目标、过敏原、运动频率等，为用户生成「高蛋白」「低卡」「无乳制品」等饮食标签，供推荐与计划使用。',
    tag: '标签生成',
    color: '#1a5f4a'
  },
  'meal-plan-rules.drl': {
    title: '用餐计划建议规则',
    subtitle: '档案 + 计划建议 → 热量与模式',
    description:
      '在用户生成用餐计划建议时，按减脂/增肌、BMI、运动时段、周末等条件调整建议热量、模式与提示标记。',
    tag: '计划建议',
    color: '#2d8f6f'
  }
}

export function getRuleMeta(filename) {
  return (
    RULE_FILE_META[filename] || {
      title: filename,
      subtitle: 'Drools 规则文件',
      description: '编辑将影响线上规则引擎行为，保存前会自动备份。',
      tag: '规则',
      color: '#409eff'
    }
  )
}
