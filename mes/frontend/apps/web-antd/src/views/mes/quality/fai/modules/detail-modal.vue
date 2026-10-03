<script lang="ts" setup>
import type { MesFaiApi } from '#/api/mes/quality/fai';

import { computed, reactive, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Checkbox,
  Empty,
  Image,
  Input,
  Modal as AntModal,
  Radio,
  Spin,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';

import {
  parseEntryRuleParams,
  resolveEntryRuleRepeatCount,
} from '#/api/mes/quality/entry-rule';
import {
  auditFaiProgramEntry,
  getFaiDetail,
  getFaiRetentionDetail,
  getGlueBoardFaiDetail,
  resolveFaiDisplayBatchNo,
} from '#/api/mes/quality/fai';

import {
  faiStatusOptions,
  formatInspectionQty,
  resolveFaiOperationLabel,
  retentionStatusLabel,
  submissionTypeOptions,
} from '../data';

defineOptions({ name: 'FaiDetailModal' });

interface DataRuleField {
  code: string;
  name: string;
  unit?: string;
}

interface DataRuleConfig {
  inputFields: DataRuleField[];
  resultFields: DataRuleField[];
}

interface EntryGroup {
  items: MesFaiApi.FaiItem[];
  key: string;
  subtitle: string;
  title: string;
}

interface SampleColumn {
  key: string;
  label: string;
  source?: string;
  type: 'field' | 'group' | 'position' | 'remark' | 'result' | 'seq' | 'value';
}

interface SamplePositionGroup {
  audit?: MesFaiApi.FaiGroupAudit;
  duplicateGroups: number[];
  expectedCount: number;
  key: string;
  missingGroups: number[];
  name: string;
  rows: Array<Record<string, any>>;
  unexpected: boolean;
}

interface SummaryCard {
  label: string;
  mono?: boolean;
  value?: unknown;
}

type GroupAuditAction = 'CONFIRM' | 'REJECT_RECHECK';
type ReviewFilter = 'ALL' | 'NG' | 'WAIT_AUDIT' | 'WAIT_RECHECK';

type GroupAuditDraft = {
  auditRemark: string;
  auditResult?: GroupAuditAction;
  groupKey: string;
  itemId: number;
  samplePosition: string;
};

const emit = defineEmits<{
  success: [record: MesFaiApi.FaiRecord];
}>();

const record = ref<MesFaiApi.FaiRecord | null>(null);
const loading = ref(false);
const submitting = ref(false);
const modalData = ref<Record<string, any>>({});
const basicInfoExpanded = ref(true);
const expandedItemKeys = ref<string[]>([]);
const onlyNgItems = ref(false);
const reviewFilter = ref<ReviewFilter>('ALL');
const finalAuditOpen = ref(false);
const auditResult = ref<MesFaiApi.AuditResult>('PASS');
const auditRemark = ref('');
const groupAudits = reactive<Record<string, GroupAuditDraft>>({});

const isGlueBoardMode = computed(
  () => modalData.value?.apiMode === 'GLUE_BOARD',
);
const isRetentionMode = computed(
  () => modalData.value?.apiMode === 'RETENTION',
);
const isAuditMode = computed(() => modalData.value?.mode === 'AUDIT');

const activeItems = computed(() => record.value?.items || []);

const sortedItems = computed(() =>
  activeItems.value.toSorted((a, b) => {
    const leftSort = Number(a.sort ?? 0);
    const rightSort = Number(b.sort ?? 0);
    if (leftSort !== rightSort) return leftSort - rightSort;
    return groupKey(a).localeCompare(groupKey(b));
  }),
);

const groupedItems = computed<EntryGroup[]>(() => {
  const groupMap = new Map<string, EntryGroup>();
  sortedItems.value.forEach((item) => {
    const key = groupKey(item);
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        items: [],
        key,
        subtitle: groupSubtitle(item),
        title: groupTitle(item),
      });
    }
    groupMap.get(key)!.items.push(item);
  });
  return [...groupMap.values()];
});

const visibleGroupedItems = computed<EntryGroup[]>(() => {
  return groupedItems.value
    .map((group) => ({
      ...group,
      items: group.items.filter(
        (item) => visibleSamplePositionGroups(item).length > 0,
      ),
    }))
    .filter((group) => group.items.length > 0);
});

const detailEmptyDescription = computed(() =>
  isAuditMode.value && reviewFilter.value === 'WAIT_RECHECK'
    ? '当前检验单暂无待复检组'
    : isAuditMode.value && reviewFilter.value === 'WAIT_AUDIT'
      ? '当前检验单暂无复检待审组'
      : onlyNgItems.value || (isAuditMode.value && reviewFilter.value === 'NG')
        ? '当前检验单暂无不合格项'
        : '当前检验单暂无录入项',
);

const modalTitle = computed(() => {
  if (isAuditMode.value) return '首件检验审核';
  if (isGlueBoardMode.value) return '胶板检验查看';
  if (isRetentionMode.value) return '首件留样记录查看';
  return '首件检验查看';
});

const recordNo = computed(() => record.value?.faiNo || '-');

const basicInfoCards = computed<SummaryCard[]>(() => {
  const current = record.value;
  if (!current) return [];

  if (isGlueBoardMode.value) {
    return [
      { label: '检验单号', mono: true, value: current.faiNo },
      { label: '工序', value: resolveFaiOperationLabel(current) },
      submissionAmountCard(current),
      { label: '单据状态', value: statusLabel(current.status) },
      { label: '送检时间', value: formatDate(current.submissionTime) },
      { label: '送检人员', value: current.submitterName },
      { label: '检验人', value: current.operatorName },
      {
        label: '检验时间',
        value: formatDate(current.inspectionTime || current.operatorTime),
      },
      { label: '审核人', value: current.qaInspectorName },
      { label: '审核时间', value: formatDate(current.qaTime) },
    ];
  }

  return [
    { label: 'FAI单号', mono: true, value: current.faiNo },
    { label: '工序', value: resolveFaiOperationLabel(current) },
    {
      label: '送检类型',
      value: optionLabel(submissionTypeOptions, current.submissionType),
    },
    { label: '工单号', mono: true, value: current.workOrderNo },
    { label: '物料编码', mono: true, value: current.materialCode },
    { label: '物料名称', value: current.materialName },
    { label: '产品型号', value: current.productModel },
    {
      label: '产品批次',
      mono: true,
      value: resolveFaiDisplayBatchNo(current),
    },
    submissionAmountCard(current),
    { label: '单据状态', value: statusLabel(current.status) },
    { label: '判定结果', value: judgmentLabel(current.judgment) },
    { label: '送检时间', value: formatDate(current.submissionTime) },
    { label: '检验时间', value: formatDate(current.inspectionTime) },
    { label: '送检人员', value: current.submitterName },
    { label: '留样状态', value: retentionStatusLabel(current.retentionStatus) },
    { label: '留样确认人', value: current.retentionConfirmUserName },
    { label: '留样确认时间', value: formatDate(current.retentionConfirmTime) },
    { label: '检验人', value: current.operatorName },
    { label: '审核人', value: current.qaInspectorName },
    { label: '审核时间', value: formatDate(current.qaTime) },
    {
      label: '最近保存',
      value: formatDate(current.lastSaveTime || current.qaTime),
    },
  ];
});

