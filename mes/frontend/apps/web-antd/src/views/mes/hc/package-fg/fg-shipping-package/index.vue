<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessFormApi } from '#/api/mes/hc/processform';
import type { MesHcFinishedPackagingApi } from '#/api/mes/hc/package-fg/finished-packaging';
import type { MesHcStationFormApi } from '#/api/mes/hc/stationform';
import type { MesHcToolingConsumableLedgerApi } from '#/api/mes/hc/tooling-consumable-ledger';
import type { MesHcVisualPrintDesignerApi } from '#/api/mes/hc/visualprintdesigner';

import { computed, nextTick, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Button,
  Checkbox,
  DatePicker,
  Image,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Tabs,
  TabPane,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid, VxeColumn, VxeTable } from '#/adapter/vxe-table';
import {
  getProcessFormRecordDetail,
  getProcessFormRecordPage,
  updateProcessFormRecord,
  confirmProcessFormRecordBySigner,
} from '#/api/mes/hc/processform';
import {
  completeShippingNotice,
  getShippingNotice,
  getShippingNoticePage,
  packShippingNotice,
  pushShippingNoticeOqc,
} from '#/api/mes/hc/package-fg/finished-packaging';
import {
  getStationFormDetail,
  getStationFormSimpleList,
} from '#/api/mes/hc/stationform';
import { getToolingConsumableBalancePage } from '#/api/mes/hc/tooling-consumable-ledger';
import {
  getVisualPrintCustomerInfoList,
  getVisualPrintDesigns,
} from '#/api/mes/hc/visualprintdesigner';
import StationFormRuntimeRenderer from '#/views/mes/hc/stationform/modules/StationFormRuntimeRenderer.vue';
import StationFormRuntimeFillModal from '#/views/mes/hc/stationform/modules/runtime-fill-modal.vue';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';
import { resolveVisualPrintImageUrl } from '#/views/mes/hc/execution/visual-print-designer/mock-data';

import '../shared/cut-round-board.css';

defineOptions({ name: 'MesPackageFgShippingPackage' });

type ShippingNotice = MesHcFinishedPackagingApi.ShippingNotice;
type PackageAuxBalance = MesHcToolingConsumableLedgerApi.Balance;
type PackageAuxConsumeFormItem = {
  consumeQty?: number;
  ledgerId?: number;
};
type ShippingNoticeItem = MesHcFinishedPackagingApi.ShippingNoticeItem & {
  rowNo?: number;
};
type ShippingNoticePickItem =
  MesHcFinishedPackagingApi.ShippingNoticePickItem & { rowNo?: number };
type VisualPrintCustomerInfo = MesHcVisualPrintDesignerApi.CustomerInfo;
type VisualPrintDesign = MesHcVisualPrintDesignerApi.Design;
type VisualLabelKind = MesHcVisualPrintDesignerApi.LabelKind;
type MainTabKey =
  | 'wait'
  | 'pendingInspection'
  | 'pendingPackage'
  | 'completed'
  | 'all'
  | 'startupCheck'
  | 'cleaningCheck'
  | 'innerCheck'
  | 'outerCheck';
type ProcessFormEditorMode = 'edit' | 'view';
type RuntimeViewSignerAction = 'CONFIRM' | 'SAVE';
type ShippingProcessFormAction = 'CLEANING' | 'INNER' | 'OUTER' | 'STARTUP';
type ShippingDailyCheckCardStatus = 'CONFIRMED' | 'PENDING_CONFIRM' | 'UNFILLED';
type ShippingDailyCheckCard = {
  action: ShippingProcessFormAction;
  formName: string;
  record?: MesHcProcessFormApi.Record;
  status: ShippingDailyCheckCardStatus;
  timing: string;
  title: string;
};
type VisualPrintScope = 'DETAIL' | 'GROUP';
type VisualPrintStatusValue = 'FAILED' | 'IDLE' | 'PRINTING' | 'SUCCESS';
type InnerPackageGroupMode = 'batch' | 'none' | 'packageSlice';
type ShippingPackageGroup = {
  actualSliceText: string;
  auxMaterialName: string;
  customerBatchText: string;
  itemCount: number;
  items: ShippingNoticeItem[];
  oqcNo: string;
  oqcStatus: string;
  packageKey: string;
  packageMethod: string;
  packageName: string;
  packageNo: string;
  packageTime?: string;
  rowNo: number;
};
type InnerPackageDisplayRow = Partial<ShippingNoticeItem> & {
  __group?: boolean;
  children?: InnerPackageDisplayRow[];
  groupCount?: number;
  groupKey?: string;
  groupLabel?: string;
  groupMode?: InnerPackageGroupMode;
  groupSelectableCount?: number;
  groupSelectedCount?: number;
  innerDisplayRowKey: string;
};
type VisualPrintAction = {
  kind: VisualLabelKind;
  packageGroup?: ShippingPackageGroup;
  row?: ShippingNoticeItem;
  scope: VisualPrintScope;
  title: string;
};
type VisualPrintCandidate = {
  design: VisualPrintDesign;
  key: string;
  row: VisualPrintCustomerInfo;
};
type VisualPrintDesignerElement = {
  fieldKey?: string;
  type?: string;
  [key: string]: any;
};
type VisualPrintDesignerDraft = {
  dpi?: number;
  elements?: VisualPrintDesignerElement[];
  heightMm?: number;
  labelKind?: string;
  labelName?: string;
  printRotation?: number;
  rendererTemplate?: Record<string, any>;
  widthMm?: number;
  [key: string]: any;
};
type VisualPrintMissingField = {
  fieldKey: string;
  label: string;
};
type VisualPrintState = {
  count: number;
  message?: string;
  status: VisualPrintStatusValue;
  templateName?: string;
  time?: string;
};

const PACKAGING_PROCESS_CODE = 'PACKAGING';
const SHIPPING_PROCESS_NAME = '发货包装';
const INNER_PACKAGING_FORM_TYPE = 'INNER_PACKAGING_CHECK';
const OUTER_PACKAGING_FORM_TYPE = 'OUTER_PACKAGING_CHECK';
const STARTUP_CHECK_TAB = 'startupCheck';
const CLEANING_CHECK_TAB = 'cleaningCheck';
const INNER_CHECK_TAB = 'innerCheck';
const OUTER_CHECK_TAB = 'outerCheck';
const SHIPPING_CHECK_TABS = [
  STARTUP_CHECK_TAB,
  CLEANING_CHECK_TAB,
  INNER_CHECK_TAB,
  OUTER_CHECK_TAB,
];
const SHIPPING_DAILY_CHECK_ACTIONS: ShippingProcessFormAction[] = [
  'STARTUP',
  'CLEANING',
  'INNER',
  'OUTER',
];
const VISUAL_PRINT_AGENT_URL = 'http://127.0.0.1:18081';
const VISUAL_PRINT_OUTPUT_MODE = 'windows_raw';
const VISUAL_PRINT_INPUT_STORAGE_KEY =
  'hc-mes:fg-shipping-package:visual-print-inputs:v1';
const VISUAL_LABEL_KIND_META: Record<
  VisualLabelKind,
  { shortTitle: string; title: string }
> = {
  boxFront: { shortTitle: '盒正标', title: '打印盒正标' },
  cleanBag: { shortTitle: '洁净袋', title: '打印洁净袋' },
  customerSide: { shortTitle: '客户侧标', title: '打印客户侧标' },
  padBack: { shortTitle: 'PAD背标', title: '打印PAD背标' },
};
const VISUAL_LABEL_KINDS: VisualLabelKind[] = [
  'padBack',
  'cleanBag',
  'boxFront',
  'customerSide',
];
const VISUAL_PRINT_FIELD_LABELS: Record<string, string> = {
  batchNo: '生产批号 Batch No.',
  customer: '客户',
  customerSideMethod: '客户侧标方式',
  customerSideSize: '客户侧标尺寸',
  customerSideTemplate: '客户侧标模板',
  deliveryNote: '送货单',
  expDate: 'EXP Date',
  expirationDate: '有效日期 Expiration Date',
  hasMark: '是否有唛头',
  info: 'INFO',
  materialDescription: 'Material Description',
  needEcoa: '是否需要ECOA',
  needPaperCoa: '是否需要随货纸版COA',
  packageIndex: 'PKG of TTL',
  padNo: '片号 Pad No.',
  plant: 'Plant',
  pn: 'PN',
  po: 'PO',
  productInfo: '产品信息 Product Infor.',
  productNo: '产品编码 Product No.',
  productType: '产品类型',
  productionDate: '生产日期 Production Date',
  purUom: 'PUR UOM',
  quantity: '数量 Quantity',
  serialNo: '序号',
  shipmentFilePackageMethod: '随货文件包装方式',
  shippingDate: 'Shipping Date',
  shippingMethod: '发货方式',
  sizeMm: '尺寸/mm',
  specialRemark: '特殊备注',
  vendorPn: 'Vendor PN',
};
const SHIPPING_PROCESS_FORMS: Record<
  ShippingProcessFormAction,
  {
    cardTitle: string;
    formCode: string;
    formType: string;
    formTypeName: string;
    timing: string;
    title: string;
  }
> = {
  CLEANING: {
    cardTitle: '清洁点检表',
    formCode: 'PACKAGING_CLEANING_CHECK_DEV',
    formType: 'CLEANING_CHECK',
    formTypeName: '内包装清洁保养',
    timing: '清洁后/开机前',
    title: 'CMP软垫内包装设备清洁点检表',
  },
  INNER: {
    cardTitle: '内包装点检表',
    formCode: 'PACKAGING_INNER_PROCESS_CHECK_DEV',
    formType: INNER_PACKAGING_FORM_TYPE,
    formTypeName: '内包装',
    timing: '内包装',
    title: 'CMP软垫内包装点检表',
  },
  OUTER: {
    cardTitle: '外包装点检表',
    formCode: 'SHIPPING_OUTER_PROCESS_CHECK_DEV',
    formType: OUTER_PACKAGING_FORM_TYPE,
    formTypeName: '外包装',
    timing: '外包装',
    title: 'CMP软垫外包装点检表',
  },
  STARTUP: {
    cardTitle: '开机点检表',
    formCode: 'PACKAGING_STARTUP_CHECK_DEV',
    formType: 'STARTUP_CHECK',
    formTypeName: '内包装开机点检',
    timing: '开机前',
    title: 'CMP软垫内包装开机点检表',
  },
};

const userStore = useUserStore();
const currentUserName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '系统',
);

const mainTabs: Array<{
  key: MainTabKey;
  noticeStatus: string;
  tableTitle: string;
  title: string;
}> = [
  {
    key: 'wait',
    noticeStatus: 'INSPECTED',
    tableTitle: '待推送 OQC 发货需求单',
    title: '待推检',
  },
  {
    key: 'pendingInspection',
    noticeStatus: 'OQC_INSPECTING',
    tableTitle: '待检验发货需求单',
    title: '待检验',
  },
  {
    key: 'pendingPackage',
    noticeStatus: 'PENDING_OUTER_PACKAGING',
    tableTitle: '待外包装发货需求单',
    title: '待外包装',
  },
  {
    key: 'completed',
    noticeStatus: 'CLOSED',
    tableTitle: '已完成发货包装需求单',
    title: '已完成',
  },
  {
    key: 'all',
    noticeStatus: 'SHIPPING_PACKAGE_ALL',
    tableTitle: '发货包装相关需求单',
    title: '全部',
  },
];
const activeMainTab = ref<MainTabKey>('wait');
const activeDetailTab = ref('items');
const showShippingDailyRecordTabs = ref(false);
const query = reactive({ keyword: '' });
const noticeTotal = ref(0);
const detailVisible = ref(false);
const detailMaximized = ref(false);
const detailLoading = ref(false);
const packageLoading = ref(false);
const visualPrintLoading = ref(false);
const currentNotice = ref<ShippingNotice>();
const currentNoticeCompleted = computed(() =>
  isCompletedStatus(currentNotice.value?.noticeStatus),
);
const selectedInnerKeys = ref<number[]>([]);
const innerPackageGroupMode = ref<InnerPackageGroupMode>('batch');
watch(() => currentNotice.value?.productType, (type) => {
  innerPackageGroupMode.value = type === 'SAMPLE' ? 'none' : 'batch';
});
const visualTemplatePickerVisible = ref(false);
const visualTemplateCandidates = ref<VisualPrintCandidate[]>([]);
const visualTemplateKeyword = ref('');
const visualSelectedTemplateKey = ref('');
const visualTemplatePickerHint = ref('');
const visualPendingAction = ref<VisualPrintAction>();
const visualMissingVisible = ref(false);
const visualMissingFields = ref<VisualPrintMissingField[]>([]);
const visualMissingInputs = reactive<Record<string, string>>({});
const visualPendingCandidate = ref<VisualPrintCandidate>();
const visualPendingData = ref<Record<string, string>>({});
const visualPrintStates = reactive<Record<string, VisualPrintState>>({});
const visualSavedInputs = reactive<Record<string, Record<string, string>>>(
  loadVisualSavedInputs(),
);
const packageCreateVisible = ref(false);
const packageSubmitting = ref(false);
const shippingDailyCheckDialogVisible = ref(false);
const shippingDailyCheckCards = ref<ShippingDailyCheckCard[]>([]);
const shippingDailyCheckLoading = ref(false);
const runtimeFillOpen = ref(false);
const runtimeFillAction = ref<ShippingProcessFormAction>('OUTER');
const runtimeFillForm = ref<MesHcStationFormApi.StationForm | null>(null);
const runtimeInitialParams = ref<Record<string, any>>({});
const runtimeViewVisible = ref(false);
const runtimeViewLoading = ref(false);
const runtimeViewSaving = ref(false);
const runtimeViewConfirming = ref(false);
const runtimeViewMode = ref<ProcessFormEditorMode>('view');
const runtimeViewSignerAction = ref<RuntimeViewSignerAction>('SAVE');
const runtimeViewSignerRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeViewSignerVisible = ref(false);
const runtimeViewRecord = ref<MesHcProcessFormApi.Record | null>(null);
const runtimeViewHeaderData = ref<Record<string, any>>({});
const runtimeViewSchema = ref<Record<string, any>>({});
const runtimeRendererRef = ref<any>();
const packageForm = reactive({
  auxConsumeItems: [{}] as PackageAuxConsumeFormItem[],
  packageMethod: '纸盒',
  remark: '',
});
const packageAuxBalances = ref<PackageAuxBalance[]>([]);
const packageAuxLoading = ref(false);
const shippingDailyCheckQuery = reactive({
  recordDate: dayjs().format('YYYY-MM-DD'),
});
const packageMethodOptions = [
  { label: '纸盒', value: '纸盒' },
  { label: '纸板', value: '纸板' },
];
const innerPackageGroupModeOptions: Array<{
  label: string;
  value: InnerPackageGroupMode;
}> = [
  { label: '不分组', value: 'none' },
  { label: '产品批号', value: 'batch' },
  { label: '包装片号', value: 'packageSlice' },
];

const filteredVisualTemplateCandidates = computed(() => {
  const keyword = visualTemplateKeyword.value.trim().toLowerCase();
  if (!keyword) return visualTemplateCandidates.value;
  return visualTemplateCandidates.value.filter((item) => {
    const row = item.row;
    const design = item.design;
    return [
      buildVisualTemplateOptionLabel(item),
      getVisualCandidateSourceText(item),
      row.customer,
      row.productType,
      row.sizeMm,
      design.labelKind,
      design.labelName,
      design.id,
    ].some((value) =>
      String(value || '')
        .toLowerCase()
        .includes(keyword),
    );
  });
});
const visualPendingActionTitle = computed(
  () => visualPendingAction.value?.title || '可视化标签打印',
);

