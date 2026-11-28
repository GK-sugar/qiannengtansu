<template>
  <div class="page">
    <template v-if="stat">
      <div class="city-head">
        <div class="ch-left">
          <el-button round class="ghost-btn" @click="goScreen"><el-icon><Back /></el-icon>返回态势大屏</el-button>
          <el-button round class="ghost-btn" @click="goDashboard">省级工作台</el-button>
        </div>
        <div class="ch-main">
          <div class="ch-tag">市州专页 · 数据隔离</div>
          <h1 class="ch-title">{{ stat.short }}<span v-if="fullName" class="ch-full">{{ fullName }}</span></h1>
          <div class="ch-sub">
            纳入监测企业 {{ detail?.enterprises ?? '—' }} 家 ｜ 占全省排放
            <b>{{ share }}%</b> ｜ 统计期 2026-10
          </div>
        </div>
        <div class="ch-badge">
          <svg class="ch-ring" viewBox="0 0 44 44">
            <circle cx="22" cy="22" r="18" fill="none" stroke="rgba(0,181,129,.18)" stroke-width="4" />
            <circle cx="22" cy="22" r="18" fill="none" stroke="#00b581" stroke-width="4" stroke-linecap="round"
              :stroke-dasharray="`${share * 1.13} 113`" transform="rotate(-90 22 22)" />
          </svg>
          <div class="ch-badge-text"><b>{{ stat.short }}</b><span>本页仅含该市数据</span></div>
        </div>
      </div>

      <div class="stat-grid">
        <StatCard label="排放总量" :value="stat.emission" suffix=" tCO₂e" icon="Odometer" extra="2026-10 核算值" />
        <StatCard label="碳强度" :value="detail?.intensity ?? 0" :animated="false" suffix=" t/万元" icon="TrendCharts" tint="#00d1ff" extra="单位工业增加值" />
        <div class="handle-card" title="点击进入智能初审台处理" @click="goHandle">
          <StatCard label="待处置预警" :value="stat.warnings" suffix=" 项" icon="Warning" tint="#ffb020" extra="点击进入初审台处理" />
          <span class="handle-link">去处理<el-icon :size="11"><ArrowRight /></el-icon></span>
        </div>
        <StatCard label="纳入企业" :value="detail?.enterprises ?? 0" suffix=" 家" icon="OfficeBuilding" tint="#a06bff" :extra="`重点排放企业 ${detail?.companies.length ?? 0} 家`" :trend="`+${detail?.growth ?? 0}%`" />
      </div>

      <div class="main-grid">
        <div class="page-card">
          <div class="page-card-title"><h3>月度排放趋势 · 2026</h3><el-tag size="small" round type="success">{{ stat.short }}口径</el-tag></div>
          <div ref="lineRef" class="chart-box"></div>
        </div>
        <div class="page-card">
          <div class="page-card-title"><h3>行业排放结构</h3><span class="hint">按排放量占比</span></div>
          <div ref="pieRef" class="chart-box"></div>
        </div>
      </div>

      <div class="page-card">
        <div class="page-card-title">
          <h3>重点排放企业</h3>
          <span class="hint">仅列示 {{ stat.short }} 辖区内企业</span>
        </div>
        <el-table :data="detail?.companies ?? []" size="small">
          <el-table-column type="index" label="#" width="46" />
          <el-table-column prop="name" label="企业名称" min-width="180" />
          <el-table-column prop="industry" label="所属行业" width="110" />
          <el-table-column label="排放量 (tCO₂e)" width="130" align="right">
            <template #default="{ row }"><span class="num-cell">{{ row.emission.toLocaleString() }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag size="small" round :type="statusMeta[row.status].type">{{ statusMeta[row.status].text }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-empty v-else description="未找到该市州数据">
      <el-button round type="primary" @click="goScreen">返回态势大屏</el-button>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { cityStatMap, provinceTotalEmission } from '@/config/regions'
import { CITY_DETAIL } from '@/config/cityDetail'

const route = useRoute()
const router = useRouter()

const cityName = computed(() => String(route.params.name ?? ''))
const stat = computed(() => cityStatMap[cityName.value])
const detail = computed(() => CITY_DETAIL[cityName.value])
const share = computed(() => (stat.value ? ((stat.value.emission / provinceTotalEmission) * 100).toFixed(1) : '0'))
const fullName = computed(() => {
  const s = stat.value
  return s && s.name !== s.short + '市' ? s.name : ''
})

const statusMeta = {
  normal: { text: '正常', type: 'success' as const },
  warn: { text: '关注', type: 'warning' as const },
  high: { text: '预警', type: 'danger' as const }
}

const MONTHS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
const MONTH_RATIO = [0.074, 0.069, 0.081, 0.076, 0.083, 0.089, 0.092, 0.094, 0.078, 0.082, 0.089, 0.093]

const lineRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
let line: echarts.ECharts | null = null
let pie: echarts.ECharts | null = null

