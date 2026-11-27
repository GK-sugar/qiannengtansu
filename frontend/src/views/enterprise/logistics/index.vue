<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">物流追踪</div>
      <div class="sub">登记车牌与起终点，高德地图绘出模拟轨迹（未配置 JS Key 时自动切换 Canvas 自绘轨迹），自动累计里程并按 F4 折算范围三排放</div>
    </div>

    <div class="logi-grid">
      <!-- 左：登记表 -->
      <div class="page-card">
        <div class="page-card-title"><h3>运输任务登记</h3></div>
        <el-form label-width="90px" label-position="left">
          <el-form-item label="车牌号"><el-input v-model="form.plate" placeholder="如 贵A·XXXXX" /></el-form-item>
          <el-form-item label="起点">
            <el-select v-model="form.from" style="width: 100%">
              <el-option v-for="c in cities" :key="c.name" :label="c.name" :value="c.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="终点">
            <el-select v-model="form.to" style="width: 100%">
              <el-option v-for="c in cities" :key="c.name" :label="c.name" :value="c.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="货物 / 载重">
            <el-input-number v-model="form.weight" :min="1" :max="60" style="width: 60%" controls-position="right" />
            <span class="unit">t</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="planning" @click="onPlan">规划轨迹</el-button>
            <el-button plain @click="onSave">存为运输记录</el-button>
          </el-form-item>
        </el-form>

        <el-divider />
        <div class="result-block">
          <div class="rb-row"><span>推算里程</span><b>{{ planned.mileage }} km</b></div>
          <div class="rb-row"><span>周转量</span><b>{{ planned.turnover }} t·km</b></div>
          <div class="rb-row"><span>范围三排放（F4）</span><b class="green">{{ planned.emission }} tCO₂e</b></div>
          <div class="rb-row"><span>轨迹点</span><b>{{ planned.points }} 个</b></div>
        </div>
      </div>

      <!-- 右：地图 -->
      <div class="map-wrap page-card">
        <div class="map-head">
          <el-tag round size="small" :type="mapMode === 'amap' ? 'success' : 'info'">
            {{ mapMode === 'amap' ? '高德地图 JS API · 真实底图' : '轨迹示意图 · Canvas 渲染' }}
          </el-tag>
          <span class="map-tip">{{ mapTip }}</span>
        </div>
        <div ref="mapBox" class="map-box" @mousemove="onMove">
          <div ref="amapBox" v-show="mapMode === 'amap'" class="amap-box"></div>
          <canvas ref="trackCanvas" v-show="mapMode === 'canvas'" class="track-canvas"></canvas>
          <div v-show="mapMode === 'canvas'" class="map-mark start-mark">起</div>
          <div v-show="mapMode === 'canvas'" class="map-mark end-mark">终</div>
          <div class="map-hud">
            <div class="hud-title">模拟在途 · {{ form.plate || '贵A·88888' }}</div>
            <div class="hud-miles">{{ Math.round(travelKm) }} / {{ planned.mileage }} km</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史轨迹 -->
    <div class="page-card">
      <div class="page-card-title"><h3>近期运输轨迹</h3></div>
      <el-table :data="tracks" size="small">
        <el-table-column prop="plate" label="车牌" width="130" />
        <el-table-column prop="route" label="路线" />
        <el-table-column prop="mileage" label="里程(km)" width="100" />
        <el-table-column prop="emission" label="排放(tCO₂e)" width="110" />
        <el-table-column prop="time" label="完成时间" width="150" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === '已完成' ? 'success' : 'warning'" round>{{ row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const amapKey = import.meta.env.VITE_AMAP_JS_KEY || ''
const mapMode = ref<'canvas' | 'amap'>('canvas')
const mapTip = ref(amapKey ? '正在尝试加载高德底图…' : '在 .env 配置 VITE_AMAP_JS_KEY 后自动切换高德底图')

const cities = [
  { name: '贵阳市', lng: 106.63, lat: 26.65 },
  { name: '六盘水市', lng: 104.83, lat: 26.59 },
  { name: '遵义市', lng: 106.93, lat: 27.73 },
  { name: '安顺市', lng: 105.93, lat: 26.25 },
  { name: '毕节市', lng: 105.29, lat: 27.30 },
  { name: '铜仁市', lng: 109.19, lat: 27.72 },
  { name: '黔西南州', lng: 104.90, lat: 25.09 },
  { name: '黔东南州', lng: 107.98, lat: 26.58 },
  { name: '黔南州', lng: 107.52, lat: 26.26 }
]