const detailItems = computed<ShippingNoticeItem[]>(() =>
  (currentNotice.value?.items || []).map((item, index) => ({
    ...item,
    rowNo: index + 1,
  })),
);
const innerPackageTreeConfig = computed(() =>
  (currentNotice.value?.productType === 'SAMPLE' || innerPackageGroupMode.value === 'none')
    ? undefined
    : {
        children: 'children',
        expandAll: true,
        reserve: true,
        showLine: true,
      },
);
const innerPackageDisplayRows = computed<InnerPackageDisplayRow[]>(() => {
  const rows = detailItems.value.map((item) => toInnerPackageDisplayRow(item));
  const mode = currentNotice.value?.productType === 'SAMPLE' ? 'none' : innerPackageGroupMode.value;
  if (mode === 'none') return rows;
  const groupMap = new Map<string, InnerPackageDisplayRow[]>();
  for (const row of rows) {
    const key = getInnerPackageGroupKey(row, mode);
    const children = groupMap.get(key) || [];
    children.push(row);
    groupMap.set(key, children);
  }
  return Array.from(groupMap.entries()).map(([groupKey, children], index) =>
    buildInnerPackageGroupRow(mode, groupKey, children, index),
  );
});
const pickItems = computed<ShippingNoticePickItem[]>(() =>
  (currentNotice.value?.pickItems || []).map((item, index) => ({
    ...item,
    rowNo: index + 1,
  })),
);
const selectedInnerItems = computed(() =>
  detailItems.value.filter(
    (item) =>
      selectedInnerKeys.value.includes(item.id) && !!item.actualSliceBatchNo,
  ),
);
const oqcRows = computed(() =>
  detailItems.value.filter(
    (item) =>
      item.oqcOrderId ||
      item.shippingQualityNo ||
      ['OQC_INSPECTING', 'OQC_PASSED', 'OQC_REJECTED', 'PACKAGED', 'CLOSED'].includes(
        currentNotice.value?.noticeStatus || '',
      ),
  ),
);
const packageGroups = computed<ShippingPackageGroup[]>(() => {
  const groupMap = new Map<string, ShippingNoticeItem[]>();
  for (const item of detailItems.value) {
    const packageKey = packageKeyOf(item);
    if (!packageKey) continue;
    const rows = groupMap.get(packageKey) || [];
    rows.push(item);
    groupMap.set(packageKey, rows);
  }
  return Array.from(groupMap.entries()).map(([packageKey, rows], index) => ({
    actualSliceText: fullJoined(
      rows.map((row) => sliceNoAFront3(row.actualSliceBatchNo)),
    ),
    auxMaterialName: extractAuxMaterialName(rows),
    customerBatchText: fullJoined(
      rows.map((row) => row.customerProductBatchNo || row.customerSliceBatchNo),
    ),
    itemCount: rows.length,
    items: rows,
    oqcNo: compactJoined(
      rows.map((row) => row.shippingQualityNo),
      2,
    ),
    oqcStatus: firstText(...rows.map((row) => row.oqcStatus)),
    packageKey,
    packageMethod: extractPackageMethod(rows, '-'),
    packageName:
      firstText(...rows.map((row) => row.shippingPackageName)) || '-',
    packageNo: packageKey,
    packageTime: firstText(...rows.map((row) => row.shippingPackageTime)),
    rowNo: index + 1,
  }));
});
const packageAuxOptions = computed(() =>
  packageAuxBalances.value.map((item) => ({
    label: `${item.consumableTypeName || '-'}${item.model ? ` / ${item.model}` : ''}｜批次 ${item.batchNo || '-'}｜可用 ${formatPackageAuxQty(item.balanceQty)} ${item.uomName || item.uom || '个'}`,
    value: item.ledgerId,
  })),
);
const currentMainTab = computed(
  () => mainTabs.find((tab) => tab.key === activeMainTab.value) || mainTabs[0],
);
const isShippingCheckTab = computed(() =>
  SHIPPING_CHECK_TABS.includes(String(activeMainTab.value)),
);
const activeShippingCheckAction = computed<ShippingProcessFormAction>(() => {
  if (activeMainTab.value === STARTUP_CHECK_TAB) return 'STARTUP';
  if (activeMainTab.value === CLEANING_CHECK_TAB) return 'CLEANING';
  if (activeMainTab.value === INNER_CHECK_TAB) return 'INNER';
  return 'OUTER';
});
const activeShippingCheckForm = computed(
  () => SHIPPING_PROCESS_FORMS[activeShippingCheckAction.value],
);
const runtimeViewAction = computed(() =>
  resolveProcessFormAction(runtimeViewRecord.value || {}),
);
const runtimeViewReadOnly = computed(
  () =>
    runtimeViewMode.value === 'view' ||
    runtimeViewRecord.value?.recordStatus === 'CONFIRMED',
);
const runtimeViewTitle = computed(
  () =>
    runtimeViewRecord.value?.templateName ||
    SHIPPING_PROCESS_FORMS[runtimeViewAction.value].title,
);
const runtimeViewRecordMeta = computed(() => [
  `工序：${runtimeViewRecord.value?.processName || SHIPPING_PROCESS_NAME}`,
  `类型：${runtimeViewRecord.value?.formTypeName || SHIPPING_PROCESS_FORMS[runtimeViewAction.value].formTypeName}`,
  `状态：${recordStatusText(runtimeViewRecord.value?.recordStatus)}`,
  `上报日期：${runtimeViewRecord.value?.recordDate || '-'}`,
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

function dailyCheckCardStatus(
  record?: MesHcProcessFormApi.Record,
): ShippingDailyCheckCardStatus {
  if (!record) {
    return 'UNFILLED';
  }
  return record.recordStatus === 'CONFIRMED' ? 'CONFIRMED' : 'PENDING_CONFIRM';
}

function dailyCheckCardStatusText(status: ShippingDailyCheckCardStatus) {
  const map: Record<ShippingDailyCheckCardStatus, string> = {
    CONFIRMED: '已确认',
    PENDING_CONFIRM: '待确认',
    UNFILLED: '未填写',
  };
  return map[status];
}

function dailyCheckCardStatusColor(status: ShippingDailyCheckCardStatus) {
  const map: Record<ShippingDailyCheckCardStatus, string> = {
    CONFIRMED: 'green',
    PENDING_CONFIRM: 'gold',
    UNFILLED: 'orange',
  };
  return map[status];
}

function selectShippingDailyCheckRecord(records: MesHcProcessFormApi.Record[]) {
  return records.find((record) => record.recordStatus !== 'VOID');
}

async function loadShippingDailyCheckCards() {
  shippingDailyCheckLoading.value = true;
  try {
    const recordDate = dayjs().format('YYYY-MM-DD');
    const pages = await Promise.all(
      SHIPPING_DAILY_CHECK_ACTIONS.map((action) =>
        getProcessFormRecordPage({
          formType: SHIPPING_PROCESS_FORMS[action].formType,
          pageNo: 1,
          pageSize: 100,
          processCode: PACKAGING_PROCESS_CODE,
          recordDate,
        }),
      ),
    );
    shippingDailyCheckCards.value = SHIPPING_DAILY_CHECK_ACTIONS.map(
      (action, index) => {
        const config = SHIPPING_PROCESS_FORMS[action];
        const record = selectShippingDailyCheckRecord(
          (pages[index]?.list || []) as MesHcProcessFormApi.Record[],
        );
        return {
          action,
          formName: config.title,
          record,
          status: dailyCheckCardStatus(record),
          timing: config.timing,
          title: config.cardTitle,
        };
      },
    );
  } finally {
    shippingDailyCheckLoading.value = false;
  }
}

function openQuickCheck(_type: ShippingProcessFormAction) {
  void openShippingDailyCheckDialog();
}

function openShippingCheckForm(type: ShippingProcessFormAction) {
  void openProcessFormEditor(type);
}

function toggleShippingDailyRecordTabs() {
  showShippingDailyRecordTabs.value = !showShippingDailyRecordTabs.value;
  if (!showShippingDailyRecordTabs.value && isShippingCheckTab.value) {
    activeMainTab.value = 'wait';
  }
}

async function openShippingDailyCheckDialog() {
  shippingDailyCheckQuery.recordDate = dayjs().format('YYYY-MM-DD');
  shippingDailyCheckDialogVisible.value = true;
  try {
    await loadShippingDailyCheckCards();
  } catch (error: any) {
    message.error(error?.message || '加载今日点检记录失败');
  }
}

function openShippingDailyCheckCard(
  card: ShippingDailyCheckCard,
  mode: ProcessFormEditorMode = 'edit',
) {
  void openProcessFormEditor(card.action, card.record, mode);
}

const noticeColumns = [
  {
    field: 'noticeNo',
    fixed: 'left',
    showOverflow: 'tooltip',
    title: '需求单号',
    width: 180,
  },
  {
    field: 'productType',
    slots: { default: 'productType' },
    title: '类型',
    width: 100,
  },
  {
    field: 'requirement',
    minWidth: 260,
    slots: { default: 'requirement' },
    title: '发货要求',
  },
  {
    align: 'center',
    field: 'noticeStatus',
    slots: { default: 'noticeStatus' },
    title: '打包状态',
    width: 130,
  },
  {
    field: 'shippingPackageName',
    slots: { default: 'shippingPackageName' },
    title: '打包记录人',
    width: 120,
  },
  {
    field: 'shippingPackageTime',
    slots: { default: 'shippingPackageTime' },
    title: '记录时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 110,
  },
];

const shippingCheckColumns = [
  { field: 'recordNo', fixed: 'left', title: '记录编号', width: 190 },
  {
    field: 'templateName',
    minWidth: 230,
    showOverflow: 'tooltip',
    title: '表单名称',
  },
  { field: 'recordDate', title: '上报日期', width: 120 },
  { field: 'modelCode', title: '型号', width: 130 },
  { field: 'batchNo', title: '批号', width: 150 },
  {
    field: 'resultStatus',
    slots: { default: 'processResultStatus' },
    title: '结果',
    width: 90,
  },
  {
    field: 'recordStatus',
    slots: { default: 'processRecordStatus' },
    title: '状态',
    width: 100,
  },
  { field: 'fillUserName', title: '填写人', width: 110 },
  {
    field: 'fillTime',
    formatter: ({ row }: { row: MesHcProcessFormApi.Record }) =>
      formatDateTime(row.fillTime || row.createTime),
    title: '填写时间',
    width: 160,
  },
  { field: 'confirmUserName', title: '确认人', width: 110 },
  {
    field: 'confirmTime',
    formatter: ({ row }: { row: MesHcProcessFormApi.Record }) =>
      formatDateTime(row.confirmTime),
    title: '确认时间',
    width: 160,
  },
  {
    align: 'center',
    field: 'action',
    fixed: 'right',
    slots: { default: 'shippingCheckActions' },
    title: '操作',
    width: 220,
  },
];

function formatDateTime(value?: string) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function noticeTypeText(value?: string) {
  const map: Record<string, string> = {
    MASS: '量产',
    RND: '研发',
    SAMPLE: '样品',
    研发: '研发',
    样品: '样品',
    量产: '量产',
  };
  return map[value || ''] || value || '-';
}

function noticeTypeColor(value?: string) {
  const map: Record<string, string> = {
    MASS: 'blue',
    RND: 'purple',
    SAMPLE: 'orange',
    研发: 'purple',
    样品: 'orange',
    量产: 'blue',
  };
  return map[value || ''] || 'default';
}

function packingStatusText(status?: string) {
  const map: Record<string, string> = {
    CLOSED: '已完成',
    INSPECTED: '发货检验完成',
    OQC_INSPECTING: 'OQC 待检验',
    OQC_PASSED: 'OQC 合格待外包装',
    OQC_REJECTED: 'OQC 不合格',
    OUTBOUND: '出库中',
    PACKAGED: '外包装完成待发货',
    PICKED: '已下架配货',
    SHIP_CONFIRMED: '出货已确认',
    SHIPPED: '已完成',
  };
  return map[status || ''] || status || '-';
}

function packingStatusColor(status?: string) {
  const map: Record<string, string> = {
    CLOSED: 'green',
    INSPECTED: 'purple',
    OQC_INSPECTING: 'processing',
    OQC_PASSED: 'cyan',
    OQC_REJECTED: 'red',
    OUTBOUND: 'processing',
    PACKAGED: 'cyan',
    PICKED: 'blue',
    SHIP_CONFIRMED: 'geekblue',
    SHIPPED: 'green',
  };
  return map[status || ''] || 'default';
}

function isCompletedStatus(status?: string) {
  return ['CLOSED', 'SHIPPED'].includes(status || '');
}

function qualityColor(value?: string) {
  if (value === 'OK') return 'green';
  if (value === 'NG') return 'red';
  return 'default';
}

function resultColor(value?: string) {
  return qualityColor(value);
}

function recordStatusText(status?: string) {
  const map: Record<string, string> = {
    CONFIRMED: '已确认',
    DRAFT: '草稿',
    SUBMITTED: '已提交',
    VOID: '已作废',
  };
  return map[status || ''] || status || '-';
}

function safeParseJson<T = any>(value?: null | string): T | null {
  if (!value) return null;
  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function schemaText(schema: Record<string, any>, ...keys: string[]) {
  for (const key of keys) {
    const value = schema?.[key];
    if (Array.isArray(value)) {
      const text = value
        .map((item) => String(item ?? '').trim())
        .filter(Boolean)
        .join(',');
      if (text) return text;
      continue;
    }
    const text = String(value ?? '').trim();
    if (text) return text;
  }
  return '';
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

function resolveProcessFormAction(
  record: Partial<MesHcProcessFormApi.Record>,
): ShippingProcessFormAction {
  if (record.formType === 'STARTUP_CHECK') return 'STARTUP';
  if (record.formType === 'CLEANING_CHECK') return 'CLEANING';
  if (record.formType === INNER_PACKAGING_FORM_TYPE) return 'INNER';
  return 'OUTER';
}

async function loadStationTemplate(action: ShippingProcessFormAction) {
  const config = SHIPPING_PROCESS_FORMS[action];
  const forms = await getStationFormSimpleList(PACKAGING_PROCESS_CODE);
  const target =
    forms.find((item) => item.formCode === config.formCode) ||
    forms.find((item) => {
      const schema = safeParseJson<Record<string, any>>(item.schemaJson) || {};
      return (
        schemaText(schema, 'processFormType', 'formType') === config.formType
      );
    });
  if (!target?.id) {
    throw new Error(`未找到 DEV 模板：${config.formCode}`);
  }
  return getStationFormDetail(target.id);
}

function stationFormType(
  form: MesHcStationFormApi.StationForm,
  action: ShippingProcessFormAction,
) {
  const schema = safeParseJson<Record<string, any>>(form.schemaJson) || {};
  return (
    schemaText(schema, 'processFormType', 'formType') ||
    SHIPPING_PROCESS_FORMS[action].formType
  );
}

function stationFormTypeName(
  form: MesHcStationFormApi.StationForm,
  action: ShippingProcessFormAction,
) {
  const schema = safeParseJson<Record<string, any>>(form.schemaJson) || {};
  return (
    schemaText(schema, 'formTypeName', 'processFormTypeName') ||
    SHIPPING_PROCESS_FORMS[action].formTypeName
  );
}

function buildNowText() {
  return dayjs().format('YYYY-MM-DD HH:mm:ss');
}

function buildRuntimeInitialParams(
  form: MesHcStationFormApi.StationForm,
  action: ShippingProcessFormAction,
) {
  const recordDate =
    shippingDailyCheckQuery.recordDate || dayjs().format('YYYY-MM-DD');
  const formType = stationFormType(form, action);
  const formTypeName = stationFormTypeName(form, action);
  const notice = currentNotice.value;
  const isOuterPackagingCheck = action === 'OUTER';
  const packageBoxQty = packageGroups.value.length
    ? String(packageGroups.value.length)
    : '';
  return {
    batchNo: isOuterPackagingCheck
      ? ''
      : firstText(notice?.requiredBatchNo, notice?.noticeNo),
    checkerName: currentUserName.value,
    fillUserName: currentUserName.value,
    formType,
    formTypeName,
    materialCode: isOuterPackagingCheck
      ? ''
      : firstText(notice?.externalProductCode, notice?.materialCode),
    modelCode: isOuterPackagingCheck
      ? 'COMMON'
      : firstText(notice?.externalProductModel, notice?.modelCode, 'COMMON'),
    month: dayjs(recordDate).format('YYYY-MM'),
    noticeId: isOuterPackagingCheck ? undefined : notice?.id,
    noticeNo: isOuterPackagingCheck ? '' : notice?.noticeNo,
    packageBoxQty: isOuterPackagingCheck ? '' : packageBoxQty,
    packageProductQty: isOuterPackagingCheck
      ? ''
      : firstText(notice?.requiredShipQty, notice?.noticeQty),
    packagingType: action === 'OUTER' ? 'OUTER' : 'INNER',
    packagingTypeName: action === 'OUTER' ? '外包装' : '内包装',
    planNo: isOuterPackagingCheck ? '' : notice?.noticeNo,
    processCode: PACKAGING_PROCESS_CODE,
    processName: SHIPPING_PROCESS_NAME,
    productSpec: isOuterPackagingCheck
      ? ''
      : firstText(
          (notice as any)?.productSpec,
          notice?.requiredSliceRange,
          notice?.packingRequirement,
        ),
    productionDate: recordDate,
    recordDate,
    recordUserName: currentUserName.value,
    recorder: currentUserName.value,
    recorderName: currentUserName.value,
    workshop: action === 'OUTER' ? '外包装' : '内包装',
  };
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
    ...(safeParseJson<Record<string, any>>(record?.headerDataJson) || {}),
  };
  header.formType = firstText(header.formType, record?.formType);
  header.formTypeName = firstText(header.formTypeName, record?.formTypeName);
  header.processCode = firstText(
    header.processCode,
    record?.processCode,
    PACKAGING_PROCESS_CODE,
  );
  header.processName = firstText(
    header.processName,
    record?.processName,
    SHIPPING_PROCESS_NAME,
  );
  header.recordDate = firstText(header.recordDate, record?.recordDate);
  if (!Array.isArray(header.previewDetails)) {
    header.previewDetails = buildRuntimeDetailRows(record);
  }
  return header;
}

function applyRuntimeUser(
  header: Record<string, any>,
  options: { confirmer?: boolean; recorder?: boolean; userName?: string },
) {
  const userName = firstText(options.userName, currentUserName.value);
  if (options.recorder) {
    header.recorder = userName;
    header.recorderName = userName;
    header.recordUserName = userName;
    header.checkerName = userName;
    header.fillUserName = userName;
  }
  if (options.confirmer) {
    header.confirmer = userName;
    header.confirmerName = userName;
    header.confirmUserName = userName;
  }
  return header;
}

async function loadRuntimeSchemaForRecord(record: MesHcProcessFormApi.Record) {
  const context = safeParseJson<Record<string, any>>(record.contextJson) || {};
  if (context.runtimeSchema?.runtimeLayout) {
    return context.runtimeSchema as Record<string, any>;
  }
  try {
    const form = await loadStationTemplate(resolveProcessFormAction(record));
    return safeParseJson<Record<string, any>>(form.schemaJson) || {};
  } catch {
    return context.runtimeSchema || {};
  }
}

async function refreshShippingDailyCheckGrid() {
  await shippingDailyCheckGridApi.query();
}

async function refreshShippingProcessFormGrid(
  _action: ShippingProcessFormAction,
) {
  if (showShippingDailyRecordTabs.value) {
    await refreshShippingDailyCheckGrid();
  }
  if (shippingDailyCheckDialogVisible.value) {
    await loadShippingDailyCheckCards();
  }
}

async function openProcessFormEditor(
  action: ShippingProcessFormAction,
  row?: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'edit',
) {
  runtimeFillAction.value = action;
  try {
    if (row?.id) {
      await openRuntimeView(row, mode);
      return;
    }

    const stationForm = await loadStationTemplate(action);
    runtimeFillForm.value = stationForm;
    runtimeInitialParams.value = buildRuntimeInitialParams(stationForm, action);
    runtimeFillOpen.value = true;
  } catch (error: any) {
    message.error(error?.message || '加载点检模板失败');
  }
}

async function handleRuntimeFillSaved() {
  await refreshShippingProcessFormGrid(runtimeFillAction.value);
}

async function openRuntimeView(
  row: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'view',
) {
  if (!row.id) return;
  runtimeViewLoading.value = true;
  runtimeViewVisible.value = true;
  runtimeViewMode.value = mode;
  try {
    const detail = await getProcessFormRecordDetail(row.id);
    runtimeViewRecord.value = detail;
    runtimeViewHeaderData.value = applyRuntimeUser(
      buildRuntimeHeaderData(detail),
      {
        confirmer: false,
        recorder: false,
      },
    );
    runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
  } catch (error: any) {
    runtimeViewVisible.value = false;
    message.error(error?.message || '加载点检表详情失败');
  } finally {
    runtimeViewLoading.value = false;
  }
}

function closeRuntimeView() {
  runtimeViewVisible.value = false;
  runtimeViewRecord.value = null;
  runtimeViewSignerRecord.value = null;
  runtimeViewHeaderData.value = {};
  runtimeViewSchema.value = {};
  runtimeViewMode.value = 'view';
}

function buildRuntimeUpdatePayload(
  record: MesHcProcessFormApi.Record,
  headerData: Record<string, any>,
  items?: MesHcProcessFormApi.RecordItem[],
  signerName?: string,
) {
  const { template: _template, ...payload } =
    record as MesHcProcessFormApi.Record & { template?: unknown };
  const runtimeItems = items || record.items || [];
  const resultStatus = runtimeItems.some(
    (item) => normalizeResultFlag(item.resultFlag) === 'NG',
  )
    ? 'NG'
    : 'OK';
  return {
    ...payload,
    fillTime: firstText(record.fillTime, buildNowText()),
    fillUserName: firstText(
      signerName,
      record.fillUserName,
      currentUserName.value,
    ),
    headerDataJson: JSON.stringify(headerData),
    items: runtimeItems,
    processCode: firstText(record.processCode, PACKAGING_PROCESS_CODE),
    processName: firstText(record.processName, SHIPPING_PROCESS_NAME),
    resultStatus,
  } as MesHcProcessFormApi.Record;
}

async function refreshRuntimeViewRecord(id: number) {
  const detail = await getProcessFormRecordDetail(id);
  runtimeViewRecord.value = detail;
  runtimeViewHeaderData.value = buildRuntimeHeaderData(detail);
  runtimeViewSchema.value = await loadRuntimeSchemaForRecord(detail);
  await refreshShippingProcessFormGrid(resolveProcessFormAction(detail));
  return detail;
}

function requestRuntimeViewSigner(
  action: RuntimeViewSignerAction,
  record = runtimeViewRecord.value,
) {
  if (!record?.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该点检表已确认');
    return;
  }
  runtimeViewSignerAction.value = action;
  runtimeViewSignerRecord.value = record;
  runtimeViewSignerVisible.value = true;
}

function resolveRuntimeSigner(userInfo: any) {
  const userId = Number(userInfo?.userId || 0);
  const userName = String(
    userInfo?.empName || userInfo?.nickname || userInfo?.username || '',
  ).trim();
  if (!userId || !userName) {
    message.warning('未识别到填写人员，请输入有效用户名完成确认');
    return undefined;
  }
  return { userId, userName };
}

async function handleRuntimeViewSignerSuccess(userInfo: any) {
  runtimeViewSignerVisible.value = false;
  const signer = resolveRuntimeSigner(userInfo);
  if (!signer) return;
  if (runtimeViewSignerAction.value === 'SAVE') {
    await saveRuntimeView(false, signer.userName);
    return;
  }
  const record = runtimeViewSignerRecord.value || runtimeViewRecord.value;
  if (record) {
    await confirmRuntimeRecord(record, signer);
  }
}

async function saveRuntimeView(silent = false, signerName?: string) {
  const record = runtimeViewRecord.value;
  if (!record?.id) return undefined;
  if (record.recordStatus === 'CONFIRMED') {
    message.warning('已确认的点检表不允许修改');
    return undefined;
  }
  runtimeViewSaving.value = true;
  try {
    const runtimeHeader =
      runtimeRendererRef.value?.getHeaderData?.() ||
      runtimeViewHeaderData.value;
    const effectiveSignerName = firstText(signerName, currentUserName.value);
    applyRuntimeUser(runtimeHeader, {
      confirmer: false,
      recorder: true,
      userName: effectiveSignerName,
    });
    const recordTime = buildNowText();
    runtimeHeader.recorderTime = recordTime;
    runtimeHeader.recordTime = recordTime;
    runtimeHeader.checkerTime = recordTime;
    const runtimeItems =
      runtimeRendererRef.value?.buildRecordItems?.() || record.items || [];
    await updateProcessFormRecord(
      buildRuntimeUpdatePayload(
        record,
        runtimeHeader,
        runtimeItems,
        effectiveSignerName,
      ),
    );
    if (!silent) {
      message.success('保存成功');
    }
    await refreshRuntimeViewRecord(record.id);
    return record.id;
  } finally {
    runtimeViewSaving.value = false;
  }
}

async function confirmRuntimeRecord(
  record: MesHcProcessFormApi.Record,
  signer?: { userId: number; userName: string },
) {
  if (!record.id) return;
  if (record.recordStatus === 'CONFIRMED') {
    message.info('该点检表已确认');
    return;
  }
  if (!signer) {
    requestRuntimeViewSigner('CONFIRM', record);
    return;
  }
  const confirmedRecordId = record.id;
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
    applyRuntimeUser(header, {
      confirmer: true,
      recorder: false,
      userName: signer.userName,
    });
    header.inspectionResult = firstText(
      header.inspectionResult,
      detail.resultStatus,
      'OK',
    );
    const confirmTime = buildNowText();
    header.confirmerTime = confirmTime;
    header.confirmTime = confirmTime;
    await updateProcessFormRecord(
      buildRuntimeUpdatePayload(detail, header, items, signer.userName),
    );
    await confirmProcessFormRecordBySigner({
      confirmUserId: signer.userId,
      id: confirmedRecordId,
    });
    message.success('确认成功');
    if (runtimeViewRecord.value?.id === confirmedRecordId) {
      await refreshRuntimeViewRecord(confirmedRecordId);
      runtimeViewMode.value = 'view';
    } else {
      await refreshShippingProcessFormGrid(resolveProcessFormAction(detail));
    }
  } finally {
    runtimeViewConfirming.value = false;
  }
}

function confirmShippingCheckRow(row: MesHcProcessFormApi.Record) {
  void confirmRuntimeRecord(row);
}

function openShippingCheckRecord(
  row: MesHcProcessFormApi.Record,
  mode: ProcessFormEditorMode = 'edit',
) {
  void openProcessFormEditor(resolveProcessFormAction(row), row, mode);
}

function oqcStatusText(value?: string) {
  const map: Record<string, string> = {
    CANCELED: '已取消',
    COMPLETED: '已完成',
    INSPECTING: '检验中',
    PENDING: '待检验',
    REJECTED: '不合格',
    WAITING_QA: '待判定',
  };
  return map[value || ''] || value || '未生成';
}

function oqcStatusColor(value?: string) {
  const map: Record<string, string> = {
    CANCELED: 'default',
    COMPLETED: 'green',
    INSPECTING: 'blue',
    PENDING: 'orange',
    REJECTED: 'red',
    WAITING_QA: 'purple',
  };
  return map[value || ''] || 'default';
}

function requirementText(row: ShippingNotice) {
  if (row.productType === 'SAMPLE') return `样品 ${row.requiredShipQty || row.noticeQty || 0}片`;
  if (row.productType === 'RND') {
    return (
      [
        row.customerName,
        row.externalProductModel,
        row.requiredBatchNo,
        row.requiredShipQty ? `${row.requiredShipQty}片` : '',
      ]
        .filter(Boolean)
        .join(' / ') || '-'
    );
  }
  return (
    [
      row.customerName,
      row.externalProductModel,
      row.externalProductCode,
      row.requiredSliceRange,
      row.requiredShipQty ? `${row.requiredShipQty}片` : '',
    ]
      .filter(Boolean)
      .join(' / ') || '-'
  );
}

function rowPackageName(row: ShippingNotice) {
  return (
    row.shippingPackageName ||
    (hasPackageRecordStatus(row.noticeStatus) ? row.recorderName : '') ||
    '-'
  );
}

function rowPackageTime(row: ShippingNotice) {
  return formatDateTime(
    row.shippingPackageTime ||
      (hasPackageRecordStatus(row.noticeStatus) ? row.recorderTime : undefined),
  );
}

function hasPackageRecordStatus(status?: string) {
  return ['CLOSED', 'OUTBOUND', 'PACKAGED', 'SHIPPED'].includes(status || '');
}

function itemLocationText(row?: ShippingNoticeItem | ShippingNoticePickItem) {
  const shelf = (row as any)?.actualLocationName || row?.locationName;
  const code = (row as any)?.actualLocationCode || row?.locationCode;
  return [code, shelf].filter(Boolean).join(' / ') || '-';
}

function firstText(...values: Array<number | string | undefined | null>) {
  const value = values.find(
    (item) => item !== undefined && item !== null && String(item).trim() !== '',
  );
  return value === undefined || value === null ? '' : String(value);
}

function packageKeyOf(row: ShippingNoticeItem) {
  if (!isShippingPackaged(row)) {
    return '';
  }
  return firstText(row.packageNo, row.outerBoxNo);
}

function isShippingPackaged(row: ShippingNoticeItem) {
  return (
    !!row.actualSliceBatchNo &&
    !!row.shippingPackageName &&
    !!row.shippingPackageTime
  );
}

function extractPackageRemarkValue(rows: ShippingNoticeItem[], label: string) {
  const remark =
    rows.map((row) => row.shippingPackageRemark || '').find(Boolean) || '';
  const matched = remark.match(new RegExp(`${label}[:：]\\s*([^；;]+)`));
  return matched?.[1]?.trim() || '';
}

function extractPackageMethod(
  rows: ShippingNoticeItem[],
  fallback = packageForm.packageMethod || '-',
) {
  return extractPackageRemarkValue(rows, '包装方式') || fallback;
}

function extractAuxMaterialName(rows: ShippingNoticeItem[]) {
  return extractPackageRemarkValue(rows, '耗材名称') || '-';
}

function formatPackageAuxQty(value?: number) {
  const qty = Number(value || 0);
  return Number.isFinite(qty) ? qty.toFixed(3) : '-';
}

function getPackageAuxAvailableQty(ledgerId?: number) {
  const balance = packageAuxBalances.value.find(
    (item) => Number(item.ledgerId) === Number(ledgerId),
  );
  if (!balance) {
    return '-';
  }
  return `${formatPackageAuxQty(balance.balanceQty)} ${balance.uomName || balance.uom || '个'}`;
}

async function loadPackageAuxStocks() {
  packageAuxLoading.value = true;
  try {
    const page = await getToolingConsumableBalancePage({
      onlyPositiveBalance: true,
      pageNo: 1,
      pageSize: 200,
      processCode: 'PACKAGING',
      usageStatus: 'ACTIVE',
    });
    packageAuxBalances.value = page.list || [];
  } catch {
    message.error('加载包装工序耗材领用台账失败，请稍后重试');
  } finally {
    packageAuxLoading.value = false;
  }
}

function buildPackageAuxConsumeItems() {
  if (packageForm.auxConsumeItems.length === 0) {
    message.warning('请至少选择一条外包装辅材');
    return undefined;
  }
  const usedLedgerIds = new Set<number>();
  const items: Array<{ consumeQty: number; ledgerId: number }> = [];
  for (const item of packageForm.auxConsumeItems) {
    const ledgerId = Number(item.ledgerId || 0);
    const consumeQty = Number(item.consumeQty || 0);
    if (ledgerId <= 0 || consumeQty <= 0) {
      message.warning('请完整填写外包装辅材批次和本次领用量');
      return undefined;
    }
    if (usedLedgerIds.has(ledgerId)) {
      message.warning('同一外包装辅材批次只能填写一行，请合并领用数量');
      return undefined;
    }
    usedLedgerIds.add(ledgerId);
    items.push({ consumeQty, ledgerId });
  }
  return items;
}

function compactJoined(values: Array<string | undefined>, limit = 3) {
  const filtered = Array.from(new Set(values.filter(Boolean) as string[]));
  if (filtered.length <= limit) return filtered.join('，') || '-';
  return `${filtered.slice(0, limit).join('，')}...`;
}

function fullJoined(values: Array<string | undefined>) {
  return (
    Array.from(new Set(values.filter(Boolean) as string[])).join('，') || '-'
  );
}

function sliceNoAFront3(value?: string) {
  const text = String(value || '').trim();
  if (!text) return '';
  const upperText = text.toUpperCase();
  const suffixIndex = Math.max(
    upperText.lastIndexOf('A'),
    upperText.lastIndexOf('B'),
  );
  if (suffixIndex > 0) {
    return text.slice(Math.max(0, suffixIndex - 3), suffixIndex);
  }
  return text.slice(-3);
}

function toInnerPackageDisplayRow(
  row: ShippingNoticeItem,
): InnerPackageDisplayRow {
  return {
    ...row,
    innerDisplayRowKey: `item-${row.id || row.rowNo || row.actualSliceBatchNo || row.packageSliceNo}`,
  };
}

function getInnerPackageGroupLabel(mode: InnerPackageGroupMode) {
  if (mode === 'batch') return '产品批号';
  if (mode === 'packageSlice') return '包装片号';
  return '不分组';
}

function getInnerPackageGroupKey(
  row: Partial<ShippingNoticeItem>,
  mode: InnerPackageGroupMode,
) {
  if (mode === 'batch') {
    return firstText(
      row.customerProductBatchNo,
      row.customerSliceBatchNo,
      row.batchNo,
      '未维护产品批号',
    );
  }
  if (mode === 'packageSlice') {
    return firstText(row.packageSliceNo, '未维护包装片号');
  }
  return '不分组';
}

function buildInnerPackageGroupRow(
  mode: InnerPackageGroupMode,
  groupKey: string,
  children: InnerPackageDisplayRow[],
  index: number,
): InnerPackageDisplayRow {
  const selectableChildren = getInnerGroupSelectableChildren({
    children,
  } as InnerPackageDisplayRow);
  const selectedCount = selectableChildren.filter((item) =>
    selectedInnerKeys.value.includes(item.id as number),
  ).length;
  return {
    __group: true,
    actualSliceBatchNo: fullJoined(
      children.map((item) => sliceNoAFront3(item.actualSliceBatchNo)),
    ),
    children,
    customerProductBatchNo:
      mode === 'batch'
        ? groupKey
        : compactJoined(
            children.map(
              (item) =>
                item.customerProductBatchNo || item.customerSliceBatchNo,
            ),
            2,
          ),
    groupCount: children.length,
    groupKey,
    groupLabel: getInnerPackageGroupLabel(mode),
    groupMode: mode,
    groupSelectableCount: selectableChildren.length,
    groupSelectedCount: selectedCount,
    innerDisplayRowKey: `group-${mode}-${index}-${groupKey}`,
    packageSliceNo:
      mode === 'packageSlice'
        ? groupKey
        : compactJoined(
            children.map((item) => item.packageSliceNo),
            2,
          ),
    rowNo: index + 1,
  };
}

function isInnerGroupRow(row?: InnerPackageDisplayRow) {
  return !!row?.__group;
}

function innerPackageRowClassName({ row }: { row: InnerPackageDisplayRow }) {
  return isInnerGroupRow(row) ? 'inner-package-group-row' : '';
}

function getInnerGroupSelectableChildren(row: InnerPackageDisplayRow) {
  return (row.children || []).filter(
    (item) => !!item.actualSliceBatchNo && !!item.id,
  );
}

function isInnerGroupSelected(row: InnerPackageDisplayRow) {
  const children = getInnerGroupSelectableChildren(row);
  return (
    !!children.length &&
    children.every((item) =>
      selectedInnerKeys.value.includes(item.id as number),
    )
  );
}

function isInnerGroupIndeterminate(row: InnerPackageDisplayRow) {
  const children = getInnerGroupSelectableChildren(row);
  const selectedCount = children.filter((item) =>
    selectedInnerKeys.value.includes(item.id as number),
  ).length;
  return selectedCount > 0 && selectedCount < children.length;
}

function toggleInnerGroupSelected(row: InnerPackageDisplayRow) {
  const children = getInnerGroupSelectableChildren(row);
  const ids = children.map((item) => item.id as number);
  if (!ids.length) return;
  const selectedSet = new Set(selectedInnerKeys.value);
  const allSelected = ids.every((id) => selectedSet.has(id));
  if (allSelected) {
    selectedInnerKeys.value = selectedInnerKeys.value.filter(
      (id) => !ids.includes(id),
    );
    return;
  }
  for (const id of ids) {
    selectedSet.add(id);
  }
  selectedInnerKeys.value = Array.from(selectedSet);
}

function isInnerSelected(row: ShippingNoticeItem) {
  return selectedInnerKeys.value.includes(row.id);
}

function toggleInnerSelected(row: ShippingNoticeItem) {
  if (!row.actualSliceBatchNo) return;
  if (isInnerSelected(row)) {
    selectedInnerKeys.value = selectedInnerKeys.value.filter(
      (id) => id !== row.id,
    );
    return;
  }
  selectedInnerKeys.value = [...selectedInnerKeys.value, row.id];
}

function loadVisualSavedInputs(): Record<string, Record<string, string>> {
  if (typeof window === 'undefined') return {};
  try {
    const raw = window.localStorage.getItem(VISUAL_PRINT_INPUT_STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === 'object'
      ? (parsed as Record<string, Record<string, string>>)
      : {};
  } catch {
    return {};
  }
}

function persistVisualSavedInputs() {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(
    VISUAL_PRINT_INPUT_STORAGE_KEY,
    JSON.stringify(visualSavedInputs),
  );
}

function buildVisualPrintAction(
  kind: VisualLabelKind,
  row?: ShippingNoticeItem,
  packageGroup?: ShippingPackageGroup,
): VisualPrintAction | undefined {
  const meta = VISUAL_LABEL_KIND_META[kind];
  if (kind === 'padBack' || kind === 'cleanBag') {
    if (!row?.id) {
      message.warning('请选择要打印的内包装明细');
      return undefined;
    }
    if (!row.actualSliceBatchNo) {
      message.warning('请选择已回填实际片号的内包装明细');
      return undefined;
    }
    return {
      kind,
      row,
      scope: 'DETAIL',
      title: meta.title,
    } as VisualPrintAction;
  }
  if (!packageGroup?.packageKey) {
    message.warning('请选择要打印的外包装');
    return undefined;
  }
  const rows = (packageGroup.items || []).filter(isShippingPackaged);
  if (!rows.length) {
    message.warning('当前外包装没有可打印的已打包明细');
    return undefined;
  }
  if (rows.some((item) => !item.shippingPackageTime)) {
    message.warning('请先点击打包，完成包装方式确认后再打印外包装标签');
    return undefined;
  }
  return {
    kind,
    packageGroup,
    scope: 'GROUP',
    title: meta.title,
  } as VisualPrintAction;
}

async function openVisualPrint(
  kind: VisualLabelKind,
  row?: ShippingNoticeItem,
  packageGroup?: ShippingPackageGroup,
) {
  if (!currentNotice.value?.id) {
    message.warning('请先选择发货需求单');
    return;
  }
  if (!currentNotice.value.customerName) {
    message.warning('当前发货包装单没有客户名称，无法匹配可视化打印模板');
    return;
  }
  const action = buildVisualPrintAction(kind, row, packageGroup);
  if (!action) return;
  visualPendingAction.value = action;
  visualPrintLoading.value = true;
  try {
    const customerName = currentNotice.value.customerName;
    const candidates = await resolveVisualPrintCandidates(kind, customerName);
    if (!candidates.length) {
      const fallbackCandidates = await resolveAllVisualPrintCandidates();
      openVisualTemplatePicker(
        fallbackCandidates,
        fallbackCandidates.length
          ? `未找到客户「${customerName}」的${VISUAL_LABEL_KIND_META[kind].shortTitle}可视化打印模板，已加载全部可视化模板，请搜索后手动选择。`
          : `未找到客户「${customerName}」的${VISUAL_LABEL_KIND_META[kind].shortTitle}可视化打印模板，且当前没有可用的可视化模板。`,
      );
      return;
    }
    if (candidates.length === 1) {
      await prepareVisualPrintCandidate(action, candidates[0]!);
      return;
    }
    openVisualTemplatePicker(
      candidates,
      `匹配到 ${candidates.length} 条客户模板，请选择本次打印使用的模板。`,
    );
  } catch (error: any) {
    Modal.warning({
      content: `可视化打印模板解析失败：${getRequestErrorMessage(error)}`,
      title: `${action.title}失败`,
    });
  } finally {
    visualPrintLoading.value = false;
  }
}

async function resolveVisualPrintCandidates(
  kind: VisualLabelKind,
  customerName?: string,
): Promise<VisualPrintCandidate[]> {
  const params: Record<string, any> = {
    labelKind: kind,
    pageNo: 1,
    pageSize: 200,
  };
  if (customerName) {
    params.customer = customerName;
  }
  const rows = await getVisualPrintCustomerInfoList(params);
  const candidates: VisualPrintCandidate[] = [];
  const existed = new Set<string>();
  for (const row of rows || []) {
    if (!row.id) continue;
    const [boundDesigns, sharedDesigns] = await Promise.all([
      row.productItemId
        ? getVisualPrintDesigns(row.id, row.productItemId)
        : Promise.resolve([]),
      getVisualPrintDesigns(row.id),
    ]);
    const matchedDesigns = [...(boundDesigns || []), ...(sharedDesigns || [])]
      .filter((design) => design?.id && design.labelKind === kind)
      .filter(
        (design) =>
          design.productItemId === row.productItemId ||
          isVisualDesignImageMatched(row, kind, design),
      );
    for (const design of matchedDesigns) {
      const key = `${row.id}-${row.productItemId || 'master'}-${design.id}`;
      if (existed.has(key)) continue;
      existed.add(key);
      candidates.push({
        design,
        key,
        row,
      });
    }
    if (!matchedDesigns.length && hasVisualLabelSource(row, kind)) {
      candidates.push(buildReferenceVisualPrintCandidate(row, kind));
    }
  }
  return candidates;
}

function isVisualDesignImageMatched(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
  design: VisualPrintDesign,
) {
  const targetImageId = normalizeVisualPrintValue(
    getVisualLabelImageId(row, kind),
  );
  const targetImageFile = normalizeVisualPrintValue(
    getVisualLabelImageFile(row, kind),
  );
  const designImageId = normalizeVisualPrintValue(design.imageId);
  const designImageFile = normalizeVisualPrintValue(design.imageFile);
  if (targetImageId || targetImageFile) {
    return (
      (!!targetImageId && targetImageId === designImageId) ||
      (!!targetImageFile && targetImageFile === designImageFile)
    );
  }
  return !designImageId && !designImageFile;
}

async function resolveAllVisualPrintCandidates(): Promise<
  VisualPrintCandidate[]
> {
  const rows = await getVisualPrintCustomerInfoList({
    pageNo: 1,
    pageSize: 200,
  });
  const candidates: VisualPrintCandidate[] = [];
  const existed = new Set<string>();
  for (const row of rows || []) {
    if (!row.id) continue;
    const designs = await getVisualPrintDesigns(row.id, row.productItemId);
    const designedKinds = new Set<string>();
    for (const design of designs || []) {
      if (!design?.id) continue;
      if (design.labelKind) {
        designedKinds.add(String(design.labelKind));
      }
      const key = `${row.id}-${row.productItemId || 'master'}-${design.id}`;
      if (existed.has(key)) continue;
      existed.add(key);
      candidates.push({ design, key, row });
    }
    for (const kind of VISUAL_LABEL_KINDS) {
      if (designedKinds.has(kind) || !hasVisualLabelSource(row, kind)) continue;
      const candidate = buildReferenceVisualPrintCandidate(row, kind);
      if (existed.has(candidate.key)) continue;
      existed.add(candidate.key);
      candidates.push(candidate);
    }
  }
  return candidates;
}

function openVisualTemplatePicker(
  candidates: VisualPrintCandidate[],
  hint = '',
) {
  visualTemplateCandidates.value = candidates;
  visualTemplateKeyword.value = '';
  visualSelectedTemplateKey.value = candidates[0]?.key || '';
  visualTemplatePickerHint.value = hint;
  visualTemplatePickerVisible.value = true;
}

function buildVisualTemplateOptionLabel(candidate: VisualPrintCandidate) {
  const row = candidate.row;
  const design = candidate.design;
  const labelKind = getVisualCandidateLabelKind(candidate);
  return [
    VISUAL_LABEL_KIND_META[labelKind]?.shortTitle || labelKind,
    design.labelName ||
      VISUAL_LABEL_KIND_META[labelKind]?.title ||
      '未命名模板',
    row.customer,
    row.productType,
    row.sizeMm ? `${row.sizeMm}mm` : '',
    design.id ? `#${design.id}` : '参考图',
  ]
    .filter(Boolean)
    .join(' / ');
}

function selectVisualTemplateCandidate(candidate?: VisualPrintCandidate) {
  visualSelectedTemplateKey.value = candidate?.key || '';
}

function handleVisualTemplateCellClick({ row }: { row: VisualPrintCandidate }) {
  selectVisualTemplateCandidate(row);
}

function getVisualCandidateLabelKind(candidate: VisualPrintCandidate) {
  return (candidate.design.labelKind ||
    visualPendingAction.value?.kind ||
    'padBack') as VisualLabelKind;
}

function getVisualCandidateSourceText(candidate: VisualPrintCandidate) {
  return candidate.design.id ? '已设计' : '参考图';
}

function getVisualCandidateImageUrl(candidate: VisualPrintCandidate) {
  const labelKind = getVisualCandidateLabelKind(candidate);
  return resolveVisualPrintImageUrl(
    candidate.design.imageFile ||
      getVisualLabelImageFile(candidate.row, labelKind),
  );
}

function getVisualLabelImageId(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
) {
  if (kind === 'padBack') return row.padBackLabelImageId;
  if (kind === 'cleanBag') return row.cleanBagLabelImageId;
  if (kind === 'boxFront') return row.boxFrontLabelImageId;
  return row.customerSideLabelImageId;
}

function getVisualLabelImageFile(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
) {
  if (kind === 'padBack') return row.padBackLabelImageFile;
  if (kind === 'cleanBag') return row.cleanBagLabelImageFile;
  if (kind === 'boxFront') return row.boxFrontLabelImageFile;
  return row.customerSideLabelImageFile;
}

function getVisualLabelDesignId(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
) {
  if (kind === 'padBack') return row.padBackDesignId;
  if (kind === 'cleanBag') return row.cleanBagDesignId;
  if (kind === 'boxFront') return row.boxFrontDesignId;
  return row.customerSideDesignId;
}

function hasVisualLabelSource(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
) {
  return (
    !!getVisualLabelDesignId(row, kind) ||
    !!getVisualLabelImageId(row, kind) ||
    !!getVisualLabelImageFile(row, kind)
  );
}

function buildReferenceVisualPrintCandidate(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
): VisualPrintCandidate {
  const draft = buildReferenceVisualPrintDraft(row, kind);
  return {
    design: {
      dpi: draft.dpi,
      designJson: JSON.stringify(draft),
      heightMm: draft.heightMm,
      imageFile: draft.imageFile,
      imageId: draft.imageId,
      imageHeightPx: draft.imageHeightPx,
      imageWidthPx: draft.imageWidthPx,
      labelKind: kind,
      labelName: draft.labelName,
      widthMm: draft.widthMm,
    },
    key: `${row.id}-${row.productItemId || 'master'}-${kind}-reference`,
    row,
  };
}

function buildReferenceVisualPrintDraft(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
): VisualPrintDesignerDraft {
  const size = inferReferenceVisualCanvasSize(row, kind);
  return {
    customer: row.customer,
    dpi: 300,
    elements: buildReferenceVisualElements(kind, size),
    heightMm: size.heightMm,
    imageFile: getVisualLabelImageFile(row, kind),
    imageId: getVisualLabelImageId(row, kind),
    labelKind: kind,
    labelName: `${VISUAL_LABEL_KIND_META[kind].shortTitle}参考模板`,
    printRotation: 0,
    widthMm: size.widthMm,
  };
}

function inferReferenceVisualCanvasSize(
  row: VisualPrintCustomerInfo,
  kind: VisualLabelKind,
) {
  const sideSize = String(row.customerSideSize || '').trim();
  const matched = sideSize.match(/(\d+(?:\.\d+)?)\s*\*\s*(\d+(?:\.\d+)?)/);
  if (kind === 'customerSide' && matched) {
    return { heightMm: Number(matched[2]), widthMm: Number(matched[1]) };
  }
  if (kind === 'boxFront') return { heightMm: 80, widthMm: 120 };
  if (kind === 'cleanBag') return { heightMm: 70, widthMm: 105 };
  return { heightMm: 60, widthMm: 100 };
}

function makeReferenceVisualElement(
  type: string,
  name: string,
  x: number,
  y: number,
  width: number,
  height: number,
  extra: Record<string, any> = {},
): VisualPrintDesignerElement {
  return {
    height,
    id: `${type}-${name}-${x}-${y}`,
    name,
    type,
    width,
    x,
    y,
    ...extra,
  };
}

function buildReferenceVisualElements(
  kind: VisualLabelKind,
  size: { heightMm: number; widthMm: number },
) {
  const w = size.widthMm;
  const h = size.heightMm;
  const px = (value: number) => (w * value) / 100;
  const py = (value: number) => (h * value) / 100;
  if (kind === 'customerSide') {
    return [
      makeReferenceVisualElement(
        'rect',
        '外框',
        px(0),
        py(0),
        px(100),
        py(100),
        { strokeWidth: 0.4 },
      ),
      makeReferenceVisualElement(
        'text',
        'PN标题',
        px(2),
        py(6),
        px(14),
        py(6),
        { bold: true, fontSize: 5, text: 'PN:' },
      ),
      makeReferenceVisualElement(
        'barcode',
        'PN条码',
        px(18),
        py(4),
        px(58),
        py(14),
        { fieldKey: 'pn' },
      ),
      makeReferenceVisualElement(
        'qrcode',
        '二维码',
        px(80),
        py(3),
        px(17),
        py(25),
        { fieldKey: 'batchNo' },
      ),
      makeReferenceVisualElement(
        'field',
        '物料描述',
        px(20),
        py(30),
        px(58),
        py(7),
        { fieldKey: 'materialDescription', fontSize: 5 },
      ),
      makeReferenceVisualElement(
        'field',
        'INFO',
        px(2),
        py(48),
        px(60),
        py(6),
        { fieldKey: 'info', fontSize: 4.2 },
      ),
      makeReferenceVisualElement(
        'field',
        '批号',
        px(64),
        py(48),
        px(32),
        py(6),
        { fieldKey: 'batchNo', fontSize: 5 },
      ),
      makeReferenceVisualElement(
        'field',
        'EXP',
        px(22),
        py(68),
        px(26),
        py(6),
        { fieldKey: 'expDate', fontSize: 5 },
      ),
      makeReferenceVisualElement(
        'field',
        'Shipping',
        px(72),
        py(68),
        px(24),
        py(6),
        { fieldKey: 'shippingDate', fontSize: 4.8 },
      ),
      makeReferenceVisualElement(
        'field',
        'QTY',
        px(32),
        py(86),
        px(14),
        py(6),
        { fieldKey: 'quantity', fontSize: 4.8 },
      ),
      makeReferenceVisualElement('field', 'PO', px(76), py(86), px(22), py(6), {
        fieldKey: 'po',
        fontSize: 4.8,
      }),
    ];
  }
  return [
    makeReferenceVisualElement(
      'text',
      '产品型号',
      px(10),
      py(16),
      px(32),
      py(8),
      { bold: true, fontSize: 7.2, text: 'DTP' },
    ),
    makeReferenceVisualElement(
      'text',
      '标签标题',
      px(50),
      py(16),
      px(42),
      py(8),
      {
        align: 'center',
        bold: true,
        fontSize: 6.8,
        text: VISUAL_LABEL_KIND_META[kind].shortTitle,
      },
    ),
    makeReferenceVisualElement(
      'field',
      '产品编码',
      px(18),
      py(34),
      px(24),
      py(6),
      { fieldKey: 'productNo', fontSize: 3.8 },
    ),
    makeReferenceVisualElement(
      'barcode',
      '产品条码',
      px(52),
      py(30),
      px(24),
      py(8),
      { fieldKey: 'productNo' },
    ),
    makeReferenceVisualElement(
      'field',
      '产品信息',
      px(18),
      py(46),
      px(54),
      py(6),
      { fieldKey: 'productInfo', fontSize: 3.8 },
    ),
    makeReferenceVisualElement('field', '数量', px(18), py(58), px(14), py(6), {
      fieldKey: 'quantity',
      fontSize: 4.1,
    }),
    makeReferenceVisualElement('field', '片号', px(58), py(58), px(22), py(6), {
      fieldKey: 'padNo',
      fontSize: 4.1,
    }),
    makeReferenceVisualElement('field', '批号', px(18), py(70), px(26), py(6), {
      fieldKey: 'batchNo',
      fontSize: 3.8,
    }),
    makeReferenceVisualElement(
      'barcode',
      '批号条码',
      px(52),
      py(68),
      px(28),
      py(8),
      { fieldKey: 'batchNo' },
    ),
    makeReferenceVisualElement(
      'field',
      '生产日期',
      px(18),
      py(84),
      px(24),
      py(6),
      { fieldKey: 'productionDate', fontSize: 3.8 },
    ),
    makeReferenceVisualElement(
      'field',
      '有效日期',
      px(58),
      py(84),
      px(24),
      py(6),
      { fieldKey: 'expirationDate', fontSize: 3.8 },
    ),
  ];
}

async function submitVisualTemplateSelection() {
  const action = visualPendingAction.value;
  const candidate = filteredVisualTemplateCandidates.value.find(
    (item) => item.key === visualSelectedTemplateKey.value,
  );
  if (!action || !candidate) {
    message.warning('请选择要打印的模板');
    return;
  }
  visualTemplatePickerVisible.value = false;
  visualPrintLoading.value = true;
  try {
    await prepareVisualPrintCandidate(action, candidate);
  } catch (error: any) {
    Modal.warning({
      content: `可视化打印模板解析失败：${getRequestErrorMessage(error)}`,
      title: `${action.title}失败`,
    });
  } finally {
    visualPrintLoading.value = false;
  }
}

async function prepareVisualPrintCandidate(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
) {
  const draft = parseVisualPrintDraft(candidate.design);
  const requiredFields = getVisualDesignRequiredFields(draft);
  const data = {
    ...buildVisualPrintData(action, candidate),
    ...getSavedVisualInputs(action, candidate),
  };
  const missingFields = requiredFields
    .filter((fieldKey) => !hasVisualValue(data[fieldKey]))
    .map((fieldKey) => ({ fieldKey, label: getVisualFieldLabel(fieldKey) }));
  if (missingFields.length) {
    openVisualMissingInput(action, candidate, data, missingFields);
    return;
  }
  await executeVisualPrint(action, candidate, data);
}

function openVisualMissingInput(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
  data: Record<string, string>,
  missingFields: VisualPrintMissingField[],
) {
  visualPendingAction.value = action;
  visualPendingCandidate.value = candidate;
  visualPendingData.value = { ...data };
  visualMissingFields.value = missingFields;
  for (const key of Object.keys(visualMissingInputs)) {
    delete visualMissingInputs[key];
  }
  for (const field of missingFields) {
    visualMissingInputs[field.fieldKey] = data[field.fieldKey] || '';
  }
  visualMissingVisible.value = true;
}

async function submitVisualMissingInputs() {
  const action = visualPendingAction.value;
  const candidate = visualPendingCandidate.value;
  if (!action || !candidate) {
    visualMissingVisible.value = false;
    return;
  }
  const emptyField = visualMissingFields.value.find(
    (field) => !hasVisualValue(visualMissingInputs[field.fieldKey]),
  );
  if (emptyField) {
    message.warning(`请填写 ${emptyField.label}`);
    return;
  }
  const data = { ...visualPendingData.value };
  const saved: Record<string, string> = {};
  for (const field of visualMissingFields.value) {
    const value = normalizeVisualPrintValue(
      visualMissingInputs[field.fieldKey],
    );
    data[field.fieldKey] = value;
    saved[field.fieldKey] = value;
  }
  visualSavedInputs[buildVisualInputStorageKey(action, candidate)] = saved;
  persistVisualSavedInputs();
  visualMissingVisible.value = false;
  visualPrintLoading.value = true;
  try {
    await executeVisualPrint(action, candidate, data);
  } finally {
    visualPrintLoading.value = false;
  }
}

function parseVisualPrintDraft(design: VisualPrintDesign) {
  let parsed: VisualPrintDesignerDraft = {};
  if (design.designJson) {
    parsed = JSON.parse(design.designJson) as VisualPrintDesignerDraft;
  }
  return {
    ...parsed,
    dpi: Number(parsed.dpi || design.dpi || 300),
    elements: Array.isArray(parsed.elements) ? parsed.elements.filter((element) => !isHiddenSamplePrintField(element.fieldKey)) : [],
    heightMm: Number(parsed.heightMm || design.heightMm || 50),
    labelKind: parsed.labelKind || design.labelKind,
    labelName:
      parsed.labelName ||
      design.labelName ||
      VISUAL_LABEL_KIND_META[(design.labelKind || 'padBack') as VisualLabelKind]
        ?.title,
    printRotation: normalizeVisualPrintRotation(parsed.printRotation),
    widthMm: Number(parsed.widthMm || design.widthMm || 80),
  } as VisualPrintDesignerDraft;
}

function isHiddenSamplePrintField(fieldKey?: string) {
  return currentNotice.value?.productType === 'SAMPLE'
    && ['customer', 'customerName', 'batchNo', 'customerProductBatchNo', 'packageSliceNo',
      'externalProductModel', 'requiredBatchNo'].includes(fieldKey || '');
}

function getVisualDesignRequiredFields(draft: VisualPrintDesignerDraft) {
  const fields = new Set<string>();
  for (const element of draft.elements || []) {
    const fieldKey = String(element.fieldKey || '').trim();
    if (!fieldKey || isHiddenSamplePrintField(fieldKey)) continue;
    fields.add(fieldKey);
  }
  return Array.from(fields);
}

function buildVisualPrintData(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
) {
  const notice = currentNotice.value;
  const config = candidate.row;
  const rows =
    action.scope === 'GROUP'
      ? action.packageGroup?.items || []
      : action.row
        ? [action.row]
        : [];
  const row = action.row || rows[0];
  const group = action.packageGroup;
  const packageTotal = packageGroups.value.length || 1;
  const actualSliceText =
    action.scope === 'GROUP'
      ? group?.actualSliceText ||
        fullJoined(rows.map((item) => sliceNoAFront3(item.actualSliceBatchNo)))
      : firstText(row?.actualSliceBatchNo, row?.sliceBatchNo);
  const fullSliceText =
    action.scope === 'GROUP'
      ? fullJoined(rows.map((item) => item.actualSliceBatchNo))
      : firstText(row?.actualSliceBatchNo, row?.sliceBatchNo);
  const batchText = notice?.productType === 'SAMPLE' ? '' :
    action.scope === 'GROUP'
      ? group?.customerBatchText ||
        fullJoined(
          rows.map(
            (item) => item.customerProductBatchNo || item.customerSliceBatchNo,
          ),
        )
      : firstText(
          row?.customerProductBatchNo,
          row?.customerSliceBatchNo,
          notice?.requiredBatchNo,
          row?.batchNo,
        );
  const productType = firstText(
    config.productType,
    row?.internalModelCode,
    row?.modelCode,
    notice?.externalProductModel,
    notice?.modelCode,
    notice?.productType,
  );
  const sizeMm = firstText(
    config.sizeMm,
    row?.productSize,
    notice?.productSize,
  );
  const productNo = firstText(
    notice?.externalProductCode,
    row?.customerModelCode,
    row?.materialCode,
    notice?.materialCode,
  );
  const productInfo = firstText(
    notice?.externalProductInfo,
    notice?.externalProductModel,
    row?.internalModelCode,
    notice?.materialName,
    row?.materialName,
    notice?.modelCode,
  );
  return normalizeVisualPrintData({
    actualSliceBatchNo: fullSliceText,
    batchNo: batchText,
    customer: firstText(notice?.customerName, config.customer),
    customerSideMethod: config.customerSideMethod,
    customerSideSize: config.customerSideSize,
    customerSideTemplate: config.customerSideTemplate,
    deliveryNote: config.deliveryNote,
    expDate: formatVisualPrintDate(notice?.requiredExpiryDate),
    expirationDate: formatVisualPrintDate(notice?.requiredExpiryDate),
    hasMark: config.hasMark,
    info: firstText(
      config.specialRemark,
      notice?.packingRequirement,
      group?.packageMethod,
    ),
    materialDescription: firstText(
      notice?.externalProductInfo,
      notice?.materialName,
      row?.materialName,
      productInfo,
    ),
    needEcoa: config.needEcoa,
    needPaperCoa: config.needPaperCoa,
    noticeNo: notice?.noticeNo,
    operatorName: currentUserName.value,
    packageIndex:
      action.scope === 'GROUP' && group
        ? `${group.rowNo}/${packageTotal}`
        : String(row?.rowNo || ''),
    packageMethod: group?.packageMethod || extractPackageMethod(rows, ''),
    packageNo: group?.packageNo || row?.packageNo || row?.outerBoxNo,
    padNo: actualSliceText,
    plant: '',
    pn: firstText(productNo, notice?.materialCode),
    po: firstText(notice?.erpOrderNo, notice?.orderNo),
    productInfo,
    productModel: productType,
    productNo,
    productType,
    productTypeName: '化学机械研磨垫',
    productionDate: formatVisualPrintDate(
      firstText(notice?.requiredProductionDate, row?.inboundTime),
    ),
    purUom: 'PC',
    quantity:
      action.scope === 'GROUP'
        ? String(group?.itemCount || rows.length || '')
        : String(row?.actualShipQty || 1),
    serialNo: config.serialNo,
    shipmentFilePackageMethod: config.shipmentFilePackageMethod,
    shippingDate: formatVisualPrintDate(notice?.shippingTime),
    shippingMethod: firstText(config.shippingMethod, notice?.productType),
    sizeMm,
    specialRemark: firstText(config.specialRemark, notice?.remark),
    vendorPn: firstText(row?.customerModelCode, notice?.externalProductCode),
  });
}

function normalizeVisualPrintData(
  data: Record<string, any>,
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(data).map(([key, value]) => [
      key,
      normalizeVisualPrintValue(value),
    ]),
  );
}

function normalizeVisualPrintValue(value: any) {
  if (value === undefined || value === null) return '';
  return String(value).trim();
}

function normalizeVisualPrintRotation(value: unknown) {
  const rotation = Number(value);
  return rotation === 90 || rotation === 180 || rotation === 270 ? rotation : 0;
}

function getRequestErrorMessage(error: any) {
  if (!error) return '未知错误';
  if (typeof error === 'string') return error;
  const messageText =
    error.message ||
    error.msg ||
    error.data?.message ||
    error.data?.msg ||
    error.response?.data?.message ||
    error.response?.data?.msg;
  if (messageText) return String(messageText);
  try {
    return JSON.stringify(error);
  } catch {
    return String(error);
  }
}

function hasVisualValue(value: any) {
  const text = normalizeVisualPrintValue(value);
  return !!text && text !== '-';
}

function formatVisualPrintDate(value?: string) {
  const text = normalizeVisualPrintValue(value);
  if (!text) return '';
  const date = dayjs(text.replace('T', ' '));
  return date.isValid() ? date.format('YYYY-MM-DD') : text.slice(0, 10);
}

function getVisualFieldLabel(fieldKey: string) {
  return VISUAL_PRINT_FIELD_LABELS[fieldKey] || fieldKey;
}

function buildVisualInputStorageKey(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
) {
  return `${visualPrintStateKey(action)}:${candidate.design.id || 'design'}`;
}

function getSavedVisualInputs(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
) {
  return visualSavedInputs[buildVisualInputStorageKey(action, candidate)] || {};
}

async function executeVisualPrint(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
  data: Record<string, string>,
) {
  let draft: VisualPrintDesignerDraft | undefined;
  try {
    draft = parseVisualPrintDraft(candidate.design);
    data = Object.fromEntries(Object.entries(data).map(([key, value]) =>
      [key, isHiddenSamplePrintField(key) ? '' : value]));
    const selectedLabelKind = resolveVisualCandidateLabelKind(
      action,
      candidate,
    );
    const requestId = `${currentNotice.value?.noticeNo || 'NOTICE'}-${action.kind}-${Date.now()}`;
    updateVisualPrintState(action, 'PRINTING', {
      message: '已调用打印服务，等待返回',
      templateName: draft.labelName,
      time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    });
    message.info(`${action.title}已调用打印服务，等待服务返回...`);
    const response = await fetch(`${VISUAL_PRINT_AGENT_URL}/print`, {
      body: JSON.stringify({
        data,
        design: draft.rendererTemplate
          ? undefined
          : {
              background: draft.background,
              contentOffsetXmm: draft.contentOffsetXmm,
              contentOffsetYmm: draft.contentOffsetYmm,
              designHeightMm: draft.designHeightMm,
              designWidthMm: draft.designWidthMm,
              dpi: draft.dpi,
              elements: draft.elements || [],
              heightMm: draft.heightMm,
              labelKind: selectedLabelKind,
              labelName: draft.labelName || action.title,
              preservePageSizeAfterRotation:
                draft.preservePageSizeAfterRotation !== false,
              printRotation: normalizeVisualPrintRotation(draft.printRotation),
              rotatedContentOriginXmm: draft.rotatedContentOriginXmm,
              rotatedContentOriginYmm: draft.rotatedContentOriginYmm,
              rotationDirection: draft.rotationDirection,
              schemaVersion: draft.schemaVersion,
              threshold: draft.threshold,
              widthMm: draft.widthMm,
            },
        meta: {
          customer: currentNotice.value?.customerName,
          labelKind: selectedLabelKind,
          requestedLabelKind: action.kind,
          noticeId: currentNotice.value?.id,
          noticeNo: currentNotice.value?.noticeNo,
          operatorName: currentUserName.value,
          packageNo: action.packageGroup?.packageNo,
          productType: candidate.row.productType,
          rowId: action.row?.id,
          sizeMm: candidate.row.sizeMm,
          sourcePage: 'mes/execution/fg-shipping-package',
          printScope: action.scope,
        },
        outputMode: VISUAL_PRINT_OUTPUT_MODE,
        rendererTemplate: draft.rendererTemplate,
        requestId,
      }),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
    const result = await response.json().catch(() => ({}));
    if (!response.ok || result?.success === false) {
      throw new Error(
        result?.message || `本机打印服务返回异常：${response.status}`,
      );
    }
    const target = result?.target || result?.outputFile || '默认打印机';
    updateVisualPrintState(
      action,
      'SUCCESS',
      {
        message: `已发送到 ${target}`,
        templateName: draft.labelName,
        time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      },
      true,
    );
    message.success(`${action.title}已发送到 ${target}`);
  } catch (error: any) {
    updateVisualPrintState(action, 'FAILED', {
      message: getRequestErrorMessage(error),
      templateName: draft?.labelName || candidate.design.labelName,
      time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    });
    Modal.warning({
      content: `未能连接或确认 MVP 位图打印服务：${getRequestErrorMessage(error)}。请先启动 ${VISUAL_PRINT_AGENT_URL} 的本地打印服务。`,
      title: `${action.title}失败`,
    });
  }
}

function resolveVisualCandidateLabelKind(
  action: VisualPrintAction,
  candidate: VisualPrintCandidate,
): VisualLabelKind {
  return (candidate.design.labelKind || action.kind) as VisualLabelKind;
}

function visualPrintStateKey(action: VisualPrintAction) {
  const noticeKey =
    currentNotice.value?.id || currentNotice.value?.noticeNo || 'NOTICE';
  const targetKey =
    action.scope === 'GROUP'
      ? action.packageGroup?.packageKey || 'GROUP'
      : action.row?.id || action.row?.actualSliceBatchNo || 'ITEM';
  return `${noticeKey}:${action.scope}:${targetKey}:${action.kind}`;
}

function updateVisualPrintState(
  action: VisualPrintAction,
  status: VisualPrintStatusValue,
  patch: Partial<VisualPrintState> = {},
  increaseCount = false,
) {
  const key = visualPrintStateKey(action);
  const previous = visualPrintStates[key] || {
    count: 0,
    status: 'IDLE' as VisualPrintStatusValue,
  };
  visualPrintStates[key] = {
    ...previous,
    ...patch,
    count: increaseCount ? previous.count + 1 : previous.count,
    status,
  };
}

function getItemVisualPrintState(
  kind: VisualLabelKind,
  row: ShippingNoticeItem,
): VisualPrintState {
  return (
    visualPrintStates[
      visualPrintStateKey({
        kind,
        row,
        scope: 'DETAIL',
        title: VISUAL_LABEL_KIND_META[kind].title,
      })
    ] || { count: 0, status: 'IDLE' }
  );
}

function getPackageVisualPrintState(
  kind: VisualLabelKind,
  packageGroup: ShippingPackageGroup,
): VisualPrintState {
  return (
    visualPrintStates[
      visualPrintStateKey({
        kind,
        packageGroup,
        scope: 'GROUP',
        title: VISUAL_LABEL_KIND_META[kind].title,
      })
    ] || { count: 0, status: 'IDLE' }
  );
}

function visualPrintStatusColor(status?: VisualPrintStatusValue) {
  const map: Record<VisualPrintStatusValue, string> = {
    FAILED: 'red',
    IDLE: 'default',
    PRINTING: 'processing',
    SUCCESS: 'green',
  };
  return map[status || 'IDLE'] || 'default';
}

function visualPrintTagText(kind: VisualLabelKind, state: VisualPrintState) {
  const suffixMap: Record<VisualPrintStatusValue, string> = {
    FAILED: '失败',
    IDLE: '',
    PRINTING: '调用中',
    SUCCESS: '已打印',
  };
  const suffix = suffixMap[state.status || 'IDLE'];
  return `${VISUAL_LABEL_KIND_META[kind].shortTitle}${state.count ? ` ${state.count}` : ''}${suffix ? ` ${suffix}` : ''}`;
}

async function queryNoticePage(page?: {
  currentPage?: number;
  pageSize?: number;
}) {
  const result = await getShippingNoticePage({
    keyword: query.keyword.trim() || undefined,
    noticeStatus: currentMainTab.value.noticeStatus,
    pageNo: page?.currentPage || 1,
    pageSize: page?.pageSize || 20,
  });
  noticeTotal.value = Number(result.total || 0);
  return result;
}

async function searchNotices(reload?: boolean | Event) {
  await (reload === true ? gridApi.reload() : gridApi.query());
}

function toMainTabKey(key: number | string): MainTabKey {
  const tabKey = String(key);
  return mainTabs.some((tab) => tab.key === tabKey) ||
    SHIPPING_CHECK_TABS.includes(tabKey)
    ? (tabKey as MainTabKey)
    : 'wait';
}

async function switchMainTab(key: number | string) {
  activeMainTab.value = toMainTabKey(key);
  if (isShippingCheckTab.value) {
    await nextTick();
    await refreshShippingDailyCheckGrid();
    return;
  }
  await searchNotices(true);
}

async function refreshDetail(silent = false) {
  if (!currentNotice.value?.id) return;
  if (!silent) detailLoading.value = true;
  try {
    const previousNotice = currentNotice.value;
    const latestNotice = await getShippingNotice(previousNotice.id);
    const mergedNotice = mergeSilentRefreshNotice(
      previousNotice,
      latestNotice,
      silent,
    );
    currentNotice.value = mergedNotice;
    if (
      activeMainTab.value === 'pendingInspection' &&
      isCompletedStatus(mergedNotice.noticeStatus)
    ) {
      activeMainTab.value = 'completed';
      await searchNotices(true);
    }
  } finally {
    if (!silent) detailLoading.value = false;
  }
}

function mergeSilentRefreshNotice(
  previousNotice: ShippingNotice,
  latestNotice: ShippingNotice | undefined,
  silent: boolean,
) {
  if (!silent) {
    return latestNotice || previousNotice;
  }
  if (!latestNotice?.id || latestNotice.id !== previousNotice.id) {
    return previousNotice;
  }
  const mergedNotice = { ...previousNotice, ...latestNotice };
  const keepItems =
    !!previousNotice.items?.length &&
    (!Array.isArray(latestNotice.items) || latestNotice.items.length === 0);
  const keepPickItems =
    !!previousNotice.pickItems?.length &&
    (!Array.isArray(latestNotice.pickItems) ||
      latestNotice.pickItems.length === 0);
  if (!keepItems && !keepPickItems) {
    return mergedNotice;
  }
  return {
    ...mergedNotice,
    items: keepItems ? previousNotice.items : latestNotice.items,
    pickItems: keepPickItems
      ? previousNotice.pickItems
      : latestNotice.pickItems,
  };
}

async function openDetail(row: ShippingNotice) {
  detailVisible.value = true;
  detailMaximized.value = false;
  detailLoading.value = true;
  activeDetailTab.value = 'items';
  selectedInnerKeys.value = [];
  currentNotice.value = undefined;
  try {
    currentNotice.value = await getShippingNotice(row.id);
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailVisible.value = false;
}

function openPackageCreate() {
  if (!currentNotice.value?.id) return;
  if (currentNoticeCompleted.value) {
    message.warning('已完成发货包装单仅允许查看');
    return;
  }
  if (!['OQC_PASSED', 'PACKAGED'].includes(currentNotice.value.noticeStatus || '')) {
    message.warning('请先推送 OQC 并待检验合格后再进行外包装');
    return;
  }
  if (!selectedInnerItems.value.length) {
    message.warning('请先勾选要打包的客户批号明细');
    activeDetailTab.value = 'items';
    return;
  }
  const invalidRow = selectedInnerItems.value.find(
    (row) => row.shippingInspectionResult !== 'OK',
  );
  if (invalidRow) {
    message.warning(
      `客户批号明细未发货检验合格，不能打包：${invalidRow.actualSliceBatchNo || invalidRow.internalItemCode || '-'}`,
    );
    return;
  }
  packageForm.packageMethod = packageForm.packageMethod || '纸盒';
  packageForm.auxConsumeItems = [{}];
  packageForm.remark = '';
  void loadPackageAuxStocks();
  packageCreateVisible.value = true;
}

async function submitPackageCreate() {
  if (!currentNotice.value?.id) return;
  if (!selectedInnerItems.value.length) {
    message.warning('请先勾选要打包的客户批号明细');
    return;
  }
  const auxConsumeItems = buildPackageAuxConsumeItems();
  if (!auxConsumeItems) {
    return;
  }
  packageSubmitting.value = true;
  try {
    currentNotice.value = await packShippingNotice({
      auxConsumeItems,
      noticeId: currentNotice.value.id,
      noticeItemIds: selectedInnerItems.value.map((row) => row.id),
      operatorName: currentUserName.value,
      packageMethod: packageForm.packageMethod,
      remark: packageForm.remark,
    });
    packageCreateVisible.value = false;
    message.success(
      `已完成 ${selectedInnerItems.value.length} 条客户批号明细打包`,
    );
    await searchNotices();
  } finally {
    packageSubmitting.value = false;
  }
}

async function doPushOqc() {
  if (!currentNotice.value?.id) return;
  if (currentNoticeCompleted.value) {
    message.warning('已完成发货包装单仅允许查看');
    return;
  }
  packageLoading.value = true;
  try {
    currentNotice.value = await pushShippingNoticeOqc({
      noticeId: currentNotice.value.id,
      operatorName: currentUserName.value,
    });
    activeDetailTab.value = 'oqc';
    activeMainTab.value = 'pendingInspection';
    selectedInnerKeys.value = [];
    message.success('已推送出货检验，OQC 合格后才能进行外包装');
    await searchNotices(true);
  } finally {
    packageLoading.value = false;
  }
}

function pushOqc() {
  if (!currentNotice.value?.id) return;
  if (currentNoticeCompleted.value) {
    message.warning('已完成发货包装单仅允许查看');
    return;
  }
  Modal.confirm({
    content:
      '确认后会生成 OQC 出货检验单。OQC 判定合格前，系统将禁止外包装。',
    okText: '确认发送',
    onOk: doPushOqc,
    title: '推送出货检验',
  });
}

async function doCompleteShipping() {
  if (!currentNotice.value?.id) return;
  packageLoading.value = true;
  try {
    currentNotice.value = await completeShippingNotice({
      noticeId: currentNotice.value.id,
      operatorName: currentUserName.value,
    });
    activeMainTab.value = 'completed';
    message.success('发货完成，发货需求单已关闭');
    await searchNotices(true);
  } finally {
    packageLoading.value = false;
  }
}

function completeShipping() {
  if (!currentNotice.value?.id) return;
  if (currentNoticeCompleted.value) {
    message.warning('已完成发货包装单仅允许查看');
    return;
  }
  const unpackagedRow = detailItems.value.find(
    (row) => row.actualSliceBatchNo && (!row.shippingPackageName || !row.shippingPackageTime),
  );
  if (unpackagedRow) {
    activeDetailTab.value = 'items';
    Modal.warning({
      content: `客户批号明细还未完成外包装：${unpackagedRow.actualSliceBatchNo || unpackagedRow.internalItemCode || '-'}`,
      okText: '去外包装',
      onOk: openPackageCreate,
      title: '请先完成外包装',
    });
    return;
  }
  Modal.confirm({
    content: '确认后将完成实际出库、写入库存流水并关闭整张发货需求单。',
    okText: '确认发货完成',
    onOk: doCompleteShipping,
    title: '发货完成',
  });
}

const [NoticeGrid, gridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: noticeColumns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [10, 20, 50, 100] },
    proxyConfig: { ajax: { query: async ({ page }) => queryNoticePage(page) } },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
  } as VxeTableGridOptions<ShippingNotice>,
});

function getPageNo(page?: { currentPage?: number; pageNo?: number }) {
  return Number(page?.currentPage || page?.pageNo || 1);
}

function getPageSize(page?: { pageSize?: number }) {
  return Number(page?.pageSize || 20);
}

const [ShippingDailyCheckGrid, shippingDailyCheckGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: shippingCheckColumns,
    height: 'auto',
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [10, 20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          shippingDailyCheckLoading.value = true;
          try {
            return await getProcessFormRecordPage({
              formType: activeShippingCheckForm.value.formType,
              pageNo: getPageNo(page),
              pageSize: getPageSize(page),
              processCode: PACKAGING_PROCESS_CODE,
              recordDate: shippingDailyCheckQuery.recordDate || undefined,
            });
          } finally {
            shippingDailyCheckLoading.value = false;
          }
        },
      },
    },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
  } as VxeTableGridOptions<MesHcProcessFormApi.Record>,
});

