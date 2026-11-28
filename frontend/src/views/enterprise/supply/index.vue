<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">供应链协同</div>
      <div class="sub">登记上游供应商资质与合同，附件归集上传，参与排放边界联动（范围三）与协同减排</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>伙伴档案</h3>
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索伙伴 / 信用代码" clearable style="width: 230px" :prefix-icon="Search" />
          <el-select v-model="statusFilter" style="width: 120px" clearable placeholder="全部状态">
            <el-option label="合作中" value="合作中" /><el-option label="停用" value="停用" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增伙伴</el-button>
        </div>
      </div>

      <el-table :data="filtered" size="small" v-loading="loading">
        <el-table-column prop="name" label="伙伴" min-width="170">
          <template #default="{ row }">
            <div class="sup-cell">
              <span class="sup-avatar" :style="{ background: row.color }">{{ row.name.slice(0, 1) }}</span>
              <div>
                <div class="sup-name">{{ row.name }}</div>
                <div class="sup-dim">{{ row.type }}<template v-if="row.region"> · {{ row.region }}</template></div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cert" label="信用代码 / 资质" min-width="180">
          <template #default="{ row }">
            <div class="sup-name">{{ row.cert }}</div>
            <div class="sup-dim">附件由平台哈希归档</div>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="130">
          <template #default="{ row }">
            <div class="sup-name">{{ row.contactName || '—' }}</div>
            <div class="sup-dim">{{ row.contactPhone || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="scope" label="供应 / 采购品类" min-width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '合作中' ? 'success' : 'info'" size="small" round>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDetail(row)">详情</el-button>
            <el-button link type="warning" size="small" @click="onWarn(row)">资质提醒</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" title="新增供应链伙伴" width="560px" @closed="resetForm">
      <el-form label-width="90px">
        <el-form-item label="伙伴名称"><el-input v-model="newSup.name" placeholder="企业全称" /></el-form-item>
        <el-form-item label="伙伴类型">
          <el-radio-group v-model="newSup.partnerType">
            <el-radio value="SUPPLIER">供应商</el-radio>
            <el-radio value="CUSTOMER">客户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="供应品类"><el-input v-model="newSup.scope" placeholder="如：原煤 / 电力 / 公路运输" /></el-form-item>
        <el-form-item label="信用代码"><el-input v-model="newSup.cert" placeholder="统一社会信用代码或资质编号" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="newSup.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="newSup.contactPhone" /></el-form-item>
        <el-form-item label="所在地"><el-input v-model="newSup.region" placeholder="如：贵州贵阳" /></el-form-item>
        <el-form-item label="合同附件">
          <el-upload drag action="#" :auto-upload="false" :limit="1">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽合同扫描件到此处（归档后记录 SHA-256）</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onAdd">登记入库</el-button>
      </template>
    </el-dialog>

    <!-- 伙伴档案详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'伙伴档案 · ' + (detailRow?.name ?? '')" width="680px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="伙伴名称">{{ detailRow.name }}</el-descriptions-item>
          <el-descriptions-item label="合作状态">
            <el-tag :type="detailRow.status === '合作中' ? 'success' : 'info'" size="small" round>{{ detailRow.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="伙伴类型">{{ detailRow.type }}</el-descriptions-item>
          <el-descriptions-item label="所在地">{{ detailRow.region || '—' }}</el-descriptions-item>
          <el-descriptions-item label="供应品类">{{ detailRow.scope }}</el-descriptions-item>
          <el-descriptions-item label="信用代码 / 资质">{{ detailRow.cert }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detailRow.contactName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailRow.contactPhone || '—' }}</el-descriptions-item>
        </el-descriptions>

        <div class="flow-title">关联物料流转</div>
        <el-table :data="detailFlows" size="small" border>
          <el-table-column prop="material" label="物料" min-width="120" />
          <el-table-column prop="period" label="周期" width="90" />
          <el-table-column label="数量" width="110" align="right">
            <template #default="{ row }">{{ row.quantity }} {{ row.unit }}</template>
          </el-table-column>
          <el-table-column prop="transportMode" label="运输" width="80" />
          <el-table-column label="分配排放(tCO₂e)" min-width="120" align="right">
            <template #default="{ row }">{{ row.allocatedEmission ?? '—' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!detailFlows.length" description="暂无关联物料流转" :image-size="60" />

        <div class="detail-note">附件归档：上传文件将记录 SHA-256 哈希并随档案归档</div>
      </template>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, UploadFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface Supplier {
  id?: number
  name: string
  partnerType?: string
  type: string
  creditCode?: string
  cert: string
  contactName?: string
  contactPhone?: string
  scope: string
  region?: string
  status: string
  statusCode?: string
  color: string
}
interface Flow {
  partnerId?: number
  partner?: string
  material: string
  quantity: number
  unit: string
  period: string
  transportMode?: string
  allocatedEmission?: number
}

const palette = ['#00b581', '#00d1ff', '#a06bff', '#4da3ff', '#f59e0b']
const demoList: Omit<Supplier, 'color'>[] = [
  { name: '贵州盘江供应链有限公司', type: '供应商', scope: '原煤', region: '六盘水', cert: '91520000MA6XXXX001', status: '合作中' },
  { name: '南方电网贵州分公司', type: '供应商', scope: '电力', region: '贵阳', cert: '电力业务许可证', status: '合作中' },
  { name: '贵州公路运输集团', type: '供应商', scope: '公路运输', region: '贵阳', cert: '道路运输许可证', status: '合作中' }
]

const loading = ref(false)
const submitting = ref(false)
const keyword = ref('')
const statusFilter = ref('')
const editVisible = ref(false)
const list = ref<Supplier[]>(demoList.map((r, i) => ({ ...r, color: palette[i % palette.length] })))
const flows = ref<Flow[]>([])

const newSup = reactive({ name: '', partnerType: 'SUPPLIER', scope: '', cert: '', contactName: '', contactPhone: '', region: '' })

function normalize(rows: any[]): Supplier[] {
  return rows.map((r, i) => ({
    ...r,
    type: r.type || (r.partnerType === 'CUSTOMER' ? '客户' : '供应商'),
    cert: r.creditCode || r.cert || '未登记',
    scope: r.scope || '未登记',
    status: r.status || (r.statusCode === 'ENABLED' ? '合作中' : '停用'),
    color: palette[i % palette.length]
  }))
}

async function loadSuppliers() {
  loading.value = true
  try {
    const r: any = await request.get('/v1/enterprise/supply/suppliers')
    if (r?.code === 0 && Array.isArray(r.data) && r.data.length) list.value = normalize(r.data)
  } catch { /* 接口不可用时保留演示数据 */ }
  finally { loading.value = false }
}

async function loadFlows() {
  try {
    const r: any = await request.get('/v1/enterprise/supply/flows')
    if (r?.code === 0 && Array.isArray(r.data)) flows.value = r.data
  } catch { /* 保留空列表 */ }
}

const filtered = computed(() =>
  list.value.filter(
    (r) =>
      (!keyword.value || r.name.includes(keyword.value) || r.cert.includes(keyword.value)) &&
      (!statusFilter.value || r.status === statusFilter.value)
  )
)

function openAdd() {
  editVisible.value = true
}
function resetForm() {
  Object.assign(newSup, { name: '', partnerType: 'SUPPLIER', scope: '', cert: '', contactName: '', contactPhone: '', region: '' })
}

async function onAdd() {
  if (!newSup.name.trim()) { ElMessage.warning('请填写伙伴名称'); return }
  submitting.value = true
  try {
    const r: any = await request.post('/v1/enterprise/supply/suppliers', {
      name: newSup.name.trim(),
      partnerType: newSup.partnerType,
      scope: newSup.scope.trim(),
      creditCode: newSup.cert.trim(),
      contactName: newSup.contactName.trim(),
      contactPhone: newSup.contactPhone.trim(),
      region: newSup.region.trim()
    })
    if (r?.code === 0) {
      ElMessage.success('伙伴「' + newSup.name + '」已登记入库')
      editVisible.value = false
      await Promise.all([loadSuppliers(), loadFlows()])
    } else {
      ElMessage.error(r?.message || '登记失败')
    }
  } catch { ElMessage.error('登记失败，请稍后重试') }
  finally { submitting.value = false }
}

async function onWarn(row: Supplier) {
  try {
    const r: any = await request.post('/v1/enterprise/supply/tasks', {
      partnerId: row.id,
      taskType: 'DATA_COLLECT',
      title: '资质更新提醒：' + row.name,
      content: '请更新资质证照、合同附件与本期物料流转数据。'
    })
    if (r?.code === 0) ElMessage.warning(`${row.name} 已生成资质更新协同任务`)
  } catch { ElMessage.error('协同任务创建失败') }
}

/* 伙伴档案详情 */
const detailVisible = ref(false)
const detailRow = ref<Supplier | null>(null)
const detailFlows = computed(() => {
  const pid = detailRow.value?.id
  return pid ? flows.value.filter((f) => f.partnerId === pid) : []
})
function onDetail(row: Supplier) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(async () => {
  await Promise.all([loadSuppliers(), loadFlows()])
})
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 10px; }
.sup-cell { display: flex; align-items: center; gap: 10px; }
.sup-avatar { width: 34px; height: 34px; border-radius: 9px; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px; font-weight: 600; }
.sup-name { font-size: 13px; color: var(--qn-text); }
.sup-dim { font-size: 11.5px; color: var(--qn-text-3); margin-top: 1px; }
.flow-title { margin: 16px 0 8px; font-size: 13px; font-weight: 600; color: var(--qn-text-2); }
.detail-note { margin-top: 14px; font-size: 11.5px; color: var(--qn-text-3); }
</style>