<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmSampleEvaluationProjectApi } from '#/api/mes/srm/sample-evaluation-project';
import type { SystemUserApi } from '#/api/system/user';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Modal as AntModal,
  Button,
  Checkbox,
  Input,
  InputNumber,
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
  updateProject,
} from '#/api/mes/srm/sample-evaluation-project';
import { UserSelectModal } from '#/views/system/user/components';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmStandardSampleEvaluationProject' });

type DetailMode = 'create' | 'detail' | 'edit';

interface DeptOption {
  deptCode: string;
  deptName: string;
  label: string;
  value: string;
}

interface DeptConfigRow {
  canAssign: boolean;
  canInitiate: boolean;
  deptCode?: string;
  deptName?: string;
  remark?: string;
  rowKey: string;
  sortNo: number;
  userIds: number[];
  userNameMap: Record<number, string>;
}

type ProjectForm = SrmSampleEvaluationProjectApi.Project & {
  departmentRows: DeptConfigRow[];
};

const SRM_NESTED_MODAL_Z_INDEX = 5600;
const SRM_NESTED_DROPDOWN_Z_INDEX = SRM_NESTED_MODAL_Z_INDEX + 10;

const deptOptions: DeptOption[] = [
  { deptCode: 'TECH', deptName: '技术部', label: '技术部', value: 'TECH' },
  {
    deptCode: 'PRODUCTION',
    deptName: '生产部',
    label: '生产部',
    value: 'PRODUCTION',
  },
  {
    deptCode: 'QUALITY',
    deptName: '品质部',
    label: '品质部',
    value: 'QUALITY',
  },
  {
    deptCode: 'WAREHOUSE',
    deptName: '仓管部',
    label: '仓管部',
    value: 'WAREHOUSE',
  },
  {
    deptCode: 'PURCHASE',
    deptName: '采购部',
    label: '采购部',
    value: 'PURCHASE',
  },
];

const detailMode = ref<DetailMode>('detail');
const saving = ref(false);
const basicExpanded = ref(true);
const form = reactive<ProjectForm>(createEmptyForm());
const selectedDeptRow = ref<DeptConfigRow>();
let rowSeq = 0;

const isReadonly = computed(() => detailMode.value === 'detail');
const subtitleItems = computed(() =>
  [
    form.projectCode ? `编码 ${form.projectCode}` : '',
    form.projectName || '',
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
    minWidth: 240,
    slots: { default: 'projectName' },
    title: '项目名称',
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

const deptColumns: TableColumnsType = [
  { align: 'center', dataIndex: 'sortNo', title: '排序', width: 90 },
  { dataIndex: 'deptName', title: '部门', width: 180 },
  { dataIndex: 'users', title: '责任人', width: 340 },
  { align: 'center', dataIndex: 'canInitiate', title: '可发起', width: 100 },
  { align: 'center', dataIndex: 'canAssign', title: '可分配', width: 100 },
  { dataIndex: 'remark', title: '备注', width: 260 },
  {
    align: 'center',
    dataIndex: 'actions',
    fixed: 'right',
    title: '操作',
    width: 90,
  },
];

const deptTableScrollX = computed(() =>
  deptColumns.reduce((sum, column) => sum + Number(column.width || 120), 0),
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
  } as VxeTableGridOptions<SrmSampleEvaluationProjectApi.Project>,
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
    departmentRows: [],
    projectCode: '',
    projectName: '',
    status: 'ENABLED',
  };
}

function createRowKey() {
  rowSeq += 1;
  return `sample-evaluation-project-dept-${Date.now()}-${rowSeq}`;
}

function resetForm(record: Partial<ProjectForm> = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, createEmptyForm(), record, {
    departmentRows:
      record.departmentRows || buildDefaultDepartmentRows(record.users || []),
  });
}

