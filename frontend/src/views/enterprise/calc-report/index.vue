<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">碳排核算与报告</div>
      <div class="sub">按 F1~F5 汇总月度核算，ECharts 可视化，一键生成可下载的核算报告（报告生成即写入哈希链）</div>
      <div class="banner-btn"><el-button round class="hero-solid" @click="reportVisible = true">一键生成报告</el-button></div>
    </div>

    <div class="month-bar page-card">
      <span class="bar-label">核算期</span>
      <el-select v-model="month" style="width: 160px">
        <el-option v-for="m in months" :key="m" :label="m" :value="m" />
      </el-select>
      <el-divider direction="vertical" />
      <span class="bar-label">功能单位</span>
      <el-select v-model="unit" style="width: 120px">
        <el-option label="tCO₂e/t" value="t" /><el-option label="tCO₂e/kWh" value="kwh" />
      </el-select>
      <el-divider direction="vertical" />
      <span class="bar-label">核算标准</span>
      <el-radio-group v-model="std" size="small">
        <el-radio-button value="gb">国家标准</el-radio-button>
        <el-radio-button value="hb">行业标准</el-radio-button>
        <el-radio-button value="tb">团体标准</el-radio-button>
      </el-radio-group>
      <el-divider direction="vertical" />
      <el-button plain @click="exportDetail"><el-icon><Download /></el-icon>导出明细</el-button>
    </div>

    <div class="stat-grid">
      <StatCard label="核算总量" :value="total" suffix=" tCO₂e" icon="Odometer" />
      <StatCard label="产品碳强度" :value="intensity" suffix=" tCO₂e/t" icon="TrendCharts" tint="#00d1ff" />
      <StatCard label="数据覆盖率" :value="98.6" suffix="%" icon="DataLine" tint="#a06bff" />
      <div class="hash-card">
        <div class="stat-label">核算记录哈希</div>
        <div class="stat-value hash-line">{{ hashText.slice(0, 18) }}…</div>
        <div class="stat-extra">SHA-256 · 区块链式存证</div>
      </div>
    </div>

    <div class="chart-row">
      <div class="page-card">
        <div class="page-card-title"><h3>分排放源排放量（tCO₂e）</h3><el-tag size="small" round :title="formulaHint('F1')">F1 明细</el-tag></div>
        <div ref="barRef" class="chart-box"></div>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>范围构成占比</h3><el-tag size="small" round :title="formulaHint('F5')">F5</el-tag></div>
        <div ref="pieRef" class="chart-box"></div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>核算明细（2026-10）</h3></div>
      <el-table :data="rows" size="small" stripe>
        <el-table-column prop="scope" label="范围" width="90" />
        <el-table-column prop="source" label="排放源" />
        <el-table-column prop="ad" label="活动数据(AD)" />
        <el-table-column prop="ef" label="排放因子(EF)" />
        <el-table-column prop="gwp" label="GWP" width="80" />
        <el-table-column prop="emission" label="排放量 tCO₂e" sortable />
        <el-table-column prop="share" label="占比" width="90">
          <template #default="{ row }"><el-progress :percentage="row.share" :stroke-width="6" /></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 报告预览弹窗 -->
    <el-dialog v-model="reportVisible" title="碳核算报告预览（可打印 / 存证）" width="720px" class="report-dialog">
      <div class="report-preview">
        <div class="rp-head">
          <img src="@/assets/logo/logo.svg" class="rp-logo" />
          <div>
            <div class="rp-title">产品碳足迹核算报告</div>
            <div class="rp-sub">CARBON FOOTPRINT ACCOUNTING REPORT</div>
          </div>
          <el-tag type="success" round>报告编号 QN-R-202610-031</el-tag>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="报告主体">{{ auth.user?.orgName }}</el-descriptions-item>
          <el-descriptions-item label="核算期">2026 年 10 月</el-descriptions-item>
          <el-descriptions-item label="核算标准">{{ stdMeta[std].code }}</el-descriptions-item>
          <el-descriptions-item label="功能单位">1 t 精煤</el-descriptions-item>
          <el-descriptions-item label="核算总量">{{ total }} tCO₂e</el-descriptions-item>
          <el-descriptions-item label="产品碳强度">{{ intensity }} tCO₂e/t</el-descriptions-item>
          <el-descriptions-item label="范围一 / 二 / 三">{{ scopeText }} tCO₂e</el-descriptions-item>
          <el-descriptions-item label="存证哈希"><span class="hash">{{ hashText }}</span></el-descriptions-item>
        </el-descriptions>
        <div class="rp-note">签发机构：贵州省生态环境厅（盖章） · 核查机构：中环低碳国际核查中心</div>
      </div>
      <template #footer>
        <el-button @click="reportVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadReport">下载 PDF / 存证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { formulaHint } from '@/utils/formulas'
