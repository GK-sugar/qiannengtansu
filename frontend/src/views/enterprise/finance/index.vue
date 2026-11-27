<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">绿色金融</div>
      <div class="sub">基于历史核算与申报记录计算碳数据信誉分（F7），联动预授信额度（F8），输出金融机构可读的申请单</div>
    </div>

    <div class="fin-grid">
      <!-- 信誉分 -->
      <div class="page-card score-card">
        <div class="score-ring" :style="{ '--p': 87 }">
          <div class="ring-num">{{ score.value }}</div>
          <div class="ring-label">综合信誉分</div>
        </div>
        <div class="score-meta">
          <div class="level">评级 <b class="A">A</b> · 稳健</div>
          <div class="level-sub">全省同行业前 18%</div>
          <el-button type="primary" round @click="applyVisible = true">申请授信</el-button>
        </div>
        <div class="detail-row">
          <div class="dr-item"><span class="dr-name">数据完整度 A（40%）</span><el-progress :percentage="96" :stroke-width="8" color="#00b581" /></div>
          <div class="dr-item"><span class="dr-name">及时提交率 B（25%）</span><el-progress :percentage="90" :stroke-width="8" color="#00d1ff" /></div>
          <div class="dr-item"><span class="dr-name">核算稳定性 C（20%）</span><el-progress :percentage="78" :stroke-width="8" color="#a06bff" /></div>
          <div class="dr-item"><span class="dr-name">历史合规 D（15%）</span><el-progress :percentage="84" :stroke-width="8" color="#ffb020" /></div>
        </div>
      </div>

      <!-- 雷达图 -->
      <div class="page-card">
        <div class="page-card-title"><h3>企业碳能力雷达</h3><el-tag size="small" round>F7 指标</el-tag></div>
        <div ref="radarRef" class="chart-box"></div>
        <div class="formula-note">S = 40×A + 25×B + 20×C + 15×D，得分与同业均值对比</div>
      </div>

      <!-- 预授信 -->
      <div class="page-card credit-card">
        <div class="page-card-title"><h3>预授信额度（F8）</h3></div>
        <div class="credit-num">￥<b>1,200</b> 万</div>
        <div class="credit-sub">Q = Q基准(1,500万) × k等级(0.85)，A 级系数 0.85</div>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="基准额度">1,500 万元（煤炭采选 · 中型）</el-descriptions-item>
          <el-descriptions-item label="等级系数 k">0.85（A 级）</el-descriptions-item>
          <el-descriptions-item label="有效期">测算日起 90 天</el-descriptions-item>
          <el-descriptions-item label="合作机构">贵州银行绿色金融部（示例）</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <!-- 授信申请单 -->
    <el-dialog v-model="applyVisible" title="绿色金融授信申请单" width="620px">
      <div class="app-preview">
        <div class="ap-head"><img src="@/assets/logo/logo.svg" style="width:30px" /><b>碳数据信誉与预授信申请单</b></div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="申请企业">贵州黔能矿业有限责任公司</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码">91520000MA6XXXXXXX</el-descriptions-item>
          <el-descriptions-item label="信誉分 S">87（A 级）</el-descriptions-item>
          <el-descriptions-item label="预授信额度">1,200 万元</el-descriptions-item>
          <el-descriptions-item label="核算期覆盖">2026-01 ~ 2026-10</el-descriptions-item>
          <el-descriptions-item label="存证哈希"><span class="hash-line">{{ hash }}</span></el-descriptions-item>
        </el-descriptions>
        <div class="ap-note">本单由黔能碳溯平台依据 F7/F8 公式计算，仅供金融机构参考授信，不构成信用承诺。</div>
      </div>
      <template #footer>
        <el-button @click="applyVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitApply">提交给金融机构</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'

const applyVisible = ref(false)
const score = ref({ value: 87, label: 'A' })
const radarRef = ref<HTMLElement>()
let radar: echarts.ECharts | null = null
const hash = '0x' + Array.from({ length: 40 }, () => '0123456789abcdef'[Math.floor(Math.random() * 16)]).join('')