const form = reactive({ plate: '贵A·88888', from: '六盘水市', to: '贵阳市', weight: 32 })
const planning = ref(false)
const planned = reactive({ mileage: 0, turnover: 0, emission: 0, points: 0 })

const tracks = [
  { plate: '贵A·88888', route: '六盘水市 → 贵阳市', mileage: 251, emission: 0.498, time: '2026-10-29 14:20', status: '已完成' },
  { plate: '贵B·61237', route: '毕节市 → 遵义市', mileage: 186, emission: 0.369, time: '2026-10-27 09:05', status: '已完成' },
  { plate: '贵C·33012', route: '贵阳市 → 黔南州', mileage: 133, emission: 0.264, time: '2026-10-25 16:48', status: '在途' },
  { plate: '贵A·52609', route: '贵阳市 → 遵义市', mileage: 147, emission: 0.292, time: '2026-10-24 11:32', status: '已完成' },
  { plate: '贵B·70415', route: '六盘水市 → 黔西南州', mileage: 212, emission: 0.421, time: '2026-10-22 08:15', status: '已完成' },
  { plate: '贵C·18873', route: '安顺市 → 贵阳市', mileage: 98, emission: 0.194, time: '2026-10-20 15:40', status: '已完成' },
  { plate: '贵A·90226', route: '贵阳市 → 黔东南州', mileage: 168, emission: 0.333, time: '2026-10-18 10:07', status: '已完成' },
  { plate: '贵B·45108', route: '毕节市 → 六盘水市', mileage: 203, emission: 0.403, time: '2026-10-16 13:52', status: '已完成' },
  { plate: '贵C·62794', route: '遵义市 → 铜仁市', mileage: 295, emission: 0.585, time: '2026-10-14 09:26', status: '已完成' }
]

// ---------- Canvas 自绘轨迹 ----------
const mapBox = ref<HTMLElement>()
const trackCanvas = ref<HTMLCanvasElement>()
let animRaf = 0
const travelKm = ref(0)

function hav(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371, toRad = Math.PI / 180
  const dLat = (lat2 - lat1) * toRad, dLng = (lng2 - lng1) * toRad
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(a))
}

function drawRoute() {
  const canvas = trackCanvas.value
  const box = mapBox.value
  if (!canvas || !box) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const W = (canvas.width = box.clientWidth * dpr)
  const H = (canvas.height = box.clientHeight * dpr)
  const ctx = canvas.getContext('2d')!
  ctx.scale(dpr, dpr)
  const w = box.clientWidth, h = box.clientHeight

  const from = cities.find((c) => c.name === form.from) || cities[1]
  const to = cities.find((c) => c.name === form.to) || cities[0]
  // 起点固定左侧，终点固定右侧
  const p1 = { x: w * 0.18, y: h * 0.28 }
  const p2 = { x: w * 0.82, y: h * 0.62 }
  const dist = Math.hypot(p2.x - p1.x, p2.y - p1.y)

  const draw = (t: number) => {
    ctx.clearRect(0, 0, w, h)
    // 底图感：网格 + 深浅分区 + 城市点
    ctx.strokeStyle = 'rgba(140,170,210,0.14)'
    ctx.lineWidth = 1
    for (let x = 0; x < w; x += 34) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke() }
    for (let y = 0; y < h; y += 34) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke() }
    ctx.fillStyle = 'rgba(0,181,129,0.05)'
    ctx.fillRect(0, 0, w * 0.5, h)
    // 城市标注
    for (const c of cities) {
      const cx = (c.lng - 104.4) / (109.6 - 104.4) * (w * 0.92) + w * 0.06
      const cy = (27.7 - c.lat) / (27.7 - 24.8) * (h * 0.72) + h * 0.2
      ctx.fillStyle = 'rgba(90,112,146,0.85)'
      ctx.beginPath(); ctx.arc(cx, cy, 3, 0, Math.PI * 2); ctx.fill()
      ctx.font = '10px sans-serif'
      ctx.fillText(c.name, cx + 5, cy - 4)
    }
    // 轨迹虚线
    ctx.setLineDash([8, 7])
    ctx.strokeStyle = 'rgba(0,181,129,0.42)'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(p1.x, p1.y)
    ctx.bezierCurveTo(p1.x + dist * 0.25, p1.y - dist * 0.22, p2.x - dist * 0.25, p2.y + dist * 0.2, p2.x, p2.y)
    ctx.stroke()
    ctx.setLineDash([])
    // 行驶进度光点
    const p = (Math.sin(t / 1300) + 1) / 2
    const tt = 1 - p
    const x = Math.pow(1 - tt, 3) * p1.x + 3 * Math.pow(1 - tt, 2) * tt * (p1.x + dist * 0.25) + 3 * (1 - tt) * tt * tt * (p2.x - dist * 0.25) + Math.pow(tt, 3) * p2.x
    const y = Math.pow(1 - tt, 3) * p1.y + 3 * Math.pow(1 - tt, 2) * tt * (p1.y - dist * 0.22) + 3 * (1 - tt) * tt * tt * (p2.y + dist * 0.2) + Math.pow(tt, 3) * p2.y
    const glow = ctx.createRadialGradient(x, y, 0, x, y, 18)
    glow.addColorStop(0, 'rgba(0,233,180,0.95)')
    glow.addColorStop(1, 'rgba(0,233,180,0)')
    ctx.fillStyle = glow
    ctx.beginPath(); ctx.arc(x, y, 18, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#fff'
    ctx.beginPath(); ctx.arc(x, y, 4.5, 0, Math.PI * 2); ctx.fill()
    travelKm.value = Math.round(planned.mileage * p)
    animRaf = requestAnimationFrame(draw)
  }
  animRaf = requestAnimationFrame(draw)
}

