<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">审批意见书回传</div>
      <div class="sub">下载初审意见书 → 打印后加盖实体公章并签字 → 扫描为 PDF 上传回传；系统对回传文件做哈希存证</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>待处理审批意见书</h3>
        <el-tag size="small" round type="warning">{{ pendingCount }} 份待回传</el-tag>
      </div>
      <el-table :data="docs" size="small">
        <el-table-column prop="no" label="意见书编号" width="160" />
        <el-table-column prop="company" label="企业" min-width="180" />
        <el-table-column prop="period" label="核算期" width="90" />
        <el-table-column prop="reviewer" label="初审人" width="100" />
        <el-table-column prop="time" label="初审时间" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.returned ? 'success' : 'warning'" round>{{ row.returned ? '已回传' : '待回传' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="盖章文件" min-width="180">
          <template #default="{ row }">
            <div v-if="row.fileName" class="file-cell">
              <span class="file-name">{{ row.fileName }}</span>
              <span v-if="row.fileHash" class="file-hash mono">#{{ row.fileHash }}</span>
            </div>
            <span v-else class="dim">尚未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :icon="Download" @click="downloadDoc(row)">下载</el-button>
            <el-button link type="success" size="small" :icon="Upload" @click="openUpload(row)">上传</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flow-tip">
        <el-icon><InfoFilled /></el-icon>
        下载后请在打印窗口选择「另存为 PDF」，完成纸质盖章扫描后再上传；上传文件将记录 SHA-256 哈希并归档。
      </div>
      <input ref="fileInput" type="file" accept=".pdf,application/pdf" class="hidden-input" @change="onFilePicked" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, InfoFilled, Upload } from '@element-plus/icons-vue'
import { sha256Hex } from '@/utils/hash'

interface Doc {
  no: string
  company: string
  period: string
  reviewer: string
  time: string
  returned: boolean
  fileName?: string
  fileHash?: string
}

const docs = ref<Doc[]>([
  { no: 'QN-YJ-2026-118', company: '贵州黔能矿业有限责任公司', period: '2026-10', reviewer: '李处长', time: '2026-11-26 10:12', returned: false },
  { no: 'QN-YJ-2026-116', company: '贵州振华新材料股份有限公司', period: '2026-10', reviewer: '张副处', time: '2026-11-25 16:40', returned: false },
  { no: 'QN-YJ-2026-113', company: '贵州西南水泥有限公司', period: '2026-10', reviewer: '李处长', time: '2026-11-24 09:31', returned: true, fileName: 'QN-YJ-2026-113-盖章扫描件.pdf', fileHash: '8c1f3a72d9e04b65' },
  { no: 'QN-YJ-2026-112', company: '贵州黔能煤化工有限责任公司', period: '2026-10', reviewer: '张副处', time: '2026-11-23 15:08', returned: false },
  { no: 'QN-YJ-2026-110', company: '贵州钢绳股份有限公司', period: '2026-10', reviewer: '李处长', time: '2026-11-22 11:26', returned: true, fileName: 'QN-YJ-2026-110-盖章扫描件.pdf', fileHash: 'a73d90e21b8c4f06' },
  { no: 'QN-YJ-2026-108', company: '贵州乌江航运物流有限公司', period: '2026-10', reviewer: '赵科长', time: '2026-11-21 14:52', returned: true, fileName: 'QN-YJ-2026-108-盖章扫描件.pdf', fileHash: '4e92b6c08d17a35f' },
  { no: 'QN-YJ-2026-106', company: '铜仁金瑞锰业有限责任公司', period: '2026-10', reviewer: '张副处', time: '2026-11-20 10:05', returned: false },
  { no: 'QN-YJ-2026-104', company: '遵义铝业股份有限公司', period: '2026-09', reviewer: '李处长', time: '2026-11-18 16:33', returned: true, fileName: 'QN-YJ-2026-104-盖章扫描件.pdf', fileHash: 'f10c82d5a6e39b74' },
  { no: 'QN-YJ-2026-102', company: '中伟新材料股份有限公司', period: '2026-09', reviewer: '赵科长', time: '2026-11-17 09:47', returned: true, fileName: 'QN-YJ-2026-102-盖章扫描件.pdf', fileHash: '9b4e17c3f08a2d61' }
])

const pendingCount = computed(() => docs.value.filter((d) => !d.returned).length)
const fileInput = ref<HTMLInputElement>()
const uploadRow = ref<Doc | null>(null)

function esc(s: string) {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] || c))
}

