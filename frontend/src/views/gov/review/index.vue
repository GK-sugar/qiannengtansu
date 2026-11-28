<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">智能初审台</div>
      <div class="sub">对企业提交的核算与历史数据、行业基准作 F6 波动比对，异常自动标红并生成预警原因（LLM 辅助说明）</div>
    </div>

    <div class="page-card">
      <div class="page-card-title">
        <h3>待初审清单（{{ filtered.length }} 家）</h3>
        <div class="toolbar">
          <el-select v-model="cityFilter" size="small" placeholder="全部市州" style="width: 138px" @change="onCityChange">
            <el-option label="全部市州" value="" />
            <el-option v-for="c in cityOptions" :key="c.name" :label="`${c.short}（${c.count}）`" :value="c.name" />
          </el-select>
          <el-radio-group v-model="filter" size="small">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="warn">异常</el-radio-button>
            <el-radio-button value="ok">正常</el-radio-button>
          </el-radio-group>
          <el-input v-model="kw" placeholder="搜索企业" clearable style="width: 160px" size="small" :prefix-icon="Search" />
          <el-button v-if="cityFilter" link type="primary" size="small" @click="backToCity">打开{{ cityShort }}专页</el-button>
        </div>
      </div>

      <el-table :data="filtered" size="small" highlight-current-row>
        <el-table-column prop="company" label="企业" min-width="190" />
        <el-table-column label="所属市州" width="120">
          <template #default="{ row }">{{ cityStatMap[row.city]?.short ?? row.city }}</template>
        </el-table-column>
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

    <!-- 审查详情 -->
    <el-dialog v-model="detailVisible" title="智能初审详情" width="min(720px, 94vw)" align-center append-to-body class="review-detail-dialog">
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
          <div class="fb-title" :title="formulaHint('F6')">F6 异常波动判定 <span class="f-q">?</span></div>
          <div class="fb-formula">r = |X本期 − X基准| ÷ max(X基准, ε) = {{ (current.rate * 100).toFixed(1) }}%</div>
          <div class="fb-note">r &gt; 20% 判定异常；超过 3σ 升级为高等级预警</div>
        </div>
        <el-alert v-if="current.rate > current.threshold" type="error" show-icon :closable="false" style="margin-bottom:14px"
          :title="'预警原因（LLM 生成）：' + current.reason" />
        <el-alert v-else type="success" show-icon :closable="false" style="margin-bottom:14px" title="数据波动在正常区间，建议通过初审。" />
        <div class="ai-note"><el-icon><MagicStick /></el-icon> 预警说明由大模型 qwen-plus 依据企业上下文自动生成；未配置 DASHSCOPE_API_KEY 时使用规则模板。</div>
      </template>
      <template #footer>
        <div v-if="current" class="detail-actions">
          <el-button type="success" @click="pass(current)">初审通过</el-button>
          <el-button type="warning" plain :disabled="current.dispatched" @click="dispatchTask(current)">{{ current.dispatched ? '已派单' : '派发线下核查' }}</el-button>
          <el-button type="danger" plain @click="back(current)">退回修改</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { formulaHint } from '@/utils/formulas'
import { cityStatMap, CITY_STATS } from '@/config/regions'

interface Row { company: string; city: string; industry: string; period: string; total: number; base: number; indBase: number; rate: number; threshold: number; reason: string; dispatched?: boolean }

const route = useRoute()
const router = useRouter()
const filter = ref(route.query.status === 'warn' ? 'warn' : 'all')
const kw = ref('')
const cityFilter = ref(String(route.query.city ?? ''))
const detailVisible = ref(false)
const current = ref<Row | null>(null)

const cityShort = computed(() => cityStatMap[cityFilter.value]?.short ?? cityFilter.value)
watch(() => route.query.city, (v) => {
  cityFilter.value = String(v ?? '')
  if (route.query.status === 'warn') filter.value = 'warn'
})