// ---------- 高德真实底图（加载失败自动回退 Canvas，绝不白屏） ----------
const amapBox = ref<HTMLElement>()
let amap: any = null
let amapLine: any = null
let amapMarks: any[] = []
let moveTimer = 0

function routeLngLat(): [number, number][] {
  const from = cities.find((c) => c.name === form.from) || cities[1]
  const to = cities.find((c) => c.name === form.to) || cities[0]
  const pts: [number, number][] = []
  const c1 = [from.lng + (to.lng - from.lng) * 0.25, from.lat + 0.5]
  const c2 = [to.lng - (to.lng - from.lng) * 0.25, to.lat - 0.5]
  for (let i = 0; i <= 30; i++) {
    const t = i / 30, u = 1 - t
    pts.push([
      u * u * u * from.lng + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * to.lng,
      u * u * u * from.lat + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * to.lat
    ])
  }
  return pts
}

function drawAmapRoute() {
  if (!amap) return
  const AMap = (window as any).AMap
  const path = routeLngLat()
  amapLine?.setMap(null)
  amapMarks.forEach((m) => m.setMap(null))
  amapLine = new AMap.Polyline({ path, strokeColor: '#00e9b4', strokeWeight: 3, strokeStyle: 'dashed', strokeOpacity: 0.9, showDir: true })
  const mk = (pos: [number, number], text: string, color: string) => new AMap.Marker({
    position: pos,
    content: `<div style="width:24px;height:24px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);background:${color};color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;"><span style="transform:rotate(45deg)">${text}</span></div>`,
    offset: new AMap.Pixel(-12, -24)
  })
  amapMarks = [mk(path[0], '起', '#00b581'), mk(path[path.length - 1], '终', '#ff6b6b')]
  const mover = mk(path[0], '', '#2bd8a2')
  amapMarks.push(mover)
  amap.add([amapLine, ...amapMarks])
  amap.setFitView([amapLine])
  clearInterval(moveTimer)
  let i = 0
  moveTimer = window.setInterval(() => {
    i = (i + 1) % path.length
    mover.setPosition(path[i])
    travelKm.value = Math.round(planned.mileage * i / (path.length - 1))
  }, 120)
}

