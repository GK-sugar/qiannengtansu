<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">系统运行与接口监控</div>
      <div class="sub">服务器负载、在线用户与 API 调用频率实时采集（30 秒请求周期模拟 · 正式环境对接 Spring Boot Actuator / Prometheus）</div>
    </div>

    <div class="stat-grid">
      <StatCard label="CPU 使用率" :value="cpu" suffix="%" icon="Cpu" tint="#00b581" :trend="cpuTrend" :trend-type="cpu >= 70 ? 'up' : 'down'" />
      <StatCard label="内存使用率" :value="mem" suffix="%" icon="Memory" tint="#4da3ff" trend="+1.8%" />
      <StatCard label="磁盘 I/O" :value="disk" suffix="%" icon="Coin" tint="#a06bff" extra="数据库 8 核心 · 主从同步正常" />
      <StatCard label="在线用户" :value="online" icon="User" tint="#ffb020" trend="+18" extra="峰值 412 · 今日" />
    </div>

    <div class="chart-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>服务器负载趋势（近 30 分钟）</h3><el-tag size="small" round type="info">采集周期 30s</el-tag></div>
        <div ref="loadRef" class="chart"></div>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>API 调用频率（QPS）</h3><span class="dim">峰值 {{ peak }} QPS</span></div>
        <div ref="apiRef" class="chart"></div>
      </div>
    </div>

    <div class="bottom-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>接口调用 TOP 10</h3><el-button size="small" plain @click="refresh">刷新</el-button></div>
        <el-table :data="apis" size="small">
          <el-table-column prop="path" label="接口路径" min-width="240"><template #default="{ row }"><span class="mono">{{ row.path }}</span></template></el-table-column>
          <el-table-column prop="module" label="模块" width="110">
            <template #default="{ row }"><el-tag size="small" round type="info">{{ row.module }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="calls" label="今日调用" width="100" align="right"><template #default="{ row }"><b>{{ row.calls.toLocaleString() }}</b></template></el-table-column>
          <el-table-column prop="avg" label="平均耗时" width="100" align="right" />
          <el-table-column prop="qps" label="当前 QPS" width="100" align="right"><template #default="{ row }"><b class="qps">{{ row.qps }}</b></template></el-table-column>
          <el-table-column label="健康度" width="110">
            <template #default="{ row }"><el-progress :percentage="row.health" :stroke-width="7" :color="row.health > 90 ? '#00b581' : '#ffb020'" :format="() => row.health + '%'" /></template>
          </el-table-column>
        </el-table>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>在线用户（实时会话）</h3><el-tag size="small" round type="success">Redis Session</el-tag></div>
        <div class="user-list">
          <div v-for="u in users.slice(0, 6)" :key="u.name" class="user-row">
            <span class="u-avatar">{{ u.name[0] }}</span>
            <div class="u-main">
              <div class="u-name">{{ u.name }}<span class="u-role">{{ u.role }}</span></div>
              <div class="u-sub mono">{{ u.ip }} · 活跃 {{ u.dur }}</div>
            </div>
            <el-tag size="small" :type="u.health ? 'success' : 'warning'" round>{{ u.health ? '在线' : '空闲' }}</el-tag>
          </div>
        </div>
        <div class="dim-note"><el-icon><InfoFilled /></el-icon>会话由 Redis Token 管理：支持按用户强制下线，踢人后 Token 版本号 +1 立即失效</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'

const cpu = ref(42)
const mem = ref(61)
const disk = ref(37)
const online = ref(286)
const cpuTrend = ref('+3.2%')
const peak = ref(132)

const loadRef = ref<HTMLDivElement>()
const apiRef = ref<HTMLDivElement>()
let loadChart: echarts.ECharts | null = null
let apiChart: echarts.ECharts | null = null

const times = Array.from({ length: 30 }, (_, i) => ((29 - i) * 30) + 's 前').reverse()
const cpuSeries = [32, 35, 31, 38, 41, 39, 44, 42, 47, 45, 51, 48, 46, 52, 49, 54, 51, 56, 53, 58, 55, 60, 57, 63, 59, 62, 61, 65, 63, cpu.value]
const memSeries = [52, 53, 54, 53, 55, 56, 55, 57, 56, 58, 59, 58, 60, 59, 61, 60, 62, 61, 63, 62, 64, 63, 64, 65, 64, 66, 65, 66, 67, mem.value]
const qpsSeries = syncQps()
function syncQps() {
  return Array.from({ length: 30 }, (_, i) => Math.round(60 + Math.sin(i / 4) * 28 + Math.random() * 22))
}