const list: Row[] = [
  { company: '贵州黔能矿业有限责任公司', city: '六盘水市', industry: '煤炭开采', period: '2026-10', total: 13840, base: 9804, indBase: 11200, rate: 0.412, threshold: 0.2, reason: '原煤消耗环比 +38%，超过 3σ 与 ±20% 阈值，疑似井下作业时序调整或数据填报口径变化，请核实 10 月入井量与销售台账。' },
  { company: '贵州盘江精煤股份有限公司', city: '六盘水市', industry: '煤炭开采', period: '2026-10', total: 9860, base: 7120, indBase: 8400, rate: 0.385, threshold: 0.2, reason: '原煤入洗量与销售台账差异 9.8%，范围一排放同步抬升，请核实洗选工艺与瓦斯抽采记录。' },
  { company: '贵州黔桂发电有限责任公司', city: '六盘水市', industry: '电力热力', period: '2026-10', total: 11200, base: 8900, indBase: 9600, rate: 0.259, threshold: 0.2, reason: '机组利用小时数回升导致燃煤消耗上行，供电煤耗高于同类机组 6%，请核对入炉煤热值与发电量。' },
  { company: '贵州黔能煤化工有限责任公司', city: '六盘水市', industry: '焦化', period: '2026-10', total: 11260, base: 10500, indBase: 9800, rate: 0.072, threshold: 0.2, reason: '' },
  { company: '贵州振华新材料股份有限公司', city: '贵阳市', industry: '锂电材料', period: '2026-10', total: 7620, base: 7280, indBase: 8000, rate: 0.047, threshold: 0.2, reason: '' },
  { company: '中伟新材料股份有限公司', city: '贵阳市', industry: '锂电材料', period: '2026-09', total: 15420, base: 12300, indBase: 13100, rate: 0.254, threshold: 0.2, reason: '三元前驱体产线扩产致外购电与蒸汽同步上行，请补充新增产线能耗分摊说明与购能凭证。' },
  { company: '贵州乌江航运物流有限公司', city: '贵阳市', industry: '交通运输', period: '2026-10', total: 1860, base: 1720, indBase: 1800, rate: 0.081, threshold: 0.2, reason: '' },
  { company: '遵义铝业股份有限公司', city: '遵义市', industry: '有色金属', period: '2026-09', total: 9580, base: 7600, indBase: 8400, rate: 0.261, threshold: 0.2, reason: '范围二外购电环比 +15%，高于行业均值 9 个百分点，请核对购电发票与生产班次。' },
  { company: '贵州钢绳股份有限公司', city: '遵义市', industry: '金属制品', period: '2026-10', total: 3120, base: 2980, indBase: 3050, rate: 0.047, threshold: 0.2, reason: '' },
  { company: '贵州西南水泥有限公司', city: '安顺市', industry: '建材', period: '2026-10', total: 6480, base: 6120, indBase: 5900, rate: 0.059, threshold: 0.2, reason: '' },
  { company: '安顺永城建材有限公司', city: '安顺市', industry: '新型建材', period: '2026-10', total: 3260, base: 2648, indBase: 2800, rate: 0.231, threshold: 0.2, reason: '熟料产量环比 +18%，超出行业基准波动区间，请核对窑炉运行记录与产量台账。' },
  { company: '贵州黔西能源开发有限公司', city: '毕节市', industry: '煤炭开采', period: '2026-10', total: 6240, base: 4670, indBase: 5100, rate: 0.336, threshold: 0.2, reason: '新建采区投产，原煤产量环比 +42%，请补充产能核增批复与产量台账。' },
  { company: '贵州毕节华电发电有限公司', city: '毕节市', industry: '电力热力', period: '2026-10', total: 9180, base: 7180, indBase: 8200, rate: 0.278, threshold: 0.2, reason: '发电量下滑但排放上升，疑似存在漏报或计量异常，请核对负荷曲线与燃料入库单。' },
  { company: '铜仁金瑞锰业有限责任公司', city: '铜仁市', industry: '锰系材料', period: '2026-10', total: 4380, base: 4160, indBase: 4300, rate: 0.053, threshold: 0.2, reason: '' },
  { company: '贵州大龙汇成新材料有限公司', city: '铜仁市', industry: '新能源电池', period: '2026-10', total: 4520, base: 3630, indBase: 3900, rate: 0.245, threshold: 0.2, reason: '锰系产线扩产，外购电与蒸汽同步上行，请补充新增产线能耗分摊说明。' },
  { company: '贵州磷化集团有限责任公司', city: '黔南布依族苗族自治州', industry: '化工', period: '2026-10', total: 5230, base: 4210, indBase: 4600, rate: 0.242, threshold: 0.2, reason: '运输周转量连续两期跳增，建议核验第三方物流单据并做轨迹比对。' },
  { company: '贵州兴义电力发展有限公司', city: '黔西南布依族苗族自治州', industry: '电力', period: '2026-10', total: 7480, base: 6115, indBase: 6800, rate: 0.223, threshold: 0.2, reason: '火电机组调峰频次增加，单位供电排放强度上升 8%，请核对负荷曲线。' },
  { company: '贵州青酒厂股份有限公司', city: '黔东南苗族侗族自治州', industry: '白酒酿造', period: '2026-10', total: 1980, base: 1625, indBase: 1750, rate: 0.218, threshold: 0.2, reason: '燃气锅炉燃料消耗与产量匹配度下降，请补充锅炉运行台账。' }
]