function renderRadar() {
  if (!radarRef.value) return
  radar = echarts.init(radarRef.value)
  radar.setOption({
    tooltip: {},
    legend: { bottom: 0, textStyle: { color: '#5c6b82' } },
    radar: {
      indicator: [
        { name: '数据完整度', max: 100 },
        { name: '及时提交率', max: 100 },
        { name: '核算稳定性', max: 100 },
        { name: '历史合规', max: 100 },
        { name: '流程数字化', max: 100 },
        { name: '协同参与度', max: 100 }
      ],
      radius: '62%',
      axisName: { color: '#5c6b82' },
      splitArea: { areaStyle: { color: ['#f7fafc', '#eef4f9'] } },
      splitLine: { lineStyle: { color: '#dbe6f0' } },
      axisLine: { lineStyle: { color: '#dbe6f0' } }
    },
    series: [{
      type: 'radar',
      data: [
        { value: [96, 90, 78, 84, 92, 70], name: '本企业', areaStyle: { color: 'rgba(0,181,129,0.28)' }, itemStyle: { color: '#00b581' }, lineStyle: { color: '#00b581' } },
        { value: [88, 85, 82, 86, 80, 72], name: '同业均值', areaStyle: { color: 'rgba(0,209,255,0.12)' }, itemStyle: { color: '#00d1ff' }, lineStyle: { color: '#00d1ff' } }
      ]
    }]
  })
}
const resize = () => radar?.resize()
onMounted(() => { renderRadar(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); radar?.dispose() })

function submitApply() {
  ElMessage.success('申请单已加密提交至合作金融机构，并写入哈希链存证')
  applyVisible.value = false
}
</script>

<style scoped lang="scss">
.fin-grid { display: grid; grid-template-columns: 5fr 5fr 4fr; gap: 16px; align-items: stretch; }
.score-card { display: grid; grid-template-columns: 130px 1fr; gap: 16px; align-items: center; }
.score-ring {
  width: 130px; height: 130px; border-radius: 50%;
  background: conic-gradient(#00b581 calc(var(--p) * 1%), #e8eef5 0);
  display: flex; flex-direction: column; align-items: center; justify-content: center; position: relative;
  &::before { content: ''; position: absolute; inset: 10px; border-radius: 50%; background: #fff; }
  .ring-num { position: relative; font-size: 30px; font-weight: 800; color: var(--qn-text); }
  .ring-label { position: relative; font-size: 11px; color: var(--qn-text-3); }
}
.score-meta { .level { font-size: 15px; color: var(--qn-text-2); margin-bottom: 4px; b { font-size: 22px; color: var(--qn-primary); margin: 0 4px; } }
  .level-sub { font-size: 12px; color: var(--qn-text-3); margin-bottom: 14px; } }
.detail-row { grid-column: 1 / -1; display: grid; grid-template-columns: 1fr 1fr; gap: 6px 20px;
  .dr-name { display: block; font-size: 12px; color: var(--qn-text-2); margin-bottom: 4px; } }
.chart-box { height: 300px; }
.formula-note { text-align: center; font-size: 11.5px; color: var(--qn-text-3); background: #f5f8fb; border-radius: 8px; padding: 8px; margin-top: 6px; }
.credit-card .credit-num { font-size: 14px; color: var(--qn-text-2); margin: 8px 0 4px; b { font-size: 34px; color: var(--qn-primary); margin: 0 4px; font-variant-numeric: tabular-nums; } }
.credit-sub { font-size: 11.5px; color: var(--qn-text-3); margin-bottom: 14px; }
.hash-line { font-family: monospace; font-size: 11px; word-break: break-all; }
.ap-head { display: flex; align-items: center; gap: 10px; font-size: 15px; color: var(--qn-text); margin-bottom: 14px; }
.ap-note { margin-top: 12px; font-size: 11.5px; color: var(--qn-text-3); }
@media (max-width: 1200px) { .fin-grid { grid-template-columns: 1fr; } }
</style>