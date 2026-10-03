<script lang="ts" setup>
import type { UploadProps } from 'ant-design-vue';
import type { CSSProperties } from 'vue';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import type { MesHcVisualPrintDesignerApi } from '#/api/mes/hc/visualprintdesigner';

import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';
import { useAppConfig } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  Empty,
  Form,
  FormItem,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Switch,
  Table as ATable,
  Tabs,
  Tag,
  Tooltip,
  Upload,
  message,
} from 'ant-design-vue';

import { useVbenVxeGrid } from '#/adapter/vxe-table';

import {
  attachVisualPrintDesign,
  createVisualPrintCustomerInfo,
  deleteVisualPrintCustomerInfo,
  getVisualPrintCustomerInfoDetail,
  getVisualPrintCustomerInfoPage,
  getVisualPrintDesign,
  getVisualPrintDesigns,
  getVisualPrintFieldOptions,
  importVisualPrintCustomerInfoExcel,
  saveVisualPrintDesign,
  updateVisualPrintCustomerInfo,
} from '#/api/mes/hc/visualprintdesigner';
import { buildPrintAgentLatestDownloadPath } from '#/api/mes/hc/printagentpackage';

import {
  type VisualLabelKind,
  visualLabelKinds,
  visualPrintImageMetaMap,
  visualPrintSampleData,
  resolveVisualPrintImageUrl,
} from './mock-data';

defineOptions({ name: 'MesHcExecutionVisualPrintDesigner' });

type CustomerInfo = MesHcVisualPrintDesignerApi.CustomerInfo;
type Design = MesHcVisualPrintDesignerApi.Design;
type FieldOption = MesHcVisualPrintDesignerApi.FieldOption;
type LabelKind = MesHcVisualPrintDesignerApi.LabelKind;
type ProductItem = MesHcVisualPrintDesignerApi.ProductItem;

type DesignerElementType = 'barcode' | 'field' | 'image' | 'line' | 'qrcode' | 'rect' | 'text';
type CanvasViewMode = 'design' | 'final';
type PrintRotation = 0 | 90 | 180 | 270;
type RotationDirection = 'clockwise' | 'counterclockwise';
type TextAlign = 'center' | 'left' | 'right';
type TextVerticalAlign = 'bottom' | 'middle' | 'top';

interface DesignerElement {
  align?: TextAlign;
  bold?: boolean;
  enabled?: boolean;
  fieldKey?: string;
  fill?: string;
  fit?: boolean;
  fontFamily?: 'bold' | 'heavy' | 'latin_bold' | 'regular';
  fontSize?: number;
  id: string;
  height: number;
  humanFontSize?: number;
  humanReadable?: boolean;
  guideOnly?: boolean;
  importedFromPrn?: boolean;
  lineSpacing?: number;
  minFontSize?: number;
  name: string;
  quietMm?: number;
  prnObjectType?: string;
  sourceHeightDot?: number;
  sourceWidthDot?: number;
  sourceXDot?: number;
  sourceYDot?: number;
  stroke?: string;
  strokeWidth?: number;
  text?: string;
  type: DesignerElementType;
  valign?: TextVerticalAlign;
  width: number;
  x: number;
  y: number;
}

interface DesignerDraft {
  background: string;
  contentOffsetXmm: number;
  contentOffsetYmm: number;
  id: string;
  persistedId?: number;
  forceNew?: boolean;
  customer: string;
  designHeightMm: number;
  designWidthMm: number;
  dpi: number;
  elements: DesignerElement[];
  heightMm: number;
  imageFile?: string;
  imageHeightPx?: number;
  imageId?: string;
  imageWidthPx?: number;
  labelKind: LabelKind;
  labelName: string;
  preservePageSizeAfterRotation: boolean;
  printRotation: PrintRotation;
  prnSourceDpi?: number;
  prnSourceFileName?: string;
  referenceImageDataUrl?: string;
  referenceImageName?: string;
  rendererTemplate?: Record<string, unknown>;
  rotatedContentOriginXmm: number;
  rotatedContentOriginYmm: number;
  rotationDirection: RotationDirection;
  scaleElementsWithDesignSize: boolean;
  schemaVersion: number;
  sourceRow?: number;
  threshold: number;
  updatedAt: string;
  widthMm: number;
}

interface LabelVariant {
  imageFile?: string;
  imageHeightPx?: number;
  imageId?: string;
  imageUrl?: string;
  imageWidthPx?: number;
  labelKind: LabelKind;
  labelName: string;
  sourceRow?: number;
}

interface DragState {
  elementId: string;
  startClientX: number;
  startClientY: number;
  startX: number;
  startY: number;
}

interface LocalPrinter {
  dpi?: number;
  dpiSource?: string;
  isDefault?: boolean;
  name: string;
}

type FieldElementType = Extract<DesignerElementType, 'barcode' | 'field'>;

interface DataFieldOption {
  label: string;
  value: string;
}

interface FieldDragPayload {
  elementType: FieldElementType;
  fieldKey: string;
  fieldLabel: string;
}

type MvpJsonObject = Record<string, any>;

const Textarea = Input.TextArea;

const PRINT_AGENT_URL = 'http://127.0.0.1:18081';
const DRAFT_SCHEMA_VERSION = 7;
const FILE_PRINT_TARGET = '__file__';
const PRINT_AGENT_PACKAGE_CODE = 'HC_MES_PRINT_AGENT';
const DEFAULT_REFERENCE_OPACITY = 0.22;
const FIELD_DRAG_MIME = 'application/x-hc-visual-print-field';
const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

const DEFAULT_MVP_PAPER_BY_KIND: Partial<
  Record<LabelKind, {
    designHeightMm: number;
    designWidthMm: number;
    mediaHeightMm: number;
    mediaWidthMm: number;
    presetId: string;
  }>
> = {
  boxFront: {
    designHeightMm: 165,
    designWidthMm: 180,
    mediaHeightMm: 180,
    mediaWidthMm: 165,
    presetId: 'outer_180x165',
  },
  cleanBag: {
    designHeightMm: 100,
    designWidthMm: 120,
    mediaHeightMm: 120,
    mediaWidthMm: 100,
    presetId: 'inner_120x100',
  },
  padBack: {
    designHeightMm: 100,
    designWidthMm: 120,
    mediaHeightMm: 120,
    mediaWidthMm: 100,
    presetId: 'inner_120x100',
  },
};

const canvasViewModeOptions = [
  { label: 'Web设计层', value: 'design' },
  { label: '最终打印方向', value: 'final' },
] as const;

const elementTypeOptions = [
  { icon: 'lucide:type', label: '文本', value: 'text' },
  { icon: 'lucide:braces', label: '字段', value: 'field' },
  { icon: 'lucide:barcode', label: '一维码', value: 'barcode' },
  { icon: 'lucide:minus', label: '线条', value: 'line' },
  { icon: 'lucide:square', label: '矩形', value: 'rect' },
] as const;

const printRotationOptions = [
  { label: '0度', value: 0 },
  { label: '90度', value: 90 },
  { label: '180度', value: 180 },
  { label: '270度', value: 270 },
] as const;

const fontFamilyOptions = [
  { label: '系统常规字体', value: 'regular' },
  { label: '系统粗体', value: 'bold' },
  { label: '系统重体', value: 'heavy' },
  { label: '拉丁粗体', value: 'latin_bold' },
] as const;

const textAlignOptions = [
  { label: '左对齐', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '右对齐', value: 'right' },
] as const;

const textVerticalAlignOptions = [
  { label: '顶部', value: 'top' },
  { label: '垂直居中', value: 'middle' },
  { label: '底部', value: 'bottom' },
] as const;

const rotationDirectionOptions = [
  { label: '顺时针', value: 'clockwise' },
  { label: '逆时针', value: 'counterclockwise' },
] as const;

const customerFormItems: Array<{ key: keyof CustomerInfo; label: string; span?: 'full' }> = [
  { key: 'serialNo', label: '序号' },
  { key: 'customer', label: '客户' },
  { key: 'customerSideSize', label: '客户侧标尺寸' },
  { key: 'customerSideMethod', label: '客户侧标方式', span: 'full' },
  { key: 'shippingMethod', label: '发货方式', span: 'full' },
  { key: 'needPaperCoa', label: '是否需要随货纸版COA' },
  { key: 'needEcoa', label: '是否需要ECOA' },
  { key: 'hasMark', label: '是否有唛头' },
  { key: 'deliveryNote', label: '送货单', span: 'full' },
  { key: 'shipmentFilePackageMethod', label: '随货文件包装方式', span: 'full' },
  { key: 'customerSideTemplate', label: '客户侧标模板', span: 'full' },
  { key: 'specialRemark', label: '特殊备注', span: 'full' },
];

const customerProductColumns = [
  { dataIndex: 'productType', title: '产品类型', width: 180 },
  { dataIndex: 'sizeMm', title: '尺寸/mm', width: 140 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 72 },
];

const attachProductColumns = [
  { dataIndex: 'productType', title: '产品类型', width: 180 },
  { dataIndex: 'sizeMm', title: '尺寸/mm', width: 140 },
];

const elementColumns = [
  { dataIndex: 'name', ellipsis: true, title: '元素', width: 150 },
  { dataIndex: 'type', title: '类型', width: 76 },
  { align: 'center', dataIndex: 'actions', title: '操作', width: 82 },
];

const customerGridColumns: VxeTableGridOptions<CustomerInfo>['columns'] = [
  { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 72 },
  { field: 'customer', minWidth: 190, showOverflow: 'tooltip', title: '客户' },
  { align: 'center', field: 'productType', slots: { default: 'productType' }, title: '产品类型', width: 96 },
  { align: 'center', field: 'sizeMm', slots: { default: 'sizeMm' }, title: '尺寸', width: 90 },
  { align: 'center', field: 'customerSideSize', slots: { default: 'customerSideSize' }, title: '侧标尺寸', width: 100 },
  { align: 'center', field: 'padBack', slots: { default: 'padBackPreview' }, title: 'Pad背标', width: 154 },
  { align: 'center', field: 'cleanBag', slots: { default: 'cleanBagPreview' }, title: '洁净袋', width: 154 },
  { align: 'center', field: 'boxFront', slots: { default: 'boxFrontPreview' }, title: '盒正标', width: 154 },
  { align: 'center', field: 'customerSide', slots: { default: 'customerSidePreview' }, title: '客户侧标', width: 154 },
  { field: 'customerSideMethod', minWidth: 170, showOverflow: 'tooltip', title: '侧标方式' },
  { field: 'shippingMethod', minWidth: 170, showOverflow: 'tooltip', title: '发货方式' },
  { align: 'center', field: 'coa', slots: { default: 'coa' }, title: 'COA', width: 110 },
  { field: 'specialRemark', minWidth: 190, showOverflow: 'tooltip', title: '备注' },
  { align: 'center', field: 'action', fixed: 'right', slots: { default: 'customerActions' }, title: '操作', width: 76 },
];

const queryForm = reactive({
  customer: '',
  labelKind: undefined as LabelKind | undefined,
  productType: '',
  sizeMm: '',
});

const customerLoading = ref(false);
const importLoading = ref(false);
const customerModalVisible = ref(false);
const customerSaving = ref(false);
const editingCustomerId = ref<number>();
const customerPageNo = ref(1);
const customerPageSize = ref(12);
const customerTotal = ref(0);
const customerForm = reactive<CustomerInfo>(buildEmptyCustomerForm());
const customerModalActiveKey = ref('base');

const fieldOptions = ref<FieldOption[]>([]);
const designerVisible = ref(false);
const designerLoading = ref(false);
const designerLeftActiveKey = ref('elements');
const selectedRow = ref<CustomerInfo>();
const selectedKind = ref<LabelKind>('padBack');
const currentDraft = ref<DesignerDraft>();
const designerTemplateRows = ref<Design[]>([]);
const designerTemplateLoading = ref(false);
const currentTemplateSharedCount = ref(0);
const currentTemplateLinkedItems = ref<MesHcVisualPrintDesignerApi.ProductItem[]>([]);
const selectedElementId = ref('');
const elementListPanelRef = ref<HTMLElement>();
const elementListScrollY = ref(240);
const attachModalVisible = ref(false);
const attachLoading = ref(false);
const attachProductItems = ref<ProductItem[]>([]);
const attachSelectedProductItemIds = ref<number[]>([]);
const showReference = ref(true);
const referenceOpacity = ref(DEFAULT_REFERENCE_OPACITY);
const agentLoading = ref(false);
const agentPreviewImageUrl = ref('');
const agentPreviewVisible = ref(false);
const canvasRenderError = ref('');
const canvasRenderImageUrl = ref('');
const canvasRenderLoading = ref(false);
const canvasViewMode = ref<CanvasViewMode>('design');
const pythonCanvasEnabled = ref(false);
const dragState = ref<DragState>();
const localPrinters = ref<LocalPrinter[]>([]);
const printerLoading = ref(false);
const selectedPrintTarget = ref(FILE_PRINT_TARGET);
const printAgentStatus = ref<'checking' | 'offline' | 'online'>('checking');
const printAgentVersion = ref('');
const printAgentError = ref('');
const importedMvpConfig = ref<MvpJsonObject>();
const selectedMvpPaperPresetId = ref('');
const prnImportDpi = ref(203);
const prnImportLoading = ref(false);
let elementListResizeObserver: ResizeObserver | undefined;
let canvasRenderRequestId = 0;
let canvasRenderTimer: number | undefined;

const labelKindOptions = computed(() =>
  visualLabelKinds.map((item) => ({
    label: item.label,
    value: item.kind as LabelKind,
  })),
);

const mvpPaperPresetOptions = computed(() => {
  const config = importedMvpConfig.value;
  const presets = Array.isArray(config?.paper_presets) ? config.paper_presets : [];
  if (presets.length) {
    return presets.map((item: MvpJsonObject, index: number) => ({
      label: String(item.label || item.id || `规格${index + 1}`),
      value: String(item.id || `preset-${index + 1}`),
    }));
  }
  return config
    ? [{
        label: `${config.page?.width_mm || 100}×${config.page?.height_mm || 70}mm`,
        value: 'default',
      }]
    : [];
});

const selectedKindMeta = computed(
  () => visualLabelKinds.find((item) => item.kind === selectedKind.value) || visualLabelKinds[0],
);

const selectedVariant = computed(() =>
  selectedRow.value ? buildLabelVariant(selectedRow.value, selectedKind.value) : undefined,
);

const selectedVariantImageSize = computed(() => {
  const draft = currentDraft.value;
  if (draft?.imageWidthPx && draft.imageHeightPx) return `${draft.imageWidthPx} * ${draft.imageHeightPx}px`;
  return '';
});

const selectedReferenceImageUrl = computed(() => {
  return currentDraft.value?.referenceImageDataUrl || selectedVariant.value?.imageUrl || '';
});

const selectedReferenceImageName = computed(() => {
  return currentDraft.value?.referenceImageName || currentDraft.value?.imageFile || selectedVariant.value?.imageFile || '';
});

const selectedElement = computed(() =>
  currentDraft.value?.elements.find((item) => item.id === selectedElementId.value),
);

const templateBindingCount = computed(() => {
  if (!currentDraft.value?.persistedId) return 0;
  return Math.max(1, currentTemplateSharedCount.value || 0);
});

const templateStatusText = computed(() => {
  if (!currentDraft.value?.persistedId) return '模板：未保存';
  if (templateBindingCount.value > 1) return `模板：共用 ${templateBindingCount.value} 条配置`;
  return '模板：独立 1 条配置';
});

const templateStatusColor = computed(() => {
  if (!currentDraft.value?.persistedId) return 'orange';
  return templateBindingCount.value > 1 ? 'blue' : 'green';
});

const templateLinkedProductLabels = computed(() =>
  currentTemplateLinkedItems.value.map(formatProductModelSize).filter(Boolean),
);

const templateLinkedProductTitle = computed(() => templateLinkedProductLabels.value.join('、'));

const designerTemplateList = computed(() =>
  designerTemplateRows.value.filter((item) => item.labelKind === selectedKind.value),
);

const attachRowSelection = computed(() => ({
  onChange: (keys: Array<number | string>) => {
    attachSelectedProductItemIds.value = keys.map((key) => Number(key)).filter(Boolean);
  },
  selectedRowKeys: attachSelectedProductItemIds.value,
}));

const canvasWidthMm = computed(() => {
  const draft = currentDraft.value;
  if (!draft) return 1;
  if (canvasViewMode.value === 'design') {
    return Number(draft.designWidthMm || draft.widthMm || 1);
  }
  if (
    draft.preservePageSizeAfterRotation === false
    && [90, 270].includes(normalizePrintRotation(draft.printRotation))
  ) {
    return Number(draft.designHeightMm || draft.heightMm || 1);
  }
  return Number(draft.widthMm || 1);
});

const canvasHeightMm = computed(() => {
  const draft = currentDraft.value;
  if (!draft) return 1;
  if (canvasViewMode.value === 'design') {
    return Number(draft.designHeightMm || draft.heightMm || 1);
  }
  if (
    draft.preservePageSizeAfterRotation === false
    && [90, 270].includes(normalizePrintRotation(draft.printRotation))
  ) {
    return Number(draft.designWidthMm || draft.widthMm || 1);
  }
  return Number(draft.heightMm || 1);
});

const canvasScale = computed(() => {
  const draft = currentDraft.value;
  if (!draft) return 1;
  return Math.min(6.2, 760 / Math.max(1, canvasWidthMm.value), 430 / Math.max(1, canvasHeightMm.value));
});

const canvasStyle = computed<CSSProperties>(() => {
  if (!currentDraft.value) return {};
  return {
    height: `${canvasHeightMm.value * canvasScale.value}px`,
    width: `${canvasWidthMm.value * canvasScale.value}px`,
  };
});

const activePrintData = computed<Record<string, string>>(() => {
  const row = selectedRow.value;
  return {
    ...visualPrintSampleData,
    serialNo: formatText(row?.serialNo),
    customer: formatText(row?.customer),
    productType: formatText(row?.productType),
    sizeMm: formatText(row?.sizeMm),
    customerSideSize: formatText(row?.customerSideSize),
    customerSideMethod: formatText(row?.customerSideMethod),
    shippingMethod: formatText(row?.shippingMethod),
    needPaperCoa: formatText(row?.needPaperCoa),
    needEcoa: formatText(row?.needEcoa),
    hasMark: formatText(row?.hasMark),
    deliveryNote: formatText(row?.deliveryNote),
    shipmentFilePackageMethod: formatText(row?.shipmentFilePackageMethod),
    customerSideTemplate: formatText(row?.customerSideTemplate),
    specialRemark: formatText(row?.specialRemark),
    productModel: formatText(row?.productType) || visualPrintSampleData.productModel,
    productTypeName: visualPrintSampleData.productTypeName,
    productInfo: row?.productType ? `${row.productType}, ${formatText(row.sizeMm)}mm` : visualPrintSampleData.productInfo,
    materialDescription: row?.productType
      ? `${row.productType}${row.sizeMm ? `,${row.sizeMm}mm` : ''}`
      : visualPrintSampleData.materialDescription,
  };
});

const dataFieldOptions = computed<DataFieldOption[]>(() =>
  fieldOptions.value.map((item) => ({
    label: item.excelColumn ? `${item.excelColumn} ${item.fieldLabel}` : item.fieldLabel,
    value: item.fieldKey,
  })),
);

const printTargetOptions = computed(() => [
  { label: '生成预览文件（PNG）', value: FILE_PRINT_TARGET },
  ...localPrinters.value.map((printer) => ({
    label: `${printer.isDefault ? `${printer.name}（默认）` : printer.name}${
      isPdfPrinterName(printer.name) ? '（不支持位图RAW）' : ''
    }${printer.dpi ? ` / ${printer.dpi}dpi` : ''}`,
    disabled: isPdfPrinterName(printer.name),
    value: printer.name,
  })),
]);

const selectedPrinterName = computed(() =>
  selectedPrintTarget.value === FILE_PRINT_TARGET ? '' : selectedPrintTarget.value,
);

