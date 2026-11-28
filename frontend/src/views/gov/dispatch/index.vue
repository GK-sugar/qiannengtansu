<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">核查派单</div>
      <div class="sub">异常企业生成线下核查任务，由省厅执法核查员（胡工 / 周工 / 刘工，分黔中、黔北、黔南片区）赴现场；核查员手机打开 H5 打卡链接，GPS 定位 + 现场照片完成执法留痕（第三方核查机构仅参与国际标准复核，不参与执法打卡）</div>
    </div>

    <div class="stat-grid">
      <StatCard label="待派发" :value="6" icon="Clock" tint="#ffb020" />
      <StatCard label="执行中" :value="17" icon="Position" tint="#00d1ff" />
      <StatCard label="已完成" :value="54" icon="CircleCheck" />
      <StatCard label="逾期未打卡" :value="2" icon="Warning" tint="#ff6b6b" />
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>核查任务</h3>
        <el-button type="primary" :icon="Plus" @click="createVisible = true">新建派单</el-button>
      </div>
      <el-table :data="tasks" size="small">
        <el-table-column prop="no" label="任务编号" width="150" />
        <el-table-column prop="company" label="企业" min-width="170" />
        <el-table-column prop="content" label="核查事项" min-width="200" />
        <el-table-column prop="inspector" label="核查员" width="100" />
        <el-table-column prop="deadline" label="期限" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tag(row.status)" size="small" round>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="H5 打卡" width="180">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="copyLink(row)">复制链接</el-button>
            <el-button link size="small" @click="openCheckin(row)">预览</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="新建核查任务" width="560px">
      <el-form label-width="90px">
        <el-form-item label="异常企业">
          <el-select v-model="newTask.company" style="width: 100%">
            <el-option v-for="c in ['贵州黔能矿业有限责任公司', '遵义铝业股份有限公司', '贵州磷化集团有限责任公司', '中伟新材料股份有限公司']" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="核查事项"><el-input v-model="newTask.content" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="指派核查员">
          <el-select v-model="newTask.inspector" style="width: 100%">
            <el-option v-for="i in ['胡工 · 黔中片区', '周工 · 黔北片区', '刘工 · 黔南片区']" :key="i" :label="i" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="核查点坐标"><el-input v-model="newTask.coord" placeholder="经度,纬度（用于 F9 离场判定）" /></el-form-item>
        <el-form-item label="完成期限">
          <el-date-picker v-model="newTask.deadline" type="date" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="onCreate">派发（短信通知 ✓）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import { publicOrigin } from '@/utils/publicUrl'

const createVisible = ref(false)
const newTask = reactive({ company: '贵州黔能矿业有限责任公司', content: '核实 10 月原煤消耗台账与销售发票一致性', inspector: '胡工 · 黔中片区', coord: '104.83,26.59', deadline: '2026-12-06' })

const tasks = [
  { no: 'QN-JC-2026-118', company: '贵州黔能矿业有限责任公司', content: '核实 10 月原煤消耗台账与发票一致性', inspector: '胡工', status: '执行中', deadline: '2026-12-06' },
  { no: 'QN-JC-2026-117', company: '遵义铝业股份有限公司', content: '购电发票核对 · 生产班次日志', inspector: '周工', status: '已打卡', deadline: '2026-12-02' },
  { no: 'QN-JC-2026-116', company: '贵州磷化集团有限责任公司', content: '第三方物流单据核验', inspector: '刘工', status: '待派发', deadline: '2026-12-10' },
  { no: 'QN-JC-2026-115', company: '贵州西南水泥有限公司', content: '化验单原始记录抽查', inspector: '胡工', status: '已完成', deadline: '2026-11-22' },
  { no: 'QN-JC-2026-114', company: '中伟新材料股份有限公司', content: '新增产线能耗分摊核查', inspector: '周工', status: '执行中', deadline: '2026-12-04' },
  { no: 'QN-JC-2026-113', company: '贵州黔能煤化工有限责任公司', content: '焦炭化验单与入炉台账抽查', inspector: '刘工', status: '已完成', deadline: '2026-11-18' },
  { no: 'QN-JC-2026-112', company: '贵州钢绳股份有限公司', content: '热处理炉天然气消耗核对', inspector: '胡工', status: '已打卡', deadline: '2026-11-28' },
  { no: 'QN-JC-2026-111', company: '贵州乌江航运物流有限公司', content: '船舶燃油加注记录核验', inspector: '刘工', status: '待派发', deadline: '2026-12-12' },
  { no: 'QN-JC-2026-110', company: '铜仁金瑞锰业有限责任公司', content: '电能计量装置现场复核', inspector: '周工', status: '已完成', deadline: '2026-11-10' },
  { no: 'QN-JC-2026-109', company: '贵州振华新材料股份有限公司', content: '烧结炉天然气台账盘点', inspector: '胡工', status: '待派发', deadline: '2026-12-15' }
]
const tag = (s: string) => ({ 待派发: 'info', 执行中: 'warning', 已打卡: 'primary', 已完成: 'success' }[s] || 'info') as any

async function copyLink(row: { no: string }) {
  const base = await publicOrigin()
  const url = `${base}/checkin/${row.no.toLowerCase()}`
  navigator.clipboard?.writeText(url)
  ElMessage.success('打卡链接已复制（手机需与电脑同一 Wi-Fi）：' + url)
}
async function openCheckin(row: { no: string }) {
  const base = await publicOrigin()
  window.open(`${base}/checkin/${row.no.toLowerCase()}`, '_blank')
}
function onCreate() {
  ElMessage.success('任务已创建并短信通知核查员（未配置 SMS Key 时仅站内信）')
  createVisible.value = false
}
</script>

<style scoped lang="scss">
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
@media (max-width: 1100px) { .stat-grid { grid-template-columns: 1fr 1fr; } }
</style>