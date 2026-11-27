<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">碳足迹护照申请</div>
      <div class="sub">按「填报 → 省审 → 复核 → 终审 → 发证」五级状态机流转，全程哈希链留痕，终审通过后签发并生成防伪二维码与公开验真链接</div>
    </div>

    <div class="stepper-card page-card">
      <el-steps :active="activeStep" finish-status="success" align-center>
        <el-step title="填报" description="活动数据 + 核算报告" />
        <el-step title="省审" description="省厅智能初审" />
        <el-step title="复核" description="核查机构国际复核" />
        <el-step title="终审" description="国家级终审" />
        <el-step title="发证" description="碳足迹护照签发" />
      </el-steps>

      <div class="apply-bar">
        <el-button type="primary" :icon="Plus" @click="applyVisible = true">发起新申请</el-button>
        <el-tag round type="success" effect="plain">当前企业 2 本有效护照</el-tag>
        <el-tooltip content="申请材料经 SHA-256 哈希后写入 sys_hash_chain 存证链">
          <el-tag round type="info" effect="plain"><el-icon><Link /></el-icon>&nbsp;哈希链存证开启</el-tag>
        </el-tooltip>
      </div>
    </div>

    <!-- 申请历史 -->
    <div class="page-card">
      <div class="page-card-title"><h3>申请记录</h3></div>
      <el-table :data="apps" size="small">
        <el-table-column prop="no" label="申请编号" width="170" />
        <el-table-column prop="period" label="核算期" width="100" />
        <el-table-column prop="product" label="产品 / 功能单位" />
        <el-table-column label="状态" width="200">
          <template #default="{ row }">
            <el-progress :percentage="row.percent" :color="row.percent === 100 ? '#00b581' : '#00d1ff'" :format="() => row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="time" label="更新时间" width="140" />
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDetail(row)">详情</el-button>
            <el-button v-if="row.status.includes('发证')" link type="success" size="small" @click="goVerify(row)">护照 / 验真</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新申请 -->
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
          <el-descriptions-item label="当前状态"><el-tag size="small" round>{{ detailRow.status }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="流程进度">{{ detailRow.percent }}%（{{ detailRow.status }}）</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailRow.time }}</el-descriptions-item>
        </el-descriptions>
        <div class="attach-block">
          <div class="attach-title">上传核验附件</div>
          <div class="attach-item" v-for="f in attachs" :key="f.name">
            <el-icon class="attach-icon"><Document /></el-icon>
            <span class="attach-name">{{ f.name }}</span>
            <el-tag size="small" :type="f.ok ? 'success' : 'info'" round>{{ f.ok ? '已核验' : '待补充' }}</el-tag>
          </div>
          <div class="attach-note">对接后端后展示真实上传文件及 SHA-256 哈希，附件变更全部写入审计日志与哈希链存证</div>
        </div>
      </template>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const applyVisible = ref(false)
const newApp = reactive({ period: '2026-10', market: 'EU-CBAM', report: 'QN-R-202610-031', note: '' })

const apps = [
  { no: 'QN-PP-2026-0008', period: '2026-09', product: '精煤 · 1t', status: '终审中', percent: 87, time: '2026-10-18' },
  { no: 'QN-PP-2026-0007', period: '2026-09', product: '焦炭 · 1t', status: '复核中', percent: 62, time: '2026-10-12' },
  { no: 'QN-PP-2026-0006', period: '2026-08', product: '精煤 · 1t', status: '已发证', percent: 100, time: '2026-09-30' },
  { no: 'QN-PP-2026-0005', period: '2026-Q3', product: '磷酸铁锂 · 1kg', status: '已发证', percent: 100, time: '2026-09-22' },
  { no: 'QN-PP-2026-0004', period: '2026-Q3', product: '精煤 · 1t', status: '省审中', percent: 38, time: '2026-09-10' },
  { no: 'QN-PP-2026-0003', period: '2026-07', product: '精煤 · 1t', status: '已退回', percent: 25, time: '2026-08-05' },
  { no: 'QN-PP-2026-0002', period: '2026-06', product: '磷酸铁锂 · 1kg', status: '已退回', percent: 25, time: '2026-07-14' },
  { no: 'QN-PP-2026-0001', period: '2026-Q2', product: '精煤 · 1t', status: '已退回', percent: 20, time: '2026-06-28' }
]

const activeStep = computed(() => {
  const s = apps[0]?.status
  if (s === '已发证') return 5
  if (s?.includes('终审')) return 4
  if (s === '复核中') return 3
  if (s?.includes('省审')) return 2
  return 1
})

function onApply() {
  ElMessage.success('申请已提交省厅智能初审：QN-PP-2026-' + String(apps.length + 9).padStart(4, '0') + '（进度：填报 → 省审）')
  applyVisible.value = false
}

/* 申请详情（含核验附件占位） */
type AppRow = typeof apps[number]
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
function goVerify(row: { no: string }) {
  router.push('/verify/' + row.no.toLowerCase())
}
</script>

<style scoped lang="scss">
.stepper-card { padding: 28px 24px; }
.apply-bar { margin-top: 26px; display: flex; align-items: center; gap: 14px; }
.attach-block { margin-top: 16px; padding: 14px 16px; background: #f5f8fc; border: 1px dashed #c9d9e8; border-radius: 10px; }
.attach-title { font-size: 12px; color: var(--qn-text-2); margin-bottom: 10px; }
.attach-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 13px; color: var(--qn-text);
  .attach-icon { color: var(--qn-primary); }
  .attach-name { flex: 1; }
  .el-tag { margin-left: auto; } }
.attach-note { margin-top: 10px; font-size: 11.5px; color: var(--qn-text-3); }
</style>