<template>
  <div class="big-screen">
    <canvas ref="bgRef" class="bg-canvas"></canvas>
    <!-- 面板装饰边框 -->
    <div class="panel-tl"></div><div class="panel-tr"></div><div class="panel-bl"></div><div class="panel-br"></div>

    <!-- 顶部标题 -->
    <header class="bs-head">
      <div class="head-side left">
        <span class="hl-line"></span>
        <span class="hl-text">碳数据 · 双轨合规治理</span>
      </div>
      <div class="head-title">
        <div class="ht-main">贵州省重点产业碳排放态势感知大屏</div>
        <div class="ht-sub">CARBON EMISSION SITUATION AWARENESS · GUIZHOU PROVINCE</div>
      </div>
      <div class="head-side right">
        <span class="hl-text r-text">{{ now }}</span>
        <span class="hl-line r"></span>
        <el-button size="small" text class="exit-btn" @click="$router.push('/gov/dashboard')">
          <el-icon><Close /></el-icon>退出大屏
        </el-button>
      </div>
    </header>

    <div class="bs-body">
      <!-- 左列 -->
      <div class="col left">
        <div class="panel">
          <div class="panel-title"><span class="pt-bar"></span>全省排放总量（实时）</div>
          <div class="total-block">
            <div class="tb-main"><span class="num-roll big-num">{{ bigTotal }}</span><span class="tb-unit">tCO₂e</span></div>
            <div class="tb-sub">较昨日 <b class="up">+2.3%</b> ｜ 覆盖企业 <b>2,384</b> 家</div>
          </div>
          <div class="mini-grid">
            <div class="mini"><div class="m-num">{{ strongTotal }}</div><div class="m-label">碳强度均值 t/t</div></div>
            <div class="mini"><div class="m-num cyan">{{ checkTotal }}</div><div class="m-label">本月存证哈希</div></div>
            <div class="mini"><div class="m-num warn">{{ warnTotal }}</div><div class="m-label">待处置预警</div></div>
          </div>
        </div>

        <div class="panel tall">
          <div class="panel-title"><span class="pt-bar"></span>重点行业排放排行（tCO₂e）</div>
          <div class="rank-list">
            <div v-for="(r, i) in industryRank" :key="r.name" class="rank-item">
              <span class="rank-no" :class="'no' + i">{{ i + 1 }}</span>
              <span class="rank-name">{{ r.name }}</span>
              <div class="rank-bar"><div class="rank-fill" :style="{ width: r.pct + '%' }"></div></div>
              <span class="rank-val">{{ r.value }}</span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title"><span class="pt-bar"></span>主要排放源构成</div>
          <div ref="pieRef" class="bs-chart"></div>
        </div>
      </div>

      <!-- 中列：贵州地图 -->
      <div class="col center">
        <div ref="mapRef" class="map-panel"></div>
        <div class="map-legend">
          <span>低</span>
          <span v-for="i in 5" :key="i" class="lg-blk" :style="{ background: heatColors[i - 1] }"></span>
          <span>高</span>
          <span class="lg-tip">市州排放热力 · F12 五级分位着色</span>
        </div>
      </div>

      <!-- 右列 -->
      <div class="col right">
        <div class="panel">
          <div class="panel-title"><span class="pt-bar"></span>智能初审预警（滚动）</div>
          <div class="warn-list">
            <div v-for="w in warnings" :key="w.id" class="warn-item" :class="w.level">
              <span class="w-dot"></span>
              <div class="w-body">
                <div class="w-title">{{ w.title }}<el-tag size="small" :type="w.level === 'high' ? 'danger' : 'warning'" round>{{ w.level === 'high' ? '高' : '中' }}</el-tag></div>
                <div class="w-sub">{{ w.reason }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title"><span class="pt-bar"></span>各市州排放（tCO₂e）</div>
          <div class="city-bars">
            <div v-for="c in cityRank" :key="c.name" class="cb-row">
              <span class="cb-name">{{ c.name }}</span>
              <el-progress :percentage="100" :show-text="false" :stroke-width="6" :color="heatColors[0]">
                <template #default><div class="cb-inner" :style="{ width: c.pct + '%', background: heatColors[0] }"></div></template>
              </el-progress>
              <span class="cb-val">{{ c.value }}</span>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title"><span class="pt-bar"></span>大屏快照</div>
          <div class="snap-note">每 5 分钟自动生成大屏数据快照并写入 sys_big_screen_snapshot，纳入哈希链存证（F10），支持审计回放。</div>
          <el-button size="small" type="primary" plain round class="snap-btn" @click="fakeSnapshot">手动存证快照</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const heatColors = ['#0d5c8a', '#0e9b7d', '#0ec56f', '#a7d927', '#ffdf32']
const cityData: Record<string, number> = {
  贵阳市: 12460, 六盘水市: 14730, 遵义市: 10820, 安顺市: 5210, 毕节市: 13680,
  铜仁市: 4620, 黔西南布依族苗族自治州: 6140, 黔东南苗族侗族自治州: 3880, 黔南布依族苗族自治州: 5760
}
const shortName: Record<string, string> = {
  黔西南布依族苗族自治州: '黔西南州', 黔东南苗族侗族自治州: '黔东南州', 黔南布依族苗族自治州: '黔南州'
}
const cityRank = Object.entries(cityData).map(([name, value]) => ({ name: shortName[name] ?? name, value, pct: Math.round((value / 14730) * 100) })).sort((a, b) => b.value - a.value)
const industryRank = [
  { name: '煤炭开采和洗选业', value: 62400, pct: 100 },
  { name: '电力、热力生产供应', value: 51800, pct: 83 },
  { name: '化学原料及制品', value: 24300, pct: 39 },
  { name: '有色金属冶炼', value: 21700, pct: 35 },
  { name: '非金属矿物制品', value: 15800, pct: 25 },
  { name: '黑色金属冶炼压延', value: 12600, pct: 20 },
  { name: '交通运输仓储邮政', value: 8900, pct: 14 },
  { name: '石油煤炭及其他燃料加工', value: 6700, pct: 11 }
]

const warnings = [
  { id: 1, level: 'high', title: '六盘水 · 某矿业公司', reason: '原煤消耗环比 +38%，超过 3σ 阈值（F6 波动率 41.2%）' },
  { id: 2, level: 'mid', title: '毕节 · 某焦化企业', reason: '低位发热量缺省因子连续 3 期，数据完整度下降' },
  { id: 3, level: 'mid', title: '遵义 · 某铝业公司', reason: '范围二外购电环比 +15%，高于行业均值 9 个百分点' },
  { id: 4, level: 'mid', title: '贵阳 · 某水泥企业', reason: '化验单未按时上传，自动标记待核查' },
  { id: 5, level: 'skin', title: '安顺 · 某磷化企业', reason: '数据包签章文件即将到期' },
  { id: 6, level: 'high', title: '黔西南 · 某铁合金企业', reason: '矿热炉电耗强度超基准值 22%，疑似漏报范围一' },
  { id: 7, level: 'mid', title: '铜仁 · 某锰业公司', reason: '运输里程与 GPS 轨迹偏差 8.6%，待线下核查' },
  { id: 8, level: 'skin', title: '黔南 · 某硅材料企业', reason: '因子版本即将切换，提示更新引用' }
]

const now = ref('')
const bigTotal = ref(874629)
const strongTotal = ref(0.642)
const checkTotal = ref(12864)
const warnTotal = ref(23)

let clockTimer = 0
let dataTimer = 0
let mapChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
const mapRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()

async function renderMap() {
  if (!mapRef.value) return
  mapChart = echarts.init(mapRef.value)
  const geo = await fetch('/map/guizhou.json').then((r) => r.json())
  echarts.registerMap('guizhou', geo)
  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => `${p.name}<br/>排放量：<b>${p.data?.value ?? 0}</b> tCO₂e（占${((p.data?.value ?? 0) / bigTotal.value * 100).toFixed(1)}%）`
    },
    visualMap: {
      min: 0, max: 15000, left: 20, bottom: 10, calculable: false,
      inRange: { color: heatColors }, textStyle: { color: '#9fb6d4' },
      formatter: (v: number) => v + ''
    },
    geo: {
      map: 'guizhou', roam: false, zoom: 1.06, layoutCenter: ['50%', '52%'], layoutSize: '88%',
      itemStyle: {
        areaColor: '#0c2238',
        borderColor: 'rgba(0,209,255,0.55)',
        borderWidth: 1,
        shadowColor: 'rgba(0,209,255,0.35)',
        shadowBlur: 8
      },
      emphasis: { itemStyle: { areaColor: '#123c56' }, label: { show: true, color: '#fff' } },
      label: { show: true, color: '#b9d3ea', fontSize: 11 },
      select: { disabled: true }
    },
    series: [{
      type: 'map', map: 'guizhou', geoIndex: 0,
      data: Object.entries(cityData).map(([name, value]) => ({ name, value }))
    }]
  })
  mapChart.on('click', (p: any) => {
    ElMessage.info({ message: p.name + ' 详情：点击跳转监管工作台市级下钻', grouping: true })
  })
}

