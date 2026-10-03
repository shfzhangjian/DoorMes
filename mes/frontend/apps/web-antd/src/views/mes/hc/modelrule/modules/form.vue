<script lang="ts" setup>
import type { MesHcModelRuleApi } from '#/api/mes/hc/modelrule';

import { computed, nextTick, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal as AntModal,
  Select,
  Switch,
  Tooltip,
} from 'ant-design-vue';

import { useVbenForm } from '#/adapter/form';
import { VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  createModelRule,
  generateModelRuleCode,
  getModelRuleDetail,
  updateModelRule,
} from '#/api/mes/hc/modelrule';

import { useFormSchema } from '../data';

type ItemRow = MesHcModelRuleApi.ModelRuleItem & { _rowKey: string };
type DictRow = MesHcModelRuleApi.ModelRuleDict & { _rowKey: string; _itemRowKey?: string };

const emit = defineEmits(['success']);

const formData = ref<MesHcModelRuleApi.ModelRule>();
const activeKey = ref('base');
const rowSeed = ref(1);
const openToken = ref(0);
const activePreviewKey = ref('');
const modelRuleItems = ref<ItemRow[]>([]);
const modelRuleDicts = ref<DictRow[]>([]);
const itemTableRef = ref<any>();
const dictTableRef = ref<any>();
const invalidItemRowKeys = ref<string[]>([]);
const invalidDictRowKeys = ref<string[]>([]);
const currentItemRowKey = ref('');
const currentDictRowKey = ref('');
const pendingItemScrollRowKey = ref('');
const pendingDictScrollRowKey = ref('');
const dictModalVisible = ref(false);
const dictModalItemRowKey = ref('');
const dictModalItemCode = ref('');
const dictModalItemName = ref('');
const dictModalRows = ref<DictRow[]>([]);
const testModalVisible = ref(false);
const testFormValues = ref<Record<string, string>>({});
const generatedRuleText = ref('');
const baseExpanded = ref(true);
const previewExpanded = ref(true);
const itemsExpanded = ref(true);

const parseTypeOptions = [
  { label: '固定值', value: 'fixed' },
  { label: '跳过', value: 'skip' },
  { label: '剩余截取', value: 'remainder' },
  { label: '按长度截取', value: 'segment' },
];

const dataSourceTypeOptions = [
  { label: '字典', value: 'dict' },
  { label: '人工输入', value: 'manual' },
  { label: '物料属性', value: 'material_attr' },
  { label: '公式', value: 'formula' },
  { label: '系统变量', value: 'system' },
];

const getTitle = computed(() => (formData.value?.id ? '编辑型号编码规则' : '新增型号编码规则'));

const [Form, formApi] = useVbenForm({
  commonConfig: {
    componentProps: { class: 'w-full' },
    formItemClass: 'col-span-1',
    labelWidth: 120,
  },
  wrapperClass: 'grid-cols-2 gap-x-6',
  layout: 'horizontal',
  schema: useFormSchema(),
  showDefaultActions: false,
});

const nextRowKey = (prefix = 'row') => `${prefix}-${Date.now()}-${++rowSeed.value}`;
const setActivePreview = (key?: string) => {
  activePreviewKey.value = key || '';
};

const normalizeItemRow = (row?: Partial<MesHcModelRuleApi.ModelRuleItem>): ItemRow =>
  ({
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey('item'),
    sort: row?.sort || modelRuleItems.value.length + 1,
    requiredFlag: row?.requiredFlag ?? true,
  }) as ItemRow;

const normalizeDictRow = (
  row?: Partial<MesHcModelRuleApi.ModelRuleDict>,
  itemRowKey?: string,
): DictRow =>
  ({
    ...(row || {}),
    _rowKey: (row as any)?._rowKey || nextRowKey('dict'),
    _itemRowKey: (row as any)?._itemRowKey || itemRowKey,
    sort: row?.sort || 1,
    enabled: row?.enabled ?? true,
  }) as DictRow;

const isEmptyItemRow = (row: Partial<MesHcModelRuleApi.ModelRuleItem>) =>
  ![
    row.itemCode,
    row.itemName,
    row.parseType,
    row.segmentLength,
    row.fixedValue,
    row.dataSourceType,
  ].some((value) => value !== undefined && value !== null && String(value).trim() !== '');

const isEmptyDictRow = (row: Partial<MesHcModelRuleApi.ModelRuleDict>) =>
  ![row.dictCode, row.dictValue, row.extAttrJson].some(
    (value) => value !== undefined && value !== null && String(value).trim() !== '',
  );

