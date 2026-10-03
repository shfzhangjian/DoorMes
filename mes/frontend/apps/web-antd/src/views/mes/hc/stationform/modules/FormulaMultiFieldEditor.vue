<script setup lang="ts">
import { computed } from 'vue';
import { Button, Checkbox, Input, Select } from 'ant-design-vue';
import { MAX_FORMULA_FIELDS, multiFields } from './formula-multi-fields';
import type { FormulaSubField } from './formula-multi-fields';
const props = defineProps<{ value?: string }>();
const emit = defineEmits<{ 'update:value': [value: string] }>();
const fields = computed(() => multiFields({ fieldDefinitionsJson: props.value }));
function save(fields: FormulaSubField[]) { emit('update:value', JSON.stringify(fields)); }
function update(index: number, patch: Partial<FormulaSubField>) {
  save(fields.value.map((field, i) => i === index ? { ...field, ...patch } : field));
}
function add() {
  if (fields.value.length >= MAX_FORMULA_FIELDS) return;
  save([...fields.value, { key: `f_${`${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`}`, label: '', type: 'TEXT', unit: '', required: true }]);
}
function move(index: number, offset: number) {
  const next = [...fields.value];
  const target = index + offset;
  if (target < 0 || target >= next.length) return;
  [next[index], next[target]] = [next[target]!, next[index]!];
  save(next);
}
</script>
<template>
  <div class="multi-editor">
    <div class="multi-editor-fields">
    <div v-for="(field, index) in fields" :key="field.key" class="multi-editor-row">
      <Input :value="field.label" :maxlength="80" placeholder="子字段名称" @update:value="(value) => update(index, { label: value })" />
      <Select :value="field.type" :options="[{ label: '文本', value: 'TEXT' }, { label: '数字', value: 'NUMBER' }]" @update:value="(value) => update(index, { type: value as FormulaSubField['type'] })" />
      <Input :value="field.unit" :maxlength="20" placeholder="单位（选填）" @update:value="(value) => update(index, { unit: value })" />
      <Checkbox :checked="field.required" @update:checked="(value) => update(index, { required: value })">必填</Checkbox>
      <div>
        <Button size="small" :disabled="index === 0" @click="move(index, -1)">上移</Button>
        <Button size="small" :disabled="index === fields.length - 1" @click="move(index, 1)">下移</Button>
        <Button size="small" danger @click="save(fields.filter((_, i) => i !== index))">删除</Button>
      </div>
    </div>
    </div>
    <Button class="multi-editor-add" size="small" :disabled="fields.length >= MAX_FORMULA_FIELDS" @click="add">新增子字段（{{ fields.length }}/{{ MAX_FORMULA_FIELDS }}）</Button>
  </div>
</template>
<style scoped>
.multi-editor { display: grid; gap: 8px; width: 100%; min-width: 0; margin-top: 8px; }
.multi-editor-fields { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr)); gap: 8px; max-height: 340px; overflow: auto; align-items: start; }
.multi-editor-row { display: grid; grid-template-columns: minmax(0, 1fr) 85px; gap: 6px; min-width: 0; padding: 8px; border: 1px solid #dbe3ec; border-radius: 4px; background: #fff; }
.multi-editor-row > * { min-width: 0; }
.multi-editor-row > div:last-child { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 4px; }
.multi-editor-add { justify-self: start; max-width: 100%; }
</style>
