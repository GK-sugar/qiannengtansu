<template>
  <div class="verify-page">
    <div class="vp-aurora a"></div>
    <div class="vp-aurora b"></div>
    <div class="vp-grid"></div>
    <div class="vp-vignette"></div>

    <header class="vp-head">
      <div class="vp-brand">
        <img src="@/assets/logo/logo-mark.svg" class="vp-logo" alt="黔能碳溯" />
        <div class="vp-brand-txt">
          <div class="vp-name">黔能<span class="grad-text">碳溯</span></div>
          <div class="vp-tag">碳足迹护照 · 公开验真中心</div>
        </div>
      </div>
      <div class="vp-head-right">
        <span class="vp-open"><i class="pulse-dot"></i>免登录公开页</span>
        <el-button text class="vp-back" @click="$router.push('/login')">返回登录</el-button>
      </div>
    </header>

    <main class="vp-main">
      <!-- ========== 左：护照展示 ========== -->
      <section class="vp-pass">
        <div class="pp-ribbon">CARBON FOOTPRINT PASSPORT · 双轨合规认证</div>
        <div class="pp-body">
          <div class="pp-info">
            <div class="pp-title">碳足迹护照</div>
            <div class="pp-sub">贵州省重点产业 · 多级溯源防伪认证</div>
            <div class="pp-kv">
              <div class="pp-row"><label>护照编号</label><b class="mono">{{ pno }}</b></div>
              <div class="pp-row"><label>持证企业</label><b>{{ company }}</b></div>
              <div class="pp-row"><label>产品 / 单位</label><b>{{ product }} · {{ unit }}</b></div>
              <div class="pp-row"><label>核算期</label><b>{{ period }}</b></div>
              <div class="pp-row"><label>碳足迹</label><b class="pp-carbon">{{ carbon }} <i>tCO₂e/{{ unit }}</i></b></div>
              <div class="pp-row"><label>签发日期</label><b>2026-11-26</b></div>
              <div class="pp-row"><label>核查标准</label><b>ISO 14067 / EU CBAM</b></div>
            </div>
            <div class="pp-qr">
              <qrcode-vue :value="selfUrl" :size="92" level="M" class="qr-svg" />
              <div class="pp-qr-txt">
                <div class="pp-qr-t">扫码验真</div>
                <div class="pp-qr-s">防伪令牌 {{ shortToken }}</div>
              </div>
            </div>
          </div>
          <div class="pp-side">
            <img src="@/assets/seal/agency-seal.svg" class="pp-seal" alt="核查机构印章" />
            <div class="pp-sig">
              <div class="pp-sig-line"></div>
              <span>中环低碳国际核查中心</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ========== 右：核算汇总 / 存证哈希 / 验链结论 ========== -->
      <section class="vp-right">
        <!-- 核算汇总 -->
        <div class="vp-card">
          <div class="vc-head"><h3>核算汇总</h3><el-tag size="small" type="success" round effect="dark">核算口径 GB/T 24067</el-tag></div>
          <div class="scope-list">
            <div class="scope-item" v-for="s in scopes" :key="s.k">
              <div class="si-bar" :style="{ width: (tampered ? 86 : s.w) + '%', background: s.c }"></div>
              <div class="si-row">
                <span class="si-name">{{ s.k }} {{ s.name }}</span>
                <span class="si-val">{{ (tampered ? Math.round(s.val * 1.21) : s.val).toLocaleString() }} <i>tCO₂e</i></span>
                <span class="si-pct">{{ s.pct }}%</span>
              </div>
            </div>
          </div>
          <div class="total-row">
            <span>产品碳足迹合计（F5）</span>
            <b>{{ carbon }} tCO₂e/{{ unit }}<em> ≈ {{ total.toLocaleString() }} tCO₂e</em></b>
          </div>
        </div>

        <!-- 存证与验链 -->
        <div class="vp-card">
          <div class="vc-head">
            <h3>哈希链存证查验（F10）</h3>
            <el-button size="small" plain :type="tampered ? 'danger' : 'warning'" @click="tampered = !tampered">
              {{ tampered ? '恢复原始账本' : '模拟篡改' }}
            </el-button>
          </div>
          <div class="chain">
            <div class="chain-link" v-for="(b, i) in blocks" :key="i">
              <div class="cl-head">
                <span class="cl-tag">区块 #{{ i }}</span>
                <span class="cl-name">{{ b.name }}</span>
                <span class="cl-status" :class="b.ok ? 'ok' : 'bad'">{{ b.ok ? '√ 一致' : '× 异常' }}</span>
              </div>
              <div class="cl-data">数据快照：{{ b.dataText }}<span v-if="tampered && b.touched" class="cl-dirty">（副本已被恶意改写 1.21×）</span></div>
              <div class="cl-hash mono">h{{ i }} = {{ b.hash }}</div>
            </div>
          </div>
          <div class="chain-result" :class="chainOk ? 'good' : 'bad'">
            <div class="cr-icon">{{ chainOk ? '✓' : '!' }}</div>
            <div class="cr-txt">
              <div class="cr-title">{{ chainOk ? '整链校验通过 · 存证未被篡改' : '整链校验失败 · 检测到存证异常' }}</div>
              <div class="cr-sub">SHA-256 逐区块重算比对：h<sub>i</sub> = SHA256(h<sub>i−1</sub> ‖ data<sub>i</sub> ‖ salt)，篡改将导致断链并传导至后续区块</div>
            </div>
          </div>
        </div>

        <!-- 验真结论 -->
        <div class="vp-stamp">
          <div class="vs-left">
            <div class="vs-time">{{ verifyTime }}</div>
            <div class="vs-token mono"><i class="pulse-dot"></i>Token {{ shortToken }} · 校验通过（F11）</div>
          </div>
          <div class="vs-right">
            <img src="@/assets/seal/gov-seal.svg" class="vs-seal" />
            <span>验真完成</span>
          </div>
        </div>
      </section>
    </main>

    <footer class="vp-foot">© 2026 黔能碳溯 · 验真结果基于链上存证实时重算</footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import QrcodeVue from 'qrcode.vue'

