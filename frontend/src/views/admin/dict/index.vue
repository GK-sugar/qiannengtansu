<template>
  <div class="page">
    <div class="hero-banner">
      <div class="title">系统数据字典与基础配置</div>
      <div class="sub">统一维护行业分类、物料分类、省份区域等基础数据字典，以及平台级运行参数（异常阈值、因子库版本等），服务管控规则实时读取</div>
    </div>

    <div class="layout">
      <!-- 左：字典分类 -->
      <div class="page-card side">
        <div class="page-card-title"><h3>字典分类</h3><el-button link type="primary" size="small" :icon="Plus" @click="openCat">新增</el-button></div>
        <div class="dict-nav">
          <div v-for="d in dicts" :key="d.key" class="nav-item" :class="{ active: curKey === d.key }" @click="curKey = d.key">
            <el-icon :size="15"><component :is="d.icon" /></el-icon>
            <span class="nav-name">{{ d.name }}</span>
            <span class="nav-count">{{ d.items.length }}</span>
          </div>
        </div>
        <div class="side-tip"><el-icon><InfoFilled /></el-icon>字典改动实时写入 Redis 缓存并生效，全量变更进审计日志</div>
      </div>

      <!-- 右：字典项 -->
      <div class="page-card main">
        <div class="page-card-title">
          <h3>{{ cur?.name }}（{{ cur?.code }}）</h3>
          <div class="toolbar">
            <el-input v-model="kw" placeholder="搜索名称 / 编码" clearable style="width: 200px" :prefix-icon="Search" />
            <el-button type="primary" :icon="Plus" @click="openAdd">新增字典项</el-button>
          </div>
        </div>
        <el-table :data="curFiltered" size="small">
          <el-table-column prop="code" label="字典编码" width="160"><template #default="{ row }"><span class="mono">{{ row.code }}</span></template></el-table-column>
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column prop="remark" label="说明" min-width="230" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="70" align="center" />
          <el-table-column label="状态" width="86">
            <template #default="{ row }"><el-tag size="small" :type="row.on ? 'success' : 'info'" round>{{ row.on ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="row.on = !row.on">{{ row.on ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 基础配置 -->
    <div class="page-card">
      <div class="page-card-title"><h3>平台基础配置（sys_config）</h3><el-button type="primary" size="small" @click="saveConfig">保存配置</el-button></div>
      <el-form :model="cfg" label-width="220px" class="cfg-form">
        <el-form-item label="平台名称"><el-input v-model="cfg.siteName" style="width: 360px" /></el-form-item>
        <el-form-item label="异常判定阈值（F6）">
          <el-input-number v-model="cfg.anomalyThreshold" :min="5" :max="100" /> <span class="unit">%（|X本期 − X基准| / max(X基准, ε) 超出即预警）</span>
        </el-form-item>
        <el-form-item label="省级因子库当前版本"><el-input v-model="cfg.factorVer" style="width: 200px" disabled /><el-tag size="small" type="success" round style="margin-left:10px">由省厅因子库管理维护</el-tag></el-form-item>
        <el-form-item label="登录 Token 有效期"><el-input-number v-model="cfg.tokenHours" :min="1" :max="168" /> <span class="unit">小时（Redis 管理，支持踢人下线）</span></el-form-item>
        <el-form-item label="登录失败锁定"><span class="cfg-line">连续 <el-input-number v-model="cfg.lockTimes" :min="3" :max="10" style="width:90px" /> 次失败锁定 <el-input-number v-model="cfg.lockMinutes" :min="5" :max="120" style="width:90px" /> 分钟</span></el-form-item>
        <el-form-item label="验证码开关"><el-switch v-model="cfg.captcha" /></el-form-item>
        <el-form-item label="数据水印开关"><el-switch v-model="cfg.watermark" /></el-form-item>
        <el-form-item label="哈希链存证盐（site_salt）"><el-input v-model="cfg.salt" type="password" show-password style="width: 360px" /><span class="unit">加密存储，用于 F10/F11，请勿外泄</span></el-form-item>
      </el-form>
    </div>

    <el-dialog v-model="dlg" :title="editing ? '编辑字典项' : '新增字典项 · ' + (cur?.name || '')" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="字典编码"><el-input v-model="form.code" placeholder="如 IND-COAL" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增字典分类弹窗 -->
    <el-dialog v-model="catDlg" title="新增字典分类" width="480px">
      <el-form :model="catForm" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="catForm.name" placeholder="如 温室气体类型" /></el-form-item>
        <el-form-item label="分类编码"><el-input v-model="catForm.code" placeholder="如 dict_ghg_type" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catDlg = false">取消</el-button>
        <el-button type="primary" @click="saveCat">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, InfoFilled } from '@element-plus/icons-vue'

