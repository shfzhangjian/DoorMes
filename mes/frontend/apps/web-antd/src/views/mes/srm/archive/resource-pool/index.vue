<script lang="ts" setup>
import type { SrmPreviewableAttachment } from '../../shared/attachmentPreview';

import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmPreliminaryEvaluationApi } from '#/api/mes/srm/preliminary-evaluation';
import type { SrmSupplierCandidateApi } from '#/api/mes/srm/supplier-candidate';
import type { SrmSupplierFileApi } from '#/api/mes/srm/supplier-file';
import type { MesSupplierApi } from '#/api/mes/supplier';
import type { PickerOption } from '#/components/picker';

import { computed, onDeactivated, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  DatePicker,
  Input,
  InputNumber,
  message,
  Radio,
  Select,
  Space,
  Table,
  TabPane,
  Tabs,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getEvaluationPage } from '#/api/mes/srm/preliminary-evaluation';
import {
  adjustSupplierResourceStatus,
  getSupplierCandidatePage,
  getSupplierResourceStatusLogs,
} from '#/api/mes/srm/supplier-candidate';
import { getSupplierFilePage } from '#/api/mes/srm/supplier-file';
import { getSupplierScopeSimpleList } from '#/api/mes/srm/supplier-scope';
import {
  createSupplier,
  getSupplier,
  updateSupplier,
} from '#/api/mes/supplier';
import {
  materialPickerConfig,
  PickerModal,
  productBomPickerConfig,
} from '#/components/picker';
import PreliminaryEvaluationInlineDetail from '#/views/mes/srm/certification/preliminary-evaluation/modules/inline-detail.vue';

import { canPreviewSrmAttachment } from '../../shared/attachmentPreview';
import SrmAttachmentPreviewModal from '../../shared/SrmAttachmentPreviewModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmCertificationSupplierResourcePool' });

type ResourceRecord = MesSupplierApi.Supplier &
  SrmSupplierCandidateApi.Candidate;

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '考察中', value: 'PENDING' },
  { label: '合格', value: 'QUALIFIED' },
  { label: '冻结', value: 'FROZEN' },
  { label: '淘汰', value: 'ELIMINATED' },
  { label: '退出', value: 'EXITED' },
  { label: '不合格', value: 'UNQUALIFIED' },
];

const SRM_RESOURCE_DETAIL_MODAL_Z_INDEX = 4100;
const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_DROPDOWN_Z_INDEX = 5700;
const SRM_ATTACHMENT_PREVIEW_MODAL_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 400;
const resourceDetailOpen = ref(false);
const resourceDetailPopupHostRef = ref<HTMLElement>();

const companyNatureOptions = [
  { label: '生产厂家(原厂)', value: 'MANUFACTURER' },
  { label: '贸易/代理商', value: 'AGENT' },
];

const materialGradeOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];

const levelOptions = [
  { label: 'A级', value: 'A' },
  { label: 'B级', value: 'B' },
  { label: 'C级', value: 'C' },
  { label: 'D级', value: 'D' },
];

const permissionTextMap: Record<string, string> = {
  EDIT: '可以编辑',
  FULL: '查看全部字段',
  MASKED: '脱敏查看',
};

const permissionColorMap: Record<string, string> = {
  EDIT: 'success',
  FULL: 'processing',
  MASKED: 'default',
};

const statusTextMap: Record<string, string> = {
  ELIMINATED: '淘汰',
  EXITED: '退出',
  FROZEN: '冻结',
  PENDING: '考察中',
  QUALIFIED: '合格',
  UNQUALIFIED: '不合格',
};

const preliminaryStatusTextMap: Record<string, string> = {
  DRAFT: '草稿',
  GM_REVIEW: '总经理办理',
  PENDING_CALCULATION: '待计算',
  PENDING_DECISION: '待最终判定',
  PENDING_PUBLISH: '待发布',
  PUBLISHED: '已发布',
  SCORING: '评分中',
};

const preliminaryStatusColorMap: Record<string, string> = {
  DRAFT: 'default',
  GM_REVIEW: 'processing',
  PENDING_CALCULATION: 'warning',
  PENDING_DECISION: 'warning',
  PENDING_PUBLISH: 'processing',
  PUBLISHED: 'success',
  SCORING: 'processing',
};

const preliminaryDecisionTextMap: Record<string, string> = {
  QUALIFIED: '合格',
  UNQUALIFIED: '不合格',
};

const preliminaryDecisionColorMap: Record<string, string> = {
  QUALIFIED: 'success',
  UNQUALIFIED: 'error',
};

const scopeOptions = ref<Array<{ label: string; value: number }>>([]);
const activeResourceStatus = ref('ALL');
const detailMode = ref<'create' | 'detail' | 'edit'>('detail');
const activeTab = ref('lifecycle');
const saving = ref(false);
const detailLoading = ref(false);
const selectedCandidate = ref<null | ResourceRecord>(null);
const detailForm = reactive<Record<string, any>>({});
const supplierFiles = ref<SrmSupplierFileApi.SupplierFile[]>([]);
const preliminaryEvaluations = ref<SrmPreliminaryEvaluationApi.Evaluation[]>(
  [],
);
const resourceStatusLogs = ref<SrmSupplierCandidateApi.ResourceStatusLog[]>([]);
const preliminaryPreviewOpen = ref(false);
const preliminaryPreviewId = ref<number>();
const attachmentPreviewOpen = ref(false);
const attachmentPreviewTarget = ref<SrmPreviewableAttachment>();
const materialPickerOpen = ref(false);
const productBomPickerOpen = ref(false);
const statusAdjustOpen = ref(false);
const statusAdjustSaving = ref(false);
const statusAdjustTarget = ref<null | ResourceRecord>(null);
const statusAdjustForm = reactive({
  reason: '',
  status: '',
});