const route = useRoute()

/* ---------------- 数据模型（演示模式：真实后端将替换为 /api/v1/public/passports/{token}） ---------------- */
const pno = ref('QN-PP-2026-0058')
const company = ref('贵州黔能矿业有限责任公司')
const product = ref('精煤')
const unit = ref('t')
const period = ref('2026-Q4')
const carbon = ref(0.642)
const total = ref(115320)
const scopes = ref([
  { k: '范围一', name: '直接排放（自用煤燃烧 · F2）', val: 58920, pct: 51.1, color: 'linear-gradient(90deg,#ff7a59,#ffb020)', w: 51 },
  { k: '范围二', name: '外购电力隐含排放（F3）', val: 34780, pct: 30.2, color: 'linear-gradient(90deg,#00b581,#2bd8a2)', w: 30 },
  { k: '范围三', name: '运输与供应链（F4）', val: 21620, pct: 18.7, color: 'linear-gradient(90deg,#4da3ff,#00d1ff)', w: 19 }
])

/* F10 哈希链：h_i = SHA256(h_{i-1} ‖ data_i ‖ salt) */
const SALT = 'qnct-site-salt' // 正式环境：加密存于 sys_config，后端注入
const tampered = ref(false)

interface Block { name: string; dataText: string; hash: string; ok: boolean; touched: boolean }
const blocks = ref<Block[]>([])
const chainOk = ref(true)

const selfUrl = computed(() => location.origin + '/verify/' + route.params.token)
const fullToken = ref('')
const shortToken = computed(() => (fullToken.value || '').slice(0, 16).toUpperCase())
const verifyTime = computed(() => new Date().toLocaleString('zh-CN', { hour12: false }))

async function sha256(text: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return Array.from(new Uint8Array(buf), (x) => x.toString(16).padStart(2, '0')).join('')
}

async function seedFromToken(t: string): Promise<number> {
  const h = await sha256(t)
  return parseInt(h.slice(0, 8), 16)
}

