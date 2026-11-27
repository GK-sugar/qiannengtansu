<template>
  <div class="page ai-page">
    <div class="hero-banner">
      <div class="title">AI 碳管家</div>
      <div class="sub">集成大语言模型（qwen-plus）的企业碳智能问答：政策解读 · 核算方法 · 异常解释 · 建议生成</div>
    </div>

    <div class="chat-card">
      <!-- 侧边：快捷提问 / 能力说明 -->
      <div class="chat-side">
        <div class="side-block">
          <div class="side-title"><el-icon><MagicStick /></el-icon> 建议问题</div>
          <button v-for="q in quickQs" :key="q" class="q-chip" @click="ask(q)">{{ q }}</button>
        </div>
        <div class="side-block">
          <div class="side-title"><el-icon><InfoFilled /></el-icon> 已挂载企业上下文</div>
          <div class="ctx-list">
            <div class="ctx-item"><span class="dot"></span>核算期 2026-04 ~ 2026-09 · 6 期活动数据</div>
            <div class="ctx-item"><span class="dot"></span>排放因子库 v3.2 · 电网 0.5703 tCO₂e/MWh</div>
            <div class="ctx-item"><span class="dot"></span>波动预警阈值 ±20% · 化验单 18 份</div>
          </div>
        </div>
        <div class="side-foot">
          {{ modeText }}<br />
          <span class="dim">回答结合企业活动数据与省级因子库生成</span>
        </div>
      </div>

      <!-- 对话区 -->
      <div class="chat-main">
        <div ref="msgBox" class="msg-list">
          <div class="msg welcome">
            <img src="@/assets/logo/logo-mark.svg" class="bot-avatar" />
            <div>
              <div class="welcome-text">您好，我是您的 AI 碳管家 👋<br />已挂载贵企业 2026-04 ~ 2026-09 活动数据与省级因子库 v3.2，可以问我核算方法、法规政策，或让我解释本期排放波动。</div>
            </div>
          </div>
          <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
            <img v-if="m.role === 'ai'" src="@/assets/logo/logo-mark.svg" class="bot-avatar" />
            <div class="bubble" v-html="renderText(m.content)"></div>
          </div>
          <div v-if="typing" class="msg ai"><img src="@/assets/logo/logo-mark.svg" class="bot-avatar" /><div class="bubble typing"><span></span><span></span><span></span></div></div>
        </div>
        <div class="input-bar">
          <el-input v-model="draft" type="textarea" :rows="2" resize="none" placeholder="输入你的问题，Enter 发送，Shift+Enter 换行" @keydown.enter.exact.prevent="send" />
          <el-button type="primary" class="send-btn" :loading="typing" @click="send">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { MagicStick, InfoFilled, Promotion } from '@element-plus/icons-vue'

interface Msg { role: 'user' | 'ai'; content: string }

const messages = ref<Msg[]>([])
const draft = ref('')
const typing = ref(false)
const msgBox = ref<HTMLElement>()
const modeText = ref('后端在线时由 qwen-plus 结合数据库实时推理')

const quickQs = [
  '9 月排放环比变化的原因是什么？',
  '欧盟 CBAM 对贵州煤炭出口有何要求？',
  '化验单低位发热量如何影响范围一核算？',
  '如何降低外购电力的范围二排放？'
]

/* ============ 企业活动数据（与「数据填报」页同源，单位：煤 t / 电 kWh / 货运 t·km / 排放 tCO₂e） ============ */
const records = [
  { period: '2026-09', coal: 7420, power: 458000, freight: 630000, total: 12840.6 },
  { period: '2026-08', coal: 7660, power: 451000, freight: 640000, total: 12431.2 },
  { period: '2026-07', coal: 7920, power: 462000, freight: 650000, total: 13128.8 },
  { period: '2026-06', coal: 8350, power: 459000, freight: 680000, total: 13602.5 },
  { period: '2026-05', coal: 8120, power: 448000, freight: 660000, total: 13031.9 },
  { period: '2026-04', coal: 7980, power: 436000, freight: 620000, total: 12791.3 }
]
const EF_GRID = 0.5703 // kgCO₂e/kWh（省级电网因子 v3.2）
const EF_ROAD = 0.078 // kgCO₂e/t·km
const EF_RAIL = 0.036
const pct = (cur: number, base: number) => (((cur - base) / base) * 100).toFixed(1)
const sign = (v: string) => (Number(v) >= 0 ? '+' : '') + v + '%'

