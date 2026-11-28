<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">物流追踪</div>
      <div class="sub">快递式运单跟踪：揽收 → 运输 → 转运 → 派送 → 签收 五段节点实时流转，高德地图绘制在途轨迹（未配置 JS Key 自动切换 Canvas），里程按 F4 折算范围三排放</div>
    </div>

    <!-- 运单跟踪（快递式） -->
    <div class="page-card track-card">
      <div class="track-head">
        <div class="th-left">
          <div class="waybill">
            <span class="wb-label">运单号</span>
            <span class="wb-no">{{ waybill }}</span>
            <el-button link class="copy-btn" @click="copyWaybill"><el-icon><CopyDocument /></el-icon></el-button>
          </div>
          <div class="th-meta">
            <el-tag round size="small" effect="dark" class="plate-tag">{{ form.plate || '贵A·88888' }}</el-tag>
            <span class="route">{{ form.from }} → {{ form.to }}</span>
            <span class="sep">·</span>
            <span class="eta">预计送达 {{ etaText }}</span>
          </div>
        </div>
        <div class="th-status" :class="statusKey">
          <span class="dot"></span>{{ statusText }}
        </div>
      </div>

      <!-- 五段状态步 -->
      <div class="steps">
        <div v-for="(s, i) in steps" :key="s" class="step" :class="stepState(i)">
          <div class="step-node">
            <el-icon v-if="stepState(i) === 'done'"><Check /></el-icon>
            <span v-else class="step-idx">{{ i + 1 }}</span>
          </div>
          <div class="step-label">{{ s }}</div>
          <div v-if="i < steps.length - 1" class="step-line" :class="{ on: stepState(i) === 'done' }"></div>
        </div>
      </div>

      <div class="track-body">
        <!-- 节点时间线 -->
        <div class="timeline-wrap">
          <div class="tl-title">物流节点</div>
          <div class="timeline">
            <div v-for="(n, i) in timelineNodes" :key="i" class="tl-item" :class="n.state">
              <div class="tl-rail">
                <span class="tl-dot"><el-icon v-if="n.state === 'done'"><Check /></el-icon></span>
                <span v-if="i < timelineNodes.length - 1" class="tl-line"></span>
              </div>
              <div class="tl-content">
                <div class="tl-head">
                  <span class="tl-city">{{ n.city }}</span>
                  <span class="tl-time">{{ n.timeText }}</span>
                </div>
                <div class="tl-desc">{{ n.desc }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 地图 -->
        <div class="map-wrap">
          <div class="map-head">
            <el-tag round size="small" :type="mapMode === 'amap' ? 'success' : 'info'">
              {{ mapMode === 'amap' ? '高德地图 · 真实底图' : '轨迹示意图 · Canvas' }}
            </el-tag>
            <span class="map-tip">{{ mapTip }}</span>
          </div>
          <div ref="mapBox" class="map-box">
            <div ref="amapBox" v-show="mapMode === 'amap'" class="amap-box"></div>
            <canvas ref="trackCanvas" v-show="mapMode === 'canvas'" class="track-canvas"></canvas>
            <div v-show="mapMode === 'canvas'" class="map-mark start-mark">起</div>
            <div v-show="mapMode === 'canvas'" class="map-mark end-mark">终</div>
            <div class="map-hud">
              <div class="hud-title">在途里程 · {{ form.plate || '贵A·88888' }}</div>
              <div class="hud-miles">{{ Math.round(travelKm) }} / {{ planned.mileage }} km</div>
              <div class="hud-bar"><span :style="{ width: (progress * 100) + '%' }"></span></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="logi-grid">
      <!-- 登记 -->
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
            <el-button class="ghost-btn" :loading="saving" @click="onSave">存为运输记录</el-button>
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

      <!-- 历史 -->
      <div class="page-card">
        <div class="page-card-title">
          <h3>近期运输轨迹</h3>
          <el-radio-group v-model="histFilter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="在途">在途</el-radio-button>
            <el-radio-button value="已完成">已完成</el-radio-button>
          </el-radio-group>
        </div>
        <div class="hist-scroll">
          <el-table :data="filteredTracks" size="small" highlight-current-row @row-click="loadTrack">
            <el-table-column prop="plate" label="车牌" width="88" />
            <el-table-column prop="route" label="路线" min-width="100" />
            <el-table-column prop="mileage" label="里程(km)" width="68" align="right" />
            <el-table-column prop="emission" label="排放(tCO₂e)" width="86" align="right">
              <template #default="{ row }"><span class="em-cell">{{ row.emission }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="68">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === '已完成' ? 'success' : 'warning'" round>{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="完成时间" width="102" />
          </el-table>
        </div>
        <div class="hist-tip">点击任意运单可回看其节点跟踪</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, CopyDocument } from '@element-plus/icons-vue'