interface DictType { key: string; name: string; code: string; icon: string; items: DictItem[] }
interface DictItem { code: string; name: string; remark: string; sort: number; on: boolean }

const dicts = ref<DictType[]>([
  {
    key: 'industry', name: '行业分类', code: 'dict_industry', icon: 'Grid',
    items: [
      { code: 'IND-COAL', name: '煤炭开采和洗选业', remark: '原煤 / 精煤生产', sort: 1, on: true },
      { code: 'IND-BAT', name: '新能源电池材料', remark: '磷酸铁锂 / 三元前驱体', sort: 2, on: true },
      { code: 'IND-PHOS', name: '磷化工', remark: '磷酸二铵 / 磷系化工', sort: 3, on: true },
      { code: 'IND-ALU', name: '电解铝及铝加工', remark: '高载能行业', sort: 4, on: true },
      { code: 'IND-CEM', name: '水泥制造', remark: '熟料生产线', sort: 5, on: true },
      { code: 'IND-STEEL', name: '金属制品（钢丝绳）', remark: '钢丝拉拔 / 绳股捻制', sort: 6, on: true },
      { code: 'IND-CHEM', name: '煤化工', remark: '焦化 / 煤制甲醇', sort: 7, on: true },
      { code: 'IND-LOG', name: '交通运输与物流', remark: '公路 / 铁路 / 水运周转', sort: 8, on: true },
      { code: 'IND-POWER', name: '电力生产', remark: '火电 / 水电 / 新能源', sort: 9, on: true }
    ]
  },
  {
    key: 'material', name: '物料分类', code: 'dict_material', icon: 'Box',
    items: [
      { code: 'MAT-RCOAL', name: '原煤', remark: '矿井自用 / 外售', sort: 1, on: true },
      { code: 'MAT-CCOAL', name: '精煤（洗选产品）', remark: '护照产品主物料', sort: 2, on: true },
      { code: 'MAT-LFP', name: '磷酸铁锂', remark: '正极材料', sort: 3, on: true },
      { code: 'MAT-DAP', name: '磷酸二铵', remark: '化肥产品', sort: 4, on: true },
      { code: 'MAT-ELEC', name: '电力（外购）', remark: '范围二载体', sort: 5, on: true },
      { code: 'MAT-COKE', name: '焦炭', remark: '焦化产品 / 还原剂', sort: 6, on: true },
      { code: 'MAT-AL', name: '铝锭', remark: '电解铝产品', sort: 7, on: true },
      { code: 'MAT-CLINKER', name: '水泥熟料', remark: '过程排放主物料', sort: 8, on: true },
      { code: 'MAT-WIRE', name: '钢丝绳', remark: '金属制品产品', sort: 9, on: true },
      { code: 'MAT-DIESEL', name: '柴油', remark: '运输 / 设备燃料', sort: 10, on: true }
    ]
  },
  {
    key: 'region', name: '省份 / 区域', code: 'dict_region', icon: 'Location',
    items: [
      { code: 'GZ-520100', name: '贵阳市', remark: '省会 · 数据中心', sort: 1, on: true },
      { code: 'GZ-520200', name: '六盘水市', remark: '煤炭主产区', sort: 2, on: true },
      { code: 'GZ-520300', name: '遵义市', remark: '', sort: 3, on: true },
      { code: 'GZ-522300', name: '黔西南州', remark: '', sort: 4, on: true },
      { code: 'GZ-522600', name: '黔东南州', remark: '', sort: 5, on: true },
      { code: 'GZ-520400', name: '安顺市', remark: '', sort: 6, on: true },
      { code: 'GZ-520500', name: '毕节市', remark: '煤炭主产区', sort: 7, on: true },
      { code: 'GZ-520600', name: '铜仁市', remark: '锰系材料产区', sort: 8, on: true },
      { code: 'GZ-522700', name: '黔南州', remark: '磷化工主产区', sort: 9, on: true }
    ]
  },
  {
    key: 'source', name: '排放源分类', code: 'dict_emission_source', icon: 'Cloudy',
    items: [
      { code: 'SRC-FUEL', name: '化石燃料燃烧', remark: '范围一 · F2 完全氧化法', sort: 1, on: true },
      { code: 'SRC-ELEC', name: '外购电力', remark: '范围二 · F3 区域电网因子', sort: 2, on: true },
      { code: 'SRC-TRANS', name: '运输周转', remark: '范围三 · F4 周转量法', sort: 3, on: true },
      { code: 'SRC-PROC', name: '工业过程排放', remark: '范围一 · 工艺逸散', sort: 4, on: true },
      { code: 'SRC-CH4', name: '煤矿瓦斯逸散', remark: '范围一 · CH₄ 按 GWP 折算', sort: 5, on: true },
      { code: 'SRC-ANODE', name: '电解铝阳极效应', remark: '范围一 · PFCs 逸散', sort: 6, on: true },
      { code: 'SRC-CARB', name: '碳酸盐分解', remark: '范围一 · 石灰石 / 熟料煅烧', sort: 7, on: true },
      { code: 'SRC-HEAT', name: '外购热力', remark: '范围二 · 蒸汽 / 热水', sort: 8, on: true }
    ]
  }
])

