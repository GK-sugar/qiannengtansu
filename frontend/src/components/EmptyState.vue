<template>
  <div class="empty-state">
    <img :src="img" alt="empty" class="empty-img" />
    <div class="empty-title">{{ title }}</div>
    <div class="empty-desc">{{ desc }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import emptyDefault from '@/assets/empty/empty-default.svg'
import emptySearch from '@/assets/empty/empty-search.svg'

const props = withDefaults(
  defineProps<{ title?: string; desc?: string; type?: 'default' | 'search' }>(),
  { title: '暂无数据', desc: '这里还没有内容，快去创建第一条记录吧', type: 'default' }
)
const img = computed(() => (props.type === 'search' ? emptySearch : emptyDefault))
</script>

<style scoped lang="scss">
.empty-state {
  padding: 48px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--qn-text-3);
}
.empty-img { width: 150px; opacity: 0.9; }
.empty-title { font-size: 14px; color: var(--qn-text-2); font-weight: 600; }
.empty-desc { font-size: 12.5px; }
</style>