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
        <div class="bs-clock">
          <div class="bc-date"><span class="bc-live"></span>{{ dateText }}</div>
          <div class="bc-time">{{ timeText }}</div>
        </div>
        <span class="hl-line r"></span>
        <el-button size="small" text class="exit-btn" @click="exitScreen">
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
      <div class="col center" @mouseleave="hideSpot">
        <div class="map-stage">
          <div ref="mapRef" class="map-panel"></div>
          <div class="map-fog" :class="{ on: !!spotCity }"></div>
          <div ref="spotRef" class="map-spot" :class="{ on: !!spotCity }"></div>
          <transition name="spot-in">
            <div v-if="spotCity" class="spot-card">
              <div class="sc-head">
                <span class="sc-name">{{ shortOf(spotCity.name) }}</span>
                <span class="sc-full">{{ spotCity.name }}</span>
              </div>
              <div class="sc-grid">
                <div class="sc-cell"><span>碳排放量</span><b>{{ spotCity.emission.toLocaleString() }}</b><i>tCO₂e</i></div>
                <div class="sc-cell"><span>占全省</span><b>{{ spotShare }}%</b></div>
                <div class="sc-cell"><span>在册企业</span><b>{{ spotEnterprises }}</b><i>家</i></div>
                <div class="sc-cell"><span>待处置预警</span><b class="warn">{{ spotCity.warnings }}</b><i>项</i></div>
              </div>
              <el-button size="small" class="sc-btn" @click="enterCity">进入{{ shortOf(spotCity.name) }}专页</el-button>
            </div>
          </transition>
        </div>
      </div>

      <!-- 右列 -->
      <div class="col right">
        <div class="panel warn-panel">
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

        <div class="panel rank-panel">
          <div class="panel-title"><span class="pt-bar"></span>各市州排放排名<span class="pt-note">悬浮地图查看数值</span></div>
          <div class="city-rank">
            <div v-for="(c, i) in cityRank" :key="c.name" class="cr-item">
              <span class="cr-no" :class="'t' + (i + 1)">{{ i + 1 }}</span>
              <span class="cr-name">{{ c.name }}</span>
              <div class="cr-bar"><div class="cr-fill" :style="{ width: c.pct + '%' }"></div></div>
              <span class="cr-val">{{ c.value.toLocaleString() }}</span>
            </div>
          </div>
        </div>

        <div class="panel hash-panel">
          <div class="panel-title"><span class="pt-bar"></span>链上存证流水<span class="pt-note">实时上链 · 哈希校验一致</span></div>
          <div class="hash-list">
            <div v-for="h in hashRows" :key="h.time + h.hash" class="hash-row">
              <span class="hr-time">{{ h.time }}</span>
              <div class="hr-main">
                <div class="hr-org">{{ h.org }}</div>
                <div class="hr-hash">{{ h.hash }}</div>
              </div>
              <span class="hr-tag">已确认</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { CITY_STATS, cityStatMap, provinceTotalEmission } from '@/config/regions'
import { CITY_DETAIL } from '@/config/cityDetail'
import type { CityStat } from '@/config/regions'

const heatColors = ['#0d5c8a', '#0e9b7d', '#0ec56f', '#a7d927', '#ffdf32']
const cityData: Record<string, number> = Object.fromEntries(CITY_STATS.map((c) => [c.name, c.emission]))
const shortName: Record<string, string> = {
  黔西南布依族苗族自治州: '黔西南州', 黔东南苗族侗族自治州: '黔东南州', 黔南布依族苗族自治州: '黔南州'
}
const cityRankBase = Object.entries(cityData).map(([name, value]) => ({ name: shortName[name] ?? name, value })).sort((a, b) => b.value - a.value)
const maxCityValue = cityRankBase[0]?.value ?? 1
const cityRank = cityRankBase.map((c) => ({ ...c, pct: Math.round((c.value / maxCityValue) * 100) }))
const shortOf = (name: string) => cityStatMap[name]?.short ?? name
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

const router = useRouter()
const dateText = ref('')
const timeText = ref('')
const bigTotal = ref(874629)
const strongTotal = ref(0.642)
const checkTotal = ref(12864)
const warnTotal = ref(23)

const pad = (n: number) => String(n).padStart(2, '0')