function renderLine() {
  if (!lineRef.value || !stat.value) return
  line = echarts.init(lineRef.value)
  const data = MONTH_RATIO.map((r) => Math.round(stat.value!.emission * r))
  line.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `${v.toLocaleString()} tCO₂e` },
    grid: { left: 10, right: 16, top: 26, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: MONTHS, boundaryGap: false, axisLabel: { color: '#64748b', fontSize: 11 }, axisLine: { lineStyle: { color: '#e2e8f0' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#64748b', fontSize: 11 } },
    series: [{
      type: 'line', smooth: true, data, symbol: 'circle', symbolSize: 6,
      lineStyle: { width: 3, color: '#00b581' },
      itemStyle: { color: '#00b581', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(0,181,129,0.28)' },
          { offset: 1, color: 'rgba(0,181,129,0.02)' }
        ])
      }
    }]
  })
}

function renderPie() {
  if (!pieRef.value || !detail.value) return
  pie = echarts.init(pieRef.value)
  pie.setOption({
    tooltip: { trigger: 'item', valueFormatter: (v: number) => `${v}%` },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 11.5, color: '#475569' } },
    series: [{
      type: 'pie', radius: ['46%', '68%'], center: ['50%', '44%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 13, fontWeight: 700, formatter: '{d}%' } },
      data: detail.value.industries.map((i) => ({ name: i.name, value: i.ratio, itemStyle: { color: i.color } }))
    }]
  })
}

const resize = () => { line?.resize(); pie?.resize() }
function goScreen() { router.push('/gov/big-screen') }
function goDashboard() { router.push('/gov/dashboard') }
function goHandle() {
  if (!stat.value) return
  router.push({ path: '/gov/review', query: { city: stat.value.name, status: 'warn' } })
}

onMounted(() => {
  if (stat.value) document.title = `${stat.value.short}碳排放详情 · 黔能碳溯`
  renderLine()
  renderPie()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  line?.dispose()
  pie?.dispose()
})
</script>

<style scoped lang="scss">
.city-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 26px;
  margin-bottom: 16px;
  border-radius: var(--qn-radius);
  border: 1px solid #b7e8d6;
  background:
    radial-gradient(120% 140% at 0% 0%, rgba(0, 181, 129, 0.14), transparent 56%),
    linear-gradient(120deg, #eefbf5, #f7fffb 55%, #eef8ff);
  overflow: hidden;
  &::after {
    content: '';
    position: absolute;
    right: -80px;
    top: -120px;
    width: 280px;
    height: 280px;
    border-radius: 50%;
    border: 1px solid rgba(0, 181, 129, 0.18);
  }
}
.ch-left { display: flex; flex-direction: column; gap: 8px; z-index: 1; }
.ghost-btn { margin-left: 0 !important; background: rgba(255, 255, 255, 0.8); border-color: #cde9dd; color: #157a5f; }
.ch-main { flex: 1; z-index: 1; }
.ch-tag { display: inline-block; font-size: 11px; font-weight: 700; letter-spacing: 2px; color: #008560; background: rgba(0, 181, 129, 0.1); padding: 3px 10px; border-radius: 999px; }
.ch-title { margin: 8px 0 4px; font-size: 26px; color: #064a3a; display: flex; align-items: baseline; gap: 10px;
  .ch-full { font-size: 13px; font-weight: 500; color: #2f6f5e; } }
.ch-sub { font-size: 12.5px; color: #2f6f5e;
  b { color: #008560; font-size: 14px; } }
.ch-badge { display: flex; align-items: center; gap: 10px; z-index: 1;
  .ch-ring { width: 54px; height: 54px; }
  .ch-badge-text { display: flex; flex-direction: column;
    b { font-size: 15px; color: #064a3a; }
    span { font-size: 11px; color: #2f6f5e; } } }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px;
  :deep(.stat-card) { padding: 18px 16px; gap: 12px; }
  :deep(.stat-value) { font-size: 21px; letter-spacing: 0; white-space: nowrap; }
}
.handle-card { position: relative; cursor: pointer; border-radius: var(--qn-radius);
  &:hover :deep(.stat-card) { border-color: #ffcf7a; box-shadow: 0 10px 24px rgba(255, 176, 32, 0.22); }
  .handle-link { position: absolute; right: 12px; top: 12px; display: inline-flex; align-items: center; gap: 2px;
    font-size: 11px; font-weight: 600; color: #a15c00; background: #fff6e5; border: 1px solid #ffe0a8;
    border-radius: 999px; padding: 2px 8px; transition: background 0.2s;
  }
  &:hover .handle-link { background: #ffeccb; }
}
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; margin-bottom: 16px; }
.chart-box { height: 280px; }
.hint { font-size: 11.5px; color: var(--qn-text-3); }
.num-cell { font-weight: 600; color: var(--qn-text); font-variant-numeric: tabular-nums; }
@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: 1fr 1fr; }
  .main-grid { grid-template-columns: 1fr; }
  .ch-badge { display: none; }
}
</style>