<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">护照复核与终审推送</div>
      <div class="sub">复核通过后推送国家级终审：由国家级终审与发证中心生成护照编号、写入哈希链（F10）并生成防伪二维码（F11），扫码直达公开验真页</div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>待推送终审清单</h3></div>
      <el-table :data="list" size="small">
        <el-table-column prop="company" label="企业" min-width="170" />
        <el-table-column prop="product" label="产品 / 功能单位" min-width="140" />
        <el-table-column prop="period" label="核算期" width="100" />
        <el-table-column prop="c" label="碳足迹（tCO₂e/t）" width="140">
          <template #default="{ row }"><b class="green">{{ row.c }}</b></template>
        </el-table-column>
        <el-table-column label="复核结论" width="110">
          <template #default="{ row }"><el-tag size="small" :type="row.passed ? 'success' : 'info'" round>{{ row.passed ? '通过' : '复核中' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.passed && !row.issued && !row.pushed" link type="success" size="small" @click="openIssue(row)">推送国家级终审</el-button>
            <el-button v-else-if="row.issued" link type="primary" size="small" @click="openIssue(row)">查看护照</el-button>
            <el-tag v-else-if="row.pushed" size="small" type="warning" round>已推送终审</el-tag>
            <span v-else style="color:#c0c8d4;font-size:12px">等待复核</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 推送确认 / 护照预览 -->
    <el-dialog v-model="dialogVisible" width="820px" class="pp-dialog" :title="issued ? '碳足迹护照 · 已签发' : pushed ? '已推送国家级终审' : '推送终审确认'">
      <div class="passport" :key="issueNonce">
        <div class="pp-left">
          <div class="pp-kicker">CARBON FOOTPRINT PASSPORT</div>
          <div class="pp-title">碳足迹护照</div>
          <div class="pp-sub">贵州省重点产业 · 双轨合规认证</div>
          <div class="pp-field"><label>{{ issued ? '护照编号' : '申请编号' }}</label><b>{{ pno }}</b></div>
          <div class="pp-field"><label>持证企业</label><b>{{ current?.company }}</b></div>
          <div class="pp-field"><label>产品 / 功能单位</label><b>{{ current?.product }} · 1t</b></div>
          <div class="pp-field"><label>核算期</label><b>{{ current?.period }}</b></div>
          <div class="pp-field"><label>碳足迹</label><b class="big">{{ current?.c }} tCO₂e/t</b></div>
          <div class="pp-field"><label v-if="issued">签发日期</label><label v-else>推送日期</label><b>2026-11-26</b></div>
        </div>
        <div class="pp-right">
          <div class="qr-wrap">
            <qrcode-vue :value="verifyUrl" :size="180" level="M" class="qr-svg" />
            <div class="qr-tip">{{ issued ? '扫码验真' : '签发后生成' }} · 防伪令牌 {{ shortToken }}</div>
          </div>
          <div class="pp-meta">
            <div class="pm-row"><span>存证哈希</span><b class="mono">{{ hashShort }}</b></div>
            <div class="pm-row"><span>复核机构</span><b>中环低碳国际核查中心</b></div>
            <div class="pm-row"><span>终审机构</span><b>国家级终审与发证中心</b></div>
            <div class="pm-row"><span>核查标准</span><b>ISO 14067 / EU CBAM</b></div>
          </div>
          <img src="@/assets/seal/agency-seal.svg" class="pp-seal" />
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button v-if="!issued && !pushed" type="primary" :loading="issuing" @click="doIssue">确认推送终审</el-button>
        <el-button v-else-if="issued" type="primary" plain @click="openVerifyNew">打开公开验真页</el-button>
        <el-tag v-else type="warning" round effect="plain">已推送 · 等待国家级终审</el-tag>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import QrcodeVue from 'qrcode.vue'

interface Pkg { company: string; product: string; period: string; c: number; passed: boolean; issued?: boolean; pushed?: boolean; no?: string }

