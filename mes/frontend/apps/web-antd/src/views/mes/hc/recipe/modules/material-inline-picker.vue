<script lang="ts" setup>
import type { MesHcMaterialApi } from '#/api/mes/hc/material';

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { Input, Table } from 'ant-design-vue';

import { getMaterialPage } from '#/api/mes/hc/material';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    mode: 'code' | 'name';
    placeholder?: string;
  }>(),
  {
    modelValue: '',
    placeholder: '',
  },
);

const emit = defineEmits<{
  'update:modelValue': [string];
  search: [void];
  pick: [MesHcMaterialApi.Material];
}>();

const open = ref(false);
const loading = ref(false);
const wrapRef = ref<HTMLElement>();
const panelRef = ref<HTMLElement>();
const panelStyle = ref<Record<string, string>>({});
const activeIndex = ref(0);
const filterForm = reactive({ keyword: '' });
const sourceList = ref<MesHcMaterialApi.Material[]>([]);
const panelMouseDown = ref(false);

const dataSource = computed(() => {
  const keyword = (filterForm.keyword || '').trim().toLowerCase();
  const rows = !keyword
    ? sourceList.value
    : sourceList.value.filter((item) => {
        const values = [
          item.materialCode,
          item.materialName,
          item.specModel,
          item.modelCode,
          item.defaultRecipeCode,
          item.defaultRecipeName,
        ]
          .filter(Boolean)
          .map((value) => String(value).toLowerCase());
        return values.some((value) => value.includes(keyword));
      });
  if (activeIndex.value >= rows.length) {
    activeIndex.value = rows.length > 0 ? rows.length - 1 : 0;
  }
  return rows;
});

const columns = [
  { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 150 },
  { title: '物料名称', dataIndex: 'materialName', key: 'materialName', width: 180 },
  { title: '规格型号描述', dataIndex: 'specModel', key: 'specModel', width: 180 },
  { title: '型号编码', dataIndex: 'modelCode', key: 'modelCode', width: 150 },
  { title: '配方编码', dataIndex: 'defaultRecipeCode', key: 'defaultRecipeCode', width: 150 },
];

async function loadList() {
  loading.value = true;
  try {
    const keyword = (filterForm.keyword || '').trim();
    const res = await getMaterialPage({
      pageNo: 1,
      pageSize: 20,
      materialCode: keyword || undefined,
      materialName: keyword || undefined,
      specModel: keyword || undefined,
      modelCode: keyword || undefined,
      defaultRecipeCode: keyword || undefined,
      defaultRecipeName: keyword || undefined,
    });
    sourceList.value = (res.list || []) as MesHcMaterialApi.Material[];
    activeIndex.value = 0;
  } finally {
    loading.value = false;
  }
}

function syncKeyword(value: string) {
  filterForm.keyword = value || '';
  activeIndex.value = 0;
}

function updatePanelPosition() {
  if (!wrapRef.value) return;
  const rect = wrapRef.value.getBoundingClientRect();
  const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;
  const panelHeight = panelRef.value?.offsetHeight || 320;
  const gap = 4;
  const spaceBelow = viewportHeight - rect.bottom;
  const top =
    spaceBelow >= panelHeight + gap
      ? rect.bottom + gap
      : Math.max(8, rect.top - panelHeight - gap);
  panelStyle.value = {
    position: 'fixed',
    top: `${top}px`,
    left: `${rect.left}px`,
    width: `${Math.max(rect.width, 820)}px`,
    maxHeight: `${Math.max(220, viewportHeight - 16)}px`,
    zIndex: '5000',
  };
}

function closePanel() {
  open.value = false;
}

function handlePanelMouseDown() {
  panelMouseDown.value = true;
  window.setTimeout(() => {
    panelMouseDown.value = false;
  }, 0);
}

function isInsideCurrentPicker(target: EventTarget | null) {
  const node = target as Node | null;
  if (!node) return false;
  const panel = document.querySelector('.material-inline-picker__panel');
  return Boolean(wrapRef.value?.contains(node) || panel?.contains(node));
}

function handleInputBlur(event: FocusEvent) {
  window.setTimeout(() => {
    if (panelMouseDown.value) {
      return;
    }
    const nextTarget = event.relatedTarget || document.activeElement;
    if (isInsideCurrentPicker(nextTarget)) {
      return;
    }
    closePanel();
  }, 0);
}

async function ensurePanelData() {
  updatePanelPosition();
  if (sourceList.value.length === 0) {
    await loadList();
  }
  await Promise.resolve();
  updatePanelPosition();
  window.setTimeout(() => updatePanelPosition(), 0);
}

