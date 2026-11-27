<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">系统监控看板</div>
      <div class="sub">服务器指标定时采集、在线用户、API 调用量与哈希链健康度一屏掌握</div>
    </div>

    <div class="stat-grid">
      <StatCard label="CPU 使用率" :value="38" suffix="%" icon="Cpu" tint="#00d1ff" :animated="false" />
      <StatCard label="内存使用" :value="62" suffix="%" icon="Coin" tint="#00b581" :animated="false" />
      <StatCard label="磁盘 I/O" :value="24" suffix=" MB/s" icon="Coin" tint="#a06bff" :animated="false" />
      <StatCard label="今日 API 调用" :value="18432" icon="Connection" tint="#ffb020" />
      <StatCard label="在线用户" :value="127" icon="User" tint="#00b581" />
      <StatCard label="哈希链高度" :value="94128" icon="Link" tint="#00d1ff" />
    </div>

    <div class="main-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>近 24 小时 API 调用（按角色）</h3></div>
        <div ref="lineRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>慢接口 TOP5</h3></div>
        <el-table :data="slowApis" size="small">
          <el-table-column prop="api" label="接口" min-width="180"><template #default="{ row }"><span class="mono">{{ row.api }}</span></template></el-table-column>
          <el-table-column prop="avg" label="平均耗时" width="110" />
          <el-table-column prop="p99" label="P99" width="110" />
          <el-table-column label="健康度" width="130">
            <template #default="{ row }"><el-progress :percentage="row.health" :stroke-width="6" :color="row.health > 80 ? '#00b581' : '#ffb020'" /></template>
          </el-table-column>
        </el-table>
        <div class="table-note">接口调用明细落库 sys_api_log，审计与监控共用一份事实表。</div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>系统事件流</h3><el-tag size="small" round type="success">Normal</el-tag></div>
      <el-timeline>
        <el-timeline-item v-for="e in events" :key="e.id" :type="e.type" :timestamp="e.time">{{ e.text }}</el-timeline-item>
      </el-timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'

const lineRef = ref<HTMLElement>()
let line: echarts.ECharts | null = null

const slowApis = [
  { api: '/v1/lab/recognize', avg: '1,240ms', p99: '3,100ms', health: 62 },
  { api: '/v1/ai/chat/stream', avg: '890ms', p99: '2,400ms', health: 71 },
  { api: '/v1/export/cbam', avg: '640ms', p99: '1,800ms', health: 86 },
  { api: '/v1/national/issuance/decide', avg: '320ms', p99: '900ms', health: 91 },
  { api: '/v1/passport/issue', avg: '210ms', p99: '520ms', health: 94 },
  { api: '/v1/checkin/submit', avg: '180ms', p99: '460ms', health: 95 },
  { api: '/v1/gov/review/list', avg: '150ms', p99: '410ms', health: 96 },
  { api: '/v1/verify/:token', avg: '86ms', p99: '240ms', health: 99 }
]
const events = [
  { id: 1, type: 'success', time: '10:42', text: '哈希链验链通过：区块高度 94,128，连续无断链' },
  { id: 2, type: 'primary', time: '10:15', text: '核查机构「中环低碳」签发护照 QN-PP-2026-0041' },
  { id: 3, type: 'warning', time: '09:58', text: 'Redis 内存使用率 62%，接近 70% 水位阈值' },
  { id: 4, type: 'primary', time: '09:31', text: '数据迁移任务完成：lab_report 视觉字段补充 18 行' },
  { id: 5, type: 'warning', time: '09:12', text: '黔能矿业用电波动 +41.2% 触发 F6 预警，已派发核查任务 T-2026-118' },
  { id: 6, type: 'success', time: '08:55', text: '省级因子库 v3.2 每日备份完成（132 项因子）' },
  { id: 7, type: 'primary', time: '08:40', text: '核查员 H5 打卡存证成功：T-2026-118（500m 半径内）' },
  { id: 8, type: 'warning', time: '08:05', text: '企业账号 ent04 登录失败 3 次，已触发风控锁定规则' }
]

function renderLine() {
  line = echarts.init(lineRef.value!)
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  line.setOption({
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#5c6b82' } },
    grid: { left: 10, right: 14, top: 30, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: hours, axisLabel: { color: '#93a1b5', interval: 3 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    series: [
      { name: '企业端', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 300 + Math.random() * 400), itemStyle: { color: '#00b581' }, areaStyle: { opacity: 0.1, color: '#00b581' } },
      { name: '监管端', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 80 + Math.random() * 160), itemStyle: { color: '#00d1ff' }, areaStyle: { opacity: 0.08, color: '#00d1ff' } },
      { name: '核查机构', type: 'line', smooth: true, data: Array.from({ length: 24 }, () => 30 + Math.random() * 90), itemStyle: { color: '#a06bff' } }
    ]
  })
}
const resize = () => line?.resize()
onMounted(() => { renderLine(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); line?.dispose() })
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; }
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-box { height: 300px; }
.mono { font-family: monospace; font-size: 12px; }
.table-note { margin-top: 10px; font-size: 11.5px; color: var(--qn-text-3); }
@media (max-width: 1200px) { .stat-grid { grid-template-columns: repeat(3, 1fr); } .main-grid { grid-template-columns: 1fr; } }
</style>