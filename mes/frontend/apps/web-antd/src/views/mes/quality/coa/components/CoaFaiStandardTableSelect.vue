<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesQmsCoaApi } from '#/api/mes/quality/coa';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Input, Select, Table, Tag } from 'ant-design-vue';

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    loading?: boolean;
    options: MesQmsCoaApi.FaiStandard[];
    value?: number;
  }>(),
  {
    disabled: false,
    loading: false,
    value: undefined,
  },
);

const emit = defineEmits<{
  select: [MesQmsCoaApi.FaiStandard | undefined];
  'update:value': [number | undefined];
}>();

const dropdownOpen = ref(false);
const keyword = ref('');

const selectedId = computed({
  get: () => props.value,
  set: (value?: number) => {
    emit('update:value', value);
    emit(
      'select',
      props.options.find((item) => item.id === value),
    );
  },
});

const selectOptions = computed(() =>
  props.options.map((item) => ({
    label: [
      item.standardNo,
      item.standardName,
      item.version,
      item.processName || '通用工序',
    ]
      .filter(Boolean)
      .join('｜'),
    value: item.id,
  })),
);

const selectedStandard = computed(() =>
  props.options.find((item) => item.id === selectedId.value),
);

const filteredOptions = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase();
  if (!normalizedKeyword) return props.options;
  return props.options.filter((item) =>
    [
      item.standardNo,
      item.standardName,
      item.version,
      item.processCode,
      item.processName,
      item.productModelCode,
      item.productModelName,
      item.materialCode,
      item.materialName,
    ].some((value) =>
      String(value || '')
        .toLowerCase()
        .includes(normalizedKeyword),
    ),
  );
});

const columns: TableColumnsType = [
  { title: '标准编号', dataIndex: 'standardNo', width: 150, fixed: 'left' },
  { title: '标准名称', dataIndex: 'standardName', width: 210 },
  { title: '版本', dataIndex: 'version', width: 80 },
  { title: '工序编码', dataIndex: 'processCode', width: 120 },
  { title: '工序名称', dataIndex: 'processName', width: 120 },
  { title: '产品型号', dataIndex: 'productModelCode', width: 135 },
  { title: '产品名称', dataIndex: 'productModelName', width: 170 },
  { title: '物料编码', dataIndex: 'materialCode', width: 145 },
  { title: '物料名称', dataIndex: 'materialName', width: 180 },
  { title: '项目数', key: 'itemCount', width: 80, align: 'center' },
  { title: '状态', key: 'status', width: 100, align: 'center', fixed: 'right' },
];

function handleSelect(row: MesQmsCoaApi.FaiStandard) {
  selectedId.value = row.id;
  dropdownOpen.value = false;
}

function handleDropdownVisibleChange(open: boolean) {
  dropdownOpen.value = open;
  if (open) keyword.value = '';
}

function tableRowClassName(row: MesQmsCoaApi.FaiStandard) {
  return row.id === selectedId.value ? 'coa-standard-row--selected' : '';
}

function tableCustomRow(row: MesQmsCoaApi.FaiStandard) {
  return {
    onClick: () => handleSelect(row),
    onDblclick: () => handleSelect(row),
  };
}
</script>

<template>
  <!-- eslint-disable vue/html-closing-bracket-newline vue/multiline-html-element-content-newline -->
  <Select
    v-model:open="dropdownOpen"
    v-model:value="selectedId"
    :disabled="disabled"
    :filter-option="false"
    :loading="loading"
    :options="selectOptions"
    :dropdown-match-select-width="false"
    allow-clear
    class="coa-standard-table-select"
    placeholder="请选择启用且已审核的FAI标准"
    popup-class-name="coa-standard-table-dropdown"
    @dropdown-visible-change="handleDropdownVisibleChange"
  >
    <template #dropdownRender>
      <div class="coa-standard-dropdown-content" @mousedown.stop>
        <div class="coa-standard-dropdown-toolbar">
          <div class="coa-standard-dropdown-title">
            <IconifyIcon icon="lucide:list-filter" />
            <div>
              <strong>选择FAI检验标准</strong>
              <span>单击或双击表格行完成选择，仅显示启用且已审核标准</span>
            </div>
          </div>
          <Input
            v-model:value="keyword"
            allow-clear
            class="coa-standard-dropdown-search"
            placeholder="标准编号 / 名称 / 版本 / 工序 / 产品型号 / 物料"
          >
            <template #prefix><IconifyIcon icon="lucide:search" /></template>
          </Input>
        </div>
        <Table
          :columns="columns"
          :custom-row="tableCustomRow"
          :data-source="filteredOptions"
          :loading="loading"
          :pagination="false"
          :row-class-name="tableRowClassName"
          :scroll="{ x: 1590, y: 350 }"
          bordered
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <span v-if="column.key === 'itemCount'">{{
              record.items?.length || 0
            }}</span>
            <Tag v-else-if="column.key === 'status'" color="success"
              >已审核 / 启用</Tag
            >
            <span
              v-else-if="
                [
                  'processCode',
                  'processName',
                  'productModelCode',
                  'productModelName',
                  'materialCode',
                  'materialName',
                ].includes(String(column.dataIndex))
              "
            >
              {{ record[column.dataIndex] || '通用' }}
            </span>
          </template>
        </Table>
        <div class="coa-standard-dropdown-footer">
          <span>当前显示 {{ filteredOptions.length }} 条</span>
          <strong v-if="selectedStandard">
            已选：{{ selectedStandard.standardNo }}｜{{
              selectedStandard.standardName
            }}｜{{ selectedStandard.version }}
          </strong>
        </div>
      </div>
    </template>
  </Select>
</template>

<style scoped>
.coa-standard-table-select {
  width: 500px;
}
</style>

<style>
.coa-standard-table-dropdown {
  width: min(1380px, calc(100vw - 48px)) !important;
  padding: 0 !important;
  overflow: hidden;
  border: 1px solid #8794a4;
  border-radius: 2px;
  box-shadow: 0 12px 32px rgb(15 23 42 / 24%);
}

.coa-standard-dropdown-content {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr) max-content;
  gap: 8px;
  padding: 10px;
  background: #eef3f8;
}

.coa-standard-dropdown-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.coa-standard-dropdown-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #075985;
}

.coa-standard-dropdown-title > .iconify {
  font-size: 22px;
}

.coa-standard-dropdown-title div {
  display: flex;
  flex-direction: column;
}

.coa-standard-dropdown-title strong {
  color: #172033;
  font-size: 14px;
}

.coa-standard-dropdown-title span,
.coa-standard-dropdown-footer {
  color: #64748b;
  font-size: 12px;
}

.coa-standard-dropdown-search {
  width: min(520px, 42vw);
}

.coa-standard-dropdown-content .ant-table-thead > tr > th {
  color: #263445;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
  border-color: #a2adba;
}

.coa-standard-dropdown-content .ant-table-tbody > tr > td {
  color: #172033;
  cursor: pointer;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border-color: #d7dee8;
}

.coa-standard-dropdown-content .ant-table-tbody > tr:hover > td,
.coa-standard-dropdown-content
  .ant-table-tbody
  > tr.coa-standard-row--selected
  > td {
  font-weight: 700;
  background: linear-gradient(180deg, #dbeafe 0%, #bfdbfe 100%) !important;
}

.coa-standard-dropdown-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 24px;
  padding: 0 4px;
}

.coa-standard-dropdown-footer strong {
  color: #075985;
}
</style>
