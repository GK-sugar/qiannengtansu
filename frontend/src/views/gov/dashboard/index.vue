<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">监管工作台</div>
      <div class="sub">{{ auth.user?.orgName }} · 应对气候变化处 ｜ 全省 2,384 家企业纳入监测</div>
      <div class="banner-btn">
        <el-button round class="hero-solid" @click="goBigScreen"><el-icon><DataBoard /></el-icon>进入态势大屏</el-button>
        <el-button round class="hero-ghost" @click="$router.push('/gov/review')">开始初审</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="待初审核算" :value="38" icon="Warning" suffix=" 家" tint="#ffb020" extra="其中高预警 3 家" />
      <StatCard label="本月核准通过" :value="212" icon="CircleCheck" suffix=" 家" />
      <StatCard label="在途核查任务" :value="17" icon="Position" tint="#00d1ff" extra="6 名核查员执行中" />
      <StatCard label="数据回填率" :value="93.6" suffix="%" icon="DataLine" tint="#a06bff" trend="+1.2%" />
    </div>

    <div class="main-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>初审动态</h3><el-button link type="primary" @click="$router.push('/gov/review')">全部</el-button></div>
        <el-table :data="flows" size="small">
          <el-table-column prop="company" label="企业" min-width="170" />
          <el-table-column prop="period" label="核算期" width="90" />
          <el-table-column prop="event" label="事件" min-width="190">
            <template #default="{ row }"><span :style="{ color: evColor[row.type] }">{{ row.event }}</span></template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="130" />
        </el-table>
      </div>

      <div class="page-card dist-card" title="点击放大查看" @click="openZoom">
        <div class="page-card-title">
          <h3>各市州预警分布</h3>
          <div class="dist-actions">
            <el-tag size="small" round :title="formulaHint('F6')">F6 波动率</el-tag>
            <button type="button" class="zoom-btn" aria-label="放大查看" @click.stop="openZoom">
              <el-icon :size="14"><FullScreen /></el-icon>
            </button>
          </div>
        </div>
        <div ref="barRef" class="chart-box"></div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>快捷入口</h3></div>
      <div class="quick-grid-inner">
        <button v-for="q in quicks" :key="q.path" class="qk-card" @click="$router.push(q.path)">
          <el-icon :size="24" :color="q.color"><component :is="q.icon" /></el-icon>
          <div class="qk-name">{{ q.name }}</div>
          <div class="qk-desc">{{ q.desc }}</div>
        </button>
      </div>
    </div>

    <el-dialog
      v-model="zoomVisible"
      width="min(940px, 94vw)"
      top="7vh"
      append-to-body
      destroy-on-close
      modal-class="glass-modal"
      class="dist-dialog"
    >
      <template #header>
        <div class="dd-head">
          <div>
            <div class="dd-title">各市州预警分布</div>
            <div class="dd-sub">贵州 9 个市州 · F6 波动率 &gt; 20% 的异常预警数量 · 2026-10</div>
          </div>
          <el-tag round size="small" effect="plain" class="dd-tag">点击柱体进入市州专页</el-tag>
        </div>
      </template>
      <div ref="zoomRef" class="zoom-chart"></div>
      <div class="dd-foot">异常由 F6 波动比对自动判定，预警说明经大模型辅助生成，结论全程写入审计与哈希链存证。</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'
import { formulaHint } from '@/utils/formulas'

const auth = useAuthStore()
const router = useRouter()
const barRef = ref<HTMLElement>()
let bar: echarts.ECharts | null = null

function goBigScreen() {
  router.push({ path: '/gov/big-screen' })
}

