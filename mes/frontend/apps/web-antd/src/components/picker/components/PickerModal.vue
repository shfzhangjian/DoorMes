<script lang="ts" setup>
import { computed, nextTick, reactive, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { Alert, Button, Input, Tree } from 'ant-design-vue';

import type { PickerEntityConfig, PickerOption } from '../types';
import PickerQueryBar from './PickerQueryBar.vue';
import PickerTable from './PickerTable.vue';

const props = withDefaults(
  defineProps<{
    config: PickerEntityConfig;
    open: boolean;
    title?: string;
    initialFilters?: Record<string, any>;
    zIndex?: number;
  }>(),
  {},
);

const emit = defineEmits<{
  close: [];
  pick: [PickerOption];
}>();

const PICKER_MODAL_Z_INDEX = 5200;
const effectiveZIndex = computed(() => props.zIndex ?? PICKER_MODAL_Z_INDEX);

const tableRef = ref<InstanceType<typeof PickerTable>>();
const filters = reactive<Record<string, any>>({});
const treeKeyword = ref('');
const treeData = ref<Record<string, any>[]>([]);
const selectedTreeKeys = ref<(number | string)[]>([]);
const expandedTreeKeys = ref<(number | string)[]>([]);
const treeCollapsed = ref(false);

const treeFilter = computed(() => props.config.treeFilter);
const allNodeKey = computed(() => {
  const config = treeFilter.value;
  if (!config?.allNode) return 0;
  return config.allNode[config.fieldNames.key] ?? 0;
});

const filteredTreeData = computed(() =>
  filterTree(treeData.value, treeKeyword.value.trim()),
);

function resetFilters() {
  Object.keys(filters).forEach((k) => delete filters[k]);
  props.config.queryFields.forEach((field) => {
    if (field.defaultValue !== undefined) {
      filters[field.field] = field.defaultValue;
    }
  });
  treeKeyword.value = '';
  treeCollapsed.value = false;
  if (props.initialFilters) {
    Object.assign(filters, props.initialFilters);
  }
  resetTreeSelection();
}

function handleSearch(newFilters: Record<string, any>) {
  Object.keys(filters).forEach((k) => delete filters[k]);
  Object.assign(filters, newFilters);
  tableRef.value?.reload();
}

function handleReset() {
  resetFilters();
  tableRef.value?.reload();
}

function resetTreeSelection() {
  const config = treeFilter.value;
  if (!config) return;
  selectedTreeKeys.value = [allNodeKey.value];
  filters[config.filterField] = undefined;
}

function filterTree(list: Record<string, any>[], keyword: string): Record<string, any>[] {
  const config = treeFilter.value;
  if (!config || !keyword) {
    return list;
  }
  const childrenField = config.fieldNames.children;
  const codeField = config.codeField;
  const nameField = config.nameField;
  return list
    .map((item) => {
      const children = filterTree(item[childrenField] || [], keyword);
      const matched =
        (codeField && String(item[codeField] || '').includes(keyword)) ||
        (nameField && String(item[nameField] || '').includes(keyword));
      if (matched || children.length > 0) {
        return { ...item, [childrenField]: children };
      }
      return null;
    })
    .filter(Boolean) as Record<string, any>[];
}

async function loadTreeData() {
  const config = treeFilter.value;
  if (!config) return;
  const data = await config.loadTree();
  treeData.value = config.allNode
    ? [{ ...config.allNode, [config.fieldNames.children]: data }]
    : data;
  const rootKeys = data.map((item: Record<string, any>) => item[config.fieldNames.key]);
  expandedTreeKeys.value = [allNodeKey.value, ...rootKeys];
  resetTreeSelection();
}

function handleTreeSelect(keys: (number | string)[]) {
  const config = treeFilter.value;
  if (!config) return;
  const selectedKey = keys[0] ?? allNodeKey.value;
  selectedTreeKeys.value = [selectedKey];
  filters[config.filterField] =
    selectedKey && selectedKey !== allNodeKey.value ? selectedKey : undefined;
  tableRef.value?.reload();
}

function handleTreeExpand(keys: (number | string)[]) {
  expandedTreeKeys.value = keys;
}

function handleTreeReset() {
  treeKeyword.value = '';
  resetTreeSelection();
  tableRef.value?.reload();
}

function handlePick(option: PickerOption) {
  emit('pick', option);
  emit('close');
}

const [Modal, modalApi] = useVbenModal({
  fullscreenButton: true,
  footer: null,
  closable: true,
  closeOnClickModal: false,
  closeOnPressEscape: true,
  class: 'hc-picker-modal',
  zIndex: effectiveZIndex.value,
  onClosed() {
    emit('close');
  },
});

// 读取 Vben modal 内部 fullscreen 状态
const modalState = modalApi.useStore?.();
const isFullscreen = computed(() => !!modalState?.value?.fullscreen);

// fullscreen 切换时重新计算表格布局
watch(isFullscreen, async () => {
  await nextTick();
  tableRef.value?.reload();
});

watch(
  () => props.open,
  async (open) => {
    if (open) {
      modalApi.setState({
        title: props.title ?? props.config.title,
        class: `w-[${props.config.modalWidth ?? 1180}px] hc-picker-modal`,
        zIndex: effectiveZIndex.value,
      });
      modalApi.open();
      resetFilters();
      await loadTreeData();
      await nextTick();
      tableRef.value?.reload();
    } else {
      modalApi.close();
    }
  },
);
</script>

<template>
  <Modal>
    <div class="hc-picker-modal__body" :class="{ 'is-fullscreen': isFullscreen }">
      <PickerQueryBar
        :fields="config.queryFields"
        :model-value="filters"
        @update:model-value="handleSearch"
        @search="() => tableRef?.reload()"
        @reset="handleReset"
      />
      <Alert
        v-if="config.notice"
        class="hc-picker-modal__notice"
        :message="config.notice"
        show-icon
        type="info"
      />
      <div class="hc-picker-modal__content">
        <div
          v-if="treeFilter"
          class="hc-picker-modal__tree-panel"
          :class="{ 'is-collapsed': treeCollapsed }"
        >
          <template v-if="!treeCollapsed">
            <div class="hc-picker-modal__tree-header">
              <div class="text-base font-medium">{{ treeFilter.title }}</div>
              <div class="hc-picker-modal__tree-actions">
                <Button type="link" @click="handleTreeReset">清空</Button>
                <Button type="text" size="small" @click="treeCollapsed = true">
                  收起
                </Button>
              </div>
            </div>
            <Input
              v-model:value="treeKeyword"
              allow-clear
              class="mb-3"
              :placeholder="treeFilter.keywordPlaceholder || '请输入关键字'"
            />
            <div class="hc-picker-modal__tree-body">
              <Tree
                :tree-data="filteredTreeData"
                :selected-keys="selectedTreeKeys"
                :expanded-keys="expandedTreeKeys"
                :auto-expand-parent="false"
                :field-names="treeFilter.fieldNames"
                show-line
                block-node
                @expand="handleTreeExpand"
                @select="handleTreeSelect"
              />
            </div>
          </template>
          <template v-else>
            <Button
              class="hc-picker-modal__tree-collapse-btn"
              type="text"
              size="small"
              @click="treeCollapsed = false"
            >
              展开
            </Button>
            <div
              class="hc-picker-modal__tree-collapsed-title"
              role="button"
              tabindex="0"
              @click="treeCollapsed = false"
              @keydown.enter="treeCollapsed = false"
            >
              {{ treeFilter.title }}
            </div>
          </template>
        </div>
        <div class="hc-picker-modal__table">
          <PickerTable
            ref="tableRef"
            :config="config"
            :filters="filters"
            :table-title="config.tableTitle"
            @pick="handlePick"
          />
        </div>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.hc-picker-modal__body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 260px);
  min-height: 300px;
  overflow: hidden;
}

