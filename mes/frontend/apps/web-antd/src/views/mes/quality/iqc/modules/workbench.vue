<script lang="ts" setup>
import type { MesIqcApi } from '#/api/mes/quality/iqc';

import { computed, defineComponent, h, onMounted, ref, watch } from 'vue';

import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Alert,
  Button,
  Empty,
  Input,
  message,
  Modal,
  RadioGroup,
  Select,
  Tag,
} from 'ant-design-vue';

import { resolveEntryRuleExpectedSampleCount } from '#/api/mes/quality/entry-rule';
import {
  buildIqcCoaWordFileName,
  createIqcRecord,
  exportIqcCoaWord,
  getIqcDetail,
  getIqcStandardCandidates,
  getPendingReceipts,
  recalculateIqcProgramEntry,
  resolveIqcRetentionRule,
  saveIqcProgramEntry,
  selectIqcStandard,
  submitIqcProgramEntry,
} from '#/api/mes/quality/iqc';

import QmsIqcItemImportModal from './qms-iqc-item-import-modal.vue';
import QmsIqcItemValueInputModal from './qms-iqc-item-value-input-modal.vue';
import QmsIqcTemplatePreviewModal from './qms-iqc-template-preview-modal.vue';

const props = defineProps<{
  initialRecordId?: number;
}>();

const emit = defineEmits<{
  'back-to-ledger': [];
  backToLedger: [];
}>();

const activeRecord = ref<MesIqcApi.IqcRecord | null>(null);
const activeItems = ref<MesIqcApi.IqcItem[]>([]);
const expandedItemKeys = ref<string[]>([]);
const importModalOpen = ref(false);
const inputModalOpen = ref(false);
const loading = ref(false);
const modalItem = ref<MesIqcApi.IqcItem>();
const pendingList = ref<MesIqcApi.IqcRecord[]>([]);
const saving = ref(false);
const searchKeyword = ref('');
const selectedItemId = ref<number | string>();
const showTaskSidebar = false;
const standardBinding = ref(false);
const standardBindReason = ref('');
const standardCandidates = ref<MesIqcApi.StandardCandidate[]>([]);
const standardLoading = ref(false);
const standardSelectorOpen = ref(false);
const selectedStandardId = ref<number>();
const templatePreviewOpen = ref(false);

const RetentionConfirmContent = defineComponent({
  name: 'RetentionConfirmContent',
  props: {
    modelValue: {
      required: true,
      type: String,
    },
    rule: {
      required: true,
      type: Object,
    },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const selected = ref(props.modelValue as MesIqcApi.RetentionStatus);
    watch(selected, (value) => emit('update:modelValue', value));
    return () => {
      const rule = props.rule as ReturnType<typeof resolveIqcRetentionRule>;
      return h('div', { class: 'space-y-3' }, [
        h('div', { class: 'text-slate-600' }, [
          h('div', `物料种类：${rule.materialCategoryName}`),
          h('div', `留样数量：${rule.ruleDesc}`),
        ]),
        h(RadioGroup, {
          buttonStyle: 'solid',
          options: [
            { label: '需要留样', value: 'RETAINED' },
            { label: '不留样', value: 'NOT_RETAINED' },
          ],
          optionType: 'button',
          value: selected.value,
          onChange: (event: any) => {
            selected.value = event?.target?.value;
          },
        }),
      ]);
    };
  },
});

const standardOptions = computed(() =>
  standardCandidates.value.map((standard) => ({
    label: [
      standard.recommended ? '推荐' : '',
      standard.standardNo || `标准#${standard.id}`,
      standard.standardName,
      standard.version ? `V${standard.version}` : '',
      standardMatchModeLabel(standard.matchType),
      standard.materialName || standard.materialCode,
      `${standard.itemCount ?? 0}项`,
    ]
      .filter(Boolean)
      .join(' / '),
    value: standard.id,
  })),
);

const standardPanelVisible = computed(
  () =>
    !!activeRecord.value &&
    (standardSelectorOpen.value ||
      !activeRecord.value.standardId ||
      activeRecord.value.standardContentChanged === true),
);

const filteredTaskList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return pendingList.value;
  return pendingList.value.filter((item) =>
    [
      item.iqcNo,
      item.receiptNo,
      item.batchNo,
      item.arrivalDate,
      item.productionDate,
      item.expiryDate,
      item.materialCode,
      item.materialName,
      item.supplierName,
    ]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword)),
  );
});

const modalItemIndex = computed(() => {
  if (!modalItem.value) return 0;
  return (
    activeItems.value.findIndex(
      (item) => resolveItemKey(item) === resolveItemKey(modalItem.value!),
    ) + 1
  );
});

onMounted(async () => {
  await refreshTaskList();
  if (props.initialRecordId) {
    await loadRecordById(props.initialRecordId);
  }
});

watch(
  () => props.initialRecordId,
  async (id) => {
    if (id) await loadRecordById(id);
  },
);