const selectedPrinterProfile = computed(() =>
  localPrinters.value.find((item) => item.name === selectedPrinterName.value),
);

const selectedOutputMode = computed(() => (selectedPrinterName.value ? 'windows_raw' : 'file'));

const selectedPrintTargetLabel = computed(() => {
  const option = printTargetOptions.value.find((item) => item.value === selectedPrintTarget.value);
  return option?.label || '生成预览文件（PNG）';
});

const printGeometry = computed(() => {
  const draft = currentDraft.value;
  if (!draft) return undefined;
  const rotation = normalizePrintRotation(draft.printRotation);
  const rotatedWidthMm = [90, 270].includes(rotation) ? draft.designHeightMm : draft.designWidthMm;
  const rotatedHeightMm = [90, 270].includes(rotation) ? draft.designWidthMm : draft.designHeightMm;
  const warnings: string[] = [];
  const tolerance = 0.05;
  if (draft.preservePageSizeAfterRotation) {
    if (draft.rotatedContentOriginXmm < -tolerance || draft.rotatedContentOriginYmm < -tolerance) {
      warnings.push('旋转原点为负数，会裁切左侧或顶部内容');
    }
    if (draft.rotatedContentOriginXmm + rotatedWidthMm > draft.widthMm + tolerance) {
      warnings.push('旋转后的内容宽度超出物理介质');
    }
    if (draft.rotatedContentOriginYmm + rotatedHeightMm > draft.heightMm + tolerance) {
      warnings.push('旋转后的内容高度超出物理介质');
    }
  }
  return {
    outputHeightMm: draft.preservePageSizeAfterRotation ? draft.heightMm : rotatedHeightMm,
    outputWidthMm: draft.preservePageSizeAfterRotation ? draft.widthMm : rotatedWidthMm,
    rotatedHeightMm,
    rotatedWidthMm,
    warnings,
  };
});

const agentPreviewTitle = computed(() =>
  currentDraft.value ? `本地渲染结果 / ${currentDraft.value.labelName}` : '本地渲染结果',
);

const printAgentDownloadUrl = computed(() =>
  buildFullApiUrl(buildPrintAgentLatestDownloadPath(PRINT_AGENT_PACKAGE_CODE)),
);

const customerGridHeight = Math.max(
  520,
  typeof window === 'undefined' ? 640 : window.innerHeight - 238,
);

const [CustomerGrid, customerGridApi] = useVbenVxeGrid({
  gridOptions: {
    border: true,
    columns: customerGridColumns,
    height: customerGridHeight,
    keepSource: true,
    pagerConfig: {
      pageSize: 12,
      pageSizes: [8, 12, 20, 40],
    },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          customerLoading.value = true;
          customerPageNo.value = page.currentPage;
          customerPageSize.value = page.pageSize;
          try {
            const result = await getVisualPrintCustomerInfoPage({
              customer: queryForm.customer || undefined,
              labelKind: queryForm.labelKind,
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              productType: queryForm.productType || undefined,
              sizeMm: queryForm.sizeMm || undefined,
              status: 0,
            });
            customerTotal.value = result.total || 0;
            return {
              ...result,
              list: normalizeCustomerRows(result.list || []).sort(compareCustomerRow),
            };
          } finally {
            customerLoading.value = false;
          }
        },
      },
    },
    rowConfig: {
      isHover: true,
      keyField: 'rowKey',
    },
    seqConfig: {
      seqMethod: ({ rowIndex }) => (customerPageNo.value - 1) * customerPageSize.value + rowIndex + 1,
    },
    showOverflow: 'tooltip',
    toolbarConfig: {
      enabled: false,
    },
  } as VxeTableGridOptions<CustomerInfo>,
});

const excelUploadProps: UploadProps = {
  accept: '.xlsx,.xls',
  beforeUpload: async (file) => {
    await handleExcelImport(file as File);
    return false;
  },
  maxCount: 1,
  showUploadList: false,
};

const referenceUploadProps: UploadProps = {
  accept: 'image/png,image/jpeg,image/webp',
  beforeUpload: async (file) => {
    await handleReferenceImageUpload(file as File);
    return false;
  },
  maxCount: 1,
  showUploadList: false,
};

function bindElementListResizeObserver() {
  if (!elementListPanelRef.value || !elementListResizeObserver) return;
  elementListResizeObserver.disconnect();
  elementListResizeObserver.observe(elementListPanelRef.value);
}

function updateElementListScrollHeight() {
  if (typeof window === 'undefined') return;
  window.requestAnimationFrame(() => {
    const panel = elementListPanelRef.value;
    if (!panel) return;

    const title = panel.querySelector<HTMLElement>('.panel-title');
    const tableHeader = panel.querySelector<HTMLElement>('.ant-table-thead');
    const titleHeight = title?.offsetHeight || 38;
    const headerHeight = tableHeader?.offsetHeight || 39;
    const availableHeight = panel.clientHeight - titleHeight - headerHeight - 10;
    elementListScrollY.value = Math.max(140, Math.floor(availableHeight));
  });
}

async function refreshElementListLayout() {
  await nextTick();
  bindElementListResizeObserver();
  updateElementListScrollHeight();
}

async function scrollSelectedElementRowIntoView() {
  const selectedId = selectedElementId.value;
  if (!selectedId || typeof window === 'undefined') return;

  await nextTick();
  window.requestAnimationFrame(() => {
    if (selectedElementId.value !== selectedId) return;
    const panel = elementListPanelRef.value;
    if (!panel) return;

    const tableBody = panel.querySelector<HTMLElement>('.ant-table-body');
    const rows = Array.from(panel.querySelectorAll<HTMLElement>('.ant-table-tbody tr[data-row-key]'));
    const selectedRow = rows.find((row) => row.getAttribute('data-row-key') === selectedId);
    if (!tableBody || !selectedRow) return;

    const bodyRect = tableBody.getBoundingClientRect();
    const rowRect = selectedRow.getBoundingClientRect();
    const topOffset = rowRect.top - bodyRect.top;
    const bottomOffset = rowRect.bottom - bodyRect.bottom;
    if (topOffset < 0) {
      tableBody.scrollTop += topOffset - 8;
    } else if (bottomOffset > 0) {
      tableBody.scrollTop += bottomOffset + 8;
    }
  });
}

onMounted(async () => {
  if (typeof ResizeObserver !== 'undefined') {
    elementListResizeObserver = new ResizeObserver(updateElementListScrollHeight);
  }
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', updateElementListScrollHeight);
  }
  await loadFieldOptions();
  await refreshElementListLayout();
  void checkPrintAgentHealth(true);
});

onBeforeUnmount(() => {
  elementListResizeObserver?.disconnect();
  if (canvasRenderTimer !== undefined) {
    window.clearTimeout(canvasRenderTimer);
  }
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateElementListScrollHeight);
  }
  stopDragElement();
  revokeCanvasRenderImageUrl();
  revokeAgentPreviewImageUrl();
});

watch(
  () => [designerVisible.value, selectedElementId.value, currentDraft.value?.elements.length || 0],
  () => {
    void refreshElementListLayout();
    void scrollSelectedElementRowIntoView();
  },
  { flush: 'post' },
);

watch(
  [
    () => currentDraft.value,
    () => activePrintData.value,
    () => canvasViewMode.value,
    () => designerVisible.value,
  ],
  () => scheduleCanvasRender(),
  { deep: true, flush: 'post' },
);

function buildEmptyCustomerForm(): CustomerInfo {
  return {
    productItems: [buildEmptyProductItem()],
    status: 0,
  };
}

function buildEmptyProductItem(): ProductItem {
  return {
    status: 0,
  };
}

function buildProductItemFromRow(row: CustomerInfo): ProductItem {
  return {
    boxFrontLabelImageFile: row.boxFrontLabelImageFile,
    boxFrontLabelImageId: row.boxFrontLabelImageId,
    cleanBagLabelImageFile: row.cleanBagLabelImageFile,
    cleanBagLabelImageId: row.cleanBagLabelImageId,
    customerInfoId: row.id,
    customerSideLabelImageFile: row.customerSideLabelImageFile,
    customerSideLabelImageId: row.customerSideLabelImageId,
    id: row.productItemId,
    padBackLabelImageFile: row.padBackLabelImageFile,
    padBackLabelImageId: row.padBackLabelImageId,
    productType: row.productType,
    sizeMm: row.sizeMm,
    sourceRow: row.sourceRow,
    status: row.status ?? 0,
  };
}

function normalizeProductItemRows(items?: ProductItem[]) {
  const rows = items?.length ? items : [buildEmptyProductItem()];
  return rows.map((item) => ({
    ...item,
    status: item.status ?? 0,
  }));
}

function getCustomerProductRowKey(record: ProductItem, index?: number) {
  return record.id ? `item-${record.id}` : `new-${index || 0}`;
}

function toSortableNumber(value: unknown) {
  const text = String(value ?? '').trim();
  if (!text) return Number.MAX_SAFE_INTEGER;
  const matched = text.match(/\d+/);
  return matched ? Number.parseInt(matched[0], 10) : Number.MAX_SAFE_INTEGER;
}

function compareCustomerRow(left: CustomerInfo, right: CustomerInfo) {
  return (
    toSortableNumber(left.serialNo) - toSortableNumber(right.serialNo) ||
    toSortableNumber(left.sourceRow) - toSortableNumber(right.sourceRow) ||
    Number(left.productItemId || 0) - Number(right.productItemId || 0) ||
    Number(left.id || 0) - Number(right.id || 0)
  );
}

function normalizeCustomerRows(rows: CustomerInfo[]) {
  return rows.map((row, index) => ({
    ...row,
    rowKey: row.productItemId ? `item-${row.productItemId}` : `customer-${row.id || row.sourceRow || index}`,
  }));
}

async function loadFieldOptions() {
  try {
    fieldOptions.value = await getVisualPrintFieldOptions();
  } catch {
    fieldOptions.value = [
      { fieldKey: 'customer', fieldLabel: '客户' },
      { fieldKey: 'productType', fieldLabel: '产品类型' },
      { fieldKey: 'sizeMm', fieldLabel: '尺寸/mm' },
      { fieldKey: 'batchNo', fieldLabel: '生产批号 Batch No.' },
      { fieldKey: 'productNo', fieldLabel: '产品编码 Product No.' },
    ];
  }
}

function handleSearch() {
  customerGridApi.query();
}

function handleReset() {
  queryForm.customer = '';
  queryForm.productType = '';
  queryForm.sizeMm = '';
  queryForm.labelKind = undefined;
  handleSearch();
}

async function handleExcelImport(file: File) {
  importLoading.value = true;
  try {
    const result = await importVisualPrintCustomerInfoExcel(file);
    message.success(
      `导入完成：新建 ${result.createCount || 0} 行，更新 ${result.updateCount || 0} 行，跳过 ${
        result.skipCount || 0
      } 行，模板绑定已同步`,
    );
    if (result.messages?.length) {
      console.info('[visual-print-designer] import messages', result.messages);
    }
    customerGridApi.query();
  } finally {
    importLoading.value = false;
  }
}

function openCreateCustomer() {
  editingCustomerId.value = undefined;
  Object.assign(customerForm, buildEmptyCustomerForm());
  customerModalActiveKey.value = 'base';
  customerModalVisible.value = true;
}

async function openEditCustomer(row: CustomerInfo) {
  if (!row.id) return;
  editingCustomerId.value = row.id;
  customerSaving.value = true;
  try {
    const detail = await getVisualPrintCustomerInfoDetail(row.id);
    Object.assign(customerForm, buildEmptyCustomerForm(), detail, {
      productItems: normalizeProductItemRows(detail.productItems?.length ? detail.productItems : [buildProductItemFromRow(row)]),
    });
    customerModalActiveKey.value = 'base';
    customerModalVisible.value = true;
  } finally {
    customerSaving.value = false;
  }
}

async function saveCustomerInfo() {
  if (!formatText(customerForm.customer)) {
    message.warning('请填写客户');
    return;
  }
  const productItems = normalizeProductItemRows(customerForm.productItems);
  if (!productItems.length) {
    message.warning('请至少维护一条产品型号尺寸');
    return;
  }
  for (const [index, item] of productItems.entries()) {
    if (!formatText(item.productType) || !formatText(item.sizeMm)) {
      message.warning(`第 ${index + 1} 条产品型号尺寸不完整`);
      return;
    }
  }
  customerForm.productItems = productItems;
  const firstItem = productItems[0];
  const payload = {
    ...customerForm,
    productItemId: firstItem?.id,
    productItems,
    productType: firstItem?.productType,
    sizeMm: firstItem?.sizeMm,
  };
  customerSaving.value = true;
  try {
    if (editingCustomerId.value) {
      await updateVisualPrintCustomerInfo({ ...payload, id: editingCustomerId.value });
      message.success('客户打印信息已更新');
    } else {
      await createVisualPrintCustomerInfo(payload);
      message.success('客户打印信息已创建');
    }
    customerModalVisible.value = false;
    customerGridApi.query();
  } finally {
    customerSaving.value = false;
  }
}

function addCustomerProductItem() {
  customerForm.productItems = [...normalizeProductItemRows(customerForm.productItems), buildEmptyProductItem()];
}

function removeCustomerProductItem(index: number) {
  const rows = normalizeProductItemRows(customerForm.productItems);
  if (rows.length <= 1) {
    message.warning('至少保留一条产品型号尺寸');
    return;
  }
  rows.splice(index, 1);
  customerForm.productItems = rows;
}

async function handleDeleteCustomer(row: CustomerInfo) {
  if (!row.id) return;
  await deleteVisualPrintCustomerInfo(row.id, row.productItemId);
  message.success('客户打印信息已删除');
  customerGridApi.query();
}

function confirmDeleteCustomer(row: CustomerInfo) {
  Modal.confirm({
    cancelText: '取消',
    okButtonProps: { danger: true },
    okText: '删除',
    onOk: () => handleDeleteCustomer(row),
    title: '确认删除该客户打印信息及设计稿？',
  });
}

async function loadDesignerTemplateRows(row: CustomerInfo, kind: LabelKind) {
  if (!row.id) {
    designerTemplateRows.value = [];
    return;
  }
  designerTemplateLoading.value = true;
  try {
    const [sharedRows, boundRows] = await Promise.all([
      getVisualPrintDesigns(row.id),
      row.productItemId ? getVisualPrintDesigns(row.id, row.productItemId) : Promise.resolve([] as Design[]),
    ]);
    const rowMap = new Map<number, Design>();
    for (const item of [...(sharedRows || []), ...(boundRows || [])]) {
      if (!item?.id || item.labelKind !== kind) continue;
      if (!isDesignerTemplateMatched(row, kind, item)) continue;
      rowMap.set(item.id, item);
    }
    designerTemplateRows.value = Array.from(rowMap.values()).sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
  } finally {
    designerTemplateLoading.value = false;
  }
}

function isDesignerTemplateMatched(row: CustomerInfo, kind: LabelKind, design: Design) {
  if (design.productItemId && design.productItemId === row.productItemId) {
    return true;
  }
  const targetImageId = formatText(getLabelImageId(row, kind));
  const targetImageFile = formatText(getLabelImageFile(row, kind));
  const designImageId = formatText(design.imageId);
  const designImageFile = formatText(design.imageFile);
  if (targetImageId || targetImageFile) {
    return (!!targetImageId && targetImageId === designImageId) || (!!targetImageFile && targetImageFile === designImageFile);
  }
  return !designImageId && !designImageFile;
}

function applyDesignerTemplate(row: CustomerInfo, kind: LabelKind, design?: Design) {
  const variant = buildLabelVariant(row, kind);
  currentDraft.value = design?.designJson
    ? parsePersistedDraft(row, kind, variant, design)
    : buildDefaultDraft(row, kind, variant);
  if (!design?.designJson) {
    applyDefaultMvpPresetForKind(kind);
  }
  currentTemplateSharedCount.value = design?.sharedCount || 0;
  currentTemplateLinkedItems.value = resolveTemplateLinkedItems(row, design);
  selectedElementId.value = currentDraft.value?.elements[0]?.id || '';
}

async function openDesigner(row: CustomerInfo, kind: LabelKind) {
  if (!row.id) {
    message.warning('请先保存客户打印信息');
    return;
  }
  selectedRow.value = row;
  selectedKind.value = kind;
  selectedElementId.value = '';
  designerLeftActiveKey.value = 'templates';
  revokeAgentPreviewImageUrl();
  referenceOpacity.value = DEFAULT_REFERENCE_OPACITY;
  currentTemplateSharedCount.value = 0;
  designerLoading.value = true;
  designerVisible.value = true;
  void loadLocalPrinters(true);
  currentTemplateLinkedItems.value = [];
  designerTemplateRows.value = [];
  try {
    await loadDefaultMvpConfig(true);
    await loadDesignerTemplateRows(row, kind);
    const design = await getVisualPrintDesign(row.id, kind, row.productItemId);
    applyDesignerTemplate(row, kind, design?.id ? design : designerTemplateList.value[0]);
  } finally {
    designerLoading.value = false;
  }
}

function selectDesignerTemplate(design: Design) {
  if (!selectedRow.value) return;
  applyDesignerTemplate(selectedRow.value, selectedKind.value, design);
}

function createDesignerTemplateDraft() {
  if (!selectedRow.value) return;
  const variant = buildLabelVariant(selectedRow.value, selectedKind.value);
  const draft = buildDefaultDraft(selectedRow.value, selectedKind.value, variant);
  draft.forceNew = true;
  draft.id = `${draft.id}:new:${Date.now()}`;
  draft.labelName = `${getLabelKindLabel(selectedKind.value)}-${String(designerTemplateList.value.length + 1).padStart(2, '0')}`;
  currentDraft.value = draft;
  applyDefaultMvpPresetForKind(selectedKind.value);
  currentTemplateSharedCount.value = 0;
  currentTemplateLinkedItems.value = resolveTemplateLinkedItems(selectedRow.value);
  selectedElementId.value = currentDraft.value?.elements[0]?.id || '';
  designerLeftActiveKey.value = 'canvas';
}

function getDesignerTemplateKey(design: Design) {
  return String(design.id || `${design.labelKind}-${design.imageId || design.imageFile || 'draft'}`);
}

function isActiveDesignerTemplate(design: Design) {
  return !!design.id && design.id === currentDraft.value?.persistedId;
}

function getDesignerTemplateTitle(design: Design) {
  return design.labelName || getLabelKindLabel((design.labelKind || selectedKind.value) as LabelKind);
}

function getDesignerTemplateSubTitle(design: Design) {
  const mode = getDesignerTemplateModeText(design);
  return [
    design.id ? `#${design.id}` : '',
    mode,
    design.sharedCount ? `共用${design.sharedCount}` : '未挂接',
  ].filter(Boolean).join(' / ');
}

function getDesignerTemplateImageName(design: Design) {
  return design.imageFile || selectedReferenceImageName.value || '无参考图';
}

function getDesignerTemplateModeText(_design: Design) {
  return 'Python MVP位图';
}

function getDesignerTemplateModeColor(_design: Design) {
  return 'green';
}

async function openAttachDesigner() {
  if (!selectedRow.value?.id || !currentDraft.value?.persistedId) {
    message.warning('请先保存当前设计稿，再维护挂接型号尺寸');
    return;
  }
  attachLoading.value = true;
  try {
    const detail = await getVisualPrintCustomerInfoDetail(selectedRow.value.id);
    attachProductItems.value = normalizeProductItemRows(detail.productItems);
    attachSelectedProductItemIds.value = currentTemplateLinkedItems.value
      .map((item) => Number(item.id))
      .filter(Boolean);
    attachModalVisible.value = true;
  } finally {
    attachLoading.value = false;
  }
}