import request from '@/utils/request'

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
const saving = ref(false)
const planned = reactive({ mileage: 0, turnover: 0, emission: 0, points: 0 })

interface Track {
  id?: number
  tripNo?: string
  plate: string
  route: string
  from: string
  to: string
  mileage: number
  emission: number
  time: string
  status: '在途' | '已完成'
  startLng?: number
  startLat?: number
  endLng?: number
  endLat?: number
  points?: [number, number][]
}

const demoTracks: Track[] = [
  { plate: '贵A·88888', route: '六盘水市 → 贵阳市', from: '六盘水市', to: '贵阳市', mileage: 251, emission: 0.498, time: '2026-10-29 14:20', status: '已完成' },
  { plate: '贵B·61237', route: '毕节市 → 遵义市', from: '毕节市', to: '遵义市', mileage: 186, emission: 0.369, time: '2026-10-27 09:05', status: '已完成' },
  { plate: '贵C·33012', route: '贵阳市 → 黔南州', from: '贵阳市', to: '黔南州', mileage: 133, emission: 0.264, time: '2026-10-25 16:48', status: '在途' },
  { plate: '贵A·52609', route: '贵阳市 → 遵义市', from: '贵阳市', to: '遵义市', mileage: 147, emission: 0.292, time: '2026-10-24 11:32', status: '已完成' },
  { plate: '贵B·70415', route: '六盘水市 → 黔西南州', from: '六盘水市', to: '黔西南州', mileage: 212, emission: 0.421, time: '2026-10-22 08:15', status: '已完成' },
  { plate: '贵C·18873', route: '安顺市 → 贵阳市', from: '安顺市', to: '贵阳市', mileage: 98, emission: 0.194, time: '2026-10-20 15:40', status: '已完成' },
  { plate: '贵A·90226', route: '贵阳市 → 黔东南州', from: '贵阳市', to: '黔东南州', mileage: 168, emission: 0.333, time: '2026-10-18 10:07', status: '已完成' },
  { plate: '贵B·45108', route: '毕节市 → 六盘水市', from: '毕节市', to: '六盘水市', mileage: 203, emission: 0.403, time: '2026-10-16 13:52', status: '已完成' },
  { plate: '贵C·62794', route: '遵义市 → 铜仁市', from: '遵义市', to: '铜仁市', mileage: 295, emission: 0.585, time: '2026-10-14 09:26', status: '已完成' }
]
const tracks = ref<Track[]>(demoTracks)
const selectedRoute = ref<[number, number][] | null>(null)
const histFilter = ref<'all' | '在途' | '已完成'>('all')
const filteredTracks = computed(() => histFilter.value === 'all' ? tracks.value : tracks.value.filter(t => t.status === histFilter.value))

function apiToTrack(t: any): Track {
  const mileage = Number(t.km ?? 0)
  const rawPoints = Array.isArray(t.route?.points) ? t.route.points : Array.isArray(t.route) ? t.route : []
  const points: [number, number][] = rawPoints
    .map((p: any) => Array.isArray(p) ? [Number(p[0]), Number(p[1])] : [Number(p?.lng), Number(p?.lat)])
    .filter((p: [number, number]) => Number.isFinite(p[0]) && Number.isFinite(p[1])) as [number, number][]
  return {
    id: t.id,
    tripNo: t.tripNo,
    plate: t.plate || '—',
    route: `${t.from || '—'} → ${t.to || '—'}`,
    from: t.from || '',
    to: t.to || '',
    mileage,
    emission: +((mileage * 32 * 0.062) / 1000).toFixed(3),
    time: String(t.arriveTime || t.startTime || '').slice(0, 16).replace('T', ' '),
    status: t.statusCode === 'ARRIVED' ? '已完成' : '在途',
    startLng: Number(t.startLng ?? points[0]?.[0]),
    startLat: Number(t.startLat ?? points[0]?.[1]),
    endLng: Number(t.endLng ?? points[points.length - 1]?.[0]),
    endLat: Number(t.endLat ?? points[points.length - 1]?.[1]),
    points
  }
}
async function loadTrips(selectFirst = false): Promise<boolean> {
  try {
    const r: any = await request.get('/v1/enterprise/logistics/trips')
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) {
      tracks.value = r.data.map(apiToTrack)
      if (selectFirst) loadTrack(tracks.value[0], true)
      return true
    }
  } catch { /* 接口不可用时保留演示数据 */ }
  return false
}

