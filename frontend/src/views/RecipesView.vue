<template>
  <div class="recipes-page">
    <!-- 移动端：顶部横向筛选 -->
    <div class="filter-bar" v-if="!isPC">
      <div class="filter-scroll">
        <div
          v-for="tab in categoryTabs"
          :key="tab.value"
          class="filter-tab"
          :class="{ active: activeCategory === tab.value }"
          @click="selectCategory(tab.value)"
        >
          {{ tab.label }}
        </div>
      </div>
    </div>

    <div class="layout" :class="{ pc: isPC }">
      <!-- 原型：左侧固定筛选区（PC） -->
      <aside v-if="isPC" class="left-filter">
        <div class="filter-title">筛选</div>
        <el-input v-model="keyword" placeholder="搜索菜名" clearable @clear="applyFilters" @keyup.enter="applyFilters" />

        <el-divider content-position="left">餐类型</el-divider>
        <el-radio-group v-model="mealType" class="filter-radio" @change="applyFilters">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="BREAKFAST">早餐</el-radio-button>
          <el-radio-button label="LUNCH">午餐</el-radio-button>
          <el-radio-button label="DINNER">晚餐</el-radio-button>
          <el-radio-button label="SNACK">加餐</el-radio-button>
        </el-radio-group>

        <el-divider content-position="left">难度</el-divider>
        <el-select v-model="difficulty" placeholder="难度" clearable style="width: 100%" @change="applyFilters">
          <el-option label="简单" value="EASY" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="困难" value="HARD" />
        </el-select>

        <el-divider content-position="left">所需时间</el-divider>
        <el-select v-model="maxTime" placeholder="时间上限" clearable style="width: 100%" @change="applyFilters">
          <el-option label="15分钟内" :value="15" />
          <el-option label="30分钟内" :value="30" />
          <el-option label="45分钟内" :value="45" />
        </el-select>

        <el-divider content-position="left">标签</el-divider>
        <el-select v-model="tag" placeholder="选择标签" clearable style="width: 100%" @change="applyFilters">
          <el-option label="高蛋白" value="HIGH_PROTEIN" />
          <el-option label="低脂/低卡" value="LOW_CALORIE" />
          <el-option label="低碳水" value="LOW_CARB" />
          <el-option label="快手菜" value="QUICK" />
        </el-select>

        <el-divider content-position="left">排序</el-divider>
        <el-select v-model="sort" placeholder="排序" style="width: 100%" @change="applyFilters">
          <el-option label="最匹配" value="match" />
          <el-option label="最新" value="newest" />
          <el-option label="最受欢迎" value="popular" />
        </el-select>

        <div class="filter-actions">
          <el-button type="primary" @click="applyFilters">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </aside>

      <main class="content">
        <!-- 推荐区 -->
        <div v-if="displayRecommend.length > 0" class="recommend-section">
          <div class="section-head">
            <h4>为你推荐</h4>
            <div class="sub">排序：{{ sortLabel }}{{ maxTime ? ` · ≤${maxTime}分钟` : '' }}</div>
          </div>
          <div class="recipe-grid" :class="{ 'grid-4': isPC, 'grid-2': !isPC }">
            <div
              v-for="r in displayRecommend"
              :key="r.id"
              class="recipe-card"
              @click="$router.push(`/recipe/${r.id}`)"
            >
              <div class="card-img">
                <img v-lazy="r.coverImage || '/vite.svg'" :alt="r.title" />
                <span v-if="showMatchBadge(r)" class="match-tag">适合你</span>
                <button class="fav-btn" @click.stop="toggleFav(r.id)">
                  {{ isFav(r.id) ? '★' : '☆' }}
                </button>
              </div>
              <div class="card-info">
                <div class="card-title">{{ r.title }}</div>
                <div class="card-meta">{{ getCalories(r) }}kcal · {{ r.cookingTime || '--' }}分钟</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 列表区 -->
        <div class="list-section">
          <div class="section-head">
            <h4>菜谱库</h4>
            <div class="section-actions">
              <div class="sub">收藏：{{ favIds.size }}</div>
              <el-button type="primary" size="small" @click="$router.push('/recipe/create')">发布菜谱</el-button>
            </div>
          </div>
          <RecipeGridSkeleton v-if="loading && recipeList.length === 0" :is-pc="isPC" :count="6" />
          <div v-else class="recipe-grid" :class="{ 'grid-4': isPC, 'grid-2': !isPC }">
            <div
              v-for="r in recipeList"
              :key="r.id"
              class="recipe-card"
              @click="$router.push(`/recipe/${r.id}`)"
            >
              <div class="card-img">
                <img v-lazy="r.coverImage || '/vite.svg'" :alt="r.title" />
                <span v-if="showMatchBadge(r)" class="match-tag">适合你</span>
                <button class="fav-btn" @click.stop="toggleFav(r.id)">
                  {{ isFav(r.id) ? '★' : '☆' }}
                </button>
              </div>
              <div class="card-info">
                <div class="card-title">{{ r.title }}</div>
                <div class="card-meta">
                  {{ getCalories(r) }}kcal · {{ r.cookingTime || '--' }}分钟 · {{ difficultyLabel(r.difficulty) }}
                </div>
              </div>
            </div>
          </div>
          <el-empty v-if="!loading && recipeList.length === 0" description="暂无菜谱" />
          <div v-if="total > 0" class="pager-wrap">
            <el-pagination
              v-model:current-page="page"
              background
              layout="prev, pager, next, total"
              :page-size="pageSize"
              :total="total"
              @current-change="onPageChange"
            />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { searchRecipes, getRecommend } from '../api/recipe'