/* 依据 URL 令牌派生护照数据 + 按 F10 重算整条哈希链，并对账本进行一致性比对 */
async function init() {
  const token = String(route.params.token || 'QN-PP-2026-0058')
  if (/^qn-pp-\d{4}-\d{4}$/i.test(token)) pno.value = token.toUpperCase()
  const seed = await seedFromToken(token)

  period.value = `2026-Q${(seed % 4) + 1}`
  carbon.value = +(0.6 + ((seed % 100) / 1000)).toFixed(3)
  total.value = 100000 + (seed % 40000)

  const tx = [
    { name: '核算数据存证', data: { period: period.value, total: total.value } },
    { name: '核算报告上链', data: { report: 'QN-BG-' + token.slice(-4), carbon: carbon.value } },
    { name: '护照签发', data: { passportNo: pno.value, standard: 'ISO 14067 / CBAM' } }
  ]

  let ledgerPrev = await sha256('QNCT-GENESIS' + '\u0001' + SALT) // 链上账本：始终按原始数据推进
  let dirtyPrev = ledgerPrev                                        // 现场副本：用读取到的数据重算
  let allOk = true
  const list: Block[] = []

  for (let i = 0; i < tx.length; i++) {
    const clean = JSON.stringify(tx[i].data)
    ledgerPrev = await sha256(ledgerPrev + '\u0001' + clean + '\u0002' + SALT)

    // 篡改演示：默认篡改首个区块，验证“断链传导”
    let dirtyText = clean
    if (tampered.value && i === 0) {
      dirtyText = JSON.stringify({ period: tx[i].data.period, total: 99999 })
    }
    dirtyPrev = await sha256(dirtyPrev + '\u0001' + dirtyText + '\u0002' + SALT)

    const ok = dirtyPrev === ledgerPrev
    if (!ok) allOk = false
    list.push({
      name: tx[i].name,
      dataText: Object.entries(tx[i].data).map(([k, v]) => `${k}=${v}`).join(' ｜ '),
      hash: '0x' + dirtyPrev.slice(0, 26) + '…' + dirtyPrev.slice(-8),
      ok,
      touched: tampered.value && i === 0
    })
  }

  blocks.value = list
  chainOk.value = allOk

  // F11：防伪令牌 = SHA256(passport_no + ':' + site_salt)，取前 16 位展示
  fullToken.value = await sha256(pno.value + ':' + SALT)
}

watch(tampered, () => { void init() })
onMounted(init)
</script>

