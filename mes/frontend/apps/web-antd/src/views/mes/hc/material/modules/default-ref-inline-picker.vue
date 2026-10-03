<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { Input, Table } from 'ant-design-vue';

import { formatRecipeType } from '../../recipe/data';

export interface RefOption {
  value: number;
  code?: string;
  label: string;
  status?: number;
  name?: string;
  recipeType?: string;
  modelCode?: string;
  remark?: string;
}

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    placeholder?: string;
    disabled?: boolean;
    fetchOptions: () => Promise<RefOption[]>;
  }>(),
  {
    modelValue: '',
    placeholder: '',
    disabled: false,
  },
);

const emit = defineEmits<{
  'update:modelValue': [string];
  search: [void];
  pick: [RefOption];
}>();

const open = ref(false);
const loading = ref(false);
const wrapRef = ref<HTMLElement>();
const panelRef = ref<HTMLElement>();
const panelStyle = ref<Record<string, string>>({});
const activeIndex = ref(0);
const filterForm = reactive({ keyword: '' });
const sourceList = ref<RefOption[]>([]);
const panelMouseDown = ref(false);

const recipeMode = computed(() =>
  sourceList.value.some(
    (item) =>
      item.name !== undefined ||
      item.recipeType !== undefined ||
      item.modelCode !== undefined ||
      item.remark !== undefined,
  ),
);

const dataSource = computed(() => {
  const keyword = (filterForm.keyword || '').trim().toLowerCase();
  const rows = !keyword
    ? sourceList.value
    : sourceList.value.filter((item) =>
        [item.name, item.code, item.recipeType, item.modelCode, item.remark, item.label]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(keyword)),
      );
  if (activeIndex.value >= rows.length) {
    activeIndex.value = rows.length > 0 ? rows.length - 1 : 0;
  }
  return rows;
});

const columns = computed(() =>
  recipeMode.value
    ? [
        { title: '配方名称', dataIndex: 'name', key: 'name', width: 160 },
        { title: '配方编码', dataIndex: 'code', key: 'code', width: 150 },
        {
          title: '配方类型',
          dataIndex: 'recipeType',
          key: 'recipeType',
          width: 120,
          customRender: ({ text }: any) => formatRecipeType(text),
        },
        { title: '型号编码', dataIndex: 'modelCode', key: 'modelCode', width: 150 },
        { title: '备注说明', dataIndex: 'remark', key: 'remark' },
      ]
    : [
        { title: '编号', dataIndex: 'code', key: 'code', width: 180 },
        { title: '名称', dataIndex: 'label', key: 'label' },
      ],
);

async function loadList() {
  loading.value = true;
  try {
    sourceList.value = await props.fetchOptions();
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
  const width = recipeMode.value ? Math.max(rect.width, 940) : Math.max(rect.width, 520);
  const top =
    spaceBelow >= panelHeight + gap ? rect.bottom + gap : Math.max(8, rect.top - panelHeight - gap);
  panelStyle.value = {
    position: 'fixed',
    top: `${top}px`,
    left: `${rect.left}px`,
    width: `${width}px`,
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
  const panel = document.querySelector('.default-ref-inline-picker__panel');
  return Boolean(wrapRef.value?.contains(node) || panel?.contains(node));
}

function handleInputBlur(event: FocusEvent) {
  window.setTimeout(() => {
    if (panelMouseDown.value) return;
    const nextTarget = event.relatedTarget || document.activeElement;
    if (isInsideCurrentPicker(nextTarget)) return;
    closePanel();
  }, 0);
}

async function ensurePanelData() {
  updatePanelPosition();
  if (sourceList.value.length === 0) await loadList();
  updatePanelPosition();
  window.setTimeout(() => updatePanelPosition(), 0);
}

function handleInput(value: string) {
  if (props.disabled) return;
  emit('update:modelValue', value);
  syncKeyword(value);
  open.value = true;
  ensurePanelData();
}

function handleSearchClick() {
  if (props.disabled) return;
  closePanel();
  emit('search');
}

function handleRowHover(index: number) {
  activeIndex.value = index;
}

function handleRowPick(record: RefOption) {
  emit('pick', record);
  closePanel();
}

function handleCellMouseDown(event: MouseEvent, record: RefOption) {
  event.preventDefault();
  event.stopPropagation();
  handleRowPick(record);
}

function handleDocumentClick(event: MouseEvent) {
  const target = event.target as Node | null;
  if (!target || !wrapRef.value) return;
  const panel = document.querySelector('.default-ref-inline-picker__panel');
  if (wrapRef.value.contains(target) || panel?.contains(target)) return;
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
  <div ref="wrapRef" class="default-ref-inline-picker">
    <Input
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
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
        class="default-ref-inline-picker__panel"
        :style="panelStyle"
        @mousedown="handlePanelMouseDown"
      >
        <div class="default-ref-inline-picker__filters">
          <Input
            v-model:value="filterForm.keyword"
            :placeholder="
              recipeMode
                ? '输入关键字，匹配配方名称、配方编码、配方类型、型号编码、备注说明'
                : '输入关键字，匹配编号和名称'
            "
            @blur="handleInputBlur"
            @keydown="handleKeydown"
          />
        </div>
        <div class="default-ref-inline-picker__table">
          <Table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="false"
            :scroll="{ x: recipeMode ? 940 : 520, y: 220 }"
            row-key="value"
            size="small"
            :row-class-name="(_, index) => (index === activeIndex ? 'is-active-row' : '')"
            :custom-row="
              (record, index) => ({
                onMouseenter: () => handleRowHover(index ?? 0),
                onDblclick: () => handleRowPick(record as RefOption),
              })
            "
          >
            <template #bodyCell="{ column, record }">
              <button
                class="default-ref-inline-picker__cell-btn"
                type="button"
                @mousedown="(event) => handleCellMouseDown(event, record as RefOption)"
              >
                {{
                  String(
                    String(column?.key ?? column?.dataIndex ?? '') === 'recipeType'
                      ? formatRecipeType((record as any).recipeType)
                      : (record as any)[String(column?.key ?? column?.dataIndex ?? '')] ?? '-',
                  )
                }}
              </button>
            </template>
          </Table>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.default-ref-inline-picker {
  position: relative;
  width: 100%;
}

.default-ref-inline-picker__panel {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 6px 18px rgb(0 0 0 / 12%);
}

.default-ref-inline-picker__filters {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.default-ref-inline-picker__table {
  padding: 0 10px 10px;
}

.default-ref-inline-picker__table :deep(.ant-table-wrapper) {
  border: 1px solid #f0f0f0;
}

.default-ref-inline-picker__table :deep(.ant-table-tbody > .is-active-row > td) {
  background: #e6f4ff !important;
}

.default-ref-inline-picker__cell-btn {
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
