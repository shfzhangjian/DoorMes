<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmPreliminaryEvaluationApi } from '#/api/mes/srm/preliminary-evaluation';
import type { SrmEvaluationTemplateApi } from '#/api/mes/srm/standard/template';
import type { SystemUserApi } from '#/api/system/user';
import type {
  BusinessLogItem,
  BusinessLogMode,
} from '#/components/business-log';

import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useUserStore } from '@vben/stores';

import {
  Modal as AntModal,
  Button,
  Checkbox,
  Dropdown,
  Form,
  Input,
  InputNumber,
  Menu,
  message,
  Radio,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  assignScorers,
  calculateEvaluation,
  createEvaluation,
  deleteEvaluation,
  getEvaluation,
  getEvaluationPage,
  publishEvaluation,
  sendEvaluation,
  submitDecision,
  submitGeneralManagerOpinion,
  submitScore,
  updateEvaluation,
} from '#/api/mes/srm/preliminary-evaluation';
import { getProjectScorerConfig } from '#/api/mes/srm/preliminary-project';
import { getTemplateDetail } from '#/api/mes/srm/standard/template';
import { getSimpleRoleList } from '#/api/system/role';
import { getUserSelectPage } from '#/api/system/user';
import { getUserProfile } from '#/api/system/user/profile';
import { BusinessLogDrawer } from '#/components/business-log';
import BpmProcessAuditModal from '#/views/bpm/processInstance/detail/modules/process-audit-modal.vue';
import { UserSelectModal } from '#/views/system/user/components';

import SrmAttachmentPanel from '../../shared/SrmAttachmentPanel.vue';
import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmPreliminaryEvaluationManagement' });

const props = defineProps<{ id?: number | string }>();
const emit = defineEmits<{ close: [] }>();

type DetailMode = 'create' | 'detail' | 'edit';
type UserSelectPurpose = 'cc' | 'scorer';
type EvaluationForm = SrmPreliminaryEvaluationApi.Evaluation & {
  items: SrmPreliminaryEvaluationApi.Item[];
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const route = useRoute();
const router = useRouter();
const openedEntryKey = ref('');
const selectedScorerItem = ref<SrmPreliminaryEvaluationApi.Item>();
const selectedAttachmentItem = ref<SrmPreliminaryEvaluationApi.Item>();
const attachmentOpen = ref(false);
const logDrawerVisible = ref(false);
const logDrawerMode = ref<BusinessLogMode>('operation');
const userSelectPurpose = ref<UserSelectPurpose>('scorer');
const generalManagerUsers = ref<SystemUserApi.User[]>([]);
const decisionOpen = ref(false);
const gmOpinionOpen = ref(false);
const finalDecision = ref<SrmPreliminaryEvaluationApi.Decision>('QUALIFIED');
const finalDescription = ref('');
const finalTotalScore = ref<null | number>();
const sendGeneralManager = ref(false);
const selectedGeneralManagerId = ref<number>();
const generalManagerOpinion = ref('');

const form = reactive<EvaluationForm>(createEmptyEvaluation());
const projectFilter = reactive<{
  projectCode?: string;
  projectId?: number;
  projectName?: string;
}>({});
const userStore = useUserStore();
const currentUserProfile = ref<Record<string, any>>();

const isNew = computed(() => !form.id);
const isReadonly = computed(() => detailMode.value === 'detail');
const canEditBasic = computed(
  () =>
    !isReadonly.value &&
    (isNew.value || form.canMaintain === true || detailMode.value === 'create'),
);
const canEditScorer = computed(
  () =>
    !isReadonly.value &&
    (isNew.value || form.canMaintain === true || detailMode.value === 'create'),
);
const isScoringHandleMode = computed(() => form.canScore === true);
const isEmbeddedBusinessForm = computed(() => !!normalizeId(props.id));
const isRouteEntryMode = computed(
  () =>
    !!firstText(
      route.query.id,
      route.query.evaluationId,
      route.query.businessKey,
      route.query.bizId,
      route.query.evaluationNo,
    ),
);
const myScoreItems = computed(() =>
  (form.items || []).filter((item) => item.currentUserItem),
);
const businessLogs = computed<BusinessLogItem[]>(() =>
  (form.logs || []).map((log) => ({
    actionCode: log.action,
    actionName: flowActionText(log.action),
    fromNodeName: statusText(log.fromStatus),
    fromStatus: log.fromStatus,
    handleTime: log.createTime,
    handlerUserName: log.operatorName,
    id: log.id,
    opinion: log.actionDescription,
    toNodeName: statusText(log.toStatus),
    toStatus: log.toStatus,
  })),
);
const subtitleItems = computed(() =>
  [
    form.evaluationNo ? `单号 ${form.evaluationNo}` : '',
    form.projectName ? `项目 ${form.projectName}` : '',
    form.supplierName ? `供应商 ${form.supplierName}` : '',
    form.status ? statusText(form.status) : '',
    detailModeText(detailMode.value),
  ].filter(Boolean),
);
const businessActions = computed(() =>
  [
    {
      icon: 'lucide:send',
      key: 'send',
      label: '确认发送',
      show:
        form.canMaintain === true && form.status === 'DRAFT' && !isNew.value,
    },
    {
      icon: 'lucide:clipboard-check',
      key: 'score',
      label: '提交本人评分',
      show: form.canScore === true,
    },
    {
      icon: 'lucide:calculator',
      key: 'calculate',
      label: '计算得分',
      show: form.canCalculate === true,
    },
    {
      icon: 'lucide:file-check-2',
      key: 'decision',
      label: '填写最终判定',
      show:
        form.status === 'PENDING_DECISION' &&
        ['ADMIN', 'INITIATOR'].includes(form.viewerScope || ''),
    },
    {
      icon: 'lucide:message-square-text',
      key: 'gm',
      label: '填写总经理意见',
      show: form.canHandleGeneralManager === true,
    },
    {
      icon: 'lucide:send-horizontal',
      key: 'publish',
      label: '选择抄送人并发布',
      show: form.canPublish === true,
    },
  ].filter((item) => item.show),
);

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入初评单号' },
    fieldName: 'evaluationNo',
    label: '初评单号',
  },
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
    fieldName: 'supplierName',
    label: '供应商名称',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: [
        { label: '草稿', value: 'DRAFT' },
        { label: '评分中', value: 'SCORING' },
        { label: '待计算得分', value: 'PENDING_CALCULATION' },
        { label: '待最终判定', value: 'PENDING_DECISION' },
        { label: '总经理办理', value: 'GM_REVIEW' },
        { label: '待发布', value: 'PENDING_PUBLISH' },
        { label: '已发布', value: 'PUBLISHED' },
      ],
      placeholder: '请选择状态',
    },
    fieldName: 'status',
    label: '当前状态',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: [
        { label: '是', value: true },
        { label: '否', value: false },
      ],
      placeholder: '只看待我评分',
    },
    fieldName: 'todoOnly',
    label: '待办范围',
  },
];