function emitBackToLedger() {
  emit('backToLedger');
  // eslint-disable-next-line vue/custom-event-name-casing
  emit('back-to-ledger');
}

async function refreshTaskList() {
  loading.value = true;
  try {
    pendingList.value = await getPendingReceipts();
  } finally {
    loading.value = false;
  }
}

async function handleRefresh() {
  await refreshTaskList();
  if (activeRecord.value?.id) {
    await loadRecordById(activeRecord.value.id);
  }
}

async function handleSelectTask(record: MesIqcApi.IqcRecord) {
  if (activeRecord.value?.id && activeRecord.value.id !== record.id) {
    Modal.confirm({
      title: '切换进料检验单',
      content: '切换前请确认当前录入内容已保存。是否继续切换？',
      onOk: () => loadTaskRecord(record),
    });
    return;
  }
  await loadTaskRecord(record);
}

async function loadTaskRecord(record: MesIqcApi.IqcRecord) {
  if (record.id) {
    await loadRecordById(record.id);
    return;
  }
  const created = await createIqcRecord(record);
  loadRecord(created);
  await refreshTaskList();
}

async function loadRecordById(id: number) {
  loading.value = true;
  try {
    loadRecord(await getIqcDetail(id));
  } finally {
    loading.value = false;
  }
}

function loadRecord(record: MesIqcApi.IqcRecord) {
  const items = (record.items || []).map((item) => normalizeItem(item));
  activeRecord.value = { ...record, items };
  activeItems.value = items;
  selectedItemId.value = items[0] ? resolveItemKey(items[0]) : undefined;
  expandedItemKeys.value = [];
  standardSelectorOpen.value =
    !record.standardId || record.standardContentChanged === true;
  if (record.standardSwitchAllowed && standardSelectorOpen.value) {
    void loadStandardCandidates(record);
  } else {
    standardCandidates.value = [];
    selectedStandardId.value = undefined;
    standardBindReason.value = '';
  }
}

async function loadStandardCandidates(record: MesIqcApi.IqcRecord) {
  if (!record.id) return;
  standardLoading.value = true;
  selectedStandardId.value = record.standardId;
  standardBindReason.value = '';
  try {
    standardCandidates.value = await getIqcStandardCandidates(record.id);
    if (
      selectedStandardId.value &&
      !standardCandidates.value.some(
        (item) => item.id === selectedStandardId.value,
      )
    ) {
      selectedStandardId.value = undefined;
    }
  } catch {
    standardCandidates.value = [];
    message.warning('检验任务已加载，但候选检验标准加载失败，请稍后刷新重试');
  } finally {
    standardLoading.value = false;
  }
}

async function handleOpenStandardSelector() {
  if (!activeRecord.value?.standardSwitchAllowed) return;
  standardSelectorOpen.value = true;
  await loadStandardCandidates(activeRecord.value);
}

async function handleSelectStandard() {
  if (!activeRecord.value?.id || !selectedStandardId.value) {
    message.warning('请选择检验标准');
    return;
  }
  const inspectionId = activeRecord.value.id;
  const standardId = selectedStandardId.value;
  const execute = async () => {
    standardBinding.value = true;
    try {
      const record = await selectIqcStandard({
        id: inspectionId,
        standardId,
        reason: standardBindReason.value.trim() || undefined,
      });
      standardSelectorOpen.value = false;
      loadRecord(record);
      message.success('已按所选标准重新生成本次进料检验明细');
    } finally {
      standardBinding.value = false;
    }
  };
  if (!activeRecord.value.standardId && activeItems.value.length === 0) {
    await execute();
    return;
  }
  Modal.confirm({
    title: '确认重新加载检验标准？',
    content:
      '重新加载会清空当前检验明细、填值、附件、异常及退回过程数据；旧数据将保存到标准切换留痕中。',
    okText: '确认重新加载',
    cancelText: '取消',
    onOk: execute,
  });
}

function normalizeItem(item: MesIqcApi.IqcItem): MesIqcApi.IqcItem {
  return {
    ...item,
    sampleValues: buildEditableValues(item),
  };
}

function resolveItemKey(item: MesIqcApi.IqcItem) {
  return item.id ?? item.standardItemId ?? item.inspectionItem;
}

function isItemExpanded(item: MesIqcApi.IqcItem) {
  return expandedItemKeys.value.includes(String(resolveItemKey(item)));
}

function toggleItemTree(item: MesIqcApi.IqcItem) {
  const key = String(resolveItemKey(item));
  expandedItemKeys.value = isItemExpanded(item)
    ? expandedItemKeys.value.filter((itemKey) => itemKey !== key)
    : [...expandedItemKeys.value, key];
}

function resolveSampleSize(item: MesIqcApi.IqcItem) {
  return resolveEntryRuleExpectedSampleCount(item);
}

