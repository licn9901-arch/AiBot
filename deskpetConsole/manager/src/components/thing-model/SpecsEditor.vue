<template>
  <div :class="['specs-editor', { compact }]">
    <!-- int / float / double -->
    <template v-if="isNumeric">
      <el-row :gutter="12">
        <el-col :span="12">
          <div class="field-label">最小值</div>
          <el-input-number v-model="local.min" :controls="false" placeholder="min" style="width: 100%" />
        </el-col>
        <el-col :span="12">
          <div class="field-label">最大值</div>
          <el-input-number v-model="local.max" :controls="false" placeholder="max" style="width: 100%" />
        </el-col>
      </el-row>
      <el-row :gutter="12" style="margin-top: 8px">
        <el-col :span="12">
          <div class="field-label">单位</div>
          <el-input v-model="local.unit" placeholder="如 ℃" />
        </el-col>
        <el-col :span="12">
          <div class="field-label">步长</div>
          <el-input-number v-model="local.step" :controls="false" placeholder="step" style="width: 100%" />
        </el-col>
      </el-row>
    </template>

    <!-- bool -->
    <template v-else-if="dataType === 'bool'">
      <el-row :gutter="12">
        <el-col :span="12">
          <div class="field-label">0 值标签</div>
          <el-input v-model="local['0']" placeholder="如 关闭" />
        </el-col>
        <el-col :span="12">
          <div class="field-label">1 值标签</div>
          <el-input v-model="local['1']" placeholder="如 开启" />
        </el-col>
      </el-row>
    </template>

    <!-- string -->
    <template v-else-if="dataType === 'string'">
      <div class="field-label">最大长度</div>
      <el-input-number v-model="local.maxLength" :min="1" :controls="false" placeholder="maxLength" style="width: 200px" />
    </template>

    <!-- enum -->
    <template v-else-if="dataType === 'enum'">
      <div v-for="(item, index) in enumList" :key="index" class="enum-row">
        <el-input v-model="item.value" placeholder="枚举值" style="width: 120px" @input="syncEnum" />
        <el-input v-model="item.name" placeholder="说明（如：摇摆）" style="flex: 1; margin: 0 8px" @input="syncEnum" />
        <el-button link type="danger" @click="removeEnum(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
      <el-button size="small" @click="addEnum" style="margin-top: 4px">+ 添加枚举值</el-button>
    </template>

    <!-- struct：嵌套参数表格 -->
    <template v-else-if="dataType === 'struct'">
      <div class="field-label">结构体字段</div>
      <ParamTableAsync v-model="structFields" />
    </template>

    <!-- array -->
    <template v-else-if="dataType === 'array'">
      <el-row :gutter="12">
        <el-col :span="12">
          <div class="field-label">元素类型</div>
          <el-select v-model="local.itemType" style="width: 100%" @change="onArrayItemTypeChange">
            <el-option
              v-for="opt in DATA_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-col>
        <el-col :span="12">
          <div class="field-label">最大长度</div>
          <el-input-number v-model="local.maxSize" :min="1" :controls="false" placeholder="maxSize" style="width: 100%" />
        </el-col>
      </el-row>
      <div v-if="local.itemType === 'struct'" style="margin-top: 8px">
        <div class="field-label">元素结构体字段</div>
        <ParamTableAsync v-model="arrayStructFields" />
      </div>
    </template>

    <!-- 其他类型回退到 JSON -->
    <template v-else>
      <el-input
        v-model="jsonFallback"
        type="textarea"
        :rows="compact ? 2 : 3"
        placeholder="JSON 格式"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, defineAsyncComponent } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { DATA_TYPE_OPTIONS } from '@/types/product'

// 异步导入 ParamTable 打破循环依赖
const ParamTableAsync = defineAsyncComponent(() => import('./ParamTable.vue'))

const props = defineProps<{
  modelValue: Record<string, any> | null
  dataType: string
  compact?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any> | null): void
}>()

const isNumeric = computed(() => ['int', 'float', 'double'].includes(props.dataType))

// 通用 reactive 对象，用于 numeric / bool / string / array
const local = reactive<Record<string, any>>({})

// enum 专用列表
const enumList = ref<{ value: string; name: string }[]>([])

// struct 专用字段列表
const structFields = ref<Record<string, any>[] | null>(null)

// array 中 struct 元素的字段列表
const arrayStructFields = ref<Record<string, any>[] | null>(null)

