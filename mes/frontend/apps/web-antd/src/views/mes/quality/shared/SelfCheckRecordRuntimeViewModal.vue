<script lang="ts" setup>
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, nextTick, ref, watch } from 'vue';

import { Button, message, Modal, Spin } from 'ant-design-vue';

import {
  confirmProcessFormRecordBySigner,
  getProcessFormRecordDetail,
  updateProcessFormRecord,
} from '#/api/mes/hc/processform';
import {
  getStationFormDetail,
  getStationFormPage,
} from '#/api/mes/hc/stationform';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

import { todayDateText } from './selfCheckStationFormGroup';

defineOptions({ name: 'SelfCheckRecordRuntimeViewModal' });

const props = withDefaults(
  defineProps<{
    confirmAuthActionName?: string;
    emptyMessage: string;
    fallbackFormType?: string;
    fallbackFormTypeName?: string;
    fallbackProcessCode: string;
    fallbackProcessName: string;
    open: boolean;
    record?: MesHcProcessFormApi.Record | null;
    title?: string;
  }>(),
  {
    fallbackFormType: 'STARTUP_CHECK',
    fallbackFormTypeName: '设备开机点检',
    record: null,
    title: '',
  },
);

const emit = defineEmits<{
  success: [];
  'update:open': [open: boolean];
}>();

const loading = ref(false);
const confirming = ref(false);
const confirmAuthVisible = ref(false);
const runtimeRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeHeaderData = ref<Record<string, any>>({});
const runtimeSchema = ref<Record<string, any>>({});
const runtimeRendererRef = ref<any>();

