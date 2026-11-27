<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">整改计划生成与预算预估</div>
      <div class="sub">针对初审退回与波动异常事项，AI 辅助生成整改措施清单与责任台账，并按分项预算预估改造投入，形成闭环跟踪</div>
    </div>

    <div class="top-grid">
      <!-- ================= 整改事项 ================= -->
      <div class="page-card">
        <div class="page-card-title">
          <h3>整改事项清单</h3>
          <el-tag size="small" round type="danger">{{ items.filter(i => i.status === '待整改').length }} 项待整改</el-tag>
        </div>
        <el-table :data="items" size="small" highlight-current-row @current-change="(cur) => current = cur">
          <el-table-column prop="no" label="事项编号" width="128">
            <template #default="{ row }"><span class="mono">{{ row.no }}</span></template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="94">
            <template #default="{ row }"><el-tag size="small" :type="row.source === '初审退回' ? 'warning' : 'danger'" round>{{ row.source }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="topic" label="事项" min-width="180" show-overflow-tooltip />
          <el-table-column label="风险" width="70">
            <template #default="{ row }"><span :class="'risk risk-' + row.risk">{{ row.risk }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="84">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === '已闭环' ? 'success' : row.status === '整改中' ? 'primary' : 'danger'" round>{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="108">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openGenerate(row)">生成计划</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="dim-note"><el-icon><InfoFilled /></el-icon>事项来自省厅智能初审台（F6 波动预警）与第三方核查机构退回意见，闭环后自动写入哈希链存证</div>
      </div>

      <!-- ================= 生成的整改计划 ================= -->
      <div class="page-card">
        <div class="page-card-title">
          <h3>整改计划 · {{ current?.no || '未选择' }}</h3>
          <div class="title-actions">
            <el-button size="small" plain :disabled="!current" :loading="genLoading" @click="openGenerate(current)">AI 重新生成</el-button>
            <el-button size="small" type="primary" :disabled="!plan.length" @click="savePlan">保存计划</el-button>
          </div>
        </div>
        <div v-if="!plan.length" class="plan-empty">
          <div class="pe-icon"><el-icon :size="30"><MagicStick /></el-icon></div>
          <div>选择左侧事项后点击「生成计划」</div>
          <div class="pe-sub">AI 将依据事项类型、风险等级与行业案例库生成措施清单</div>
        </div>
        <div v-else class="plan-list">
          <div v-for="(p, i) in plan" :key="i" class="plan-item" :style="{ '--d': i }">
            <div class="pi-seq">{{ i + 1 }}</div>
            <div class="pi-body">
              <div class="pi-text">{{ p.measure }}</div>
              <div class="pi-meta">
                <span class="meta-tag"><el-icon><User /></el-icon>{{ p.dept }}</span>
                <span class="meta-tag"><el-icon><Calendar /></el-icon>{{ p.deadline }}</span>
                <span :class="['pri', 'pri-' + p.pri]">{{ '优先级 ' + p.pri }}</span>
              </div>
            </div>
            <el-switch v-model="p.done" />
          </div>
          <div class="plan-sum">
            <span>总工期：<b>{{ totalDays }} 天</b></span>
            <span>责任部门：<b>{{ deptCount }} 个</b></span>
            <el-progress :percentage="donePct" :stroke-width="8" style="flex:1" />
            <span>{{ donePct }}% 完成</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= 预算预估 ================= -->
    <div class="page-card">
      <div class="page-card-title">
        <h3>整改预算预估（分项估算）</h3>
        <div class="title-actions">
          <el-tag size="small" round type="info">基准价 × 工程量 → 小计 → +10% 预备费</el-tag>
          <el-button size="small" type="primary" plain :disabled="!plan.length" @click="genBudget">按计划生成预算</el-button>
        </div>
      </div>
      <el-table :data="budgetRows" size="small" show-summary :summary-method="summaryRow">
        <el-table-column prop="cat" label="预算科目" min-width="160" />
        <el-table-column prop="basis" label="工程量说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="工程量" width="130">
          <template #default="{ row }"><el-input-number v-model="row.qty" :min="0" size="small" controls-position="right" style="width: 106px" /></template>
        </el-table-column>
        <el-table-column label="参考单价（元）" width="150">
          <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :step="1000" size="small" controls-position="right" style="width: 126px" /></template>
        </el-table-column>
        <el-table-column label="金额（万元）" width="122" align="right">
          <template #default="{ row }"><b class="money">{{ ((row.qty * row.price) / 10000).toFixed(2) }}</b></template>
        </el-table-column>
      </el-table>
      <div class="budget-foot">
        <div class="bf-item"><span>分项小计</span><b>{{ subTotalFmt }} 元</b></div>
        <el-icon class="bf-plus"><Plus /></el-icon>
        <div class="bf-item"><span>不可预见费（10%）</span><b>{{ contingencyFmt }} 元</b></div>
        <el-icon class="bf-plus"><Plus /></el-icon>
        <div class="bf-item total"><span>整改预算总额</span><b>{{ grandFmt }} 元</b></div>
        <el-button type="primary" size="large" :disabled="!plan.length" :loading="submitLoading" @click="submitBudget">提交预算审批</el-button>
      </div>
      <div class="dim-note"><el-icon><InfoFilled /></el-icon>预算依据：行业技改项目基准计价 × 本次整改工程量，人工可调整；提交后推送企业财务线与省厅备案，全流程写入审计日志</div>
    </div>

    <!-- 生成中动画 -->
    <el-dialog v-model="genVisible" title="AI 辅助生成整改计划" width="560px" :close-on-click-modal="false">
      <div class="gen-box">
        <template v-if="genLoading">
          <div class="ai-spin"><el-icon class="rot" :size="34"><MagicStick /></el-icon></div>
          <div class="gen-step">{{ genStep }}</div>
          <div class="gen-sub">模型正在结合事项类型、行业整改案例库与预算基准价生成…</div>
        </template>
        <template v-else>
          <el-result icon="success" :title="'已生成 ' + plan.length + ' 条整改措施'" sub-title="含责任部门、完成时限与优先级，可在右侧微调后保存" />
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, MagicStick, User, Calendar, Plus } from '@element-plus/icons-vue'

