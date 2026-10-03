<script lang="ts" setup>
import type { MesProcessApi } from '#/api/mes/base/process';
import type { MesHcBomApi } from '#/api/mes/hc/bom';
import type { MesHcMaterialApi } from '#/api/mes/hc/material';
import type { MesExceptionApi } from '#/api/mes/quality/abnormal/exception';
import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';
import type { MesProductAbnormalEventApi } from '#/api/mes/quality/abnormal/product-event';
import type { MesDefectCodeApi } from '#/api/mes/quality/base/defect-code';
import type { MesCutRoundFqcApi } from '#/api/mes/quality/cut-round-fqc';
import type { MesFaiApi } from '#/api/mes/quality/fai';
import type { MesFgShippingFqcApi } from '#/api/mes/quality/fg-shipping-fqc';
import type { MesOqcApi } from '#/api/mes/quality/oqc';
import type { SystemDeptApi } from '#/api/system/dept';
import type { SystemUserApi } from '#/api/system/user';
import type { BusinessLogMode } from '#/components/business-log';
import type { PickerEntityConfig, PickerOption } from '#/components/picker';

import { computed, ref, watch } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';
import { handleTree } from '@vben/utils';

import {
  Alert,
  Modal as AntModal,
  Button,
  Checkbox,
  DatePicker,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Radio,
  Select,
  Spin,
  Table,
  Tag,
  Tooltip,
  TreeSelect,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import { getProcessPage } from '#/api/mes/base/process';
import { getBomPage } from '#/api/mes/hc/bom';
import {
  confirmNcrDispositionScope,
  createNcrRecord,
  delegateNcrMrbReview,
  finalApproveNcr,
  getNcrDispositionContext,
  getNcrRecord,
  getNcrReviewConfigSimpleList,
  handleNcr,
  linkNcrException,
  replyNcrDispositionNotify,
  returnNcr,
  stockDisposeNcr,
  submitNcr,
  updateNcrRecord,
} from '#/api/mes/quality/abnormal/ncr';
import {
  createRawMaterialNcrRecord,
  delegateRawMaterialNcrMrbReview,
  finalApproveRawMaterialNcr,
  getRawMaterialNcrRecord,
  handleRawMaterialNcr,
  linkRawMaterialNcrException,
  replyRawMaterialNcrDispositionNotify,
  returnRawMaterialNcr,
  stockDisposeRawMaterialNcr,
  submitRawMaterialNcr,
  updateRawMaterialNcrRecord,
} from '#/api/mes/quality/abnormal/raw-material-ncr';
import { getProductAbnormalEventDetail } from '#/api/mes/quality/abnormal/product-event';
import { getCutRoundFqcLightDetail } from '#/api/mes/quality/cut-round-fqc';
import { getFaiDetail, getGlueBoardFaiDetail } from '#/api/mes/quality/fai';
import { getFgShippingFqcDetail } from '#/api/mes/quality/fg-shipping-fqc';
import { getOqcDetail } from '#/api/mes/quality/oqc';
import { getSimpleDeptList } from '#/api/system/dept';
import { getSimpleUserList } from '#/api/system/user';
import { BusinessLogDrawer } from '#/components/business-log';
import {
  materialPickerConfig,
  PickerInline,
  PickerModal,
} from '#/components/picker';
import { DefectCodePicker } from '#/components/quality';
import { FileUpload } from '#/components/upload';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import IqcDetailModalForm from '#/views/mes/quality/iqc/modules/detail-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import CutRoundFqcEventDetailPanel from '../../product-event/modules/CutRoundFqcEventDetailPanel.vue';
import ProductEventDetailModalForm from '../../product-event/modules/detail-modal.vue';
import FaiEventDetailPanel from '../../product-event/modules/FaiEventDetailPanel.vue';
import FgShippingFqcEventDetailPanel from '../../product-event/modules/FgShippingFqcEventDetailPanel.vue';
import OqcEventDetailPanel from '../../product-event/modules/OqcEventDetailPanel.vue';
import { QMS_NCR_DICT } from '../data';
import DispositionScopePanel from './disposition-scope-panel.vue';
import ExceptionSelectModal from './exception-select-modal.vue';
import IqcSourceSelectModal from './iqc-source-select-modal.vue';
import ProductSourceSelectModal from './product-source-select-modal.vue';

const emit = defineEmits(['success']);

const ATTACHMENT_RELATION_TYPE = 'ATTACHMENT';
const attachmentAcceptTypes = [
  'pdf',
  'doc',
  'docx',
  'xls',
  'xlsx',
  'png',
  'jpg',
  'jpeg',
  'zip',
  'rar',
];
const sourceTypeFallbackOptions = [
  { label: '成品', value: 'FINISHED_PRODUCT' },
  { label: '半成品', value: 'SEMI_FINISHED' },
  { label: '客退品', value: 'CUSTOMER_RETURN' },
  { label: '原材料', value: 'RAW_MATERIAL' },
];
const ncLevelFallbackOptions = [
  { label: '轻微', value: 'MINOR' },
  { label: '一般', value: 'MAJOR' },
  { label: '严重', value: 'CRITICAL' },
];
const responsibilityDeptFallbackOptions = [
  { label: '研发中心', value: 'R_AND_D_CENTER' },
  { label: '事业部生产', value: 'BUSINESS_PRODUCTION' },
  { label: '品质部', value: 'QUALITY_DEPT' },
  { label: '事业部工艺', value: 'BUSINESS_PROCESS' },
  { label: '采购部', value: 'PURCHASE_DEPT' },
];
const sourceBizTypeFallbackOptions = [
  { label: '首件检验', value: 'FAI' },
  { label: '胶板检验', value: 'GLUE_BOARD_FAI' },
  { label: '裁切成品检验', value: 'CUT_ROUND_FQC' },
  { label: '发货成品检验', value: 'FG_SHIPPING_FQC' },
  { label: '出货检验(OQC)', value: 'OQC' },
  { label: '异常事件', value: 'EXCEPTION' },
  { label: '客户退货', value: 'CUSTOMER_RETURN' },
  { label: '进料检验(IQC)', value: 'IQC' },
];
const inspectionSourceTypes = new Set<MesProductAbnormalEventApi.SourceType>([
  'CUT_ROUND_FQC',
  'FAI',
  'FG_SHIPPING_FQC',
  'GLUE_BOARD_FAI',
  'OQC',
]);
const dispositionOptions = [
  { label: '挑选', value: 'PICK' },
  { label: '返工', value: 'REWORK' },
  { label: '改切', value: 'RECUT' },
  { label: '报废', value: 'SCRAP' },
  { label: '特采', value: 'CONCESSION' },
];
const rawMaterialDispositionOptions = [
  { label: '退货', value: 'RETURN' },
  { label: '报废', value: 'SCRAP' },
  { label: '挑选', value: 'PICK' },
  { label: '特采', value: 'CONCESSION' },
];
const rawMaterialSourceTypeOptions = [
  { label: '原材料', value: 'RAW_MATERIAL' },
];
const rawMaterialSourceBizTypeOptions = [
  { label: '进料检验(IQC)', value: 'IQC' },
];
const rawMaterialAbnormalCategoryOptions = [
  { label: '来料品异常', value: 'INCOMING' },
  { label: '在库品异常', value: 'STOCK' },
];
const MAX_VISIBLE_TOOLBAR_ACTIONS = 5;
const TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW = MAX_VISIBLE_TOOLBAR_ACTIONS - 2;
const FINAL_APPROVER_UNIT_CODE = 'FINAL_APPROVER';
const DISPOSITION_ATTACHMENT_RELATION_TYPE = 'DISPOSITION_ATTACHMENT';
const RAW_TRANSFER_ROUTE_FINAL = 'FINAL_APPROVAL';
const RAW_TRANSFER_ROUTE_EXECUTION = 'DISPOSITION_EXECUTION';
const RAW_TRANSFER_ROUTE_CLOSE = 'DIRECT_CLOSE';
const returnableStatuses = new Set(['CLOSE_CONFIRM']);
const nextHandlerRequiredStatuses = new Set([
  'EXECUTION_ASSIGN',
  'REVIEW_ASSIGN',
]);

const productBomPickerConfig: PickerEntityConfig<MesHcBomApi.Bom> = {
  entityKey: 'ncrProductBom',
  title: '选择产品料号',
  tableTitle: 'BOM产品料号列表',
  modalWidth: 1180,
  inlinePanelWidth: 900,
  queryFields: [
    {
      field: 'productMaterialKeyword',
      label: '产品料号',
      placeholder: '请输入产品料号或名称',
    },
    {
      field: 'productModelCode',
      label: '产品型号',
      placeholder: '请输入产品型号',
    },
    {
      field: 'productSpec',
      label: '尺寸规格',
      placeholder: '请输入尺寸规格',
    },
    {
      defaultHidden: true,
      field: 'bomCode',
      label: 'BOM编码',
      placeholder: '请输入BOM编码',
    },
  ],
  columns: [
    { field: 'productModelCode', minWidth: 130, title: '产品型号' },
    { field: 'productMaterialCode', minWidth: 150, title: '产品料号' },
    { field: 'productMaterialName', minWidth: 190, title: '产品名称' },
    { field: 'productSpec', minWidth: 110, title: '尺寸规格' },
    { field: 'bomCode', minWidth: 150, title: 'BOM编码' },
    { field: 'bomName', minWidth: 190, title: 'BOM名称' },
    { field: 'versionNo', title: '版本', width: 90 },
    {
      align: 'center',
      field: 'status',
      formatter: (value) => (Number(value) === 1 ? '启用' : '停用'),
      title: '状态',
      width: 80,
    },
  ],
  fetchPage: async (params) => {
    const keyword = params.filters?.keyword || undefined;
    const res = await getBomPage({
      pageNo: params.pageNo,
      pageSize: params.pageSize,
      bomCode: params.filters?.bomCode || undefined,
      productMaterialKeyword: params.filters?.productMaterialKeyword || keyword,
      productModelCode: params.filters?.productModelCode || undefined,
      productSpec: params.filters?.productSpec || undefined,
      status: 1,
    });
    return { list: res.list, total: res.total };
  },
  buildOption: (row) => ({
    code: row.productMaterialCode || '',
    extra: {
      bomCode: row.bomCode,
      bomId: row.id,
      bomName: row.bomName,
      bomType: row.bomType,
      bomVersion: row.versionNo,
      productMaterialCode: row.productMaterialCode,
      productMaterialId: row.productMaterialId,
      productMaterialName: row.productMaterialName,
      productModelCode: row.productModelCode,
      productModelId: row.productModelId,
      productModelName: row.productModelName,
      productSpec: row.productSpec,
      routeCode: row.routeCode,
      routeId: row.routeId,
    },
    id: row.id,
    label: row.productMaterialCode || '',
    name: row.productMaterialName || '',
    raw: row as any,
    status: Number(row.status),
  }),
};

interface ToolbarActionItem {
  danger?: boolean;
  key: string;
  label: string;
  onClick: () => Promise<void> | void;
  title: string;
  type?: 'default' | 'primary';
}

type NgInspectionDetailRow =
  MesProductAbnormalEventApi.ProductAbnormalEventDetailItem & {
    __rowKey: string;
  };

type NcrDefectRow = MesNcrApi.NcDefect & {
  __rowKey?: string;
};
type IqcSourceRelationRow = MesNcrApi.Relation & {
  __rowKey: string;
};

type OpinionTemplateScope = 'product' | 'rawMaterial';
type OpinionTemplateState = Record<
  OpinionTemplateScope,
  Record<string, string>
>;
type AutoTemplateReview = MesNcrApi.MrbReview & {
  __lastOpinionTemplateText?: string;
};

type DefectSelectPayload = {
  code?: string;
  level?: string;
  name?: string;
  raw?: MesDefectCodeApi.DefectCode;
};

interface UserSelectOption {
  label: string;
  searchText: string;
  username: string;
  value: number;
}

interface ReviewDelegateTarget {
  id?: number;
  reviewId?: number;
}

let localRowSeed = 0;

const isNew = ref(false);
const formData = ref<MesNcrApi.NcrRecord>({});
const formRef = ref();
const finalOpinionAutoTemplateText = ref('');
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('audit');
const reviewConfigList = ref<MesNcrApi.ReviewConfig[]>([]);
const reviewConfigLoaded = ref(false);
const suppressReviewDeptSync = ref(false);
const selectedReviewDeptNames = ref<string[]>([]);
const actionOpinion = ref('');
const dispositionNotifyUserIds = ref<number[]>([]);
const dispositionNotifyReplyConclusion = ref('');
const nextHandlerUserId = ref<number>();
const nextHandlerUserIds = ref<number[]>([]);
const transferRoute = ref<
  'DIRECT_CLOSE' | 'DISPOSITION_EXECUTION' | 'FINAL_APPROVAL' | undefined
>();
const dispositionExecution = ref<MesNcrApi.DispositionExecution>();
const isRollPickQualification = computed(() =>
  !isRawMaterialNcr.value && dispositionExecution.value?.dispositionType === 'PICK'
  && ['MOTHER_BATCH', 'SEGMENT'].includes(dispositionExecution.value?.scopeLevel || '')
  && ['WET', 'ROUGH_GRINDING', 'ADHESIVE1'].includes(dispositionExecution.value?.ngProcessCode || ''),
);
const isPendingRollPickConfirmation = computed(() =>
  isRollPickQualification.value && dispositionExecution.value?.executionStatus !== 'COMPLETED',
);
const dispositionScopePanelRef = ref<{
  getConfirmSummary: () => string;
  validateAndBuild: (
    executionUserId?: number,
    executionUserName?: string,
  ) => MesNcrApi.DispositionScopeConfirmReq | undefined;
}>();
const reviewDelegateTarget = ref<ReviewDelegateTarget>();
const userSelectPurpose = ref<'contentConfirm' | 'reviewDelegate'>();
const contentConfirmUserSelectResolver = ref<
  ((confirmed: boolean) => void) | undefined
>();
const finalApproverPickerOpen = ref(false);
const finalApproverPickerConfirmed = ref(false);
const FINAL_APPROVER_MODAL_Z_INDEX = 5550;
const FINAL_APPROVER_DROPDOWN_Z_INDEX = FINAL_APPROVER_MODAL_Z_INDEX + 20;
const stockResult = ref('');
const attachmentUrls = ref<string[]>([]);
const dispositionAttachmentUrls = ref<string[]>([]);
const descriptionEditorOpen = ref(false);
const ngDetailModalOpen = ref(false);
const iqcRelationPickerOpen = ref(false);
const ngDetailLoading = ref(false);
const ngDetailOnlyNg = ref(false);
const ngInspectionDetail =
  ref<MesProductAbnormalEventApi.ProductAbnormalEventDetail | null>(null);
const ngFaiRecord = ref<MesFaiApi.FaiRecord | null>(null);
const ngCutRoundRecord = ref<MesCutRoundFqcApi.Record | null>(null);
const ngFgShippingRecord = ref<MesFgShippingFqcApi.Record | null>(null);
const ngOqcRecord = ref<MesOqcApi.OqcRecord | null>(null);
const userStore = useUserStore();
const deptOptions = ref<SystemDeptApi.Dept[]>([]);
const userOptions = ref<UserSelectOption[]>([]);
const userOptionsLoaded = ref(false);
const userOptionsLoading = ref(false);
const processOptions = ref<
  Array<{ label: string; raw: MesProcessApi.Process; value: number | string }>
>([]);
const processOptionsLoaded = ref(false);
const processOptionsLoading = ref(false);
const productBomPickerOpen = ref(false);

const [SourcePickerModal, sourcePickerModalApi] = useVbenModal({
  connectedComponent: ProductSourceSelectModal,
  destroyOnClose: true,
});
const [IqcSourceSelectModalComp, iqcSourceSelectModalApi] = useVbenModal({
  connectedComponent: IqcSourceSelectModal,
  destroyOnClose: true,
});
const [ExceptionSelectModalComp, exceptionSelectModalApi] = useVbenModal({
  connectedComponent: ExceptionSelectModal,
  destroyOnClose: true,
});
const [IqcDetailModal, iqcDetailModalApi] = useVbenModal({
  connectedComponent: IqcDetailModalForm,
  destroyOnClose: true,
});
const [ProductEventDetailModal, productEventDetailModalApi] = useVbenModal({
  connectedComponent: ProductEventDetailModalForm,
  destroyOnClose: true,
});
const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});
const [UserSelectModalComp, userSelectModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

const currentUserId = computed(() => userStore.userInfo?.id);

const pendingDispositionNotify = computed(() =>
  (formData.value.dispositionNotifies || []).find(
    (row) =>
      row.notifyStatus === 'PENDING' &&
      String(row.notifyUserId || '') === String(currentUserId.value || '') &&
      String(formData.value.currentHandlerUserId || '') !==
        String(currentUserId.value || ''),
  ),
);

const isDispositionNotifyReplyMode = computed(
  () => !!pendingDispositionNotify.value,
);

const canHandleCurrentRecord = computed(() => {
  if (isNew.value) {
    return true;
  }
  if (isDispositionNotifyReplyMode.value) {
    return true;
  }
  if (['CANCELLED', 'CLOSED'].includes(formData.value.status || '')) {
    return false;
  }
  if (formData.value.tabType === 'todo') {
    return true;
  }
  if (formData.value.canHandle === true) {
    return true;
  }
  if (formData.value.canHandle === false) {
    return false;
  }
  return (
    !formData.value.tabType &&
    ['DRAFT', 'RETURNED'].includes(formData.value.status || '')
  );
});

const formDisabled = computed(() => {
  return !canHandleCurrentRecord.value;
});

const deptTreeData = computed(
  () =>
    handleTree(
      deptOptions.value.map((dept) => ({ ...dept })),
    ) as SystemDeptApi.Dept[],
);

const primaryActionLabel = computed(() => {
  if (isNew.value) {
    return '提交';
  }
  if (isDispositionNotifyReplyMode.value) {
    return '提交通知回复';
  }
  if (
    formData.value.status === 'DRAFT' ||
    formData.value.status === 'RETURNED'
  ) {
    return '提交';
  }
  if (formData.value.status === 'CONTENT_CONFIRM') {
    return '提交确认';
  }
  if (
    formData.value.status === 'SUBMITTED' ||
    formData.value.status === 'MRB_REVIEW'
  ) {
    return '确认';
  }
  if (formData.value.status === 'REVIEW_ASSIGN') {
    if (transferRoute.value === RAW_TRANSFER_ROUTE_FINAL) {
      return '提交终审';
    }
    return transferRoute.value === RAW_TRANSFER_ROUTE_CLOSE
      ? '直接关闭'
      : '分派执行';
  }
  if (formData.value.status === 'FINAL_APPROVAL') {
    return '终审';
  }
  if (formData.value.status === 'EXECUTION_ASSIGN') {
    if (transferRoute.value === RAW_TRANSFER_ROUTE_CLOSE) {
      return '直接关闭';
    }
    if (isRawMaterialNcr.value) {
      return formData.value.stockDisposeStatus === 'REWORK'
        ? '重新分派执行'
        : '分派执行';
    }
    return '确认范围并提交执行';
  }
  if (formData.value.status === 'PENDING_STOCK_DISPOSE') {
    return isPendingRollPickConfirmation.value ? '确认挑选合格' : '提交处置结果';
  }
  if (formData.value.status === 'CLOSE_CONFIRM') {
    return '复检关闭';
  }
  return '关闭';
});

const isRawMaterialNcr = computed(
  () => formData.value.sourceType === 'RAW_MATERIAL',
);

const descriptionConfirmUserName = computed(
  () =>
    formData.value.contentConfirmUserName ||
    formData.value.qualityConfirmUserName,
);

const needsContentConfirmUser = computed(
  () =>
    isNew.value || ['DRAFT', 'RETURNED'].includes(formData.value.status || ''),
);

const formTitle = computed(() =>
  isRawMaterialNcr.value ? '原材料不合格处置单' : '不合格品处置单',
);

const sourceTypeOptions = computed(() =>
  isRawMaterialNcr.value
    ? rawMaterialSourceTypeOptions
    : mergeDictOptions(QMS_NCR_DICT.sourceType, sourceTypeFallbackOptions),
);

const ncLevelOptions = computed(() =>
  mergeDictOptions(QMS_NCR_DICT.level, ncLevelFallbackOptions),
);
const responsibilityDeptOptions = computed(() =>
  mergeDictOptions(
    QMS_NCR_DICT.responsibilityDept,
    responsibilityDeptFallbackOptions,
  ),
);
const sourceBizTypeOptions = computed(() =>
  isRawMaterialNcr.value
    ? rawMaterialSourceBizTypeOptions
    : mergeDictOptions(
        QMS_NCR_DICT.sourceBizType,
        sourceBizTypeFallbackOptions,
      ),
);

const effectiveDispositionOptions = computed(() =>
  isRawMaterialNcr.value ? rawMaterialDispositionOptions : dispositionOptions,
);

const finalDispositionValues = computed(() =>
  parseDispositionValues(formData.value.finalDisposition),
);

const activeMaterialPickerConfig = computed(() =>
  isRawMaterialNcr.value ? materialPickerConfig : productBomPickerConfig,
);

const productBomPickerInitialFilters = computed(() =>
  formData.value.materialCode
    ? isRawMaterialNcr.value
      ? { materialCode: formData.value.materialCode }
      : { productMaterialKeyword: formData.value.materialCode }
    : {},
);

const sourceTypeLabel = computed(() =>
  getOptionLabel(sourceTypeOptions.value, formData.value.sourceType),
);

const ncLevelLabel = computed(() =>
  getOptionLabel(ncLevelOptions.value, formData.value.ncLevel),
);
const responsibilityDeptCodesModel = computed<string[]>({
  get: () => normalizeMultiValue(formData.value.responsibilityDeptCodes),
  set: (values) => {
    const codes = normalizeMultiValue(values);
    formData.value.responsibilityDeptCodes = codes;
    formData.value.responsibilityDeptNames =
      resolveResponsibilityDeptNames(codes);
  },
});
const sourceBizTypeLabel = computed(() =>
  getOptionLabel(sourceBizTypeOptions.value, formData.value.sourceBizType),
);
const rawMaterialAbnormalCategoryLabel = computed(() =>
  getOptionLabel(
    rawMaterialAbnormalCategoryOptions,
    formData.value.rawMaterialAbnormalCategory,
  ),
);
const sourceNoLabel = computed(() =>
  isRawMaterialNcr.value ? 'IQC单号' : '来源单据',
);
const canOpenSourceDetail = computed(
  () =>
    !isRawMaterialNcr.value &&
    !!formData.value.sourceBizType &&
    !!formData.value.sourceId,
);
const canOpenIqcDetail = computed(
  () => isIqcSourceType(formData.value.sourceBizType) && !!getIqcSourceId(),
);
const iqcSourceRelations = computed(() => getIqcSourceRelations());
const canOpenNgDetail = computed(
  () =>
    canOpenIqcDetail.value ||
    (!!normalizeInspectionSourceType(formData.value.sourceBizType) &&
      !!formData.value.sourceId),
);
const ngDetailEntryText = computed(() => {
  if (canOpenIqcDetail.value) {
    if (iqcSourceRelations.value.length > 1) {
      return `已关联 ${iqcSourceRelations.value.length} 张 IQC 进料检验记录`;
    }
    return 'IQC 进料检验记录明细';
  }
  return canOpenNgDetail.value
    ? '对应检验单 NG 项目明细'
    : '未关联支持明细的检验单';
});
const ngDetailTitle = computed(
  () =>
    `不良品详情 - ${ngInspectionDetail.value?.inspectionNo || formData.value.sourceNo || '检验单'}`,
);
const ngAllDetailRows = computed<NgInspectionDetailRow[]>(() =>
  (ngInspectionDetail.value?.details || []).map((row, index) => ({
    ...row,
    __rowKey: `${row.rowNo ?? index + 1}-${row.sectionName || ''}-${row.inspectionItem || ''}`,
  })),
);
const ngDetailRows = computed<NgInspectionDetailRow[]>(() =>
  ngDetailOnlyNg.value
    ? ngAllDetailRows.value.filter((row) => isNgInspectionDetail(row))
    : ngAllDetailRows.value,
);
const hasNgSourceDetail = computed(() => {
  const sourceType = ngInspectionDetail.value?.sourceType;
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') {
    return !!ngFaiRecord.value;
  }
  if (sourceType === 'CUT_ROUND_FQC') return !!ngCutRoundRecord.value;
  if (sourceType === 'FG_SHIPPING_FQC') return !!ngFgShippingRecord.value;
  if (sourceType === 'OQC') return !!ngOqcRecord.value;
  return false;
});
const ngDetailNgFilterAvailable = computed(() => {
  const sourceType = ngInspectionDetail.value?.sourceType;
  return (
    !hasNgSourceDetail.value ||
    sourceType === 'CUT_ROUND_FQC' ||
    sourceType === 'FG_SHIPPING_FQC'
  );
});
const ngDetailSummaryText = computed(() => {
  const sourceType = ngInspectionDetail.value?.sourceType;
  if (sourceType === 'FAI' || sourceType === 'GLUE_BOARD_FAI') {
    const itemCount = ngFaiRecord.value?.items?.length ?? 0;
    return `${itemCount > 0 ? itemCount : ngDetailRows.value.length} 项`;
  }
  if (sourceType === 'CUT_ROUND_FQC') {
    const record = ngCutRoundRecord.value;
    if (!record) {
      return `${ngDetailRows.value.length} 项`;
    }
    if (!ngDetailOnlyNg.value) {
      return `${record.submissionDetails?.length || 0} 个片号 / ${countCutRoundItems(record, record.submissionDetails || [])} 项`;
    }
    const detailRows = (record.submissionDetails || []).filter((detail) =>
      hasNgCutRoundDetail(record, detail),
    );
    return `${detailRows.length} 个不良片号 / ${countNgCutRoundItems(record, detailRows)} 项不良项目`;
  }
  if (sourceType === 'FG_SHIPPING_FQC') {
    const record = ngFgShippingRecord.value;
    if (!record) {
      return `${ngDetailRows.value.length} 项`;
    }
    if (!ngDetailOnlyNg.value) {
      return `${record.shippingDetails?.length || 0} 个片号 / ${countFgShippingItems(record, record.shippingDetails || [])} 项`;
    }
    const detailRows = (record.shippingDetails || []).filter((detail) =>
      hasNgFgShippingDetail(record, detail),
    );
    return `${detailRows.length} 个不良片号 / ${countNgFgShippingItems(record, detailRows)} 项不良项目`;
  }
  if (sourceType === 'OQC') {
    const itemCount = ngOqcRecord.value?.items?.length ?? 0;
    return `${itemCount > 0 ? itemCount : ngDetailRows.value.length} 项`;
  }
  return `${ngDetailRows.value.length} 项`;
});
const ngDetailTableScroll = { x: 1760, y: 'calc(70vh - 190px)' };
const ngDetailColumns = [
  { align: 'center', dataIndex: 'rowNo', title: '序号', width: 64 },
  { dataIndex: 'sectionName', title: '明细来源', width: 130 },
  { dataIndex: 'inspectionItem', title: '检验项目', width: 220 },
  { dataIndex: 'itemType', title: '类型', width: 90 },
  { dataIndex: 'standardDesc', title: '标准/要求', width: 260 },
  { align: 'right', dataIndex: 'sampleSize', title: '样本数', width: 90 },
  { dataIndex: 'measuredValue', title: '实测/统计值', width: 170 },
  { dataIndex: 'unit', title: '单位', width: 80 },
  { align: 'center', dataIndex: 'result', title: '判定', width: 90 },
  { dataIndex: 'defectCode', title: '缺陷代码', width: 120 },
  { dataIndex: 'defectName', title: '缺陷名称', width: 140 },
  { dataIndex: 'abnormalDesc', title: '异常描述', width: 220 },
  { dataIndex: 'inspectorName', title: '检验员', width: 110 },
  { dataIndex: 'inspectionTime', title: '检验时间', width: 170 },
];
const getNgDetailRowKey = (record: NgInspectionDetailRow) => record.__rowKey;
const iqcRelationTableScroll = { x: 1046, y: 280 };
const iqcRelationColumns = [
  { align: 'center', dataIndex: 'index', title: '序号', width: 60 },
  { dataIndex: 'relatedObjectNo', title: 'IQC单号', width: 180 },
  { dataIndex: 'relatedObjectName', title: '供应商/对象', width: 180 },
  { dataIndex: 'materialInfo', title: '物料信息', width: 260 },
  { dataIndex: 'batchNo', title: '批次号', width: 150 },
  { dataIndex: 'arrivalDate', title: '来料日期', width: 120 },
  { align: 'center', dataIndex: 'action', title: '操作', width: 96 },
];
const getIqcRelationRowKey = (record: IqcSourceRelationRow) => record.__rowKey;
const otherDefectTableScroll = { x: 920, y: 154 };
const otherDefectColumns = [
  { align: 'center', dataIndex: 'index', title: '序号', width: 58 },
  { dataIndex: 'defectCode', title: '缺陷代码', width: 240 },
  { dataIndex: 'defectName', title: '缺陷名称', width: 180 },
  { dataIndex: 'sourceInspectionItem', title: '来源项目', width: 220 },
  { dataIndex: 'sourceSectionName', title: '来源分组', width: 140 },
  { align: 'center', dataIndex: 'action', title: '操作', width: 96 },
];
const reviewHandlerTableScroll = { y: 142 };
const reviewHandlerColumns = [
  { dataIndex: 'deptName', title: '会签单位', width: 180 },
  { dataIndex: 'handlerUserId', title: '会签人', width: 260 },
  { align: 'center', dataIndex: 'action', title: '操作', width: 118 },
];
const getOtherDefectRowKey = (record: NcrDefectRow) =>
  record.__rowKey ||
  record.id ||
  `${record.defectCode || 'defect'}-${record.sort || ''}`;
