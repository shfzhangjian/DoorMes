<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmPreliminaryProjectApi } from '#/api/mes/srm/preliminary-project';
import type { SrmEvaluationTemplateApi } from '#/api/mes/srm/standard/template';
import type { SystemUserApi } from '#/api/system/user';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Input,
  message,
  Select,
  Space,
  Spin,
  Table,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createProject,
  deleteProject,
  getProjectDetail,
  getProjectPage,
  getProjectScorerConfig,
  saveProjectScorerConfig,
  updateProject,
} from '#/api/mes/srm/preliminary-project';
import { getPublishedTemplateList } from '#/api/mes/srm/standard/template';
import { UserSelectModal } from '#/views/system/user/components';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmPreliminaryProjectConfig' });

type DetailMode = 'create' | 'detail' | 'edit';

type ProjectForm = SrmPreliminaryProjectApi.Project & {
  scorerItems: SrmPreliminaryProjectApi.ScorerConfigItem[];
  selectedTemplateVersionId?: number;
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const loadingConfig = ref(false);
const publishedTemplates = ref<SrmEvaluationTemplateApi.Template[]>([]);
const selectedScorerItem = ref<SrmPreliminaryProjectApi.ScorerConfigItem>();
const form = reactive<ProjectForm>(createEmptyForm());

const isReadonly = computed(() => detailMode.value === 'detail');
const templateOptions = computed(() =>
  publishedTemplates.value
    .map((template) => {
      const version = template.currentVersion;
      const versionId = normalizeId(template.currentVersionId || version?.id);
      if (!versionId) {
        return undefined;
      }
      return {
        label: [
          template.templateCode,
          template.templateName,
          template.currentVersionNo || version?.versionNo,
        ]
          .filter(Boolean)
          .join(' / '),
        template,
        value: versionId,
      };
    })
    .filter(
      (
        item,
      ): item is {
        label: string;
        template: SrmEvaluationTemplateApi.Template;
        value: number;
      } => !!item,
    ),
);

const selectedTemplateLabel = computed(() => {
  const selectedOption = templateOptions.value.find(
    (item) => item.value === form.selectedTemplateVersionId,
  );
  return selectedOption?.label || projectTemplateText(form);
});
const subtitleItems = computed(() =>
  [
    form.projectCode ? `编码 ${form.projectCode}` : '',
    form.projectName || '',
    selectedTemplateLabel.value,
    statusText(form.status),
    detailModeText(),
  ].filter(Boolean),
);

const gridFormSchema: VbenFormSchema[] = [
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入项目编码' },
    fieldName: 'projectCode',
    label: '项目编码',
  },
  {
    component: 'Input',
    componentProps: { allowClear: true, placeholder: '请输入项目名称' },
    fieldName: 'projectName',
    label: '项目名称',
  },
  {
    component: 'Select',
    componentProps: {
      allowClear: true,
      options: statusOptions(),
      placeholder: '请选择状态',
    },
    fieldName: 'status',
    label: '状态',
  },
];

