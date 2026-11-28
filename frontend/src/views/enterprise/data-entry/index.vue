<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">碳数据填报</div>
      <div class="sub">按月填报原煤消耗、外购电力与运输周转量，系统按 F1 = AD × EF × GWP 自动核算并分范围入库</div>
    </div>

    <div class="entry-grid">
      <!-- 左：填报表单 -->
      <div class="page-card">
        <div class="page-card-title"><h3>2026 年 10 月 · 活动数据</h3>
          <el-tag type="success" size="small" round :title="formulaHint('F1')">核算公式 F1</el-tag>
        </div>
        <el-form :model="form" label-width="130px" label-position="left">
          <el-divider content-position="left"><span class="scope-tag scope1">范围一 · 直接排放（原煤燃烧）</span></el-divider>
          <el-form-item label="原煤消耗量" required>
            <el-input-number v-model="form.coal" :min="0" :step="100" style="width: 62%" controls-position="right" />
            <span class="unit">吨 / 月</span>
          </el-form-item>
          <el-form-item label="低位发热量">
            <el-input v-model="form.ncv" style="width: 62%" /><span class="unit">GJ/t（来自化验单）</span>
          </el-form-item>
          <el-divider content-position="left"><span class="scope-tag scope2">范围二 · 间接排放（外购电力）</span></el-divider>
          <el-form-item label="外购电量" required>
            <el-input-number v-model="form.power" :min="0" :step="1000" style="width: 62%" controls-position="right" />
            <span class="unit">kWh / 月</span>
          </el-form-item>
          <el-divider content-position="left"><span class="scope-tag scope3">范围三 · 运输排放</span></el-divider>
          <el-form-item label="运输周转量">
            <el-input-number v-model="form.freight" :min="0" :step="1000" style="width: 62%" controls-position="right" />
            <span class="unit">t·km / 月</span>
          </el-form-item>
          <el-form-item label="产品产量" required>
            <el-input-number v-model="form.output" :min="0" :step="100" style="width: 62%" controls-position="right" />
            <span class="unit">吨 / 月</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSave">保存草稿</el-button>
            <el-button type="success" plain @click="onSubmit">校验并提交</el-button>
            <el-button @click="onPreview">预览核算</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 右：历史记录 -->
      <div class="page-card">
        <div class="page-card-title">
          <h3>历史填报记录</h3>
          <div class="filters">
            <el-select v-model="year" style="width: 110px" size="small">
              <el-option v-for="y in [2026, 2025, 2024]" :key="y" :label="y + ' 年'" :value="y" />
            </el-select>
            <el-button size="small" type="primary" plain @click="exportRecords"><el-icon><Download /></el-icon>导出</el-button>
          </div>
        </div>
        <el-table :data="records" size="small" stripe>
          <el-table-column prop="period" label="核算期" width="92" />
          <el-table-column prop="coal" label="原煤(t)" />
          <el-table-column prop="power" label="电力(kWh)" />
          <el-table-column prop="freight" label="运输(t·km)" />
          <el-table-column prop="total" label="排放(tCO₂e)" sortable />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === '已提交' ? 'success' : 'warning'" size="small" round>{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formulaHint } from '@/utils/formulas'

const year = ref(2026)
const form = reactive({
  coal: 6800,
  ncv: '22.42',
  power: 462000,
  freight: 526000,
  output: 20000
})

const records = [
  { period: '2026-09', coal: 7420, power: 458000, freight: 630000, total: 12840.6, status: '已提交' },
  { period: '2026-08', coal: 7660, power: 451000, freight: 640000, total: 12431.2, status: '已提交' },
  { period: '2026-07', coal: 7920, power: 462000, freight: 650000, total: 13128.8, status: '已提交' },
  { period: '2026-06', coal: 8350, power: 459000, freight: 680000, total: 13602.5, status: '已提交' },
  { period: '2026-05', coal: 8120, power: 448000, freight: 660000, total: 13031.9, status: '已提交' },
  { period: '2026-04', coal: 7980, power: 436000, freight: 620000, total: 12791.3, status: '已退回' }
]

function validate(): boolean {
  if (!form.coal && !form.power) { ElMessage.warning('原煤消耗与外购电量至少填报一项'); return false }
  if (!form.output) { ElMessage.warning('产品产量为必填项，用于计算碳强度（F5）'); return false }
  if (!form.ncv) { ElMessage.info('建议先完成化验单识别以读取低位发热量，缺省将使用默认因子') }
  return true
}
function onSave() { ElMessage.success('草稿已保存到活动数据记录') }
function onSubmit() {
  if (!validate()) return
  ElMessage.success('校验通过，已按 F1 核算并提交省厅初审（哈希链存证块 +1）')
}
function onPreview() {
  if (!validate()) return
  const coalE = (form.coal * (parseFloat(form.ncv) || 22.42) * 26.37 * 0.93 * 44 / 12).toFixed(1)
  const powerE = (form.power * 0.5703 / 1000).toFixed(1)
  const freightE = (form.freight * 0.062 / 1000).toFixed(1)
  ElMessageBox.alert(
    `原煤排放：${coalE} tCO₂e（F2）\n外购电：${powerE} tCO₂e（F3）\n运输：${freightE} tCO₂e（F4）\n合计 ≈ ${(parseFloat(coalE) + parseFloat(powerE) + parseFloat(freightE)).toFixed(1)} tCO₂e`,
    '核算预览（F1~F4）',
    { confirmButtonText: '知道了' }
  )
}

function exportRecords() {
  const head = ['核算期', '原煤(t)', '电力(kWh)', '运输(t·km)', '排放(tCO₂e)', '状态']
  const body = records.map(r => [r.period, r.coal, r.power, r.freight, r.total, r.status])
  const csv = '\uFEFF' + [head, ...body].map(row => row.map(c => String(c)).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `碳数据填报记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('填报记录已导出：' + a.download + '（共 ' + records.length + ' 期）')
}
</script>

<style scoped lang="scss">
.entry-grid { display: grid; grid-template-columns: 5fr 6fr; gap: 16px; align-items: start; }
.unit { margin-left: 10px; color: var(--qn-text-3); font-size: 12px; }
.scope-tag { font-size: 12.5px; font-weight: 600; letter-spacing: 1px;
  &.scope1 { color: #00a97e; } &.scope2 { color: #00a3d1; } &.scope3 { color: #a06bff; }
}
.filters { display: flex; gap: 8px; }
@media (max-width: 1100px) { .entry-grid { grid-template-columns: 1fr; } }
</style>