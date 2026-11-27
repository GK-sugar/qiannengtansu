<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">国际关税预警</div>
      <div class="sub">欧盟 CBAM 碳边境调节机制规则库与影响测算：输入出口产品碳排放即可推演碳关税成本（内置规则推演，无需外部 API）</div>
      <div class="banner-actions">
        <el-button type="primary" :icon="Plus" @click="ruleVisible = true">新增关税规则</el-button>
        <el-tag round effect="plain" type="success">EU ETS 均价 71.50 €/tCO2e · 2026 基准</el-tag>
      </div>
    </div>

    <div class="grid-2">
      <!-- ============ 规则库 ============ -->
      <div class="page-card">
        <div class="page-card-title"><h3>CBAM 税率规则库</h3></div>
        <el-table :data="rules" size="small">
          <el-table-column prop="industry" label="行业" width="90">
            <template #default="{ row }"><el-tag size="small" round type="info">{{ row.industry }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="productScope" label="产品范围" min-width="150" />
          <el-table-column label="税率" width="110">
            <template #default="{ row }"><b class="green">{{ row.tariffRate }}</b><span class="dim"> €/t</span></template>
          </el-table-column>
          <el-table-column label="免费配额" width="130">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.freeAllowance)" :stroke-width="8" :color="row.freeAllowance >= 95 ? '#00b581' : '#ffb020'" />
            </template>
          </el-table-column>
          <el-table-column prop="note" label="口径说明" min-width="140" show-overflow-tooltip />
        </el-table>
        <div class="note-row">
          <el-icon><InfoFilled /></el-icon>
          <span>免费配额逐年退坡，2034 年归零后将对出口成本形成完整冲击。规则由国家级管理员维护，全国省/市/企业可见。</span>
        </div>
      </div>

      <!-- ============ 影响测算器 ============ -->
      <div class="page-card calc-card">
        <div class="page-card-title"><h3>关税影响测算器</h3></div>
        <el-form label-width="130px" size="default">
          <el-form-item label="目标行业">
            <el-select v-model="calc.industry" style="width: 100%" placeholder="选择行业">
              <el-option v-for="r in rules" :key="r.industry" :label="r.industry" :value="String(r.industry)" />
            </el-select>
          </el-form-item>
          <el-form-item label="单位产品碳排放">
            <el-input-number v-model="calc.emissionPerTon" :min="0.01" :max="30" :step="0.1" :precision="2" style="width: 100%" />
            <span class="unit">tCO2e / 吨产品</span>
          </el-form-item>
          <el-form-item label="预计出口量">
            <el-input-number v-model="calc.exportQty" :min="1" :max="100000000" :step="1000" style="width: 100%" />
            <span class="unit">吨</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="calcLoading" style="width: 100%" @click="runImpact">
              <el-icon><Cpu /></el-icon>&nbsp;开始测算
            </el-button>
          </el-form-item>
        </el-form>

        <div v-if="result" class="result-panel">
          <div class="r-head">
            <span>测算结果</span>
            <el-tag :type="tagType(result.risk)" size="small" round effect="dark">{{ riskText(result.risk) }}</el-tag>
          </div>
          <div class="r-grid">
            <div class="r-item">
              <div class="r-label">应税排放</div>
              <div class="r-value">{{ result.taxableEmission }}<small> tCO2e</small></div>
            </div>
            <div class="r-item">
              <div class="r-label">碳关税（EUR）</div>
              <div class="r-value eur">€ {{ result.amountEur }}</div>
            </div>
            <div class="r-item">
              <div class="r-label">折合人民币</div>
              <div class="r-value cny">¥ {{ result.amountCny }}</div>
            </div>
            <div class="r-item">
              <div class="r-label">每吨出口成本</div>
              <div class="r-value">{{ result.perTonCny }}<small> 元/吨</small></div>
            </div>
          </div>
          <div class="r-break">
            <span>计算口径</span>
            <b>{{ result.totalEmission }} t × (1 − {{ result.freeAllowance }}%) × {{ result.tariffRate }} €/t</b>
          </div>
          <div class="r-suggest">
            <el-icon><ChatDotRound /></el-icon>
            <span>{{ result.suggest }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增规则 -->
    <el-dialog v-model="ruleVisible" title="新增 / 更新 CBAM 关税规则" width="560px">
      <el-form label-width="110px">
        <el-form-item label="行业"><el-input v-model="newRule.industry" placeholder="如：钢铁 / 铝 / 水泥" /></el-form-item>
        <el-form-item label="产品范围"><el-input v-model="newRule.productScope" placeholder="如：粗钢、钢材轧制产品（CN 72）" /></el-form-item>
        <el-form-item label="税率 EUR/t"><el-input-number v-model="newRule.tariffRate" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="免费配额 %"><el-input-number v-model="newRule.freeAllowance" :min="0" :max="100" :precision="1" style="width: 100%" /></el-form-item>
        <el-form-item label="口径说明"><el-input v-model="newRule.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存规则</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface Rule { industry: string; productScope: string; tariffRate: number; freeAllowance: number; note: string }

const rules = ref<Rule[]>([
  { industry: '钢铁', productScope: '粗钢、钢材轧制产品（CN 72）', tariffRate: 71.5, freeAllowance: 97.5, note: '2034 年免费配额逐年退坡至 0' },
  { industry: '铝', productScope: '原铝及铝制品（CN 76）', tariffRate: 71.5, freeAllowance: 97.5, note: '直接排放纳入，间接排放暂缓' },
  { industry: '水泥', productScope: '水泥熟料及水泥（CN 2523）', tariffRate: 71.5, freeAllowance: 97.5, note: '2026 年全面执行' },
  { industry: '化肥', productScope: '氮肥及部分磷肥（CN 31）', tariffRate: 71.2, freeAllowance: 96, note: '单位排放高，出口体量较小' },
  { industry: '电力', productScope: '并网直购电力（间接排放）', tariffRate: 71.5, freeAllowance: 0, note: 'PPA 购电协议可抵扣' },
  { industry: '氢', productScope: '灰氢与部分蓝氢（CN 2804）', tariffRate: 70.9, freeAllowance: 95, note: '绿氢占比逐年提升' },
  { industry: '钢制品', productScope: '螺钉螺栓及钢结构件（CN 73）', tariffRate: 71.5, freeAllowance: 97.5, note: '下游制品扩围清单，按前体粗钢隐含排放计' },
  { industry: '铝材', productScope: '铝型材、板带箔（CN 7604-7607）', tariffRate: 71.5, freeAllowance: 97.5, note: '加工材按原铝隐含排放折算' },
  { industry: '焦炭', productScope: '焦炭及半焦（CN 2704）', tariffRate: 70.8, freeAllowance: 96, note: '扩围候选行业，提前纳入观测' }
])

const calc = reactive({ industry: '钢铁', emissionPerTon: 1.8, exportQty: 10000 })
const calcLoading = ref(false)
const result = ref<any>(null)
const ruleVisible = ref(false)
const newRule = reactive({ industry: '', productScope: '', tariffRate: 71.5, freeAllowance: 97.5, note: '' })

function tagType(risk: string) { return risk === 'HIGH' ? 'danger' : risk === 'MEDIUM' ? 'warning' : 'success' }
function riskText(risk: string) { return risk === 'HIGH' ? '高风险' : risk === 'MEDIUM' ? '中风险' : '低风险' }

async function runImpact() {
  calcLoading.value = true
  let res: any = null
  try {
    res = (await request.post('/v1/national/tariff/impact', {
      industry: calc.industry, emissionPerTon: String(calc.emissionPerTon), exportQty: String(calc.exportQty)
    })) as any
  } catch { /* 后端不可用 → 本地推算 */ }
  if (res && res.code === 0 && res.data) { result.value = res.data }
  else {
    const rule = rules.value.find((r) => r.industry === calc.industry) || rules.value[0]
    const total = +(calc.emissionPerTon * calc.exportQty).toFixed(2)
    const taxable = +(total * (1 - rule.freeAllowance / 100)).toFixed(2)
    const eur = +(taxable * rule.tariffRate).toFixed(2)
    const cny = +(eur * 7.85).toFixed(2)
    const perTon = +(cny / calc.exportQty).toFixed(2)
    const risk = perTon >= 120 ? 'HIGH' : perTon >= 40 ? 'MEDIUM' : 'LOW'
    result.value = {
      industry: calc.industry, emissionPerTon: calc.emissionPerTon, exportQty: calc.exportQty,
      tariffRate: rule.tariffRate, freeAllowance: rule.freeAllowance, totalEmission: total, taxableEmission: taxable,
      amountEur: eur, amountCny: cny, perTonCny: perTon, risk,
      suggest: risk === 'HIGH' ? '碳成本已侵蚀出口毛利，建议优先采购绿电、切换低碳工艺并申请零碳追溯标识'
        : risk === 'MEDIUM' ? '建议建立一级供应商核算协同，按季度跟踪欧盟碳价与免费配额退坡' : '风险可控，持续维护 CBAM 报告即可'
    }
  }
  calcLoading.value = false
}

async function saveRule() {
  if (!newRule.industry) { ElMessage.warning('请填写行业名称'); return }
  const r: Rule = { industry: newRule.industry, productScope: newRule.productScope, tariffRate: newRule.tariffRate, freeAllowance: newRule.freeAllowance, note: newRule.note }
  const old = rules.value.findIndex((x) => x.industry === r.industry)
  if (old >= 0) rules.value[old] = r; else rules.value.push(r)
  try {
    await request.post('/v1/national/tariff/rules', { ...r, effectiveFrom: '2026-11-26' })
  } catch { /* 本地演示 */ }
  ruleVisible.value = false
  ElMessage.success('规则已保存：' + r.industry)
}
</script>

<style scoped lang="scss">
.banner-actions { margin-top: 16px; display: flex; align-items: center; gap: 12px; }
.grid-2 { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.calc-card { padding: 22px 24px; }
.unit { margin-left: 10px; font-size: 12px; color: var(--qn-text-3); white-space: nowrap; }
.green { color: var(--qn-primary); }
.dim { color: var(--qn-text-3); font-size: 12px; }
.note-row { margin-top: 14px; display: flex; gap: 8px; align-items: flex-start; font-size: 12.5px; color: var(--qn-text-3); line-height: 1.6; }
.result-panel { margin-top: 6px; border-radius: 14px; overflow: hidden; border: 1px solid rgba(0, 209, 255, 0.25); background: linear-gradient(160deg, rgba(0, 209, 255, 0.06), rgba(0, 181, 129, 0.05));
  .r-head { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; font-weight: 600; }
  .r-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 0 16px 14px; }
  .r-item { padding: 12px; border-radius: 10px; background: rgba(255, 255, 255, 0.7); border: 1px solid var(--qn-border); }
  .r-label { font-size: 11.5px; color: var(--qn-text-3); margin-bottom: 5px; }
  .r-value { font-size: 20px; font-weight: 700; color: var(--qn-text); font-variant-numeric: tabular-nums;
    small { font-size: 11px; font-weight: 400; color: var(--qn-text-3); }
    &.eur { color: #00b581; } &.cny { color: #ff5f56; } }
  .r-break { margin: 0 16px 12px; padding: 10px 12px; border-radius: 8px; font-size: 12px; background: rgba(255, 255, 255, 0.8); color: var(--qn-text-2);
    b { font-family: Consolas, monospace; color: var(--qn-text); margin-left: 6px; } }
  .r-suggest { display: flex; gap: 8px; align-items: flex-start; padding: 12px 16px; font-size: 12.5px; color: #0b6e50; background: rgba(0, 181, 129, 0.1); border-top: 1px dashed rgba(0, 181, 129, 0.35); line-height: 1.6; } }
</style>