// ---------- 运单 / 进度 ----------
const steps = ['已揽收', '运输中', '到达转运', '派送中', '已签收']
const thresholds = [0, 0.12, 0.45, 0.8, 1]
const waybill = ref('QN-L-20261127-001')
const progress = ref(0)
const depart = ref(Date.now())
let progTimer = 0
let seq = 1

const travelKm = computed(() => planned.mileage * progress.value)
const delivered = computed(() => progress.value >= 1)
const currentStage = computed(() => {
  if (delivered.value) return steps.length - 1
  let idx = 0
  for (let i = 0; i < thresholds.length; i++) if (progress.value >= thresholds[i]) idx = i
  return idx
})
const statusKey = computed(() => delivered.value ? 'ok' : 'run')
const statusText = computed(() => delivered.value ? '已签收' : steps[currentStage.value])
const etaText = computed(() => fmt(depart.value + spanMs.value))
// 节点时间按真实运输时长（里程/70km/h）展开，动画进度仍按秒级演示推进
const spanMs = computed(() => Math.max(2, planned.mileage / 70) * 3600000)

function fmt(ts: number): string {
  const d = new Date(ts)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
function midCity(): string {
  const f = cities.find(c => c.name === form.from)
  const t = cities.find(c => c.name === form.to)
  if (!f || !t) return '转运中心'
  const mx = (f.lng + t.lng) / 2, my = (f.lat + t.lat) / 2
  let best = '', bd = Infinity
  for (const c of cities) {
    if (c.name === form.from || c.name === form.to) continue
    const d = (c.lng - mx) ** 2 + (c.lat - my) ** 2
    if (d < bd) { bd = d; best = c.name }
  }
  return best || '安顺市'
}
function stepState(i: number): 'done' | 'active' | 'wait' {
  if (delivered.value) return 'done'
  if (i < currentStage.value) return 'done'
  if (i === currentStage.value) return 'active'
  return 'wait'
}
const timelineNodes = computed(() => {
  const mid = midCity()
  const defs = [
    { city: form.from, desc: '货物已装车，运单已生成，等待发车' },
    { city: form.from, desc: '车辆已发出，正在前往转运中心' },
    { city: mid, desc: '到达转运中心，分拣换装中' },
    { city: form.to, desc: '货物到达目的城市，派送中' },
    { city: form.to, desc: '收货方已签收，运单完成' }
  ]
  // 最新节点在上
  return defs.map((d, i) => {
    const reached = progress.value >= thresholds[i]
    const state = delivered.value ? 'done' : (i === currentStage.value ? 'active' : (reached ? 'done' : 'wait'))
    const ts = depart.value + thresholds[i] * spanMs.value
    return { ...d, state, timeText: (reached ? '' : '预计 ') + fmt(ts) }
  }).reverse()
})

function startProgress(ms: number) {
  clearInterval(progTimer)
  depart.value = Date.now()
  progress.value = 0
  const t0 = Date.now()
  progTimer = window.setInterval(() => {
    progress.value = Math.min(1, (Date.now() - t0) / ms)
    depart.value = Date.now() - progress.value * spanMs.value
    if (progress.value >= 1) clearInterval(progTimer)
    syncMover()
    if (mapMode.value === 'canvas') drawRoute()
  }, 120)
}
function copyWaybill() {
  const text = waybill.value
  const done = () => ElMessage.success('运单号已复制：' + text)
  const fallback = () => {
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.focus()
    ta.select()
    try { document.execCommand('copy') ? done() : ElMessage.warning('复制失败，请手动记录运单号') }
    catch { ElMessage.warning('复制失败，请手动记录运单号') }
    finally { ta.remove() }
  }
  if (navigator.clipboard?.writeText) navigator.clipboard.writeText(text).then(done).catch(fallback)
  else fallback()
}
function loadTrack(row: Track, silent = false) {
  form.plate = row.plate; form.from = row.from; form.to = row.to
  planned.mileage = row.mileage
  planned.turnover = row.mileage * form.weight
  planned.emission = row.emission
  planned.points = Math.max(6, Math.round(row.mileage / 40))
  selectedRoute.value = row.points && row.points.length >= 2 ? row.points : null
  waybill.value = row.tripNo || ('QN-L-' + row.time.slice(0, 10).replace(/-/g, '') + '-' + String(tracks.value.indexOf(row) + 1).padStart(3, '0'))
  clearInterval(progTimer)
  if (row.status === '已完成') { progress.value = 1; depart.value = Date.now() - spanMs.value }
  else { progress.value = 0.55; depart.value = Date.now() - 0.55 * spanMs.value }
  redraw()
  if (!silent) ElMessage.success('已加载运单 ' + waybill.value)
}

// ---------- Canvas ----------
const mapBox = ref<HTMLElement>()
const trackCanvas = ref<HTMLCanvasElement>()
let animRaf = 0
let guizhouGeo: any = null

const GEO_BOUNDS = { minLng: 103.6, maxLng: 109.8, minLat: 24.4, maxLat: 29.3 }
const LABEL_OFFSET: Record<string, [number, number]> = {
  贵阳市: [9, -8], 遵义市: [9, -8], 六盘水市: [-60, -8], 安顺市: [-8, 16],
  毕节市: [-8, -12], 铜仁市: [9, -6], 黔西南州: [-68, 12], 黔东南州: [9, 12], 黔南州: [-12, 18]
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

function hav(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371, toRad = Math.PI / 180
  const dLat = (lat2 - lat1) * toRad, dLng = (lng2 - lng1) * toRad
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(a))
}
function bez(p1: any, p2: any, c1: any, c2: any, t: number) {
  const u = 1 - t
  return {
    x: u * u * u * p1.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * p2.x,
    y: u * u * u * p1.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * p2.y
  }
}

async function loadGeo() {
  try {
    const r = await fetch('/map/guizhou.json')
    guizhouGeo = r.ok ? await r.json() : null
    if (mapMode.value !== 'amap') redraw()
  } catch { /* 底图轮廓不可用时仅画城市点位 */ }
}

function drawRoute() {
  const canvas = trackCanvas.value, box = mapBox.value
  if (!canvas || !box) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvas.width = box.clientWidth * dpr; canvas.height = box.clientHeight * dpr
  const ctx = canvas.getContext('2d')!
  ctx.scale(dpr, dpr)
  const w = box.clientWidth, h = box.clientHeight
  const p1 = { x: w * 0.18, y: h * 0.3 }, p2 = { x: w * 0.82, y: h * 0.62 }
  const dist = Math.hypot(p2.x - p1.x, p2.y - p1.y)
  const c1 = { x: p1.x + dist * 0.25, y: p1.y - dist * 0.22 }, c2 = { x: p2.x - dist * 0.25, y: p2.y + dist * 0.2 }
  const px = (lng: number) => w * 0.04 + ((lng - GEO_BOUNDS.minLng) / (GEO_BOUNDS.maxLng - GEO_BOUNDS.minLng)) * w * 0.92
  const py = (lat: number) => h * 0.08 + ((GEO_BOUNDS.maxLat - lat) / (GEO_BOUNDS.maxLat - GEO_BOUNDS.minLat)) * h * 0.84

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    ctx.strokeStyle = 'rgba(140,170,210,0.12)'; ctx.lineWidth = 1
    for (let x = 0; x < w; x += 34) { ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, h); ctx.stroke() }
    for (let y = 0; y < h; y += 34) { ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(w, y); ctx.stroke() }

    // 贵州地市轮廓：让示意图具备真实地理形状
    if (guizhouGeo?.features) {
      ctx.save()
      ctx.lineJoin = 'round'
      for (const f of guizhouGeo.features) {
        const g = f.geometry
        if (!g) continue
        const polys = g.type === 'MultiPolygon' ? g.coordinates : [g.coordinates]
        for (const poly of polys) {
          for (const ring of poly) {
            if (!ring?.length) continue
            ctx.beginPath()
            ring.forEach((pt: number[], i: number) => {
              const x = px(pt[0]), y = py(pt[1])
              if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y)
            })
            ctx.closePath()
            ctx.fillStyle = 'rgba(0,181,129,0.10)'
            ctx.fill()
            ctx.strokeStyle = 'rgba(120,220,200,0.55)'
            ctx.lineWidth = 1.2
            ctx.stroke()
          }
        }
      }
      ctx.restore()
    } else {
      ctx.fillStyle = 'rgba(0,181,129,0.05)'; ctx.fillRect(0, 0, w * 0.5, h)
    }

    // 剩余路段（虚线浅灰）
    ctx.setLineDash([7, 6]); ctx.strokeStyle = 'rgba(147,161,181,0.5)'; ctx.lineWidth = 2
    ctx.beginPath(); ctx.moveTo(p1.x, p1.y); ctx.bezierCurveTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y); ctx.stroke()
    ctx.setLineDash([])
    // 已行驶路段（实线翡翠绿）
    const t = progress.value
    ctx.strokeStyle = '#00b581'; ctx.lineWidth = 3.5; ctx.beginPath(); ctx.moveTo(p1.x, p1.y)
    for (let i = 1; i <= 40; i++) { const pt = bez(p1, p2, c1, c2, t * i / 40); ctx.lineTo(pt.x, pt.y) }
    ctx.stroke()

    // 城市点位与高对比标签
    for (const c of cities) {
      const cx = px(c.lng), cy = py(c.lat)
      ctx.shadowColor = 'rgba(0,230,180,0.85)'
      ctx.shadowBlur = 8
      ctx.fillStyle = '#7ff0cf'
      ctx.beginPath(); ctx.arc(cx, cy, 3.4, 0, Math.PI * 2); ctx.fill()
      ctx.shadowBlur = 0
      const [dx, dy] = LABEL_OFFSET[c.name] ?? [8, -8]
      const lx = cx + dx, ly = cy + dy
      ctx.font = '600 11px "PingFang SC", "Microsoft YaHei", sans-serif'
      const tw = ctx.measureText(c.name).width
      ctx.fillStyle = 'rgba(4,14,26,0.62)'
      roundRect(ctx, lx - 4, ly - 10, tw + 8, 14, 5)
      ctx.fill()
      ctx.fillStyle = 'rgba(226,246,255,0.94)'
      ctx.fillText(c.name, lx, ly)
    }

    // 在途车辆光点
    const m = bez(p1, p2, c1, c2, t)
    const glow = ctx.createRadialGradient(m.x, m.y, 0, m.x, m.y, 18)
    glow.addColorStop(0, 'rgba(0,233,180,0.9)'); glow.addColorStop(1, 'rgba(0,233,180,0)')
    ctx.fillStyle = glow; ctx.beginPath(); ctx.arc(m.x, m.y, 18, 0, Math.PI * 2); ctx.fill()
    ctx.fillStyle = '#fff'; ctx.beginPath(); ctx.arc(m.x, m.y, 4.5, 0, Math.PI * 2); ctx.fill()
  }
  cancelAnimationFrame(animRaf)
  draw()
}