async function saveDesignAttachment() {
  if (!selectedRow.value?.id || !currentDraft.value?.persistedId) return;
  if (!attachSelectedProductItemIds.value.length) {
    message.warning('请选择需要挂接的产品型号尺寸');
    return;
  }
  attachLoading.value = true;
  try {
    const design = await attachVisualPrintDesign({
      customerInfoId: selectedRow.value.id,
      designId: currentDraft.value.persistedId,
      labelKind: currentDraft.value.labelKind,
      productItemIds: attachSelectedProductItemIds.value,
    });
    currentTemplateSharedCount.value = design.sharedCount || 0;
    currentTemplateLinkedItems.value = resolveTemplateLinkedItems(selectedRow.value, design);
    attachModalVisible.value = false;
    customerGridApi.query();
    message.success(`模板挂接已更新，${templateStatusText.value}`);
  } finally {
    attachLoading.value = false;
  }
}

function buildLabelVariant(row: CustomerInfo, kind: LabelKind): LabelVariant {
  const imageId = getLabelImageId(row, kind);
  const imageFile = getLabelImageFile(row, kind);
  const meta = imageFile ? visualPrintImageMetaMap[imageFile] : undefined;
  return {
    imageFile,
    imageHeightPx: meta?.heightPx,
    imageId,
    imageUrl: resolveVisualPrintImageUrl(imageFile),
    imageWidthPx: meta?.widthPx,
    labelKind: kind,
    labelName: getLabelKindLabel(kind),
    sourceRow: row.sourceRow,
  };
}

function getLabelImageId(row: CustomerInfo, kind: LabelKind) {
  if (kind === 'padBack') return row.padBackLabelImageId;
  if (kind === 'cleanBag') return row.cleanBagLabelImageId;
  if (kind === 'boxFront') return row.boxFrontLabelImageId;
  return row.customerSideLabelImageId;
}

function getLabelImageFile(row: CustomerInfo, kind: LabelKind) {
  if (kind === 'padBack') return row.padBackLabelImageFile;
  if (kind === 'cleanBag') return row.cleanBagLabelImageFile;
  if (kind === 'boxFront') return row.boxFrontLabelImageFile;
  return row.customerSideLabelImageFile;
}

function getLabelKindLabel(kind: LabelKind | VisualLabelKind) {
  return visualLabelKinds.find((item) => item.kind === kind)?.label || kind;
}

function getLabelShortLabel(kind: LabelKind | VisualLabelKind) {
  return visualLabelKinds.find((item) => item.kind === kind)?.shortLabel || kind;
}

function hasLabelImage(row: CustomerInfo, kind: LabelKind) {
  return Boolean(getLabelImageId(row, kind) || getLabelImageFile(row, kind));
}

function getLabelDesignId(row: CustomerInfo, kind: LabelKind) {
  if (kind === 'padBack') return row.padBackDesignId;
  if (kind === 'cleanBag') return row.cleanBagDesignId;
  if (kind === 'boxFront') return row.boxFrontDesignId;
  return row.customerSideDesignId;
}

function getLabelSharedCount(row: CustomerInfo, kind: LabelKind) {
  if (kind === 'padBack') return row.padBackSharedCount || 0;
  if (kind === 'cleanBag') return row.cleanBagSharedCount || 0;
  if (kind === 'boxFront') return row.boxFrontSharedCount || 0;
  return row.customerSideSharedCount || 0;
}

function getLabelPreviewStatus(row: CustomerInfo, kind: LabelKind) {
  if (getLabelDesignId(row, kind)) return '已设计';
  return hasLabelImage(row, kind) ? '参考图' : '未维护';
}

function getLabelPreviewStatusClass(row: CustomerInfo, kind: LabelKind) {
  if (getLabelDesignId(row, kind)) return 'is-designed';
  return hasLabelImage(row, kind) ? 'is-reference' : 'is-empty';
}

function parsePersistedDraft(
  row: CustomerInfo,
  kind: LabelKind,
  variant: LabelVariant,
  design: MesHcVisualPrintDesignerApi.Design,
): DesignerDraft {
  try {
    const parsed = JSON.parse(design.designJson || '{}') as DesignerDraft;
    const defaults = buildDefaultDraft(row, kind, variant);
    let widthMm = Number(design.widthMm || parsed.widthMm || defaults.widthMm);
    let heightMm = Number(design.heightMm || parsed.heightMm || defaults.heightMm);
    const designWidthMm = Number(parsed.designWidthMm || widthMm);
    const designHeightMm = Number(parsed.designHeightMm || heightMm);
    const printRotation = normalizePrintRotation(parsed.printRotation);
    const migrateLegacyRotationMedia = Number(parsed.schemaVersion || 0) < 7
      && parsed.preservePageSizeAfterRotation !== false
      && [90, 270].includes(printRotation)
      && Math.abs(widthMm - designWidthMm) < 0.05
      && Math.abs(heightMm - designHeightMm) < 0.05;
    if (migrateLegacyRotationMedia) {
      widthMm = designHeightMm;
      heightMm = designWidthMm;
    }
    return {
      ...defaults,
      ...parsed,
      background: parsed.background || '#FFFFFF',
      contentOffsetXmm: Number(parsed.contentOffsetXmm || 0),
      contentOffsetYmm: Number(parsed.contentOffsetYmm || 0),
      designHeightMm,
      designWidthMm,
      dpi: design.dpi || parsed.dpi || 300,
      elements: (parsed.elements?.length ? parsed.elements : defaults.elements).map(normalizeDesignerElement),
      heightMm,
      imageFile: design.imageFile || variant.imageFile,
      imageHeightPx: design.imageHeightPx || variant.imageHeightPx,
      imageId: design.imageId || variant.imageId,
      imageWidthPx: design.imageWidthPx || variant.imageWidthPx,
      preservePageSizeAfterRotation: parsed.preservePageSizeAfterRotation !== false,
      labelKind: kind,
      labelName: design.labelName || getLabelKindLabel(kind),
      persistedId: design.id,
      printRotation,
      rotatedContentOriginXmm: migrateLegacyRotationMedia ? 0 : Number(parsed.rotatedContentOriginXmm || 0),
      rotatedContentOriginYmm: migrateLegacyRotationMedia ? 0 : Number(parsed.rotatedContentOriginYmm || 0),
      rotationDirection: parsed.rotationDirection === 'counterclockwise' ? 'counterclockwise' : 'clockwise',
      scaleElementsWithDesignSize: parsed.scaleElementsWithDesignSize !== false,
      schemaVersion: DRAFT_SCHEMA_VERSION,
      threshold: Number(parsed.threshold || 188),
      widthMm,
    };
  } catch {
    return {
      ...buildDefaultDraft(row, kind, variant),
      persistedId: design.id,
    };
  }
}

function buildDefaultDraft(row: CustomerInfo, kind: LabelKind, variant: LabelVariant): DesignerDraft {
  const size = inferCanvasSize(row, kind, variant);
  const mvpPaper = DEFAULT_MVP_PAPER_BY_KIND[kind];
  return {
    background: '#FFFFFF',
    contentOffsetXmm: 0,
    contentOffsetYmm: 0,
    customer: formatText(row.customer),
    designHeightMm: mvpPaper?.designHeightMm || size.heightMm,
    designWidthMm: mvpPaper?.designWidthMm || size.widthMm,
    dpi: 300,
    elements: buildDefaultElements(kind, size),
    heightMm: mvpPaper?.mediaHeightMm || size.heightMm,
    id: buildDraftKey(row, kind, variant),
    imageFile: variant.imageFile,
    imageHeightPx: variant.imageHeightPx,
    imageId: variant.imageId,
    imageWidthPx: variant.imageWidthPx,
    labelKind: kind,
    labelName: getLabelKindLabel(kind),
    preservePageSizeAfterRotation: true,
    printRotation: mvpPaper ? 90 : 0,
    rotatedContentOriginXmm: 0,
    rotatedContentOriginYmm: 0,
    rotationDirection: 'clockwise',
    scaleElementsWithDesignSize: true,
    schemaVersion: DRAFT_SCHEMA_VERSION,
    sourceRow: row.sourceRow,
    threshold: 188,
    updatedAt: new Date().toISOString(),
    widthMm: mvpPaper?.mediaWidthMm || size.widthMm,
  };
}

function buildDraftKey(row: CustomerInfo, kind: LabelKind, variant: LabelVariant) {
  return `${row.id || 'new'}:${row.productItemId || row.sourceRow || 'master'}:${kind}:${
    variant.imageId || 'empty'
  }`;
}

const mvpFieldKeyMap: Record<string, string> = {
  'product.batchNo': 'batchNo',
  'product.expirationDate': 'expirationDate',
  'product.model': 'productModel',
  'product.padNo': 'padNo',
  'product.productInfo': 'productInfo',
  'product.productNo': 'productNo',
  'product.productionDate': 'productionDate',
  'product.quantity': 'quantity',
  'product.typeName': 'productTypeName',
};

function cloneMvpJson<T>(value: T): T {
  return structuredClone(value);
}

function deepMergeMvpJson(target: MvpJsonObject, override: MvpJsonObject) {
  for (const [key, value] of Object.entries(override)) {
    if (
      value &&
      typeof value === 'object' &&
      !Array.isArray(value) &&
      target[key] &&
      typeof target[key] === 'object' &&
      !Array.isArray(target[key])
    ) {
      deepMergeMvpJson(target[key] as MvpJsonObject, value as MvpJsonObject);
    } else {
      target[key] = cloneMvpJson(value);
    }
  }
  return target;
}

function applyMvpPaperPreset(config: MvpJsonObject, presetId?: string) {
  const result = cloneMvpJson(config);
  const presets = Array.isArray(config.paper_presets) ? config.paper_presets : [];
  const defaultPresetId = String(config.defaults?.paper_preset_id || presets[0]?.id || 'default');
  const preset = presets.find((item: MvpJsonObject) => String(item.id) === String(presetId || defaultPresetId))
    || presets[0];
  if (!preset) return result;
  if (preset.meta) {
    result.meta = deepMergeMvpJson(result.meta || {}, preset.meta);
  }
  for (const key of ['template_code', 'template_name', 'source_note']) {
    if (preset[key] !== undefined) {
      result.meta ||= {};
      result.meta[key] = cloneMvpJson(preset[key]);
    }
  }
  for (const key of ['page', 'calibration', 'printer', 'data']) {
    if (preset[key] && typeof preset[key] === 'object') {
      result[key] = deepMergeMvpJson(result[key] || {}, preset[key]);
    }
  }
  const overrides = preset.element_overrides || preset.elements;
  if (overrides && typeof overrides === 'object' && !Array.isArray(overrides)) {
    for (const element of result.elements || []) {
      const override = overrides[String(element.id || '')];
      if (override && typeof override === 'object') {
        deepMergeMvpJson(element, override);
      }
    }
  }
  return result;
}

function getDefaultMvpPresetId(kind: LabelKind) {
  return DEFAULT_MVP_PAPER_BY_KIND[kind]?.presetId || '';
}

async function loadDefaultMvpConfig(silent = true) {
  if (importedMvpConfig.value?.page && Array.isArray(importedMvpConfig.value.elements)) {
    return true;
  }
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/templates/default`);
    const result = await response.json();
    if (!response.ok || !result?.page || !Array.isArray(result?.elements)) {
      throw new Error(result?.message || '默认 MVP 模板内容不完整');
    }
    importedMvpConfig.value = result;
    return true;
  } catch (error) {
    if (!silent) {
      message.warning(
        `读取默认 MVP 模板失败：${error instanceof Error ? error.message : '请确认本地打印服务已启动'}`,
      );
    }
    return false;
  }
}

function applyDefaultMvpPresetForKind(kind: LabelKind) {
  const presetId = getDefaultMvpPresetId(kind);
  if (!presetId || !importedMvpConfig.value) return false;
  selectedMvpPaperPresetId.value = presetId;
  applyImportedMvpPaperPreset(presetId);
  return true;
}

function resolveMvpDataValue(data: MvpJsonObject, path: string) {
  let current: any = data;
  for (const part of path.split('.')) {
    if (!current || typeof current !== 'object') return '';
    current = current[part];
  }
  return current === undefined || current === null ? '' : String(current);
}

function resolveMvpStaticText(value: unknown, data: MvpJsonObject) {
  return String(value || '').replaceAll(/\{\{([^{}]+)\}\}/g, (_matched, path: string) =>
    resolveMvpDataValue(data, path.trim()),
  );
}

function convertMvpElement(element: MvpJsonObject, data: MvpJsonObject): DesignerElement | undefined {
  const type = String(element.type || '').toLowerCase();
  const common = {
    enabled: element.enabled !== false,
    height: Number(element.h || 0),
    id: String(element.id || buildElementId(type === 'barcode128' ? 'barcode' : type as DesignerElementType)),
    name: String(element.name || element.id || type),
    width: Number(element.w || 0),
    x: Number(element.x || 0),
    y: Number(element.y || 0),
  };
  if (type === 'text') {
    const sourceText = String(element.text || '');
    const fontSize = Number(element.font_size || 3);
    const exactField = sourceText.match(/^\{\{([^{}]+)\}\}$/);
    const mappedFieldKey = exactField ? mvpFieldKeyMap[(exactField[1] || '').trim()] : undefined;
    return normalizeDesignerElement({
      ...common,
      align: element.align || 'left',
      fieldKey: mappedFieldKey,
      fill: String(element.fill || '#111111'),
      fit: element.fit !== false,
      fontFamily: element.font || 'regular',
      fontSize,
      lineSpacing: Number(element.line_spacing || 0),
      minFontSize: Number(element.min_font_size ?? Math.max(1, fontSize * 0.55)),
      text: mappedFieldKey ? undefined : resolveMvpStaticText(sourceText, data),
      type: mappedFieldKey ? 'field' : 'text',
      valign: element.valign || 'top',
    } as DesignerElement);
  }
  if (type === 'barcode128') {
    const sourceValue = String(element.value || '');
    const exactField = sourceValue.match(/^\{\{([^{}]+)\}\}$/);
    const mappedFieldKey = exactField ? mvpFieldKeyMap[(exactField[1] || '').trim()] : undefined;
    return normalizeDesignerElement({
      ...common,
      fieldKey: mappedFieldKey,
      fill: String(element.fill || '#111111'),
      humanFontSize: Number(element.human_font_size || 1.2),
      humanReadable: element.human_readable !== false,
      quietMm: Number(element.quiet_mm || 0.4),
      text: mappedFieldKey ? undefined : resolveMvpStaticText(sourceValue, data),
      type: 'barcode',
    } as DesignerElement);
  }
  if (type === 'rect') {
    return normalizeDesignerElement({
      ...common,
      fill: element.fill ? String(element.fill) : undefined,
      stroke: String(element.stroke || '#111111'),
      strokeWidth: Number(element.stroke_width || 0),
      type: 'rect',
    } as DesignerElement);
  }
  if (type === 'line') {
    return normalizeDesignerElement({
      ...common,
      stroke: String(element.stroke || '#111111'),
      strokeWidth: Number(element.stroke_width || 0.2),
      type: 'line',
    } as DesignerElement);
  }
  return undefined;
}

function applyImportedMvpPaperPreset(presetId?: string) {
  if (!currentDraft.value || !importedMvpConfig.value) return;
  const config = applyMvpPaperPreset(importedMvpConfig.value, presetId);
  const page = config.page || {};
  const calibration = config.calibration?.content_offset_mm || {};
  const origin = page.rotated_content_origin_mm || {};
  const elements = (config.elements || [])
    .map((item: MvpJsonObject) => convertMvpElement(item, config.data || {}))
    .filter((item: DesignerElement | undefined): item is DesignerElement => Boolean(item));
  if (!elements.length) {
    throw new Error('MVP JSON中没有可导入的文本、条码、矩形或线条元素');
  }
  currentDraft.value = {
    ...currentDraft.value,
    background: String(page.background || '#FFFFFF'),
    contentOffsetXmm: Number(calibration.x || 0),
    contentOffsetYmm: Number(calibration.y || 0),
    designHeightMm: Number(page.design_height_mm || page.height_mm || currentDraft.value.heightMm),
    designWidthMm: Number(page.design_width_mm || page.width_mm || currentDraft.value.widthMm),
    dpi: Number(page.dpi || 300),
    elements,
    heightMm: Number(page.height_mm || currentDraft.value.heightMm),
    labelName: String(config.meta?.template_name || currentDraft.value.labelName),
    preservePageSizeAfterRotation: page.preserve_page_size_after_rotation !== false,
    printRotation: normalizePrintRotation(page.output_rotation_degrees),
    rotatedContentOriginXmm: Number(origin.x || 0),
    rotatedContentOriginYmm: Number(origin.y || 0),
    rotationDirection:
      ['ccw', 'counterclockwise'].includes(String(page.output_rotation_direction || '').toLowerCase())
        ? 'counterclockwise'
        : 'clockwise',
    schemaVersion: DRAFT_SCHEMA_VERSION,
    threshold: Number(page.threshold || 188),
    updatedAt: new Date().toISOString(),
    widthMm: Number(page.width_mm || currentDraft.value.widthMm),
  };
  selectedElementId.value = elements[0]?.id || '';
  revokeAgentPreviewImageUrl();
}

const handleMvpTemplateImport: UploadProps['beforeUpload'] = async (file) => {
  try {
    const parsed = JSON.parse(await file.text()) as MvpJsonObject;
    if (!parsed.page || !Array.isArray(parsed.elements)) {
      throw new Error('JSON缺少 page 或 elements');
    }
    importedMvpConfig.value = parsed;
    const presets = Array.isArray(parsed.paper_presets) ? parsed.paper_presets : [];
    selectedMvpPaperPresetId.value = String(parsed.defaults?.paper_preset_id || presets[0]?.id || 'default');
    applyImportedMvpPaperPreset(selectedMvpPaperPresetId.value);
    message.success(`已导入Python MVP模板：${parsed.meta?.template_name || file.name}`);
  } catch (error) {
    message.error(`导入Python MVP模板失败：${error instanceof Error ? error.message : 'JSON格式不正确'}`);
  }
  return false;
};

let designResizeSnapshot: { heightMm: number; widthMm: number } | undefined;

function captureDesignSizeBeforeChange() {
  const draft = currentDraft.value;
  if (!draft) return;
  designResizeSnapshot = { heightMm: draft.designHeightMm, widthMm: draft.designWidthMm };
}

function handleDesignSizeChange() {
  const draft = currentDraft.value;
  const previous = designResizeSnapshot;
  designResizeSnapshot = undefined;
  if (!draft || !previous || !draft.scaleElementsWithDesignSize) return;
  if (previous.widthMm <= 0 || previous.heightMm <= 0) return;
  const scaleX = draft.designWidthMm / previous.widthMm;
  const scaleY = draft.designHeightMm / previous.heightMm;
  if (!Number.isFinite(scaleX) || !Number.isFinite(scaleY) || (scaleX === 1 && scaleY === 1)) return;
  for (const element of draft.elements) {
    element.x = roundMm(element.x * scaleX);
    element.y = roundMm(element.y * scaleY);
    element.width = roundMm(element.width * scaleX);
    element.height = roundMm(element.height * scaleY);
  }
  syncPhysicalMediaToRotation();
}

function syncPhysicalMediaToRotation() {
  const draft = currentDraft.value;
  if (!draft) return;
  const rotation = normalizePrintRotation(draft.printRotation);
  draft.widthMm = roundMm([90, 270].includes(rotation) ? draft.designHeightMm : draft.designWidthMm);
  draft.heightMm = roundMm([90, 270].includes(rotation) ? draft.designWidthMm : draft.designHeightMm);
  draft.preservePageSizeAfterRotation = true;
  draft.rotatedContentOriginXmm = 0;
  draft.rotatedContentOriginYmm = 0;
  scheduleCanvasRender(true);
}

function handleImportedPrnElementTypeChange(element: DesignerElement, type: DesignerElementType) {
  element.type = type;
  element.guideOnly = false;
  const normalized = normalizeDesignerElement(element);
  Object.assign(element, normalized);
  scheduleCanvasRender(true);
}

function handleSelectedPrnElementTypeChange(type: DesignerElementType) {
  if (selectedElement.value) {
    handleImportedPrnElementTypeChange(selectedElement.value, type);
  }
}

const handlePrnCoordinateImport: UploadProps['beforeUpload'] = async (file) => {
  if (!currentDraft.value) return false;
  prnImportLoading.value = true;
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/import-prn`, {
      body: JSON.stringify({
        content: await file.text(),
        fileName: file.name,
        sourceDpi: prnImportDpi.value,
      }),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || 'PRN坐标解析失败');
    }
    const importedElements = (Array.isArray(result.elements) ? result.elements : [])
      .map((item: DesignerElement) => normalizeDesignerElement(item));
    if (!importedElements.length) {
      throw new Error('PRN中没有识别到可定位对象');
    }
    const output = result.output || {};
    const origin = output.rotatedContentOriginMm || {};
    currentDraft.value = {
      ...currentDraft.value,
      designHeightMm: Number(result.designPage?.heightMm || currentDraft.value.designHeightMm),
      designWidthMm: Number(result.designPage?.widthMm || currentDraft.value.designWidthMm),
      dpi: Number(result.sourceDpi || prnImportDpi.value),
      elements: importedElements,
      heightMm: Number(result.rawPage?.heightMm || currentDraft.value.heightMm),
      preservePageSizeAfterRotation: output.preservePageSizeAfterRotation !== false,
      printRotation: normalizePrintRotation(output.rotationDegrees),
      prnSourceDpi: Number(result.sourceDpi || prnImportDpi.value),
      prnSourceFileName: file.name,
      rotatedContentOriginXmm: Number(origin.x || 0),
      rotatedContentOriginYmm: Number(origin.y || 0),
      rotationDirection: output.rotationDirection === 'counterclockwise' ? 'counterclockwise' : 'clockwise',
      schemaVersion: DRAFT_SCHEMA_VERSION,
      updatedAt: new Date().toISOString(),
      widthMm: Number(result.rawPage?.widthMm || currentDraft.value.widthMm),
    };
    selectedElementId.value = importedElements[0]?.id || '';
    canvasViewMode.value = 'design';
    revokeAgentPreviewImageUrl();
    scheduleCanvasRender(true);
    const warningText = Array.isArray(result.warnings) && result.warnings.length
      ? `；${result.warnings.join('；')}`
      : '';
    message.success(`已从${file.name}导入${importedElements.length}个坐标对象${warningText}`, 8);
  } catch (error) {
    message.error(`导入PRN坐标失败：${error instanceof Error ? error.message : '请确认本地打印代理已启动'}`);
  } finally {
    prnImportLoading.value = false;
  }
  return false;
};