const runtimeTitle = computed(
  () =>
    runtimeRecord.value?.templateName ||
    props.title ||
    props.fallbackProcessName,
);
const runtimeRecordMeta = computed(() => [
  `记录编号：${runtimeRecord.value?.recordNo || '-'}`,
  `点检日期：${formatDateText(runtimeRecord.value?.recordDate)}`,
  `状态：${recordStatusText(runtimeRecord.value?.recordStatus)}`,
  `结果：${resultStatusText(runtimeRecord.value?.resultStatus)}`,
]);
const runtimeItems = computed<MesHcStationFormApi.StationFormItem[]>(() =>
  buildRuntimeDetailRows(runtimeRecord.value).map((row) => ({
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
const canRender = computed(
  () => !!runtimeRecord.value && !!runtimeSchema.value?.runtimeLayout,
);
const canConfirm = computed(
  () =>
    !!runtimeRecord.value?.id &&
    runtimeRecord.value.recordStatus !== 'CONFIRMED',
);
const confirmAuthActionName = computed(
  () => props.confirmAuthActionName || `确认${props.fallbackProcessName}`,
);

watch(
  () => props.open,
  (open) => {
    if (open) {
      void loadRuntimeRecord();
    } else {
      resetState();
    }
  },
);

watch(
  () => props.record?.id,
  () => {
    if (props.open) {
      void loadRuntimeRecord();
    }
  },
);

function close() {
  emit('update:open', false);
}

function resetState() {
  loading.value = false;
  confirming.value = false;
  confirmAuthVisible.value = false;
  runtimeRecord.value = null;
  runtimeHeaderData.value = {};
  runtimeSchema.value = {};
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text && text !== '-') return text;
  }
  return '';
}

function safeParseJson<T = any>(value?: unknown): null | T {
  if (!value) return null;
  if (typeof value === 'object' && !Array.isArray(value)) return value as T;
  try {
    const parsed = JSON.parse(String(value));
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as T)
      : null;
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
  const matched = /^(\d{4})[-/,.年](\d{1,2})[-/,.月](\d{1,2})/u.exec(text);
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
  const map: Record<string, string> = {
    CONFIRMED: '已确认',
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    VOID: '已作废',
  };
  return map[String(status || '')] || status || '-';
}

function resultStatusText(status?: string) {
  if (status === 'OK') return 'OK';
  if (status === 'NG') return 'NG';
  return status || '-';
}

function normalizeResultFlag(value?: unknown) {
  const text = String(value ?? '')
    .trim()
    .toUpperCase();
  if (text === 'OK' || text === 'NG') return text;
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

function buildRuntimeDetailRows(record?: MesHcProcessFormApi.Record | null) {
  return (record?.items || []).map((item, index) => {
    const sourceRow =
      safeParseJson<Record<string, any>>(item.sourceRowJson) || {};
    return {
      actualValue: firstText(item.actualValue, sourceRow.actualValue),
      actualValue2: firstText(item.actualValue2, sourceRow.actualValue2),
      abnormalRemark: firstText(item.abnormalRemark, sourceRow.abnormalRemark),
      fieldLabel: firstText(item.fieldLabel, sourceRow.fieldLabel),
      id: item.id || index + 1,
      itemCategory: firstText(item.itemCategory, sourceRow.itemCategory),
      itemName: firstText(item.fieldLabel, sourceRow.itemName, item.fieldKey),
      resultFlag: firstText(item.resultFlag, sourceRow.resultFlag, 'OK'),
      seq: item.itemSeq || Number(sourceRow.seq) || index + 1,
      sortNo: item.itemSeq || Number(sourceRow.sortNo) || index + 1,
      standardText: firstText(item.standardText, sourceRow.standardText, '-'),
      stepNode: firstText(item.stepNode, sourceRow.stepNode),
      templateItemId: item.templateItemId,
      valueMode: firstText(item.valueMode, sourceRow.valueMode, 'TEXT'),
    };
  });
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

function normalizeRuntimeHeaderForConfirm(headerData: Record<string, any>) {
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
  const confirmerTime = firstText(
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
      props.fallbackFormType,
    ),
    formTypeName: firstText(
      record.formTypeName,
      headerData.formTypeName,
      props.fallbackFormTypeName,
    ),
    headerDataJson: JSON.stringify(headerData),
    items: runtimeItems,
    processCode: firstText(
      record.processCode,
      headerData.processCode,
      props.fallbackProcessCode,
    ),
    processName: firstText(
      record.processName,
      headerData.processName,
      props.fallbackProcessName,
    ),
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
      props.fallbackProcessName,
    ),
  } as MesHcProcessFormApi.Record;
}

function buildRuntimeHeaderData(record?: MesHcProcessFormApi.Record | null) {
  const header = {
    ...(safeParseJson<Record<string, any>>(record?.headerDataJson) || {}),
  };
  header.formCode = firstText(header.formCode, record?.templateCode);
  header.formName = firstText(
    header.formName,
    record?.templateName,
    props.fallbackProcessName,
  );
  header.formType = firstText(
    header.formType,
    record?.formType,
    props.fallbackFormType,
  );
  header.formTypeName = firstText(
    header.formTypeName,
    record?.formTypeName,
    props.fallbackFormTypeName,
  );
  header.processCode = firstText(
    header.processCode,
    record?.processCode,
    props.fallbackProcessCode,
  );
  header.processName = firstText(
    header.processName,
    record?.processName,
    props.fallbackProcessName,
  );
  header.recordDate = firstDateText(header.recordDate, record?.recordDate);
  header.productionDate = firstDateText(
    header.productionDate,
    header.recordDate,
  );
  header.instrumentCode = firstText(
    header.instrumentCode,
    record?.equipmentCode,
  );
  header.instrumentName = firstText(
    header.instrumentName,
    record?.equipmentName,
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
  throw new Error(props.emptyMessage);
}

async function loadRuntimeRecord() {
  if (!props.record?.id) return;
  loading.value = true;
  try {
    const detail = await getProcessFormRecordDetail(props.record.id);
    runtimeRecord.value = detail;
    runtimeHeaderData.value = buildRuntimeHeaderData(detail);
    const form = await loadSelfCheckStationFormDetail(detail);
    runtimeSchema.value = safeParseJson<Record<string, any>>(form.schemaJson) || {};
    await nextTick();
    runtimeRendererRef.value?.setHeaderData?.(runtimeHeaderData.value);
  } catch (error: any) {
    close();
    message.error(getErrorMessage(error, props.emptyMessage));
  } finally {
    loading.value = false;
  }
}

async function executeConfirmRuntimeRecord(userInfo?: any) {
  const record = runtimeRecord.value;
  if (!record?.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该自检记录已确认');
    return;
  }
  const confirmUserId = userInfo ? Number(userInfo.userId || 0) : 0;
  if (
    userInfo &&
    (!Number.isSafeInteger(confirmUserId) || confirmUserId <= 0)
  ) {
    message.warning('未识别到认证确认人，请重新进行身份认证');
    return;
  }
  confirming.value = true;
  try {
    const detail = await getProcessFormRecordDetail(record.id);
    const header =
      runtimeRendererRef.value?.getHeaderData?.() ||
      runtimeHeaderData.value ||
      buildRuntimeHeaderData(detail);
    const items =
      runtimeRendererRef.value?.buildRecordItems?.() || detail.items || [];
    normalizeRuntimeHeaderForConfirm(header);
    const hasManualConfirmation = hasManualConfirmHeader(header);
    if (!hasManualConfirmation && !userInfo) {
      confirmAuthVisible.value = true;
      return;
    }
    header.inspectionResult = firstText(
      header.inspectionResult,
      detail.resultStatus,
      'OK',
    );
    await updateProcessFormRecord(
      buildRuntimeUpdatePayload(detail, header, items),
    );
    if (!hasManualConfirmation) {
      await confirmProcessFormRecordBySigner({
        confirmUserId,
        id: record.id,
      });
    }
    message.success('确认提交成功');
    emit('success');
    close();
  } catch (error: any) {
    message.error(getErrorMessage(error, '确认自检记录失败'));
  } finally {
    confirming.value = false;
  }
}

function confirmRuntimeRecord() {
  void executeConfirmRuntimeRecord();
}

async function handleConfirmAuthSuccess(userInfo: any) {
  confirmAuthVisible.value = false;
  await executeConfirmRuntimeRecord(userInfo);
}

function openAttachment(attachment: Record<string, any>) {
  const url = String(attachment?.url || attachment?.path || '').trim();
  if (url) window.open(url, '_blank');
}
</script>

<template>
  <AuthModal
    v-model:visible="confirmAuthVisible"
    :action-name="confirmAuthActionName"
    auth-mode="username"
    @cancel="confirmAuthVisible = false"
    @success="handleConfirmAuthSuccess"
  />

  <Modal
    :footer="null"
    :open="open"
    :title="null"
    :z-index="3220"
    destroy-on-close
    width="100vw"
    wrap-class-name="hc-pass-work-modal self-check-runtime-record-view-modal"
    @cancel="close"
  >
    <div class="self-check-runtime-record-view">
      <div class="self-check-runtime-record-view__header">
        <div>
          <h3>{{ runtimeTitle }}</h3>
          <div class="self-check-runtime-record-view__meta">
            <span v-for="item in runtimeRecordMeta" :key="item">{{ item }}</span>
          </div>
        </div>
        <div class="self-check-runtime-record-view__actions">
          <Button
            v-if="canConfirm"
            :loading="confirming"
            danger
            type="primary"
            @click="confirmRuntimeRecord"
          >
            确认
          </Button>
          <Button @click="close">关闭</Button>
        </div>
      </div>
      <div class="self-check-runtime-record-view__body">
        <Spin :spinning="loading">
          <StationFormRuntimeRenderer
            v-if="canRender && !loading"
            ref="runtimeRendererRef"
            v-model:header-data="runtimeHeaderData"
            :form-name="runtimeTitle"
            :items="runtimeItems"
            :readonly="true"
            :record-meta="runtimeRecordMeta"
            :schema="runtimeSchema"
            @open-attachment="openAttachment"
          />
          <div v-else class="self-check-runtime-record-view__loading">
            正在加载自检记录详情...
          </div>
        </Spin>
      </div>
    </div>
  </Modal>
</template>

<style scoped>
.self-check-runtime-record-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background: #edf2f7;
}

.self-check-runtime-record-view__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 8px 14px;
  background: #e3ebf4;
  border-bottom: 1px solid #c8d4e2;
}

.self-check-runtime-record-view__header h3 {
  margin: 0;
  color: #001f3f;
  font-size: 18px;
  font-weight: 700;
}

.self-check-runtime-record-view__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 6px;
  color: #35506d;
  font-size: 13px;
}

.self-check-runtime-record-view__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.self-check-runtime-record-view__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.self-check-runtime-record-view__loading {
  padding: 80px 0;
  color: #64748b;
  text-align: center;
}

:global(.self-check-runtime-record-view-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  margin: 0;
  padding-bottom: 0;
}

:global(.self-check-runtime-record-view-modal .ant-modal-content) {
  min-height: 100vh;
  padding: 0;
  border-radius: 0;
}

:global(.self-check-runtime-record-view-modal .ant-modal-body) {
  min-height: 100vh;
  padding: 0;
}

:global(.self-check-runtime-record-view-modal .ant-modal-close) {
  display: none;
}
</style>