<style scoped lang="scss">
.verify-page {
  position: relative; min-height: 100vh; overflow-x: hidden;
  background: radial-gradient(1200px 700px at 15% -10%, #0b2943 0%, transparent 55%),
              radial-gradient(1000px 600px at 105% 110%, #063a33 0%, transparent 55%),
              linear-gradient(165deg, #071322 0%, #081a2e 55%, #05101f 100%);
  color: #dce8f5;
  padding: 0 0 40px;
}
.vp-aurora { position: absolute; border-radius: 50%; filter: blur(90px); opacity: 0.35; pointer-events: none;
  &.a { width: 520px; height: 520px; left: -140px; top: -160px; background: radial-gradient(circle, #00b581, transparent 65%); animation: drift 14s ease-in-out infinite alternate; }
  &.b { width: 460px; height: 460px; right: -120px; top: 25%; background: radial-gradient(circle, #00d1ff, transparent 65%); animation: drift 17s ease-in-out infinite alternate-reverse; } }
@keyframes drift { from { transform: translate(0, 0) scale(1); } to { transform: translate(40px, 60px) scale(1.12); } }
.vp-grid { position: absolute; inset: 0;
  background-image: linear-gradient(rgba(120,180,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(120,180,255,0.04) 1px, transparent 1px);
  background-size: 54px 54px; mask-image: radial-gradient(ellipse at 50% 30%, #000 20%, transparent 75%); }
.vp-vignette { position: absolute; inset: 0; box-shadow: inset 0 0 200px rgba(0,0,0,0.6); pointer-events: none; }

.vp-head { position: relative; z-index: 2; display: flex; justify-content: space-between; align-items: center;
  width: min(1240px, 94vw); margin: 0 auto; padding: 22px 0; }
.vp-brand { display: flex; align-items: center; gap: 14px; }
.vp-logo { width: 46px; height: 46px; filter: drop-shadow(0 0 16px rgba(0, 209, 255, 0.5)); animation: float 5s ease-in-out infinite; }
@keyframes float { 50% { transform: translateY(-6px); } }
.vp-brand-txt .vp-name { font-size: 24px; font-weight: 800; letter-spacing: 2px; }
.vp-brand-txt .vp-tag { font-size: 11.5px; letter-spacing: 3px; color: #67a8c8; margin-top: 4px; }
.vp-head-right { display: flex; align-items: center; gap: 18px; }
.vp-open { font-size: 12px; color: #8fdcc4; display: flex; align-items: center; gap: 7px;
  .pulse-dot { width: 7px; height: 7px; border-radius: 50%; background: #00e6a2; box-shadow: 0 0 0 rgba(0,230,162,0.5); animation: pulse 2s infinite; } }
@keyframes pulse { 70% { box-shadow: 0 0 0 8px rgba(0,230,162,0); } }
.vp-back { color: #7d99bd !important; font-size: 13px; }

.vp-main { position: relative; z-index: 2; width: min(1240px, 94vw); margin: 10px auto 0;
  display: grid; grid-template-columns: 7.2fr 8fr; gap: 26px; align-items: start; }

/* ---------- 护照 ---------- */
.vp-pass { border-radius: 22px; overflow: hidden; box-shadow: 0 30px 80px rgba(0,0,0,0.45), 0 0 0 1px rgba(0,209,255,0.12);
  animation: pass-in 0.8s cubic-bezier(0.22, 1, 0.36, 1) both; }
@keyframes pass-in { from { opacity: 0; transform: translateY(24px) rotateX(6deg); } to { opacity: 1; transform: none; } }
.pp-ribbon { background: linear-gradient(90deg, #00b581, #00d1ff); color: #05231d; font-size: 10.5px; font-weight: 700; letter-spacing: 3px; text-align: center; padding: 9px 0; }
.pp-body { display: grid; grid-template-columns: 1fr 132px; background: linear-gradient(150deg, #fbfdfc 0%, #eef6f2 100%); color: #123; position: relative; }
.pp-info { padding: 30px 26px 30px 32px; }
.pp-title { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #063b31; }
.pp-sub { font-size: 12px; color: #667d7a; letter-spacing: 2px; margin: 6px 0 22px; }
.pp-kv { border-top: 1px solid #dbeae4; }
.pp-row { display: flex; justify-content: space-between; gap: 12px; padding: 9.5px 0; border-bottom: 1px dashed #d6e6e0; font-size: 13px;
  label { color: #729089; white-space: nowrap; }
  b { text-align: right; font-variant-numeric: tabular-nums; color: #17332e;
    &.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12.5px; letter-spacing: 1px; }
    &.pp-carbon { color: #009c6d; font-size: 20px; i { font-style: normal; font-size: 11px; color: #729089; } } } }
.pp-qr { margin-top: 20px; display: flex; align-items: center; gap: 14px;
  .qr-svg { border-radius: 10px; background: #fff; box-shadow: 0 8px 22px rgba(16,54,50,0.16); }
  .pp-qr-txt { .pp-qr-t { font-size: 13px; font-weight: 700; color: #063b31; letter-spacing: 2px; }
    .pp-qr-s { font-size: 11px; color: #7c948e; margin-top: 4px; font-family: ui-monospace, Consolas, monospace; } } }
.pp-side { background: linear-gradient(180deg, #073d33 0%, #052e26 100%); display: flex; flex-direction: column; align-items: center; justify-content: flex-end; padding: 24px 14px 26px; position: relative;
  &::before { content: ''; position: absolute; inset: 0; background-image: radial-gradient(220px 180px at 50% 8%, rgba(0,209,255,0.28), transparent 70%); } }
.pp-seal { width: 120px; height: 120px; transform: rotate(-10deg); filter: drop-shadow(0 4px 8px rgba(0,0,0,0.35)); position: relative; }
.pp-sig { margin-top: 26px; text-align: center; position: relative;
  .pp-sig-line { width: 104px; border-top: 1px solid rgba(120,220,190,0.5); margin-bottom: 6px; }
  span { font-size: 11px; color: #8cd3bd; letter-spacing: 1px; } }

/* ---------- 右栏卡片 ---------- */
.vp-right { display: flex; flex-direction: column; gap: 22px; }
.vp-card { background: rgba(10, 30, 50, 0.62); border: 1px solid rgba(120, 190, 255, 0.14); border-radius: 18px; padding: 24px 28px;
  backdrop-filter: blur(16px); box-shadow: 0 18px 46px rgba(0,0,0,0.3); animation: card-in 0.7s cubic-bezier(0.22,1,0.36,1) both; }
@keyframes card-in { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: none; } }
.vc-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px;
  h3 { margin: 0; font-size: 16px; font-weight: 700; letter-spacing: 1px; display: flex; align-items: center; gap: 9px;
    &::before { content: ''; width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, #00b581, #00d1ff); } } }

.scope-list { display: flex; flex-direction: column; gap: 6px; }
.scope-item { position: relative; border-radius: 10px; overflow: hidden; padding: 10px 14px; background: rgba(255,255,255,0.03); }
.si-bar { position: absolute; inset: 0; opacity: 0.16; transition: width 0.8s ease; }
.si-row { position: relative; display: flex; align-items: baseline; gap: 12px; font-size: 12.5px; }
.si-name { flex: 1; color: #c5d6e8; }
.si-val { color: #fff; font-weight: 700; font-variant-numeric: tabular-nums; i { font-style: normal; font-size: 10.5px; color: #6686a3; } }
.si-pct { width: 46px; text-align: right; color: #7ee7c4; font-weight: 700; }
.total-row { margin-top: 16px; padding: 13px 16px; border-radius: 12px; display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(90deg, rgba(0,181,129,0.16), rgba(0,209,255,0.08)); border: 1px solid rgba(0,209,255,0.25);
  span { font-size: 12px; color: #9cc9bb; letter-spacing: 1px; }
  b { font-size: 17px; color: #4ce6b8; em { font-style: normal; font-size: 11px; color: #7fa9a4; margin-left: 8px; font-weight: 400; } } }

/* 哈希链 */
.chain { display: flex; flex-direction: column; gap: 10px; }
.chain-link { border: 1px solid rgba(120,190,255,0.15); border-radius: 12px; padding: 12px 16px; background: rgba(255,255,255,0.025); transition: all 0.25s;
  &:hover { border-color: rgba(0,209,255,0.4); background: rgba(0,209,255,0.05); } }
.cl-head { display: flex; gap: 10px; align-items: center; font-size: 12.5px; }
.cl-tag { font-size: 10.5px; padding: 2px 8px; border-radius: 6px; background: rgba(0,181,129,0.16); color: #62e0ba; border: 1px solid rgba(0,181,129,0.35); letter-spacing: 1px; }
.cl-name { color: #e3edf7; font-weight: 600; flex: 1; }
.cl-status { font-size: 11px; padding: 2px 9px; border-radius: 20px;
  &.ok { color: #63e6b4; background: rgba(0,181,129,0.14); }
  &.bad { color: #ff8d80; background: rgba(255,77,60,0.14); animation: flash 1s infinite; } }
@keyframes flash { 50% { opacity: 0.45; } }
.cl-data { margin: 7px 0 6px; font-size: 11.5px; color: #7f9cba;
  .cl-dirty { color: #ff8d80; font-weight: 600; } }
.cl-hash { font-size: 11.5px; color: #4fa5cf; word-break: break-all; }

.chain-result { margin-top: 16px; display: flex; gap: 14px; align-items: center; padding: 15px 18px; border-radius: 14px;
  &.good { background: linear-gradient(90deg, rgba(0,181,129,0.2), rgba(0,181,129,0.08)); border: 1px solid rgba(0,230,162,0.4); }
  &.bad { background: linear-gradient(90deg, rgba(255,77,60,0.22), rgba(255,77,60,0.08)); border: 1px solid rgba(255,120,100,0.5); } }
.cr-icon { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; font-size: 19px; font-weight: 800; flex-shrink: 0;
  .good & { color: #05231d; background: #00e6a2; box-shadow: 0 0 20px rgba(0,230,162,0.45); }
  .bad & { color: #fff; background: #ff4d3c; box-shadow: 0 0 20px rgba(255,77,60,0.45); } }
.cr-title { font-size: 14.5px; font-weight: 700; .bad & { color: #ffb3a8; } }
.cr-sub { margin-top: 5px; font-size: 11px; color: #7595b3; }

/* 验真章 */
.vp-stamp { position: relative; display: flex; justify-content: space-between; align-items: center; border-radius: 18px; padding: 18px 26px;
  background: linear-gradient(110deg, rgba(11,40,64,0.7), rgba(13,48,56,0.7)); border: 1px solid rgba(120,190,255,0.18); backdrop-filter: blur(16px); }
.vs-time { font-size: 12px; color: #8fb3d4; letter-spacing: 1px; }
.vs-token { margin-top: 6px; font-size: 12.5px; color: #4ce6b8; letter-spacing: 1px; display: flex; align-items: center; gap: 7px;
  .pulse-dot { width: 6px; height: 6px; border-radius: 50%; background: #00e6a2; animation: pulse 2s infinite; } }
.vs-right { display: flex; align-items: center; gap: 14px; }
.vs-seal { width: 64px; height: 64px; transform: rotate(-12deg); filter: drop-shadow(0 3px 8px rgba(200,30,30,0.4)); animation: seal-in 0.7s cubic-bezier(0.2,1.6,0.4,1) both; }
@keyframes seal-in { from { opacity: 0; transform: scale(2) rotate(-40deg); } to { opacity: 1; transform: scale(1) rotate(-12deg); } }
.vs-right span { font-size: 13px; font-weight: 700; color: #ff9c8f; letter-spacing: 2px; }

.vp-foot { position: relative; z-index: 2; margin-top: 38px; text-align: center; font-size: 11px; letter-spacing: 2px; color: #3d5474; }

.mono { font-family: ui-monospace, 'Cascadia Mono', Consolas, monospace; }
@media (max-width: 960px) {
  .vp-main { grid-template-columns: 1fr; }
  .vp-open { display: none; }
}
</style>