<script lang="ts" setup>
import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmEvaluationTemplateApi } from '#/api/mes/srm/standard/template';
import type { SystemUserApi } from '#/api/system/user';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Drawer,
  Dropdown,
  Input,
  InputNumber,
  Menu,
  message,
  Modal as AntModal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
} from 'ant-design-vue';

import {
  ACTION_ICON,
  TableAction,
  useVbenVxeGrid,
} from '#/adapter/vxe-table';
import {
  auditTemplate,
  createTemplate,
  getTemplateDetail,
  getTemplatePage,
  publishTemplate,
  submitTemplateAudit,
  updateTemplate,
  upgradeTemplate,
} from '#/api/mes/srm/standard/template';
import { UserSelectModal } from '#/views/system/user/components';

import SrmReferenceSelectModal from '../../shared/SrmReferenceSelectModal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmEvaluationTemplateManagement' });

type DetailMode = 'create' | 'detail' | 'edit';

const SRM_NESTED_MODAL_Z_INDEX = 5600;

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const detail = ref<SrmEvaluationTemplateApi.Template>();
const operationLogOpen = ref(false);
const selectedTemplateItem = ref<SrmEvaluationTemplateApi.Item>();
const versionHistoryOpen = ref(false);

const form = reactive<SrmEvaluationTemplateApi.SaveReq>(createEmptyForm());

const isNew = computed(() => !form.id);
const isReadonly = computed(() => detailMode.value === 'detail');
const currentVersionStatus = computed(
  () => detail.value?.currentVersion?.status || 'DRAFT',
);
const editableVersion = computed(() =>
  ['DRAFT', 'REJECTED'].includes(currentVersionStatus.value),
);
const itemScoreTotal = computed(() =>
  (form.items || []).reduce((sum, item) => sum + Number(item.maxScore || 0), 0),
);
const subtitleItems = computed(() =>
  [
    form.templateCode ? `编码 ${form.templateCode}` : '',
    form.versionNo ? `版本 ${form.versionNo}` : '',
    form.sceneType ? sceneText(form.sceneType) : '',
    versionStatusText(currentVersionStatus.value),
    detailMode.value === 'create'
      ? '新增'
      : detailMode.value === 'edit'
        ? '编辑'
        : '明细',
  ].filter(Boolean),
);
const templateActions = computed(() =>
  [
    {
      icon: 'lucide:send',
      key: 'submit',
      label: '提交审核',
      show: !!form.versionId && ['DRAFT', 'REJECTED'].includes(currentVersionStatus.value),
    },
    {
      icon: 'lucide:badge-check',
      key: 'approve',
      label: '审核通过',
      show: currentVersionStatus.value === 'PENDING_AUDIT',
    },
    {
      icon: 'lucide:undo-2',
      key: 'reject',
      label: '审核驳回',
      show: currentVersionStatus.value === 'PENDING_AUDIT',
    },
    {
      icon: 'lucide:rocket',
      key: 'publish',
      label: '发布版本',
      show: currentVersionStatus.value === 'APPROVED',
    },
    {
      icon: 'lucide:git-branch-plus',
      key: 'upgrade',
      label: '版本升级',
      show: currentVersionStatus.value === 'PUBLISHED',
    },
  ].filter((item) => item.show),
);

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入模板编码' },
    fieldName: 'templateCode',
    label: '模板编码',
  },
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入模板名称' },
    fieldName: 'templateName',
    label: '模板名称',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: sceneOptions(),
      placeholder: '请选择适用场景',
    },
    fieldName: 'sceneType',
    label: '适用场景',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: [
        { label: '启用', value: 'ENABLED' },
        { label: '停用', value: 'DISABLED' },
      ],
      placeholder: '请选择模板状态',
    },
    fieldName: 'status',
    label: '模板状态',
  },
];