const glueBoardInfoCards = computed<SummaryCard[]>(() => {
  const current = record.value;
  if (!current || !hasMeaningfulValue(current.glueBoardModel)) return [];
  return [
    { label: '胶板型号', value: current.glueBoardModel },
    {
      label: '胶板料号',
      mono: true,
      value:
        current.glueBoardMaterialCode ||
        (isGlueBoardMode.value ? current.materialCode : undefined),
    },
    { label: '胶板批号', mono: true, value: current.gluePlateBatchNo },
  ];
});

const [Modal, modalApi] = useVbenModal({
  class: 'qms-fai-detail-modal',
  closeOnClickModal: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
  title: '检验详情',
  onOpenChange: async (isOpen) => {
    if (!isOpen) {
      record.value = null;
      modalData.value = {};
      basicInfoExpanded.value = true;
      expandedItemKeys.value = [];
      onlyNgItems.value = false;
      reviewFilter.value = 'ALL';
      finalAuditOpen.value = false;
      auditResult.value = 'PASS';
      auditRemark.value = '';
      resetGroupAudits();
      return;
    }

    loading.value = true;
    try {
      basicInfoExpanded.value = true;
      onlyNgItems.value = false;
      reviewFilter.value = 'ALL';
      modalApi.setState({ fullscreen: true });
      modalData.value = modalApi.getData<Record<string, any>>() || {};
      if (isGlueBoardMode.value) {
        record.value = await getGlueBoardFaiDetail(modalData.value.id);
      } else if (isRetentionMode.value) {
        record.value = await getFaiRetentionDetail(modalData.value.id);
      } else {
        record.value = await getFaiDetail(modalData.value.id);
      }
      if (isAuditMode.value) {
        initGroupAudits();
        applyDefaultAuditResult();
      }
      expandAllItems();
    } finally {
      loading.value = false;
    }
  },
});

function closeModal() {
  modalApi.close();
}

function resetGroupAudits() {
  Object.keys(groupAudits).forEach((key) => delete groupAudits[key]);
}

function initGroupAudits() {
  resetGroupAudits();
  auditRemark.value = record.value?.auditRemark || '';
  for (const item of activeItems.value) {
    if (!item.id) continue;
    for (const group of samplePositionGroups(item, false)) {
      const key = groupAuditDraftKey(item, group);
      const currentAudit = group.audit;
      groupAudits[key] = {
        auditRemark: currentAudit?.auditRemark || '',
        auditResult:
          currentAudit?.auditResult === 'CONFIRM' ||
          currentAudit?.auditResult === 'REJECT_RECHECK'
            ? currentAudit.auditResult
            : undefined,
        groupKey: group.key,
        itemId: item.id,
        samplePosition: group.name,
      };
    }
  }
}

function groupDraft(
  item: MesFaiApi.FaiItem,
  group: SamplePositionGroup,
): GroupAuditDraft | undefined {
  if (!item.id) return undefined;
  const key = groupAuditDraftKey(item, group);
  if (!groupAudits[key]) {
    groupAudits[key] = {
      auditRemark: group.audit?.auditRemark || '',
      auditResult:
        group.audit?.auditResult === 'CONFIRM' ||
        group.audit?.auditResult === 'REJECT_RECHECK'
          ? group.audit.auditResult
          : undefined,
      groupKey: group.key,
      itemId: item.id,
      samplePosition: group.name,
    };
  }
  return groupAudits[key];
}

function groupAuditDraftKey(
  item: MesFaiApi.FaiItem,
  group: SamplePositionGroup,
) {
  return `${item.id || item.inspectionItem}|${group.key}`;
}

function handleConfirmAllGroups() {
  for (const item of activeItems.value) {
    for (const group of samplePositionGroups(item, false)) {
      const draft = groupDraft(item, group);
      if (draft) draft.auditResult = 'CONFIRM';
    }
  }
  applyDefaultAuditResult();
}

function handleGroupAuditChange() {
  applyDefaultAuditResult();
}

function handleOpenFinalAudit() {
  if (!validateGroupAuditDrafts()) return;
  applyDefaultAuditResult();
  finalAuditOpen.value = true;
}

function applyDefaultAuditResult() {
  const drafts = Object.values(groupAudits);
  if (drafts.some((draft) => draft.auditResult === 'REJECT_RECHECK')) {
    auditResult.value = 'REJECT';
    return;
  }
  const allConfirmed =
    drafts.length > 0 &&
    drafts.every((draft) => draft.auditResult === 'CONFIRM');
  if (!allConfirmed) {
    auditResult.value = 'PASS';
    return;
  }
  const hasNgItem = activeItems.value.some((item) => hasNgInspectionItem(item));
  auditResult.value = hasNgItem ? 'FAIL' : 'PASS';
}

function validateGroupAuditDrafts() {
  if (!record.value?.id) return false;
  const drafts = Object.values(groupAudits);
  if (drafts.length === 0) {
    message.warning('当前检验单暂无可审核样本组');
    return false;
  }
  for (const draft of drafts) {
    if (!draft.auditResult) {
      message.warning(`请完成样本组【${draft.samplePosition}】的审核确认`);
      return false;
    }
    if (draft.auditResult === 'REJECT_RECHECK' && !draft.auditRemark.trim()) {
      message.warning(`请填写样本组【${draft.samplePosition}】的驳回重检说明`);
      return false;
    }
  }
  return true;
}

function validateAuditSubmit(result: MesFaiApi.AuditResult) {
  if (!validateGroupAuditDrafts()) return false;
  const drafts = Object.values(groupAudits);
  if (
    drafts.some((draft) => draft.auditResult === 'REJECT_RECHECK') &&
    result !== 'REJECT'
  ) {
    message.warning('存在驳回重检组时，只允许退回检验');
    return false;
  }
  if (result === 'REJECT' && !auditRemark.value.trim()) {
    message.warning('请填写退回原因');
    return false;
  }
  return true;
}

async function handleAuditSubmit() {
  await submitAudit(auditResult.value);
}

async function submitAudit(result: MesFaiApi.AuditResult) {
  if (!validateAuditSubmit(result) || !record.value?.id) return;
  submitting.value = true;
  try {
    const auditedRecord = await auditFaiProgramEntry({
      id: record.value.id,
      auditResult: result,
      rejectReason: auditRemark.value.trim() || undefined,
      groups: Object.values(groupAudits).map((draft) => ({
        itemId: draft.itemId,
        groupKey: draft.groupKey,
        samplePosition: draft.samplePosition,
        auditResult: draft.auditResult!,
        auditRemark: draft.auditRemark.trim() || undefined,
      })),
    });
    message.success(resolveAuditSuccessMessage(result));
    emit('success', auditedRecord);
    finalAuditOpen.value = false;
    closeModal();
  } finally {
    submitting.value = false;
  }
}

function judgmentLabel(result?: MesFaiApi.Judgment) {
  if (result === 'OK') return '合格';
  if (result === 'NG') return '不合格';
  return '待判定';
}

function resolveAuditSuccessMessage(result: MesFaiApi.AuditResult) {
  if (result === 'PASS') return '首检单已审核通过';
  if (result === 'FAIL') return '首检单已判定不合格';
  return '首检单已驳回重检';
}

function auditResultLabel(result: MesFaiApi.AuditResult) {
  if (result === 'PASS') return '放行';
  if (result === 'FAIL') return '不合格';
  return '驳回';
}

function auditResultColor(result: MesFaiApi.AuditResult) {
  if (result === 'PASS') return 'green';
  if (result === 'FAIL') return 'red';
  return 'orange';
}

