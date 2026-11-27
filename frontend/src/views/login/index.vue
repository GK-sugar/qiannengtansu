<template>
  <div class="login-page">
    <!-- CSS 流动极光层（性能友好） -->
    <div class="aurora aurora-a"></div>
    <div class="aurora aurora-b"></div>
    <div class="aurora aurora-c"></div>
    <!-- Canvas：粒子星空 + 星座连线 + 上升碳泡 -->
    <canvas ref="canvasRef" class="star-canvas"></canvas>
    <!-- 网格与暗角 -->
    <div class="grid-overlay"></div>
    <div class="vignette"></div>

    <div class="login-wrap">
      <!-- ======== 左侧品牌区 ======== -->
      <div class="brand-panel">
        <div class="brand-top">
          <img src="@/assets/logo/logo-mark.svg" class="brand-logo" alt="黔能碳溯" />
          <span class="brand-ver">V2.0 · 双轨合规</span>
        </div>
        <h1 class="brand-title">
          黔能<span class="grad-text">碳溯</span>
        </h1>
        <p class="brand-slogan">贵州省重点产业碳足迹双轨合规与多级溯源智能系统</p>
        <div class="chip-row">
          <span class="chip">双轨合规</span>
          <span class="chip">多级溯源</span>
          <span class="chip">AI 核算引擎</span>
          <span class="chip">哈希链存证</span>
        </div>
        <div class="brand-stats">
          <div class="bs-item">
            <div class="bs-num">2,3<em>84</em></div>
            <div class="bs-label">在册重点企业</div>
          </div>
          <div class="bs-item">
            <div class="bs-num">1<em>46.2</em><i>万t</i></div>
            <div class="bs-label">累计核算排放</div>
          </div>
          <div class="bs-item">
            <div class="bs-num">99<em>.96%</em></div>
            <div class="bs-label">存证链健康度</div>
          </div>
        </div>
        <div class="brand-foot">GUIZHOU PROVINCE · CARBON FOOTPRINT GOVERNANCE PLATFORM</div>
      </div>

      <!-- ======== 右侧玻璃登录卡 ======== -->
      <div class="login-card">
        <div class="card-head">
          <div class="card-title">欢迎登录</div>
          <div class="card-sub">Sign in to QNCT Platform</div>
        </div>

        <el-form :model="form" size="large" @keyup.enter="onLogin">
          <el-form-item>
            <el-input v-model="form.username" placeholder="请输入账号" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
          </el-form-item>
          <div class="card-options">
            <el-checkbox v-model="remember">记住我</el-checkbox>
            <a class="forget" @click.prevent="hint">忘记密码？请联系平台管理员</a>
          </div>
          <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
            登 录
          </el-button>
        </el-form>

        <el-divider class="quick-divider"><span>演示账号 · 一键进入</span></el-divider>

        <div class="quick-grid">
          <button v-for="u in DEMO_USERS" :key="u.key" class="quick-btn"
                  :style="{ '--c': u.color }" @click="quickLogin(u.key)">
            <span class="qb-dot"></span>
            <span class="qb-main">{{ u.label }}</span>
            <span class="qb-sub">{{ u.desc }}</span>
          </button>
        </div>

        <div class="card-foot">
          <el-icon><Lock /></el-icon>
          <span>数据经 SHA-256 哈希链存证 · 登录行为纳入审计日志</span>
        </div>
      </div>
    </div>

    <!-- 找回密码说明弹窗 -->
    <el-dialog v-model="hintVisible" title="找回密码" width="480px" class="forget-dialog">
      <div class="forget-body">
        <div class="fb-line">当前环境未开通短信 / 邮箱找回，可使用下方<b>演示账号</b>一键进入。</div>
        <div class="fb-line">正式环境找回流程：</div>
        <ol class="fb-steps">
          <li>联系所在单位系统管理员，核实身份后由管理员在「RBAC 权限控制中心 → 用户管理」中重置密码；</li>
          <li>或联系平台运营方（贵州省生态环境厅信息化中心）提交书面重置申请；</li>
          <li>重置成功后首次登录将强制修改密码，操作全程写入审计日志。</li>
        </ol>
      </div>
      <template #footer>
        <el-button type="primary" @click="hintVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <div class="page-foot">© 2026 黔能碳溯 · 贵州省重点产业碳足迹治理平台</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { DEMO_USERS, demoUserMap, getPassword } from '@/utils/demo'
