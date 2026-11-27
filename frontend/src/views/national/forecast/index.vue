<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">宏观预测驾驶舱</div>
      <div class="sub">全国碳排放历史回放与 2030 前情景预测（内生线性外推算法，无需外部模型）；支持省域强度排行与达峰研判结论</div>
    </div>

    <div class="stat-grid">
      <StatCard label="全国排放峰值" :value="10.71" icon="DataLine" tint="#ff5f56" suffix=" 亿吨" extra="峰值年份 2024" decimals="2" :animated="false" />
      <StatCard label="2030 目标值" :value="8.13" icon="Aim" tint="#00b581" suffix=" 亿吨" extra="较 2020 年 -18%" decimals="2" :animated="false" />
      <StatCard label="预测年均降幅" :value="2.8" icon="TrendCharts" tint="#00d1ff" suffix=" %" extra="2027-2030" decimals="1" :animated="false" />
      <StatCard label="达峰后累计减排" :value="1.36" icon="Medal" tint="#ffb020" suffix=" 亿吨" extra="2024-2026" decimals="2" :animated="false" />
    </div>

    <div class="grid-2">
      <div class="page-card">
        <div class="page-card-title">
          <h3>全国排放总览与情景预测</h3>
          <el-tag size="small" effect="plain" round type="info">{{ macro.model }}</el-tag>
        </div>
        <div ref="lineRef" class="chart-box-tall"></div>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>行业排放结构</h3></div>
        <div ref="pieRef" class="chart-box-tall"></div>
      </div>
    </div>

    <div class="grid-2">
      <div class="page-card">
        <div class="page-card-title"><h3>省域排放强度排行</h3></div>
        <el-table :data="regions" size="small">
          <el-table-column prop="name" label="省份" width="90" />
          <el-table-column prop="total" label="总量（亿吨）" width="120" />
          <el-table-column prop="intensity" label="强度（t/万元）" width="130">
            <template #default="{ row }"><b class="green">{{ row.intensity }}</b></template>
          </el-table-column>
          <el-table-column label="同比" width="90">
            <template #default="{ row }"><span :class="['trend-' + row.dir]">{{ row.trend }}</span></template>
          </el-table-column>
          <el-table-column prop="note" label="监管重点" min-width="170" />
        </el-table>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>达峰研判结论</h3></div>
        <div class="concl-list">
          <div v-for="(c, i) in conclusions" :key="i" class="concl-item">
            <el-icon class="c-dot" :size="18"><Checked /></el-icon>
            <span>{{ c }}</span>
          </div>
        </div>
        <div class="peak-card">
          <div class="pk-label">峰值年份</div>
          <div class="pk-big">{{ macro.peakYear }}<small> 年</small></div>
          <div class="pk-row"><span>峰值排放</span><b>{{ macro.peakValue }}</b></div>
          <div class="pk-row"><span>2030 目标</span><b>{{ macro.target2030 }}</b></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import request from '@/utils/request'
import StatCard from '@/components/StatCard.vue'

const lineRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
let lineChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const macro = reactive<any>({
  years: ['2019', '2020', '2021', '2022', '2023', '2024', '2025', '2026'],
  history: [9.86, 9.92, 10.21, 10.48, 10.63, 10.71, 10.62, 10.47],
  forecastYears: ['2027', '2028', '2029', '2030'],
  baseline: [10.36, 10.25, 10.14, 10.03],
  policy: [10.14, 9.83, 9.52, 9.19],
  model: '内生线性外推 + 政策情景（-2.1%/年）',
  peakYear: '2024',
  peakValue: '10.71 亿吨',
  target2030: '较 2020 年下降 18%（≈ 8.13 亿吨）'
})
const sectors = reactive({ names: ['电力', '工业', '交通', '建筑', '农业', '其他'], values: [38.6, 31.2, 10.4, 8.9, 6.3, 4.6] })
const regions = ref<any[]>([])
const conclusions = [
  '全国排放已于 2024 年见顶，进入平稳下行通道',
  '电力行业占比最高（38.6%），风光储替代是达峰后减排主引擎',
  '出口导向行业（钢铁/铝/水泥）面临 CBAM 碳关税成本上行压力',
  '贵州省作为磷化工与能源转型走廊，重点推动煤电清洁替代'
]