function optionLabel(
  options: Array<{ label: string; value: string }>,
  value?: string,
) {
  return options.find((item) => item.value === value)?.label || '-';
}

function groupKey(item: MesFaiApi.FaiItem) {
  return [
    item.stepCode || '-',
    item.sheetSectionCode || '-',
    item.stepName || '-',
    item.sheetSectionName || '-',
  ].join('|');
}

function groupTitle(item: MesFaiApi.FaiItem) {
  return (
    item.stepName || item.sheetSectionName || item.stepCode || '未分组项目'
  );
}

function groupSubtitle(item: MesFaiApi.FaiItem) {
  return [
    item.stepCode ? `步骤 ${item.stepCode}` : '',
    item.sheetSectionCode ? `区块 ${item.sheetSectionCode}` : '',
    item.sheetSectionName && item.sheetSectionName !== groupTitle(item)
      ? item.sheetSectionName
      : '',
  ]
    .filter(Boolean)
    .join(' / ');
}

function itemTitle(item: MesFaiApi.FaiItem) {
  return (
    item.sheetMetricName || item.inspectionItem || item.sheetMetricCode || '-'
  );
}

function attachmentName(url: string, index: number) {
  const cleanUrl = url.split('?')[0] || '';
  return decodeURIComponent(cleanUrl.split('/').pop() || `附件${index + 1}`);
}

function isImageAttachment(url: string) {
  return /\.(?:avif|bmp|gif|ico|jpe?g|png|svg|tiff?|webp)(?:$|[?#])/i.test(url);
}

function itemCollapseKey(item: MesFaiApi.FaiItem) {
  return String(
    item.id ??
      item.standardItemId ??
      `${item.inspectionItem || '-'}|${item.sheetMetricCode || '-'}`,
  );
}

function allItemCollapseKeys() {
  const items = onlyNgItems.value
    ? activeItems.value.filter((item) => hasNgInspectionItem(item))
    : activeItems.value;
  return items.map((item) => itemCollapseKey(item));
}

function isDetailItemExpanded(item: MesFaiApi.FaiItem) {
  return expandedItemKeys.value.includes(itemCollapseKey(item));
}

function toggleDetailItem(item: MesFaiApi.FaiItem) {
  const key = itemCollapseKey(item);
  if (expandedItemKeys.value.includes(key)) {
    expandedItemKeys.value = expandedItemKeys.value.filter(
      (current) => current !== key,
    );
  } else {
    expandedItemKeys.value = [...expandedItemKeys.value, key];
  }
}

function expandAllItems() {
  expandedItemKeys.value = allItemCollapseKeys();
}

function collapseAllItems() {
  expandedItemKeys.value = [];
}

function handleOnlyNgItemsChange(event: { target?: { checked?: boolean } }) {
  const checked = event?.target?.checked ?? onlyNgItems.value;
  if (checked) {
    expandedItemKeys.value = activeItems.value
      .filter((item) => hasNgInspectionItem(item))
      .map((item) => itemCollapseKey(item));
  }
}

function shouldShowGroupHeader(group: EntryGroup) {
  return group.title !== '基础信息' && !group.key.includes('BASIC_INFO');
}

function toggleBasicInfo() {
  basicInfoExpanded.value = !basicInfoExpanded.value;
}

function submissionAmountCard(current: MesFaiApi.FaiRecord): SummaryCard {
  if (shouldDisplaySampleLength(current)) {
    return {
      label: '送检米数',
      value: hasMeaningfulValue(current.sampleLength)
        ? current.sampleLength
        : current.inspectionQty,
    };
  }
  return { label: '送检数量', value: formatInspectionQty(current) };
}

function shouldDisplaySampleLength(current: MesFaiApi.FaiRecord) {
  const tokens = [
    current.processCategory,
    (current as any).operationCode,
    current.operationName,
    current.sourceOperationName,
  ].map((item) =>
    String(item || '')
      .trim()
      .toUpperCase(),
  );
  const labelText = [
    current.processCategory,
    (current as any).operationCode,
    current.operationName,
    current.sourceOperationName,
  ]
    .filter(Boolean)
    .join(' ');
  return (
    tokens.some((item) =>
      [
        'ADHESIVE',
        'ADHESIVE1',
        'GLUE_1',
        'WC-ADH1',
        'GRINDING',
        'ROUGH_GRINDING',
        'WC-GRIND',
        'WET',
        'WC-COAT',
      ].includes(item),
    ) ||
    labelText.includes('粘胶1') ||
    labelText.includes('磨皮') ||
    labelText.includes('湿法')
  );
}

function hasNgInspectionItem(item: MesFaiApi.FaiItem) {
  return (
    isNgValue(item.qaResult) ||
    sampleRows(item).some((row) => isNgSampleRow(row))
  );
}

function isNgSampleRow(row: Record<string, any>) {
  return (
    isNgValue(row.sampleResult) ||
    isNgValue(row.qaResult) ||
    isNgValue(row.result)
  );
}

function isNgValue(value?: unknown) {
  const text = String(value ?? '')
    .trim()
    .toUpperCase();
  return ['NG', 'NOK', 'FAIL', 'FAILED', '不合格'].includes(text);
}

function itemCodeText(item: MesFaiApi.FaiItem) {
  return [
    item.metricGroupCode ? `组 ${item.metricGroupCode}` : '',
    item.metricCode ? `指标 ${item.metricCode}` : '',
    item.sheetMetricCode ? `表格指标 ${item.sheetMetricCode}` : '',
  ]
    .filter(Boolean)
    .join(' / ');
}

function typeLabel(item: MesFaiApi.FaiItem) {
  return item.itemType === 'QUALITATIVE' ? '定性' : '定量';
}

function valueTemplateLabel(item: MesFaiApi.FaiItem) {
  if (item.valueTemplateName) return item.valueTemplateName;
  if (item.valueTemplate === 'DENSITY_CALC') return '密度计算';
  if (item.valueTemplate === 'COMPRESSION_CALC') return '压缩性能';
  if (item.valueTemplate === 'SINGLE_VALUE') return '单值';
  return '常规';
}

function resultLabel(value?: string) {
  if (value === 'OK') return '合格';
  if (value === 'NG') return '不合格';
  if (value === 'PENDING' || value === '-') return '待判定';
  return value || '待判定';
}

function resultColor(value?: string) {
  if (value === 'OK') return 'success';
  if (value === 'NG') return 'error';
  return 'default';
}

function groupAuditStatusText(
  item: MesFaiApi.FaiItem,
  group: SamplePositionGroup,
) {
  const draft = groupDraft(item, group);
  if (draft?.auditResult === 'REJECT_RECHECK') return '驳回重检';
  if (group.audit?.itemRecheckStatus === 'WAIT_RECHECK') return '待复检';
  if (group.audit?.itemRecheckStatus === 'WAIT_AUDIT') return '复检待审';
  if (group.audit?.itemRecheckStatus === 'CONFIRMED') return '复检已确认';
  if (
    draft?.auditResult === 'CONFIRM' ||
    group.audit?.auditResult === 'CONFIRM'
  ) {
    return '已确认';
  }
  if (group.rows.some((row) => isNgSampleRow(row))) return '不合格待确认';
  return '待确认';
}

function groupAuditStatusColor(
  item: MesFaiApi.FaiItem,
  group: SamplePositionGroup,
) {
  const draft = groupDraft(item, group);
  if (
    draft?.auditResult === 'REJECT_RECHECK' ||
    group.audit?.itemRecheckStatus === 'WAIT_RECHECK'
  ) {
    return 'error';
  }
  if (group.audit?.itemRecheckStatus === 'WAIT_AUDIT') return 'purple';
  if (
    draft?.auditResult === 'CONFIRM' ||
    group.audit?.auditResult === 'CONFIRM' ||
    group.audit?.itemRecheckStatus === 'CONFIRMED'
  ) {
    return 'success';
  }
  if (group.rows.some((row) => isNgSampleRow(row))) return 'warning';
  return 'default';
}

function statusLabel(value?: string) {
  return (
    faiStatusOptions.find((item) => item.value === value)?.label || value || '-'
  );
}

function statusColor(value?: string) {
  if (value === 'COMPLETED') return 'success';
  if (value === 'REJECTED' || value === 'CANCELED') return 'error';
  if (value === 'WAITING_QA') return 'purple';
  if (value === 'PENDING') return 'warning';
  return 'blue';
}

function inputStatusLabel(status?: string) {
  if (status === 'COMPLETE') return '已完成';
  if (status === 'FILLING') return '填写中';
  if (status === 'ABNORMAL') return '异常';
  return '未填';
}

function inputStatusColor(status?: string) {
  if (status === 'COMPLETE') return 'success';
  if (status === 'FILLING') return 'processing';
  if (status === 'ABNORMAL') return 'error';
  return 'default';
}

function sampleRows(item: MesFaiApi.FaiItem) {
  const qaSamples = (item.samples || []).filter(
    (sample) => !sample.sampleRole || sample.sampleRole === 'QA',
  );
  const values =
    Array.isArray(item.qaValues) && item.qaValues.length > 0
      ? item.qaValues
      : qaSamples;
  return values
    .map((sample, index) => normalizeSampleRow(sample, item, index))
    .toSorted((left, right) => compareSampleRows(left, right, item));
}

function normalizeSampleRow(
  sample: MesFaiApi.FaiSample | Record<string, any>,
  item: MesFaiApi.FaiItem,
  index: number,
) {
  const row =
    sample && typeof sample === 'object' ? { ...sample } : { value: sample };
  if ('rawValuesJson' in row && row.rawValuesJson) {
    try {
      Object.assign(row, JSON.parse(String(row.rawValuesJson)));
    } catch {
      // 保留原始字段展示，避免异常 JSON 阻断查看。
    }
  }
  return {
    ...row,
    metricCode: row.metricCode ?? item.metricCode,
    metricGroupCode: row.metricGroupCode ?? item.metricGroupCode,
    sampleGroupNo: row.sampleGroupNo ?? index + 1,
    samplePosition: row.samplePosition ?? `位置${index + 1}`,
    sampleResult:
      row.sampleResult ??
      (item.itemType === 'QUALITATIVE'
        ? (row.qualitativeValue ?? row.value)
        : undefined) ??
      (item.qaResult === '-' ? 'PENDING' : item.qaResult),
    sampleSeq: row.sampleSeq ?? index + 1,
  };
}

function samplePositionGroups(
  item: MesFaiApi.FaiItem,
  ngOnly = onlyNgItems.value,
): SamplePositionGroup[] {
  const rows = ngOnly
    ? sampleRows(item).filter((row) => isNgSampleRow(row))
    : sampleRows(item);

  const configuredPositions = configuredEntryRulePositions(item);
  const aliasMap = new Map<string, string>();
  const groupMap = new Map<string, SamplePositionGroup>();
  const expectedCount = Math.max(1, resolveEntryRuleRepeatCount(item));

  configuredPositions.forEach((position) => {
    const key = normalizePositionKey(position.code || position.name);
    groupMap.set(key, {
      duplicateGroups: [],
      expectedCount,
      key,
      missingGroups: [],
      name: position.name || position.code,
      rows: [],
      unexpected: false,
    });
    addPositionAlias(aliasMap, position.code, key);
    addPositionAlias(aliasMap, position.name, key);
  });

  (item.auditGroups || []).forEach((audit) => {
    const auditKeys = [audit.groupKey, audit.samplePosition]
      .map((value) => normalizePositionKey(value))
      .filter(Boolean);
    const key =
      auditKeys.map((auditKey) => aliasMap.get(auditKey)).find(Boolean) ||
      auditKeys[0];
    if (!key) return;
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        audit,
        duplicateGroups: [],
        expectedCount,
        key,
        missingGroups: [],
        name: audit.samplePosition || audit.groupKey,
        rows: [],
        unexpected: configuredPositions.length > 0,
      });
    } else {
      const group = groupMap.get(key)!;
      group.audit = audit;
      group.name = group.name || audit.samplePosition || audit.groupKey;
    }
  });

  rows.forEach((row, index) => {
    const rawPosition =
      row.samplePosition || row.samplePositionCode || `位置${index + 1}`;
    const aliasKey = normalizePositionKey(rawPosition);
    const groupKey = aliasMap.get(aliasKey) || aliasKey;
    if (!groupMap.has(groupKey)) {
      groupMap.set(groupKey, {
        duplicateGroups: [],
        expectedCount,
        key: groupKey,
        missingGroups: [],
        name: String(rawPosition),
        rows: [],
        unexpected: configuredPositions.length > 0,
      });
    }
    groupMap.get(groupKey)!.rows.push(row);
  });

  return [...groupMap.values()]
    .filter((group) => group.rows.length > 0 || !!group.audit)
    .map((group) =>
      ngOnly ? withFilteredGroupIssues(group) : withGroupIssues(group),
    );
}