function inferCanvasSize(row: CustomerInfo, kind: LabelKind, variant?: LabelVariant) {
  const mvpPaper = DEFAULT_MVP_PAPER_BY_KIND[kind];
  if (mvpPaper) {
    return {
      heightMm: mvpPaper.designHeightMm,
      widthMm: mvpPaper.designWidthMm,
    };
  }
  const sideSize = formatText(row.customerSideSize);
  const match = sideSize.match(/(\d+(?:\.\d+)?)\s*\*\s*(\d+(?:\.\d+)?)/);
  const defaultWidthMm = kind === 'boxFront' ? 120 : kind === 'customerSide' && match ? Number(match[1]) : 100;
  if (variant?.imageWidthPx && variant.imageHeightPx) {
    return {
      heightMm: roundMm((defaultWidthMm * variant.imageHeightPx) / variant.imageWidthPx),
      widthMm: defaultWidthMm,
    };
  }
  if (kind === 'customerSide' && match) {
    return { heightMm: Number(match[2]), widthMm: Number(match[1]) };
  }
  if (kind === 'boxFront') return { heightMm: 80, widthMm: 120 };
  if (kind === 'padBack') return { heightMm: 60, widthMm: 100 };
  return { heightMm: 70, widthMm: 105 };
}

function buildElementId(type: DesignerElementType) {
  return `${type}-${Date.now()}-${Math.round(Math.random() * 10_000)}`;
}

function normalizeDesignerElement(element: DesignerElement): DesignerElement {
  const isText = element.type === 'field' || element.type === 'text';
  const isBarcode = element.type === 'barcode';
  const isStrokeElement = element.type === 'line' || element.type === 'rect';
  return {
    ...element,
    align: element.align || 'left',
    enabled: element.enabled !== false,
    fill: element.fill || (isText || isBarcode ? '#111111' : undefined),
    fit: isText ? element.fit !== false : element.fit,
    fontFamily: element.fontFamily || (element.bold ? 'bold' : 'regular'),
    humanFontSize: isBarcode ? Number(element.humanFontSize ?? 1.2) : element.humanFontSize,
    humanReadable: isBarcode ? element.humanReadable !== false : element.humanReadable,
    lineSpacing: isText ? Number(element.lineSpacing || 0) : element.lineSpacing,
    minFontSize: isText ? Number(element.minFontSize || 1) : element.minFontSize,
    quietMm: isBarcode ? Number(element.quietMm ?? 0.4) : element.quietMm,
    stroke: element.stroke || (isStrokeElement ? '#111111' : undefined),
    strokeWidth:
      isStrokeElement ? Number(element.strokeWidth ?? (element.type === 'line' ? 0.2 : 0.4)) : element.strokeWidth,
    valign: isText ? element.valign || 'top' : element.valign,
  };
}

function makeLayoutElement(
  type: DesignerElementType,
  name: string,
  x: number,
  y: number,
  width: number,
  height: number,
  extra: Partial<DesignerElement> = {},
): DesignerElement {
  return normalizeDesignerElement({
    id: buildElementId(type),
    name,
    type,
    x: roundMm(x),
    y: roundMm(y),
    width: roundMm(width),
    height: roundMm(height),
    ...extra,
  });
}

function buildDefaultElements(kind: LabelKind, size: { heightMm: number; widthMm: number }): DesignerElement[] {
  const w = size.widthMm;
  const h = size.heightMm;
  const px = (value: number) => (w * value) / 100;
  const py = (value: number) => (h * value) / 100;
  if (kind === 'customerSide') {
    return [
      makeLayoutElement('rect', '外框', px(0), py(0), px(100), py(100), { strokeWidth: 0.4 }),
      makeLayoutElement('line', '列线-标题', px(18), py(0), 0, py(100), { strokeWidth: 0.3 }),
      makeLayoutElement('line', '列线-二维码', px(80), py(0), 0, py(41), { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-Plant', px(0), py(9), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-PN', px(0), py(27), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-Material', px(0), py(41), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-INFO', px(0), py(56), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-日期', px(0), py(70), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('line', '行线-底部', px(0), py(83), px(100), 0, { strokeWidth: 0.3 }),
      makeLayoutElement('text', 'Plant标题', px(0.6), py(1.2), px(17), py(6), {
        bold: true,
        fontSize: 5,
        text: 'Plant',
      }),
      makeLayoutElement('field', 'Plant值', px(44), py(1.4), px(26), py(6), {
        align: 'center',
        fieldKey: 'plant',
        fontSize: 5.5,
      }),
      makeLayoutElement('text', 'PN标题', px(0.6), py(13.2), px(16), py(7), {
        bold: true,
        fontSize: 5,
        text: 'PN:',
      }),
      makeLayoutElement('barcode', 'PN条码', px(19), py(9.5), px(58), py(13.5), { fieldKey: 'pn' }),
      makeLayoutElement('text', '物料描述标题', px(0.6), py(28.4), px(17), py(11), {
        bold: true,
        fontSize: 4.8,
        text: 'Material\nDescription',
      }),
      makeLayoutElement('field', '物料描述', px(42), py(31.8), px(36), py(6), {
        align: 'center',
        fieldKey: 'materialDescription',
        fontSize: 5.2,
      }),
      makeLayoutElement('text', 'Vendor PN标题', px(0.6), py(44), px(18), py(6), {
        bold: true,
        fontSize: 4.8,
        text: 'Vendor PN',
      }),
      makeLayoutElement('text', 'PKG标题', px(65), py(42.5), px(14), py(11), {
        bold: true,
        fontSize: 4.6,
        text: 'PKG of\nTTL:',
      }),
      makeLayoutElement('field', 'PKG值', px(84), py(45), px(12), py(6), {
        align: 'center',
        fieldKey: 'packageIndex',
        fontSize: 5.2,
      }),
      makeLayoutElement('text', 'INFO标题', px(0.6), py(57), px(12), py(6), {
        bold: true,
        fontSize: 4.8,
        text: 'INFO:',
      }),
      makeLayoutElement('field', 'INFO值', px(0.8), py(64), px(62), py(5), {
        bold: true,
        fieldKey: 'info',
        fontSize: 4.2,
      }),
      makeLayoutElement('field', 'Batch值', px(65), py(61), px(32), py(6), {
        bold: true,
        fieldKey: 'batchNo',
        fontSize: 5,
      }),
      makeLayoutElement('text', 'EXP标题', px(0.6), py(72), px(17), py(7), {
        bold: true,
        fontSize: 4.8,
        text: 'EXP Date',
      }),
      makeLayoutElement('field', 'EXP值', px(38), py(73), px(21), py(6), {
        align: 'center',
        fieldKey: 'expDate',
        fontSize: 5,
      }),
      makeLayoutElement('text', 'Shipping标题', px(65), py(70.5), px(15), py(11), {
        bold: true,
        fontSize: 4.5,
        text: 'Shipping\nDate',
      }),
      makeLayoutElement('field', 'Shipping值', px(82), py(73), px(16), py(6), {
        align: 'center',
        fieldKey: 'shippingDate',
        fontSize: 4.8,
      }),
      makeLayoutElement('text', 'PUR标题', px(0.6), py(84.5), px(18), py(12), {
        bold: true,
        fontSize: 4.6,
        text: 'PUR UOM\nPC',
      }),
      makeLayoutElement('text', 'QTY标题', px(19), py(85.5), px(12), py(5), {
        bold: true,
        fontSize: 4,
        text: 'QTY:',
      }),
      makeLayoutElement('barcode', 'QTY条码', px(40), py(83.5), px(18), py(10), { fieldKey: 'quantity' }),
      makeLayoutElement('field', '数量值', px(47), py(93), px(6), py(5), {
        align: 'center',
        fieldKey: 'quantity',
        fontSize: 4.6,
      }),
      makeLayoutElement('text', 'PO标题', px(65), py(87), px(10), py(6), {
        bold: true,
        fontSize: 4.8,
        text: 'PO:',
      }),
      makeLayoutElement('field', 'PO值', px(83), py(87.5), px(16), py(6), { fieldKey: 'po', fontSize: 4.6 }),
    ];
  }
  return [
    makeLayoutElement('line', '页眉分隔线', px(0), py(20), px(100), 0, { strokeWidth: 0.35 }),
    makeLayoutElement('text', '产品型号', px(11), py(24), px(30), py(8), {
      bold: true,
      fontSize: 7.6,
      text: 'DTP0201',
    }),
    makeLayoutElement('text', '标签标题', px(52), py(23), px(42), py(9), {
      align: 'center',
      bold: true,
      fontSize: 7.4,
      text: '化学机械研磨垫',
    }),
    makeLayoutElement('text', '产品编码标题', px(11), py(38), px(22), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '产品编码 Product No.:',
    }),
    makeLayoutElement('field', '产品编码', px(21), py(43), px(18), py(5), {
      bold: true,
      fieldKey: 'productNo',
      fontSize: 3.8,
    }),
    makeLayoutElement('barcode', '产品条码', px(52), py(37.5), px(20), py(7), { fieldKey: 'productNo' }),
    makeLayoutElement('text', '产品信息标题', px(11), py(47), px(25), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '产品信息 Product Infor.:',
    }),
    makeLayoutElement('field', '产品信息', px(21), py(52), px(50), py(5), {
      bold: true,
      fieldKey: 'productInfo',
      fontSize: 3.8,
    }),
    makeLayoutElement('text', '数量标题', px(11), py(58), px(20), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '数    量 Quantity:',
    }),
    makeLayoutElement('field', '数量', px(21), py(64), px(12), py(5), {
      bold: true,
      fieldKey: 'quantity',
      fontSize: 4.1,
    }),
    makeLayoutElement('text', '片号标题', px(52), py(58), px(20), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '片    号 Pad No.:',
    }),
    makeLayoutElement('field', '片号', px(62), py(64), px(12), py(5), {
      align: 'center',
      bold: true,
      fieldKey: 'padNo',
      fontSize: 4.1,
    }),
    makeLayoutElement('text', '批号标题', px(11), py(70), px(22), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '生产批号 Batch No.:',
    }),
    makeLayoutElement('field', '批号', px(21), py(75), px(22), py(5), {
      bold: true,
      fieldKey: 'batchNo',
      fontSize: 3.8,
    }),
    makeLayoutElement('barcode', '批号条码', px(52), py(69), px(24), py(8), { fieldKey: 'batchNo' }),
    makeLayoutElement('text', '生产日期标题', px(11), py(81), px(26), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '生产日期 Production Date:',
    }),
    makeLayoutElement('field', '生产日期', px(20), py(86), px(20), py(5), {
      bold: true,
      fieldKey: 'productionDate',
      fontSize: 3.8,
    }),
    makeLayoutElement('text', '失效日期标题', px(52), py(81), px(28), py(5), {
      bold: true,
      fontSize: 3.5,
      text: '失效日期 Expiration Date:',
    }),
    makeLayoutElement('field', '失效日期', px(62), py(86), px(20), py(5), {
      bold: true,
      fieldKey: 'expirationDate',
      fontSize: 3.8,
    }),
    makeLayoutElement('line', '底部分隔线', px(5), py(92), px(90), 0, { strokeWidth: 0.35 }),
  ];
}

function makeElement(
  type: DesignerElementType,
  index: number,
  options: { field?: DataFieldOption; x?: number; y?: number } = {},
): DesignerElement {
  const fieldName = getDataFieldDisplayName(options.field);
  const base = {
    id: buildElementId(type),
    name: getElementTypeLabel(type),
    x: options.x ?? 8 + index * 4,
    y: options.y ?? 8 + index * 5,
  };
  if (type === 'text') {
    return { ...base, bold: true, fontSize: 8, height: 9, text: 'DTP0201', type, width: 36 };
  }
  if (type === 'field') {
    return {
      ...base,
      fieldKey: options.field?.value || 'productNo',
      fontSize: 5,
      height: 8,
      name: fieldName || '产品编码',
      type,
      width: 45,
    };
  }
  if (type === 'barcode') {
    return {
      ...base,
      fieldKey: options.field?.value || 'productNo',
      height: 12,
      name: buildFieldElementName(type, options.field),
      type,
      width: 42,
    };
  }
  if (type === 'qrcode') {
    return {
      ...base,
      fieldKey: options.field?.value || 'batchNo',
      height: 24,
      name: buildFieldElementName(type, options.field),
      type,
      width: 24,
    };
  }
  if (type === 'line') {
    return { ...base, height: 0, name: '分隔线', strokeWidth: 0.4, type, width: 80 };
  }
  if (type === 'rect') {
    return { ...base, height: 18, name: '边框', strokeWidth: 0.5, type, width: 40 };
  }
  return { ...base, height: 18, name: '图片', type, width: 32 };
}

function addElement(type: DesignerElementType) {
  if (!currentDraft.value) return;
  canvasViewMode.value = 'design';
  const next = normalizeDesignerElement(makeElement(type, currentDraft.value.elements.length));
  currentDraft.value.elements.push(next);
  selectedElementId.value = next.id;
}

function addFieldElementFromSource(field: DataFieldOption, elementType: FieldElementType) {
  createFieldBoundElement(field, elementType);
}

function createFieldBoundElement(
  field: DataFieldOption,
  elementType: FieldElementType,
  position?: { x: number; y: number },
) {
  if (!currentDraft.value) return;
  canvasViewMode.value = 'design';
  const next = makeElement(elementType, currentDraft.value.elements.length, {
    field,
    x: position?.x,
    y: position?.y,
  });
  currentDraft.value.elements.push(next);
  selectedElementId.value = next.id;
}

function startFieldDrag(event: DragEvent, field: DataFieldOption, elementType: FieldElementType = 'field') {
  if (!event.dataTransfer) return;
  const payload: FieldDragPayload = {
    elementType,
    fieldKey: field.value,
    fieldLabel: field.label,
  };
  event.dataTransfer.effectAllowed = 'copy';
  event.dataTransfer.setData(FIELD_DRAG_MIME, JSON.stringify(payload));
  event.dataTransfer.setData('text/plain', field.label);
}

function handleCanvasFieldDragOver(event: DragEvent) {
  if (canvasViewMode.value !== 'design' || !hasFieldDragPayload(event)) return;
  event.preventDefault();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy';
  }
}

function handleCanvasFieldDrop(event: DragEvent) {
  if (canvasViewMode.value !== 'design') return;
  const payload = readFieldDragPayload(event);
  if (!payload) return;
  event.preventDefault();
  const point = getCanvasDropPoint(event);
  createFieldBoundElement(payload.field, payload.elementType, point);
}

function handleElementFieldDragOver(event: DragEvent, element: DesignerElement) {
  if (
    canvasViewMode.value !== 'design'
    || !hasFieldDragPayload(event)
    || !isFieldBindableElement(element)
  ) return;
  event.preventDefault();
  event.stopPropagation();
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'link';
  }
}

function handleElementFieldDrop(event: DragEvent, element: DesignerElement) {
  if (canvasViewMode.value !== 'design') return;
  const payload = readFieldDragPayload(event);
  if (!payload || !isFieldBindableElement(element)) return;
  event.preventDefault();
  event.stopPropagation();
  bindFieldToElement(element, payload.field);
}

function bindFieldToElement(element: DesignerElement, field: DataFieldOption) {
  element.fieldKey = field.value;
  element.name = buildFieldElementName(element.type as FieldElementType, field);
  selectedElementId.value = element.id;
}

function hasFieldDragPayload(event: DragEvent) {
  return Array.from(event.dataTransfer?.types || []).includes(FIELD_DRAG_MIME);
}

function readFieldDragPayload(event: DragEvent) {
  const raw = event.dataTransfer?.getData(FIELD_DRAG_MIME);
  if (!raw) return undefined;
  try {
    const payload = JSON.parse(raw) as FieldDragPayload;
    const elementType = normalizeFieldElementType(payload.elementType);
    if (!payload.fieldKey || !elementType) return undefined;
    const field = dataFieldOptions.value.find((item) => item.value === payload.fieldKey) || {
      label: payload.fieldLabel || payload.fieldKey,
      value: payload.fieldKey,
    };
    return { elementType, field };
  } catch {
    return undefined;
  }
}

function normalizeFieldElementType(type?: string): FieldElementType | undefined {
  return type === 'barcode' || type === 'field' ? type : undefined;
}

function getCanvasDropPoint(event: DragEvent) {
  const canvas = event.currentTarget as HTMLElement;
  const rect = canvas.getBoundingClientRect();
  const draft = currentDraft.value;
  const x = roundMm((event.clientX - rect.left) / canvasScale.value);
  const y = roundMm((event.clientY - rect.top) / canvasScale.value);
  return {
    x: Math.max(0, Math.min(draft?.designWidthMm || draft?.widthMm || x, x)),
    y: Math.max(0, Math.min(draft?.designHeightMm || draft?.heightMm || y, y)),
  };
}

function isFieldBindableElement(element: DesignerElement) {
  return ['barcode', 'field'].includes(element.type);
}

function getDataFieldDisplayName(field?: DataFieldOption) {
  return (field?.label || '').replace(/^[A-Z]+\s+/, '').trim();
}

function buildFieldElementName(type: FieldElementType, field?: DataFieldOption) {
  const fieldName = getDataFieldDisplayName(field);
  if (type === 'barcode') return fieldName ? `${fieldName}条码` : '产品条码';
  return fieldName || '产品编码';
}