const previewSegments = computed(() =>
  [...modelRuleItems.value]
    .sort((a, b) => (a.sort || 0) - (b.sort || 0))
    .map((row, index) => ({
      key: row._rowKey,
      itemName: row.itemName || `字段${index + 1}`,
      parseTypeLabel:
        parseTypeOptions.find((item) => item.value === row.parseType)?.label || '未设置',
      length: Number(row.segmentLength || 0),
      safeLength: Number(row.segmentLength || 0) > 0 ? Number(row.segmentLength) : 1,
      sample:
        row.parseType === 'fixed'
          ? row.fixedValue?.trim() || ''
          : row.parseType === 'skip'
            ? '跳过'
            : row.parseType === 'remainder'
              ? '剩余段'
              : row.itemName || `字段${index + 1}`,
      requiredFlag: row.requiredFlag ?? true,
    })),
);

const previewCodeText = computed(() =>
  previewSegments.value.length
    ? previewSegments.value.map((segment) => segment.sample).join('')
    : '暂无可预览的编码结果',
);

const testFieldOptions = computed(() => {
  const result: Record<string, Array<{ label: string; value: string }>> = {};
  modelRuleItems.value.forEach((item) => {
    result[item._rowKey] = modelRuleDicts.value
      .filter((dict) => dict._itemRowKey === item._rowKey)
      .map((dict) => ({
        label: dict.dictValue || dict.dictCode || '',
        value: dict.dictCode || dict.dictValue || '',
      }));
  });
  return result;
});

function focusItemRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(
      `.hc-model-rule-modal [data-item-focus-key="${rowKey}"]`,
    ) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    (holder.querySelector('input') as HTMLElement | null)?.focus?.();
  });
}

function scrollToItemRow(rowKey?: string) {
  if (!rowKey) return;
  activeKey.value = 'items';
  setActivePreview(rowKey);
  nextTick(() => {
    itemTableRef.value?.scrollToRow?.({ _rowKey: rowKey });
    focusItemRow(rowKey);
  });
}

function focusDictRow(rowKey?: string) {
  if (!rowKey) return;
  nextTick(() => {
    const holder = document.querySelector(
      `.ant-modal [data-dict-focus-key="${rowKey}"]`,
    ) as HTMLElement | null;
    if (!holder) return;
    holder.scrollIntoView({ block: 'center', behavior: 'smooth' });
    (holder.querySelector('input') as HTMLElement | null)?.focus?.();
  });
}

function recalculateItemTable() {
  nextTick(() => itemTableRef.value?.recalculate?.(true));
}

function recalculateDictTable() {
  nextTick(() => dictTableRef.value?.recalculate?.(true));
}

function scrollToItemTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = modelRuleItems.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    itemTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function scrollToDictTableRow(rowKey?: string) {
  if (!rowKey) return;
  const doScroll = () => {
    const row = dictModalRows.value.find((item) => item._rowKey === rowKey);
    if (!row) return;
    dictTableRef.value?.scrollToRow?.(row);
  };
  nextTick(() => {
    doScroll();
    setTimeout(doScroll, 60);
  });
}

function buildGeneratedRuleText() {
  const orderedRows = [...modelRuleItems.value].sort((a, b) => (a.sort || 0) - (b.sort || 0));
  return orderedRows
    .map((row) => {
      if (row.parseType === 'skip') return '';
      if (row.parseType === 'fixed') return row.fixedValue || '';
      return testFormValues.value[row._rowKey] || '';
    })
    .join('');
}

function buildGeneratePayload(): MesHcModelRuleApi.GenerateCodeReq {
  const testValues: Record<string, string> = {};
  modelRuleItems.value.forEach((row) => {
    testValues[row.itemCode || row._rowKey] = testFormValues.value[row._rowKey] || '';
  });
  return {
    modelRuleItems: modelRuleItems.value.map(({ _rowKey, ...rest }) => ({ ...rest })),
    modelRuleDicts: modelRuleDicts.value.map(({ _rowKey, _itemRowKey, ...rest }) => ({ ...rest })),
    testValues,
  };
}

function validateItemRows(items: ItemRow[]) {
  const emptyIndex = items.findIndex((item) => isEmptyItemRow(item));
  if (emptyIndex >= 0) {
    invalidItemRowKeys.value = [items[emptyIndex]!._rowKey];
    scrollToItemRow(items[emptyIndex]!._rowKey);
    message.warning(`规则字段第 ${emptyIndex + 1} 行为空，请填写后提交或删除空行`);
    return false;
  }
  const invalidIndex = items.findIndex((item) => !item.itemCode?.trim() || !item.itemName?.trim());
  if (invalidIndex >= 0) {
    invalidItemRowKeys.value = [items[invalidIndex]!._rowKey];
    scrollToItemRow(items[invalidIndex]!._rowKey);
    message.warning(`规则字段第 ${invalidIndex + 1} 行请填写字段编码和字段名称`);
    return false;
  }
  invalidItemRowKeys.value = [];
  return true;
}