function visibleSamplePositionGroups(item: MesFaiApi.FaiItem) {
  if (!isAuditMode.value) return samplePositionGroups(item, onlyNgItems.value);
  const groups = samplePositionGroups(item, reviewFilter.value === 'NG');
  if (reviewFilter.value === 'WAIT_RECHECK') {
    return groups.filter(
      (group) => group.audit?.itemRecheckStatus === 'WAIT_RECHECK',
    );
  }
  if (reviewFilter.value === 'WAIT_AUDIT') {
    return groups.filter(
      (group) => group.audit?.itemRecheckStatus === 'WAIT_AUDIT',
    );
  }
  return groups;
}

function withGroupIssues(group: SamplePositionGroup): SamplePositionGroup {
  const counts = new Map<number, number>();
  group.rows.forEach((row) => {
    const groupNo = positiveGroupNo(row.sampleGroupNo);
    if (!groupNo) return;
    counts.set(groupNo, (counts.get(groupNo) || 0) + 1);
  });
  const missingGroups = Array.from({ length: group.expectedCount })
    .map((_, index) => index + 1)
    .filter((groupNo) => !counts.has(groupNo));
  const duplicateGroups = [...counts.entries()]
    .filter(([, count]) => count > 1)
    .map(([groupNo]) => groupNo);
  return { ...group, duplicateGroups, missingGroups };
}

function withFilteredGroupIssues(
  group: SamplePositionGroup,
): SamplePositionGroup {
  return {
    ...group,
    duplicateGroups: [],
    expectedCount: Math.max(1, group.rows.length),
    missingGroups: [],
  };
}

function samplePositionGroupIssueText(group: SamplePositionGroup) {
  const issues: string[] = [];
  if (group.unexpected) issues.push('非模板位置');
  if (group.missingGroups.length > 0) {
    issues.push(`缺第 ${group.missingGroups.join('、')} 组`);
  }
  if (group.duplicateGroups.length > 0) {
    issues.push(`第 ${group.duplicateGroups.join('、')} 组重复`);
  }
  return issues.join('；');
}