const isCreating = computed(() => detailMode.value === 'create');
const isReadonly = computed(() => detailMode.value === 'detail');
const detailCanEdit = computed(
  () =>
    Boolean(detailForm.supplierId || detailForm.id) &&
    detailForm.canEdit !== false,
);
const isRegisteredSupplier = computed(() =>
  Boolean(detailForm.supplierId || detailForm.id),
);
const canEditRegistryFields = computed(
  () => !isReadonly.value && (isRegisteredSupplier.value || isCreating.value),
);
const canViewSensitiveResource = computed(() =>
  ['EDIT', 'FULL'].includes(String(detailForm.viewPermission || '')),
);
const shouldMaskSensitiveResource = computed(
  () => !canViewSensitiveResource.value,
);
const canAdjustResourceStatus = computed(() => detailCanEdit.value);
const usingDepartmentOptions = computed(() =>
  getDictOptions('mes_srm_apply_department').map((option) => ({
    label: String(option.label ?? option.value),
    value: String(option.label ?? option.value),
  })),
);
const usingDepartmentValues = computed({
  get: () => splitMultiValue(detailForm.usingDepartment),
  set: (values: string[]) => {
    detailForm.usingDepartment = (values || []).join('、');
  },
});
const statusAdjustOptions = computed(() =>
  statusOptions.filter((option) => option.value !== 'ALL'),
);
const materialPickerInitialFilters = computed(() => ({
  materialCode: detailForm.materialCode || undefined,
  specModel: detailForm.model || undefined,
}));
const productBomPickerInitialFilters = computed(() => ({
  productMaterialKeyword: detailForm.applicableProduct || undefined,
}));

const detailTitle = computed(() => {
  if (detailMode.value === 'create') {
    return '新增供应商资源';
  }
  if (detailMode.value === 'edit') {
    return '编辑供应商资源';
  }
  return '查看供应商资源';
});

const lifecycleRows = computed(() => {
  const rows = [
    {
      key: 'pool',
      node: '资源入池',
      reference: storageText(detailForm),
      remark: '供应商主数据',
      status: storageText(detailForm),
    },
    {
      key: 'survey',
      node: '基本情况调查',
      reference: detailForm.sourceSurveyNo || '-',
      remark: detailForm.sourceSurveyNo ? '已关联调查记录' : '暂无关联调查记录',
      status: detailForm.sourceSurveyNo ? '已关联' : '未关联',
    },
    {
      key: 'state',
      node: '当前资源状态',
      reference: formatDash(detailForm.supplierCode),
      remark: formatDash(detailForm.scopeName || detailForm.scopeCode),
      status: statusText(detailForm),
    },
  ];
  return rows;
});

const businessRows = computed(() => {
  const rows = [
    {
      key: 'supplier',
      bizNo: formatDash(detailForm.supplierCode),
      bizType: '供应商档案',
      remark: formatDash(detailForm.supplierName),
      status: statusText(detailForm),
    },
  ];
  if (detailForm.sourceSurveyNo) {
    rows.push({
      key: 'survey',
      bizNo: detailForm.sourceSurveyNo,
      bizType: '基本情况调查',
      remark: '来源调查表',
      status: '已登记',
    });
  }
  preliminaryEvaluations.value.forEach((item) => {
    rows.push({
      businessId: item.id,
      key: `preliminary-${item.id || item.evaluationNo}`,
      bizNo: item.evaluationNo || '-',
      bizType: '选择初评',
      remark: `${formatDash(item.templateNameSnapshot)} / ${formatDash(
        item.templateVersionSnapshot,
      )}`,
      status: preliminaryStatusText(item.status),
    });
  });
  return rows;
});

const performanceRows = computed(() => [
  {
    key: 'quarterly',
    bizType: '季度绩效评定',
    currentResult: '-',
    latestNo: '-',
    status: '暂无关联记录',
  },
  {
    key: 'annual',
    bizType: '年度绩效评定',
    currentResult: '-',
    latestNo: '-',
    status: '暂无关联记录',
  },
  {
    key: 'corrective',
    bizType: '供方异常与整改',
    currentResult: '-',
    latestNo: '-',
    status: '暂无关联记录',
  },
  {
    key: 'audit',
    bizType: '供方评审计划/记录',
    currentResult: '-',
    latestNo: '-',
    status: '暂无关联记录',
  },
]);

const operationLogRows = computed(() => [
  ...resourceStatusLogs.value.map((item) => ({
    key: `resource-status-${item.id}`,
    action: '调整资源状态',
    operator: item.operatorUserName || '-',
    remark: `${statusValueText(item.fromStatus)} → ${statusValueText(item.toStatus)}；${valueText(
      item.reason,
    )}`,
    time: item.createTime || '-',
  })),
  {
    key: 'permission',
    action: '访问控制',
    operator: '-',
    remark: `${permissionText(detailForm.viewPermission)} · ${
      detailForm.canEdit === false ? '只读' : '按范围权限控制'
    }`,
    time: '-',
  },
]);

const lifecycleColumns = [
  { dataIndex: 'node', title: '阶段', width: 180 },
  { dataIndex: 'status', title: '状态', width: 140 },
  { dataIndex: 'reference', title: '关联编号', width: 220 },
  { dataIndex: 'remark', title: '说明' },
];

const businessColumns = [
  { dataIndex: 'bizType', title: '业务类型', width: 180 },
  { dataIndex: 'bizNo', title: '业务编号', width: 240 },
  { dataIndex: 'status', title: '状态', width: 140 },
  { dataIndex: 'remark', title: '摘要' },
];

const preliminaryEvaluationColumns = [
  { dataIndex: 'evaluationNo', title: '初评单号', width: 220 },
  { dataIndex: 'templateNameSnapshot', title: '评估模板', minWidth: 240 },
  { dataIndex: 'templateVersionSnapshot', title: '模板版本', width: 110 },
  { dataIndex: 'status', title: '当前状态', width: 130 },
  { dataIndex: 'totalScoreDisplay', title: '最终得分', width: 120 },
  { dataIndex: 'finalDecision', title: '最终判定', width: 130 },
  { dataIndex: 'updateTime', title: '更新时间', width: 180 },
  { dataIndex: 'actions', title: '操作', width: 100 },
];

const fileColumns = [
  { dataIndex: 'fileType', title: '档案类型', width: 160 },
  { dataIndex: 'fileName', title: '档案名称', width: 260 },
  { dataIndex: 'fileStatus', title: '状态', width: 120 },
  { dataIndex: 'effectDate', title: '生效日期', width: 120 },
  { dataIndex: 'expiryDate', title: '到期日期', width: 120 },
  { dataIndex: 'daysLeft', title: '剩余天数', width: 110 },
  { dataIndex: 'attachmentName', title: '附件', width: 220 },
  { dataIndex: 'remark', title: '档案摘要', minWidth: 240 },
];