function buildEditableValues(item: MesIqcApi.IqcItem) {
  const source = Array.isArray(item.sampleValues) ? item.sampleValues : [];
  const samples = Array.isArray(item.samples) ? item.samples : [];
  return Array.from({ length: resolveSampleSize(item) }).map((_, index) => {
    if (source[index] !== undefined) return source[index];
    const sample = samples.find((row) => row.sampleSeq === index + 1);
    if (item.itemType === 'QUALITATIVE') return sample?.qualitativeValue;
    if (item.itemType === 'DATE') {
      return sample?.dateValue
        ? {
            evaluationDate: sample.evaluationDate,
            sampleResult: sample.sampleResult,
            value: sample.dateValue,
          }
        : undefined;
    }
    return sample?.measuredValue;
  });
}

function countFilled(item: MesIqcApi.IqcItem) {
  return buildEditableValues(item).filter((value) => {
    const sampleValue = resolveSampleValue(value);
    return (
      sampleValue !== undefined && sampleValue !== null && sampleValue !== ''
    );
  }).length;
}

function valueUnit(item: MesIqcApi.IqcItem) {
  return item.unit || '';
}

function formatNumber(value?: number | string) {
  if (value === undefined || value === null || value === '') return '-';
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) return String(value);
  return Number.isInteger(numberValue)
    ? String(numberValue)
    : numberValue.toFixed(3).replace(/0+$/, '').replace(/\.$/, '');
}

function controlText(minValue?: number, maxValue?: number, unit = '') {
  if (minValue !== undefined && maxValue !== undefined) {
    return `${formatNumber(minValue)} - ${formatNumber(maxValue)}${unit}`;
  }
  if (minValue !== undefined) return `>= ${formatNumber(minValue)}${unit}`;
  if (maxValue !== undefined) return `<= ${formatNumber(maxValue)}${unit}`;
  return '-';
}

function itemTypeLabel(type?: string) {
  if (type === 'QUANTITATIVE') return '定量';
  if (type === 'QUALITATIVE') return '定性';
  if (type === 'DATE') return '时间';
  return '-';
}

function standardRangeText(item: MesIqcApi.IqcItem) {
  if (item.itemType === 'DATE') {
    return `距当前日期不超过 ${item.expiryDays ?? '-'} 天`;
  }
  return controlText(item.minValueLimit, item.maxValueLimit, valueUnit(item));
}

type IqcStatusRecord = Pick<
  MesIqcApi.IqcRecord,
  'lastReturnReason' | 'returnCount' | 'status'
>;

function isAuditReturnedRecord(record?: IqcStatusRecord | null) {
  return (
    record?.status === 'INSPECTING' &&
    (!!record.lastReturnReason || Number(record.returnCount || 0) > 0)
  );
}

function statusColor(record?: IqcStatusRecord | null) {
  if (isAuditReturnedRecord(record)) return 'warning';
  const status = record?.status;
  if (status === 'COMPLETED' || status === 'FINISHED') return 'success';
  if (status === 'REJECTED') return 'error';
  if (status === 'WAITING_CONFIRM') return 'processing';
  if (status === 'INSPECTING') return 'processing';
  if (status === 'PENDING') return 'processing';
  if (status === 'SUSPENDED') return 'warning';
  return 'default';
}

function statusLabel(record?: IqcStatusRecord | null) {
  if (isAuditReturnedRecord(record)) return '已驳回待重填';
  const status = record?.status;
  const map: Record<string, string> = {
    CANCELED: '已取消',
    COMPLETED: '已完成',
    FINISHED: '已完成',
    INSPECTING: '检验中',
    PENDING: '待检验',
    REJECTED: '已拒收',
    SUSPENDED: '已挂起',
    WAITING_CONFIRM: '待审核',
  };
  return status ? map[status] || status : '-';
}

function standardMatchModeColor(value?: string) {
  return value === 'UNIVERSAL' ? 'purple' : 'blue';
}

function standardMatchModeLabel(value?: string) {
  if (value === 'UNIVERSAL') return '通用标准';
  return '物料专用';
}

function resultColor(result?: string) {
  if (result === 'OK') return 'success';
  if (result === 'NG') return 'error';
  return 'default';
}

function resultLabel(result?: string) {
  if (result === 'SKIP') return '不判定';
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  return '待判定';
}

function isReadOnly() {
  return [
    'CANCELED',
    'COMPLETED',
    'FINISHED',
    'REJECTED',
    'WAITING_CONFIRM',
  ].includes(activeRecord.value?.status || '');
}

function canMutateItems() {
  return !!activeRecord.value && !isReadOnly();
}

function sampleText(item: MesIqcApi.IqcItem, value: unknown) {
  const sampleValue = resolveSampleValue(value);
  if (sampleValue === undefined || sampleValue === null || sampleValue === '')
    return '-';
  if (item.itemType === 'QUANTITATIVE') {
    return `${formatNumber(sampleValue as number)}${valueUnit(item)}`;
  }
  return String(sampleValue);
}