function sampleDetailColumns(item: MesFaiApi.FaiItem) {
  return sampleColumns(item).filter((column) => column.type !== 'position');
}

function configuredEntryRulePositions(item: MesFaiApi.FaiItem) {
  const params = parseEntryRuleParams(item.templateParams);
  if (!Array.isArray(params.positions) || params.positions.length === 0) {
    return [];
  }
  return params.positions
    .map((position: any, index: number) => ({
      code: String(position?.code || position?.name || `P${index + 1}`),
      name: String(position?.name || position?.code || `${index + 1}`),
      sort: Number(position?.sort ?? index + 1),
    }))
    .toSorted((left, right) => left.sort - right.sort);
}

function compareSampleRows(
  left: Record<string, any>,
  right: Record<string, any>,
  item: MesFaiApi.FaiItem,
) {
  const leftPosition = positionOrderIndex(
    item,
    left.samplePosition || left.samplePositionCode,
  );
  const rightPosition = positionOrderIndex(
    item,
    right.samplePosition || right.samplePositionCode,
  );
  if (leftPosition !== rightPosition) return leftPosition - rightPosition;
  const leftGroup =
    positiveGroupNo(left.sampleGroupNo) ?? Number.MAX_SAFE_INTEGER;
  const rightGroup =
    positiveGroupNo(right.sampleGroupNo) ?? Number.MAX_SAFE_INTEGER;
  if (leftGroup !== rightGroup) return leftGroup - rightGroup;
  return Number(left.sampleSeq ?? 0) - Number(right.sampleSeq ?? 0);
}

function positionOrderIndex(item: MesFaiApi.FaiItem, position?: unknown) {
  const key = normalizePositionKey(position);
  const index = configuredEntryRulePositions(item).findIndex(
    (entry) =>
      normalizePositionKey(entry.code) === key ||
      normalizePositionKey(entry.name) === key,
  );
  return index >= 0 ? index : Number.MAX_SAFE_INTEGER;
}

function normalizePositionKey(value?: unknown) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase();
}

function addPositionAlias(
  map: Map<string, string>,
  alias: unknown,
  key: string,
) {
  const normalized = normalizePositionKey(alias);
  if (normalized) map.set(normalized, key);
}

function positiveGroupNo(value?: unknown) {
  const number = Number(value);
  return Number.isInteger(number) && number > 0 ? number : undefined;
}

function dataRule(item: MesFaiApi.FaiItem): DataRuleConfig | undefined {
  try {
    const params = parseEntryRuleParams(item.templateParams);
    const rule = params.dataRule || params;
    const inputFields = Array.isArray(rule.inputFields) ? rule.inputFields : [];
    const resultFields = Array.isArray(rule.resultFields)
      ? rule.resultFields
      : [];
    if (inputFields.length === 0 && resultFields.length === 0) return undefined;
    return {
      inputFields: inputFields.map((field: any) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit,
      })),
      resultFields: resultFields.map((field: any) => ({
        code: String(field.code || ''),
        name: String(field.name || field.code || ''),
        unit: field.unit,
      })),
    };
  } catch {
    return undefined;
  }
}

