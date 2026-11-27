<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">省级因子库管理</div>
      <div class="sub">燃料 / 电力 / 运输排放因子统一版本管理（v3.2），全省企业核算统一引用，修订留痕并推送到各企业</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>因子清单 · 版本 v3.2（2026-09-01 生效）</h3>
        <div class="toolbar">
          <el-button size="small" plain @click="diffVisible = true"><el-icon><RefreshRight /></el-icon>比对版本</el-button>
          <el-button size="small" type="primary" :icon="Plus" @click="onAdd">新增因子</el-button>
        </div>
      </div>

      <el-tabs v-model="tab">
        <el-tab-pane label="化石燃料" name="fuel" />
        <el-tab-pane label="电力 / 热力" name="power" />
        <el-tab-pane label="交通运输" name="transport" />
        <el-tab-pane label="版本记录" name="ver" />
      </el-tabs>

      <el-table v-if="tab !== 'ver'" :data="rowsOf(tab)" size="small" stripe>
        <el-table-column prop="code" label="因子编码" width="140" />
        <el-table-column prop="name" label="因子名称" min-width="190" />
        <el-table-column prop="value" label="因子值" width="140" sortable />
        <el-table-column prop="unit" label="单位" width="130">
          <template #default="{ row }"><span class="mono">{{ row.unit }}</span></template>
        </el-table-column>
        <el-table-column prop="gwp" label="GWP" width="100" />
        <el-table-column prop="valid" label="有效期" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === '现行' ? 'success' : 'info'" round>{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === '历史' ? 'success' : 'danger'" size="small" @click="toggleFactor(row)">{{ row.status === '历史' ? '启用' : '停用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-timeline v-else style="margin-top: 16px">
        <el-timeline-item type="success" timestamp="2026-09-01 · 李处长" >v3.2 发布：南方电网电力因子更新为 0.5703 tCO₂e/MWh</el-timeline-item>
        <el-timeline-item timestamp="2026-03-15 · 王科员" >v3.1 发布：新增磷酸铁锂工艺因子组</el-timeline-item>
        <el-timeline-item timestamp="2025-11-20 · 李处长" >v3.0 发布：对标 GB/T 24067，全库切换新版口径</el-timeline-item>
      </el-timeline>
    </div>

    <!-- 版本比对弹窗 -->
    <el-dialog v-model="diffVisible" title="版本比对 v3.1 → v3.2" width="760px">
      <el-table :data="versionDiff" size="small" border>
        <el-table-column prop="code" label="因子编码" width="150" />
        <el-table-column prop="name" label="因子名称" min-width="180" />
        <el-table-column label="旧值（v3.1）" min-width="180">
          <template #default="{ row }"><span class="old-val mono">{{ row.old }}</span></template>
        </el-table-column>
        <el-table-column label="新值（v3.2）" min-width="180">
          <template #default="{ row }"><span class="new-val mono">{{ row.new }}</span></template>
        </el-table-column>
      </el-table>
      <div class="diff-note"><el-icon><InfoFilled /></el-icon>v3.2 于 2026-09-01 生效，全部变更经修订留痕（前后快照入审计中心），并已推送至全省企业核算引擎</div>
      <template #footer><el-button type="primary" @click="diffVisible = false">关闭</el-button></template>
    </el-dialog>

    <!-- 新增 / 编辑因子弹窗 -->
    <el-dialog v-model="factorDlg" :title="editingRow ? '编辑因子 · ' + editingRow.code : '新增因子 · 版本 v3.2'" width="560px">
      <el-form :model="factorForm" label-width="90px">
        <el-form-item label="因子编码"><el-input v-model="factorForm.code" :disabled="!!editingRow" placeholder="如 E-GRID-GZ" /></el-form-item>
        <el-form-item label="因子名称"><el-input v-model="factorForm.name" placeholder="如 南方电网（贵州）" /></el-form-item>
        <el-form-item label="因子值"><el-input v-model="factorForm.value" placeholder="如 0.5703" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="factorForm.unit" placeholder="如 tCO₂e/MWh" /></el-form-item>
        <el-form-item label="GWP"><el-input-number v-model="factorForm.gwp" :min="1" :step="1" /></el-form-item>
        <el-form-item label="适用分类">
          <el-select v-model="factorForm.cat" :disabled="!!editingRow" style="width: 100%">
            <el-option v-for="c in catOptions" :key="c.key" :label="c.label" :value="c.key" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="factorDlg = false">取消</el-button>
        <el-button type="primary" @click="saveFactor">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, RefreshRight } from '@element-plus/icons-vue'

interface FactorRow { code: string; name: string; value: string; unit: string; gwp: number; valid: string; status: string }

const tab = ref('fuel')
const data: Record<string, FactorRow[]> = {
  fuel: [
    { code: 'F-COAL-WC', name: '无烟煤（贵州）', value: '98.4', unit: 'tCO₂/TJ · NCV 缺省', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-COAL-BC', name: '烟煤（化验单优先）', value: '95.1', unit: 'tCO₂/TJ', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-DIESEL', name: '柴油', value: '74.1', unit: 'tCO₂/TJ', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-GAS-MINE', name: '煤矿瓦斯（CH₄）', value: '25', unit: 'tCO₂e/tCH₄', gwp: 25, valid: '长期', status: '现行' },
    { code: 'F-GAS-NG', name: '天然气', value: '2162', unit: 'kgCO₂e/千m³', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-COKE', name: '焦炭', value: '2860', unit: 'kgCO₂e/t', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-GASOLINE', name: '汽油', value: '69.3', unit: 'tCO₂/TJ', gwp: 1, valid: '长期', status: '现行' },
    { code: 'F-LPG', name: '液化石油气', value: '63.1', unit: 'tCO₂/TJ', gwp: 1, valid: '长期', status: '现行' },
    { code: 'P-LIME', name: '石灰石分解（工业过程）', value: '440', unit: 'kgCO₂e/t', gwp: 1, valid: '长期', status: '现行' },
    { code: 'P-CLINKER', name: '水泥熟料（工业过程）', value: '530', unit: 'kgCO₂e/t', gwp: 1, valid: '长期', status: '现行' },
    { code: 'P-AL-ANODE', name: '电解铝（含阳极效应）', value: '650', unit: 'kgCO₂e/t', gwp: 1, valid: '长期', status: '现行' }
  ],
  power: [
    { code: 'E-GRID-SC', name: '南方电网（贵州）', value: '0.5703', unit: 'tCO₂e/MWh', gwp: 1, valid: '2026-09-01 ~', status: '现行' },
    { code: 'E-GRID-SC-OLD', name: '南方电网（贵州）旧值', value: '0.5810', unit: 'tCO₂e/MWh', gwp: 1, valid: '~2026-08-31', status: '历史' },
    { code: 'E-HEAT', name: '外购热力', value: '0.110', unit: 'tCO₂e/GJ', gwp: 1, valid: '长期', status: '现行' },
    { code: 'E-HEAT-OLD', name: '外购热力旧值', value: '0.112', unit: 'tCO₂e/GJ', gwp: 1, valid: '~2026-08-31', status: '历史' },
    { code: 'E-GRID-CN', name: '全国电网平均', value: '0.5568', unit: 'tCO₂e/MWh', gwp: 1, valid: '长期', status: '现行' },
    { code: 'E-WIND', name: '风电（省内绿电直购）', value: '0', unit: 'tCO₂e/MWh', gwp: 1, valid: '长期', status: '现行' },
    { code: 'E-SOLAR', name: '光伏发电（绿电直购）', value: '0', unit: 'tCO₂e/MWh', gwp: 1, valid: '长期', status: '现行' },
    { code: 'E-HYDRO', name: '水电（乌江流域绿电）', value: '0', unit: 'tCO₂e/MWh', gwp: 1, valid: '长期', status: '现行' }
  ],
  transport: [
    { code: 'T-ROAD-H', name: '公路重型货车', value: '0.062', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-RAIL', name: '铁路货运', value: '0.010', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-WATER', name: '水路（乌江航线）', value: '0.009', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-ROAD-M', name: '公路中型货车', value: '0.115', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-ROAD-L', name: '公路轻型货车', value: '0.180', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-RAIL-E', name: '铁路电气化（贵州段）', value: '0.008', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-WATER-YZ', name: '水路（长江干线出海段）', value: '0.006', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' },
    { code: 'T-AIR', name: '航空货运', value: '0.602', unit: 'tCO₂e/(t·100km)', gwp: 1, valid: '长期', status: '现行' }
  ],
  ver: []
}
const rowsOf = (k: string) => data[k] || []

/* ============ 版本比对 ============ */
const diffVisible = ref(false)
const versionDiff = [
  { code: 'E-GRID-SC', name: '南方电网（贵州）', old: '0.5810 tCO₂e/MWh', new: '0.5703 tCO₂e/MWh' },
  { code: 'E-HEAT', name: '外购热力', old: '0.112 tCO₂e/GJ', new: '0.110 tCO₂e/GJ' },
  { code: 'T-ROAD-H', name: '公路重型货车', old: '0.063 tCO₂e/(t·100km)', new: '0.062 tCO₂e/(t·100km)' },
  { code: 'T-RAIL', name: '铁路货运', old: '0.010 tCO₂e/(t·100km)', new: '0.010 tCO₂e/(t·100km)' }
]

/* ============ 新增 / 编辑因子 ============ */
const catOptions = [
  { key: 'fuel', label: '化石燃料' },
  { key: 'power', label: '电力 / 热力' },
  { key: 'transport', label: '交通运输' }
]
const factorDlg = ref(false)
const editingRow = ref<FactorRow | null>(null)
const factorForm = reactive({ code: '', name: '', value: '', unit: 'tCO₂e/t', gwp: 1, cat: 'fuel' })

function onAdd() {
  editingRow.value = null
  const cat = tab.value === 'ver' ? 'fuel' : tab.value
  Object.assign(factorForm, { code: '', name: '', value: '', unit: 'tCO₂e/t', gwp: 1, cat })
  factorDlg.value = true
}
function openEdit(row: FactorRow) {
  editingRow.value = row
  const cat = tab.value === 'ver' ? 'fuel' : tab.value
  Object.assign(factorForm, { code: row.code, name: row.name, value: row.value, unit: row.unit, gwp: row.gwp, cat })
  factorDlg.value = true
}
function saveFactor() {
  if (!factorForm.code || !factorForm.name || factorForm.value === '') { ElMessage.warning('请填写因子编码、名称与因子值'); return }
  if (editingRow.value) {
    Object.assign(editingRow.value, { code: factorForm.code, name: factorForm.name, value: String(factorForm.value), unit: factorForm.unit, gwp: factorForm.gwp })
    factorDlg.value = false
    ElMessage.success('因子「' + factorForm.code + '」已更新，修订留痕并推送至全省企业')
  } else {
    data[factorForm.cat].push({ code: factorForm.code, name: factorForm.name, value: String(factorForm.value), unit: factorForm.unit, gwp: factorForm.gwp, valid: '长期', status: '现行' })
    tab.value = factorForm.cat
    factorDlg.value = false
    ElMessage.success('因子「' + factorForm.code + '」已加入 v3.2 因子库，全省企业核算即时引用')
  }
}
function toggleFactor(row: FactorRow) {
  if (row.status === '历史') {
    row.status = '现行'
    ElMessage.success('因子「' + row.code + '」已重新启用')
  } else {
    row.status = '历史'
    ElMessage.success('因子「' + row.code + '」已停用（状态：历史，已引用该因子的历史核算记录不受影响）')
  }
}
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 10px; }
.mono { font-family: monospace; font-size: 12.5px; }
.old-val { color: #e04635; }
.new-val { color: #009c6d; }
.diff-note { margin-top: 12px; font-size: 11.5px; color: var(--qn-text-3); display: flex; align-items: center; gap: 5px; }
</style>