import type { RoleKey } from '@/utils/demo'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const remember = ref(true)
const loading = ref(false)

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const user = await auth.login(form.username, form.password)
    ElMessage.success(`欢迎回来，${user.realName}`)
    router.push((route.query.redirect as string) || user.homePath)
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function quickLogin(key: RoleKey) {
  const u = demoUserMap[key]
  form.username = u.username
  form.password = getPassword(u.username) ?? u.password
  onLogin()
}

const hintVisible = ref(false)
function hint() {
  hintVisible.value = true
}

/* ================= Canvas 粒子星空 ================= */
const canvasRef = ref<HTMLCanvasElement>()

interface Star {
  x: number; y: number; z: number; r: number; vx: number; vy: number; tw: number; phase: number
}
interface Bubble { x: number; y: number; r: number; speed: number; alpha: number }

let stars: Star[] = []
let bubbles: Bubble[] = []
let raf = 0
let running = true
let W = 0
let H = 0

function initScene() {
  const canvas = canvasRef.value
  if (!canvas) return
  W = canvas.width = window.innerWidth * Math.min(window.devicePixelRatio || 1, 2)
  H = canvas.height = window.innerHeight * Math.min(window.devicePixelRatio || 1, 2)
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  stars = Array.from({ length: 130 }, () => ({
    x: Math.random() * W,
    y: Math.random() * H,
    z: 0.35 + Math.random() * 0.65,
    r: (0.4 + Math.random() * 1.1) * dpr,
    vx: (Math.random() - 0.5) * 0.06 * dpr,
    vy: (Math.random() - 0.5) * 0.06 * dpr,
    tw: 0.4 + Math.random() * 1.6,
    phase: Math.random() * Math.PI * 2
  }))
  bubbles = Array.from({ length: 22 }, () => spawnBubble(true))
}

function spawnBubble(anyY = false): Bubble {
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  return {
    x: Math.random() * W,
    y: anyY ? Math.random() * H : H + 8,
    r: (1 + Math.random() * 2.4) * dpr,
    speed: (0.25 + Math.random() * 0.6) * dpr,
    alpha: 0.15 + Math.random() * 0.5
  }
}

function draw(t: number) {
  if (!running) return
  const ctx = canvasRef.value?.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, W, H)

  // 星座连线（近距离粒子）
  ctx.lineWidth = 0.6
  for (let i = 0; i < stars.length; i++) {
    for (let j = i + 1; j < stars.length; j++) {
      const dx = stars[i].x - stars[j].x
      const dy = stars[i].y - stars[j].y
      const d2 = dx * dx + dy * dy
      if (d2 < 120 * 120 * Math.min(window.devicePixelRatio || 1, 2)) {
        const a = (1 - Math.sqrt(d2) / (120 * Math.min(window.devicePixelRatio || 1, 2))) * 0.14
        ctx.strokeStyle = `rgba(0,209,255,${a.toFixed(3)})`
        ctx.beginPath()
        ctx.moveTo(stars[i].x, stars[i].y)
        ctx.lineTo(stars[j].x, stars[j].y)
        ctx.stroke()
      }
    }
  }

  // 星点（闪烁 + 漂移 + 边界回绕）
  for (const s of stars) {
    const twinkle = 0.55 + 0.45 * Math.sin(t / 1000 * s.tw + s.phase)
    ctx.fillStyle = `rgba(${s.z > 0.75 ? '220,255,245' : '180,230,255'},${(0.18 + 0.55 * s.z) * twinkle})`
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.r * s.z, 0, Math.PI * 2)
    ctx.fill()
    s.x += s.vx
    s.y += s.vy
    if (s.x < -4) s.x = W + 4; if (s.x > W + 4) s.x = -4
    if (s.y < -4) s.y = H + 4; if (s.y > H + 4) s.y = -4
  }

  // 上升碳泡（翡翠绿微光）
  for (const b of bubbles) {
    b.y -= b.speed
    const a = b.alpha * (b.y > H - 60 ? 0 : 1)
    const grad = ctx.createRadialGradient(b.x, b.y, 0, b.x, b.y, b.r * 3)
    grad.addColorStop(0, `rgba(0,231,170,${a})`)
    grad.addColorStop(1, 'rgba(0,231,170,0)')
    ctx.fillStyle = grad
    ctx.beginPath()
    ctx.arc(b.x, b.y, b.r * 3, 0, Math.PI * 2)
    ctx.fill()
    if (b.y < -12) Object.assign(b, spawnBubble(false))
  }

  raf = requestAnimationFrame(draw)
}

