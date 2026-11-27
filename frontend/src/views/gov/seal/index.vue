<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">电子签章</div>
      <div class="sub">对初审通过的申请，在审批意见书上叠加电子印章图形并导出入库；每次盖章动作写入哈希链存证</div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>待盖章审批意见书</h3></div>
      <el-table :data="docs" size="small">
        <el-table-column prop="no" label="意见书编号" width="160" />
        <el-table-column prop="company" label="企业" min-width="180" />
        <el-table-column prop="period" label="核算期" width="90" />
        <el-table-column prop="reviewer" label="初审人" width="100" />
        <el-table-column prop="time" label="初审时间" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag size="small" :type="row.sealed ? 'success' : 'warning'" round>{{ row.sealed ? '已签章' : '待签章' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="!row.sealed" link type="danger" size="small" @click="openSeal(row)">盖章</el-button>
            <el-button v-else link type="primary" size="small" @click="openSeal(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 盖章对话框：意见书 + 印章叠层 -->
    <el-dialog v-model="sealVisible" title="审批意见书 · 盖章" width="760px">
      <div class="doc-preview">
        <div class="doc-head">
          <img src="@/assets/logo/logo.svg" style="height: 30px" />
          <div class="doc-title">贵州省重点产业碳足迹初审意见书</div>
          <div class="doc-no">{{ current?.no || 'QN-YJ-2026-118' }}</div>
        </div>
        <div class="doc-body">
          <p>申请人：{{ current?.company || '贵州黔能矿业有限责任公司' }}</p>
          <p>核算期：{{ current?.period || '2026-10' }} ｜ 核算总量：13,840 tCO₂e</p>
          <p>初审结论：依据 GB/T 24067 与省级因子库 v3.2 审核，数据波动率 6.2%，<b>同意通过初审</b>，转入第三方核查机构国际复核。</p>
          <p>初审人：{{ current?.reviewer || '李处长' }} ｜ {{ current?.time || '2026-11-26' }}</p>
          <!-- 印章叠层（盖章后显示） -->
          <div v-if="stamping" class="seal-overlay">
            <img :src="govSeal" class="seal-img" :class="{ stamped: true }" />
          </div>
        </div>
        <div class="doc-foot">本意见书经 SHA-256 哈希后写入哈希链存证编号 {{ hashShort }}</div>
      </div>
      <template #footer>
        <el-button @click="sealVisible = false">{{ stamping ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!stamping" type="danger" :loading="stampLoading" @click="doStamp">加盖电子章</el-button>
        <el-button v-else type="primary" @click="exportDoc">导出 PDF / 抄送机构</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import govSeal from '@/assets/seal/gov-seal.svg'

interface Doc { no: string; company: string; period: string; reviewer: string; time: string; sealed: boolean }

const docs = ref<Doc[]>([
  { no: 'QN-YJ-2026-118', company: '贵州黔能矿业有限责任公司', period: '2026-10', reviewer: '李处长', time: '2026-11-26 10:12', sealed: false },
  { no: 'QN-YJ-2026-116', company: '贵州振华新材料股份有限公司', period: '2026-10', reviewer: '张副处', time: '2026-11-25 16:40', sealed: false },
  { no: 'QN-YJ-2026-113', company: '贵州西南水泥有限公司', period: '2026-10', reviewer: '李处长', time: '2026-11-24 09:31', sealed: true },
  { no: 'QN-YJ-2026-112', company: '贵州黔能煤化工有限责任公司', period: '2026-10', reviewer: '张副处', time: '2026-11-23 15:08', sealed: false },
  { no: 'QN-YJ-2026-110', company: '贵州钢绳股份有限公司', period: '2026-10', reviewer: '李处长', time: '2026-11-22 11:26', sealed: true },
  { no: 'QN-YJ-2026-108', company: '贵州乌江航运物流有限公司', period: '2026-10', reviewer: '赵科长', time: '2026-11-21 14:52', sealed: true },
  { no: 'QN-YJ-2026-106', company: '铜仁金瑞锰业有限责任公司', period: '2026-10', reviewer: '张副处', time: '2026-11-20 10:05', sealed: false },
  { no: 'QN-YJ-2026-104', company: '遵义铝业股份有限公司', period: '2026-09', reviewer: '李处长', time: '2026-11-18 16:33', sealed: true },
  { no: 'QN-YJ-2026-102', company: '中伟新材料股份有限公司', period: '2026-09', reviewer: '赵科长', time: '2026-11-17 09:47', sealed: true }
])
const sealVisible = ref(false)
const stamping = ref(false)
const stampLoading = ref(false)
const current = ref<Doc | null>(null)
const hashShort = ref('')

function openSeal(row: Doc) {
  current.value = row
  stamping.value = row.sealed
  sealVisible.value = true
  hashShort.value = '0x' + Array.from({ length: 16 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')
}
function doStamp() {
  stampLoading.value = true
  setTimeout(() => {
    stamping.value = true
    stampLoading.value = false
    if (current.value) current.value.sealed = true
    ElMessage.success('盖章完成：印章已合成到意见书，存证哈希写入 sys_hash_chain')
  }, 900)
}
function exportDoc() {
  ElMessage.success('已导出 QN-YJ-XXXX 签署版 PDF 并抄送核查机构')
  sealVisible.value = false
}
</script>

<style scoped lang="scss">
.doc-preview { border: 1px solid var(--qn-border); border-radius: 12px; overflow: hidden; }
.doc-head { display: flex; align-items: center; gap: 14px; padding: 18px 22px; background: linear-gradient(90deg, #f0f7f4, #f6fbfe); border-bottom: 1px solid var(--qn-border);
  .doc-title { font-size: 16px; font-weight: 700; color: var(--qn-text); flex: 1; }
  .doc-no { font-size: 12px; color: var(--qn-text-3); } }
.doc-body { padding: 26px 30px; font-size: 14px; line-height: 2.2; color: var(--qn-text); position: relative; min-height: 300px; }
.seal-overlay { position: absolute; right: 48px; top: 130px; pointer-events: none; }
.seal-img { width: 148px; height: 148px; opacity: 0.92; animation: seal-in 0.6s cubic-bezier(0.2, 1.4, 0.4, 1) both; filter: drop-shadow(0 3px 6px rgba(200,30,30,0.25)); }
@keyframes seal-in { from { opacity: 0; transform: scale(2.2) rotate(-24deg); } to { opacity: 0.92; transform: scale(1) rotate(-12deg); } }
.doc-foot { padding: 14px 22px; background: #f8fafc; font-size: 11.5px; color: var(--qn-text-3); border-top: 1px dashed var(--qn-border); }
</style>