async function saveCurrentDraft() {
  if (!currentDraft.value || !selectedRow.value?.id) return;
  if (printGeometry.value?.warnings.length) {
    message.error(`无法保存：${printGeometry.value.warnings.join('；')}，请先点击“按设计尺寸和方向匹配”`);
    return;
  }
  const draft = currentDraft.value;
  draft.schemaVersion = DRAFT_SCHEMA_VERSION;
  draft.updatedAt = new Date().toISOString();
  const shouldForceNew = !!draft.forceNew && !draft.persistedId;
  const persistedDraft = {
    ...draft,
    forceNew: false,
    rendererTemplate: buildMvpRendererTemplate(draft),
  };
  const savedId = await saveVisualPrintDesign({
    customerInfoId: selectedRow.value.id,
    designJson: JSON.stringify(persistedDraft),
    dpi: draft.dpi,
    heightMm: draft.heightMm,
    id: draft.persistedId,
    imageFile: draft.imageFile,
    imageHeightPx: draft.imageHeightPx,
    imageId: draft.imageId,
    imageWidthPx: draft.imageWidthPx,
    btwCallFile: '',
    btwTemplateRootDir: '',
    forceNew: shouldForceNew,
    labelKind: draft.labelKind,
    labelName: draft.labelName,
    productItemId: selectedRow.value.productItemId,
    status: 0,
    widthMm: draft.widthMm,
  });
  draft.persistedId = savedId;
  draft.forceNew = false;
  await loadDesignerTemplateRows(selectedRow.value, draft.labelKind);
  const savedDesign = designerTemplateRows.value.find((item) => item.id === savedId)
    || await getVisualPrintDesign(selectedRow.value.id, draft.labelKind, selectedRow.value.productItemId);
  if (savedDesign?.id) {
    applyDesignerTemplate(selectedRow.value, draft.labelKind, savedDesign);
  } else {
    currentTemplateSharedCount.value = 0;
    currentTemplateLinkedItems.value = resolveTemplateLinkedItems(selectedRow.value);
  }
  customerGridApi.query();
  message.success(`设计稿已保存，${templateStatusText.value}`);
}

function resetCurrentDraft() {
  if (!selectedRow.value || !currentDraft.value) return;
  Modal.confirm({
    content: '重置后会恢复 Python MVP 的默认纸张、打印方向、旋转原点和元素布局。',
    onOk: () => {
      const nextDraft = buildDefaultDraft(selectedRow.value!, selectedKind.value, buildLabelVariant(selectedRow.value!, selectedKind.value));
      currentDraft.value = nextDraft;
      applyDefaultMvpPresetForKind(selectedKind.value);
      selectedElementId.value = currentDraft.value?.elements[0]?.id || '';
      revokeAgentPreviewImageUrl();
      scheduleCanvasRender(true);
    },
    title: '重置当前设计',
  });
}

async function restoreCurrentMvpPaperPreset() {
  if (!getDefaultMvpPresetId(selectedKind.value)) {
    message.warning('当前标签类型没有对应的 Python MVP 纸张预设');
    return;
  }
  const loaded = await loadDefaultMvpConfig(false);
  if (!loaded || !applyDefaultMvpPresetForKind(selectedKind.value)) return;
  canvasViewMode.value = 'design';
  scheduleCanvasRender(true);
  message.success('已恢复 Python MVP 的纸张、方向和旋转参数');
}

async function handleReferenceImageUpload(file: File) {
  if (!currentDraft.value) return;
  const isSupportedImage = /^image\/(png|jpe?g|webp)$/i.test(file.type) || /\.(png|jpe?g|webp)$/i.test(file.name);
  if (!isSupportedImage) {
    message.warning('请上传 PNG、JPG 或 WebP 图片');
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    message.warning('参考图不能超过 5MB');
    return;
  }
  try {
    const dataUrl = await readFileAsDataUrl(file);
    const meta = await readImageSize(dataUrl);
    const safeName = file.name.slice(0, 240);
    currentDraft.value.referenceImageDataUrl = dataUrl;
    currentDraft.value.referenceImageName = safeName;
    currentDraft.value.imageFile = safeName;
    currentDraft.value.imageId = `upload:${Date.now()}`;
    currentDraft.value.imageWidthPx = meta.width;
    currentDraft.value.imageHeightPx = meta.height;
    currentDraft.value.updatedAt = new Date().toISOString();
    showReference.value = true;
    message.success(`参考图已上传：${safeName}`);
  } catch (error) {
    message.error(`参考图上传失败：${error instanceof Error ? error.message : '图片读取失败'}`);
  }
}

function restoreDefaultReferenceImage() {
  if (!currentDraft.value) return;
  const variant = selectedVariant.value;
  currentDraft.value.referenceImageDataUrl = undefined;
  currentDraft.value.referenceImageName = undefined;
  currentDraft.value.imageFile = variant?.imageFile;
  currentDraft.value.imageId = variant?.imageId;
  currentDraft.value.imageWidthPx = variant?.imageWidthPx;
  currentDraft.value.imageHeightPx = variant?.imageHeightPx;
  currentDraft.value.updatedAt = new Date().toISOString();
  showReference.value = Boolean(variant?.imageUrl);
  message.success(variant?.imageUrl ? '已恢复Excel参考图' : '已清除自定义参考图');
}

function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener('load', () => resolve(String(reader.result || '')));
    reader.addEventListener('error', () => reject(reader.error || new Error('读取图片失败')));
    reader.readAsDataURL(file);
  });
}

function readImageSize(dataUrl: string) {
  return new Promise<{ height: number; width: number }>((resolve, reject) => {
    const image = new Image();
    image.addEventListener('load', () => resolve({ height: image.naturalHeight, width: image.naturalWidth }));
    image.addEventListener('error', () => reject(new Error('读取图片尺寸失败')));
    image.src = dataUrl;
  });
}

function getElementStyle(element: DesignerElement): CSSProperties {
  const scale = canvasScale.value;
  const isVerticalLine = element.type === 'line' && element.height > element.width;
  const style: CSSProperties = {
    alignItems:
      element.valign === 'middle' ? 'center' : element.valign === 'bottom' ? 'flex-end' : 'flex-start',
    color: element.fill || '#111111',
    height: `${Math.max(2, Math.abs(element.height) * scale)}px`,
    justifyContent:
      element.align === 'center' ? 'center' : element.align === 'right' ? 'flex-end' : 'flex-start',
    left: `${element.x * scale}px`,
    opacity: element.enabled === false ? 0.35 : 1,
    top: `${element.y * scale}px`,
    width: `${Math.max(isVerticalLine ? 2 : 6, Math.abs(element.width) * scale)}px`,
  };
  if (!pythonCanvasEnabled.value) {
    if (element.type === 'field' || element.type === 'text') {
      style.fontFamily = 'Arial, "Microsoft YaHei", sans-serif';
      style.fontSize = `${Math.max(7, Number(element.fontSize || 3) * scale)}px`;
      style.fontWeight = element.fontFamily === 'regular' && !element.bold ? '400' : '700';
      style.lineHeight = '1.08';
      style.textAlign = element.align || 'left';
      style.whiteSpace = 'pre-wrap';
    } else if (element.type === 'line') {
      const lineWidth = Math.max(1, Number(element.strokeWidth || 0.2) * scale);
      style.background = element.stroke || '#111111';
      style.border = '0';
      style.height = `${isVerticalLine ? Math.max(2, Math.abs(element.height) * scale) : lineWidth}px`;
      style.minHeight = `${lineWidth}px`;
      style.minWidth = `${lineWidth}px`;
      style.width = `${isVerticalLine ? lineWidth : Math.max(2, Math.abs(element.width) * scale)}px`;
    } else if (element.type === 'rect') {
      style.background = element.fill || 'transparent';
      style.border = `${Math.max(1, Number(element.strokeWidth || 0.4) * scale)}px solid ${element.stroke || '#111111'}`;
    }
  }
  return style;
}

function getLocalElementText(element: DesignerElement) {
  if (element.guideOnly) return element.name;
  if (element.type === 'field') {
    return element.fieldKey ? formatText(activePrintData.value[element.fieldKey]) : '';
  }
  if (element.type === 'text') return element.text || '';
  if (element.type === 'barcode') {
    const value = element.fieldKey ? formatText(activePrintData.value[element.fieldKey]) : formatText(element.text);
    return value || element.name;
  }
  return '';
}

function startDragElement(event: PointerEvent, element: DesignerElement) {
  if (canvasViewMode.value !== 'design') return;
  dragState.value = {
    elementId: element.id,
    startClientX: event.clientX,
    startClientY: event.clientY,
    startX: element.x,
    startY: element.y,
  };
  window.addEventListener('pointermove', handleDragElement);
  window.addEventListener('pointerup', stopDragElement);
}

function handleDragElement(event: PointerEvent) {
  if (canvasViewMode.value !== 'design' || !dragState.value || !currentDraft.value) return;
  const element = currentDraft.value.elements.find((item) => item.id === dragState.value?.elementId);
  if (!element) return;
  const dx = (event.clientX - dragState.value.startClientX) / canvasScale.value;
  const dy = (event.clientY - dragState.value.startClientY) / canvasScale.value;
  element.x = Math.max(0, roundMm(dragState.value.startX + dx));
  element.y = Math.max(0, roundMm(dragState.value.startY + dy));
}

function stopDragElement() {
  const shouldRender = !!dragState.value;
  dragState.value = undefined;
  window.removeEventListener('pointermove', handleDragElement);
  window.removeEventListener('pointerup', stopDragElement);
  if (shouldRender) scheduleCanvasRender(true);
}

function removeSelectedElement() {
  removeElementById(selectedElementId.value);
}

function removeElementById(elementId?: string) {
  if (!currentDraft.value || !elementId) return;
  currentDraft.value.elements = currentDraft.value.elements.filter((item) => item.id !== elementId);
  selectedElementId.value = currentDraft.value.elements[0]?.id || '';
}

function copySelectedElement() {
  if (!currentDraft.value || !selectedElement.value) return;
  const copy = {
    ...JSON.parse(JSON.stringify(selectedElement.value)),
    id: buildElementId(selectedElement.value.type),
    name: `${selectedElement.value.name}副本`,
    x: roundMm(selectedElement.value.x + 3),
    y: roundMm(selectedElement.value.y + 3),
  } as DesignerElement;
  currentDraft.value.elements.push(copy);
  selectedElementId.value = copy.id;
}

function moveSelectedElement(direction: 'backward' | 'forward') {
  if (!currentDraft.value || !selectedElementId.value) return;
  const list = currentDraft.value.elements;
  const index = list.findIndex((item) => item.id === selectedElementId.value);
  if (index < 0) return;
  const targetIndex = direction === 'forward' ? Math.min(list.length - 1, index + 1) : Math.max(0, index - 1);
  if (targetIndex === index) return;
  const [item] = list.splice(index, 1);
  list.splice(targetIndex, 0, item!);
}

function getElementTypeLabel(type: DesignerElementType) {
  return elementTypeOptions.find((item) => item.value === type)?.label || type;
}

function getElementTypeIcon(type: DesignerElementType) {
  return elementTypeOptions.find((item) => item.value === type)?.icon || 'lucide:square';
}

function getElementRowClassName(record: DesignerElement) {
  return record.id === selectedElementId.value ? 'is-selected-element-row' : '';
}

function getReferenceOpacity() {
  return showReference.value ? referenceOpacity.value : 0;
}

function revokeCanvasRenderImageUrl() {
  if (canvasRenderImageUrl.value) {
    URL.revokeObjectURL(canvasRenderImageUrl.value);
    canvasRenderImageUrl.value = '';
  }
}

function scheduleCanvasRender(immediate = false) {
  if (canvasRenderTimer !== undefined) {
    window.clearTimeout(canvasRenderTimer);
  }
  if (!pythonCanvasEnabled.value || !designerVisible.value || !currentDraft.value) {
    canvasRenderRequestId += 1;
    canvasRenderLoading.value = false;
    canvasRenderError.value = '';
    revokeCanvasRenderImageUrl();
    return;
  }
  canvasRenderTimer = window.setTimeout(
    () => void requestCanvasRender(),
    immediate ? 0 : 180,
  );
}

async function requestCanvasRender() {
  if (!pythonCanvasEnabled.value || !designerVisible.value || !currentDraft.value) return;
  const requestId = ++canvasRenderRequestId;
  const endpoint = canvasViewMode.value === 'design' ? '/render-design' : '/render';
  canvasRenderLoading.value = true;
  canvasRenderError.value = '';
  try {
    const response = await fetch(`${PRINT_AGENT_URL}${endpoint}`, {
      body: JSON.stringify(buildAgentRequestPayload()),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
    if (!response.ok) {
      const result = await response.json().catch(() => ({}));
      throw new Error(result?.message || `MVP画布渲染失败：${response.status}`);
    }
    const blob = await response.blob();
    if (requestId !== canvasRenderRequestId) return;
    const nextUrl = URL.createObjectURL(blob);
    revokeCanvasRenderImageUrl();
    canvasRenderImageUrl.value = nextUrl;
  } catch (error) {
    if (requestId !== canvasRenderRequestId) return;
    revokeCanvasRenderImageUrl();
    canvasRenderError.value =
      error instanceof Error ? error.message : '请确认本地 MVP 打印服务已启动';
  } finally {
    if (requestId === canvasRenderRequestId) {
      canvasRenderLoading.value = false;
    }
  }
}

function handlePythonCanvasToggle(enabled: boolean | number | string) {
  if (enabled !== true) {
    canvasViewMode.value = 'design';
    scheduleCanvasRender(true);
    return;
  }
  void checkPrintAgentHealth(true);
  scheduleCanvasRender(true);
}

async function requestAgentPreview() {
  if (!currentDraft.value) return;
  if (!localPrinters.value.length) {
    void loadLocalPrinters(true);
  }
  agentLoading.value = true;
  try {
    revokeAgentPreviewImageUrl();
    const response = await fetch(`${PRINT_AGENT_URL}/render`, {
      body: JSON.stringify(buildAgentRequestPayload()),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error(await response.text());
    }
    const blob = await response.blob();
    agentPreviewImageUrl.value = URL.createObjectURL(blob);
    agentPreviewVisible.value = true;
  } catch (error) {
    message.error(`本地预览失败：${error instanceof Error ? error.message : '请确认本地打印服务已启动'}`);
  } finally {
    agentLoading.value = false;
  }
}

async function sendTestPrint() {
  if (!currentDraft.value) return;
  const printerName = selectedPrinterName.value;
  const outputMode = selectedOutputMode.value;
  agentLoading.value = true;
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/print`, {
      body: JSON.stringify(buildAgentRequestPayload()),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || '打印服务返回失败');
    }
    if (outputMode === 'windows_raw') {
      message.success(`测试打印已发送到本地打印机：${result.target || printerName}`);
    } else {
      message.success(`MVP同源预览文件已生成：${result.outputFile || 'visual_print_outputs'}`);
      await requestAgentPreview();
    }
  } catch (error) {
    message.error(`生成测试打印失败：${error instanceof Error ? error.message : '请确认本地打印服务已启动'}`);
  } finally {
    agentLoading.value = false;
  }
}

async function loadLocalPrinters(silent = false) {
  printerLoading.value = true;
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/printers`);
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || '读取本地打印机失败');
    }
    printAgentStatus.value = 'online';
    printAgentError.value = '';
    const printers = Array.isArray(result.printers) ? result.printers : [];
    localPrinters.value = printers
      .map((item: LocalPrinter) => ({
        dpi: Number(item.dpi || 0) || undefined,
        dpiSource: item.dpiSource,
        isDefault: Boolean(item.isDefault),
        name: String(item.name || '').trim(),
      }))
      .filter((item: LocalPrinter) => item.name);
    const configuredPrinter = String(result.selectedPrinterName || '').trim();
    if (configuredPrinter && isPdfPrinterName(configuredPrinter)) {
      selectedPrintTarget.value = FILE_PRINT_TARGET;
    } else if (configuredPrinter && localPrinters.value.some((item) => item.name === configuredPrinter)) {
      selectedPrintTarget.value = configuredPrinter;
    } else if (
      selectedPrintTarget.value !== FILE_PRINT_TARGET &&
      !localPrinters.value.some((item) => item.name === selectedPrintTarget.value)
    ) {
      selectedPrintTarget.value = FILE_PRINT_TARGET;
    }
    if (!silent) {
      message.success(`已读取本地打印机：${localPrinters.value.length} 台`);
    }
  } catch (error) {
    printAgentStatus.value = 'offline';
    printAgentError.value = error instanceof Error ? error.message : '本地打印服务未响应';
    if (!silent) {
      message.warning(`读取本地打印机失败：${error instanceof Error ? error.message : '请确认本地打印服务已启动'}`);
    }
  } finally {
    printerLoading.value = false;
  }
}

async function checkPrintAgentHealth(silent = false) {
  printAgentStatus.value = 'checking';
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), 2500);
  try {
    const response = await fetch(`${PRINT_AGENT_URL}/health`, { signal: controller.signal });
    const result = await response.json();
    if (!response.ok || result?.success === false) {
      throw new Error(result?.message || '本地打印服务未响应');
    }
    printAgentStatus.value = 'online';
    printAgentVersion.value = String(result.service || '');
    printAgentError.value = '';
    await loadLocalPrinters(true);
    if (!silent) {
      message.success('本地打印服务已连接');
    }
  } catch (error) {
    printAgentStatus.value = 'offline';
    printAgentVersion.value = '';
    printAgentError.value = error instanceof Error ? error.message : '本地打印服务未启动';
    localPrinters.value = [];
    if (!silent) {
      message.warning('未检测到本地打印服务，请先启动或下载安装包');
    }
  } finally {
    window.clearTimeout(timer);
  }
}

async function handlePrintTargetChange(value: string) {
  const printerName = value === FILE_PRINT_TARGET ? '' : value;
  try {
    await fetch(`${PRINT_AGENT_URL}/config`, {
      body: JSON.stringify({
        printMode: printerName ? 'windows_raw' : 'file',
        printerName,
      }),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
    });
  } catch {
    // 本地打印代理未启动时不阻断设计器使用，打印动作会给出明确错误。
  }
}

function handlePrintTargetDropdownVisibleChange(open: boolean) {
  if (open && !localPrinters.value.length) {
    void loadLocalPrinters(true);
  }
}

function buildAgentRequestPayload() {
  const printerName = selectedPrinterName.value;
  return {
    ...buildAgentPayload(),
    outputMode: selectedOutputMode.value,
    printerDpi: selectedPrinterProfile.value?.dpi,
    printerName,
  };
}

function isPdfPrinterName(name: string) {
  return /pdf|xps|onenote/i.test(name);
}

function revokeAgentPreviewImageUrl() {
  if (agentPreviewImageUrl.value) {
    URL.revokeObjectURL(agentPreviewImageUrl.value);
    agentPreviewImageUrl.value = '';
  }
}

function buildMvpRendererElement(element: DesignerElement): Record<string, unknown> {
  const common = {
    enabled: element.enabled !== false,
    h: element.height,
    id: element.id,
    w: element.width,
    x: element.x,
    y: element.y,
  };
  if (element.type === 'field' || element.type === 'text') {
    const text = element.type === 'field' && element.fieldKey ? `{{${element.fieldKey}}}` : element.text || '';
    return {
      ...common,
      align: element.align || 'left',
      fill: element.fill || '#111111',
      fit: element.fit !== false,
      font: element.fontFamily || (element.bold ? 'bold' : 'regular'),
      font_size: Number(element.fontSize || 3),
      line_spacing: Number(element.lineSpacing || 0),
      min_font_size: Number(element.minFontSize || 1),
      text,
      type: 'text',
      valign: element.valign || 'top',
    };
  }
  if (element.type === 'barcode') {
    return {
      ...common,
      fill: element.fill || '#111111',
      human_font_size: Number(element.humanFontSize || 1.2),
      human_readable: element.humanReadable !== false,
      quiet_mm: Number(element.quietMm || 0.4),
      type: 'barcode128',
      value: element.fieldKey ? `{{${element.fieldKey}}}` : element.text || '',
    };
  }
  if (element.type === 'rect') {
    return {
      ...common,
      fill: element.fill || undefined,
      stroke: element.stroke || '#111111',
      stroke_width: Number(element.strokeWidth ?? 0.4),
      type: 'rect',
    };
  }
  if (element.type === 'line') {
    return {
      ...common,
      stroke: element.stroke || '#111111',
      stroke_width: Number(element.strokeWidth ?? 0.2),
      type: 'line',
    };
  }
  throw new Error(`Python MVP渲染器暂不支持元素类型：${element.type}`);
}