</script>

<template>
  <Page auto-content-height>
    <div
      class="fg-packaging-workbench package-fg-console fg-shipping-package-board"
    >
      <section class="prototype-banner">
        <button
          class="console-main-icon packaging-record-toggle"
          :class="{ 'is-active': showShippingDailyRecordTabs }"
          type="button"
          :title="
            showShippingDailyRecordTabs
              ? '点击隐藏点检记录表'
              : '点击显示点检记录表'
          "
          @click="toggleShippingDailyRecordTabs"
        >
          <IconifyIcon
            :icon="
              showShippingDailyRecordTabs
                ? 'lucide:layers'
                : 'lucide:package-check'
            "
          />
        </button>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">发货包装</h2>
          </div>
        </div>
        <div class="console-action-group">
          <button class="action-tile" type="button" @click="searchNotices">
            <IconifyIcon icon="lucide:refresh-cw" />
            <span>刷新</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('STARTUP')"
          >
            <IconifyIcon icon="lucide:power" />
            <span>开机点检</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('CLEANING')"
          >
            <IconifyIcon icon="lucide:spray-can" />
            <span>清洁保养</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('INNER')"
          >
            <IconifyIcon icon="lucide:clipboard-check" />
            <span>内包装点检</span>
          </button>
          <button
            class="action-tile"
            type="button"
            @click="openQuickCheck('OUTER')"
          >
            <IconifyIcon icon="lucide:clipboard-check" />
            <span>外包装点检</span>
          </button>
        </div>
      </section>

      <div class="fg-packaging-main">
        <Tabs
          v-model:active-key="activeMainTab"
          class="fg-packaging-tabs"
          @change="switchMainTab"
        >
          <TabPane v-for="tab in mainTabs" :key="tab.key" :tab="tab.title" />
          <TabPane
            v-if="showShippingDailyRecordTabs"
            :key="STARTUP_CHECK_TAB"
            tab="开机点检记录"
          />
          <TabPane
            v-if="showShippingDailyRecordTabs"
            :key="CLEANING_CHECK_TAB"
            tab="清洁保养记录"
          />
          <TabPane
            v-if="showShippingDailyRecordTabs"
            :key="INNER_CHECK_TAB"
            tab="内包装点检记录"
          />
          <TabPane
            v-if="showShippingDailyRecordTabs"
            :key="OUTER_CHECK_TAB"
            tab="外包装点检记录"
          />
        </Tabs>
        <div v-if="isShippingCheckTab" class="fg-tab-panel">
          <div class="shipping-check-query-bar">
            <DatePicker
              v-model:value="shippingDailyCheckQuery.recordDate"
              allow-clear
              placeholder="点检日期"
              value-format="YYYY-MM-DD"
              @change="() => refreshShippingDailyCheckGrid()"
            />
            <Button
              type="primary"
              :loading="shippingDailyCheckLoading"
              @click="refreshShippingDailyCheckGrid"
            >
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
            <Button
              type="primary"
              @click="openShippingCheckForm(activeShippingCheckAction)"
            >
              <template #icon>
                <IconifyIcon
                  :icon="
                    activeShippingCheckAction === 'STARTUP'
                      ? 'lucide:power'
                      : activeShippingCheckAction === 'CLEANING'
                        ? 'lucide:spray-can'
                        : 'lucide:clipboard-check'
                  "
                />
              </template>
              新增{{ activeShippingCheckForm.formTypeName }}
            </Button>
          </div>

          <section class="package-fg-grid-panel">
            <ShippingDailyCheckGrid
              :table-title="`${activeShippingCheckForm.formTypeName}记录`"
            >
              <template #processRecordStatus="{ row }">
                <Tag :color="recordStatusColor(row.recordStatus)">
                  {{ recordStatusText(row.recordStatus) }}
                </Tag>
              </template>
              <template #processResultStatus="{ row }">
                <Tag :color="resultColor(row.resultStatus)">
                  {{ row.resultStatus || '-' }}
                </Tag>
              </template>
              <template #shippingCheckActions="{ row }">
                <div class="fg-row-actions">
                  <Button
                    size="small"
                    type="link"
                    @click="openShippingCheckRecord(row, 'view')"
                  >
                    查看
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    :disabled="row.recordStatus === 'CONFIRMED'"
                    @click="openShippingCheckRecord(row, 'edit')"
                  >
                    修改
                  </Button>
                  <Button
                    danger
                    size="small"
                    type="link"
                    :disabled="row.recordStatus === 'CONFIRMED'"
                    @click="confirmShippingCheckRow(row)"
                  >
                    确认
                  </Button>
                </div>
              </template>
            </ShippingDailyCheckGrid>
          </section>
        </div>
        <div v-else class="fg-tab-panel">
          <div class="fg-query-bar">
            <Input
              v-model:value="query.keyword"
              allow-clear
              placeholder="需求单号 / 客户 / 型号 / 片号"
              @press-enter="searchNotices"
            />
            <Button type="primary" @click="searchNotices">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询{{ currentMainTab.title }}
            </Button>
          </div>

          <section class="package-fg-grid-panel">
            <NoticeGrid :table-title="currentMainTab.tableTitle">
              <template #productType="{ row }">
                <Tag :color="noticeTypeColor(row.productType)">{{
                  noticeTypeText(row.productType)
                }}</Tag>
              </template>
              <template #requirement="{ row }">{{
                requirementText(row)
              }}</template>
              <template #noticeStatus="{ row }">
                <Tag :color="packingStatusColor(row.noticeStatus)">{{
                  packingStatusText(row.noticeStatus)
                }}</Tag>
              </template>
              <template #shippingPackageName="{ row }">{{
                rowPackageName(row)
              }}</template>
              <template #shippingPackageTime="{ row }">{{
                rowPackageTime(row)
              }}</template>
              <template #action="{ row }">
                <Button size="small" type="link" @click="openDetail(row)">{{
                  isCompletedStatus(row.noticeStatus) ? '查看' : '进入打包'
                }}</Button>
              </template>
            </NoticeGrid>
          </section>
        </div>
      </div>

      <Modal
        v-model:open="detailVisible"
        :footer="null"
        :title="null"
        width="calc(100vw - 24px)"
        wrap-class-name="hc-pass-work-modal rough-report-work-modal inspection-task-work-modal shipping-notice-work-modal outbound-work-modal"
        @cancel="closeDetail"
      >
        <div
          class="report-modal-body inspection-task-modal-body outbound-detail-body"
          :class="{
            'is-loading': detailLoading,
            'shipping-detail-maximized-body': detailMaximized,
          }"
        >
          <div
            class="shipping-notice-form-top"
            :class="{ 'shipping-notice-form-top--compact': detailMaximized }"
          >
            <div class="shipping-notice-form-title-row">
              <span></span>
              <div class="inspection-form-title">发货包装单</div>
              <div class="shipping-notice-form-actions">
                <Button size="small" @click="refreshDetail(false)">
                  <template #icon
                    ><IconifyIcon icon="lucide:refresh-cw"
                  /></template>
                  刷新
                </Button>
                <Button size="small" @click="closeDetail">关闭</Button>
              </div>
            </div>
            <div class="shipping-form-sections">
              <fieldset class="shipping-form-fieldset">
                <legend>基本信息</legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <template v-if="currentNotice?.productType === 'SAMPLE'">
                    <label>样品数量</label>
                    <div class="inspection-form-control">{{ currentNotice?.requiredShipQty || currentNotice?.noticeQty || 0 }}</div>
                  </template>
                  <label>通知单号</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.noticeNo || '-' }}
                  </div>
                  <label>类型</label>
                  <div class="inspection-form-control">
                    <Tag :color="noticeTypeColor(currentNotice?.productType)">{{
                      noticeTypeText(currentNotice?.productType)
                    }}</Tag>
                  </div>
                  <label>ERP订单号</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.erpOrderNo || '-' }}
                  </div>
                  <label>发货时间</label>
                  <div class="inspection-form-control">
                    {{ formatDateTime(currentNotice?.shippingTime) }}
                  </div>
                  <label>记录人</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.recorderName || '-' }}
                  </div>
                  <label>记录时间</label>
                  <div class="inspection-form-control">
                    {{ formatDateTime(currentNotice?.recorderTime) }}
                  </div>
                  <label>发货备注</label>
                  <div
                    class="inspection-form-control shipping-form-control--span-2"
                  >
                    {{ currentNotice?.remark || '-' }}
                  </div>
                </div>
              </fieldset>
              <fieldset v-if="currentNotice?.productType !== 'SAMPLE'" class="shipping-form-fieldset">
                <legend>
                  {{
                    currentNotice?.productType === 'SAMPLE' ||
                    currentNotice?.productType === '样品'
                      ? '样品要求'
                      : '客户要求'
                  }}
                </legend>
                <div class="inspection-form-head shipping-section-form-head">
                  <label>客户名称</label>
                  <div class="inspection-form-control">
                    {{ currentNotice?.customerName || '-' }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE' ||
                    currentNotice?.productType === '样品'
                      ? '样品型号'
                      : '产品型号'
                  }}</label>
                  <div class="inspection-form-control">
                    {{
                      currentNotice?.externalProductModel ||
                      currentNotice?.modelCode ||
                      '-'
                    }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE' ||
                    currentNotice?.productType === '样品'
                      ? '样品批号'
                      : '产品编码'
                  }}</label>
                  <div class="inspection-form-control">
                    {{
                      currentNotice?.productType === 'SAMPLE' ||
                      currentNotice?.productType === '样品'
                        ? currentNotice?.requiredBatchNo || '-'
                        : currentNotice?.externalProductCode ||
                          currentNotice?.materialCode ||
                          '-'
                    }}
                  </div>
                  <label>{{
                    currentNotice?.productType === 'SAMPLE' ||
                    currentNotice?.productType === '样品'
                      ? '样品数量'
                      : '发货数量'
                  }}</label>
                  <div class="inspection-form-control">
                    {{
                      currentNotice?.requiredShipQty ||
                      currentNotice?.noticeQty ||
                      '-'
                    }}
                  </div>
                  <label
                    v-if="
                      currentNotice?.productType !== 'SAMPLE' &&
                      currentNotice?.productType !== '样品'
                    "
                    >片号范围</label
                  >
                  <div
                    v-if="
                      currentNotice?.productType !== 'SAMPLE' &&
                      currentNotice?.productType !== '样品'
                    "
                    class="inspection-form-control"
                  >
                    {{ currentNotice?.requiredSliceRange || '-' }}
                  </div>
                  <label
                    v-if="
                      currentNotice?.productType !== 'SAMPLE' &&
                      currentNotice?.productType !== '样品'
                    "
                    >生产批号</label
                  >
                  <div
                    v-if="
                      currentNotice?.productType !== 'SAMPLE' &&
                      currentNotice?.productType !== '样品'
                    "
                    class="inspection-form-control"
                  >
                    {{ currentNotice?.requiredBatchNo || '-' }}
                  </div>
                  <label>包装要求</label>
                  <div
                    class="inspection-form-control shipping-form-control--span-2"
                  >
                    {{ currentNotice?.packingRequirement || '-' }}
                  </div>
                </div>
              </fieldset>
            </div>
          </div>

          <Tabs
            v-model:active-key="activeDetailTab"
            class="shipping-detail-tabs outbound-detail-tabs"
          >
            <template #rightExtra>
              <Button
                class="shipping-tab-maximize-btn"
                size="small"
                @click="detailMaximized = !detailMaximized"
              >
                <template #icon
                  ><IconifyIcon
                    :icon="
                      detailMaximized
                        ? 'lucide:minimize-2'
                        : 'lucide:maximize-2'
                    "
                /></template>
                {{ detailMaximized ? '还原' : '最大化' }}
              </Button>
            </template>
            <TabPane key="items" tab="内包装">
              <div
                class="shipping-tab-toolbar shipping-tab-toolbar--with-control"
              >
                <span>内包装 {{ detailItems.length }} 条</span>
                <div class="shipping-tab-toolbar-controls">
                  <Button
                    :disabled="
                      currentNoticeCompleted ||
                      !selectedInnerItems.length ||
                      !['OQC_PASSED', 'PACKAGED'].includes(
                        currentNotice?.noticeStatus || '',
                      )
                    "
                    :loading="packageSubmitting"
                    size="small"
                    type="primary"
                    @click="openPackageCreate"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:package-check"
                    /></template>
                    外包装盒
                  </Button>
                  <Button
                    :disabled="
                      currentNoticeCompleted ||
                      currentNotice?.noticeStatus !== 'INSPECTED'
                    "
                    :loading="packageLoading"
                    size="small"
                    type="primary"
                    @click="pushOqc"
                  >
                    <template #icon><IconifyIcon icon="lucide:send" /></template>
                    推送检验
                  </Button>
                  <em>已选择 {{ selectedInnerItems.length }} 条</em>
                  <span>分组</span>
                  <Select
                    v-model:value="innerPackageGroupMode"
                    :options="currentNotice?.productType === 'SAMPLE' ? innerPackageGroupModeOptions.filter((item) => item.value === 'none') : innerPackageGroupModeOptions"
                    :disabled="currentNotice?.productType === 'SAMPLE'"
                    class="inner-package-group-select"
                    size="small"
                  />
                </div>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="innerPackageDisplayRows"
                  :loading="detailLoading"
                  :row-class-name="innerPackageRowClassName"
                  :tree-config="innerPackageTreeConfig"
                  align="center"
                  auto-resize
                  border
                  header-align="center"
                  height="100%"
                  row-id="innerDisplayRowKey"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    align="center"
                    fixed="left"
                    title="选择"
                    width="54"
                  >
                    <template #default="{ row }">
                      <Checkbox
                        v-if="isInnerGroupRow(row)"
                        :checked="isInnerGroupSelected(row)"
                        :disabled="!row.groupSelectableCount"
                        :indeterminate="isInnerGroupIndeterminate(row)"
                        @change="toggleInnerGroupSelected(row)"
                      />
                      <Checkbox
                        v-else
                        :checked="isInnerSelected(row)"
                        :disabled="!row.actualSliceBatchNo"
                        @change="toggleInnerSelected(row)"
                      />
                    </template>
                  </VxeColumn>
                  <VxeColumn field="rowNo" fixed="left" title="序号" width="62">
                    <template #default="{ row }">{{
                      isInnerGroupRow(row) ? '-' : row.rowNo
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    align="left"
                    field="internalModelCode"
                    title="内部型号"
                    tree-node
                    width="180"
                  >
                    <template #default="{ row }">
                      <div
                        v-if="isInnerGroupRow(row)"
                        class="inner-package-group-cell"
                      >
                        <strong
                          >{{ row.groupLabel }}：{{ row.groupKey }}</strong
                        >
                        <em
                          >{{ row.groupCount }} 条，已选
                          {{ row.groupSelectedCount || 0 }}/{{
                            row.groupSelectableCount || 0
                          }}</em
                        >
                      </div>
                      <span v-else>{{ row.internalModelCode || '-' }}</span>
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    field="internalItemCode"
                    title="内部批号"
                    width="170"
                  >
                    <template #default="{ row }">{{
                      isInnerGroupRow(row) ? '-' : row.internalItemCode || '-'
                    }}</template>
                  </VxeColumn>
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="customerProductBatchNo"
                    title="产品批号"
                    width="150"
                  >
                    <template #default="{ row }">{{
                      row.customerProductBatchNo || '-'
                    }}</template>
                  </VxeColumn>
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="packageSliceNo"
                    title="包装片号"
                    width="170"
                  >
                    <template #default="{ row }">{{
                      row.packageSliceNo || '-'
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    :show-overflow="false"
                    field="actualSliceBatchNo"
                    title="实际片号"
                    width="190"
                  >
                    <template #default="{ row }">
                      <span class="shipping-wrap-cell">{{
                        row.actualSliceBatchNo || '-'
                      }}</span>
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingInspectionResult"
                    title="发货检验"
                    width="110"
                  >
                    <template #default="{ row }">
                      <Tag v-if="isInnerGroupRow(row)" color="blue">分组</Tag>
                      <Tag
                        v-else
                        :color="qualityColor(row.shippingInspectionResult)"
                        >{{ row.shippingInspectionResult || '-' }}</Tag
                      >
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingInspectorName"
                    title="检验人"
                    width="120"
                  >
                    <template #default="{ row }">{{
                      isInnerGroupRow(row)
                        ? '-'
                        : row.shippingInspectorName || '-'
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingInspectionTime"
                    title="检验时间"
                    width="160"
                  >
                    <template #default="{ row }">{{
                      isInnerGroupRow(row)
                        ? '-'
                        : formatDateTime(row.shippingInspectionTime)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingPackageName"
                    title="包装人"
                    width="120"
                  >
                    <template #default="{ row }">{{
                      isInnerGroupRow(row)
                        ? '-'
                        : row.shippingPackageName || '-'
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingPackageTime"
                    title="包装时间"
                    width="160"
                  >
                    <template #default="{ row }">{{
                      isInnerGroupRow(row)
                        ? '-'
                        : formatDateTime(row.shippingPackageTime)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    align="center"
                    field="visualPrintStatus"
                    title="打印状态"
                    width="210"
                  >
                    <template #default="{ row }">
                      <Tag v-if="isInnerGroupRow(row)" color="blue"
                        >{{ row.groupCount }} 条</Tag
                      >
                      <div v-else class="visual-print-status-tags">
                        <Tag
                          :color="
                            visualPrintStatusColor(
                              getItemVisualPrintState('padBack', row).status,
                            )
                          "
                        >
                          {{
                            visualPrintTagText(
                              'padBack',
                              getItemVisualPrintState('padBack', row),
                            )
                          }}
                        </Tag>
                        <Tag
                          :color="
                            visualPrintStatusColor(
                              getItemVisualPrintState('cleanBag', row).status,
                            )
                          "
                        >
                          {{
                            visualPrintTagText(
                              'cleanBag',
                              getItemVisualPrintState('cleanBag', row),
                            )
                          }}
                        </Tag>
                      </div>
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    align="center"
                    field="visualPrintAction"
                    fixed="right"
                    title="操作"
                    width="190"
                  >
                    <template #default="{ row }">
                      <span
                        v-if="isInnerGroupRow(row)"
                        class="inner-package-group-placeholder"
                        >-</span
                      >
                      <div v-else class="fg-row-actions">
                        <Button
                          :loading="visualPrintLoading"
                          size="small"
                          type="link"
                          @click="openVisualPrint('padBack', row)"
                        >
                          打印PAD背标
                        </Button>
                        <Button
                          :loading="visualPrintLoading"
                          size="small"
                          type="link"
                          @click="openVisualPrint('cleanBag', row)"
                        >
                          打印洁净袋
                        </Button>
                      </div>
                    </template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
            <TabPane key="packages" tab="外包装">
              <div
                class="shipping-tab-toolbar shipping-tab-toolbar--with-control"
              >
                <div>
                  <span>外包装分组 {{ packageGroups.length }} 个</span>
                  <em>按发货包装编号 / 箱号聚合</em>
                </div>
                <div class="shipping-tab-toolbar-controls">
                  <Button
                    v-access="['mes:inv:fg-shipping-package:shipping-complete']"
                    :disabled="
                      currentNoticeCompleted ||
                      currentNotice?.noticeStatus !== 'PACKAGED'
                    "
                    :loading="packageLoading"
                    size="small"
                    type="primary"
                    @click="completeShipping"
                  >
                    <template #icon
                      ><IconifyIcon icon="lucide:circle-check-big"
                    /></template>
                    发货完成
                  </Button>
                </div>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="packageGroups"
                  :loading="detailLoading"
                  align="center"
                  auto-resize
                  border
                  header-align="center"
                  height="100%"
                  row-id="packageKey"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    align="center"
                    field="rowNo"
                    fixed="left"
                    title="序号"
                    width="62"
                  />
                  <VxeColumn
                    :show-overflow="false"
                    :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="customerBatchText"
                    fixed="left"
                    title="产品批号"
                    width="150"
                  >
                    <template #default="{ row }">
                      <span class="shipping-wrap-cell">{{
                        row.customerBatchText || '-'
                      }}</span>
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    field="packageMethod"
                    title="包装方式"
                    width="110"
                  />
                  <VxeColumn
                    align="center"
                    field="itemCount"
                    title="包装内货物数"
                    width="120"
                  />
                  <VxeColumn
                    :show-overflow="false"
                    field="actualSliceText"
                    min-width="290"
                    title="实际片号"
                  >
                    <template #default="{ row }">
                      <span class="shipping-wrap-cell">{{
                        row.actualSliceText || '-'
                      }}</span>
                    </template>
                  </VxeColumn>
                  <VxeColumn field="packageName" title="包装人" width="120" />
                  <VxeColumn field="packageTime" title="包装时间" width="160">
                    <template #default="{ row }">{{
                      formatDateTime(row.packageTime)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="auxMaterialName"
                    title="耗材名称"
                    width="160"
                  />
                  <VxeColumn
                    align="center"
                    field="visualPrintStatus"
                    title="打印状态"
                    width="230"
                  >
                    <template #default="{ row }">
                      <div class="visual-print-status-tags">
                        <Tag
                          :color="
                            visualPrintStatusColor(
                              getPackageVisualPrintState('boxFront', row)
                                .status,
                            )
                          "
                        >
                          {{
                            visualPrintTagText(
                              'boxFront',
                              getPackageVisualPrintState('boxFront', row),
                            )
                          }}
                        </Tag>
                        <Tag
                          :color="
                            visualPrintStatusColor(
                              getPackageVisualPrintState('customerSide', row)
                                .status,
                            )
                          "
                        >
                          {{
                            visualPrintTagText(
                              'customerSide',
                              getPackageVisualPrintState('customerSide', row),
                            )
                          }}
                        </Tag>
                      </div>
                    </template>
                  </VxeColumn>
                  <VxeColumn
                    align="center"
                    field="packagePrintAction"
                    fixed="right"
                    title="操作"
                    width="190"
                  >
                    <template #default="{ row }">
                      <div class="fg-row-actions">
                        <Button
                          :loading="visualPrintLoading"
                          size="small"
                          type="link"
                          @click="openVisualPrint('boxFront', undefined, row)"
                        >
                          打印盒正标
                        </Button>
                        <Button
                          :loading="visualPrintLoading"
                          size="small"
                          type="link"
                          @click="
                            openVisualPrint('customerSide', undefined, row)
                          "
                        >
                          打印客户侧标
                        </Button>
                      </div>
                    </template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
            <TabPane key="pick" tab="配货领用">
              <div class="shipping-tab-toolbar">
                <span>配货领用 {{ pickItems.length }} 条</span>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="pickItems"
                  :loading="detailLoading"
                  align="center"
                  auto-resize
                  border
                  header-align="center"
                  height="100%"
                  row-id="id"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    field="rowNo"
                    fixed="left"
                    title="序号"
                    width="62"
                  />
                  <VxeColumn
                    field="internalModelCode"
                    title="产品型号"
                    width="140"
                  />
                  <VxeColumn
                    field="actualSliceBatchNo"
                    title="片号"
                    width="190"
                  />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="customerProductBatchNo"
                    title="客户批号"
                    width="150"
                  />
                  <VxeColumn field="location" title="库位" width="220">
                    <template #default="{ row }">{{
                      itemLocationText(row)
                    }}</template>
                  </VxeColumn>
                  <VxeColumn
                    field="shippingInspectionResult"
                    title="发货检验"
                    width="110"
                  >
                    <template #default="{ row }"
                      ><Tag
                        :color="qualityColor(row.shippingInspectionResult)"
                        >{{ row.shippingInspectionResult || '-' }}</Tag
                      ></template
                    >
                  </VxeColumn>
                  <VxeColumn
                    field="shippingPackageName"
                    title="包装人"
                    width="120"
                  />
                  <VxeColumn
                    field="shippingPackageTime"
                    title="包装时间"
                    width="160"
                  >
                    <template #default="{ row }">{{
                      formatDateTime(row.shippingPackageTime)
                    }}</template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
            <TabPane key="oqc" tab="出货检验">
              <div class="shipping-tab-toolbar">
                <span>出货检验单 {{ oqcRows.length }} 条</span>
                <em>每 15 秒自动刷新</em>
              </div>
              <div class="outbound-detail-table shipping-vxe-table">
                <VxeTable
                  :data="oqcRows"
                  :loading="detailLoading"
                  align="center"
                  auto-resize
                  border
                  header-align="center"
                  height="100%"
                  row-id="id"
                  show-overflow
                  size="small"
                  stripe
                >
                  <VxeColumn
                    field="rowNo"
                    fixed="left"
                    title="序号"
                    width="62"
                  />
                  <VxeColumn
                    field="shippingQualityNo"
                    title="出货检验单号"
                    width="170"
                  />
                  <VxeColumn field="oqcStatus" title="检验状态" width="120">
                    <template #default="{ row }"
                      ><Tag :color="oqcStatusColor(row.oqcStatus)">{{
                        oqcStatusText(row.oqcStatus)
                      }}</Tag></template
                    >
                  </VxeColumn>
                  <VxeColumn
                    field="internalModelCode"
                    title="内部型号"
                    width="140"
                  />
                  <VxeColumn
                    field="internalItemCode"
                    title="内部批号"
                    width="170"
                  />
                  <VxeColumn
                    field="actualSliceBatchNo"
                    title="实际片号"
                    width="190"
                  />
                  <VxeColumn :visible="currentNotice?.productType !== 'SAMPLE'"
                    field="customerProductBatchNo"
                    title="产品批号"
                    width="150"
                  />
                  <VxeColumn
                    field="shippingPackageName"
                    title="包装人"
                    width="120"
                  />
                  <VxeColumn
                    field="shippingPackageTime"
                    title="包装时间"
                    width="160"
                  >
                    <template #default="{ row }">{{
                      formatDateTime(row.shippingPackageTime)
                    }}</template>
                  </VxeColumn>
                </VxeTable>
              </div>
            </TabPane>
          </Tabs>
        </div>
      </Modal>

      <Modal
        v-model:open="shippingDailyCheckDialogVisible"
        :footer="null"
        :width="860"
        destroy-on-close
        title="今日点检/清洁记录"
      >
        <div class="shipping-daily-check-modal-head">
          <span>记录日期：{{ dayjs().format('YYYY-MM-DD') }}</span>
          <Button
            size="small"
            :loading="shippingDailyCheckLoading"
            @click="loadShippingDailyCheckCards"
          >
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            刷新
          </Button>
        </div>
        <div class="shipping-daily-check-card-grid">
          <section
            v-for="card in shippingDailyCheckCards"
            :key="card.action"
            class="shipping-daily-check-card"
          >
            <span class="shipping-daily-check-card__icon">
              <IconifyIcon
                :icon="
                  card.action === 'CLEANING'
                    ? 'lucide:sparkles'
                    : 'lucide:clipboard-check'
                "
              />
            </span>
            <span class="shipping-daily-check-card__content">
              <strong>{{ card.title }}</strong>
              <em>{{ card.formName }}</em>
              <span>{{ card.timing }} / {{ dailyCheckCardStatusText(card.status) }}</span>
            </span>
            <Tag
              :color="dailyCheckCardStatusColor(card.status)"
              class="shipping-daily-check-card__tag"
            >
              {{ dailyCheckCardStatusText(card.status) }}
            </Tag>
            <span class="shipping-daily-check-card__actions">
              <Button
                v-if="card.status !== 'CONFIRMED'"
                size="small"
                type="primary"
                @click="openShippingDailyCheckCard(card)"
              >
                填写
              </Button>
              <Button
                v-if="card.status === 'PENDING_CONFIRM' && card.record"
                danger
                size="small"
                @click="confirmShippingCheckRow(card.record)"
              >
                确认
              </Button>
              <Button
                v-if="card.record"
                size="small"
                @click="openShippingDailyCheckCard(card, 'view')"
              >
                查看
              </Button>
            </span>
          </section>
        </div>
        <div
          v-if="!shippingDailyCheckLoading && shippingDailyCheckCards.length === 0"
          class="shipping-daily-check-empty"
        >
          暂无可用的包装点检模板，请检查动态表单配置。
        </div>
        <div class="shipping-daily-check-modal-footer">
          <Button @click="shippingDailyCheckDialogVisible = false">关闭</Button>
        </div>
      </Modal>

      <StationFormRuntimeFillModal
        v-model:open="runtimeFillOpen"
        confirm-auth-action-name="填写确认人员并确认"
        :enable-confirm="true"
        :form="runtimeFillForm"
        :initial-params="runtimeInitialParams"
        save-auth-action-name="填写保存人员并确认"
        skip-business-param-step
        @success="handleRuntimeFillSaved"
      />

      <Modal
        :open="runtimeViewVisible"
        :footer="null"
        :title="null"
        :z-index="3220"
        destroy-on-close
        width="100vw"
        wrap-class-name="hc-pass-work-modal shipping-dev-process-form-view-modal"
        @cancel="closeRuntimeView"
      >
        <div class="shipping-dev-runtime-view">
          <div class="shipping-dev-runtime-view__header">
            <div>
              <h3>{{ runtimeViewTitle }}</h3>
              <div class="shipping-dev-runtime-view__meta">
                <span v-for="item in runtimeViewRecordMeta" :key="item">{{
                  item
                }}</span>
              </div>
            </div>
            <div class="shipping-dev-runtime-view__actions">
              <Button
                v-if="!runtimeViewReadOnly"
                :loading="runtimeViewSaving"
                type="primary"
                @click="requestRuntimeViewSigner('SAVE')"
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
                @click="requestRuntimeViewSigner('CONFIRM')"
              >
                确认
              </Button>
              <Button @click="closeRuntimeView">关闭</Button>
            </div>
          </div>
          <div class="shipping-dev-runtime-view__body">
            <StationFormRuntimeRenderer
              v-if="runtimeViewRecord && !runtimeViewLoading"
              ref="runtimeRendererRef"
              v-model:header-data="runtimeViewHeaderData"
              :form-name="runtimeViewTitle"
              :items="runtimeViewItems"
              :record-meta="runtimeViewRecordMeta"
              :schema="runtimeViewSchema"
              :readonly="runtimeViewReadOnly"
            />
            <div v-else class="shipping-dev-runtime-loading">
              正在加载包装点检表详情...
            </div>
          </div>
        </div>
      </Modal>

      <AuthModal
        v-model:visible="runtimeViewSignerVisible"
        :action-name="
          runtimeViewSignerAction === 'SAVE'
            ? '填写保存人员并确认'
            : '填写确认人员并确认'
        "
        auth-mode="username"
        title="点检人员确认"
        @success="handleRuntimeViewSignerSuccess"
      />

      <Modal
        v-model:open="packageCreateVisible"
        :confirm-loading="packageSubmitting"
        title="打包确认"
        width="620px"
        @ok="submitPackageCreate"
      >
        <div class="package-create-summary">
          <span>已选择 {{ selectedInnerItems.length }} 条客户批号明细</span>
          <em>{{
            compactJoined(
              selectedInnerItems.map((row) => row.actualSliceBatchNo),
              5,
            )
          }}</em>
        </div>
        <div class="aux-consume-form package-create-form">
          <label>包装方式</label>
          <Select
            v-model:value="packageForm.packageMethod"
            :options="packageMethodOptions"
            placeholder="请选择包装方式"
          />
          <label>外包装辅材</label>
          <div class="package-aux-lines">
            <div
              v-for="(item, index) in packageForm.auxConsumeItems"
              :key="index"
              class="package-aux-line"
            >
              <Select
                v-model:value="item.ledgerId"
                allow-clear
                class="package-aux-batch-select"
                :loading="packageAuxLoading"
                :options="packageAuxOptions"
                placeholder="选择包装工序耗材批次"
                show-search
              />
              <div class="package-aux-quantity-field">
                <span>可用量</span>
                <Input
                  :value="getPackageAuxAvailableQty(item.ledgerId)"
                  class="w-full"
                  disabled
                  placeholder="-"
                />
              </div>
              <div class="package-aux-quantity-field">
                <span>本次领用量</span>
                <InputNumber
                  v-model:value="item.consumeQty"
                  :min="0.001"
                  :precision="3"
                  class="w-full"
                  placeholder="请输入"
                />
              </div>
            </div>
            <span
              >显示包装工序耗材的有效批次，打包成功后自动扣减可用结存。</span
            >
          </div>
          <label>备注</label>
          <Input
            v-model:value="packageForm.remark"
            allow-clear
            placeholder="备注"
          />
        </div>
      </Modal>

      <Modal
        v-model:open="visualTemplatePickerVisible"
        :confirm-loading="visualPrintLoading"
        :title="`${visualPendingActionTitle} - 选择模板`"
        width="980px"
        @ok="submitVisualTemplateSelection"
      >
        <div class="visual-print-modal">
          <div class="visual-print-summary">
            <strong>{{ currentNotice?.noticeNo || '-' }}</strong>
            <span>{{ currentNotice?.customerName || '-' }}</span>
          </div>
          <p v-if="visualTemplatePickerHint" class="visual-print-hint">
            {{ visualTemplatePickerHint }}
          </p>
          <div class="visual-template-picker">
            <div class="visual-template-picker-title">模板配置</div>
            <div class="visual-template-picker-toolbar">
              <Input
                v-model:value="visualTemplateKeyword"
                allow-clear
                placeholder="输入模板名称、客户、产品类型、尺寸、标签类型过滤"
              />
              <span
                >共 {{ filteredVisualTemplateCandidates.length }} /
                {{ visualTemplateCandidates.length }} 条</span
              >
            </div>
            <VxeTable
              border
              class="visual-template-picker-table"
              :data="filteredVisualTemplateCandidates"
              empty-text="暂无匹配模板"
              :height="360"
              row-id="key"
              show-overflow
              size="small"
              stripe
              @cell-click="handleVisualTemplateCellClick"
            >
              <VxeColumn align="center" fixed="left" title="选择" width="62">
                <template #default="{ row }">
                  <Checkbox
                    :checked="visualSelectedTemplateKey === row.key"
                    @change="selectVisualTemplateCandidate(row)"
                  />
                </template>
              </VxeColumn>
              <VxeColumn align="center" title="参考图" width="118">
                <template #default="{ row }">
                  <div class="visual-template-image-cell">
                    <Image
                      v-if="getVisualCandidateImageUrl(row)"
                      class="visual-template-image"
                      :height="48"
                      :src="getVisualCandidateImageUrl(row)"
                      :width="82"
                    />
                    <span v-else>-</span>
                  </div>
                </template>
              </VxeColumn>
              <VxeColumn align="center" title="标签类型" width="108">
                <template #default="{ row }">
                  <Tag>{{
                    VISUAL_LABEL_KIND_META[getVisualCandidateLabelKind(row)]
                      ?.shortTitle || getVisualCandidateLabelKind(row)
                  }}</Tag>
                </template>
              </VxeColumn>
              <VxeColumn align="center" title="来源" width="92">
                <template #default="{ row }">
                  <Tag :color="row.design.id ? 'green' : 'blue'">{{
                    getVisualCandidateSourceText(row)
                  }}</Tag>
                </template>
              </VxeColumn>
              <VxeColumn min-width="180" title="模板名称">
                <template #default="{ row }">{{
                  row.design.labelName || '-'
                }}</template>
              </VxeColumn>
              <VxeColumn min-width="150" title="客户">
                <template #default="{ row }">{{
                  row.row.customer || '-'
                }}</template>
              </VxeColumn>
              <VxeColumn title="产品类型" width="120">
                <template #default="{ row }">{{
                  row.row.productType || '-'
                }}</template>
              </VxeColumn>
              <VxeColumn title="尺寸/mm" width="100">
                <template #default="{ row }">{{
                  row.row.sizeMm || '-'
                }}</template>
              </VxeColumn>
            </VxeTable>
          </div>
          <div class="visual-print-form">
            <label>本机服务</label>
            <Input :value="VISUAL_PRINT_AGENT_URL" disabled />
          </div>
        </div>
      </Modal>

      <Modal
        v-model:open="visualMissingVisible"
        :confirm-loading="visualPrintLoading"
        :title="`${visualPendingActionTitle} - 补录打印变量`"
        ok-text="保存并打印"
        width="680px"
        @ok="submitVisualMissingInputs"
      >
        <div class="visual-print-modal">
          <div class="visual-print-summary">
            <strong>{{ currentNotice?.noticeNo || '-' }}</strong>
            <span
              >当前模板仍有
              {{ visualMissingFields.length }}
              个变量无法从发货包装单自动获得</span
            >
          </div>
          <div class="visual-print-form">
            <template
              v-for="field in visualMissingFields"
              :key="field.fieldKey"
            >
              <label>
                {{ field.label }}
                <em>{{ field.fieldKey }}</em>
              </label>
              <Input
                v-model:value="visualMissingInputs[field.fieldKey]"
                allow-clear
                :placeholder="`请输入${field.label}`"
              />
            </template>
          </div>
          <p class="visual-print-hint">
            补录值会按当前通知单、当前打印对象和模板记忆，下次点击同一行同一模板会自动带入。
          </p>
        </div>
      </Modal>
    </div>
  </Page>
