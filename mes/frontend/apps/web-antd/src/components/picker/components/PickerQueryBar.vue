<script lang="ts" setup>
import { ref, computed } from 'vue';

import { Button, Form, Input, Select } from 'ant-design-vue';

import type { PickerQueryField } from '../types';

const props = withDefaults(
  defineProps<{
    fields: PickerQueryField[];
    modelValue: Record<string, any>;
    loading?: boolean;
  }>(),
  { loading: false },
);

const emit = defineEmits<{
  'update:modelValue': [Record<string, any>];
  search: [];
  reset: [];
}>();

const expanded = ref(false);

const hasHiddenFields = computed(() => props.fields.some((f) => f.defaultHidden));

const visibleFields = computed(() =>
  props.fields.filter((f) => !f.defaultHidden || expanded.value),
);

function onFieldChange(field: string, value: any) {
  emit('update:modelValue', { ...props.modelValue, [field]: value });
}

function resolvePopupContainer(triggerNode: HTMLElement) {
  return (
    (triggerNode.closest('[role="dialog"]') as HTMLElement | null) ||
    triggerNode.parentElement ||
    triggerNode.ownerDocument?.body ||
    document.body
  );
}

function handleSearch() {
  emit('search');
}

function handleReset() {
  const cleared: Record<string, any> = {};
  props.fields.forEach((f) => {
    cleared[f.field] = undefined;
  });
  emit('update:modelValue', cleared);
  emit('reset');
}
</script>

<template>
  <div class="hc-picker-query-bar">
    <Form layout="inline" class="hc-picker-query-bar__form">
      <div class="hc-picker-query-bar__fields">
        <Form.Item
          v-for="field in visibleFields"
          :key="field.field"
          :label="field.label"
        >
          <Select
            v-if="field.type === 'select'"
            :value="modelValue[field.field]"
            :options="field.options"
            :placeholder="field.placeholder ?? `请选择${field.label}`"
            :get-popup-container="resolvePopupContainer"
            allow-clear
            style="min-width: 160px"
            @change="(v) => onFieldChange(field.field, v)"
          />
          <Input
            v-else
            :value="modelValue[field.field]"
            :placeholder="field.placeholder ?? `请输入${field.label}`"
            allow-clear
            style="min-width: 160px"
            @update:value="(v) => onFieldChange(field.field, v)"
            @press-enter="handleSearch"
          />
        </Form.Item>
      </div>
      <div class="hc-picker-query-bar__actions">
        <Button type="primary" :loading="loading" @click="handleSearch">查询</Button>
        <Button @click="handleReset">重置</Button>
        <Button v-if="hasHiddenFields" type="link" @click="expanded = !expanded">
          {{ expanded ? '收起' : '展开' }}
        </Button>
      </div>
    </Form>
  </div>
</template>

<style scoped>
.hc-picker-query-bar {
  padding: 0 0 12px;
}

.hc-picker-query-bar__form {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.hc-picker-query-bar__fields {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px 0;
}

.hc-picker-query-bar__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  align-self: flex-start;
  gap: 8px;
  margin-left: auto;
}
</style>
