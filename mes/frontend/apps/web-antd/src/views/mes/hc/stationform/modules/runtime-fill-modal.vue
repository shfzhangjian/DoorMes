<script lang="ts" setup>
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';

import { computed, nextTick, reactive, ref, watch } from 'vue';

import { downloadFileFromBlobPart } from '@vben/utils';

import {
  Form as AForm,
  FormItem as AFormItem,
  Button,
  Input,
  message,
  Modal,
  Spin,
} from 'ant-design-vue';

import { getPressSlotProcessParamList, savePressSlotProcessParam, confirmPressSlotProcessParam } from '#/api/mes/hc/execution/press-slot-console';
import { productionCheckRows, productionCheckSchema, isPressSlotProductionCheck } from '#/views/mes/hc/shared/pressSlotProductionCheck';
import { uploadFile } from '#/api/infra/file';
import {
  confirmProcessFormRecordBySigner,
  createProcessFormRecord,
  exportProcessFormRecordLayout,
  importProcessFormRecordLayout,
} from '#/api/mes/hc/processform';
import { getStationFormDetail } from '#/api/mes/hc/stationform';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

import StationFormRuntimeRenderer from './StationFormRuntimeRenderer.vue';

defineOptions({ name: 'HcStationFormRuntimeFillModal' });

const props = defineProps<{
  confirmAuthActionName?: string;
  enableConfirm?: boolean;
  form?: MesHcStationFormApi.StationForm | null;
  initialParams?: Partial<BusinessParams>;
  open: boolean;
  readonlyPassWorkHeader?: boolean;
  saveAuthActionName?: string;
  skipBusinessParamStep?: boolean;
}>();

const emit = defineEmits<{
  success: [];
  'update:open': [open: boolean];
}>();

type RuntimeAuthenticatedAction = 'CONFIRM' | 'SAVE';

type BusinessParams = {
  batchNo?: string;
  checkerName?: string;
  checkerTime?: string;
  confirmer?: string;
  confirmerName?: string;
  confirmUserName?: string;
  endSliceNo?: string;
  equipmentCode?: string;
  equipmentId?: string;
  equipmentName?: string;
  fillUserName?: string;
  frontSliceNo?: string;
  grindingPass?: string;
  materialCode?: string;
  middleSliceNo?: string;
  modelCode?: string;
  month?: string;
  noticeId?: string;
  noticeNo?: string;
  packageBoxQty?: string;
  packageProductQty?: string;
  packagingType?: string;
  packagingTypeName?: string;
  passName?: string;
  planId?: string;
  planNo?: string;
  planOperationId?: string;
  pressSlotSliceNo?: string;
  productSpec?: string;
  recordDate?: string;
  recorder?: string;
  recorderName?: string;
  recorderTime?: string;
  recordTime?: string;
  recordUserName?: string;
  sourceRowId?: string;
  workshop?: string;
};

const loading = ref(false);
const saving = ref(false);
const excelLoading = ref(false);
const paramVisible = ref(false);
const runtimeVisible = ref(false);
const pressSlotExistingRecord = ref<{ id?: number; templateId?: number; recordStatus?: string }>();
const detail = ref<MesHcStationFormApi.StationForm>();
const schema = ref<Record<string, any>>({});
const headerData = ref<Record<string, any>>({});
const runtimeRef = ref<any>();
const excelInputRef = ref<HTMLInputElement>();
const businessParams = reactive<BusinessParams>(createDefaultBusinessParams());
const runtimeAuthVisible = ref(false);
const runtimeAuthenticatedAction = ref<RuntimeAuthenticatedAction>('SAVE');

const paramTitle = computed(
  () => `填写业务参数 - ${props.form?.formName || '动态表单'}`,
);
const runtimeTitle = computed(
  () => `填写${detail.value?.formName || props.form?.formName || '动态表单'}`,
);
const canRender = computed(
  () => !!detail.value && !!schema.value?.runtimeLayout,
);
const recordMeta = computed(() => buildRuntimeRecordMeta());
const runtimeAuthActionName = computed(() =>
  runtimeAuthenticatedAction.value === 'CONFIRM'
    ? props.confirmAuthActionName || '确认点检记录'
    : props.saveAuthActionName || '保存点检记录',
);

watch(
  () => props.open,
  (open) => {
    if (open) {
      startParamStep();
    } else {
      resetState();
    }
  },
);

function createDefaultBusinessParams(): BusinessParams {
  return {
    batchNo: '',
    endSliceNo: '',
    equipmentCode: '',
    equipmentId: '',
    equipmentName: '',
    frontSliceNo: '',
    grindingPass: '',
    materialCode: '',
    middleSliceNo: '',
    modelCode: '',
    month: '',
    noticeId: '',
    noticeNo: '',
    packageBoxQty: '',
    packageProductQty: '',
    packagingType: '',
    packagingTypeName: '',
    passName: '',
    planId: '',
    planNo: '',
    planOperationId: '',
    pressSlotSliceNo: '',
    productSpec: '',
    recordDate: new Date().toISOString().slice(0, 10),
    sourceRowId: '',
    workshop: '',
  };
}

function resetState() {
  loading.value = false;
  saving.value = false;
  excelLoading.value = false;
  paramVisible.value = false;
  runtimeVisible.value = false;
  detail.value = undefined;
  schema.value = {};
  headerData.value = {};
  runtimeAuthVisible.value = false;
  runtimeAuthenticatedAction.value = 'SAVE';
  Object.assign(businessParams, createDefaultBusinessParams());
}