const hashRows = ref([
  { time: '21:32:08', org: '六盘水黔能矿业 · 月度核算包', hash: '0x7f3a…c21d' },
  { time: '21:31:44', org: '中伟新材料 · 化验单批次 2026-10', hash: '0x2c9e…41a7' },
  { time: '21:30:19', org: '遵义铝业 · 范围二购电凭证', hash: '0x8b1d…9f30' },
  { time: '21:28:52', org: '乌江航运 · 水运里程存证', hash: '0x4e77…02b8' }
])
const hashPool = [
  { org: '西南水泥 · 熟料产量台账', hash: '0x5d08…7a4f' },
  { org: '贵州磷化 · 因子引用快照', hash: '0x1f6b…d92e' },
  { org: '黔西南铁合金 · 矿热炉电耗', hash: '0x3b94…58c0' },
  { org: '铜仁锰业 · 运输轨迹回传', hash: '0x6e21…af76' }
]
let hashIdx = 0

let clockTimer = 0
let dataTimer = 0
let hashTimer = 0
let mapChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null
const mapRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
const spotRef = ref<HTMLElement>()
const spotCity = ref<CityStat | null>(null)
let spotChart: echarts.ECharts | null = null

const spotShare = computed(() => spotCity.value ? ((spotCity.value.emission / provinceTotalEmission) * 100).toFixed(1) : '0')
const spotEnterprises = computed(() => spotCity.value ? (CITY_DETAIL[spotCity.value.name]?.enterprises ?? '-') : '-')
function enterCity() {
  if (!spotCity.value) return
  router.push({ name: 'GovCityDetail', params: { name: spotCity.value.name } })
}

function showSpot(name: string) {
  const stat = cityStatMap[name]
  if (!stat || !spotRef.value) return
  spotCity.value = stat
  if (!spotChart) spotChart = echarts.init(spotRef.value)
  else spotChart.resize()
  const value = cityData[name] ?? stat.emission
  spotChart.setOption({
    animationDuration: 260,
    animationEasing: 'cubicOut',
    series: [{
      type: 'map', map: 'guizhou', z: 3, silent: true,
      zoom: 1.06, layoutCenter: ['50%', '50%'], layoutSize: '93%',
      itemStyle: { areaColor: 'transparent', borderWidth: 0 },
      data: Object.keys(cityData).map((n) => ({
        name: n,
        value: n === name ? value : 0,
        itemStyle: n === name
          ? { areaColor: '#0f9480', borderColor: '#9fffe0', borderWidth: 2, shadowColor: 'rgba(0, 230, 170, 0.55)', shadowBlur: 22 }
          : { opacity: 0 },
        label: { show: n === name }
      })),
      label: {
        show: false,
        position: 'bottom',
        distance: 8,
        formatter: () => `{n|${shortOf(name)}}\n{v|${value.toLocaleString()} tCO₂e}`,
        rich: {
          n: { color: '#ffffff', fontSize: 15, fontWeight: 700, lineHeight: 21, textShadowColor: 'rgba(0, 20, 16, 0.9)', textShadowBlur: 6 },
          v: { color: '#8fffd6', fontSize: 10.5, fontWeight: 700, lineHeight: 15 }
        },
        textBorderColor: 'rgba(2, 16, 32, 0.75)',
        textBorderWidth: 3
      }
    }]
  }, true)
  mapChart?.setOption({ tooltip: { show: false } })
}

function hideSpot() {
  if (!spotCity.value) return
  spotCity.value = null
  spotChart?.clear()
  mapChart?.setOption({ tooltip: { show: true } })
}