import RecipeGridSkeleton from '../components/RecipeGridSkeleton.vue'
import { logError, logInfo } from '../utils/logger'

const isPC = ref(window.innerWidth >= 768)
const activeCategory = ref('')
const keyword = ref('')
const maxCalories = ref(null)
const difficulty = ref('')
const maxTime = ref(null)
const mealType = ref('')
const tag = ref('')
const sort = ref('match')
const recipeList = ref([])
const recommendList = ref([])
const page = ref(1)
// 与程序回档一致：菜谱库区分页每页 16 条；「为你推荐」固定 4 条单独加载
const pageSize = 16
const total = ref(0)
const loading = ref(false)

const categoryTabs = [
  { label: '全部', value: '' },
  { label: '早餐', value: 'BREAKFAST' },
  { label: '午餐', value: 'LUNCH' },
  { label: '晚餐', value: 'DINNER' },
  { label: '加餐', value: 'SNACK' }
]

const sortLabel = computed(() => ({ match: '最匹配', newest: '最新', popular: '最受欢迎' }[sort.value] || '最匹配'))

/** 「为你推荐」固定 4 道，不受左侧筛选/时间条件影响（与程序回档一致） */
const displayRecommend = computed(() => (recommendList.value || []).slice(0, 4))

function getCalories(r) {
  const ni = r.nutritionInfo
  if (ni?.calories != null) return Math.round(ni.calories)
  if (r.caloriesPer100g != null) return Math.round(r.caloriesPer100g * 2)
  return '--'
}

function difficultyLabel(v) {
  return ({ EASY: '简单', MEDIUM: '中等', HARD: '困难' }[v] || (v || '--'))
}

/** 仅当后端根据用户饮食标签与菜谱 match_tags 交集得到 personalMatchTags 时展示「适合你」 */
function showMatchBadge(recipe) {
  const p = recipe?.personalMatchTags
  if (!Array.isArray(p) || !p.length) return false
  const nonGeneric = p.filter((t) => !['LOW_CALORIE', 'BALANCED', 'LIGHT_MEAL', 'MAINTAIN_CAL'].includes(t))
  return nonGeneric.length > 0 || p.length >= 2
}

function selectCategory(val) {
  if (val.startsWith('tag:')) {
    activeCategory.value = ''
    tag.value = val.replace('tag:', '')
    applyFilters()
  } else {
    activeCategory.value = val
    mealType.value = val
    page.value = 1
    applyFilters()
  }
}

async function loadRecipes(opts = {}) {
  loading.value = true
  try {
    const p = opts.page ?? page.value
    const params = {
      page: p,
      size: pageSize,
      keyword: keyword.value || undefined,
      maxCalories: maxCalories.value || undefined,
      difficulty: difficulty.value || undefined,
      category: (opts.category ?? mealType.value ?? activeCategory.value) || undefined,
      tags: opts.tags ?? (tag.value ? [tag.value] : undefined),
      maxTime: maxTime.value || undefined,
      sort: sort.value
    }
    logInfo('RECIPES', 'search_start', params)
    const res = await searchRecipes(params)
    recipeList.value = res.data.records || []
    total.value = res.data.total || 0
    page.value = p
    logInfo('RECIPES', 'search_ok', { count: recipeList.value.length, total: total.value })
  } finally {
    loading.value = false
  }
}

