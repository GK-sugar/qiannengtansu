<template>
  <div class="page">
    <!-- 问候横幅 -->
    <div class="hero-banner">
      <div class="title">早安，{{ auth.user?.realName }} · 今天也是减碳的一天 ☘</div>
      <div class="sub">{{ auth.user?.orgName }} ｜ 核算期 2026-09 ｜ 数据健康度 98% ｜ 距离下一期填报还有 4 天</div>
      <div class="banner-btn">
        <el-button round class="hero-solid" @click="$router.push('/enterprise/data-entry')">去填报</el-button>
        <el-button round class="hero-ghost" @click="$router.push('/enterprise/ai')">问 AI 碳管家</el-button>
      </div>
    </div>

    <!-- 指标卡 -->
    <div class="stat-grid">
      <StatCard label="本月碳排放（tCO₂e）" :value="12864" icon="Odometer" suffix=" t" trend="+3.2%" trend-type="down" extra="较上月 -2.8%" />
      <StatCard label="产品碳强度" :value="0.642" icon="TrendCharts" suffix=" tCO₂e/t" tint="#00d1ff" trend="-1.5%" extra="优于行业均值 12%" />
      <StatCard label="护照申请" value="8" icon="Stamp" tint="#a06bff" extra="2 本已签发 · 3 本在途 · 3 本退回" :animated="false" />
      <StatCard label="绿色金融信誉分" :value="87" icon="Money" tint="#ffb020" trend="+5" extra="A 级 · 预授信 1,200 万" />
    </div>

    <div class="chart-row">
      <!-- 排放趋势 -->
      <div class="page-card chart-card">
        <div class="page-card-title">
          <h3>近 12 个月排放趋势（tCO₂e）</h3>
          <el-radio-group v-model="trendRange" size="small">
            <el-radio-button value="6">6 月</el-radio-button>
            <el-radio-button value="12">12 月</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="trendRef" class="chart-box"></div>
      </div>
      <!-- 范围构成 -->
      <div class="page-card chart-card">
        <div class="page-card-title"><h3>排放范围构成</h3><el-tag size="small" round>范围一 ~ 三</el-tag></div>
        <div ref="pieRef" class="chart-box"></div>
      </div>
    </div>

    <div class="bottom-row">
      <!-- 待办 -->
      <div class="page-card">
        <div class="page-card-title"><h3>待办事项</h3><el-button link type="primary" @click="toggleTodoFilter">{{ todoFilter === 'all' ? '全部' : '未读' }}（{{ shownTodos.length }}）</el-button></div>
        <el-timeline>
          <el-timeline-item v-for="t in shownTodos" :key="t.id" :type="t.type" :timestamp="t.time">
            <div class="todo-line">
              <span class="todo-text"><span v-if="t.unread" class="unread-dot"></span>{{ t.text }}</span>
              <el-button v-if="t.action" link type="primary" size="small" @click="$router.push(t.link)">{{ t.action }}</el-button>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <!-- 数据包动态 -->
      <div class="page-card">
        <div class="page-card-title"><h3>数据包动态</h3><el-button link type="primary" @click="$router.push('/enterprise/export')">出海数据包</el-button></div>
        <el-table :data="packets" size="small">
          <el-table-column prop="name" label="数据包" />
          <el-table-column prop="period" label="核算期" width="110" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }"><el-tag :type="statusMap[row.status]" size="small" round>{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const trendRange = ref<'6' | '12'>('12')
const trendRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const months = ['24-11', '24-12', '25-01', '25-02', '25-03', '25-04', '25-05', '25-06', '25-07', '25-08', '25-09', '25-10']
const coalSeries = [7120, 6890, 7550, 7210, 7980, 8120, 7860, 8350, 7920, 7660, 7420, 7550]
const powerSeries = [3860, 3750, 4020, 4150, 4360, 4480, 4310, 4590, 4620, 4510, 4580, 4730]
const transSeries = [460, 520, 510, 580, 620, 660, 610, 680, 650, 640, 630, 584]

