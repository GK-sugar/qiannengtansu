<template>
  <div class="ci-page">
    <div class="ci-aurora a"></div>
    <div class="ci-aurora b"></div>

    <div class="ci-shell">
      <!-- 顶部任务卡 -->
      <div class="ci-head">
        <img src="@/assets/logo/logo-mark.svg" class="ci-logo" alt="黔能碳溯" />
        <div class="ci-head-txt">
          <div class="ci-head-title">移动执法 · H5 打卡</div>
          <div class="ci-head-sub">QNCT · MOBILE ENFORCEMENT CHECK-IN</div>
        </div>
      </div>

      <div class="ci-task">
        <div class="ct-row"><label>核查任务</label><b class="mono">{{ task.taskNo }}</b></div>
        <div class="ct-row"><label>被核查企业</label><b>{{ task.company }}</b></div>
        <div class="ct-row"><label>核查点坐标</label><b class="mono">{{ task.lat.toFixed(5) }}, {{ task.lng.toFixed(5) }}</b></div>
        <div class="ct-row"><label>有效半径</label><b>≤ {{ task.radius }} m（现场核验）</b></div>
        <div class="ct-row"><label>核查时段</label><b>{{ task.time }}</b></div>
      </div>

      <!-- 步骤一：GPS 定位 -->
      <div class="ci-card">
        <div class="ci-step"><i>1</i>现场定位（浏览器 Geolocation）</div>
        <div class="loc-row">
          <div class="loc-info">
            <div class="li-coords mono">{{ loc.lat.toFixed(6) }}, {{ loc.lng.toFixed(6) }}</div>
            <div class="li-state" :class="locState">
              <i class="pulse-dot"></i>{{ locStateText }}
            </div>
          </div>
          <el-button type="primary" round :loading="locating" @click="locate">重新定位</el-button>
        </div>
        <div class="sim-row">
          <span class="sim-label">模拟定位（无 GPS 环境）：</span>
          <el-button size="small" round plain @click="sim(0)">站在核查点</el-button>
          <el-button size="small" round plain @click="sim(300)">偏移 300m</el-button>
          <el-button size="small" round plain @click="sim(1200)">偏移 1.2km</el-button>
        </div>
      </div>

      <!-- 步骤二：判距雷达 -->
      <div class="ci-card">
        <div class="ci-step"><i>2</i>离场判定（F9 · Haversine 球面距离）</div>
        <div class="radar-box">
          <svg viewBox="0 0 280 212" class="radar">
            <circle cx="140" cy="106" :r="task.radius * rpx" fill="rgba(0,181,129,0.10)" stroke="rgba(0,230,162,0.4)" stroke-width="1.2" stroke-dasharray="5 4" />
            <circle cx="140" cy="106" :r="task.radius * 2 * rpx" fill="none" stroke="rgba(0,209,255,0.18)" stroke-width="1" />
            <circle cx="140" cy="106" :r="task.radius * 3 * rpx" fill="none" stroke="rgba(0,209,255,0.10)" stroke-width="1" />
            <line x1="140" y1="106" :x2="ux" :y2="uy" stroke="rgba(255,255,255,0.32)" stroke-width="1.4" stroke-dasharray="4 4" />
            <circle cx="140" cy="106" r="7" fill="#00d1ff">
              <animate attributeName="r" values="6;10;6" dur="2s" repeatCount="indefinite" />
            </circle>
            <text x="140" y="34" text-anchor="middle" fill="#8fb3d4" font-size="10">核查点</text>
            <circle :cx="ux" :cy="uy" r="8" :fill="inRange ? '#00e6a2' : '#ff4d3c'">
              <animate attributeName="r" values="6;11;6" dur="1.6s" repeatCount="indefinite" />
            </circle>
            <text :x="ux" :y="uy - 16" text-anchor="middle" :fill="inRange ? '#63e6b4' : '#ff8d80'" font-size="10">执法员</text>
          </svg>
          <div class="radar-meta">
            <div class="rm-dist">
              <span>实际距离</span>
              <b :class="inRange ? 'ok' : 'bad'">{{ dist.toFixed(1) }} m</b>
            </div>
            <div class="rm-formula mono">d = 2R·arcsin(√(sin²(Δφ/2) + cosφ₁·cosφ₂·sin²(Δλ/2)))</div>
            <div class="rm-remark">R = 6 371 km ｜ φ=纬度 λ=经度</div>
          </div>
        </div>
        <div class="verdict" :class="verdictClass">
          <div class="vd-icon">{{ inRange ? '✓' : '✕' }}</div>
          <div>
            <div class="vd-title">{{ inRange ? '在场有效 · 可正常打卡' : '超出半径 · 标记为离场' }}</div>
            <div class="vd-sub" v-if="inRange">距离核查点 {{ dist.toFixed(1) }} m，位于 {{ task.radius }} m 有效半径内</div>
            <div class="vd-sub" v-else>距离核查点 {{ dist.toFixed(1) }} m，超出 {{ task.radius }} m 有效半径，将记录“离场”异常状态</div>
          </div>
        </div>
      </div>

      <!-- 步骤三：现场拍照 -->
      <div class="ci-card">
        <div class="ci-step"><i>3</i>现场照片（调用相机 / 相册）</div>
        <div class="cam-zone">
          <label class="cam-btn" :class="{ has: !!photo }">
            <input type="file" accept="image/*" capture="environment" class="cam-input" @change="onPhoto" />
            <template v-if="photo">
              <img :src="photo" class="cam-preview" alt="现场照片" />
              <span class="cam-overlay">点击更换照片</span>
            </template>
            <template v-else>
              <div class="cam-plus">+</div>
              <div class="cam-tip">拍摄核查点现场实况照片</div>
              <div class="cam-tip2">水印：企业 + 经纬度 + 时间，提交后写入哈希链存证</div>
            </template>
          </label>
        </div>
      </div>

      <!-- 提交 -->
      <div class="ci-submit">
        <el-button type="primary" size="large" round class="ci-submit-btn" :loading="submitting" :disabled="!photo" @click="submit">
          提交打卡记录
        </el-button>
        <div class="ci-submit-hint" v-if="!photo">需先拍摄现场照片</div>
      </div>

      <!-- 成功回执 -->
      <el-dialog v-model="doneVisible" title="打卡回执" width="92%" class="ci-dialog" :show-close="false">
        <div class="receipt">
          <div class="rc-stamp-wrap"><img src="@/assets/seal/gov-seal.svg" class="rc-stamp" /></div>
          <div class="rc-title">打卡成功 · 已存证</div>
          <div class="rc-grid">
            <span>任务编号</span><b class="mono">{{ task.taskNo }}</b>
            <span>{{ inRange ? '打卡状态' : '打卡状态' }}</span><b :class="inRange ? 'rc-ok' : 'rc-bad'">{{ inRange ? '在场有效' : '离场异常' }}</b>
            <span>坐标快照</span><b class="mono">{{ loc.lat.toFixed(6) }}, {{ loc.lng.toFixed(6) }}</b>
            <span>打卡时间</span><b class="mono">{{ doneTime }}</b>
            <span>存证哈希</span><b class="mono rc-hash">{{ doneHash }}…</b>
          </div>
        </div>
        <template #footer>
          <el-button type="primary" round style="width:100%" @click="doneVisible = false">完成</el-button>
        </template>
      </el-dialog>
    </div>

    <div class="ci-foot">黔能碳溯 · 核查执法 H5 · 定位数据仅用于任务核验</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()