const flows = [
  { company: '六盘水黔能矿业有限公司', period: '2026-10', event: '触发高等级预警：波动率 41.2%', type: 'high', time: '09:42' },
  { company: '中伟新材料股份有限公司', period: '2026-10', event: '核算初审通过 · 已派发核查', type: 'ok', time: '10:18' },
  { company: '遵义铝业股份有限公司', period: '2026-09', event: '数据退回：范围二因子引用过期', type: 'back', time: '10:36' },
  { company: '贵州黔能煤化工有限责任公司', period: '2026-10', event: '缺失化验单附件，提示补传', type: 'mid', time: '11:02' },
  { company: '贵州磷化（集团）有限责任公司', period: '2026-10', event: '化验单多模态识别完成，待人工确认', type: 'mid', time: '11:20' },
  { company: '贵州钢绳股份有限公司', period: '2026-09', event: '复核通过 · 盖章文件已回传', type: 'ok', time: '11:47' },
  { company: '西南水泥（贵州）有限公司', period: '2026-10', event: '熟料产量环比 +18%，触发中等级预警', type: 'mid', time: '13:05' },
  { company: '乌江航运物流集团有限公司', period: '2026-10', event: '水运里程存证上链，哈希校验一致', type: 'ok', time: '14:12' }
]
const evColor: Record<string, string> = { high: '#f56c6c', ok: '#00b581', back: '#a06bff', mid: '#ffb020' }

const quicks = [
  { name: '态势感知大屏', desc: '全省热力与动态数据', path: '/gov/big-screen', icon: 'DataBoard', color: '#00b581' },
  { name: '智能初审台', desc: '异常比对与预警', path: '/gov/review', icon: 'Warning', color: '#ffb020' },
  { name: '核查派单', desc: '线下核查与 H5 打卡', path: '/gov/dispatch', icon: 'Position', color: '#00d1ff' },
  { name: '因子库管理', desc: '省级因子版本维护', path: '/gov/factors', icon: 'Collection', color: '#a06bff' },
  { name: '意见书回传', desc: '下载盖章后扫描上传', path: '/gov/seal', icon: 'Stamp', color: '#ff6b6b' }
]

const distData = [
  { city: '贵阳', value: 4 },
  { city: '遵义', value: 6 },
  { city: '六盘水', value: 9 },
  { city: '毕节', value: 5 },
  { city: '安顺', value: 2 },
  { city: '铜仁', value: 1 },
  { city: '黔西南', value: 3 },
  { city: '黔东南', value: 2 },
  { city: '黔南', value: 2 }
].sort((a, b) => b.value - a.value)

const fullCity: Record<string, string> = {
  贵阳: '贵阳市', 遵义: '遵义市', 六盘水: '六盘水市', 毕节: '毕节市', 安顺: '安顺市', 铜仁: '铜仁市',
  黔西南: '黔西南布依族苗族自治州', 黔东南: '黔东南苗族侗族自治州', 黔南: '黔南布依族苗族自治州'
}

function barColor(v: number) {
  if (v >= 6) return new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ff9a6b' }, { offset: 1, color: '#f5563c' }])
  if (v >= 4) return new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#ffd166' }, { offset: 1, color: '#ff9838' }])
  return new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#7fe3c4' }, { offset: 1, color: '#00b581' }])
}

function distOption(big: boolean): echarts.EChartsOption {
  return {
    animationDuration: 700,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255,255,255,.92)',
      borderColor: 'rgba(255,255,255,.9)',
      padding: [10, 14],
      textStyle: { color: '#1e293b', fontSize: 12 },
      extraCssText: 'backdrop-filter: blur(10px); border-radius: 10px; box-shadow: 0 12px 30px rgba(15,23,42,.16);',
      formatter: (p: any) =>
        `${fullCity[p.name] ?? p.name}<br/>异常预警 <b style="color:#e2572b">${p.value} 项</b>` +
        (big ? '<br/><span style="color:#94a3b8;font-size:11px">点击查看该市州专页</span>' : '')
    },
    grid: big ? { left: 34, right: 80, top: 26, bottom: 26, containLabel: true } : { left: 4, right: 50, top: 6, bottom: 4, containLabel: true },
    xAxis: { type: 'value', show: false, max: (v: any) => Math.max(1, Math.ceil(v.max * 1.18)) },
    yAxis: {
      type: 'category',
      inverse: true,
      data: distData.map((d) => d.city),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#475569', fontSize: big ? 13 : 11.5, fontWeight: 600, margin: big ? 14 : 8 }
    },
    series: [{
      type: 'bar',
      barWidth: big ? 18 : 11,
      showBackground: true,
      backgroundStyle: { color: '#f2f6fa', borderRadius: 99 },
      itemStyle: { borderRadius: 99, color: (p: any) => barColor(p.value) },
      label: {
        show: true, position: 'right', distance: big ? 10 : 6, formatter: '{c} 项',
        color: '#475569', fontSize: big ? 12.5 : 10.5, fontWeight: 600
      },
      data: distData.map((d) => d.value)
    }]
  }
}