const gridColumns: VxeTableGridOptions['columns'] = [
  { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
  {
    field: 'evaluationNo',
    fixed: 'left',
    minWidth: 190,
    slots: { default: 'evaluationNo' },
    title: '初评单号',
  },
  {
    field: 'projectName',
    minWidth: 220,
    slots: { default: 'projectName' },
    title: '项目',
  },
  { field: 'supplierCode', minWidth: 170, title: '供应商编号' },
  {
    field: 'supplierName',
    minWidth: 230,
    slots: { default: 'supplierName' },
    title: '供应商名称',
  },
  {
    field: 'templateNameSnapshot',
    minWidth: 260,
    slots: { default: 'templateNameSnapshot' },
    title: '评估模板/版本',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 130,
    slots: { default: 'status' },
    title: '当前状态',
  },
  {
    align: 'center',
    field: 'totalScoreDisplay',
    minWidth: 100,
    slots: { default: 'totalScoreDisplay' },
    title: '总分',
  },
  {
    align: 'center',
    field: 'finalDecision',
    minWidth: 110,
    slots: { default: 'finalDecision' },
    title: '最终判定',
  },
  { align: 'center', field: 'initiatorName', minWidth: 110, title: '发起人' },
  { align: 'center', field: 'updateTime', minWidth: 170, title: '更新时间' },
  {
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 126,
  },
];

const allItemColumns = [
  {
    align: 'center',
    dataIndex: 'seq',
    fixed: 'left',
    title: '序号',
    width: 70,
  },
  { dataIndex: 'groupNameSnapshot', fixed: 'left', title: '维度', width: 120 },
  {
    dataIndex: 'indicatorNameSnapshot',
    fixed: 'left',
    title: '指标',
    width: 220,
  },
  { dataIndex: 'scoringRuleSnapshot', title: '评分规则', width: 330 },
  { align: 'center', dataIndex: 'maxScoreSnapshot', title: '满分', width: 80 },
  { dataIndex: 'veto', title: '否决线', width: 120 },
  { dataIndex: 'defaultDeptNamesSnapshot', title: '评分部门', width: 140 },
  { dataIndex: 'scorerUserNameDisplay', title: '评分人', width: 210 },
  { dataIndex: 'actualScoreDisplay', title: '实际得分', width: 120 },
  { dataIndex: 'scoringDescriptionDisplay', title: '评分说明', width: 300 },
  { dataIndex: 'actualScoreTime', title: '实际评分时间', width: 170 },
  {
    align: 'center',
    dataIndex: 'attachment',
    fixed: 'right',
    title: '附件',
    width: 90,
  },
  {
    align: 'center',
    dataIndex: 'actions',
    fixed: 'right',
    title: '操作',
    width: 82,
  },
];
const scoringHiddenColumns = new Set([
  'actualScoreTime',
  'defaultDeptNamesSnapshot',
  'scorerUserNameDisplay',
  'veto',
]);
const itemColumns = computed(() =>
  allItemColumns.filter(
    (column) =>
      !isScoringHandleMode.value ||
      !scoringHiddenColumns.has(String(column.dataIndex)),
  ),
);
const itemTableScrollX = computed(() =>
  itemColumns.value.reduce(
    (sum, column) => sum + Number(column.width || 120),
    0,
  ),
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: gridFormSchema,
  },
  gridOptions: {
    columns: gridColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getEvaluationPage({
            ...formValues,
            ...(projectFilter.projectId
              ? { projectId: projectFilter.projectId }
              : {}),
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
          });
          return {
            list: result.list || [],
            total: Number(result.total || 0),
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmPreliminaryEvaluationApi.Evaluation>,
});

const [DetailModal, detailModalApi] = useVbenModal({
  class: 'qms-product-event-detail-modal srm-erp-crud-modal',
  closeOnClickModal: false,
  closable: false,
  contentClass: '!p-0 overflow-hidden',
  footer: false,
  fullscreen: true,
  fullscreenButton: false,
  header: false,
  showCancelButton: false,
  showConfirmButton: false,
});

const [ProcessAuditModal, processAuditModalApi] = useVbenModal({
  connectedComponent: BpmProcessAuditModal,
  destroyOnClose: true,
});

const [SupplierModal, supplierModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [ProjectModal, projectModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [ProjectFilterModal, projectFilterModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [TemplateModal, templateModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function createEmptyEvaluation(): EvaluationForm {
  return {
    evaluationNo: buildNo(),
    items: [],
    projectCode: '',
    projectName: '',
    status: 'DRAFT',
    supplierCode: '',
    supplierName: '',
    templateVersionId: 0,
  };
}

function resetForm(record: Partial<EvaluationForm> = {}) {
  const items = normalizeScorerCandidates(
    (record.items || []).map((item) => ({ ...item })),
  );
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyEvaluation(), record, {
    items,
  });
}

async function openCreate() {
  resetForm(await getCreateDefaults());
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmPreliminaryEvaluationApi.Evaluation) {
  await openDetailById(row.id!);
}

async function openDetailById(id: number) {
  const result = await getEvaluation(id);
  resetForm({ ...result, items: result.items || [] });
  detailMode.value = 'detail';
  basicExpanded.value = false;
  detailModalApi.open();
}

function switchToEdit() {
  detailMode.value = 'edit';
  basicExpanded.value = true;
}

async function getCreateDefaults() {
  const user = await getCurrentUser();
  return {
    initiatorId: user.userId,
    initiatorName: user.userName,
    status: 'DRAFT',
  };
}

async function getCurrentUser() {
  const storeUser = (userStore.userInfo || {}) as Record<string, any>;
  let profile = currentUserProfile.value;
  if (!profile) {
    try {
      profile = (await getUserProfile()) as Record<string, any>;
      currentUserProfile.value = profile;
    } catch {
      profile = {};
    }
  }
  const mergedUser = { ...profile, ...storeUser };
  return {
    userId: normalizeId(mergedUser.id || mergedUser.userId),
    userName:
      firstText(
        mergedUser.nickname,
        mergedUser.realName,
        mergedUser.empName,
        mergedUser.username,
      ) || '',
  };
}

async function closeDetail() {
  logDrawerVisible.value = false;
  await detailModalApi.close();
  if (isEmbeddedBusinessForm.value) {
    emit('close');
    return;
  }
  if (isRouteEntryMode.value) {
    router.back();
  }
}

function openLogDrawer(mode: BusinessLogMode) {
  logDrawerMode.value = mode;
  logDrawerVisible.value = true;
}

function openAuditLog() {
  if (!form.processInstanceId) {
    message.warning('当前单据未发起审批流程，暂无审批日志');
    return;
  }
  processAuditModalApi
    .setData({
      processInstanceId: form.processInstanceId,
      title: `审批日志 - ${form.evaluationNo || '选择初评'}`,
    })
    .open();
}

function openProjectPicker() {
  if (!canEditBasic.value) {
    return;
  }
  projectModalApi
    .setData({
      keyword: form.projectName || form.projectCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'project',
      title: '选择初评项目',
    })
    .open();
}

async function handleProjectSelected(row: Record<string, any>) {
  if (!canEditBasic.value) {
    return;
  }
  Object.assign(form, {
    projectCode: row.projectCode,
    projectId: normalizeId(row.id || row.projectId),
    projectName: row.projectName,
  });
  if (form.templateVersionId) {
    await reloadTemplateItemsByProject();
    message.success('已按项目配置刷新评分人');
  }
}

function openProjectFilterPicker() {
  projectFilterModalApi
    .setData({
      keyword: projectFilter.projectName || projectFilter.projectCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'project',
      title: '按项目筛选选择初评',
    })
    .open();
}

async function handleProjectFilterSelected(row: Record<string, any>) {
  Object.assign(projectFilter, {
    projectCode: row.projectCode,
    projectId: normalizeId(row.id || row.projectId),
    projectName: row.projectName,
  });
  await gridApi.query();
}

async function clearProjectFilter() {
  Object.assign(projectFilter, {
    projectCode: undefined,
    projectId: undefined,
    projectName: undefined,
  });
  await gridApi.query();
}

function openSupplierPicker() {
  if (!canEditBasic.value) {
    return;
  }
  supplierModalApi
    .setData({
      keyword: form.supplierName || form.supplierCode,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'supplier',
      supplierSource: 'candidate',
      title: '选择供应商',
    })
    .open();
}

function handleSupplierSelected(row: Record<string, any>) {
  Object.assign(form, {
    supplierCode: row.supplierCode,
    supplierId: row.supplierId,
    supplierName: row.supplierName,
    supplierSourceType: 'REGISTERED',
  });
}

function openTemplatePicker() {
  if (!canEditBasic.value) {
    return;
  }
  if (!form.projectId) {
    message.warning('请先选择初评项目');
    return;
  }
  templateModalApi
    .setData({
      keyword: form.templateNameSnapshot || form.templateCodeSnapshot,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      referenceType: 'template',
      title: '选择评估模板',
    })
    .open();
}

async function handleTemplateSelected(row: Record<string, any>) {
  if (!canEditBasic.value) {
    return;
  }
  if (isTemplateUpgradeUnpublished(row)) {
    message.warning('该评估模板正在升级审核中，发布前不能选择');
    return;
  }
  const templateId = Number(row.id || row.templateId);
  if (!templateId) {
    message.warning('请选择有效的评估模板');
    return;
  }
  const versionId = Number(row.currentVersionId || row.currentVersion?.id || 0);
  const template = await getTemplateDetail(templateId, versionId || undefined);
  const version = template?.currentVersion;
  if (!template || !version?.id) {
    message.warning('所选模板没有可用版本');
    return;
  }
  if (template.sceneType !== 'PRELIMINARY') {
    message.warning('请选择适用场景为“选择初评”的模板');
    return;
  }
  if (version.status !== 'PUBLISHED') {
    message.warning('请选择已发布版本的评估模板');
    return;
  }
  await applyTemplateToForm(template);
}

async function reloadTemplateItemsByProject() {
  const templateId = normalizeId(form.templateId);
  const templateVersionId = normalizeId(form.templateVersionId);
  if (!templateId || !templateVersionId) {
    return;
  }
  const template = await getTemplateDetail(templateId, templateVersionId);
  await applyTemplateToForm(template);
}

async function applyTemplateToForm(
  template: SrmEvaluationTemplateApi.Template,
) {
  const version = template?.currentVersion;
  if (!template || !version?.id) {
    return;
  }
  const items = normalizeScorerCandidates(
    await buildEvaluationItemsFromTemplate(version),
  );
  Object.assign(form, {
    items,
    qualificationScoreSnapshot: version.qualificationScore,
    templateCodeSnapshot: template.templateCode,
    templateId: template.id,
    templateNameSnapshot: template.templateName,
    templateVersionId: version.id,
    templateVersionSnapshot: version.versionNo,
    totalScoreBaseline: version.totalScore,
  });
}

async function buildEvaluationItemsFromTemplate(
  version: SrmEvaluationTemplateApi.Version,
): Promise<SrmPreliminaryEvaluationApi.Item[]> {
  const projectScorerMap = await loadProjectScorerMap(version.id);
  return (version.items || []).map((item, index) => {
    const projectScorer = projectScorerMap.get(Number(item.id || 0));
    return {
      actualScoreDisplay: '-',
      evaluationId: 0,
      defaultDeptNamesSnapshot:
        projectScorer?.defaultDeptNames || item.defaultDeptNames,
      groupCodeSnapshot: item.groupCode,
      groupMaxScoreSnapshot: item.groupMaxScore,
      groupNameSnapshot: item.groupName,
      groupSort: item.groupSort,
      id: -(index + 1),
      indicatorCodeSnapshot: item.indicatorCode,
      indicatorNameSnapshot: item.indicatorName,
      indicatorSort: item.indicatorSort,
      maxScoreSnapshot: item.maxScore,
      scoreStatus: 'PENDING' as const,
      templateItemId: item.id,
      scoringRuleSnapshot: item.scoringRule,
      vetoOperatorSnapshot: item.vetoOperator,
      vetoScoreSnapshot: item.vetoScore,
      ...buildTemplateScorerFields({
        defaultScorerUserId:
          projectScorer?.scorerUserId ?? item.defaultScorerUserId,
        defaultScorerUserIds:
          projectScorer?.scorerCandidateUserIds ?? item.defaultScorerUserIds,
        defaultScorerUserName:
          projectScorer?.scorerUserName ?? item.defaultScorerUserName,
        defaultScorerUserNames:
          projectScorer?.scorerCandidateUserNames ??
          item.defaultScorerUserNames,
      }),
    };
  });
}

async function loadProjectScorerMap(templateVersionId?: number) {
  const projectId = normalizeId(form.projectId);
  const versionId = normalizeId(templateVersionId);
  if (!projectId || !versionId) {
    return new Map<number, Record<string, any>>();
  }
  const projectConfig = await getProjectScorerConfig(projectId, versionId);
  return new Map(
    (projectConfig.scorerItems || [])
      .filter((item) => normalizeId(item.templateItemId))
      .map((item) => [
        Number(item.templateItemId),
        item as Record<string, any>,
      ]),
  );
}

async function saveDraft(closeAfter = false) {
  if (
    !form.evaluationNo ||
    !form.projectId ||
    !form.supplierCode ||
    !form.supplierName ||
    !form.templateVersionId ||
    form.totalScoreBaseline === null ||
    form.totalScoreBaseline === undefined ||
    form.qualificationScoreSnapshot === null ||
    form.qualificationScoreSnapshot === undefined
  ) {
    message.warning('请填写初评单号、项目、供应商、评估模板、满分和合格线');
    return;
  }
  if (
    Number(form.qualificationScoreSnapshot) > Number(form.totalScoreBaseline)
  ) {
    message.warning('合格线不能高于满分');
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateEvaluation(form);
      message.success('保存成功');
      await refreshDetail();
    } else {
      const id = await createEvaluation(form);
      const result = await getEvaluation(id);
      resetForm({ ...result, items: result.items || [] });
      detailMode.value = 'edit';
      message.success('草稿已创建');
    }
    await gridApi.query();
    if (closeAfter) {
      await closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

function chooseScorer(item: SrmPreliminaryEvaluationApi.Item) {
  if (!canEditScorer.value) {
    return;
  }
  selectedScorerItem.value = item;
  userSelectPurpose.value = 'scorer';
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: getScorerCandidateIds(item),
    })
    .open();
}

async function handleUserConfirm(users: SystemUserApi.User[]) {
  const validUsers = users.filter(
    (user): user is SystemUserApi.User & { id: number } =>
      normalizeId(user.id) !== undefined,
  );
  if (userSelectPurpose.value === 'cc') {
    if (!form.id || validUsers.length === 0) {
      return;
    }
    await publishEvaluation(
      form.id,
      validUsers.map((user) => user.id),
    );
    message.success('评估表已发布并完成抄送');
    await refreshDetail();
    await gridApi.query();
    return;
  }

  const item = selectedScorerItem.value;
  if (!item?.id || validUsers.length === 0) {
    return;
  }
  const currentScorerId = normalizeId(item.scorerUserId);
  const scorerUserId =
    currentScorerId &&
    validUsers.some((candidate) => candidate.id === currentScorerId)
      ? currentScorerId
      : validUsers[0].id;
  const mergedCandidateIds = mergeIds(
    getScorerCandidateIds(item),
    validUsers.map((candidate) => Number(candidate.id)),
  );
  await saveScorerAssignment(
    item,
    scorerUserId,
    mergedCandidateIds,
    validUsers,
  );
  selectedScorerItem.value = undefined;
}

async function saveScorerAssignment(
  item: SrmPreliminaryEvaluationApi.Item,
  scorerUserId: number,
  candidateUserIds = getScorerCandidateIds(item),
  selectedUsers: Array<SystemUserApi.User & { id: number }> = [],
) {
  if (!canEditScorer.value) {
    return;
  }
  const normalizedScorerUserId = normalizeId(scorerUserId);
  if (!item.id || !normalizedScorerUserId) {
    return;
  }
  const normalizedCandidateIds = mergeIds(candidateUserIds, [
    normalizedScorerUserId,
  ]);
  if (!form.id || item.id < 0) {
    applyLocalScorerAssignment(
      item,
      normalizedScorerUserId,
      normalizedCandidateIds,
      selectedUsers,
    );
    message.success('评分人已更新');
    return;
  }
  await assignScorers(form.id, [
    {
      itemId: item.id,
      scorerCandidateUserIds: normalizedCandidateIds,
      scorerUserId: normalizedScorerUserId,
    },
  ]);
  await refreshDetail();
  await gridApi.query();
  message.success('评分人已更新');
}

async function confirmSend() {
  if (!form.id) {
    message.warning('请先保存草稿');
    return;
  }
  if ((form.items || []).some((item) => !item.scorerUserId)) {
    message.warning('请先为所有指标分配评分人');
    return;
  }
  AntModal.confirm({
    content: '确认后将启动流程，并给所有评分人生成并行待办。',
    okText: '确认发送',
    async onOk() {
      await sendEvaluation(form.id!);
      message.success('已发送评分');
      await refreshDetail();
      await gridApi.query();
    },
    title: '确认发送评分',
    zIndex: SRM_NESTED_MODAL_Z_INDEX,
  });
}

async function submitMyScore() {
  if (!form.id || myScoreItems.value.length === 0) {
    return;
  }
  const invalid = myScoreItems.value.find(
    (item) => item.actualScore === undefined || item.actualScore === null,
  );
  if (invalid) {
    message.warning(`请填写“${invalid.indicatorNameSnapshot}”的实际得分`);
    return;
  }
  const scoreItems = myScoreItems.value.map((item) => ({
    actualScore: Number(item.actualScore),
    itemId: item.id,
    scoringDescription: String(item.scoringDescription || ''),
  }));
  AntModal.confirm({
    content: '提交后将完成本人评分待办，并进入后续流程流转。确认提交评分吗？',
    okText: '确认提交',
    async onOk() {
      await submitScore(form.id!, scoreItems);
      message.success('本人评分已提交');
      await refreshDetail();
      await gridApi.query();
    },
    title: '确认提交评分',
    zIndex: SRM_NESTED_MODAL_Z_INDEX,
  });
}

async function calculate() {
  if (!form.id) {
    return;
  }
  await calculateEvaluation(form.id);
  message.success('自动计算完成，可在最终判定中修正最终得分');
  await refreshDetail();
  await gridApi.query();
}

async function openDecision() {
  finalDecision.value = form.autoDecision || 'QUALIFIED';
  finalDescription.value = form.finalDescription || '';
  finalTotalScore.value = normalizeScoreNumber(
    form.totalScore,
    form.totalScoreDisplay,
  );
  sendGeneralManager.value = false;
  selectedGeneralManagerId.value = undefined;
  await loadGeneralManagerUsers();
  decisionOpen.value = true;
}

async function loadGeneralManagerUsers() {
  const roles = await getSimpleRoleList();
  const role = roles.find((item) => item.code === 'srm_general_manager');
  if (!role?.id) {
    generalManagerUsers.value = [];
    return;
  }
  const result = await getUserSelectPage({
    pageNo: 1,
    pageSize: 200,
    roleId: role.id,
  });
  generalManagerUsers.value = result.list || [];
}

async function confirmDecision() {
  if (!form.id) {
    return;
  }
  const normalizedTotalScore = normalizeScoreNumber(finalTotalScore.value);
  if (normalizedTotalScore === undefined) {
    message.warning('请填写最终得分');
    return;
  }
  if (
    form.totalScoreBaseline !== null &&
    form.totalScoreBaseline !== undefined &&
    normalizedTotalScore > Number(form.totalScoreBaseline)
  ) {
    message.warning('最终得分不能高于满分');
    return;
  }
  if (!finalDescription.value.trim()) {
    message.warning('请填写最终导入判定说明');
    return;
  }
  if (sendGeneralManager.value && !selectedGeneralManagerId.value) {
    message.warning('请选择总经理角色办理人');
    return;
  }
  await submitDecision({
    evaluationId: form.id,
    finalDecision: finalDecision.value,
    finalDescription: finalDescription.value.trim(),
    generalManagerUserId: sendGeneralManager.value
      ? selectedGeneralManagerId.value
      : undefined,
    totalScore: normalizedTotalScore,
  });
  decisionOpen.value = false;
  message.success(
    sendGeneralManager.value ? '已发送总经理办理' : '已进入待发布',
  );
  await refreshDetail();
  await gridApi.query();
}

function openGeneralManagerOpinion() {
  generalManagerOpinion.value = form.generalManagerOpinion || '';
  gmOpinionOpen.value = true;
}

async function confirmGeneralManagerOpinion() {
  if (!form.id) {
    return;
  }
  if (!generalManagerOpinion.value.trim()) {
    message.warning('请填写总经理意见');
    return;
  }
  await submitGeneralManagerOpinion(
    form.id,
    generalManagerOpinion.value.trim(),
  );
  gmOpinionOpen.value = false;
  message.success('总经理意见已提交，已返回发起人');
  await refreshDetail();
  await gridApi.query();
}

function chooseCcAndPublish() {
  userSelectPurpose.value = 'cc';
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: form.ccUsers?.map((item) => item.userId) || [],
    })
    .open();
}

async function refreshDetail() {
  if (!form.id) {
    return;
  }
  const result = await getEvaluation(form.id);
  resetForm({ ...result, items: result.items || [] });
}

async function openEntryDetail() {
  const rawBusinessKey = firstText(
    props.id,
    route.query.id,
    route.query.evaluationId,
    route.query.businessKey,
    route.query.bizId,
  );
  const evaluationId = normalizeId(rawBusinessKey);
  const evaluationNo = firstText(
    route.query.evaluationNo,
    evaluationId ? undefined : rawBusinessKey,
  );
  const entryKey = buildEntryKey(evaluationId, evaluationNo);
  if (!entryKey || openedEntryKey.value === entryKey) {
    return;
  }
  openedEntryKey.value = entryKey;
  if (evaluationId) {
    await openDetailById(evaluationId);
    return;
  }
  const page = await getEvaluationPage({
    evaluationNo,
    pageNo: 1,
    pageSize: 1,
  });
  const row = page.list?.[0];
  if (row?.id) {
    await openDetailById(row.id);
    return;
  }
  await gridApi.formApi.setValues({ evaluationNo });
  await gridApi.query();
}

function openAttachment(item: SrmPreliminaryEvaluationApi.Item) {
  selectedAttachmentItem.value = item;
  attachmentOpen.value = true;
}

function handleBusinessAction(key: string) {
  const actionMap: Record<string, () => Promise<void> | void> = {
    calculate,
    decision: openDecision,
    gm: openGeneralManagerOpinion,
    publish: chooseCcAndPublish,
    score: submitMyScore,
    send: confirmSend,
  };
  void actionMap[key]?.();
}

function handleDelete(row: SrmPreliminaryEvaluationApi.Evaluation) {
  AntModal.confirm({
    content: `确认删除草稿 ${row.evaluationNo} 吗？`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteEvaluation(row.id!);
      message.success('草稿已删除');
      await gridApi.query();
    },
    title: '删除初评草稿',
    zIndex: SRM_NESTED_MODAL_Z_INDEX,
  });
}

function statusText(status?: string) {
  return (
    (
      {
        DRAFT: '草稿',
        GM_REVIEW: '总经理办理',
        PENDING_CALCULATION: '待计算得分',
        PENDING_DECISION: '待最终判定',
        PENDING_PUBLISH: '待发布',
        PUBLISHED: '已发布',
        SCORING: '评分中',
      } as Record<string, string>
    )[status || ''] ||
    status ||
    '-'
  );
}

function detailModeText(mode: DetailMode) {
  if (mode === 'create') {
    return '新增';
  }
  if (mode === 'edit') {
    return '编辑';
  }
  return '明细';
}

function statusColor(status?: string) {
  return (
    (
      {
        DRAFT: 'default',
        GM_REVIEW: 'purple',
        PENDING_CALCULATION: 'cyan',
        PENDING_DECISION: 'gold',
        PENDING_PUBLISH: 'orange',
        PUBLISHED: 'success',
        SCORING: 'processing',
      } as Record<string, string>
    )[status || ''] || 'default'
  );
}

function flowActionText(action?: string) {
  return (
    (
      {
        ALL_SCORED: '全部评分完成',
        ASSIGN_SCORER: '维护评分人',
        CALCULATE: '计算得分',
        CREATE: '创建',
        DECISION: '最终判定',
        GM_OPINION: '总经理意见',
        PUBLISH: '发布',
        SCORE: '提交评分',
        SEND: '确认发送',
        SEND_GM: '发送总经理',
        UPDATE: '更新',
      } as Record<string, string>
    )[String(action || '').toUpperCase()] ||
    action ||
    '-'
  );
}

function decisionText(value?: string) {
  if (value === 'QUALIFIED') {
    return '合格';
  }
  if (value === 'UNQUALIFIED') {
    return '不合格';
  }
  return '-';
}

function decisionColor(value?: string) {
  if (value === 'QUALIFIED') {
    return 'success';
  }
  if (value === 'UNQUALIFIED') {
    return 'error';
  }
  return 'default';
}

function buildEntryKey(evaluationId?: number, evaluationNo?: string) {
  if (evaluationId) {
    return `id:${evaluationId}`;
  }
  if (evaluationNo) {
    return `no:${evaluationNo}`;
  }
  return '';
}

function vetoText(item: SrmPreliminaryEvaluationApi.Item) {
  if (!item.vetoOperatorSnapshot || item.vetoScoreSnapshot === undefined) {
    return '-';
  }
  const operator = (
    { EQ: '=', GE: '≥', GT: '>', LE: '≤', LT: '<' } as Record<string, string>
  )[item.vetoOperatorSnapshot];
  return `${operator || item.vetoOperatorSnapshot}${item.vetoScoreSnapshot}`;
}

function buildNo() {
  const date = new Date();
  return `SRM-PE-${date.getFullYear()}${pad2(date.getMonth() + 1)}${pad2(date.getDate())}${pad2(date.getHours())}${pad2(date.getMinutes())}${pad2(date.getSeconds())}`;
}

function pad2(value: number) {
  return String(value).padStart(2, '0');
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0
    ? numericValue
    : undefined;
}

function normalizeScoreNumber(...values: unknown[]) {
  const rawValue = values.find(
    (value) => value !== undefined && value !== null && value !== '',
  );
  if (rawValue === undefined || rawValue === null) {
    return undefined;
  }
  const text = String(rawValue).trim();
  if (!text || text === '-' || text === '*') {
    return undefined;
  }
  const numericValue = Number(text);
  return Number.isFinite(numericValue) && numericValue >= 0
    ? numericValue
    : undefined;
}

function isTemplateUpgradeUnpublished(row: Record<string, any>) {
  const latestVersion = row.currentVersion || {};
  const latestVersionId = normalizeId(latestVersion.id);
  const publishedVersionId = normalizeId(row.currentVersionId);
  return (
    !!publishedVersionId &&
    !!latestVersionId &&
    latestVersionId !== publishedVersionId &&
    latestVersion.status !== 'PUBLISHED'
  );
}

function firstText(...values: unknown[]) {
  return values
    .flatMap((value) => (Array.isArray(value) ? value : [value]))
    .map((value) => String(value ?? '').trim())
    .find((value) => value.length > 0);
}

function parseIdList(value?: string) {
  return String(value || '')
    .split(/[,，;；\s]+/)
    .map(Number)
    .filter(
      (item, index, array) =>
        Number.isFinite(item) && item > 0 && array.indexOf(item) === index,
    );
}

function splitNameList(value?: string) {
  return String(value || '')
    .split(/[,，;；、\n\r]+/)
    .map((item) => item.trim())
    .filter(
      (item, index, array) =>
        !!sanitizeName(item) && array.indexOf(sanitizeName(item)!) === index,
    )
    .map((item) => sanitizeName(item)!);
}

function mergeIds(...groups: number[][]) {
  const result: number[] = [];
  groups.flat().forEach((id) => {
    if (Number.isFinite(id) && id > 0 && !result.includes(id)) {
      result.push(id);
    }
  });
  return result;
}

function getScorerCandidateIds(
  item: Partial<SrmPreliminaryEvaluationApi.Item>,
) {
  return mergeIds(
    parseIdList(item.scorerCandidateUserIds),
    item.scorerUserId ? [item.scorerUserId] : [],
  );
}

function getScorerCandidateOptions(
  item: Partial<SrmPreliminaryEvaluationApi.Item>,
) {
  const ids = getScorerCandidateIds(item);
  const names = splitNameList(item.scorerCandidateUserNames);
  return ids.map((id, index) => ({
    label:
      names[index] ||
      (id === item.scorerUserId ? item.scorerUserName : '') ||
      String(id),
    value: id,
  }));
}

function buildTemplateScorerFields(item: Record<string, any>) {
  const defaultScorerUserId = normalizeId(item.defaultScorerUserId);
  const candidateIds = mergeIds(
    parseIdList(item.defaultScorerUserIds),
    defaultScorerUserId ? [defaultScorerUserId] : [],
  );
  const candidateNames = splitNameList(item.defaultScorerUserNames);
  const scorerUserId = defaultScorerUserId || candidateIds[0];
  const scorerUserName =
    sanitizeName(item.defaultScorerUserName) || candidateNames[0] || '';
  return {
    scorerCandidateUserIds: candidateIds.join(',') || undefined,
    scorerCandidateUserNames: candidateNames.join('、') || undefined,
    scorerUserId,
    scorerUserName,
    scorerUserNameDisplay: scorerUserName || '-',
  };
}

type ScorerCandidateOption = { label: string; value: number };

function normalizeScorerCandidates<
  T extends Partial<SrmPreliminaryEvaluationApi.Item>,
>(items: T[]) {
  const optionsByDept = new Map<string, ScorerCandidateOption[]>();
  items.forEach((item) => {
    const key = getScorerCandidateDeptKey(item);
    if (!key) {
      return;
    }
    const options = getScorerCandidateOptions(item);
    optionsByDept.set(
      key,
      mergeScorerCandidateOptions(optionsByDept.get(key) || [], options),
    );
  });
  items.forEach((item) => {
    if (isMaskedPersistedScorerItem(item)) {
      return;
    }
    const key = getScorerCandidateDeptKey(item);
    const deptOptions = key ? optionsByDept.get(key) || [] : [];
    if (deptOptions.length <= 1) {
      return;
    }
    applyScorerCandidateOptions(
      item,
      mergeScorerCandidateOptions(deptOptions, getScorerCandidateOptions(item)),
    );
  });
  return items;
}

function getScorerCandidateDeptKey(
  item: Partial<SrmPreliminaryEvaluationApi.Item>,
) {
  return sanitizeName(item.defaultDeptNamesSnapshot);
}

function mergeScorerCandidateOptions(...groups: ScorerCandidateOption[][]) {
  const result: ScorerCandidateOption[] = [];
  groups.flat().forEach((option) => {
    const value = normalizeId(option.value);
    const label = sanitizeName(option.label);
    if (!value) {
      return;
    }
    const existing = result.find((item) => item.value === value);
    if (existing) {
      if (label && existing.label === String(value)) {
        existing.label = label;
      }
      return;
    }
    result.push({ label: label || String(value), value });
  });
  return result;
}

function applyScorerCandidateOptions(
  item: Partial<SrmPreliminaryEvaluationApi.Item>,
  options: ScorerCandidateOption[],
) {
  if (isMaskedPersistedScorerItem(item)) {
    return;
  }
  const selectedOption =
    options.find((option) => option.value === item.scorerUserId) || options[0];
  item.scorerCandidateUserIds =
    options.map((option) => option.value).join(',') || undefined;
  item.scorerCandidateUserNames =
    options.map((option) => option.label).join('、') || undefined;
  if (!selectedOption) {
    return;
  }
  item.scorerUserId = selectedOption.value;
  item.scorerUserName = selectedOption.label;
  item.scorerUserNameDisplay = selectedOption.label;
}

function isMaskedPersistedScorerItem(
  item: Partial<SrmPreliminaryEvaluationApi.Item>,
) {
  return (
    Number(item.id || 0) > 0 &&
    !normalizeId(item.scorerUserId) &&
    sanitizeName(item.scorerUserNameDisplay) === '*'
  );
}

function sanitizeName(value: unknown) {
  const text = String(value ?? '').trim();
  return text && text !== '-' && text !== '0' ? text : '';
}

function applyLocalScorerAssignment(
  item: SrmPreliminaryEvaluationApi.Item,
  scorerUserId: number,
  candidateUserIds: number[],
  selectedUsers: Array<SystemUserApi.User & { id: number }>,
) {
  const optionMap = new Map<number, string>();
  getScorerCandidateOptions(item).forEach((option) =>
    optionMap.set(option.value, option.label),
  );
  selectedUsers.forEach((user) =>
    optionMap.set(user.id, user.nickname || user.username || String(user.id)),
  );
  const scorerUserName = optionMap.get(scorerUserId) || String(scorerUserId);
  item.scorerUserId = scorerUserId;
  item.scorerUserName = scorerUserName;
  item.scorerUserNameDisplay = scorerUserName;
  item.scorerCandidateUserIds = candidateUserIds.join(',') || undefined;
  item.scorerCandidateUserNames =
    candidateUserIds.map((id) => optionMap.get(id) || String(id)).join('、') ||
    undefined;
}

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}

