<template>
  <!--
    动态扩展属性表单（物料页/销售行/采购行/工单 共用）
    schema: 属性定义列表（来自 getEffAttrSchema，含继承）
    modelValue: { attrCode: value } 扁平值对象
    schema 中没有但 modelValue 里存在的 key → 作为"游离历史值"只读降级展示（设计文档 12.1）
  -->
  <div class="ext-attr-form">
    <el-empty v-if="!renderList.length" description="该分类未配置扩展属性" :image-size="60" />
    <el-form v-else ref="formRef" :model="formData" label-width="120px" class="ext-attr-inner">
      <el-form-item
        v-for="item in renderList"
        :key="item.attrCode"
        :label="labelText(item)"
        :prop="item.attrCode"
        :rules="item.required === '1' ? [{ required: true, message: `${item.attrName}不能为空`, trigger: 'blur' }] : undefined"
      >
        <!-- TEXT -->
        <el-input v-if="!item.attrType || item.attrType === 'TEXT'"
          v-model="formData[item.attrCode!]" placeholder="请输入" clearable :disabled="item.readonly" />
        <!-- NUMBER -->
        <el-input-number v-else-if="item.attrType === 'NUMBER'"
          v-model="formData[item.attrCode!]" :controls="false" style="width: 100%"
          placeholder="请输入" :disabled="item.readonly" />
        <!-- SELECT -->
        <el-select v-else-if="item.attrType === 'SELECT'"
          v-model="formData[item.attrCode!]" placeholder="请选择" clearable :disabled="item.readonly" style="width: 100%">
          <el-option v-for="opt in parseOptions(item.optionsJson)" :key="opt" :label="opt" :value="opt" />
        </el-select>
        <!-- BOOL -->
        <el-switch v-else-if="item.attrType === 'BOOL'"
          v-model="formData[item.attrCode!]" :disabled="item.readonly" />
        <!-- DATE -->
        <el-date-picker v-else-if="item.attrType === 'DATE'"
          v-model="formData[item.attrCode!]" type="date" value-format="YYYY-MM-DD"
          placeholder="请选择" :disabled="item.readonly" style="width: 100%" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { MdItemTypeAttr } from '@/types/api/mes/md/attr'

/** 游离历史值（schema 中已删除但 lineAttrs 里残留的 key）的哨兵标记 */
const LEGACY_ATTR_ID = -1
const LEGACY_INHERIT_DEPTH = -1

const props = defineProps<{
  schema: MdItemTypeAttr[]
  modelValue?: Record<string, any>
  /** 是否只读（单据查看态） */
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: Record<string, any>): void
}>()

// 内部可编辑数据副本
const formData = reactive<Record<string, any>>({})

/** 合并 schema 属性 + modelValue 中游离的历史 key（降级只读展示） */
const renderList = computed<(MdItemTypeAttr & { readonly?: boolean })[]>(() => {
  const list: (MdItemTypeAttr & { readonly?: boolean })[] = []
  const covered = new Set<string>()
  for (const s of props.schema || []) {
    if (s.attrCode) {
      list.push({ ...s, readonly: props.readonly })
      covered.add(s.attrCode)
    }
  }
  // 游离历史值：在 modelValue 但不在 schema 中（属性后被删/改名），只读降级展示
  if (props.modelValue) {
    for (const key of Object.keys(props.modelValue)) {
      if (!covered.has(key) && props.modelValue[key] !== null && props.modelValue[key] !== undefined && props.modelValue[key] !== '') {
        list.push({
          attrId: LEGACY_ATTR_ID, attrCode: key, attrName: key, attrType: 'TEXT',
          inheritDepth: LEGACY_INHERIT_DEPTH, readonly: true
        })
      }
    }
  }
  return list
})

function labelText(item: MdItemTypeAttr): string {
  return item.attrName + (item.attrUnit ? `(${item.attrUnit})` : '')
    + (item.inheritDepth && item.inheritDepth > 0 ? ' [继承]' : '')
}

function parseOptions(optionsJson?: string): string[] {
  if (!optionsJson) return []
  try {
    const arr = JSON.parse(optionsJson)
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

// modelValue → formData（外部变化时同步）
watch(() => props.modelValue, (val: Record<string, any> | undefined) => {
  if (!val) return
  for (const k of Object.keys(val)) {
    if (formData[k] !== val[k]) formData[k] = val[k]
  }
}, { immediate: true, deep: true })

// formData → emit（内部编辑时上抛）
// 注意：合并父级传入值与内部编辑值，避免覆盖父级单独绑定的业务字段（如采购 PAPER_ROLL_COUNT）
watch(formData, (val: Record<string, any>) => {
  emit('update:modelValue', { ...(props.modelValue || {}), ...val })
}, { deep: true })

/** 暴露校验方法（供父组件提交前调用） */
async function validate() {
  // el-form 无 ref 绑定时跳过；这里通过 DOM 校验 required
  for (const item of props.schema || []) {
    if (item.required === '1' && item.attrCode) {
      const v = formData[item.attrCode]
      if (v === null || v === undefined || v === '') {
        return Promise.reject(new Error(`${item.attrName}不能为空`))
      }
    }
  }
  return Promise.resolve()
}
defineExpose({ validate })
</script>

<style scoped>
.ext-attr-form { width: 100%; }
.ext-attr-inner { max-width: 600px; }
</style>