function buildDocHtml(row: Doc) {
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<title>${esc(row.no)} · 审批意见书</title>
<style>
  @page { size: A4; margin: 20mm; }
  body { font-family: "Songti SC", "SimSun", "Microsoft YaHei", serif; color: #111; line-height: 2; font-size: 15px; }
  h1 { text-align: center; font-size: 24px; letter-spacing: 4px; margin: 0 0 8px; }
  .sub { text-align: center; font-size: 12px; color: #555; letter-spacing: 2px; margin-bottom: 28px; }
  .meta { display: flex; justify-content: space-between; font-size: 13px; color: #333; margin-bottom: 18px; }
  p { margin: 10px 0; text-indent: 2em; }
  .sign { margin-top: 70px; text-align: right; line-height: 2.4; }
  .seal-place { display: inline-block; width: 130px; height: 130px; border: 1px dashed #999; border-radius: 50%; color: #999; font-size: 12px; line-height: 130px; text-align: center; margin-top: 10px; }
  .foot { margin-top: 50px; font-size: 11px; color: #666; border-top: 1px solid #ddd; padding-top: 10px; }
</style></head><body>
<h1>贵州省重点产业碳足迹初审意见书</h1>
<div class="sub">CARBON FOOTPRINT PRELIMINARY REVIEW OPINION</div>
<div class="meta"><span>编号：${esc(row.no)}</span><span>初审时间：${esc(row.time)}</span></div>
<p>申请人：${esc(row.company)}</p>
<p>核算期：${esc(row.period)} ｜ 核算总量：13,840 tCO₂e</p>
<p>初审结论：依据 GB/T 24067 与省级因子库 v3.2 审核，数据波动率 6.2%，同意通过初审，转入第三方核查机构国际复核。</p>
<div class="sign">
  初审人：${esc(row.reviewer)}<br>
  日期：${esc(row.time.slice(0, 10))}<br>
  单位（盖章）：<div class="seal-place">加盖公章处</div>
</div>
<div class="foot">本意见书电子版由黔能碳溯平台生成；纸质文件加盖公章并回传扫描件后完成归档，文件哈希写入哈希链存证。</div>
</body></html>`
}

function downloadDoc(row: Doc) {
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;left:-10000px;top:0;width:800px;height:1100px;border:0;opacity:0;'
  document.body.appendChild(iframe)
  const doc = iframe.contentWindow?.document
  if (!doc) { fallbackDownload(row); iframe.remove(); return }
  doc.open()
  doc.write(buildDocHtml(row))
  doc.close()
  setTimeout(() => {
    try {
      iframe.contentWindow?.focus()
      iframe.contentWindow?.print()
      ElMessage.info('请在打印窗口选择「另存为 PDF」，打印盖章后再上传扫描件')
    } catch {
      fallbackDownload(row)
    } finally {
      setTimeout(() => iframe.remove(), 1000)
    }
  }, 350)
}

function fallbackDownload(row: Doc) {
  const blob = new Blob([buildDocHtml(row)], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${row.no}-意见书.html`
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 2000)
  ElMessage.warning('打印窗口不可用，已下载意见书文件；打开后可用浏览器「另存为 PDF」')
}

function openUpload(row: Doc) {
  uploadRow.value = row
  fileInput.value?.click()
}

async function onFilePicked(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  const row = uploadRow.value
  if (!file || !row) return
  if (!(file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf'))) {
    ElMessage.warning('请上传盖章后的 PDF 文件')
    input.value = ''
    return
  }
  row.fileName = file.name
  row.fileHash = (await sha256Hex(file.name + '|' + file.size + '|' + file.lastModified)).slice(0, 16)
  row.returned = true
  ElMessage.success(`已上传盖章扫描件：${file.name}，文件哈希已写入存证`)
  uploadRow.value = null
  input.value = ''
}
</script>

<style scoped lang="scss">
.file-cell { display: flex; flex-direction: column; gap: 2px; }
.file-name { font-size: 12.5px; color: var(--qn-text); word-break: break-all; }
.file-hash { font-size: 10.5px; color: var(--qn-text-3); }
.dim { font-size: 12px; color: var(--qn-text-3); }
.flow-tip {
  margin-top: 14px; display: flex; align-items: flex-start; gap: 6px;
  font-size: 11.5px; line-height: 1.7; color: var(--qn-text-3);
  background: #f5f9fc; border: 1px solid #e3edf3; border-radius: 10px; padding: 10px 14px;
  :deep(.el-icon) { margin-top: 2px; color: var(--qn-primary); flex: none; }
}
.hidden-input { display: none; }
</style>