/* ============ 本地数据引擎：按问题类型结合企业数据生成差异化回答 ============ */
function localAnswer(q: string): string {
  const cur = records[0]
  const prev = records[1]
  const has = (...keys: string[]) => keys.some((k) => q.includes(k))

  if (has('波动', '上升', '下降', '环比', '为什么', '原因', '异常', '变化')) {
    const dTotal = pct(cur.total, prev.total)
    const dCoal = pct(cur.coal, prev.coal)
    const dPower = pct(cur.power, prev.power)
    const dFreight = pct(cur.freight, prev.freight)
    return `已对比 ${cur.period} 与 ${prev.period} 两期活动数据：\n\n`
      + `1. <b>总量</b>：${cur.total} tCO₂e，环比 ${sign(dTotal)}（${prev.total} → ${cur.total}），未超出 ±20% 预警阈值，属正常波动。\n`
      + `2. <b>归因拆解</b>：原煤 ${cur.coal} t（环比 ${sign(dCoal)}）、外购电 ${cur.power.toLocaleString()} kWh（环比 ${sign(dPower)}）、货运 ${cur.freight.toLocaleString()} t·km（环比 ${sign(dFreight)}）。本期总量变化主要由<b>外购电量</b>驱动，煤耗反而下降，说明排放结构正在向范围二转移。\n`
      + `3. <b>趋势</b>：近 6 期峰值出现在 2026-06（13602.5 t），此后连续回落，9 月小幅反弹与用电回升一致。\n\n`
      + `建议：核对 9 月新增用电设备台账，确认是否为新增产线投运；若属正常增产，可在报告中附产能说明以免触发复核。`
  }
  if (has('CBAM', '欧盟', '出口', '碳边境', '出海')) {
    return `欧盟 CBAM（碳边境调节机制）对贵企业出口的影响：\n\n`
      + `1. <b>申报义务</b>：过渡期内按季度报送吨产品直接排放与间接排放；2026 年起进入缴费期，需购买 CBAM 证书抵扣欧盟碳价。\n`
      + `2. <b>数据要求</b>：排放因子须采用欧盟认可口径或经核查的实测值。贵企业 9 月货运 ${cur.freight.toLocaleString()} t·km 对应的运输排放需计入产品碳足迹边界。\n`
      + `3. <b>平台支持</b>：「出海数据包」模块可按 CBAM 模板自动拼装 Excel，附企业章与哈希清单并存证上链，供欧方进口商核验。\n\n`
      + `建议：优先为出口批次建立独立核算单元，保留化验单与因子来源证据链，避免采用缺省值带来的惩罚性默认排放。`
  }
  if (has('化验', '发热量', 'Qnet', '检测', '煤质')) {
    return `化验单低位发热量（Qnet,ar）是范围一核算精度的关键：\n\n`
      + `1. <b>公式</b>：E = 消耗量 × Qnet,ar × 单位热值含碳量 × 氧化率。发热量实测值每偏差 5%，范围一排放同向偏差约 5%。\n`
      + `2. <b>贵企业情况</b>：9 月原煤消耗 ${cur.coal} t，若全部采用缺省因子（约 1.93 tCO₂e/t）而非化验单实测值，核算不确定度可达 ±8%。\n`
      + `3. <b>平台流程</b>：化验单拍照 → qwen3-vl-plus 多模态识别 → 双人复核 → 自动带入核算引擎，识别与复核记录全程留痕。\n\n`
      + `建议：按批次送检并保留化验单原件扫描件，月度加权平均后作为当期 NCV，可显著降低核查风险。`
  }
  if (has('电力', '范围二', '购电', '用电', '绿电')) {
    const gridEmission = (cur.power * EF_GRID) / 1000
    const saving10 = gridEmission * 0.1
    return `范围二（外购电力）排放分析与降碳路径：\n\n`
      + `1. <b>现状</b>：9 月外购电 ${cur.power.toLocaleString()} kWh，按省级电网因子 ${EF_GRID} kgCO₂e/kWh 折算约 <b>${gridEmission.toFixed(1)} tCO₂e</b>。\n`
      + `2. <b>绿电替代</b>：若 10% 电量改为绿电交易或分布式光伏自发自用，每月可减排约 ${saving10.toFixed(1)} t，全年约 ${(saving10 * 12).toFixed(0)} t。\n`
      + `3. <b>能效改造</b>：高效电机 + 变频改造通常可节电 8%~15%；结合峰谷电价错峰生产，还能同步降低电费。\n\n`
      + `建议：先在「数据填报」中拆分各设施用电（当前已有 2101/2103/2104 三个计量点），定位高耗电工段后再立项改造。`
  }
  if (has('因子', '版本', 'v3.2', '口径')) {
    return `当前引用省级因子库 v3.2（2026-09-01 生效）关键因子：\n\n`
      + `· 南方电网（贵州）：<b>0.5703 tCO₂e/MWh</b>（v3.1 为 0.5810，下调 1.8%）\n`
      + `· 原煤（低位发热量法）：约 1.93 tCO₂e/t，化验单实测优先\n`
      + `· 柴油：3.16 tCO₂e/t\n`
      + `· 公路重卡：0.078 kgCO₂e/t·km；铁路：0.036；水运：0.012\n\n`
      + `因子修订全程留痕并推送全省企业，历史核算记录仍按当时版本追溯，不受新版本影响。可在「省级因子库管理」查看版本比对。`
  }
  if (has('哈希', '存证', '篡改', '验真', '区块链')) {
    return `平台采用哈希链存证保障数据不可篡改：\n\n`
      + `1. <b>原理</b>：h(i) = SHA-256(salt ‖ h(i−1) ‖ SHA-256(data_i ‖ salt))，每个数据块的指纹都依赖前一块。\n`
      + `2. <b>效果</b>：任一历史数据被改动，本块及后续所有块校验必然失败，篡改会沿链传导、无法隐藏。\n`
      + `3. <b>核验</b>：碳足迹护照附带公开验真页与二维码，监管方、进口商扫码即可实时重算全链指纹。\n\n`
      + `贵企业的核算报告、出海数据包与护照签发记录均已入链，可在「审计中心」查看存证明细。`
  }
  if (has('金融', '信用', '授信', 'CCER', '碳资产', '贷款')) {
    return `绿色金融与碳资产路径：\n\n`
      + `1. <b>信用分（F7）</b>：平台按报送及时性、数据完整率、预警处理情况动态评分。贵企业近 6 期均按期提交，无高危未处理预警，信用基础良好。\n`
      + `2. <b>预授信（F8）</b>：银行可依据信用分与减排量给予绿色信贷额度，「绿色金融」页可一键生成授信申请单。\n`
      + `3. <b>CCER</b>：光伏自发自用、余热回收等减排项目可申报国家核证自愿减排量，形成可交易碳资产。\n\n`
      + `建议：保持按月报送与预警闭环处理，信用分提升后授信额度与利率优惠都会更可观。`
  }
  if (has('运输', '物流', '范围三', '货运')) {
    const roadEmission = (cur.freight * EF_ROAD) / 1000
    const shiftSaving = (cur.freight * 0.6 * (EF_ROAD - EF_RAIL)) / 1000
    return `范围三运输排放分析：\n\n`
      + `1. <b>现状</b>：9 月公路货运 ${cur.freight.toLocaleString()} t·km，按重卡因子 ${EF_ROAD} kgCO₂e/t·km 折算约 <b>${roadEmission.toFixed(1)} tCO₂e</b>。\n`
      + `2. <b>结构优化</b>：若 60% 运量切换为铁路（因子 ${EF_RAIL}），每月可减排约 ${shiftSaving.toFixed(1)} t；乌江航道水运（0.012）减排空间更大。\n`
      + `3. <b>数据支撑</b>：「物流追踪」页已接入运输轨迹与运单，可按车型/线路细分因子，提高核算精度。\n\n`
      + `建议：大宗原煤与产品外运优先签订铁水联运协议，并在供应链协同中要求承运商提供分车型运量数据。`
  }
  if (has('整改', '预警处理', '闭环')) {
    return `整改闭环流程：\n\n`
      + `1. 预警触发（环比 ±20% 或证据缺失）→ 自动生成整改事项并定级（高/中/低）。\n`
      + `2. 「AI 辅助生成整改计划」可按事项类型输出措施清单、责任部门、时限与预算。\n`
      + `3. 整改完成后上传验证材料，监管端复核通过即闭环，记录同步入哈希链。\n\n`
      + `贵企业当前无高危未闭环事项；可在「整改跟踪」页查看历史事项与预算执行情况。`
  }
  // 默认：企业数据概览 + 引导
  const peak = records.reduce((a, b) => (b.total > a.total ? b : a))
  return `已读取贵企业 ${records[records.length - 1].period} ~ ${cur.period} 共 ${records.length} 期核算数据：\n\n`
    + `· 最新一期（${cur.period}）总排放 <b>${cur.total} tCO₂e</b>，环比 ${sign(pct(cur.total, prev.total))}\n`
    + `· 区间峰值 ${peak.period}（${peak.total} t），此后总体回落\n`
    + `· 活动数据：原煤 ${cur.coal} t · 外购电 ${cur.power.toLocaleString()} kWh · 货运 ${cur.freight.toLocaleString()} t·km\n\n`
    + `我可以进一步为您：解释排放波动归因、测算绿电/铁路替代减排量、解读 CBAM 与绿色金融政策、说明核算公式与因子口径。直接提问即可。`
}