function sampleColumns(item: MesFaiApi.FaiItem): SampleColumn[] {
  const baseColumns: SampleColumn[] = [
    { key: 'seq', label: '序号', type: 'seq' },
    { key: 'position', label: '位置', type: 'position' },
    { key: 'group', label: '组别', type: 'group' },
  ];
  if (item.itemType === 'QUALITATIVE') {
    return [
      ...baseColumns,
      { key: 'qualitative', label: '判定值', type: 'value' },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  const rule = dataRule(item);
  if (rule) {
    return [
      ...baseColumns,
      ...rule.inputFields.map((field) => ({
        key: `input-${field.code}`,
        label: labelWithUnit(field.name, field.unit),
        source: field.code,
        type: 'field' as const,
      })),
      ...rule.resultFields.map((field) => ({
        key: `result-${field.code}`,
        label: labelWithUnit(field.name, field.unit),
        source: field.code,
        type: 'field' as const,
      })),
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  if (item.valueTemplate === 'DENSITY_CALC') {
    return [
      ...baseColumns,
      {
        key: 'thicknessMm',
        label: '厚度/mm',
        source: 'thicknessMm',
        type: 'field',
      },
      { key: 'weightG', label: '重量/g', source: 'weightG', type: 'field' },
      {
        key: 'densityValue',
        label: '密度',
        source: 'densityValue',
        type: 'field',
      },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  if (item.valueTemplate === 'COMPRESSION_CALC') {
    return [
      ...baseColumns,
      { key: 't1Mm', label: 'T1/mm', source: 't1Mm', type: 'field' },
      { key: 't2Mm', label: 'T2/mm', source: 't2Mm', type: 'field' },
      { key: 't3Mm', label: 'T3/mm', source: 't3Mm', type: 'field' },
      {
        key: 'compressionRate',
        label: '压缩率',
        source: 'compressionRate',
        type: 'field',
      },
      {
        key: 'compressionElasticityRate',
        label: '弹性率',
        source: 'compressionElasticityRate',
        type: 'field',
      },
      { key: 'result', label: '结果', type: 'result' },
      { key: 'remark', label: '备注', type: 'remark' },
    ];
  }

  return [
    ...baseColumns,
    {
      key: 'actualValue',
      label: labelWithUnit('实测值', item.unit),
      type: 'value',
    },
    { key: 'result', label: '结果', type: 'result' },
    { key: 'remark', label: '备注', type: 'remark' },
  ];
}

function sampleColumnValue(
  row: Record<string, any>,
  item: MesFaiApi.FaiItem,
  column: SampleColumn,
) {
  if (column.type === 'seq') return row.sampleSeq;
  if (column.type === 'position') return row.samplePosition || '-';
  if (column.type === 'group') return row.sampleGroupNo || '-';
  if (column.type === 'remark') return row.remark || row.defectName || '-';
  if (column.type === 'result') return resultLabel(row.sampleResult);
  if (column.type === 'field')
    return formatDisplayValue(row[column.source || '']);
  return formatDisplayValue(resolveActualValue(row, item));
}

function resolveActualValue(row: Record<string, any>, item: MesFaiApi.FaiItem) {
  if (item.itemType === 'QUALITATIVE') {
    return row.value ?? row.qualitativeValue ?? row.sampleResult;
  }
  return (
    row.value ??
    row.resultValue ??
    row.measuredValue ??
    row.densityValue ??
    row.compressionRate ??
    row.compressionElasticityRate
  );
}

function labelWithUnit(label: string, unit?: string) {
  return unit ? `${label}/${unit}` : label;
}

function limitText(item: MesFaiApi.FaiItem) {
  const baseLimit = rangeText(
    item.minValueLimit,
    item.maxValueLimit,
    item.unit,
  );
  const avgLimit = rangeText(item.avgMinLimit, item.avgMaxLimit, item.unit);
  const stdLimit = rangeText(item.stdMinLimit, item.stdMaxLimit, item.unit);
  return [
    baseLimit ? `单值 ${baseLimit}` : '',
    avgLimit ? `平均值 ${avgLimit}` : '',
    stdLimit ? `标准差 ${stdLimit}` : '',
  ]
    .filter(Boolean)
    .join('；');
}

function statText(item: MesFaiApi.FaiItem) {
  const rows: string[] = [];
  appendStat(rows, '最小', item.calculatedMin ?? item.qaMin);
  appendStat(rows, '最大', item.calculatedMax ?? item.qaMax);
  appendStat(rows, '平均', item.calculatedAvg ?? item.qaAvg);
  appendStat(rows, '标准差', item.calculatedStd);
  return rows.join(' / ');
}

function appendStat(rows: string[], label: string, value?: number) {
  if (value === undefined) return;
  rows.push(`${label} ${formatDisplayValue(value)}`);
}

function rangeText(min?: number, max?: number, unit?: string) {
  if (min === undefined && max === undefined) return '';
  return `${formatDisplayValue(min)}~${formatDisplayValue(max)}${unit || ''}`;
}

function progressText(item: MesFaiApi.FaiItem) {
  const required =
    item.requiredSampleCount ?? item.cellRequiredCount ?? item.sampleSize ?? 0;
  const completed = item.completedSampleCount ?? item.cellCompletedCount ?? 0;
  return required > 0 ? `${completed}/${required}` : '-';
}

function formatDisplayValue(value: unknown) {
  if (value === undefined || value === null || value === '') return '-';
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) return '-';
    const fixedNumber = Number(value.toFixed(6));
    return Object.is(fixedNumber, -0) ? '0' : String(fixedNumber);
  }
  return String(value);
}

function hasMeaningfulValue(value: unknown) {
  if (value === undefined || value === null) return false;
  const text = String(value).trim();
  return text !== '' && text !== '-';
}

function formatDate(value?: string) {
  if (!value) return '-';
  return value.slice(0, 19).replace('T', ' ');
}
</script>

<template>
  <Modal>
    <div
      :class="{
        'qms-fai-detail-content': !isGlueBoardMode && !isRetentionMode,
      }"
      class="flex h-screen flex-col overflow-hidden bg-slate-100"
    >
      <header
        class="flex h-14 shrink-0 items-center justify-between border-b bg-white px-4"
      >
        <div class="flex min-w-0 items-center gap-3">
          <div
            class="flex h-8 w-8 items-center justify-center rounded border bg-slate-50 text-indigo-600"
          >
            <IconifyIcon
              :icon="isGlueBoardMode ? 'lucide:layers' : 'lucide:file-check-2'"
            />
          </div>
          <div class="min-w-0">
            <div class="truncate text-base font-bold text-slate-800">
              {{ modalTitle }}
            </div>
            <div class="truncate font-mono text-xs text-slate-500">
              {{ recordNo }}
            </div>
          </div>
          <Tag v-if="record" :color="statusColor(record.status)" class="!m-0">
            {{ statusLabel(record.status) }}
          </Tag>
        </div>
        <Button size="small" @click="closeModal">
          <IconifyIcon icon="lucide:x" class="mr-1" /> 关闭
        </Button>
      </header>

      <Spin :spinning="loading" class="min-h-0 flex-1 overflow-hidden">
        <div v-if="record" class="flex h-full min-h-0 flex-col overflow-hidden">
          <section class="shrink-0 border-b bg-white px-4 py-3">
            <div class="qms-fai-erp-form">
              <div class="qms-fai-erp-form__title-row">
                <div class="qms-fai-erp-form__title">检验项目</div>
                <div class="qms-fai-erp-form__actions">
                  <Radio.Group
                    v-if="isAuditMode"
                    v-model:value="reviewFilter"
                    size="small"
                  >
                    <Radio.Button value="ALL">全部</Radio.Button>
                    <Radio.Button value="NG">不合格</Radio.Button>
                    <Radio.Button value="WAIT_RECHECK">待复检</Radio.Button>
                    <Radio.Button value="WAIT_AUDIT">复检待审</Radio.Button>
                  </Radio.Group>
                  <Checkbox
                    v-else
                    v-model:checked="onlyNgItems"
                    class="qms-fai-ng-filter"
                    @change="handleOnlyNgItemsChange"
                  >
                    只看不合格
                  </Checkbox>
                  <Button
                    v-if="isAuditMode"
                    size="small"
                    type="primary"
                    @click="handleConfirmAllGroups"
                  >
                    一键确认
                  </Button>
                  <Tooltip title="全部展开">
                    <Button
                      aria-label="全部展开"
                      class="qms-fai-icon-action"
                      size="small"
                      type="text"
                      @click="expandAllItems"
                    >
                      <IconifyIcon icon="lucide:chevrons-down" />
                    </Button>
                  </Tooltip>
                  <Tooltip title="全部收起">
                    <Button
                      aria-label="全部收起"
                      class="qms-fai-icon-action"
                      size="small"
                      type="text"
                      @click="collapseAllItems"
                    >
                      <IconifyIcon icon="lucide:chevrons-up" />
                    </Button>
                  </Tooltip>
                  <Tooltip
                    :title="basicInfoExpanded ? '收起单据信息' : '展开单据信息'"
                  >
                    <Button
                      :aria-label="
                        basicInfoExpanded ? '收起单据信息' : '展开单据信息'
                      "
                      class="qms-fai-icon-action"
                      size="small"
                      type="text"
                      @click="toggleBasicInfo"
                    >
                      <IconifyIcon
                        :icon="
                          basicInfoExpanded
                            ? 'lucide:chevron-up'
                            : 'lucide:chevron-down'
                        "
                      />
                    </Button>
                  </Tooltip>
                </div>
              </div>
              <template v-if="basicInfoExpanded">
                <div class="qms-fai-erp-form__grid">
                  <div
                    v-for="card in basicInfoCards"
                    :key="card.label"
                    class="qms-fai-erp-form__field"
                  >
                    <div class="qms-fai-erp-form__label">
                      {{ card.label }}
                    </div>
                    <div
                      class="qms-fai-erp-form__value"
                      :class="card.mono ? 'font-mono' : ''"
                      :title="formatDisplayValue(card.value)"
                    >
                      {{ formatDisplayValue(card.value) }}
                    </div>
                  </div>
                </div>
                <template v-if="glueBoardInfoCards.length > 0">
                  <div class="qms-fai-erp-form__group-title">胶板信息</div>
                  <div class="qms-fai-erp-form__grid">
                    <div
                      v-for="card in glueBoardInfoCards"
                      :key="card.label"
                      class="qms-fai-erp-form__field"
                    >
                      <div class="qms-fai-erp-form__label">
                        {{ card.label }}
                      </div>
                      <div
                        class="qms-fai-erp-form__value"
                        :class="card.mono ? 'font-mono' : ''"
                        :title="formatDisplayValue(card.value)"
                      >
                        {{ formatDisplayValue(card.value) }}
                      </div>
                    </div>
                  </div>
                </template>
              </template>
            </div>
            <div
              v-if="
                basicInfoExpanded && (record.remark || record.lastReturnReason)
              "
              class="mt-3 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800"
            >
              <span class="font-bold">{{
                record.lastReturnReason ? '驳回原因：' : '备注：'
              }}</span>
              <span>{{ record.lastReturnReason || record.remark }}</span>
            </div>
            <div
              v-if="isAuditMode"
              class="mt-3 rounded border border-blue-100 bg-blue-50 px-3 py-3"
            >
              <div class="flex flex-wrap items-center justify-between gap-3">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-sm font-bold text-slate-700">
                    审核结果
                  </span>
                  <Tag :color="auditResultColor(auditResult)" class="!m-0">
                    {{ auditResultLabel(auditResult) }}
                  </Tag>
                  <span class="text-xs text-slate-500">
                    完成各样本组确认后提交最终审核
                  </span>
                </div>
                <Button
                  :loading="submitting"
                  type="primary"
                  @click="handleOpenFinalAudit"
                >
                  确认审核结果
                </Button>
              </div>
            </div>
          </section>

          <main class="min-h-0 flex-1 overflow-auto p-3">
            <Empty
              v-if="visibleGroupedItems.length === 0"
              :description="detailEmptyDescription"
            />

            <div v-else class="space-y-3">
              <section
                v-for="group in visibleGroupedItems"
                :key="group.key"
                class="overflow-hidden rounded border bg-white"
              >
                <header
                  v-if="shouldShowGroupHeader(group)"
                  class="border-b bg-slate-50 px-3 py-2"
                >
                  <div
                    class="qms-fai-detail-heading text-sm font-bold text-slate-800"
                  >
                    {{ group.title }}
                  </div>
                  <div
                    v-if="group.subtitle"
                    class="mt-1 text-xs text-slate-500"
                  >
                    {{ group.subtitle }}
                  </div>
                </header>

                <div class="divide-y">
                  <article
                    v-for="item in group.items"
                    :key="item.id || item.standardItemId || item.inspectionItem"
                    class="p-3"
                  >
                    <div class="flex items-start justify-between gap-3">
                      <div class="flex min-w-0 items-start gap-2">
                        <button
                          type="button"
                          class="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center border bg-white text-slate-500 hover:border-indigo-300 hover:text-indigo-700"
                          :title="
                            isDetailItemExpanded(item)
                              ? '收起检验项目'
                              : '展开检验项目'
                          "
                          @click="toggleDetailItem(item)"
                        >
                          <IconifyIcon
                            :icon="
                              isDetailItemExpanded(item)
                                ? 'lucide:chevron-down'
                                : 'lucide:chevron-right'
                            "
                          />
                        </button>
                        <div class="min-w-0">
                          <div class="flex flex-wrap items-center gap-2">
                            <span
                              class="qms-fai-detail-heading text-sm font-bold text-slate-800"
                            >
                              {{ itemTitle(item) }}
                            </span>
                            <Tag color="blue" class="!m-0">
                              {{ typeLabel(item) }}
                            </Tag>
                            <Tag color="purple" class="!m-0">
                              {{ valueTemplateLabel(item) }}
                            </Tag>
                            <Tag
                              :color="inputStatusColor(item.inputStatus)"
                              class="!m-0"
                            >
                              {{ inputStatusLabel(item.inputStatus) }}
                            </Tag>
                            <Tag
                              :color="resultColor(item.qaResult)"
                              class="!m-0"
                            >
                              {{ resultLabel(item.qaResult) }}
                            </Tag>
                          </div>
                          <div
                            v-if="itemCodeText(item)"
                            class="mt-1 text-xs text-slate-500"
                          >
                            {{ itemCodeText(item) }}
                          </div>
                        </div>
                      </div>
                      <div class="shrink-0 text-right text-xs text-slate-500">
                        录入进度
                        <span class="font-mono font-bold text-slate-800">{{
                          progressText(item)
                        }}</span>
                      </div>
                    </div>

                    <template v-if="isDetailItemExpanded(item)">
                      <div
                        class="mt-2 grid grid-cols-1 gap-2 text-xs md:grid-cols-3"
                      >
                        <div class="rounded bg-slate-50 p-2">
                          <div class="font-bold text-slate-400">标准说明</div>
                          <div class="mt-1 whitespace-pre-wrap text-slate-700">
                            {{ item.standardDesc || '-' }}
                          </div>
                        </div>
                        <div class="rounded bg-slate-50 p-2">
                          <div class="font-bold text-slate-400">控制限</div>
                          <div class="mt-1 text-slate-700">
                            {{ limitText(item) || '-' }}
                          </div>
                        </div>
                        <div class="rounded bg-slate-50 p-2">
                          <div class="font-bold text-slate-400">统计结果</div>
                          <div class="mt-1 text-slate-700">
                            {{ statText(item) || '-' }}
                          </div>
                        </div>
                      </div>

                      <div
                        v-if="
                          item.ruleDescription ||
                          item.inspectionMethod ||
                          item.testTool
                        "
                        class="mt-2 rounded border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-800"
                      >
                        <span v-if="item.ruleDescription">
                          填写说明：{{ item.ruleDescription }}
                        </span>
                        <span v-if="item.inspectionMethod" class="ml-3">
                          方法：{{ item.inspectionMethod }}
                        </span>
                        <span v-if="item.testTool" class="ml-3">
                          工具：{{ item.testTool }}
                        </span>
                      </div>

                      <div class="mt-3 space-y-2">
                        <div
                          v-if="visibleSamplePositionGroups(item).length === 0"
                          class="rounded border px-2 py-5 text-center text-xs text-slate-400"
                        >
                          暂无样本数据
                        </div>
                        <template v-else>
                          <div
                            v-for="position in visibleSamplePositionGroups(
                              item,
                            )"
                            :key="`${item.id || item.inspectionItem}-${position.key}`"
                            class="overflow-hidden rounded border"
                          >
                            <div
                              class="flex flex-wrap items-center justify-between gap-2 border-b bg-slate-50 px-3 py-2"
                            >
                              <div
                                class="flex min-w-0 items-center gap-2 text-xs font-bold text-slate-700"
                              >
                                <IconifyIcon
                                  icon="lucide:map-pin"
                                  class="shrink-0 text-indigo-500"
                                />
                                <span class="truncate">{{
                                  position.name
                                }}</span>
                              </div>
                              <div class="flex flex-wrap items-center gap-2">
                                <Tag color="blue" class="!m-0">
                                  已录 {{ position.rows.length }}/{{
                                    position.expectedCount
                                  }}
                                  组
                                </Tag>
                                <Tag
                                  v-if="isAuditMode || position.audit"
                                  :color="groupAuditStatusColor(item, position)"
                                  class="!m-0"
                                >
                                  {{ groupAuditStatusText(item, position) }}
                                </Tag>
                                <Tag
                                  v-if="samplePositionGroupIssueText(position)"
                                  color="warning"
                                  class="!m-0"
                                >
                                  {{ samplePositionGroupIssueText(position) }}
                                </Tag>
                              </div>
                            </div>
                            <div class="overflow-auto">
                              <table
                                class="w-full min-w-[760px] border-collapse text-xs"
                              >
                                <thead class="bg-white text-slate-500">
                                  <tr>
                                    <template
                                      v-for="column in sampleDetailColumns(
                                        item,
                                      )"
                                      :key="column.key"
                                    >
                                      <th class="border-b px-2 py-2 text-left">
                                        {{ column.label }}
                                      </th>
                                    </template>
                                  </tr>
                                </thead>
                                <tbody>
                                  <tr
                                    v-for="row in position.rows"
                                    :key="`${item.id || item.inspectionItem}-${position.key}-${row.id || row.sampleSeq}`"
                                    class="border-b last:border-b-0"
                                  >
                                    <template
                                      v-for="column in sampleDetailColumns(
                                        item,
                                      )"
                                      :key="column.key"
                                    >
                                      <td
                                        class="px-2 py-2 text-slate-700"
                                        :class="
                                          column.type === 'result'
                                            ? 'font-bold'
                                            : ''
                                        "
                                      >
                                        <Tag
                                          v-if="column.type === 'result'"
                                          :color="resultColor(row.sampleResult)"
                                          class="!m-0"
                                        >
                                          {{
                                            sampleColumnValue(row, item, column)
                                          }}
                                        </Tag>
                                        <span v-else>{{
                                          sampleColumnValue(row, item, column)
                                        }}</span>
                                      </td>
                                    </template>
                                  </tr>
                                  <tr
                                    v-if="position.rows.length === 0"
                                    class="border-b last:border-b-0"
                                  >
                                    <td
                                      :colspan="
                                        sampleDetailColumns(item).length
                                      "
                                      class="px-2 py-6 text-center text-slate-400"
                                    >
                                      原检验数据已清空，等待检验人重新提交
                                    </td>
                                  </tr>
                                </tbody>
                              </table>
                            </div>
                            <div
                              v-if="isAuditMode && groupDraft(item, position)"
                              class="border-t bg-blue-50/60 px-3 py-3"
                            >
                              <div
                                class="grid grid-cols-1 gap-3 text-xs lg:grid-cols-[auto_minmax(0,1fr)] lg:items-start"
                              >
                                <div class="flex flex-wrap items-center gap-2">
                                  <span class="font-bold text-slate-600">
                                    审核确认
                                  </span>
                                  <Radio.Group
                                    v-model:value="
                                      groupAudits[
                                        groupAuditDraftKey(item, position)
                                      ].auditResult
                                    "
                                    button-style="solid"
                                    size="small"
                                    @change="handleGroupAuditChange"
                                  >
                                    <Radio.Button value="CONFIRM">
                                      确认
                                    </Radio.Button>
                                    <Radio.Button value="REJECT_RECHECK">
                                      驳回重检
                                    </Radio.Button>
                                  </Radio.Group>
                                </div>
                                <div class="grid min-w-0 gap-1">
                                  <span class="font-bold text-slate-600">
                                    检验说明
                                  </span>
                                  <Input.TextArea
                                    v-model:value="
                                      groupAudits[
                                        groupAuditDraftKey(item, position)
                                      ].auditRemark
                                    "
                                    :maxlength="500"
                                    :rows="2"
                                    placeholder="检验说明"
                                    show-count
                                  />
                                </div>
                              </div>
                            </div>
                          </div>
                        </template>
                      </div>

                      <div
                        v-if="item.attachmentEnabled"
                        class="mt-3 rounded border bg-slate-50 p-3 text-xs"
                      >
                        <div class="mb-2 font-bold text-slate-600">
                          检验项附件
                        </div>
                        <div
                          v-if="item.attachmentUrls?.length"
                          class="flex flex-wrap items-center gap-2"
                        >
                          <template
                            v-for="(
                              url, attachmentIndex
                            ) in item.attachmentUrls"
                            :key="url"
                          >
                            <Image
                              v-if="isImageAttachment(url)"
                              :alt="attachmentName(url, attachmentIndex)"
                              :height="56"
                              :preview="{ zIndex: 10080 }"
                              :src="url"
                              :width="72"
                              class="rounded border object-cover"
                            />
                            <a
                              v-else
                              :href="url"
                              class="text-indigo-600 hover:underline"
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {{ attachmentName(url, attachmentIndex) }}
                            </a>
                          </template>
                        </div>
                        <span v-else class="text-slate-400">暂无附件</span>
                      </div>
                    </template>
                  </article>
                </div>
              </section>
            </div>
          </main>
        </div>
      </Spin>
    </div>
    <AntModal
      v-model:open="finalAuditOpen"
      :confirm-loading="submitting"
      title="确认审核结果"
      width="520px"
      wrap-class-name="qms-fai-audit-confirm-modal"
      @cancel="finalAuditOpen = false"
    >
      <div class="space-y-4">
        <Radio.Group v-model:value="auditResult">
          <Radio value="PASS">合格</Radio>
          <Radio value="REJECT">退回检验</Radio>
          <Radio value="FAIL">不合格</Radio>
        </Radio.Group>
        <Input.TextArea
          v-model:value="auditRemark"
          :maxlength="500"
          :rows="4"
          placeholder="审核说明"
          show-count
        />
      </div>
      <template #footer>
        <Button @click="finalAuditOpen = false">取消</Button>
        <Button
          :loading="submitting"
          type="primary"
          @click="handleAuditSubmit"
        >
          确认
        </Button>
      </template>
    </AntModal>
  </Modal>