// ---------- 高德 ----------
const amapBox = ref<HTMLElement>()
let amap: any = null, amapLine: any = null, amapDone: any = null, amapMarks: any[] = [], mover: any = null
function routeLngLat(): [number, number][] {
  const from = cities.find(c => c.name === form.from) || cities[1]
  const to = cities.find(c => c.name === form.to) || cities[0]
  const pts: [number, number][] = []
  const c1 = [from.lng + (to.lng - from.lng) * 0.25, from.lat + 0.5]
  const c2 = [to.lng - (to.lng - from.lng) * 0.25, to.lat - 0.5]
  for (let i = 0; i <= 30; i++) {
    const t = i / 30, u = 1 - t
    pts.push([u * u * u * from.lng + 3 * u * u * t * c1[0] + 3 * u * t * t * c2[0] + t * t * t * to.lng,
      u * u * u * from.lat + 3 * u * u * t * c1[1] + 3 * u * t * t * c2[1] + t * t * t * to.lat])
  }
  return pts
}
function drawAmapRoute() {
  if (!amap) return
  const AMap = (window as any).AMap
  const path = routeLngLat()
  amapLine?.setMap(null); amapDone?.setMap(null); amapMarks.forEach(m => m.setMap(null))
  amapLine = new AMap.Polyline({ path, strokeColor: '#93a1b5', strokeWeight: 3, strokeStyle: 'dashed', strokeOpacity: 0.7 })
  amapDone = new AMap.Polyline({ path: path.slice(0, 2), strokeColor: '#00b581', strokeWeight: 4, strokeOpacity: 0.95, showDir: true })
  const mk = (pos: [number, number], text: string, color: string) => new AMap.Marker({
    position: pos,
    content: `<div style="width:24px;height:24px;border-radius:50% 50% 50% 0;transform:rotate(-45deg);background:${color};color:#fff;font-size:12px;display:flex;align-items:center;justify-content:center;"><span style="transform:rotate(45deg)">${text}</span></div>`,
    offset: new AMap.Pixel(-12, -24)
  })
  amapMarks = [mk(path[0], '起', '#00b581'), mk(path[path.length - 1], '终', '#ff6b6b')]
  mover = mk(path[0], '', '#2bd8a2')
  amapMarks.push(mover)
  amap.add([amapLine, amapDone, ...amapMarks])
  amap.setFitView([amapLine])
  syncMover()
}
function syncMover() {
  if (mapMode.value !== 'amap' || !amap || !mover) return
  const path = routeLngLat()
  const idx = Math.min(path.length - 1, Math.round(progress.value * (path.length - 1)))
  mover.setPosition(path[idx])
  amapDone.setPath(path.slice(0, idx + 1))
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
    const ok = await new Promise<boolean>((resolve) => { map.on('complete', () => resolve(true)); setTimeout(() => resolve(false), 6000) })
    if (!ok) { map.destroy(); fallbackTip(); return }
    amap = map; mapMode.value = 'amap'; mapTip.value = ''
    drawAmapRoute()
  } catch { fallbackTip() }
}
function fallbackTip() {
  mapMode.value = 'canvas'
  mapTip.value = amapKey ? '高德底图不可用，已切换 Canvas 轨迹示意图' : '在 .env 配置 VITE_AMAP_JS_KEY 后自动切换高德底图'
  drawRoute()
}
function redraw() { if (mapMode.value === 'amap') drawAmapRoute(); else drawRoute() }