</template>

<style scoped>
.packaging-record-toggle {
  padding: 0;
  border: 0;
  cursor: pointer;
  transition:
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.packaging-record-toggle:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.32);
}

.packaging-record-toggle.is-active {
  background: linear-gradient(135deg, #0f766e, #2563eb);
  box-shadow:
    0 0 0 2px rgba(13, 148, 136, 0.26),
    0 10px 20px rgba(15, 118, 110, 0.32);
}

.packaging-record-toggle:focus-visible {
  outline: 2px solid #0ea5e9;
  outline-offset: 2px;
}

.shipping-daily-check-modal-head,
.shipping-daily-check-modal-footer {
  display: flex;
  gap: 8px;
  align-items: center;
}

.shipping-daily-check-modal-head {
  justify-content: space-between;
  padding-bottom: 12px;
  color: #475569;
  font-size: 13px;
  border-bottom: 1px solid #d8e0ea;
}

.shipping-daily-check-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 0;
}

.shipping-daily-check-card-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.shipping-daily-check-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  min-height: 136px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
}

.shipping-daily-check-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  color: #0369a1;
  font-size: 25px;
  background: #e0f2fe;
  border: 1px solid #7dd3fc;
  border-radius: 4px;
}

.shipping-daily-check-card__content {
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 3px;
}