function renderPie() {
  if (!pieRef.value) return
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['42%', '66%'], center: ['50%', '50%'],
      label: { color: '#9fb6d4', formatter: '{b}\n{d}%', fontSize: 10 },
      itemStyle: { borderColor: '#071523', borderWidth: 2, borderRadius: 5 },
      data: [
        { value: 62400, name: '煤炭', itemStyle: { color: '#00b581' } },
        { value: 51800, name: '电力', itemStyle: { color: '#00d1ff' } },
        { value: 24300, name: '化工', itemStyle: { color: '#a06bff' } },
        { value: 21700, name: '冶金', itemStyle: { color: '#ffb020' } },
        { value: 15800, name: '建材', itemStyle: { color: '#ff6b6b' } }
      ]
    }]
  })
}

function tickClock() {
  now.value = new Date().toLocaleString('zh-CN', { hour12: false })
}
function tickData() {
  bigTotal.value = 874629 + Math.round(Math.sin(Date.now() / 6000) * 420)
  warnTotal.value = 23 + Math.round(Math.sin(Date.now() / 9000) * 2)
  // 地图热力微波动（F12 分级跟随变化）
  if (mapChart) {
    mapChart.setOption({
      series: [{
        data: Object.entries(cityData).map(([name, value]) => ({
          name,
          value: Math.round(value * (1 + Math.sin(Date.now() / 5200 + name.length) * 0.03))
        }))
      }]
    })
  }
}