function validateDictRows(items: DictRow[]) {
  const emptyIndex = items.findIndex((item) => isEmptyDictRow(item));
  if (emptyIndex >= 0) {
    invalidDictRowKeys.value = [items[emptyIndex]!._rowKey];
    dictModalVisible.value = true;
    focusDictRow(items[emptyIndex]!._rowKey);
    message.warning(`字段字典第 ${emptyIndex + 1} 行为空，请填写后提交或删除空行`);
    return false;
  }
  const invalidIndex = items.findIndex((item) => !item.dictCode?.trim() || !item.dictValue?.trim());
  if (invalidIndex >= 0) {
    invalidDictRowKeys.value = [items[invalidIndex]!._rowKey];
    dictModalVisible.value = true;
    focusDictRow(items[invalidIndex]!._rowKey);
    message.warning(`字段字典第 ${invalidIndex + 1} 行请填写字典编码和字典名称`);
    return false;
  }
  invalidDictRowKeys.value = [];
  return true;
}

async function resetFormState() {
  activeKey.value = 'base';
  formData.value = undefined;
  modelRuleItems.value = [];
  modelRuleDicts.value = [];
  dictModalVisible.value = false;
  dictModalRows.value = [];
  dictModalItemRowKey.value = '';
  dictModalItemCode.value = '';
  dictModalItemName.value = '';
  invalidItemRowKeys.value = [];
  invalidDictRowKeys.value = [];
  activePreviewKey.value = '';
  testModalVisible.value = false;
  testFormValues.value = {};
  generatedRuleText.value = '';
  await formApi.resetForm();
}

function addItemRow() {
  const row = normalizeItemRow();
  modelRuleItems.value.push(row);
  invalidItemRowKeys.value = [];
  activeKey.value = 'items';
  setActivePreview(row._rowKey);
  currentItemRowKey.value = row._rowKey;
  pendingItemScrollRowKey.value = row._rowKey;
  recalculateItemTable();
  scrollToItemTableRow(row._rowKey);
  focusItemRow(row._rowKey);
}

function removeItemRow(index: number) {
  const removed = modelRuleItems.value[index];
  modelRuleItems.value.splice(index, 1);
  modelRuleItems.value.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  if (removed?._rowKey) {
    modelRuleDicts.value = modelRuleDicts.value.filter((item) => item._itemRowKey !== removed._rowKey);
    if (activePreviewKey.value === removed._rowKey) {
      activePreviewKey.value = modelRuleItems.value[0]?._rowKey || '';
    }
  }
  if (dictModalItemRowKey.value === removed?._rowKey) {
    dictModalVisible.value = false;
  }
  invalidItemRowKeys.value = [];
  recalculateItemTable();
}

function moveItemRow(index: number, direction: 'up' | 'down') {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= modelRuleItems.value.length) return;
  const rows = [...modelRuleItems.value];
  const current = rows[index];
  rows[index] = rows[targetIndex]!;
  rows[targetIndex] = current!;
  rows.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  modelRuleItems.value = rows;
  currentItemRowKey.value = rows[targetIndex]?._rowKey || '';
  recalculateItemTable();
}

function copyItemRow(index: number) {
  const source = modelRuleItems.value[index];
  if (!source) return;
  const cloned = normalizeItemRow({ ...source, id: 0 });
  modelRuleItems.value.splice(index + 1, 0, cloned);
  modelRuleItems.value.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  invalidItemRowKeys.value = [];
  currentItemRowKey.value = cloned._rowKey;
  pendingItemScrollRowKey.value = cloned._rowKey;
  scrollToItemRow(cloned._rowKey);
  recalculateItemTable();
  scrollToItemTableRow(cloned._rowKey);
}

function syncItemCode(row: ItemRow) {
  modelRuleDicts.value.forEach((dictRow) => {
    if (dictRow._itemRowKey === row._rowKey) {
      dictRow.itemCode = row.itemCode;
    }
  });
  if (dictModalItemRowKey.value === row._rowKey) {
    dictModalItemCode.value = row.itemCode || '';
    dictModalItemName.value = row.itemName || '';
    dictModalRows.value.forEach((dictRow) => {
      dictRow.itemCode = row.itemCode;
    });
  }
}

function openDictModal(row: ItemRow) {
  if (!row.itemCode?.trim()) {
    message.warning('请先填写规则字段编码，再设置字典');
    return;
  }
  if (!row.itemName?.trim()) {
    message.warning('请先填写规则字段名称，再设置字典');
    return;
  }
  dictModalItemRowKey.value = row._rowKey;
  dictModalItemCode.value = row.itemCode;
  dictModalItemName.value = row.itemName;
  dictModalRows.value = modelRuleDicts.value
    .filter((item) => item._itemRowKey === row._rowKey)
    .map((item) => normalizeDictRow(item, row._rowKey));
  currentDictRowKey.value = '';
  invalidDictRowKeys.value = [];
  dictModalVisible.value = true;
  recalculateDictTable();
}