/* ---------- 任务信息：由 URL taskId 派生（正式环境走 /api/v1/public/checkin/{taskId}） ---------- */
const RADIUS = 500 // 有效打卡半径（米），后端字段 gov_inspection_task.radius_m
const task = ref({ taskNo: '', company: '贵州黔能矿业有限责任公司', lat: 26.5982, lng: 106.7076, radius: RADIUS, time: '2026-11-26 09:00 ~ 17:00' })

/* ---------- 定位 ---------- */
const locating = ref(false)
const locState = ref<'none' | 'ok' | 'denied'>('none')
const loc = ref({ lat: 0, lng: 0 })
const locStateText = computed(() => ({ none: '尚未定位', ok: 'GPS 定位成功', denied: '定位被拒绝（模拟模式）' }[locState.value]))
const simOffset = ref(-1) // -1 表示未使用模拟

const rpx = computed(() => 92 / RADIUS) // 雷达图缩放：92px 对应半径 500m

/* 用户点相对核查点的偏移（雷达像素坐标，按米换算） */
const ux = computed(() => 140 + ((loc.value.lng - task.value.lng) * 111320 * Math.cos((task.value.lat * Math.PI) / 180) * rpx.value))
const uy = computed(() => 106 - ((loc.value.lat - task.value.lat) * 110574 * rpx.value))