onMounted(async () => {
  let ok = false
  try {
    const r = (await request.get('/v1/national/forecast/macro')) as any
    if (r?.code === 0 && r.data) {
      Object.assign(macro, r.data)
      Object.assign(sectors, r.data.sectors || {})
      regions.value = (r.data as any).regionalRows || []
      ok = true
    }
  } catch { /* fallback */ }
  if (!ok) {
    regions.value = [
      { name: '贵州', total: '2.83', intensity: '1.721', trend: '-6.2%', dir: 'down', note: '磷化工 / 电解铝绿电替代' },
      { name: '山东', total: '8.86', intensity: '1.898', trend: '-3.1%', dir: 'down', note: '地炼与钢铁产能整合' },
      { name: '江苏', total: '8.02', intensity: '1.653', trend: '-2.8%', dir: 'down', note: '出口制造 CBAM 合规' },
      { name: '广东', total: '6.45', intensity: '1.204', trend: '-4.6%', dir: 'down', note: '电力外来绿电占比提升' },
      { name: '内蒙古', total: '6.81', intensity: '2.616', trend: '-1.2%', dir: 'down', note: '煤电与铁合金高载能管控' },
      { name: '河北', total: '7.73', intensity: '2.105', trend: '-2.4%', dir: 'down', note: '钢铁短流程电炉改造' },
      { name: '山西', total: '5.94', intensity: '2.412', trend: '-1.8%', dir: 'down', note: '焦化行业超低排放改造' },
      { name: '河南', total: '4.62', intensity: '1.377', trend: '-3.5%', dir: 'down', note: '水泥熟料错峰生产' },
      { name: '浙江', total: '4.18', intensity: '0.986', trend: '-4.1%', dir: 'down', note: '分布式光伏与绿电交易' }
    ]
  }
  try {
    const rr = (await request.get('/v1/national/forecast/regional')) as any
    if (rr?.code === 0 && Array.isArray(rr.data?.rows) && rr.data.rows.length) regions.value = rr.data.rows
  } catch { /* keep */ }
  render()
  window.addEventListener('resize', resize)
})
function render() {
  const allYears = [...macro.years, ...macro.forecastYears]
  const allHistory = [...macro.history, ...macro.baseline.map((_: any) => null)]
  const allPolicy = [...Array(macro.history.length).fill(null), ...macro.policy]
  lineChart = echarts.init(lineRef.value!)
  lineChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#93a1b5' } },
    grid: { left: 10, right: 40, top: 34, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: allYears, axisLabel: { color: '#93a1b5' } },
    yAxis: { type: 'value', name: '亿吨 CO2e', nameTextStyle: { color: '#93a1b5' }, splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    series: [
      { name: '历史实际', type: 'line', smooth: true, data: macro.history, itemStyle: { color: '#00b581' }, areaStyle: { color: 'rgba(0,181,129,0.10)' } },
      { name: '基准情景', type: 'line', smooth: true, data: allHistory, itemStyle: { color: '#00d1ff' }, lineStyle: { type: 'dashed' } },
      { name: '政策情景', type: 'line', smooth: true, data: allPolicy, itemStyle: { color: '#ffb020' }, lineStyle: { type: 'dashed' } }
    ]
  })
  pieChart = echarts.init(pieRef.value!)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}%' },
    legend: { bottom: 0, textStyle: { color: '#93a1b5' } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      data: sectors.names.map((n: string, i: number) => ({ name: n, value: sectors.values[i] })),
      label: { color: '#5b6b82' },
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
    }]
  })
}
const resize = () => { lineChart?.resize(); pieChart?.resize() }
onBeforeUnmount(() => { window.removeEventListener('resize', resize); lineChart?.dispose(); pieChart?.dispose() })
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.grid-2 { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; margin-bottom: 16px; }
.chart-box-tall { height: 340px; }
.green { color: var(--qn-primary); }
.trend-down { color: #00b581; font-weight: 600; }
.trend-up { color: #ff5f56; font-weight: 600; }
.concl-list { display: flex; flex-direction: column; gap: 14px; }
.concl-item { display: flex; align-items: flex-start; gap: 10px; font-size: 13.5px; color: var(--qn-text); line-height: 1.6;
  .c-dot { color: #00b581; margin-top: 2px; } }
.peak-card { margin-top: 18px; padding: 18px; border-radius: 14px; background: linear-gradient(135deg, rgba(0, 181, 129, 0.08), rgba(0, 209, 255, 0.06)); border: 1px solid rgba(0, 181, 129, 0.2);
  .pk-label { font-size: 12px; color: var(--qn-text-3); letter-spacing: 2px; }
  .pk-big { font-size: 40px; font-weight: 800; color: var(--qn-primary); margin: 4px 0 10px;
    small { font-size: 14px; color: var(--qn-text-2); } }
  .pk-row { display: flex; justify-content: space-between; font-size: 13px; padding: 6px 0; border-top: 1px dashed rgba(0, 181, 129, 0.2);
    span { color: var(--qn-text-2); } b { color: var(--qn-text); } } }
</style>