function handleInput(value: string) {
  emit('update:modelValue', value);
  syncKeyword(value);
  open.value = true;
  ensurePanelData();
}

function handleSearchClick() {
  closePanel();
  emit('search');
}

function handleRowHover(index: number) {
  activeIndex.value = index;
}

function handleRowPick(record: MesHcMaterialApi.Material) {
  emit('pick', record);
  closePanel();
}

function handleCellMouseDown(event: MouseEvent, record: MesHcMaterialApi.Material) {
  event.preventDefault();
  event.stopPropagation();
  handleRowPick(record);
}

function getColumnField(column: Record<string, any>) {
  return String(column?.key ?? column?.dataIndex ?? '');
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node | null;
  if (!target || !wrapRef.value) return;
  const panel = document.querySelector('.material-inline-picker__panel');
  if (wrapRef.value.contains(target) || panel?.contains(target)) {
    return;
  }
  closePanel();
}

function handleKeydown(event: KeyboardEvent) {
  if (!open.value) return;
  const rows = dataSource.value;
  if (!rows.length) return;
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    activeIndex.value = Math.min(activeIndex.value + 1, rows.length - 1);
    return;
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault();
    activeIndex.value = Math.max(activeIndex.value - 1, 0);
    return;
  }
  if (event.key === 'Enter') {
    event.preventDefault();
    handleRowPick(rows[activeIndex.value]!);
    return;
  }
  if (event.key === 'Escape') {
    event.preventDefault();
    closePanel();
  }
}

watch(
  () => props.modelValue,
  (value) => {
    if (!open.value) return;
    syncKeyword(value || '');
    updatePanelPosition();
  },
);

onMounted(() => {
  document.addEventListener('click', handleDocumentClick, true);
  window.addEventListener('resize', updatePanelPosition);
  window.addEventListener('scroll', updatePanelPosition, true);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick, true);
  window.removeEventListener('resize', updatePanelPosition);
  window.removeEventListener('scroll', updatePanelPosition, true);
});
</script>

<template>
  <div ref="wrapRef" class="material-inline-picker">
    <Input
      :value="modelValue"
      :placeholder="placeholder"
      @blur="handleInputBlur"
      @keydown="handleKeydown"
      @press-enter.prevent
      @update:value="(value) => handleInput(String(value || ''))"
    >
      <template #suffix>
        <IconifyIcon
          class="cursor-pointer text-[16px] text-[var(--ant-color-text-description)]"
          icon="mdi:magnify"
          @click="handleSearchClick"
        />
      </template>
    </Input>

    <Teleport to="body">
      <div
        v-if="open"
        ref="panelRef"
        class="material-inline-picker__panel"
        :style="panelStyle"
        @mousedown="handlePanelMouseDown"
      >
        <div class="material-inline-picker__filters">
          <Input
            v-model:value="filterForm.keyword"
            placeholder="输入关键字，匹配编码、名称、规格型号、型号编码和配方"
            @blur="handleInputBlur"
            @keydown="handleKeydown"
          />
        </div>
        <div class="material-inline-picker__table">
          <Table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: 810, y: 220 }"
            row-key="id"
            size="small"
            :row-class-name="(_, index) => (index === activeIndex ? 'is-active-row' : '')"
            :custom-row="(record, index) => ({
              onMouseenter: () => handleRowHover(index ?? 0),
              onClick: () => undefined,
              onDblclick: () => handleRowPick(record as MesHcMaterialApi.Material),
            })"
          >
            <template #bodyCell="{ column, record }">
              <template
                v-if="
                  ['materialCode', 'materialName', 'specModel', 'modelCode', 'defaultRecipeCode'].includes(
                    getColumnField(column),
                  )
                "
              >
                <button
                  class="material-inline-picker__cell-btn"
                  type="button"
                  @mousedown="(event) => handleCellMouseDown(event, record)"
                >
                  {{ (record as any)[getColumnField(column)] || '-' }}
                </button>
              </template>
            </template>
          </Table>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.material-inline-picker {
  position: relative;
  width: 100%;
}

.material-inline-picker__panel {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(0 0 0 / 12%);
}

.material-inline-picker__filters {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.material-inline-picker__table {
  padding: 0 10px 10px;
}

.material-inline-picker__table :deep(.ant-table-wrapper) {
  border: 1px solid #f0f0f0;
}

.material-inline-picker__table :deep(.ant-table-tbody > tr) {
  cursor: pointer;
}

.material-inline-picker__table :deep(.ant-table-tbody > .is-active-row > td) {
  background: #e6f4ff !important;
}

.material-inline-picker__cell-btn {
  display: block;
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
  color: inherit;
  cursor: pointer;
}
</style>