</template>

<style>
.qms-fai-detail-modal [class*='modal__header'],
.qms-fai-detail-modal .ant-modal-header {
  display: none !important;
}

.qms-fai-detail-modal [class*='modal__body'],
.qms-fai-detail-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f1f5f9;
}

.qms-fai-detail-modal [class*='modal__content'],
.qms-fai-detail-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
}

.qms-fai-detail-modal .ant-spin-nested-loading,
.qms-fai-detail-modal .ant-spin-container {
  height: 100%;
  min-height: 0;
}

.qms-fai-detail-modal .ant-spin-container {
  display: flex;
  flex-direction: column;
}

.qms-fai-detail-content {
  font-size: 13px;
}

.qms-fai-detail-content :where(.text-xs, .text-sm) {
  font-size: 13px !important;
}

.qms-fai-detail-content
  :where(
    button,
    input,
    textarea,
    table,
    .ant-btn,
    .ant-checkbox-wrapper,
    .ant-empty-description,
    .ant-input-data-count,
    .ant-radio-button-wrapper,
    .ant-radio-wrapper,
    .ant-spin-text,
    .ant-tag
  ),
.qms-fai-audit-confirm-modal
  :where(
    .ant-btn,
    .ant-input,
    .ant-input-data-count,
    .ant-modal-body,
    .ant-modal-footer,
    .ant-radio-wrapper
  ) {
  font-size: 13px !important;
}

