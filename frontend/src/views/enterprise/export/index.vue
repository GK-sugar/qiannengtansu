<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">出海数据包</div>
      <div class="sub">按 CBAM / ISO 14067 报表模板拼装核算结果，导出 Excel 数据包并生成存证哈希，供核查机构与欧盟客户核验</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>CBAM 数据包生成</h3>
      </div>
      <div class="gen-row">
        <el-form inline>
          <el-form-item label="核算期">
            <el-select v-model="period" style="width: 140px">
              <el-option v-for="p in ['2026-Q3', '2026-Q2', '2026-Q1', '2026-09', '2026-08']" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板">
            <el-select v-model="tpl" style="width: 180px">
              <el-option label="EU CBAM 过渡期报告" value="cbam" />
              <el-option label="ISO 14067 产品碳足迹" value="iso" />
              <el-option label="海关 AEO 绿色通道" value="aeo" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="genLoading" @click="onGenerate">
              <el-icon><MagicStick /></el-icon>&nbsp;拼装并生成 Excel
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-alert type="info" show-icon :closable="false" class="tip-alert"
        title="CSV 可直接用 Excel 打开；正式出海数据包由 /api/v1/export/cbam 生成带企业章与哈希清单的 .xlsx（存证于哈希链）。" />

      <!-- 报表预览 -->
      <el-table :data="preview" size="small" border class="mt16">
        <el-table-column prop="module" label="报告模块 / 字段" min-width="200" />
        <el-table-column prop="value" label="取值" min-width="200" />
        <el-table-column prop="source" label="数据来源" min-width="180" />
      </el-table>
    </div>

    <div class="hist-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>历史数据包</h3></div>
        <el-table :data="history" size="small">
          <el-table-column prop="name" label="文件名" min-width="210" />
          <el-table-column prop="period" label="核算期" width="90" />
          <el-table-column prop="size" label="大小" width="80" />
          <el-table-column prop="hash" label="存证哈希（前 12 位）" width="150">
            <template #default="{ row }"><span class="mono">{{ row.hash }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }"><el-button link type="primary" size="small" @click="onDownload(row)">下载</el-button></template>
          </el-table-column>
        </el-table>
      </div>
      <div class="page-card">
        <div class="page-card-title"><h3>CBAM 要点自查</h3></div>
        <el-timeline>
          <el-timeline-item type="success" timestamp="已满足">直接排放口径：吨 CO₂e / 吨产品</el-timeline-item>
          <el-timeline-item type="success" timestamp="已满足">隐含排放：外购电（范围二）单列</el-timeline-item>
          <el-timeline-item type="warning" timestamp="待补充">前体物（precursor）类别代码需贸易商确认</el-timeline-item>
          <el-timeline-item type="success" timestamp="已满足">生产路由、设施识别号（EPN）齐全</el-timeline-item>
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const period = ref('2026-Q3')
const tpl = ref('cbam')
const genLoading = ref(false)

const preview = computed(() => {
  if (tpl.value === 'cbam') return [
    { module: 'Reporting period', value: period.value, source: '核算期口径' },
    { module: 'CN code / 商品编码', value: '2701.12 · 烟煤', source: '产品档案' },
    { module: 'Direct emissions（直接排放）', value: '22,137.5 tCO₂e', source: 'F1·F2 核算', },
    { module: 'Indirect emissions（间接排放）', value: '8,412.9 tCO₂e', source: 'F3 外购电', },
    { module: 'Specific embedded emissions SEₑ', value: '0.642 tCO₂e/t', source: 'F5 碳强度' },
    { module: 'EF electricity（电力因子）', value: '0.5703 tCO₂e/MWh', source: '省级因子库 v3.2' },
    { module: 'Production volume（产量）', value: '42,600 t', source: '活动数据台账' }
  ]
  return [
    { module: '功能单位', value: '1 t 精煤', source: '产品档案' },
    { module: '系统边界', value: '矿山 → 洗选 → 外运（摇篮到厂门）', source: '边界声明' },
    { module: 'GWP 100 年', value: 'IPCC AR6', source: '方法学' },
    { module: 'CFP 结果', value: '0.642 tCO₂e/t', source: 'F5 碳强度' },
    { module: '数据质量 DQR', value: '良好（DQ ≥ 0.85）', source: 'DQR 评分' },
    { module: '核查状态', value: '待第三方复核', source: '护照流程' }
  ]
})

const history = [
  { name: 'CBAM-2026Q3-黔能矿业.xlsx', period: '2026-Q3', size: '48KB', hash: '1f3a8c2e…9d41' },
  { name: 'CBAM-2026Q2-黔能矿业.xlsx', period: '2026-Q2', size: '47KB', hash: '0b7e26fa…3c88' },
  { name: 'ISO14067-2026-09.xlsx', period: '2026-09', size: '36KB', hash: '7d1c04ab…e2f5' },
  { name: 'CBAM-2026Q1-黔能矿业.xlsx', period: '2026-Q1', size: '45KB', hash: 'a94d17c0…6b2e' },
  { name: 'ISO14067-2026-08.xlsx', period: '2026-08', size: '35KB', hash: '3e8b52d9…c107' },
  { name: 'CBAM-2026-08-黔能矿业.xlsx', period: '2026-08', size: '43KB', hash: 'c62f90a1…8d3b' },
  { name: 'ISO14067-2026-07.xlsx', period: '2026-07', size: '34KB', hash: '58ac31e7…f904' },
  { name: 'CBAM-2026-07-黔能矿业.xlsx', period: '2026-07', size: '42KB', hash: 'e07b46d2…1a5c' }
]

function onGenerate() {
  genLoading.value = true
  setTimeout(() => {
    const rows = [['字段', '取值'], ...preview.value.map((r) => [r.module, r.value])]
    const csv = '\uFEFF' + rows.map((r) => r.join(',')).join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `CBAM-${period.value}-黔能矿业.csv`
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('数据包已生成并下载：' + a.download + '（存证哈希 ' + '0x' + Array.from({ length: 10 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('') + '…）')
    genLoading.value = false
  }, 1200)
}

function onDownload(row: { name: string; period: string; size: string; hash: string }) {
  const rows = [
    ['字段', '取值'],
    ['文件名', row.name],
    ['核算期', row.period],
    ['文件大小', row.size],
    ['存证哈希', row.hash],
    ['模板', row.name.startsWith('CBAM') ? 'EU CBAM 过渡期报告' : 'ISO 14067 产品碳足迹'],
    ['说明', '数据包内容清单；正式 .xlsx 含企业章与哈希清单']
  ]
  const csv = '\uFEFF' + rows.map(r => r.map(c => '"' + String(c).replace(/"/g, '""') + '"').join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = row.name.replace(/\.xlsx$/i, '') + '.csv'
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('数据包已下载：' + a.download)
}
</script>

<style scoped lang="scss">
.gen-row { margin-bottom: 8px; }
.mt16 { margin-top: 16px; }
.tip-alert { margin-bottom: 4px; }
.hist-grid { display: grid; grid-template-columns: 7fr 4fr; gap: 16px; }
.mono { font-family: monospace; font-size: 12px; }
@media (max-width: 1100px) { .hist-grid { grid-template-columns: 1fr; } }
</style>