function renderText(t: string) {
  return t.replace(/\n/g, '<br/>')
}

async function scrollBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

/** 本地引擎：模拟流式输出 */
async function streamReply(full: string) {
  typing.value = true
  await scrollBottom()
  const chunk = 4
  for (let i = 0; i < full.length; i += chunk) {
    if (!typing.value) return
    await new Promise((r) => setTimeout(r, 16))
  }
  messages.value.push({ role: 'ai', content: full })
  typing.value = false
  await scrollBottom()
}

/** 后端 SSE：/api/v1/enterprise/ai/chat，逐 token 输出，回答结合数据库实况 */
async function backendReply(text: string): Promise<boolean> {
  const token = localStorage.getItem('qnct_token')
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), 60000)
  try {
    const history = messages.value.slice(-7, -1).map((m) => ({
      role: m.role === 'ai' ? 'assistant' : 'user', content: m.content
    }))
    const resp = await fetch('/api/v1/enterprise/ai/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({ message: text, history }),
      signal: ctrl.signal
    })
    if (!resp.ok || !resp.body) return false
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    let answer = ''
    messages.value.push({ role: 'ai', content: '' })
    const liveMsg = messages.value[messages.value.length - 1]
    typing.value = false
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      const lines = buf.split('\n')
      buf = lines.pop() || ''
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        try {
          const obj = JSON.parse(line.slice(5).trim())
          if (obj.delta) {
            answer += obj.delta
            liveMsg.content = answer
            await scrollBottom()
          }
          if (obj.done) {
            modeText.value = obj.live ? 'qwen-plus · 结合数据库实时推理' : '知识库引擎 · 结合数据库实况回答'
          }
        } catch { /* 忽略非 JSON 行 */ }
      }
    }
    if (!answer) {
      messages.value.pop()
      return false
    }
    return true
  } catch {
    return false
  } finally {
    clearTimeout(timer)
  }
}

