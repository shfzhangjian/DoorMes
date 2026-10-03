<script lang="ts" setup>
import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { Input } from 'ant-design-vue';

import { usePickerKeyboard } from '../composables/usePickerKeyboard';
import { usePickerPanel } from '../composables/usePickerPanel';
import { usePickerSearch } from '../composables/usePickerSearch';
import type { PickerEntityConfig, PickerOption } from '../types';

const props = withDefaults(
  defineProps<{
    config: PickerEntityConfig;
    modelValue?: string;
    disabled?: boolean;
    placeholder?: string;
    debounce?: number;
    pageSize?: number;
  }>(),
  {
    modelValue: '',
    disabled: false,
    placeholder: '',
    debounce: 300,
    pageSize: 10,
  },
);

const emit = defineEmits<{
  'update:modelValue': [string];
  pick: [PickerOption];
  search: [];
}>();

const inputName = `hc-picker-inline-${props.config.entityKey}-${Math.random()
  .toString(36)
  .slice(2)}`;

const panel = usePickerPanel({ minWidth: props.config.inlinePanelWidth ?? 520 });

function syncWrapRef(el: HTMLElement | null) {
  panel.wrapRef.value = el ?? undefined;
}
function syncPanelRef(el: HTMLElement | null) {
  panel.panelRef.value = el ?? undefined;
}

const inlineSearchConfig: PickerEntityConfig = {
  ...props.config,
  fetchPage: (params) =>
    (props.config.inlineFetchPage ?? props.config.fetchPage)(params),
};

const searcher = usePickerSearch(inlineSearchConfig, {
  debounce: props.debounce,
  pageSize: props.pageSize,
});

const rows = computed(() => searcher.list.value as any[]);

const keyboard = usePickerKeyboard(
  panel.open,
  rows,
  (row) => pickRow(row),
  () => panel.closePanel(),
);

const inlineColumns = computed(() =>
  props.config.columns
    .filter((c) => !c.fixed || c.fixed === 'left')
    .slice(0, 6),
);

function isRecordObject(value: unknown): value is Record<string, any> {
  return !!value && typeof value === 'object';
}

function toSnakeCase(value: string) {
  return value.replace(/([A-Z])/g, '_$1').toLowerCase();
}

function getCandidateRecords(record: any) {
  if (!isRecordObject(record)) return [];
  return [record, record.raw, record.data, record.row, record.record].filter(
    isRecordObject,
  );
}

function getOptionSourceRow(row: any) {
  const candidates = getCandidateRecords(row);
  const optionFields = ['id', ...props.config.columns.map((col) => col.field)];
  return (
    candidates.find((candidate) =>
      optionFields.some((field) => candidate[field] !== undefined),
    ) ?? row
  );
}

function getRecordFieldValue(record: any, field: string) {
  const snakeField = toSnakeCase(field);
  const upperSnakeField = snakeField.toUpperCase();
  for (const candidate of getCandidateRecords(record)) {
    if (candidate[field] !== undefined) return candidate[field];
    if (candidate[snakeField] !== undefined) return candidate[snakeField];
    if (candidate[upperSnakeField] !== undefined) return candidate[upperSnakeField];
  }
  return undefined;
}

function getCellText(field: string, record: any) {
  const col = props.config.columns.find((c) => c.field === field);
  const raw = field ? getRecordFieldValue(record, field) : undefined;
  if (col?.formatter) return col.formatter(raw, getOptionSourceRow(record));
  return raw ?? '-';
}

function getInlineColumnWidth(col: { minWidth?: number; width?: number }) {
  return `${col.width ?? col.minWidth ?? 120}px`;
}

function getInlineRowKey(row: any, index: number) {
  return (
    getRecordFieldValue(row, 'id') ??
    getRecordFieldValue(row, props.config.columns[0]?.field || '') ??
    index
  );
}

function pickRow(row: any) {
  const option = props.config.buildOption(getOptionSourceRow(row));
  emit('update:modelValue', option.code);
  emit('pick', option);
  panel.closePanel();
}

function handleInput(value: string) {
  if (props.disabled) return;
  emit('update:modelValue', value);
  if (value) {
    if (!panel.open.value) panel.openPanel();
    else panel.updatePosition();
    searcher.reset({ keyword: value });
    keyboard.resetIndex();
  } else {
    panel.closePanel();
    searcher.reset();
  }
}

