<template>
  <div class="param-table">
    <div v-for="(item, index) in list" :key="item._key" class="param-row">
      <el-input
        v-model="item.identifier"
        placeholder="标识"
        style="width: 120px"
        @input="emitValue"
      />
      <el-input
        v-model="item.name"
        placeholder="名称"
        style="width: 120px; margin-left: 8px"
        @input="emitValue"
      />
      <el-select
        v-model="item.dataType"
        placeholder="数据类型"
        style="width: 110px; margin-left: 8px"
        @change="onDataTypeChange(index)"
      >
        <el-option
          v-for="opt in DATA_TYPE_OPTIONS"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
      <div class="param-specs">
        <SpecsEditor
          v-model="item.specs"
          :data-type="item.dataType"
          compact
          @update:model-value="emitValue"
        />
      </div>
      <el-button link type="danger" @click="removeRow(index)" style="margin-left: 8px">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
    <el-button size="small" @click="addRow" style="margin-top: 8px">+ 添加参数</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { DATA_TYPE_OPTIONS } from '@/types/product'
import SpecsEditor from './SpecsEditor.vue'

interface ParamRow {
  _key: number
  identifier: string
  name: string
  dataType: string
  specs: Record<string, any> | null
}

const props = defineProps<{
  modelValue: Record<string, any>[] | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>[] | null): void
}>()

let keyCounter = 0
const list = ref<ParamRow[]>([])
// 防止内部 emit 触发 watch 重新初始化
let internalUpdate = false

function initFromValue(val: Record<string, any>[] | null) {
  if (!val || val.length === 0) {
    list.value = []
    return
  }
  list.value = val.map(item => ({
    _key: keyCounter++,
    identifier: item.identifier || '',
    name: item.name || '',
    dataType: item.dataType || 'string',
    specs: item.specs ? { ...item.specs } : null
  }))
}

watch(() => props.modelValue, (v) => {
  if (internalUpdate) {
    internalUpdate = false
    return
  }
  initFromValue(v)
}, { immediate: true })

function addRow() {
  list.value.push({ _key: keyCounter++, identifier: '', name: '', dataType: 'string', specs: null })
  emitValue()
}

function removeRow(index: number) {
  list.value.splice(index, 1)
  emitValue()
}

function onDataTypeChange(index: number) {
  list.value[index].specs = null
  emitValue()
}

function emitValue() {
  internalUpdate = true
  if (list.value.length === 0) {
    emit('update:modelValue', null)
    return
  }
  const out = list.value.map(item => {
    const obj: Record<string, any> = {
      identifier: item.identifier,
      name: item.name,
      dataType: item.dataType
    }
    if (item.specs) obj.specs = item.specs
    return obj
  })
  emit('update:modelValue', out)
}
</script>

<style scoped>
.param-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  padding: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-fill-color-blank);
}
.param-specs {
  flex: 1;
  margin-left: 8px;
  min-width: 0;
}
</style>