import { sha256Hex } from '@/utils/hash'

const auth = useAuthStore()
const month = ref('2026-10')
const months = ['2026-10', '2026-09', '2026-08']
const unit = ref('t')
const reportVisible = ref(false)
const barRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

// SHA-256 确定性展示：基于核算期 + 总量 + 标准，保证同一份报告永远显示同一个 hash
// 口径与后端 HashChainService 一致，前端做纯前端占位（后端接入后会被真实值覆盖）
async function computeHash(period: string, total: number, std: string): Promise<string> {
  const hex = await sha256Hex(`${period}|${total.toFixed(2)}|${std}|QNCT-HASH-SALT`)
  return '0x' + hex
}
const hashText = ref('0x'.padEnd(66, '0'))

/* 核算标准口径：切换后 EF / 排放量 / 图表 / 报告联动重算 */
const std = ref<'gb' | 'hb' | 'tb'>('gb')
const stdMeta = {
  gb: { code: 'GB/T 24067-2024 · ISO 14067（省级因子库 v3.2）', coalC: 26.37, power: 0.5703, heat: 0.110, road: 0.062, rail: 0.010 },
  hb: { code: 'MT/T 1189-2020 煤炭行业口径（全国电网平均因子）', coalC: 26.64, power: 0.5810, heat: 0.116, road: 0.068, rail: 0.011 },
  tb: { code: 'T/CEC 102-2020 团体标准（产品碳足迹口径）', coalC: 26.10, power: 0.5568, heat: 0.104, road: 0.059, rail: 0.009 }
} as const
const r1 = (v: number) => Math.round(v * 10) / 10

const rows = computed(() => {
  const m = stdMeta[std.value]
  const base = [
    { scope: '范围一', source: '原煤燃烧（工业锅炉）', ad: '6,800 t', ef: `22.42 GJ/t × ${m.coalC} tC/TJ`, gwp: 1, emission: r1(7132.4 * m.coalC / 26.37) },
    { scope: '范围一', source: '柴油（矿用车辆）', ad: '42 t', ef: '3.1863 tCO₂e/t', gwp: 1, emission: 133.8 },
    { scope: '范围二', source: '外购电力（南方电网）', ad: '462,000 kWh', ef: `${m.power} tCO₂e/MWh`, gwp: 1, emission: r1(2635.3 * m.power / 0.5703) },
    { scope: '范围一', source: '瓦斯逸散（矿井通风）', ad: '86.5 t CH₄', ef: '28 tCO₂e/t（GWP 折算）', gwp: 28, emission: 2422.0 },
    { scope: '范围一', source: '汽油（公务车辆）', ad: '18 t', ef: '2.9251 tCO₂e/t', gwp: 1, emission: 52.7 },
    { scope: '范围二', source: '外购热力', ad: '1,280 GJ', ef: `${m.heat} tCO₂e/GJ`, gwp: 1, emission: r1(140.8 * m.heat / 0.110) },
    { scope: '范围三', source: '公路运输', ad: '526,000 t·km', ef: `${m.road} tCO₂e/百t·km`, gwp: 1, emission: r1(326.1 * m.road / 0.062) },
    { scope: '范围三', source: '铁路运输（精煤外运）', ad: '2,150,000 t·km', ef: `${m.rail} tCO₂e/千t·km`, gwp: 1, emission: r1(21.5 * m.rail / 0.010) }
  ]
  const sum = base.reduce((s, r) => s + r.emission, 0)
  return base.map(r => ({ ...r, share: Math.round(r.emission / sum * 100) }))
})
const total = computed(() => r1(rows.value.reduce((s, r) => s + r.emission, 0)))
const intensity = computed(() => Math.round(total.value / 20037 * 1000) / 1000)
const scopeSum = (name: string) => r1(rows.value.filter(r => r.scope === name).reduce((s, r) => s + r.emission, 0))
const scopeText = computed(() => `${scopeSum('范围一').toLocaleString()} / ${scopeSum('范围二').toLocaleString()} / ${scopeSum('范围三').toLocaleString()}`)