async function startParamStep() {
  resetState();
  if (props.skipBusinessParamStep) {
    runtimeVisible.value = true;
  }
  await loadRuntimeForm();
  if (props.skipBusinessParamStep) {
    await confirmBusinessParams();
    return;
  }
  paramVisible.value = true;
}

function closeAll() {
  paramVisible.value = false;
  runtimeVisible.value = false;
  runtimeAuthVisible.value = false;
  emit('update:open', false);
}

function cancelParamStep() {
  if (runtimeVisible.value) {
    paramVisible.value = false;
    return;
  }
  closeAll();
}

async function loadRuntimeForm() {
  if (!props.form?.id) return;
  loading.value = true;
  try {
    const formDetail = await getStationFormDetail(props.form.id);
    if (formDetail.status !== 1) {
      console.warn('[DEV动态表单运行] form disabled', {
        formCode: formDetail.formCode,
        formId: formDetail.id,
        status: formDetail.status,
      });
      message.warning('当前动态表单已停用，请先启用后再填写');
      closeAll();
      return;
    }
    pressSlotExistingRecord.value = undefined;
    detail.value = formDetail;
    schema.value = parseJsonObject(formDetail.schemaJson);
    if (isPressSlotProductionCheck(schema.value, formDetail.processCode)) schema.value = productionCheckSchema(schema.value);
    const presetHeader = parseJsonObject(formDetail.presetHeaderDataJson);
    if (isPressSlotProductionCheck(schema.value, formDetail.processCode)) presetHeader.previewDetails = productionCheckRows(formDetail.items || [], presetHeader.previewDetails || []);
    Object.assign(
      businessParams,
      createDefaultBusinessParams(),
      buildDefaultBusinessParams(formDetail, schema.value, presetHeader),
      normalizeInitialBusinessParams(props.initialParams),
    );
    headerData.value = buildRuntimeHeader(presetHeader);
  } finally {
    loading.value = false;
  }
}

function normalizeInitialBusinessParams(params?: Partial<BusinessParams>) {
  const normalized: Partial<BusinessParams> = {};
  if (!params) return normalized;
  Object.entries(params).forEach(([key, value]) => {
    const text = firstText(value);
    if (text) {
      (normalized as Record<string, string>)[key] = text;
    }
  });
  return normalized;
}

function parseJsonObject(value?: Record<string, any> | string) {
  if (!value) return {} as Record<string, any>;
  if (typeof value === 'object' && !Array.isArray(value)) return value;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, any>)
      : {};
  } catch {
    return {} as Record<string, any>;
  }
}

function cloneObject<T>(value: T): T {
  try {
    return structuredClone(value ?? {}) as T;
  } catch {
    return value;
  }
}