const gridColumns: VxeTableGridOptions['columns'] = [
  { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
  {
    field: 'templateCode',
    fixed: 'left',
    minWidth: 180,
    slots: { default: 'templateCode' },
    title: '模板编码',
  },
  {
    field: 'templateName',
    fixed: 'left',
    minWidth: 260,
    slots: { default: 'templateName' },
    title: '模板名称',
  },
  {
    align: 'center',
    field: 'sceneType',
    minWidth: 120,
    slots: { default: 'sceneType' },
    title: '适用场景',
  },
  { align: 'center', field: 'currentVersionNo', minWidth: 130, title: '当前发布版本' },
  {
    align: 'center',
    field: 'currentVersionStatus',
    minWidth: 120,
    slots: { default: 'currentVersionStatus' },
    title: '版本状态',
  },
  {
    align: 'center',
    field: 'scoreLine',
    minWidth: 130,
    slots: { default: 'scoreLine' },
    title: '满分/合格线',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 100,
    slots: { default: 'status' },
    title: '模板状态',
  },
  { align: 'center', field: 'updateTime', minWidth: 170, title: '更新时间' },
  {
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 126,
  },
];

const itemColumns = [
  { dataIndex: 'groupCode', title: '维度编码', width: 120 },
  { dataIndex: 'groupName', title: '维度名称', width: 130 },
  { dataIndex: 'groupSort', title: '维度排序', width: 90 },
  { dataIndex: 'groupMaxScore', title: '维度满分', width: 100 },
  { dataIndex: 'veto', title: '否决条件', width: 160 },
  { dataIndex: 'indicatorCode', title: '指标编码', width: 130 },
  { dataIndex: 'indicatorName', title: '指标名称', width: 220 },
  { dataIndex: 'indicatorSort', title: '指标排序', width: 90 },
  { dataIndex: 'scoringRule', title: '评分规则', width: 330 },
  { dataIndex: 'maxScore', title: '满分', width: 90 },
  { dataIndex: 'defaultDeptNames', title: '默认评分部门', width: 150 },
  { dataIndex: 'defaultScorerUserName', title: '默认评分人', width: 210 },
  { dataIndex: 'attachmentRequired', title: '需附件', width: 85 },
  { align: 'center', dataIndex: 'actions', fixed: 'right', title: '操作', width: 70 },
];

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
          const result = await getTemplatePage({
            ...(formValues || {}),
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
  } as VxeTableGridOptions<SrmEvaluationTemplateApi.Template>,
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

const [MetricModal, metricModalApi] = useVbenModal({
  connectedComponent: SrmReferenceSelectModal,
  destroyOnClose: true,
});

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function createEmptyForm(): SrmEvaluationTemplateApi.SaveReq {
  return {
    items: [],
    qualificationScore: 60,
    sceneType: 'PRELIMINARY',
    templateCode: '',
    templateName: '',
    templateStatus: 'ENABLED',
    totalScore: 100,
    versionNo: 'V1',
  };
}

function resetForm(record: Partial<SrmEvaluationTemplateApi.SaveReq> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record, {
    items: record.items || [],
  });
}

function openCreate() {
  detail.value = undefined;
  resetForm();
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmEvaluationTemplateApi.Template) {
  const result = await getTemplateDetail(row.id!);
  applyTemplateDetail(result);
  detailMode.value = 'detail';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openEdit(row: SrmEvaluationTemplateApi.Template) {
  await openDetail(row);
  detailMode.value = 'edit';
}

function switchToEdit() {
  detailMode.value = 'edit';
}

async function closeDetail() {
  operationLogOpen.value = false;
  selectedTemplateItem.value = undefined;
  versionHistoryOpen.value = false;
  await detailModalApi.close();
}

function applyTemplateDetail(result: SrmEvaluationTemplateApi.Template) {
  detail.value = result;
  const version = result.currentVersion;
  resetForm({
    changeSummary: version?.changeSummary,
    id: result.id,
    items: (version?.items || []).map((item) => ({ ...item })),
    materialType: result.materialType,
    qualificationScore: Number(version?.qualificationScore || 60),
    sceneType: result.sceneType,
    templateCode: result.templateCode,
    templateName: result.templateName,
    templateRemark: result.remark,
    templateStatus: result.status,
    totalScore: Number(version?.totalScore || 100),
    version: version?.version,
    versionId: version?.id,
    versionNo: version?.versionNo || 'V1',
    versionRemark: version?.remark,
  });
}

async function switchVersion(versionId: number) {
  if (!detail.value?.id) {
    return;
  }
  const result = await getTemplateDetail(detail.value.id, versionId);
  applyTemplateDetail(result);
}

function addItem(seed: Partial<SrmEvaluationTemplateApi.Item> = {}) {
  const index = form.items.length + 1;
  form.items.push({
    attachmentRequired: false,
    defaultDeptNames: '',
    defaultScorerUserName: '',
    groupCode: seed.groupCode || `GROUP-${index}`,
    groupMaxScore: Number(seed.groupMaxScore || 0),
    groupName: seed.groupName || '',
    groupSort: Number(seed.groupSort || index * 10),
    indicatorCode: seed.indicatorCode || `ITEM-${index}`,
    indicatorName: seed.indicatorName || '',
    indicatorSort: Number(seed.indicatorSort || index * 10),
    maxScore: Number(seed.maxScore || 0),
    scoringRule: seed.scoringRule || '',
    vetoResult: seed.vetoResult || 'UNQUALIFIED',
    ...seed,
  });
}

function openMetricPicker() {
  if (isReadonly.value) {
    return;
  }
  metricModalApi
    .setData({
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      referenceType: 'metric',
      title: '选择考核指标',
    })
    .open();
}

function handleMetricSelected(row: Record<string, any>) {
  if (appendMetricItem(row)) {
    message.success('指标已带入，请维护分值和评分部门');
  }
}

function handleMetricSelectedList(rows: Record<string, any>[]) {
  let addedCount = 0;
  let duplicateCount = 0;
  rows.forEach((row) => {
    const result = appendMetricItem(row);
    if (result === true) {
      addedCount += 1;
    } else if (result === false && row?.code) {
      duplicateCount += 1;
    }
  });
  if (addedCount > 0) {
    message.success(
      `已带入 ${addedCount} 个指标${
        duplicateCount > 0 ? `，跳过 ${duplicateCount} 个重复指标` : ''
      }`,
    );
    return;
  }
  if (duplicateCount > 0) {
    message.info('所选指标已在当前模板中，无需重复添加');
  }
}

function appendMetricItem(row: Record<string, any>) {
  if (!row?.code) {
    return undefined;
  }
  if (form.items.some((item) => item.indicatorCode === row.code)) {
    return false;
  }
  addItem({
    defaultDeptNames: row.category ? `${row.category}部门` : '',
    groupCode: row.category || 'GROUP',
    groupName: row.category || '',
    indicatorCode: row.code,
    indicatorName: row.name,
    maxScore: 0,
    scoringRule: row.scoringMethod,
  });
  return true;
}

function openTemplateScorerPicker(item: SrmEvaluationTemplateApi.Item) {
  if (isReadonly.value) {
    return;
  }
  selectedTemplateItem.value = item;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: getTemplateScorerIds(item),
    })
    .open();
}