const performanceColumns = [
  { dataIndex: 'bizType', title: '业务类型', width: 220 },
  { dataIndex: 'latestNo', title: '最近单号', width: 220 },
  { dataIndex: 'currentResult', title: '最近结果', width: 160 },
  { dataIndex: 'status', title: '状态' },
];

const operationLogColumns = [
  { dataIndex: 'time', title: '时间', width: 180 },
  { dataIndex: 'operator', title: '操作人', width: 160 },
  { dataIndex: 'action', title: '动作', width: 160 },
  { dataIndex: 'remark', title: '说明' },
];

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入供应商代码' },
        fieldName: 'supplierCode',
        label: '供应商代码',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
        fieldName: 'supplierName',
        label: '供应商名称',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料代码' },
        fieldName: 'materialCode',
        label: '物料代码',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'supplierCode',
        fixed: 'left',
        minWidth: 170,
        slots: { default: 'supplierCode' },
        title: '供应商代码',
      },
      {
        field: 'supplierName',
        minWidth: 240,
        slots: { default: 'supplierName' },
        title: '供应商名称',
      },
      {
        field: 'usingDepartment',
        minWidth: 140,
        slots: { default: 'textCell' },
        title: '使用部门',
      },
      {
        align: 'center',
        field: 'storageStatus',
        minWidth: 130,
        slots: { default: 'storageStatus' },
        title: '入库状态',
      },
      {
        align: 'center',
        field: 'status',
        minWidth: 120,
        slots: { default: 'status' },
        title: '资源状态',
      },
      {
        field: 'scopeName',
        minWidth: 180,
        slots: { default: 'scopeName' },
        title: '名录范围',
      },
      {
        field: 'providedProduct',
        minWidth: 220,
        slots: { default: 'textCell' },
        title: '供应/协作内容',
      },
      {
        field: 'mainProducts',
        minWidth: 200,
        slots: { default: 'textCell' },
        title: '主营产品',
      },
      {
        field: 'materialCode',
        minWidth: 150,
        slots: { default: 'textCell' },
        title: '物料代码',
      },
      {
        field: 'model',
        minWidth: 150,
        slots: { default: 'textCell' },
        title: '型号',
      },
      {
        align: 'center',
        field: 'materialGrade',
        minWidth: 110,
        slots: { default: 'materialGrade' },
        title: '物料等级',
      },
      {
        field: 'applicableProduct',
        minWidth: 180,
        slots: { default: 'textCell' },
        title: '适用产品',
      },
      {
        align: 'center',
        field: 'viewPermission',
        minWidth: 130,
        slots: { default: 'viewPermission' },
        title: '查看权限',
      },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 132,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'candidateKey' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSupplierCandidatePage({
            ...formValues,
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
            status:
              activeResourceStatus.value === 'ALL'
                ? undefined
                : activeResourceStatus.value,
          });
          return {
            list: result.list || [],
            total: result.total || 0,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmSupplierCandidateApi.Candidate>,
});

function resetDetail(record: Partial<ResourceRecord> = {}) {
  Object.keys(detailForm).forEach((key) => delete detailForm[key]);
  Object.assign(detailForm, record);
  if (!detailForm.materialGrade && detailForm.materialCategory) {
    detailForm.materialGrade = detailForm.materialCategory;
  }
}

function openCreate() {
  const defaultScopeId =
    scopeOptions.value.length === 1 ? scopeOptions.value[0]?.value : undefined;
  selectedCandidate.value = null;
  detailLoading.value = false;
  activeTab.value = 'lifecycle';
  resourceDetailOpen.value = true;
  supplierFiles.value = [];
  preliminaryEvaluations.value = [];
  resourceStatusLogs.value = [];
  resetDetail({
    canEdit: true,
    scopeId: defaultScopeId,
    sourceType: 'REGISTERED',
    status: 'PENDING',
    viewPermission: 'EDIT',
  });
  detailMode.value = 'create';
}

async function openDetail(row: ResourceRecord) {
  detailLoading.value = true;
  selectedCandidate.value = row;
  detailMode.value = 'detail';
  activeTab.value = 'lifecycle';
  resourceDetailOpen.value = true;
  try {
    const supplierId = row.supplierId || row.id;
    if (supplierId) {
      const supplier = await getSupplier(Number(supplierId));
      resetDetail({
        ...row,
        ...supplier,
        candidateKey: row.candidateKey,
        sourceType: 'REGISTERED',
        supplierId,
      });
    } else {
      resetDetail(row);
    }
    await Promise.all([
      loadSupplierFiles(),
      loadPreliminaryEvaluations(),
      loadResourceStatusLogs(),
    ]);
  } finally {
    detailLoading.value = false;
  }
}

async function openEdit(row: ResourceRecord) {
  await openDetail(row);
  if (!detailCanEdit.value) {
    message.warning('当前供应商资源不可编辑');
    return;
  }
  detailMode.value = 'edit';
}

function switchToEdit() {
  if (!detailCanEdit.value) {
    message.warning('当前供应商资源不可编辑');
    return;
  }
  detailMode.value = 'edit';
}

function closeDetail() {
  resourceDetailOpen.value = false;
  materialPickerOpen.value = false;
  productBomPickerOpen.value = false;
  statusAdjustOpen.value = false;
  preliminaryPreviewOpen.value = false;
}

function getResourceDetailPopupContainer(triggerNode?: HTMLElement) {
  return (
    resourceDetailPopupHostRef.value ||
    triggerNode?.parentElement ||
    document.body
  );
}

onDeactivated(closeDetail);

async function loadSupplierFiles() {
  supplierFiles.value = [];
  if (!detailForm.supplierCode) {
    return;
  }
  const result = await getSupplierFilePage({
    pageNo: 1,
    pageSize: 50,
    supplierCode: detailForm.supplierCode,
  });
  supplierFiles.value = result.list || [];
}

async function loadPreliminaryEvaluations() {
  preliminaryEvaluations.value = [];
  const supplierCode = String(detailForm.supplierCode || '').trim();
  if (!supplierCode) {
    return;
  }
  const result = await getEvaluationPage({
    pageNo: 1,
    pageSize: 100,
    supplierCode,
  });
  preliminaryEvaluations.value = (result.list || []).filter(
    (item) => String(item.supplierCode || '').trim() === supplierCode,
  );
}

async function loadResourceStatusLogs() {
  resourceStatusLogs.value = [];
  const supplierId = detailForm.supplierId || detailForm.id;
  if (supplierId) {
    resourceStatusLogs.value =
      (await getSupplierResourceStatusLogs({
        supplierId: Number(supplierId),
      })) || [];
  }
}