onMounted(() => void openEntryDetail());
watch(
  () => props.id,
  () => void openEntryDetail(),
);
watch(
  () => route.query.id,
  () => void openEntryDetail(),
);
watch(
  () => route.query.evaluationId,
  () => void openEntryDetail(),
);
watch(
  () => route.query.businessKey,
  () => void openEntryDetail(),
);
watch(
  () => route.query.bizId,
  () => void openEntryDetail(),
);
watch(
  () => route.query.evaluationNo,
  () => void openEntryDetail(),
);
</script>

<template>
  <Page auto-content-height>
    <SupplierModal @select="handleSupplierSelected" />
    <ProjectModal @select="handleProjectSelected" />
    <ProjectFilterModal @select="handleProjectFilterSelected" />
    <TemplateModal @select="handleTemplateSelected" />
    <UserModal title="选择人员" @confirm="handleUserConfirm" />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">选择初评</div>
      </div>
      <div class="srm-crud-page__body">
        <Grid>
          <template #toolbar-tools>
            <Space :size="8">
              <Button @click="openProjectFilterPicker">
                <IconifyIcon icon="lucide:folder-search" />
                {{ projectFilter.projectName || '筛选项目' }}
              </Button>
              <Button
                v-if="projectFilter.projectId"
                title="清除项目筛选"
                type="text"
                @click="clearProjectFilter"
              >
                <IconifyIcon icon="lucide:x" />
              </Button>
              <TableAction
                :actions="[
                  {
                    icon: ACTION_ICON.ADD,
                    label: '新增选择初评',
                    onClick: openCreate,
                    type: 'primary',
                  },
                ]"
              />
            </Space>
          </template>

          <template #evaluationNo="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.evaluationNo || '-' }}
            </a>
          </template>
          <template #supplierName="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.supplierName || '-' }}
            </a>
          </template>
          <template #projectName="{ row }">
            <span class="srm-crud-cell">
              {{
                [row.projectCode, row.projectName]
                  .filter(Boolean)
                  .join(' / ') || '-'
              }}
            </span>
          </template>
          <template #templateNameSnapshot="{ row }">
            <span class="srm-crud-cell">
              {{ row.templateNameSnapshot || '-' }} /
              {{ row.templateVersionSnapshot || '-' }}
            </span>
          </template>
          <template #status="{ row }">
            <Tag :color="statusColor(row.status)" class="!m-0">
              {{ statusText(row.status) }}
            </Tag>
          </template>
          <template #totalScoreDisplay="{ row }">
            {{ row.totalScoreDisplay || '-' }}
          </template>
          <template #finalDecision="{ row }">
            <Tag
              v-if="row.finalDecision"
              :color="decisionColor(row.finalDecision)"
              class="!m-0"
            >
              {{ decisionText(row.finalDecision) }}
            </Tag>
            <span v-else>-</span>
          </template>
          <template #actions="{ row }">
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.PREVIEW,
                  label:
                    row.canScore ||
                    row.canCalculate ||
                    row.canHandleGeneralManager ||
                    row.canPublish
                      ? '办理'
                      : '查看',
                  onClick: openDetail.bind(null, row),
                  type: 'link',
                },
              ]"
              :drop-down-actions="[
                {
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  ifShow: row.status === 'DRAFT' && row.canMaintain !== false,
                  label: '删除草稿',
                  onClick: handleDelete.bind(null, row),
                  type: 'link',
                },
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>

    <DetailModal>
      <Spin :spinning="saving" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>

            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">选择初评</div>
              <div class="qms-ncr-title-panel__subtitle">
                <span
                  v-for="item in subtitleItems"
                  :key="item"
                  class="qms-ncr-title-panel__subtitle-item"
                >
                  {{ item }}
                </span>
              </div>
            </div>

            <div class="qms-ncr-toolbar__actions">
              <div v-if="form.id" class="qms-ncr-toolbar__log-icons">
                <Button
                  class="qms-ncr-toolbar-icon-btn"
                  size="small"
                  title="审批日志"
                  @click="openAuditLog"
                >
                  <IconifyIcon icon="mdi:clipboard-check-outline" />
                </Button>
                <Button
                  v-if="!isScoringHandleMode"
                  class="qms-ncr-toolbar-icon-btn"
                  size="small"
                  title="操作日志"
                  @click="openLogDrawer('operation')"
                >
                  <IconifyIcon icon="mdi:history" />
                </Button>
              </div>
              <Button
                v-if="isReadonly && form.canMaintain"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="switchToEdit"
              >
                <IconifyIcon icon="lucide:edit-3" />
                编辑
              </Button>
              <Button
                v-if="!isReadonly"
                class="qms-ncr-toolbar-action"
                type="primary"
                @click="saveDraft(false)"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Button
                v-if="businessActions.length === 1"
                class="qms-ncr-toolbar-action"
                @click="handleBusinessAction(String(businessActions[0]?.key))"
              >
                <IconifyIcon :icon="businessActions[0]?.icon" />
                {{ businessActions[0]?.label }}
              </Button>
              <Dropdown
                v-else-if="businessActions.length > 1"
                placement="bottomRight"
                trigger="click"
              >
                <Button class="qms-ncr-toolbar-action">
                  <IconifyIcon icon="lucide:workflow" />
                  办理动作
                </Button>
                <template #overlay>
                  <Menu
                    @click="({ key }: any) => handleBusinessAction(String(key))"
                  >
                    <Menu.Item
                      v-for="action in businessActions"
                      :key="action.key"
                    >
                      <span class="srm-nowrap-action">
                        <IconifyIcon :icon="action.icon" />
                        {{ action.label }}
                      </span>
                    </Menu.Item>
                  </Menu>
                </template>
              </Dropdown>
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
                    <Button
                      size="small"
                      type="link"
                      @click="basicExpanded = !basicExpanded"
                    >
                      <span class="srm-nowrap-action">
                        <IconifyIcon
                          :icon="
                            basicExpanded
                              ? 'lucide:chevron-up'
                              : 'lucide:chevron-down'
                          "
                        />
                        {{ basicExpanded ? '收起' : '展开' }}
                      </span>
                    </Button>
                  </div>
                  <div
                    v-if="!basicExpanded && isReadonly"
                    class="srm-basic-summary-grid"
                  >
                    <div class="srm-basic-summary-item">
                      <span class="srm-basic-summary-label">初评项目</span>
                      <span class="srm-basic-summary-value">
                        {{
                          [form.projectCode, form.projectName]
                            .filter(Boolean)
                            .join(' / ') || '-'
                        }}
                      </span>
                    </div>
                    <div class="srm-basic-summary-item">
                      <span class="srm-basic-summary-label">供应商编号</span>
                      <span class="srm-basic-summary-value">
                        {{ form.supplierCode || '-' }}
                      </span>
                    </div>
                    <div class="srm-basic-summary-item">
                      <span class="srm-basic-summary-label">供应商名称</span>
                      <span class="srm-basic-summary-value">
                        {{ form.supplierName || '-' }}
                      </span>
                    </div>
                    <div class="srm-basic-summary-item">
                      <span class="srm-basic-summary-label">最终得分</span>
                      <span class="srm-basic-summary-value">
                        {{ form.totalScoreDisplay || '-' }}
                      </span>
                    </div>
                  </div>
                  <div v-show="basicExpanded" class="erp-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">初评单号</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="canEditBasic"
                          v-model:value="form.evaluationNo"
                          placeholder="请输入初评单号"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.evaluationNo || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">初评项目</label>
                      <div class="erp-form-value">
                        <div
                          v-if="canEditBasic"
                          class="qms-exception-linked-ncr srm-reference-picker"
                        >
                          <Input
                            :value="
                              [form.projectCode, form.projectName]
                                .filter(Boolean)
                                .join(' / ')
                            "
                            placeholder="点击右侧选择初评项目"
                            readonly
                            @click="openProjectPicker"
                          >
                            <template #suffix>
                              <Button
                                class="qms-exception-inline-icon-btn"
                                size="small"
                                title="选择初评项目"
                                type="text"
                                @click.stop="openProjectPicker"
                              >
                                <IconifyIcon icon="lucide:search" />
                              </Button>
                            </template>
                          </Input>
                        </div>
                        <span v-else class="qms-exception-readonly-value">
                          {{
                            [form.projectCode, form.projectName]
                              .filter(Boolean)
                              .join(' / ') || '-'
                          }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商编号</label>
                      <div class="erp-form-value">
                        <div
                          v-if="canEditBasic"
                          class="qms-exception-linked-ncr srm-reference-picker"
                        >
                          <Input
                            v-model:value="form.supplierCode"
                            placeholder="点击右侧选择供应商"
                            readonly
                            @click="openSupplierPicker"
                          >
                            <template #suffix>
                              <Button
                                class="qms-exception-inline-icon-btn"
                                size="small"
                                title="选择供应商"
                                type="text"
                                @click.stop="openSupplierPicker"
                              >
                                <IconifyIcon icon="lucide:search" />
                              </Button>
                            </template>
                          </Input>
                        </div>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.supplierCode || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">供应商名称</label>
                      <div class="erp-form-value">
                        <div
                          v-if="canEditBasic"
                          class="qms-exception-linked-ncr srm-reference-picker"
                        >
                          <Input
                            v-model:value="form.supplierName"
                            placeholder="点击右侧选择供应商"
                            readonly
                            @click="openSupplierPicker"
                          >
                            <template #suffix>
                              <Button
                                class="qms-exception-inline-icon-btn"
                                size="small"
                                title="选择供应商"
                                type="text"
                                @click.stop="openSupplierPicker"
                              >
                                <IconifyIcon icon="lucide:search" />
                              </Button>
                            </template>
                          </Input>
                        </div>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.supplierName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评估模板</label>
                      <div class="erp-form-value">
                        <div
                          v-if="canEditBasic"
                          class="qms-exception-linked-ncr srm-reference-picker"
                        >
                          <Input
                            :value="
                              [
                                form.templateCodeSnapshot,
                                form.templateNameSnapshot,
                              ]
                                .filter(Boolean)
                                .join(' / ')
                            "
                            placeholder="点击右侧选择评估模板"
                            readonly
                            @click="openTemplatePicker"
                          >
                            <template #suffix>
                              <Button
                                class="qms-exception-inline-icon-btn"
                                size="small"
                                title="选择评估模板"
                                type="text"
                                @click.stop="openTemplatePicker"
                              >
                                <IconifyIcon icon="lucide:search" />
                              </Button>
                            </template>
                          </Input>
                        </div>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.templateCodeSnapshot || '-' }} /
                          {{ form.templateNameSnapshot || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">模板版本</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.templateVersionSnapshot || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">满分/合格线</label>
                      <div class="erp-form-value">
                        <Space v-if="canEditBasic" class="w-full" :size="8">
                          <InputNumber
                            v-model:value="form.totalScoreBaseline"
                            class="srm-score-line-input"
                            :min="0"
                            placeholder="满分"
                          />
                          <span class="text-slate-400">/</span>
                          <InputNumber
                            v-model:value="form.qualificationScoreSnapshot"
                            class="srm-score-line-input"
                            :min="0"
                            placeholder="合格线"
                          />
                        </Space>
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.totalScoreBaseline || '-' }} /
                          {{ form.qualificationScoreSnapshot || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">发起人</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.initiatorName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">当前状态</label>
                      <div class="erp-form-value">
                        <Tag :color="statusColor(form.status)" class="!m-0">
                          {{ statusText(form.status) }}
                        </Tag>
                      </div>
                    </div>
                  </div>
                </section>

                <section class="erp-basic-form srm-detail-section">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>评估指标</strong>
                    </div>
                  </div>
                  <Table
                    :columns="itemColumns"
                    :data-source="form.items || []"
                    :pagination="false"
                    row-key="id"
                    :scroll="{ x: itemTableScrollX, y: 420 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, index, record }">
                      <template v-if="column.dataIndex === 'seq'">
                        {{ index + 1 }}
                      </template>
                      <template v-else-if="column.dataIndex === 'veto'">
                        {{ vetoText(record) }}
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'scorerUserNameDisplay'"
                      >
                        <Space
                          v-if="canEditScorer"
                          class="srm-scorer-cell"
                          :size="4"
                        >
                          <Select
                            v-if="getScorerCandidateOptions(record).length > 1"
                            :value="record.scorerUserId"
                            class="srm-scorer-select"
                            :dropdown-style="{
                              zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                            }"
                            :get-popup-container="resolveNestedPopupContainer"
                            :options="getScorerCandidateOptions(record)"
                            placeholder="选择评分人"
                            popup-class-name="srm-preliminary-evaluation-dropdown"
                            @change="
                              (value) =>
                                saveScorerAssignment(record, Number(value))
                            "
                          />
                          <Input
                            v-else
                            :value="
                              record.scorerUserNameDisplay ||
                              record.scorerUserName ||
                              '-'
                            "
                            class="srm-scorer-input"
                            readonly
                          />
                          <Button
                            size="small"
                            title="选择评分人"
                            type="text"
                            @click="chooseScorer(record)"
                          >
                            <IconifyIcon icon="lucide:users-round" />
                          </Button>
                        </Space>
                        <span v-else>
                          {{
                            record.scorerUserNameDisplay ||
                            record.scorerUserName ||
                            '-'
                          }}
                        </span>
                      </template>
                      <template
                        v-else-if="column.dataIndex === 'actualScoreDisplay'"
                      >
                        <InputNumber
                          v-if="form.canScore && record.currentUserItem"
                          v-model:value="record.actualScore"
                          :max="record.maxScoreSnapshot"
                          :min="0"
                          class="srm-score-input"
                        />
                        <span v-else>{{
                          record.actualScoreDisplay || '-'
                        }}</span>
                      </template>
                      <template
                        v-else-if="
                          column.dataIndex === 'scoringDescriptionDisplay'
                        "
                      >
                        <Input.TextArea
                          v-if="form.canScore && record.currentUserItem"
                          v-model:value="record.scoringDescription"
                          :auto-size="{ minRows: 1, maxRows: 3 }"
                          class="srm-score-description"
                          placeholder="填写评分说明（选填）"
                        />
                        <span v-else>
                          {{ record.scoringDescriptionDisplay || '-' }}
                        </span>
                      </template>
                      <template v-else-if="column.dataIndex === 'attachment'">
                        <Button
                          :disabled="!form.id || form.status === 'DRAFT'"
                          size="small"
                          type="link"
                          @click="openAttachment(record)"
                        >
                          附件
                        </Button>
                      </template>
                      <template v-else-if="column.dataIndex === 'actions'">
                        <Tag
                          :color="
                            record.scoreStatus === 'COMPLETED'
                              ? 'success'
                              : 'default'
                          "
                          class="!m-0"
                        >
                          {{
                            record.scoreStatus === 'COMPLETED'
                              ? '已评分'
                              : '待评分'
                          }}
                        </Tag>
                      </template>
                    </template>
                  </Table>
                </section>

                <section
                  v-if="form.status && form.status !== 'DRAFT'"
                  class="erp-basic-form srm-detail-section"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>汇总与判定</strong>
                    </div>
                  </div>
                  <div class="erp-form-grid">
                    <div class="erp-form-item">
                      <label class="erp-form-label">最终得分</label>
                      <div class="erp-form-value">
                        {{ form.totalScoreDisplay || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">自动判定</label>
                      <div class="erp-form-value">
                        {{ decisionText(form.autoDecision) }}
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">最终判定</label>
                      <div class="erp-form-value">
                        {{ decisionText(form.finalDecision) }}
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        否决说明
                      </label>
                      <div class="erp-form-value">
                        {{ form.vetoDescription || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        最终说明
                      </label>
                      <div class="erp-form-value">
                        {{ form.finalDescription || '-' }}
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--full">
                      <label class="erp-form-label erp-form-label--tall">
                        总经理意见
                      </label>
                      <div class="erp-form-value">
                        {{ form.generalManagerOpinionDisplay || '-' }}
                        <span v-if="form.generalManagerHandleTime">
                          （{{ form.generalManagerHandleTime }}）
                        </span>
                      </div>
                    </div>
                  </div>
                </section>
              </div>
            </div>
          </div>
        </div>
      </Spin>
    </DetailModal>

    <ProcessAuditModal />
    <BusinessLogDrawer
      v-model:open="logDrawerVisible"
      :logs="businessLogs"
      :mode="logDrawerMode"
      :title="logDrawerMode === 'operation' ? '操作日志' : '流程日志'"
    />

    <AntModal
      v-model:open="attachmentOpen"
      :footer="null"
      :title="`评分附件｜${selectedAttachmentItem?.indicatorNameSnapshot || ''}`"
      width="860px"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <SrmAttachmentPanel
        v-if="selectedAttachmentItem"
        :biz-id="selectedAttachmentItem.id"
        biz-type="PRELIMINARY_EVALUATION_SCORE"
        :mode="
          form.canScore && selectedAttachmentItem.currentUserItem
            ? 'edit'
            : 'detail'
        "
      />
    </AntModal>

    <AntModal
      v-model:open="decisionOpen"
      title="最终导入判定"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="confirmDecision"
    >
      <Form layout="vertical">
        <Form.Item label="自动计算得分">
          <Input :value="form.totalScoreDisplay || '-'" disabled />
        </Form.Item>
        <Form.Item label="最终得分" required>
          <InputNumber
            v-model:value="finalTotalScore"
            :max="form.totalScoreBaseline"
            :min="0"
            :precision="2"
            class="!w-full"
            placeholder="可在自动计算得分基础上修正"
          />
        </Form.Item>
        <Form.Item label="自动判定">
          <Input :value="decisionText(form.autoDecision)" disabled />
        </Form.Item>
        <Form.Item label="最终导入判定" required>
          <Radio.Group v-model:value="finalDecision">
            <Radio value="QUALIFIED">合格</Radio>
            <Radio value="UNQUALIFIED">不合格</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="判定说明" required>
          <Input.TextArea v-model:value="finalDescription" :rows="4" />
        </Form.Item>
        <Form.Item>
          <Checkbox v-model:checked="sendGeneralManager">
            发送总经理角色办理
          </Checkbox>
        </Form.Item>
        <Form.Item v-if="sendGeneralManager" label="总经理办理人" required>
          <Select
            v-model:value="selectedGeneralManagerId"
            :dropdown-style="{ zIndex: SRM_NESTED_DROPDOWN_Z_INDEX }"
            :get-popup-container="resolveNestedPopupContainer"
            placeholder="仅显示“总经理（供应商评估）”角色用户"
            popup-class-name="srm-preliminary-evaluation-dropdown"
          >
            <Select.Option
              v-for="user in generalManagerUsers"
              :key="user.id"
              :value="user.id"
            >
              {{ user.nickname || user.username }}
            </Select.Option>
          </Select>
        </Form.Item>
      </Form>
    </AntModal>

    <AntModal
      v-model:open="gmOpinionOpen"
      title="总经理意见"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
      @ok="confirmGeneralManagerOpinion"
    >
      <Form layout="vertical">
        <Form.Item label="办理意见" required>
          <Input.TextArea v-model:value="generalManagerOpinion" :rows="5" />
        </Form.Item>
      </Form>
    </AntModal>
  </Page>
</template>

<style scoped>
.srm-crud-page {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  background: #fff;
}

.srm-crud-page__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  padding: 12px 16px;
}

.srm-crud-page__title {
  color: #10233d;
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.srm-crud-page__body {
  flex: 1 1 0%;
  min-height: 0;
}

.srm-crud-link {
  color: #1d4ed8;
  cursor: pointer;
  font-weight: 700;
}

.srm-crud-link:hover {
  text-decoration: underline;
}

.srm-crud-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-detail-section {
  margin-top: 12px;
}

.srm-basic-summary-grid {
  display: grid;
  gap: 0;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.srm-basic-summary-item {
  display: grid;
  min-width: 0;
  border-top: 1px solid #dbe5f1;
  border-right: 1px solid #dbe5f1;
  grid-template-columns: 132px minmax(0, 1fr);
}

.srm-basic-summary-item:first-child {
  border-left: 1px solid #dbe5f1;
}

.srm-basic-summary-label {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef3f9;
  color: #31465f;
  font-weight: 700;
  padding: 12px 10px;
  white-space: nowrap;
}

.srm-basic-summary-value {
  overflow: hidden;
  color: #10233d;
  padding: 12px 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-score-input {
  width: 88px;
}

.srm-score-line-input {
  width: 128px;
}

.srm-score-description {
  min-width: 250px;
}

.srm-scorer-cell {
  display: grid;
  min-width: 190px;
  align-items: center;
  grid-template-columns: minmax(0, 1fr) 32px;
}

.srm-scorer-input,
.srm-scorer-select {
  width: 154px;
}

.srm-scorer-cell :deep(.ant-select-selection-item),
.srm-scorer-cell :deep(.ant-input) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-nowrap-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
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

.qms-ncr-toolbar-icon-btn.ant-btn:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.qms-ncr-toolbar-icon-btn :deep(svg) {
  width: 18px;
  height: 18px;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>