function handleTemplateScorerConfirm(users: SystemUserApi.User[]) {
  const item = selectedTemplateItem.value;
  if (!item) {
    return;
  }
  const selectedUsers = users
    .filter((user) => user.id !== undefined && Number(user.id) > 0)
    .map((user) => ({
      id: Number(user.id),
      name: user.nickname || user.username || String(user.id),
    }));
  const mergedUsers = mergeUserOptions(getTemplateScorerOptions(item), selectedUsers);
  applyTemplateScorers(item, mergedUsers);
  message.success(`已选择 ${mergedUsers.length} 个默认评分人`);
}

function getTemplateScorerIds(item: SrmEvaluationTemplateApi.Item) {
  return getTemplateScorerOptions(item).map((user) => user.id);
}

function getTemplateScorerOptions(item: SrmEvaluationTemplateApi.Item) {
  const ids = parseIdList(item.defaultScorerUserIds);
  if (
    item.defaultScorerUserId &&
    item.defaultScorerUserId > 0 &&
    !ids.includes(item.defaultScorerUserId)
  ) {
    ids.unshift(item.defaultScorerUserId);
  }
  const names = splitNameList(item.defaultScorerUserNames);
  if (
    item.defaultScorerUserName &&
    item.defaultScorerUserName !== '0' &&
    item.defaultScorerUserName !== '-' &&
    !names.includes(item.defaultScorerUserName)
  ) {
    names.unshift(item.defaultScorerUserName);
  }
  return ids.map((id, index) => ({
    id,
    name: names[index] || String(id),
  }));
}

