<script lang="ts" setup>
import type { MesDefectCodeApi } from '#/api/mes/quality/base/defect-code';

import { computed, ref } from 'vue';

import { IconifyIcon } from '@vben/icons';

import { Button, Input, Modal, Tree, TreeSelect } from 'ant-design-vue';

import { getDefectCodeList } from '#/api/mes/quality/base/defect-code';

interface DefectTreeNode {
  children?: DefectTreeNode[];
  code?: string;
  isLeaf: boolean;
  key: string;
  level?: string;
  name?: string;
  raw: MesDefectCodeApi.DefectCode;
  selectable: boolean;
  title: string;
  value: string;
}

defineOptions({ name: 'DefectCodePicker' });

const props = withDefaults(
  defineProps<{
    disabled?: boolean;
    dropdownWidth?: number;
    listHeight?: number;
    modalTitle?: string;
    modalWidth?: number;
    modalZIndex?: number;
    placeholder?: string;
    popupClassName?: string;
    value?: string;
  }>(),
  {
    disabled: false,
    dropdownWidth: undefined,
    listHeight: 320,
    modalTitle: '选择缺陷代码',
    modalWidth: 760,
    modalZIndex: 5300,
    placeholder: '输入或选择缺陷代码',
    popupClassName: undefined,
    value: undefined,
  },
);

const emit = defineEmits<{
  change: [value?: string];
  select: [
    payload: {
      code?: string;
      level?: string;
      name?: string;
      raw?: MesDefectCodeApi.DefectCode;
    },
  ];
  'update:value': [value?: string];
}>();

const loading = ref(false);
const treeData = ref<DefectTreeNode[]>([]);
const codeMap = new Map<string, DefectTreeNode>();
const pickerVisible = ref(false);
const pickerKeyword = ref('');
const pickerExpandedKeys = ref<string[]>([]);

const selectedKeys = computed(() => (props.value ? [props.value] : []));

const dropdownMatchSelectWidth = computed(() => props.dropdownWidth || true);

const dropdownStyle = computed(() =>
  props.dropdownWidth
    ? {
        minWidth: `${props.dropdownWidth}px`,
        width: `${props.dropdownWidth}px`,
      }
    : undefined,
);

const popupClassName = computed(() =>
  ['defect-code-picker-dropdown', props.popupClassName]
    .filter(Boolean)
    .join(' '),
);

const filteredTreeData = computed(() =>
  filterDefectTree(treeData.value, pickerKeyword.value.trim()),
);

const effectiveExpandedKeys = computed(() =>
  pickerKeyword.value.trim()
    ? collectExpandableDefectKeys(filteredTreeData.value)
    : pickerExpandedKeys.value,
);

async function loadDefectCodes(force = false) {
  if (!force && treeData.value.length > 0) {
    return;
  }
  loading.value = true;
  try {
    const list = await getDefectCodeList();
    codeMap.clear();
    treeData.value = toDefectTree(normalizeDefectTreeSource(list || []));
    pickerExpandedKeys.value = collectExpandableDefectKeys(treeData.value);
  } finally {
    loading.value = false;
  }
}

function normalizeDefectTreeSource(list: MesDefectCodeApi.DefectCode[]) {
  const flatList = flattenDefectNodes(list);
  const nodeMap = new Map<
    number,
    MesDefectCodeApi.DefectCode & { children: MesDefectCodeApi.DefectCode[] }
  >();
  const roots: Array<
    MesDefectCodeApi.DefectCode & { children: MesDefectCodeApi.DefectCode[] }
  > = [];

  flatList.forEach((item) => {
    if (item.id === undefined || item.id === null) {
      return;
    }
    nodeMap.set(item.id, { ...item, children: [] });
  });

  const hasParentLinks = flatList.some(
    (item) => !!item.parentId && nodeMap.has(item.parentId),
  );

  if (!hasParentLinks) {
    return list;
  }

  flatList.forEach((item) => {
    if (item.id === undefined || item.id === null) {
      return;
    }
    const node = nodeMap.get(item.id);
    if (!node) {
      return;
    }
    if (item.parentId && nodeMap.has(item.parentId)) {
      nodeMap.get(item.parentId)?.children.push(node);
    } else {
      roots.push(node);
    }
  });

  sortDefectTree(roots);
  return roots;
}

function flattenDefectNodes(
  list: MesDefectCodeApi.DefectCode[],
): MesDefectCodeApi.DefectCode[] {
  return list.flatMap((item) => [
    { ...item, children: undefined },
    ...flattenDefectNodes(item.children || []),
  ]);
}

function sortDefectTree(list: MesDefectCodeApi.DefectCode[]) {
  list.sort((a, b) => {
    const sortA = a.sort ?? 0;
    const sortB = b.sort ?? 0;
    if (sortA !== sortB) {
      return sortA - sortB;
    }
    return String(a.code || a.name || '').localeCompare(
      String(b.code || b.name || ''),
      'zh-Hans-CN',
    );
  });
  list.forEach((item) => sortDefectTree(item.children || []));
}

function toDefectTree(list: MesDefectCodeApi.DefectCode[]): DefectTreeNode[] {
  return list.map((item) => {
    const children = item.children ? toDefectTree(item.children) : [];
    const isLeaf = children.length === 0;
    const selectable = isLeaf && item.type !== 'CATEGORY';
    const value = item.code || `CATEGORY_${item.id || item.name}`;
    const node: DefectTreeNode = {
      children: children.length > 0 ? children : undefined,
      code: item.code,
      isLeaf,
      key: value,
      level: item.level,
      name: item.name,
      raw: item,
      selectable,
      title: formatDefectNodeTitle(item),
      value,
    };
    if (item.code && selectable) {
      codeMap.set(item.code, node);
    }
    return node;
  });
}