const gridColumns: VxeTableGridOptions['columns'] = [
  { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
  {
    field: 'projectCode',
    fixed: 'left',
    minWidth: 180,
    slots: { default: 'projectCode' },
    title: '项目编码',
  },
  {
    field: 'projectName',
    minWidth: 260,
    slots: { default: 'projectName' },
    title: '项目名称',
  },
  {
    field: 'templateNameSnapshot',
    minWidth: 260,
    slots: { default: 'templateName' },
    title: '评估模板',
  },
  {
    align: 'center',
    field: 'status',
    minWidth: 100,
    slots: { default: 'status' },
    title: '状态',
  },
  { field: 'remark', minWidth: 360, title: '备注' },
  { align: 'center', field: 'updateTime', minWidth: 170, title: '更新时间' },
  {
    fixed: 'right',
    slots: { default: 'actions' },
    title: '操作',
    width: 138,
  },
];

const scorerColumns: TableColumnsType = [
  { dataIndex: 'groupCodeSnapshot', title: '维度编码', width: 120 },
  { dataIndex: 'groupNameSnapshot', title: '维度名称', width: 130 },
  { dataIndex: 'groupSort', title: '维度排序', width: 90 },
  {
    dataIndex: 'indicatorCodeSnapshot',
    title: '指标编码',
    width: 130,
  },
  {
    dataIndex: 'indicatorNameSnapshot',
    title: '评估指标',
    width: 240,
  },
  { dataIndex: 'indicatorSort', title: '指标排序', width: 90 },
  { dataIndex: 'defaultDeptNames', title: '默认评分部门', width: 150 },
  { dataIndex: 'scorerUserName', title: '默认评分人', width: 210 },
  { dataIndex: 'scorerCandidateUserNames', title: '候选人员', width: 300 },
  {
    align: 'center',
    dataIndex: 'actions',
    fixed: 'right',
    title: '操作',
    width: 90,
  },
];

const scorerTableScrollX = computed(() =>
  scorerColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
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
          const result = await getProjectPage({
            ...formValues,
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
  } as VxeTableGridOptions<SrmPreliminaryProjectApi.Project>,
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

const [UserModal, userModalApi] = useVbenModal({
  connectedComponent: UserSelectModal,
  destroyOnClose: true,
});

function createEmptyForm(): ProjectForm {
  return {
    projectCode: '',
    projectName: '',
    scorerItems: [],
    status: 'ENABLED',
  };
}

function resetForm(record: Partial<ProjectForm> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record, {
    scorerItems: record.scorerItems || [],
  });
}

async function openCreate() {
  await loadPublishedTemplates();
  resetForm();
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmPreliminaryProjectApi.Project) {
  if (!row.id) {
    return;
  }
  await loadPublishedTemplates();
  const result = await getProjectDetail(row.id);
  applyProjectDetail(result);
  detailMode.value = 'detail';
  basicExpanded.value = true;
  detailModalApi.open();
  if (normalizeId(form.selectedTemplateVersionId)) {
    await loadScorerConfig({ silent: true });
  }
}

async function openEdit(row: SrmPreliminaryProjectApi.Project) {
  await openDetail(row);
  detailMode.value = 'edit';
}

function switchToEdit() {
  detailMode.value = 'edit';
}

async function closeDetail() {
  selectedScorerItem.value = undefined;
  await detailModalApi.close();
}

function applyProjectDetail(result: SrmPreliminaryProjectApi.Project) {
  resetForm({
    ...result,
    scorerItems: normalizeScorerRows(result.scorerItems || []),
    selectedTemplateVersionId: normalizeId(result.currentTemplateVersionId),
  });
}

async function loadPublishedTemplates() {
  if (publishedTemplates.value.length > 0) {
    return;
  }
  publishedTemplates.value = await getPublishedTemplateList('PRELIMINARY');
}

async function saveProjectConfig(closeAfter = false) {
  const id = await saveProject();
  if (!id) {
    return;
  }
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (templateVersionId && form.scorerItems.length > 0) {
    await persistScorerConfig(id, templateVersionId);
    message.success('项目评分人配置已保存');
  } else if (templateVersionId) {
    await loadScorerConfig({ silent: true });
    message.success('项目已保存，请维护评分人配置');
  } else {
    message.success('项目已保存');
  }
  detailMode.value = 'edit';
  await gridApi.query();
  if (closeAfter) {
    await closeDetail();
  }
}

async function saveProject() {
  if (!form.projectCode?.trim() || !form.projectName?.trim()) {
    message.warning('请填写项目编码和项目名称');
    return undefined;
  }
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!templateVersionId) {
    message.warning('请选择已发布的选择初评模板');
    return undefined;
  }
  saving.value = true;
  try {
    const payload: SrmPreliminaryProjectApi.Project = {
      currentTemplateVersionId: templateVersionId,
      id: form.id,
      projectCode: form.projectCode.trim(),
      projectName: form.projectName.trim(),
      remark: form.remark,
      status: (form.status ||
        'ENABLED') as SrmPreliminaryProjectApi.ProjectStatus,
      version: form.version,
    };
    if (form.id) {
      await updateProject(payload);
    } else {
      form.id = await createProject(payload);
    }
    form.currentTemplateVersionId = templateVersionId;
    applySelectedTemplateSnapshot();
    return form.id;
  } finally {
    saving.value = false;
  }
}

async function handleTemplateChange(value: number) {
  form.selectedTemplateVersionId = value;
  applySelectedTemplateSnapshot();
  form.scorerItems = [];
  if (form.id) {
    await loadScorerConfig({ silent: true });
  }
}

async function loadScorerConfig(options: { silent?: boolean } = {}) {
  const projectId = normalizeId(form.id);
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!projectId) {
    if (!options.silent) {
      message.warning('请先保存项目基本信息');
    }
    return;
  }
  if (!templateVersionId) {
    if (!options.silent) {
      message.warning('请选择已发布的选择初评模板');
    }
    return;
  }
  loadingConfig.value = true;
  try {
    const result = await getProjectScorerConfig(projectId, templateVersionId);
    form.scorerItems = normalizeScorerRows(result.scorerItems || []);
  } finally {
    loadingConfig.value = false;
  }
}

