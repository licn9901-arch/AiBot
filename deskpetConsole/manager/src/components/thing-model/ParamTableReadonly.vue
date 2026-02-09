<template>
  <div class="param-table-readonly">
    <div v-if="!params || params.length === 0" class="empty-hint">暂无参数</div>
    <el-table v-else :data="params" size="small" border style="width: 100%">
      <el-table-column prop="identifier" label="标识" width="140" />
      <el-table-column prop="name" label="名称" width="140" />
      <el-table-column prop="dataType" label="数据类型" width="100" />
      <el-table-column label="规格">
        <template #default="{ row }">
          <SpecsDisplay :specs="row.specs || null" :data-type="row.dataType || ''" />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import SpecsDisplay from './SpecsDisplay.vue'

defineProps<{
  params: Record<string, any>[] | null
}>()
</script>

<style scoped>
.param-table-readonly {
  padding: 8px 0;
}
.empty-hint {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
  padding: 8px 0;
}
</style>