async function openCreate() {
  resetForm({
    departmentRows: buildDefaultDepartmentRows([]),
  });
  detailMode.value = 'create';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openDetail(row: SrmSampleEvaluationProjectApi.Project) {
  if (!row.id) {
    return;
  }
  const result = await getProjectDetail(row.id);
  applyProjectDetail(result);
  detailMode.value = 'detail';
  basicExpanded.value = true;
  detailModalApi.open();
}

async function openEdit(row: SrmSampleEvaluationProjectApi.Project) {
  await openDetail(row);
  detailMode.value = 'edit';
}

function switchToEdit() {
  detailMode.value = 'edit';
}

async function closeDetail() {
  selectedDeptRow.value = undefined;
  await detailModalApi.close();
}

function applyProjectDetail(result: SrmSampleEvaluationProjectApi.Project) {
  resetForm({
    ...result,
    departmentRows: groupUserConfigs(result.users || []),
  });
}

async function saveProjectConfig(closeAfter = false) {
  if (!form.projectCode?.trim() || !form.projectName?.trim()) {
    message.warning('请填写项目编码和项目名称');
    return;
  }
  const users = buildSaveUsers();
  if (form.status !== 'DISABLED' && users.length === 0) {
    message.warning('启用项目至少需要维护一个部门责任人');
    return;
  }
  if (form.status !== 'DISABLED' && !users.some((user) => user.canInitiate)) {
    message.warning('启用项目至少需要一个允许发起的责任人');
    return;
  }
  saving.value = true;
  try {
    const payload: SrmSampleEvaluationProjectApi.Project = {
      id: form.id,
      projectCode: form.projectCode.trim(),
      projectName: form.projectName.trim(),
      remark: form.remark,
      status: (form.status ||
        'ENABLED') as SrmSampleEvaluationProjectApi.ProjectStatus,
      users,
      version: form.version,
    };
    const projectId = form.id || (await createProject(payload));
    if (form.id) {
      await updateProject(payload);
    }
    const detail = await getProjectDetail(projectId);
    applyProjectDetail(detail);
    detailMode.value = 'edit';
    message.success('样品评价项目配置已保存');
    await gridApi.query();
    if (closeAfter) {
      await closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

function addDepartmentRow() {
  const usedDeptCodes = new Set(
    form.departmentRows.map((row) => row.deptCode).filter(Boolean),
  );
  const option =
    deptOptions.find((item) => !usedDeptCodes.has(item.deptCode)) ||
    deptOptions[0];
  form.departmentRows.push(
    createDepartmentRow(option, form.departmentRows.length),
  );
}

function removeDepartmentRow(row: DeptConfigRow) {
  form.departmentRows = form.departmentRows.filter(
    (item) => item.rowKey !== row.rowKey,
  );
}

function handleDeptChange(row: DeptConfigRow, value: string) {
  const option = deptOptions.find((item) => item.deptCode === value);
  row.deptCode = option?.deptCode || value;
  row.deptName = option?.deptName || value;
}

function chooseUsers(row: DeptConfigRow) {
  selectedDeptRow.value = row;
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: row.userIds,
    })
    .open();
}

function handleUserConfirm(users: SystemUserApi.User[]) {
  const row = selectedDeptRow.value;
  if (!row) {
    return;
  }
  const validUsers = users.filter(
    (user): user is SystemUserApi.User & { id: number } =>
      normalizeId(user.id) !== undefined,
  );
  row.userIds = mergeIds(validUsers.map((user) => user.id));
  row.userNameMap = {};
  validUsers.forEach((user) => {
    row.userNameMap[user.id] =
      user.nickname || user.username || String(user.id);
  });
  selectedDeptRow.value = undefined;
}

function handleDelete(row: SrmSampleEvaluationProjectApi.Project) {
  AntModal.confirm({
    content: `确认删除项目 ${row.projectName || row.projectCode} 吗？已被样品评价表引用的项目不能删除。`,
    okButtonProps: { danger: true },
    okText: '删除',
    async onOk() {
      await deleteProject(row.id!);
      message.success('项目已删除');
      await gridApi.query();
    },
    title: '删除样品评价项目',
  });
}

function buildDefaultDepartmentRows(
  users: SrmSampleEvaluationProjectApi.UserConfig[],
) {
  const groupedRows = groupUserConfigs(users);
  if (groupedRows.length > 0) {
    return groupedRows;
  }
  return deptOptions.map((option, index) => createDepartmentRow(option, index));
}

function groupUserConfigs(users: SrmSampleEvaluationProjectApi.UserConfig[]) {
  const rowMap = new Map<string, DeptConfigRow>();
  [...users]
    .toSorted(
      (left, right) => Number(left.sortNo || 0) - Number(right.sortNo || 0),
    )
    .forEach((user, index) => {
      const deptName = sanitizeText(user.deptName);
      if (!deptName || !user.userId) {
        return;
      }
      const deptCode = user.deptCode || inferDeptCode(deptName);
      const key = deptCode || deptName;
      let row = rowMap.get(key);
      if (!row) {
        row = {
          canAssign: Boolean(user.canAssign),
          canInitiate: Boolean(user.canInitiate),
          deptCode,
          deptName,
          remark: user.remark,
          rowKey: createRowKey(),
          sortNo: user.sortNo || (index + 1) * 10,
          userIds: [],
          userNameMap: {},
        };
        rowMap.set(key, row);
      }
      row.canAssign = row.canAssign || Boolean(user.canAssign);
      row.canInitiate = row.canInitiate || Boolean(user.canInitiate);
      row.userIds = mergeIds(row.userIds, [user.userId]);
      row.userNameMap[user.userId] = user.userName || String(user.userId);
    });
  return [...rowMap.values()];
}

function createDepartmentRow(option: DeptOption, index: number): DeptConfigRow {
  const canLead =
    option.deptCode === 'TECH' || option.deptCode === 'PRODUCTION';
  return {
    canAssign: canLead,
    canInitiate: canLead,
    deptCode: option.deptCode,
    deptName: option.deptName,
    rowKey: createRowKey(),
    sortNo: (index + 1) * 10,
    userIds: [],
    userNameMap: {},
  };
}

function buildSaveUsers(): SrmSampleEvaluationProjectApi.UserConfig[] {
  return form.departmentRows.flatMap((row) =>
    row.userIds.map((userId, index) => ({
      canAssign: Boolean(row.canAssign),
      canInitiate: Boolean(row.canInitiate),
      deptCode: row.deptCode,
      deptName: row.deptName,
      remark: row.remark,
      sortNo: Number(row.sortNo || 0) + index,
      userId,
      userName: row.userNameMap[userId],
    })),
  );
}

function displayUsers(row: DeptConfigRow) {
  return row.userIds.map((id) => row.userNameMap[id] || String(id)).join('、');
}

function getCellValue(record: Record<string, any>, dataIndex: unknown) {
  return typeof dataIndex === 'string' ? record[dataIndex] : undefined;
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

function mergeIds(...groups: number[][]) {
  const result: number[] = [];
  groups.flat().forEach((id) => {
    if (Number.isFinite(id) && id > 0 && !result.includes(id)) {
      result.push(id);
    }
  });
  return result;
}

function sanitizeText(value: unknown) {
  return String(value ?? '').trim();
}

function inferDeptCode(deptName?: string) {
  return deptOptions.find((item) => item.deptName === deptName)?.deptCode;
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

function statusColor(status?: string) {
  return status === 'DISABLED' ? 'error' : 'success';
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

function resolveNestedPopupContainer(triggerNode: HTMLElement) {
  return triggerNode.parentElement || triggerNode;
}
</script>

<template>
  <Page auto-content-height>
    <UserModal title="选择责任人" @confirm="handleUserConfirm" />

    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">样品评价项目配置</div>
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
              <div class="qms-ncr-title-panel__name">样品评价项目配置</div>
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
                    <div class="erp-form-item">
                      <label class="erp-form-label">更新时间</label>
                      <div class="erp-form-value">
                        <span class="qms-exception-readonly-value">
                          {{ form.updateTime || '-' }}
                        </span>
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
                  </div>
                </section>

                <section class="erp-basic-form srm-detail-section">
                  <div class="detail-list-head">
                    <div class="detail-list-title">
                      <strong>
                        部门责任人（{{ form.departmentRows.length }} 项）
                      </strong>
                    </div>
                    <Space v-if="!isReadonly" :size="8">
                      <Button @click="addDepartmentRow">
                        <IconifyIcon icon="lucide:plus" />
                        新增部门
                      </Button>
                      <Button type="primary" @click="saveProjectConfig(false)">
                        <IconifyIcon icon="lucide:users-round" />
                        保存配置
                      </Button>
                    </Space>
                  </div>
                  <Table
                    :columns="deptColumns"
                    :data-source="form.departmentRows"
                    :pagination="false"
                    row-key="rowKey"
                    :scroll="{ x: deptTableScrollX, y: 430 }"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.dataIndex === 'sortNo'">
                        <InputNumber
                          v-if="!isReadonly"
                          v-model:value="record.sortNo"
                          :min="1"
                          :precision="0"
                          class="srm-sort-input"
                        />
                        <span v-else>{{ record.sortNo || '-' }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'deptName'">
                        <Select
                          v-if="!isReadonly"
                          v-model:value="record.deptCode"
                          :dropdown-style="{
                            zIndex: SRM_NESTED_DROPDOWN_Z_INDEX,
                          }"
                          :get-popup-container="resolveNestedPopupContainer"
                          :options="deptOptions"
                          placeholder="请选择部门"
                          @change="(value) => handleDeptChange(record, value)"
                        />
                        <span v-else>{{ record.deptName || '-' }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'users'">
                        <div
                          v-if="!isReadonly"
                          class="srm-inline-picker"
                          @click="chooseUsers(record)"
                        >
                          <Input
                            :value="displayUsers(record)"
                            placeholder="点击选择责任人"
                            readonly
                          />
                          <Button size="small" type="text">
                            <IconifyIcon icon="lucide:users-round" />
                          </Button>
                        </div>
                        <span v-else>{{ displayUsers(record) || '-' }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'canInitiate'">
                        <Checkbox
                          v-if="!isReadonly"
                          v-model:checked="record.canInitiate"
                        />
                        <Tag
                          v-else
                          :color="record.canInitiate ? 'success' : 'default'"
                          class="!m-0"
                        >
                          {{ record.canInitiate ? '是' : '否' }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'canAssign'">
                        <Checkbox
                          v-if="!isReadonly"
                          v-model:checked="record.canAssign"
                        />
                        <Tag
                          v-else
                          :color="record.canAssign ? 'success' : 'default'"
                          class="!m-0"
                        >
                          {{ record.canAssign ? '是' : '否' }}
                        </Tag>
                      </template>
                      <template v-else-if="column.dataIndex === 'remark'">
                        <Input
                          v-if="!isReadonly"
                          v-model:value="record.remark"
                          placeholder="请输入备注"
                        />
                        <span v-else>{{ record.remark || '-' }}</span>
                      </template>
                      <template v-else-if="column.dataIndex === 'actions'">
                        <Button
                          v-if="!isReadonly"
                          danger
                          size="small"
                          type="link"
                          @click="removeDepartmentRow(record)"
                        >
                          <IconifyIcon icon="lucide:trash-2" />
                        </Button>
                      </template>
                      <template v-else>
                        {{ getCellValue(record, column.dataIndex) || '-' }}
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
  min-width: 220px;
  align-items: center;
  gap: 4px;
  grid-template-columns: minmax(0, 1fr) 32px;
}

.srm-inline-picker :deep(.ant-input) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.srm-sort-input {
  width: 72px;
}

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>