function onResize() {
  initScene()
}
function onVisibility() {
  running = !document.hidden
  if (running) { cancelAnimationFrame(raf); raf = requestAnimationFrame(draw) }
}

onMounted(() => {
  initScene()
  raf = requestAnimationFrame(draw)
  window.addEventListener('resize', onResize)
  document.addEventListener('visibilitychange', onVisibility)
})
onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('resize', onResize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(1200px 700px at 85% -10%, rgba(0, 87, 167, 0.28), transparent 60%),
    radial-gradient(900px 600px at -10% 110%, rgba(0, 113, 87, 0.22), transparent 55%),
    linear-gradient(160deg, #02070f 0%, #04101f 45%, #030d1c 100%);
  color: #fff;
}
/* ---------- 极光 ---------- */
.aurora {
  position: absolute;
  border-radius: 50%;
  filter: blur(70px);
  opacity: 0.55;
  will-change: transform;
}
.aurora-a {
  width: 62vw; height: 46vh; left: -12vw; top: -12vh;
  background: radial-gradient(ellipse at center, rgba(0, 181, 129, 0.5), transparent 65%);
  animation: drift-a 22s ease-in-out infinite alternate;
}
.aurora-b {
  width: 55vw; height: 42vh; right: -14vw; top: 8vh;
  background: radial-gradient(ellipse at center, rgba(0, 209, 255, 0.4), transparent 65%);
  animation: drift-b 26s ease-in-out infinite alternate;
}
.aurora-c {
  width: 48vw; height: 40vh; left: 28vw; bottom: -18vh;
  background: radial-gradient(ellipse at center, rgba(60, 80, 220, 0.32), transparent 65%);
  animation: drift-c 30s ease-in-out infinite alternate;
}
@keyframes drift-a { from { transform: translate(0, 0) rotate(0deg) scale(1); } to { transform: translate(9vw, 7vh) rotate(18deg) scale(1.18); } }
@keyframes drift-b { from { transform: translate(0, 0) scale(1.1); } to { transform: translate(-10vw, -6vh) rotate(-14deg) scale(0.92); } }
@keyframes drift-c { from { transform: translate(0, 0) scale(0.95); } to { transform: translate(6vw, -8vh) rotate(10deg) scale(1.15); } }
/* ---------- 星空 / 网格 / 暗角 ---------- */
.star-canvas { position: absolute; inset: 0; z-index: 1; }
.grid-overlay {
  position: absolute; inset: 0; z-index: 0;
  background-image:
    linear-gradient(rgba(120, 180, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 180, 255, 0.045) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(ellipse at center, #000 30%, transparent 78%);
}
.vignette { position: absolute; inset: 0; z-index: 2; pointer-events: none; box-shadow: inset 0 0 180px rgba(0, 0, 0, 0.55); }

/* ---------- 主区 ---------- */
.login-wrap {
  position: relative;
  z-index: 3;
  width: min(1120px, 94vw);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 48px;
}
.brand-panel { flex: 1; min-width: 0; padding-left: 12px; }
.brand-top { display: flex; align-items: center; gap: 12px; margin-bottom: 26px; }
.brand-logo { width: 52px; height: 52px; filter: drop-shadow(0 0 18px rgba(0, 209, 255, 0.55)); animation: float 5s ease-in-out infinite; }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-7px); } }
.brand-ver {
  font-size: 11px;
  letter-spacing: 2px;
  padding: 4px 12px;
  border-radius: 20px;
  color: #7ee7c4;
  background: rgba(0, 181, 129, 0.12);
  border: 1px solid rgba(0, 181, 129, 0.35);
}
.brand-title { font-size: 64px; font-weight: 800; margin: 0 0 6px; letter-spacing: 6px; }
.brand-slogan { font-size: 16px; color: #8ea2bc; letter-spacing: 1.5px; margin: 0 0 26px; }
.chip-row { display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 40px; }
.chip {
  padding: 7px 16px;
  font-size: 12.5px;
  letter-spacing: 1px;
  border-radius: 8px;
  color: #bfe9fe;
  background: linear-gradient(135deg, rgba(0, 181, 129, 0.14), rgba(0, 209, 255, 0.08));
  border: 1px solid rgba(0, 209, 255, 0.22);
  backdrop-filter: blur(6px);
}
.brand-stats { display: flex; gap: 44px; }
.bs-num { font-size: 30px; font-weight: 700; font-variant-numeric: tabular-nums;
  em { font-style: normal; font-size: 18px; color: #4ce6b8; margin-left: 4px; }
  i { font-style: normal; font-size: 13px; color: #8ea2bc; margin-left: 4px; }
}
.bs-label { font-size: 12px; color: #5f7695; margin-top: 6px; letter-spacing: 1px; }
.brand-foot { margin-top: 52px; font-size: 10px; letter-spacing: 3px; color: #3d5474; }

/* ---------- 登录卡 ---------- */
.login-card {
  width: 404px;
  flex-shrink: 0;
  padding: 36px 34px 26px;
  border-radius: 20px;
  background: rgba(10, 26, 46, 0.55);
  border: 1px solid rgba(130, 190, 255, 0.16);
  backdrop-filter: blur(18px) saturate(140%);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.45), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  animation: card-in 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes card-in { from { opacity: 0; transform: translateY(26px) scale(0.98); } to { opacity: 1; transform: none; } }
.card-head { margin-bottom: 22px; }
.card-title { font-size: 22px; font-weight: 700; letter-spacing: 2px; }
.card-sub { font-size: 11px; color: #66809f; letter-spacing: 2px; margin-top: 6px; text-transform: uppercase; }
.card-options { display: flex; justify-content: space-between; align-items: center; margin: -4px 0 18px; font-size: 12px; }
.forget { color: #5aa8d8; cursor: pointer; }
.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 8px;
  border: none;
  background: linear-gradient(120deg, #00b581, #00c9a7 55%, #00d1ff);
  box-shadow: 0 10px 28px rgba(0, 190, 150, 0.35);
  transition: all 0.25s ease;
  &:hover { box-shadow: 0 14px 34px rgba(0, 200, 160, 0.5); transform: translateY(-1px); }
}
.quick-divider { margin: 20px 0 14px; :deep(.el-divider__text) { font-size: 11px; color: #5f7695; letter-spacing: 1px; background: #0a1a2e; border-radius: 10px; } }
.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.quick-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 12px 14px;
  border-radius: 12px;
  text-align: left;
  cursor: pointer;
  color: #dbe7f5;
  background: rgba(255, 255, 255, 0.045);
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.2s ease;
  position: relative;
  &:hover { background: rgba(255, 255, 255, 0.09); border-color: var(--c); transform: translateY(-2px);
    .qb-dot { box-shadow: 0 0 12px var(--c); } }
  .qb-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--c); transition: box-shadow 0.2s; }
  .qb-main { font-size: 13px; font-weight: 600; }
  .qb-sub { font-size: 11px; color: #6f87a5; }
}
.card-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 18px;
  font-size: 11px;
  color: #4d6583;
}
.page-foot {
  position: absolute;
  bottom: 18px;
  left: 0; right: 0;
  z-index: 3;
  text-align: center;
  font-size: 11px;
  letter-spacing: 2px;
  color: #3d5474;
}
.forget-body { font-size: 13px; color: #2c3e50; line-height: 1.8; }
.fb-line { margin-bottom: 8px; b { color: #0d7a5f; } }
.fb-steps { padding-left: 20px; margin: 4px 0 0; color: #4a5a6a;
  li { margin-bottom: 6px; } }
@media (max-width: 900px) {
  .brand-panel { display: none; }
  .login-card { width: min(420px, 92vw); }
}
</style>