function attachmentName(url: string, index: number) {
  const cleanUrl = url.split('?')[0] || '';
  return decodeURIComponent(cleanUrl.split('/').pop() || `附件${index + 1}`);
}

function resolveSampleValue(value: unknown) {
  if (value && typeof value === 'object') {
    const row = value as Record<string, unknown>;
    return row.resultValue ?? row.value;
  }
  return value;
}

function buildSampleRows(item: MesIqcApi.IqcItem) {
  return buildEditableValues(item).map((value, index) => ({
    filled:
      resolveSampleValue(value) !== undefined &&
      resolveSampleValue(value) !== null &&
      resolveSampleValue(value) !== '',
    index: index + 1,
    text: sampleText(item, value),
  }));
}

function handleOpenItemModal(item: MesIqcApi.IqcItem) {
  modalItem.value = { ...item, sampleValues: buildEditableValues(item) };
  inputModalOpen.value = true;
}

function mergeItemIntoRows(item: MesIqcApi.IqcItem) {
  const normalized = normalizeItem(item);
  activeItems.value = activeItems.value.map((row) =>
    resolveItemKey(row) === resolveItemKey(normalized)
      ? { ...row, ...normalized }
      : row,
  );
  if (activeRecord.value) {
    activeRecord.value.items = activeItems.value;
  }
}

function findItemByKey(key?: number | string) {
  return activeItems.value.find((item) => resolveItemKey(item) === key);
}

function resolveNextItemKey(item: MesIqcApi.IqcItem) {
  const currentKey = resolveItemKey(item);
  const currentIndex = item
    ? activeItems.value.findIndex((row) => resolveItemKey(row) === currentKey)
    : -1;
  const nextItem =
    currentIndex >= 0 ? activeItems.value[currentIndex + 1] : undefined;
  return nextItem ? resolveItemKey(nextItem) : undefined;
}

async function handleSaveModalItem(
  item: MesIqcApi.IqcItem,
  closeAfterSave = false,
) {
  if (!activeRecord.value) return;
  if (isReadOnly()) {
    message.warning('当前单据已提交审核或已封单，只能查看检验数据');
    return;
  }
  const currentKey = resolveItemKey(item);
  const nextKey = closeAfterSave ? resolveNextItemKey(item) : undefined;
  saving.value = true;
  try {
    mergeItemIntoRows(item);
    const resp = await saveIqcProgramEntry({
      ...activeRecord.value,
      items: activeItems.value,
    });
    loadRecord(resp);
    if (closeAfterSave) {
      const nextItem = findItemByKey(nextKey);
      if (nextItem) {
        modalItem.value = nextItem;
        inputModalOpen.value = true;
        message.success('当前检验项已保存，已自动跳转下一项');
      } else {
        modalItem.value = findItemByKey(currentKey) || item;
        inputModalOpen.value = false;
        message.success('当前检验项已保存，已完成最后一项');
      }
    } else {
      modalItem.value = findItemByKey(currentKey) || item;
      message.success('已保存并重算当前检验项');
    }
  } finally {
    saving.value = false;
  }
}