.shipping-daily-check-card__content strong {
  color: #0f172a;
  font-weight: 800;
}

.shipping-daily-check-card__content em,
.shipping-daily-check-card__content span {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shipping-daily-check-card__tag {
  align-self: start;
  margin: 0;
}

.shipping-daily-check-card__actions {
  display: flex;
  grid-column: 2 / 4;
  gap: 8px;
  justify-content: flex-end;
}

.shipping-daily-check-empty {
  padding: 32px 0;
  color: #64748b;
  text-align: center;
}

.shipping-daily-check-modal-footer {
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #d8e0ea;
}

.fg-packaging-workbench {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.fg-query-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 128px;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.shipping-check-query-bar {
  display: grid;
  grid-template-columns: minmax(220px, 320px) max-content max-content;
  gap: 8px;
  align-items: center;
  padding: 8px 10px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.shipping-check-query-bar :deep(.ant-picker) {
  width: 100%;
}

.fg-packaging-main {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.fg-packaging-tabs {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.fg-packaging-tabs :deep(.ant-tabs-nav) {
  margin: 0;
  padding: 0 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.fg-packaging-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.fg-tab-panel {
  display: grid;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
}

.package-fg-grid-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  height: 100%;
  min-height: 0;
  min-width: 0;
  padding: 8px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid #8794a4;
}

.package-fg-grid-panel :deep(.vben-vxe-grid),
.package-fg-grid-panel :deep(.vxe-grid) {
  height: 100%;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid) {
  display: flex;
  flex-direction: column;
}

.package-fg-grid-panel :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
}

.package-fg-grid-panel :deep(.vxe-grid--pager-wrapper) {
  flex: 0 0 auto;
}

.package-fg-grid-panel :deep(.vxe-pager) {
  margin: 0;
  border-top: 1px solid #d8e0ea;
}

.outbound-detail-body {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  gap: 10px;
  overflow: hidden;
}

.outbound-detail-body.is-loading {
  cursor: progress;
}

.shipping-notice-form-top {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  min-height: 0;
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-notice-form-top--compact {
  gap: 0;
  padding-bottom: 6px;
}

.shipping-notice-form-top--compact .shipping-form-sections {
  display: none;
}

.shipping-notice-form-title-row {
  display: grid;
  grid-template-columns: minmax(520px, 1fr) auto minmax(520px, 1fr);
  align-items: center;
  min-height: 28px;
}

.shipping-notice-form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.shipping-form-sections {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  min-height: 0;
}

.shipping-form-fieldset {
  display: grid;
  gap: 6px;
  min-width: 0;
  padding: 10px 10px 8px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.shipping-form-fieldset > legend {
  padding: 0 8px;
  color: #1677ff;
  font-size: 13px;
  font-weight: 700;
  line-height: 20px;
  background: #fff;
}

.inspection-form-title {
  color: #0f172a;
  font-size: 24px;
  font-weight: 900;
  line-height: 1.1;
  text-align: center;
}

.inspection-form-head {
  display: grid;
  grid-template-columns:
    92px minmax(0, 1fr) 92px minmax(0, 1fr) 92px minmax(0, 1fr)
    92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #9fb6cd;
  border-right: 0;
  border-bottom: 0;
}

.inspection-form-head label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 800;
  background: #e2e8f0;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.inspection-form-control {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #f8fafc;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.shipping-section-form-head {
  grid-template-columns:
    110px minmax(0, 1fr) 110px minmax(0, 1fr) 110px minmax(0, 1fr)
    110px minmax(0, 1fr);
}

.shipping-form-control--span-2 {
  grid-column: span 3;
}

.shipping-form-label--full-row {
  grid-column: 1;
}

.shipping-form-control--full-row {
  grid-column: span 7;
}

.shipping-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0 8px 8px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.shipping-detail-tabs :deep(.ant-tabs-nav) {
  align-items: center;
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-tab-maximize-btn {
  display: inline-flex;
  gap: 4px;
  align-items: center;
}

.shipping-detail-tabs :deep(.ant-tabs-content-holder),
.shipping-detail-tabs :deep(.ant-tabs-content),
.shipping-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.shipping-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  overflow: hidden;
}

.shipping-tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex: 0 0 auto;
  min-height: 34px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
}

.shipping-tab-toolbar em {
  color: #64748b;
  font-style: normal;
}

.shipping-tab-toolbar > div:first-child {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.shipping-tab-toolbar-controls {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.shipping-tab-toolbar-controls > span {
  color: #334155;
  font-weight: 900;
  white-space: nowrap;
}

.inner-package-group-select {
  width: 128px;
}

.shipping-vxe-table :deep(.inner-package-group-row) {
  background: #f8fafc;
}

.shipping-vxe-table :deep(.vxe-header--column .vxe-cell) {
  justify-content: center;
  text-align: center;
}

.shipping-vxe-table :deep(.vxe-cell--tree-btn) {
  display: inline-flex;
  width: 16px;
  height: 16px;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  color: #2563eb;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 900;
  line-height: 1;
  vertical-align: middle;
}

.shipping-vxe-table :deep(.vxe-cell--tree-btn::before) {
  content: '+';
}

.shipping-vxe-table
  :deep(.vxe-cell--tree-node.is--active .vxe-cell--tree-btn::before) {
  content: '-';
}

.shipping-vxe-table :deep(.vxe-cell--tree-btn > *) {
  display: none;
}

.inner-package-group-cell {
  display: grid;
  min-width: 0;
  gap: 2px;
  line-height: 1.2;
}

.inner-package-group-cell strong {
  overflow: hidden;
  color: #075985;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inner-package-group-cell em {
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
}

.inner-package-group-placeholder {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

.shipping-wrap-cell {
  display: block;
  max-width: 100%;
  line-height: 1.35;
  white-space: normal;
  word-break: break-all;
}

.package-group-summary {
  display: grid;
  flex: 0 0 auto;
  gap: 8px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.package-group-summary span {
  display: grid;
  min-width: 0;
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
  gap: 4px;
}

.package-group-summary em {
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.package-group-summary strong {
  overflow: hidden;
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 18px;
  font-weight: 950;
  line-height: 1.1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-oqc-cell {
  display: inline-flex;
  max-width: 100%;
  gap: 6px;
  align-items: center;
}

.package-oqc-cell span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.outbound-detail-table,
.outbound-detail-table.shipping-vxe-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.shipping-tab-check-panel {
  display: grid;
  flex: 1 1 auto;
  grid-template-rows: max-content minmax(0, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.shipping-package-check-toolbar {
  display: grid;
  gap: 8px;
  grid-template-columns: 180px 96px 112px 128px minmax(0, 1fr);
  align-items: center;
  flex: 0 0 auto;
  padding: 8px;
  background: #eef3f8;
  border: 1px solid #8794a4;
}

.shipping-package-check-toolbar :deep(.ant-picker),
.shipping-package-check-toolbar :deep(.ant-btn) {
  height: 34px;
  border-radius: 0;
  font-weight: 800;
}

.shipping-detail-check-grid-panel {
  flex: 1 1 auto;
  height: 100%;
}

.fg-row-actions {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.fg-row-actions :deep(.ant-btn) {
  padding-right: 4px;
  padding-left: 4px;
}

.visual-print-status-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
}

.visual-print-status-tags :deep(.ant-tag) {
  margin-inline-end: 0;
  font-size: 12px;
  font-weight: 800;
}

.aux-consume-form {
  display: grid;
  align-items: center;
  gap: 12px;
  grid-template-columns: 96px minmax(0, 1fr);
}

.aux-consume-form label {
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  text-align: right;
}

.package-aux-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.package-aux-line {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  align-items: center;
}

.package-aux-batch-select {
  grid-column: 1 / -1;
  width: 100%;
}

.package-aux-quantity-field {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.package-aux-quantity-field > span {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  text-align: right;
  white-space: nowrap;
}

.package-aux-lines > span {
  color: #64748b;
  font-size: 12px;
}

.full-input {
  width: 100%;
}

.package-create-summary {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.package-create-summary span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 900;
}

.package-create-summary em {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.visual-print-modal {
  display: grid;
  gap: 12px;
}

.visual-print-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #d8e0ea;
}

.visual-print-summary strong,
.visual-print-summary span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visual-print-summary strong {
  color: #0f172a;
}

.visual-print-summary span {
  color: #075985;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 12px;
  font-weight: 800;
}

.visual-print-form {
  display: grid;
  align-items: center;
  gap: 12px;
  grid-template-columns: 132px minmax(0, 1fr);
}

.visual-print-form label {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  color: #334155;
  font-size: 13px;
  font-weight: 900;
  text-align: right;
}

.visual-print-form label em {
  color: #64748b;
  font-family: Consolas, 'Microsoft YaHei', monospace;
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
}

.visual-print-hint {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
}

.visual-template-picker {
  display: grid;
  gap: 8px;
}

.visual-template-picker-title {
  color: #334155;
  font-size: 13px;
  font-weight: 900;
}

.visual-template-picker-toolbar {
  display: grid;
  align-items: center;
  gap: 10px;
  grid-template-columns: minmax(0, 1fr) max-content;
}

.visual-template-picker-toolbar span {
  color: #475569;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.visual-template-picker-table {
  overflow: hidden;
  border: 1px solid #d8e0ea;
}

.visual-template-image-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 54px;
}

.visual-template-image {
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e0ea;
}

.visual-template-image :deep(img) {
  object-fit: contain;
}

:deep(.shipping-dev-process-form-view-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  padding-bottom: 0;
  margin: 0;
}

:deep(.shipping-dev-process-form-view-modal .ant-modal-content) {
  height: 100vh;
  padding: 0;
  overflow: hidden;
  background: #f8fafc;
  border-radius: 0;
}

:deep(.shipping-dev-process-form-view-modal .ant-modal-body) {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.shipping-dev-runtime-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  color: #0f172a;
  background: #eef2f7;
}

.shipping-dev-runtime-view__header {
  display: flex;
  flex: 0 0 auto;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 10px 16px;
  background: #0f172a;
  border-bottom: 1px solid #1e293b;
}

.shipping-dev-runtime-view__header h3 {
  margin: 0;
  color: #f8fafc;
  font-size: 18px;
  font-weight: 900;
}

.shipping-dev-runtime-view__meta,
.shipping-dev-runtime-view__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.shipping-dev-runtime-view__meta {
  flex-wrap: wrap;
  margin-top: 4px;
  color: #cbd5e1;
  font-size: 12px;
}

.shipping-dev-runtime-view__actions {
  flex: 0 0 auto;
}

.shipping-dev-runtime-view__body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  background: #f8fafc;
}

.shipping-dev-runtime-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 240px;
  color: #64748b;
  font-weight: 800;
}

@media (max-width: 1200px) {
  .shipping-section-form-head {
    grid-template-columns: 110px minmax(0, 1fr) 110px minmax(0, 1fr);
  }

  .shipping-form-control--span-2 {
    grid-column: span 3;
  }

  .shipping-form-control--full-row {
    grid-column: span 3;
  }
}

@media (max-width: 900px) {
  .shipping-daily-check-card-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .shipping-check-query-bar {
    grid-template-columns: minmax(0, 1fr) max-content;
  }

  .shipping-check-query-bar > :last-child {
    grid-column: 1 / -1;
  }

  .shipping-notice-form-title-row {
    align-items: stretch;
    display: flex;
    flex-direction: column;
  }

  .shipping-notice-form-actions {
    justify-content: flex-start;
  }

  .package-group-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

<style>
.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: none;
  height: 100vh;
  margin: 0 !important;
  padding-bottom: 0;
}

.hc-pass-work-modal .ant-modal-header,
.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.hc-pass-work-modal .ant-modal-body {
  display: flex;
  width: 100vw !important;
  height: 100vh !important;
  flex-direction: column;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.hc-pass-work-modal .ant-modal-content {
  width: 100vw !important;
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
  box-shadow: none;
}

.rough-report-work-modal .report-modal-body {
  box-sizing: border-box;
  flex: 1 1 auto;
  width: 100vw;
  height: auto;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    #f5f7fa;
}

.outbound-work-modal .ant-modal {
  top: 12px;
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px);
  margin: 0 12px !important;
}

.outbound-work-modal .ant-modal-body {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
}

.outbound-work-modal .ant-modal-content {
  width: calc(100vw - 24px) !important;
  height: calc(100vh - 24px) !important;
  border: 1px solid #9fb6cd;
}

.outbound-work-modal .report-modal-body {
  width: calc(100vw - 24px);
  height: calc(100vh - 24px);
}
</style>