async function renderMap() {
  if (!mapRef.value) return
  mapChart = echarts.init(mapRef.value)
  const geo = await fetch('/map/guizhou.json').then((r) => r.json())
  echarts.registerMap('guizhou', geo)
  mapChart.setOption({
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(5, 20, 34, 0.94)',
      borderColor: 'rgba(0, 209, 255, 0.35)',
      textStyle: { color: '#d7e6f7' },
      formatter: (p: any) => {
        const st = cityStatMap[p.name]
        const v = cityData[p.name] ?? Math.round(p.data?.value ?? 0)
        const pct = ((v / provinceTotalEmission) * 100).toFixed(1)
        return `<div style="min-width:158px">
          <div style="font-size:13px;font-weight:700;margin-bottom:6px">${shortOf(p.name)}</div>
          <div style="font-size:12px;line-height:1.85">排放量：<b style="color:#4ce6b8">${v.toLocaleString()}</b> tCO₂e</div>
          <div style="font-size:12px;line-height:1.85">占全省：<b>${pct}%</b></div>
          <div style="font-size:12px;line-height:1.85">待处置预警：<b style="color:#ffd166">${st?.warnings ?? 0}</b> 家</div>
        </div>`
      }
    },
    visualMap: {
      min: 0, max: 15000,
      left: 22, bottom: '19%',
      orient: 'vertical',
      itemWidth: 14, itemHeight: 158,
      calculable: false,
      inRange: { color: heatColors },
      text: ['高', '低'],
      textGap: 10,
      textStyle: { color: '#9fb6d4', fontSize: 12, fontWeight: 600 },
      formatter: () => '',
      itemStyle: {
        borderColor: 'rgba(0, 209, 255, 0.45)',
        borderWidth: 1,
        shadowColor: 'rgba(0, 209, 255, 0.35)',
        shadowBlur: 10
      }
    },
    geo: {
      map: 'guizhou', roam: false, zoom: 1.06,
      layoutCenter: ['50%', '50%'], layoutSize: '93%',
      itemStyle: {
        areaColor: '#0c2238',
        borderColor: 'rgba(140, 245, 210, 0.68)',
        borderWidth: 1.2
      },
      emphasis: {
        itemStyle: { areaColor: '#12707a', borderColor: '#7ef0c2', borderWidth: 1.6 },
        label: { show: false }
      },
      label: {
        show: true, color: '#eaf5ff', fontSize: 11.5, fontWeight: 600, letterSpacing: 0.5,
        formatter: (p: any) => shortOf(p.name),
        textShadowColor: 'rgba(2, 16, 32, 0.9)', textShadowBlur: 4
      },
      select: { disabled: true }
    },
    series: [{
      type: 'map', map: 'guizhou', geoIndex: 0,
      data: Object.entries(cityData).map(([name, value]) => ({ name, value }))
    }, {
      // 毕节的威宁自治县被六盘水大湾镇飞地隔开，地图数据里是独立多边形，单独标注避免误读
      type: 'scatter', coordinateSystem: 'geo', geoIndex: 0, symbolSize: 6, zlevel: 3,
      itemStyle: { color: '#ffdf32', borderColor: 'rgba(255, 223, 50, 0.45)', borderWidth: 3, shadowColor: 'rgba(255,223,50,.7)', shadowBlur: 10 },
      label: {
        show: true, position: 'right', distance: 7, formatter: '威宁（毕节）',
        color: '#eaf5ff', fontSize: 11.5, fontWeight: 700,
        textShadowColor: 'rgba(2, 16, 32, 0.9)', textShadowBlur: 4
      },
      tooltip: { formatter: '威宁自治县 · 隶属毕节市<br/>中间隔六盘水市钟山区大湾镇飞地，故地图上呈两块' },
      data: [{ name: '威宁自治县', value: [104.23, 26.83] }]
    }]
  })
  mapChart.on('mouseover', (p: any) => {
    if (p.name === '威宁自治县') {
      hideSpot()
      return
    }
    if (cityStatMap[p.name]) showSpot(p.name)
  })
  mapChart.on('click', (p: any) => {
    if (p.name === '威宁自治县') {
      ElMessage.info('威宁自治县隶属毕节市，因中间隔六盘水市钟山区大湾镇飞地，地图上显示为独立色块')
      return
    }
    const stat = cityStatMap[p.name]
    if (!stat) return
    router.push({ name: 'GovCityDetail', params: { name: stat.name } })
  })
}