function handleSearchClick() {
  if (props.disabled) return;
  panel.closePanel();
  emit('search');
}

function handleRowMouseDown(event: MouseEvent, record: any) {
  event.preventDefault();
  pickRow(record);
}
</script>

<template>
  <div :ref="syncWrapRef" class="hc-picker-inline">
    <Input
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      autocomplete="new-password"
      data-form-type="other"
      data-lpignore="true"
      :name="inputName"
      spellcheck="false"
      @input="(e: any) => handleInput(String(e?.target?.value ?? ''))"
      @blur="panel.handleInputBlur"
      @keydown="keyboard.handleKeydown"
      @keydown.enter.prevent
    >
      <template #suffix>
        <IconifyIcon
          class="cursor-pointer text-[16px] text-[var(--ant-color-text-description)]"
          icon="mdi:magnify"
          @mousedown.prevent
          @click="handleSearchClick"
        />
      </template>
    </Input>

    <Teleport to="body">
      <div
        v-show="panel.open.value"
        :ref="syncPanelRef"
        :class="['hc-picker-inline-panel', panel.PANEL_CLASS]"
        :style="panel.panelStyle.value"
        @mousedown="panel.handlePanelMouseDown"
      >
        <div class="hc-picker-inline-panel__table">
          <div class="hc-picker-inline-panel__scroll">
            <table class="hc-picker-inline-grid">
              <thead>
                <tr>
                  <th
                    v-for="col in inlineColumns"
                    :key="col.field"
                    :style="{ width: getInlineColumnWidth(col) }"
                  >
                    {{ col.title }}
                  </th>
                </tr>
              </thead>
              <tbody v-if="!searcher.loading.value && rows.length > 0">
                <tr
                  v-for="(record, index) in rows"
                  :key="getInlineRowKey(record, index)"
                  :class="{ 'is-active-row': index === keyboard.activeIndex.value }"
                  @mouseenter="keyboard.activeIndex.value = index"
                  @dblclick="pickRow(record)"
                  @mousedown="(event) => handleRowMouseDown(event, record)"
                >
                  <td
                    v-for="col in inlineColumns"
                    :key="col.field"
                    :style="{ width: getInlineColumnWidth(col) }"
                  >
                    <span>{{ getCellText(col.field, record) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
            <div
              v-if="searcher.loading.value"
              class="hc-picker-inline-panel__state"
            >
              加载中...
            </div>
            <div
              v-else-if="rows.length === 0"
              class="hc-picker-inline-panel__state"
            >
              暂无数据
            </div>
          </div>
        </div>
        <div class="hc-picker-inline-panel__hint">
          更多结果请点击
          <IconifyIcon icon="mdi:magnify" style="vertical-align: -2px" />
          使用完整搜索
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.hc-picker-inline {
  position: relative;
  width: 100%;
}

.hc-picker-inline-panel {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(0 0 0 / 12%);
}

.hc-picker-inline-panel__table {
  padding: 6px 10px 4px;
}

.hc-picker-inline-panel__scroll {
  max-height: 240px;
  overflow: auto;
  border: 1px solid #f0f0f0;
}

.hc-picker-inline-grid {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  color: #334155;
  font-size: 12px;
}

.hc-picker-inline-grid th {
  height: 34px;
  padding: 6px 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  color: #1f2937;
  font-weight: 600;
  text-align: left;
  white-space: nowrap;
}

.hc-picker-inline-grid td {
  height: 32px;
  padding: 5px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  vertical-align: middle;
}

.hc-picker-inline-grid td span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hc-picker-inline-grid tr.is-active-row td {
  background: var(--picker-active-bg, #e6f4ff) !important;
}

.hc-picker-inline-panel__state {
  display: flex;
  height: 96px;
  align-items: center;
  justify-content: center;
  color: #a8abb2;
  font-size: 13px;
}

.hc-picker-inline-panel__hint {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px 10px 8px;
  color: #8c8c8c;
  font-size: 12px;
  white-space: nowrap;
  border-top: 1px solid #f0f0f0;
}
</style>
