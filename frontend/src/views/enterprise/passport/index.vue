<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">碳足迹护照</div>
      <div class="sub">按「填报 → 省审 → 复核 → 终审 → 发证」五级状态机流转，全程哈希链留痕；签发后生成防伪二维码，扫码直达公开验真页（F11）</div>
    </div>

    <div class="pp-grid">
      <!-- 已签发护照卡 -->
      <div class="issued-card">
        <div class="ic-ribbon">CARBON FOOTPRINT PASSPORT · 双轨合规认证</div>
        <div class="ic-main">
          <div class="ic-info">
            <div class="ic-title">碳足迹护照</div>
            <div class="ic-sub">贵州省重点产业 · 多级溯源防伪</div>
            <div class="ic-row"><label>护照编号</label><b class="mono">{{ issued.no }}</b></div>
            <div class="ic-row"><label>产品 / 功能单位</label><b>{{ issued.product }}</b></div>
            <div class="ic-row"><label>核算期</label><b>{{ issued.period }}</b></div>
            <div class="ic-row"><label>碳足迹</label><b class="carbon">{{ issued.carbon }} <i>tCO₂e/t</i></b></div>
            <div class="ic-row"><label>签发日期</label><b>{{ issued.time }}</b></div>
            <el-button class="ic-btn" @click="goVerify(issued)">打开公开验真页</el-button>
          </div>
          <div class="ic-qr">
            <qrcode-vue :value="verifyUrlFor(issued)" :size="120" level="M" render-as="svg" :margin="1" class="qr-svg" />
            <div class="ic-qr-tip">扫码验真</div>
            <div class="ic-qr-token mono">{{ issued.no }}</div>
            <div v-if="isLocalOrigin" class="ic-qr-lan">手机需与电脑同一 Wi-Fi</div>
            <div v-if="isLocalOrigin" class="ic-qr-host mono">{{ baseHost }}</div>
            <el-button v-if="isLocalOrigin" link size="small" class="ic-qr-set" @click="lanVisible = true">手机扫不开？设置地址</el-button>
          </div>
        </div>
        <div class="ic-foot">
          <span>发证：国家级终审与发证中心</span>
          <span>核查：中环低碳国际核查中心</span>
          <span>标准：ISO 14067 / EU CBAM</span>
        </div>
      </div>

      <!-- 当前申请进度 -->
      <div class="page-card flow-card">
        <div class="page-card-title">
          <h3>当前申请进度</h3>
          <el-tag round size="small" :type="statusType(current.status)">{{ current.status }}</el-tag>
        </div>
        <div class="fc-meta">
          <span class="fc-no mono">{{ current.no }}</span>
          <span>{{ current.product }}</span>
          <span>{{ current.period }}</span>
        </div>
        <el-steps :active="activeStep" finish-status="success" align-center class="fc-steps">
          <el-step title="填报" />
          <el-step title="省审" />
          <el-step title="复核" />
          <el-step title="终审" />
          <el-step title="发证" />
        </el-steps>
        <div class="fc-tip"><el-icon><InfoFilled /></el-icon>{{ stageTip }}</div>
        <el-button type="primary" :icon="Plus" class="fc-btn" @click="applyVisible = true">发起新申请</el-button>
      </div>
    </div>

    <!-- 申请记录 -->
    <div class="page-card">
      <div class="page-card-title">
        <h3>申请记录</h3>
        <div class="rec-tags">
          <el-tag round size="small" type="success" effect="plain">有效护照 {{ issuedCount }} 本</el-tag>
          <el-tag round size="small" type="primary" effect="plain">流程在途 {{ inProgressCount }} 本</el-tag>
          <el-tag round size="small" type="info" effect="plain">已退回 {{ returnedCount }} 本</el-tag>
        </div>
      </div>
      <div class="rec-scroll">
        <el-table :data="apps" size="small">
          <el-table-column prop="no" label="申请编号" width="160">
            <template #default="{ row }"><span class="mono no-cell">{{ row.no }}</span></template>
          </el-table-column>
          <el-table-column prop="period" label="核算期" width="96" />
          <el-table-column prop="product" label="产品 / 功能单位" min-width="130" />
          <el-table-column label="流程状态" min-width="180">
            <template #default="{ row }">
              <div class="status-cell">
                <el-tag size="small" round :type="statusType(row.status)">{{ row.status }}</el-tag>
                <el-progress :percentage="row.percent" :show-text="false" :stroke-width="4"
                  :color="row.percent === 100 ? '#00b581' : '#00d1ff'" class="status-bar" />
                <span class="status-pct">{{ row.percent }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="更新时间" width="110" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="onDetail(row)">详情</el-button>
              <el-button v-if="row.status.includes('发证')" link type="success" size="small" @click="goVerify(row)">验真</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 新申请 -->
    <el-dialog v-model="lanVisible" title="设置扫码使用的地址" width="500px">
      <div class="lan-note">
        手机必须与这台电脑连接同一 Wi-Fi。系统已自动检测本机局域网地址，若扫码仍打不开（多网卡 / 防火墙拦截），可手动填写：
      </div>
      <el-input v-model="lanInput" placeholder="例如 http://192.168.1.8:5175" class="lan-input" />
      <div class="lan-cur">当前二维码地址：<span class="mono">{{ baseOrigin }}</span></div>
      <template #footer>
        <el-button @click="resetLan">恢复自动检测</el-button>
        <el-button type="primary" @click="saveLan">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="applyVisible" title="发起护照申请" width="620px">
      <el-form label-width="110px">
        <el-form-item label="核算期">
          <el-select v-model="newApp.period" style="width: 100%">
            <el-option v-for="p in ['2026-10', '2026-09', '2026-08', '2026-Q3']" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标市场">
          <el-radio-group v-model="newApp.market">
            <el-radio value="EU-CBAM">欧盟 CBAM</el-radio>
            <el-radio value="ISO14067">ISO 14067 通用</el-radio>
            <el-radio value="DOMESTIC">国内双碳合规</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关联报告">
          <el-select v-model="newApp.report" style="width: 100%">
            <el-option label="QN-R-202610-031 · 2026年10月核算报告" value="QN-R-202610-031" />
            <el-option label="QN-R-202609-028 · 2026年09月核算报告" value="QN-R-202609-028" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="newApp.note" type="textarea" :rows="3" placeholder="补充说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="onApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 申请详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'申请详情 · ' + (detailRow?.no ?? '')" width="640px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请编号">{{ detailRow.no }}</el-descriptions-item>
          <el-descriptions-item label="核算期">{{ detailRow.period }}</el-descriptions-item>
          <el-descriptions-item label="产品 / 功能单位">{{ detailRow.product }}</el-descriptions-item>
          <el-descriptions-item label="当前状态"><el-tag size="small" round :type="statusType(detailRow.status)">{{ detailRow.status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="流程进度">{{ detailRow.percent }}%（{{ detailRow.status }}）</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailRow.time }}</el-descriptions-item>
        </el-descriptions>
        <div class="attach-block">
          <div class="attach-title">核验附件</div>
          <div class="attach-item" v-for="f in attachs" :key="f.name">
            <el-icon class="attach-icon"><Document /></el-icon>
            <span class="attach-name">{{ f.name }}</span>
            <el-tag size="small" :type="f.ok ? 'success' : 'info'" round>{{ f.ok ? '已核验' : '待补充' }}</el-tag>
          </div>
          <div class="attach-note">附件 SHA-256 哈希写入哈希链存证；任何变更都会留痕并同步审计日志。</div>
        </div>
      </template>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, InfoFilled } from '@element-plus/icons-vue'