async function saveSupplier() {
  if (!detailForm.supplierCode?.trim()) {
    message.warning('请填写供应商代码');
    return;
  }
  if (!detailForm.supplierName?.trim()) {
    message.warning('请填写供应商名称');
    return;
  }
  if (!detailForm.status) {
    message.warning('请选择资源状态');
    return;
  }
  saving.value = true;
  try {
    const payload = buildSupplierPayload();
    const savedId = isCreating.value
      ? await createSupplier(payload)
      : payload.id;
    if (isCreating.value) {
      payload.id = Number(savedId);
    } else {
      await updateSupplier(payload);
    }
    message.success(isCreating.value ? '新增成功' : '保存成功');
    const supplier = await getSupplier(Number(payload.id));
    resetDetail({
      ...selectedCandidate.value,
      ...supplier,
      candidateKey:
        selectedCandidate.value?.candidateKey || `REGISTERED:${payload.id}`,
      sourceType: 'REGISTERED',
      supplierId: payload.id,
    });
    detailMode.value = 'detail';
    if (payload.status) {
      activeResourceStatus.value = payload.status;
    }
    await Promise.all([
      loadSupplierFiles(),
      loadPreliminaryEvaluations(),
      loadResourceStatusLogs(),
    ]);
    await gridApi.query();
  } finally {
    saving.value = false;
  }
}

function buildSupplierPayload(): MesSupplierApi.Supplier {
  const payload: MesSupplierApi.Supplier = {
    address: detailForm.address,
    applicableProduct: detailForm.applicableProduct,
    companyNature: detailForm.companyNature,
    contactPerson: detailForm.contactPerson,
    contactPhone: detailForm.contactPhone,
    deliveryMethod: detailForm.deliveryMethod,
    email: detailForm.email,
    establishDate: detailForm.establishDate,
    importDate: detailForm.importDate,
    legalPerson: detailForm.legalPerson,
    level: detailForm.level,
    mainProducts: detailForm.mainProducts,
    materialCategory: detailForm.materialCategory || detailForm.materialGrade,
    materialCode: detailForm.materialCode,
    materialGrade: detailForm.materialGrade || detailForm.materialCategory,
    model: detailForm.model,
    originPlace: detailForm.originPlace,
    originalFactoryInfo: detailForm.originalFactoryInfo,
    paymentTerms: detailForm.paymentTerms,
    providedProduct: detailForm.providedProduct,
    registeredCapital: detailForm.registeredCapital,
    remark: detailForm.remark,
    scopeId: detailForm.scopeId,
    shortName: detailForm.shortName,
    sort: detailForm.sort,
    status: detailForm.status,
    sourceSurveyId: detailForm.sourceSurveyId,
    sourceSurveyNo: detailForm.sourceSurveyNo,
    supplierCode: detailForm.supplierCode?.trim(),
    supplierName: detailForm.supplierName?.trim(),
    usingDepartment: detailForm.usingDepartment,
  };
  const supplierId = detailForm.id || detailForm.supplierId;
  if (supplierId) {
    payload.id = Number(supplierId);
  }
  if (detailForm.version !== undefined && detailForm.version !== null) {
    payload.version = detailForm.version;
  }
  return payload;
}

function getRowDropDownActions(row: ResourceRecord) {
  const statusAction = [
    {
      icon: 'lucide:refresh-cw',
      label: '调整资源状态',
      onClick: openStatusAdjust.bind(null, row),
      type: 'link',
    },
  ];
  if (row.canEdit === false) {
    return [];
  }
  return [
    {
      icon: ACTION_ICON.EDIT,
      label: '编辑资料',
      onClick: openEdit.bind(null, row),
      type: 'link',
    },
    ...statusAction,
  ];
}

function openStatusAdjust(row?: ResourceRecord) {
  const target = row || (detailForm as ResourceRecord);
  if (!target?.supplierId && !target?.id) {
    message.warning('请先选择供应商资源');
    return;
  }
  statusAdjustTarget.value = { ...target };
  statusAdjustForm.status = String(target.status || '');
  statusAdjustForm.reason = '';
  statusAdjustOpen.value = true;
}

async function submitStatusAdjust() {
  const target = statusAdjustTarget.value;
  if (!target?.supplierId && !target?.id) {
    message.warning('请先选择供应商资源');
    return;
  }
  if (!statusAdjustForm.status) {
    message.warning('请选择资源状态');
    return;
  }
  if (!statusAdjustForm.reason.trim()) {
    message.warning('请填写调整原因说明');
    return;
  }
  statusAdjustSaving.value = true;
  try {
    await adjustSupplierResourceStatus({
      reason: statusAdjustForm.reason.trim(),
      sourceType: 'REGISTERED',
      status: statusAdjustForm.status,
      supplierId: target.supplierId || target.id,
    });
    message.success('资源状态已调整');
    statusAdjustOpen.value = false;
    if (
      detailForm.candidateKey &&
      detailForm.candidateKey === target.candidateKey
    ) {
      detailForm.status = statusAdjustForm.status;
      await loadResourceStatusLogs();
    }
    await gridApi.query();
  } finally {
    statusAdjustSaving.value = false;
  }
}

function handleStatusTabChange(status: string) {
  activeResourceStatus.value = status;
  void gridApi.query();
}

function storageText(record: Partial<ResourceRecord>) {
  return record.status === 'QUALIFIED' ? '已入库' : '未入库';
}

function storageColor(record: Partial<ResourceRecord>) {
  return record.status === 'QUALIFIED' ? 'success' : 'warning';
}

function statusMeta(record: Partial<ResourceRecord>) {
  if (record.status === 'PENDING') {
    return { color: 'warning', text: '考察中' };
  }
  if (record.status === 'QUALIFIED') {
    return { color: 'success', text: '合格' };
  }
  if (record.status === 'FROZEN') {
    return { color: 'warning', text: '冻结' };
  }
  if (record.status === 'ELIMINATED') {
    return { color: 'error', text: '淘汰' };
  }
  if (record.status === 'EXITED') {
    return { color: 'default', text: '退出' };
  }
  if (record.status === 'UNQUALIFIED') {
    return { color: 'error', text: '不合格' };
  }
  return { color: 'default', text: statusTextMap[record.status || ''] || '-' };
}