async function saveScorerConfig() {
  const projectId = normalizeId(form.id) || (await saveProject());
  const templateVersionId = normalizeId(form.selectedTemplateVersionId);
  if (!projectId || !templateVersionId) {
    message.warning('请先保存项目并选择模板');
    return;
  }
  if (form.scorerItems.length === 0) {
    await loadScorerConfig();
  }
  await persistScorerConfig(projectId, templateVersionId);
  message.success('项目评分人配置已保存');
}

async function persistScorerConfig(
  projectId: number,
  templateVersionId: number,
) {
  await saveProjectScorerConfig({
    items: form.scorerItems.map((item) => ({
      scorerCandidateUserIds: getScorerCandidateIds(item),
      scorerUserId: normalizeId(item.scorerUserId),
      templateItemId: Number(item.templateItemId),
    })),
    projectId,
    templateVersionId,
  });
  form.currentTemplateVersionId = templateVersionId;
  applySelectedTemplateSnapshot();
  await loadScorerConfig({ silent: true });
}

function chooseScorer(item: SrmPreliminaryProjectApi.ScorerConfigItem) {
  selectedScorerItem.value = item;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: getScorerCandidateIds(item),
    })
    .open();
}

function handleUserConfirm(users: SystemUserApi.User[]) {
  const item = selectedScorerItem.value;
  const validUsers = users.filter(
    (user): user is SystemUserApi.User & { id: number } =>
      normalizeId(user.id) !== undefined,
  );
  if (!item || validUsers.length === 0) {
    return;
  }
  const currentScorerId = normalizeId(item.scorerUserId);
  const scorerUserId =
    currentScorerId &&
    validUsers.some((candidate) => candidate.id === currentScorerId)
      ? currentScorerId
      : validUsers[0]!.id;
  const candidateIds = mergeIds(
    getScorerCandidateIds(item),
    validUsers.map((user) => user.id),
  );
  applyScorerAssignment(item, scorerUserId, candidateIds, validUsers);
  selectedScorerItem.value = undefined;
}

function applyScorerAssignment(
  item: SrmPreliminaryProjectApi.ScorerConfigItem,
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
  const scorerName = optionMap.get(scorerUserId) || String(scorerUserId);
  item.scorerUserId = scorerUserId;
  item.scorerUserName = scorerName;
  item.scorerCandidateUserIds = candidateUserIds.join(',') || undefined;
  item.scorerCandidateUserNames =
    candidateUserIds.map((id) => optionMap.get(id) || String(id)).join('、') ||
    undefined;
}

function normalizeScorerRows(
  rows: SrmPreliminaryProjectApi.ScorerConfigItem[],
) {
  return rows.map((row) => {
    const ids = getScorerCandidateIds(row);
    return {
      ...row,
      scorerCandidateUserIds: ids.join(',') || row.scorerCandidateUserIds,
      scorerUserId: normalizeId(row.scorerUserId) || ids[0],
      scorerUserName:
        row.scorerUserName || splitNameList(row.scorerCandidateUserNames)[0],
    };
  });
}