function firstText(...values: unknown[]) {
  for (const value of values) {
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
}

function readRuntimeMetaValue(field: string) {
  return firstText(
    headerData.value?.[field],
    (businessParams as Record<string, string | undefined>)[field],
    field === 'processName'
      ? detail.value?.processName || props.form?.processName
      : '',
    field === 'triggerTimingName'
      ? detail.value?.triggerTimingName || props.form?.triggerTimingName
      : '',
  );
}

function buildRuntimeRecordMeta() {
  const metaFields = schema.value?.runtimeLayout?.metaFields;
  if (Array.isArray(metaFields) && metaFields.length > 0) {
    return metaFields
      .map((field) => {
        const label = firstText(field.label, field.title, field.field);
        const value = readRuntimeMetaValue(firstText(field.field, field.key));
        return label ? `${label}：${value || '-'}` : '';
      })
      .filter(Boolean);
  }

  const processCode = firstText(
    detail.value?.processCode,
    props.form?.processCode,
    schema.value?.processCode,
  ).toUpperCase();
  if (['FORMULA', 'WET'].includes(processCode)) {
    return [
      `计划号：${firstText(businessParams.planNo, headerData.value?.planNo, '-')}`,
      `型号：${firstText(businessParams.modelCode, headerData.value?.modelCode, '-')}`,
      `批号：${firstText(businessParams.batchNo, headerData.value?.batchNo, headerData.value?.motherBatchNo, '-')}`,
      `当前工序：${firstText(detail.value?.processName, props.form?.processName, '-')}`,
      `执行时机：${firstText(detail.value?.triggerTimingName, props.form?.triggerTimingName, '-')}`,
    ];
  }

  return [
    `计划号：${businessParams.planNo || '-'}`,
    `压槽片号：${businessParams.pressSlotSliceNo || '-'}`,
    '状态：草稿',
  ];
}

function buildDefaultBusinessParams(
  formDetail: MesHcStationFormApi.StationForm,
  runtimeSchema: Record<string, any>,
  presetHeader: Record<string, any>,
): BusinessParams {
  const schemaModelCode = String(
    runtimeSchema.modelCode || runtimeSchema.sourceModelCode || '',
  ).trim();
  const effectiveModelCode =
    schemaModelCode && schemaModelCode !== 'COMMON' ? schemaModelCode : '';
  return {
    batchNo: firstText(presetHeader.batchNo, presetHeader.motherBatchNo),
    endSliceNo: firstText(presetHeader.endSliceNo),
    equipmentCode: firstText(presetHeader.equipmentCode),
    equipmentId: firstText(presetHeader.equipmentId),
    equipmentName: firstText(presetHeader.equipmentName),
    frontSliceNo: firstText(presetHeader.frontSliceNo),
    materialCode: firstText(presetHeader.materialCode),
    middleSliceNo: firstText(presetHeader.middleSliceNo),
    modelCode: firstText(presetHeader.modelCode, effectiveModelCode),
    month: firstText(presetHeader.month, presetHeader.recordMonth),
    noticeId: firstText(presetHeader.noticeId),
    noticeNo: firstText(presetHeader.noticeNo),
    packageBoxQty: firstText(presetHeader.packageBoxQty, presetHeader.boxQty),
    packageProductQty: firstText(
      presetHeader.packageProductQty,
      presetHeader.packageQty,
      presetHeader.quantity,
    ),
    packagingType: firstText(presetHeader.packagingType),
    packagingTypeName: firstText(presetHeader.packagingTypeName),
    planId: firstText(presetHeader.planId),
    planNo: firstText(presetHeader.planNo),
    planOperationId: firstText(presetHeader.planOperationId),
    pressSlotSliceNo: firstText(
      presetHeader.pressSlotSliceNo,
      presetHeader.sliceBatchNo,
    ),
    productSpec: firstText(
      presetHeader.productSpec,
      presetHeader.specification,
    ),
    recordDate: firstText(
      presetHeader.recordDate,
      new Date().toISOString().slice(0, 10),
    ),
    workshop: firstText(presetHeader.workshop),
  };
}

function buildRuntimeHeader(baseHeader: Record<string, any>) {
  const next = cloneObject(baseHeader || {});
  const preserveRuntimeHeader =
    props.skipBusinessParamStep && !!runtimeRef.value;
  next.formCode =
    detail.value?.formCode || props.form?.formCode || next.formCode;
  next.formName =
    detail.value?.formName || props.form?.formName || next.formName;
  next.processCode =
    detail.value?.processCode || props.form?.processCode || next.processCode;
  next.processName =
    detail.value?.processName || props.form?.processName || next.processName;
  if (!preserveRuntimeHeader || !firstText(next.recordDate)) {
    next.recordDate = businessParams.recordDate || next.recordDate;
  }
  if (!preserveRuntimeHeader || !firstText(next.productionDate)) {
    next.productionDate = businessParams.recordDate || next.productionDate;
  }
  next.modelCode = businessParams.modelCode || next.modelCode;
  next.materialCode = businessParams.materialCode || next.materialCode;
  next.batchNo = businessParams.batchNo || next.batchNo;
  next.motherBatchNo = businessParams.batchNo || next.motherBatchNo;
  next.month = businessParams.month || next.month;
  next.recordMonth = businessParams.month || next.recordMonth;
  next.noticeId = businessParams.noticeId || next.noticeId;
  next.noticeNo = businessParams.noticeNo || next.noticeNo;
  next.packageBoxQty = businessParams.packageBoxQty || next.packageBoxQty;
  next.packageProductQty =
    businessParams.packageProductQty || next.packageProductQty;
  next.packageQty = businessParams.packageProductQty || next.packageQty;
  next.packagingType = businessParams.packagingType || next.packagingType;
  next.packagingTypeName =
    businessParams.packagingTypeName || next.packagingTypeName;
  next.planId = toNumberOrText(businessParams.planId) || next.planId;
  next.planNo = businessParams.planNo || next.planNo;
  next.planOperationId =
    toNumberOrText(businessParams.planOperationId) || next.planOperationId;
  next.pressSlotSliceNo =
    businessParams.pressSlotSliceNo || next.pressSlotSliceNo;
  next.equipmentId =
    toNumberOrText(businessParams.equipmentId) || next.equipmentId;
  next.equipmentCode = businessParams.equipmentCode || next.equipmentCode;
  next.equipmentName = businessParams.equipmentName || next.equipmentName;
  next.productSpec = businessParams.productSpec || next.productSpec;
  next.grindingPass = businessParams.grindingPass || next.grindingPass;
  next.passName = businessParams.passName || next.passName;
  next.sourceRowId = businessParams.sourceRowId || next.sourceRowId;
  next.recorder = businessParams.recorder || next.recorder;
  next.recorderName = businessParams.recorderName || next.recorderName;
  next.recordUserName = businessParams.recordUserName || next.recordUserName;
  next.checkerName = businessParams.checkerName || next.checkerName;
  next.fillUserName = businessParams.fillUserName || next.fillUserName;
  next.confirmer = businessParams.confirmer || next.confirmer;
  next.confirmerName = businessParams.confirmerName || next.confirmerName;
  next.confirmUserName = businessParams.confirmUserName || next.confirmUserName;
  next.workshop = businessParams.workshop || next.workshop;
  next.machine =
    businessParams.equipmentCode || next.machine || next.equipmentCode;
  next.mixerEquipmentCode =
    businessParams.equipmentCode ||
    next.mixerEquipmentCode ||
    next.equipmentCode;
  next.defoamingEquipmentCode =
    businessParams.equipmentCode ||
    next.defoamingEquipmentCode ||
    next.equipmentCode;
  next.processName =
    detail.value?.processName || props.form?.processName || next.processName;
  next.triggerTimingName =
    detail.value?.triggerTimingName ||
    props.form?.triggerTimingName ||
    next.triggerTimingName;
  next.docStatus = 'DRAFT';
  next.recordStatus = 'DRAFT';
  hydratePreviewDetails(next);
  return next;
}

function buildNowText() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

function applyRuntimeSaveRecorderTime(header: Record<string, any>) {
  if (
    !firstText(
      header.recorder,
      header.recorderName,
      header.recordUserName,
      header.checkerName,
      header.fillUserName,
    )
  ) {
    return header;
  }
  const now = buildNowText();
  header.recorderTime = now;
  header.recordTime = now;
  header.checkerTime = now;
  return header;
}

function toNumberOrText(value?: string) {
  const text = String(value ?? '').trim();
  if (!text) return undefined;
  const numberValue = Number(text);
  return Number.isFinite(numberValue) ? numberValue : text;
}

function hydratePreviewDetails(target: Record<string, any>) {
  const rows = Array.isArray(target.previewDetails)
    ? target.previewDetails
    : [];
  if (rows.length === 0) return;
  const sliceNoMap: Record<string, string | undefined> = {
    后段: businessParams.endSliceNo,
    前段: businessParams.frontSliceNo,
    中段: businessParams.middleSliceNo,
  };
  rows.forEach((row: Record<string, any>, index: number) => {
    const sampleName = String(
      row.samplePositionName || row.samplePosition || '',
    ).trim();
    row.seq = row.seq || index + 1;
    row.sortNo = row.sortNo || index + 1;
    if (!row.sliceBatchNo) {
      row.sliceBatchNo =
        sliceNoMap[sampleName] ||
        [
          businessParams.frontSliceNo,
          businessParams.middleSliceNo,
          businessParams.endSliceNo,
        ][index] ||
        '';
    }
  });
}

function applyBusinessParams(silent = false) {
  headerData.value = buildRuntimeHeader(
    runtimeRef.value?.getHeaderData?.() || headerData.value,
  );
  runtimeRef.value?.setHeaderData?.(headerData.value);
  if (!silent) {
    message.success('业务参数已应用到运行表单');
  }
}

async function confirmBusinessParams() {
  if (!detail.value) return;
  if (detail.value.status !== 1) {
    message.warning('当前动态表单已停用，请先启用后再填写');
    closeAll();
    return;
  }
  applyBusinessParams(true);
  if (isPressSlotProductionCheck(schema.value, detail.value.processCode)) {
    const sliceNo = firstText(businessParams.pressSlotSliceNo, headerData.value.batchNo);
    const operationId = Number(businessParams.planOperationId);
    if (!sliceNo || !operationId) { message.warning('请填写计划工序 ID 和送检片号'); return; }
    const rows = await getPressSlotProcessParamList(operationId, { formType: 'PRODUCTION_CHECK', productionBatchNo: sliceNo });
    const candidates = rows.filter((row) => (row.firstInspectionSliceNo || row.productionBatchNo || '').toUpperCase() === sliceNo.toUpperCase());
    if (candidates.length > 1) { message.error('当前片号存在多份点检记录，请核查'); return; }
    const existed = candidates[0];
    pressSlotExistingRecord.value = existed;
    if (existed) {
      if (existed.templateId !== detail.value.id) { message.warning('该片已有其他模板的点检记录，请从压槽报工的对应片号打开'); return; }
      schema.value = productionCheckSchema(existed.runtimeSchema || schema.value);
      headerData.value = { ...headerData.value, ...existed.headerData, batchNo: sliceNo,
        previewDetails: (existed.items || []).map((item) => ({ ...item, standardText: item.standardValue, resultFlag: item.checkResult })),
      };
    } else headerData.value.batchNo = sliceNo;
  }
  paramVisible.value = false;
  runtimeVisible.value = true;
  await nextTick();
  runtimeRef.value?.setHeaderData?.(headerData.value);
}

function reopenBusinessParams() {
  if (props.skipBusinessParamStep) return;
  paramVisible.value = true;
}

function resolveFormType() {
  const runtimeSchema = schema.value || {};
  return firstText(
    runtimeSchema.formType,
    runtimeSchema.processFormType,
    runtimeSchema.runtimeFormType,
    runtimeSchema.businessBinding?.formType,
    (detail.value?.formName || props.form?.formName || '').includes('中间品')
      ? 'INTERMEDIATE_RECORD'
      : '',
    'OTHER',
  );
}

function resolveFormTypeName(formType: string) {
  const nameMap: Record<string, string> = {
    INTERMEDIATE_RECORD: '中间品记录',
    OTHER: '其他',
    PROCESS_PARAM: '工艺参数',
    PRODUCTION_CHECK: '生产点检',
    STATION_RECORD: '点检清洁',
  };
  return firstText(
    schema.value.formTypeName,
    schema.value.processFormTypeName,
    nameMap[formType],
    formType,
  );
}

function buildPayload(): MesHcProcessFormApi.Record {
  if (!isPressSlotProductionCheck(schema.value, detail.value?.processCode)) applyBusinessParams(true);
  const runtimeHeader = runtimeRef.value?.getHeaderData?.() || headerData.value;
  applyRuntimeSaveRecorderTime(runtimeHeader);
  const formType = resolveFormType();
  const processCode = firstText(
    detail.value?.processCode,
    props.form?.processCode,
    schema.value.processCode,
    'PRESS_SLOT',
  );
  const processName = firstText(
    detail.value?.processName,
    props.form?.processName,
    schema.value.processName,
    '压槽',
  );
  const modelCode = firstText(
    businessParams.modelCode,
    runtimeHeader.modelCode,
    schema.value.modelCode,
    'COMMON',
  );
  const context = {
    businessBinding: schema.value.businessBinding || {},
    businessParams: cloneObject(businessParams),
    formCode: detail.value?.formCode || props.form?.formCode,
    formName: detail.value?.formName || props.form?.formName,
    runtimeLayoutVersion: schema.value.runtimeLayout?.version,
    runtimeSchema: schema.value,
    source: 'station-form-dev-runtime',
  };
  return {
    batchNo: firstText(
      businessParams.batchNo,
      runtimeHeader.batchNo,
      runtimeHeader.motherBatchNo,
    ),
    contextJson: JSON.stringify(context),
    equipmentCode: firstText(
      businessParams.equipmentCode,
      runtimeHeader.equipmentCode,
    ),
    equipmentId: toNumberValue(
      businessParams.equipmentId || runtimeHeader.equipmentId,
    ),
    equipmentName: firstText(
      businessParams.equipmentName,
      runtimeHeader.equipmentName,
    ),
    formType,
    formTypeName: resolveFormTypeName(formType),
    headerDataJson: JSON.stringify(runtimeHeader),
    items: runtimeRef.value?.buildRecordItems?.() || [],
    modelCode,
    modelName: modelCode === 'COMMON' ? '通用' : modelCode,
    planId: toNumberValue(businessParams.planId || runtimeHeader.planId),
    planNo: firstText(businessParams.planNo, runtimeHeader.planNo),
    planOperationId: toNumberValue(
      businessParams.planOperationId || runtimeHeader.planOperationId,
    ),
    processCode,
    processName,
    recordDate: props.skipBusinessParamStep
      ? firstText(
          runtimeHeader.recordDate,
          businessParams.recordDate,
          new Date().toISOString().slice(0, 10),
        )
      : firstText(
          businessParams.recordDate,
          runtimeHeader.recordDate,
          new Date().toISOString().slice(0, 10),
        ),
    recordStatus: 'DRAFT',
    remark: `DEV动态表单运行组件填写：${detail.value?.formCode || props.form?.formCode || ''}`,
    templateId: detail.value?.id || props.form?.id,
  };
}

function toNumberValue(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text) return undefined;
  const numberValue = Number(text);
  return Number.isFinite(numberValue) ? numberValue : undefined;
}