const curKey = ref('industry')
const cur = computed(() => dicts.value.find(d => d.key === curKey.value))
const kw = ref('')
const curFiltered = computed(() => (cur.value?.items ?? []).filter(i => !kw.value || i.name.includes(kw.value) || i.code.toLowerCase().includes(kw.value.toLowerCase())))

/* 基础配置 */
const cfg = reactive({
  siteName: '黔能碳溯 · 贵州省重点产业碳足迹治理平台',
  anomalyThreshold: 20,
  factorVer: 'v3.2（132 项因子）',
  tokenHours: 24,
  lockTimes: 5,
  lockMinutes: 15,
  captcha: true,
  watermark: true,
  salt: 'qnct-site-salt-demo-2026'
})
function saveConfig() { ElMessage.success('基础配置已保存并同步至 Redis 缓存，变更已写入审计日志') }

/* 字典项维护 */
const dlg = ref(false)
const editing = ref(false)
const form = reactive({ code: '', name: '', remark: '', sort: 1 })
function openAdd() { editing.value = false; Object.assign(form, { code: '', name: '', remark: '', sort: (cur.value?.items.length ?? 0) + 1 }); dlg.value = true }
function openEdit(row: DictItem) { editing.value = true; Object.assign(form, row); dlg.value = true }
function saveItem() {
  if (!form.code || !form.name) { ElMessage.warning('请填写编码与名称'); return }
  if (editing.value) {
    const row = cur.value?.items.find(i => i.name === form.name || i.code === form.code)
    if (row) Object.assign(row, form)
  } else {
    cur.value?.items.push({ ...form, on: true })
  }
  dlg.value = false
  ElMessage.success('已保存：' + form.name + '（变更同步哈希链存证）')
}

/* 字典分类维护 */
const catDlg = ref(false)
const catForm = reactive({ name: '', code: '' })
function openCat() { Object.assign(catForm, { name: '', code: '' }); catDlg.value = true }
function saveCat() {
  if (!catForm.name || !catForm.code) { ElMessage.warning('请填写分类名称与编码'); return }
  if (dicts.value.some(d => d.code === catForm.code)) { ElMessage.warning('分类编码「' + catForm.code + '」已存在'); return }
  const key = 'cat-' + Date.now().toString(36)
  dicts.value.push({ key, name: catForm.name, code: catForm.code, icon: 'Menu', items: [] })
  curKey.value = key
  catDlg.value = false
  ElMessage.success('字典分类「' + catForm.name + '」已创建，请在右侧为其新增字典项')
}
</script>

<style scoped lang="scss">
.layout { display: grid; grid-template-columns: 264px 1fr; gap: 16px; align-items: start; }
.side { padding: 18px; }
.dict-nav { display: flex; flex-direction: column; gap: 6px; }
.nav-item { display: flex; align-items: center; gap: 10px; padding: 11px 13px; border-radius: 10px; cursor: pointer; color: var(--qn-text-2); font-size: 13.5px; transition: all .18s;
  &:hover { background: #f0f6f3; color: var(--qn-primary); }
  &.active { background: linear-gradient(90deg, rgba(0,181,129,0.12), rgba(0,209,255,0.06)); color: var(--qn-primary); font-weight: 600; box-shadow: inset 3px 0 0 var(--qn-primary); } }
.nav-count { margin-left: auto; font-size: 11px; color: var(--qn-text-3); background: #eef2f7; border-radius: 10px; padding: 1px 8px;
  .active & { background: rgba(0,181,129,0.16); color: var(--qn-primary); } }
.side-tip { margin-top: 14px; font-size: 11px; color: var(--qn-text-3); line-height: 1.7; display: flex; gap: 6px; }
.toolbar { display: flex; gap: 10px; }
.mono { font-family: ui-monospace, Consolas, monospace; font-size: 12px; color: #4d6c89; }
.cfg-form { padding-top: 6px; max-width: 860px; }
.unit { margin-left: 10px; font-size: 11.5px; color: var(--qn-text-3); }
.cfg-line { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; color: var(--qn-text-2); }
:deep(.el-form-item) { margin-bottom: 18px; }
@media (max-width: 1000px) { .layout { grid-template-columns: 1fr; } }
</style>