function getScorerCandidateIds(
  item: Partial<SrmPreliminaryProjectApi.ScorerConfigItem>,
) {
  return mergeIds(
    parseIdList(item.scorerCandidateUserIds),
    item.scorerUserId ? [Number(item.scorerUserId)] : [],
  );
}

function getScorerCandidateOptions(
  item: Partial<SrmPreliminaryProjectApi.ScorerConfigItem>,
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

function displayProjectScorers(
  item: Partial<SrmPreliminaryProjectApi.ScorerConfigItem>,
) {
  const names = splitNameList(item.scorerCandidateUserNames);
  if (names.length > 0) {
    return names.join('、');
  }
  return item.scorerUserName || '';
}

function handleDelete(row: SrmPreliminaryProjectApi.Project) {
  AntModal.confirm({
    content: `确认删除项目 ${row.projectName || row.projectCode} 吗？已被初评单引用的项目不能删除。`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteProject(row.id!);
      message.success('项目已删除');
      await gridApi.query();
    },
    title: '删除初评项目',
  });
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
    .map((item) => sanitizeName(item))
    .filter(
      (item, index, array): item is string =>
        !!item && array.indexOf(item) === index,
    );
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

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) && numericValue > 0
    ? numericValue
    : undefined;
}

function sanitizeName(value: unknown) {
  const text = String(value ?? '').trim();
  return text && text !== '-' && text !== '0' ? text : '';
}

function displayValue(value?: unknown) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return String(value);
}

function getCellValue(record: Record<string, any>, dataIndex: unknown) {
  return typeof dataIndex === 'string' ? record[dataIndex] : undefined;
}

function statusOptions() {
  return [
    { label: '启用', value: 'ENABLED' },
    { label: '停用', value: 'DISABLED' },
  ];
}

function statusText(status?: string) {
  return status === 'DISABLED' ? '停用' : '启用';
}

function detailModeText() {
  if (detailMode.value === 'create') {
    return '新增';
  }
  if (detailMode.value === 'edit') {
    return '编辑';
  }
  return '明细';
}

function statusColor(status?: string) {
  return status === 'DISABLED' ? 'error' : 'success';
}

function projectTemplateText(
  project: Partial<SrmPreliminaryProjectApi.Project>,
) {
  return [
    project.templateCodeSnapshot,
    project.templateNameSnapshot,
    project.templateVersionNoSnapshot,
  ]
    .filter(Boolean)
    .join(' / ');
}

function applySelectedTemplateSnapshot() {
  const selectedOption = templateOptions.value.find(
    (item) => item.value === form.selectedTemplateVersionId,
  );
  const template = selectedOption?.template;
  form.currentTemplateId = normalizeId(template?.id);
  form.currentTemplateVersionId = normalizeId(selectedOption?.value);
  form.templateCodeSnapshot = template?.templateCode;
  form.templateNameSnapshot = template?.templateName;
  form.templateVersionNoSnapshot =
    template?.currentVersionNo || template?.currentVersion?.versionNo;
}

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}
</script>