async function tryAmap() {
  if (!amapKey) return
  const AMap = await new Promise<any>((resolve) => {
    const s = document.createElement('script')
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${amapKey}`
    const sec = import.meta.env.VITE_AMAP_SECURITY_CODE
    if (sec) (window as any)._AMapSecurityConfig = { securityJsCode: sec }
    s.onload = () => resolve((window as any).AMap || null)
    s.onerror = () => resolve(null)
    setTimeout(() => resolve(null), 8000)
    document.head.appendChild(s)
  })
  if (!AMap || !amapBox.value) { fallbackTip(); return }
  try {
    const map = new AMap.Map(amapBox.value, { zoom: 7, center: [106.63, 26.65], mapStyle: 'amap://styles/darkblue' })
    const ok = await new Promise<boolean>((resolve) => {
      map.on('complete', () => resolve(true))
      setTimeout(() => resolve(false), 6000)
    })
    if (!ok) { map.destroy(); fallbackTip(); return }
    amap = map
    mapMode.value = 'amap'
    mapTip.value = ''
    drawAmapRoute()
  } catch {
    fallbackTip()
  }
}

function fallbackTip() {
  mapMode.value = 'canvas'
  mapTip.value = amapKey
    ? '高德底图不可用（Key 失效或缺安全密钥 securityJsCode），已切换 Canvas 轨迹示意图'
    : '在 .env 配置 VITE_AMAP_JS_KEY 后自动切换高德底图'
  drawRoute()
}

function onMove(e: MouseEvent) { /* 预留：真实高德模式下的逆地理标注 */ }

function onPlan() {
  if (form.from === form.to) { ElMessage.warning('起点与终点不能相同'); return }
  planning.value = true
  setTimeout(() => {
    const from = cities.find((c) => c.name === form.from)!
    const to = cities.find((c) => c.name === form.to)!
    // F4: E = 载重t × 里程km × EF运输(0.062 kgCO2e/t·km) → t
    const mileage = Math.round(hav(from.lat, from.lng, to.lat, to.lng) * 1.32)
    planned.mileage = mileage
    planned.turnover = mileage * form.weight
    planned.emission = +(planned.turnover * 0.062 / 1000).toFixed(3)
    planned.points = Math.max(6, Math.round(mileage / 40))
    ElMessage.success('轨迹规划完成：' + from.name + ' → ' + to.name)
    if (mapMode.value === 'amap') drawAmapRoute()
    else drawRoute()
    planning.value = false
  }, 900)
}

function onSave() {
  ElMessage.success('运输记录已入库（enterprise 物流模块），里程与 F4 排放已同步范围三台账')
}

onMounted(() => { onPlan(); tryAmap() })
onBeforeUnmount(() => { cancelAnimationFrame(animRaf); clearInterval(moveTimer); amap?.destroy() })
</script>

<style scoped lang="scss">
.logi-grid { display: grid; grid-template-columns: 5fr 7fr; gap: 16px; align-items: start; }
.unit { margin-left: 10px; color: var(--qn-text-3); font-size: 12px; }
.result-block { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 18px; font-size: 13px; color: var(--qn-text-2);
  .rb-row b { margin-left: 8px; color: var(--qn-text); font-variant-numeric: tabular-nums; &.green { color: var(--qn-primary); } } }
.map-wrap { padding: 0; overflow: hidden; display: flex; flex-direction: column; }
.map-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; border-bottom: 1px solid var(--qn-border); }
.map-tip { font-size: 11.5px; color: var(--qn-text-3); }
.map-box { position: relative; height: 430px; background: linear-gradient(180deg, #0e1b2f 0%, #0a1424 100%); }
.amap-box { position: absolute; inset: 0; }
.track-canvas { position: absolute; inset: 0; width: 100%; height: 100%; }
.map-mark { position: absolute; z-index: 2; width: 24px; height: 24px; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center;
  &::after { content: ''; } }
.start-mark { left: 16%; top: 26%; background: #00b581; box-shadow: 0 4px 14px rgba(0,181,129,.5); }
.end-mark { left: 80%; top: 60%; background: #ff6b6b; box-shadow: 0 4px 14px rgba(255,107,107,.5); }
.map-hud { position: absolute; left: 16px; bottom: 14px; z-index: 2; padding: 10px 16px; border-radius: 10px; background: rgba(6,13,27,0.72); color: #fff; backdrop-filter: blur(8px); border: 1px solid rgba(0,209,255,0.25);
  .hud-title { font-size: 12px; opacity: 0.8; }
  .hud-miles { font-size: 20px; font-weight: 700; color: #2bd8a2; font-variant-numeric: tabular-nums; } }
@media (max-width: 1100px) { .logi-grid { grid-template-columns: 1fr; } }
</style>