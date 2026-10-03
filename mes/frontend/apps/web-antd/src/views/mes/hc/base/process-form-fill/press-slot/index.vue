<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcAdhesiveConsoleApi } from '#/api/mes/hc/execution/press-slot-console';

import { computed, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { Button, Modal, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getPressSlotFormRecordPage } from '#/api/mes/hc/execution/press-slot-console';
import RoughMiddleProductRecordSheet from '#/views/mes/hc/shared/RoughMiddleProductRecordSheet.vue';
import { shouldShowPressSlotIntermediateDepthSection } from '#/views/mes/hc/shared/pressSlotRuntimeLayout';
import { productionCheckSchema } from '#/views/mes/hc/shared/pressSlotProductionCheck';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';

defineOptions({ name: 'MesHcProcessFormFillPressSlot' });

const props = defineProps<{ viewerOnly?: boolean }>();
defineExpose({ openDetail });

type PressSlotFormRecord = MesHcAdhesiveConsoleApi.FormRecord;
type ImportAttachment = {
  name?: string;
  path?: string;
  size?: number;
  type?: string;
  uploadTime?: string;
  url?: string;
};
type DetailColumn = {
  key: string;
  title: string;
  width?: number;
};

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';
const MIDDLE_PRODUCT_PAGE_SIZE = 50;

const PRESS_SLOT_FORM_TYPE_OPTIONS = [
  { label: '点检清洁', value: 'STATION_RECORD' },
  { label: '首检送检', value: 'CHANGEOVER' },
  { label: '生产点检', value: 'PRODUCTION_CHECK' },
  { label: '工艺参数', value: 'PROCESS_PARAM' },
  { label: '中间品记录', value: 'INTERMEDIATE_RECORD' },
];

const PRESS_SLOT_INTERMEDIATE_COLUMNS = [
  { key: 'samplePositionName', title: '采样段', width: 100 },
  { key: 'sliceBatchNo', title: '片号', width: 180 },
  { key: 'widthMm', title: '宽幅/mm', width: 120 },
  ...Array.from({ length: 10 }, (_, index) => ({
    key: `thickness${index + 1}`,
    title: `${(index + 1) * 100}cm厚度/mm(1.183±0.045)`,
    width: 190,
  })),
  { key: 'remark', title: '备注', width: 180 },
];

const PRESS_SLOT_PROCESS_PARAM_COLUMNS = [
  { key: 'reportDate', title: '日期', width: 110 },
  { key: 'modelCode', title: '型号', width: 120 },
  { key: 'motherBatchNo', title: '批号', width: 150 },
  { key: 'productionBatchNo', title: '压槽片号', width: 170 },
  { key: 'measuredTemperature1', title: '实测点温度1(℃)', width: 150 },
  { key: 'measuredTemperature2', title: '实测点温度2(℃)', width: 150 },
  { key: 'measuredTemperature3', title: '实测点温度3(℃)', width: 150 },
  { key: 'measuredTemperature4', title: '实测点温度4(℃)', width: 150 },
  { key: 'measuredTemperature5', title: '实测点温度5(℃)', width: 150 },
  { key: 'recorderName', title: '记录人', width: 110 },
  { key: 'remark', title: '备注', width: 180 },
];

const currentRecord = ref<PressSlotFormRecord | null>(null);
const detailVisible = ref(false);
const detailLoading = ref(false);
const middleProductPage = ref(1);

const currentPayload = computed(() => currentRecord.value?.payload || {});
const isRuntimeDynamicForm = computed(() => !!currentPayload.value.runtimeSchema || !!currentPayload.value.runtimeRecord || !!currentPayload.value.processParam?.runtimeSchema);
const runtimeDynamicSchema = computed(() => currentPayload.value.processParam?.runtimeSchema ? productionCheckSchema(currentPayload.value.processParam.runtimeSchema) : currentPayload.value.runtimeSchema || parseJsonObject(currentPayload.value.runtimeContext?.runtimeSchema));
const runtimeDynamicHeader = computed(() => currentPayload.value.processParam?.runtimeSchema ? {
  ...currentPayload.value.processParam.headerData,
  previewDetails: (currentPayload.value.processParam.items || []).map((item: any) => ({ ...item, standardText: item.standardValue, resultFlag: item.checkResult })),
} : currentPayload.value.runtimeHeader || parseJsonObject(currentPayload.value.runtimeRecord?.headerDataJson));
const runtimeDynamicMetaItems = computed(() => [
  `计划号：${formatText(currentRecord.value?.planNo)}`,
  `压槽片号：${formatText(currentRecord.value?.pressSlotSliceNo)}`,
  `状态：${formatText(currentRecord.value?.statusName || currentRecord.value?.status)}`,
]);
const stationItems = computed(() => normalizeArray(currentPayload.value.stationItems));
const changeoverRecord = computed(() => currentPayload.value.changeover || {});
const changeoverItems = computed(() => normalizeArray(changeoverRecord.value.checkItems));
const processParamRecord = computed(() => currentPayload.value.processParam || {});
const processParamItems = computed(() => {
  const payloadItems = normalizeArray(currentPayload.value.processParamItems);
  if (payloadItems.some((item) => item.productionBatchNo)) {
    return payloadItems;
  }
  if (payloadItems.length > 0) {
    return payloadItems.map((item, index) => ({
      ...item,
      itemSeq: item.itemSeq || index + 1,
      resultFlag: item.resultFlag || item.checkResult || 'OK',
    }));
  }
  return [];
});
const processParamRows = computed(() =>
  normalizeArray(processParamRecord.value.items || processParamItems.value).map((item, index) => ({
    ...item,
    seq: item.seq || index + 1,
  })),
);
const middleLedgerRecord = computed(() => currentPayload.value.middleLedger || {});
const intermediateRecord = computed(() => currentPayload.value.intermediate || {});
const showIntermediateSlotDepthSection = computed(() =>
  shouldShowPressSlotIntermediateDepthSection(intermediateRecord.value),
);
const intermediateSlotDepthStandardText = computed(() =>
  formatText(intermediateRecord.value.slotDepthStandard, '0.705±0.044'),
);

const isChangeoverForm = computed(() => currentRecord.value?.sourceType === 'CHANGEOVER' || currentRecord.value?.type === 'CHANGEOVER');
const isProductionCheckForm = computed(() => currentRecord.value?.sourceType === 'PRODUCTION_CHECK' || currentRecord.value?.type === 'PRODUCTION_CHECK');
const isProcessParamForm = computed(() => currentRecord.value?.sourceType === 'PROCESS_PARAM' || currentRecord.value?.type === 'PROCESS_PARAM');
const isMiddleLedgerForm = computed(() => currentRecord.value?.sourceType === 'MIDDLE_LEDGER' || currentRecord.value?.type === 'MIDDLE_LEDGER');
const isIntermediateForm = computed(() => currentRecord.value?.sourceType === 'INTERMEDIATE_RECORD' || currentRecord.value?.type === 'INTERMEDIATE_RECORD');

const dialogTitle = computed(() => `查看${getPressSlotDisplayFormName(currentRecord.value) || '压槽表单'}`);

const currentImportAttachments = computed(() => findImportAttachments(currentPayload.value));

const intermediateAttachments = computed(() => currentImportAttachments.value);

const headerFields = computed(() => [
  { field: 'formName', label: '表单名称', value: getPressSlotDisplayFormName(currentRecord.value) || '-' },
  { field: 'planNo', label: '计划号', value: currentRecord.value?.planNo || '-' },
  { field: 'pressSlotSliceNo', label: '压槽片号', value: currentRecord.value?.pressSlotSliceNo || '-' },
  { field: 'modelCode', label: '型号', value: currentRecord.value?.modelCode || '-' },
  { field: 'typeName', label: '类型', value: currentRecord.value?.typeName || '-' },
  { field: 'equipment', label: '设备', value: formatEquipment(currentRecord.value) },
  { field: 'recordUserName', label: '记录人', value: currentRecord.value?.fillUserName || '-' },
  { field: 'recordTime', label: '记录时间', value: formatDateTime(currentRecord.value?.fillTime || currentRecord.value?.createTime) },
  { field: 'confirmUserName', label: '确认人', value: currentRecord.value?.confirmUserName || '-' },
  { field: 'confirmTime', label: '确认时间', value: formatDateTime(currentRecord.value?.confirmTime) },
  { field: 'status', label: '状态', value: currentRecord.value?.statusName || currentRecord.value?.status || '-' },
  { field: 'recordDate', label: '记录日期', value: formatDate(currentRecord.value?.recordDate) },
]);

const detailColumns = computed<DetailColumn[]>(() => {
  if (isMiddleLedgerForm.value) {
    return [
      { key: 'reportDate', title: '生产日期', width: 120 },
      { key: 'sliceBatchNo', title: '片号', width: 180 },
      { key: 'reportTypeName', title: '类型', width: 120 },
      { key: 'widthMm', title: '宽幅/mm', width: 110 },
      ...Array.from({ length: 10 }, (_, index) => ({
        key: `thickness${index + 1}`,
        title: `厚度${index + 1}/mm(1.183±0.045)`,
        width: 176,
      })),
      { key: 'remark', title: '备注', width: 160 },
    ];
  }
  if (isProcessParamForm.value) {
    return PRESS_SLOT_PROCESS_PARAM_COLUMNS;
  }
  if (isProductionCheckForm.value) {
    return [
      { key: 'itemCategory', title: '类别', width: 140 },
      { key: 'itemName', title: '点检项目', width: 240 },
      { key: 'standardValue', title: '标准', width: 260 },
      { key: 'actualValue', title: '实测值', width: 180 },
      { key: 'abnormalRemark', title: '异常备注', width: 240 },
    ];
  }
  if (isChangeoverForm.value) {
    return [
      { key: 'itemCategory', title: '类别', width: 140 },
      { key: 'itemName', title: '工艺参数项目', width: 220 },
      { key: 'standardValue', title: '标准', width: 220 },
      { key: 'actualValue', title: '实测值', width: 180 },
      { key: 'abnormalRemark', title: '异常备注', width: 240 },
    ];
  }
  return [
    { key: 'itemSeq', title: '序号', width: 90 },
    { key: 'itemCategory', title: '类别', width: 140 },
    { key: 'stepNode', title: '确认节点', width: 140 },
    { key: 'itemName', title: '点检项目', width: 240 },
    { key: 'standardText', title: '标准', width: 320 },
    { key: 'actualText', title: '实际/记录', width: 220 },
    { key: 'abnormalRemark', title: '备注', width: 240 },
  ];
});

const detailRows = computed<Record<string, any>[]>(() => {
  if (isMiddleLedgerForm.value) {
    return [middleLedgerRecord.value];
  }
  if (isProcessParamForm.value) {
    return processParamRows.value;
  }
  if (isProductionCheckForm.value) {
    return processParamRows.value;
  }
  if (isChangeoverForm.value) {
    return changeoverItems.value;
  }
  return stationItems.value.map((item, index) => ({
    ...item,
    actualText: formatStationActual(item),
    itemSeq: item.itemSeq || index + 1,
  }));
});

const detailColumnCount = computed(() => detailColumns.value.length);
const detailTableWidth = computed(() =>
  detailColumns.value.reduce((total, column) => total + (Number(column.width) || 120), 0),
);
const detailTableStyle = computed(() => {
  if (!isProcessParamForm.value) {
    return undefined;
  }
  const width = `${detailTableWidth.value}px`;
  return { minWidth: width, width };
});

const intermediateHeadFields = computed(() => {
  const record = intermediateRecord.value;
  return [
    { field: 'recordDate', label: '生产日期', value: formatDate(record.recordDate || currentRecord.value?.recordDate) },
    { field: 'modelCode', label: '型号', value: record.modelCode || '-' },
    { field: 'materialCode', label: '料号', value: record.materialCode || '-' },
    { field: 'batchNo', label: '批号', value: record.batchNo || '-' },
  ];
});

const intermediateSignatureFields = computed(() => [
  { field: 'recorderName', label: '填写人', value: intermediateRecord.value.recorderName || currentRecord.value?.fillUserName || '-' },
  { field: 'recordTime', label: '填写时间', value: formatDateTime(currentRecord.value?.fillTime || currentRecord.value?.createTime) },
  { field: 'confirmerName', label: '确认人', value: intermediateRecord.value.confirmerName || currentRecord.value?.confirmUserName || '-' },
  { field: 'confirmTime', label: '确认时间', value: formatDateTime(currentRecord.value?.confirmTime) },
]);

const intermediateRows = computed(() =>
  normalizeArray(intermediateRecord.value.details).map((item, index) => ({
    key: index + 1,
    remark: item.remark || '',
    samplePositionName: item.samplePositionName || item.samplePosition || '-',
    seq: item.sortNo || item.seq || index + 1,
    sliceBatchNo: item.sliceBatchNo || '',
    thickness1: item.thickness1 ?? '',
    thickness2: item.thickness2 ?? '',
    thickness3: item.thickness3 ?? '',
    thickness4: item.thickness4 ?? '',
    thickness5: item.thickness5 ?? '',
    thickness6: item.thickness6 ?? '',
    thickness7: item.thickness7 ?? '',
    thickness8: item.thickness8 ?? '',
    thickness9: item.thickness9 ?? '',
    thickness10: item.thickness10 ?? '',
    widthMm: item.widthMm ?? '',
  })),
);

const intermediateMetaItems = computed(() => [
  `计划号：${formatText(currentRecord.value?.planNo)}`,
  `压槽片号：${formatText(currentRecord.value?.pressSlotSliceNo)}`,
  `状态：${formatText(currentRecord.value?.statusName || currentRecord.value?.status)}`,
]);

const gridFormSchema: VbenFormSchema[] = [
  { component: 'Input', fieldName: 'formName', label: '表单名称' },
  { component: 'Input', fieldName: 'planNo', label: '计划号' },
  { component: 'Input', fieldName: 'pressSlotSliceNo', label: '压槽片号' },
  { component: 'Input', fieldName: 'modelCode', label: '型号' },
  {
    component: 'Select',
    componentProps: { allowClear: true, options: PRESS_SLOT_FORM_TYPE_OPTIONS, placeholder: '全部类型' },
    fieldName: 'type',
    label: '类型',
  },
];

const recordColumns: VxeTableGridOptions<PressSlotFormRecord>['columns'] = [
  { field: 'formName', title: '表单名称', minWidth: 250, slots: { default: 'formName' } },
  { field: 'planNo', title: '计划号', minWidth: 150, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'pressSlotSliceNo', title: '压槽片号', minWidth: 180, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'modelCode', title: '型号', minWidth: 140, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'typeName', title: '类型', width: 130, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'equipmentName', title: '设备', minWidth: 150, formatter: ({ row }) => formatEquipment(row) },
  { field: 'recordDate', title: '记录日期', width: 120, formatter: ({ cellValue }) => formatDate(cellValue) },
  { field: 'statusName', title: '状态', width: 110, align: 'center', slots: { default: 'statusName' } },
  { field: 'fillUserName', title: '填写人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'confirmUserName', title: '确认人', width: 110, formatter: ({ cellValue }) => formatText(cellValue) },
  { field: 'createTime', title: '创建时间', width: 170, formatter: ({ cellValue }) => formatDateTime(cellValue) },
  { title: '操作', width: 90, fixed: 'right', align: 'center', slots: { default: 'actions' } },
];

const [Grid] = useVbenVxeGrid({
  formOptions: { schema: gridFormSchema },
  gridOptions: {
    columns: recordColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: { ajax: { query: queryPressSlotRecordPage } },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<PressSlotFormRecord>,
});

async function queryPressSlotRecordPage({ page }: any, formValues: Record<string, any>) {
  const pageNo = Number(page.currentPage || 1);
  const pageSize = Number(page.pageSize || 20);
  return getPressSlotFormRecordPage({ ...formValues, pageNo, pageSize });
}

function openDetail(row: PressSlotFormRecord) {
  currentRecord.value = row;
  middleProductPage.value = 1;
  detailVisible.value = true;
}

function closeDetail() {
  detailVisible.value = false;
  currentRecord.value = null;
  middleProductPage.value = 1;
}

function normalizeArray<T = any>(value?: T[]) {
  return Array.isArray(value) ? value : [];
}

function parseJsonObject(value?: Record<string, any> | string) {
  if (!value) return {} as Record<string, any>;
  if (typeof value === 'object' && !Array.isArray(value)) return value;
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
}

function formatText(value?: null | number | string, fallback = '-') {
  const text = String(value ?? '').trim();
  return text || fallback;
}

function formatIntermediateDepthValue(primary?: null | number | string, ...legacyValues: Array<null | number | string | undefined>) {
  return formatText(primary ?? legacyValues.find((item) => String(item ?? '').trim()));
}

function formatDateTime(value?: any) {
  if (!value) return '-';
  if (Array.isArray(value) && value.length >= 5) {
    const date = dayjs(`${value[0]}-${value[1]}-${value[2]} ${value[3]}:${value[4]}:${value[5] || 0}`);
    return date.isValid() ? date.format(DATETIME_FORMAT) : value.join('-');
  }
  const date = dayjs(value);
  return date.isValid() ? date.format(DATETIME_FORMAT) : String(value);
}

function formatDate(value?: any) {
  if (!value) return '-';
  if (Array.isArray(value) && value.length >= 3) {
    const date = dayjs(`${value[0]}-${value[1]}-${value[2]}`);
    return date.isValid() ? date.format('YYYY-MM-DD') : value.join('-');
  }
  const text = String(value).trim();
  if (/^\d{4},\d{1,2},\d{1,2}$/u.test(text)) {
    const [year, month, day] = text.split(',');
    const date = dayjs(`${year}-${month}-${day}`);
    return date.isValid() ? date.format('YYYY-MM-DD') : text.replaceAll(',', '-');
  }
  const date = dayjs(text);
  return date.isValid() ? date.format('YYYY-MM-DD') : text;
}

function stripModelPrefix(name?: string) {
  const text = String(name || '').trim();
  if (!text) return '';
  return text.replace(/^.*?[（(][^）)]*[）)]\s*/u, '').trim() || text;
}

function getPressSlotDisplayFormName(record?: PressSlotFormRecord | null) {
  return stripModelPrefix(record?.formName);
}

function formatEquipment(record?: Partial<PressSlotFormRecord> | null) {
  return formatText([record?.equipmentCode, record?.equipmentName].filter(Boolean).join(' / '));
}

function statusMeta(status?: string, statusName?: string) {
  const normalized = String(status || '').toUpperCase();
  if (['CONFIRMED', 'OK', 'QUALIFIED'].includes(normalized)) {
    return { color: 'success', text: statusName || '已确认' };
  }
  if (['ABNORMAL', 'FAILED', 'NG', 'REJECTED'].includes(normalized)) {
    return { color: 'error', text: statusName || status || '异常' };
  }
  if (['DRAFT', 'PENDING', 'PENDING_CHECK', 'WAITING_QA'].includes(normalized)) {
    return { color: 'warning', text: statusName || status || '待处理' };
  }
  if (['RECORDED', 'SUBMITTED'].includes(normalized)) {
    return { color: 'processing', text: statusName || '已填写' };
  }
  return { color: 'default', text: statusName || status || '-' };
}

function resultMeta(result?: string) {
  const normalized = String(result || '').toUpperCase();
  if (normalized === 'OK' || normalized === 'PASS' || normalized === 'QUALIFIED') return { color: 'success', text: result };
  if (normalized === 'NG' || normalized === 'FAIL' || normalized === 'FAILED') return { color: 'error', text: result };
  return { color: 'default', text: result || '-' };
}

function formatStationActual(row: Record<string, any>) {
  const values = [
    row.actualValue ? `${row.dualLabel1 ? `${row.dualLabel1}：` : ''}${row.actualValue}` : '',
    row.actualValue2 ? `${row.dualLabel2 ? `${row.dualLabel2}：` : ''}${row.actualValue2}` : '',
  ].filter(Boolean);
  return values.length ? values.join(' / ') : '-';
}

function normalizeAttachments(value?: ImportAttachment | ImportAttachment[] | unknown): ImportAttachment[] {
  const list = Array.isArray(value) ? value : value ? [value] : [];
  return list
    .map((item, index) => {
      if (!item || typeof item !== 'object') return null;
      const raw = item as ImportAttachment & Record<string, any>;
      const path = String(raw.path || raw.filePath || '');
      const url = String(raw.url || raw.fileUrl || path || '');
      const name = String(raw.name || raw.fileName || url.split('/').pop() || `附件${index + 1}`);
      if (!url) return null;
      return { name, path: path || undefined, size: Number(raw.size) || undefined, type: raw.type, uploadTime: raw.uploadTime, url };
    })
    .filter(Boolean) as ImportAttachment[];
}

function attachmentKey(attachment: ImportAttachment) {
  return [attachment.url, attachment.path, attachment.name, attachment.uploadTime].filter(Boolean).join('|');
}

function findImportAttachments(source?: Record<string, any>): ImportAttachment[] {
  const candidates: unknown[] = [];
  const visit = (value: any) => {
    if (!value || typeof value !== 'object') return;
    candidates.push(value.attachments, value.importAttachment, value.processFormExcelAttachments);
    if (typeof value.extraJson === 'string') visit(parseJsonObject(value.extraJson));
    if (typeof value.headerDataJson === 'string') visit(parseJsonObject(value.headerDataJson));
    ['intermediate', 'middleLedger', 'processParam', 'changeover'].forEach((key) => visit(value[key]));
  };
  visit(source);
  const seen = new Set<string>();
  return candidates
    .flatMap((candidate) => normalizeAttachments(candidate))
    .filter((attachment) => {
      const key = attachmentKey(attachment);
      if (key && seen.has(key)) return false;
      if (key) seen.add(key);
      return true;
    });
}

function formatAttachmentSize(size?: number) {
  if (!size || size <= 0) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function downloadImportAttachment(attachment?: ImportAttachment) {
  const url = formatText(attachment?.url || attachment?.path, '');
  if (url) window.open(url, '_blank');
}
</script>

<template>
  <component :is="props.viewerOnly ? 'div' : Page" auto-content-height class="press-slot-station-record-page">
    <div class="press-slot-record-layout">
      <Grid v-if="!props.viewerOnly" @cell-dblclick="({ row }) => openDetail(row)">
        <template #formName="{ row }">
          <button class="press-slot-record-link" type="button" @click.stop="openDetail(row)">
            {{ getPressSlotDisplayFormName(row) || '-' }}
          </button>
        </template>
        <template #statusName="{ row }">
          <Tag :color="statusMeta(row.status, row.statusName).color">
            {{ statusMeta(row.status, row.statusName).text }}
          </Tag>
        </template>
        <template #actions="{ row }">
          <Button :loading="detailLoading" size="small" type="link" @click.stop="openDetail(row)">查看</Button>
        </template>
      </Grid>
    </div>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      destroy-on-close
      width="100vw"
      wrap-class-name="rough-station-record-modal press-slot-station-record-modal"
      @cancel="closeDetail"
    >
      <div v-if="currentRecord && isRuntimeDynamicForm" class="press-slot-runtime-view">
        <StationFormRuntimeRenderer
          :form-name="dialogTitle"
          :header-data="runtimeDynamicHeader"
          :readonly="true"
          :record-meta="runtimeDynamicMetaItems"
          :schema="runtimeDynamicSchema"
          compact
          @open-attachment="downloadImportAttachment"
        >
          <template #actions>
            <Button size="small" @click="closeDetail">关闭</Button>
          </template>
        </StationFormRuntimeRenderer>
      </div>

      <RoughMiddleProductRecordSheet
        v-else-if="currentRecord && isIntermediateForm"
        v-model:page="middleProductPage"
        :attachments="intermediateAttachments"
        :columns="PRESS_SLOT_INTERMEDIATE_COLUMNS"
        detail-title="中间品记录明细"
        :editable="false"
        :head-fields="intermediateHeadFields"
        :meta-items="intermediateMetaItems"
        :page-size="MIDDLE_PRODUCT_PAGE_SIZE"
        :rows="intermediateRows"
        :signature-fields="intermediateSignatureFields"
        :title="dialogTitle"
        @open-attachment="downloadImportAttachment"
      >
        <template #extra-sections>
          <fieldset v-if="showIntermediateSlotDepthSection" class="press-slot-depth-fieldset">
            <legend>首件槽深/mm（标准：{{ intermediateSlotDepthStandardText }}）</legend>
            <div class="press-slot-depth-matrix">
              <div class="press-slot-depth-cell press-slot-depth-cell--head">XY最小值</div>
              <div class="press-slot-depth-cell press-slot-depth-cell--head">XY最大值</div>
              <div class="press-slot-depth-cell press-slot-depth-cell--head">XY平均值</div>
              <div class="press-slot-depth-cell">
                {{ formatIntermediateDepthValue(intermediateRecord.firstSlotDepthMin, intermediateRecord.firstSlotDepthXMin, intermediateRecord.firstSlotDepthYMin) }}
              </div>
              <div class="press-slot-depth-cell">
                {{ formatIntermediateDepthValue(intermediateRecord.firstSlotDepthMax, intermediateRecord.firstSlotDepthXMax, intermediateRecord.firstSlotDepthYMax) }}
              </div>
              <div class="press-slot-depth-cell">
                {{ formatIntermediateDepthValue(intermediateRecord.firstSlotDepthAvg, intermediateRecord.firstSlotDepthXAvg, intermediateRecord.firstSlotDepthYAvg) }}
              </div>
            </div>
          </fieldset>
        </template>
        <template #actions>
          <Button size="small" @click="closeDetail">关闭</Button>
        </template>
      </RoughMiddleProductRecordSheet>

      <div v-else class="press-slot-detail-modal">
        <div class="press-slot-detail-toolbar">
          <div class="press-slot-detail-toolbar__title">
            <span class="press-slot-detail-toolbar__main">{{ dialogTitle }}</span>
            <div class="press-slot-detail-toolbar__meta">
              <span>计划号：{{ formatText(currentRecord?.planNo) }}</span>
              <span>压槽片号：{{ formatText(currentRecord?.pressSlotSliceNo) }}</span>
              <span>类型：{{ formatText(currentRecord?.typeName) }}</span>
              <span>设备：{{ formatEquipment(currentRecord) }}</span>
            </div>
          </div>
          <div class="press-slot-detail-toolbar__actions">
            <Button size="small" @click="closeDetail">关闭</Button>
          </div>
        </div>

        <div v-if="currentRecord" class="press-slot-detail-body">
          <fieldset class="press-slot-fieldset">
            <legend>表单信息</legend>
            <div class="press-slot-head-grid">
              <div v-for="item in headerFields" :key="item.field" class="press-slot-head-item">
                <span class="press-slot-head-item__label">{{ item.label }}</span>
                <span class="press-slot-head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
            <div v-if="currentImportAttachments.length" class="press-slot-import-attachment">
              <span class="press-slot-import-attachment__label">导入附件</span>
              <div
                v-for="(attachment, index) in currentImportAttachments"
                :key="(attachment.url || attachment.path || attachment.name || 'attachment') + '-' + index"
                class="press-slot-import-attachment__item"
              >
                <Button size="small" type="link" @click="downloadImportAttachment(attachment)">
                  {{ attachment.name || ('原始导入文件' + (index + 1)) }}
                </Button>
                <span>导入时间：{{ formatDateTime(attachment.uploadTime) }}</span>
                <span v-if="formatAttachmentSize(attachment.size)">
                  大小：{{ formatAttachmentSize(attachment.size) }}
                </span>
              </div>
            </div>
          </fieldset>

          <div v-if="isChangeoverForm" class="press-slot-fieldset press-slot-fieldset--compact">
            <legend>首检信息</legend>
            <div class="press-slot-head-grid">
              <div class="press-slot-head-item"><span class="press-slot-head-item__label">当前料号</span><span class="press-slot-head-item__value">{{ changeoverRecord.productionMaterialCode || '-' }}</span></div>
              <div class="press-slot-head-item"><span class="press-slot-head-item__label">母卷批号</span><span class="press-slot-head-item__value">{{ changeoverRecord.motherSegmentBatchNo || '-' }}</span></div>
              <div class="press-slot-head-item"><span class="press-slot-head-item__label">送检时间</span><span class="press-slot-head-item__value">{{ formatDateTime(changeoverRecord.submitTime) }}</span></div>
              <div class="press-slot-head-item"><span class="press-slot-head-item__label">检测结果</span><span class="press-slot-head-item__value">{{ changeoverRecord.feedbackResult || '-' }}</span></div>
            </div>
          </div>

          <div class="press-slot-detail-panel">
            <div class="press-slot-detail-panel__header">明细项目</div>
            <div class="press-slot-table-wrap">
              <table
                class="press-slot-grid"
                :class="{ 'press-slot-grid--fit-columns': isProcessParamForm }"
                :style="detailTableStyle"
              >
                <colgroup v-if="isProcessParamForm">
                  <col
                    v-for="column in detailColumns"
                    :key="`${column.key}-col`"
                    :style="{ width: `${column.width}px` }"
                  />
                </colgroup>
                <thead>
                  <tr>
                    <th v-for="column in detailColumns" :key="column.key" :width="column.width">{{ column.title }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in detailRows" :key="item.id || item.key || index">
                    <td v-for="column in detailColumns" :key="column.key" :align="['itemSeq', 'resultFlag', 'checkResult'].includes(column.key) ? 'center' : undefined">
                      <Tag v-if="['resultFlag', 'checkResult'].includes(column.key)" :color="resultMeta(item[column.key]).color">
                        {{ resultMeta(item[column.key]).text }}
                      </Tag>
                      <span v-else>{{ formatText(item[column.key]) }}</span>
                    </td>
                  </tr>
                  <tr v-if="!detailRows.length">
                    <td :colspan="detailColumnCount" class="press-slot-empty-cell" align="center">暂无明细数据</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  </component>
</template>

<style scoped>
.press-slot-station-record-page,
.press-slot-record-layout,
.press-slot-detail-modal,
.press-slot-detail-body,
.press-slot-detail-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.press-slot-record-layout {
  gap: 8px;
  height: 100%;
}

.press-slot-record-link {
  padding: 0;
  color: #1677ff;
  font: inherit;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.press-slot-record-link:hover {
  text-decoration: underline;
}

.press-slot-detail-modal {
  height: 100vh;
  background: #f5f7fa;
}

.press-slot-detail-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.press-slot-detail-toolbar__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: baseline;
  min-width: 0;
}

.press-slot-detail-toolbar__main {
  color: #172033;
  font-size: 18px;
  font-weight: 800;
}

.press-slot-detail-toolbar__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #334155;
  font-size: 13px;
}

.press-slot-detail-toolbar__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
  align-items: center;
}

.press-slot-detail-body {
  flex: 1;
  gap: 10px;
  padding: 10px;
  overflow: hidden;
}

.press-slot-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.press-slot-fieldset--compact {
  padding-bottom: 8px;
}

.press-slot-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 800;
}

.press-slot-head-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid #d8e0ea;
  border-left: 1px solid #d8e0ea;
}

