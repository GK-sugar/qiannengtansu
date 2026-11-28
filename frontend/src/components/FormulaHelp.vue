<template>
  <el-button text class="fh-trigger" title="查看页面中 F1–F13 编号的完整定义" @click="open = true">
    <el-icon><QuestionFilled /></el-icon>
    <span>F 指标说明</span>
  </el-button>

  <el-drawer v-model="open" size="min(520px, 100vw)" append-to-body class="fh-drawer">
    <template #header>
      <div class="fh-drawer-head">
        <div class="fh-drawer-title">F1–F13 公式与指标说明</div>
        <div class="fh-drawer-sub">平台统一口径 · 共 {{ FORMULAS.length }} 项指标</div>
      </div>
    </template>
    <div class="fh-note">
      <el-icon><InfoFilled /></el-icon>
      页面中出现的 <b>F1~F13</b> 是平台内置公式编号，含义与算法如下（完整推导见《公式与算法说明》文档）。
    </div>
    <div class="fh-list">
      <div v-for="f in FORMULAS" :key="f.code" class="fh-item">
        <div class="fh-head">
          <span class="fh-code">{{ f.code }}</span>
          <span class="fh-name">{{ f.name }}</span>
          <span class="fh-where">{{ f.where }}</span>
        </div>
        <div class="fh-expr">{{ f.expr }}</div>
        <div class="fh-desc">{{ f.desc }}</div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { InfoFilled, QuestionFilled } from '@element-plus/icons-vue'
import { FORMULAS } from '@/utils/formulas'

const open = ref(false)
</script>

<style scoped lang="scss">
.fh-trigger.el-button {
  color: #00644c;
  font-size: 13px;
  font-weight: 600;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(0, 181, 129, 0.11), rgba(0, 209, 255, 0.14));
  border: 1px solid rgba(0, 181, 129, 0.28);
  transition: background .2s, border-color .2s, box-shadow .2s;
  :deep(.el-icon) { margin-right: 5px; font-size: 15px; color: var(--qn-primary); }
  &:hover, &:focus {
    color: #00543f;
    background: linear-gradient(135deg, rgba(0, 181, 129, 0.18), rgba(0, 209, 255, 0.2));
    border-color: rgba(0, 181, 129, 0.45);
    box-shadow: 0 4px 14px rgba(0, 181, 129, 0.18);
  }
}
.fh-drawer-head { display: flex; flex-direction: column; gap: 3px; }
.fh-drawer-title { font-size: 18px; font-weight: 800; color: var(--qn-text); letter-spacing: .3px; }
.fh-drawer-sub { font-size: 12.5px; color: var(--qn-text-3); font-weight: 500; }
.fh-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
  background: linear-gradient(135deg, #f1faf6, #f2f8fc);
  border: 1px solid #dceee6;
  border-radius: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  :deep(.el-icon) { margin-top: 3px; color: var(--qn-primary); flex-shrink: 0; }
  b { color: #0f172a; }
}
.fh-list { display: flex; flex-direction: column; gap: 14px; }
.fh-item {
  border: 1px solid #e3edf3;
  border-left: 3px solid rgba(0, 181, 129, 0.55);
  border-radius: 12px;
  padding: 14px 16px;
  background: linear-gradient(180deg, #fbfdfc, #f7fafc);
}
.fh-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.fh-code {
  min-width: 42px;
  text-align: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12.5px;
  font-weight: 800;
  color: #00795c;
  background: rgba(0, 133, 96, 0.1);
  border: 1px solid rgba(0, 133, 96, 0.25);
}
.fh-name { font-size: 14.5px; font-weight: 700; color: var(--qn-text); }
.fh-where {
  margin-left: auto;
  font-size: 11.5px;
  color: #557086;
  padding: 2px 9px;
  border-radius: 999px;
  background: #eef4f8;
}
.fh-expr {
  margin-top: 10px;
  padding: 9px 12px;
  border-radius: 9px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13.5px;
  font-weight: 600;
  color: #0f4c3a;
  background: linear-gradient(90deg, rgba(0, 181, 129, 0.09), rgba(0, 209, 255, 0.08));
  border: 1px solid rgba(0, 181, 129, 0.14);
  overflow-x: auto;
}
.fh-desc { margin-top: 8px; font-size: 13px; line-height: 1.75; color: #475569; }
</style>