<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">供应链协同</div>
      <div class="sub">登记上游供应商资质与合同，附件归集上传，参与排放边界联动（范围三）与协同减排</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>供应商档案</h3>
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="搜索供应商 / 资质编号" clearable style="width: 230px" :prefix-icon="Search" />
          <el-select v-model="statusFilter" style="width: 120px" clearable placeholder="全部状态">
            <el-option label="合作中" value="合作中" /><el-option label="待审核" value="待审核" /><el-option label="资质预警" value="资质预警" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="editVisible = true">新增供应商</el-button>
        </div>
      </div>

      <el-table :data="filtered" size="small">
        <el-table-column prop="name" label="供应商" min-width="170">
          <template #default="{ row }">
            <div class="sup-cell">
              <span class="sup-avatar" :style="{ background: row.color }">{{ row.name.slice(0, 1) }}</span>
              <div><div class="sup-name">{{ row.name }}</div><div class="sup-dim">{{ row.type }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cert" label="资质证书" width="190">
          <template #default="{ row }">
            <div class="sup-name">{{ row.cert }}</div>
            <div class="sup-dim">有效期至 {{ row.expire }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="contract" label="合同" width="180">
          <template #default="{ row }"><div class="sup-name">{{ row.contract }}</div><div class="sup-dim">金额 {{ row.amount }}</div></template>
        </el-table-column>
        <el-table-column prop="scope" label="供应品类" width="150" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagMap[row.status]" size="small" round>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onDetail(row)">详情</el-button>
            <el-button link type="danger" size="small" @click="onWarn(row)">预警</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editVisible" title="新增供应商" width="560px">
      <el-form label-width="90px">
        <el-form-item label="供应商名称"><el-input v-model="newSup.name" /></el-form-item>
        <el-form-item label="供应品类"><el-input v-model="newSup.scope" /></el-form-item>
        <el-form-item label="资质证书"><el-input v-model="newSup.cert" placeholder="如：营业执照 / ISO14001 / 采矿许可证" /></el-form-item>
        <el-form-item label="合同附件">
          <el-upload drag action="#" :auto-upload="false" :limit="1">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽合同扫描件到此处</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="onAdd">登记入库</el-button>
      </template>
    </el-dialog>

    <!-- 供应商档案详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="'供应商档案 · ' + (detailRow?.name ?? '')" width="640px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="供应商名称">{{ detailRow.name }}</el-descriptions-item>
          <el-descriptions-item label="合作状态">
            <el-tag :type="tagMap[detailRow.status]" size="small" round>{{ detailRow.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="供应商类型" :span="2">{{ detailRow.type }}</el-descriptions-item>
          <el-descriptions-item label="供应品类">{{ detailRow.scope }}</el-descriptions-item>
          <el-descriptions-item label="资质证书">{{ detailRow.cert }}</el-descriptions-item>
          <el-descriptions-item label="证书有效期">{{ detailRow.expire }}</el-descriptions-item>
          <el-descriptions-item label="合同编号">{{ detailRow.contract }}</el-descriptions-item>
          <el-descriptions-item label="合同金额">{{ detailRow.amount }}</el-descriptions-item>
          <el-descriptions-item label="归档附件">合同扫描件、资质证照副本</el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="detailRow.status === '资质预警'" type="warning" show-icon :closable="false" style="margin-top: 14px"
          :title="'资质预警：' + detailRow.cert + ' 将于 ' + detailRow.expire + ' 到期，请尽快联系供应商更新证照'" />
        <div class="detail-note">附件归档：上传文件将记录 SHA-256 哈希并随档案归档</div>
      </template>
      <template #footer><el-button type="primary" @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, UploadFilled } from '@element-plus/icons-vue'

const keyword = ref('')
const statusFilter = ref('')
const editVisible = ref(false)
const newSup = reactive({ name: '', scope: '', cert: '' })

const list = [
  { name: '贵州中煤供应链有限公司', type: '原煤 / 精煤供应商', cert: '采矿许可证 C520000X08', expire: '2026-12-05', contract: 'HT-2026-118', amount: '420 万元', scope: '原煤', status: '资质预警', color: '#f59e0b' },
  { name: '中国南方电网贵州公司', type: '电力供应', cert: '电力业务许可证', expire: '2029-06-30', contract: 'HT-2025-204', amount: '年度协议', scope: '电力', status: '合作中', color: '#00b581' },
  { name: '黔通物流运输有限公司', type: '运输服务商', cert: '道路运输许可证 5201', expire: '2027-03-21', contract: 'HT-2026-031', amount: '180 万元', scope: '公路运输', status: '合作中', color: '#00d1ff' },
  { name: '贵州精工设备有限公司', type: '设备 / 备件', cert: 'ISO9001:2015', expire: '2026-11-14', contract: 'HT-2026-045', amount: '96 万元', scope: '备件', status: '待审核', color: '#a06bff' },
  { name: '贵州乌江航运物流有限公司', type: '水运服务商', cert: '水路运输许可证 5203', expire: '2028-05-18', contract: 'HT-2026-052', amount: '142 万元', scope: '水路运输', status: '合作中', color: '#4da3ff' },
  { name: '贵州磷化集团有限责任公司', type: '化工原料供应商', cert: '安全生产许可证（黔）WH-0217', expire: '2027-09-30', contract: 'HT-2026-067', amount: '260 万元', scope: '硫酸 / 磷酸', status: '合作中', color: '#00b581' },
  { name: '西南水泥有限公司贵州分公司', type: '建材供应商', cert: 'ISO14001:2015', expire: '2027-11-02', contract: 'HT-2026-073', amount: '88 万元', scope: '水泥 / 熟料', status: '合作中', color: '#00d1ff' },
  { name: '贵州钢绳股份有限公司', type: '金属材料供应商', cert: 'ISO9001:2015', expire: '2026-12-22', contract: 'HT-2026-081', amount: '134 万元', scope: '钢丝绳 / 支护材料', status: '资质预警', color: '#f59e0b' },
  { name: '贵州黔能煤化工有限责任公司', type: '焦炭 / 煤化工产品供应商', cert: '安全生产许可证（黔）WH-0341', expire: '2028-02-14', contract: 'HT-2026-090', amount: '310 万元', scope: '焦炭', status: '合作中', color: '#a06bff' },
  { name: '遵义铝业股份有限公司', type: '金属材料供应商', cert: '排污许可证 91520300MA6DK', expire: '2027-06-11', contract: 'HT-2026-096', amount: '205 万元', scope: '铝锭 / 铝型材', status: '待审核', color: '#4da3ff' },
  { name: '贵州绿源环保科技有限公司', type: '环保服务商', cert: '环境污染治理资质 乙级', expire: '2027-01-25', contract: 'HT-2026-102', amount: '64 万元', scope: '脱硫剂 / 环保耗材', status: '合作中', color: '#00b581' }
]
const tagMap: Record<string, string> = { 合作中: 'success', 待审核: 'info', 资质预警: 'warning' }

const filtered = computed(() =>
  list.filter(
    (r) => (!keyword.value || r.name.includes(keyword.value)) && (!statusFilter.value || r.status === statusFilter.value)
  )
)

function onWarn(row: { name: string; expire: string }) {
  ElMessage.warning(`${row.name} 的资质将于 ${row.expire} 到期，已自动置为「资质预警」并提醒责任人`)
}
function onAdd() {
  ElMessage.success('供应商「' + newSup.name + '」登记入库，附件待归档')
  editVisible.value = false
}

/* 供应商档案详情 */
const detailVisible = ref(false)
const detailRow = ref<(typeof list)[number] | null>(null)
function onDetail(row: (typeof list)[number]) {
  detailRow.value = row
  detailVisible.value = true
}
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 10px; }
.sup-cell { display: flex; align-items: center; gap: 10px; }
.sup-avatar { width: 34px; height: 34px; border-radius: 9px; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 15px; font-weight: 600; }
.sup-name { font-size: 13px; color: var(--qn-text); }
.sup-dim { font-size: 11.5px; color: var(--qn-text-3); margin-top: 1px; }
.detail-note { margin-top: 14px; font-size: 11.5px; color: var(--qn-text-3); }
</style>