function statusHit(r: Row) {
  return filter.value === 'all' || (filter.value === 'warn' ? r.rate > r.threshold : r.rate <= r.threshold)
}
const filtered = computed(() => list.filter((r) =>
  statusHit(r) && (!cityFilter.value || r.city === cityFilter.value) && (!kw.value || r.company.includes(kw.value))
))
const cityOptions = computed(() => CITY_STATS.map((c) => ({
  name: c.name,
  short: c.short,
  count: list.filter((r) => r.city === c.name && statusHit(r) && (!kw.value || r.company.includes(kw.value))).length
})))

function onCityChange(v: string) {
  router.replace({ path: '/gov/review', query: v ? { city: v } : {} })
}
function backToCity() {
  router.push({ name: 'GovCityDetail', params: { name: cityFilter.value } })
}

function openDetail(row: Row) { current.value = row; detailVisible.value = true }
function pass(row: Row) {
  ElMessage.success(`${row.company} 初审通过，意见书进入回传队列（哈希链存证 +1）`)
  detailVisible.value = false
}
function back(row: Row) {
  ElMessage.warning(`${row.company} 已退回企业修改，附退回原因`)
  detailVisible.value = false
}
const inspectors = ['胡工 · 黔中片区', '周工 · 黔北片区', '刘工 · 黔南片区']
let dispatchSeq = 0
function dispatchTask(row: Row) {
  row.dispatched = true
  const inspector = inspectors[dispatchSeq % inspectors.length]
  const taskNo = 'QN-JC-2026-' + String(119 + dispatchSeq)
  dispatchSeq++
  ElMessage.success(`已派发核查员 ${inspector}：${row.company}（任务 ${taskNo}，H5 打卡链接已短信推送）`)
  detailVisible.value = false
}
</script>

<style scoped lang="scss">
.toolbar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.wave { font-weight: 600; color: #00a97e; &.bad { color: #f56c6c; } }
.formula-box { background: #f5f8fc; border-radius: 10px; padding: 14px 16px; margin: 14px 0; border: 1px dashed #c9d9e8;
  .fb-title { font-size: 12px; color: var(--qn-text-2); margin-bottom: 6px; cursor: help; }
  .f-q { display: inline-flex; align-items: center; justify-content: center; width: 14px; height: 14px; border-radius: 50%;
    border: 1px solid #cbd5e1; color: #64748b; font-size: 10px; margin-left: 4px; }
  .fb-formula { font-family: 'Cambria Math', monospace; font-size: 13px; color: var(--qn-text); }
  .fb-note { font-size: 11.5px; color: var(--qn-text-3); margin-top: 6px; } }
.detail-actions { display: flex; gap: 10px; justify-content: flex-end; flex-wrap: wrap;
  :deep(.el-button--success) { background: #008560; border-color: #008560; color: #fff;
    &:hover, &:focus { background: #00795c; border-color: #00795c; color: #fff; } }
  :deep(.el-button--warning.is-plain) { background: #fff8ec; border-color: #e2a94a; color: #9a5b00;
    &:hover, &:focus { background: #fff1d9; border-color: #d99a33; color: #7c4900; } }
  :deep(.el-button--danger.is-plain) { background: #fff5f4; border-color: #eeb2aa; color: #a9271b;
    &:hover, &:focus { background: #fdeceb; border-color: #e59a90; color: #8d1f15; } } }
.ai-note { font-size: 11.5px; color: #64748b; display: flex; gap: 6px; align-items: center; }
/* 表格操作链接加深，保证对比度 */
:deep(.el-button.is-link.el-button--primary) { color: #00659a; }
:deep(.el-button.is-link.el-button--warning) { color: #9a5b00; }
:deep(.el-button.is-link.el-button--success) { color: #00795c; }
:deep(.el-tag--primary) { background: #e6f7ff; color: #00659a; border-color: #b3e3f7; }
:deep(.el-tag--danger) { background: #fdecec; color: #a9271b; border-color: #f6c6c0; }
:deep(.el-tag--success) { background: #e6f9f2; color: #00795c; border-color: #b3ebd9; }
</style>