// 兜底 JSON 文本
const jsonFallback = ref('')

// 防止内部变更触发 watch 循环
let internalUpdate = false

// 从 modelValue 初始化内部状态
function initFromValue(val: Record<string, any> | null) {
  // 清空 local
  Object.keys(local).forEach(k => delete local[k])

  if (!val) {
    enumList.value = []
    structFields.value = null
    arrayStructFields.value = null
    jsonFallback.value = ''
    return
  }

  if (isNumeric.value || props.dataType === 'bool' || props.dataType === 'string') {
    Object.assign(local, val)
  } else if (props.dataType === 'enum') {
    if (Array.isArray(val.values)) {
      enumList.value = val.values.map((item: any) => ({
        value: String(item.value ?? ''),
        name: String(item.name ?? '')
      }))
    } else {
      enumList.value = []
    }
  } else if (props.dataType === 'struct') {
    structFields.value = Array.isArray(val.fields) ? [...val.fields] : null
  } else if (props.dataType === 'array') {
    local.itemType = val.itemType || 'string'
    if (val.maxSize !== undefined) local.maxSize = val.maxSize
    arrayStructFields.value = Array.isArray(val.itemFields) ? [...val.itemFields] : null
  } else {
    jsonFallback.value = JSON.stringify(val, null, 2)
  }
}

// 监听外部值变化
watch(() => props.modelValue, (v) => {
  if (internalUpdate) {
    internalUpdate = false
    return
  }
  initFromValue(v)
}, { immediate: true })

// 监听 dataType 变化时重置
watch(() => props.dataType, () => {
  internalUpdate = true
  emit('update:modelValue', null)
  initFromValue(null)
})

// 监听 local 变化 → 发射（numeric / bool / string / array 基础字段）
watch(local, () => {
  if (isNumeric.value || props.dataType === 'bool' || props.dataType === 'string') {
    const out: Record<string, any> = {}
    for (const [k, v] of Object.entries(local)) {
      if (v !== undefined && v !== null && v !== '') out[k] = v
    }
    internalUpdate = true
    emit('update:modelValue', Object.keys(out).length ? out : null)
  } else if (props.dataType === 'array') {
    emitArrayValue()
  }
}, { deep: true })

// struct 字段变化
watch(structFields, (fields) => {
  if (props.dataType === 'struct') {
    internalUpdate = true
    if (!fields || fields.length === 0) {
      emit('update:modelValue', null)
    } else {
      emit('update:modelValue', { fields })
    }
  }
}, { deep: true })

// array struct 字段变化
watch(arrayStructFields, () => {
  if (props.dataType === 'array') {
    emitArrayValue()
  }
}, { deep: true })

function emitArrayValue() {
  const out: Record<string, any> = {}
  if (local.itemType) out.itemType = local.itemType
  if (local.maxSize !== undefined && local.maxSize !== null && local.maxSize !== '') out.maxSize = local.maxSize
  if (local.itemType === 'struct' && arrayStructFields.value && arrayStructFields.value.length > 0) {
    out.itemFields = arrayStructFields.value
  }
  internalUpdate = true
  emit('update:modelValue', Object.keys(out).length ? out : null)
}

function onArrayItemTypeChange() {
  arrayStructFields.value = null
  emitArrayValue()
}

// enum 操作
function addEnum() {
  enumList.value.push({ value: '', name: '' })
  syncEnum()
}

function removeEnum(index: number) {
  enumList.value.splice(index, 1)
  syncEnum()
}

function syncEnum() {
  const values = enumList.value
    .filter(item => item.value)
    .map(item => ({ value: item.value, name: item.name }))
  internalUpdate = true
  emit('update:modelValue', values.length ? { values } : null)
}

// JSON 兜底
watch(jsonFallback, (v) => {
  if (!['int', 'float', 'double', 'bool', 'string', 'enum', 'struct', 'array'].includes(props.dataType)) {
    internalUpdate = true
    if (!v.trim()) {
      emit('update:modelValue', null)
      return
    }
    try {
      emit('update:modelValue', JSON.parse(v))
    } catch {
      // 格式不正确时不发射
    }
  }
})
</script>

<style scoped>
.specs-editor.compact {
  font-size: 12px;
}
.specs-editor.compact .field-label {
  font-size: 12px;
  margin-bottom: 2px;
}
.field-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.enum-row {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
</style>