function statusText(record: Partial<ResourceRecord>) {
  return statusMeta(record).text;
}

function permissionText(permission?: string) {
  return permissionTextMap[permission || ''] || '按范围权限控制';
}

function fileStatusText(status?: string) {
  if (status === 'EXPIRED') {
    return '已过期';
  }
  if (status === 'WARNING') {
    return '临期';
  }
  if (status === 'VALID') {
    return '有效';
  }
  return '-';
}

function fileStatusColor(status?: string) {
  if (status === 'EXPIRED') {
    return 'error';
  }
  if (status === 'WARNING') {
    return 'warning';
  }
  if (status === 'VALID') {
    return 'success';
  }
  return 'default';
}

function preliminaryStatusText(status?: string) {
  return preliminaryStatusTextMap[status || ''] || valueText(status);
}

function preliminaryStatusColor(status?: string) {
  return preliminaryStatusColorMap[status || ''] || 'default';
}

function preliminaryDecisionText(decision?: string) {
  return preliminaryDecisionTextMap[decision || ''] || valueText(decision);
}

function preliminaryDecisionColor(decision?: string) {
  return preliminaryDecisionColorMap[decision || ''] || 'default';
}

function openPreliminaryEvaluation(
  row: SrmPreliminaryEvaluationApi.Evaluation,
) {
  openPreliminaryEvaluationById(row.id);
}

function openPreliminaryEvaluationById(id?: number) {
  if (!id) {
    return;
  }
  preliminaryPreviewId.value = id;
  preliminaryPreviewOpen.value = true;
}

function openSupplierFilePreview(record: SrmSupplierFileApi.SupplierFile) {
  const attachment = toSupplierFilePreviewAttachment(record);
  if (!canPreviewSrmAttachment(attachment)) {
    message.warning('当前附件格式暂不支持在线预览');
    return;
  }
  attachmentPreviewTarget.value = attachment;
  attachmentPreviewOpen.value = true;
}

function openSupplierFileAttachment(record: SrmSupplierFileApi.SupplierFile) {
  if (!record.attachmentUrl) {
    return;
  }
  window.open(record.attachmentUrl, '_blank', 'noopener,noreferrer');
}

function canPreviewSupplierFile(record: SrmSupplierFileApi.SupplierFile) {
  return canPreviewSrmAttachment(toSupplierFilePreviewAttachment(record));
}

function toSupplierFilePreviewAttachment(
  record: SrmSupplierFileApi.SupplierFile,
): SrmPreviewableAttachment {
  return {
    fileName: record.attachmentName || record.fileName,
    fileType: resolveSupplierFilePreviewType(record),
    fileUrl: record.attachmentUrl,
  };
}

function resolveSupplierFilePreviewType(
  record: SrmSupplierFileApi.SupplierFile,
) {
  return (
    getFileExtension(record.attachmentName) ||
    getFileExtension(record.attachmentUrl) ||
    getFileExtension(record.fileName) ||
    record.fileType
  );
}