/* ---------- F9 Haversine ---------- */
function haversine(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const toRad = (x: number) => (x * Math.PI) / 180
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a = Math.sin(dLat / 2) ** 2 + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return 2 * 6371 * 1000 * Math.asin(Math.sqrt(a)) // 返回米
}
const dist = computed(() => haversine(loc.value.lat, loc.value.lng, task.value.lat, task.value.lng))
const inRange = computed(() => dist.value <= task.value.radius)
const verdictClass = computed(() => (locState.value === 'none' ? 'idle' : inRange.value ? 'ok' : 'bad'))

function applyLoc(lat: number, lng: number) {
  loc.value = { lat, lng }
  locState.value = 'ok'
}

/* 真实 GPS */
function locate() {
  locating.value = true
  if (!('geolocation' in navigator)) {
    ElMessage.warning('当前设备不支持 Geolocation，请使用模拟定位坐标')
    locating.value = false
    return
  }
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      applyLoc(pos.coords.latitude, pos.coords.longitude)
      locating.value = false
      ElMessage.success('定位成功：' + pos.coords.latitude.toFixed(5) + ', ' + pos.coords.longitude.toFixed(5))
    },
    () => {
      locateDenied()
    },
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 10000 }
  )
}

/* GPS 被拒 / 超时 → 演示模式降级 */
function locateDenied() {
  locating.value = false
  locState.value = 'denied'
  loc.value = { lat: task.value.lat, lng: task.value.lng }
  ElMessage.info('当前环境无 GPS（HTTP/拒绝授权），已降级为模拟坐标，可点击“模拟定位”调整')
}

/* 演示模拟：在核查点东向偏移 offset 米 */
function sim(offset: number) {
  simOffset.value = offset
  const dLat = offset / 111320
  const dLng = offset / (111320 * Math.cos((task.value.lat * Math.PI) / 180))
  applyLoc(task.value.lat + dLat, task.value.lng + dLng)
}

/* ---------- 拍照 ---------- */
const photo = ref('')
function onPhoto(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) return
  photo.value = URL.createObjectURL(f)
}

/* ---------- 提交 ---------- */
const submitting = ref(false)
const doneVisible = ref(false)
const doneTime = ref('')
const doneHash = ref('')

async function sha256Hex(seed: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(seed + Date.now()))
  return Array.from(new Uint8Array(buf), (x) => x.toString(16).padStart(2, '0')).join('')
}

async function submit() {
  if (!photo.value) return
  submitting.value = true
  doneHash.value = (await sha256Hex(task.value.taskNo)).slice(0, 40)
  doneTime.value = new Date().toLocaleString('zh-CN', { hour12: false })
  setTimeout(() => {
    submitting.value = false
    doneVisible.value = true
  }, 900)
}

/* ---------- 初始化：由 taskId 派生确定性核查点 ---------- */
async function seedOf(s: string): Promise<number> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s))
  return parseInt(Array.from(new Uint8Array(buf), (x) => x.toString(16).padStart(2, '0')).join('').slice(0, 8), 16)
}

onMounted(async () => {
  const taskId = String(route.params.taskId || 'T-2026-118')
  const seed = await seedOf(taskId)
  // 在贵阳附近（106.63, 26.65）以种子抖动，得到确定性核查点
  const baseLat = 26.65 + ((seed >> 6) & 0xff) / 1600 - 0.08
  const baseLng = 106.63 + ((seed >> 14) & 0xff) / 1700 - 0.075
  task.value = {
    taskNo: taskId.toUpperCase(),
    company: '贵州黔能矿业有限责任公司',
    lat: +baseLat.toFixed(5),
    lng: +baseLng.toFixed(5),
    radius: RADIUS,
    time: '2026-11-26 09:00 ~ 17:00'
  }
  locate()
})
</script>