const getReviewHandlerRowKey = (record: MesNcrApi.MrbReview) =>
  record.id ||
  `${record.deptName || ''}-${record.handlerUserId || ''}-${record.sort || ''}`;

const canSaveDraft = computed(
  () =>
    isNew.value ||
    (canHandleCurrentRecord.value &&
      (formData.value.status === 'DRAFT' ||
        formData.value.status === 'RETURNED')),
);

const canOperate = computed(() => {
  if (isNew.value) {
    return true;
  }
  if (isDispositionNotifyReplyMode.value) {
    return true;
  }
  if (!canHandleCurrentRecord.value) {
    return false;
  }
  return !['CANCELLED', 'CLOSED'].includes(formData.value.status || '');
});

const canSelectRelatedException = computed(() => canOperate.value);

const isDraftLikeStage = computed(
  () =>
    isNew.value ||
    !formData.value.status ||
    ['DRAFT', 'RETURNED'].includes(formData.value.status),
);

const canEditPreSubmitFields = computed(
  () => !formDisabled.value && isDraftLikeStage.value,
);

const canEditContentConfirmDescription = computed(
  () => formData.value.status === 'CONTENT_CONFIRM' && canOperate.value,
);

const canEditDescription = computed(
  () => canEditPreSubmitFields.value || canEditContentConfirmDescription.value,
);

const canEditQualityConfirm = computed(
  () => formData.value.status === 'SUBMITTED' && canOperate.value,
);

const canEditQualityFields = computed(
  () => canEditPreSubmitFields.value || canEditQualityConfirm.value,
);

const canEditReviewHandlers = computed(
  () => canEditPreSubmitFields.value || canEditQualityConfirm.value,
);

const canEditFinalFields = computed(
  () =>
    formData.value.status === 'FINAL_APPROVAL' &&
    canOperate.value &&
    (!formData.value.currentHandlerUserId ||
      String(formData.value.currentHandlerUserId) ===
        String(currentUserId.value)),
);

const showQualitySection = computed(
  () => !isDraftLikeStage.value && formData.value.status !== 'CONTENT_CONFIRM',
);

const showTransferControls = computed(() =>
  ['EXECUTION_ASSIGN', 'REVIEW_ASSIGN'].includes(formData.value.status || ''),
);

const useMultipleNextHandlers = computed(
  () =>
    !isRawMaterialNcr.value &&
    formData.value.status === 'REVIEW_ASSIGN' &&
    transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION,
);

const needsNextHandler = computed(() => {
  if (needsContentConfirmUser.value) {
    return true;
  }
  if (showTransferControls.value) {
    return [RAW_TRANSFER_ROUTE_EXECUTION, RAW_TRANSFER_ROUTE_FINAL].includes(
      transferRoute.value || '',
    );
  }
  return nextHandlerRequiredStatuses.has(formData.value.status || '');
});

const nextHandlerLabel = computed(() => {
  if (needsContentConfirmUser.value) {
    return '再次确认人';
  }
  if (showTransferControls.value) {
    if (useMultipleNextHandlers.value) {
      return '范围确认办理人';
    }
    return transferRoute.value === RAW_TRANSFER_ROUTE_FINAL
      ? '终审办理人'
      : '处置执行人';
  }
  if (formData.value.status === 'REVIEW_ASSIGN') {
    return '终审办理人';
  }
  if (formData.value.status === 'EXECUTION_ASSIGN') {
    return '处置执行人';
  }
  return '下一办理人';
});

const showInlineNextHandler = computed(() => {
  if (needsContentConfirmUser.value) {
    return false;
  }
  if (showTransferControls.value) {
    if (!isRawMaterialNcr.value && formData.value.status === 'REVIEW_ASSIGN') {
      return transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION;
    }
    return transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION;
  }
  return needsNextHandler.value && formData.value.status !== 'REVIEW_ASSIGN';
});

const canEditTransferDecision = computed(
  () =>
    showTransferControls.value &&
    formData.value.status === 'REVIEW_ASSIGN' &&
    [RAW_TRANSFER_ROUTE_CLOSE, RAW_TRANSFER_ROUTE_EXECUTION].includes(
      transferRoute.value || '',
    ),
);

const showTransferMeasure = computed(
  () =>
    showTransferControls.value &&
    (formData.value.status === 'EXECUTION_ASSIGN' ||
      [RAW_TRANSFER_ROUTE_CLOSE, RAW_TRANSFER_ROUTE_EXECUTION].includes(
        transferRoute.value || '',
      )),
);

const finalApproverConfig = computed(() =>
  reviewConfigList.value.find(
    (item) => item.unitCode === FINAL_APPROVER_UNIT_CODE,
  ),
);

const finalApproverOptions = computed(() => {
  const userIds = finalApproverConfig.value?.handlerUserIds || [];
  const userNames = finalApproverConfig.value?.handlerUserNames || [];
  const seen = new Set<number>();
  return userIds
    .map((userId, index) => {
      const normalizedUserId = Number(userId);
      if (!normalizedUserId || seen.has(normalizedUserId)) {
        return undefined;
      }
      seen.add(normalizedUserId);
      const userOption = getUserOptionById(normalizedUserId);
      const label =
        userNames[index] || userOption?.label || String(normalizedUserId);
      const username = userOption?.username || '';
      return {
        label,
        searchText: `${label} ${username}`.toLowerCase(),
        username,
        value: normalizedUserId,
      };
    })
    .filter(Boolean) as UserSelectOption[];
});

const dispositionNotifyOptions = computed(() => {
  const executionUserIds = new Set(getSelectedNextHandlerUserIds());
  return userOptions.value.filter(
    (item) => !executionUserIds.has(Number(item.value)),
  );
});

const hasDispositionNotifies = computed(
  () => (formData.value.dispositionNotifies || []).length > 0,
);

const showDispositionNotifyPicker = computed(
  () =>
    showTransferControls.value &&
    transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION &&
    showInlineNextHandler.value,
);

const reviewDeptOptions = computed(
  () =>
    reviewConfigList.value
      .filter((item) => item.unitCode !== FINAL_APPROVER_UNIT_CODE)
      .map((item) => item.unitName)
      .filter(Boolean) as string[],
);

const showReviewSignSection = computed(
  () =>
    !isDraftLikeStage.value &&
    !!formData.value.reviews &&
    formData.value.reviews.length > 0,
);

const showActionSection = computed(
  () =>
    hasDispositionNotifies.value ||
    [
      'CLOSE_CONFIRM',
      'CLOSED',
      'EXECUTION_ASSIGN',
      'PENDING_STOCK_DISPOSE',
      'REVIEW_ASSIGN',
    ].includes(formData.value.status || ''),
);

const actionSectionLegend = computed(() => {
  if (isDispositionNotifyReplyMode.value) {
    return '通知人回复';
  }
  if (
    ['EXECUTION_ASSIGN', 'REVIEW_ASSIGN'].includes(formData.value.status || '')
  ) {
    return '品质部转办';
  }
  if (formData.value.status === 'PENDING_STOCK_DISPOSE') {
    return '不合格处置结果';
  }
  return '复检关闭';
});

const actionOpinionPlaceholder = computed(() => {
  if (formData.value.status === 'CLOSE_CONFIRM') {
    return '填写复检关闭办理说明';
  }
  if (formData.value.status === 'PENDING_STOCK_DISPOSE') {
    return '填写不合格处置结果上传办理说明（选填）';
  }
  return '填写本次办理说明';
});

const selectedDispositionPieceListText = computed(() => {
  const scopes = (dispositionExecution.value?.scopes || []).filter(
    (scope) => scope.scopeRole !== 'PICK_OUTSIDE_SCRAP',
  );
  const rows = scopes
    .map((scope, index) => {
      const pieceNo = resolveDispositionScopePieceNo(scope);
      if (!pieceNo) {
        return '';
      }
      const quantityText =
        scope.quantity === undefined || scope.quantity === null
          ? ''
          : `（数量${scope.quantity}）`;
      const dispositionText = scope.dispositionType
        ? `【${displayDisposition(scope.dispositionType)}】`
        : '';
      const resultText = scope.executionResult === 'PICK_QUALIFIED' ? '【合格（NCR挑选后）】'
        : scope.executionResult === 'PENDING_PICK' ? '【待确认挑选合格】' : '';
      const remarkText = `${resultText}${scope.remark ? `：${scope.remark}` : ''}`;
      return `${index + 1}. ${pieceNo}${dispositionText}${quantityText}${remarkText}`;
    })
    .filter(Boolean);
  if (rows.length > 0) {
    return rows.join('\n');
  }
  return displayValue(formData.value.finalDisposeDescription);
});

const stockDisposeUploadOpinion = computed(() => {
  const flowLog = [...(formData.value.flowLogs || [])]
    .reverse()
    .find((log) => log.actionCode === 'STOCK_DISPOSE');
  const opinion = String(flowLog?.opinion || '').trim();
  if (!opinion || isSystemStockDisposeOpinion(opinion)) {
    return '';
  }
  return opinion;
});

const showStockDisposeUploadOpinion = computed(
  () =>
    ['CLOSE_CONFIRM', 'CLOSED'].includes(formData.value.status || '') &&
    !!stockDisposeUploadOpinion.value,
);

const showFinalSection = computed(() => {
  if (
    formData.value.status === 'REVIEW_ASSIGN' &&
    !formData.value.finalApproveTime
  ) {
    return false;
  }
  return (
    formData.value.status === 'FINAL_APPROVAL' ||
    !!formData.value.finalApproveTime ||
    !!formData.value.finalDisposition ||
    !!formData.value.finalOpinion
  );
});

const showDisposeAssignmentSummary = computed(() => {
  if (formData.value.status === 'EXECUTION_ASSIGN' && !isRawMaterialNcr.value) {
    return false;
  }
  return (
    !!formData.value.stockDisposeQty ||
    !!formData.value.finalDisposeDescription ||
    !!dispositionExecution.value?.scopes?.length ||
    [
      'EXECUTION_ASSIGN',
      'PENDING_STOCK_DISPOSE',
      'CLOSE_CONFIRM',
      'CLOSED',
    ].includes(formData.value.status || '')
  );
});

const canReturn = computed(() => {
  if (isNew.value) {
    return false;
  }
  return (
    canOperate.value && returnableStatuses.has(formData.value.status || '')
  );
});

watch(
  selectedReviewDeptNames,
  (deptNames) => {
    if (suppressReviewDeptSync.value) {
      return;
    }
    formData.value.reviews = buildReviewRowsFromSelectedDepts(deptNames);
  },
  { deep: true },
);

watch(transferRoute, (route) => {
  if (
    !['EXECUTION_ASSIGN', 'REVIEW_ASSIGN'].includes(formData.value.status || '')
  ) {
    return;
  }
  finalApproverPickerConfirmed.value = false;
  nextHandlerUserId.value = undefined;
  nextHandlerUserIds.value = [];
  if (
    route === RAW_TRANSFER_ROUTE_FINAL &&
    formData.value.status === 'REVIEW_ASSIGN'
  ) {
    formData.value.finalDisposition = undefined;
    formData.value.finalOpinion = undefined;
    formData.value.finalDisposeDescription = undefined;
  }
  if (route === RAW_TRANSFER_ROUTE_EXECUTION) {
    applyDefaultNextHandler(true);
  } else {
    dispositionNotifyUserIds.value = [];
  }
});

watch(nextHandlerUserId, (userId) => {
  const userIds = userId ? [userId] : [];
  removeExecutionUsersFromNotify(userIds);
});

watch(
  nextHandlerUserIds,
  (userIds) => {
    removeExecutionUsersFromNotify(userIds);
  },
  { deep: true },
);

function removeExecutionUsersFromNotify(userIds?: Array<number | string>) {
  const executionUserIds = new Set(normalizeUserIdList(userIds));
  if (executionUserIds.size === 0 || dispositionNotifyUserIds.value.length === 0) {
    return;
  }
  dispositionNotifyUserIds.value = dispositionNotifyUserIds.value.filter(
    (item) => !executionUserIds.has(Number(item)),
  );
}

const [Modal, modalApi] = useVbenModal({
  title: '',
  class: 'qms-abnormal-workbench-modal qms-ncr-erp-modal',
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  header: false,
  fullscreen: true,
  fullscreenButton: false,
  showCancelButton: false,
  showConfirmButton: false,
  async onConfirm() {
    await handleModalConfirm();
  },
  onOpenChange(isOpen) {
    if (!isOpen) {
      resetNgDetailState();
      return;
    }
    void openModal();
  },
});

const toolbarActions = computed<ToolbarActionItem[]>(() => {
  const actions: ToolbarActionItem[] = [];

  if (canSaveDraft.value) {
    actions.push({
      key: 'save',
      label: '保存',
      onClick: handleSaveDraft,
      title: '保存',
    });
  }

  if (canOperate.value) {
    actions.push({
      key: 'primary',
      label: primaryActionLabel.value,
      onClick: handleModalConfirm,
      title: primaryActionLabel.value,
      type: 'primary',
    });
  }

  if (canReturn.value) {
    actions.push({
      danger: true,
      key: 'return',
      label: '退回',
      onClick: handleReturn,
      title: '退回',
    });
  }

  actions.push({
    key: 'close',
    label: '关闭',
    onClick: () => modalApi.close(),
    title: '关闭',
  });

  return actions;
});

function openLogDrawer(mode: 'audit' | 'operation') {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
}

function openAuditLog() {
  if (!formData.value.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      processInstanceId: formData.value.processInstanceId,
      title: `审批日志 - ${formData.value.ncNo || formTitle.value}`,
    })
    .open();
}

async function loadReviewConfigOptions(force = false) {
  if (reviewConfigLoaded.value && !force) {
    return;
  }
  const list = await getNcrReviewConfigSimpleList();
  reviewConfigList.value = list || [];
  reviewConfigLoaded.value = true;
}

function buildReviewRowsFromSelectedDepts(deptNames: string[]) {
  const existed = normalizeReviewList(formData.value.reviews);
  const nextReviews: MesNcrApi.MrbReview[] = [];
  let sort = 1;
  [...new Set(deptNames.filter(Boolean))].forEach((deptName) => {
    const config = reviewConfigList.value.find(
      (item) => item.unitName === deptName,
    );
    const existedRows = existed.filter((item) => item.deptName === deptName);
    if (existedRows.length > 0) {
      existedRows.forEach((row) => {
        nextReviews.push({
          ...row,
          deptId: config?.deptId || row.deptId,
          deptName,
          reviewStatus: row.reviewStatus || 'PENDING',
          sort: sort++,
        });
      });
      return;
    }
    const handlerUserIds = config?.handlerUserIds || [];
    if (handlerUserIds.length > 0) {
      handlerUserIds.forEach((handlerUserId, index) => {
        nextReviews.push({
          deptId: config?.deptId,
          deptName,
          handlerUserId,
          handlerUserName: config?.handlerUserNames?.[index],
          reviewStatus: 'PENDING',
          sort: sort++,
        });
      });
      return;
    }
    const existedRow = findExistingReviewRow(existed, deptName);
    nextReviews.push({
      ...existedRow,
      deptId: config?.deptId,
      deptName,
      reviewStatus: 'PENDING',
      sort: sort++,
    });
  });
  return nextReviews;
}

function findExistingReviewRow(
  reviews: MesNcrApi.MrbReview[],
  deptName: string,
  handlerUserId?: number,
) {
  if (handlerUserId !== undefined) {
    return reviews.find(
      (item) =>
        item.deptName === deptName &&
        String(item.handlerUserId || '') === String(handlerUserId),
    );
  }
  return reviews.find((item) => item.deptName === deptName);
}

function getReviewConfigByDeptName(deptName?: string) {
  return reviewConfigList.value.find((item) => item.unitName === deptName);
}

function createEmptyOpinionTemplates(): OpinionTemplateState {
  return {
    product: {},
    rawMaterial: {},
  };
}

function normalizeTemplateMap(value: unknown) {
  const result: Record<string, string> = {};
  if (!value || typeof value !== 'object') {
    return result;
  }
  Object.entries(value as Record<string, unknown>).forEach(([key, text]) => {
    const normalizedKey = key.trim().toUpperCase();
    const normalizedText = typeof text === 'string' ? text.trim() : '';
    if (normalizedKey && normalizedText) {
      result[normalizedKey] = normalizedText;
    }
  });
  return result;
}

function parseOpinionTemplateJson(value?: string) {
  const templates = createEmptyOpinionTemplates();
  if (!value?.trim()) {
    return templates;
  }
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    templates.product = normalizeTemplateMap(parsed.product);
    templates.rawMaterial = normalizeTemplateMap(parsed.rawMaterial);
    if (
      Object.keys(templates.product).length === 0 &&
      Object.keys(templates.rawMaterial).length === 0
    ) {
      const flatTemplates = normalizeTemplateMap(parsed);
      templates.product = { ...flatTemplates };
      templates.rawMaterial = { ...flatTemplates };
    }
  } catch {
    return templates;
  }
  return templates;
}

function getOpinionTemplateMap(config?: MesNcrApi.ReviewConfig) {
  const templates = parseOpinionTemplateJson(config?.opinionTemplateJson);
  return isRawMaterialNcr.value ? templates.rawMaterial : templates.product;
}

function getTemplateContext(disposition: string, row?: MesNcrApi.MrbReview) {
  const dispositionLabel = getOptionLabel(
    effectiveDispositionOptions.value,
    disposition,
  );
  const handlerUserName =
    row?.handlerUserName || getUserNameById(row?.handlerUserId) || '';
  const materialText = firstText(
    formData.value.materialName,
    formData.value.materialCode,
  );
  return {
    defectName: firstText(formData.value.defectName),
    defectQty: firstText(formData.value.defectQty),
    deptName: firstText(row?.deptName),
    disposition: dispositionLabel,
    dispositionLabel,
    handlerUserName,
    lotNo: firstText(formData.value.lotNo),
    materialCode: firstText(formData.value.materialCode),
    materialName: firstText(formData.value.materialName),
    ncDescription: firstText(formData.value.ncDescription),
    productModel: materialText,
    productName: materialText,
    sourceNo: firstText(formData.value.sourceNo),
    specification: firstText(formData.value.specification),
    unitCode: firstText(formData.value.unitCode),
    不合格数量: firstText(formData.value.defectQty),
    不良描述: firstText(formData.value.ncDescription),
    会签人: handlerUserName,
    单位: firstText(formData.value.unitCode),
    品名: materialText,
    处置选项: dispositionLabel,
    批号: firstText(formData.value.lotNo),
    数量: firstText(formData.value.defectQty),
    来源单号: firstText(formData.value.sourceNo),
    物料名称: firstText(formData.value.materialName),
    规格: firstText(formData.value.specification),
    会签单位: firstText(row?.deptName),
    责任单位: firstText(row?.deptName),
    缺陷名称: firstText(formData.value.defectName),
  } as Record<string, string>;
}

function renderOpinionTemplate(
  template: string,
  disposition: string,
  row?: MesNcrApi.MrbReview,
) {
  const context = getTemplateContext(disposition, row);
  return template
    .replace(
      /\{\{\s*([^{}]+?)\s*\}\}|\{\s*([^{}]+?)\s*\}/g,
      (match, key1, key2) => {
        const key = String(key1 || key2 || '').trim();
        return Object.prototype.hasOwnProperty.call(context, key)
          ? context[key] || ''
          : match;
      },
    )
    .trim();
}

function buildOpinionTemplateText(
  config: MesNcrApi.ReviewConfig | undefined,
  dispositions: string[],
  row?: MesNcrApi.MrbReview,
) {
  const templates = getOpinionTemplateMap(config);
  return dispositions
    .map((disposition) => {
      const template = templates[disposition];
      return template ? renderOpinionTemplate(template, disposition, row) : '';
    })
    .filter(Boolean)
    .join('\n');
}

function canApplyOpinionTemplate(current: unknown, lastTemplate?: string) {
  const currentText = firstText(current);
  return !currentText || currentText === firstText(lastTemplate);
}

function applyReviewOpinionTemplate(
  row: MesNcrApi.MrbReview,
  dispositions: string[],
) {
  const templateText = buildOpinionTemplateText(
    getReviewConfigByDeptName(row.deptName),
    dispositions,
    row,
  );
  if (!templateText) {
    return;
  }
  const target = row as AutoTemplateReview;
  const currentText = row.dispositionDetail || row.reviewOpinion;
  if (canApplyOpinionTemplate(currentText, target.__lastOpinionTemplateText)) {
    row.dispositionDetail = templateText;
    row.reviewOpinion = templateText;
  }
  target.__lastOpinionTemplateText = templateText;
}