function getFileExtension(value?: string) {
  const cleanValue = String(value || '').split(/[?#]/)[0] || '';
  const index = cleanValue.lastIndexOf('.');
  return index === -1 ? '' : cleanValue.slice(index + 1).toLowerCase();
}

function openMaterialPicker() {
  if (isReadonly.value) {
    return;
  }
  materialPickerOpen.value = true;
}

function handleMaterialPickerClose() {
  materialPickerOpen.value = false;
}

function handleMaterialPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  detailForm.materialCode = firstText(row.materialCode, row.code, option.value);
  detailForm.model = firstText(
    row.specModel,
    row.modelCode,
    row.model,
    detailForm.model,
  );
  materialPickerOpen.value = false;
}

function openProductBomPicker() {
  if (isReadonly.value) {
    return;
  }
  productBomPickerOpen.value = true;
}

function handleProductBomPickerClose() {
  productBomPickerOpen.value = false;
}

function handleProductBomPick(option: PickerOption) {
  const row = (option.raw || {}) as Record<string, any>;
  detailForm.applicableProduct = firstText(
    row.productMaterialName,
    row.bomName,
    row.materialName,
    option.label,
  );
  productBomPickerOpen.value = false;
}

function valueText(value?: boolean | null | number | string) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function statusValueText(value?: string) {
  return statusTextMap[value || ''] || valueText(value);
}

function formatDash(value?: number | string) {
  return valueText(value);
}

function optionLabel(
  options: Array<{ label: string; value: number | string }>,
  value?: number | string,
) {
  return (
    options.find((item) => item.value === value)?.label || valueText(value)
  );
}

function splitMultiValue(value?: string) {
  if (!value) {
    return [];
  }
  return value
    .split(/[、,，;；]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function firstText(...values: Array<number | string | undefined>) {
  const match = values.find(
    (value) => value !== undefined && value !== null && String(value).trim(),
  );
  return match === undefined || match === null ? '' : String(match).trim();
}

onMounted(async () => {
  const scopes = await getSupplierScopeSimpleList();
  scopeOptions.value = (scopes || [])
    .filter((scope) => scope.id)
    .map((scope) => ({
      label: `${scope.scopeName || '-'}（${scope.scopeCode || '-'}）`,
      value: Number(scope.id),
    }));
});
</script>

<template>
  <Page auto-content-height>
    <div class="srm-resource-page">
      <div class="srm-resource-page__header">
        <div>
          <div class="srm-resource-page__title">供应商资源池</div>
        </div>
      </div>
      <div class="srm-resource-page__body">
        <Grid>
          <template #toolbar-tools>
            <div class="srm-resource-toolbar">
              <Button type="primary" @click="openCreate">
                <IconifyIcon icon="lucide:plus" />
                新增供应商资源
              </Button>
              <div class="srm-resource-status-tabs">
                <button
                  v-for="item in statusOptions"
                  :key="item.value"
                  class="srm-resource-status-tab"
                  :class="{
                    'srm-resource-status-tab--active':
                      activeResourceStatus === item.value,
                  }"
                  type="button"
                  @click="handleStatusTabChange(item.value)"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>
          </template>
          <template #supplierCode="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.supplierCode || '-' }}
            </a>
          </template>
          <template #supplierName="{ row }">
            <span class="srm-resource-cell">{{ row.supplierName || '-' }}</span>
          </template>
          <template #storageStatus="{ row }">
            <Tag :color="storageColor(row)" class="srm-resource-nowrap">
              {{ storageText(row) }}
            </Tag>
          </template>
          <template #status="{ row }">
            <Tag :color="statusMeta(row).color" class="srm-resource-nowrap">
              {{ statusMeta(row).text }}
            </Tag>
          </template>
          <template #scopeName="{ row }">
            <span class="srm-resource-cell">
              {{ row.scopeName || row.scopeCode || '-' }}
            </span>
          </template>
          <template #textCell="{ row, column }">
            <span class="srm-resource-cell">
              {{ row[column.field] || '-' }}
            </span>
          </template>
          <template #materialGrade="{ row }">
            <span class="srm-resource-cell">
              {{
                optionLabel(
                  materialGradeOptions,
                  row.materialGrade || row.materialCategory,
                )
              }}
            </span>
          </template>
          <template #viewPermission="{ row }">
            <Tag
              :color="permissionColorMap[row.viewPermission] || 'default'"
              class="srm-resource-nowrap"
            >
              {{ permissionText(row.viewPermission) }}
            </Tag>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.PREVIEW,
                  label: '查看',
                  onClick: openDetail.bind(null, row),
                  type: 'link',
                },
              ]"
              :drop-down-actions="getRowDropDownActions(row)"
            />
          </template>
        </Grid>
      </div>
    </div>

    <AntModal
      v-model:open="resourceDetailOpen"
      :body-style="{ height: '100dvh', overflow: 'hidden', padding: 0 }"
      :closable="false"
      :destroy-on-close="true"
      :footer="null"
      :keyboard="false"
      :mask-closable="false"
      :style="{ paddingBottom: 0, top: 0 }"
      width="100vw"
      wrap-class-name="qms-product-event-detail-modal srm-erp-crud-modal srm-independent-detail-modal"
      :z-index="SRM_RESOURCE_DETAIL_MODAL_Z_INDEX"
      @cancel="closeDetail"
    >
      <div ref="resourceDetailPopupHostRef" class="srm-independent-detail-host">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>
            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">供应商资源池</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{
                    detailForm.supplierCode
                      ? `编号 ${detailForm.supplierCode}`
                      : '-'
                  }}
                </span>
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{ detailTitle }}
                </span>
                <span class="qms-ncr-title-panel__subtitle-item">
                  {{ statusText(detailForm) }}
                </span>
              </div>
            </div>
            <div class="qms-ncr-toolbar__actions">
              <Button
                v-if="isReadonly && canAdjustResourceStatus"
                class="qms-ncr-toolbar-action"
                @click="openStatusAdjust()"
              >
                <IconifyIcon icon="lucide:refresh-cw" />
                调整资源状态
              </Button>
              <Button
                v-if="isReadonly && detailCanEdit"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑资料
              </Button>
              <Button
                v-if="!isReadonly"
                class="qms-ncr-toolbar-action"
                :loading="saving"
                type="primary"
                @click="saveSupplier"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                关闭
              </Button>
            </div>
          </div>
          <div class="detail-content">
            <div class="qms-exception-workbench">
              <div class="qms-exception-form">
                <section class="erp-basic-form">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>基本信息</strong>
                    </div>
                    <Space :size="8">
                      <Tag
                        :color="storageColor(detailForm)"
                        class="srm-resource-nowrap"
                      >
                        {{ storageText(detailForm) }}
                      </Tag>
                      <Tag
                        :color="statusMeta(detailForm).color"
                        class="srm-resource-nowrap"
                      >
                        {{ statusText(detailForm) }}
                      </Tag>
                    </Space>
                  </div>
                  <div class="erp-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商代码</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.supplierCode"
                          placeholder="请输入供应商代码"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.supplierCode) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商名称</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.supplierName"
                          placeholder="请输入供应商名称"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.supplierName) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">使用部门</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="usingDepartmentValues"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="usingDepartmentOptions"
                          allow-clear
                          mode="multiple"
                          placeholder="请选择使用部门"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.usingDepartment) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">资源状态</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="isCreating"
                          v-model:value="detailForm.status"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="statusAdjustOptions"
                          placeholder="请选择资源状态"
                        />
                        <Tag
                          v-else
                          :color="statusMeta(detailForm).color"
                          class="srm-resource-nowrap"
                        >
                          {{ statusText(detailForm) }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">联系电话</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.contactPhone"
                          placeholder="请输入联系电话"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.contactPhone) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评定级别</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="detailForm.level"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="levelOptions"
                          allow-clear
                          placeholder="请选择评定级别"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ optionLabel(levelOptions, detailForm.level) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料等级</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="detailForm.materialGrade"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="materialGradeOptions"
                          allow-clear
                          placeholder="请选择物料等级"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{
                            optionLabel(
                              materialGradeOptions,
                              detailForm.materialGrade ||
                                detailForm.materialCategory,
                            )
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">企业性质</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="detailForm.companyNature"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="companyNatureOptions"
                          allow-clear
                          placeholder="请选择企业性质"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{
                            optionLabel(
                              companyNatureOptions,
                              detailForm.companyNature,
                            )
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">联系人</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.contactPerson"
                          placeholder="请输入联系人"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.contactPerson) }}
                        </span>
                      </div>
                    </div>
                    <div
                      class="erp-form-item erp-form-item--full srm-resource-scope-range"
                    >
                      <label class="erp-form-label erp-form-label--tall">
                        名录范围
                      </label>
                      <div class="erp-form-value">
                        <Select
                          v-if="canEditRegistryFields"
                          v-model:value="detailForm.scopeId"
                          :dropdown-style="{ zIndex: SRM_DROPDOWN_Z_INDEX }"
                          :get-popup-container="getResourceDetailPopupContainer"
                          :options="scopeOptions"
                          allow-clear
                          placeholder="请选择名录范围"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{
                            valueText(
                              detailForm.scopeName || detailForm.scopeCode,
                            )
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        主营产品
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="detailForm.mainProducts"
                          :rows="3"
                          placeholder="请输入主营产品"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ valueText(detailForm.mainProducts) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        供应/协作内容
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="detailForm.providedProduct"
                          :rows="3"
                          placeholder="请输入供应/协作内容"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ valueText(detailForm.providedProduct) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">物料代码</label>
                      <div class="erp-form-value">
                        <Input.Search
                          v-if="!isReadonly"
                          v-model:value="detailForm.materialCode"
                          allow-clear
                          placeholder="可输入物料代码或点击右侧选择"
                          @search="openMaterialPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.materialCode) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">型号</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.model"
                          placeholder="请输入型号"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.model) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">适用产品</label>
                      <div class="erp-form-value">
                        <Input.Search
                          v-if="!isReadonly"
                          v-model:value="detailForm.applicableProduct"
                          allow-clear
                          placeholder="可输入适用产品或点击右侧选择"
                          @search="openProductBomPicker"
                        >
                          <template #enterButton>
                            <Button>
                              <IconifyIcon icon="lucide:search" />
                            </Button>
                          </template>
                        </Input.Search>
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.applicableProduct) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">法定代表人</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.legalPerson"
                          placeholder="请输入法定代表人"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.legalPerson) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">注册资本</label>
                      <div class="erp-form-value">
                        <InputNumber
                          v-if="!isReadonly"
                          v-model:value="detailForm.registeredCapital"
                          class="w-full"
                          :min="0"
                          placeholder="请输入注册资本"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.registeredCapital) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">成立日期</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="!isReadonly"
                          v-model:value="detailForm.establishDate"
                          class="w-full"
                          :get-popup-container="getResourceDetailPopupContainer"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.establishDate) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">导入日期</label>
                      <div class="erp-form-value">
                        <DatePicker
                          v-if="canEditRegistryFields"
                          v-model:value="detailForm.importDate"
                          class="w-full"
                          :get-popup-container="getResourceDetailPopupContainer"
                          value-format="YYYY-MM-DD"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.importDate) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">结算条件</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.paymentTerms"
                          placeholder="请输入结算条件"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.paymentTerms) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">交货方式</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="detailForm.deliveryMethod"
                          placeholder="请输入交货方式"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ valueText(detailForm.deliveryMethod) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        供应商地址
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="detailForm.address"
                          :rows="3"
                          placeholder="请输入供应商地址"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ valueText(detailForm.address) }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        备注
                      </label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="detailForm.remark"
                          :rows="3"
                          placeholder="请输入备注"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ valueText(detailForm.remark) }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <Tabs
                  v-model:active-key="activeTab"
                  class="shipping-detail-tabs srm-resource-detail-tabs"
                >
                  <TabPane key="lifecycle" tab="生命周期记录">
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="lifecycleColumns"
                        :data-source="lifecycleRows"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="key"
                        size="small"
                      />
                    </div>
                  </TabPane>
                  <TabPane key="business" tab="关联业务单据">
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="businessColumns"
                        :data-source="businessRows"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="key"
                        size="small"
                      >
                        <template #bodyCell="{ column, record }">
                          <template v-if="column.dataIndex === 'bizNo'">
                            <a
                              v-if="
                                record.bizType === '选择初评' &&
                                record.businessId
                              "
                              class="srm-crud-link"
                              @click="
                                openPreliminaryEvaluationById(record.businessId)
                              "
                            >
                              {{ record.bizNo || '-' }}
                            </a>
                            <span v-else>{{ valueText(record.bizNo) }}</span>
                          </template>
                          <template v-else>
                            {{ valueText(record[column.dataIndex]) }}
                          </template>
                        </template>
                      </Table>
                    </div>
                  </TabPane>
                  <TabPane
                    key="preliminary"
                    :tab="`选择初评(${preliminaryEvaluations.length})`"
                  >
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="preliminaryEvaluationColumns"
                        :data-source="preliminaryEvaluations"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="id"
                        size="small"
                        :scroll="{ x: 1220 }"
                      >
                        <template #bodyCell="{ column, record }">
                          <template v-if="column.dataIndex === 'evaluationNo'">
                            <a
                              class="srm-crud-link"
                              @click="openPreliminaryEvaluation(record)"
                            >
                              {{ record.evaluationNo || '-' }}
                            </a>
                          </template>
                          <template
                            v-else-if="
                              column.dataIndex === 'templateNameSnapshot'
                            "
                          >
                            <span class="srm-resource-cell">
                              {{ record.templateNameSnapshot || '-' }}
                            </span>
                          </template>
                          <template v-else-if="column.dataIndex === 'status'">
                            <Tag
                              :color="preliminaryStatusColor(record.status)"
                              class="srm-resource-nowrap"
                            >
                              {{ preliminaryStatusText(record.status) }}
                            </Tag>
                          </template>
                          <template
                            v-else-if="column.dataIndex === 'finalDecision'"
                          >
                            <Tag
                              v-if="record.finalDecision"
                              :color="
                                preliminaryDecisionColor(record.finalDecision)
                              "
                              class="srm-resource-nowrap"
                            >
                              {{
                                preliminaryDecisionText(record.finalDecision)
                              }}
                            </Tag>
                            <span v-else>-</span>
                          </template>
                          <template v-else-if="column.dataIndex === 'actions'">
                            <Button
                              v-if="record.id"
                              size="small"
                              type="link"
                              @click="openPreliminaryEvaluation(record)"
                            >
                              查看
                            </Button>
                          </template>
                          <template v-else>
                            {{ valueText(record[column.dataIndex]) }}
                          </template>
                        </template>
                      </Table>
                    </div>
                  </TabPane>
                  <TabPane
                    key="files"
                    :tab="`合规资料(${supplierFiles.length})`"
                  >
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="fileColumns"
                        :data-source="supplierFiles"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="id"
                        size="small"
                        :scroll="{ x: 1280 }"
                      >
                        <template #bodyCell="{ column, record }">
                          <template v-if="column.dataIndex === 'fileStatus'">
                            <Tag
                              :color="fileStatusColor(record.fileStatus)"
                              class="srm-resource-nowrap"
                            >
                              {{ fileStatusText(record.fileStatus) }}
                            </Tag>
                          </template>
                          <template
                            v-else-if="column.dataIndex === 'attachmentName'"
                          >
                            <div
                              v-if="record.attachmentUrl"
                              class="srm-resource-attachment-actions"
                            >
                              <span class="srm-resource-attachment-name">
                                {{ record.attachmentName || '附件' }}
                              </span>
                              <Button
                                v-if="canPreviewSupplierFile(record)"
                                size="small"
                                type="link"
                                @click="openSupplierFilePreview(record)"
                              >
                                预览
                              </Button>
                              <Button
                                size="small"
                                type="link"
                                @click="openSupplierFileAttachment(record)"
                              >
                                打开
                              </Button>
                            </div>
                            <span v-else>{{
                              record.attachmentName || '-'
                            }}</span>
                          </template>
                          <template v-else>
                            {{ valueText(record[column.dataIndex]) }}
                          </template>
                        </template>
                      </Table>
                    </div>
                  </TabPane>
                  <TabPane key="performance" tab="绩效与稽核">
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="performanceColumns"
                        :data-source="performanceRows"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="key"
                        size="small"
                      />
                    </div>
                  </TabPane>
                  <TabPane key="logs" tab="操作日志">
                    <div class="srm-resource-tab-body">
                      <Table
                        :columns="operationLogColumns"
                        :data-source="operationLogRows"
                        :loading="detailLoading"
                        :pagination="false"
                        row-key="key"
                        size="small"
                      />
                    </div>
                  </TabPane>
                </Tabs>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AntModal>

    <PickerModal
      :config="materialPickerConfig"
      :initial-filters="materialPickerInitialFilters"
      :open="materialPickerOpen"
      title="选择物料"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @close="handleMaterialPickerClose"
      @pick="handleMaterialPick"
    />
    <PickerModal
      :config="productBomPickerConfig"
      :initial-filters="productBomPickerInitialFilters"
      :open="productBomPickerOpen"
      title="选择适用产品"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @close="handleProductBomPickerClose"
      @pick="handleProductBomPick"
    />

    <AntModal
      v-model:open="statusAdjustOpen"
      centered
      :confirm-loading="statusAdjustSaving"
      :destroy-on-close="true"
      title="调整资源状态"
      :width="560"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="submitStatusAdjust"
    >
      <div class="srm-resource-status-adjust">
        <div class="srm-resource-status-adjust__current">
          <span>当前状态：</span>
          <Tag
            :color="statusMeta(statusAdjustTarget || {}).color"
            class="srm-resource-nowrap"
          >
            {{ statusText(statusAdjustTarget || {}) }}
          </Tag>
        </div>
        <div class="srm-resource-status-adjust__field">
          <label>调整为</label>
          <Radio.Group
            v-model:value="statusAdjustForm.status"
            button-style="solid"
            class="srm-resource-status-adjust__radio"
            option-type="button"
            :options="statusAdjustOptions"
          />
        </div>
        <div class="srm-resource-status-adjust__field">
          <label>原因说明</label>
          <Input.TextArea
            v-model:value="statusAdjustForm.reason"
            :rows="4"
            placeholder="请填写资源状态调整原因"
          />
        </div>
      </div>
    </AntModal>

    <AntModal
      v-model:open="preliminaryPreviewOpen"
      centered
      class="srm-resource-pre-eval-modal"
      :destroy-on-close="true"
      :footer="null"
      title="选择初评详情"
      width="calc(100vw - 96px)"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <div class="srm-resource-pre-eval-modal__body">
        <PreliminaryEvaluationInlineDetail
          :hide-scoring-owner="true"
          :id="preliminaryPreviewId"
          :mask-sensitive-fields="shouldMaskSensitiveResource"
          :show-flow-log="canViewSensitiveResource"
        />
      </div>
    </AntModal>
    <SrmAttachmentPreviewModal
      v-model:open="attachmentPreviewOpen"
      :file-name="attachmentPreviewTarget?.fileName"
      :file-type="attachmentPreviewTarget?.fileType"
      :file-url="attachmentPreviewTarget?.fileUrl"
      :z-index="SRM_ATTACHMENT_PREVIEW_MODAL_Z_INDEX"
    />
  </Page>