function buildMvpRendererTemplate(draft: DesignerDraft) {
  const printableElements = draft.elements.filter((item) => !item.guideOnly).map(buildMvpRendererElement);
  return {
    calibration: {
      content_offset_mm: {
        x: Number(draft.contentOffsetXmm || 0),
        y: Number(draft.contentOffsetYmm || 0),
      },
    },
    elements: printableElements.length
      ? printableElements
      : [{ enabled: false, h: 0, id: '__empty_design__', stroke: '#FFFFFF', stroke_width: 0, type: 'rect', w: 0, x: 0, y: 0 }],
    fonts: {
      bold: ['system:bold'],
      heavy: ['system:bold'],
      latin_bold: ['system:bold'],
      regular: ['system:regular'],
    },
    meta: {
      schema_version: DRAFT_SCHEMA_VERSION,
      coordinate_contract: 'design-mm-to-physical-media',
      template_code: draft.id,
      template_name: draft.labelName,
    },
    page: {
      background: draft.background || '#FFFFFF',
      design_height_mm: Number(draft.designHeightMm || draft.heightMm),
      design_width_mm: Number(draft.designWidthMm || draft.widthMm),
      dpi: Number(draft.dpi || 300),
      height_mm: Number(draft.heightMm),
      output_rotation_degrees: normalizePrintRotation(draft.printRotation),
      output_rotation_direction: draft.rotationDirection || 'clockwise',
      preserve_page_size_after_rotation: draft.preservePageSizeAfterRotation !== false,
      rotated_content_origin_mm: {
        x: Number(draft.rotatedContentOriginXmm || 0),
        y: Number(draft.rotatedContentOriginYmm || 0),
      },
      threshold: Number(draft.threshold || 188),
      width_mm: Number(draft.widthMm),
    },
  };
}

function buildAgentPayload() {
  const draft = currentDraft.value!;
  return {
    copies: 1,
    data: activePrintData.value,
    meta: {
      customer: selectedRow.value?.customer,
      imageFile: draft.imageFile,
      imageId: draft.imageId,
      productType: selectedRow.value?.productType,
      sizeMm: selectedRow.value?.sizeMm,
    },
    rendererTemplate: buildMvpRendererTemplate(draft),
    requestId: `WEB-${Date.now()}`,
    templateName: draft.labelName,
  };
}

function buildFullApiUrl(path: string) {
  const baseUrl = apiURL || '/admin-api';
  const normalizedBase = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
  if (/^https?:\/\//i.test(normalizedBase)) {
    return `${normalizedBase}${path}`;
  }
  return new URL(`${normalizedBase}${path}`, globalThis.location.origin).toString();
}

function roundMm(value: number) {
  return Math.round(value * 1000) / 1000;
}

function normalizePrintRotation(value: unknown): PrintRotation {
  const rotation = Number(value);
  return rotation === 90 || rotation === 180 || rotation === 270 ? rotation : 0;
}

function resolveTemplateLinkedItems(row?: CustomerInfo, design?: MesHcVisualPrintDesignerApi.Design) {
  if (design?.linkedProductItems?.length) {
    return design.linkedProductItems;
  }
  if (!row || (!row.productItemId && !row.productType && !row.sizeMm)) {
    return [];
  }
  return [
    {
      customerInfoId: row.id,
      id: row.productItemId,
      productType: row.productType,
      sizeMm: row.sizeMm,
      sourceRow: row.sourceRow,
    },
  ];
}

function formatProductModelSize(item?: MesHcVisualPrintDesignerApi.ProductItem) {
  if (!item) return '';
  return [item.productType, item.sizeMm].filter(Boolean).join('/');
}

function formatText(value?: number | string) {
  if (value === undefined || value === null) return '';
  return String(value).trim();
}
</script>