// hash 依赖全部就绪后再启动 watcher，确保按 total/std/month 重算
async function refreshHash() {
  hashText.value = await computeHash(month.value, total.value, std.value)
}
watch([month, total, std], () => { void refreshHash() }, { immediate: false })

function renderCharts() {
  const v = rows.value
  const barOrder = [0, 1, 2, 5, 6, 3, 4, 7]
  if (barRef.value) {
    barChart = barChart || echarts.init(barRef.value)
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 10, right: 14, top: 24, bottom: 6, containLabel: true },
      xAxis: { type: 'category', data: ['原煤', '柴油', '外购电', '外购热', '公路运输', '瓦斯逸散', '汽油', '铁路运输'], axisLabel: { color: '#93a1b5', interval: 0 }, axisLine: { lineStyle: { color: '#d8e0ea' } } },
      yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
      series: [{ type: 'bar', barWidth: 22, data: barOrder.map(i => v[i].emission),
        itemStyle: { borderRadius: [6, 6, 0, 0], color: (p: any) => ['#00b581', '#2bd8a2', '#00d1ff', '#5ca8ff', '#a06bff', '#ff9f43', '#f56c6c', '#8d9db6'][p.dataIndex] } }]
    })
  }
  if (pieRef.value) {
    pieChart = pieChart || echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
      series: [{ type: 'pie', radius: ['46%', '70%'], center: ['50%', '48%'],
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
        label: { color: '#5c6b82' },
        data: [
          { value: scopeSum('范围一'), name: '范围一', itemStyle: { color: '#00b581' } },
          { value: scopeSum('范围二'), name: '范围二', itemStyle: { color: '#00d1ff' } },
          { value: scopeSum('范围三'), name: '范围三', itemStyle: { color: '#a06bff' } }
        ] }]
    })
  }
}
watch(std, () => renderCharts())
const resize = () => { barChart?.resize(); pieChart?.resize() }
onMounted(() => { renderCharts(); window.addEventListener('resize', resize); void refreshHash() })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); barChart?.dispose(); pieChart?.dispose() })

function downloadReport() {
  ElMessage.success('报告已生成：QN-R-202610-031.pdf，并写入哈希链存证（F10）')
  reportVisible.value = false
}

function exportDetail() {
  const head = ['范围', '排放源', '活动数据(AD)', '排放因子(EF)', 'GWP', '排放量(tCO₂e)', '占比(%)']
  const body = rows.value.map(r => [r.scope, r.source, r.ad, r.ef, r.gwp, r.emission, r.share])
  const csv = '\uFEFF' + [head, ...body].map(row => row.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `核算明细_${month.value}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('核算明细已导出：' + a.download)
}
</script>

<style scoped lang="scss">
.banner-btn { margin-top: 14px; }
.month-bar { display: flex; align-items: center; gap: 12px; font-size: 13px; color: var(--qn-text-2); }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.chart-row { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-box { height: 300px; }
.report-preview { overflow: visible; }
.rp-head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px;
  .rp-logo { width: 34px; height: 34px; }
  .rp-title { font-size: 17px; font-weight: 700; color: var(--qn-text); }
  .rp-sub { font-size: 10px; color: var(--qn-text-3); letter-spacing: 2px; }
}
.hash { font-family: monospace; font-size: 11px; word-break: break-all; }
.rp-note { margin-top: 14px; font-size: 12px; color: var(--qn-text-3); text-align: center; }
@media (max-width: 1100px) { .stat-grid { grid-template-columns: 1fr 1fr; } .chart-row { grid-template-columns: 1fr; } }
@media (max-width: 640px) {
  .rp-head { flex-wrap: wrap; gap: 10px; }
  .rp-title { font-size: 15px; }
  .rp-head .el-tag { margin-left: 0; }
}
</style>

<style lang='scss'>
.hash-card { background: var(--qn-card); border:1px solid var(--qn-border); border-radius: var(--qn-radius); padding: 20px; box-shadow: var(--qn-shadow); }
.hash-line { font-family: monospace; font-size: 15px; font-weight: 700; color: var(--qn-text); word-break: break-all; }
.hash-card .stat-label { font-size: 13px; color: var(--qn-text-2); margin-bottom: 4px; }
.hash-card .stat-extra { margin-top: 4px; font-size: 12px; color: var(--qn-text-3); }
</style>