function formatDefectNodeTitle(item: MesDefectCodeApi.DefectCode) {
  if (item.type === 'CATEGORY') {
    return item.name || item.code || '-';
  }
  if (item.name && item.code) {
    return `${item.name}（${item.code}）`;
  }
  return item.name || item.code || '-';
}

function collectExpandableDefectKeys(list: DefectTreeNode[]): string[] {
  return list.flatMap((item) => {
    const children = item.children || [];
    return children.length > 0
      ? [String(item.value), ...collectExpandableDefectKeys(children)]
      : [];
  });
}

function filterDefectTree(list: DefectTreeNode[], keyword: string) {
  if (!keyword) {
    return list;
  }
  const lowerKeyword = keyword.toLowerCase();
  return list
    .map((item) => {
      const children = filterDefectTree(item.children || [], keyword);
      const matched = getDefectNodeKeyword(item).includes(lowerKeyword);
      if (matched || children.length > 0) {
        return {
          ...item,
          children: children.length > 0 ? children : item.children,
        };
      }
      return null;
    })
    .filter(Boolean) as DefectTreeNode[];
}

function getDefectNodeKeyword(node: Partial<DefectTreeNode>) {
  return [node.title, node.code, node.name, node.value]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
}

function filterDefectTreeNode(inputValue: string, treeNode: any) {
  return getDefectNodeKeyword(treeNode).includes(inputValue.toLowerCase());
}

function emitValue(value?: string) {
  emit('update:value', value);
  emit('change', value);
  if (!value) {
    return;
  }
  const node = codeMap.get(value);
  if (node) {
    emit('select', {
      code: node.code,
      level: node.level,
      name: node.name,
      raw: node.raw,
    });
  }
}

function handleTreeSelectChange(value?: string) {
  emitValue(value);
}

async function openPicker() {
  if (props.disabled) {
    return;
  }
  await loadDefectCodes();
  pickerVisible.value = true;
}

function handlePickerSelect(_selectedKeys: string[], info: any) {
  const node = (info?.node?.dataRef || info?.node || {}) as DefectTreeNode;
  if (!node.selectable) {
    const key = String(node.value || node.key || '');
    if (key && !pickerExpandedKeys.value.includes(key)) {
      pickerExpandedKeys.value = [...pickerExpandedKeys.value, key];
    }
    return;
  }
  emitValue(String(node.value || node.key || ''));
  pickerVisible.value = false;
}

function handlePickerExpand(keys: Array<number | string>) {
  pickerExpandedKeys.value = keys.map(String);
}

void loadDefectCodes();
</script>

<template>
  <div class="defect-code-picker">
    <TreeSelect
      :value="value"
      :disabled="disabled"
      :dropdown-match-select-width="dropdownMatchSelectWidth"
      :dropdown-style="dropdownStyle"
      :filter-tree-node="filterDefectTreeNode"
      :list-height="listHeight"
      :loading="loading"
      :placeholder="placeholder"
      :popup-class-name="popupClassName"
      :tree-data="treeData"
      allow-clear
      class="defect-code-picker__select"
      show-search
      tree-default-expand-all
      tree-line
      @change="handleTreeSelectChange"
    />
    <Button
      class="defect-code-picker__button"
      :disabled="disabled"
      title="选择缺陷代码"
      @click="openPicker"
    >
      <IconifyIcon icon="mdi:magnify" />
    </Button>

    <Modal
      v-model:open="pickerVisible"
      :body-style="{ padding: '14px 18px', overflow: 'hidden' }"
      centered
      :footer="null"
      :title="modalTitle"
      :width="modalWidth"
      :z-index="modalZIndex"
    >
      <div class="defect-code-picker-modal">
        <Input.Search
          v-model:value="pickerKeyword"
          allow-clear
          placeholder="输入缺陷代码或名称"
        />
        <div class="defect-code-picker-modal__tree-wrap">
          <Tree
            :auto-expand-parent="!!pickerKeyword.trim()"
            block-node
            :expanded-keys="effectiveExpandedKeys"
            :field-names="{
              title: 'title',
              key: 'value',
              children: 'children',
            }"
            :selected-keys="selectedKeys"
            show-line
            :tree-data="filteredTreeData"
            @expand="handlePickerExpand"
            @select="handlePickerSelect"
          />
        </div>
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.defect-code-picker {
  display: flex;
  width: 100%;
  min-width: 0;
}

.defect-code-picker__select {
  min-width: 0;
  flex: 1;
}

.defect-code-picker__select :deep(.ant-select-selector) {
  border-start-end-radius: 0 !important;
  border-end-end-radius: 0 !important;
}

.defect-code-picker__button.ant-btn {
  display: inline-flex;
  width: 36px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border-start-start-radius: 0;
  border-end-start-radius: 0;
  padding: 0;
  color: #475569;
}

.defect-code-picker-modal {
  display: flex;
  height: min(560px, calc(100vh - 210px));
  min-height: 320px;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.defect-code-picker-modal__tree-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  border: 1px solid #d8e0ec;
  background: #fff;
  padding: 8px;
}

:global(.defect-code-picker-dropdown .ant-select-tree-list-holder-inner) {
  min-width: max-content;
}

:global(.defect-code-picker-dropdown .ant-select-tree-title) {
  white-space: nowrap;
}
</style>