/* ============================================================
 * 整改计划生成：正式环境调用后端 POST /api/v1/enterprise/rectify/generate
 * 后端内部调用大模型（LLM）按校验建议生成整改措施：
 *   env：QN_LLM_API_KEY / QN_LLM_BASE_URL / QN_LLM_MODEL=qwen-plus
 *   【此处需填入API Key】——阿里云百炼 DashScope 申请：https://dashscope.console.aliyun.com/
 * 无 Key 时后端走与本页相同的规则模板降级逻辑，保证功能可用。
 * ============================================================ */

interface Issue { no: string; source: string; topic: string; risk: string; status: string }
interface PlanItem { measure: string; dept: string; deadline: string; pri: string; done: boolean }
interface BudgetRow { cat: string; basis: string; qty: number; price: number }

const items = ref<Issue[]>([
  { no: 'ZG-2026-041', source: '初审退回', topic: '上月原煤消耗台账缺磅单佐证，数据完整性不足', risk: '高', status: '待整改' },
  { no: 'ZG-2026-039', source: '波动预警', topic: '范围二购电排放环比 +27.4%，超出 ±20% 阈值', risk: '高', status: '待整改' },
  { no: 'ZG-2026-035', source: '核查退回', topic: '运输因子选用与 CBAM 口径不一致，需补充里程证据链', risk: '中', status: '整改中' },
  { no: 'ZG-2026-030', source: '波动预警', topic: '化验单发热量字段缺失 2 批次，影响 F2 核算参数', risk: '中', status: '已闭环' },
  { no: 'ZG-2026-028', source: '核查退回', topic: '运输承运商 GPS 里程证据链缺失 3 个趟次，需补传电子围栏记录', risk: '中', status: '整改中' },
  { no: 'ZG-2026-026', source: '波动预警', topic: '场内倒运车辆柴油消耗环比 +31%，超出阈值需复核加油台账', risk: '高', status: '待整改' },
  { no: 'ZG-2026-022', source: '初审退回', topic: '外购蒸汽结算单与用热台账不一致，数据口径待统一', risk: '中', status: '已闭环' },
  { no: 'ZG-2026-019', source: '核查退回', topic: '磅房电子汽车衡检定证书过期，计量数据有效性存疑', risk: '高', status: '已闭环' }
])
const current = ref<Issue | null>(null)

const plan = ref<PlanItem[]>([])
const genVisible = ref(false)
const genLoading = ref(false)
const genStep = ref('正在分析事项…')

