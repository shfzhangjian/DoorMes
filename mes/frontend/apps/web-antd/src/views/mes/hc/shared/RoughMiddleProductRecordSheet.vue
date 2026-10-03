<script lang="ts" setup>
import { computed } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, InputNumber, Pagination } from 'ant-design-vue';

type SheetAttachment = {
  name?: string;
  path?: string;
  size?: number;
  uid?: string;
  uploadTime?: string;
  url?: string;
};

type SheetColumn = {
  key: string;
  title: string;
  width?: number;
};

type SheetField = {
  editable?: boolean;
  field?: string;
  key?: string;
  label: string;
  type?: 'input' | 'number' | 'text';
  value?: null | number | string;
};

type SheetRow = Record<string, any>;

const props = withDefaults(
  defineProps<{
    allowRemoveAttachment?: boolean;
    attachments?: SheetAttachment[];
    columns: SheetColumn[];
    detailTitle: string;
    editable?: boolean;
    headFields: SheetField[];
    loading?: boolean;
    metaItems?: Array<null | string | undefined>;
    page: number;
    pageSize: number;
    rows: SheetRow[];
    signatureFields: SheetField[];
    title: string;
  }>(),
  {
    allowRemoveAttachment: false,
    attachments: () => [],
    editable: false,
    loading: false,
    metaItems: () => [],
  },
);

const emit = defineEmits<{
  'open-attachment': [attachment: SheetAttachment];
  'remove-attachment': [attachment: SheetAttachment];
  'row-keydown': [event: KeyboardEvent, row: SheetRow, field: string];
  'update-head-field': [key: string, value: any];
  'update-row-field': [row: SheetRow, field: string, value: any];
  'update-signature-field': [key: string, value: any];
  'update:page': [page: number];
}>();

const visibleMetaItems = computed(() => (props.metaItems || []).filter(Boolean) as string[]);

const pagedRows = computed(() => {
  const start = (props.page - 1) * props.pageSize;
  return (props.rows || []).slice(start, start + props.pageSize);
});

function fieldKey(field: SheetField) {
  return String(field.key || field.field || '');
}

function fieldValue(field: SheetField) {
  const value = field.value;
  return value === null || value === undefined || value === '' ? '-' : value;
}

function updateHeadField(field: SheetField, value: any) {
  const key = fieldKey(field);
  if (!key) return;
  emit('update-head-field', key, value);
}

function updateSignatureField(field: SheetField, value: any) {
  const key = fieldKey(field);
  if (!key) return;
  emit('update-signature-field', key, value);
}

function updateRowField(row: SheetRow, field: string, value: any) {
  row[field] = value;
  emit('update-row-field', row, field, value);
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}
</script>

<template>
  <div class="rough-middle-sheet" :class="{ 'rough-middle-sheet--loading': loading }">
    <div class="rough-middle-sheet__toolbar">
      <div class="rough-middle-sheet__title">
        <span class="rough-middle-sheet__main">{{ title }}</span>
        <div v-if="visibleMetaItems.length" class="rough-middle-sheet__meta">
          <span v-for="item in visibleMetaItems" :key="item">{{ item }}</span>
        </div>
      </div>
      <div class="rough-middle-sheet__actions">
        <slot name="actions"></slot>
      </div>
    </div>

    <div class="rough-middle-sheet__body">
      <fieldset class="rough-middle-fieldset">
        <legend>表单信息</legend>
        <div class="rough-middle-head-grid">
          <div
            v-for="field in headFields"
            :key="fieldKey(field)"
            class="rough-middle-head-cell"
          >
            <span class="rough-middle-head-cell__label">{{ field.label }}</span>
            <span
              v-if="field.editable && field.type === 'number' && editable"
              class="rough-middle-head-cell__value rough-middle-head-cell__input-wrap"
            >
              <InputNumber
                :controls="false"
                :value="field.value"
                class="rough-middle-head-cell__input"
                @update:value="updateHeadField(field, $event)"
              />
            </span>
            <span
              v-else-if="field.editable && editable"
              class="rough-middle-head-cell__value rough-middle-head-cell__input-wrap"
            >
              <Input
                :value="field.value"
                class="rough-middle-head-cell__input"
                @update:value="updateHeadField(field, $event)"
              />
            </span>
            <span v-else class="rough-middle-head-cell__value">{{ fieldValue(field) }}</span>
          </div>
        </div>
      </fieldset>

      <slot name="extra-sections"></slot>

      <div class="rough-middle-attachment">
        <div class="rough-middle-attachment__toolbar">
          <span class="rough-middle-attachment__title">原始导入附件</span>
        </div>
        <div v-if="attachments.length" class="rough-middle-attachment__list">
          <div
            v-for="attachment in attachments"
            :key="attachment.uid || attachment.url || attachment.path || attachment.name"
            class="rough-middle-attachment__item"
          >
            <button
              class="rough-middle-attachment__link"
              type="button"
              @click="emit('open-attachment', attachment)"
            >
              <IconifyIcon icon="lucide:paperclip" />
              <span>{{ attachment.name || '原始导入文件' }}</span>
            </button>
            <span v-if="attachment.uploadTime" class="rough-middle-attachment__time">
              导入时间：{{ attachment.uploadTime }}
            </span>
            <span v-if="formatAttachmentSize(attachment.size)" class="rough-middle-attachment__size">
              {{ formatAttachmentSize(attachment.size) }}
            </span>
            <Button
              v-if="allowRemoveAttachment"
              danger
              size="small"
              type="link"
              @click="emit('remove-attachment', attachment)"
            >
              <IconifyIcon icon="lucide:trash-2" />
            </Button>
          </div>
        </div>
        <span v-else class="rough-middle-attachment__empty">暂无原始导入附件</span>
      </div>

      <div class="rough-middle-panel">
        <div class="rough-middle-panel__header">
          <span>{{ detailTitle }}</span>
        </div>
        <div class="rough-middle-table-wrap">
          <div class="rough-middle-table-body">
            <table class="rough-middle-grid">
              <thead>
                <tr>
                  <th v-for="column in columns" :key="column.key" :width="column.width">
                    {{ column.title }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in pagedRows" :key="row.key || row.seq">
                  <td v-for="column in columns" :key="column.key">
                    <Input
                      v-if="editable"
                      :data-middle-product-cell="`${row.seq}-${column.key}`"
                      :value="row[column.key]"
                      size="small"
                      @keydown="emit('row-keydown', $event, row, column.key)"
                      @update:value="updateRowField(row, column.key, $event)"
                    />
                    <span v-else>{{ row[column.key] || '-' }}</span>
                  </td>
                </tr>
                <tr v-if="!rows.length">
                  <td :colspan="columns.length" class="rough-middle-empty" align="center">暂无明细</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="rough-middle-pagination">
            <Pagination
              :current="page"
              :page-size="pageSize"
              :show-size-changer="false"
              :total="rows.length"
              show-less-items
              size="small"
              @update:current="emit('update:page', $event)"
            />
          </div>
          <div class="rough-middle-signature-row">
            <div
              v-for="field in signatureFields"
              :key="fieldKey(field)"
              class="rough-middle-signature-cell"
            >
              <span class="rough-middle-signature-cell__label">{{ field.label }}</span>
              <span
                v-if="editable && fieldKey(field) === 'confirmer'"
                class="rough-middle-signature-cell__value rough-middle-signature-cell__input-wrap"
              >
                <Input
                  :value="field.value"
                  class="rough-middle-signature-cell__input"
                  placeholder="请输入确认人"
                  @update:value="updateSignatureField(field, $event)"
                />
              </span>
              <span v-else class="rough-middle-signature-cell__value">{{ fieldValue(field) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.rough-middle-sheet {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 / 28px 28px,
    #f5f7fa;
}

.rough-middle-sheet__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 22px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.rough-middle-sheet__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: baseline;
  min-width: 0;
}

.rough-middle-sheet__main {
  color: #172033;
  font-size: 16px;
  font-weight: 800;
  white-space: nowrap;
}

.rough-middle-sheet__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 12px;
}

.rough-middle-sheet__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.rough-middle-sheet__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.rough-middle-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.rough-middle-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
}