<template>
  <Page auto-content-height content-class="visual-print-page">
    <section class="filter-panel">
      <Form class="filter-form" layout="vertical">
        <FormItem label="客户">
          <Input v-model:value="queryForm.customer" allow-clear placeholder="客户编码/名称" @press-enter="handleSearch" />
        </FormItem>
        <FormItem label="产品类型">
          <Input v-model:value="queryForm.productType" allow-clear placeholder="DTP0201" @press-enter="handleSearch" />
        </FormItem>
        <FormItem label="尺寸/mm">
          <Input v-model:value="queryForm.sizeMm" allow-clear placeholder="775" @press-enter="handleSearch" />
        </FormItem>
        <FormItem label="标签类型">
          <Select
            v-model:value="queryForm.labelKind"
            allow-clear
            :options="labelKindOptions"
            placeholder="全部标签"
          />
        </FormItem>
        <FormItem class="filter-actions" label=" ">
          <Space>
            <Button type="primary" @click="handleSearch">
              <template #icon><IconifyIcon icon="lucide:search" /></template>
              查询
            </Button>
            <Button @click="handleReset">
              <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
              重置
            </Button>
          </Space>
        </FormItem>
      </Form>
    </section>

    <Alert
      v-if="printAgentStatus === 'offline'"
      class="print-agent-alert"
      show-icon
      type="warning"
    >
      <template #message>未检测到本地打印服务</template>
      <template #description>
        <span>
          当前页面需要连接本机 {{ PRINT_AGENT_URL }} 才能读取打印机、生成预览和打印。
          {{ printAgentError ? `检测结果：${printAgentError}` : '请确认本地打印服务已启动。' }}
        </span>
      </template>
      <template #action>
        <Space>
          <Button size="small" :loading="printAgentStatus === 'checking'" @click="checkPrintAgentHealth(false)">
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            重新检测
          </Button>
          <Button size="small" type="primary" :href="printAgentDownloadUrl" target="_blank">
            <template #icon><IconifyIcon icon="lucide:download" /></template>
            下载本地打印服务
          </Button>
        </Space>
      </template>
    </Alert>

    <section class="summary-bar">
      <div>
        <strong>客户打印信息</strong>
        <span>共 {{ customerTotal }} 行，同客户同标签参考图自动共用设计稿</span>
      </div>
      <Space>
        <Upload v-bind="excelUploadProps">
          <Button :loading="importLoading">
            <template #icon><IconifyIcon icon="lucide:upload" /></template>
            导入Excel
          </Button>
        </Upload>
        <Button @click="openCreateCustomer">
          <template #icon><IconifyIcon icon="lucide:plus" /></template>
          新建
        </Button>
        <Button :loading="customerLoading" @click="handleSearch">
          <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
          刷新
        </Button>
      </Space>
    </section>

    <CustomerGrid class="customer-vben-grid">
      <template #productType="{ row }">
        <Tag color="blue">{{ row.productType || '-' }}</Tag>
      </template>
      <template #sizeMm="{ row }">
        <Tag color="cyan">{{ row.sizeMm || '-' }}mm</Tag>
      </template>
      <template #customerSideSize="{ row }">
        <Tag v-if="row.customerSideSize" color="purple">{{ row.customerSideSize }}</Tag>
        <span v-else>-</span>
      </template>
      <template #padBackPreview="{ row }">
        <button class="grid-preview-button" type="button" @click="openDesigner(row, 'padBack')">
          <span class="grid-preview-badges">
            <span :class="['grid-preview-status', getLabelPreviewStatusClass(row, 'padBack')]">
              {{ getLabelPreviewStatus(row, 'padBack') }}
            </span>
            <span v-if="getLabelSharedCount(row, 'padBack') > 1" class="grid-preview-share">
              共{{ getLabelSharedCount(row, 'padBack') }}
            </span>
          </span>
          <span class="grid-preview-thumb">
            <img
              v-if="resolveVisualPrintImageUrl(getLabelImageFile(row, 'padBack'))"
              :alt="visualLabelKinds[0]?.label"
              :src="resolveVisualPrintImageUrl(getLabelImageFile(row, 'padBack'))"
            />
            <span v-else>-</span>
          </span>
        </button>
      </template>
      <template #cleanBagPreview="{ row }">
        <button class="grid-preview-button" type="button" @click="openDesigner(row, 'cleanBag')">
          <span class="grid-preview-badges">
            <span :class="['grid-preview-status', getLabelPreviewStatusClass(row, 'cleanBag')]">
              {{ getLabelPreviewStatus(row, 'cleanBag') }}
            </span>
            <span v-if="getLabelSharedCount(row, 'cleanBag') > 1" class="grid-preview-share">
              共{{ getLabelSharedCount(row, 'cleanBag') }}
            </span>
          </span>
          <span class="grid-preview-thumb">
            <img
              v-if="resolveVisualPrintImageUrl(getLabelImageFile(row, 'cleanBag'))"
              :alt="visualLabelKinds[1]?.label"
              :src="resolveVisualPrintImageUrl(getLabelImageFile(row, 'cleanBag'))"
            />
            <span v-else>-</span>
          </span>
        </button>
      </template>
      <template #boxFrontPreview="{ row }">
        <button class="grid-preview-button" type="button" @click="openDesigner(row, 'boxFront')">
          <span class="grid-preview-badges">
            <span :class="['grid-preview-status', getLabelPreviewStatusClass(row, 'boxFront')]">
              {{ getLabelPreviewStatus(row, 'boxFront') }}
            </span>
            <span v-if="getLabelSharedCount(row, 'boxFront') > 1" class="grid-preview-share">
              共{{ getLabelSharedCount(row, 'boxFront') }}
            </span>
          </span>
          <span class="grid-preview-thumb">
            <img
              v-if="resolveVisualPrintImageUrl(getLabelImageFile(row, 'boxFront'))"
              :alt="visualLabelKinds[2]?.label"
              :src="resolveVisualPrintImageUrl(getLabelImageFile(row, 'boxFront'))"
            />
            <span v-else>-</span>
          </span>
        </button>
      </template>
      <template #customerSidePreview="{ row }">
        <button class="grid-preview-button" type="button" @click="openDesigner(row, 'customerSide')">
          <span class="grid-preview-badges">
            <span :class="['grid-preview-status', getLabelPreviewStatusClass(row, 'customerSide')]">
              {{ getLabelPreviewStatus(row, 'customerSide') }}
            </span>
            <span v-if="getLabelSharedCount(row, 'customerSide') > 1" class="grid-preview-share">
              共{{ getLabelSharedCount(row, 'customerSide') }}
            </span>
          </span>
          <span class="grid-preview-thumb">
            <img
              v-if="resolveVisualPrintImageUrl(getLabelImageFile(row, 'customerSide'))"
              :alt="visualLabelKinds[3]?.label"
              :src="resolveVisualPrintImageUrl(getLabelImageFile(row, 'customerSide'))"
            />
            <span v-else>-</span>
          </span>
        </button>
      </template>
      <template #coa="{ row }">
        <div class="grid-coa-cell">
          <span>纸质 {{ row.needPaperCoa || '-' }}</span>
          <span>ECOA {{ row.needEcoa || '-' }}</span>
        </div>
      </template>
      <template #customerActions="{ row }">
        <div class="grid-action-cell">
          <Tooltip title="维护">
            <Button class="grid-action-button" size="small" type="text" @click="openEditCustomer(row)">
              <template #icon><IconifyIcon icon="lucide:edit" /></template>
            </Button>
          </Tooltip>
          <Tooltip title="删除">
            <Button
              class="grid-action-button"
              danger
              size="small"
              type="text"
              @click="confirmDeleteCustomer(row)"
            >
              <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
            </Button>
          </Tooltip>
        </div>
      </template>
    </CustomerGrid>

    <Modal
      v-model:open="customerModalVisible"
      :confirm-loading="customerSaving"
      :title="editingCustomerId ? '维护客户打印信息' : '新建客户打印信息'"
      width="100%"
      wrap-class-name="customer-print-fullscreen-modal"
      @ok="saveCustomerInfo"
    >
      <div class="customer-print-modal">
        <Tabs v-model:active-key="customerModalActiveKey" class="customer-print-modal__tabs">
          <Tabs.TabPane key="base" tab="基本信息">
            <div class="customer-print-modal__pane customer-print-modal__pane--base">
              <Form :label-col="{ style: { width: '158px' } }" size="small">
                <div class="customer-form-grid">
                  <FormItem label="状态">
                    <Select
                      v-model:value="customerForm.status"
                      :options="[
                        { label: '启用', value: 0 },
                        { label: '停用', value: 1 },
                      ]"
                    />
                  </FormItem>
                  <FormItem
                    v-for="item in customerFormItems"
                    :key="item.key"
                    :class="{ 'is-full': item.span === 'full' }"
                    :label="item.label"
                  >
                    <Textarea
                      v-if="item.span === 'full'"
                      v-model:value="customerForm[item.key]"
                      :auto-size="{ minRows: 2, maxRows: 4 }"
                    />
                    <Input v-else v-model:value="customerForm[item.key]" allow-clear />
                  </FormItem>
                </div>
              </Form>
            </div>
          </Tabs.TabPane>
          <Tabs.TabPane key="products" tab="产品型号尺寸" force-render>
            <div class="customer-print-modal__pane customer-print-modal__pane--products">
              <div class="customer-product-title">
                <strong>产品型号尺寸</strong>
                <Button size="small" type="primary" @click="addCustomerProductItem">
                  <template #icon><IconifyIcon icon="lucide:plus" /></template>
                  新增
                </Button>
              </div>
              <div class="customer-product-table-wrap">
                <ATable
                  bordered
                  class="customer-product-table"
                  :columns="customerProductColumns"
                  :data-source="customerForm.productItems || []"
                  :pagination="false"
                  :row-key="getCustomerProductRowKey"
                  :scroll="{ y: 520 }"
                  size="small"
                >
                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'productType'">
                      <Input v-model:value="record.productType" allow-clear placeholder="DTP0201" />
                    </template>
                    <template v-else-if="column.dataIndex === 'sizeMm'">
                      <Input v-model:value="record.sizeMm" allow-clear placeholder="775" />
                    </template>
                    <template v-else-if="column.dataIndex === 'actions'">
                      <Tooltip title="删除">
                        <Button danger size="small" type="text" @click="removeCustomerProductItem(index)">
                          <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                        </Button>
                      </Tooltip>
                    </template>
                  </template>
                </ATable>
              </div>
            </div>
          </Tabs.TabPane>
        </Tabs>
      </div>
    </Modal>

    <Modal
      v-model:open="designerVisible"
      destroy-on-close
      :footer="null"
      :title="currentDraft ? `${currentDraft.customer} / ${currentDraft.labelName}` : '可视化打印设计器'"
      width="100%"
      wrap-class-name="visual-print-fullscreen-modal"
    >
      <div v-if="currentDraft" class="designer-shell">
        <div class="designer-topbar">
          <div class="designer-title-block">
            <div class="designer-title-line">
              <strong>{{ currentDraft.customer }}</strong>
              <span>
                {{ selectedRow?.productType || '-' }} / {{ selectedRow?.sizeMm || '-' }}mm /
                {{ currentDraft.labelName }}
              </span>
            </div>
            <Tag class="designer-template-status" :color="templateStatusColor">{{ templateStatusText }}</Tag>
            <Tooltip v-if="templateLinkedProductLabels.length" :title="templateLinkedProductTitle">
              <div class="designer-linked-products">
                <span class="linked-products-label">挂接型号尺寸</span>
                <div class="linked-products-tags">
                  <Tag
                    v-for="(label, index) in templateLinkedProductLabels.slice(0, 5)"
                    :key="`${label}-${index}`"
                    color="geekblue"
                  >
                    {{ label }}
                  </Tag>
                  <Tag v-if="templateLinkedProductLabels.length > 5" color="default">
                    +{{ templateLinkedProductLabels.length - 5 }}
                  </Tag>
                </div>
              </div>
            </Tooltip>
          </div>
          <Space>
            <Switch v-model:checked="showReference" checked-children="参考图" un-checked-children="参考图" />
            <Button @click="openAttachDesigner">
              <template #icon><IconifyIcon icon="lucide:link" /></template>
              挂接模板
            </Button>
            <Button @click="resetCurrentDraft">
              <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
              重置
            </Button>
            <Button type="primary" @click="saveCurrentDraft">
              <template #icon><IconifyIcon icon="lucide:save" /></template>
              保存设计稿
            </Button>
          </Space>
        </div>

        <div class="designer-main" :class="{ 'is-loading': designerLoading }">
          <aside class="designer-left">
            <div class="tool-panel designer-left-tabs-panel">
              <Tabs v-model:active-key="designerLeftActiveKey" class="designer-left-tabs" size="small">
                <Tabs.TabPane key="templates" tab="模板">
                  <div class="designer-tab-pane designer-template-panel">
                    <div class="template-panel-head">
                      <div>
                        <strong>{{ getLabelKindLabel(selectedKind) }}模板</strong>
                        <span>{{ designerTemplateList.length }} 条</span>
                      </div>
                      <Button size="small" type="primary" @click="createDesignerTemplateDraft">
                        <template #icon><IconifyIcon icon="lucide:plus" /></template>
                        新增
                      </Button>
                    </div>
                    <div v-if="currentDraft?.forceNew" class="designer-template-card is-active is-draft">
                      <div class="designer-template-card-main">
                        <strong>{{ currentDraft.labelName }}</strong>
                        <span>新模板 / 未保存</span>
                      </div>
                      <Tag color="orange">草稿</Tag>
                    </div>
                    <div v-if="designerTemplateLoading" class="designer-template-loading">加载中...</div>
                    <div v-else-if="designerTemplateList.length" class="designer-template-list">
                      <button
                        v-for="item in designerTemplateList"
                        :key="getDesignerTemplateKey(item)"
                        class="designer-template-card"
                        :class="{ 'is-active': isActiveDesignerTemplate(item) }"
                        type="button"
                        @click="selectDesignerTemplate(item)"
                      >
                        <div class="designer-template-card-main">
                          <strong>{{ getDesignerTemplateTitle(item) }}</strong>
                          <span>{{ getDesignerTemplateSubTitle(item) }}</span>
                          <small :title="getDesignerTemplateImageName(item)">
                            {{ getDesignerTemplateImageName(item) }}
                          </small>
                        </div>
                        <Tag :color="getDesignerTemplateModeColor(item)">
                          {{ getDesignerTemplateModeText(item) }}
                        </Tag>
                      </button>
                    </div>
                    <Empty
                      v-else-if="!currentDraft?.forceNew"
                      :image="Empty.PRESENTED_IMAGE_SIMPLE"
                      description="暂无已保存模板"
                    />
                  </div>
                </Tabs.TabPane>
                <Tabs.TabPane key="elements" tab="元素">
                  <div class="designer-tab-pane designer-element-panel">
                    <div class="tool-grid">
                      <Button v-for="tool in elementTypeOptions" :key="tool.value" @click="addElement(tool.value)">
                        <template #icon><IconifyIcon :icon="tool.icon" /></template>
                        {{ tool.label }}
                      </Button>
                    </div>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="canvas" tab="画布">
                  <div class="designer-tab-pane designer-canvas-panel">
                    <Form :label-col="{ style: { width: '70px' } }" size="small">
                      <FormItem label="标签">
                        <Select
                          v-model:value="selectedKind"
                          :options="labelKindOptions"
                          @change="() => selectedRow && openDesigner(selectedRow, selectedKind)"
                        />
                      </FormItem>
                      <FormItem label="纸宽/mm">
                        <InputNumber v-model:value="currentDraft.widthMm" :min="20" :precision="3" class="full-input" />
                      </FormItem>
                      <FormItem label="纸高/mm">
                        <InputNumber
                          v-model:value="currentDraft.heightMm"
                          :min="20"
                          :precision="3"
                          class="full-input"
                        />
                      </FormItem>
                      <FormItem label="设计宽/mm">
                        <InputNumber
                          v-model:value="currentDraft.designWidthMm"
                          :min="1"
                          :precision="3"
                          class="full-input"
                          @focus="captureDesignSizeBeforeChange"
                          @change="handleDesignSizeChange"
                        />
                      </FormItem>
                      <FormItem label="设计高/mm">
                        <InputNumber
                          v-model:value="currentDraft.designHeightMm"
                          :min="1"
                          :precision="3"
                          class="full-input"
                          @focus="captureDesignSizeBeforeChange"
                          @change="handleDesignSizeChange"
                        />
                      </FormItem>
                      <FormItem label="尺寸联动">
                        <Switch
                          v-model:checked="currentDraft.scaleElementsWithDesignSize"
                          checked-children="元素同比缩放"
                          un-checked-children="仅改画布"
                        />
                      </FormItem>
                      <FormItem label="DPI">
                        <Select
                          v-model:value="currentDraft.dpi"
                          :options="[
                            { label: '203 dpi', value: 203 },
                            { label: '300 dpi', value: 300 },
                            { label: '600 dpi', value: 600 },
                          ]"
                        />
                      </FormItem>
                      <FormItem label="旋转角度">
                        <Select v-model:value="currentDraft.printRotation" :options="printRotationOptions" />
                      </FormItem>
                      <FormItem label="旋转方向">
                        <Select v-model:value="currentDraft.rotationDirection" :options="rotationDirectionOptions" />
                      </FormItem>
                      <FormItem label="保持纸张">
                        <Switch
                          v-model:checked="currentDraft.preservePageSizeAfterRotation"
                          checked-children="是"
                          un-checked-children="否"
                        />
                      </FormItem>
                      <FormItem label="介质匹配">
                        <Button block @click="syncPhysicalMediaToRotation">
                          <template #icon><IconifyIcon icon="lucide:scan-line" /></template>
                          按设计尺寸和方向匹配
                        </Button>
                      </FormItem>
                      <FormItem label="坐标校验">
                        <Alert
                          v-if="printGeometry"
                          :message="printGeometry.warnings.length
                            ? printGeometry.warnings.join('；')
                            : `旋转内容 ${printGeometry.rotatedWidthMm}×${printGeometry.rotatedHeightMm}mm，最终介质 ${printGeometry.outputWidthMm}×${printGeometry.outputHeightMm}mm`"
                          :type="printGeometry.warnings.length ? 'error' : 'success'"
                          show-icon
                        />
                      </FormItem>
                      <div class="prop-grid">
                        <FormItem label="旋转原点X">
                          <InputNumber
                            v-model:value="currentDraft.rotatedContentOriginXmm"
                            :precision="3"
                            class="full-input"
                          />
                        </FormItem>
                        <FormItem label="旋转原点Y">
                          <InputNumber
                            v-model:value="currentDraft.rotatedContentOriginYmm"
                            :precision="3"
                            class="full-input"
                          />
                        </FormItem>
                        <FormItem label="内容偏移X">
                          <InputNumber
                            v-model:value="currentDraft.contentOffsetXmm"
                            :precision="3"
                            class="full-input"
                          />
                        </FormItem>
                        <FormItem label="内容偏移Y">
                          <InputNumber
                            v-model:value="currentDraft.contentOffsetYmm"
                            :precision="3"
                            class="full-input"
                          />
                        </FormItem>
                      </div>
                      <FormItem label="二值阈值">
                        <InputNumber
                          v-model:value="currentDraft.threshold"
                          :max="255"
                          :min="0"
                          :precision="0"
                          class="full-input"
                        />
                      </FormItem>
                      <FormItem label="渲染引擎">
                        <Tag :color="pythonCanvasEnabled ? 'green' : 'blue'">
                          {{ pythonCanvasEnabled ? 'Python MVP 精确画布' : 'Web 本地设计画布' }}
                        </Tag>
                      </FormItem>
                      <FormItem label="MVP模板">
                        <Upload
                          accept=".json,application/json"
                          :before-upload="handleMvpTemplateImport"
                          :show-upload-list="false"
                        >
                          <Button block>
                            <template #icon><IconifyIcon icon="lucide:file-up" /></template>
                            导入Python MVP JSON
                          </Button>
                        </Upload>
                      </FormItem>
                      <FormItem label="PRN坐标">
                        <div class="prn-import-control">
                          <InputNumber
                            v-model:value="prnImportDpi"
                            :min="72"
                            :max="1200"
                            :precision="0"
                            addon-after="dpi"
                          />
                          <Upload
                            accept=".prn,.zpl,text/plain"
                            :before-upload="handlePrnCoordinateImport"
                            :show-upload-list="false"
                          >
                            <Button :loading="prnImportLoading">
                              <template #icon><IconifyIcon icon="lucide:scan-search" /></template>
                              导入坐标
                            </Button>
                          </Upload>
                        </div>
                        <div v-if="currentDraft.prnSourceFileName" class="reference-name">
                          {{ currentDraft.prnSourceFileName }} / {{ currentDraft.prnSourceDpi }}dpi
                        </div>
                      </FormItem>
                      <FormItem v-if="mvpPaperPresetOptions.length" label="MVP规格">
                        <Select
                          v-model:value="selectedMvpPaperPresetId"
                          :options="mvpPaperPresetOptions"
                          @change="applyImportedMvpPaperPreset"
                        />
                      </FormItem>
                      <FormItem label="参考图">
                        <div class="reference-maintain">
                          <div class="reference-size">{{ selectedVariantImageSize || '无图片尺寸' }}</div>
                          <div class="reference-name" :title="selectedReferenceImageName">
                            {{ selectedReferenceImageName || '未维护参考图' }}
                          </div>
                          <Space class="reference-actions">
                            <Upload v-bind="referenceUploadProps">
                              <Button size="small">
                                <template #icon><IconifyIcon icon="lucide:upload" /></template>
                                上传
                              </Button>
                            </Upload>
                            <Button
                              size="small"
                              :disabled="!currentDraft.referenceImageDataUrl && !selectedVariant?.imageUrl"
                              @click="restoreDefaultReferenceImage"
                            >
                              <template #icon><IconifyIcon icon="lucide:image" /></template>
                              恢复
                            </Button>
                          </Space>
                        </div>
                      </FormItem>
                      <FormItem label="透明度">
                        <InputNumber
                          v-model:value="referenceOpacity"
                          :max="1"
                          :min="0"
                          :precision="2"
                          :step="0.05"
                          class="full-input"
                        />
                      </FormItem>
                      <FormItem label="校准">
                        <Button
                          block
                          :disabled="!getDefaultMvpPresetId(selectedKind)"
                          @click="restoreCurrentMvpPaperPreset"
                        >
                          <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
                          恢复MVP规格
                        </Button>
                      </FormItem>
                    </Form>
                  </div>
                </Tabs.TabPane>

                <Tabs.TabPane key="data" tab="数据源">
                  <div class="designer-tab-pane designer-data-panel">
                    <div class="field-list">
                      <div
                        v-for="item in dataFieldOptions"
                        :key="item.value"
                        class="field-source-row"
                        draggable="true"
                        @dragstart="startFieldDrag($event, item, 'field')"
                      >
                        <button
                          class="field-source-main"
                          type="button"
                          :title="item.label"
                          @click="addFieldElementFromSource(item, 'field')"
                        >
                          {{ item.label }}
                        </button>
                        <div class="field-source-actions">
                          <Tooltip title="字段文本">
                            <Button size="small" type="text" @click.stop="addFieldElementFromSource(item, 'field')">
                              <template #icon><IconifyIcon icon="lucide:braces" /></template>
                            </Button>
                          </Tooltip>
                          <Tooltip title="一维码">
                            <Button size="small" type="text" @click.stop="addFieldElementFromSource(item, 'barcode')">
                              <template #icon><IconifyIcon icon="lucide:barcode" /></template>
                            </Button>
                          </Tooltip>
                        </div>
                      </div>
                    </div>
                  </div>
                </Tabs.TabPane>
              </Tabs>
            </div>
          </aside>

          <section class="designer-center">
            <div class="canvas-toolbar">
              <div>
                <strong>设计画布</strong>
                <span>
                  {{
                    canvasViewMode === 'design'
                      ? (pythonCanvasEnabled ? 'Python精确设计层（可编辑）' : 'Web本地设计层（可编辑）')
                      : '最终打印方向（只读）'
                  }}，单位 mm，当前缩放 {{ canvasScale.toFixed(2) }}
                </span>
              </div>
              <Space>
                <span class="python-canvas-toggle">
                  <span>Python精确画布</span>
                  <Switch
                    v-model:checked="pythonCanvasEnabled"
                    checked-children="开"
                    un-checked-children="关"
                    @change="handlePythonCanvasToggle"
                  />
                </span>
                <Select
                  v-model:value="canvasViewMode"
                  class="canvas-view-select"
                  :disabled="!pythonCanvasEnabled"
                  :options="canvasViewModeOptions"
                />
                <Select
                  v-model:value="selectedPrintTarget"
                  class="print-target-select"
                  :loading="printerLoading"
                  :options="printTargetOptions"
                  @dropdown-visible-change="handlePrintTargetDropdownVisibleChange"
                  @change="handlePrintTargetChange"
                />
                <Tooltip title="刷新本地打印机">
                  <Button :loading="printerLoading" @click="loadLocalPrinters(false)">
                    <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
                  </Button>
                </Tooltip>
                <Button :loading="agentLoading" @click="requestAgentPreview">
                  <template #icon><IconifyIcon icon="lucide:monitor" /></template>
                  本地预览
                </Button>
                <Button :loading="agentLoading" @click="sendTestPrint">
                  <template #icon><IconifyIcon icon="lucide:printer" /></template>
                  生成测试打印
                </Button>
              </Space>
            </div>

            <div class="canvas-stage">
              <div
                class="design-canvas"
                :class="{ 'is-web-local-canvas': !pythonCanvasEnabled }"
                :style="canvasStyle"
                @click="selectedElementId = ''"
                @dragover="handleCanvasFieldDragOver"
                @drop="handleCanvasFieldDrop"
              >
                <img
                  v-if="pythonCanvasEnabled && canvasRenderImageUrl"
                  class="mvp-canvas-image"
                  alt="Python MVP实时渲染画布"
                  :src="canvasRenderImageUrl"
                />

                <div
                  v-else-if="pythonCanvasEnabled"
                  class="canvas-render-state"
                  :class="{ 'is-error': !!canvasRenderError }"
                >
                  <IconifyIcon
                    :icon="canvasRenderError ? 'lucide:triangle-alert' : 'lucide:loader-circle'"
                    :class="{ 'is-spinning': canvasRenderLoading && !canvasRenderError }"
                  />
                  <span>
                    {{
                      canvasRenderError
                        ? `${canvasRenderError}。请启动 HC-MES Visual Print Designer 本地服务`
                        : '正在更新打印效果预览…'
                    }}
                  </span>
                </div>

                <div v-else class="web-canvas-badge">Web定位预览</div>

                <img
                  v-if="canvasViewMode === 'design' && selectedReferenceImageUrl"
                  class="reference-image"
                  :alt="selectedKindMeta?.label"
                  :src="selectedReferenceImageUrl"
                  :style="{ opacity: getReferenceOpacity() }"
                />

                <template v-if="canvasViewMode === 'design'">
                  <button
                    v-for="element in currentDraft.elements"
                    :key="element.id"
                    class="design-element"
                    :class="[
                      `is-${element.type}`,
                      {
                        'is-active': element.id === selectedElementId,
                        'is-dragging': dragState?.elementId === element.id,
                        'is-prn-guide': element.guideOnly,
                      },
                    ]"
                    :style="getElementStyle(element)"
                    type="button"
                    @click.stop="selectedElementId = element.id"
                    @dragover="handleElementFieldDragOver($event, element)"
                    @drop="handleElementFieldDrop($event, element)"
                    @pointerdown.stop.prevent="startDragElement($event, element)"
                  >
                    <span
                      v-if="!pythonCanvasEnabled && ['field', 'text'].includes(element.type)"
                      class="local-element-content"
                    >
                      {{ getLocalElementText(element) }}
                    </span>
                    <span
                      v-else-if="!pythonCanvasEnabled && element.type === 'barcode'"
                      class="local-barcode-preview"
                    >
                      <span class="local-barcode-bars"></span>
                      <small v-if="element.humanReadable !== false">{{ getLocalElementText(element) }}</small>
                    </span>
                    <span
                      v-if="element.id === selectedElementId"
                      class="element-hit-label"
                    >
                      {{ element.name }}
                    </span>
                  </button>
                </template>
              </div>
            </div>

          </section>

          <aside class="designer-right">
            <div class="tool-panel designer-property-panel">
              <div class="panel-title">
                <strong>元素属性</strong>
                <Space v-if="selectedElement" :size="2">
                  <Tooltip title="后移">
                    <Button size="small" type="text" @click="moveSelectedElement('backward')">
                      <template #icon><IconifyIcon icon="lucide:send-to-back" /></template>
                    </Button>
                  </Tooltip>
                  <Tooltip title="前移">
                    <Button size="small" type="text" @click="moveSelectedElement('forward')">
                      <template #icon><IconifyIcon icon="lucide:bring-to-front" /></template>
                    </Button>
                  </Tooltip>
                  <Tooltip title="复制">
                    <Button size="small" type="text" @click="copySelectedElement">
                      <template #icon><IconifyIcon icon="lucide:copy" /></template>
                    </Button>
                  </Tooltip>
                  <Tooltip title="删除">
                    <Button danger size="small" type="text" @click="removeSelectedElement">
                      <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                    </Button>
                  </Tooltip>
                </Space>
              </div>

              <Form v-if="selectedElement" :label-col="{ style: { width: '76px' } }" size="small">
                <FormItem label="名称">
                  <Input v-model:value="selectedElement.name" />
                </FormItem>
                <FormItem label="类型">
                  <Select
                    v-if="selectedElement.importedFromPrn"
                    v-model:value="selectedElement.type"
                    :options="elementTypeOptions"
                    @change="handleSelectedPrnElementTypeChange"
                  />
                  <Tag v-else color="blue">
                    <IconifyIcon :icon="getElementTypeIcon(selectedElement.type)" />
                    {{ getElementTypeLabel(selectedElement.type) }}
                  </Tag>
                </FormItem>
                <FormItem v-if="selectedElement.importedFromPrn" label="PRN对象">
                  <Alert
                    :message="selectedElement.guideOnly
                      ? '当前仅作为坐标参考，不会进入打印；选择正确类型后自动转为可打印元素。'
                      : `已转换为${getElementTypeLabel(selectedElement.type)}，请继续绑定动态字段。`"
                    :type="selectedElement.guideOnly ? 'warning' : 'success'"
                    show-icon
                  />
                </FormItem>
                <div class="prop-grid">
                  <FormItem label="X">
                    <InputNumber v-model:value="selectedElement.x" :precision="3" class="full-input" />
                  </FormItem>
                  <FormItem label="Y">
                    <InputNumber v-model:value="selectedElement.y" :precision="3" class="full-input" />
                  </FormItem>
                  <FormItem label="宽">
                    <InputNumber v-model:value="selectedElement.width" :min="0" :precision="3" class="full-input" />
                  </FormItem>
                  <FormItem label="高">
                    <InputNumber v-model:value="selectedElement.height" :min="0" :precision="3" class="full-input" />
                  </FormItem>
                </div>
                <FormItem v-if="selectedElement.type === 'text'" label="文本">
                  <Textarea v-model:value="selectedElement.text" :auto-size="{ minRows: 2, maxRows: 4 }" />
                </FormItem>
                <FormItem v-if="['barcode', 'field'].includes(selectedElement.type)" label="绑定字段">
                  <Select v-model:value="selectedElement.fieldKey" :options="dataFieldOptions" show-search />
                </FormItem>
                <FormItem label="启用打印">
                  <Switch v-model:checked="selectedElement.enabled" checked-children="是" un-checked-children="否" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="字体">
                  <Select v-model:value="selectedElement.fontFamily" :options="fontFamilyOptions" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="字号">
                  <InputNumber v-model:value="selectedElement.fontSize" :min="0.5" :precision="3" class="full-input" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="最小字号">
                  <InputNumber v-model:value="selectedElement.minFontSize" :min="0.5" :precision="3" class="full-input" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="自动适配">
                  <Switch v-model:checked="selectedElement.fit" checked-children="是" un-checked-children="否" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="水平对齐">
                  <Select v-model:value="selectedElement.align" :options="textAlignOptions" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="垂直对齐">
                  <Select v-model:value="selectedElement.valign" :options="textVerticalAlignOptions" />
                </FormItem>
                <FormItem v-if="['field', 'text'].includes(selectedElement.type)" label="行距/mm">
                  <InputNumber v-model:value="selectedElement.lineSpacing" :min="0" :precision="3" class="full-input" />
                </FormItem>
                <FormItem v-if="['barcode', 'field', 'text'].includes(selectedElement.type)" label="前景色">
                  <Input v-model:value="selectedElement.fill" placeholder="#111111" />
                </FormItem>
                <FormItem v-if="selectedElement.type === 'barcode'" label="静区/mm">
                  <InputNumber v-model:value="selectedElement.quietMm" :min="0" :precision="3" class="full-input" />
                </FormItem>
                <FormItem v-if="selectedElement.type === 'barcode'" label="显示码文">
                  <Switch v-model:checked="selectedElement.humanReadable" checked-children="是" un-checked-children="否" />
                </FormItem>
                <FormItem v-if="selectedElement.type === 'barcode' && selectedElement.humanReadable" label="码文字号">
                  <InputNumber v-model:value="selectedElement.humanFontSize" :min="0.5" :precision="3" class="full-input" />
                </FormItem>
                <FormItem v-if="selectedElement.type === 'rect'" label="填充色">
                  <Input v-model:value="selectedElement.fill" allow-clear placeholder="留空表示不填充" />
                </FormItem>
                <FormItem v-if="['line', 'rect'].includes(selectedElement.type)" label="描边色">
                  <Input v-model:value="selectedElement.stroke" placeholder="#111111" />
                </FormItem>
                <FormItem v-if="['line', 'rect'].includes(selectedElement.type)" label="线宽/mm">
                  <InputNumber v-model:value="selectedElement.strokeWidth" :min="0" :precision="3" class="full-input" />
                </FormItem>
              </Form>
              <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="请选择画布元素" />
            </div>

            <div ref="elementListPanelRef" class="tool-panel element-list-panel">
              <div class="panel-title"><strong>元素列表</strong></div>
              <ATable
                bordered
                class="element-table"
                :columns="elementColumns"
                :data-source="currentDraft.elements"
                :pagination="false"
                :row-class-name="getElementRowClassName"
                row-key="id"
                :scroll="{ y: elementListScrollY }"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'name'">
                    <Button size="small" type="link" @click="selectedElementId = record.id">
                      {{ record.name }}
                    </Button>
                  </template>
                  <template v-else-if="column.dataIndex === 'type'">
                    {{ getElementTypeLabel(record.type) }}
                  </template>
                  <template v-else-if="column.dataIndex === 'position'">
                    {{ record.x }}, {{ record.y }}
                  </template>
                  <template v-else-if="column.dataIndex === 'size'">
                    {{ record.width }} * {{ record.height }}
                  </template>
                  <template v-else-if="column.dataIndex === 'actions'">
                    <Space :size="2">
                      <Tooltip title="属性">
                        <Button size="small" type="text" @click="selectedElementId = record.id">
                          <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
                        </Button>
                      </Tooltip>
                      <Tooltip title="删除">
                        <Button danger size="small" type="text" @click="removeElementById(record.id)">
                          <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                        </Button>
                      </Tooltip>
                    </Space>
                  </template>
                </template>
              </ATable>
            </div>
          </aside>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="attachModalVisible"
      :confirm-loading="attachLoading"
      title="挂接模板到产品型号尺寸"
      width="720px"
      @ok="saveDesignAttachment"
    >
      <div class="attach-template-summary">
        <strong>{{ currentDraft?.labelName || '-' }}</strong>
        <span>{{ templateStatusText }}</span>
      </div>
      <ATable
        bordered
        :columns="attachProductColumns"
        :data-source="attachProductItems"
        :loading="attachLoading"
        :pagination="false"
        row-key="id"
        :row-selection="attachRowSelection"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'productType'">
            <Tag color="blue">{{ record.productType || '-' }}</Tag>
          </template>
          <template v-else-if="column.dataIndex === 'sizeMm'">
            <Tag color="cyan">{{ record.sizeMm || '-' }}mm</Tag>
          </template>
        </template>
      </ATable>
    </Modal>

    <Modal
      v-model:open="agentPreviewVisible"
      destroy-on-close
      :footer="null"
      :title="agentPreviewTitle"
      width="920px"
    >
      <div class="agent-preview-dialog">
        <div class="agent-preview-dialog-toolbar">
          <div>
            <strong>输出目标</strong>
            <span>{{ selectedPrintTargetLabel }}</span>
          </div>
          <Space>
            <Select
              v-model:value="selectedPrintTarget"
              class="print-target-select"
              :loading="printerLoading"
              :options="printTargetOptions"
              @dropdown-visible-change="handlePrintTargetDropdownVisibleChange"
              @change="handlePrintTargetChange"
            />
            <Tooltip title="刷新本地打印机">
              <Button :loading="printerLoading" @click="loadLocalPrinters(false)">
                <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
              </Button>
            </Tooltip>
            <Button :loading="agentLoading" type="primary" @click="sendTestPrint">
              <template #icon><IconifyIcon icon="lucide:printer" /></template>
              按当前目标打印
            </Button>
          </Space>
        </div>
        <div class="agent-preview-dialog-frame">
          <img v-if="agentPreviewImageUrl" :src="agentPreviewImageUrl" alt="Python MVP同源渲染预览" />
          <Empty v-else :image="Empty.PRESENTED_IMAGE_SIMPLE" description="暂无本地渲染结果" />
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.visual-print-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
}