function applyFinalOpinionTemplate(dispositions: string[]) {
  const templateText = buildOpinionTemplateText(
    finalApproverConfig.value,
    dispositions,
  );
  if (!templateText) {
    return;
  }
  if (
    canApplyOpinionTemplate(
      formData.value.finalOpinion,
      finalOpinionAutoTemplateText.value,
    )
  ) {
    formData.value.finalOpinion = templateText;
  }
  finalOpinionAutoTemplateText.value = templateText;
}

function getUserNameById(userId?: number) {
  if (!userId) {
    return undefined;
  }
  return userOptions.value.find((item) => String(item.value) === String(userId))
    ?.label;
}

function buildUserOption(
  user: SystemUserApi.User,
): UserSelectOption | undefined {
  if (!user.id) {
    return undefined;
  }
  const label = user.nickname || user.username || String(user.id);
  const username = user.username || '';
  return {
    label,
    searchText: `${label} ${username}`.toLowerCase(),
    username,
    value: user.id,
  };
}

function syncSelectedUserOption(user: SystemUserApi.User) {
  const option = buildUserOption(user);
  if (!option) {
    return;
  }
  const nextOptions = userOptions.value.filter(
    (item) => String(item.value) !== String(option.value),
  );
  userOptions.value = [option, ...nextOptions];
}

function getUserOptionById(userId?: number) {
  if (!userId) {
    return undefined;
  }
  return userOptions.value.find(
    (item) => String(item.value) === String(userId),
  );
}

function handleReviewHandlerChange(
  row: MesNcrApi.MrbReview,
  handlerUserId?: number | string,
) {
  const normalizedUserId =
    handlerUserId === undefined || handlerUserId === null
      ? undefined
      : Number(handlerUserId);
  row.handlerUserId = Number.isNaN(normalizedUserId)
    ? undefined
    : normalizedUserId;
  row.handlerUserName = getUserNameById(row.handlerUserId) || undefined;
}

function addReviewHandlerRow(deptName?: string) {
  const effectiveDeptName =
    deptName || selectedReviewDeptNames.value[0] || reviewDeptOptions.value[0];
  if (!effectiveDeptName) {
    message.warning('请先选择会签单位');
    return;
  }
  if (!selectedReviewDeptNames.value.includes(effectiveDeptName)) {
    setSelectedReviewDeptNames([
      ...selectedReviewDeptNames.value,
      effectiveDeptName,
    ]);
  }
  const config = getReviewConfigByDeptName(effectiveDeptName);
  const reviews = normalizeReviewList(formData.value.reviews);
  reviews.push({
    deptId: config?.deptId,
    deptName: effectiveDeptName,
    reviewStatus: 'PENDING',
    sort: reviews.length + 1,
  });
  formData.value.reviews = refreshReviewSort(reviews);
}

function removeReviewHandlerRow(row: MesNcrApi.MrbReview) {
  const rowKey = getReviewHandlerRowKey(row);
  const reviews = normalizeReviewList(formData.value.reviews).filter(
    (item) => getReviewHandlerRowKey(item) !== rowKey,
  );
  formData.value.reviews = refreshReviewSort(reviews);
  setSelectedReviewDeptNames([
    ...new Set(
      reviews.map((item) => item.deptName).filter(Boolean) as string[],
    ),
  ]);
}

function resetReviewHandlersByConfig(deptName?: string) {
  if (!deptName) {
    return;
  }
  const config = getReviewConfigByDeptName(deptName);
  const reviews = normalizeReviewList(formData.value.reviews).filter(
    (item) => item.deptName !== deptName,
  );
  const handlerUserIds = config?.handlerUserIds || [];
  const defaultRows: MesNcrApi.MrbReview[] =
    handlerUserIds.length > 0
      ? handlerUserIds.map((handlerUserId, index) => ({
          deptId: config?.deptId,
          deptName,
          handlerUserId,
          handlerUserName: config?.handlerUserNames?.[index],
          reviewStatus: 'PENDING',
        }))
      : [
          {
            deptId: config?.deptId,
            deptName,
            reviewStatus: 'PENDING',
          },
        ];
  formData.value.reviews = refreshReviewSort([...reviews, ...defaultRows]);
}

function refreshReviewSort(reviews: MesNcrApi.MrbReview[]) {
  return reviews.map((review, index) => ({
    ...review,
    sort: index + 1,
  }));
}

const leadingToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return toolbarActions.value;
  }
  return toolbarActions.value.slice(0, TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW);
});

const overflowToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return [];
  }
  return toolbarActions.value.slice(TOOLBAR_LEADING_COUNT_WHEN_OVERFLOW, -1);
});

const trailingToolbarActions = computed(() => {
  if (toolbarActions.value.length <= MAX_VISIBLE_TOOLBAR_ACTIONS) {
    return [];
  }
  return toolbarActions.value.slice(-1);
});

async function handleModalConfirm() {
  if (!canOperate.value && !isNew.value) {
    modalApi.close();
    return;
  }
  try {
    const confirmed = await handleConfirm();
    if (!confirmed) {
      return;
    }
    emit('success');
    modalApi.close();
  } finally {
    finalApproverPickerConfirmed.value = false;
    modalApi.setState({ loading: false });
  }
}

async function openModal() {
  const data = modalApi.getData() || {};
  isNew.value = !!data.isNew;
  logDrawerVisible.value = false;
  logDrawerMode.value = 'audit';
  actionOpinion.value = '';
  finalOpinionAutoTemplateText.value = '';
  dispositionNotifyUserIds.value = [];
  dispositionNotifyReplyConclusion.value = '';
  nextHandlerUserId.value = undefined;
  nextHandlerUserIds.value = [];
  transferRoute.value = undefined;
  reviewDelegateTarget.value = undefined;
  userSelectPurpose.value = undefined;
  contentConfirmUserSelectResolver.value = undefined;
  finalApproverPickerOpen.value = false;
  finalApproverPickerConfirmed.value = false;
  stockResult.value = '';
  dispositionAttachmentUrls.value = [];
  dispositionExecution.value = undefined;
  resetNgDetailState();
  void loadProcessOptions();
  void loadUserOptions();
  await loadDeptOptions();

  if (isNew.value) {
    const rawMaterialMode =
      data.sourceType === 'RAW_MATERIAL' || data.rawMaterial;
    const defaultHappenDeptName = rawMaterialMode ? '品质部' : undefined;
    formData.value = {
      sourceType: rawMaterialMode ? 'RAW_MATERIAL' : 'FINISHED_PRODUCT',
      sourceTypeName: rawMaterialMode ? '原材料' : undefined,
      sourceBizType: rawMaterialMode ? 'IQC' : undefined,
      sourceBizTypeName: rawMaterialMode ? '进料检验(IQC)' : undefined,
      happenTime: dayjs().format('YYYY-MM-DD HH:mm:ss'),
      happenDeptId: findDeptIdByName(defaultHappenDeptName),
      happenDeptName: defaultHappenDeptName,
      isolatedFlag: rawMaterialMode ? false : undefined,
      rawMaterialAbnormalCategory: rawMaterialMode ? 'INCOMING' : undefined,
      rawMaterialAbnormalCategoryName: rawMaterialMode
        ? '来料品异常'
        : undefined,
      createExceptionFlag: false,
      responsibilityDeptCodes: [],
      defects: [],
      reviews: [],
      relations: [],
    };
    setSelectedReviewDeptNames([]);
    attachmentUrls.value = [];
    dispositionAttachmentUrls.value = [];
  } else if (data.id) {
    modalApi.setState({ loading: true });
    try {
      const record = await fetchNcrRecord(
        data.id,
        data.sourceType === 'RAW_MATERIAL',
      );
      applyLoadedRecord(record, data.tabType);
      await loadDispositionExecutionState();
    } finally {
      modalApi.setState({ loading: false });
    }
  } else {
    const reviews = normalizeReviewList(data.reviews);
    formData.value = {
      ...data,
      createExceptionFlag: data.createExceptionFlag === true,
      responsibilityDeptCodes: normalizeMultiValue(
        data.responsibilityDeptCodes,
      ),
      defects: normalizeDefectRows(
        data.defects,
        data.defectCode,
        data.defectName,
      ),
      reviews,
    };
    setSelectedReviewDeptNames(
      reviews
        .map((item: MesNcrApi.MrbReview) => item.deptName)
        .filter(Boolean) as string[],
    );
    syncAttachmentUrls(data.relations);
    syncDispositionAttachmentUrls(data.relations);
    syncDispositionNotifyUserIds(data.dispositionNotifies);
  }
  await loadReviewConfigOptions();
  syncHappenDeptFromDeptOptions();
  applyDefaultReviewAssignDecision();
  applyDefaultNextHandler();
  void hydrateDefectsFromSourceIfNeeded();
  syncModalButtons();
}

function applyLoadedRecord(record: MesNcrApi.NcrRecord, tabType?: string) {
  const reviews = normalizeReviewList(record.reviews);
  formData.value = {
    ...record,
    createExceptionFlag: record.createExceptionFlag === true,
    responsibilityDeptCodes: normalizeMultiValue(
      record.responsibilityDeptCodes,
    ),
    defects: normalizeDefectRows(
      record.defects,
      record.defectCode,
      record.defectName,
    ),
    reviews,
    tabType: tabType ?? record.tabType,
  };
  setSelectedReviewDeptNames([
    ...new Set(
      reviews.map((item) => item.deptName).filter(Boolean) as string[],
    ),
  ]);
  syncAttachmentUrls(record.relations);
  syncDispositionAttachmentUrls(record.relations);
  syncDispositionNotifyUserIds(record.dispositionNotifies);
  dispositionNotifyReplyConclusion.value =
    pendingDispositionNotify.value?.replyConclusion || '';
  transferRoute.value =
    record.status === 'EXECUTION_ASSIGN'
      ? RAW_TRANSFER_ROUTE_EXECUTION
      : undefined;
}

function applyDefaultReviewAssignDecision() {
  if (formData.value.status !== 'REVIEW_ASSIGN') {
    return;
  }
  if (transferRoute.value || formData.value.finalDisposition) {
    return;
  }
  const unanimousDisposition = getUnanimousReviewDisposition(
    formData.value.reviews,
  );
  if (!unanimousDisposition) {
    return;
  }
  transferRoute.value = RAW_TRANSFER_ROUTE_EXECUTION;
  formData.value.finalDisposition = unanimousDisposition;
}

function getUnanimousReviewDisposition(reviews?: MesNcrApi.MrbReview[]) {
  const reviewRows = normalizeReviewList(reviews).filter(
    (row) => row.id || row.deptName || row.handlerUserId,
  );
  if (reviewRows.length === 0) {
    return undefined;
  }
  const dispositions = reviewRows.map((row) =>
    normalizeDispositionString(row.suggestedDisposition),
  );
  if (dispositions.some((item) => !item)) {
    return undefined;
  }
  const uniqueDispositions = [...new Set(dispositions)];
  if (uniqueDispositions.length !== 1) {
    return undefined;
  }
  const disposition = uniqueDispositions[0];
  return parseDispositionValues(disposition).every((value) =>
    effectiveDispositionOptions.value.some((option) => option.value === value),
  )
    ? disposition
    : undefined;
}

function setSelectedReviewDeptNames(deptNames: string[]) {
  suppressReviewDeptSync.value = true;
  selectedReviewDeptNames.value = deptNames;
  Promise.resolve().then(() => {
    suppressReviewDeptSync.value = false;
  });
}

async function reloadCurrentRecord() {
  if (!formData.value.id) {
    return;
  }
  const tabType = formData.value.tabType;
  const record = await fetchNcrRecord(
    formData.value.id,
    isRawMaterialNcr.value,
  );
  applyLoadedRecord(record, tabType);
  await loadDispositionExecutionState();
}

async function refreshCurrentDetail() {
  await reloadCurrentRecord();
  syncModalButtons();
}

async function loadDispositionExecutionState() {
  dispositionExecution.value = undefined;
  if (
    !formData.value.id ||
    isRawMaterialNcr.value ||
    !['CLOSE_CONFIRM', 'CLOSED', 'PENDING_STOCK_DISPOSE'].includes(
      formData.value.status || '',
    )
  ) {
    return;
  }
  try {
    const context = await getNcrDispositionContext(formData.value.id);
    dispositionExecution.value = context.existingExecution;
  } catch {
    // 兼容阶段1之前创建、没有 V2 执行单的历史产品 NCR。
  }
}

function fetchNcrRecord(id: number, rawMaterialMode = false) {
  return rawMaterialMode ? getRawMaterialNcrRecord(id) : getNcrRecord(id);
}

function createCurrentNcrRecord(data: MesNcrApi.NcrRecord) {
  return data.sourceType === 'RAW_MATERIAL'
    ? createRawMaterialNcrRecord(data)
    : createNcrRecord(data);
}

function updateCurrentNcrRecord(data: MesNcrApi.NcrRecord) {
  return data.sourceType === 'RAW_MATERIAL'
    ? updateRawMaterialNcrRecord(data)
    : updateNcrRecord(data);
}

function submitCurrentNcr(data: MesNcrApi.SubmitReq) {
  return isRawMaterialNcr.value ? submitRawMaterialNcr(data) : submitNcr(data);
}

function handleCurrentNcr(data: MesNcrApi.HandleReq) {
  return isRawMaterialNcr.value ? handleRawMaterialNcr(data) : handleNcr(data);
}

function delegateCurrentNcrReview(data: MesNcrApi.MrbReviewDelegateReq) {
  return isRawMaterialNcr.value
    ? delegateRawMaterialNcrMrbReview(data)
    : delegateNcrMrbReview(data);
}

function returnCurrentNcr(data: MesNcrApi.ReturnReq) {
  return isRawMaterialNcr.value ? returnRawMaterialNcr(data) : returnNcr(data);
}

function finalApproveCurrentNcr(data: MesNcrApi.FinalApproveReq) {
  return isRawMaterialNcr.value
    ? finalApproveRawMaterialNcr(data)
    : finalApproveNcr(data);
}

function stockDisposeCurrentNcr(data: MesNcrApi.StockDisposeReq) {
  return isRawMaterialNcr.value
    ? stockDisposeRawMaterialNcr(data)
    : stockDisposeNcr(data);
}

function replyCurrentNcrDispositionNotify(
  data: MesNcrApi.DispositionNotifyReplyReq,
) {
  return isRawMaterialNcr.value
    ? replyRawMaterialNcrDispositionNotify(data)
    : replyNcrDispositionNotify(data);
}

function syncModalButtons() {
  modalApi.setState({
    showCancelButton: false,
    showConfirmButton: false,
  });
}

async function loadProcessOptions() {
  if (processOptionsLoaded.value || processOptionsLoading.value) {
    return;
  }
  processOptionsLoading.value = true;
  try {
    const res = await getProcessPage({
      pageNo: 1,
      pageSize: 200,
      status: 0,
    });
    processOptions.value = (res.list || []).map((row) => ({
      label: row.name || row.code || '',
      raw: row,
      value: row.name || row.code || '',
    }));
    processOptionsLoaded.value = true;
  } finally {
    processOptionsLoading.value = false;
  }
}

async function loadUserOptions() {
  if (userOptionsLoaded.value || userOptionsLoading.value) {
    return;
  }
  userOptionsLoading.value = true;
  try {
    const list = await getSimpleUserList();
    userOptions.value = (list || [])
      .filter((user) => user.id)
      .map((user) => {
        const label = user.nickname || user.username || String(user.id);
        const username = user.username || '';
        return {
          label,
          searchText: `${label} ${username}`.toLowerCase(),
          username,
          value: user.id!,
        };
      });
    userOptionsLoaded.value = true;
  } finally {
    userOptionsLoading.value = false;
  }
}

function filterUserOption(input: string, option?: UserSelectOption) {
  const keyword = input.trim().toLowerCase();
  if (!keyword) {
    return true;
  }
  return (option?.searchText || '').includes(keyword);
}

async function loadDeptOptions() {
  if (deptOptions.value.length > 0) {
    return;
  }
  deptOptions.value = await getSimpleDeptList();
}

function normalizePositiveNumber(value: unknown) {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue > 0
    ? numberValue
    : undefined;
}

function findDeptName(deptId?: number) {
  if (!deptId) {
    return undefined;
  }
  return deptOptions.value.find((dept) => dept.id === deptId)?.name;
}

function findDeptIdByName(deptName?: string) {
  if (!deptName) {
    return undefined;
  }
  return deptOptions.value.find((dept) => dept.name === deptName)?.id;
}

function syncHappenDeptFromDeptOptions() {
  if (!isRawMaterialNcr.value) {
    return;
  }
  const deptId = normalizePositiveNumber(formData.value.happenDeptId);
  if (deptId) {
    formData.value.happenDeptId = deptId;
    formData.value.happenDeptName =
      formData.value.happenDeptName || findDeptName(deptId);
    return;
  }
  formData.value.happenDeptId = findDeptIdByName(formData.value.happenDeptName);
}

function handleHappenDeptChange(value?: number | string) {
  const deptId = normalizePositiveNumber(value);
  formData.value.happenDeptId = deptId;
  formData.value.happenDeptName = findDeptName(deptId);
}

function getNextHandlerUserName() {
  if (!nextHandlerUserId.value) {
    return undefined;
  }
  if (formData.value.status === 'REVIEW_ASSIGN') {
    const finalApprover = finalApproverOptions.value.find(
      (item) => String(item.value) === String(nextHandlerUserId.value),
    );
    if (finalApprover?.label) {
      return finalApprover.label;
    }
  }
  if (
    String(nextHandlerUserId.value) === String(formData.value.applicantUserId)
  ) {
    return (
      formData.value.applicantUserName ||
      getUserNameById(nextHandlerUserId.value)
    );
  }
  if (
    String(nextHandlerUserId.value) ===
    String(formData.value.stockDisposeUserId)
  ) {
    return (
      formData.value.stockDisposeUserName ||
      getUserNameById(nextHandlerUserId.value)
    );
  }
  return getUserNameById(nextHandlerUserId.value);
}

function getFinalApproverPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || document.body;
}

function applyDefaultNextHandler(force = false) {
  if (!needsNextHandler.value) {
    return;
  }
  if (nextHandlerUserId.value && !force) {
    return;
  }
  if (needsContentConfirmUser.value) {
    return;
  }
  if (formData.value.status === 'REVIEW_ASSIGN') {
    if (transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION) {
      if (isRawMaterialNcr.value) {
        nextHandlerUserId.value = formData.value.applicantUserId;
      }
      return;
    }
    if (transferRoute.value === RAW_TRANSFER_ROUTE_FINAL) {
      const defaultUserId = finalApproverOptions.value[0]?.value;
      if (defaultUserId) {
        nextHandlerUserId.value = Number(defaultUserId);
      }
    }
    return;
  }
  if (formData.value.status === 'EXECUTION_ASSIGN') {
    nextHandlerUserId.value =
      formData.value.stockDisposeUserId || formData.value.applicantUserId;
    return;
  }
  nextHandlerUserId.value =
    formData.value.currentHandlerUserId || nextHandlerUserId.value;
}

async function openFinalApproverPicker() {
  await loadReviewConfigOptions(true);
  if (finalApproverOptions.value.length === 0) {
    message.warning('请先在 NCR评审会签配置中维护默认终审人');
    return;
  }
  const selectedExists = finalApproverOptions.value.some(
    (item) => String(item.value) === String(nextHandlerUserId.value),
  );
  if (!selectedExists) {
    nextHandlerUserId.value = finalApproverOptions.value[0]?.value;
  }
  finalApproverPickerOpen.value = true;
}

async function handleFinalApproverConfirm() {
  if (finalApproverOptions.value.length === 0) {
    message.warning('请先在 NCR评审会签配置中维护默认终审人');
    return;
  }
  if (!nextHandlerUserId.value) {
    message.warning('请选择终审办理人');
    return;
  }
  finalApproverPickerOpen.value = false;
  finalApproverPickerConfirmed.value = true;
  await handleModalConfirm();
}

function handleProcessSelect(
  _value: number | string,
  option:
    | Array<{ raw?: MesProcessApi.Process }>
    | { raw?: MesProcessApi.Process },
) {
  const selected = Array.isArray(option) ? option[0]?.raw : option?.raw;
  if (!selected) {
    return;
  }
  formData.value.processId = selected.id;
  formData.value.processName = selected.name || selected.code;
  applyUnitDefaultByProcess(selected.name || selected.code);
}

function applyUnitDefaultByProcess(processName?: string) {
  const unitCode = resolveDefaultUnitByProcess(processName);
  if (unitCode) {
    formData.value.unitCode = unitCode;
  }
}

function resolveDefaultUnitByProcess(processName?: string) {
  const name = String(processName || '').trim();
  if (!name) {
    return undefined;
  }
  if (name.includes('配料')) {
    return 'kg';
  }
  if (
    name.includes('湿法') ||
    name.includes('磨皮') ||
    name.includes('粘胶1') ||
    name.includes('粘胶一') ||
    name.includes('胶板') ||
    name.includes('胶板检验')
  ) {
    return 'm';
  }
  if (
    name.includes('分切') ||
    name.includes('压槽') ||
    name.includes('粘胶2') ||
    name.includes('粘胶二') ||
    name.includes('裁切') ||
    name.includes('包装') ||
    name.includes('首检') ||
    name.includes('成品') ||
    name.includes('发货') ||
    name.includes('出货')
  ) {
    return '片';
  }
  return undefined;
}

function openProductBomPicker() {
  if (!canEditPreSubmitFields.value) {
    return;
  }
  productBomPickerOpen.value = true;
}

function handleProductMaterialInput(value: string) {
  if (!canEditPreSubmitFields.value) {
    return;
  }
  formData.value.materialId = undefined;
  formData.value.materialCode = String(value || '');
  formData.value.materialName = '';
  formData.value.specification = '';
  if (isRawMaterialNcr.value) {
    formData.value.unitCode = '';
  }
}

function handleProductBomPick(option: PickerOption) {
  if (isRawMaterialNcr.value) {
    const row = option.raw as MesHcMaterialApi.Material;
    formData.value.materialId = row.id;
    formData.value.materialCode = row.materialCode || option.code;
    formData.value.materialName = row.materialName || option.name;
    formData.value.specification =
      row.specModel || row.modelCode || formData.value.specification;
    formData.value.unitCode =
      row.baseUom || row.baseUnitCode || formData.value.unitCode;
    productBomPickerOpen.value = false;
    return;
  }
  const row = option.raw as MesHcBomApi.Bom;
  formData.value.materialId = row.productMaterialId;
  formData.value.materialCode = row.productMaterialCode || option.code;
  formData.value.materialName =
    row.productModelCode || row.productMaterialName || option.name;
  formData.value.specification =
    row.productSpec || formData.value.specification;
  productBomPickerOpen.value = false;
}

function syncAttachmentUrls(relations?: MesNcrApi.Relation[]) {
  attachmentUrls.value = (relations || [])
    .filter((relation) => relation.relationType === ATTACHMENT_RELATION_TYPE)
    .map((relation) => relation.remark || relation.relatedObjectNo || '')
    .filter(Boolean);
}

function syncDispositionAttachmentUrls(relations?: MesNcrApi.Relation[]) {
  dispositionAttachmentUrls.value = (relations || [])
    .filter(
      (relation) =>
        relation.relationType === DISPOSITION_ATTACHMENT_RELATION_TYPE,
    )
    .map((relation) => relation.remark || relation.relatedObjectNo || '')
    .filter(Boolean);
}

function syncDispositionNotifyUserIds(
  notifies?: MesNcrApi.DispositionNotify[],
) {
  dispositionNotifyUserIds.value = [
    ...new Set(
      (notifies || [])
        .filter((item) => item.notifyStatus === 'PENDING')
        .map((item) => toPositiveNumber(item.notifyUserId))
        .filter(Boolean) as number[],
    ),
  ];
}

function normalizeUserIdList(values?: Array<number | string>) {
  return [
    ...new Set(
      (values || [])
        .map((value) => toPositiveNumber(value))
        .filter(Boolean) as number[],
    ),
  ];
}

function getSelectedNextHandlerUserIds() {
  if (useMultipleNextHandlers.value) {
    return normalizeUserIdList(nextHandlerUserIds.value);
  }
  return nextHandlerUserId.value ? [Number(nextHandlerUserId.value)] : [];
}

function getNextHandlerUserNamesByIds(userIds: number[]) {
  return userIds.map((userId) => getUserNameById(userId) || String(userId));
}

function buildNextHandlerPayload() {
  const ids = getSelectedNextHandlerUserIds();
  const names = getNextHandlerUserNamesByIds(ids);
  return {
    nextHandlerUserId: ids[0],
    nextHandlerUserIds: useMultipleNextHandlers.value ? ids : undefined,
    nextHandlerUserName: names[0],
    nextHandlerUserNames: useMultipleNextHandlers.value ? names : undefined,
  };
}

function buildDispositionNotifyPayload() {
  const executionUserIds = new Set(getSelectedNextHandlerUserIds());
  const ids = normalizeUserIdList(dispositionNotifyUserIds.value).filter(
    (userId) => !executionUserIds.has(Number(userId)),
  );
  return {
    dispositionNotifyUserIds: ids,
    dispositionNotifyUserNames: ids.map(
      (userId) => getUserNameById(userId) || String(userId),
    ),
  };
}