function addDictRow() {
  const row = normalizeDictRow({ itemCode: dictModalItemCode.value }, dictModalItemRowKey.value);
  dictModalRows.value.push(row);
  dictModalRows.value.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  invalidDictRowKeys.value = [];
  currentDictRowKey.value = row._rowKey;
  pendingDictScrollRowKey.value = row._rowKey;
  recalculateDictTable();
  scrollToDictTableRow(row._rowKey);
  focusDictRow(row._rowKey);
}

function removeDictRow(index: number) {
  dictModalRows.value.splice(index, 1);
  dictModalRows.value.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  invalidDictRowKeys.value = [];
  recalculateDictTable();
}

function moveDictRow(index: number, direction: 'up' | 'down') {
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= dictModalRows.value.length) return;
  const rows = [...dictModalRows.value];
  const current = rows[index];
  rows[index] = rows[targetIndex]!;
  rows[targetIndex] = current!;
  rows.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  dictModalRows.value = rows;
  currentDictRowKey.value = rows[targetIndex]?._rowKey || '';
  recalculateDictTable();
}

function copyDictRow(index: number) {
  const source = dictModalRows.value[index];
  if (!source) return;
  const cloned = normalizeDictRow({ ...source, id: 0 }, dictModalItemRowKey.value);
  dictModalRows.value.splice(index + 1, 0, cloned);
  dictModalRows.value.forEach((item, itemIndex) => {
    item.sort = itemIndex + 1;
  });
  invalidDictRowKeys.value = [];
  currentDictRowKey.value = cloned._rowKey;
  pendingDictScrollRowKey.value = cloned._rowKey;
  recalculateDictTable();
  scrollToDictTableRow(cloned._rowKey);
  focusDictRow(cloned._rowKey);
}

function handleDictModalOk() {
  if (!validateDictRows(dictModalRows.value)) return;
  const itemRowKey = dictModalItemRowKey.value;
  modelRuleDicts.value = modelRuleDicts.value.filter((item) => item._itemRowKey !== itemRowKey);
  modelRuleDicts.value.push(
    ...dictModalRows.value
      .filter((item) => !isEmptyDictRow(item))
      .map((item, index) =>
        normalizeDictRow(
          {
            ...item,
            itemCode: dictModalItemCode.value,
            sort: item.sort || index + 1,
          },
          itemRowKey,
        ),
      ),
  );
  dictModalVisible.value = false;
}

const dictCount = (row: ItemRow) =>
  modelRuleDicts.value.filter((item) => item._itemRowKey === row._rowKey).length;

function openTestModal() {
  const orderedRows = [...modelRuleItems.value].sort((a, b) => (a.sort || 0) - (b.sort || 0));
  if (!orderedRows.length) {
    message.warning('请先维护规则字段后再测试生成');
    return;
  }
  if (!validateItemRows(orderedRows)) return;
  const values: Record<string, string> = {};
  orderedRows.forEach((item) => {
    values[item._rowKey] = item.fixedValue || '';
  });
  testFormValues.value = values;
  generatedRuleText.value = '';
  testModalVisible.value = true;
}

async function handleGenerateRule() {
  generatedRuleText.value = buildGeneratedRuleText();
  try {
    const resp = await generateModelRuleCode(buildGeneratePayload());
    generatedRuleText.value = resp.generatedCode || '';
  } catch {
    message.error('后台生成编码失败');
  }
}

function getRowLabel(row: ItemRow) {
  return row.itemName || row.itemCode || '未命名字段';
}

watch(activeKey, async (value) => {
  if (value !== 'items') return;
  await nextTick();
  recalculateItemTable();
  if (pendingItemScrollRowKey.value) {
    const rowKey = pendingItemScrollRowKey.value;
    pendingItemScrollRowKey.value = '';
    scrollToItemTableRow(rowKey);
    focusItemRow(rowKey);
  }
});

watch(
  previewSegments,
  (segments) => {
    if (!segments.length) {
      activePreviewKey.value = '';
      return;
    }
    if (!activePreviewKey.value || !segments.some((item) => item.key === activePreviewKey.value)) {
      activePreviewKey.value = segments[0]!.key;
    }
  },
  { immediate: true },
);

watch(dictModalVisible, async (value) => {
  if (!value) return;
  await nextTick();
  recalculateDictTable();
  if (pendingDictScrollRowKey.value) {
    const rowKey = pendingDictScrollRowKey.value;
    pendingDictScrollRowKey.value = '';
    scrollToDictTableRow(rowKey);
    focusDictRow(rowKey);
  }
});

