<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">数据包接收</div>
      <div class="sub">{{ auth.user?.orgName }} ｜ 接收企业出海数据包（CBAM / ISO 14067），分派复核员执行国际标准复核</div>
    </div>

    <div class="stat-grid">
      <StatCard label="待接收" :value="list.length" icon="Box" tint="#ffb020" />
      <StatCard label="复核中" :value="5" icon="Refresh" tint="#00d1ff" />
      <StatCard label="本月签发护照" :value="12" icon="Medal" />
      <StatCard label="平均复核时长" :value="4.2" suffix=" 天" icon="Timer" tint="#a06bff" />
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>待接收数据包</h3>
        <div class="toolbar">
          <el-input v-model="kw" placeholder="搜索企业 / 文件名" clearable style="width: 220px" :prefix-icon="Search" />
          <el-button size="small" plain @click="batchDownload">批量下载</el-button>
        </div>
      </div>
      <el-table :data="filtered" size="small">
        <el-table-column prop="tpl" label="模板" width="110">
          <template #default="{ row }"><el-tag size="small" round :type="row.tpl === 'CBAM' ? 'primary' : 'success'">{{ row.tpl }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="file" label="数据包" min-width="220">
          <template #default="{ row }"><div class="file-name">{{ row.file }}</div><div class="file-hash">存证哈希：{{ row.hash }}</div></template>
        </el-table-column>
        <el-table-column prop="company" label="企业" min-width="170" />
        <el-table-column prop="period" label="核算期" width="90" />
        <el-table-column prop="size" label="大小" width="80" />
        <el-table-column prop="time" label="提交时间" width="130" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onAccept(row)">接收</el-button>
            <el-button link size="small" @click="onDownload(row)">下载</el-button>
            <el-button link type="success" size="small" @click="onAssign(row)">分派复核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const kw = ref('')

const list = [
  { tpl: 'CBAM', file: 'CBAM-2026Q3-黔能矿业.xlsx', hash: '1f3a8c2e…9d41', company: '贵州黔能矿业有限责任公司', period: '2026-Q3', size: '48KB', time: '2026-11-25 14:02' },
  { tpl: 'ISO', file: 'ISO14067-2026-09-磷化集团.xlsx', hash: '7d1c04ab…e2f5', company: '贵州磷化集团有限责任公司', period: '2026-09', size: '41KB', time: '2026-11-24 09:18' },
  { tpl: 'CBAM', file: 'CBAM-2026Q3-遵义铝业.xlsx', hash: '0b7e26fa…3c88', company: '遵义铝业股份有限公司', period: '2026-Q3', size: '52KB', time: '2026-11-23 17:44' },
  { tpl: 'CBAM', file: 'CBAM-2026Q3-中伟新材料.xlsx', hash: 'a94d17c0…6b2e', company: '中伟新材料股份有限公司', period: '2026-Q3', size: '57KB', time: '2026-11-23 10:26' },
  { tpl: 'ISO', file: 'ISO14067-2026-10-振华新材料.xlsx', hash: '3e8b52d9…c107', company: '贵州振华新材料股份有限公司', period: '2026-10', size: '44KB', time: '2026-11-22 16:03' },
  { tpl: 'CBAM', file: 'CBAM-2026Q3-贵州钢绳.xlsx', hash: 'c62f90a1…8d3b', company: '贵州钢绳股份有限公司', period: '2026-Q3', size: '49KB', time: '2026-11-22 09:37' },
  { tpl: 'ISO', file: 'ISO14067-2026-10-西南水泥.xlsx', hash: '58ac31e7…f904', company: '贵州西南水泥有限公司', period: '2026-10', size: '46KB', time: '2026-11-21 15:12' },
  { tpl: 'CBAM', file: 'CBAM-2026Q3-黔能煤化工.xlsx', hash: 'e07b46d2…1a5c', company: '贵州黔能煤化工有限责任公司', period: '2026-Q3', size: '54KB', time: '2026-11-21 11:08' },
  { tpl: 'ISO', file: 'ISO14067-2026-09-乌江航运物流.xlsx', hash: '91d5c8f3…7e60', company: '贵州乌江航运物流有限公司', period: '2026-09', size: '38KB', time: '2026-11-20 14:55' }
]
const filtered = computed(() => list.filter((r) => !kw.value || r.company.includes(kw.value) || r.file.toLowerCase().includes(kw.value.toLowerCase())))

function onAccept(row: { file: string }) { ElMessage.success('已接收「' + row.file + '」，进入复核队列') }
function onDownload(row: { tpl: string; file: string; hash: string; company: string; period: string; size: string; time: string }) {
  downloadJson(row.file.replace(/\.xlsx$/i, '') + '.json', {
    模板: row.tpl, 数据包: row.file, 存证哈希: row.hash, 企业: row.company,
    核算期: row.period, 大小: row.size, 提交时间: row.time,
    说明: '数据包内容清单与哈希校验值（.xlsx 由后端导出服务生成）'
  })
  ElMessage.success('已下载「' + row.file + '」的数据包元数据')
}
function onAssign(row: { file: string }) { ElMessage.success('已分派复核员：高工（国际标准组），开始国际标准复核') }

function downloadJson(name: string, payload: unknown) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = name
  a.click()
  URL.revokeObjectURL(a.href)
}
function batchDownload() {
  downloadJson(`待接收数据包清单_${new Date().toISOString().slice(0, 10)}.json`, {
    导出时间: new Date().toLocaleString('zh-CN', { hour12: false }),
    数据包数量: filtered.value.length,
    清单: filtered.value.map(r => ({ 模板: r.tpl, 数据包: r.file, 存证哈希: r.hash, 企业: r.company, 核算期: r.period, 大小: r.size, 提交时间: r.time }))
  })
  ElMessage.success('已批量下载当前待接收清单（共 ' + filtered.value.length + ' 个数据包）')
}
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.toolbar { display: flex; gap: 10px; }
.file-name { font-size: 13px; color: var(--qn-text); }
.file-hash { font-size: 11px; color: var(--qn-text-3); font-family: monospace; margin-top: 2px; }
@media (max-width: 1100px) { .stat-grid { grid-template-columns: 1fr 1fr; } }
</style>