function handleDispositionNotifyChange(values?: Array<number | string>) {
  dispositionNotifyUserIds.value = normalizeUserIdList(values);
}

function handleNextHandlerUsersChange(values?: Array<number | string>) {
  nextHandlerUserIds.value = normalizeUserIdList(values);
}

function buildRelationsPayload() {
  const sourceRelations = (formData.value.relations || []).filter(
    (relation) => relation.relationType !== ATTACHMENT_RELATION_TYPE,
  );
  const attachmentRelations = attachmentUrls.value
    .filter(Boolean)
    .map((url, index) => ({
      relationType: ATTACHMENT_RELATION_TYPE,
      relatedObjectName: getAttachmentName(url, index),
      relatedObjectNo: url,
      relationStatus: 'ACTIVE',
      primaryFlag: false,
      remark: url,
    }));
  return [...sourceRelations, ...attachmentRelations];
}

function buildDispositionAttachmentPayload() {
  return dispositionAttachmentUrls.value.filter(Boolean).map((url, index) => ({
    relationType: DISPOSITION_ATTACHMENT_RELATION_TYPE,
    relatedObjectName: getAttachmentName(url, index),
    relatedObjectNo: url,
    relationStatus: 'ACTIVE',
    primaryFlag: false,
    remark: url,
  }));
}

function getAttachmentName(url: string, index: number) {
  const cleanUrl = String(url || '').split('?')[0] || '';
  const fileName = cleanUrl.split('/').pop();
  if (!fileName) {
    return `附件${index + 1}`;
  }
  try {
    return decodeURIComponent(fileName);
  } catch {
    return fileName;
  }
}

function handleAttachmentPreview(file: any) {
  const url =
    file?.url ||
    file?.response?.url ||
    file?.response?.data ||
    file?.response ||
    '';
  if (url) {
    window.open(String(url), '_blank');
  }
}

function handleDefectChange(value?: string) {
  formData.value.defectCode = value;
  syncPrimaryDefectRowFromMain();
}

function handleDefectSelect(defect: DefectSelectPayload) {
  formData.value.defectCode = defect.code || formData.value.defectCode;
  formData.value.defectName = defect.name;
  if (!formData.value.ncLevel) {
    formData.value.ncLevel = defect.level;
  }
  syncPrimaryDefectRowFromMain(defect);
}

function handleDefectNameChange() {
  syncPrimaryDefectRowFromMain();
}

function buildLocalRowKey(prefix: string) {
  localRowSeed += 1;
  return `${prefix}-${Date.now()}-${localRowSeed}`;
}

function firstText(...values: Array<unknown>) {
  return (
    values
      .map((value) =>
        value === undefined || value === null ? '' : String(value).trim(),
      )
      .find(Boolean) || ''
  );
}

function normalizeDefectRows(
  defects?: MesNcrApi.NcDefect[],
  mainDefectCode?: string,
  mainDefectName?: string,
  keepBlankRows = false,
) {
  const rows = (defects || [])
    .filter(
      (defect) =>
        keepBlankRows ||
        !isBlank(defect.defectCode) ||
        !isBlank(defect.defectName),
    )
    .map((defect, index) => ({
      ...defect,
      __rowKey:
        (defect as NcrDefectRow).__rowKey ||
        (defect.id ? `defect-id-${defect.id}` : buildLocalRowKey('defect')),
      primaryFlag: Boolean(defect.primaryFlag),
      sort: defect.sort || index + 1,
    })) as NcrDefectRow[];
  if (
    rows.length === 0 &&
    (!isBlank(mainDefectCode) || !isBlank(mainDefectName))
  ) {
    rows.push({
      __rowKey: buildLocalRowKey('defect'),
      defectCode: mainDefectCode,
      defectName: mainDefectName,
      primaryFlag: true,
      sort: 1,
    });
  }
  if (rows.length > 0 && !rows.some((row) => row.primaryFlag)) {
    rows[0]!.primaryFlag = true;
  }
  return rows.map((row, index) => ({ ...row, sort: index + 1 }));
}

function ensureDefectRows() {
  const rows = normalizeDefectRows(
    formData.value.defects,
    formData.value.defectCode,
    formData.value.defectName,
  );
  formData.value.defects = rows;
  return rows;
}

function syncPrimaryDefectRowFromMain(defect?: DefectSelectPayload) {
  const rows = ensureDefectRows();
  const primaryRow = rows.find((row) => row.primaryFlag) || rows[0];
  if (!primaryRow) {
    return;
  }
  const previousCode = primaryRow.defectCode;
  primaryRow.primaryFlag = true;
  primaryRow.defectCode = formData.value.defectCode;
  if (defect?.raw?.id) {
    primaryRow.defectCodeId = defect.raw.id;
  } else if (
    isBlank(formData.value.defectCode) ||
    previousCode !== formData.value.defectCode
  ) {
    primaryRow.defectCodeId = undefined;
  }
  primaryRow.defectName = formData.value.defectName;
  formData.value.defects = normalizeDefectRows(
    rows,
    undefined,
    undefined,
    true,
  );
}

function syncMainDefectFromPrimary() {
  const primaryRow =
    (formData.value.defects || []).find((row) => row.primaryFlag) ||
    formData.value.defects?.[0];
  if (!primaryRow) {
    formData.value.defectCode = undefined;
    formData.value.defectName = undefined;
    return;
  }
  formData.value.defectCode = primaryRow.defectCode;
  formData.value.defectName = primaryRow.defectName;
}

function addOtherDefectRow() {
  const rows = normalizeDefectRows(
    formData.value.defects,
    formData.value.defectCode,
    formData.value.defectName,
    true,
  );
  rows.push({
    __rowKey: buildLocalRowKey('defect'),
    primaryFlag: rows.length === 0,
    sort: rows.length + 1,
  });
  formData.value.defects = normalizeDefectRows(
    rows,
    undefined,
    undefined,
    true,
  );
}

function removeOtherDefectRow(row: NcrDefectRow) {
  const rowKey = getOtherDefectRowKey(row);
  const rows = (formData.value.defects || []).filter(
    (item) => getOtherDefectRowKey(item as NcrDefectRow) !== rowKey,
  );
  formData.value.defects = normalizeDefectRows(
    rows,
    undefined,
    undefined,
    true,
  );
  syncMainDefectFromPrimary();
}

function handleOtherDefectChange(row: NcrDefectRow, value?: string) {
  row.defectCode = value;
  if (!value) {
    row.defectCodeId = undefined;
  }
  if (row.primaryFlag) {
    syncMainDefectFromPrimary();
  }
}

function handleOtherDefectNameChange(row: NcrDefectRow) {
  if (row.primaryFlag) {
    syncMainDefectFromPrimary();
  }
}

function handleOtherDefectSelect(
  row: NcrDefectRow,
  defect: DefectSelectPayload,
) {
  row.defectCodeId = defect.raw?.id;
  row.defectCode = defect.code || row.defectCode;
  row.defectName = defect.name || row.defectName;
  if (!formData.value.ncLevel) {
    formData.value.ncLevel = defect.level;
  }
  if (row.primaryFlag) {
    syncMainDefectFromPrimary();
  }
}

function extractDefectsFromProductEventDetail(
  detail?: MesProductAbnormalEventApi.ProductAbnormalEventDetail | null,
) {
  if (!detail?.details?.length) {
    return [];
  }
  const defectMap = new Map<string, NcrDefectRow>();
  detail.details
    .filter((row) => isNgInspectionDetail(row))
    .forEach((row) => {
      const defectCode = firstText(row.defectCode);
      const defectName = firstText(
        row.defectName,
        row.abnormalDesc,
        row.inspectionItem,
      );
      const key = firstText(
        defectCode,
        defectName,
        row.inspectionItem,
        row.standardDesc,
      );
      if (!key || defectMap.has(key)) {
        return;
      }
      defectMap.set(key, {
        __rowKey: buildLocalRowKey('defect'),
        defectCode,
        defectCodeId: row.defectCodeId,
        defectName,
        primaryFlag: defectMap.size === 0,
        sourceInspectionItem: row.inspectionItem,
        sourceResult: row.result,
        sourceSectionName: row.sectionName,
        sort: defectMap.size + 1,
      });
    });
  return [...defectMap.values()];
}

function applyDefaultDefectsFromDetail(
  detail?: MesProductAbnormalEventApi.ProductAbnormalEventDetail | null,
  force = false,
) {
  if (!detail || (!force && (formData.value.defects || []).length > 0)) {
    return;
  }
  const defects = extractDefectsFromProductEventDetail(detail);
  if (defects.length === 0) {
    return;
  }
  formData.value.defects = normalizeDefectRows(defects);
  if (
    isBlank(formData.value.defectCode) &&
    isBlank(formData.value.defectName)
  ) {
    syncMainDefectFromPrimary();
  }
}

async function hydrateDefectsFromSourceIfNeeded(force = false) {
  if (!canEditQualityFields.value && !force) {
    return;
  }
  if (!force && (formData.value.defects || []).length > 0) {
    return;
  }
  const sourceType = normalizeInspectionSourceType(
    formData.value.sourceBizType,
  );
  const inspectionId = Number(formData.value.sourceId);
  if (!sourceType || !inspectionId) {
    return;
  }
  try {
    const detail = await getProductAbnormalEventDetail(
      sourceType,
      inspectionId,
    );
    applyDefaultDefectsFromDetail(detail, force);
  } catch {
    if (force) {
      message.warning('不良品详情读取失败，无法带入缺陷列表');
    }
  }
}

function buildDefectPayload() {
  return normalizeDefectRows(
    formData.value.defects,
    formData.value.defectCode,
    formData.value.defectName,
  )
    .filter((row) => !isBlank(row.defectCode) || !isBlank(row.defectName))
    .map((row, index) => ({
      defectCode: firstText(row.defectCode) || undefined,
      defectCodeId: row.defectCodeId,
      defectName: firstText(row.defectName) || undefined,
      defectPath: firstText(row.defectPath) || undefined,
      id: row.id,
      primaryFlag: Boolean(row.primaryFlag) || index === 0,
      sort: index + 1,
      sourceInspectionItem: firstText(row.sourceInspectionItem) || undefined,
      sourceResult: firstText(row.sourceResult) || undefined,
      sourceSectionName: firstText(row.sourceSectionName) || undefined,
    }));
}

function buildRecordPayload() {
  const responsibilityDeptCodes = normalizeMultiValue(
    formData.value.responsibilityDeptCodes,
  );
  return {
    ...formData.value,
    defects: buildDefectPayload(),
    sourceBizTypeName:
      formData.value.sourceBizTypeName ||
      (sourceBizTypeLabel.value === '-' ? undefined : sourceBizTypeLabel.value),
    sourceTypeName: sourceTypeLabel.value,
    ncLevelName: ncLevelLabel.value,
    rawMaterialAbnormalCategoryName: isRawMaterialNcr.value
      ? rawMaterialAbnormalCategoryLabel.value
      : formData.value.rawMaterialAbnormalCategoryName,
    responsibilityDeptCodes: responsibilityDeptCodes.join(',') || undefined,
    responsibilityDeptNames:
      resolveResponsibilityDeptNames(responsibilityDeptCodes) || undefined,
    reviews: buildReviewPayload().filter((item) => item.deptName),
    relations: buildRelationsPayload(),
  } as MesNcrApi.NcrRecord;
}

function buildReviewPayload() {
  const canWriteDisposition = isReviewDispositionEditStage();
  return normalizeReviewList(formData.value.reviews).map((review) => {
    const {
      __lastOpinionTemplateText: _lastOpinionTemplateText,
      ...payloadReview
    } = review as AutoTemplateReview;
    if (canWriteDisposition) {
      return payloadReview;
    }
    return {
      ...payloadReview,
      causeAnalysis: undefined,
      dispositionDetail: undefined,
      reviewOpinion: undefined,
      rootCauseCategory: undefined,
      suggestedDisposition: undefined,
    };
  });
}

function normalizeReviewList(reviews?: MesNcrApi.MrbReview[]) {
  return (reviews || []).map((review) => {
    const dispositionDetail =
      review.dispositionDetail || review.reviewOpinion || review.causeAnalysis;
    return {
      ...review,
      dispositionDetail,
      reviewOpinion: review.reviewOpinion || dispositionDetail,
    };
  });
}

function mergeDictOptions(
  dictType: string,
  fallbackOptions: Array<{ label: string; value: string }>,
) {
  const options = getDictOptions(dictType, 'string') as Array<{
    label?: string;
    value?: string;
  }>;
  const values = new Set(options.map((item) => item.value));
  return [
    ...options,
    ...fallbackOptions.filter((item) => !values.has(item.value)),
  ];
}

function getOptionLabel(
  options: Array<{ label?: string; value?: string }>,
  value?: string,
) {
  if (!value) {
    return '-';
  }
  return options.find((item) => item.value === value)?.label || value;
}

function normalizeMultiValue(value?: string | string[]) {
  if (Array.isArray(value)) {
    return [
      ...new Set(value.map((item) => String(item).trim()).filter(Boolean)),
    ];
  }
  return [
    ...new Set(
      String(value || '')
        .split(/[,，、;；]/)
        .map((item) => item.trim())
        .filter(Boolean),
    ),
  ];
}

function resolveResponsibilityDeptNames(codes: string[]) {
  return codes
    .map(
      (code) =>
        responsibilityDeptOptions.value.find((option) => option.value === code)
          ?.label || code,
    )
    .filter(Boolean)
    .join('、');
}

function displayResponsibilityDeptNames(record = formData.value) {
  const names = firstText(record.responsibilityDeptNames);
  if (names) {
    return names;
  }
  const codes = normalizeMultiValue(record.responsibilityDeptCodes);
  return resolveResponsibilityDeptNames(codes) || '-';
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function displayIqcMaterialInfo(record: IqcSourceRelationRow) {
  const parts = [
    record.materialCode,
    record.materialName,
    record.specification,
  ].filter((item) => !isBlank(item));
  return parts.length > 0 ? parts.join(' / ') : '-';
}

function resolveDispositionScopePieceNo(
  scope: MesNcrApi.DispositionExecutionScope,
) {
  if (scope.pieceNo || scope.segmentBatchNo || scope.motherBatchNo) {
    return scope.pieceNo || scope.segmentBatchNo || scope.motherBatchNo;
  }
  if (scope.objectKey?.startsWith('PIECE:')) {
    return scope.objectKey.slice('PIECE:'.length);
  }
  return scope.objectKey || '';
}

function isSystemStockDisposeOpinion(opinion: string) {
  return [
    '原材料不合格处置结果上传完成',
    '产品NCR处置指令已自动下达',
    '处置确认完成',
  ].includes(opinion);
}

function displayCodeLabel(value?: unknown) {
  const normalized =
    typeof value === 'string' ? value.trim().toUpperCase() : '';
  if (normalized === 'NG') return '不合格';
  if (normalized === 'OK') return '合格';
  if (normalized === 'PENDING') return '待判定';
  if (normalized === 'ABNORMAL') return '异常';
  if (normalized === 'FAIL' || normalized === 'FAILED') return '失败';
  return displayValue(value);
}

function resultColor(value?: unknown) {
  const normalized =
    typeof value === 'string' ? value.trim().toUpperCase() : '';
  if (
    normalized === 'NG' ||
    normalized === 'ABNORMAL' ||
    normalized === 'FAIL' ||
    normalized === 'FAILED' ||
    normalized.includes('不合格') ||
    normalized.includes('异常')
  ) {
    return 'error';
  }
  if (normalized === 'OK' || normalized === 'PASS' || normalized === 'PASSED') {
    return 'success';
  }
  if (normalized === 'PENDING') {
    return 'warning';
  }
  return 'default';
}

function displayDisposition(value?: string) {
  const labels = parseDispositionValues(value).map((item) =>
    getOptionLabel(effectiveDispositionOptions.value, item),
  );
  return labels.length > 0 ? labels.join('、') : '-';
}

function parseDispositionValues(value?: string) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index);
}

function getSingleDispositionValue(value?: string) {
  return parseDispositionValues(value)[0];
}

function joinDispositionValues(values?: Array<boolean | number | string>) {
  const optionValues = new Set(
    effectiveDispositionOptions.value.map((option) => option.value),
  );
  return (values || [])
    .map((value) =>
      String(value || '')
        .trim()
        .toUpperCase(),
    )
    .filter((value) => optionValues.has(value))
    .filter((value, index, array) => array.indexOf(value) === index)
    .join(',');
}

function normalizeDispositionString(value?: string) {
  return joinDispositionValues(parseDispositionValues(value));
}

function hasDispositionSelection(value?: string) {
  return parseDispositionValues(value).length > 0;
}

function dispositionIncludes(value: string | undefined, disposition: string) {
  return parseDispositionValues(value).includes(disposition);
}

function handleReviewDispositionChange(
  row: MesNcrApi.MrbReview,
  values: Array<boolean | number | string>,
) {
  const normalized = joinDispositionValues(values);
  row.suggestedDisposition = normalized || undefined;
  applyReviewOpinionTemplate(row, parseDispositionValues(normalized));
}

function handleRawReviewDispositionChange(
  row: MesNcrApi.MrbReview,
  value?: boolean | number | string,
) {
  const normalized = joinDispositionValues(value ? [value] : []);
  row.suggestedDisposition = normalized || undefined;
  applyReviewOpinionTemplate(row, parseDispositionValues(normalized));
}

function handleFinalDispositionChange(
  values: Array<boolean | number | string>,
) {
  const normalized = joinDispositionValues(values);
  formData.value.finalDisposition = normalized || undefined;
  applyFinalOpinionTemplate(parseDispositionValues(normalized));
}

function handleRawFinalDispositionChange(value?: boolean | number | string) {
  const normalized = joinDispositionValues(value ? [value] : []);
  formData.value.finalDisposition = normalized || undefined;
  applyFinalOpinionTemplate(parseDispositionValues(normalized));
}

function displayNotifyStatus(value?: string) {
  if (value === 'PENDING') {
    return '待回复';
  }
  if (value === 'REPLIED') {
    return '已回复';
  }
  if (value === 'CANCELLED') {
    return '已取消';
  }
  return displayValue(value);
}

function notifyStatusColor(value?: string) {
  if (value === 'PENDING') {
    return 'warning';
  }
  if (value === 'REPLIED') {
    return 'success';
  }
  if (value === 'CANCELLED') {
    return 'default';
  }
  return 'processing';
}

function isBlank(value?: unknown) {
  return value === undefined || value === null || String(value).trim() === '';
}

function normalizeInspectionSourceType(
  value?: unknown,
): MesProductAbnormalEventApi.SourceType | undefined {
  const normalized =
    typeof value === 'string' ? value.trim().toUpperCase() : '';
  if (
    inspectionSourceTypes.has(
      normalized as MesProductAbnormalEventApi.SourceType,
    )
  ) {
    return normalized as MesProductAbnormalEventApi.SourceType;
  }
  return undefined;
}

function isIqcSourceType(value?: unknown) {
  return typeof value === 'string' && value.trim().toUpperCase() === 'IQC';
}

function toPositiveNumber(value?: unknown) {
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}

function getIqcSourceId() {
  const directId = toPositiveNumber(formData.value.sourceId);
  if (directId) {
    return directId;
  }
  const relation = (formData.value.relations || []).find(
    (item) =>
      item.relationType === 'IQC' ||
      (isIqcSourceType(formData.value.sourceBizType) &&
        item.relatedObjectNo === formData.value.sourceNo),
  );
  return toPositiveNumber(relation?.relatedObjectId);
}

function getIqcSourceRelations(): IqcSourceRelationRow[] {
  const rows = new Map<string, IqcSourceRelationRow>();
  (formData.value.relations || [])
    .filter((item) => item.relationType === 'IQC')
    .forEach((item, index) => {
      const key =
        item.relatedObjectId === undefined || item.relatedObjectId === null
          ? `no-${item.relatedObjectNo || index}`
          : `id-${item.relatedObjectId}`;
      rows.set(key, {
        ...item,
        __rowKey: key,
      });
    });

  const directId = toPositiveNumber(formData.value.sourceId);
  if (
    isIqcSourceType(formData.value.sourceBizType) &&
    directId &&
    !rows.has(`id-${directId}`)
  ) {
    rows.set(`id-${directId}`, {
      __rowKey: `id-${directId}`,
      ncNo: formData.value.ncNo,
      primaryFlag: true,
      relatedObjectId: directId,
      relatedObjectName:
        formData.value.rawMaterialAbnormalCategoryName ||
        formData.value.sourceBizTypeName ||
        '进料检验(IQC)',
      relatedObjectNo: formData.value.sourceNo,
      relationType: 'IQC',
    });
  }
  return [...rows.values()];
}

function isNgInspectionDetail(
  row?: MesProductAbnormalEventApi.ProductAbnormalEventDetailItem,
) {
  if (!row) {
    return false;
  }
  const result =
    typeof row.result === 'string' ? row.result.trim().toUpperCase() : '';
  return (
    result === 'NG' ||
    result === 'N' ||
    result === 'ABNORMAL' ||
    result === 'FAIL' ||
    result === 'FAILED' ||
    result === 'FALSE' ||
    result.includes('不合格') ||
    result.includes('异常') ||
    !isBlank(row.defectCode) ||
    !isBlank(row.defectName) ||
    !isBlank(row.abnormalDesc)
  );
}

function hasNgCutRoundDetail(
  record: MesCutRoundFqcApi.Record,
  detail: MesCutRoundFqcApi.SubmissionDetail,
) {
  if (
    isNgSourceResult(detail.rowJudgment) ||
    !isBlank(detail.defectCode) ||
    !isBlank(detail.defectName) ||
    !isBlank(detail.ngReason) ||
    Number(detail.abnormalItemCount || 0) > 0
  ) {
    return true;
  }
  if ((detail.items || []).some((item) => hasNgCutRoundItem(item))) {
    return true;
  }
  return (record.items || []).some(
    (item) =>
      hasNgCutRoundItem(item) &&
      (item.submissionDetailId === detail.id ||
        item.cutRoundInspectionDetailId === detail.cutRoundInspectionDetailId ||
        item.productionBatchNo === detail.productionBatchNo),
  );
}

function countNgCutRoundItems(
  record: MesCutRoundFqcApi.Record,
  details: MesCutRoundFqcApi.SubmissionDetail[],
) {
  const keys = new Set<string>();
  const collect = (item: MesCutRoundFqcApi.FqcItem) => {
    if (hasNgCutRoundItem(item)) {
      keys.add(
        String(
          item.id ||
            `${item.submissionDetailId || item.cutRoundInspectionDetailId || item.productionBatchNo || '-'}-${item.inspectionItem}`,
        ),
      );
    }
  };
  details.forEach((detail) => {
    const scopedItems =
      detail.items && detail.items.length > 0
        ? detail.items
        : (record.items || []).filter(
            (item) =>
              item.submissionDetailId === detail.id ||
              item.cutRoundInspectionDetailId ===
                detail.cutRoundInspectionDetailId ||
              item.productionBatchNo === detail.productionBatchNo,
          );
    scopedItems.forEach(collect);
  });
  if (keys.size === 0) {
    (record.items || []).forEach(collect);
  }
  return keys.size || ngDetailRows.value.length;
}

function countCutRoundItems(
  record: MesCutRoundFqcApi.Record,
  details: MesCutRoundFqcApi.SubmissionDetail[],
) {
  const keys = new Set<string>();
  const collect = (item: MesCutRoundFqcApi.FqcItem) => {
    keys.add(
      String(
        item.id ||
          `${item.submissionDetailId || item.cutRoundInspectionDetailId || item.productionBatchNo || '-'}-${item.inspectionItem}`,
      ),
    );
  };
  details.forEach((detail) => {
    const scopedItems =
      detail.items && detail.items.length > 0
        ? detail.items
        : (record.items || []).filter(
            (item) =>
              item.submissionDetailId === detail.id ||
              item.cutRoundInspectionDetailId ===
                detail.cutRoundInspectionDetailId ||
              item.productionBatchNo === detail.productionBatchNo,
          );
    scopedItems.forEach(collect);
  });
  if (keys.size === 0) {
    (record.items || []).forEach(collect);
  }
  return keys.size || ngAllDetailRows.value.length;
}

function hasNgCutRoundItem(item?: MesCutRoundFqcApi.FqcItem) {
  if (!item) return false;
  if (isNgSourceResult(item.qaResult || item.itemResult || item.inputStatus)) {
    return true;
  }
  const samples = [...(item.samples || []), ...(item.qaValues || [])];
  return samples.some((sample) => hasNgSourceSample(sample));
}

function hasNgFgShippingDetail(
  record: MesFgShippingFqcApi.Record,
  detail: MesFgShippingFqcApi.ShippingDetail,
) {
  if (
    isNgSourceResult(detail.rowJudgment) ||
    !isBlank(detail.defectCode) ||
    !isBlank(detail.defectName) ||
    !isBlank(detail.ngReason) ||
    Number(detail.abnormalItemCount || 0) > 0
  ) {
    return true;
  }
  if ((detail.items || []).some((item) => hasNgFgShippingItem(item))) {
    return true;
  }
  return (record.items || []).some(
    (item) =>
      hasNgFgShippingItem(item) &&
      (item.submissionDetailId === detail.id ||
        item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
        item.productionBatchNo === detail.actualSliceBatchNo ||
        item.productionBatchNo === detail.sliceBatchNo),
  );
}