function renderBar() {
  bar = echarts.init(barRef.value!)
  bar.setOption(distOption(false))
}

const zoomVisible = ref(false)
const zoomRef = ref<HTMLElement>()
let zoom: echarts.ECharts | null = null

function openZoom() {
  zoomVisible.value = true
}
function renderZoom() {
  if (!zoomRef.value) return
  zoom?.dispose()
  zoom = echarts.init(zoomRef.value)
  zoom.setOption(distOption(true))
  zoom.on('click', (p: any) => {
    const full = fullCity[p.name]
    if (!full) return
    zoomVisible.value = false
    router.push({ name: 'GovCityDetail', params: { name: full } })
  })
}
function onZoomClosed() {
  zoom?.dispose()
  zoom = null
}
watch(zoomVisible, (v) => {
  if (v) nextTick(() => setTimeout(renderZoom, 60))
  else onZoomClosed()
})

const resize = () => { bar?.resize(); zoom?.resize() }
onMounted(() => { renderBar(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); bar?.dispose(); zoom?.dispose() })
</script>

<style scoped lang="scss">
.banner-btn { margin-top: 16px; display: flex; gap: 10px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-box { height: 280px; }

.dist-card {
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: radial-gradient(420px 220px at 88% -30%, rgba(0, 209, 255, 0.1), transparent 70%);
  }
  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 18px 40px rgba(9, 60, 90, 0.14);
  }
}
.dist-actions { display: flex; align-items: center; gap: 8px; }
.zoom-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  border: 1px solid var(--qn-border);
  background: #fff;
  color: #00659a;
  cursor: pointer;
  transition: all 0.2s ease;
  &:hover { background: #e6f7ff; border-color: #9ad8f5; transform: scale(1.06); }
}

.dd-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding-right: 30px; }
.dd-title { font-size: 19px; font-weight: 700; color: var(--qn-text); letter-spacing: 0.5px; }
.dd-sub { font-size: 12px; color: #64748b; margin-top: 5px; }
.dd-tag { flex: none; color: #00659a; background: #e9f7ff; border-color: #bfe6f8; }
.zoom-chart { height: min(56vh, 470px); }
.dd-foot { padding: 2px 10px 6px; font-size: 11.5px; color: #94a3b8; text-align: center; }

:global(.glass-modal) {
  background: linear-gradient(145deg, rgba(226, 240, 248, 0.42), rgba(203, 226, 239, 0.36)) !important;
  backdrop-filter: blur(18px) saturate(125%);
  -webkit-backdrop-filter: blur(18px) saturate(125%);
}
:global(.dist-dialog) {
  border-radius: 22px !important;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.85);
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(240, 248, 253, 0.9)) !important;
  box-shadow: 0 36px 90px rgba(4, 40, 62, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}
:global(.dist-dialog .el-dialog__header) { padding: 22px 26px 4px; margin-right: 0; }
:global(.dist-dialog .el-dialog__body) { padding: 6px 18px 8px; }
.quick-grid-inner { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.qk-card { padding: 20px 14px; border-radius: 12px; border: 1px solid var(--qn-border); background: #fbfcfe; cursor: pointer; text-align: center; transition: all 0.2s;
  &:hover { transform: translateY(-3px); box-shadow: var(--qn-shadow); border-color: var(--qn-primary); }
  .qk-name { font-size: 14px; font-weight: 600; color: var(--qn-text); margin-top: 10px; }
  .qk-desc { font-size: 11.5px; color: var(--qn-text-3); margin-top: 4px; } }
@media (max-width: 1100px) { .stat-grid, .main-grid { grid-template-columns: 1fr 1fr; } .quick-grid-inner { grid-template-columns: repeat(2, 1fr); } }
</style>