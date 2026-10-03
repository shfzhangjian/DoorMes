<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, nextTick, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, message, Modal, Spin, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  confirmProcessFormRecordBySigner,
  deleteProcessFormRecord,
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import {
  getStationFormDetail,
  getStationFormPage,
} from '#/api/mes/hc/stationform';
import { PROCESS_FORM_RECORD_STATUS_OPTIONS } from '#/views/mes/hc/base/process-form-options';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

import {
  CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_CODES,
  CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
  todayDateText,
} from '../cut-round-fqc/shared/cutRoundFqcSelfCheck';
import {
  loadEnabledSelfCheckStationForms,
  selectTodaySelfCheckRecord,
} from '../shared/selfCheckStationFormGroup';

import '../cut-round-fqc/shared/cutRoundFqcSelfCheckRuntime.css';

defineOptions({ name: 'MesQualityCutRoundFqcSelfCheck' });

type RuntimeViewMode = 'edit' | 'view';

const PROCESS_SELF_CHECK_NAME = '裁切成品检验自检记录';
const PROCESS_SELF_CHECK_TYPE = 'STARTUP_CHECK';
const PROCESS_SELF_CHECK_TYPE_NAME = '设备开机点检';
const RESULT_STATUS_OPTIONS = [
  { label: 'OK', value: 'OK' },
  { label: 'NG', value: 'NG' },
];

const runtimeFillOpen = ref(false);
const runtimeFillForm = ref<MesHcStationFormApi.StationForm | null>(null);
const runtimeViewVisible = ref(false);
const runtimeViewLoading = ref(false);
const runtimeViewSaving = ref(false);
const runtimeViewConfirming = ref(false);
const runtimeViewMode = ref<RuntimeViewMode>('view');
const runtimeViewRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeViewHeaderData = ref<Record<string, any>>({});
const runtimeViewSchema = ref<Record<string, any>>({});
const runtimeRendererRef = ref<any>();
const runtimeConfirmAuthVisible = ref(false);
const runtimeConfirmAuthRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeInitialParams = computed(() => ({ recordDate: todayDateText() }));

const runtimeViewReadOnly = computed(
  () =>
    runtimeViewMode.value === 'view' ||
    runtimeViewRecord.value?.recordStatus === 'CONFIRMED',
);
const runtimeViewTitle = computed(
  () => runtimeViewRecord.value?.templateName || PROCESS_SELF_CHECK_NAME,
);
const runtimeViewRecordMeta = computed(() => [
  `记录编号：${runtimeViewRecord.value?.recordNo || '-'}`,
  `点检日期：${formatDateText(runtimeViewRecord.value?.recordDate)}`,
  `状态：${recordStatusText(runtimeViewRecord.value?.recordStatus)}`,
  `结果：${resultStatusText(runtimeViewRecord.value?.resultStatus)}`,
]);
const runtimeViewItems = computed<MesHcStationFormApi.StationFormItem[]>(() =>
  buildRuntimeDetailRows(runtimeViewRecord.value).map((row) => ({
    defaultResult: row.resultFlag,
    id: row.templateItemId,
    itemCategory: row.itemCategory,
    itemName: row.itemName,
    itemSeq: row.seq,
    standardText: row.standardText,
    stepNode: row.stepNode,
    valueMode: row.valueMode,
  })),
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: true,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入记录编号' },
        fieldName: 'recordNo',
        label: '记录编号',
      },
      {
        component: 'DatePicker',
        componentProps: { allowClear: true, valueFormat: 'YYYY-MM-DD' },
        fieldName: 'recordDate',
        label: '点检日期',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: PROCESS_FORM_RECORD_STATUS_OPTIONS,
        },
        fieldName: 'recordStatus',
        label: '状态',
      },
      {
        component: 'Select',
        componentProps: { allowClear: true, options: RESULT_STATUS_OPTIONS },
        fieldName: 'resultStatus',
        label: '结果',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入填写人' },
        fieldName: 'fillUserName',
        label: '填写人',
      },
    ],
  },
  gridOptions: {
    columns: [
      { field: 'recordNo', minWidth: 170, title: '记录编号' },
      {
        field: 'recordDate',
        formatter: ({ cellValue }: { cellValue?: unknown }) =>
          formatDateText(cellValue),
        title: '点检日期',
        width: 120,
      },
      { field: 'templateName', minWidth: 180, title: '表单名称' },
      {
        field: 'recordStatus',
        slots: { default: 'recordStatus' },
        title: '状态',
        width: 110,
      },
      {
        field: 'resultStatus',
        slots: { default: 'resultStatus' },
        title: '结果',
        width: 90,
      },
      { field: 'fillUserName', title: '填写人', width: 120 },
      { field: 'fillTime', title: '填写时间', width: 170 },
      { field: 'confirmUserName', title: '确认人', width: 120 },
      { field: 'confirmTime', title: '确认时间', width: 170 },
      { field: 'createTime', title: '创建时间', width: 170 },
      {
        align: 'center',
        field: 'actions',
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 260,
      },
    ],
    height: 'auto',
    keepSource: true,
    rowConfig: { isHover: true, keyField: 'id' },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getProcessFormRecordPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            processCode: CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
          }),
      },
    },
  } as VxeTableGridOptions<MesHcProcessFormApi.Record>,
});

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function safeParseJson<T = any>(value?: unknown): null | T {
  if (!value) return null;
  if (typeof value === 'object' && !Array.isArray(value)) return value as T;
  try {
    return JSON.parse(String(value)) as T;
  } catch {
    return null;
  }
}