.qms-fai-detail-content .qms-fai-detail-heading {
  font-size: 14px !important;
}

.qms-fai-erp-form {
  border: 1px solid #d7dee8;
  background: #fff;
}

.qms-fai-erp-form__title-row {
  align-items: center;
  border-bottom: 1px solid #d7dee8;
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  min-height: 34px;
  padding: 0 8px 0 12px;
}

.qms-fai-erp-form__title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  line-height: 34px;
}

.qms-fai-erp-form__actions {
  align-items: center;
  display: flex;
  gap: 6px;
}

.qms-fai-erp-form__group-title {
  border-bottom: 1px solid #d7dee8;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 30px;
  padding: 0 12px;
}

.qms-fai-erp-form__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.qms-fai-erp-form__field {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  min-height: 36px;
}

.qms-fai-erp-form__label,
.qms-fai-erp-form__value {
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  display: flex;
  min-width: 0;
  padding: 6px 10px;
}

.qms-fai-erp-form__label {
  background: #f1f5f9;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  justify-content: flex-end;
}

.qms-fai-erp-form__value {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-fai-icon-action {
  align-items: center;
  border: 1px solid #d7dee8;
  border-radius: 4px;
  color: #475569;
  display: inline-flex;
  height: 24px;
  justify-content: center;
  padding: 0;
  width: 28px;
}

.qms-fai-icon-action:hover {
  border-color: #60a5fa;
  color: #1677ff;
}

.qms-fai-ng-filter {
  color: #334155;
  font-size: 13px;
  margin-right: 4px;
}

@media (max-width: 1280px) {
  .qms-fai-erp-form__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .qms-fai-erp-form__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .qms-fai-erp-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
