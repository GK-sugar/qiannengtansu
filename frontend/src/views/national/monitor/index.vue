<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">跨国互认监控</div>
      <div class="sub">碳足迹护照海外扫码查验回流：欧盟海关、采购商扫码后实时返校核查结果，异常令牌（DISCREPANCY）自动标红预警</div>
    </div>

    <div class="stat-grid">
      <StatCard label="近 7 日海外查验" :value="summary.total" icon="Connection" tint="#00b581" suffix=" 次" extra="实时回流" />
      <StatCard label="覆盖国家 / 地区" :value="summary.countries" icon="Position" tint="#00d1ff" suffix=" 个" extra="欧盟成员国为主" />
      <StatCard label="异常告警" :value="summary.discrepancy" icon="Warning" tint="#ff5f56" suffix=" 次" extra="疑似伪造/篡改" trend="较上周 +1" trendType="down" />
      <StatCard label="互认通过率" :value="99.8" icon="Medal" tint="#ffb020" suffix=" %" extra="令牌健康度" decimals="1" :animated="false" />
    </div>

    <div class="grid-2">
      <div class="page-card">
        <div class="page-card-title"><h3>逐日查验趋势（14 日）</h3></div>
        <div ref="trendRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>查验来源国家 TOP5</h3></div>
        <div ref="countryRef" class="chart-box"></div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>海外扫描明细</h3></div>
      <el-table :data="scans" size="small">
        <el-table-column prop="passportNo" label="护照编号" width="180">
          <template #default="{ row }"><b class="mono">{{ row.passportNo }}</b></template>
        </el-table-column>
        <el-table-column label="查验地点" min-width="180">
          <template #default="{ row }"><span class="flag">{{ flag(row.country) }}</span> {{ row.country }} · {{ row.city }}</template>
        </el-table-column>
        <el-table-column prop="ip" label="来源 IP" width="140">
          <template #default="{ row }"><span class="mono dim">{{ row.ip }}</span></template>
        </el-table-column>
        <el-table-column label="验真结果" width="130">
          <template #default="{ row }">
            <el-tag :type="row.verifyResult === 'VERIFIED' ? 'success' : 'danger'" size="small" round effect="dark">
              {{ row.verifyResult === 'VERIFIED' ? 'VERIFIED' : 'DISCREPANCY' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scanTime" label="扫码时间" width="170" />
        <el-table-column label="处置" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.verifyResult === 'DISCREPANCY'" link type="danger" size="small" @click="alertRow(row)">启动核查</el-button>
            <span v-else class="dim">已互认</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import request from '@/utils/request'
import StatCard from '@/components/StatCard.vue'

interface Scan { passportNo: string; country: string; city: string; ip: string; verifyResult: string; scanTime: string }

const summary = ref({ total: 346, countries: 13, discrepancy: 3 })
const scans = ref<Scan[]>([
  { passportNo: 'QN-PP-2026-0041', country: '德国', city: '汉堡', ip: '91.66.8.120', verifyResult: 'VERIFIED', scanTime: '2026-11-25 14:23:08' },
  { passportNo: 'QN-PP-2026-0038', country: '法国', city: '马赛', ip: '176.148.21.90', verifyResult: 'VERIFIED', scanTime: '2026-11-25 11:02:44' },
  { passportNo: 'QN-PP-2026-0021', country: '荷兰', city: '鹿特丹', ip: '80.112.3.77', verifyResult: 'DISCREPANCY', scanTime: '2026-11-24 22:41:19' },
  { passportNo: 'QN-PP-2026-0055', country: '比利时', city: '安特卫普', ip: '81.246.18.12', verifyResult: 'VERIFIED', scanTime: '2026-11-24 09:16:30' },
  { passportNo: 'QN-PP-2026-0033', country: '德国', city: '杜伊斯堡', ip: '91.65.44.201', verifyResult: 'VERIFIED', scanTime: '2026-11-23 18:37:55' },
  { passportNo: 'QN-PP-2026-0048', country: '意大利', city: '热那亚', ip: '93.66.102.35', verifyResult: 'VERIFIED', scanTime: '2026-11-23 08:12:03' },
  { passportNo: 'QN-PP-2026-0036', country: '西班牙', city: '巴塞罗那', ip: '46.26.11.58', verifyResult: 'VERIFIED', scanTime: '2026-11-22 16:45:12' },
  { passportNo: 'QN-PP-2026-0030', country: '波兰', city: '格但斯克', ip: '83.9.140.66', verifyResult: 'VERIFIED', scanTime: '2026-11-22 10:28:37' },
  { passportNo: 'QN-PP-2026-0027', country: '奥地利', city: '维也纳', ip: '178.113.9.24', verifyResult: 'VERIFIED', scanTime: '2026-11-21 15:54:20' },
  { passportNo: 'QN-PP-2026-0041', country: '荷兰', city: '鹿特丹', ip: '80.112.9.103', verifyResult: 'DISCREPANCY', scanTime: '2026-11-21 09:03:48' }
])
const trendRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
const countryRef = ref<HTMLElement>()
let countryChart: echarts.ECharts | null = null

const flagMap: Record<string, string> = { 德国: '🇩🇪', 法国: '🇫🇷', 荷兰: '🇳🇱', 比利时: '🇧🇪', 意大利: '🇮🇹', 西班牙: '🇪🇸', 波兰: '🇵🇱', 奥地利: '🇦🇹' }
function flag(c: string) { return flagMap[c] || '🌐' }
function alertRow(row: Scan) { ElMessage.warning('已启动跨部门核查：' + row.passportNo + '（' + row.city + '）') }

onMounted(async () => {
  try {
    const r = (await request.get('/v1/national/monitor/trend')) as any
    if (r?.code === 0 && r.data?.days?.length) {
      renderTrend(r.data.days, r.data.total, r.data.discrepancy, r.data.topCountries, r.data.topValues)
    } else renderTrendLocal()
    const s = (await request.get('/v1/national/monitor/summary')) as any
    if (s?.code === 0 && s.data) summary.value = { total: s.data.total, countries: s.data.countries, discrepancy: s.data.discrepancy }
  } catch { renderTrendLocal() }
  window.addEventListener('resize', resize)
  // 明细 attempt
  try {
    const r = (await request.get('/v1/national/monitor/scans')) as any
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) scans.value = r.data
  } catch { /* fallback to local demo */ }
})

function renderTrendLocal() {
  const days: string[] = [], total: number[] = [], bad: number[] = []
  for (let i = 13; i >= 0; i--) { days.push('11-' + String(26 - i).padStart(2, '0')); total.push(18 + Math.round(Math.sin(i / 2.2) * 8 + (i % 5) * 3 + Math.random() * 5)); bad.push(Math.floor(Math.random() * 3)) }
  renderTrend(days, total, bad, ['德国', '荷兰', '法国', '比利时', '意大利'], [126, 88, 64, 41, 27])
}

function renderTrend(days: string[], total: number[], bad: number[], topCountries: string[], topValues: number[]) {
  trendChart = echarts.init(trendRef.value!)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0, textStyle: { color: '#93a1b5' } },
    grid: { left: 10, right: 10, top: 32, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: days, axisLabel: { color: '#93a1b5' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    series: [
      { name: '查验总数', type: 'line', smooth: true, data: total, itemStyle: { color: '#00b581' }, areaStyle: { color: 'rgba(0,181,129,0.12)' } },
      { name: '异常令牌', type: 'bar', barWidth: 10, data: bad, itemStyle: { color: '#ff5f56', borderRadius: [4, 4, 0, 0] } }
    ]
  })
  countryChart = echarts.init(countryRef.value!)
  countryChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 16, top: 16, bottom: 6, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    yAxis: { type: 'category', data: [...topCountries].reverse(), axisLabel: { color: '#93a1b5' } },
    series: [{ type: 'bar', barWidth: 14, data: [...topValues].reverse(), itemStyle: { color: '#00d1ff', borderRadius: [0, 5, 5, 0] }, label: { show: true, position: 'right', color: '#00d1ff' } }]
  })
}
const resize = () => { trendChart?.resize(); countryChart?.resize() }
onBeforeUnmount(() => { window.removeEventListener('resize', resize); trendChart?.dispose(); countryChart?.dispose() })
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.grid-2 { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; margin-bottom: 16px; }
.chart-box { height: 260px; }
.flag { font-size: 14px; margin-right: 2px; }
.mono { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12.5px; }
.dim { color: var(--qn-text-3); font-size: 12px; }
</style>