const todos = [
  { id: 1, text: '2026 年 10 月活动数据填报待提交', action: '去填报', link: '/enterprise/data-entry', type: 'primary', time: '截止 11-30', unread: true },
  { id: 2, text: '化验单 BH-1029 微信小程序上传待确认', action: '去确认', link: '/enterprise/lab-report', type: 'warning', time: '今天 10:30', unread: true },
  { id: 3, text: '供应商「贵州中煤供应链」资质即将过期', action: '去处理', link: '/enterprise/supply', type: 'danger', time: '剩余 12 天', unread: false },
  { id: 4, text: '护照 QN-PP-2026-0008 已进入国际复核', link: '/enterprise/passport', type: 'success', time: '09-28', unread: false },
  { id: 5, text: '整改通知单 QN-ZG-2026-021 待提交整改证据', action: '去整改', link: '/enterprise/rectify', type: 'danger', time: '截止 11-25', unread: true },
  { id: 6, text: '物流运单 YD-2026-1108 轨迹里程待确认', action: '去确认', link: '/enterprise/logistics', type: 'warning', time: '昨天 16:20', unread: true },
  { id: 7, text: 'CBAM-2026Q2 数据包已签发，可下载正式版', action: '去下载', link: '/enterprise/export', type: 'success', time: '10-12', unread: false },
  { id: 8, text: '绿色信贷申请待补充三季度财务报表', action: '去补充', link: '/enterprise/finance', type: 'primary', time: '截止 11-28', unread: false }
]
const todoFilter = ref<'all' | 'unread'>('all')
const shownTodos = computed(() => (todoFilter.value === 'unread' ? todos.filter(t => t.unread) : todos))
function toggleTodoFilter() {
  todoFilter.value = todoFilter.value === 'all' ? 'unread' : 'all'
}
const packets = [
  { name: 'CBAM-2026Q3-黔能矿业.xlsx', period: '2026-Q3', status: '已提交' },
  { name: 'ISO14067-2026-10.xlsx', period: '2026-10', status: '已提交' },
  { name: 'ISO14067-2026-09.xlsx', period: '2026-09', status: '复核中' },
  { name: 'CBAM-2026Q2-黔能矿业.xlsx', period: '2026-Q2', status: '已签发' },
  { name: 'ISO14067-2026-08.xlsx', period: '2026-08', status: '已签发' },
  { name: 'ISO14067-2026-07.xlsx', period: '2026-07', status: '已签发' },
  { name: 'CBAM-2026Q1-黔能矿业.xlsx', period: '2026-Q1', status: '已签发' },
  { name: 'ISO14067-2026-06.xlsx', period: '2026-06', status: '已签发' }
]
const statusMap: Record<string, string> = { 已提交: 'info', 复核中: 'warning', 已签发: 'success' }

function renderTrend() {
  if (!trendRef.value) return
  trendChart = echarts.init(trendRef.value)
  const n = trendRange.value === '6' ? -6 : 0
  const slice = (a: number[]) => a.slice(n)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 0, textStyle: { color: '#5c6b82' } },
    grid: { left: 10, right: 14, top: 38, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: months.slice(n), axisLine: { lineStyle: { color: '#d8e0ea' } }, axisLabel: { color: '#93a1b5' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    series: [
      { name: '燃煤', type: 'line', smooth: true, data: slice(coalSeries), itemStyle: { color: '#00b581' }, areaStyle: { opacity: 0.12, color: '#00b581' } },
      { name: '电力', type: 'line', smooth: true, data: slice(powerSeries), itemStyle: { color: '#00d1ff' }, areaStyle: { opacity: 0.08, color: '#00d1ff' } },
      { name: '运输', type: 'line', smooth: true, data: slice(transSeries), itemStyle: { color: '#a06bff' }, areaStyle: { opacity: 0.08, color: '#a06bff' } }
    ]
  })
}
function renderPie() {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} t ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#5c6b82' } },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '44%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
      label: { color: '#5c6b82', formatter: '{b}\n{d}%' },
      data: [
        { value: 7550, name: '范围一 · 直接排放', itemStyle: { color: '#00b581' } },
        { value: 4730, name: '范围二 · 外购电力', itemStyle: { color: '#00d1ff' } },
        { value: 584, name: '范围三 · 运输', itemStyle: { color: '#a06bff' } }
      ]
    }]
  })
}

const resize = () => { trendChart?.resize(); pieChart?.resize() }
watch(trendRange, renderTrend)
onMounted(() => { renderTrend(); renderPie(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); trendChart?.dispose(); pieChart?.dispose() })
</script>

<style scoped lang="scss">
.banner-btn { margin-top: 16px; display: flex; gap: 10px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.chart-row { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-box { height: 320px; }
.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.todo-line { display: flex; align-items: center; justify-content: space-between; font-size: 13.5px; color: var(--qn-text); }
.todo-text { display: inline-flex; align-items: flex-start; gap: 7px; }
.unread-dot { width: 7px; height: 7px; border-radius: 50%; background: #ff5f56; margin-top: 6px; flex-shrink: 0; }
@media (max-width: 1100px) {
  .stat-grid { grid-template-columns: 1fr 1fr; }
  .chart-row, .bottom-row { grid-template-columns: 1fr; }
}
</style>