async function ask(q: string) {
  draft.value = q
  send()
}

async function send() {
  const text = draft.value.trim()
  if (!text || typing.value) return
  messages.value.push({ role: 'user', content: text })
  draft.value = ''
  typing.value = true
  await scrollBottom()

  // 优先走后端（结合数据库数据推理）；后端不可用时回退本地数据引擎
  const ok = await backendReply(text)
  if (!ok) {
    modeText.value = '本地数据引擎 · 基于企业活动数据回答'
    await new Promise((r) => setTimeout(r, 500))
    await streamReply(localAnswer(text))
  }
}
</script>

<style scoped lang="scss">
.ai-page { min-height: calc(100vh - 180px); }
.chat-card {
  display: flex;
  flex: 1;
  height: calc(100vh - 280px);
  min-height: 520px;
  background: var(--qn-card);
  border: 1px solid var(--qn-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--qn-shadow);
}
/* ---- 侧边 ---- */
.chat-side {
  width: 240px;
  flex-shrink: 0;
  padding: 18px 14px;
  background: linear-gradient(180deg, #f7fafc 0%, #f0f5f9 100%);
  border-right: 1px solid var(--qn-border);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.side-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--qn-text); margin-bottom: 10px; }
.q-chip {
  display: block;
  width: 100%;
  text-align: left;
  padding: 9px 12px;
  margin-bottom: 8px;
  font-size: 12.5px;
  color: var(--qn-text-2);
  background: #fff;
  border: 1px solid var(--qn-border);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover { color: var(--qn-primary); border-color: var(--qn-primary); transform: translateX(2px); }
}
.ctx-item { display: flex; gap: 8px; align-items: center; font-size: 12px; color: var(--qn-text-2); margin-bottom: 8px;
  .dot { width: 6px; height: 6px; border-radius: 50%; background: var(--qn-primary); }
}
.side-foot { margin-top: auto; font-size: 11px; color: var(--qn-text-2); line-height: 1.7;
  .dim { color: var(--qn-text-3); }
}
/* ---- 对话 ---- */
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.msg-list { flex: 1; overflow-y: auto; padding: 22px 26px; }
.msg { display: flex; gap: 10px; margin-bottom: 18px; max-width: 80%;
  &.user { margin-left: auto; flex-direction: row-reverse;
    .bubble { background: linear-gradient(120deg, #00b581, #00c9a7); color: #fff; border-radius: 14px 4px 14px 14px; } }
  &.ai .bubble { background: #f2f6fa; color: var(--qn-text); border-radius: 4px 14px 14px 14px; }
}
.bot-avatar { width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 0 2px rgba(0,181,129,.2); }
.bubble { padding: 11px 15px; font-size: 13.5px; line-height: 1.75; white-space: pre-wrap; }
.welcome-text { background: #f2f6fa; padding: 11px 15px; border-radius: 4px 14px 14px 14px; font-size: 13.5px; color: var(--qn-text); line-height: 1.8; }
.typing { display: inline-flex; gap: 4px; padding: 14px 16px;
  span { width: 6px; height: 6px; border-radius: 50%; background: #9db2c8; animation: blink 1.2s infinite;
    &:nth-child(2) { animation-delay: 0.2s; } &:nth-child(3) { animation-delay: 0.4s; } } }
@keyframes blink { 0%, 80%, 100% { opacity: 0.25; } 40% { opacity: 1; } }
.input-bar { display: flex; gap: 12px; padding: 14px 18px; border-top: 1px solid var(--qn-border);
  .send-btn { width: 44px; height: 100%; border-radius: 10px; } }
@media (max-width: 900px) { .chat-side { display: none; } }
</style>