/* ---------- AI 生成（模拟流式）：按事项类型产出措施模板 ---------- */
const TEMPLATES: Record<string, PlanItem[]> = {
  台账: [
    { measure: '补齐最近 6 个月原煤消耗磅单并逐单校验，重建活动数据三级台账', dept: '碳管理部', deadline: '7 天', pri: '高', done: false },
    { measure: '上线磅房数据自动采集接口，减少人工录入与单据缺失', dept: '信息中心', deadline: '30 天', pri: '中', done: false },
    { measure: '建立月度数据完整性自查机制（填报前 5 项强校验）', dept: '生产运行部', deadline: '14 天', pri: '中', done: false }
  ],
  排放: [
    { measure: '复核外购电发票与电网结算单，按省级因子库 v3.2 重算范围二排放', dept: '碳管理部', deadline: '7 天', pri: '高', done: false },
    { measure: '对高耗能工序加装二级计量电表，实现分环节用电追踪', dept: '设备工程部', deadline: '45 天', pri: '高', done: false },
    { measure: '滚动监测月度用能波动，超过 ±15% 自动触发预警提示', dept: '信息中心', deadline: '21 天', pri: '低', done: false }
  ],
  因子: [
    { measure: '将运输因子切换为 CBAM 认可口径并导出证据链（线路、里程、载重）', dept: '碳管理部', deadline: '7 天', pri: '高', done: false },
    { measure: '与物流承运商签订数据共享协议，按趟次回传 GPS 里程电子围栏数据', dept: '供应链部', deadline: '30 天', pri: '中', done: false },
    { measure: '补充球磨/破碎工序化验单，完善 NCV 参数并复算 F2 排放', dept: '化验室', deadline: '10 天', pri: '中', done: false }
  ],
  默认: [
    { measure: '针对问题开展根因分析，输出《整改分析报告》', dept: '碳管理部', deadline: '15 天', pri: '高', done: false },
    { measure: '修订相关管理制度并组织责任部门培训', dept: '综合管理部', deadline: '30 天', pri: '中', done: false },
    { measure: '整改完成后提交复核材料至省厅初审台', dept: '碳管理部', deadline: '45 天', pri: '低', done: false }
  ]
}
function pickTemplate(topic: string): PlanItem[] {
  if (topic.includes('台账') || topic.includes('缺失') || topic.includes('数据')) return TEMPLATES['台账']
  if (topic.includes('排放') || topic.includes('用电') || topic.includes('波动')) return TEMPLATES['排放']
  if (topic.includes('因子') || topic.includes('口径') || topic.includes('化验')) return TEMPLATES['因子']
  return TEMPLATES['默认']
}

const steps = ['正在分析事项…', '检索行业整改案例库…', '生成措施与责任分工…', '匹配预算基准价…', '整理完成']
function openGenerate(row: Issue | null) {
  if (!row) return
  current.value = row
  genVisible.value = true
  genLoading.value = true
  let i = 0
  const timer = setInterval(() => {
    i++
    if (i >= steps.length) {
      clearInterval(timer)
      plan.value = pickTemplate(row.topic).map(p => ({ ...p }))
      genLoading.value = false
      setTimeout(() => { genVisible.value = false }, 900)
    } else {
      genStep.value = steps[i]
    }
  }, 620)
}
function savePlan() {
  if (current.value) current.value.status = '整改中'
  ElMessage.success('整改计划已保存并指派责任人，进度同步至省厅监管端')
}

/* ---------- 预算预估 ---------- */
const budgetRows = ref<BudgetRow[]>([
  { cat: '设备技改与计量改造', basis: '二级计量电表 18 套 + 磅房采集终端 2 套', qty: 20, price: 8500 },
  { cat: '第三方检测与认证', basis: '化验检测批次 × 样品数', qty: 12, price: 6800 },
  { cat: '咨询与技术服务', basis: 'CBAM 口径咨询人天', qty: 15, price: 3000 },
  { cat: '信息系统改造', basis: '数据采集接口与台账模块（人天）', qty: 25, price: 1800 },
  { cat: '人员培训', basis: '碳管理专题培训场次', qty: 4, price: 6500 }
])

const subTotal = computed(() => budgetRows.value.reduce((s, r) => s + r.qty * r.price, 0))
const contingency = computed(() => subTotal.value * 0.1)
const grandTotal = computed(() => subTotal.value + contingency.value)
const subTotalFmt = computed(() => subTotal.value.toLocaleString())
const contingencyFmt = computed(() => contingency.value.toLocaleString())
const grandFmt = computed(() => grandTotal.value.toLocaleString())

function summaryRow({ columns, data }: { columns: { property?: string }[]; data: BudgetRow[] }) {
  const idx = columns.findIndex(c => c.property === 'cat')
  const amt = data.reduce((s, r) => s + r.qty * r.price, 0)
  return columns.map((_, i) => (i === idx ? '分项小计（不含预备费）' : i === idx + 4 ? (amt / 10000).toFixed(2) + ' 万元' : ''))
}