<template>
  <Page auto-content-height>
    <UserModal title="选择评分人" @confirm="handleUserConfirm" />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">初评项目配置</div>
      </div>
      <div class="srm-crud-page__body">
        <Grid>
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.ADD,
                  label: '新增项目',
                  onClick: openCreate,
                  type: 'primary',
                },
              ]"
            />
          </template>

          <template #projectCode="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.projectCode || '-' }}
            </a>
          </template>
          <template #projectName="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.projectName || '-' }}
            </a>
          </template>
          <template #templateName="{ row }">
            <span class="srm-crud-cell">
              {{ projectTemplateText(row) || '-' }}
            </span>
          </template>
          <template #status="{ row }">
            <Tag :color="statusColor(row.status)" class="!m-0">
              {{ statusText(row.status) }}
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
                  label: '编辑',
                  onClick: openEdit.bind(null, row),
                  type: 'link',
                },
                {
                  danger: true,
                  icon: ACTION_ICON.DELETE,
                  label: '删除',
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
      <Spin :spinning="saving || loadingConfig" class="detail-spin">
        <div class="qms-ncr-detail">
          <div class="qms-ncr-toolbar">
            <div class="qms-ncr-toolbar__placeholder">
              <Button class="qms-ncr-toolbar-action" @click="closeDetail">
                <IconifyIcon icon="lucide:arrow-left" />
                返回
              </Button>
            </div>

            <div class="qms-ncr-title-panel">
              <div class="qms-ncr-title-panel__name">初评项目配置</div>
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
              <Button
                v-if="isReadonly"
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
                @click="saveProjectConfig(false)"
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
                      <strong>项目基本信息</strong>
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
                      <label class="erp-form-label">项目编码</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="form.projectCode"
                          placeholder="请输入项目编码"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.projectCode || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">项目名称</label>
                      <div class="erp-form-value">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="form.projectName"
                          placeholder="请输入项目名称"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ form.projectName || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">评估模板</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="form.selectedTemplateVersionId"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="templateOptions"
                          placeholder="请选择已发布的选择初评模板"
                          show-search
                          @change="handleTemplateChange"
                        />
                        <span v-else class="qms-exception-readonly-value">
                          {{ selectedTemplateLabel || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">项目状态</label>
                      <div class="erp-form-value">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="form.status"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="statusOptions()"
                        />
                        <Tag
                          v-else
                          :color="statusColor(form.status)"
                          class="!m-0"
                        >
                          {{ statusText(form.status) }}
                        </Tag>
                      </div>
                    </div>
                    <div class="erp-form-item erp-form-item--wide">
                      <label class="erp-form-label">备注</label>
                      <div class="erp-form-value">
                        <Input.TextArea
                          v-if="!isReadonly"
                          v-model:value="form.remark"
                          :rows="2"
                          placeholder="请输入备注"
                        />
                        <span
                          v-else
                          class="qms-exception-readonly-value qms-exception-readonly-value--multiline"
                        >
                          {{ form.remark || '-' }}
                        </span>
                      </div>
                    </div>
                    <div class="erp-form-item">
                      <label class="erp-form-label">更新时间</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.updateTime || '-' }}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>

                <section
                  v-if="form.id"
                  class="erp-basic-form srm-detail-section"
                >
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>
                        评分人明细（{{ form.scorerItems.length }} 项）
                      </strong>
                    </div>
                    <Space v-if="!isReadonly" :size="8">
                      <Button @click="loadScorerConfig()">
                        <IconifyIcon icon="lucide:refresh-cw" />
                        重新加载
                      </Button>
                      <Button type="primary" @click="saveScorerConfig">
                        <IconifyIcon icon="lucide:users-round" />
                        保存评分人
                      </Button>
                    </Space>
                  </div>
                  <Table
                    :columns="scorerColumns"
                    :data-source="form.scorerItems"
                    :pagination="false"
                    row-key="templateItemId"
                    :scroll="{ x: scorerTableScrollX, y: 430 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'scorerUserName'">
                        <div
                          v-if="!isReadonly"
                          class="srm-inline-picker"
                          @click="chooseScorer(record)"
                        >
                          <Input
                            :value="displayProjectScorers(record)"
                            placeholder="点击选择默认评分人"
                            readonly
                          />
                          <Button size="small" type="text">
                            <IconifyIcon icon="lucide:users-round" />
                          </Button>
                        </div>
                        <span v-else>
                          {{ displayProjectScorers(record) || '-' }}
                        </span>
                      </template>
                      <template v-else-if="column.dataIndex === 'actions'">
                        <Button
                          v-if="!isReadonly"
                          size="small"
                          type="link"
                          @click="chooseScorer(record)"
                        >
                          <IconifyIcon icon="lucide:users-round" />
                        </Button>
                      </template>
                      <template v-else>
                        {{
                          displayValue(getCellValue(record, column.dataIndex))
                        }}
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

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>
