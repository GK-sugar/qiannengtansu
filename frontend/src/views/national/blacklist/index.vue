<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">黑名单通报系统</div>
      <div class="sub">涉嫌数据造假、违规核算的企业纳入全国黑名单：冻结权限、跨省通报、留痕审计。全国 / 省 / 市三级监管端同步可见</div>
      <div class="banner-actions">
        <el-button type="danger" :icon="Plus" @click="addVisible = true">新增通报</el-button>
        <el-button :icon="Refresh" @click="load()">刷新名单</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="已冻结企业" :value="stat.frozen" icon="Lock" tint="#ff5f56" suffix=" 家" extra="全国权限冻结" :animated="false" />
      <StatCard label="观察期企业" :value="stat.observing" icon="View" tint="#ffb020" suffix=" 家" extra="限期整改跟踪" :animated="false" />
      <StatCard label="已通报" :value="stat.notified" icon="BellFilled" tint="#00d1ff" suffix=" 家" extra="跨省同步" :animated="false" />
      <StatCard label="冻结资产影响" :value="stat.amount" icon="Money" tint="#a06bff" prefix="¥" suffix=" 万" extra="涉及补贴与金融授信" />
    </div>

    <div class="page-card">
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column label="违规企业" min-width="190">
          <template #default="{ row }">
            <div class="ent-cell">
              <el-avatar :size="28" class="ent-avatar">{{ row.entName.slice(0, 1) }}</el-avatar>
              <div>
                <div class="ent-name">{{ row.entName }}</div>
                <div class="dim mono">{{ row.evidenceNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="违规事项" min-width="250" show-overflow-tooltip />
        <el-table-column label="严重级别" width="110">
          <template #default="{ row }">
            <el-tag :type="row.severity === 'HIGH' ? 'danger' : 'warning'" size="small" round effect="dark">{{ row.severity === 'HIGH' ? '重大违规' : '一般违规' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处置状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.penaltyStatus)" size="small" round>{{ statusText(row.penaltyStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="证据来源" width="110">
          <template #default="{ row }"><span class="dim">{{ row.source }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="通报时间" width="130" />
        <el-table-column label="处置操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.penaltyStatus !== 'FROZEN'" link type="danger" size="small" @click="freeze(row)">冻结权限</el-button>
            <el-button v-else link type="success" size="small" @click="release(row)">解除冻结</el-button>
            <el-button link type="primary" size="small" @click="detail(row)">通报详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="note-row">
        <el-icon><Warning /></el-icon>
        <span>冻结操作写入全链路操作审计与可信存证中心（SHA-256 指纹留痕），解除冻结自动转为观察期。</span>
      </div>
    </div>

    <!-- 新增通报 -->
    <el-dialog v-model="addVisible" title="新增全国黑名单通报" width="560px">
      <el-form label-width="100px">
        <el-form-item label="企业名称"><el-input v-model="form.entName" placeholder="如：贵州某焦化有限公司" /></el-form-item>
        <el-form-item label="违规事项"><el-input v-model="form.reason" type="textarea" :rows="3" placeholder="描述违规事实（核算造假 / 数据篡改等）" /></el-form-item>
        <el-form-item label="严重级别">
          <el-radio-group v-model="form.severity">
            <el-radio value="HIGH">重大违规</el-radio>
            <el-radio value="MEDIUM">一般违规</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="证据来源">
          <el-select v-model="form.source" style="width: 100%">
            <el-option label="数据核查" value="数据核查" />
            <el-option label="海关协查" value="海关协查" />
            <el-option label="匿名举报" value="匿名举报" />
            <el-option label="第三方审计" value="第三方审计" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="danger" @click="submitAdd">确认通报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import StatCard from '@/components/StatCard.vue'

interface Row { id?: number; entName: string; reason: string; severity: string; penaltyStatus: string; source: string; evidenceNo: string; createTime: string }

const list = ref<Row[]>([])
const loading = ref(false)
const addVisible = ref(false)
const form = reactive({ entName: '', reason: '', severity: 'HIGH', source: '数据核查' })
const stat = computed(() => ({
  frozen: list.value.filter((r) => r.penaltyStatus === 'FROZEN').length,
  observing: list.value.filter((r) => r.penaltyStatus === 'OBSERVING').length,
  notified: list.value.filter((r) => r.penaltyStatus === 'NOTIFIED').length,
  amount: list.value.filter((r) => r.penaltyStatus === 'FROZEN').length * 860
}))

const demo: Row[] = [
  { id: 1, entName: '贵州某焦化有限公司', reason: '核算边界造假：原料煤耗重复计入、运输因子选用不当', severity: 'HIGH', penaltyStatus: 'FROZEN', source: '海关协查', evidenceNo: 'EV-2026-0917', createTime: '2026-11-18' },
  { id: 2, entName: '黔中某铝业有限公司', reason: '化验单热值字段与原始记录不一致（疑似篡改）', severity: 'HIGH', penaltyStatus: 'NOTIFIED', source: '数据核查', evidenceNo: 'EV-2026-0871', createTime: '2026-11-12' },
  { id: 3, entName: '某磷化工企业', reason: '第三方核查期内提供不完整运行台账', severity: 'MEDIUM', penaltyStatus: 'OBSERVING', source: '匿名举报', evidenceNo: 'EV-2026-0755', createTime: '2026-11-03' },
  { id: 4, entName: '黔北某水泥有限公司', reason: '熟料产量少报 12%，过程排放核算基数失真', severity: 'HIGH', penaltyStatus: 'FROZEN', source: '第三方审计', evidenceNo: 'EV-2026-0702', createTime: '2026-10-27' },
  { id: 5, entName: '黔南某铁合金有限公司', reason: '外购电发票与电网结算单差异超 8%，未按期补正', severity: 'MEDIUM', penaltyStatus: 'OBSERVING', source: '数据核查', evidenceNo: 'EV-2026-0668', createTime: '2026-10-19' },
  { id: 6, entName: '黔西某煤矿企业', reason: '瓦斯抽采利用量虚报，CH₄ 逸散排放漏算', severity: 'HIGH', penaltyStatus: 'NOTIFIED', source: '匿名举报', evidenceNo: 'EV-2026-0611', createTime: '2026-10-08' },
  { id: 7, entName: '贵阳某物流园运营公司', reason: '运输周转量台账与 GPS 轨迹批量不符', severity: 'MEDIUM', penaltyStatus: 'OBSERVING', source: '数据核查', evidenceNo: 'EV-2026-0542', createTime: '2026-09-24' },
  { id: 8, entName: '黔东某锰业有限公司', reason: '出口报告碳强度与备案核算结果不一致，涉嫌双套账', severity: 'HIGH', penaltyStatus: 'FROZEN', source: '海关协查', evidenceNo: 'EV-2026-0487', createTime: '2026-09-15' },
  { id: 9, entName: '安顺某化工有限公司', reason: '逾期未提交年度核算报告且拒不配合线下核查', severity: 'MEDIUM', penaltyStatus: 'NOTIFIED', source: '第三方审计', evidenceNo: 'EV-2026-0433', createTime: '2026-09-02' }
]

function statusType(s: string) { return s === 'FROZEN' ? 'danger' : s === 'OBSERVING' ? 'warning' : 'info' }
function statusText(s: string) { return s === 'FROZEN' ? '已冻结' : s === 'OBSERVING' ? '观察期' : '已通报' }

async function load() {
  loading.value = true
  try {
    const r = (await request.get('/v1/national/blacklist/list')) as any
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) list.value = r.data
    else list.value = demo
  } catch { list.value = demo }
  loading.value = false
}
load()

async function freeze(row: Row) {
  await ElMessageBox.confirm('冻结后该企业将被禁止提交核算数据，并同步全国 / 省 / 市三级监管端。确认冻结？', '冻结权限', { type: 'warning', confirmButtonText: '确认冻结', cancelButtonText: '取消' })
  try { await request.post('/v1/national/blacklist/freeze', { id: row.id, entName: row.entName }) } catch { /* 本地演示 */ }
  row.penaltyStatus = 'FROZEN'
  ElMessage.error('已冻结：' + row.entName + '（存证指纹已写入审计中心）')
}
async function release(row: Row) {
  await ElMessageBox.confirm('解除冻结后企业转为观察期，继续接受跟踪核查。确认解除？', '解除冻结', { type: 'info', confirmButtonText: '确认解除', cancelButtonText: '取消' })
  try { await request.post('/v1/national/blacklist/release', { id: row.id, entName: row.entName }) } catch { /* 本地演示 */ }
  row.penaltyStatus = 'OBSERVING'
  ElMessage.success('已解除冻结，转为观察期：' + row.entName)
}
function submitAdd() {
  if (!form.entName || !form.reason) { ElMessage.warning('请填写企业名称与违规事项'); return }
  const row: Row = {
    entName: form.entName, reason: form.reason, severity: form.severity, penaltyStatus: 'NOTIFIED',
    source: form.source, evidenceNo: 'EV-2026-' + Math.floor(1000 + Math.random() * 9000), createTime: '2026-11-26'
  }
  list.value.unshift(row)
  try { request.post('/v1/national/blacklist/add', { ...row }) } catch { /* 本地演示 */ }
  addVisible.value = false
  ElMessage.success('通报已签发并同步三级监管端')
}
function detail(row: Row) { ElMessage.info('证据链：' + row.evidenceNo + ' · ' + row.source + '（详见全链路审计中心）') }
</script>

<style scoped lang="scss">
.banner-actions { margin-top: 16px; display: flex; gap: 12px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.note-row { margin-top: 14px; display: flex; gap: 8px; align-items: center; font-size: 12.5px; color: var(--qn-text-3); }
.mono { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 11.5px; }
.dim { color: var(--qn-text-3); font-size: 11.5px; }
.ent-cell { display: flex; align-items: center; gap: 10px; }
.ent-avatar { background: linear-gradient(135deg, #ff5f56, #a06bff); color: #fff; font-size: 13px; flex-shrink: 0; }
.ent-name { font-weight: 600; }
</style>