function countNgFgShippingItems(
  record: MesFgShippingFqcApi.Record,
  details: MesFgShippingFqcApi.ShippingDetail[],
) {
  const keys = new Set<string>();
  const collect = (item: MesFgShippingFqcApi.FqcItem) => {
    if (hasNgFgShippingItem(item)) {
      keys.add(
        String(
          item.id ||
            `${item.submissionDetailId || item.cutRoundInspectionDetailId || item.productionBatchNo || '-'}-${item.inspectionItem}`,
        ),
      );
    }
  };
  details.forEach((detail) => {
    const scopedItems =
      detail.items && detail.items.length > 0
        ? detail.items
        : (record.items || []).filter(
            (item) =>
              item.submissionDetailId === detail.id ||
              item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
              item.productionBatchNo === detail.actualSliceBatchNo ||
              item.productionBatchNo === detail.sliceBatchNo,
          );
    scopedItems.forEach(collect);
  });
  if (keys.size === 0) {
    (record.items || []).forEach(collect);
  }
  return keys.size || ngDetailRows.value.length;
}

function countFgShippingItems(
  record: MesFgShippingFqcApi.Record,
  details: MesFgShippingFqcApi.ShippingDetail[],
) {
  const keys = new Set<string>();
  const collect = (item: MesFgShippingFqcApi.FqcItem) => {
    keys.add(
      String(
        item.id ||
          `${item.submissionDetailId || item.cutRoundInspectionDetailId || item.productionBatchNo || '-'}-${item.inspectionItem}`,
      ),
    );
  };
  details.forEach((detail) => {
    const scopedItems =
      detail.items && detail.items.length > 0
        ? detail.items
        : (record.items || []).filter(
            (item) =>
              item.submissionDetailId === detail.id ||
              item.cutRoundInspectionDetailId === detail.shippingNoticeItemId ||
              item.productionBatchNo === detail.actualSliceBatchNo ||
              item.productionBatchNo === detail.sliceBatchNo,
          );
    scopedItems.forEach(collect);
  });
  if (keys.size === 0) {
    (record.items || []).forEach(collect);
  }
  return keys.size || ngAllDetailRows.value.length;
}

function hasNgFgShippingItem(item?: MesFgShippingFqcApi.FqcItem) {
  if (!item) return false;
  if (isNgSourceResult(item.qaResult || item.itemResult || item.inputStatus)) {
    return true;
  }
  const samples = [...(item.samples || []), ...(item.qaValues || [])];
  return samples.some((sample) => hasNgSourceSample(sample));
}

function isNgSourceResult(value?: string) {
  const result = typeof value === 'string' ? value.trim().toUpperCase() : '';
  return (
    result === 'NG' ||
    result === 'N' ||
    result === 'ABNORMAL' ||
    result === 'FAIL' ||
    result === 'FAILED' ||
    result.includes('不合格') ||
    result.includes('异常')
  );
}

function hasNgSourceSample(sample: {
  defects?: unknown[];
  qualitativeValue?: string;
  sampleResult?: string;
}) {
  return (
    isNgSourceResult(sample.sampleResult || sample.qualitativeValue) ||
    (sample.defects || []).length > 0
  );
}

function resetNgDetailState() {
  ngDetailModalOpen.value = false;
  iqcRelationPickerOpen.value = false;
  ngDetailLoading.value = false;
  ngDetailOnlyNg.value = false;
  ngInspectionDetail.value = null;
  clearNgSourceDetail();
}

function clearNgSourceDetail() {
  ngFaiRecord.value = null;
  ngCutRoundRecord.value = null;
  ngFgShippingRecord.value = null;
  ngOqcRecord.value = null;
}

function displayReviewDeptNames() {
  return selectedReviewDeptNames.value.length > 0
    ? selectedReviewDeptNames.value.join('、')
    : '-';
}

function displayReviewHandlers() {
  const reviews = normalizeReviewList(formData.value.reviews).filter(
    (row) => !isBlank(row.deptName),
  );
  if (reviews.length === 0) {
    return '-';
  }
  return reviews
    .map(
      (row) =>
        `${row.deptName || '未指定部门'}：${row.handlerUserName || getUserNameById(row.handlerUserId) || '-'}`,
    )
    .join('、');
}

function isReviewDispositionEditStage() {
  return formData.value.status === 'MRB_REVIEW' && canOperate.value;
}

function canEditReviewDisposition(row: MesNcrApi.MrbReview) {
  if (!isReviewDispositionEditStage()) {
    return false;
  }
  return isCurrentUserReviewHandler(row) && isPendingReview(row);
}

function isPendingReview(row: MesNcrApi.MrbReview) {
  return (row.reviewStatus || 'PENDING') === 'PENDING';
}

function isCurrentUserOriginalReviewHandler(row: MesNcrApi.MrbReview) {
  const userId = currentUserId.value;
  return !!userId && Number(row.handlerUserId) === userId;
}

function isCurrentUserReviewHandler(row: MesNcrApi.MrbReview) {
  const userId = currentUserId.value;
  if (!userId) {
    return false;
  }
  return (
    Number(row.handlerUserId) === userId ||
    Number(row.delegateUserId) === userId
  );
}

function canDelegateReview(row: MesNcrApi.MrbReview) {
  return (
    isReviewDispositionEditStage() &&
    isPendingReview(row) &&
    isCurrentUserOriginalReviewHandler(row)
  );
}

function getReviewHandlerDisplay(row: MesNcrApi.MrbReview) {
  const assignedName =
    row.handlerUserName || getUserNameById(row.handlerUserId) || '-';
  if (
    row.actualHandlerUserId &&
    row.handlerUserId &&
    Number(row.actualHandlerUserId) !== Number(row.handlerUserId)
  ) {
    return `${row.actualHandlerUserName || '-'}代${assignedName}办理`;
  }
  return assignedName;
}

function getReviewDelegateText(row: MesNcrApi.MrbReview) {
  return row.delegateUserName ? `委托：${row.delegateUserName}` : '';
}

function openReviewDelegateSelector(row: MesNcrApi.MrbReview) {
  if (!formData.value.id || !row.id) {
    message.warning('未找到可委托的会签行');
    return;
  }
  userSelectPurpose.value = 'reviewDelegate';
  reviewDelegateTarget.value = {
    id: formData.value.id,
    reviewId: row.id,
  };
  userSelectModalApi
    .setData({
      modalZIndex: 5000,
      multiple: false,
      userIds: row.delegateUserId ? [row.delegateUserId] : [],
    })
    .open();
}

function resolveContentConfirmUserSelect(confirmed: boolean) {
  const resolver = contentConfirmUserSelectResolver.value;
  contentConfirmUserSelectResolver.value = undefined;
  if (userSelectPurpose.value === 'contentConfirm') {
    userSelectPurpose.value = undefined;
  }
  resolver?.(confirmed);
}

function openContentConfirmUserSelector() {
  return new Promise<boolean>((resolve) => {
    userSelectPurpose.value = 'contentConfirm';
    contentConfirmUserSelectResolver.value = resolve;
    userSelectModalApi
      .setData({
        modalZIndex: 5700,
        multiple: false,
        userIds: nextHandlerUserId.value ? [nextHandlerUserId.value] : [],
      })
      .open();
  });
}

async function handleUserSelectConfirm(userList: SystemUserApi.User[]) {
  if (userSelectPurpose.value === 'contentConfirm') {
    const user = userList.find((item) => item.id);
    if (!user?.id) {
      return;
    }
    syncSelectedUserOption(user);
    nextHandlerUserId.value = user.id;
    resolveContentConfirmUserSelect(true);
    return;
  }
  await handleReviewDelegateUserConfirm(userList);
}

function handleUserSelectCancel() {
  if (userSelectPurpose.value === 'contentConfirm') {
    resolveContentConfirmUserSelect(false);
    return;
  }
  userSelectPurpose.value = undefined;
  reviewDelegateTarget.value = undefined;
}

function handleUserSelectClosed() {
  if (contentConfirmUserSelectResolver.value) {
    resolveContentConfirmUserSelect(false);
  }
  if (userSelectPurpose.value === 'reviewDelegate') {
    userSelectPurpose.value = undefined;
    reviewDelegateTarget.value = undefined;
  }
}

async function handleReviewDelegateUserConfirm(userList: SystemUserApi.User[]) {
  const user = userList.find((item) => item.id);
  const target = reviewDelegateTarget.value;
  if (!user || !target?.id || !target.reviewId) {
    return;
  }
  const delegateUserName = user.nickname || user.username || String(user.id);
  await delegateCurrentNcrReview({
    delegateUserId: user.id!,
    delegateUserName,
    id: target.id,
    reviewId: target.reviewId,
  });
  await refreshCurrentDetail();
  emit('success');
  userSelectPurpose.value = undefined;
  reviewDelegateTarget.value = undefined;
  message.success('委托代办已设置');
}

async function validateRequiredFields() {
  if (!isDraftLikeStage.value && isBlank(formData.value.defectName)) {
    message.warning('请填写缺陷名称');
    return false;
  }
  if (!isDraftLikeStage.value && isBlank(formData.value.ncLevel)) {
    message.warning('请选择不合格等级');
    return false;
  }
  try {
    await formRef.value?.validate();
    return true;
  } catch {
    message.warning('请补齐表单必填项后再提交');
    return false;
  }
}

function validateQualityReviewHandlers() {
  if (!canEditReviewHandlers.value) {
    return true;
  }
  const reviews = normalizeReviewList(formData.value.reviews).filter(
    (row) => !isBlank(row.deptName),
  );
  if (reviews.length === 0) {
    message.warning('请选择会签单位和会签人');
    return false;
  }
  const missingHandler = reviews.find((row) => !row.handlerUserId);
  if (missingHandler) {
    message.warning(`请选择${missingHandler.deptName || '会签单位'}的会签人`);
    return false;
  }
  return true;
}

function validateReviewDispositionBeforeSubmit() {
  if (formData.value.status !== 'MRB_REVIEW') {
    return true;
  }
  const editableReviews = normalizeReviewList(formData.value.reviews).filter(
    (row) => canEditReviewDisposition(row),
  );
  if (editableReviews.length === 0) {
    message.warning(
      '未找到当前账号可办理的会签行，请刷新后重试或检查办理人配置',
    );
    return false;
  }
  const missingDetail = editableReviews.some((row) =>
    isBlank(row.dispositionDetail || row.reviewOpinion),
  );
  const missingDisposition = editableReviews.some(
    (row) => !hasDispositionSelection(row.suggestedDisposition),
  );
  if (missingDetail && missingDisposition) {
    message.warning('请填写当前会签行的处置说明和会签选项');
    return false;
  }
  if (missingDetail) {
    message.warning('请填写当前会签行的处置说明');
    return false;
  }
  if (missingDisposition) {
    message.warning('请选择当前会签行的会签选项');
    return false;
  }
  return true;
}

function validateTransferBeforeSubmit() {
  if (!showTransferControls.value) {
    return true;
  }
  if (!transferRoute.value) {
    message.warning('请选择提交终审、处置执行分派或直接关闭');
    return false;
  }
  if (
    canEditTransferDecision.value &&
    (!hasDispositionSelection(formData.value.finalDisposition) ||
      isBlank(formData.value.finalOpinion))
  ) {
    message.warning('请选择处置选项并填写终审意见');
    return false;
  }
  return true;
}

function confirmFlowSubmit(actionName: string, content?: string) {
  return new Promise<boolean>((resolve) => {
    AntModal.confirm({
      cancelText: '取消',
      centered: true,
      content:
        content ||
        `是否${actionName}当前${formTitle.value}？确认后将提交流程动作。`,
      okText: '确认',
      title: '流程提交确认',
      zIndex: 5600,
      onCancel: () => resolve(false),
      onOk: () => resolve(true),
    });
  });
}

function getMissingNextHandlerMessage() {
  if (needsContentConfirmUser.value) {
    return '请选择再次确认人';
  }
  if (formData.value.status === 'REVIEW_ASSIGN') {
    if (useMultipleNextHandlers.value) {
      return '请选择产品范围确认办理人';
    }
    return isRawMaterialNcr.value &&
      transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION
      ? '请选择处置执行人'
      : '请选择终审办理人';
  }
  if (formData.value.status === 'EXECUTION_ASSIGN') {
    return '请选择处置执行人';
  }
  return '请先选择下一节点办理人';
}

async function handleConfirm() {
  if (!isNew.value && isDispositionNotifyReplyMode.value) {
    const id = formData.value.id;
    if (!id) {
      return false;
    }
    const replyConclusion = dispositionNotifyReplyConclusion.value.trim();
    if (!replyConclusion) {
      message.warning('请填写通知人办理结论');
      return false;
    }
    const confirmed = await confirmFlowSubmit('提交通知回复');
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    await replyCurrentNcrDispositionNotify({
      id,
      replyConclusion,
    });
    message.success('通知人办理结论已提交');
    return true;
  }

  if (
    isNew.value ||
    ['DRAFT', 'RETURNED'].includes(formData.value.status || '') ||
    canEditQualityConfirm.value
  ) {
    const valid = await validateRequiredFields();
    if (!valid) {
      return false;
    }
    if (!isDraftLikeStage.value && !validateQualityReviewHandlers()) {
      return false;
    }
  }

  if (!validateReviewDispositionBeforeSubmit()) {
    return false;
  }

  if (!validateTransferBeforeSubmit()) {
    return false;
  }

  if (
    formData.value.status === 'CONTENT_CONFIRM' &&
    isBlank(formData.value.ncDescription)
  ) {
    message.warning('请填写不合格说明');
    return false;
  }

  if (
    formData.value.status === 'REVIEW_ASSIGN' &&
    transferRoute.value === RAW_TRANSFER_ROUTE_FINAL &&
    !finalApproverPickerConfirmed.value
  ) {
    await openFinalApproverPicker();
    return false;
  }

  if (needsContentConfirmUser.value) {
    const selected = await openContentConfirmUserSelector();
    if (!selected) {
      return false;
    }
  }

  if (needsNextHandler.value && getSelectedNextHandlerUserIds().length === 0) {
    message.warning(getMissingNextHandlerMessage());
    return false;
  }

  if (isNew.value) {
    const confirmed = await confirmFlowSubmit('提交');
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    const payload = buildRecordPayload();
    const id = await createCurrentNcrRecord(payload);
    await linkSelectedException(
      id,
      formData.value.relatedExceptionId,
      formData.value.relatedExceptionNo,
      { manageLoading: false, notify: false, refresh: false },
    );
    await submitCurrentNcr({
      contentConfirmUserId: nextHandlerUserId.value,
      contentConfirmUserName: getNextHandlerUserName(),
      id,
      reviews: payload.reviews,
    });
    message.success('NCR 已开立并提交再次确认');
    return true;
  }

  const id = formData.value.id;
  if (!id) {
    return false;
  }

  if (
    formData.value.status === 'EXECUTION_ASSIGN' &&
    !isRawMaterialNcr.value &&
    transferRoute.value === RAW_TRANSFER_ROUTE_EXECUTION
  ) {
    const scopePayload = dispositionScopePanelRef.value?.validateAndBuild(
      nextHandlerUserId.value,
      getNextHandlerUserName(),
    );
    if (!scopePayload) {
      return false;
    }
    Object.assign(scopePayload, buildDispositionNotifyPayload());
    const confirmed = await confirmFlowSubmit(
      '确认范围并提交执行',
      dispositionScopePanelRef.value?.getConfirmSummary(),
    );
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    const execution = await confirmNcrDispositionScope(scopePayload);
    message.success(
      `处置执行单 ${execution.executionNo || ''} 已创建，工作台指令 ${execution.commandNo || ''} 待发送`,
    );
    return true;
  }

  if (
    formData.value.status === 'DRAFT' ||
    formData.value.status === 'RETURNED'
  ) {
    const confirmed = await confirmFlowSubmit('提交');
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    const payload = buildRecordPayload();
    await updateCurrentNcrRecord(payload);
    await submitCurrentNcr({
      contentConfirmUserId: nextHandlerUserId.value,
      contentConfirmUserName: getNextHandlerUserName(),
      id,
      reviews: payload.reviews,
    });
    message.success('NCR 已提交再次确认');
    return true;
  }

  if (formData.value.status === 'CONTENT_CONFIRM') {
    const confirmed = await confirmFlowSubmit(primaryActionLabel.value);
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    await handleCurrentNcr({
      id,
      ncDescription: formData.value.ncDescription,
      opinion: actionOpinion.value || '再次确认内容完成',
    });
    message.success('再次确认完成，已进入品质确认分配');
    return true;
  }

  if (
    formData.value.status === 'SUBMITTED' ||
    formData.value.status === 'MRB_REVIEW' ||
    formData.value.status === 'REVIEW_ASSIGN' ||
    formData.value.status === 'EXECUTION_ASSIGN' ||
    formData.value.status === 'CLOSE_CONFIRM'
  ) {
    const confirmed = await confirmFlowSubmit(primaryActionLabel.value);
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    const payload = buildRecordPayload();
    if (formData.value.status === 'SUBMITTED') {
      await updateCurrentNcrRecord(payload);
    }
    let defaultOpinion = `${primaryActionLabel.value}完成`;
    if (formData.value.status === 'SUBMITTED') {
      defaultOpinion = '品质确认完成';
    } else if (formData.value.status === 'MRB_REVIEW') {
      defaultOpinion = 'MRB 会签办理完成';
    }
    await handleCurrentNcr({
      createExceptionFlag: formData.value.createExceptionFlag === true,
      ...buildDispositionNotifyPayload(),
      ...buildNextHandlerPayload(),
      finalDisposition: formData.value.finalDisposition,
      finalDisposeDescription: formData.value.finalDisposeDescription,
      finalOpinion: formData.value.finalOpinion,
      id,
      opinion: actionOpinion.value || defaultOpinion,
      reviews: payload.reviews,
      stockDisposeQty: formData.value.stockDisposeQty,
      transferRoute: transferRoute.value,
    });
    let successMessage = 'NCR 办理完成';
    if (transferRoute.value === RAW_TRANSFER_ROUTE_CLOSE) {
      successMessage = `${formTitle.value}已直接关闭`;
    } else if (formData.value.status === 'SUBMITTED') {
      successMessage = '品质确认完成，已进入会签单位会签';
    }
    message.success(successMessage);
    return true;
  }

  if (formData.value.status === 'FINAL_APPROVAL') {
    if (
      !hasDispositionSelection(formData.value.finalDisposition) ||
      !formData.value.finalOpinion
    ) {
      message.warning('请填写终审处置结论和终审意见');
      return false;
    }
    const confirmed = await confirmFlowSubmit('终审');
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    await finalApproveCurrentNcr({
      createExceptionFlag: undefined,
      finalDisposition: formData.value.finalDisposition,
      finalOpinion: formData.value.finalOpinion,
      id,
    });
    message.success('NCR 终审完成');
    return true;
  }

  if (formData.value.status === 'PENDING_STOCK_DISPOSE') {
    const confirmed = await confirmFlowSubmit(
      isPendingRollPickConfirmation.value ? '确认挑选合格' : '处置',
      isPendingRollPickConfirmation.value
        ? `确认以下对象已挑选合格并改判？原始检验NG记录保留，未选对象保持原状态。\n${selectedDispositionPieceListText.value}`
        : undefined,
    );
    if (!confirmed) {
      return false;
    }
    modalApi.setState({ loading: true });
    const resolvedStockResult =
      stockResult.value ||
      (isRawMaterialNcr.value
        ? '原材料不合格处置结果上传完成'
        : formData.value.finalDisposeDescription ||
          '产品NCR处置指令已自动下达');
    await stockDisposeCurrentNcr({
      id,
      confirmPickQualified: isPendingRollPickConfirmation.value || undefined,
      opinion: actionOpinion.value || undefined,
      relations: buildDispositionAttachmentPayload(),
      stockDisposeStatus: isPendingRollPickConfirmation.value ? 'DONE' : formData.value.stockDisposeStatus || 'DONE',
      stockResult: resolvedStockResult,
    });
    message.success('处置已确认，NCR 已进入复核关闭');
    return true;
  }
  return false;
}

async function handleSaveDraft() {
  modalApi.setState({ loading: true });
  try {
    if (formData.value.id) {
      await updateCurrentNcrRecord(buildRecordPayload());
      message.success('草稿已更新');
    } else {
      const payload = buildRecordPayload();
      const id = await createCurrentNcrRecord(payload);
      await linkSelectedException(
        id,
        formData.value.relatedExceptionId,
        formData.value.relatedExceptionNo,
        { manageLoading: false, notify: false, refresh: false },
      );
      formData.value.id = id;
      isNew.value = false;
      formData.value.status = 'DRAFT';
      message.success('草稿已保存');
    }
    await reloadCurrentRecord();
    emit('success');
    syncModalButtons();
  } finally {
    modalApi.setState({ loading: false });
  }
}

async function handleReturn() {
  if (!formData.value.id) {
    return;
  }
  if (!actionOpinion.value) {
    message.warning('请先填写退回原因');
    return;
  }
  const confirmed = await confirmFlowSubmit(
    '退回',
    `确认退回当前${formTitle.value}至品质部转办并重新选择执行人？`,
  );
  if (!confirmed) {
    return;
  }
  modalApi.setState({ loading: true });
  try {
    await returnCurrentNcr({
      id: formData.value.id,
      opinion: actionOpinion.value,
      targetNodeCode: 'EXECUTION_ASSIGN',
      targetNodeName: '品质部转办',
    });
    message.success('NCR 已退回品质部转办，可重新选择执行人');
    emit('success');
    modalApi.close();
  } finally {
    modalApi.setState({ loading: false });
  }
}

function handleSelectSource(
  row: MesProductAbnormalEventApi.ProductAbnormalEvent,
) {
  formData.value.sourceId = row.inspectionId;
  formData.value.sourceNo = row.inspectionNo;
  formData.value.sourceBizType = row.sourceType;
  formData.value.sourceBizTypeName =
    row.inspectionType ||
    (sourceBizTypeLabel.value === '-' ? undefined : sourceBizTypeLabel.value);
  formData.value.happenTime = row.inspectionTime || formData.value.happenTime;
  formData.value.lotNo = row.productBatchNo || formData.value.lotNo;
  formData.value.materialName = row.productModel || formData.value.materialName;
  formData.value.specification =
    row.specification || formData.value.specification;
  formData.value.processName = row.operationName || formData.value.processName;
  formData.value.defectQty = row.unqualifiedQty ?? formData.value.defectQty;
  formData.value.ncDescription =
    row.abnormalSummary || formData.value.ncDescription;
  message.success(`已回填来源单据：${row.inspectionNo || '-'}`);
}

function handleIqcSourceSelect(row: MesNcrApi.RawMaterialInspection) {
  formData.value.sourceType = 'RAW_MATERIAL';
  formData.value.sourceTypeName = '原材料';
  formData.value.sourceBizType = row.inspectionType || 'IQC';
  formData.value.sourceBizTypeName = row.inspectionTypeName || '进料检验(IQC)';
  formData.value.sourceId = row.inspectionId;
  formData.value.sourceNo = row.inspectionNo;
  formData.value.happenTime = row.inspectionTime || formData.value.happenTime;
  formData.value.materialId = row.materialId;
  formData.value.materialCode = row.materialCode;
  formData.value.materialName = row.materialName;
  formData.value.specification = row.specification;
  formData.value.lotNo = row.lotNo;
  formData.value.defectQty = row.quantity ?? formData.value.defectQty;
  formData.value.unitCode = row.unitCode || formData.value.unitCode;
  formData.value.rawMaterialAbnormalCategory =
    row.rawMaterialAbnormalCategory ||
    formData.value.rawMaterialAbnormalCategory ||
    'INCOMING';
  formData.value.rawMaterialAbnormalCategoryName =
    row.rawMaterialAbnormalCategoryName ||
    rawMaterialAbnormalCategoryLabel.value;
  formData.value.ncDescription =
    row.abnormalSummary || formData.value.ncDescription;
  upsertIqcRelation(row);
  message.success(`已回填 IQC 单号：${row.inspectionNo || '-'}`);
}

function openExceptionSelector() {
  if (!canSelectRelatedException.value) {
    message.warning('当前状态不允许选择关联异常');
    return;
  }
  exceptionSelectModalApi
    .setData({
      currentExceptionId: formData.value.relatedExceptionId,
      currentNcrNo: formData.value.ncNo,
    })
    .open();
}

async function handleSelectException(row: MesExceptionApi.ExceptionRecord) {
  const id = Number(formData.value.id);
  if (!id) {
    formData.value.relatedExceptionId = row.id;
    formData.value.relatedExceptionNo = row.exceptionNo;
    message.success(`已选择关联异常：${row.exceptionNo || '-'}，保存后自动关联`);
    return;
  }
  await linkSelectedException(id, row.id, row.exceptionNo);
}