.rough-middle-head-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.rough-middle-head-cell {
  display: grid;
  grid-template-columns: 126px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.rough-middle-head-cell__label,
.rough-middle-head-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.rough-middle-head-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.rough-middle-head-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.rough-middle-head-cell__input-wrap {
  padding: 2px 4px;
}

.rough-middle-head-cell__input {
  width: 100%;
}

.rough-middle-head-cell__input :deep(.ant-input),
.rough-middle-head-cell__input :deep(.ant-input-number-input) {
  height: 26px;
  font-weight: 800;
}

.rough-middle-attachment {
  flex-shrink: 0;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.rough-middle-attachment__toolbar {
  display: flex;
  align-items: center;
  min-height: 24px;
}

.rough-middle-attachment__title {
  color: #1677ff;
  font-weight: 800;
}

.rough-middle-attachment__list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: center;
  margin-top: 6px;
}

.rough-middle-attachment__item {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  min-height: 28px;
  padding: 2px 6px;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.rough-middle-attachment__link {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  color: #1677ff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.rough-middle-attachment__time,
.rough-middle-attachment__size,
.rough-middle-attachment__empty {
  color: #64748b;
  font-size: 12px;
}

.rough-middle-attachment__empty {
  display: inline-flex;
  margin-top: 4px;
}

.rough-middle-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 260px;
  background: linear-gradient(180deg, #f8fafc 0%, #e1e9f2 100%);
  border: 1px solid #8794a4;
}

.rough-middle-panel__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 10px;
  color: #075985;
  font-weight: 800;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-bottom: 1px solid #8794a4;
}

.rough-middle-table-wrap {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.rough-middle-table-body {
  flex: 1 1 auto;
  min-height: 180px;
  overflow: auto;
}

.rough-middle-grid {
  width: 100%;
  min-width: 1370px;
  border-collapse: collapse;
  table-layout: fixed;
}

.rough-middle-grid th,
.rough-middle-grid td {
  min-height: 28px;
  padding: 3px 5px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #c6d0dc;
}

.rough-middle-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #263445;
  font-weight: 800;
  text-align: center;
  background:
    linear-gradient(90deg, rgba(14, 165, 233, 0.08) 0 1px, transparent 1px) 0 0 / 18px 100%,
    linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

.rough-middle-grid tbody tr:hover {
  background: #e6f4ff;
}

.rough-middle-empty {
  color: #94a3b8;
}

.rough-middle-pagination {
  display: flex;
  flex: 0 0 auto;
  justify-content: flex-end;
  min-height: 38px;
  padding: 4px 2px 0;
  background: #f5f7fa;
  border-top: 1px solid #c6d0dc;
}

.rough-middle-signature-row {
  display: grid;
  flex: 0 0 auto;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
  background: #fff;
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.rough-middle-signature-cell {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.rough-middle-signature-cell__label,
.rough-middle-signature-cell__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.rough-middle-signature-cell__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-right: 1px solid #c6d0dc;
}

.rough-middle-signature-cell__value {
  color: #172033;
  font-weight: 700;
  background: #fff;
}

.rough-middle-signature-cell__input-wrap {
  padding: 2px 4px;
}

.rough-middle-signature-cell__input {
  width: 100%;
}

.rough-middle-signature-cell__input :deep(.ant-input) {
  height: 26px;
  font-weight: 800;
}
</style>
