<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">监管工作台</div>
      <div class="sub">{{ auth.user?.orgName }} · 应对气候变化处 ｜ 全省 2,384 家企业纳入监测</div>
      <div class="banner-btn">
        <el-button type="primary" round @click="$router.push('/gov/big-screen')"><el-icon><DataBoard /></el-icon>进入态势大屏</el-button>
        <el-button round plain style="color:rgba(0, 0, 0);border-color:rgba(255,255,255,.5)" @click="$router.push('/gov/review')">开始初审</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <StatCard label="待初审核算" :value="38" icon="Warning" suffix=" 家" tint="#ffb020" extra="其中高预警 3 家" />
      <StatCard label="本月核准通过" :value="212" icon="CircleCheck" suffix=" 家" />
      <StatCard label="在途核查任务" :value="17" icon="Position" tint="#00d1ff" extra="6 名核查员执行中" />
      <StatCard label="数据回填率" :value="93.6" suffix="%" icon="DataLine" tint="#a06bff" trend="+1.2%" />
    </div>

    <div class="main-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>初审动态</h3><el-button link type="primary" @click="$router.push('/gov/review')">全部</el-button></div>
        <el-table :data="flows" size="small">
          <el-table-column prop="company" label="企业" min-width="170" />
          <el-table-column prop="period" label="核算期" width="90" />
          <el-table-column prop="event" label="事件" min-width="190">
            <template #default="{ row }"><span :style="{ color: evColor[row.type] }">{{ row.event }}</span></template>
          </el-table-column>
          <el-table-column prop="time" label="时间" width="130" />
        </el-table>
      </div>

      <div class="page-card">
        <div class="page-card-title"><h3>各市州预警分布</h3><el-tag size="small" round>F6 波动率</el-tag></div>
        <div ref="barRef" class="chart-box"></div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-card-title"><h3>快捷入口</h3></div>
      <div class="quick-grid-inner">
        <button v-for="q in quicks" :key="q.path" class="qk-card" @click="$router.push(q.path)">
          <el-icon :size="24" :color="q.color"><component :is="q.icon" /></el-icon>
          <div class="qk-name">{{ q.name }}</div>
          <div class="qk-desc">{{ q.desc }}</div>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import StatCard from '@/components/StatCard.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const barRef = ref<HTMLElement>()
let bar: echarts.ECharts | null = null

const flows = [
  { company: '六盘水黔能矿业有限公司', period: '2026-10', event: '触发高等级预警：波动率 41.2%', type: 'high', time: '09:42' },
  { company: '中伟新材料股份有限公司', period: '2026-10', event: '核算初审通过 · 已派发核查', type: 'ok', time: '10:18' },
  { company: '遵义铝业股份有限公司', period: '2026-09', event: '数据退回：范围二因子引用过期', type: 'back', time: '10:36' },
  { company: '贵州黔能煤化工有限责任公司', period: '2026-10', event: '缺失化验单附件，提示补传', type: 'mid', time: '11:02' },
  { company: '贵州磷化（集团）有限责任公司', period: '2026-10', event: '化验单多模态识别完成，待人工确认', type: 'mid', time: '11:20' },
  { company: '贵州钢绳股份有限公司', period: '2026-09', event: '复核通过 · 电子签章已归档', type: 'ok', time: '11:47' },
  { company: '西南水泥（贵州）有限公司', period: '2026-10', event: '熟料产量环比 +18%，触发中等级预警', type: 'mid', time: '13:05' },
  { company: '乌江航运物流集团有限公司', period: '2026-10', event: '水运里程存证上链，哈希校验一致', type: 'ok', time: '14:12' }
]
const evColor: Record<string, string> = { high: '#f56c6c', ok: '#00b581', back: '#a06bff', mid: '#ffb020' }

const quicks = [
  { name: '态势感知大屏', desc: '全省热力与动态数据', path: '/gov/big-screen', icon: 'DataBoard', color: '#00b581' },
  { name: '智能初审台', desc: '异常比对与预警', path: '/gov/review', icon: 'Warning', color: '#ffb020' },
  { name: '核查派单', desc: '线下核查与 H5 打卡', path: '/gov/dispatch', icon: 'Position', color: '#00d1ff' },
  { name: '因子库管理', desc: '省级因子版本维护', path: '/gov/factors', icon: 'Collection', color: '#a06bff' },
  { name: '电子签章', desc: '初审意见书盖章', path: '/gov/seal', icon: 'Stamp', color: '#ff6b6b' }
]

function renderBar() {
  bar = echarts.init(barRef.value!)
  bar.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 10, top: 20, bottom: 6, containLabel: true },
    xAxis: { type: 'category', data: ['贵阳', '遵义', '六盘水', '毕节', '安顺', '铜仁', '黔西南', '黔东南', '黔南'], axisLabel: { color: '#93a1b5' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef2f7' } }, axisLabel: { color: '#93a1b5' } },
    series: [{ type: 'bar', barWidth: 16, data: [4, 6, 9, 5, 2, 1, 3, 2, 2], itemStyle: { color: '#ffb020', borderRadius: [5, 5, 0, 0] } }]
  })
}
const resize = () => bar?.resize()
onMounted(() => { renderBar(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); bar?.dispose() })
</script>

<style scoped lang="scss">
.banner-btn { margin-top: 16px; display: flex; gap: 10px; }
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.main-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.chart-box { height: 280px; }
.quick-grid-inner { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.qk-card { padding: 20px 14px; border-radius: 12px; border: 1px solid var(--qn-border); background: #fbfcfe; cursor: pointer; text-align: center; transition: all 0.2s;
  &:hover { transform: translateY(-3px); box-shadow: var(--qn-shadow); border-color: var(--qn-primary); }
  .qk-name { font-size: 14px; font-weight: 600; color: var(--qn-text); margin-top: 10px; }
  .qk-desc { font-size: 11.5px; color: var(--qn-text-3); margin-top: 4px; } }
@media (max-width: 1100px) { .stat-grid, .main-grid { grid-template-columns: 1fr 1fr; } .quick-grid-inner { grid-template-columns: repeat(2, 1fr); } }
</style>