import QrcodeVue from 'qrcode.vue'
import { getPublicOverride, isLocalOrigin, publicOrigin, setPublicOrigin } from '@/utils/publicUrl'

const router = useRouter()
const applyVisible = ref(false)
const newApp = reactive({ period: '2026-10', market: 'EU-CBAM', report: 'QN-R-202610-031', note: '' })

interface AppRow { no: string; period: string; product: string; status: string; percent: number; time: string; carbon: string }
const apps: AppRow[] = [
  { no: 'QN-PP-2026-0008', period: '2026-09', product: '精煤 · 1t', status: '终审中', percent: 87, time: '2026-10-18', carbon: '0.641' },
  { no: 'QN-PP-2026-0007', period: '2026-09', product: '焦炭 · 1t', status: '复核中', percent: 62, time: '2026-10-12', carbon: '0.874' },
  { no: 'QN-PP-2026-0006', period: '2026-08', product: '精煤 · 1t', status: '已发证', percent: 100, time: '2026-09-30', carbon: '0.642' },
  { no: 'QN-PP-2026-0005', period: '2026-Q3', product: '磷酸铁锂 · 1kg', status: '已发证', percent: 100, time: '2026-09-22', carbon: '5.218' },
  { no: 'QN-PP-2026-0004', period: '2026-Q3', product: '精煤 · 1t', status: '省审中', percent: 38, time: '2026-09-10', carbon: '0.638' },
  { no: 'QN-PP-2026-0003', period: '2026-07', product: '精煤 · 1t', status: '已退回', percent: 25, time: '2026-08-05', carbon: '0.651' },
  { no: 'QN-PP-2026-0002', period: '2026-06', product: '磷酸铁锂 · 1kg', status: '已退回', percent: 25, time: '2026-07-14', carbon: '5.402' },
  { no: 'QN-PP-2026-0001', period: '2026-Q2', product: '精煤 · 1t', status: '已退回', percent: 20, time: '2026-06-28', carbon: '0.663' }
]

