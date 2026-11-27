<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">国际标准复核</div>
      <div class="sub">对企业数据包按 ISO 14067 / EU CBAM 方法学逐项核对：方法学、因子、系统边界、数据质量，复核通过后方可签发护照</div>
    </div>

    <div class="review-grid">
      <div class="page-card">
        <div class="page-card-title"><h3>复核队列</h3></div>
        <el-table :data="queue" size="small" highlight-current-row @current-change="onPick">
          <el-table-column prop="file" label="数据包" min-width="190" />
          <el-table-column prop="company" label="企业" min-width="140" />
          <el-table-column prop="pct" label="进度" width="120">
            <template #default="{ row }"><el-progress :percentage="row.pct" :stroke-width="6" :color="row.pct === 100 ? '#00b581' : '#00d1ff'" /></template>
          </el-table-column>
          <el-table-column prop="owner" label="复核员" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" round :type="row.status === '通过' ? 'success' : 'warning'">{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="page-card">
        <div class="page-card-title">
          <h3>核对清单{{ current ? ' · ' + current.file : '' }}</h3>
          <el-tag v-if="current?.pct === 100" type="success" round>可签发护照</el-tag>
        </div>
        <template v-if="current">
          <div class="check-list">
            <div v-for="c in checks" :key="c.name" class="check-item" :class="{ fail: c.result === 'fail' }">
              <div class="ci-head">
                <span class="ci-name">{{ c.name }}</span>
                <el-tag size="small" :type="c.result === 'pass' ? 'success' : 'warning'" round>{{ c.result === 'pass' ? '一致' : '待整改' }}</el-tag>
              </div>
              <div class="ci-detail">标准值：<b>{{ c.std }}</b> ｜ 申报值：<b>{{ c.actual }}</b></div>
              <div class="ci-comment">{{ c.comment }}</div>
            </div>
          </div>
          <div class="conclusion">
            <el-input v-model="opinion" type="textarea" :rows="3" placeholder="复核意见（LLM 可辅助润色）" />
            <div class="btn-row">
              <el-button type="success" @click="onPass">复核通过 · 转入签发</el-button>
              <el-button type="warning" plain @click="onBack">退回整改</el-button>
            </div>
          </div>
        </template>
        <EmptyState v-else title="请选择数据包" desc="在左侧选择待复核数据包，逐项核对" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import EmptyState from '@/components/EmptyState.vue'

interface Pkg { file: string; company: string; pct: number; owner: string; status: string }

const queue: Pkg[] = [
  { file: 'CBAM-2026Q3-黔能矿业.xlsx', company: '贵州黔能矿业有限责任公司', pct: 100, owner: '高工', status: '已复核' },
  { file: 'ISO14067-2026-09-磷化集团.xlsx', company: '贵州磷化集团有限责任公司', pct: 60, owner: '高工', status: '复核中' },
  { file: 'CBAM-2026Q3-遵义铝业.xlsx', company: '遵义铝业股份有限公司', pct: 20, owner: '林工', status: '复核中' },
  { file: 'CBAM-2026Q3-中伟新材料.xlsx', company: '中伟新材料股份有限公司', pct: 45, owner: '林工', status: '复核中' },
  { file: 'ISO14067-2026-10-振华新材料.xlsx', company: '贵州振华新材料股份有限公司', pct: 100, owner: '高工', status: '已复核' },
  { file: 'CBAM-2026Q3-贵州钢绳.xlsx', company: '贵州钢绳股份有限公司', pct: 70, owner: '高工', status: '复核中' },
  { file: 'ISO14067-2026-10-西南水泥.xlsx', company: '贵州西南水泥有限公司', pct: 30, owner: '林工', status: '复核中' },
  { file: 'CBAM-2026Q3-黔能煤化工.xlsx', company: '贵州黔能煤化工有限责任公司', pct: 10, owner: '林工', status: '复核中' }
]
const current = ref<Pkg | null>(queue[0])
const opinion = ref('方法学适用（ISO 14067:2018 / EU CBAM 过渡期）、因子引用省级库 v3.2、边界完整，数据质量 DQR=0.87，建议通过复核并签发碳足迹护照。')

const checks = computed(() => current.value?.company.includes('磷化') ? [
  { name: '方法学一致性', std: 'ISO 14067:2018', actual: 'ISO 14067:2018', result: 'pass', comment: '核算标准匹配' },
  { name: '排放因子引用', std: '省级库 ≥ 3.0', actual: 'v3.1', result: 'fail', comment: '因子库版本落后，需更新至 v3.2' },
  { name: '系统边界', std: '摇篮到厂门', actual: '摇篮到厂门', result: 'pass', comment: '' },
  { name: '数据质量 DQR', std: '≥ 0.8', actual: '0.74', result: 'fail', comment: '部分活动数据来自估算' }
] : [
  { name: '方法学一致性', std: 'ISO 14067:2018 / CBAM', actual: 'CBAM 过渡期模板', result: 'pass', comment: '核算标准匹配' },
  { name: '排放因子引用', std: '省级库 ≥ 3.0', actual: 'v3.2', result: 'pass', comment: '' },
  { name: '系统边界', std: '摇篮到厂门', actual: '矿山→洗选→外运', result: 'pass', comment: '边界盖章声明齐全' },
  { name: '数据质量 DQR', std: '≥ 0.8', actual: '0.87', result: 'pass', comment: '化验单与台账交叉验证一致' }
])

function onPick(row: Pkg | null) { if (row) { current.value = row } }
function onPass() {
  ElMessage.success('复核通过！该数据包已流转至「护照签发」')
}
function onBack() {
  ElMessage.warning('已退回企业整改，退回清单随站内信发送')
}
</script>

<style scoped lang="scss">
.review-grid { display: grid; grid-template-columns: 7fr 8fr; gap: 16px; align-items: start; }
.check-list { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px; }
.check-item { border: 1px solid var(--qn-border); border-radius: 10px; padding: 12px 14px; background: #fbfcfe;
  &.fail { border-color: #ffd591; background: #fffaf3; }
  .ci-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
  .ci-name { font-size: 13.5px; font-weight: 600; color: var(--qn-text); }
  .ci-detail { font-size: 12px; color: var(--qn-text-2); b { color: var(--qn-text); } }
  .ci-comment { font-size: 11.5px; color: #e6a23c; margin-top: 6px; } }
.conclusion .btn-row { margin-top: 12px; display: flex; gap: 10px; }
@media (max-width: 1100px) { .review-grid { grid-template-columns: 1fr; } }
</style>