function getRuntimeErrorMessage(error: any, fallback: string) {
  return String(
    error?.response?.data?.msg ||
      error?.response?.data?.message ||
      error?.data?.msg ||
      error?.data?.message ||
      error?.message ||
      fallback,
  );
}

function buildRuntimeSaveLogSnapshot(payload?: MesHcProcessFormApi.Record) {
  const renderer = runtimeRef.value || {};
  const runtimeHeader = renderer.getHeaderData?.() || headerData.value || {};
  return {
    businessParams: cloneObject(businessParams),
    canRender: canRender.value,
    detail: {
      formCode: detail.value?.formCode || props.form?.formCode,
      formId: detail.value?.id || props.form?.id,
      formName: detail.value?.formName || props.form?.formName,
      processCode: detail.value?.processCode || props.form?.processCode,
      processName: detail.value?.processName || props.form?.processName,
      status: detail.value?.status,
      triggerTimingCode:
        detail.value?.triggerTimingCode || props.form?.triggerTimingCode,
      triggerTimingName:
        detail.value?.triggerTimingName || props.form?.triggerTimingName,
    },
    headerData: cloneObject(runtimeHeader),
    loading: loading.value,
    modal: {
      open: props.open,
      paramVisible: paramVisible.value,
      runtimeVisible: runtimeVisible.value,
    },
    payload: payload
      ? {
          batchNo: payload.batchNo,
          contextJsonLength: payload.contextJson?.length || 0,
          equipmentCode: payload.equipmentCode,
          formType: payload.formType,
          formTypeName: payload.formTypeName,
          headerDataJsonLength: payload.headerDataJson?.length || 0,
          itemCount: payload.items?.length || 0,
          modelCode: payload.modelCode,
          planNo: payload.planNo,
          processCode: payload.processCode,
          processName: payload.processName,
          recordDate: payload.recordDate,
          recordStatus: payload.recordStatus,
          templateId: payload.templateId,
        }
      : undefined,
    renderer: {
      hasApplyImportedLayout:
        typeof renderer.applyImportedLayout === 'function',
      hasBuildExcelLayout: typeof renderer.buildExcelLayout === 'function',
      hasBuildRecordItems: typeof renderer.buildRecordItems === 'function',
      hasGetHeaderData: typeof renderer.getHeaderData === 'function',
      hasSetHeaderData: typeof renderer.setHeaderData === 'function',
    },
    saving: saving.value,
    schema: {
      hasRuntimeLayout: !!schema.value?.runtimeLayout,
      runtimeLayoutKeys: Object.keys(schema.value?.runtimeLayout || {}),
      schemaKeys: Object.keys(schema.value || {}),
    },
  };
}