<style scoped lang="scss">
.ci-page {
  position: relative;
  min-height: 100vh;
  background: radial-gradient(800px 500px at 20% -12%, #0c3150 0%, transparent 55%),
              radial-gradient(700px 480px at 110% 112%, #064033 0%, transparent 55%),
              linear-gradient(165deg, #071322 0%, #081a2e 55%, #05101f 100%);
  color: #dce8f5;
  padding: 18px 0 30px;
  -webkit-tap-highlight-color: transparent;
}
.ci-aurora { position: fixed; border-radius: 50%; filter: blur(80px); opacity: 0.3; pointer-events: none;
  &.a { width: 420px; height: 420px; left: -120px; top: -100px; background: radial-gradient(circle, #00b581, transparent 65%); }
  &.b { width: 380px; height: 380px; right: -100px; bottom: -60px; background: radial-gradient(circle, #00d1ff, transparent 65%); } }

.ci-shell { position: relative; width: min(480px, 94vw); margin: 0 auto; display: flex; flex-direction: column; gap: 14px; }

.ci-head { display: flex; align-items: center; gap: 12px; padding: 4px 4px 8px; }
.ci-logo { width: 42px; height: 42px; filter: drop-shadow(0 0 14px rgba(0,209,255,0.45)); animation: bobbing 5s ease-in-out infinite; }
@keyframes bobbing { 50% { transform: translateY(-5px); } }
.ci-head-txt .ci-head-title { font-size: 19px; font-weight: 800; letter-spacing: 1.5px; }
.ci-head-txt .ci-head-sub { font-size: 9.5px; letter-spacing: 2.5px; color: #5b7c9c; margin-top: 3px; }

.ci-task {
  border-radius: 16px; padding: 14px 18px;
  background: linear-gradient(120deg, rgba(0,181,129,0.16), rgba(0,209,255,0.07));
  border: 1px solid rgba(0,209,255,0.28);
  backdrop-filter: blur(12px);
  .ct-row { display: flex; justify-content: space-between; gap: 10px; padding: 4.5px 0; font-size: 12.5px;
    label { color: #7fa3bd; }
    b { text-align: right; font-weight: 600; } }
}

.ci-card {
  border-radius: 16px; padding: 18px 18px;
  background: rgba(10, 30, 50, 0.68);
  border: 1px solid rgba(120, 190, 255, 0.14);
  backdrop-filter: blur(14px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.3);
}
.ci-step { display: flex; align-items: center; gap: 9px; font-size: 14.5px; font-weight: 700; margin-bottom: 14px; letter-spacing: 0.5px;
  i { font-style: normal; width: 24px; height: 24px; border-radius: 8px; display: grid; place-items: center; font-size: 13px;
      background: linear-gradient(135deg, #00b581, #00d1ff); color: #05231d; font-weight: 800; } }

.loc-row { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.loc-info { min-width: 0; }
.li-coords { font-size: 14px; color: #4ce6b8; letter-spacing: 0.5px; word-break: break-all; }
.li-state { margin-top: 6px; font-size: 11.5px; display: flex; align-items: center; gap: 6px;
  .pulse-dot { width: 7px; height: 7px; border-radius: 50%; background: #ffb020; animation: pulse 1.8s infinite; } }
@keyframes pulse { 70% { box-shadow: 0 0 0 7px rgba(255,176,32,0); } }
.sim-row { margin-top: 13px; padding-top: 12px; border-top: 1px dashed rgba(120,190,255,0.16); display: flex; flex-wrap: wrap; gap: 8px; align-items: center;
  .sim-label { font-size: 11px; color: #6686a3; } }

.radar-box { display: grid; grid-template-columns: auto 1fr; gap: 14px; align-items: center; }
.radar { display: block; border-radius: 12px; background: rgba(0,0,0,0.25); border: 1px solid rgba(120,190,255,0.12); }
.rm-dist { display: flex; align-items: baseline; gap: 10px;
  span { font-size: 12px; color: #7fa3bd; }
  b { font-size: 30px; font-weight: 800; font-variant-numeric: tabular-nums;
    &.ok { color: #4ce6b8; text-shadow: 0 0 18px rgba(0,230,162,0.5); }
    &.bad { color: #ff7060; text-shadow: 0 0 18px rgba(255,77,60,0.5); } } }
.rm-formula { margin-top: 10px; font-size: 10px; color: #5f8aad; line-height: 1.6; word-break: break-all; }
.rm-remark { font-size: 10px; color: #4a6a8a; margin-top: 4px; }

.verdict { margin-top: 14px; border-radius: 12px; padding: 12px 14px; display: flex; gap: 12px; align-items: center;
  &.idle { background: rgba(255,176,32,0.08); border: 1px solid rgba(255,176,32,0.3);
    .vd-icon { background: #ffb020; color: #05231d; } .vd-title { color: #ffd58a; } }
  &.ok { background: rgba(0,181,129,0.12); border: 1px solid rgba(0,230,162,0.35);
    .vd-icon { background: #00e6a2; color: #05231d; box-shadow: 0 0 16px rgba(0,230,162,0.5); } .vd-title { color: #63e6b4; } }
  &.bad { background: rgba(255,77,60,0.12); border: 1px solid rgba(255,100,82,0.4);
    .vd-icon { background: #ff4d3c; color: #fff; box-shadow: 0 0 16px rgba(255,77,60,0.5); } .vd-title { color: #ff9c8f; } } }
.vd-icon { width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; font-size: 17px; font-weight: 800; flex-shrink: 0; }
.vd-title { font-size: 13.5px; font-weight: 700; }
.vd-sub { margin-top: 3px; font-size: 10.5px; color: #7f9cba; }

.cam-zone { display: flex; justify-content: center; }
.cam-btn { position: relative; display: grid; place-items: center; width: 100%; min-height: 210px; border-radius: 14px; cursor: pointer;
  border: 1.6px dashed rgba(0,209,255,0.35); background: rgba(0,0,0,0.25); overflow: hidden; transition: all 0.25s;
  &:hover { border-color: rgba(0,230,162,0.7); background: rgba(0,181,129,0.06); }
  &.has { border-style: solid; border-color: rgba(0,230,162,0.5); } }
.cam-input { display: none; }
.cam-plus { width: 52px; height: 52px; border-radius: 50%; display: grid; place-items: center; font-size: 30px; color: #00e6a2;
  background: rgba(0,181,129,0.14); border: 1px solid rgba(0,230,162,0.45); }
.cam-tip { margin-top: 12px; font-size: 13.5px; color: #bcd4e8; letter-spacing: 1px; }
.cam-tip2 { margin-top: 6px; font-size: 10.5px; color: #5c7c9d; }
.cam-preview { width: 100%; display: block; }
.cam-overlay { position: absolute; inset: 0; display: grid; place-items: center; background: rgba(5,16,31,0.55); color: #8fe3ca; font-size: 13px;
  opacity: 0; transition: opacity 0.2s; }
.cam-btn:hover .cam-overlay { opacity: 1; }

.ci-submit { text-align: center; padding: 4px 0 8px; }
.ci-submit-btn { width: 100%; height: 50px; font-size: 16px; letter-spacing: 6px; border: none;
  background: linear-gradient(120deg, #00b581, #00c9a7 55%, #00d1ff);
  box-shadow: 0 12px 30px rgba(0,190,150,0.35); transition: all 0.25s;
  &:hover { transform: translateY(-1px); box-shadow: 0 16px 36px rgba(0,200,160,0.5); } }
.ci-submit-hint { margin-top: 8px; font-size: 11px; color: #6b89a8; }

.receipt { text-align: center; padding: 8px 0 6px; }
.rc-stamp-wrap { display: flex; justify-content: center; margin-bottom: 4px; }
.rc-stamp { width: 96px; height: 96px; transform: rotate(-12deg); filter: drop-shadow(0 4px 10px rgba(200,30,30,0.4));
  animation: stamp-in 0.7s cubic-bezier(0.2, 1.6, 0.4, 1) both; }
@keyframes stamp-in { from { opacity: 0; transform: scale(2.2) rotate(-34deg); } to { opacity: 1; transform: scale(1) rotate(-12deg); } }
.rc-title { font-size: 17px; font-weight: 800; letter-spacing: 2px; color: #0e3b32; margin: 8px 0 14px; }
.rc-grid { display: grid; grid-template-columns: auto 1fr; gap: 8px 14px; text-align: left; font-size: 12.5px;
  border-radius: 12px; background: #f3f8f6; padding: 14px 16px;
  span { color: #6d8a82; }
  b { text-align: right; color: #17332e; font-weight: 600;
    &.rc-ok { color: #009c6d; }
    &.rc-bad { color: #e04635; }
    &.rc-hash { color: #3f7d9c; } } }

.ci-foot { position: relative; margin-top: 18px; text-align: center; font-size: 10.5px; letter-spacing: 1.5px; color: #3d5474; }
.mono { font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace; }
</style>