function normalizeDateText(value?: unknown) {
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    if ([year, month, day].every((item) => Number.isFinite(Number(item)))) {
      return `${String(year).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    }
  }
  const text = String(value ?? '')
    .trim()
    .replace('T', ' ');
  const matched = /^(\d{4})[-,/](\d{1,2})[-,/](\d{1,2})/u.exec(text);
  if (!matched) return text;
  return `${matched[1]}-${matched[2]!.padStart(2, '0')}-${matched[3]!.padStart(2, '0')}`;
}

function firstDateText(...values: unknown[]) {
  for (const value of values) {
    const text = normalizeDateText(value);
    if (text) return text;
  }
  return '';
}

function formatDateText(value?: unknown) {
  return normalizeDateText(value) || '-';
}

function recordStatusText(status?: string) {
  return (
    PROCESS_FORM_RECORD_STATUS_OPTIONS.find((item) => item.value === status)
      ?.label ||
    status ||
    '-'
  );
}

function recordStatusColor(status?: string) {
  const map: Record<string, string> = {
    CONFIRMED: 'green',
    DRAFT: 'default',
    SUBMITTED: 'blue',
    VOID: 'red',
  };
  return map[String(status || '')] || 'default';
}

function resultStatusText(status?: string) {
  if (status === 'OK') return 'OK';
  if (status === 'NG') return 'NG';
  return status || '-';
}

function resultStatusColor(status?: string) {
  if (status === 'OK') return 'green';
  if (status === 'NG') return 'red';
  return 'default';
}

function normalizeResultFlag(value?: string) {
  const text = String(value || '')
    .trim()
    .toUpperCase();
  if (!text) return '';
  if (
    text.includes('NG') ||
    text.includes('×') ||
    text.includes('不合格') ||
    text.includes('异常')
  )
    return 'NG';
  if (
    text.includes('OK') ||
    text.includes('√') ||
    text.includes('合格') ||
    text.includes('正常')
  )
    return 'OK';
  return '';
}

function getErrorMessage(error: any, fallback: string) {
  return String(
    error?.response?.data?.msg ||
      error?.response?.data?.message ||
      error?.data?.msg ||
      error?.data?.message ||
      error?.message ||
      fallback,
  );
}

async function loadSelfCheckStationFormDetail(
  record?: MesHcProcessFormApi.Record | null,
) {
  if (record?.templateId) {
    return await getStationFormDetail(record.templateId);
  }
  const templateCode = firstText(record?.templateCode);
  if (templateCode) {
    const page = await getStationFormPage({
      formCode: templateCode,
      pageNo: 1,
      pageSize: 1,
      status: 1,
    });
    const form = page.list?.[0];
    if (form?.id) {
      return await getStationFormDetail(form.id);
    }
  }
  const forms = await loadEnabledSelfCheckStationForms(
    CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
    '未找到已启用的裁切成品检验自检记录动态表单，请先执行配置脚本',
  );
  const form = forms[0];
  if (!form?.id) {
    throw new Error('未找到已启用的裁切成品检验自检记录动态表单，请先执行配置脚本');
  }
  return await getStationFormDetail(form.id!);
}

async function handleCreate() {
  try {
    const choice = await selectTodaySelfCheckRecord({
      defaultFilter: 'ALL',
      emptyMessage:
        '未找到已启用的裁切成品检验自检记录动态表单，请先执行配置脚本',
      formCodes: CUT_ROUND_FQC_REQUIRED_SELF_CHECK_FORM_CODES,
      forceCreate: true,
      processCode: CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
      selectTitle: '选择裁切成品检验自检记录表单',
      showFilters: false,
    });
    if (!choice) return;
    if (choice.mode !== 'create') return;
    runtimeFillForm.value = choice.form;
    runtimeFillOpen.value = true;
  } catch (error: any) {
    message.error(getErrorMessage(error, '加载裁切成品检验自检记录模板失败'));
  }
}

async function handleRuntimeFillSuccess() {
  await gridApi.query();
}

function buildRuntimeDetailRows(record?: MesHcProcessFormApi.Record | null) {
  return (record?.items || []).map((item, index) => ({
    actualValue: firstText(item.actualValue),
    actualValue2: firstText(item.actualValue2),
    abnormalRemark: firstText(item.abnormalRemark),
    fieldLabel: firstText(item.fieldLabel),
    id: item.id || index + 1,
    itemCategory: firstText(item.itemCategory),
    itemName: firstText(item.fieldLabel, item.fieldKey),
    resultFlag: firstText(item.resultFlag, 'OK'),
    seq: item.itemSeq || index + 1,
    sortNo: item.itemSeq || index + 1,
    standardText: firstText(item.standardText, '-'),
    stepNode: firstText(item.stepNode),
    templateItemId: item.templateItemId,
    valueMode: firstText(item.valueMode, 'TEXT'),
  }));
}

function buildRuntimeHeaderData(record?: MesHcProcessFormApi.Record | null) {
  const header = {
    ...safeParseJson<Record<string, any>>(record?.headerDataJson),
  };
  header.formCode = firstText(
    header.formCode,
    record?.templateCode,
  );
  header.formName = firstText(
    header.formName,
    record?.templateName,
    PROCESS_SELF_CHECK_NAME,
  );
  header.formType = firstText(
    header.formType,
    record?.formType,
    PROCESS_SELF_CHECK_TYPE,
  );
  header.formTypeName = firstText(
    header.formTypeName,
    record?.formTypeName,
    PROCESS_SELF_CHECK_TYPE_NAME,
  );
  header.processCode = firstText(
    header.processCode,
    record?.processCode,
    CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
  );
  header.processName = firstText(
    header.processName,
    record?.processName,
    PROCESS_SELF_CHECK_NAME,
  );
  header.recordDate = firstDateText(
    header.recordDate,
    record?.recordDate,
    todayDateText(),
  );
  header.productionDate = firstDateText(
    header.productionDate,
    header.recordDate,
  );
  header.recorder = firstText(
    header.recorder,
    header.recorderName,
    header.recordUserName,
    header.checkerName,
    header.fillUserName,
    record?.fillUserName,
  );
  header.recorderName = firstText(header.recorderName, header.recorder);
  header.recordUserName = firstText(header.recordUserName, header.recorder);
  header.checkerName = firstText(header.checkerName, header.recorder);
  header.fillUserName = firstText(header.fillUserName, header.recorder);
  header.recorderTime = firstText(
    header.recorderTime,
    header.recordTime,
    header.checkerTime,
    record?.fillTime,
  );
  header.recordTime = firstText(header.recordTime, header.recorderTime);
  header.checkerTime = firstText(header.checkerTime, header.recorderTime);
  header.confirmer = firstText(
    header.confirmer,
    header.confirmerName,
    header.confirmUserName,
    record?.confirmUserName,
  );
  header.confirmerName = firstText(header.confirmerName, header.confirmer);
  header.confirmUserName = firstText(header.confirmUserName, header.confirmer);
  header.confirmerTime = firstDateText(
    header.confirmerTime,
    header.confirmTime,
    record?.confirmTime,
  );
  header.confirmTime = firstDateText(header.confirmTime, header.confirmerTime);
  header.docStatus = firstText(record?.recordStatus, header.docStatus, 'DRAFT');
  header.recordStatus = firstText(
    record?.recordStatus,
    header.recordStatus,
    'DRAFT',
  );
  if (!Array.isArray(header.previewDetails)) {
    header.previewDetails = buildRuntimeDetailRows(record);
  }
  return header;
}

async function loadRuntimeSchemaForRecord(record: MesHcProcessFormApi.Record) {
  const form = await loadSelfCheckStationFormDetail(record);
  return safeParseJson<Record<string, any>>(form.schemaJson) || {};
}

async function openRuntimeView(
  row: MesHcProcessFormApi.Record,
  mode: RuntimeViewMode = 'view',
) {
  if (!row.id) return;
  runtimeViewLoading.value = true;
  runtimeViewVisible.value = true;
  runtimeViewMode.value = mode;
  try {
    const detail = await getProcessFormRecordDetail(row.id);
    runtimeViewRecord.value = detail;
    runtimeViewHeaderData.value = buildRuntimeHeaderData(detail);
    runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
    await nextTick();
    runtimeRendererRef.value?.setHeaderData?.(runtimeViewHeaderData.value);
  } catch (error: any) {
    runtimeViewVisible.value = false;
    message.error(getErrorMessage(error, '加载裁切成品检验自检记录详情失败'));
  } finally {
    runtimeViewLoading.value = false;
  }
}

function closeRuntimeView() {
  runtimeViewVisible.value = false;
  runtimeViewRecord.value = null;
  runtimeViewHeaderData.value = {};
  runtimeViewSchema.value = {};
  runtimeViewMode.value = 'view';
}

function buildRuntimeUpdatePayload(
  record: MesHcProcessFormApi.Record,
  headerData: Record<string, any>,
  items?: MesHcProcessFormApi.RecordItem[],
) {
  const { template: _template, ...payload } =
    record as MesHcProcessFormApi.Record & { template?: unknown };
  const runtimeItems = items || record.items || [];
  const headerResult = normalizeResultFlag(
    firstText(
      headerData.inspectionResult,
      headerData.result,
      headerData.checkResult,
    ),
  );
  const resultStatus =
    headerResult ||
    (runtimeItems.some((item) => normalizeResultFlag(item.resultFlag) === 'NG')
      ? 'NG'
      : 'OK');
  const recorderName = firstText(
    headerData.recorder,
    headerData.recorderName,
    headerData.recordUserName,
    headerData.checkerName,
    headerData.fillUserName,
  );
  const recorderTime = firstText(
    headerData.recorderTime,
    headerData.recordTime,
    headerData.checkerTime,
  );
  const confirmerName = firstText(
    headerData.confirmer,
    headerData.confirmerName,
    headerData.confirmUserName,
  );
  const confirmerTime = firstText(
    headerData.confirmerTime,
    headerData.confirmTime,
  );
  return {
    ...payload,
    confirmTime: confirmerTime || record.confirmTime,
    confirmUserName: confirmerName || record.confirmUserName,
    fillTime: recorderTime || record.fillTime,
    fillUserName: recorderName || record.fillUserName,
    formType: firstText(
      record.formType,
      headerData.formType,
      PROCESS_SELF_CHECK_TYPE,
    ),
    formTypeName: firstText(
      record.formTypeName,
      headerData.formTypeName,
      PROCESS_SELF_CHECK_TYPE_NAME,
    ),
    headerDataJson: JSON.stringify(headerData),
    items: runtimeItems,
    processCode: CUT_ROUND_FQC_SELF_CHECK_PROCESS_CODE,
    processName: PROCESS_SELF_CHECK_NAME,
    recordDate: firstDateText(
      headerData.recordDate,
      record.recordDate,
      todayDateText(),
    ),
    recordStatus: hasManualConfirmHeader(headerData)
      ? 'CONFIRMED'
      : record.recordStatus,
    resultStatus,
    templateCode: firstText(record.templateCode, headerData.formCode),
    templateName: firstText(
      record.templateName,
      headerData.formName,
      PROCESS_SELF_CHECK_NAME,
    ),
  } as MesHcProcessFormApi.Record;
}

async function refreshRuntimeViewRecord(id: number) {
  const detail = await getProcessFormRecordDetail(id);
  runtimeViewRecord.value = detail;
  runtimeViewHeaderData.value = buildRuntimeHeaderData(detail);
  runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
  await gridApi.query();
  return detail;
}

function hasManualConfirmHeader(headerData: Record<string, any>) {
  return !!(
    firstText(
      headerData.confirmer,
      headerData.confirmerName,
      headerData.confirmUserName,
    ) && firstText(headerData.confirmerTime, headerData.confirmTime)
  );
}

function normalizeRuntimeHeaderForSave(headerData: Record<string, any>) {
  headerData.recordDate = firstDateText(headerData.recordDate, todayDateText());
  headerData.productionDate = firstDateText(
    headerData.productionDate,
    headerData.recordDate,
  );
  const recorderName = firstText(
    headerData.recorder,
    headerData.recorderName,
    headerData.recordUserName,
    headerData.checkerName,
    headerData.fillUserName,
  );
  if (recorderName) {
    headerData.recorder = recorderName;
    headerData.recorderName = recorderName;
    headerData.recordUserName = recorderName;
    headerData.checkerName = recorderName;
    headerData.fillUserName = recorderName;
  }
  const confirmerName = firstText(
    headerData.confirmer,
    headerData.confirmerName,
    headerData.confirmUserName,
  );
  const confirmerTime = firstDateText(
    headerData.confirmerTime,
    headerData.confirmTime,
  );
  if (confirmerName && confirmerTime) {
    headerData.confirmer = confirmerName;
    headerData.confirmerName = confirmerName;
    headerData.confirmUserName = confirmerName;
    headerData.confirmerTime = confirmerTime;
    headerData.confirmTime = confirmerTime;
    headerData.docStatus = 'CONFIRMED';
    headerData.recordStatus = 'CONFIRMED';
  } else {
    headerData.docStatus = 'DRAFT';
    headerData.recordStatus = 'DRAFT';
  }
}

async function saveRuntimeView() {
  const record = runtimeViewRecord.value;
  if (!record?.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.warning('已确认的裁切成品检验自检记录不允许修改');
    return;
  }
  runtimeViewSaving.value = true;
  try {
    const runtimeHeader =
      runtimeRendererRef.value?.getHeaderData?.() ||
      runtimeViewHeaderData.value;
    normalizeRuntimeHeaderForSave(runtimeHeader);
    const runtimeItems =
      runtimeRendererRef.value?.buildRecordItems?.() || record.items || [];
    const payload = buildRuntimeUpdatePayload(
      record,
      runtimeHeader,
      runtimeItems,
    );
    await updateProcessFormRecord(payload);
    const confirmed = hasManualConfirmHeader(runtimeHeader);
    message.success(confirmed ? '保存并确认提交成功' : '保存提交成功');
    await refreshRuntimeViewRecord(record.id);
    if (confirmed) {
      runtimeViewMode.value = 'view';
    }
  } catch (error: any) {
    message.error(getErrorMessage(error, '保存裁切成品检验自检记录失败'));
  } finally {
    runtimeViewSaving.value = false;
  }
}

async function confirmRuntimeRecord(record: MesHcProcessFormApi.Record) {
  if (!record.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该裁切成品检验自检记录已确认');
    return;
  }
  const currentHeader =
    runtimeViewRecord.value?.id === record.id
      ? runtimeRendererRef.value?.getHeaderData?.() ||
        runtimeViewHeaderData.value
      : buildRuntimeHeaderData(record);
  if (hasManualConfirmHeader(currentHeader)) {
    await executeConfirmRuntimeRecord(record);
    return;
  }
  runtimeConfirmAuthRecord.value = record;
  runtimeConfirmAuthVisible.value = true;
}

async function executeConfirmRuntimeRecord(
  record: MesHcProcessFormApi.Record,
  userInfo?: any,
) {
  const confirmUserId = userInfo ? Number(userInfo.userId || 0) : 0;
  if (
    userInfo &&
    (!Number.isSafeInteger(confirmUserId) || confirmUserId <= 0)
  ) {
    message.warning('未识别到认证确认人，请重新进行身份认证');
    return;
  }
  const confirmedRecordId = record.id!;
  runtimeViewConfirming.value = true;
  try {
    const detail = await getProcessFormRecordDetail(confirmedRecordId);
    const isCurrentEditingRecord =
      runtimeViewRecord.value?.id === confirmedRecordId;
    const header = isCurrentEditingRecord
      ? runtimeRendererRef.value?.getHeaderData?.() ||
        runtimeViewHeaderData.value
      : buildRuntimeHeaderData(detail);
    const items = isCurrentEditingRecord
      ? runtimeRendererRef.value?.buildRecordItems?.() || detail.items || []
      : detail.items || [];
    normalizeRuntimeHeaderForSave(header);
    const hasManualConfirmation = hasManualConfirmHeader(header);
    if (!hasManualConfirmation && !userInfo) {
      runtimeConfirmAuthRecord.value = record;
      runtimeConfirmAuthVisible.value = true;
      return;
    }
    header.inspectionResult = firstText(
      header.inspectionResult,
      detail.resultStatus,
      'OK',
    );
    const payload = buildRuntimeUpdatePayload(detail, header, items);
    await updateProcessFormRecord(payload);
    if (!hasManualConfirmation) {
      await confirmProcessFormRecordBySigner({
        id: confirmedRecordId,
        confirmUserId,
      });
    }
    message.success('确认提交成功');
    if (runtimeViewRecord.value?.id === confirmedRecordId) {
      await refreshRuntimeViewRecord(confirmedRecordId);
      runtimeViewMode.value = 'view';
    } else {
      await gridApi.query();
    }
  } catch (error: any) {
    message.error(getErrorMessage(error, '确认裁切成品检验自检记录失败'));
  } finally {
    runtimeViewConfirming.value = false;
  }
}

async function handleRuntimeConfirmAuthSuccess(userInfo: any) {
  const record = runtimeConfirmAuthRecord.value;
  runtimeConfirmAuthVisible.value = false;
  runtimeConfirmAuthRecord.value = null;
  if (!record) return;
  await executeConfirmRuntimeRecord(record, userInfo);
}

function handleRuntimeConfirmAuthCancel() {
  runtimeConfirmAuthRecord.value = null;
}

async function handleDeleteRecord(row: MesHcProcessFormApi.Record) {
  if (!row.id) return;
  try {
    await deleteProcessFormRecord(row.id);
    message.success('删除成功');
    await gridApi.query();
  } catch (error: any) {
    message.error(getErrorMessage(error, '删除裁切成品检验自检记录失败'));
  }
}

function openAttachment(attachment: Record<string, any>) {
  const url = String(attachment?.url || attachment?.path || '').trim();
  if (url) window.open(url, '_blank');
}
</script>

<template>
  <Page auto-content-height class="relative">
    <StationFormRuntimeFillModal
      v-model:open="runtimeFillOpen"
      :form="runtimeFillForm"
      :initial-params="runtimeInitialParams"
      skip-business-param-step
      @success="handleRuntimeFillSuccess"
    />

    <AuthModal
      v-model:visible="runtimeConfirmAuthVisible"
      action-name="确认裁切成品检验自检记录"
      auth-mode="username"
      @cancel="handleRuntimeConfirmAuthCancel"
      @success="handleRuntimeConfirmAuthSuccess"
    />

    <Modal
      :footer="null"
      :open="runtimeViewVisible"
      :title="null"
      :z-index="3220"
      destroy-on-close
      width="100vw"
      wrap-class-name="hc-pass-work-modal cut-round-fqc-self-check-runtime-view-modal"
      @cancel="closeRuntimeView"
    >
      <div class="cut-round-fqc-self-check-runtime-view">
        <div class="cut-round-fqc-self-check-runtime-view__header">
          <div>
            <h3>{{ runtimeViewTitle }}</h3>
            <div class="cut-round-fqc-self-check-runtime-view__meta">
              <span v-for="item in runtimeViewRecordMeta" :key="item">{{
                item
              }}</span>
            </div>
          </div>
          <div class="cut-round-fqc-self-check-runtime-view__actions">
            <Button
              v-if="!runtimeViewReadOnly"
              :loading="runtimeViewSaving"
              type="primary"
              @click="saveRuntimeView()"
            >
              保存
            </Button>
            <Button
              v-if="
                runtimeViewRecord &&
                runtimeViewRecord.recordStatus !== 'CONFIRMED'
              "
              :loading="runtimeViewConfirming"
              danger
              type="primary"
              @click="confirmRuntimeRecord(runtimeViewRecord)"
            >
              确认
            </Button>
            <Button @click="closeRuntimeView">关闭</Button>
          </div>
        </div>
        <div class="cut-round-fqc-self-check-runtime-view__body">
          <Spin :spinning="runtimeViewLoading">
            <StationFormRuntimeRenderer
              v-if="runtimeViewRecord && !runtimeViewLoading"
              ref="runtimeRendererRef"
              v-model:header-data="runtimeViewHeaderData"
              :form-name="runtimeViewTitle"
              :items="runtimeViewItems"
              :readonly="runtimeViewReadOnly"
              :record-meta="runtimeViewRecordMeta"
              :schema="runtimeViewSchema"
              @open-attachment="openAttachment"
            />
            <div v-else class="cut-round-fqc-self-check-runtime-view__loading">
              正在加载裁切成品检验自检记录详情...
            </div>
          </Spin>
        </div>
      </div>
    </Modal>

    <div class="flex h-full flex-col">
      <Grid table-title="裁切成品检验自检记录表">
        <template #toolbar-tools>
          <div
            class="mr-2 flex flex-wrap items-center justify-end gap-2 border-r border-gray-200 pr-3"
          >
            <Button
              v-access:code="['mes:cut-round-fqc-self-check:create']"
              type="primary"
              @click="handleCreate"
            >
              <IconifyIcon icon="lucide:plus" class="mr-1" /> 新增填写
            </Button>
            <Button @click="gridApi.query()">
              <IconifyIcon icon="lucide:refresh-cw" class="mr-1" /> 刷新
            </Button>
          </div>
        </template>

        <template #recordStatus="{ row }">
          <Tag :color="recordStatusColor(row.recordStatus)" class="!m-0">
            {{ recordStatusText(row.recordStatus) }}
          </Tag>
        </template>

        <template #resultStatus="{ row }">
          <Tag :color="resultStatusColor(row.resultStatus)" class="!m-0">
            {{ resultStatusText(row.resultStatus) }}
          </Tag>
        </template>

        <template #actions="{ row }">
          <TableAction
            :actions="[
              {
                label: '查看',
                type: 'link',
                icon: ACTION_ICON.VIEW,
                auth: ['mes:cut-round-fqc-self-check:query'],
                onClick: () => openRuntimeView(row, 'view'),
              },
              {
                label: '修改',
                type: 'link',
                icon: ACTION_ICON.EDIT,
                auth: ['mes:cut-round-fqc-self-check:update'],
                disabled: row.recordStatus === 'CONFIRMED',
                onClick: () => openRuntimeView(row, 'edit'),
              },
              {
                label: '确认',
                type: 'link',
                icon: 'lucide:badge-check',
                auth: ['mes:cut-round-fqc-self-check:confirm'],
                disabled: row.recordStatus === 'CONFIRMED',
                onClick: () => confirmRuntimeRecord(row),
              },
              {
                label: '删除',
                type: 'link',
                icon: ACTION_ICON.DELETE,
                auth: ['mes:cut-round-fqc-self-check:delete'],
                danger: true,
                disabled: row.recordStatus === 'CONFIRMED',
                popConfirm: {
                  title: '确认删除当前裁切成品检验自检记录吗？',
                  confirm: () => handleDeleteRecord(row),
                },
              },
            ]"
          />
        </template>
      </Grid>
    </div>
  </Page>
</template>

<style scoped>
.cut-round-fqc-self-check-runtime-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background: #edf2f7;
}

.cut-round-fqc-self-check-runtime-view__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 8px 14px;
  background: #e3ebf4;
  border-bottom: 1px solid #c8d4e2;
}

.cut-round-fqc-self-check-runtime-view__header h3 {
  margin: 0;
  color: #0f2b46;
  font-size: 16px;
  font-weight: 700;
}

.cut-round-fqc-self-check-runtime-view__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 4px;
  color: #31506f;
  font-size: 13px;
}

.cut-round-fqc-self-check-runtime-view__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.cut-round-fqc-self-check-runtime-view__body {
  flex: 1;
  min-height: 0;
  padding: 10px;
  overflow: auto;
}

.cut-round-fqc-self-check-runtime-view__body :deep(.ant-spin-nested-loading),
.cut-round-fqc-self-check-runtime-view__body :deep(.ant-spin-container) {
  height: 100%;
}

.cut-round-fqc-self-check-runtime-view__body :deep(.station-form-runtime) {
  min-height: calc(100vh - 76px);
}

.cut-round-fqc-self-check-runtime-view__loading {
  padding: 36px;
  color: #66788a;
  text-align: center;
}

:global(.cut-round-fqc-self-check-runtime-view-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
  margin: 0;
}

:global(.cut-round-fqc-self-check-runtime-view-modal .ant-modal-content) {
  min-height: 100vh;
  padding: 0;
  border-radius: 0;
}

:global(.cut-round-fqc-self-check-runtime-view-modal .ant-modal-body) {
  padding: 0;
}

:global(.cut-round-fqc-self-check-runtime-view-modal .ant-modal-close) {
  display: none;
}

@media (max-width: 900px) {
  .cut-round-fqc-self-check-runtime-view__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