function renderLoad() {
  loadChart = echarts.init(loadRef.value)
  loadChart.setOption({
    grid: { left: 42, right: 14, top: 22, bottom: 28 },
    tooltip: { trigger: 'axis' },
    legend: { data: ['CPU', '内存'], right: 8, top: 0, itemWidth: 12 },
    xAxis: { type: 'category', data: times, boundaryGap: false, axisLabel: { fontSize: 10, color: '#93a1b5', interval: 5 } },
    yAxis: { type: 'value', max: 100, axisLabel: { fontSize: 10, color: '#93a1b5', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#edf1f6' } } },
    series: [
      { name: 'CPU', type: 'line', smooth: true, data: cpuSeries, symbol: 'none', lineStyle: { width: 2, color: '#00b581' }, areaStyle: { color: 'rgba(0,181,129,0.12)' } },
      { name: '内存', type: 'line', smooth: true, data: memSeries, symbol: 'none', lineStyle: { width: 2, color: '#4da3ff' }, areaStyle: { color: 'rgba(77,163,255,0.10)' } }
    ]
  })
}

function renderApi() {
  apiChart = echarts.init(apiRef.value)
  apiChart.setOption({
    grid: { left: 42, right: 14, top: 22, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: times, boundaryGap: false, axisLabel: { fontSize: 10, color: '#93a1b5', interval: 5 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10, color: '#93a1b5' }, splitLine: { lineStyle: { color: '#edf1f6' } } },
    series: [{ name: 'QPS', type: 'bar', data: qpsSeries, barWidth: '55%', itemStyle: { borderRadius: [4, 4, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#00d1ff' }, { offset: 1, color: '#00b581' }]) } }]
  })
}

/* API TOP 列表 */
const apis = ref([
  { path: '/api/v1/enterprise/calc/run', module: '碳排核算', calls: 18420, avg: '86 ms', qps: 42, health: 98 },
  { path: '/api/v1/auth/login', module: '登录鉴权', calls: 16503, avg: '132 ms', qps: 31, health: 99 },
  { path: '/api/v1/gov/review/list', module: '智能初审', calls: 12108, avg: '74 ms', qps: 26, health: 97 },
  { path: '/api/v1/enterprise/ai/chat', module: 'AI 碳管家', calls: 9322, avg: '2.4 s', qps: 11, health: 92 },
  { path: '/api/v1/public/passports/{token}', module: '公开验真', calls: 8750, avg: '58 ms', qps: 18, health: 99 },
  { path: '/api/v1/screen/snapshot', module: '态势大屏', calls: 7911, avg: '64 ms', qps: 14, health: 96 },
  { path: '/api/v1/enterprise/lab/ocr', module: '化验单识别', calls: 2430, avg: '1.8 s', qps: 3, health: 88 }
])

const users = ref([
  { name: '王总', role: '企业管理员', ip: '223.104.19.6', dur: '12 分钟', health: true },
  { name: '李处长', role: '省厅审批员', ip: '10.10.3.21', dur: '36 分钟', health: true },
  { name: '陈主任', role: '核查复核员', ip: '10.11.7.10', dur: '1 小时 04 分', health: true },
  { name: '张副处', role: '省厅审批员', ip: '10.10.3.28', dur: '22 分钟', health: false },
  { name: '刘工程师', role: '企业填报员', ip: '111.85.52.19', dur: '8 分钟', health: true },
  { name: '系统管理员', role: '超级管理员', ip: '10.20.1.9', dur: '1 小时 42 分', health: true }
])

let timer: ReturnType<typeof setInterval> | null = null
function tick() {
  cpu.value = Math.min(96, Math.max(18, cpu.value + Math.round((Math.random() - 0.48) * 6)))
  mem.value = Math.min(92, Math.max(40, mem.value + Math.round((Math.random() - 0.5) * 2)))
  disk.value = Math.min(75, Math.max(28, disk.value + Math.round((Math.random() - 0.5) * 2)))
  online.value = Math.max(220, online.value + Math.round((Math.random() - 0.45) * 10))
  cpuTrend.value = (Math.random() > 0.5 ? '+' : '−') + (Math.random() * 4).toFixed(1) + '%'
  cpuSeries.shift(); cpuSeries.push(cpu.value)
  memSeries.shift(); memSeries.push(mem.value)
  const q = syncQps(); peak.value = Math.max(...q)
  qpsSeries.shift(); qpsSeries.push(q[0])
  apis.value.forEach(a => { a.qps = Math.max(1, Math.round(a.qps * (0.85 + Math.random() * 0.3))); a.health = Math.max(80, Math.min(99, a.health + Math.round(Math.random() * 4 - 2))) })
  loadChart?.setOption({ series: [{ data: cpuSeries }, { data: memSeries }] })
  apiChart?.setOption({ series: [{ data: qpsSeries }] })
}
function refresh() { tick(); ElMessage.success('指标已刷新') }

onMounted(async () => {
  await nextTick()
  renderLoad()
  renderApi()
  timer = setInterval(tick, 3000)
})
onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  loadChart?.dispose()
  apiChart?.dispose()
})
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.chart-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 16px; }
.chart { height: 260px; }
.bottom-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; align-items: start; }
.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #4d6c89; }
.dim { font-size: 11.5px; color: var(--qn-text-3); }
.qps { color: var(--qn-primary); font-variant-numeric: tabular-nums; }
.user-list { display: flex; flex-direction: column; gap: 9px; }
.user-row { display: flex; align-items: center; gap: 12px; padding: 9px 12px; border: 1px solid var(--qn-border); border-radius: 11px; background: #fbfcfe; }
.u-avatar { width: 34px; height: 34px; border-radius: 10px; color: #fff; display: grid; place-items: center; font-weight: 700; background: linear-gradient(135deg, #00b581, #00d1ff); flex-shrink: 0; }
.u-main { flex: 1; min-width: 0; }
.u-name { font-size: 13px; font-weight: 600; color: var(--qn-text); .u-role { margin-left: 8px; font-size: 10.5px; color: var(--qn-text-3); font-weight: 400; } }
.u-sub { font-size: 11px; color: var(--qn-text-3); margin-top: 2px; }
.dim-note { margin-top: 12px; font-size: 11px; color: var(--qn-text-3); display: flex; align-items: center; gap: 5px; }
@media (max-width: 1100px) { .stat-grid, .chart-grid, .bottom-grid { grid-template-columns: 1fr; } }
</style>