function routeEndpoints(): { a: [number, number]; b: [number, number] } | null {
  const from = cities.find(c => c.name === form.from)
  const to = cities.find(c => c.name === form.to)
  if (from && to) return { a: [from.lng, from.lat], b: [to.lng, to.lat] }
  if (from || to) return null
  const r = selectedRoute.value
  if (r && r.length >= 2) return { a: r[0], b: r[r.length - 1] }
  return null
}

function onPlan() {
  if (form.from === form.to) { ElMessage.warning('起点与终点不能相同'); return }
  const ends = routeEndpoints()
  if (!ends) { ElMessage.warning('请选择起点与终点城市后再规划'); return }
  planning.value = true
  setTimeout(() => {
    try {
      const mileage = Math.round(hav(ends.a[1], ends.a[0], ends.b[1], ends.b[0]) * 1.32)
      planned.mileage = mileage
      planned.turnover = mileage * form.weight
      planned.emission = +(planned.turnover * 0.062 / 1000).toFixed(3)
      planned.points = Math.max(6, Math.round(mileage / 40))
      seq++
      waybill.value = 'QN-L-' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '-' + String(seq).padStart(3, '0')
      startProgress(Math.min(40000, Math.max(12000, mileage * 80)))
      ElMessage.success('轨迹规划完成：' + form.from + ' → ' + form.to)
      redraw()
    } finally {
      planning.value = false
    }
  }, 600)
}
async function onSave() {
  if (!planned.mileage) { ElMessage.warning('请先规划轨迹'); return }
  const isCityRoute = cities.some(c => c.name === form.from) && cities.some(c => c.name === form.to)
  const route = isCityRoute ? routeLngLat() : (selectedRoute.value && selectedRoute.value.length >= 2 ? selectedRoute.value : routeLngLat())
  saving.value = true
  try {
    const r: any = await request.post('/v1/enterprise/logistics/trips', {
      plateNo: form.plate,
      transportMode: '公路',
      startName: form.from,
      endName: form.to,
      mileageKm: planned.mileage,
      route
    })
    if (r?.code === 0) {
      if (r.data?.tripNo) waybill.value = r.data.tripNo
      ElMessage.success('运输记录已入库 ' + (r.data?.tripNo || '') + '，里程与 F4 排放已同步范围三台账')
      await loadTrips(true)
    } else {
      ElMessage.error(r?.message || '运输记录入库失败')
    }
  } catch { ElMessage.error('运输记录入库失败，请稍后重试') }
  finally { saving.value = false }
}