const [Modal, modalApi] = useVbenModal({
  closeOnClickModal: false,
  closable: true,
  showCancelButton: false,
  closeOnPressEscape: false,
  fullscreen: true,
  fullscreenButton: false,
  class: 'hc-model-rule-modal',
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) {
      activeKey.value = 'base';
      return;
    }
    modalApi.lock();
    const data = (await formApi.getValues()) as MesHcModelRuleApi.ModelRule;
    try {
      const rawItems = modelRuleItems.value.map((row, index) => normalizeItemRow({ ...row, sort: index + 1 }));
      if (!validateItemRows(rawItems)) return;
      const validItems = rawItems.filter((row) => !isEmptyItemRow(row));
      data.modelRuleItems = validItems.map((row, index) => {
        const { _rowKey, ...rest } = row as any;
        return { ...rest, ruleCode: data.ruleCode, sort: row.sort || index + 1 };
      });

      const rawDicts = modelRuleDicts.value.map((row, index) =>
        normalizeDictRow({ ...row, sort: index + 1 }, row._itemRowKey),
      );
      if (!validateDictRows(rawDicts)) return;
      data.modelRuleDicts = rawDicts
        .filter((row) => !isEmptyDictRow(row))
        .map((row, index) => {
          const matched = validItems.find((item) => item._rowKey === row._itemRowKey);
          const { _rowKey, _itemRowKey, ...rest } = row as any;
          return {
            ...rest,
            itemCode: matched?.itemCode || row.itemCode,
            ruleItemId: row.ruleItemId || matched?.id,
            sort: row.sort || index + 1,
          };
        });

      await (formData.value?.id ? updateModelRule(data) : createModelRule(data));
      await modalApi.close();
      emit('success');
      message.success('保存成功');
    } finally {
      modalApi.unlock();
    }
  },
  async onOpenChange(isOpen: boolean) {
    if (!isOpen) {
      openToken.value += 1;
      return;
    }
    const currentToken = ++openToken.value;
    const data = modalApi.getData<MesHcModelRuleApi.ModelRule>();
    if (!data?.id) {
      await resetFormState();
      return;
    }
    modalApi.lock();
    try {
      const detail = await getModelRuleDetail(data.id);
      if (currentToken !== openToken.value) return;
      formData.value = detail;
      await formApi.setValues(detail);
      if (currentToken !== openToken.value) return;
      modelRuleItems.value = (detail.modelRuleItems || []).map((item) => normalizeItemRow(item));
      modelRuleDicts.value = (detail.modelRuleDicts || []).map((item) => {
        const matchedItem = modelRuleItems.value.find((ruleItem) => ruleItem.itemCode === item.itemCode);
        return normalizeDictRow(item, matchedItem?._rowKey);
      });
      invalidItemRowKeys.value = [];
      invalidDictRowKeys.value = [];
      activeKey.value = 'base';
      if (modelRuleItems.value.length > 0) recalculateItemTable();
    } finally {
      modalApi.unlock();
    }
  },
  async onClosed() {
    await resetFormState();
  },
});

function toggleBaseExpanded() {
  baseExpanded.value = !baseExpanded.value;
}

function togglePreviewExpanded() {
  previewExpanded.value = !previewExpanded.value;
}

function toggleItemsExpanded() {
  itemsExpanded.value = !itemsExpanded.value;
  if (itemsExpanded.value) {
    nextTick(() => recalculateItemTable());
  }
}
</script>