function handleSave() {
  if (props.saveAuthActionName) {
    runtimeAuthenticatedAction.value = 'SAVE';
    runtimeAuthVisible.value = true;
    return;
  }
  void executeSave();
}

function handleConfirm() {
  runtimeAuthenticatedAction.value = 'CONFIRM';
  runtimeAuthVisible.value = true;
}

async function handleRuntimeAuthSuccess(userInfo: any) {
  const authenticatedAction = runtimeAuthenticatedAction.value;
  runtimeAuthVisible.value = false;
  await executeSave(userInfo, authenticatedAction === 'CONFIRM');
}

function applyAuthenticatedUser(userInfo: any, confirmAfterSave: boolean) {
  const userId = Number(userInfo?.userId || 0);
  const userName = String(
    userInfo?.empName ||
      userInfo?.nickname ||
      userInfo?.username ||
      userInfo?.empNo ||
      '',
  ).trim();
  const operationTime = buildNowText();
  const runtimeHeader = runtimeRef.value?.getHeaderData?.() || headerData.value;
  runtimeHeader.recorder = userName;
  runtimeHeader.recorderName = userName;
  runtimeHeader.recordUserName = userName;
  runtimeHeader.checkerName = userName;
  runtimeHeader.fillUserName = userName;
  runtimeHeader.recorderTime = operationTime;
  runtimeHeader.recordTime = operationTime;
  runtimeHeader.checkerTime = operationTime;
  businessParams.recorder = userName;
  businessParams.recorderName = userName;
  businessParams.recordUserName = userName;
  businessParams.checkerName = userName;
  businessParams.fillUserName = userName;
  businessParams.recorderTime = operationTime;
  businessParams.recordTime = operationTime;
  businessParams.checkerTime = operationTime;
  if (confirmAfterSave) {
    runtimeHeader.confirmer = userName;
    runtimeHeader.confirmerName = userName;
    runtimeHeader.confirmUserName = userName;
    runtimeHeader.confirmerTime = operationTime;
    runtimeHeader.confirmTime = operationTime;
    businessParams.confirmer = userName;
    businessParams.confirmerName = userName;
    businessParams.confirmUserName = userName;
  }
  headerData.value = { ...runtimeHeader };
  runtimeRef.value?.setHeaderData?.(headerData.value);
  return { operationTime, userId, userName };
}