async function handleRecalculateModalItem(item: MesIqcApi.IqcItem) {
  if (!activeRecord.value) return;
  if (isReadOnly()) {
    message.warning('当前单据已提交审核或已封单，只能查看检验数据');
    return;
  }
  saving.value = true;
  try {
    mergeItemIntoRows(item);
    const resp = await recalculateIqcProgramEntry({
      ...activeRecord.value,
      items: activeItems.value,
    });
    loadRecord(resp);
    modalItem.value =
      activeItems.value.find(
        (row) => resolveItemKey(row) === resolveItemKey(item),
      ) || item;
    message.success('已重算检验项判定');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit() {
  if (!activeRecord.value) return;
  if (isReadOnly()) {
    message.warning('当前单据已提交审核或已封单，不能重复提交');
    return;
  }
  const incomplete = activeItems.value.find(
    (item) => countFilled(item) < resolveSampleSize(item),
  );
  if (incomplete) {
    message.warning(`请先完成检验项：${incomplete.inspectionItem}`);
    return;
  }
  saving.value = true;
  try {
    const recalculated = await recalculateIqcProgramEntry({
      ...activeRecord.value,
      items: activeItems.value,
    });
    loadRecord(recalculated);
  } finally {
    saving.value = false;
  }
  const pending = activeItems.value.find(
    (item) => !['OK', 'NG', 'SKIP'].includes(item.itemResult),
  );
  if (pending) {
    message.warning(
      `${pending.inspectionItem}：${pending.judgmentReason || '尚未完成判定'}`,
    );
    return;
  }
  const judgment = activeRecord.value!.judgment;
  const retentionStatus = await confirmRetentionBeforeSubmit(
    activeRecord.value,
  );
  if (!retentionStatus) return;
  Modal.confirm({
    title:
      judgment === 'NG' ? '提交不合格进料检验单审核' : '提交进料检验单审核',
    content:
      judgment === 'NG'
        ? '当前存在不合格检验项，提交后将通知质检主管审核，审核前单据锁定只读。'
        : '提交后将通知质检主管审核，审核前单据锁定只读。',
    okButtonProps: { danger: judgment === 'NG' },
    okText: '提交审核',
    okType: 'primary',
    onOk: async () => {
      saving.value = true;
      try {
        const resp = await submitIqcProgramEntry({
          ...activeRecord.value!,
          disposalType:
            judgment === 'NG' ? 'NCR' : activeRecord.value?.disposalType,
          items: activeItems.value,
          judgment,
          retentionStatus,
        });
        loadRecord(resp);
        await refreshTaskList();
        message.success(
          judgment === 'NG' ? '已提交不合格判定审核' : '已提交审核',
        );
      } finally {
        saving.value = false;
      }
    },
  });
}

function confirmRetentionBeforeSubmit(record: MesIqcApi.IqcRecord) {
  const rule = resolveIqcRetentionRule(record);
  let retentionStatus: MesIqcApi.RetentionStatus = 'RETAINED';
  return new Promise<MesIqcApi.RetentionStatus | undefined>((resolve) => {
    Modal.confirm({
      title: '留样确认',
      content: h(RetentionConfirmContent, {
        rule,
        modelValue: retentionStatus,
        'onUpdate:modelValue': (value: MesIqcApi.RetentionStatus) => {
          retentionStatus = value;
        },
      }),
      okText: '继续提交',
      onCancel: () => resolve(undefined),
      onOk: () => resolve(retentionStatus),
    });
  });
}

async function handleExportCoa() {
  if (!activeRecord.value?.id) return;
  const data = await exportIqcCoaWord(activeRecord.value.id);
  downloadFileFromBlobPart({
    fileName: buildIqcCoaWordFileName(activeRecord.value),
    source: data,
  });
}

async function handleTemplateImportApply(items: MesIqcApi.IqcItem[]) {
  if (!activeRecord.value) return;
  saving.value = true;
  try {
    const resp = await saveIqcProgramEntry({
      ...activeRecord.value,
      items,
    });
    loadRecord(resp);
    importModalOpen.value = false;
    await refreshTaskList();
    message.success('模板数据已导入');
  } finally {
    saving.value = false;
  }
}

async function handleImportSuccess(record?: MesIqcApi.IqcRecord) {
  if (record) {
    loadRecord(record);
  } else if (activeRecord.value?.id) {
    await loadRecordById(activeRecord.value.id);
  }
  importModalOpen.value = false;
  await refreshTaskList();
}
</script>

<template>
  <div class="flex h-full flex-col overflow-hidden bg-slate-100">
    <header
      class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4"
    >
      <div class="flex min-w-0 items-center gap-3">
        <Button size="small" @click="emitBackToLedger">
          <IconifyIcon icon="lucide:arrow-left" class="mr-1" /> 返回台账
        </Button>
        <div class="min-w-0">
          <div class="text-base font-bold text-slate-800">
            进料检验项明细概览
          </div>
          <div class="text-xs text-slate-500">
            按检验标准快照填写数值、定性结果或日期，系统重算统计值和判定结果
          </div>
        </div>
      </div>
      <div class="flex items-center gap-2">
        <Button
          size="small"
          :disabled="!activeRecord"
          @click="templatePreviewOpen = true"
        >
          <IconifyIcon icon="lucide:eye" class="mr-1" /> 查看
        </Button>
        <Button size="small" :disabled="!activeRecord" @click="handleExportCoa">
          <IconifyIcon icon="lucide:file-down" class="mr-1" /> 导出 COA
        </Button>
        <Button
          size="small"
          :disabled="!canMutateItems()"
          @click="importModalOpen = true"
        >
          <IconifyIcon icon="lucide:file-spreadsheet" class="mr-1" />
          模板导入/导出
        </Button>
        <Button
          v-if="activeRecord?.standardId && activeRecord.standardSwitchAllowed"
          size="small"
          @click="handleOpenStandardSelector"
        >
          <IconifyIcon icon="lucide:replace" class="mr-1" />
          重新选择标准
        </Button>
        <Button size="small" :loading="loading" @click="handleRefresh">
          <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
        </Button>
      </div>
    </header>

    <main class="flex min-h-0 flex-1 gap-3 p-3">
      <aside
        v-if="showTaskSidebar"
        class="flex w-[320px] shrink-0 flex-col overflow-hidden rounded border bg-white"
      >
        <div class="border-b p-3">
          <Input
            v-model:value="searchKeyword"
            allow-clear
            placeholder="搜索IQC单号、收料单、批号、物料"
          />
        </div>
        <div class="min-h-0 flex-1 space-y-2 overflow-y-auto p-3">
          <button
            v-for="item in filteredTaskList"
            :key="item.id || item.iqcNo || item.receiptNo"
            type="button"
            class="w-full rounded border bg-white p-3 text-left transition hover:border-indigo-400 hover:bg-indigo-50"
            :class="
              activeRecord?.id === item.id
                ? 'border-indigo-500 bg-indigo-50'
                : 'border-slate-200'
            "
            @click="handleSelectTask(item)"
          >
            <div class="flex items-center justify-between gap-2">
              <span
                class="truncate font-mono text-sm font-bold text-indigo-700"
              >
                {{ item.iqcNo || item.receiptNo }}
              </span>
              <Tag :color="statusColor(item)" class="!m-0">
                {{ statusLabel(item) }}
              </Tag>
            </div>
            <div class="mt-2 grid grid-cols-2 gap-2 text-xs text-slate-500">
              <span class="truncate">{{ item.receiptNo || '-' }}</span>
              <span class="truncate text-right">{{ item.batchNo || '-' }}</span>
              <span class="truncate">{{ item.materialCode || '-' }}</span>
              <span class="truncate text-right">{{
                item.materialName || '-'
              }}</span>
            </div>
          </button>
        </div>
      </aside>

      <section
        class="flex min-w-0 flex-1 flex-col overflow-hidden rounded border bg-white"
      >
        <div
          v-if="!activeRecord"
          class="flex h-full flex-col items-center justify-center"
        >
          <Empty description="请从台账中选择进料检验单" />
        </div>

        <template v-else>
          <div class="overflow-x-auto border-b px-4 py-3">
            <div class="flex min-w-[1360px] items-center gap-8">
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">IQC单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.iqcNo || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">收料单号</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.receiptNo || '-' }}
                </div>
              </div>
              <div class="min-w-[150px]">
                <div class="text-xs font-bold text-slate-400">物料编码</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.materialCode || '-' }}
                </div>
              </div>
              <div class="min-w-[160px]">
                <div class="text-xs font-bold text-slate-400">物料名称</div>
                <div class="text-sm font-bold text-slate-800">
                  {{ activeRecord.materialName || '-' }}
                </div>
              </div>
              <div class="min-w-[140px]">
                <div class="text-xs font-bold text-slate-400">批号 / 数量</div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.batchNo || '-' }} /
                  {{ formatNumber(activeRecord.receiveQty)
                  }}{{ activeRecord.unit || '' }}
                </div>
              </div>
              <div class="min-w-[260px]">
                <div class="text-xs font-bold text-slate-400">
                  来料 / 生产 / 失效日期
                </div>
                <div class="font-mono text-sm font-bold text-slate-800">
                  {{ activeRecord.arrivalDate || '-' }} /
                  {{ activeRecord.productionDate || '-' }} /
                  {{ activeRecord.expiryDate || '-' }}
                </div>
              </div>
              <div class="min-w-[180px]">
                <div class="text-xs font-bold text-slate-400">检验标准</div>
                <div class="flex items-center gap-1">
                  <span class="font-mono text-sm font-bold text-slate-800">
                    {{
                      activeRecord.standardNo
                        ? `${activeRecord.standardNo} / ${activeRecord.standardVersion || '-'}`
                        : '待选择'
                    }}
                  </span>
                  <Tag
                    v-if="activeRecord.standardNo"
                    :color="
                      standardMatchModeColor(activeRecord.standardMatchMode)
                    "
                    class="!m-0 border-none"
                  >
                    {{ standardMatchModeLabel(activeRecord.standardMatchMode) }}
                  </Tag>
                </div>
              </div>
              <div class="flex min-w-[150px] items-center gap-2">
                <Tag :color="statusColor(activeRecord)" class="!m-0">
                  {{ statusLabel(activeRecord) }}
                </Tag>
                <Tag :color="resultColor(activeRecord.judgment)" class="!m-0">
                  {{ resultLabel(activeRecord.judgment) }}
                </Tag>
              </div>
            </div>
            <div
              v-if="activeRecord.lastReturnReason"
              class="mt-3 min-w-[960px] rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
            >
              <span class="font-bold">驳回原因：</span>
              <span>{{ activeRecord.lastReturnReason }}</span>
            </div>
          </div>

          <div
            v-if="standardPanelVisible"
            class="shrink-0 border-b border-amber-200 bg-amber-50 p-3"
          >
            <Alert
              show-icon
              type="warning"
              :message="
                activeRecord.standardContentChanged
                  ? '检验标准内容已发生变化'
                  : activeRecord.standardId
                    ? '可以重新选择检验标准'
                    : '当前检验任务尚未挂接检验标准'
              "
              :description="
                activeRecord.standardSelectionMessage ||
                '开始录入前请选择启用且已审核的检验标准，系统会按所选标准重新生成本次检验明细。'
              "
            />
            <div
              v-if="activeRecord.standardSwitchAllowed"
              class="mt-3 flex flex-wrap items-start gap-2"
            >
              <Select
                v-model:value="selectedStandardId"
                class="min-w-[420px] flex-1"
                :filter-option="true"
                :loading="standardLoading"
                :options="standardOptions"
                placeholder="请选择本次执行使用的检验标准"
                show-search
              />
              <Input.TextArea
                v-model:value="standardBindReason"
                class="min-w-[320px] flex-1"
                :auto-size="{ minRows: 1, maxRows: 3 }"
                placeholder="可填写重新选择或重新加载原因"
              />
              <Button
                type="primary"
                :disabled="
                  !selectedStandardId || !activeRecord.standardSwitchAllowed
                "
                :loading="standardBinding"
                @click="handleSelectStandard"
              >
                <IconifyIcon icon="lucide:refresh-cw" class="mr-1" />
                {{
                  activeRecord.standardId
                    ? '按所选标准重新加载'
                    : '挂接并生成检验明细'
                }}
              </Button>
              <Button
                v-if="
                  activeRecord.standardId &&
                  !activeRecord.standardContentChanged
                "
                @click="standardSelectorOpen = false"
              >
                取消
              </Button>
            </div>
            <div
              v-if="
                activeRecord.standardSwitchAllowed &&
                !standardLoading &&
                standardOptions.length === 0
              "
              class="mt-2 text-xs text-amber-700"
            >
              当前没有可用的物料专用或通用已审核启用标准，请先维护标准后刷新任务。
            </div>
          </div>

          <div class="min-h-0 flex-1 overflow-auto p-3">
            <div class="overflow-x-auto rounded border">
              <table
                class="w-full min-w-[1320px] table-fixed border-collapse text-sm"
              >
                <thead class="bg-slate-50 text-xs text-slate-500">
                  <tr>
                    <th class="w-10 border-b px-2 py-2 text-center"></th>
                    <th class="w-12 border-b px-2 py-2 text-center">序号</th>
                    <th class="w-32 border-b px-2 py-2 text-left">检验项目</th>
                    <th class="w-20 border-b px-2 py-2 text-center">类型</th>
                    <th class="w-36 border-b px-2 py-2 text-left">标准范围</th>
                    <th class="w-52 border-b px-2 py-2 text-left">标准要求</th>
                    <th class="w-32 border-b px-2 py-2 text-left">检验方法</th>
                    <th class="w-24 border-b px-2 py-2 text-center">填值</th>
                    <th class="w-24 border-b px-2 py-2 text-right">Max</th>
                    <th class="w-24 border-b px-2 py-2 text-right">Min</th>
                    <th class="w-24 border-b px-2 py-2 text-right">Avg</th>
                    <th class="w-24 border-b px-2 py-2 text-center">判定</th>
                    <th class="w-28 border-b px-2 py-2 text-center">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <template
                    v-for="(item, index) in activeItems"
                    :key="resolveItemKey(item)"
                  >
                    <tr
                      class="cursor-pointer border-b hover:bg-indigo-50"
                      :class="
                        selectedItemId === resolveItemKey(item)
                          ? 'bg-indigo-50'
                          : ''
                      "
                      @click="selectedItemId = resolveItemKey(item)"
                      @dblclick="handleOpenItemModal(item)"
                    >
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-6 w-6 items-center justify-center rounded text-slate-500 hover:bg-indigo-100 hover:text-indigo-700"
                          :title="
                            isItemExpanded(item)
                              ? '收起填报内容'
                              : '展开填报内容'
                          "
                          @click.stop="toggleItemTree(item)"
                        >
                          <IconifyIcon
                            :icon="
                              isItemExpanded(item)
                                ? 'lucide:chevron-down'
                                : 'lucide:chevron-right'
                            "
                            class="text-base"
                          />
                        </button>
                      </td>
                      <td class="px-2 py-2 text-center text-slate-500">
                        {{ index + 1 }}
                      </td>
                      <td class="px-2 py-2 font-bold text-slate-800">
                        {{ item.inspectionItem }}
                      </td>
                      <td class="px-2 py-2 text-center text-slate-600">
                        {{ itemTypeLabel(item.itemType) }}
                      </td>
                      <td class="px-2 py-2 font-mono">
                        单次：{{ standardRangeText(item) }}
                        <div
                          v-if="item.itemType === 'QUANTITATIVE'"
                          class="mt-1 text-xs text-slate-500"
                        >
                          平均值内控：{{
                            controlText(
                              item.avgMinLimit,
                              item.avgMaxLimit,
                              valueUnit(item),
                            )
                          }}
                        </div>
                      </td>
                      <td class="truncate px-2 py-2 text-slate-600">
                        {{ item.standardDesc || '-' }}
                      </td>
                      <td class="truncate px-2 py-2 text-slate-600">
                        {{ item.inspectionMethod || item.testTool || '-' }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        {{ countFilled(item) }}/{{ resolveSampleSize(item) }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        {{ formatNumber(item.maxValue) }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        {{ formatNumber(item.minValue) }}
                      </td>
                      <td class="px-2 py-2 text-right font-mono">
                        {{ formatNumber(item.averageValue) }}
                      </td>
                      <td class="px-2 py-2 text-center">
                        <Tag :color="resultColor(item.itemResult)" class="!m-0">
                          {{ resultLabel(item.itemResult) }}
                        </Tag>
                        <div
                          v-if="item.judgmentReason"
                          class="mt-1 whitespace-normal text-xs text-amber-700"
                        >
                          {{ item.judgmentReason }}
                        </div>
                      </td>
                      <td class="px-2 py-2 text-center">
                        <button
                          type="button"
                          class="inline-flex h-7 cursor-pointer items-center justify-center rounded border border-indigo-200 bg-indigo-50 px-3 text-xs font-bold text-indigo-700 hover:bg-indigo-100"
                          @click.stop="handleOpenItemModal(item)"
                          @mousedown.stop.prevent="handleOpenItemModal(item)"
                        >
                          {{ isReadOnly() ? '查看' : '填写' }}
                        </button>
                      </td>
                    </tr>
                    <tr
                      v-if="isItemExpanded(item)"
                      class="border-b bg-slate-50"
                    >
                      <td colspan="13" class="px-4 py-3">
                        <div class="rounded border bg-white p-3 text-xs">
                          <div
                            class="mb-2 flex items-center justify-between gap-2"
                          >
                            <div class="font-bold text-slate-700">
                              {{ item.inspectionItem }} 填报内容
                            </div>
                            <Tag
                              :color="resultColor(item.itemResult)"
                              class="!m-0"
                            >
                              {{ resultLabel(item.itemResult) }}
                            </Tag>
                          </div>
                          <div class="grid gap-1">
                            <div
                              v-for="sample in buildSampleRows(item)"
                              :key="sample.index"
                              class="flex min-w-0 items-center gap-2 rounded bg-slate-50 px-2 py-1"
                            >
                              <span
                                class="w-16 shrink-0 font-mono text-slate-500"
                              >
                                样本{{ sample.index }}
                              </span>
                              <Tag
                                :color="sample.filled ? 'success' : 'default'"
                                class="!m-0 shrink-0"
                              >
                                {{ sample.filled ? '已填' : '未填' }}
                              </Tag>
                              <span
                                class="min-w-0 flex-1 truncate font-mono text-slate-700"
                              >
                                {{ sample.text }}
                              </span>
                            </div>
                          </div>
                          <div
                            v-if="item.attachmentEnabled"
                            class="mt-3 border-t pt-2"
                          >
                            <span class="mr-2 font-bold text-slate-600">
                              附件：
                            </span>
                            <template v-if="item.attachmentUrls?.length">
                              <a
                                v-for="(
                                  url, attachmentIndex
                                ) in item.attachmentUrls"
                                :key="url"
                                :href="url"
                                class="mr-3 text-indigo-600 hover:underline"
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                {{ attachmentName(url, attachmentIndex) }}
                              </a>
                            </template>
                            <span v-else class="text-slate-400">暂无</span>
                          </div>
                        </div>
                      </td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>

          <footer
            class="flex shrink-0 items-center justify-between border-t bg-white px-4 py-3"
          >
            <div class="text-xs text-slate-500">
              已完成
              {{
                activeItems.filter((item) =>
                  ['OK', 'NG', 'SKIP'].includes(item.itemResult),
                ).length
              }}
              / {{ activeItems.length }} 项
            </div>
            <div class="flex items-center gap-2">
              <Tag v-if="isReadOnly()" color="default" class="!m-0">
                只读查看
              </Tag>
              <Button
                v-else
                type="primary"
                :loading="saving"
                @click="handleSubmit"
              >
                <IconifyIcon icon="lucide:send" class="mr-1" /> 提交审核
              </Button>
            </div>
          </footer>
        </template>
      </section>
    </main>

    <QmsIqcItemValueInputModal
      v-model:open="inputModalOpen"
      :item="modalItem"
      :item-count="activeItems.length"
      :item-index="modalItemIndex"
      :readonly="isReadOnly()"
      :record="activeRecord"
      :saving="saving"
      @recalculate="handleRecalculateModalItem"
      @save="handleSaveModalItem"
    />
    <QmsIqcItemImportModal
      v-model:open="importModalOpen"
      :items="activeItems"
      :iqc-id="activeRecord?.id"
      :readonly="isReadOnly()"
      :record="activeRecord"
      :saving="saving"
      @apply="handleTemplateImportApply"
      @success="handleImportSuccess"
    />
    <QmsIqcTemplatePreviewModal
      v-model:open="templatePreviewOpen"
      :items="activeItems"
      :record="activeRecord"
    />
  </div>
</template>