</template>

<style scoped>
.srm-resource-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.srm-resource-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-resource-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-resource-page__body {
  display: flex;
  flex: 1 1 0%;
  min-height: 0;
  flex-direction: column;
}

.srm-resource-toolbar {
  display: flex;
  max-width: 100%;
  align-items: center;
  gap: 12px;
  overflow-x: auto;
}

.srm-resource-status-tabs {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  background: transparent;
  padding: 0;
  white-space: nowrap;
}

.srm-resource-status-tab {
  height: 30px;
  min-width: 68px;
  border: 1px solid #d6e4ff;
  border-radius: 999px;
  padding: 0 16px;
  color: #1d4ed8;
  background: #f8fbff;
  font-size: 13px;
  font-weight: 700;
  line-height: 28px;
  white-space: nowrap;
}

.srm-resource-status-tab--active {
  border-color: #1677ff;
  color: #fff;
  background: #1677ff;
  box-shadow: 0 6px 14px rgba(22, 119, 255, 0.2);
}

.srm-crud-link {
  color: #1d4ed8;
  cursor: pointer;
  font-weight: 700;
}

.srm-crud-link:hover {
  text-decoration: underline;
}

.srm-resource-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-resource-nowrap {
  white-space: nowrap;
}

.srm-resource-attachment-actions {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.srm-resource-attachment-name {
  min-width: 0;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
}

.srm-resource-tab-body {
  min-height: 0;
  flex: 1 1 auto;
  overflow: auto;
}

.srm-resource-scope-range {
  min-height: 72px;
}

.srm-resource-status-adjust {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.srm-resource-status-adjust__current {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-size: 14px;
}

.srm-resource-status-adjust__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.srm-resource-status-adjust__field > label {
  color: #334155;
  font-weight: 700;
}

.srm-resource-status-adjust__radio {
  display: inline-flex;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.srm-resource-pre-eval-modal__body {
  min-height: 520px;
  max-height: calc(100vh - 180px);
  overflow: auto;
  padding: 2px;
}

.srm-resource-pre-eval-modal :deep(.ant-modal-body) {
  background: #f8fafc;
}

.shipping-detail-tabs {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  padding: 0 8px 8px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-nav) {
  flex: 0 0 auto;
  margin: 0 0 6px;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-tab-btn) {
  white-space: nowrap;
}

.shipping-detail-tabs :deep(.ant-tabs-content-holder),
.shipping-detail-tabs :deep(.ant-tabs-content),
.shipping-detail-tabs :deep(.ant-tabs-tabpane) {
  display: flex;
  min-height: 0;
  flex: 1 1 auto;
  background: #fff;
}

.shipping-detail-tabs :deep(.ant-tabs-content) {
  height: 100%;
}

.shipping-detail-tabs :deep(.ant-tabs-tabpane-active) {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

:deep(.ant-table-cell) {
  vertical-align: middle;
}

:deep(.ant-table-cell .ant-tag),
:deep(.vxe-cell .ant-tag) {
  margin-right: 0;
}
</style>