async function executeSave(userInfo?: any, confirmAfterSave = false) {
  try {
    if (!detail.value?.id) {
      console.warn(
        '[DEV动态表单运行保存] blocked: detail not loaded',
        buildRuntimeSaveLogSnapshot(),
      );
      message.warning('当前动态表单明细未加载完成，不能保存');
      return;
    }
    if (detail.value.status !== 1) {
      console.warn(
        '[DEV动态表单运行保存] blocked: form disabled',
        buildRuntimeSaveLogSnapshot(),
      );
      message.warning('当前动态表单已停用，不能保存填写记录');
      return;
    }
    if (confirmAfterSave && !userInfo?.userId) {
      message.warning('未识别到认证确认人，请重新进行身份认证');
      return;
    }
    if (
      userInfo &&
      (!userInfo.userId ||
        !String(
          userInfo.empName ||
            userInfo.nickname ||
            userInfo.username ||
            userInfo.empNo ||
            '',
        ).trim())
    ) {
      message.warning('未识别到认证填写人，请重新进行身份认证');
      return;
    }
    saving.value = true;
    const authenticatedUser = userInfo
      ? applyAuthenticatedUser(userInfo, confirmAfterSave)
      : undefined;
    const payload = buildPayload();
    if (authenticatedUser) {
      payload.fillUserId = authenticatedUser.userId || undefined;
      payload.fillUserName = authenticatedUser.userName;
      payload.fillTime = authenticatedUser.operationTime;
      if (confirmAfterSave) {
        payload.confirmUserId = authenticatedUser.userId;
        payload.confirmUserName = authenticatedUser.userName;
        payload.confirmTime = authenticatedUser.operationTime;
      }
    }
    if (isPressSlotProductionCheck(schema.value, payload.processCode)) {
      if (pressSlotExistingRecord.value?.recordStatus === 'CONFIRMED') { message.warning('已确认记录不能修改'); return; }
      const header = runtimeRef.value?.getHeaderData() || headerData.value;
      const request = {
        id: pressSlotExistingRecord.value?.id,
        templateId: detail.value.id,
        formType: 'PRODUCTION_CHECK',
        planId: payload.planId, planOperationId: payload.planOperationId,
        modelCode: header.modelCode || payload.modelCode,
        materialCode: header.materialCode,
        productionBatchNo: header.batchNo || businessParams.pressSlotSliceNo,
        headerData: header,
        reportDate: header.productionDate || payload.recordDate,
        fillUserName: authenticatedUser?.userName,
        confirmUserName: confirmAfterSave ? authenticatedUser?.userName : undefined,
        items: (header.previewDetails || []).map((item: any) => ({ ...item, standardValue: item.standardText, checkResult: item.resultFlag })),
      };
      if (confirmAfterSave) await confirmPressSlotProcessParam(request);
      else await savePressSlotProcessParam(request);
      message.success(confirmAfterSave ? '生产点检已保存并确认' : '生产点检已保存');
      emit('success');
      emit('update:open', false);
      return;
    }
    const id = await createProcessFormRecord(payload);
    if (confirmAfterSave && authenticatedUser) {
      await confirmProcessFormRecordBySigner({
        confirmUserId: authenticatedUser.userId,
        id,
      });
      message.success(`保存并确认成功，记录ID：${id}`);
    } else {
      message.success(`保存成功，记录ID：${id}`);
    }
    emit('success');
    closeAll();
  } catch (error: any) {
    console.error('[DEV动态表单运行保存] failed', error);
    message.error(
      getRuntimeErrorMessage(error, '保存填写记录失败，请查看控制台或接口返回'),
    );
  } finally {
    saving.value = false;
  }
}

