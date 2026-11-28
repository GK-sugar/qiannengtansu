<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">终审与发证中心</div>
      <div class="sub">国家级终审流程：核查机构复核通过 → 国家终审 → 签发碳足迹护照（F10 哈希链存证 + F11 防伪二维码），扫描直达公开验真页</div>
    </div>

    <div class="stat-grid">
      <StatCard label="待终审申请" :value="pending.length" icon="DocumentChecked" tint="#ff5f56" suffix=" 件" :animated="false" />
      <StatCard label="今日已签发" :value="todayIssued" icon="Medal" tint="#00b581" suffix=" 本" extra="含防伪令牌" />
      <StatCard label="存证链高度" :value="94128" icon="Link" tint="#00d1ff" extra="SHA-256 哈希链" />
      <StatCard label="累计签发护照" :value="418" icon="Stamp" tint="#ffb020" suffix=" 本" extra="覆盖 6 大行业" />
    </div>

    <div class="page-card">
      <el-tabs v-model="tab">
        <!-- ============ 待终审 ============ -->
        <el-tab-pane label="待终审清单" name="pending">
          <el-table :data="pending" size="small" v-loading="loading">
            <el-table-column prop="applicationNo" label="申请单号" width="160">
              <template #default="{ row }"><b class="mono">{{ row.applicationNo }}</b></template>
            </el-table-column>
            <el-table-column prop="entName" label="申请企业" min-width="180" />
            <el-table-column prop="productName" label="产品 / 声明单位" min-width="140" />
            <el-table-column prop="period" label="核算期" width="110" />
            <el-table-column label="碳足迹" width="130">
              <template #default="{ row }"><b class="green">{{ row.totalCo2e }}</b><span class="dim"> tCO2e</span></template>
            </el-table-column>
            <el-table-column label="复核结论" width="110">
              <template #default="{ row }">
                <el-tag :type="row.agencyConclusion === 'PASSED' ? 'success' : 'danger'" size="small" round>{{ row.agencyConclusion === 'PASSED' ? '复核通过' : '复核退回' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="终审操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" size="small" @click="openDecide(row, true)">通过并签发</el-button>
                <el-button link type="danger" size="small" @click="openDecide(row, false)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="note-row">
            <el-icon><InfoFilled /></el-icon>
            <span>签发即写入 <b class="mono">sys_hash_chain</b> 存证链（F10），护照二维码内含 SHA-256 防伪令牌（F11），任何篡改将在公开验真页标红告警。</span>
          </div>
        </el-tab-pane>

        <!-- ============ 已签发 ============ -->
        <el-tab-pane label="已签发护照" name="issued">
          <el-table :data="issuedList" size="small">
            <el-table-column prop="applicationNo" label="申请单号" width="160">
              <template #default="{ row }"><b class="mono">{{ row.applicationNo }}</b></template>
            </el-table-column>
            <el-table-column prop="passportNo" label="护照编号" width="170">
              <template #default="{ row }"><el-tag type="success" size="small" effect="plain" round>{{ row.passportNo }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="entName" label="持证企业" min-width="180" />
            <el-table-column prop="productName" label="产品" min-width="130" />
            <el-table-column prop="chainSeq" label="存证区块" width="120">
              <template #default="{ row }"><span class="mono">#{{ row.chainSeq }}</span></template>
            </el-table-column>
            <el-table-column prop="issueTime" label="签发时间" width="160" />
            <el-table-column label="验真" width="110" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openVerify(row.verifyToken)">扫码验真</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ============ 终审决定对话框 ============ -->
    <el-dialog v-model="decideVisible" :title="decideApprove ? '终审通过与签发确认' : '终审驳回'" width="560px">
      <el-descriptions :column="2" border size="small" v-if="current">
        <el-descriptions-item label="申请单号">{{ current.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="核算期">{{ current.period }}</el-descriptions-item>
        <el-descriptions-item label="申请企业" :span="2">{{ current.entName }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ current.productName }}</el-descriptions-item>
        <el-descriptions-item label="碳足迹">{{ current.totalCo2e }} tCO2e</el-descriptions-item>
      </el-descriptions>
      <div v-if="!decideApprove" class="reject-box">
        <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因（将退回核查机构并同步企业）" />
      </div>
      <div v-else class="issue-tip">
        <el-icon><Medal /></el-icon>
        <span>确认签发后系统将依次完成：护照编号生成 → F10 哈希链存证 → F11 防伪令牌 → 二维码公开验真。</span>
      </div>
      <template #footer>
        <el-button @click="decideVisible = false">取消</el-button>
        <el-button :type="decideApprove ? 'primary' : 'danger'" :loading="deciding" @click="doDecide">
          {{ decideApprove ? '确认签发' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ============ 护照预览 ============ -->
    <el-dialog v-model="ppVisible" width="820px" class="pp-dialog" title="碳足迹护照 · 已签发">
      <div class="passport" :key="ppNonce">
        <div class="pp-left">
          <div class="pp-kicker">CARBON FOOTPRINT PASSPORT</div>
          <div class="pp-title">碳足迹护照</div>
          <div class="pp-sub">中国国家级终审签发 · 欧盟/国际互认</div>
          <div class="pp-field"><label>护照编号</label><b>{{ pp.passportNo }}</b></div>
          <div class="pp-field"><label>持证企业</label><b>{{ pp.entName }}</b></div>
          <div class="pp-field"><label>产品 / 声明单位</label><b>{{ pp.productName }} · 1t</b></div>
          <div class="pp-field"><label>认证标准</label><b>ISO 14067 / EU CBAM</b></div>
          <div class="pp-field"><label>签发日期</label><b>2026-11-26</b></div>
          <div class="pp-field"><label>存证区块</label><b class="big">#{{ pp.chainSeq }}</b></div>
        </div>
        <div class="pp-right">
          <div class="qr-wrap">
            <qrcode-vue :value="verifyUrl" :size="180" level="M" render-as="svg" :margin="1" class="qr-svg" />
            <div class="qr-tip">扫码验真 · 防伪令牌 {{ shortToken }}</div>
          </div>
          <div class="pp-meta">
            <div class="pm-row"><span>存证哈希</span><b class="mono">{{ pp.hashValue }}</b></div>
            <div class="pm-row"><span>签发机构</span><b>国家应对气候变化战略中心</b></div>
            <div class="pm-row"><span>签发专员</span><b>周司长</b></div>
          </div>
          <img src="@/assets/seal/gov-seal.svg" class="pp-seal" />
        </div>
      </div>
      <template #footer>
        <el-button @click="ppVisible = false">关闭</el-button>
        <el-button type="primary" plain @click="openVerify(pp.verifyToken)">打开公开验真页</el-button>
      </template>
    </el-dialog>

    <!-- ============ 扫码验真弹窗 ============ -->
    <el-dialog v-model="verifyVisible" title="扫码验真 · 公开验真页" width="440px">
      <div class="verify-box" v-if="verifyToken">
        <qrcode-vue :value="verifyUrlFor" :size="190" level="M" render-as="svg" :margin="1" class="qr-svg" />
        <div class="verify-line">防伪令牌：<b class="mono">{{ verifyToken }}</b></div>
        <div class="verify-line dim">验真地址：{{ verifyUrlFor }}</div>
        <div class="verify-note">使用微信 / 浏览器扫码即可打开公开验真页，核验护照哈希与签发记录；任何篡改将在验真页标红告警</div>
        <div v-if="isLocalOrigin" class="verify-note lan-tip">手机需与电脑连接同一 Wi-Fi；若扫码打不开，请在「企业端 · 护照申请」页设置局域网地址</div>
      </div>
      <template #footer>
        <el-button @click="verifyVisible = false">关闭</el-button>
        <el-button type="primary" @click="goPublicVerify">打开公开验真页</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import QrcodeVue from 'qrcode.vue'
import request from '@/utils/request'
import StatCard from '@/components/StatCard.vue'
import { isLocalOrigin, publicOrigin } from '@/utils/publicUrl'

interface Row { applicationNo: string; entName: string; period: string; productName: string; totalCo2e: string; intensity: string; agencyConclusion: string; status?: string; passportNo?: string; chainSeq?: number; verifyToken?: string; hashValue?: string; issueTime?: string }

const tab = ref('pending')
const loading = ref(false)
const pending = ref<Row[]>([])
const issuedList = ref<Row[]>([])
const todayIssued = ref(3)

const decideVisible = ref(false)
const deciding = ref(false)
const decideApprove = ref(true)
const rejectReason = ref('')
const current = ref<Row | null>(null)

const ppVisible = ref(false)
const ppNonce = ref(0)
const pp = reactive<any>({ passportNo: '', verifyToken: '', chainSeq: 0, hashValue: '', entName: '', productName: '' })

const publicBase = ref(location.origin)
onMounted(async () => { publicBase.value = await publicOrigin() })
const verifyUrl = computed(() => `${publicBase.value}/verify/${pp.verifyToken}`)
const shortToken = computed(() => (pp.verifyToken || '').slice(0, 12))

const demoPending: Row[] = [
  { applicationNo: 'SQ-2026-0041', entName: '贵州黔能矿业有限责任公司', period: '2026-Q3', productName: '精煤（洗选产品）', totalCo2e: '3,842.75', intensity: '0.1842', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0038', entName: '贵州中伟新能源材料有限公司', period: '2026-09', productName: '磷酸铁锂正极材料', totalCo2e: '1,286.40', intensity: '1.2080', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0045', entName: '贵州磷化集团有限责任公司', period: '2026-09', productName: '磷酸二铵', totalCo2e: '7,640.20', intensity: '0.9400', agencyConclusion: 'REJECTED' },
  { applicationNo: 'SQ-2026-0046', entName: '遵义铝业股份有限公司', period: '2026-Q3', productName: '铝锭', totalCo2e: '12,480.60', intensity: '11.8500', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0044', entName: '贵州钢绳股份有限公司', period: '2026-09', productName: '钢丝绳', totalCo2e: '2,164.30', intensity: '1.9200', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0043', entName: '贵州西南水泥有限公司', period: '2026-09', productName: '水泥熟料', totalCo2e: '26,530.80', intensity: '0.8650', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0042', entName: '贵州黔能煤化工有限责任公司', period: '2026-Q3', productName: '焦炭', totalCo2e: '9,412.55', intensity: '1.3100', agencyConclusion: 'PASSED' },
  { applicationNo: 'SQ-2026-0039', entName: '贵州振华新材料股份有限公司', period: '2026-09', productName: '三元前驱体', totalCo2e: '3,286.90', intensity: '2.4100', agencyConclusion: 'REJECTED' }
]
const demoIssued: Row[] = [
  { applicationNo: 'SQ-2026-0008', passportNo: 'QN-PP-2026-0041', entName: '贵州黔能矿业有限责任公司', productName: '精煤', chainSeq: 94126, issueTime: '2026-11-25 10:12', verifyToken: '0f3a9c2e1b' },
  { applicationNo: 'SQ-2026-0006', passportNo: 'QN-PP-2026-0038', entName: '贵州中伟新能源材料有限公司', productName: '磷酸铁锂', chainSeq: 94119, issueTime: '2026-11-22 15:40', verifyToken: '7d1b8e4c22' },
  { applicationNo: 'SQ-2026-0005', passportNo: 'QN-PP-2026-0036', entName: '贵州磷化集团有限责任公司', productName: '磷酸二铵', chainSeq: 94112, issueTime: '2026-11-20 09:35', verifyToken: 'a94c17e0b3' },
  { applicationNo: 'SQ-2026-0004', passportNo: 'QN-PP-2026-0033', entName: '遵义铝业股份有限公司', productName: '铝锭', chainSeq: 94104, issueTime: '2026-11-18 14:22', verifyToken: '3e8b52d9c1' },
  { applicationNo: 'SQ-2026-0003', passportNo: 'QN-PP-2026-0030', entName: '贵州钢绳股份有限公司', productName: '钢丝绳', chainSeq: 94097, issueTime: '2026-11-15 11:08', verifyToken: 'c62f90a18d' },
  { applicationNo: 'SQ-2026-0002', passportNo: 'QN-PP-2026-0027', entName: '贵州西南水泥有限公司', productName: '水泥熟料', chainSeq: 94089, issueTime: '2026-11-12 16:47', verifyToken: '58ac31e7f9' },
  { applicationNo: 'SQ-2026-0001', passportNo: 'QN-PP-2026-0024', entName: '贵州振华新材料股份有限公司', productName: '三元前驱体', chainSeq: 94081, issueTime: '2026-11-10 10:30', verifyToken: 'e07b46d21a' },
  { applicationNo: 'SQ-2025-0198', passportNo: 'QN-PP-2026-0021', entName: '贵州黔能煤化工有限责任公司', productName: '焦炭', chainSeq: 94073, issueTime: '2026-11-07 15:18', verifyToken: '91d5c8f37e' }
]

onMounted(async () => {
  try {
    const api = useMock() ? undefined : undefined
    void api
    const r = (await request.get('/v1/national/issuance/pending')) as any
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) pending.value = normalize(r.data)
    else pending.value = demoPending
  } catch {
    pending.value = demoPending
  }
  try {
    const r = (await request.get('/v1/national/issuance/issued')) as any
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) issuedList.value = r.data.map((x: any) => ({
      applicationNo: x.applicationNo, passportNo: x.passportNo, entName: x.entName, productName: x.productName,
      chainSeq: x.hashChainSeq || 94128, issueTime: fmt(x.issueTime), verifyToken: x.verifyToken || ''
    }))
    else issuedList.value = demoIssued
  } catch {
    issuedList.value = demoIssued
  }
})

function useMock() { return import.meta.env.VITE_USE_MOCK !== 'false' }
function fmt(s?: string) { return s ? String(s).replace('T', ' ').slice(0, 19) : '2026-11-26 09:00' }
function normalize(rows: any[]): Row[] {
  return rows.map((x) => ({
    applicationNo: x.applicationNo, entName: x.entName, period: x.period, productName: x.productName,
    totalCo2e: typeof x.totalCo2e === 'number' ? String(x.totalCo2e) : x.totalCo2e,
    intensity: String(x.intensity || ''), agencyConclusion: x.agencyConclusion || 'PASSED'
  }))
}

function openDecide(row: Row, approve: boolean) {
  current.value = row
  decideApprove.value = approve
  rejectReason.value = ''
  decideVisible.value = true
}

async function doDecide() {
  if (!current.value) return
  deciding.value = true
  if (decideApprove.value) {
    let res: any = null
    try {
      res = (await request.post('/v1/national/issuance/decide', {
        applicationNo: current.value.applicationNo, approval: 'true',
        entName: current.value.entName, productName: current.value.productName,
        totalCo2e: current.value.totalCo2e.replace(/,/g, ''), intensity: current.value.intensity
      })) as any
    } catch { /* 后端不可用 → 本地模拟签署 */ }
    if (res && res.code === 0 && res.data?.passportNo) {
      Object.assign(pp, {
        passportNo: res.data.passportNo, verifyToken: res.data.verifyToken, chainSeq: res.data.hashChainSeq,
        hashValue: res.data.hashValue, entName: current.value.entName, productName: current.value.productName
      })
    } else {
      const no = 'QN-PP-2026-' + String(90 + Math.floor(Math.random() * 900)).padStart(4, '0')
      const token = Array.from({ length: 12 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')
      Object.assign(pp, {
        passportNo: no, verifyToken: token, chainSeq: 94128 + Math.floor(Math.random() * 5),
        hashValue: '0x' + Array.from({ length: 10 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('') + '…',
        entName: current.value.entName, productName: current.value.productName
      })
    }
    pending.value = pending.value.filter((r) => r.applicationNo !== current.value!.applicationNo)
    todayIssued.value += 1
    ppNonce.value++
    decideVisible.value = false
    ppVisible.value = true
    ElMessage.success(res && res.code === 0 ? '国家终审通过，护照已签发并写入哈希链存证' : '终审通过 · 护照已签发（本地模拟签署，后端未连接时自动降级）')
  } else {
    try {
      await request.post('/v1/national/issuance/decide', { applicationNo: current.value.applicationNo, approval: 'false', rejectReason: rejectReason.value })
    } catch { /* 无需处理 */ }
    pending.value = pending.value.filter((r) => r.applicationNo !== current.value!.applicationNo)
    decideVisible.value = false
    ElMessage.warning('已驳回：' + rejectReason.value + '（退回核查机构并同步企业）')
  }
  deciding.value = false
}

function openVerify(token?: string) {
  if (!token) { ElMessage.info('暂无已签发的护照，请先在终审与发证中心签发'); return }
  verifyToken.value = token
  verifyVisible.value = true
}

const verifyVisible = ref(false)
const verifyToken = ref('')
const verifyUrlFor = computed(() => `${publicBase.value}/verify/${verifyToken.value}`)
function goPublicVerify() {
  window.open(verifyUrlFor.value, '_blank')
  verifyVisible.value = false
}
</script>

<style scoped lang="scss">
.mono { font-family: 'JetBrains Mono', Consolas, monospace; font-size: 12.5px; }
.green { color: var(--qn-primary); }
.dim { color: var(--qn-text-3); font-size: 12px; }
.note-row { margin-top: 14px; display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--qn-text-3); }
.reject-box { margin-top: 6px; }
.issue-tip { display: flex; align-items: center; gap: 10px; margin-top: 10px; padding: 12px 14px; border-radius: 10px; font-size: 13px; color: #0d7a5f; background: rgba(0, 181, 129, 0.08); border: 1px solid rgba(0, 181, 129, 0.2); }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }

/* ---------- 护照样式 ---------- */
.passport {
  display: grid;
  grid-template-columns: 7fr 5fr;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 24px 60px rgba(8, 34, 60, 0.22);
  min-height: 430px;
  animation: pp-in 0.8s cubic-bezier(0.22, 1, 0.36, 1) both;
}
@keyframes pp-in { from { opacity: 0; transform: translateY(18px) rotateX(8deg); } to { opacity: 1; transform: none; } }
.pp-left {
  padding: 34px;
  color: #eaf6f2;
  background:
    radial-gradient(360px 220px at 110% -10%, rgba(0, 209, 255, 0.4), transparent 60%),
    linear-gradient(150deg, #052e26 0%, #064a3d 48%, #073d33 100%);
  position: relative;
  &::before { content: ''; position: absolute; inset: 0; background-image: repeating-linear-gradient(108deg, transparent 0 18px, rgba(255,255,255,0.012) 18px 19px); }
}
.pp-kicker { font-size: 10px; letter-spacing: 4px; color: #70dfc0; }
.pp-title { font-size: 34px; font-weight: 800; letter-spacing: 10px; margin: 9px 0 4px; }
.pp-sub { font-size: 12px; color: #9ac8b8; letter-spacing: 2px; margin-bottom: 22px; }
.pp-field { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding: 9px 0; border-bottom: 1px dashed rgba(120, 220, 190, 0.25); font-size: 13px;
  label { color: #86c5b1; white-space: nowrap; }
  b { color: #fff; font-variant-numeric: tabular-nums; text-align: right; &.big { color: #4ce6b8; font-size: 19px; } } }
.pp-right { background: #fdfefe; padding: 34px 28px; display: flex; flex-direction: column; align-items: center; gap: 14px; position: relative; }
.qr-wrap { text-align: center;
  .qr-svg { border-radius: 10px; box-shadow: 0 10px 26px rgba(8, 34, 60, 0.14); background: #fff; }
  .qr-tip { margin-top: 10px; font-size: 11.5px; color: var(--qn-text-3); letter-spacing: 1px; } }
.pp-meta { width: 100%; font-size: 12px; color: var(--qn-text-2);
  .pm-row { display: flex; justify-content: space-between; gap: 10px; padding: 7px 0; border-bottom: 1px dashed #e3ebf2;
    b { color: var(--qn-text); text-align: right; } .mono { font-size: 11px; } } }
.pp-seal { width: 106px; height: 106px; position: absolute; right: 22px; bottom: 18px; opacity: 0.9; transform: rotate(-8deg); }
@media (max-width: 700px) {
  .passport { grid-template-columns: 1fr; min-height: 0; }
  .pp-left, .pp-right { padding: 22px 18px; }
  .pp-right { align-items: flex-start; }
  .pp-title { font-size: 24px; letter-spacing: 4px; }
  .pp-seal { position: static; align-self: flex-end; margin-top: 6px; }
}

/* ---------- 扫码验真 ---------- */
.verify-box { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 8px 0 4px; text-align: center;
  .qr-svg { border-radius: 10px; box-shadow: 0 8px 22px rgba(8, 34, 60, 0.14); background: #fff; padding: 6px; } }
.verify-line { font-size: 12.5px; color: var(--qn-text); &.dim { color: var(--qn-text-3); font-size: 11.5px; word-break: break-all; } }
.verify-note { margin-top: 4px; font-size: 11.5px; color: var(--qn-text-3); line-height: 1.7; }
.lan-tip { color: #64748b; padding-top: 4px; border-top: 1px dashed var(--qn-border); }
</style>