async function linkSelectedException(
  id: number,
  exceptionId?: number,
  exceptionNo?: string,
  options: {
    manageLoading?: boolean;
    notify?: boolean;
    refresh?: boolean;
  } = {},
) {
  if (!id || (!exceptionId && !exceptionNo)) {
    return;
  }
  const { manageLoading = true, notify = true, refresh = true } = options;
  if (manageLoading) {
    modalApi.setState({ loading: true });
  }
  try {
    const payload = {
      exceptionId,
      exceptionNo,
      id,
      remark: '处置单侧选择关联异常事件',
    };
    if (isRawMaterialNcr.value) {
      await linkRawMaterialNcrException(payload);
    } else {
      await linkNcrException(payload);
    }
    if (refresh) {
      const record = await fetchNcrRecord(id, isRawMaterialNcr.value);
      applyLoadedRecord(record, formData.value.tabType);
    }
    if (notify) {
      message.success(`已关联异常：${exceptionNo || '-'}`);
      emit('success');
    }
  } finally {
    if (manageLoading) {
      modalApi.setState({ loading: false });
    }
  }
}

function upsertIqcRelation(row: MesNcrApi.RawMaterialInspection) {
  const relations = (formData.value.relations || []).filter(
    (relation) => relation.relationType !== 'IQC',
  );
  relations.push({
    primaryFlag: true,
    relatedObjectId: row.inspectionId,
    relatedObjectName:
      row.supplierName || row.inspectionTypeName || '进料检验(IQC)',
    relatedObjectNo: row.inspectionNo,
    relationStatus: row.ncrStatus,
    relationType: 'IQC',
    remark: row.abnormalSummary,
  });
  formData.value.relations = relations;
}

function openSourceDetail() {
  if (!canOpenSourceDetail.value) {
    return;
  }
  productEventDetailModalApi
    .setData({
      inspectionId: formData.value.sourceId,
      sourceType: formData.value.sourceBizType,
    })
    .open();
}

async function openNgDetailModal() {
  if (canOpenIqcDetail.value) {
    if (iqcSourceRelations.value.length > 1) {
      iqcRelationPickerOpen.value = true;
      return;
    }
    openIqcDetail(getIqcSourceId());
    return;
  }
  const sourceType = normalizeInspectionSourceType(
    formData.value.sourceBizType,
  );
  const inspectionId = Number(formData.value.sourceId);
  if (!sourceType || !inspectionId) {
    message.warning('当前处置单未关联可查看的检验单');
    return;
  }
  ngDetailModalOpen.value = true;
  ngDetailLoading.value = true;
  ngDetailOnlyNg.value = false;
  ngInspectionDetail.value = null;
  clearNgSourceDetail();
  try {
    ngInspectionDetail.value = await getProductAbnormalEventDetail(
      sourceType,
      inspectionId,
    );
    applyDefaultDefectsFromDetail(ngInspectionDetail.value);
    try {
      await loadNgSourceDetail(sourceType, inspectionId);
    } catch {
      message.warning('源检验详情读取失败，已展示统一 NG 明细');
    }
  } finally {
    ngDetailLoading.value = false;
  }
}

function handleOpenIqcRelation(row: IqcSourceRelationRow) {
  iqcRelationPickerOpen.value = false;
  openIqcDetail(toPositiveNumber(row.relatedObjectId));
}

function openIqcDetail(id?: number) {
  if (!id) {
    message.warning('当前 IQC 来源缺少可查看的检验单 ID');
    return;
  }
  iqcDetailModalApi.setData({ id }).open();
}

async function loadNgSourceDetail(
  sourceType: MesProductAbnormalEventApi.SourceType,
  inspectionId: number,
) {
  clearNgSourceDetail();
  if (sourceType === 'FAI') {
    ngFaiRecord.value = await getFaiDetail(inspectionId);
    return;
  }
  if (sourceType === 'GLUE_BOARD_FAI') {
    ngFaiRecord.value = await getGlueBoardFaiDetail(inspectionId);
    return;
  }
  if (sourceType === 'CUT_ROUND_FQC') {
    ngCutRoundRecord.value = await getCutRoundFqcLightDetail(inspectionId);
    return;
  }
  if (sourceType === 'FG_SHIPPING_FQC') {
    ngFgShippingRecord.value = await getFgShippingFqcDetail(inspectionId);
    return;
  }
  if (sourceType === 'OQC') {
    ngOqcRecord.value = await getOqcDetail(inspectionId);
  }
}
</script>