async function handleExportExcel() {
  excelLoading.value = true;
  try {
    if (!isPressSlotProductionCheck(schema.value, detail.value?.processCode)) applyBusinessParams(true);
    const layout = runtimeRef.value?.buildExcelLayout?.(
      detail.value?.formName || props.form?.formName,
      'export',
    ) as MesHcProcessFormApi.LayoutExcelReq;
    if (!layout) {
      message.warning('当前模板还没有可导出的运行布局');
      return;
    }
    const data = await exportProcessFormRecordLayout(layout);
    downloadFileFromBlobPart({
      fileName: layout.fileName || '动态表单运行记录.xlsx',
      source: data,
    });
  } catch (error: any) {
    console.error('[动态表单运行导出] 导出失败', error);
    message.error(getRuntimeErrorMessage(error, '导出 Excel 失败'));
  } finally {
    excelLoading.value = false;
  }
}

function triggerImportExcel() {
  excelInputRef.value?.click();
}

async function handleExcelImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!/\.xlsx?$/iu.test(file.name)) {
    message.warning('请选择 Excel 文件');
    return;
  }
  excelLoading.value = true;
  try {
    const layout = runtimeRef.value?.buildExcelLayout?.(
      detail.value?.formName || props.form?.formName,
      'import',
    ) as MesHcProcessFormApi.LayoutExcelReq;
    if (!layout) {
      message.warning('当前模板还没有可导入的运行布局');
      return;
    }
    const resp = await importProcessFormRecordLayout(file, layout);
    const summary = runtimeRef.value?.applyImportedLayout?.(resp);
    await appendImportAttachment(file);
    const rowText =
      summary?.rowCount === undefined
        ? ''
        : `，有效明细 ${summary.rowCount} 行`;
    const skippedText = summary?.skippedRowCount
      ? `，跳过标题/空行 ${summary.skippedRowCount} 行`
      : '';
    message.success(
      `导入成功，已回填 ${summary?.appliedCellCount ?? resp.totalCellCount ?? 0} 个单元格${rowText}${skippedText}`,
    );
  } catch (error: any) {
    console.error('[动态表单运行导入] 导入失败', error);
    message.error(getRuntimeErrorMessage(error, '导入 Excel 失败'));
  } finally {
    excelLoading.value = false;
  }
}

async function appendImportAttachment(file: File) {
  const uploaded = (await uploadFile({
    directory: 'mes/process-form-record',
    file,
  })) as any;
  const runtimeHeader = runtimeRef.value?.getHeaderData?.() || headerData.value;
  const attachments = Array.isArray(runtimeHeader.attachments)
    ? runtimeHeader.attachments
    : [];
  const nextAttachments = [
    ...attachments,
    {
      name: uploaded?.name || file.name,
      path: uploaded?.path,
      size: uploaded?.size ?? file.size,
      type: uploaded?.type || file.type,
      uploadTime: new Date().toISOString(),
      url: typeof uploaded === 'string' ? uploaded : uploaded?.url,
    },
  ];
  runtimeHeader.attachments = nextAttachments;
  runtimeHeader.processFormExcelAttachments = nextAttachments;
  headerData.value = runtimeHeader;
  runtimeRef.value?.setHeaderData?.(runtimeHeader);
}

function openAttachment(attachment: Record<string, any>) {
  const url = String(attachment?.url || attachment?.path || '').trim();
  if (url) window.open(url, '_blank');
}
</script>