.press-slot-head-item {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  min-height: 34px;
  border-right: 1px solid #d8e0ea;
  border-bottom: 1px solid #d8e0ea;
}

.press-slot-head-item__label,
.press-slot-head-item__value {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 5px 8px;
}

.press-slot-head-item__label {
  justify-content: flex-end;
  color: #334155;
  font-weight: 800;
  background: #eef2f7;
  border-right: 1px solid #d8e0ea;
}

.press-slot-head-item__value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.press-slot-import-attachment {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.press-slot-import-attachment__label {
  color: #1677ff;
  font-weight: 800;
}

.press-slot-import-attachment__item {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  align-items: center;
  min-width: 0;
}
.press-slot-detail-panel {
  flex: 1;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.press-slot-detail-panel__header {
  flex-shrink: 0;
  min-height: 34px;
  padding: 7px 10px;
  color: #1677ff;
  font-weight: 800;
  background: #eef2f7;
  border-bottom: 1px solid #d8e0ea;
}

.press-slot-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.press-slot-grid {
  width: 100%;
  min-width: 1180px;
  border-collapse: collapse;
  table-layout: fixed;
}

.press-slot-grid th,
.press-slot-grid td {
  min-height: 30px;
  padding: 4px 6px;
  color: #172033;
  vertical-align: middle;
  border: 1px solid #d8e0ea;
}

.press-slot-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  font-weight: 800;
  text-align: center;
  background: #eef2f7;
}

.press-slot-empty-cell {
  color: #94a3b8;
}

.press-slot-depth-fieldset {
  flex-shrink: 0;
  padding: 8px 10px 10px;
  margin: 0;
  background: linear-gradient(180deg, #f8fafc 0%, #edf2f7 100%);
  border: 1px solid #8794a4;
}

.press-slot-depth-fieldset legend {
  width: auto;
  padding: 0 8px;
  margin-left: 4px;
  color: #075985;
  font-size: 14px;
  font-weight: 800;
}

.press-slot-depth-matrix {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid #c6d0dc;
  border-left: 1px solid #c6d0dc;
}

.press-slot-depth-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  min-width: 0;
  padding: 4px 8px;
  color: #172033;
  font-weight: 700;
  background: #fff;
  border-right: 1px solid #c6d0dc;
  border-bottom: 1px solid #c6d0dc;
}

.press-slot-depth-cell--head,
.press-slot-depth-cell--axis {
  color: #334155;
  font-weight: 800;
  background: linear-gradient(180deg, #d7dee7 0%, #cbd5e1 100%);
}

:global(.press-slot-station-record-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
}

:global(.press-slot-station-record-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  padding: 0;
  overflow: hidden;
  border-radius: 0;
}

:global(.press-slot-station-record-modal .ant-modal-body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0;
  overflow: hidden;
}

:global(.press-slot-station-record-modal .rough-middle-sheet) {
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
}
.press-slot-runtime-view {
  height: calc(100vh - 42px);
  padding: 12px;
  overflow: auto;
  background: #edf2f7;
}

.press-slot-runtime-view :deep(.station-form-runtime) {
  min-height: calc(100vh - 66px);
}
</style>