function genBudget() { ElMessage.success('预算已按整改计划工程量生成，可在表格中人工调整') }
const submitLoading = ref(false)
function submitBudget() {
  submitLoading.value = true
  setTimeout(() => {
    submitLoading.value = false
    ElMessage.success('整改预算已提交审批：总额 ¥' + grandFmt.value + '，流程推送至企业财务线与省厅备案')
  }, 900)
}

/* ---------- 计划统计 ---------- */
const totalDays = computed(() => plan.value.reduce((s, p) => s + parseInt(p.deadline || '0', 10) * (p.pri === '高' ? 1 : 0.3), 0).toFixed(0))
const deptCount = computed(() => new Set(plan.value.map(p => p.dept)).size)
const donePct = computed(() => (plan.value.length ? Math.round((plan.value.filter(p => p.done).length / plan.value.length) * 100) : 0))
</script>

<style scoped lang="scss">
.top-grid { display: grid; grid-template-columns: 1.15fr 1fr; gap: 16px; align-items: start; }
.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #4d6c89; }
.risk { font-weight: 700; &.risk-高 { color: #e04635; } &.risk-中 { color: #e69b2e; } }
.title-actions { display: flex; align-items: center; gap: 10px; }
.dim-note { margin-top: 12px; font-size: 11px; color: var(--qn-text-3); display: flex; align-items: center; gap: 5px; }

.plan-empty { text-align: center; padding: 46px 0; color: var(--qn-text-2); font-size: 13px;
  .pe-icon { width: 58px; height: 58px; margin: 0 auto 14px; border-radius: 16px; display: grid; place-items: center; color: var(--qn-primary); background: rgba(0,181,129,0.1); }
  .pe-sub { margin-top: 6px; font-size: 11.5px; color: var(--qn-text-3); } }
.plan-list { display: flex; flex-direction: column; gap: 10px; }
.plan-item { display: flex; gap: 12px; align-items: center; padding: 12px 14px; border: 1px solid var(--qn-border); border-radius: 12px; background: #fbfcfe;
  animation: item-in .45s calc(var(--d) * 90ms) ease both; transition: box-shadow .2s;
  &:hover { box-shadow: var(--qn-shadow); } }
@keyframes item-in { from { opacity: 0; transform: translateX(14px); } to { opacity: 1; transform: none; } }
.pi-seq { width: 26px; height: 26px; border-radius: 8px; flex-shrink: 0; display: grid; place-items: center; font-size: 12.5px; font-weight: 700; color: #fff; background: linear-gradient(135deg, #00b581, #00d1ff); }
.pi-body { flex: 1; min-width: 0; }
.pi-text { font-size: 13px; color: var(--qn-text); line-height: 1.5; }
.pi-meta { margin-top: 6px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap;
  .meta-tag { font-size: 11px; color: var(--qn-text-2); display: inline-flex; align-items: center; gap: 4px; }
  .pri { font-size: 10.5px; padding: 1px 8px; border-radius: 10px;
    &.pri-高 { color: #e04635; background: #fff1f0; }
    &.pri-中 { color: #e69b2e; background: #fff7e8; }
    &.pri-低 { color: #4b8bbd; background: #eef6fc; } } }
.plan-sum { display: flex; align-items: center; gap: 14px; padding: 12px 8px 2px; font-size: 12px; color: var(--qn-text-2);
  b { color: var(--qn-primary); font-size: 14px; letter-spacing: 0.5px; } }

.money { color: var(--qn-primary); font-variant-numeric: tabular-nums; }
.budget-foot { margin-top: 16px; display: flex; align-items: center; gap: 14px; flex-wrap: wrap; padding: 14px 18px; border-radius: 12px; background: #f6faf8; border: 1px solid #e2f0ea; }
.bf-item { min-width: 150px; span { display: block; font-size: 11.5px; color: var(--qn-text-3); } b { font-size: 18px; color: var(--qn-text); font-variant-numeric: tabular-nums; }
  &.total { min-width: 190px; b { color: var(--qn-primary); font-size: 21px; } } }
.bf-plus { color: #9ec8bb; }
.gen-box { text-align: center; padding: 10px 0 18px; }
.ai-spin { display: inline-flex; padding: 18px; border-radius: 50%; background: linear-gradient(135deg, rgba(0,181,129,0.12), rgba(0,209,255,0.12)); border: 1px dashed var(--qn-primary); }
.rot { animation: rot 1.2s linear infinite; color: var(--qn-primary); }
@keyframes rot { to { transform: rotate(360deg); } }
.gen-step { margin-top: 16px; font-size: 15px; font-weight: 700; color: var(--qn-text); }
.gen-sub { margin-top: 8px; font-size: 12px; color: var(--qn-text-3); }
@media (max-width: 1150px) { .top-grid { grid-template-columns: 1fr; } }
</style>