<template>
  <Modal :title="getTitle">
    <div class="hc-master-modal">
      <div class="hc-master-modal__scroll">
        <div class="hc-master-panel hc-master-panel--base">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">基本信息</span>
            <div class="hc-master-panel__header-spacer" />
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="toggleBaseExpanded">
              <IconifyIcon :icon="baseExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
          <div v-show="baseExpanded" class="hc-master-panel__body hc-master-panel__body--base">
            <div class="hc-master-panel__base-box">
              <Form />
            </div>
          </div>
        </div>

        <div class="hc-master-panel hc-master-panel--preview">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">编码预览</span>
            <div class="hc-master-panel__header-spacer" />
            <div class="hc-master-panel__header-actions">
              <Button type="primary" ghost size="small" @click="openTestModal">测试生成</Button>
              <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="togglePreviewExpanded">
                <IconifyIcon :icon="previewExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
              </Button>
            </div>
          </div>
          <div v-show="previewExpanded" class="hc-master-panel__body hc-master-panel__body--preview">
            <div v-if="!previewSegments.length" class="hc-code-preview__empty">暂无规则字段，请先新增规则字段</div>
            <div v-else class="hc-code-preview__segments">
              <div
                v-for="segment in previewSegments"
                :key="segment.key"
                class="hc-code-preview__segment"
                :class="{ 'is-active': activePreviewKey === segment.key }"
                :style="{ flex: `${segment.safeLength} 1 0` }"
                @click="scrollToItemRow(segment.key)"
                @mouseenter="setActivePreview(segment.key)"
              >
                <div class="hc-code-preview__length">{{ segment.length > 0 ? `${segment.length} 位` : '变长' }}</div>
                <div class="hc-code-preview__name">{{ segment.itemName }}</div>
                <div class="hc-code-preview__sample">{{ segment.sample }}</div>
                <div class="hc-code-preview__meta">
                  <span>{{ segment.parseTypeLabel }}</span>
                  <span>{{ segment.requiredFlag ? '必填' : '可选' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="hc-master-panel hc-master-panel--items">
          <div class="hc-master-panel__header">
            <span class="hc-master-panel__title-chip">规则字段</span>
            <div class="hc-master-panel__header-spacer" />
            <div v-show="itemsExpanded" class="hc-master-panel__header-actions">
              <Button type="primary" size="small" @click="addItemRow">新增字段</Button>
            </div>
            <Button type="text" size="small" class="hc-master-panel__toggle-btn" @click="toggleItemsExpanded">
              <IconifyIcon :icon="itemsExpanded ? 'lucide:chevron-up' : 'lucide:chevron-down'" />
            </Button>
          </div>
        <div v-show="itemsExpanded" class="hc-master-panel__body hc-master-panel__body--items">
          <div class="hc-master-modal__table-wrap">
            <VxeTable
              ref="itemTableRef"
              :data="modelRuleItems"
              :row-class-name="({ row }) => invalidItemRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentItemRowKey === row._rowKey ? 'is-current-row' : ''"
              auto-resize
              border
              stripe
                :round="false"
                size="small"
                height="100%"
                show-overflow
              row-id="_rowKey"
              @cell-click="({ row }) => (currentItemRowKey = row._rowKey)"
            >
                <VxeColumn field="sort" title="顺序" width="80" align="center" header-align="center"><template #default="{ row }"><InputNumber v-model:value="row.sort" :min="1" :precision="0" class="w-full" @focus="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="itemCode" title="*字段编码" min-width="140" header-align="center"><template #default="{ row }"><div :data-item-focus-key="row._rowKey"><Input v-model:value="row.itemCode" placeholder="请输入字段编码" @focus="setActivePreview(row._rowKey)" @blur="syncItemCode(row)" /></div></template></VxeColumn>
                <VxeColumn field="itemName" title="*字段名称" min-width="160" header-align="center"><template #default="{ row }"><Input v-model:value="row.itemName" placeholder="请输入字段名称" @focus="setActivePreview(row._rowKey)" @blur="syncItemCode(row)" /></template></VxeColumn>
                <VxeColumn field="parseType" title="解析方式" width="140" header-align="center"><template #default="{ row }"><Select v-model:value="row.parseType" :options="parseTypeOptions" class="w-full" placeholder="请选择解析方式" @focus="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="segmentLength" title="长度" width="100" align="center" header-align="center"><template #default="{ row }"><InputNumber v-model:value="row.segmentLength" :min="0" :precision="0" class="w-full" @focus="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="fixedValue" title="固定值" min-width="140" header-align="center"><template #default="{ row }"><Input v-model:value="row.fixedValue" placeholder="请输入固定值" @focus="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="dataSourceType" title="数据来源" width="140" header-align="center"><template #default="{ row }"><Select v-model:value="row.dataSourceType" :options="dataSourceTypeOptions" class="w-full" allow-clear placeholder="请选择数据来源" @focus="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="requiredFlag" title="必填" width="90" align="center" header-align="center"><template #default="{ row }"><Switch v-model:checked="row.requiredFlag" checked-children="是" un-checked-children="否" @change="setActivePreview(row._rowKey)" /></template></VxeColumn>
                <VxeColumn field="dictConfig" title="字典" width="88" align="center" header-align="center"><template #default="{ row }"><Tooltip title="设置字典"><Button type="link" class="hc-icon-btn" @click="openDictModal(row)" @mouseenter="setActivePreview(row._rowKey)"><IconifyIcon icon="carbon:settings" /><span>{{ dictCount(row) }}</span></Button></Tooltip></template></VxeColumn>
                <VxeColumn title="操作" width="156" fixed="right" align="center" header-align="center"><template #default="{ $rowIndex, row }"><div class="hc-row-actions" @mouseenter="setActivePreview(row._rowKey)"><Tooltip title="复制"><Button type="link" class="hc-icon-btn" @click="copyItemRow($rowIndex)"><IconifyIcon icon="carbon:copy" /></Button></Tooltip><Tooltip title="上移"><Button type="link" class="hc-icon-btn" @click="moveItemRow($rowIndex, 'up')"><IconifyIcon icon="carbon:arrow-up" /></Button></Tooltip><Tooltip title="下移"><Button type="link" class="hc-icon-btn" @click="moveItemRow($rowIndex, 'down')"><IconifyIcon icon="carbon:arrow-down" /></Button></Tooltip><Tooltip title="删除"><Button danger type="link" class="hc-icon-btn" @click="removeItemRow($rowIndex)"><IconifyIcon icon="carbon:trash-can" /></Button></Tooltip></div></template></VxeColumn>
            </VxeTable>
          </div>
        </div>
      </div>
    </div>
    </div>

    <AntModal
      v-model:open="dictModalVisible"
      :mask-closable="false"
      :keyboard="false"
      :width="980"
      title="字段规则字典"
      ok-text="确认"
      @ok="handleDictModalOk"
      @cancel="dictModalVisible = false"
    >
      <div class="dict-modal-panel">
        <div class="dict-modal-panel__header">
          <div class="text-sm text-[var(--ant-color-text-secondary)]">当前字段：{{ dictModalItemCode || '-' }} / {{ dictModalItemName || '-' }}</div>
          <Button type="primary" @click="addDictRow">新增字典</Button>
        </div>
        <div class="dict-modal-panel__body">
          <VxeTable
            ref="dictTableRef"
            :data="dictModalRows"
            :row-class-name="({ row }) => invalidDictRowKeys.includes(row._rowKey) ? 'is-invalid-row' : currentDictRowKey === row._rowKey ? 'is-current-row' : ''"
            auto-resize
            border
            stripe
            :round="false"
            size="small"
            height="100%"
            show-overflow
            row-id="_rowKey"
            @cell-click="({ row }) => (currentDictRowKey = row._rowKey)"
          >
            <VxeColumn field="sort" title="顺序" width="80" align="center" header-align="center"><template #default="{ row }"><InputNumber v-model:value="row.sort" :min="1" :precision="0" class="w-full" /></template></VxeColumn>
            <VxeColumn field="dictCode" title="*字典编码" min-width="160" header-align="center"><template #default="{ row }"><div :data-dict-focus-key="row._rowKey"><Input v-model:value="row.dictCode" placeholder="请输入字典编码" /></div></template></VxeColumn>
            <VxeColumn field="dictValue" title="*字典名称" min-width="180" header-align="center"><template #default="{ row }"><Input v-model:value="row.dictValue" placeholder="请输入字典名称" /></template></VxeColumn>
            <VxeColumn field="extAttrJson" title="扩展属性" min-width="220" header-align="center"><template #default="{ row }"><Input v-model:value="row.extAttrJson" placeholder="请输入扩展属性 JSON" /></template></VxeColumn>
            <VxeColumn field="enabled" title="启用" width="90" align="center" header-align="center"><template #default="{ row }"><Switch v-model:checked="row.enabled" checked-children="是" un-checked-children="否" /></template></VxeColumn>
            <VxeColumn title="操作" width="156" fixed="right" align="center" header-align="center"><template #default="{ $rowIndex }"><div class="hc-row-actions"><Tooltip title="复制"><Button type="link" class="hc-icon-btn" @click="copyDictRow($rowIndex)"><IconifyIcon icon="carbon:copy" /></Button></Tooltip><Tooltip title="上移"><Button type="link" class="hc-icon-btn" @click="moveDictRow($rowIndex, 'up')"><IconifyIcon icon="carbon:arrow-up" /></Button></Tooltip><Tooltip title="下移"><Button type="link" class="hc-icon-btn" @click="moveDictRow($rowIndex, 'down')"><IconifyIcon icon="carbon:arrow-down" /></Button></Tooltip><Tooltip title="删除"><Button danger type="link" class="hc-icon-btn" @click="removeDictRow($rowIndex)"><IconifyIcon icon="carbon:trash-can" /></Button></Tooltip></div></template></VxeColumn>
          </VxeTable>
        </div>
      </div>
    </AntModal>

    <AntModal
      v-model:open="testModalVisible"
      :mask-closable="false"
      :keyboard="false"
      :width="720"
      title="编码测试生成"
      ok-text="生成编码"
      cancel-text="关闭"
      @ok="handleGenerateRule"
      @cancel="testModalVisible = false"
    >
      <div class="test-modal-panel">
        <div class="test-modal-panel__form">
          <div v-for="row in modelRuleItems" :key="row._rowKey" class="test-modal-panel__row">
            <div class="test-modal-panel__label">
              <span>{{ getRowLabel(row) }}</span>
              <span class="test-modal-panel__tip">{{ row.segmentLength || '变长' }} 位</span>
            </div>
            <div class="test-modal-panel__field">
              <Select
                v-if="row.dataSourceType === 'dict'"
                v-model:value="testFormValues[row._rowKey]"
                :options="testFieldOptions[row._rowKey] || []"
                allow-clear
                placeholder="请选择字典值"
              />
              <Input
                v-else
                v-model:value="testFormValues[row._rowKey]"
                :placeholder="`请输入${getRowLabel(row)}`"
              />
            </div>
          </div>
        </div>
        <div class="test-modal-panel__result">
          <div class="test-modal-panel__result-label">生成结果</div>
          <Input :value="generatedRuleText" readonly />
        </div>
      </div>
    </AntModal>
  </Modal>
</template>

<style>
.hc-model-rule-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.hc-master-modal {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.hc-master-modal__scroll {
  height: 100%;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}
.hc-master-panel {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  overflow: visible;
  min-width: 0;
  padding: 0;
}
.hc-master-panel__header,
.dict-modal-panel__header { display: flex; align-items: center; justify-content: space-between; }
.hc-master-panel__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 0;
}
.hc-master-panel__header-actions,
.hc-code-preview__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.hc-master-panel__toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  width: 28px;
  height: 28px;
  padding: 0;
  color: #6b7280;
}
.hc-master-panel__header-spacer {
  flex: 1;
}
.hc-master-panel__title-chip {
  display: inline-flex;
  align-items: center;
  min-width: 76px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 24px;
}
.hc-master-panel__body {
  padding: 2px 12px 12px;
}
.hc-master-panel__body--base {
  overflow: hidden;
}
.hc-master-panel__base-box {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 12px;
  background: #fff;
}
.hc-master-panel__body--items {
  height: 500px;
  min-height: 500px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.hc-master-modal__table-wrap,
.dict-modal-panel__body { min-height: 0; overflow: hidden; }
.hc-master-modal__table-wrap {
  flex: 1;
}
.hc-master-modal__table-wrap :deep(.vxe-table),
.dict-modal-panel__body :deep(.vxe-table) { border-radius: 0; }
.hc-master-modal__table-wrap :deep(.is-invalid-row),
.dict-modal-panel__body :deep(.is-invalid-row) { background-color: #fff1f0; }
.hc-master-modal__table-wrap :deep(.is-current-row),
.dict-modal-panel__body :deep(.is-current-row) { background-color: #e6f4ff; }
.hc-master-modal__table-wrap :deep(.vxe-table--border-wrapper),
.hc-master-modal__table-wrap :deep(.vxe-table--main-wrapper),
.dict-modal-panel__body :deep(.vxe-table--border-wrapper),
.dict-modal-panel__body :deep(.vxe-table--main-wrapper) { height: 100%; }
.hc-row-actions { display: flex; align-items: center; justify-content: center; gap: 2px; flex-wrap: nowrap; white-space: nowrap; }
.hc-icon-btn { display: inline-flex; align-items: center; justify-content: center; gap: 2px; min-width: 24px; padding-inline: 2px; }
.hc-icon-btn :deep(svg) { font-size: 14px; }
.hc-code-preview {
  border: 1px solid var(--ant-color-border-secondary);
  border-radius: 8px;
  background: #fff;
  overflow: visible;
  min-width: 0;
  padding: 0;
}
.hc-code-preview__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px 0;
}
.hc-code-preview__title {
  display: inline-flex;
  align-items: center;
  min-width: 76px;
  height: 24px;
  padding: 0 14px;
  border-radius: 3px;
  background: #8b8b8b;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  line-height: 24px;
}
.hc-code-preview__body {
  min-height: 96px;
  overflow: hidden;
  padding: 2px 12px 12px;
}
.hc-code-preview__empty { display: flex; height: 100%; align-items: center; justify-content: center; color: var(--ant-color-text-secondary); }
.hc-code-preview__segments { display: flex; height: 100%; gap: 8px; overflow-x: auto; }
.hc-code-preview__segment { display: grid; min-width: 120px; grid-template-rows: 16px 20px 1fr 14px; border: 1px solid #d9d9d9; border-radius: 8px; background: #fff; padding: 4px 10px; transition: all 0.2s ease; cursor: pointer; }
.hc-code-preview__segment.is-active { border-color: var(--ant-color-primary); box-shadow: 0 0 0 2px rgb(22 119 255 / 12%); background: #f0f7ff; }
.hc-code-preview__length { font-size: 12px; color: var(--ant-color-primary); font-weight: 600; }
.hc-code-preview__name { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hc-code-preview__sample { display: flex; align-items: center; font-size: 12px; color: var(--ant-color-text); overflow: hidden; text-overflow: ellipsis; }
.hc-code-preview__meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; font-size: 12px; color: var(--ant-color-text-secondary); }
.dict-modal-panel { display: grid; height: 460px; min-height: 460px; grid-template-rows: 40px minmax(0, 1fr); row-gap: 12px; }
.test-modal-panel { display: grid; row-gap: 16px; }
.test-modal-panel__form { display: grid; row-gap: 12px; max-height: 420px; overflow: auto; padding-right: 4px; }
.test-modal-panel__row { display: grid; grid-template-columns: 180px minmax(0, 1fr); gap: 12px; align-items: center; }
.test-modal-panel__label { display: flex; align-items: center; justify-content: space-between; gap: 8px; color: var(--ant-color-text); }
.test-modal-panel__field :deep(.ant-select),
.test-modal-panel__field :deep(.ant-input) { width: 100%; }
.test-modal-panel__tip { color: var(--ant-color-text-secondary); font-size: 12px; }
.test-modal-panel__result { display: grid; row-gap: 8px; }
.test-modal-panel__result-label { font-size: 13px; font-weight: 600; color: var(--ant-color-text); }
</style>
