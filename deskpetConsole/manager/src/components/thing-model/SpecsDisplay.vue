<template>
  <span v-if="!specs || !dataType" class="specs-empty">-</span>
  <span v-else-if="isNumeric" class="specs-text">
    [{{ specs.min ?? '*' }}, {{ specs.max ?? '*' }}]
    <template v-if="specs.unit"> {{ specs.unit }}</template>
    <template v-if="specs.step">, step={{ specs.step }}</template>
  </span>
  <span v-else-if="dataType === 'bool'" class="specs-text">
    0: {{ specs['0'] || '关闭' }} / 1: {{ specs['1'] || '开启' }}
  </span>
  <span v-else-if="dataType === 'string'" class="specs-text">
    maxLength: {{ specs.maxLength ?? '-' }}
  </span>
  <span v-else-if="dataType === 'enum'" class="specs-text">
    {{ enumSummary }}
  </span>
  <span v-else-if="dataType === 'struct'" class="specs-text">
    {{ structSummary }}
  </span>
  <span v-else-if="dataType === 'array'" class="specs-text">
    {{ arraySummary }}
  </span>
  <el-tooltip v-else :content="jsonText" placement="top" :show-after="300">
    <span class="specs-json">{{ jsonText }}</span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  specs: Record<string, any> | null
  dataType: string
}>()

const isNumeric = computed(() => ['int', 'float', 'double'].includes(props.dataType))

const enumSummary = computed(() => {
  if (!props.specs) return '-'
  const values = props.specs.values
  if (!Array.isArray(values) || values.length === 0) return '-'
  return values
    .map((item: any) => item.name ? `${item.value}(${item.name})` : item.value)
    .join(' | ')
})

const structSummary = computed(() => {
  if (!props.specs) return '-'
  const fields = props.specs.fields
  if (!Array.isArray(fields) || fields.length === 0) return '-'
  const names = fields.map((f: any) => {
    const id = f.identifier || f.name || '?'
    return `${id}: ${f.dataType || '?'}`
  })
  return `{ ${names.join(', ')} }`
})

const arraySummary = computed(() => {
  if (!props.specs) return '-'
  const parts: string[] = []
  if (props.specs.itemType) parts.push(props.specs.itemType)
  if (props.specs.maxSize) parts.push(`max=${props.specs.maxSize}`)
  if (props.specs.itemType === 'struct' && Array.isArray(props.specs.itemFields)) {
    const names = props.specs.itemFields.map((f: any) => f.identifier || f.name || '?')
    parts.push(`{${names.join(', ')}}`)
  }
  return parts.length ? `[${parts.join(', ')}]` : '-'
})

const jsonText = computed(() => {
  if (!props.specs) return '-'
  return JSON.stringify(props.specs)
})
</script>

<style scoped>
.specs-empty {
  color: var(--el-text-color-placeholder);
}
.specs-text {
  font-family: var(--el-font-family);
  font-size: 13px;
}
.specs-json {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
  vertical-align: middle;
  cursor: default;
}
</style>