<template>
  <Modal
    :confirm-loading="loading"
    :open="paramVisible"
    :title="paramTitle"
    destroy-on-close
    ok-text="确认并进入填写"
    width="760px"
    wrap-class-name="station-form-runtime-param-modal"
    @cancel="cancelParamStep"
    @ok="confirmBusinessParams"
  >
    <Spin :spinning="loading">
      <div class="runtime-param">
        <div class="runtime-param__notice">
          这些参数用于初始化运行表单、模板匹配和保存记录。确认后进入全屏填写界面。
        </div>
        <AForm layout="vertical" class="runtime-param__form">
          <AFormItem label="计划ID">
            <Input
              v-model:value="businessParams.planId"
              placeholder="例：1001"
            />
          </AFormItem>
          <AFormItem label="计划号">
            <Input
              v-model:value="businessParams.planNo"
              placeholder="例：20260618-001"
            />
          </AFormItem>
          <AFormItem label="工序任务ID">
            <Input
              v-model:value="businessParams.planOperationId"
              placeholder="例：2001"
            />
          </AFormItem>
          <AFormItem label="填写日期">
            <Input
              v-model:value="businessParams.recordDate"
              placeholder="YYYY-MM-DD"
            />
          </AFormItem>
          <AFormItem label="型号">
            <Input
              v-model:value="businessParams.modelCode"
              placeholder="例：W33P0300"
            />
          </AFormItem>
          <AFormItem label="料号">
            <Input
              v-model:value="businessParams.materialCode"
              placeholder="例：03.13.10055"
            />
          </AFormItem>
          <AFormItem label="批号">
            <Input
              v-model:value="businessParams.batchNo"
              placeholder="例：W26F069AP"
            />
          </AFormItem>
          <AFormItem label="压槽片号">
            <Input
              v-model:value="businessParams.pressSlotSliceNo"
              placeholder="例：W26F069AP002"
            />
          </AFormItem>
          <AFormItem label="前段片号">
            <Input
              v-model:value="businessParams.frontSliceNo"
              placeholder="初始化明细前段片号"
            />
          </AFormItem>
          <AFormItem label="中段片号">
            <Input
              v-model:value="businessParams.middleSliceNo"
              placeholder="初始化明细中段片号"
            />
          </AFormItem>
          <AFormItem label="后段片号">
            <Input
              v-model:value="businessParams.endSliceNo"
              placeholder="初始化明细后段片号"
            />
          </AFormItem>
          <AFormItem label="设备编码">
            <Input
              v-model:value="businessParams.equipmentCode"
              placeholder="可选"
            />
          </AFormItem>
          <AFormItem label="设备名称">
            <Input
              v-model:value="businessParams.equipmentName"
              placeholder="可选"
            />
          </AFormItem>
          <AFormItem label="磨皮阶段">
            <Input
              v-model:value="businessParams.passName"
              placeholder="例：一次磨皮 / 二次磨皮"
            />
          </AFormItem>
        </AForm>
      </div>
    </Spin>
  </Modal>

  <Modal
    :footer="null"
    :open="runtimeVisible"
    :title="null"
    destroy-on-close
    width="100vw"
    wrap-class-name="station-form-runtime-fullscreen-modal"
    @cancel="closeAll"
    @update:open="(value) => !value && closeAll()"
  >
    <Spin :spinning="loading">
      <div class="runtime-fullscreen">
        <div class="runtime-fullscreen__toolbar">
          <div class="runtime-fullscreen__title">
            <strong>{{ runtimeTitle }}</strong>
            <span>{{ detail?.processName || form?.processName || '-' }}</span>
          </div>
          <div class="runtime-fullscreen__actions">
            <input
              ref="excelInputRef"
              accept=".xls,.xlsx"
              class="runtime-fullscreen__excel-input"
              type="file"
              @change="handleExcelImportChange"
            />
            <Button
              v-if="!props.skipBusinessParamStep"
              @click="reopenBusinessParams"
            >
              业务参数
            </Button>
            <Button :loading="excelLoading" @click="handleExportExcel">
              导出Excel
            </Button>
            <Button :loading="excelLoading" @click="triggerImportExcel">
              导入Excel
            </Button>
            <Button :loading="saving" type="primary" @click="handleSave">
              保存
            </Button>
            <Button
              v-if="props.enableConfirm"
              :loading="saving"
              danger
              type="primary"
              @click="handleConfirm"
            >
              确认
            </Button>
            <Button @click="closeAll">关闭</Button>
          </div>
        </div>

        <div class="runtime-fullscreen__body">
          <StationFormRuntimeRenderer
            v-if="canRender"
            ref="runtimeRef"
            v-model:header-data="headerData"
            :form-name="detail?.formName || form?.formName"
            :items="detail?.items || []"
            :record-meta="recordMeta"
            :readonly-pass-work-header="props.readonlyPassWorkHeader"
            :schema="schema"
            :readonly="pressSlotExistingRecord?.recordStatus === 'CONFIRMED'"
            compact
            @open-attachment="openAttachment"
          />
          <div v-else class="runtime-fullscreen__empty">
            当前模板还没有 DEV 运行布局配置。
          </div>
        </div>
      </div>
    </Spin>
  </Modal>

  <AuthModal
    v-model:visible="runtimeAuthVisible"
    :action-name="runtimeAuthActionName"
    auth-mode="username"
    @success="handleRuntimeAuthSuccess"
  />
</template>

<style scoped>
.runtime-param__notice {
  padding: 10px 12px;
  margin-bottom: 14px;
  color: #31506f;
  background: #f2f7fc;
  border: 1px solid #d5e3f1;
}

.runtime-param__form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.runtime-fullscreen {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background: #edf2f7;
}

.runtime-fullscreen__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 52px;
  padding: 8px 14px;
  background: #e3ebf4;
  border-bottom: 1px solid #c8d4e2;
}

.runtime-fullscreen__title {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.runtime-fullscreen__title strong {
  color: #0f2b46;
  font-size: 16px;
}

.runtime-fullscreen__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.runtime-fullscreen__excel-input {
  display: none;
}

.runtime-fullscreen__body {
  flex: 1;
  min-height: 0;
  padding: 10px;
  overflow: auto;
}

.runtime-fullscreen__body :deep(.station-form-runtime) {
  min-height: calc(100vh - 72px);
}

.runtime-fullscreen__empty {
  padding: 36px;
  color: #66788a;
  text-align: center;
}

:global(.station-form-runtime-fullscreen-modal .ant-modal) {
  top: 0;
  z-index: 3201;
  max-width: 100vw;
  padding-bottom: 0;
  margin: 0;
}

:global(.station-form-runtime-param-modal) {
  z-index: 3190 !important;
}

:global(.station-form-runtime-fullscreen-modal) {
  z-index: 3200 !important;
}

:global(.station-form-runtime-fullscreen-modal .ant-modal-content) {
  min-height: 100vh;
  padding: 0;
  border-radius: 0;
}

:global(.station-form-runtime-fullscreen-modal .ant-modal-body) {
  padding: 0;
}

:global(.station-form-runtime-fullscreen-modal .ant-modal-close) {
  display: none;
}

:global(.station-form-runtime-fullscreen-modal .ant-picker-dropdown) {
  z-index: 3302 !important;
}

@media (max-width: 900px) {
  .runtime-param__form {
    grid-template-columns: 1fr;
  }

  .runtime-fullscreen__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