function applyTemplateScorers(
  item: SrmEvaluationTemplateApi.Item,
  users: Array<{ id: number; name: string }>,
) {
  const normalizedUsers = mergeUserOptions([], users);
  item.defaultScorerUserIds = normalizedUsers.map((user) => user.id).join(',');
  item.defaultScorerUserNames = normalizedUsers.map((user) => user.name).join('、');
  item.defaultScorerUserId = normalizedUsers[0]?.id;
  item.defaultScorerUserName = normalizedUsers[0]?.name || '';
}

function displayTemplateScorers(item: SrmEvaluationTemplateApi.Item) {
  const names = splitNameList(item.defaultScorerUserNames);
  if (names.length > 0) {
    return names.join('、');
  }
  return item.defaultScorerUserName || '';
}

async function saveTemplate(closeAfter = false) {
  if (!form.templateCode || !form.templateName || !form.versionNo) {
    message.warning('请填写模板编码、名称和版本号');
    return;
  }
  if ((form.items || []).length === 0) {
    message.warning('请至少维护一项指标');
    return;
  }
  if (Number(itemScoreTotal.value.toFixed(2)) !== Number(form.totalScore)) {
    message.warning(`指标满分合计 ${itemScoreTotal.value} 必须等于模板总分 ${form.totalScore}`);
    return;
  }
  saving.value = true;
  try {
    if (form.id) {
      await updateTemplate(form);
      const result = await getTemplateDetail(form.id, form.versionId);
      applyTemplateDetail(result);
      message.success('模板保存成功');
    } else {
      const id = await createTemplate(form);
      const result = await getTemplateDetail(id);
      applyTemplateDetail(result);
      detailMode.value = 'edit';
      message.success('模板草稿已创建');
    }
    await gridApi.query();
    if (closeAfter) {
      await closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

function handleTemplateAction(key: string) {
  const versionId = form.versionId;
  if (!versionId) {
    message.warning('当前模板没有可操作版本');
    return;
  }
  const versionNo = form.versionNo || '当前版本';
  if (key === 'submit') {
    return confirmAction('提交审核', `确认提交 ${versionNo} 审核吗？`, async () => {
      await submitTemplateAudit(versionId, '模板指标与分值配置已确认');
    });
  }
  if (key === 'approve') {
    return confirmAction('审核通过', `确认审核通过 ${versionNo} 吗？`, async () => {
      await auditTemplate(versionId, true, '审核通过');
    });
  }
  if (key === 'reject') {
    return confirmAction('审核驳回', `确认驳回 ${versionNo} 吗？`, async () => {
      await auditTemplate(versionId, false, '审核驳回，请修改后重新提交');
    });
  }
  if (key === 'publish') {
    return confirmAction('发布版本', `发布后该版本可被初评单引用，确认发布 ${versionNo} 吗？`, async () => {
      await publishTemplate(versionId, '审核通过并发布');
    });
  }
  if (key === 'upgrade') {
    return confirmAction('版本升级', `将基于 ${versionNo} 生成下一版草稿。`, async () => {
      const newVersionId = await upgradeTemplate(versionId, `基于 ${versionNo} 升级`);
      if (detail.value?.id) {
        const result = await getTemplateDetail(detail.value.id, Number(newVersionId));
        applyTemplateDetail(result);
        detailMode.value = 'edit';
      }
    });
  }
}

function handleRowAction(key: string, row: SrmEvaluationTemplateApi.Template) {
  if (key === 'edit') {
    void openEdit(row);
    return;
  }
  const version = row.currentVersion;
  if (!version?.id) {
    message.warning('当前模板没有可操作版本');
    return;
  }
  const applyRowAction = async (action: () => Promise<unknown>) => {
    await action();
    message.success('操作成功');
    await gridApi.query();
  };
  if (key === 'submit') {
    return confirmRowAction('提交审核', `确认提交 ${version.versionNo} 审核吗？`, () =>
      applyRowAction(() => submitTemplateAudit(version.id!, '模板指标与分值配置已确认')),
    );
  }
  if (key === 'approve' || key === 'reject') {
    const approved = key === 'approve';
    return confirmRowAction(
      approved ? '审核通过' : '审核驳回',
      `确认${approved ? '通过' : '驳回'} ${version.versionNo} 吗？`,
      () =>
        applyRowAction(() =>
          auditTemplate(
            version.id!,
            approved,
            approved ? '审核通过' : '审核驳回，请修改后重新提交',
          ),
        ),
    );
  }
  if (key === 'publish') {
    return confirmRowAction('发布版本', `确认发布 ${version.versionNo} 吗？`, () =>
      applyRowAction(() => publishTemplate(version.id!, '审核通过并发布')),
    );
  }
  if (key === 'upgrade') {
    return confirmRowAction('版本升级', `将基于 ${version.versionNo} 生成下一版草稿。`, () =>
      applyRowAction(() => upgradeTemplate(version.id!, `基于 ${version.versionNo} 升级`)),
    );
  }
}

function confirmRowAction(title: string, content: string, action: () => Promise<void>) {
  AntModal.confirm({
    content,
    okText: '确认',
    async onOk() {
      await action();
    },
    title,
    zIndex: SRM_NESTED_MODAL_Z_INDEX,
  });
}

function confirmAction(title: string, content: string, action: () => Promise<void>) {
  AntModal.confirm({
    content,
    okText: '确认',
    async onOk() {
      await action();
      message.success(`${title}成功`);
      if (form.id) {
        const result = await getTemplateDetail(form.id, form.versionId);
        applyTemplateDetail(result);
      }
      await gridApi.query();
    },
    title,
    zIndex: SRM_NESTED_MODAL_Z_INDEX,
  });
}

function sceneOptions() {
  return [
    { label: '选择初评', value: 'PRELIMINARY' },
    { label: '季度评定', value: 'QUARTER' },
    { label: '年度评定', value: 'YEAR' },
    { label: '现场稽核', value: 'AUDIT' },
  ];
}

function sceneText(scene?: string) {
  return sceneOptions().find((item) => item.value === scene)?.label || scene || '-';
}

function versionStatusColor(status?: string) {
  return (
    {
      APPROVED: 'cyan',
      DRAFT: 'default',
      PENDING_AUDIT: 'processing',
      PUBLISHED: 'success',
      REJECTED: 'error',
    } as Record<string, string>
  )[status || ''] || 'default';
}

function versionStatusText(status?: string) {
  return (
    {
      APPROVED: '已审核',
      DRAFT: '草稿',
      PENDING_AUDIT: '待审核',
      PUBLISHED: '已发布',
      REJECTED: '已驳回',
    } as Record<string, string>
  )[status || ''] || status || '-';
}

function templateStatusText(status?: string) {
  return status === 'DISABLED' ? '停用' : '启用';
}

function templateStatusColor(status?: string) {
  return status === 'DISABLED' ? 'default' : 'success';
}

function parseIdList(value?: string) {
  return String(value || '')
    .split(/[,，;；\s]+/)
    .map((item) => Number(item))
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
        item && item !== '-' && item !== '0' && array.indexOf(item) === index,
    );
}

function mergeUserOptions(
  originalUsers: Array<{ id: number; name: string }>,
  appendedUsers: Array<{ id: number; name: string }>,
) {
  const userMap = new Map<number, { id: number; name: string }>();
  [...originalUsers, ...appendedUsers].forEach((user) => {
    if (Number.isFinite(user.id) && user.id > 0 && !userMap.has(user.id)) {
      userMap.set(user.id, user);
    }
  });
  return [...userMap.values()];
}

function vetoText(item: SrmEvaluationTemplateApi.Item) {
  if (!item.vetoOperator || item.vetoScore === undefined) {
    return '-';
  }
  const operator = (
    { EQ: '=', GE: '≥', GT: '>', LE: '≤', LT: '<' } as Record<string, string>
  )[item.vetoOperator];
  return `${operator || item.vetoOperator}${item.vetoScore} 判不合格`;
}
</script>

<template>
  <Page auto-content-height>
    <MetricModal
      @select="handleMetricSelected"
      @select-multiple="handleMetricSelectedList"
    />
    <UserModal title="选择默认评分人" @confirm="handleTemplateScorerConfirm" />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">评估模板维护</div>
      </div>
      <div class="srm-crud-page__body">
        <Grid>
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.ADD,
                  label: '新增评估模板',
                  onClick: openCreate,
                  type: 'primary',
                },
              ]"
            />
          </template>

          <template #templateCode="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.templateCode || '-' }}
            </a>
          </template>
          <template #templateName="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.templateName || '-' }}
            </a>
          </template>
          <template #sceneType="{ row }">
            <Tag color="processing" class="!m-0">
              {{ sceneText(row.sceneType) }}
            </Tag>
          </template>
          <template #currentVersionStatus="{ row }">
            <Tag
              :color="versionStatusColor(row.currentVersion?.status)"
              class="!m-0"
            >
              {{ versionStatusText(row.currentVersion?.status) }}
            </Tag>
          </template>
          <template #scoreLine="{ row }">
            {{ row.currentVersion?.totalScore ?? '-' }} /
            {{ row.currentVersion?.qualificationScore ?? '-' }}
          </template>
          <template #status="{ row }">
            <Tag :color="templateStatusColor(row.status)" class="!m-0">
              {{ templateStatusText(row.status) }}
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
              :drop-down-actions="[
                {
                  icon: ACTION_ICON.EDIT,
                  ifShow: ['DRAFT', 'REJECTED'].includes(
                    row.currentVersion?.status || '',
                  ),
                  label: '编辑',
                  onClick: handleRowAction.bind(null, 'edit', row),
                  type: 'link',
                },
                {
                  icon: 'lucide:send',
                  ifShow: ['DRAFT', 'REJECTED'].includes(
                    row.currentVersion?.status || '',
                  ),
                  label: '提交审核',
                  onClick: handleRowAction.bind(null, 'submit', row),
                  type: 'link',
                },
                {
                  icon: 'lucide:badge-check',
                  ifShow: row.currentVersion?.status === 'PENDING_AUDIT',
                  label: '审核通过',
                  onClick: handleRowAction.bind(null, 'approve', row),
                  type: 'link',
                },
                {
                  danger: true,
                  icon: 'lucide:undo-2',
                  ifShow: row.currentVersion?.status === 'PENDING_AUDIT',
                  label: '审核驳回',
                  onClick: handleRowAction.bind(null, 'reject', row),
                  type: 'link',
                },
                {
                  icon: 'lucide:rocket',
                  ifShow: row.currentVersion?.status === 'APPROVED',
                  label: '发布版本',
                  onClick: handleRowAction.bind(null, 'publish', row),
                  type: 'link',
                },
                {
                  icon: 'lucide:git-branch-plus',
                  ifShow: row.currentVersion?.status === 'PUBLISHED',
                  label: '版本升级',
                  onClick: handleRowAction.bind(null, 'upgrade', row),
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
              <div class="qms-ncr-title-panel__name">评估模板维护</div>
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
                  title="版本历史"
                  @click="versionHistoryOpen = true"
                >
                  <IconifyIcon icon="mdi:timeline-clock-outline" />
                </Button>
                <Button
                  class="qms-ncr-toolbar-icon-btn"
                  size="small"
                  title="操作日志"
                  @click="operationLogOpen = true"
                >
                  <IconifyIcon icon="mdi:history" />
                </Button>
              </div>
              <Button
                v-if="isReadonly && editableVersion"
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
                @click="saveTemplate(false)"
              >
                <IconifyIcon icon="lucide:save" />
                保存
              </Button>
              <Dropdown
                v-if="templateActions.length > 0"
                placement="bottomRight"
                trigger="click"
              >
                <Button class="qms-ncr-toolbar-action">
                  <IconifyIcon icon="lucide:workflow" />
                  模板动作
                </Button>
                <template #overlay>
                  <Menu @click="({ key }: any) => handleTemplateAction(String(key))">
                    <Menu.Item
                      v-for="action in templateActions"
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
                          <strong>模板基本信息</strong>
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
                      <div v-show="basicExpanded" class="erp-form-grid">
                        <div class="erp-form-item">
                          <label class="erp-form-label">模板编码</label>
                          <div class="erp-form-value">
                            <Input
                              v-if="!isReadonly"
                              v-model:value="form.templateCode"
                              placeholder="请输入模板编码"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.templateCode || '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">模板名称</label>
                          <div class="erp-form-value">
                            <Input
                              v-if="!isReadonly"
                              v-model:value="form.templateName"
                              placeholder="请输入模板名称"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.templateName || '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">适用场景</label>
                          <div class="erp-form-value">
                            <Select
                              v-if="!isReadonly"
                              v-model:value="form.sceneType"
                              :options="sceneOptions()"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ sceneText(form.sceneType) }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">模板状态</label>
                          <div class="erp-form-value">
                            <Select
                              v-if="!isReadonly"
                              v-model:value="form.templateStatus"
                              :options="[
                                { label: '启用', value: 'ENABLED' },
                                { label: '停用', value: 'DISABLED' },
                              ]"
                            />
                            <Tag
                              v-else
                              :color="templateStatusColor(form.templateStatus)"
                              class="!m-0"
                            >
                              {{ templateStatusText(form.templateStatus) }}
                            </Tag>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">版本号</label>
                          <div class="erp-form-value">
                            <Input
                              v-if="!isReadonly"
                              v-model:value="form.versionNo"
                              placeholder="请输入版本号"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.versionNo || '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">模板总分</label>
                          <div class="erp-form-value">
                            <InputNumber
                              v-if="!isReadonly"
                              v-model:value="form.totalScore"
                              class="w-full"
                              :min="0"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.totalScore ?? '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">合格线</label>
                          <div class="erp-form-value">
                            <InputNumber
                              v-if="!isReadonly"
                              v-model:value="form.qualificationScore"
                              class="w-full"
                              :min="0"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.qualificationScore ?? '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">适用物料</label>
                          <div class="erp-form-value">
                            <Input
                              v-if="!isReadonly"
                              v-model:value="form.materialType"
                              placeholder="请输入适用物料分类"
                            />
                            <span v-else class="qms-exception-readonly-value">
                              {{ form.materialType || '-' }}
                            </span>
                          </div>
                        </div>
                        <div class="erp-form-item">
                          <label class="erp-form-label">版本状态</label>
                          <div class="erp-form-value">
                            <Tag
                              :color="versionStatusColor(currentVersionStatus)"
                              class="!m-0"
                            >
                              {{ versionStatusText(currentVersionStatus) }}
                            </Tag>
                          </div>
                        </div>
                        <div class="erp-form-item erp-form-item--full">
                          <label class="erp-form-label erp-form-label--tall">
                            变更说明
                          </label>
                          <div class="erp-form-value">
                            <Input.TextArea
                              v-if="!isReadonly"
                              v-model:value="form.changeSummary"
                              :rows="3"
                              placeholder="请输入版本变更说明"
                            />
                            <span
                              v-else
                              class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                            >
                              {{ form.changeSummary || '-' }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </section>

                    <section class="erp-basic-form srm-detail-section">
                      <div class="detail-list-head">
                        <div class="detail-list-title">
                          <strong>
                            指标明细（合计 {{ itemScoreTotal }} /
                            {{ form.totalScore }} 分）
                          </strong>
                        </div>
                        <Space v-if="!isReadonly" :size="8">
                          <Button @click="openMetricPicker">
                            <IconifyIcon icon="lucide:search-check" />
                            从指标库选择
                          </Button>
                          <Button type="primary" @click="addItem()">
                            <IconifyIcon icon="lucide:plus" />
                            新增指标
                          </Button>
                        </Space>
                      </div>
                      <Table
                        :columns="itemColumns"
                        :data-source="form.items"
                        :pagination="false"
                        row-key="indicatorCode"
                        :scroll="{ x: 2170, y: 430 }"
                        size="small"
                      >
                        <template #bodyCell="{ column, record, index }">
                          <template v-if="column.dataIndex === 'veto'">
                            <span v-if="isReadonly">{{ vetoText(record) }}</span>
                            <Space v-else :size="4">
                              <Select
                                v-model:value="record.vetoOperator"
                                allow-clear
                                class="srm-veto-operator"
                              >
                                <Select.Option value="LE">≤</Select.Option>
                                <Select.Option value="LT">&lt;</Select.Option>
                                <Select.Option value="EQ">=</Select.Option>
                                <Select.Option value="GE">≥</Select.Option>
                                <Select.Option value="GT">&gt;</Select.Option>
                              </Select>
                              <InputNumber
                                v-model:value="record.vetoScore"
                                :min="0"
                                class="srm-veto-score"
                              />
                            </Space>
                          </template>
                          <template
                            v-else-if="
                              column.dataIndex === 'defaultScorerUserName'
                            "
                          >
                            <div
                              v-if="!isReadonly"
                              class="srm-inline-picker"
                              @click="openTemplateScorerPicker(record)"
                            >
                              <Input
                                :value="displayTemplateScorers(record)"
                                placeholder="点击选择默认评分人"
                                readonly
                              />
                              <Button size="small" type="text">
                                <IconifyIcon icon="lucide:users-round" />
                              </Button>
                            </div>
                            <span v-else>
                              {{ displayTemplateScorers(record) || '-' }}
                            </span>
                          </template>
                          <template
                            v-else-if="column.dataIndex === 'attachmentRequired'"
                          >
                            <Switch
                              v-model:checked="record.attachmentRequired"
                              :disabled="isReadonly"
                            />
                          </template>
                          <template v-else-if="column.dataIndex === 'actions'">
                            <Popconfirm
                              v-if="!isReadonly"
                              title="确认删除该指标？"
                              @confirm="form.items.splice(index, 1)"
                            >
                              <Button danger size="small" type="link">
                                <IconifyIcon icon="lucide:trash-2" />
                              </Button>
                            </Popconfirm>
                          </template>
                          <template
                            v-else-if="
                              !isReadonly &&
                              [
                                'groupSort',
                                'groupMaxScore',
                                'indicatorSort',
                                'maxScore',
                              ].includes(String(column.dataIndex))
                            "
                          >
                            <InputNumber
                              v-model:value="record[column.dataIndex]"
                              class="w-full"
                              :min="0"
                            />
                          </template>
                          <template v-else-if="!isReadonly">
                            <Input v-model:value="record[column.dataIndex]" />
                          </template>
                          <template v-else>
                            {{ record[column.dataIndex] || '-' }}
                          </template>
                        </template>
                      </Table>
                    </section>

              </div>
            </div>
          </div>
        </div>
      </Spin>
    </DetailModal>

    <Drawer
      v-model:open="versionHistoryOpen"
      placement="right"
      title="版本历史"
      :width="760"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <Table
        :columns="[
          { title: '版本', dataIndex: 'versionNo', width: 90 },
          { title: '状态', dataIndex: 'status', width: 110 },
          { title: '变更说明', dataIndex: 'changeSummary' },
          { title: '提交人/时间', dataIndex: 'submitter', width: 180 },
          { title: '审核人/意见', dataIndex: 'auditor', width: 210 },
          { title: '发布时间', dataIndex: 'publishTime', width: 165 },
          { title: '操作', dataIndex: 'action', width: 95 },
        ]"
        :data-source="detail?.versions || []"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 1050 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <Tag :color="versionStatusColor(record.status)">
              {{ versionStatusText(record.status) }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'submitter'">
            {{ record.submitterName || '-' }}<br />
            {{ record.submitTime || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'auditor'">
            {{ record.auditorName || '-' }}<br />
            {{ record.auditOpinion || '-' }}
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <Button type="link" @click="switchVersion(record.id)">
              查看指标
            </Button>
          </template>
        </template>
      </Table>
    </Drawer>

    <Drawer
      v-model:open="operationLogOpen"
      placement="right"
      title="操作日志"
      :width="620"
      :z-index="SRM_NESTED_MODAL_Z_INDEX"
    >
      <Table
        :columns="[
          { title: '时间', dataIndex: 'createTime', width: 170 },
          { title: '操作人', dataIndex: 'operatorName', width: 120 },
          { title: '动作', dataIndex: 'action', width: 120 },
          { title: '说明', dataIndex: 'actionDescription' },
        ]"
        :data-source="detail?.logs || []"
        :pagination="false"
        row-key="id"
        size="small"
        :scroll="{ x: 700 }"
      />
    </Drawer>
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

.srm-detail-section {
  margin-top: 12px;
}

.srm-nowrap-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.srm-inline-picker {
  display: grid;
  min-width: 190px;
  align-items: center;
  gap: 4px;
  grid-template-columns: minmax(0, 1fr) 32px;
}

.srm-inline-picker :deep(.ant-input) {
  overflow: hidden;
  text-overflow: ellipsis;
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

.srm-veto-operator {
  width: 68px;
}

.srm-veto-score {
  width: 82px;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}

</style>