function fakeSnapshot() {
  ElMessage.success('大屏快照已生成并写入哈希链（区块高度 +1）')
}

const resize = () => { mapChart?.resize(); pieChart?.resize() }
onMounted(() => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  tickData()
  dataTimer = window.setInterval(tickData, 3000)
  renderMap()
  renderPie()
  window.addEventListener('resize', resize)
  drawBg()
  bgRaf = requestAnimationFrame(drawBg)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(dataTimer)
  window.removeEventListener('resize', resize)
  mapChart?.dispose(); pieChart?.dispose()
})

/* ---------- 背景粒子 ---------- */
const bgRef = ref<HTMLCanvasElement>()
let bgRaf = 0
function drawBg() {
  const canvas = bgRef.value
  if (!canvas) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = window.innerWidth * dpr
  canvas.height = window.innerHeight * dpr
  const ctx = canvas.getContext('2d')!
  const W = canvas.width, H = canvas.height
  ctx.clearRect(0, 0, W, H)
  const t = Date.now() / 1000
  for (let i = 0; i < 70; i++) {
    const x = (i * 137.5 + t * (4 + (i % 5))) % W
    const y = (i * 89.7 + Math.sin(t + i) * 30) % H
    const a = 0.12 + 0.2 * Math.abs(Math.sin(t / 2 + i))
    ctx.fillStyle = `rgba(0,209,255,${a.toFixed(3)})`
    ctx.beginPath(); ctx.arc(x, y, 1.1 * dpr, 0, Math.PI * 2); ctx.fill()
  }
  bgRaf = requestAnimationFrame(drawBg)
}
</script>