.hc-picker-modal__body.is-fullscreen {
  height: calc(100vh - 110px);
}

.hc-picker-modal__notice {
  margin-bottom: 12px;
}

.hc-picker-modal__content {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 10px;
  overflow: hidden;
}

.hc-picker-modal__tree-panel {
  display: flex;
  width: 260px;
  min-width: 260px;
  flex-direction: column;
  overflow: hidden;
  padding: 10px;
  border: 1px solid #d7e3f2;
  border-radius: 6px;
  background:
    linear-gradient(180deg, rgb(248 251 255 / 98%) 0%, rgb(255 255 255 / 100%) 44%, rgb(246 249 253 / 96%) 100%),
    repeating-linear-gradient(135deg, rgb(30 64 175 / 4%) 0 1px, transparent 1px 8px);
  box-shadow:
    inset 0 1px 0 rgb(255 255 255 / 90%),
    0 1px 2px rgb(15 23 42 / 4%);
  transition:
    width 0.2s ease,
    min-width 0.2s ease,
    padding 0.2s ease;
}

.hc-picker-modal__tree-panel.is-collapsed {
  width: 44px;
  min-width: 44px;
  align-items: center;
  padding: 8px 4px;
  border-color: #d7e3f2;
  background:
    linear-gradient(180deg, rgb(232 241 252 / 92%) 0%, rgb(248 251 255 / 96%) 100%),
    repeating-linear-gradient(135deg, rgb(30 64 175 / 8%) 0 1px, transparent 1px 7px);
}

.hc-picker-modal__tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px -4px 10px;
  padding: 4px 4px 8px;
  border-bottom: 1px solid rgb(215 227 242 / 80%);
}

.hc-picker-modal__tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hc-picker-modal__tree-collapse-btn {
  padding: 0 4px;
}

.hc-picker-modal__tree-collapsed-title {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  color: var(--ant-color-text-secondary);
  cursor: pointer;
  font-size: 13px;
  letter-spacing: 2px;
  line-height: 1.4;
  writing-mode: vertical-rl;
}

.hc-picker-modal__tree-collapsed-title:hover {
  color: var(--ant-color-primary);
}

.hc-picker-modal__tree-collapsed-title:focus-visible {
  outline: 2px solid var(--ant-color-primary-border);
  outline-offset: 2px;
}

.hc-picker-modal__tree-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.hc-picker-modal__tree-body :deep(.ant-tree) {
  background: transparent;
}

.hc-picker-modal__tree-body :deep(.ant-tree-treenode) {
  width: 100%;
}

.hc-picker-modal__tree-body :deep(.ant-tree-node-content-wrapper) {
  min-width: 0;
  border-radius: 6px;
}

.hc-picker-modal__table {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #d7e3f2;
  border-radius: 6px;
  background: #fff;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.hc-picker-modal__table :deep(.hc-picker-table) {
  height: 100%;
  padding: 10px;
}
</style>

<style>
.hc-picker-modal .ant-modal-body,
.hc-picker-modal [class*='modal__body'] {
  overflow: hidden !important;
  padding: 12px 16px !important;
}
</style>