<template>
  <Modal>
    <PickerModal
      :config="activeMaterialPickerConfig"
      :initial-filters="productBomPickerInitialFilters"
      :open="productBomPickerOpen"
      :title="isRawMaterialNcr ? '选择物料' : '选择产品料号'"
      @close="productBomPickerOpen = false"
      @pick="handleProductBomPick"
    />
    <SourcePickerModal @select="handleSelectSource" />
    <IqcSourceSelectModalComp @select="handleIqcSourceSelect" />
    <ExceptionSelectModalComp @select="handleSelectException" />
    <IqcDetailModal />
    <AntModal
      v-model:open="iqcRelationPickerOpen"
      centered
      :footer="null"
      title="关联 IQC 明细"
      width="1080px"
    >
      <Table
        bordered
        :columns="iqcRelationColumns"
        :data-source="iqcSourceRelations"
        :pagination="false"
        :row-key="getIqcRelationRowKey"
        :scroll="iqcRelationTableScroll"
        size="small"
      >
        <template #bodyCell="{ column, index, record }">
          <template v-if="column.dataIndex === 'index'">
            {{ index + 1 }}
          </template>
          <template v-else-if="column.dataIndex === 'materialInfo'">
            {{ displayIqcMaterialInfo(record) }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <Button
              size="small"
              type="link"
              @click="handleOpenIqcRelation(record)"
            >
              查看
            </Button>
          </template>
        </template>
      </Table>
    </AntModal>
    <ProductEventDetailModal />
    <ProcessAuditModal />
    <UserSelectModalComp
      class="w-3/5"
      :confirm-text="
        userSelectPurpose === 'contentConfirm' ? '确定并提交' : '确定'
      "
      :modal-z-index="5000"
      :multiple="false"
      :title="
        userSelectPurpose === 'contentConfirm' ? '选择再次确认人' : '选择人员'
      "
      @cancel="handleUserSelectCancel"
      @closed="handleUserSelectClosed"
      @confirm="handleUserSelectConfirm"
    />
    <BusinessLogDrawer
      v-model:open="logDrawerVisible"
      :logs="formData.flowLogs || []"
      :mode="logDrawerMode"
      :title="logDrawerMode === 'operation' ? '操作日志' : '流程日志'"
    />
    <AntModal
      v-model:open="descriptionEditorOpen"
      centered
      :footer="null"
      title="编辑不良描述"
      width="82vw"
    >
      <Input.TextArea
        v-model:value="formData.ncDescription"
        class="qms-ncr-description-modal-textarea"
        :disabled="!canEditDescription"
        placeholder="描述不良现象、检验数据和影响范围"
      />
    </AntModal>
    <AntModal
      v-model:open="ngDetailModalOpen"
      destroy-on-close
      :footer="null"
      :body-style="{
        overflow: 'hidden',
        padding: '12px 16px 16px',
      }"
      :title="ngDetailTitle"
      wrap-class-name="qms-ncr-ng-detail-modal"
      width="92vw"
    >
      <div class="qms-ncr-ng-detail">
        <div class="qms-ncr-ng-detail__meta">
          <span>
            检验类型：{{ displayValue(ngInspectionDetail?.inspectionType) }}
          </span>
          <span>
            检验单号：{{ displayValue(ngInspectionDetail?.inspectionNo) }}
          </span>
          <span>
            产品批次：{{ displayValue(ngInspectionDetail?.productBatchNo) }}
          </span>
          <span>
            产品型号：{{ displayValue(ngInspectionDetail?.productModel) }}
          </span>
        </div>
        <Spin :spinning="ngDetailLoading" class="qms-ncr-ng-detail__spin">
          <section class="qms-ncr-ng-detail__summary">
            <div class="qms-ncr-ng-detail__summary-label">异常总结</div>
            <div class="qms-ncr-ng-detail__summary-content">
              {{ displayValue(ngInspectionDetail?.abnormalSummary) }}
            </div>
          </section>
          <section class="qms-ncr-ng-detail__section">
            <div class="qms-ncr-ng-detail__section-head">
              <strong>检验详情列表</strong>
              <div class="qms-ncr-ng-detail__section-tools">
                <Checkbox
                  v-if="ngDetailNgFilterAvailable"
                  v-model:checked="ngDetailOnlyNg"
                >
                  只看不良品
                </Checkbox>
                <span>{{ ngDetailSummaryText }}</span>
              </div>
            </div>
            <div class="qms-ncr-ng-detail__table-host">
              <template v-if="hasNgSourceDetail">
                <FaiEventDetailPanel
                  v-if="
                    ngInspectionDetail?.sourceType === 'FAI' ||
                    ngInspectionDetail?.sourceType === 'GLUE_BOARD_FAI'
                  "
                  :record="ngFaiRecord"
                />
                <CutRoundFqcEventDetailPanel
                  v-else-if="ngInspectionDetail?.sourceType === 'CUT_ROUND_FQC'"
                  class="qms-ncr-ng-cut-round-panel"
                  :only-ng="ngDetailOnlyNg"
                  :record="ngCutRoundRecord"
                />
                <FgShippingFqcEventDetailPanel
                  v-else-if="
                    ngInspectionDetail?.sourceType === 'FG_SHIPPING_FQC'
                  "
                  :only-ng="ngDetailOnlyNg"
                  :record="ngFgShippingRecord"
                />
                <OqcEventDetailPanel
                  v-else-if="ngInspectionDetail?.sourceType === 'OQC'"
                  :record="ngOqcRecord"
                />
              </template>
              <Table
                v-else
                bordered
                class="qms-ncr-ng-detail__fallback-table"
                :columns="ngDetailColumns"
                :data-source="ngDetailRows"
                :locale="{ emptyText: '暂无 NG 项目明细' }"
                :pagination="false"
                :row-key="getNgDetailRowKey"
                :scroll="ngDetailTableScroll"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.dataIndex === 'result'">
                    <Tag :color="resultColor(record.result)" class="!m-0">
                      {{ displayCodeLabel(record.result) }}
                    </Tag>
                  </template>
                  <template v-else-if="column.dataIndex === 'standardDesc'">
                    <span class="qms-ncr-ng-detail__multiline">
                      {{ displayValue(record.standardDesc) }}
                    </span>
                  </template>
                  <template v-else-if="column.dataIndex === 'abnormalDesc'">
                    <span class="qms-ncr-ng-detail__multiline text-red-700">
                      {{ displayValue(record.abnormalDesc) }}
                    </span>
                  </template>
                </template>
              </Table>
            </div>
          </section>
        </Spin>
      </div>
    </AntModal>
    <AntModal
      v-model:open="finalApproverPickerOpen"
      centered
      destroy-on-close
      ok-text="确认送审"
      title="推送终审"
      width="520px"
      :z-index="FINAL_APPROVER_MODAL_Z_INDEX"
      @ok="handleFinalApproverConfirm"
    >
      <div class="qms-ncr-final-approver-picker">
        <Form layout="vertical">
          <Form.Item label="终审办理人" required>
            <Select
              v-model:value="nextHandlerUserId"
              :dropdown-style="{ zIndex: FINAL_APPROVER_DROPDOWN_Z_INDEX }"
              :filter-option="filterUserOption"
              :get-popup-container="getFinalApproverPopupContainer"
              :options="finalApproverOptions"
              placeholder="请选择终审办理人"
              show-search
            >
              <template #option="{ label, username }">
                <div class="qms-ncr-user-option">
                  <span class="qms-ncr-user-option__name">
                    {{ label }}
                  </span>
                  <span class="qms-ncr-user-option__code">
                    {{ username || '-' }}
                  </span>
                </div>
              </template>
            </Select>
          </Form.Item>
        </Form>
      </div>
    </AntModal>

    <div class="qms-ncr-detail">
      <div class="qms-ncr-toolbar">
        <div class="qms-ncr-toolbar__placeholder"></div>
        <div class="qms-ncr-title-panel">
          <div class="qms-ncr-title-panel__name">{{ formTitle }}</div>
          <div class="qms-ncr-title-panel__subtitle">
            <div class="qms-ncr-title-panel__subtitle-item">
              单号：{{ formData.ncNo || '新建中' }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              状态/节点：{{
                formData.currentNodeName || formData.status || '待提交'
              }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              追溯批号：{{ formData.lotNo || formData.sourceNo || '-' }}
            </div>
            <div class="qms-ncr-title-panel__subtitle-item">
              缺陷等级：{{ ncLevelLabel }}
            </div>
            <div
              v-if="formData.currentHandlerUserName"
              class="qms-ncr-title-panel__subtitle-item"
            >
              当前办理：{{ formData.currentHandlerUserName }}
            </div>
            <div
              v-if="formData.contentConfirmUserName"
              class="qms-ncr-title-panel__subtitle-item"
            >
              再次确认：{{ formData.contentConfirmUserName }}
            </div>
          </div>
        </div>
        <div class="qms-ncr-toolbar__actions">
          <div class="qms-ncr-toolbar__log-icons">
            <Button
              class="qms-ncr-toolbar-icon-btn"
              size="small"
              title="审批日志"
              @click="openAuditLog"
            >
              <IconifyIcon icon="mdi:clipboard-check-outline" />
            </Button>
            <Button
              class="qms-ncr-toolbar-icon-btn"
              size="small"
              title="操作日志"
              @click="openLogDrawer('operation')"
            >
              <IconifyIcon icon="mdi:history" />
            </Button>
          </div>
          <Button
            v-for="action in leadingToolbarActions"
            :key="action.key"
            class="qms-ncr-toolbar-action"
            :danger="action.danger"
            size="small"
            :title="action.title"
            :type="action.type"
            @click="action.onClick"
          >
            <span>{{ action.label }}</span>
          </Button>
          <Dropdown
            v-if="overflowToolbarActions.length > 0"
            placement="bottomRight"
            :trigger="['click']"
          >
            <Button class="qms-ncr-toolbar-action" size="small" title="更多">
              <span>更多</span>
            </Button>
            <template #overlay>
              <Menu>
                <Menu.Item
                  v-for="action in overflowToolbarActions"
                  :key="action.key"
                  :danger="action.danger"
                  @click="action.onClick"
                >
                  {{ action.label }}
                </Menu.Item>
              </Menu>
            </template>
          </Dropdown>
          <Button
            v-for="action in trailingToolbarActions"
            :key="action.key"
            class="qms-ncr-toolbar-action"
            :danger="action.danger"
            size="small"
            :title="action.title"
            :type="action.type"
            @click="action.onClick"
          >
            <span>{{ action.label }}</span>
          </Button>
        </div>
      </div>

      <div class="qms-ncr-workbench">
        <Form
          ref="formRef"
          class="qms-ncr-main"
          :disabled="formDisabled"
          :model="formData"
          layout="vertical"
        >
          <div class="qms-ncr-form-sections">
            <section class="qms-ncr-form-fieldset">
              <div class="qms-ncr-form-legend">不合格说明</div>
              <div
                class="qms-ncr-form-head qms-ncr-form-head--nc-info"
                :class="{ 'qms-ncr-form-head--raw-nc-info': isRawMaterialNcr }"
              >
                <label>类型</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                    name="sourceType"
                    required
                  >
                    <Select
                      v-model:value="formData.sourceType"
                      :disabled="!canEditPreSubmitFields"
                      :options="sourceTypeOptions"
                      placeholder="请选择类型"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ sourceTypeLabel }}
                  </div>
                </div>
                <label>发生日期</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                    name="happenTime"
                    required
                  >
                    <DatePicker
                      v-model:value="formData.happenTime"
                      class="w-full"
                      :disabled="!canEditPreSubmitFields"
                      format="YYYY-MM-DD HH:mm:ss"
                      show-time
                      value-format="YYYY-MM-DD HH:mm:ss"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.happenTime) }}
                  </div>
                </div>
                <label>{{ isRawMaterialNcr ? '发生部门' : '发生工序' }}</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                  >
                    <TreeSelect
                      v-if="isRawMaterialNcr"
                      v-model:value="formData.happenDeptId"
                      allow-clear
                      class="w-full"
                      :disabled="!canEditPreSubmitFields"
                      :field-names="{
                        children: 'children',
                        label: 'name',
                        value: 'id',
                      }"
                      placeholder="请选择发生部门"
                      show-search
                      :tree-data="deptTreeData"
                      tree-default-expand-all
                      tree-node-filter-prop="name"
                      @change="handleHappenDeptChange"
                    />
                    <Select
                      v-else
                      v-model:value="formData.processName"
                      :disabled="!canEditPreSubmitFields"
                      :loading="processOptionsLoading"
                      :options="processOptions"
                      option-filter-prop="label"
                      placeholder="请选择发生工序"
                      show-search
                      @change="handleProcessSelect"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{
                      displayValue(
                        isRawMaterialNcr
                          ? formData.happenDeptName
                          : formData.processName,
                      )
                    }}
                  </div>
                </div>
                <template v-if="isRawMaterialNcr">
                  <label>异常类别</label>
                  <div class="qms-ncr-form-control">
                    <Form.Item
                      v-if="canEditPreSubmitFields"
                      class="qms-ncr-form-item"
                    >
                      <Radio.Group
                        v-model:value="formData.rawMaterialAbnormalCategory"
                        :disabled="!canEditPreSubmitFields"
                      >
                        <Radio
                          v-for="option in rawMaterialAbnormalCategoryOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </Radio>
                      </Radio.Group>
                    </Form.Item>
                    <div v-else class="qms-ncr-readonly-value">
                      {{ rawMaterialAbnormalCategoryLabel }}
                    </div>
                  </div>
                  <label>是否隔离</label>
                  <div class="qms-ncr-form-control">
                    <Form.Item
                      v-if="canEditPreSubmitFields"
                      class="qms-ncr-form-item"
                    >
                      <Radio.Group
                        v-model:value="formData.isolatedFlag"
                        :disabled="!canEditPreSubmitFields"
                      >
                        <Radio :value="true">是</Radio>
                        <Radio :value="false">否</Radio>
                      </Radio.Group>
                    </Form.Item>
                    <div v-else class="qms-ncr-readonly-value">
                      {{ formData.isolatedFlag ? '是' : '否' }}
                    </div>
                  </div>
                </template>
                <label>批号</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                    name="lotNo"
                    required
                  >
                    <Input
                      v-model:value="formData.lotNo"
                      :readonly="!canEditPreSubmitFields"
                      placeholder="直接填写批号"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.lotNo) }}
                  </div>
                </div>
                <label>数量</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                    name="defectQty"
                    required
                  >
                    <InputNumber
                      v-model:value="formData.defectQty"
                      class="w-full"
                      :disabled="!canEditPreSubmitFields"
                      :min="1"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.defectQty) }}
                  </div>
                </div>
                <label>单位</label>
                <div class="qms-ncr-form-control">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                  >
                    <Input
                      v-model:value="formData.unitCode"
                      :readonly="!canEditPreSubmitFields"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.unitCode) }}
                  </div>
                </div>
                <label>物料编码</label>
                <div
                  class="qms-ncr-form-control qms-ncr-form-control--span-3"
                  :class="{
                    'qms-ncr-form-control--raw-full': isRawMaterialNcr,
                  }"
                >
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                    name="materialCode"
                    required
                  >
                    <PickerInline
                      :model-value="formData.materialCode"
                      :config="activeMaterialPickerConfig"
                      :disabled="!canEditPreSubmitFields"
                      :placeholder="
                        isRawMaterialNcr ? '请选择物料编码' : '请选择产品料号'
                      "
                      @pick="handleProductBomPick"
                      @search="openProductBomPicker"
                      @update:model-value="handleProductMaterialInput"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.materialCode) }}
                  </div>
                </div>
                <template v-if="!isRawMaterialNcr">
                  <label>品名</label>
                  <div
                    class="qms-ncr-form-control qms-ncr-form-control--span-3"
                  >
                    <div class="qms-ncr-readonly-value">
                      {{ displayValue(formData.materialName) }}
                    </div>
                  </div>
                </template>
                <label>规格</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-3">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                  >
                    <Input
                      v-model:value="formData.specification"
                      :readonly="!canEditPreSubmitFields"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.specification) }}
                  </div>
                </div>
                <template v-if="isRawMaterialNcr">
                  <label>品名</label>
                  <div
                    class="qms-ncr-form-control qms-ncr-form-control--span-3"
                  >
                    <div class="qms-ncr-readonly-value">
                      {{ displayValue(formData.materialName) }}
                    </div>
                  </div>
                </template>
                <label>{{ sourceNoLabel }}</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-5">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                  >
                    <Input
                      v-model:value="formData.sourceNo"
                      placeholder="检验单/异常单/客退单号"
                      :readonly="!canEditPreSubmitFields"
                    >
                      <template #suffix>
                        <Button
                          v-if="!isRawMaterialNcr"
                          class="qms-ncr-inline-icon-btn"
                          :disabled="!canEditPreSubmitFields"
                          size="small"
                          title="来源回填"
                          type="text"
                          @click.stop="sourcePickerModalApi.open()"
                        >
                          <IconifyIcon icon="lucide:search" />
                        </Button>
                        <Button
                          v-else
                          class="qms-ncr-inline-icon-btn"
                          :disabled="!canEditPreSubmitFields"
                          size="small"
                          title="选择 IQC 进料检验记录"
                          type="text"
                          @click.stop="iqcSourceSelectModalApi.open()"
                        >
                          <IconifyIcon icon="lucide:search" />
                        </Button>
                        <Button
                          v-if="canOpenSourceDetail"
                          class="qms-ncr-inline-icon-btn"
                          size="small"
                          title="查看来源单据"
                          type="text"
                          @click.stop="openSourceDetail"
                        >
                          <IconifyIcon icon="lucide:external-link" />
                        </Button>
                      </template>
                    </Input>
                  </Form.Item>
                  <div
                    v-else
                    class="qms-ncr-readonly-value qms-ncr-readonly-value--with-action"
                  >
                    <span>{{ displayValue(formData.sourceNo) }}</span>
                    <Button
                      v-if="canOpenSourceDetail"
                      class="qms-ncr-inline-icon-btn"
                      size="small"
                      title="查看来源单据"
                      type="text"
                      @click.stop="openSourceDetail"
                    >
                      <IconifyIcon icon="lucide:external-link" />
                    </Button>
                  </div>
                </div>
                <label>关联异常</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-5">
                  <div
                    class="qms-ncr-readonly-value qms-ncr-readonly-value--with-action"
                  >
                    <span>{{ displayValue(formData.relatedExceptionNo) }}</span>
                    <Button
                      v-if="canSelectRelatedException"
                      class="qms-ncr-inline-icon-btn"
                      size="small"
                      title="选择关联异常"
                      type="text"
                      @click.stop="openExceptionSelector"
                    >
                      <IconifyIcon icon="lucide:search" />
                    </Button>
                  </div>
                </div>
                <label class="qms-ncr-form-label--row-start">不良描述</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-11">
                  <Form.Item
                    v-if="canEditDescription"
                    class="qms-ncr-form-item"
                    name="ncDescription"
                    required
                  >
                    <div class="qms-ncr-description-box">
                      <Input.TextArea
                        v-model:value="formData.ncDescription"
                        :auto-size="{ minRows: 4, maxRows: 8 }"
                        :disabled="!canEditDescription"
                        placeholder="描述不良现象、检验数据和影响范围"
                      />
                      <Button
                        class="qms-ncr-description-expand"
                        :disabled="!canEditDescription"
                        size="small"
                        title="最大化编辑"
                        type="text"
                        @click="descriptionEditorOpen = true"
                      >
                        <IconifyIcon icon="lucide:maximize-2" />
                      </Button>
                    </div>
                  </Form.Item>
                  <div
                    v-else
                    class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                  >
                    {{ displayValue(formData.ncDescription) }}
                  </div>
                </div>
                <label class="qms-ncr-form-label--row-start">不良品详情</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-11">
                  <div class="qms-ncr-ng-detail-entry">
                    <span>
                      {{ ngDetailEntryText }}
                    </span>
                    <Button
                      class="qms-ncr-ng-detail-entry__button"
                      :disabled="!canOpenNgDetail"
                      size="small"
                      title="查看不良品详情"
                      @click="openNgDetailModal"
                    >
                      <IconifyIcon icon="lucide:list-search" />
                      <span>不良品详情</span>
                    </Button>
                  </div>
                </div>
                <label>附件</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-11">
                  <Form.Item
                    v-if="canEditPreSubmitFields"
                    class="qms-ncr-form-item"
                  >
                    <FileUpload
                      v-model="attachmentUrls"
                      :accept="attachmentAcceptTypes"
                      :disabled="!canEditPreSubmitFields"
                      directory="mes/qms/ncr"
                      :max-number="20"
                      :max-size="20"
                      multiple
                      show-description
                      @preview="handleAttachmentPreview"
                    />
                  </Form.Item>
                  <div
                    v-else
                    class="qms-ncr-readonly-value qms-ncr-attachment-readonly"
                  >
                    <template v-if="attachmentUrls.length > 0">
                      <Button
                        v-for="(url, index) in attachmentUrls"
                        :key="url"
                        size="small"
                        type="link"
                        @click="handleAttachmentPreview({ url })"
                      >
                        {{ getAttachmentName(url, index) }}
                      </Button>
                    </template>
                    <span v-else>-</span>
                  </div>
                </div>
                <label>担当</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-5">
                  <div class="qms-ncr-readonly-value">
                    {{ displayValue(formData.applicantUserName) }}
                  </div>
                </div>
                <label>确认</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-5">
                  <div class="qms-ncr-readonly-value">
                    {{ displayValue(descriptionConfirmUserName) }}
                  </div>
                </div>
              </div>
            </section>

            <section v-if="showQualitySection" class="qms-ncr-form-fieldset">
              <div class="qms-ncr-form-legend">品质确认</div>
              <div class="qms-ncr-form-head qms-ncr-form-head--quality">
                <label>不合格等级</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-7">
                  <Form.Item
                    v-if="canEditQualityFields"
                    class="qms-ncr-form-item"
                    name="ncLevel"
                    required
                  >
                    <Radio.Group
                      v-model:value="formData.ncLevel"
                      class="qms-ncr-level-radio"
                      :disabled="!canEditQualityFields"
                    >
                      <Radio
                        v-for="option in ncLevelOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </Radio>
                    </Radio.Group>
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ ncLevelLabel }}
                  </div>
                </div>
                <label>责任部门</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-7">
                  <Form.Item
                    v-if="canEditQualityFields"
                    class="qms-ncr-form-item"
                    name="responsibilityDeptCodes"
                  >
                    <Select
                      v-model:value="responsibilityDeptCodesModel"
                      allow-clear
                      mode="multiple"
                      :options="responsibilityDeptOptions"
                      placeholder="请选择责任部门"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayResponsibilityDeptNames() }}
                  </div>
                </div>
                <label>缺陷代码</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-3">
                  <Form.Item
                    v-if="canEditQualityFields"
                    class="qms-ncr-form-item"
                    name="defectCode"
                  >
                    <DefectCodePicker
                      v-model:value="formData.defectCode"
                      :disabled="!canEditQualityFields"
                      @change="handleDefectChange"
                      @select="handleDefectSelect"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.defectCode) }}
                  </div>
                </div>
                <label>缺陷名称</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-3">
                  <Form.Item
                    v-if="canEditQualityFields"
                    class="qms-ncr-form-item"
                    name="defectName"
                    required
                    :rules="[
                      {
                        required: true,
                        whitespace: true,
                        message: '请填写缺陷名称',
                      },
                    ]"
                  >
                    <Input
                      v-model:value="formData.defectName"
                      :readonly="!canEditQualityFields"
                      @change="handleDefectNameChange"
                    />
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayValue(formData.defectName) }}
                  </div>
                </div>
                <label class="qms-ncr-form-label--row-start">其他缺陷</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-7">
                  <div class="qms-ncr-subtable qms-ncr-other-defects">
                    <div
                      v-if="canEditQualityFields"
                      class="qms-ncr-subtable__toolbar"
                    >
                      <Button
                        :disabled="!canOpenNgDetail"
                        size="small"
                        title="从不良品详情带入"
                        @click="hydrateDefectsFromSourceIfNeeded(true)"
                      >
                        <IconifyIcon icon="lucide:list-plus" />
                        <span>带入明细</span>
                      </Button>
                      <Button
                        size="small"
                        title="新增缺陷"
                        @click="addOtherDefectRow"
                      >
                        <IconifyIcon icon="lucide:plus" />
                        <span>新增</span>
                      </Button>
                    </div>
                    <Table
                      bordered
                      class="qms-ncr-other-defect-table"
                      :columns="otherDefectColumns"
                      :data-source="formData.defects || []"
                      :locale="{ emptyText: '暂无其他缺陷' }"
                      :pagination="false"
                      :row-key="getOtherDefectRowKey"
                      :scroll="otherDefectTableScroll"
                      size="small"
                    >
                      <template #bodyCell="{ column, index, record }">
                        <template v-if="column.dataIndex === 'index'">
                          {{ index + 1 }}
                        </template>
                        <template v-else-if="column.dataIndex === 'defectCode'">
                          <DefectCodePicker
                            v-if="canEditQualityFields"
                            v-model:value="record.defectCode"
                            :dropdown-width="320"
                            :list-height="240"
                            modal-title="选择其他缺陷代码"
                            @change="
                              (value) => handleOtherDefectChange(record, value)
                            "
                            @select="
                              (defect) =>
                                handleOtherDefectSelect(record, defect)
                            "
                          />
                          <span v-else>
                            {{ displayValue(record.defectCode) }}
                          </span>
                        </template>
                        <template v-else-if="column.dataIndex === 'defectName'">
                          <Input
                            v-if="canEditQualityFields"
                            v-model:value="record.defectName"
                            @change="() => handleOtherDefectNameChange(record)"
                          />
                          <span v-else>{{
                            displayValue(record.defectName)
                          }}</span>
                        </template>
                        <template
                          v-else-if="
                            column.dataIndex === 'sourceInspectionItem'
                          "
                        >
                          <span>{{
                            displayValue(record.sourceInspectionItem)
                          }}</span>
                        </template>
                        <template
                          v-else-if="column.dataIndex === 'sourceSectionName'"
                        >
                          <span>{{
                            displayValue(record.sourceSectionName)
                          }}</span>
                        </template>
                        <template v-else-if="column.dataIndex === 'action'">
                          <div
                            v-if="canEditQualityFields"
                            class="qms-ncr-table-actions"
                          >
                            <Button
                              size="small"
                              title="新增一行"
                              type="text"
                              @click="addOtherDefectRow"
                            >
                              <IconifyIcon icon="lucide:plus" />
                            </Button>
                            <Button
                              danger
                              size="small"
                              title="删除"
                              type="text"
                              @click="removeOtherDefectRow(record)"
                            >
                              <IconifyIcon icon="lucide:trash-2" />
                            </Button>
                          </div>
                        </template>
                      </template>
                    </Table>
                  </div>
                </div>
                <label>会签单位</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-7">
                  <Form.Item
                    v-if="canEditPreSubmitFields || canEditQualityConfirm"
                    class="qms-ncr-form-item"
                  >
                    <Checkbox.Group
                      v-model:value="selectedReviewDeptNames"
                      class="qms-ncr-review-depts"
                      :disabled="
                        !(canEditPreSubmitFields || canEditQualityConfirm)
                      "
                    >
                      <Checkbox
                        v-for="dept in reviewDeptOptions"
                        :key="dept"
                        :value="dept"
                      >
                        {{ dept }}
                      </Checkbox>
                    </Checkbox.Group>
                  </Form.Item>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayReviewDeptNames() }}
                  </div>
                </div>
                <label class="qms-ncr-form-label--row-start">会签办理人</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-7">
                  <div
                    v-if="canEditReviewHandlers"
                    class="qms-ncr-subtable qms-ncr-review-handlers"
                  >
                    <div class="qms-ncr-subtable__toolbar">
                      <Button
                        size="small"
                        title="新增会签人"
                        @click="addReviewHandlerRow()"
                      >
                        <IconifyIcon icon="lucide:plus" />
                        <span>新增</span>
                      </Button>
                    </div>
                    <Table
                      bordered
                      class="qms-ncr-review-handler-table"
                      :columns="reviewHandlerColumns"
                      :data-source="formData.reviews || []"
                      :locale="{ emptyText: '请选择会签单位' }"
                      :pagination="false"
                      :row-key="getReviewHandlerRowKey"
                      :scroll="reviewHandlerTableScroll"
                      size="small"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.dataIndex === 'deptName'">
                          <span class="qms-ncr-table-strong">
                            {{ displayValue(record.deptName) }}
                          </span>
                        </template>
                        <template
                          v-else-if="column.dataIndex === 'handlerUserId'"
                        >
                          <Select
                            v-model:value="record.handlerUserId"
                            :filter-option="filterUserOption"
                            :loading="userOptionsLoading"
                            :options="userOptions"
                            placeholder="请选择会签人"
                            show-search
                            @change="
                              (value) =>
                                handleReviewHandlerChange(record, value)
                            "
                          >
                            <template #option="{ label, username }">
                              <div class="qms-ncr-user-option">
                                <span class="qms-ncr-user-option__name">
                                  {{ label }}
                                </span>
                                <span class="qms-ncr-user-option__code">
                                  {{ username || '-' }}
                                </span>
                              </div>
                            </template>
                          </Select>
                        </template>
                        <template v-else-if="column.dataIndex === 'action'">
                          <div class="qms-ncr-table-actions">
                            <Button
                              size="small"
                              title="新增同单位会签人"
                              type="text"
                              @click="addReviewHandlerRow(record.deptName)"
                            >
                              <IconifyIcon icon="lucide:user-plus" />
                            </Button>
                            <Button
                              size="small"
                              title="按配置刷新"
                              type="text"
                              @click="
                                resetReviewHandlersByConfig(record.deptName)
                              "
                            >
                              <IconifyIcon icon="lucide:refresh-cw" />
                            </Button>
                            <Button
                              danger
                              size="small"
                              title="删除"
                              type="text"
                              @click="removeReviewHandlerRow(record)"
                            >
                              <IconifyIcon icon="lucide:trash-2" />
                            </Button>
                          </div>
                        </template>
                      </template>
                    </Table>
                  </div>
                  <div v-else class="qms-ncr-readonly-value">
                    {{ displayReviewHandlers() }}
                  </div>
                </div>
                <label>确认日期</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-3">
                  <div class="qms-ncr-readonly-value">
                    {{ displayValue(formData.qualityConfirmTime) }}
                  </div>
                </div>
                <label>确认人</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--span-3">
                  <div class="qms-ncr-readonly-value">
                    {{ displayValue(formData.qualityConfirmUserName) }}
                  </div>
                </div>
              </div>
            </section>

            <section
              v-if="showReviewSignSection"
              class="qms-ncr-form-fieldset qms-ncr-form-fieldset--review"
            >
              <div class="qms-ncr-form-legend">
                {{ isRawMaterialNcr ? '会签单位处置会签' : '评审会签' }}
              </div>
              <div class="qms-ncr-sign-grid">
                <div
                  v-for="row in formData.reviews"
                  :key="`${row.deptName || ''}-${row.handlerUserId || row.id || row.sort || ''}`"
                  class="qms-ncr-sign-cell"
                  :class="{
                    'qms-ncr-sign-cell--active': canEditReviewDisposition(row),
                  }"
                >
                  <div class="qms-ncr-sign-cell__dept">
                    <span>{{ row.deptName }}</span>
                    <Tooltip title="委托代办">
                      <Button
                        v-if="canDelegateReview(row)"
                        size="small"
                        type="link"
                        @click="openReviewDelegateSelector(row)"
                      >
                        <IconifyIcon icon="lucide:user-plus" />
                      </Button>
                    </Tooltip>
                  </div>
                  <div class="qms-ncr-sign-cell__body">
                    <div class="qms-ncr-sign-main">
                      <Input.TextArea
                        v-if="canEditReviewDisposition(row)"
                        v-model:value="row.dispositionDetail"
                        class="qms-ncr-sign-detail"
                        :rows="2"
                        placeholder="处置详细说明"
                      />
                      <div
                        v-else
                        class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline qms-ncr-sign-detail-readonly"
                      >
                        {{ displayValue(row.dispositionDetail) }}
                      </div>
                    </div>
                    <div class="qms-ncr-sign-footer">
                      <div class="qms-ncr-sign-option-row">
                        <span class="qms-ncr-sign-option-label">会签选项</span>
                        <Radio.Group
                          v-if="isRawMaterialNcr"
                          :value="
                            getSingleDispositionValue(row.suggestedDisposition)
                          "
                          class="qms-ncr-sign-disposition"
                          :disabled="!canEditReviewDisposition(row)"
                          @update:value="
                            (value) =>
                              handleRawReviewDispositionChange(row, value)
                          "
                        >
                          <Radio
                            v-for="option in effectiveDispositionOptions"
                            :key="option.value"
                            :value="option.value"
                          >
                            {{ option.label }}
                          </Radio>
                        </Radio.Group>
                        <Checkbox.Group
                          v-else
                          :value="
                            parseDispositionValues(row.suggestedDisposition)
                          "
                          class="qms-ncr-sign-disposition"
                          :disabled="!canEditReviewDisposition(row)"
                          @update:value="
                            (values) =>
                              handleReviewDispositionChange(row, values)
                          "
                        >
                          <Checkbox
                            v-for="option in effectiveDispositionOptions"
                            :key="option.value"
                            :value="option.value"
                          >
                            {{ option.label }}
                          </Checkbox>
                        </Checkbox.Group>
                      </div>
                      <div class="qms-ncr-sign-meta">
                        <div class="qms-ncr-sign-meta-item">
                          <span>办理人</span>
                          <div class="qms-ncr-sign-meta-value">
                            <b>{{ getReviewHandlerDisplay(row) }}</b>
                            <em v-if="getReviewDelegateText(row)">
                              {{ getReviewDelegateText(row) }}
                            </em>
                          </div>
                        </div>
                        <div class="qms-ncr-sign-meta-item">
                          <span>办理时间</span>
                          <b>{{ displayValue(row.handleTime) }}</b>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <section
              v-if="showFinalSection"
              class="qms-ncr-form-fieldset qms-ncr-form-fieldset--final"
            >
              <div class="qms-ncr-form-legend">终审意见</div>
              <div class="qms-ncr-form-head qms-ncr-form-head--compact">
                <label>终审选项</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--full">
                  <Form.Item
                    v-if="canEditFinalFields"
                    class="qms-ncr-form-item"
                    name="finalDisposition"
                  >
                    <Radio.Group
                      v-if="isRawMaterialNcr"
                      :value="
                        getSingleDispositionValue(formData.finalDisposition)
                      "
                      class="qms-ncr-final-radio qms-ncr-sign-disposition"
                      @update:value="handleRawFinalDispositionChange"
                    >
                      <Radio
                        v-for="option in effectiveDispositionOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </Radio>
                    </Radio.Group>
                    <Checkbox.Group
                      v-else
                      :value="finalDispositionValues"
                      class="qms-ncr-final-radio qms-ncr-sign-disposition"
                      @update:value="handleFinalDispositionChange"
                    >
                      <Checkbox
                        v-for="option in effectiveDispositionOptions"
                        :key="option.value"
                        :value="option.value"
                      >
                        {{ option.label }}
                      </Checkbox>
                    </Checkbox.Group>
                  </Form.Item>
                  <Radio.Group
                    v-else-if="isRawMaterialNcr && formData.finalDisposition"
                    :value="
                      getSingleDispositionValue(formData.finalDisposition)
                    "
                    class="qms-ncr-final-radio qms-ncr-sign-disposition"
                    disabled
                  >
                    <Radio
                      v-for="option in effectiveDispositionOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </Radio>
                  </Radio.Group>
                  <Checkbox.Group
                    v-else-if="formData.finalDisposition"
                    :value="finalDispositionValues"
                    class="qms-ncr-final-radio qms-ncr-sign-disposition"
                    disabled
                  >
                    <Checkbox
                      v-for="option in effectiveDispositionOptions"
                      :key="option.value"
                      :value="option.value"
                    >
                      {{ option.label }}
                    </Checkbox>
                  </Checkbox.Group>
                  <div v-else class="qms-ncr-readonly-value">-</div>
                </div>
                <label>终审意见</label>
                <div class="qms-ncr-form-control qms-ncr-form-control--full">
                  <Form.Item
                    v-if="canEditFinalFields"
                    class="qms-ncr-form-item"
                  >
                    <Input.TextArea
                      v-model:value="formData.finalOpinion"
                      :rows="2"
                      placeholder="填写终审意见"
                    />
                  </Form.Item>
                  <div
                    v-else
                    class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                  >
                    {{ displayValue(formData.finalOpinion) }}
                  </div>
                </div>
                <template
                  v-if="showDisposeAssignmentSummary && !isRawMaterialNcr"
                >
                  <label>处置数量</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div class="qms-ncr-readonly-value">
                      {{ displayValue(formData.stockDisposeQty) }}
                    </div>
                  </div>
                  <label>具体选择片号列表</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div
                      class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                    >
                      {{ selectedDispositionPieceListText }}
                    </div>
                  </div>
                </template>
              </div>

              <Alert
                v-if="
                  !isRawMaterialNcr &&
                  dispositionIncludes(formData.finalDisposition, 'SCRAP')
                "
                type="error"
                show-icon
                class="qms-ncr-stock-alert"
              >
                <template #message>
                  <span class="font-bold">报废范围确认</span>
                </template>
                <template #description>
                  {{
                    formData.status === 'EXECUTION_ASSIGN'
                      ? '请在下方从真实候选中选择需要全部报废的片号，提交后自动生成并下达工作台指令。'
                      : `批次 ${formData.lotNo || '-'} 的报废执行情况以关联处置执行单和工作台指令为准。`
                  }}
                </template>
              </Alert>
            </section>

            <section
              v-if="showActionSection"
              class="qms-ncr-form-fieldset qms-ncr-form-fieldset--action"
            >
              <div class="qms-ncr-form-legend">{{ actionSectionLegend }}</div>
              <div class="qms-ncr-form-head qms-ncr-form-head--compact">
                <template v-if="showTransferControls">
                  <label>办理方式</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item" required>
                      <Radio.Group v-model:value="transferRoute">
                        <Radio
                          v-if="formData.status === 'REVIEW_ASSIGN'"
                          :value="RAW_TRANSFER_ROUTE_FINAL"
                        >
                          提交终审人审批
                        </Radio>
                        <Radio :value="RAW_TRANSFER_ROUTE_EXECUTION">
                          处置执行分派
                        </Radio>
                        <Radio :value="RAW_TRANSFER_ROUTE_CLOSE">
                          直接关闭
                        </Radio>
                      </Radio.Group>
                    </Form.Item>
                  </div>
                </template>
                <template v-if="canEditTransferDecision">
                  <label>处置选项</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item" required>
                      <Radio.Group
                        v-if="isRawMaterialNcr"
                        :value="
                          getSingleDispositionValue(formData.finalDisposition)
                        "
                        class="qms-ncr-final-radio qms-ncr-sign-disposition"
                        @update:value="handleRawFinalDispositionChange"
                      >
                        <Radio
                          v-for="option in effectiveDispositionOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </Radio>
                      </Radio.Group>
                      <Checkbox.Group
                        v-else
                        :value="finalDispositionValues"
                        class="qms-ncr-final-radio qms-ncr-sign-disposition"
                        @update:value="handleFinalDispositionChange"
                      >
                        <Checkbox
                          v-for="option in effectiveDispositionOptions"
                          :key="option.value"
                          :value="option.value"
                        >
                          {{ option.label }}
                        </Checkbox>
                      </Checkbox.Group>
                    </Form.Item>
                  </div>
                  <label>终审意见</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item" required>
                      <Input.TextArea
                        v-model:value="formData.finalOpinion"
                        :rows="2"
                        placeholder="填写本次处置的终审意见"
                      />
                    </Form.Item>
                  </div>
                </template>
                <template v-if="showTransferMeasure">
                  <label>转办建议</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item">
                      <Input.TextArea
                        v-model:value="formData.finalDisposeDescription"
                        :rows="2"
                        placeholder="填写与处置选项对应的转办建议"
                      />
                    </Form.Item>
                  </div>
                </template>
                <template v-if="showTransferControls">
                  <label>是否生成异常事件提报单</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item">
                      <Radio.Group v-model:value="formData.createExceptionFlag">
                        <Radio :value="true">是</Radio>
                        <Radio :value="false">否</Radio>
                      </Radio.Group>
                    </Form.Item>
                  </div>
                </template>
                <template
                  v-if="
                    !showTransferControls &&
                    [
                      'CLOSED',
                      'CLOSE_CONFIRM',
                      'PENDING_STOCK_DISPOSE',
                    ].includes(formData.status || '')
                  "
                >
                  <label>转办建议</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div
                      class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                    >
                      {{ displayValue(formData.finalDisposeDescription) }}
                    </div>
                  </div>
                  <label>是否生成异常事件提报单</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div class="qms-ncr-readonly-value">
                      {{ formData.createExceptionFlag ? '是' : '否' }}
                    </div>
                  </div>
                </template>
                <template v-if="showStockDisposeUploadOpinion">
                  <label>处置结果上传办理说明</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div
                      class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                    >
                      {{ stockDisposeUploadOpinion }}
                    </div>
                  </div>
                </template>
                <template v-if="showInlineNextHandler">
                  <label>{{ nextHandlerLabel }}</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item" required>
                      <Select
                        v-if="useMultipleNextHandlers"
                        v-model:value="nextHandlerUserIds"
                        :filter-option="filterUserOption"
                        :loading="userOptionsLoading"
                        mode="multiple"
                        :options="userOptions"
                        placeholder="请选择产品范围确认办理人，可多选"
                        show-search
                        @change="handleNextHandlerUsersChange"
                      >
                        <template #option="{ label, username }">
                          <div class="qms-ncr-user-option">
                            <span class="qms-ncr-user-option__name">
                              {{ label }}
                            </span>
                            <span class="qms-ncr-user-option__code">
                              {{ username || '-' }}
                            </span>
                          </div>
                        </template>
                      </Select>
                      <Select
                        v-else
                        v-model:value="nextHandlerUserId"
                        :filter-option="filterUserOption"
                        :loading="userOptionsLoading"
                        :options="userOptions"
                        :placeholder="
                          needsContentConfirmUser
                            ? '请选择再次确认人'
                            : formData.status === 'EXECUTION_ASSIGN'
                              ? '请选择处置执行人'
                              : '请选择下一节点办理人'
                        "
                        show-search
                      >
                        <template #option="{ label, username }">
                          <div class="qms-ncr-user-option">
                            <span class="qms-ncr-user-option__name">
                              {{ label }}
                            </span>
                            <span class="qms-ncr-user-option__code">
                              {{ username || '-' }}
                            </span>
                          </div>
                        </template>
                      </Select>
                    </Form.Item>
                  </div>
                </template>
                <template v-if="showDispositionNotifyPicker">
                  <label>通知人</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item">
                      <Select
                        v-model:value="dispositionNotifyUserIds"
                        :filter-option="filterUserOption"
                        :loading="userOptionsLoading"
                        mode="multiple"
                        :options="dispositionNotifyOptions"
                        placeholder="请选择需要回复办理结论的通知人"
                        show-search
                        @change="handleDispositionNotifyChange"
                      >
                        <template #option="{ label, username }">
                          <div class="qms-ncr-user-option">
                            <span class="qms-ncr-user-option__name">
                              {{ label }}
                            </span>
                            <span class="qms-ncr-user-option__code">
                              {{ username || '-' }}
                            </span>
                          </div>
                        </template>
                      </Select>
                    </Form.Item>
                  </div>
                </template>
                <template v-if="hasDispositionNotifies">
                  <label>通知回复</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div class="qms-ncr-notify-list">
                      <div
                        v-for="row in formData.dispositionNotifies || []"
                        :key="row.id || `${row.notifyUserId}-${row.notifyTime}`"
                        class="qms-ncr-notify-row"
                      >
                        <div class="qms-ncr-notify-row__head">
                          <b>{{ displayValue(row.notifyUserName) }}</b>
                          <Tag :color="notifyStatusColor(row.notifyStatus)">
                            {{ displayNotifyStatus(row.notifyStatus) }}
                          </Tag>
                          <span>
                            通知时间：{{ displayValue(row.notifyTime) }}
                          </span>
                          <span v-if="row.replyTime">
                            回复时间：{{ displayValue(row.replyTime) }}
                          </span>
                        </div>
                        <div class="qms-ncr-notify-row__body">
                          {{ displayValue(row.replyConclusion || row.remark) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </template>
                <DispositionScopePanel
                  v-if="
                    formData.status === 'EXECUTION_ASSIGN' &&
                    !isRawMaterialNcr &&
                    transferRoute === RAW_TRANSFER_ROUTE_EXECUTION
                  "
                  ref="dispositionScopePanelRef"
                  :disposition="formData.finalDisposition"
                  :ncr-id="formData.id"
                />
                <template
                  v-if="
                    !showTransferControls &&
                    !isDispositionNotifyReplyMode &&
                    !(
                      formData.status === 'EXECUTION_ASSIGN' &&
                      !isRawMaterialNcr &&
                      transferRoute === RAW_TRANSFER_ROUTE_EXECUTION
                    )
                  "
                >
                  <label>办理说明</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item">
                      <Input.TextArea
                        v-model:value="actionOpinion"
                        :disabled="!canOperate"
                        :rows="2"
                        :placeholder="actionOpinionPlaceholder"
                      />
                    </Form.Item>
                  </div>
                </template>
                <template v-if="isDispositionNotifyReplyMode">
                  <label>通知人办理结论</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item" required>
                      <Input.TextArea
                        v-model:value="dispositionNotifyReplyConclusion"
                        :rows="2"
                        placeholder="填写通知人办理结论"
                      />
                    </Form.Item>
                  </div>
                </template>
                <template
                  v-if="
                    formData.status === 'PENDING_STOCK_DISPOSE' &&
                    !isDispositionNotifyReplyMode
                  "
                >
                  <label>处置结果附件（选填）</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <Form.Item class="qms-ncr-form-item">
                      <FileUpload
                        v-model="dispositionAttachmentUrls"
                        :accept="attachmentAcceptTypes"
                        directory="mes/qms/ncr/disposition"
                        :max-number="20"
                        :max-size="20"
                        multiple
                        show-description
                        @preview="handleAttachmentPreview"
                      />
                    </Form.Item>
                  </div>
                </template>
                <template
                  v-if="
                    formData.status !== 'PENDING_STOCK_DISPOSE' &&
                    dispositionAttachmentUrls.length > 0
                  "
                >
                  <label>处置结果附件</label>
                  <div
                    class="qms-ncr-form-control qms-ncr-form-control--full qms-ncr-attachment-readonly"
                  >
                    <Button
                      v-for="(url, index) in dispositionAttachmentUrls"
                      :key="url"
                      size="small"
                      type="link"
                      @click="handleAttachmentPreview({ url })"
                    >
                      {{ getAttachmentName(url, index) }}
                    </Button>
                  </div>
                </template>
                <template
                  v-if="
                    formData.effectConfirmResult ||
                    formData.effectConfirmUserName ||
                    formData.effectConfirmTime
                  "
                >
                  <label>效果确认</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div
                      class="qms-ncr-readonly-value qms-ncr-readonly-value--multiline"
                    >
                      {{ displayValue(formData.effectConfirmResult) }}
                    </div>
                  </div>
                  <label>确认人</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div class="qms-ncr-readonly-value">
                      {{ displayValue(formData.effectConfirmUserName) }}
                    </div>
                  </div>
                  <label>确认日期</label>
                  <div class="qms-ncr-form-control qms-ncr-form-control--full">
                    <div class="qms-ncr-readonly-value">
                      {{ displayValue(formData.effectConfirmTime) }}
                    </div>
                  </div>
                </template>
              </div>
            </section>
          </div>
        </Form>
      </div>
    </div>
  </Modal>
</template>

<style>
.qms-abnormal-workbench-modal .ant-modal-body {
  height: 100%;
  overflow: hidden;
}

.qms-ncr-erp-modal .ant-modal-content {
  overflow: hidden;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  box-shadow: 0 10px 26px rgb(15 23 42 / 18%);
}

.qms-abnormal-workbench-modal .ant-modal-header,
.qms-abnormal-workbench-modal .ant-modal-close,
.qms-abnormal-workbench-modal .vben-modal-header,
.qms-ncr-erp-modal .ant-modal-header,
.qms-ncr-erp-modal .ant-modal-close {
  display: none !important;
}

.qms-ncr-erp-modal .ant-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-top: 1px solid #cbd5e1;
}

.qms-ncr-ng-detail-modal .ant-modal {
  top: 32px;
  max-width: calc(100vw - 48px);
  padding-bottom: 0;
}

.qms-ncr-ng-detail-modal .ant-modal-content {
  display: flex;
  height: calc(100vh - 64px);
  flex-direction: column;
  overflow: hidden;
}

.qms-ncr-ng-detail-modal .ant-modal-header {
  flex: 0 0 auto;
}

.qms-ncr-ng-detail-modal .ant-modal-body {
  display: flex;
  flex: 1 1 0%;
  height: auto !important;
  min-height: 0;
  flex-direction: column;
  overflow: hidden !important;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__meta,
.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__summary,
.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__section-head {
  flex: 0 0 auto;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__spin,
.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__spin.ant-spin-nested-loading,
.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__spin .ant-spin-container {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__spin .ant-spin-container {
  height: 100%;
  flex-direction: column;
  gap: 10px;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__section {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.qms-ncr-ng-detail-modal .qms-ncr-ng-detail__table-host {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden !important;
}
</style>

<style scoped>
.qms-ncr-detail {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: #1f2937;
  background:
    linear-gradient(90deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    linear-gradient(0deg, rgba(30, 41, 59, 0.04) 1px, transparent 1px) 0 0 /
      28px 28px,
    #f5f7fa;
}

.qms-ncr-toolbar {
  display: grid;
  grid-template-columns:
    minmax(260px, 1fr)
    minmax(360px, 720px)
    minmax(320px, 1fr);
  flex-shrink: 0;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 8px 14px;
  background: linear-gradient(180deg, #f7fbff 0%, #e8f1fb 100%);
  border-bottom: 1px solid #cbd5e1;
}

.qms-ncr-toolbar__placeholder {
  min-width: 0;
}

.qms-ncr-title-panel {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 4px;
  text-align: center;
}

.qms-ncr-title-panel__name {
  overflow: hidden;
  color: #075985;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-title-panel__subtitle {
  display: flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 16px;
}

.qms-ncr-title-panel__subtitle-item {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-toolbar__actions {
  display: flex;
  min-width: max-content;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 6px;
  justify-self: end;
}

.qms-ncr-toolbar__log-icons {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 4px;
}

.qms-ncr-toolbar-icon-btn.ant-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0;
  color: #475569;
}

.qms-ncr-toolbar-icon-btn :deep(svg) {
  width: 18px;
  height: 18px;
}

.qms-ncr-toolbar-action.ant-btn {
  display: inline-flex;
  min-width: 56px;
  height: 32px;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  line-height: 1;
}

.qms-ncr-toolbar-action span {
  max-width: 100%;
  overflow: hidden;
  font-size: 13px;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-workbench {
  display: block;
  min-height: 0;
  flex: 1;
  padding: 8px;
  overflow: hidden;
}

.qms-ncr-main {
  height: 100%;
  min-height: 0;
  overflow: auto;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 0;
  padding: 8px;
}

.qms-ncr-form-sections {
  display: grid;
  gap: 8px;
}

.qms-ncr-form-fieldset {
  min-width: 0;
  padding: 0 12px 10px;
  margin: 0;
  background: #fff;
  border: 1px solid #d8e0ec;
}

.qms-ncr-form-legend {
  display: flex;
  align-items: center;
  height: 32px;
  margin: 0 0 10px;
  padding: 0 10px;
  border-bottom: 1px solid #e5e7eb;
  color: #1677ff;
  font-size: 13px;
  font-weight: 800;
  line-height: 20px;
}

.qms-ncr-form-head {
  display: grid;
  grid-template-columns:
    92px minmax(0, 1fr)
    108px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr);
  align-items: stretch;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-right: 0;
  border-bottom: 0;
}

.qms-ncr-form-head--nc-info {
  grid-template-columns:
    92px minmax(0, 1.08fr)
    92px minmax(0, 1.08fr)
    92px minmax(0, 1fr)
    72px minmax(0, 0.86fr)
    58px minmax(0, 0.68fr)
    52px minmax(0, 0.54fr);
}

.qms-ncr-form-head--raw-nc-info {
  grid-template-columns:
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr)
    92px minmax(0, 1fr);
}

.qms-ncr-form-head--raw-nc-info .qms-ncr-form-control--raw-full {
  grid-column: span 7;
}

.qms-ncr-form-head--raw-nc-info .qms-ncr-form-control--span-5 {
  grid-column: span 3;
}

.qms-ncr-form-head--raw-nc-info .qms-ncr-form-control--span-11 {
  grid-column: span 7;
}

.qms-ncr-form-head--quality {
  grid-template-columns:
    92px minmax(0, 0.9fr)
    92px minmax(0, 1fr)
    92px minmax(0, 0.9fr)
    92px minmax(0, 0.9fr);
}

.qms-ncr-form-head--compact {
  grid-template-columns: 92px minmax(0, 1fr);
}

.qms-ncr-form-head > label,
.qms-ncr-basic-group > label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 32px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 16px;
  text-align: right;
  white-space: nowrap;
  background: #f1f5f9;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.qms-ncr-form-label--row-start {
  grid-column: 1;
}

.qms-ncr-form-control {
  display: flex;
  align-items: stretch;
  min-width: 0;
  min-height: 32px;
  padding: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.qms-ncr-form-control--span-3 {
  grid-column: span 3;
}

.qms-ncr-form-control--span-5 {
  grid-column: span 5;
}

.qms-ncr-form-control--span-7 {
  grid-column: span 7;
}

.qms-ncr-form-control--span-11 {
  grid-column: span 11;
}

.qms-ncr-form-control--full {
  grid-column: span 7;
}

.qms-ncr-basic-groups {
  display: grid;
  grid-column: 1 / -1;
  grid-template-columns:
    minmax(250px, 1.05fr)
    minmax(280px, 1.15fr)
    minmax(260px, 0.95fr)
    minmax(320px, 1.15fr);
  min-width: 0;
}

.qms-ncr-basic-group {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  min-width: 0;
}

.qms-ncr-basic-group--qty {
  grid-template-columns:
    72px minmax(0, 1.1fr) 72px minmax(0, 0.7fr)
    64px minmax(0, 0.55fr);
}

.qms-ncr-basic-group--qty .qms-ncr-lot-control {
  grid-column: span 5;
}

.qms-ncr-form-head--compact .qms-ncr-form-control--full {
  grid-column: span 1;
}

.qms-ncr-form-item {
  width: 100%;
  margin-bottom: 0;
}

.qms-ncr-form-control :deep(.ant-form-item),
.qms-ncr-form-control :deep(.ant-form-item-control),
.qms-ncr-form-control :deep(.ant-form-item-control-input),
.qms-ncr-form-control :deep(.ant-form-item-control-input-content) {
  width: 100%;
  min-height: 0;
  margin-bottom: 0;
}

.qms-ncr-form-control :deep(.ant-form-item-explain) {
  display: none;
}

.qms-ncr-form-control :deep(.ant-input),
.qms-ncr-form-control :deep(.ant-input-affix-wrapper),
.qms-ncr-form-control :deep(.ant-input-group-wrapper),
.qms-ncr-form-control :deep(.ant-picker),
.qms-ncr-form-control :deep(.ant-select),
.qms-ncr-form-control :deep(.ant-select-selector),
.qms-ncr-form-control :deep(.ant-input-number),
.qms-ncr-form-control :deep(.ant-input-number-input),
.qms-ncr-form-control :deep(.ant-input-number-affix-wrapper),
.qms-ncr-form-control :deep(.hc-picker-inline),
.qms-ncr-form-control :deep(.hc-picker-inline .ant-input-affix-wrapper),
.qms-ncr-form-control :deep(textarea.ant-input) {
  width: 100%;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.qms-ncr-form-control :deep(.ant-input),
.qms-ncr-form-control :deep(.ant-picker),
.qms-ncr-form-control :deep(.ant-select-selector),
.qms-ncr-form-control :deep(.ant-input-number),
.qms-ncr-form-control :deep(.ant-input-number-input),
.qms-ncr-form-control :deep(.hc-picker-inline .ant-input-affix-wrapper) {
  min-height: 31px;
}

.qms-ncr-form-control :deep(.ant-input-search-button),
.qms-ncr-form-control :deep(.ant-btn) {
  border-radius: 0;
}

.qms-ncr-form-control :deep(.ant-upload-wrapper) {
  width: 100%;
  padding: 6px 8px;
}

.qms-ncr-form-control :deep(.ant-upload-list) {
  margin-top: 4px;
}

.qms-ncr-form-control :deep(.ant-upload-list-item) {
  margin-top: 2px;
}

.qms-ncr-readonly-value {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 31px;
  align-items: center;
  padding: 6px 10px;
  color: #1e293b;
  font-size: 13px;
  line-height: 18px;
  word-break: break-word;
  white-space: pre-wrap;
  background: #fff;
}

.qms-ncr-readonly-value--with-action {
  justify-content: space-between;
  gap: 6px;
}

.qms-ncr-readonly-value--with-action > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-readonly-value--multiline {
  min-height: 72px;
  align-items: flex-start;
  white-space: pre-wrap;
}

.qms-ncr-notify-list {
  display: grid;
  width: 100%;
  gap: 6px;
  padding: 6px 8px;
}

.qms-ncr-notify-row {
  display: grid;
  min-width: 0;
  gap: 4px;
  padding: 6px 0;
  border-bottom: 1px solid #e5e7eb;
}

.qms-ncr-notify-row:last-child {
  border-bottom: 0;
}

.qms-ncr-notify-row__head {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.qms-ncr-notify-row__head b {
  color: #0f172a;
  font-size: 13px;
}

.qms-ncr-notify-row__body {
  color: #1e293b;
  font-size: 13px;
  line-height: 18px;
  white-space: pre-wrap;
}

.qms-ncr-readonly-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f2744;
  font-size: 12px;
  font-weight: 700;
}

.qms-ncr-attachment-readonly {
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 4px 8px;
}

.qms-ncr-attachment-readonly :deep(.ant-btn-link) {
  height: 24px;
  padding: 0;
}

.qms-ncr-description-box {
  position: relative;
  width: 100%;
  min-width: 0;
}

.qms-ncr-description-box :deep(textarea.ant-input) {
  min-height: 88px;
  padding-right: 34px;
  resize: vertical;
}

.qms-ncr-description-expand.ant-btn {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 28px;
  min-width: 28px;
  height: 28px;
  padding: 0;
  color: #64748b;
  background: rgb(255 255 255 / 86%);
}

.qms-ncr-description-modal-textarea {
  min-height: 62vh !important;
  resize: vertical;
}

.qms-ncr-final-approver-picker {
  padding-top: 4px;
}

.qms-ncr-ng-detail-entry {
  display: flex;
  width: 100%;
  min-width: 0;
  min-height: 31px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 4px 10px;
  color: #1e293b;
  font-size: 13px;
  line-height: 18px;
  background: #fff;
}

.qms-ncr-ng-detail-entry > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-ng-detail-entry__button.ant-btn {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  color: #1677ff;
  font-size: 12px;
}

.qms-ncr-ng-detail {
  display: flex;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.qms-ncr-ng-detail__meta {
  display: grid;
  flex-shrink: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  line-height: 18px;
}

.qms-ncr-ng-detail__meta > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-ng-detail__spin {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  overflow: hidden;
}

.qms-ncr-ng-detail__spin :deep(.ant-spin-container) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
  gap: 10px;
  overflow: hidden;
}

.qms-ncr-ng-detail__summary {
  display: grid;
  flex-shrink: 0;
  grid-template-columns: 96px minmax(0, 1fr);
  border: 1px solid #d8e0ec;
  background: #fff;
}

.qms-ncr-ng-detail__summary-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 10px;
  border-right: 1px solid #d8e0ec;
  background: #f1f5f9;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.qms-ncr-ng-detail__summary-content {
  min-width: 0;
  max-height: 74px;
  overflow: auto;
  padding: 8px 10px;
  color: #b42318;
  font-size: 13px;
  line-height: 20px;
  white-space: pre-wrap;
}

.qms-ncr-ng-detail__section {
  display: flex;
  min-height: 0;
  flex: 1 1 0%;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d8e0ec;
  background: #fff;
}

.qms-ncr-ng-detail__section-head {
  display: flex;
  height: 36px;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
  color: #0f2744;
  font-size: 13px;
}

.qms-ncr-ng-detail__section-head span {
  color: #64748b;
  font-size: 12px;
}

.qms-ncr-ng-detail__section-tools {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: #475569;
  font-size: 12px;
}

.qms-ncr-ng-detail__section-tools :deep(.ant-checkbox-wrapper) {
  color: #334155;
  font-size: 12px;
}

.qms-ncr-ng-detail__table-host {
  display: flex;
  flex: 1 1 0%;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-ncr-ng-detail__fallback-table {
  flex: 1 1 0%;
  height: 100%;
  min-height: 0;
}

.qms-ncr-ng-detail__fallback-table :deep(.ant-table) {
  font-size: 12px;
}

.qms-ncr-ng-detail__multiline {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  line-height: 18px;
  overflow-wrap: anywhere;
  -webkit-line-clamp: 3;
}

.qms-ncr-subtable {
  display: flex;
  width: 100%;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
  background: #fff;
}

.qms-ncr-other-defects {
  height: 224px;
}

.qms-ncr-review-handlers {
  height: 210px;
}

.qms-ncr-subtable__toolbar {
  display: flex;
  min-height: 26px;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.qms-ncr-subtable__toolbar .ant-btn {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  font-size: 12px;
}

.qms-ncr-other-defect-table,
.qms-ncr-review-handler-table {
  min-height: 0;
  flex: 1;
}

.qms-ncr-other-defect-table :deep(.ant-table),
.qms-ncr-review-handler-table :deep(.ant-table) {
  font-size: 12px;
}

.qms-ncr-other-defect-table :deep(.ant-table-cell),
.qms-ncr-review-handler-table :deep(.ant-table-cell) {
  padding: 4px 6px;
  vertical-align: middle;
}

.qms-ncr-other-defect-table :deep(.defect-code-picker__button.ant-btn) {
  height: 30px;
}

.qms-ncr-other-defect-table :deep(.ant-input),
.qms-ncr-other-defect-table :deep(.ant-select-selector),
.qms-ncr-review-handler-table :deep(.ant-select-selector) {
  min-height: 30px;
  border-radius: 0;
}

.qms-ncr-user-option {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.qms-ncr-user-option__name {
  min-width: 0;
  overflow: hidden;
  color: #10233d;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-user-option__code {
  flex: 0 0 auto;
  color: #64748b;
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono',
    'Courier New', monospace;
  font-size: 12px;
}

.qms-ncr-table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.qms-ncr-table-actions .ant-btn {
  display: inline-flex;
  width: 24px;
  min-width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.qms-ncr-table-strong {
  color: #0f2744;
  font-weight: 700;
}

.qms-ncr-level-radio,
.qms-ncr-final-radio,
.qms-ncr-review-depts {
  display: flex;
  width: 100%;
  min-height: 32px;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 8px;
}

.qms-ncr-level-radio :deep(.ant-radio-wrapper),
.qms-ncr-final-radio :deep(.ant-radio-wrapper),
.qms-ncr-review-depts :deep(.ant-checkbox-wrapper) {
  background: transparent !important;
  border: 0 !important;
  box-shadow: none !important;
}

.qms-ncr-level-radio :deep(.ant-radio-wrapper) {
  min-width: 72px;
  margin-inline-end: 16px;
  color: #0f2744;
  font-weight: 700;
}

.qms-ncr-sign-grid {
  display: flex;
  flex-direction: column;
  margin-top: 8px;
  border-top: 1px solid #cbd5e1;
  border-left: 1px solid #cbd5e1;
  background: #fff;
}

.qms-ncr-sign-cell {
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr);
  min-height: 138px;
  border-right: 1px solid #cbd5e1;
  border-bottom: 1px solid #cbd5e1;
}

.qms-ncr-sign-cell--active {
  box-shadow: inset 3px 0 0 #1677ff;
}

.qms-ncr-sign-cell__dept {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  border-right: 1px solid #cbd5e1;
  background: #f1f5f9;
  color: #0f2744;
  font-size: 14px;
  font-weight: 700;
  text-align: center;
}

.qms-ncr-sign-cell__dept > span {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qms-ncr-sign-cell__dept .ant-btn {
  display: inline-flex;
  width: 24px;
  min-width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.qms-ncr-sign-cell__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 8px;
}

.qms-ncr-sign-main {
  min-width: 0;
}

.qms-ncr-sign-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  min-width: 0;
  padding-top: 2px;
}

.qms-ncr-sign-option-row {
  display: flex;
  min-width: 0;
  min-height: 30px;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.qms-ncr-sign-option-label {
  flex: 0 0 auto;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.qms-ncr-sign-disposition {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  align-content: center;
  align-items: center;
  min-width: 0;
  padding: 0;
}

.qms-ncr-sign-disposition :deep(.ant-radio-wrapper),
.qms-ncr-sign-disposition :deep(.ant-checkbox-wrapper) {
  margin-inline-end: 0;
  color: #0f2744;
  font-weight: 600;
  white-space: nowrap;
}

.qms-ncr-sign-disposition :deep(.ant-radio-wrapper-disabled),
.qms-ncr-sign-disposition :deep(.ant-radio-wrapper-disabled span),
.qms-ncr-sign-disposition :deep(.ant-checkbox-wrapper-disabled),
.qms-ncr-sign-disposition :deep(.ant-checkbox-wrapper-disabled span) {
  color: #0f2744;
}

.qms-ncr-sign-detail {
  min-height: 72px;
  resize: vertical;
}

.qms-ncr-sign-detail-readonly {
  border: 1px solid #e2e8f0;
  background: #fbfdff;
}

.qms-ncr-sign-meta {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.qms-ncr-sign-meta-item {
  display: grid;
  grid-template-columns: 62px minmax(92px, 1fr);
  align-items: center;
  min-width: 168px;
  min-height: 32px;
  border: 1px solid #e2e8f0;
  background: #fff;
}

.qms-ncr-sign-meta-item span {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: flex-end;
  padding: 0 8px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.qms-ncr-sign-meta-item b {
  min-width: 0;
  overflow: hidden;
  padding: 0 8px;
  color: #0f2744;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-sign-meta-value {
  display: flex;
  min-width: 0;
  flex-direction: column;
  justify-content: center;
  line-height: 17px;
}

.qms-ncr-sign-meta-value b {
  padding: 0 8px;
}

.qms-ncr-sign-meta-value em {
  min-width: 0;
  overflow: hidden;
  padding: 0 8px;
  color: #64748b;
  font-size: 11px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qms-ncr-sign-cell :deep(.ant-input),
.qms-ncr-sign-cell :deep(.ant-input-affix-wrapper),
.qms-ncr-sign-cell :deep(.ant-picker),
.qms-ncr-sign-cell :deep(textarea.ant-input) {
  border-color: #d8e0ea;
  border-radius: 0;
}

.qms-ncr-sign-cell :deep(.ant-input[disabled]),
.qms-ncr-sign-cell :deep(.ant-picker-disabled),
.qms-ncr-sign-cell :deep(textarea.ant-input[disabled]) {
  color: #475569;
  background: #f8fafc;
}

.qms-ncr-handler-picker.ant-btn {
  width: 22px;
  min-width: 22px;
  height: 22px;
  padding: 0;
  color: #64748b;
}

.qms-ncr-handler-picker :deep(svg) {
  width: 13px;
  height: 13px;
}

.qms-ncr-inline-icon-btn.ant-btn {
  width: 22px;
  min-width: 22px;
  height: 22px;
  padding: 0;
  color: #64748b;
}

.qms-ncr-inline-icon-btn :deep(svg) {
  width: 13px;
  height: 13px;
}

.qms-ncr-stock-alert {
  margin-top: 8px;
}

.qms-ncr-detail {
  font-size: 14px;
  line-height: 20px;
}

.qms-ncr-detail .qms-ncr-title-panel__subtitle,
.qms-ncr-detail .qms-ncr-toolbar-action span {
  font-size: 13px;
}

.qms-ncr-detail .qms-ncr-form-legend {
  font-size: 14px;
}

.qms-ncr-detail .qms-ncr-form-head > label,
.qms-ncr-detail .qms-ncr-basic-group > label,
.qms-ncr-detail .qms-ncr-ng-detail__summary-label,
.qms-ncr-detail .qms-ncr-sign-option-label,
.qms-ncr-detail .qms-ncr-sign-meta-item span,
.qms-ncr-detail .qms-ncr-sign-meta-item b,
.qms-ncr-detail .qms-ncr-readonly-pill {
  font-size: 13px;
}

.qms-ncr-detail .qms-ncr-readonly-value,
.qms-ncr-detail .qms-ncr-ng-detail-entry,
.qms-ncr-detail .qms-ncr-ng-detail__summary-content,
.qms-ncr-detail .qms-ncr-ng-detail__section-head,
.qms-ncr-detail .qms-ncr-sign-cell__dept {
  font-size: 14px;
  line-height: 20px;
}

.qms-ncr-detail :deep(.ant-input),
.qms-ncr-detail :deep(.ant-input-affix-wrapper),
.qms-ncr-detail :deep(.ant-input-number),
.qms-ncr-detail :deep(.ant-input-number-input),
.qms-ncr-detail :deep(.ant-picker),
.qms-ncr-detail :deep(.ant-select),
.qms-ncr-detail :deep(.ant-select-selector),
.qms-ncr-detail :deep(.ant-select-selection-item),
.qms-ncr-detail :deep(.ant-radio-wrapper),
.qms-ncr-detail :deep(.ant-checkbox-wrapper),
.qms-ncr-detail :deep(.ant-table),
.qms-ncr-detail :deep(.ant-tag),
.qms-ncr-detail :deep(textarea.ant-input) {
  font-size: 14px;
}

.qms-ncr-detail :deep(.ant-table-cell) {
  line-height: 20px;
}

table input:focus,
table textarea:focus {
  outline: none;
}

@media (max-width: 1280px) {
  .qms-ncr-toolbar {
    grid-template-columns:
      minmax(260px, 1fr)
      minmax(360px, 720px)
      minmax(320px, 1fr);
    align-items: center;
  }

  .qms-ncr-workbench {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .qms-ncr-form-head,
  .qms-ncr-form-head--nc-info,
  .qms-ncr-form-head--quality {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .qms-ncr-form-control--span-3,
  .qms-ncr-form-control--span-5,
  .qms-ncr-form-control--span-7,
  .qms-ncr-form-control--span-11,
  .qms-ncr-form-control--raw-full,
  .qms-ncr-form-control--full {
    grid-column: span 1;
  }

  .qms-ncr-basic-groups,
  .qms-ncr-basic-group,
  .qms-ncr-basic-group--qty {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .qms-ncr-basic-group--qty .qms-ncr-lot-control {
    grid-column: span 1;
  }

  .qms-ncr-toolbar__actions {
    justify-content: flex-end;
  }
}
</style>