<style scoped lang="scss">
.big-screen {
  position: fixed;
  inset: 0;
  overflow: hidden;
  color: #d7e6f7;
  background:
    radial-gradient(1000px 600px at 70% 20%, rgba(0, 80, 140, 0.25), transparent 60%),
    radial-gradient(900px 500px at 20% 80%, rgba(0, 90, 70, 0.18), transparent 55%),
    linear-gradient(165deg, #040b16 0%, #061323 55%, #040a14 100%);
  display: flex;
  flex-direction: column;
}
.bg-canvas { position: absolute; inset: 0; z-index: 0; pointer-events: none; }
.panel-tl, .panel-tr, .panel-bl, .panel-br {
  position: absolute; width: 120px; height: 120px; z-index: 1; pointer-events: none;
  border: 2px solid transparent;
}
.panel-tl { top: -2px; left: -2px; border-top-color: rgba(0,209,255,.7); border-left-color: rgba(0,209,255,.7); }
.panel-tr { top: -2px; right: -2px; border-top-color: rgba(0,209,255,.7); border-right-color: rgba(0,209,255,.7); }
.panel-bl { bottom: -2px; left: -2px; border-bottom-color: rgba(0,209,255,.7); border-left-color: rgba(0,209,255,.7); }
.panel-br { bottom: -2px; right: -2px; border-bottom-color: rgba(0,209,255,.7); border-right-color: rgba(0,209,255,.7); }

.bs-head {
  position: relative; z-index: 2;
  display: grid; grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 14px 28px;
  .head-side { display: flex; align-items: center; gap: 12px; font-size: 13px; letter-spacing: 2px; color: #9fb6d4;
    &.right { justify-content: flex-end; } }
  .hl-line { width: 90px; height: 1px; background: linear-gradient(90deg, transparent, rgba(0,209,255,.8));
    &.r { background: linear-gradient(90deg, rgba(0,209,255,.8), transparent); } }
  .head-title { text-align: center; }
  .ht-main { font-size: 26px; font-weight: 700; letter-spacing: 5px;
    background: linear-gradient(180deg, #eaf6ff 0%, #8fd8ff 60%, #4cc9ff 100%);
    -webkit-background-clip: text; background-clip: text; color: transparent;
    text-shadow: none; filter: drop-shadow(0 0 12px rgba(0,180,255,.35)); }
  .ht-sub { font-size: 10px; letter-spacing: 3px; color: #4d7a9e; margin-top: 4px; }
  .exit-btn { color: #7ea6c8; margin-left: 10px; &:hover { color: #fff; } }
}

.bs-body {
  position: relative; z-index: 2;
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: 340px 1fr 340px;
  gap: 14px; padding: 8px 20px 18px;
}
.col { display: flex; flex-direction: column; gap: 14px; min-height: 0; }
.panel {
  position: relative;
  background: rgba(8, 24, 42, 0.55);
  border: 1px solid rgba(0, 209, 255, 0.16);
  border-radius: 8px;
  padding: 14px 16px;
  backdrop-filter: blur(4px);
  box-shadow: inset 0 0 24px rgba(0, 140, 200, 0.06);
  &.tall { flex: 1; display: flex; flex-direction: column; min-height: 0; }
  &::after {
    content: ''; position: absolute; top: -1px; left: 14px; width: 46px; height: 2px;
    background: linear-gradient(90deg, #00b581, #00d1ff);
  }
}
.panel-title { font-size: 14px; font-weight: 600; color: #cde4f7; letter-spacing: 2px; margin-bottom: 12px; display: flex; align-items: center; gap: 8px;
  .pt-bar { width: 4px; height: 14px; border-radius: 2px; background: linear-gradient(180deg, #00b581, #00d1ff); } }
.total-block .tb-main { display: flex; align-items: baseline;
  .big-num { font-size: 42px; font-weight: 800; color: #fff; font-variant-numeric: tabular-nums;
    text-shadow: 0 0 24px rgba(0,229,170,.45); }
  .tb-unit { margin-left: 10px; font-size: 14px; color: #7ea6c8; } }
.tb-sub { font-size: 12px; color: #6683a3; margin: 4px 0 12px;
  b { font-weight: 700; &.up { color: #ff8f8f; } } }
.mini-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.mini { text-align: center; background: rgba(0,180,255,0.06); border-radius: 8px; padding: 10px 6px; border: 1px solid rgba(0,180,255,0.12);
  .m-num { font-size: 18px; font-weight: 700; color: #2bd8a2; font-variant-numeric: tabular-nums;
    &.cyan { color: #4cc9ff; } &.warn { color: #ffb020; } }
  .m-label { font-size: 10.5px; color: #6683a3; margin-top: 4px; } }
.rank-list { display: flex; flex-direction: column; gap: 12px; flex: 1; min-height: 0; overflow-y: auto; }
.rank-item { display: flex; align-items: center; gap: 10px; font-size: 12.5px;
  .rank-no { width: 18px; height: 18px; border-radius: 5px; text-align: center; line-height: 18px; font-size: 12px; background: rgba(120,160,200,.2);
    &.no0 { background: #ffb020; color: #041018; font-weight: 700; } &.no1 { background: #a8c8e8; color: #041018; } &.no2 { background: #c98d5e; color: #041018; } }
  .rank-name { width: 118px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #b9d3ea; }
  .rank-bar { flex: 1; height: 8px; border-radius: 4px; background: rgba(120,160,200,.12); overflow: hidden;
    .rank-fill { height: 100%; border-radius: 4px; background: linear-gradient(90deg, #00b581, #00d1ff); } }
  .rank-val { color: #4cc9ff; font-variant-numeric: tabular-nums; width: 62px; text-align: right; } }
.bs-chart { height: 190px; }
.map-panel { flex: 1; min-height: 420px; }
.map-legend { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 6px 0; font-size: 11px; color: #6683a3;
  .lg-blk { width: 22px; height: 8px; border-radius: 2px; }
  .lg-tip { margin-left: 10px; letter-spacing: 1px; } }
.warn-list { display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow-y: auto; }
.warn-item { display: flex; gap: 10px; padding: 10px 12px; border-radius: 8px; background: rgba(255, 176, 32, 0.06); border-left: 3px solid #ffb020;
  &.high { background: rgba(255, 80, 80, 0.08); border-left-color: #ff5a5a; }
  .w-dot { width: 7px; height: 7px; border-radius: 50%; background: #ffb020; margin-top: 5px; flex-shrink: 0; animation: pulse 1.6s infinite;
    .high & { background: #ff5a5a; } }
  .w-title { font-size: 12.5px; color: #d7e6f7; display: flex; align-items: center; gap: 8px; }
  .w-sub { font-size: 11px; color: #6683a3; margin-top: 4px; line-height: 1.5; } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.25; } }
.city-bars { display: flex; flex-direction: column; gap: 13px; }
.cb-row { display: flex; align-items: center; gap: 10px; font-size: 12px; flex: 1;
  .cb-name { width: 60px; color: #b9d3ea; }
  :deep(.el-progress) { flex: 1; } .cb-inner { height: 100%; border-radius: 3px; transition: width .8s; }
  .cb-val { width: 56px; text-align: right; color: #4cc9ff; font-variant-numeric: tabular-nums; } }
.snap-note { font-size: 11.5px; color: #6683a3; line-height: 1.7; }
.snap-btn { margin-top: 10px; }
</style>