const onResize = () => { if (mapMode.value === 'amap') amap?.resize(); else redraw() }

onMounted(async () => {
  tryAmap()
  void loadGeo()
  window.addEventListener('resize', onResize)
  const fromApi = await loadTrips()
  if (fromApi) loadTrack(tracks.value[0], true)
  else onPlan()
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  cancelAnimationFrame(animRaf)
  clearInterval(progTimer)
  amap?.destroy()
})
</script>

<style scoped lang="scss">
.track-card { padding: 0; overflow: hidden; }
.track-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 18px 22px; border-bottom: 1px solid var(--qn-border); background: linear-gradient(120deg, #f4fbf8 0%, #f2f8fc 100%); }
.th-left { display: flex; flex-direction: column; gap: 8px; }
.waybill { display: flex; align-items: center; gap: 8px; }
.wb-label { font-size: 12px; color: #64748b; }
.wb-no { font-size: 18px; font-weight: 700; color: var(--qn-text); font-variant-numeric: tabular-nums; letter-spacing: .5px; }
.copy-btn { color: #00795c; padding: 4px; }
.th-meta { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--qn-text-2); flex-wrap: wrap; }
.plate-tag { background: var(--qn-ink); border-color: var(--qn-ink); color: #fff; }
.route { font-weight: 600; color: var(--qn-text); }
.sep { color: #64748b; }
.eta { color: var(--qn-text-2); }
.th-status { display: flex; align-items: center; gap: 8px; padding: 8px 16px; border-radius: 999px; font-size: 13px; font-weight: 600; white-space: nowrap;
  .dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }
  &.run { background: #e6f7ff; color: #00659a; .dot { animation: pulse 1.4s ease infinite; } }
  &.ok { background: #e6f9f2; color: #00795c; } }
@keyframes pulse { 0%,100% { box-shadow: 0 0 0 0 rgba(0,119,182,.4); } 50% { box-shadow: 0 0 0 6px rgba(0,119,182,0); } }

.steps { display: flex; padding: 20px 22px 6px; }
.step { position: relative; flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.step-node { width: 30px; height: 30px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; z-index: 1;
  background: #eef2f7; color: #64748b; border: 2px solid #d8e0ea; }
.step-label { font-size: 12.5px; color: #64748b; }
.step-line { position: absolute; top: 15px; left: calc(50% + 19px); width: calc(100% - 38px); height: 2px; background: #d8e0ea; &.on { background: var(--qn-primary); } }
.step.done .step-node { background: var(--qn-primary); border-color: var(--qn-primary); color: #fff; }
.step.done .step-label { color: #00795c; font-weight: 600; }
.step.active .step-node { background: #fff; border-color: var(--qn-primary); color: #00795c; animation: pulse-g 1.6s ease infinite; }
.step.active .step-label { color: var(--qn-text); font-weight: 700; }
@keyframes pulse-g { 0%,100% { box-shadow: 0 0 0 0 rgba(0,181,129,.35); } 50% { box-shadow: 0 0 0 8px rgba(0,181,129,0); } }

.track-body { display: grid; grid-template-columns: minmax(0, 5fr) minmax(0, 7fr); gap: 0; border-top: 1px solid var(--qn-border); }
.timeline-wrap, .map-wrap { min-width: 0; }
.timeline-wrap { padding: 18px 22px; border-right: 1px solid var(--qn-border); }
.tl-title { font-size: 13px; font-weight: 600; color: var(--qn-text-2); margin-bottom: 12px; }
.timeline { max-height: 380px; overflow-y: auto; padding-right: 6px;
  &::-webkit-scrollbar { width: 6px; } &::-webkit-scrollbar-thumb { background: #b3ebd9; border-radius: 3px; } &::-webkit-scrollbar-thumb:hover { background: var(--qn-primary-light); } &::-webkit-scrollbar-track { background: #f2f5f9; } }
.tl-item { display: flex; gap: 12px; }
.tl-rail { display: flex; flex-direction: column; align-items: center; }
.tl-dot { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; background: #eef2f7; color: #64748b; border: 2px solid #d8e0ea; flex: none; }
.tl-line { width: 2px; flex: 1; background: #e6ebf2; margin: 4px 0; }
.tl-content { padding-bottom: 18px; }
.tl-head { display: flex; align-items: baseline; gap: 10px; }
.tl-city { font-size: 13.5px; font-weight: 600; color: #64748b; }
.tl-time { font-size: 11.5px; color: #64748b; font-variant-numeric: tabular-nums; }
.tl-desc { font-size: 12.5px; color: #64748b; margin-top: 3px; line-height: 1.5; }
.tl-item.done .tl-dot { background: var(--qn-primary); border-color: var(--qn-primary); color: #fff; }
.tl-item.done .tl-city { color: var(--qn-text-2); }
.tl-item.done .tl-line { background: #b3ebd9; }
.tl-item.active .tl-dot { background: #fff; border-color: var(--qn-primary); color: var(--qn-primary); animation: pulse-g 1.6s ease infinite; }
.tl-item.active .tl-city { color: #00795c; font-weight: 700; }
.tl-item.active .tl-desc { color: var(--qn-text-2); }
.tl-item.active .tl-time { color: #00795c; }

.map-wrap { display: flex; flex-direction: column; }
.map-head { display: flex; align-items: center; justify-content: space-between; padding: 12px 18px; border-bottom: 1px solid var(--qn-border); }
.map-tip { font-size: 11.5px; color: #64748b; }
.map-box { position: relative; height: 400px; background: linear-gradient(180deg, #0e1b2f 0%, #0a1424 100%); }
.amap-box { position: absolute; inset: 0; }
.track-canvas { position: absolute; inset: 0; width: 100%; height: 100%; }
.map-mark { position: absolute; z-index: 2; width: 24px; height: 24px; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); color: #fff; font-size: 12px; display: flex; align-items: center; justify-content: center; }
.start-mark { left: 16%; top: 28%; background: #00b581; box-shadow: 0 4px 14px rgba(0,181,129,.5); }
.end-mark { left: 80%; top: 60%; background: #ff6b6b; box-shadow: 0 4px 14px rgba(255,107,107,.5); }
.map-hud { position: absolute; left: 16px; bottom: 14px; z-index: 2; padding: 10px 16px; border-radius: 10px; background: rgba(6,13,27,0.78); color: #fff; backdrop-filter: blur(8px); border: 1px solid rgba(0,209,255,0.25); min-width: 180px;
  .hud-title { font-size: 12px; opacity: 0.85; }
  .hud-miles { font-size: 20px; font-weight: 700; color: #2bd8a2; font-variant-numeric: tabular-nums; }
  .hud-bar { margin-top: 6px; height: 4px; border-radius: 2px; background: rgba(255,255,255,0.18); overflow: hidden; span { display: block; height: 100%; background: linear-gradient(90deg, #00b581, #00d1ff); transition: width .2s linear; } } }

.logi-grid { display: grid; grid-template-columns: minmax(0, 5fr) minmax(0, 7fr); gap: 16px; align-items: start; }
.logi-grid > .page-card { min-width: 0; }
.unit { margin-left: 10px; color: var(--qn-text-2); font-size: 12px; }
.ghost-btn { background: #fff; border: 1px solid var(--qn-primary); color: #00795c; font-weight: 600; &:hover { background: #e6f9f2; color: #00795c; border-color: var(--qn-primary); } }
.result-block { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 12px 18px; font-size: 13px; color: var(--qn-text-2);
  .rb-row b { margin-left: 8px; color: var(--qn-text); font-variant-numeric: tabular-nums; &.green { color: #00795c; } } }
.hist-scroll { overflow-x: auto; &::-webkit-scrollbar { height: 6px; } &::-webkit-scrollbar-thumb { background: #b3ebd9; border-radius: 3px; } &::-webkit-scrollbar-track { background: #f2f5f9; } }
.em-cell { color: #00795c; font-weight: 600; font-variant-numeric: tabular-nums; }
.hist-tip { margin-top: 10px; font-size: 11.5px; color: #64748b; }
.logi-grid .el-button--primary { background: #008560; border-color: #008560;
  &:hover, &:focus { background: #00795c; border-color: #00795c; } }
:deep(.el-radio-button) { --el-radio-button-checked-bg-color: #008560; --el-radio-button-checked-border-color: #008560; --el-radio-button-checked-text-color: #fff; }
:deep(.el-tag--success) { background: #e6f9f2; color: #00795c; border-color: #b3ebd9; }
:deep(.el-tag--warning) { background: #fff7e6; color: #9a5b00; border-color: #ffe1a6; }
:deep(.el-tag--info) { background: #eef2f7; color: #5c6b82; border-color: #d8e0ea; }
@media (max-width: 1100px) { .logi-grid, .track-body { grid-template-columns: minmax(0, 1fr); } .timeline-wrap { border-right: none; border-bottom: 1px solid var(--qn-border); } }
</style>