const list = ref<Pkg[]>([
  { company: '贵州黔能矿业有限责任公司', product: '精煤', period: '2026-Q3', c: 0.642, passed: true },
  { company: '贵州振华新材料股份有限公司', product: '磷酸铁锂', period: '2026-09', c: 1.208, passed: true, issued: true, no: 'QN-PP-2026-0041' },
  { company: '贵州磷化集团有限责任公司', product: '磷酸二铵', period: '2026-09', c: 0.94, passed: true, pushed: true },
  { company: '贵州黔能煤化工有限责任公司', product: '焦炭', period: '2026-08', c: 1.02, passed: false },
  { company: '遵义铝业股份有限公司', product: '铝锭', period: '2026-Q3', c: 11.85, passed: true },
  { company: '贵州钢绳股份有限公司', product: '钢丝绳', period: '2026-09', c: 1.92, passed: true },
  { company: '贵州西南水泥有限公司', product: '水泥熟料', period: '2026-09', c: 0.865, passed: true, pushed: true },
  { company: '中伟新材料股份有限公司', product: '三元前驱体', period: '2026-09', c: 2.41, passed: false }
])

const dialogVisible = ref(false)
const issuing = ref(false)
const issued = ref(false)
const pushed = ref(false)
const current = ref<Pkg | null>(null)
const pno = ref('')
const token = ref('')
const hashShort = ref('')
const issueNonce = ref(0)

const verifyUrl = computed(() => `${location.origin}/verify/${token.value}`)
const shortToken = computed(() => token.value.slice(0, 12))

function genHex(n: number) { return Array.from({ length: n }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('') }

function openIssue(row: Pkg) {
  current.value = row
  issued.value = !!row.issued
  pushed.value = !!row.pushed
  pno.value = row.no || 'QN-PP-2026-00' + (40 + Math.floor(Math.random() * 50))
  token.value = row.no ? row.no.toLowerCase() : pno.value.toLowerCase()
  hashShort.value = '0x' + genHex(10) + '…' + genHex(6)
  issueNonce.value++
  dialogVisible.value = true
}

function doIssue() {
  issuing.value = true
  setTimeout(() => {
    // 推送动作：申请进入国家级终审队列，由终审与发证中心完成编号 → F10 哈希链写入 → F11 验真令牌 → 二维码
    pushed.value = true
    issuing.value = false
    if (current.value) current.value.pushed = true
    ElMessage.success('已推送国家级终审：' + pno.value + ' 已提交终审与发证中心（护照编号、哈希链存证与防伪令牌将在终审通过后生成）')
  }, 1100)
}

function openVerifyNew() {
  dialogVisible.value = false
  window.open('/verify/' + token.value, '_blank')
}
</script>

<style scoped lang="scss">
.green { color: var(--qn-primary); }
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
  padding: 34px 34px;
  color: #eaf6f2;
  background:
    radial-gradient(360px 220px at 110% -10%, rgba(0, 209, 255, 0.4), transparent 60%),
    linear-gradient(150deg, #052e26 0%, #064a3d 48%, #073d33 100%);
  position: relative;
  &::before {
    content: ''; position: absolute; inset: 0;
    background-image: repeating-linear-gradient(108deg, transparent 0 18px, rgba(255,255,255,0.012) 18px 19px);
  }
}
.pp-kicker { font-size: 10px; letter-spacing: 4px; color: #70dfc0; }
.pp-title { font-size: 34px; font-weight: 800; letter-spacing: 10px; margin: 9px 0 4px; }
.pp-sub { font-size: 12px; color: #9ac8b8; letter-spacing: 2px; margin-bottom: 22px; }
.pp-field { display: flex; justify-content: space-between; align-items: baseline; gap: 12px; padding: 9px 0; border-bottom: 1px dashed rgba(120, 220, 190, 0.25); font-size: 13px;
  label { color: #86c5b1; white-space: nowrap; }
  b { color: #fff; font-variant-numeric: tabular-nums; text-align: right; &.big { color: #4ce6b8; font-size: 19px; } } }
.pp-right {
  background: #fdfefe;
  padding: 34px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  position: relative;
}
.qr-wrap { text-align: center;
  .qr-svg { border-radius: 10px; box-shadow: 0 10px 26px rgba(8, 34, 60, 0.14); background: #fff; }
  .qr-tip { margin-top: 10px; font-size: 11.5px; color: var(--qn-text-3); letter-spacing: 1px; } }
.pp-meta { width: 100%; font-size: 12px; color: var(--qn-text-2);
  .pm-row { display: flex; justify-content: space-between; gap: 10px; padding: 7px 0; border-bottom: 1px dashed #e3ebf2;
    b { color: var(--qn-text); text-align: right; }.mono { font-family: monospace; font-size: 11px; } } }
.pp-seal { width: 106px; height: 106px; position: absolute; right: 22px; bottom: 18px; opacity: 0.9; transform: rotate(-8deg); filter: drop-shadow(0 3px 6px rgba(180,30,30,0.2)); }
</style>