.filter-panel,
.tool-panel {
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.filter-panel {
  flex: 0 0 auto;
  padding: 10px 12px 0;
}

.filter-form {
  display: grid;
  grid-template-columns: minmax(150px, 220px) minmax(130px, 180px) minmax(110px, 150px) minmax(180px, 220px) auto;
  gap: 10px;
  align-items: end;
}

.filter-form :deep(.ant-form-item) {
  margin-bottom: 10px;
}

.filter-form :deep(.ant-form-item-label) {
  padding-bottom: 3px;
}

.print-agent-alert {
  flex: 0 0 auto;
}

.summary-bar,
.designer-topbar,
.canvas-toolbar,
.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.summary-bar {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 0 4px;
}

.designer-title-block {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  min-width: 0;
}

.designer-title-line {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.designer-template-status {
  flex: 0 0 auto;
  margin-inline-end: 0;
}

.designer-linked-products {
  display: inline-flex;
  flex: 1 1 360px;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
  min-width: 260px;
}

.linked-products-label {
  margin-left: 0 !important;
  color: #334155 !important;
  font-size: 12px !important;
  font-weight: 600;
}

.linked-products-tags {
  display: inline-flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  gap: 4px;
  min-width: 0;
}

.linked-products-tags :deep(.ant-tag) {
  margin-inline-end: 0;
}

.summary-bar strong,
.designer-topbar strong,
.canvas-toolbar strong,
.panel-title strong {
  color: #1f2937;
  font-size: 14px;
}

.summary-bar span,
.designer-topbar span,
.canvas-toolbar span {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.customer-print-modal {
  height: calc(100vh - 118px);
  min-height: 0;
  overflow: hidden;
  background: #f5f7fb;
}

.customer-print-modal__tabs {
  height: 100%;
  padding: 0 16px 16px;
}

.customer-print-modal :deep(.ant-tabs) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

.customer-print-modal :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  height: 44px;
  margin-bottom: 12px;
}

.customer-print-modal :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.customer-print-modal :deep(.ant-tabs-content),
.customer-print-modal :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

.customer-print-modal__pane {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding: 12px;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.customer-print-modal__pane--products {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.customer-product-title,
.attach-template-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.attach-template-summary span {
  color: #64748b;
  font-size: 12px;
}

.customer-product-table-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.customer-product-table :deep(.ant-table-wrapper),
.customer-product-table :deep(.ant-spin-nested-loading),
.customer-product-table :deep(.ant-spin-container),
.customer-product-table :deep(.ant-table),
.customer-product-table :deep(.ant-table-container) {
  height: 100%;
}

.customer-product-table :deep(.ant-table-body) {
  min-height: 0;
}

.customer-vben-grid {
  flex: 1 1 auto;
  height: calc(100vh - 238px);
  min-height: 520px;
  overflow: hidden;
}

.customer-vben-grid :deep(.vxe-grid) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.customer-vben-grid :deep(.vxe-grid--table-wrapper) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.customer-vben-grid :deep(.vxe-grid--pager-wrapper) {
  position: sticky;
  z-index: 3;
  bottom: 0;
  flex: 0 0 auto;
  border-top: 1px solid #e5edf7;
  background: #fff;
}

.customer-vben-grid :deep(.vxe-body--column) {
  height: 56px;
  padding-top: 6px;
  padding-bottom: 6px;
}

.customer-vben-grid :deep(.vxe-cell) {
  max-height: none;
  padding-right: 8px;
  padding-left: 8px;
}

.customer-vben-grid :deep(.vxe-table--fixed-right-wrapper) {
  box-shadow: -8px 0 12px rgb(15 23 42 / 8%);
}

.grid-preview-button {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  width: 100%;
  max-width: 136px;
  height: 44px;
  overflow: hidden;
  padding: 4px 6px;
  border: 1px solid #e5edf7;
  border-radius: 4px;
  background: #f8fafc;
  color: inherit;
  cursor: pointer;
  font: inherit;
  text-align: center;
}

.grid-preview-badges {
  display: inline-flex;
  flex: 0 0 38px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 0;
}

.grid-preview-button:hover {
  border-color: #4096ff;
  background: #eef6ff;
}

.grid-preview-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 20px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 18px;
  white-space: nowrap;
}

.grid-preview-status.is-designed {
  border: 1px solid #95de64;
  background: #f6ffed;
  color: #389e0d;
}

.grid-preview-status.is-reference {
  border: 1px solid #91caff;
  background: #e6f4ff;
  color: #0958d9;
}

.grid-preview-status.is-empty {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #8c8c8c;
}

.grid-preview-share {
  width: 38px;
  overflow: hidden;
  color: #1677ff;
  font-size: 11px;
  line-height: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.grid-preview-thumb {
  display: inline-flex;
  flex: 1 1 auto;
  align-items: center;
  justify-content: center;
  min-width: 0;
  height: 34px;
  overflow: hidden;
  color: #94a3b8;
}

.grid-preview-thumb img {
  max-width: 100%;
  max-height: 32px;
  object-fit: contain;
  min-width: 0;
  background: #fff;
}

.grid-action-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 100%;
}

.grid-action-button {
  width: 28px;
  height: 28px;
  padding: 0;
}

.grid-coa-cell {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 18px;
}

.customer-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
  max-height: 62vh;
  overflow: auto;
  padding-right: 4px;
}

.customer-form-grid .is-full {
  grid-column: 1 / -1;
}

.full-input {
  width: 100%;
}

.designer-shell {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 55px);
  min-height: 0;
  background: #f5f7fb;
}

.designer-topbar {
  flex: 0 0 auto;
  min-height: 52px;
  padding: 8px 12px;
  border-bottom: 1px solid #d9e2ef;
  background: #fff;
}

.designer-main {
  display: grid;
  flex: 1 1 auto;
  grid-template-columns: 280px minmax(0, 1fr) 360px;
  gap: 10px;
  min-height: 0;
  padding: 10px;
}

.designer-main.is-loading {
  opacity: 0.72;
  pointer-events: none;
}

.designer-left,
.designer-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.designer-right {
  display: grid;
  grid-template-rows: 320px minmax(0, 1fr);
}

.designer-left > .tool-panel,
.designer-right > .tool-panel {
  min-height: 0;
}

.designer-left-tabs-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
}

.designer-left-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0 8px 8px;
}

.designer-left-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin-bottom: 0;
}

.designer-left-tabs :deep(.ant-tabs-content-holder) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}

.designer-left-tabs :deep(.ant-tabs-content),
.designer-left-tabs :deep(.ant-tabs-tabpane) {
  height: 100%;
  min-height: 0;
}

.designer-tab-pane {
  height: 100%;
  min-height: 0;
  overflow: auto;
  padding-top: 8px;
}

.designer-template-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
}

.template-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.template-panel-head > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.template-panel-head strong {
  color: #0f172a;
  font-size: 13px;
}

.template-panel-head span {
  color: #64748b;
  font-size: 12px;
}

.designer-template-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.designer-template-card {
  display: grid;
  width: 100%;
  min-height: 72px;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: start;
  padding: 8px;
  text-align: left;
  cursor: pointer;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.designer-template-card:hover,
.designer-template-card.is-active {
  border-color: #1677ff;
  background: #eff6ff;
}

.designer-template-card.is-draft {
  cursor: default;
  border-color: #f59e0b;
  background: #fff7ed;
}

.designer-template-card-main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.designer-template-card-main strong,
.designer-template-card-main span,
.designer-template-card-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.designer-template-card-main strong {
  color: #0f172a;
  font-size: 13px;
}

.designer-template-card-main span {
  color: #334155;
  font-size: 12px;
}

.designer-template-card-main small {
  color: #64748b;
}

.designer-template-loading {
  display: grid;
  min-height: 96px;
  place-items: center;
  color: #64748b;
}

.designer-canvas-panel,
.designer-data-panel {
  display: flex;
  flex-direction: column;
}

.designer-property-panel,
.element-list-panel {
  display: flex;
  flex-direction: column;
}

.designer-property-panel {
  height: 320px;
  overflow: hidden;
}

.designer-property-panel :deep(.ant-form) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
}

.designer-property-panel :deep(.ant-empty) {
  flex: 1 1 auto;
  min-height: 120px;
  margin: 0;
  display: grid;
  place-items: center;
}

.designer-center {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #fff;
}

.tool-panel {
  min-width: 0;
  overflow: hidden;
}

.panel-title {
  flex: 0 0 auto;
  min-height: 38px;
  padding: 8px 10px;
  border-bottom: 1px solid #e5edf7;
  background: #f8fafc;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px;
  padding: 8px;
}

.tool-panel :deep(.ant-form) {
  padding: 10px;
}

.tool-panel :deep(.ant-form-item) {
  margin-bottom: 8px;
}

.designer-canvas-panel :deep(.ant-form) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: visible;
  padding: 0;
}

.designer-canvas-panel :deep(.ant-form-item) {
  margin-bottom: 6px;
}

.reference-size {
  min-height: 24px;
  padding: 3px 8px;
  border-radius: 3px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  line-height: 18px;
}

.reference-maintain {
  display: grid;
  gap: 6px;
}

.reference-name {
  overflow: hidden;
  min-height: 22px;
  padding: 2px 6px;
  border-radius: 3px;
  background: #fff;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reference-actions {
  display: flex;
}

.field-list {
  display: grid;
  flex: 1 1 auto;
  gap: 4px;
  align-content: start;
  min-height: 0;
  overflow: auto;
  padding: 8px;
}

.field-source-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  min-height: 30px;
  border: 1px solid #e2e8f0;
  border-radius: 3px;
  background: #f1f5f9;
  cursor: grab;
}

.field-source-row:hover {
  border-color: #91caff;
  background: #eff6ff;
}

.field-source-row:active {
  cursor: grabbing;
}

.field-source-main {
  appearance: none;
  overflow: hidden;
  min-width: 0;
  height: 100%;
  padding: 4px 6px;
  border: 0;
  background: transparent;
  color: #334155;
  font-size: 12px;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-source-main:hover {
  color: #0958d9;
}

.field-source-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  padding-right: 2px;
}

.field-source-actions :deep(.ant-btn-sm) {
  width: 24px;
  height: 24px;
}

.canvas-toolbar {
  flex: 0 0 auto;
  min-height: 46px;
  padding: 8px 10px;
  border-bottom: 1px solid #e5edf7;
  background: #f8fafc;
}

.print-target-select {
  width: 240px;
}

.canvas-view-select {
  width: 150px;
}

.python-canvas-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 12px;
  white-space: nowrap;
}

.prn-import-control {
  display: grid;
  grid-template-columns: minmax(90px, 1fr) auto;
  gap: 6px;
  width: 100%;
}

.canvas-stage {
  display: grid;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  place-items: center;
  padding: 18px;
  background:
    linear-gradient(90deg, rgb(226 232 240 / 55%) 1px, transparent 1px) 0 0 / 16px 16px,
    linear-gradient(rgb(226 232 240 / 55%) 1px, transparent 1px) 0 0 / 16px 16px,
    #f8fafc;
}

.design-canvas {
  position: relative;
  flex: 0 0 auto;
  overflow: hidden;
  border: 1px solid #111827;
  background: #fff;
  box-shadow: 0 12px 30px rgb(15 23 42 / 12%);
}

.mvp-canvas-image,
.reference-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: fill;
  pointer-events: none;
}

.mvp-canvas-image {
  z-index: 0;
}

.reference-image {
  z-index: 1;
  transition: opacity 0.2s ease;
}

.web-canvas-badge {
  position: absolute;
  z-index: 3;
  top: 6px;
  right: 6px;
  padding: 2px 6px;
  border: 1px solid #91caff;
  border-radius: 3px;
  background: rgb(230 244 255 / 88%);
  color: #0958d9;
  font-size: 10px;
  line-height: 16px;
  pointer-events: none;
}

.canvas-render-state {
  position: absolute;
  z-index: 3;
  inset: 0;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 8px;
  padding: 24px;
  background: rgb(248 250 252 / 92%);
  color: #475569;
  font-size: 13px;
  text-align: center;
}

.canvas-render-state.is-error {
  color: #cf1322;
}

.canvas-render-state .is-spinning {
  animation: canvas-render-spin 0.9s linear infinite;
}

@keyframes canvas-render-spin {
  to {
    transform: rotate(360deg);
  }
}

.design-element {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 6px;
  min-height: 6px;
  overflow: hidden;
  padding: 0;
  border: 1px dashed transparent;
  background: transparent;
  color: #111827;
  cursor: grab;
  touch-action: none;
  z-index: 2;
}

.design-element.is-active {
  border-color: #1677ff;
  background: rgb(22 119 255 / 8%);
}

.design-element.is-dragging {
  border-color: #fa8c16;
  background: rgb(250 140 22 / 10%);
  cursor: grabbing;
}

.is-web-local-canvas .design-element.is-field,
.is-web-local-canvas .design-element.is-text,
.is-web-local-canvas .design-element.is-barcode {
  border-color: rgb(100 116 139 / 35%);
}

.design-element.is-prn-guide {
  border: 1px dashed #fa8c16;
  background: rgb(255 247 230 / 42%);
  color: #ad4e00;
}

.local-element-content {
  display: block;
  overflow: hidden;
  width: 100%;
  max-height: 100%;
  pointer-events: none;
  text-overflow: clip;
}

.local-barcode-preview {
  display: flex;
  align-items: stretch;
  flex: 1;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 8px;
  pointer-events: none;
}

.local-barcode-bars {
  flex: 1;
  min-height: 5px;
  background: repeating-linear-gradient(
    90deg,
    #111827 0,
    #111827 1px,
    transparent 1px,
    transparent 3px,
    #111827 3px,
    #111827 5px,
    transparent 5px,
    transparent 7px
  );
}

.local-barcode-preview small {
  overflow: hidden;
  color: #111827;
  font-size: 8px;
  line-height: 10px;
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.element-hit-label {
  position: absolute;
  top: -18px;
  left: -1px;
  max-width: 180px;
  overflow: hidden;
  padding: 1px 5px;
  border-radius: 3px 3px 0 0;
  background: #1677ff;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  pointer-events: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.prop-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 8px;
}

.element-list-panel {
  flex: 1 1 0;
  min-height: 0;
}

.element-table,
.element-list-panel :deep(.ant-spin-container),
.element-list-panel :deep(.ant-spin-nested-loading),
.element-list-panel :deep(.ant-table),
.element-list-panel :deep(.ant-table-wrapper) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.element-list-panel :deep(.ant-table-container) {
  min-height: 0;
}

.element-list-panel :deep(.ant-table-cell) {
  padding: 8px 10px;
}

.element-list-panel :deep(.ant-table-tbody > tr.is-selected-element-row > td) {
  border-color: #91caff;
  background: #e6f4ff !important;
}

.element-list-panel :deep(.ant-table-tbody > tr.is-selected-element-row:hover > td) {
  background: #bae0ff !important;
}

.element-list-panel :deep(.ant-table-tbody > tr.is-selected-element-row > td:first-child) {
  box-shadow: inset 3px 0 0 #1677ff;
}

.element-list-panel :deep(.ant-table-cell-fix-right),
.element-list-panel :deep(.ant-table-cell-fix-right-first::after),
.element-list-panel :deep(.ant-table-cell-fix-right-last::after) {
  box-shadow: none !important;
}

.element-table :deep(.ant-table-body) {
  min-height: 0;
  overflow: auto !important;
}

.agent-preview-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 560px;
}

.agent-preview-dialog-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background: #f8fafc;
}

.agent-preview-dialog-toolbar strong {
  color: #1f2937;
  font-size: 14px;
}

.agent-preview-dialog-toolbar span {
  margin-left: 8px;
  color: #64748b;
  font-size: 12px;
}

.agent-preview-dialog-frame {
  display: grid;
  min-height: 500px;
  overflow: auto;
  place-items: center;
  border: 1px solid #d9e2ef;
  border-radius: 6px;
  background:
    linear-gradient(90deg, rgb(226 232 240 / 45%) 1px, transparent 1px) 0 0 / 16px 16px,
    linear-gradient(rgb(226 232 240 / 45%) 1px, transparent 1px) 0 0 / 16px 16px,
    #f8fafc;
}

.agent-preview-dialog-frame img {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
  background: #fff;
}

:global(.visual-print-fullscreen-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  width: 100vw !important;
  height: 100vh;
  margin: 0;
  padding-bottom: 0;
}

:global(.visual-print-fullscreen-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
  padding: 0;
}

:global(.visual-print-fullscreen-modal .ant-modal-header) {
  flex: 0 0 auto;
  margin-bottom: 0;
  padding: 16px 24px;
  border-bottom: 1px solid #e5edf7;
}

:global(.visual-print-fullscreen-modal .ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  height: auto;
  overflow: hidden;
  padding: 0;
}

:global(.visual-print-fullscreen-modal .ant-modal-close) {
  top: 16px;
}

:global(.customer-print-fullscreen-modal .ant-modal) {
  top: 0;
  max-width: 100vw;
  width: 100vw !important;
  height: 100vh;
  margin: 0;
  padding-bottom: 0;
}

:global(.customer-print-fullscreen-modal .ant-modal-content) {
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-radius: 0;
  padding: 0;
}

:global(.customer-print-fullscreen-modal .ant-modal-header) {
  flex: 0 0 auto;
  margin-bottom: 0;
  padding: 16px 24px;
  border-bottom: 1px solid #e5edf7;
}

:global(.customer-print-fullscreen-modal .ant-modal-body) {
  flex: 1 1 auto;
  min-height: 0;
  height: auto;
  overflow: hidden;
  padding: 0;
}

:global(.customer-print-fullscreen-modal .ant-modal-footer) {
  flex: 0 0 auto;
  margin-top: 0;
  padding: 12px 24px;
  border-top: 1px solid #e5edf7;
}

:global(.customer-print-fullscreen-modal .ant-modal-close) {
  top: 16px;
}

@media (max-width: 1380px) {
  .designer-main {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .designer-right {
    display: none;
  }
}

@media (max-width: 960px) {
  .filter-form,
  .designer-main,
  .customer-form-grid {
    grid-template-columns: 1fr;
  }

  .designer-left {
    max-height: 260px;
  }
}
</style>
