<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">智能初审台</div>
      <div class="sub">对企业提交的核算与历史数据、行业基准作 F6 波动比对，异常自动标红并生成预警原因（LLM 辅助说明）</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>待初审清单（38 家）</h3>
        <div class="toolbar">
          <el-radio-group v-model="filter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="warn">异常</el-radio-button>
            <el-radio-button value="ok">正常</el-radio-button>
          </el-radio-group>
          <el-input v-model="kw" placeholder="搜索企业" clearable style="width: 180px" size="small" :prefix-icon="Search" />
        </div>
      </div>

      <el-table :data="filtered" size="small" highlight-current-row>
        <el-table-column prop="company" label="企业" min-width="190" />
        <el-table-column prop="industry" label="行业" width="130" />
        <el-table-column prop="period" label="核算期" width="90" />
        <el-table-column prop="total" label="本期排放(t)" width="115" sortable />
        <el-table-column label="F6 波动率" width="150">
          <template #default="{ row }">
            <span class="wave" :class="{ bad: row.rate > row.threshold }">{{ (row.rate * 100).toFixed(1) }}%</span>
            <el-progress :percentage="Math.min(row.rate * 100, 100)" :show-text="false" :stroke-width="5"
              :color="row.rate > row.threshold ? '#f56c6c' : '#00d1ff'" style="width: 60px; display: inline-block; margin-left: 8px" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.dispatched" type="primary" size="small" round style="margin-right: 6px">已派单</el-tag>
            <el-tag :type="row.rate > row.threshold ? 'danger' : 'success'" size="small" round>{{ row.rate > row.threshold ? '异常' : '正常' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">审查</el-button>
            <el-button v-if="row.rate <= row.threshold" link type="success" size="small" @click="pass(row)">通过</el-button>
            <el-button v-else link type="warning" size="small" :disabled="row.dispatched" @click="dispatchTask(row)">{{ row.dispatched ? '已派单' : '派单核查' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 审查抽屉 -->
    <el-drawer v-model="detailVisible" size="560px" title="智能初审详情">
      <template v-if="current">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="企业">{{ current.company }}</el-descriptions-item>
          <el-descriptions-item label="核算期">{{ current.period }}</el-descriptions-item>
          <el-descriptions-item label="本期排放">{{ current.total }} tCO₂e</el-descriptions-item>
          <el-descriptions-item label="历史基准">{{ current.base }} tCO₂e</el-descriptions-item>
          <el-descriptions-item label="行业基准">{{ current.indBase }} tCO₂e</el-descriptions-item>
          <el-descriptions-item label="阈值">±20%</el-descriptions-item>
        </el-descriptions>
        <div class="formula-box">
          <div class="fb-title">F6 异常波动判定</div>
          <div class="fb-formula">r = |X本期 − X基准| ÷ max(X基准, ε) = {{ (current.rate * 100).toFixed(1) }}%</div>
          <div class="fb-note">r &gt; 20% 判定异常；超过 3σ 升级为高等级预警</div>
        </div>
        <el-alert v-if="current.rate > current.threshold" type="error" show-icon :closable="false" style="margin-bottom:14px"
          :title="'预警原因（LLM 生成）：' + current.reason" />
        <el-alert v-else type="success" show-icon :closable="false" style="margin-bottom:14px" title="数据波动在正常区间，建议通过初审。" />
        <div class="drawer-actions">
          <el-button type="success" @click="pass(current)">初审通过</el-button>
          <el-button type="warning" plain :disabled="current.dispatched" @click="dispatchTask(current)">{{ current.dispatched ? '已派单' : '派发线下核查' }}</el-button>
          <el-button type="danger" plain @click="back(current)">退回修改</el-button>
        </div>
        <div class="ai-note"><el-icon><MagicStick /></el-icon> 预警说明由大模型 qwen-plus 依据企业上下文自动生成；未配置 DASHSCOPE_API_KEY 时使用规则模板。</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

interface Row { company: string; industry: string; period: string; total: number; base: number; indBase: number; rate: number; threshold: number; reason: string; dispatched?: boolean }

const filter = ref('all')
const kw = ref('')
const detailVisible = ref(false)
const current = ref<Row | null>(null)

const list: Row[] = [
  { company: '贵州黔能矿业有限责任公司', industry: '煤炭开采', period: '2026-10', total: 13840, base: 9804, indBase: 11200, rate: 0.412, threshold: 0.2, reason: '原煤消耗环比 +38%，超过 3σ 与 ±20% 阈值，疑似井下作业时序调整或数据填报口径变化，请核实 10 月入井量与销售台账。' },
  { company: '贵州振华新材料股份有限公司', industry: '锂电材料', period: '2026-10', total: 7620, base: 7280, indBase: 8000, rate: 0.047, threshold: 0.2, reason: '' },
  { company: '遵义铝业股份有限公司', industry: '有色金属', period: '2026-09', total: 9580, base: 7600, indBase: 8400, rate: 0.261, threshold: 0.2, reason: '范围二外购电环比 +15%，高于行业均值 9 个百分点，请核对购电发票与生产班次。' },
  { company: '贵州黔能煤化工有限责任公司', industry: '焦化', period: '2026-10', total: 11260, base: 10500, indBase: 9800, rate: 0.072, threshold: 0.2, reason: '' },
  { company: '贵州西南水泥有限公司', industry: '建材', period: '2026-10', total: 6480, base: 6120, indBase: 5900, rate: 0.059, threshold: 0.2, reason: '' },
  { company: '贵州磷化集团有限责任公司', industry: '化工', period: '2026-10', total: 5230, base: 4210, indBase: 4600, rate: 0.242, threshold: 0.2, reason: '运输周转量连续两期跳增，建议核验第三方物流单据并做轨迹比对。' },
  { company: '中伟新材料股份有限公司', industry: '锂电材料', period: '2026-09', total: 15420, base: 12300, indBase: 13100, rate: 0.254, threshold: 0.2, reason: '三元前驱体产线扩产致外购电与蒸汽同步上行，请补充新增产线能耗分摊说明与购能凭证。' },
  { company: '贵州钢绳股份有限公司', industry: '金属制品', period: '2026-10', total: 3120, base: 2980, indBase: 3050, rate: 0.047, threshold: 0.2, reason: '' },
  { company: '贵州乌江航运物流有限公司', industry: '交通运输', period: '2026-10', total: 1860, base: 1720, indBase: 1800, rate: 0.081, threshold: 0.2, reason: '' },
  { company: '铜仁金瑞锰业有限责任公司', industry: '锰系材料', period: '2026-10', total: 4380, base: 4160, indBase: 4300, rate: 0.053, threshold: 0.2, reason: '' }
]

const filtered = computed(() => list.filter((r) => {
  const hitF = filter.value === 'all' || (filter.value === 'warn' ? r.rate > r.threshold : r.rate <= r.threshold)
  return hitF && (!kw.value || r.company.includes(kw.value))
}))

function openDetail(row: Row) { current.value = row; detailVisible.value = true }
function pass(row: Row) { ElMessage.success(`${row.company} 初审通过，意见书进入电子签章队列（哈希链存证 +1）`) }
function back(row: Row) { ElMessage.warning(`${row.company} 已退回企业修改，附退回原因`) }
function dispatchTask(row: Row) {
  row.dispatched = true
  ElMessage.success('已生成核查任务并派发核查员：' + row.company + '（任务号 HC-2026-' + String(100 + Math.floor(Math.random() * 900)) + '，核查机构：中环低碳国际核查中心）')
}
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 10px; align-items: center; }
.wave { font-weight: 600; color: #00a97e; &.bad { color: #f56c6c; } }
.formula-box { background: #f5f8fc; border-radius: 10px; padding: 14px 16px; margin: 14px 0; border: 1px dashed #c9d9e8;
  .fb-title { font-size: 12px; color: var(--qn-text-2); margin-bottom: 6px; }
  .fb-formula { font-family: 'Cambria Math', monospace; font-size: 13px; color: var(--qn-text); }
  .fb-note { font-size: 11.5px; color: var(--qn-text-3); margin-top: 6px; } }
.drawer-actions { display: flex; gap: 10px; margin-bottom: 14px; }
.ai-note { font-size: 11.5px; color: var(--qn-text-3); display: flex; gap: 6px; align-items: center; }
</style>