const issued = apps.find((a) => a.status === '已发证')!
const current = apps[0]
const issuedCount = apps.filter((a) => a.status === '已发证').length
const returnedCount = apps.filter((a) => a.status === '已退回').length
const inProgressCount = apps.length - issuedCount - returnedCount

const activeStep = computed(() => {
  const s = current.status
  if (s === '已发证') return 5
  if (s.includes('终审')) return 4
  if (s === '复核中') return 3
  if (s.includes('省审')) return 2
  return 1
})
const stageTip = computed(() => {
  const s = current.status
  if (s === '已发证') return '护照已签发，扫码或打开公开验真页即可核验哈希与签发记录。'
  if (s.includes('终审')) return '已通过核查机构复核，等待国家级终审与发证中心签发。'
  if (s === '复核中') return '核查机构正按 ISO 14067 / EU CBAM 进行国际标准复核。'
  if (s.includes('省审')) return '省厅智能初审中：作 F6 波动比对与数据一致性检查。'
  return '申请已退回，请按退回意见补充材料后重新提交。'
})

function statusType(s: string): 'success' | 'primary' | 'warning' | 'danger' | 'info' {
  if (s === '已发证') return 'success'
  if (s.includes('终审')) return 'primary'
  if (s === '复核中' || s.includes('省审')) return 'warning'
  if (s === '已退回') return 'danger'
  return 'info'
}
const publicBase = ref(location.origin)
const baseOrigin = computed(() => publicBase.value)
const baseHost = computed(() => publicBase.value.replace(/^https?:\/\//, ''))
const lanVisible = ref(false)
const lanInput = ref(getPublicOverride())
onMounted(async () => { publicBase.value = await publicOrigin() })

function verifyUrlFor(row: AppRow) {
  return `${publicBase.value}/verify/${row.no.toLowerCase()}`
}
function saveLan() {
  setPublicOrigin(lanInput.value)
  publicOrigin().then((v) => { publicBase.value = v })
  lanVisible.value = false
  ElMessage.success('扫码地址已更新：' + lanInput.value)
}
function resetLan() {
  setPublicOrigin('')
  lanInput.value = ''
  publicOrigin().then((v) => { publicBase.value = v })
  lanVisible.value = false
  ElMessage.info('已恢复自动检测的局域网地址')
}

function onApply() {
  ElMessage.success('申请已提交省厅智能初审：QN-PP-2026-' + String(apps.length + 9).padStart(4, '0') + '（进度：填报 → 省审）')
  applyVisible.value = false
}

const detailVisible = ref(false)
const detailRow = ref<AppRow | null>(null)
const attachs = [
  { name: '碳核算报告 QN-R-202610-031.pdf', ok: true },
  { name: '活动数据台账（2026-09）.xlsx', ok: true },
  { name: '化验单 BH-1029 扫描件.pdf', ok: true },
  { name: '产品工艺流程图.docx', ok: false }
]
function onDetail(row: AppRow) {
  detailRow.value = row
  detailVisible.value = true
}
function goVerify(row: AppRow) {
  router.push('/verify/' + row.no.toLowerCase())
}
</script>

<style scoped lang="scss">
.pp-grid { display: grid; grid-template-columns: minmax(0, 6fr) minmax(0, 7fr); gap: 16px; align-items: stretch; }

/* 已签发护照卡 */
.issued-card {
  border-radius: var(--qn-radius);
  overflow: hidden;
  box-shadow: 0 16px 44px rgba(6, 40, 32, 0.28);
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.ic-ribbon {
  background: linear-gradient(90deg, #00b581, #00d1ff);
  color: #05231d; font-size: 10.5px; font-weight: 700; letter-spacing: 3px;
  text-align: center; padding: 9px 0;
}
.ic-main {
  flex: 1;
  display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 18px;
  padding: 28px 28px 22px;
  background: linear-gradient(150deg, #fbfdfc 0%, #e9f4ef 100%);
  position: relative;
  &::after {
    content: ''; position: absolute; right: -70px; bottom: -90px; width: 240px; height: 240px; border-radius: 50%;
    background: radial-gradient(circle, rgba(0, 181, 129, 0.16), transparent 70%);
    pointer-events: none;
  }
}
.ic-info { position: relative; z-index: 1; min-width: 0; }
.ic-title { font-size: 28px; font-weight: 800; letter-spacing: 6px; color: #063b31; }
.ic-sub { font-size: 11.5px; color: #667d7a; letter-spacing: 1.5px; margin: 6px 0 16px; }
.ic-row {
  display: flex; justify-content: space-between; gap: 12px;
  padding: 8px 0; border-bottom: 1px dashed #d6e6e0; font-size: 13px;
  label { color: #729089; white-space: nowrap; }
  b { color: #17332e; text-align: right; font-weight: 600; }
  b.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12.5px; letter-spacing: 1px; }
  b.carbon { color: #009c6d; font-size: 20px; i { font-style: normal; font-size: 11px; color: #729089; } }
}
.ic-btn {
  margin-top: 18px;
  background: #00795c; border-color: #00795c; color: #fff; font-weight: 600;
  &:hover, &:focus { background: #00634b; border-color: #00634b; color: #fff; }
}
.ic-qr {
  position: relative; z-index: 1;
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  align-self: start;
  .qr-svg { border-radius: 10px; background: #fff; padding: 6px; box-shadow: 0 8px 22px rgba(16, 54, 50, 0.16); }
  .ic-qr-tip { font-size: 12px; font-weight: 700; color: #063b31; letter-spacing: 2px; }
  .ic-qr-token { font-size: 10.5px; color: #7c948e; }
  .ic-qr-lan { font-size: 10.5px; color: #64748b; margin-top: 2px; }
  .ic-qr-host { font-size: 10.5px; color: #0f766e; }
  .ic-qr-set { font-size: 11px; color: #64748b; height: auto; padding: 0; }
}
.lan-note { font-size: 12.5px; line-height: 1.7; color: #475569; margin-bottom: 12px; }
.lan-input { width: 100%; }
.lan-cur { margin-top: 10px; font-size: 12px; color: #64748b; .mono { color: #0f766e; } }
.ic-foot {
  display: flex; flex-wrap: wrap; gap: 8px 18px;
  padding: 12px 20px;
  background: #063b31; color: #8cd3bd; font-size: 11px; letter-spacing: 0.5px;
}

/* 流程卡 */
.flow-card { display: flex; flex-direction: column; min-width: 0; }
.fc-meta { display: flex; flex-wrap: wrap; gap: 6px 14px; font-size: 12.5px; color: var(--qn-text-2); margin-bottom: 18px;
  .fc-no { color: var(--qn-primary-dark); font-weight: 600; } }
.fc-steps { margin: 4px 0 8px; }
.fc-tip {
  margin-top: 14px; padding: 11px 14px; border-radius: 10px;
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; color: #5c6b82;
  background: #f2f5f9; border: 1px dashed var(--qn-border);
}
.fc-btn { margin-top: 16px; align-self: flex-start; }

/* 记录表 */
.rec-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.rec-scroll { overflow-x: auto; }
.no-cell { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: var(--qn-text); }
.status-cell { display: flex; align-items: center; gap: 10px; }
.status-bar { flex: 1; min-width: 60px; }
.status-pct { font-size: 11.5px; color: #64748b; font-variant-numeric: tabular-nums; width: 36px; text-align: right; }

.attach-block { margin-top: 16px; padding: 14px 16px; background: #f5f8fc; border: 1px dashed #c9d9e8; border-radius: 10px; }
.attach-title { font-size: 12px; color: var(--qn-text-2); margin-bottom: 10px; }
.attach-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 13px; color: var(--qn-text);
  .attach-icon { color: var(--qn-primary); }
  .attach-name { flex: 1; }
  .el-tag { margin-left: auto; } }
.attach-note { margin-top: 10px; font-size: 11.5px; color: #64748b; }

/* 状态标签加深，保证可读 */
:deep(.el-tag--success) { background: #e6f9f2; color: #00795c; border-color: #b3ebd9; }
:deep(.el-tag--warning) { background: #fff7e6; color: #9a5b00; border-color: #ffe1a6; }
:deep(.el-tag--danger) { background: #fdecec; color: #a9271b; border-color: #f6c6c0; }
:deep(.el-tag--primary) { background: #e6f7ff; color: #00659a; border-color: #b3e3f7; }
:deep(.el-tag--info) { background: #eef2f7; color: #5c6b82; border-color: #d8e0ea; }

@media (max-width: 1100px) {
  .pp-grid { grid-template-columns: minmax(0, 1fr); }
}
</style>