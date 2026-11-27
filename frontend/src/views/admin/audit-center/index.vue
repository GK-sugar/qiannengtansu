<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">全链路操作审计与可信存证中心</div>
      <div class="sub">全量操作日志追踪 · 数据变更快照比对 · SHA-256 哈希校验预警——谁在几点改了啥、改前改后值、哈希指纹是否失真，一屏闭环</div>
    </div>

    <div class="stat-grid">
      <StatCard label="今日操作总量" :value="2417" icon="DataLine" tint="#00b581" trend="+12.4%" />
      <StatCard label="审计覆盖率" :value="100" suffix="%" icon="CircleCheck" tint="#4da3ff" extra="全量行为落库 sys_audit_log" />
      <StatCard label="存证区块高度" :value="94128" icon="Link" tint="#a06bff" extra="哈希链连续无断裂" />
      <StatCard label="异常预警" :value="alarmCount" icon="Warning" tint="#ff6b6b" :trend="alarmCount ? '+2' : '0'" :trend-type="alarmCount ? 'up' : 'down'" extra="SHA-256 指纹比对" />
    </div>

    <div class="page-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="全量操作日志追踪" name="log" />
        <el-tab-pane label="数据变更快照比对" name="diff" />
        <el-tab-pane :label="'SHA-256 哈希校验预警' + (alarmCount ? ' (' + alarmCount + ')' : '')" name="hash" />
      </el-tabs>

      <!-- ============ Tab 1：操作日志 ============ -->
      <div v-if="tab === 'log'" class="pane">
        <div class="toolbar">
          <el-select v-model="module" placeholder="全部模块" clearable style="width: 150px">
            <el-option v-for="m in ['护照签发', '因子库', '初审', 'RBAC', '登录', '核算']" :key="m" :label="m" :value="m" />
          </el-select>
          <el-input v-model="kw" placeholder="搜索操作人 / 对象" clearable style="width: 210px" :prefix-icon="Search" />
          <el-date-picker v-model="range" type="daterange" style="width: 260px" placeholder="时间范围" />
          <el-button plain size="small" @click="exportLog"><el-icon><Download /></el-icon>导出</el-button>
        </div>
        <el-table :data="filtered" size="small">
          <el-table-column prop="time" label="时间" width="150" />
          <el-table-column prop="operator" label="操作人" width="128">
            <template #default="{ row }"><div class="op-cell"><span class="op-avatar">{{ row.operator[0] }}</span>{{ row.operator }}</div></template>
          </el-table-column>
          <el-table-column prop="module" label="模块" width="96">
            <template #default="{ row }"><el-tag size="small" round type="info">{{ row.module }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="action" label="操作行为" min-width="170" />
          <el-table-column label="前后值差异" min-width="240">
            <template #default="{ row }">
              <div class="diff-cell" v-if="row.before !== '—'">
                <span class="old mono">{{ row.before }}</span>
                <el-icon><Right /></el-icon>
                <span class="new mono">{{ row.after }}</span>
              </div>
              <span v-else class="mono dim">{{ row.after }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP" width="128"><template #default="{ row }"><span class="mono">{{ row.ip }}</span></template></el-table-column>
          <el-table-column label="风险" width="76">
            <template #default="{ row }"><el-tag size="small" :type="row.risk === '高' ? 'danger' : row.risk === '中' ? 'warning' : 'success'" round>{{ row.risk }}</el-tag></template>
          </el-table-column>
          <el-table-column label="快照" width="84">
            <template #default="{ row }"><el-button v-if="row.chgId" link type="primary" size="small" @click="openDiff(row.chgId)">比对</el-button></template>
          </el-table-column>
        </el-table>
      </div>

      <!-- ============ Tab 2：变更快照比对 ============ -->
      <div v-if="tab === 'diff'" class="pane">
        <div class="toolbar">
          <span class="dim">选择一次变更记录，逐字段对比「修改前 / 修改后」：</span>
          <el-select v-model="curChange" placeholder="选择变更记录" style="width: 420px">
            <el-option v-for="c in changes" :key="c.id" :label="c.time + ' · ' + c.operator + ' · ' + c.module" :value="c.id" />
          </el-select>
        </div>
        <div class="snap-grid" v-if="cur">
          <div class="snap-card before">
            <div class="sc-head"><el-icon><Clock /></el-icon>修改前快照<span class="sc-tag">before</span></div>
            <div class="field-row" v-for="(v, k) in cur.before" :key="'b' + k" :class="{ changed: isChanged(k) }">
              <span class="f-key">{{ k }}</span><span class="f-val">{{ formatVal(v) }}</span>
              <span class="f-flag" v-if="isChanged(k)">已变更</span>
            </div>
          </div>
          <div class="snap-arrow"><el-icon :size="22"><Right /></el-icon><span>{{ changedCount }} 个字段发生变化</span></div>
          <div class="snap-card after">
            <div class="sc-head"><el-icon><CircleCheck /></el-icon>修改后快照<span class="sc-tag">after</span></div>
            <div class="field-row" v-for="(v, k) in cur.after" :key="'a' + k" :class="{ changed: isChanged(k) }">
              <span class="f-key">{{ k }}</span><span class="f-val">{{ formatVal(v) }}</span>
              <span class="f-flag" v-if="isChanged(k)">{{ arrowSign(k) }}</span>
            </div>
          </div>
        </div>
        <div class="dim-note"><el-icon><InfoFilled /></el-icon>快照在 MySQL 中以 JSON 双份存储（before / after），并同步写入 sys_hash_chain（F10：h<sub>i</sub> = SHA256(h<sub>i−1</sub> ‖ data<sub>i</sub> ‖ salt)），供第三页哈希校验与公开验真引用</div>
      </div>

      <!-- ============ Tab 3：SHA-256 哈希校验预警 ============ -->
      <div v-if="tab === 'hash'" class="pane">
        <div class="alarm-strip" v-if="alarmList.length">
          <div class="alarm-item" v-for="a in alarmList" :key="a.name">
            <el-icon><Warning /></el-icon>
            <b>{{ a.name }}</b>哈希指纹与链上存证不一致，疑似被篡改
          </div>
        </div>
        <div class="toolbar">
          <el-button type="primary" :loading="checking" @click="checkAll"><el-icon><Refresh /></el-icon>一键哈希校验（全量重算）</el-button>
          <el-button plain @click="checkAll(false)">仅本页核心数据</el-button>
          <span class="dim" v-if="lastCheck">最近校验：{{ lastCheck }} · 重算方式：SHA-256(数据快照 JSON ‖ 盐)</span>
        </div>
        <el-table :data="resources" size="small" v-loading="checking">
          <el-table-column prop="name" label="核心数据资源" min-width="190" />
          <el-table-column prop="snap" label="数据快照摘要" min-width="210" show-overflow-tooltip />
          <el-table-column label="链上存证哈希" min-width="210">
            <template #default="{ row }"><span class="mono hash-text">{{ shortHash(row.chainHash) }}</span></template>
          </el-table-column>
          <el-table-column label="现算哈希指纹" min-width="210">
            <template #default="{ row }"><span class="mono hash-text" :class="{ red: row.status === 'bad' }">{{ shortHash(row.nowHash) }}</span></template>
          </el-table-column>
          <el-table-column label="校验结果" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'bad' ? 'danger' : 'success'" round>{{ row.status === 'bad' ? '异常 · 报警' : '一致 · 正常' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="checkOne(row)">重算</el-button>
              <el-button v-if="row.demo" link type="danger" size="small" @click="toggleTamper(row)">{{ row.tampered ? '还原' : '模拟篡改' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download, Right, Clock, CircleCheck, InfoFilled, Warning, Refresh } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'

const tab = ref('log')

/* ================= Tab1 操作日志 ================= */
interface AuditRow { time: string; operator: string; module: string; action: string; before: string; after: string; ip: string; risk: string; chgId?: string }
const module = ref('')
const kw = ref('')
const range = ref<[Date, Date] | null>(null)
const logs: AuditRow[] = [
  { time: '2026-11-26 10:42:18', operator: '系统管理员', module: '护照签发', action: '签发护照 QN-PP-2026-0041', before: '状态：待签发', after: '状态：已签发 / 哈希 +1', ip: '10.20.1.9', risk: '低', chgId: 'c4' },
  { time: '2026-11-26 10:15:07', operator: '李处长', module: '因子库', action: '更新电力因子 E-GRID-SC', before: '因子值 0.5810', after: '因子值 0.5703', ip: '10.10.3.21', risk: '中', chgId: 'c3' },
  { time: '2026-11-26 09:58:31', operator: '系统管理员', module: 'RBAC', action: '为用户 gov2 绑定角色', before: '角色：省厅审批员', after: '角色：省厅审批员, 因子库管理员', ip: '10.20.1.9', risk: '中', chgId: 'c2' },
  { time: '2026-11-26 09:31:44', operator: '张副处', module: '初审', action: '通过企业核算初审', before: '状态：待初审', after: '状态：已通过 · 待签章', ip: '10.10.3.28', risk: '低', chgId: 'c1' },
  { time: '2026-11-26 09:12:02', operator: '王总', module: '核算', action: '修正 2026-10 用电量', before: '用电量 8,420,000 kWh', after: '用电量 8,619,500 kWh', ip: '223.104.19.6', risk: '高', chgId: 'c0' },
  { time: '2026-11-26 08:55:19', operator: '未知', module: '登录', action: '尝试以 admin 登录失败 3 次', before: '—', after: '账号锁定 15 分钟', ip: '185.220.101.37', risk: '高' },
  { time: '2026-11-26 08:40:52', operator: '陈主任', module: '护照签发', action: '复核护照 QN-PP-2026-0040（中伟新材料）', before: '状态：待复核', after: '状态：复核通过', ip: '10.10.3.12', risk: '低', chgId: 'c5' },
  { time: '2026-11-26 08:22:36', operator: '李处长', module: '因子库', action: '新增运输因子 T-RAIL-GZ', before: '—', after: '因子值 0.010 tCO₂e/(t·100km)', ip: '10.10.3.21', risk: '中' },
  { time: '2026-11-25 17:48:03', operator: '张副处', module: '初审', action: '退回磷化集团核算报告（活动数据缺凭证）', before: '状态：待初审', after: '状态：已退回 · 待补正', ip: '10.10.3.28', risk: '中', chgId: 'c6' },
  { time: '2026-11-25 16:30:27', operator: '王总', module: '核算', action: '提交 2026-Q3 CBAM 核算报告', before: '—', after: '报告 QN-BG-2026Q3-031 已提交', ip: '223.104.19.6', risk: '低' },
  { time: '2026-11-25 15:05:44', operator: '系统管理员', module: 'RBAC', action: '停用离职账号 agency3', before: '状态：启用', after: '状态：停用', ip: '10.20.1.9', risk: '中' },
  { time: '2026-11-25 11:26:18', operator: '高工', module: '初审', action: '完成遵义铝业国际标准复核', before: '状态：复核中', after: '状态：复核通过', ip: '10.30.2.15', risk: '低' }
]
const filtered = computed(() => logs.filter(r => (!module.value || r.module === module.value) && (!kw.value || r.operator.includes(kw.value) || r.action.includes(kw.value))))

function exportLog() {
  const head = ['时间', '操作人', '模块', '操作行为', '修改前', '修改后', 'IP', '风险']
  const body = filtered.value.map(r => [r.time, r.operator, r.module, r.action, r.before, r.after, r.ip, r.risk])
  const csv = '\uFEFF' + [head, ...body].map(row => row.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `审计日志_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('操作日志已导出：' + a.download + '（共 ' + filtered.value.length + ' 条，含当前筛选条件）')
}

/* ================= Tab2 快照比对 ================= */
interface Chg { id: string; time: string; operator: string; module: string; note: string; before: Record<string, string | number>; after: Record<string, string | number> }
const changes: Chg[] = [
  {
    id: 'c0', time: '2026-11-26 09:12', operator: '王总', module: '碳数据填报', note: '修正 2026-10 用电量',
    before: { 核算期: '2026-10', 用电量_kWh: 8420000, 操作人: '王总', 状态: '已提交', 备注: '' },
    after: { 核算期: '2026-10', 用电量_kWh: 8619500, 操作人: '王总', 状态: '已提交', 备注: '补录 2# 车间电表' }
  },
  {
    id: 'c1', time: '2026-11-26 09:31', operator: '张副处', module: '智能初审', note: '通过企业核算初审',
    before: { 企业: '贵州黔能矿业有限责任公司', 审核状态: '待初审', 波动率: '+14.7%', 初审意见: '' },
    after: { 企业: '贵州黔能矿业有限责任公司', 审核状态: '已通过', 波动率: '+14.7%', 初审意见: '波动在阈值内，通过' }
  },
  {
    id: 'c2', time: '2026-11-26 09:58', operator: '系统管理员', module: 'RBAC', note: '为用户 gov2 绑定角色',
    before: { 用户: 'gov2', 绑定角色: '省厅审批员', 数据范围: '全省', 生效时间: '立即' },
    after: { 用户: 'gov2', 绑定角色: '省厅审批员, 因子库管理员', 数据范围: '全省', 生效时间: '立即' }
  },
  {
    id: 'c3', time: '2026-11-26 10:15', operator: '李处长', module: '因子库', note: '更新电力因子',
    before: { 因子编码: 'E-GRID-SC', 因子值: 0.581, 单位: 'tCO₂e/MWh', 版本: 'v3.1', 状态: '启用' },
    after: { 因子编码: 'E-GRID-SC', 因子值: 0.5703, 单位: 'tCO₂e/MWh', 版本: 'v3.2', 状态: '启用' }
  },
  {
    id: 'c4', time: '2026-11-26 10:42', operator: '陈主任', module: '护照签发', note: '签发碳足迹护照',
    before: { 护照编号: '待生成', 签发状态: '复核通过', 碳足迹: 0.94, 区块高度: 94127 },
    after: { 护照编号: 'QN-PP-2026-0041', 签发状态: '已签发', 碳足迹: 0.94, 区块高度: 94128 }
  },
  {
    id: 'c5', time: '2026-11-26 08:40', operator: '陈主任', module: '护照签发', note: '复核中伟新材料护照',
    before: { 护照编号: 'QN-PP-2026-0040', 复核状态: '待复核', 碳足迹: 12.68, 复核意见: '' },
    after: { 护照编号: 'QN-PP-2026-0040', 复核状态: '复核通过', 碳足迹: 12.68, 复核意见: '数据链完整，同意签发' }
  },
  {
    id: 'c6', time: '2026-11-25 17:48', operator: '张副处', module: '智能初审', note: '退回磷化集团核算报告',
    before: { 企业: '贵州磷化集团有限责任公司', 审核状态: '待初审', 波动率: '+3.2%', 初审意见: '' },
    after: { 企业: '贵州磷化集团有限责任公司', 审核状态: '已退回', 波动率: '+3.2%', 初审意见: '磷矿石消耗缺化验凭证，请补正' }
  }
]
const curChange = ref('c0')
const cur = computed(() => changes.find(c => c.id === curChange.value) ?? changes[0])
function isChanged(k: string): boolean {
  if (!cur.value) return false
  return String(cur.value.before[k]) !== String(cur.value.after[k])
}
const changedCount = computed(() => (cur.value ? Object.keys(cur.value.before).filter(isChanged).length : 0))
function formatVal(v: string | number) { return typeof v === 'number' ? v.toLocaleString() : v || '—' }
function arrowSign(k: string) {
  if (!cur.value) return ''
  const b = String(cur.value.before[k]); const a = String(cur.value.after[k])
  if (typeof cur.value.before[k] === 'number' && typeof cur.value.after[k] === 'number') {
    return Number(a) >= Number(b) ? '↑ 增加' : '↓ 减少'
  }
  return '内容修改'
}
function openDiff(id: string) { curChange.value = id; tab.value = 'diff' }

/* ================= Tab3 哈希校验 ================= */
interface Res { name: string; snap: string; chainHash: string; nowHash: string; status: 'ok' | 'bad'; demo?: boolean; tampered?: boolean }
const resources = ref<Res[]>([
  { name: '企业核算结果 · QN-R-202610-031', snap: '{"period":"2026-10","total":12864,"elec_kWh":8619500}', chainHash: '', nowHash: '', status: 'ok', demo: true, tampered: false },
  { name: '碳足迹护照 · QN-PP-2026-0041', snap: '{"passportNo":"QN-PP-2026-0041","carbon":0.94,"standard":"ISO 14067"}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '审批意见书 · QN-YJ-2026-118', snap: '{"docNo":"QN-YJ-2026-118","conclusion":"pass","seal":"gov-seal"}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '省级因子库 · 版本 v3.2', snap: '{"ver":"v3.2","items":132,"airport":[{"E-GRID-SC":0.5703}]}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '授信申请单 · GL-2026-0117', snap: '{"score":87,"quota":12000000,"enterpriseId":2384}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '核查任务 · T-2026-118', snap: '{"taskNo":"T-2026-118","company":"贵州黔能矿业有限责任公司","radius_m":500}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '现场打卡存证 · QN-CK-2026-0342', snap: '{"taskNo":"T-2026-118","lat":26.5982,"lng":106.7076,"inRange":true}', chainHash: '', nowHash: '', status: 'ok' },
  { name: '整改通知单 · QN-ZG-2026-021', snap: '{"docNo":"QN-ZG-2026-021","enterprise":"遵义铝业股份有限公司","deadline":"2026-12-10"}', chainHash: '', nowHash: '', status: 'ok' }
])
const checking = ref(false)
const lastCheck = ref('')

async function sha256Hex(text: string): Promise<string> {
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))
  return Array.from(new Uint8Array(buf), x => x.toString(16).padStart(2, '0')).join('')
}
function shortHash(h: string) { return h ? '0x' + h.slice(0, 18) + '…' + h.slice(-10) : '…' }

async function checkOne(row: Res) {
  row.nowHash = await sha256Hex(row.snap + '||qnct-site-salt')
  row.status = row.nowHash === row.chainHash ? 'ok' : 'bad'
}
async function checkAll(msg = true) {
  checking.value = true
  for (const r of resources.value) await checkOne(r)
  checking.value = false
  lastCheck.value = new Date().toLocaleString('zh-CN', { hour12: false })
  if (msg) ElMessage.success('全量哈希校验完成：' + resources.value.filter(r => r.status === 'bad').length + ' 项异常')
}

async function toggleTamper(row: Res) {
  row.tampered = !row.tampered
  if (row.tampered) {
    // 模拟第三方悄悄改写链上存证哈希 → 现算指纹必然不一致，触发报警
    row.chainHash = 'ff' + Array.from({ length: 62 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')
    await checkOne(row)
  } else {
    row.chainHash = await sha256Hex(row.snap + '||qnct-site-salt')
    row.nowHash = row.chainHash
    row.status = 'ok'
  }
}

const alarmList = computed(() => resources.value.filter(r => r.status === 'bad'))
const alarmCount = computed(() => alarmList.value.length)

onMounted(async () => {
  for (const r of resources.value) {
    r.chainHash = await sha256Hex(r.snap + '||qnct-site-salt')
    r.nowHash = r.chainHash
  }
  lastCheck.value = new Date().toLocaleString('zh-CN', { hour12: false })
})
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.pane { padding-top: 8px; }
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-bottom: 14px;
  .dim { font-size: 11.5px; color: var(--qn-text-3); } }
.op-cell { display: flex; align-items: center; gap: 8px; }
.op-avatar { width: 26px; height: 26px; border-radius: 50%; background: linear-gradient(135deg, #00b581, #00d1ff); color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; }
.diff-cell { display: flex; align-items: center; gap: 6px; font-size: 11.5px;
  .old { color: #f56c6c; background: #fff1f0; padding: 2px 6px; border-radius: 4px; }
  .new { color: #00a97e; background: #eefaf5; padding: 2px 6px; border-radius: 4px; } }
.mono { font-family: ui-monospace, Consolas, monospace; font-size: 11.5px; &.dim { color: var(--qn-text-3); } &.red { color: #e04635; } }
.hash-text { font-size: 11.5px; color: #4d6c89; word-break: break-all; }

.snap-grid { display: grid; grid-template-columns: 1fr 130px 1fr; gap: 14px; align-items: center; }
.snap-card { border: 1px solid var(--qn-border); border-radius: 14px; padding: 16px 18px; background: #fbfcfe;
  &.before { border-color: #ffd6d2; background: #fffafa; }
  &.after { border-color: #bfe9da; background: #f8fdfb; } }
.sc-head { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 700; color: var(--qn-text); margin-bottom: 12px;
  .before & .el-icon { color: #f56c6c; }
  .after & .el-icon { color: #00b581; } }
.sc-tag { margin-left: auto; font-size: 10.5px; font-family: ui-monospace, monospace; padding: 1px 8px; border-radius: 10px;
  .before & { background: #fff1f0; color: #e04635; }
  .after & { background: #eafaf4; color: #009c6d; } }
.field-row { display: flex; align-items: baseline; gap: 10px; padding: 8px 10px; border-radius: 8px; font-size: 12.5px; margin-bottom: 4px; background: rgba(255,255,255,0.6);
  &.changed { background: rgba(255,205,80,0.16); box-shadow: inset 2px 0 0 #f5a623; } }
.f-key { width: 96px; flex-shrink: 0; color: var(--qn-text-3); font-size: 11.5px; }
.f-val { flex: 1; color: var(--qn-text); font-variant-numeric: tabular-nums; word-break: break-all; }
.f-flag { font-size: 10.5px; padding: 0 8px; border-radius: 8px; background: #fff3df; color: #b8790a; }
.snap-arrow { text-align: center; color: var(--qn-primary); display: flex; flex-direction: column; align-items: center; gap: 6px;
  span { font-size: 11px; color: var(--qn-text-3); } }
.dim-note { margin-top: 12px; font-size: 11px; color: var(--qn-text-3); display: flex; align-items: center; gap: 5px; }

.alarm-strip { display: flex; flex-direction: column; gap: 8px; margin-bottom: 14px; }
.alarm-item { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: #e04635; background: #fff5f4; border: 1px solid #ffd6d2; border-radius: 10px; padding: 9px 14px;
  b { color: #b63020; } }
@media (max-width: 1100px) { .stat-grid { grid-template-columns: 1fr 1fr; } .snap-grid { grid-template-columns: 1fr; } }
</style>