async function loadRecommend() {
  try {
    const res = await getRecommend('LUNCH')
    recommendList.value = (res.data || []).slice(0, 4)
    logInfo('RECIPES', 'recommend_ok', { count: recommendList.value.length })
  } catch (_) {
    recommendList.value = []
  }
}

function applyFilters() {
  page.value = 1
  loadRecipes({ page: 1 })
}

function onPageChange(p) {
  page.value = p
  loadRecipes({ page: p })
}

function resetFilters() {
  keyword.value = ''
  maxCalories.value = null
  difficulty.value = ''
  maxTime.value = null
  mealType.value = ''
  tag.value = ''
  sort.value = 'match'
  activeCategory.value = ''
  applyFilters()
}

// 收藏：localStorage 持久化
const FAV_KEY = 'diet_fav_recipes_v1'
const favIds = ref(new Set())

function loadFav() {
  try {
    const raw = localStorage.getItem(FAV_KEY)
    const arr = raw ? JSON.parse(raw) : []
    favIds.value = new Set(Array.isArray(arr) ? arr : [])
  } catch (e) {
    favIds.value = new Set()
    logError('RECIPES', 'fav_load_failed', e?.message)
  }
}

function saveFav() {
  localStorage.setItem(FAV_KEY, JSON.stringify(Array.from(favIds.value)))
}

function isFav(id) {
  return favIds.value.has(id)
}

function toggleFav(id) {
  const set = new Set(favIds.value)
  if (set.has(id)) set.delete(id)
  else set.add(id)
  favIds.value = set
  saveFav()
  logInfo('RECIPES', 'fav_toggle', { id, fav: set.has(id) })
}

watch(activeCategory, (v) => {
  if (!v?.startsWith('tag:')) {
    page.value = 1
    mealType.value = v || ''
    applyFilters()
  }
})

onMounted(() => {
  logInfo('RECIPES', 'page_mounted')
  loadFav()
  loadRecipes()
  loadRecommend()
  window.addEventListener('resize', () => { isPC.value = window.innerWidth >= 768 })
})
</script>

<style scoped>
.recipes-page {
  padding-bottom: 80px;
}

.layout.pc {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
  align-items: start;
}

.left-filter {
  position: sticky;
  top: 72px;
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.filter-title {
  font-weight: 700;
  margin-bottom: 10px;
}

.filter-actions {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.filter-radio {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.content {
  min-width: 0;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.section-head .sub {
  font-size: 12px;
  color: #909399;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-bar {
  margin-bottom: 16px;
  overflow: hidden;
  position: sticky;
  top: 48px;
  z-index: 5;
  background: #f5f7fa;
  padding-bottom: 8px;
}

.filter-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 4px 0;
  -webkit-overflow-scrolling: touch;
}

.filter-scroll::-webkit-scrollbar {
  display: none;
}

.filter-tab {
  flex-shrink: 0;
  padding: 8px 16px;
  border-radius: 20px;
  background: #f5f7fa;
  font-size: 14px;
  cursor: pointer;
  white-space: nowrap;
}

.filter-tab.active {
  background: #1a5f4a;
  color: #fff;
}

.pc-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.recommend-section, .list-section {
  margin-bottom: 24px;
}

.recommend-section h4, .list-section h4 {
  font-size: 1rem;
  margin-bottom: 12px;
}

.pager-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.recipe-grid {
  display: grid;
  gap: 16px;
}

.recipe-grid.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.recipe-grid.grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.recipe-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.2s;
}

.recipe-card:hover {
  transform: translateY(-2px);
}

.card-img {
  position: relative;
  aspect-ratio: 4/3;
  background: #f5f7fa;
}

.card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.match-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  background: #1a5f4a;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}

.fav-btn {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.12);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c45656;
  transition: transform 0.15s ease, background 0.15s ease;
}

.fav-btn:hover {
  background: #fff;
  transform: scale(1.06);
}

.card-info {
  padding: 12px;
}

.card-title {
  font-weight: 500;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

@media (max-width: 767px) {
  .layout.pc { display: block; }
}
</style>