function exitScreen() {
  router.push({ path: '/gov/dashboard' })
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
  const d = new Date()
  dateText.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 星期${'日一二三四五六'[d.getDay()]}`
  timeText.value = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
function tickHash() {
  const d = new Date()
  const pick = hashPool[hashIdx++ % hashPool.length]
  hashRows.value = [{ time: `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`, ...pick }, ...hashRows.value].slice(0, 4)
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

const resize = () => { mapChart?.resize(); pieChart?.resize(); spotChart?.resize() }
onMounted(() => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  tickData()
  dataTimer = window.setInterval(tickData, 3000)
  hashTimer = window.setInterval(tickHash, 5200)
  renderMap()
  renderPie()
  window.addEventListener('resize', resize)
  drawBg()
  bgRaf = requestAnimationFrame(drawBg)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(dataTimer)
  clearInterval(hashTimer)
  window.removeEventListener('resize', resize)
  mapChart?.dispose(); pieChart?.dispose(); spotChart?.dispose()
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

.bs-clock {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  padding-right: 4px;
  .bc-date { display: flex; align-items: center; gap: 6px; font-size: 10.5px; letter-spacing: 1.5px; color: #7ea6c8; }
  .bc-live {
    width: 5px; height: 5px; border-radius: 50%;
    background: #00e5aa;
    box-shadow: 0 0 8px #00e5aa;
    animation: pulse 1.8s infinite;
  }
  .bc-time {
    font-size: 27px;
    font-weight: 800;
    line-height: 1.05;
    letter-spacing: 3px;
    font-variant-numeric: tabular-nums;
    background: linear-gradient(180deg, #f2fbff 0%, #9fe4ff 55%, #3fc4ff 100%);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    filter: drop-shadow(0 0 10px rgba(0, 190, 255, 0.45));
  }
}

.bs-body {
  position: relative; z-index: 2;
  flex: 1; min-height: 0;
  display: grid; grid-template-columns: 340px 1fr 340px;
  gap: 14px; padding: 8px 20px 18px;
}
.col { display: flex; flex-direction: column; gap: 14px; min-height: 0; }
.col.center { position: relative; }
.col.right > .panel { display: flex; flex-direction: column; min-height: 0; }
.warn-panel { flex: 1.3; }
.rank-panel { flex: 1.15; }
.hash-panel { flex: 0.95; }
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
  .pt-bar { width: 4px; height: 14px; border-radius: 2px; background: linear-gradient(180deg, #00b581, #00d1ff); }
  .pt-note { margin-left: auto; font-size: 10.5px; font-weight: 400; color: #5d7d9e; letter-spacing: 0.5px; } }
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
.map-stage { position: relative; flex: 1; min-height: 420px; }
.map-panel {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(52% 8% at 50% 79%, rgba(0, 209, 255, 0.26), transparent 70%),
    radial-gradient(46% 6% at 50% 83%, rgba(0, 0, 0, 0.55), transparent 72%),
    radial-gradient(64% 14% at 50% 84%, rgba(0, 181, 129, 0.14), transparent 75%);
}
.map-fog {
  position: absolute;
  inset: 0;
  z-index: 2;
  border-radius: 12px;
  pointer-events: none;
  opacity: 0;
  background: linear-gradient(160deg, rgba(6, 22, 38, 0.42), rgba(3, 14, 26, 0.5));
  backdrop-filter: blur(13px) saturate(85%);
  -webkit-backdrop-filter: blur(13px) saturate(85%);
  box-shadow: inset 0 0 80px rgba(0, 209, 255, 0.06);
  transition: opacity 0.3s ease;
  &.on { opacity: 1; }
}
.map-spot {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  opacity: 0;
  filter: drop-shadow(0 12px 20px rgba(0, 0, 0, 0.45));
  transform: translateY(8px) scale(0.995);
  transition: opacity 0.3s ease, transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);
  &.on { opacity: 1; transform: none; }
}
.spot-card {
  position: absolute;
  left: 16px;
  bottom: 16px;
  z-index: 4;
  width: 218px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(155deg, rgba(11, 36, 54, 0.94), rgba(5, 20, 34, 0.9));
  border: 1px solid rgba(0, 209, 255, 0.3);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.5), 0 0 26px rgba(0, 209, 255, 0.14), inset 0 1px 0 rgba(126, 240, 194, 0.14);
  backdrop-filter: blur(10px);
  .sc-head { display: flex; align-items: baseline; gap: 8px; padding-bottom: 9px; border-bottom: 1px dashed rgba(0, 209, 255, 0.22); }
  .sc-name { font-size: 19px; font-weight: 800; letter-spacing: 1px; color: #fff; text-shadow: 0 0 14px rgba(0, 229, 170, 0.5); }
  .sc-full { flex: 1; min-width: 0; font-size: 10.5px; color: #6f8fae; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
  .sc-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 10px; }
  .sc-cell {
    background: rgba(0, 180, 255, 0.06);
    border: 1px solid rgba(0, 180, 255, 0.12);
    border-radius: 8px;
    padding: 7px 9px;
    span { display: block; font-size: 10px; color: #6f8fae; letter-spacing: 0.5px; }
    b { font-size: 15px; font-weight: 700; color: #4ce6b8; font-variant-numeric: tabular-nums;
      &.warn { color: #ffd166; } }
    i { font-style: normal; font-size: 9.5px; color: #5d7d9e; margin-left: 3px; }
  }
  .sc-btn {
    width: 100%;
    margin-top: 11px;
    border: none;
    color: #041018;
    font-weight: 700;
    background: linear-gradient(120deg, #4ce6b8, #00d1ff);
    box-shadow: 0 8px 18px rgba(0, 200, 170, 0.28);
    &:hover { background: linear-gradient(120deg, #6ff0c9, #3fdcff); }
  }
}
.spot-in-enter-active,
.spot-in-leave-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.spot-in-enter-from,
.spot-in-leave-to { opacity: 0; transform: translateY(10px); }
.warn-list { display: flex; flex-direction: column; gap: 10px; flex: 1; min-height: 0; overflow-y: auto; }
.warn-item { display: flex; gap: 10px; padding: 10px 12px; border-radius: 8px; background: rgba(255, 176, 32, 0.06); border-left: 3px solid #ffb020;
  &.high { background: rgba(255, 80, 80, 0.08); border-left-color: #ff5a5a; }
  .w-dot { width: 7px; height: 7px; border-radius: 50%; background: #ffb020; margin-top: 5px; flex-shrink: 0; animation: pulse 1.6s infinite;
    .high & { background: #ff5a5a; } }
  .w-title { font-size: 12.5px; color: #d7e6f7; display: flex; align-items: center; gap: 8px; }
  .w-sub { font-size: 11px; color: #6683a3; margin-top: 4px; line-height: 1.5; } }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.25; } }
.city-rank { display: flex; flex-direction: column; gap: 6px; flex: 1; min-height: 0; overflow-y: auto; }
.cr-item { display: flex; align-items: center; gap: 8px; padding: 5px 10px; border-radius: 7px;
  background: rgba(0, 180, 255, 0.045); border: 1px solid rgba(0, 180, 255, 0.08);
  transition: background 0.2s ease, border-color 0.2s ease; }
.cr-item:hover { background: rgba(0, 209, 255, 0.1); border-color: rgba(0, 209, 255, 0.22); }
.cr-no { width: 20px; height: 20px; flex: none; border-radius: 5px; text-align: center; line-height: 20px; font-size: 11.5px; font-weight: 700;
  color: #7ea6c8; background: rgba(120, 160, 200, 0.16);
  &.t1 { background: linear-gradient(135deg, #ffd76a, #ff9f1a); color: #3a2600; }
  &.t2 { background: linear-gradient(135deg, #e2edf8, #9fb6d4); color: #20344a; }
  &.t3 { background: linear-gradient(135deg, #e8b487, #b87333); color: #33200c; } }
.cr-name { width: 62px; flex: none; font-size: 12px; color: #cde4f7; letter-spacing: 0.5px; white-space: nowrap; }
.cr-bar { flex: 1; height: 6px; border-radius: 3px; background: rgba(120, 160, 200, 0.12); overflow: hidden; }
.cr-fill { height: 100%; border-radius: 3px; background: linear-gradient(90deg, #00b581, #00d1ff); }
.cr-val { width: 52px; flex: none; text-align: right; font-size: 11px; color: #4cc9ff; font-variant-numeric: tabular-nums; }

.hash-list { display: flex; flex-direction: column; gap: 6px; flex: 1; min-height: 0; overflow: hidden;
  mask-image: linear-gradient(180deg, #000 82%, transparent 100%);
  -webkit-mask-image: linear-gradient(180deg, #000 82%, transparent 100%); }
.hash-row { display: flex; align-items: center; gap: 8px; padding: 6px 9px; border-radius: 6px;
  background: rgba(0, 180, 255, 0.045); border: 1px solid rgba(0, 180, 255, 0.08);
  animation: hashIn 0.45s ease; }
.hr-time { width: 44px; flex: none; font-size: 10px; color: #5d7d9e; font-variant-numeric: tabular-nums; }
.hr-main { flex: 1; min-width: 0; }
.hr-org { font-size: 11px; color: #cde4f7; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hr-hash { font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace; font-size: 9.5px; color: #4cc9ff; opacity: 0.85; letter-spacing: 0.3px; }
.hr-tag { flex: none; font-size: 9.5px; color: #4ce6b8; border: 1px solid rgba(0, 229, 170, 0.3); border-radius: 99px; padding: 1px 6px; }
@keyframes hashIn { from { opacity: 0; transform: translateY(-6px); } to { opacity: 1; transform: none; } }
</style>