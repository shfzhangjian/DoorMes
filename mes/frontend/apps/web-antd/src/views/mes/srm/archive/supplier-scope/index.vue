<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SystemUserApi } from '#/api/system/user';
import type { SrmSupplierScopeApi } from '#/api/mes/srm/supplier-scope';

import { computed, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal as AntModal,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  createSupplierScope,
  deleteSupplierScope,
  getSupplierScope,
  getSupplierScopePage,
  updateSupplierScope,
} from '#/api/mes/srm/supplier-scope';
import UserSelectModal from '#/views/system/user/components/select-modal.vue';

import '../../shared/srm-erp-detail.css';

defineOptions({ name: 'SrmCertificationSupplierScope' });

const SRM_NESTED_MODAL_Z_INDEX = 5600;

const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];
const permissionOptions = [
  { label: '脱敏查看', value: 'MASKED' },
  { label: '查看全部字段', value: 'FULL' },
  { label: '可以编辑', value: 'EDIT' },
];
const permissionLabelMap: Record<string, string> = {
  EDIT: '可以编辑',
  FULL: '查看全部字段',
  MASKED: '脱敏查看',
};
const permissionColorMap: Record<string, string> = {
  EDIT: 'success',
  FULL: 'processing',
  MASKED: 'default',
};

const detailMode = ref<'create' | 'detail' | 'edit'>('detail');
const saving = ref(false);
const form = reactive<SrmSupplierScopeApi.Scope>({
  members: [],
  sort: 0,
  status: 'ENABLED',
});

const isReadonly = computed(() => detailMode.value === 'detail');
const pageTitle = computed(() =>
  detailMode.value === 'create'
    ? '新增名录管理范围'
    : detailMode.value === 'edit'
      ? '编辑名录管理范围'
      : '查看名录管理范围',
);

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入范围编号' },
        fieldName: 'scopeCode',
        label: '范围编号',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入分组名称' },
        fieldName: 'scopeName',
        label: '分组名称',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: statusOptions,
          placeholder: '请选择状态',
        },
        fieldName: 'status',
        label: '状态',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      { field: 'scopeCode', minWidth: 190, title: '范围编号', slots: { default: 'scopeCode' } },
      { field: 'scopeName', minWidth: 220, title: '分组名称', slots: { default: 'scopeName' } },
      { align: 'center', field: 'memberCount', minWidth: 120, title: '成员数量' },
      { align: 'center', field: 'status', minWidth: 100, title: '状态', slots: { default: 'status' } },
      { align: 'right', field: 'sort', minWidth: 90, title: '排序' },
      { field: 'remark', minWidth: 260, title: '备注' },
      { fixed: 'right', slots: { default: 'actions' }, title: '操作', width: 140 },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getSupplierScopePage({
            ...(formValues || {}),
            pageNo: page?.currentPage || 1,
            pageSize: page?.pageSize || 20,
          });
          return {
            list: result.list || [],
            total: result.total || 0,
          };
        },
      },
    },
  } as VxeTableGridOptions<SrmSupplierScopeApi.Scope>,
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

function resetForm(record: SrmSupplierScopeApi.Scope = {}) {
  Object.keys(form).forEach((key) => delete (form as Record<string, any>)[key]);
  Object.assign(form, {
    members: [],
    sort: 0,
    status: 'ENABLED',
    ...record,
    members: record.members || [],
  });
}

function openCreate() {
  resetForm();
  detailMode.value = 'create';
  detailModalApi.open();
}

async function openDetail(row: SrmSupplierScopeApi.Scope) {
  const result = await getSupplierScope(Number(row.id));
  resetForm(result);
  detailMode.value = 'detail';
  detailModalApi.open();
}

async function openEdit(row: SrmSupplierScopeApi.Scope) {
  const result = await getSupplierScope(Number(row.id));
  resetForm(result);
  detailMode.value = 'edit';
  detailModalApi.open();
}

function switchToEdit() {
  detailMode.value = 'edit';
}

function closeDetail() {
  detailModalApi.close();
}

function openUserPicker() {
  if (isReadonly.value) {
    return;
  }
  userModalApi
    .setData({
      maximized: true,
      modalZIndex: SRM_NESTED_MODAL_Z_INDEX,
      multiple: true,
      userIds: (form.members || []).map((member) => member.userId).filter(Boolean),
    })
    .open();
}

function handleUserConfirm(users: SystemUserApi.User[]) {
  const currentMembers = form.members || [];
  const memberMap = new Map<number, SrmSupplierScopeApi.Member>();
  currentMembers.forEach((member) => {
    if (member.userId) {
      memberMap.set(member.userId, member);
    }
  });
  users
    .filter((user) => user.id !== undefined)
    .forEach((user) => {
      const userId = Number(user.id);
      if (!memberMap.has(userId)) {
        memberMap.set(userId, {
          permissionLevel: 'MASKED',
          userId,
          userName: user.nickname || user.username || String(user.id),
        });
      }
    });
  form.members = [...memberMap.values()];
}

function removeMember(index: number) {
  form.members = (form.members || []).filter((_, itemIndex) => itemIndex !== index);
}

async function saveScope(closeAfter = false) {
  if (!form.scopeName?.trim()) {
    message.warning('请填写分组名称');
    return;
  }
  saving.value = true;
  try {
    if (detailMode.value === 'create') {
      const id = await createSupplierScope(form);
      const result = await getSupplierScope(Number(id));
      resetForm(result);
      detailMode.value = 'edit';
      message.success('新增成功');
    } else {
      await updateSupplierScope(form);
      const result = await getSupplierScope(Number(form.id));
      resetForm(result);
      message.success('保存成功');
    }
    await gridApi.query();
    if (closeAfter) {
      closeDetail();
    }
  } finally {
    saving.value = false;
  }
}

function deleteScope(row: SrmSupplierScopeApi.Scope) {
  AntModal.confirm({
    content: `确认删除 ${row.scopeName || row.scopeCode}？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      await deleteSupplierScope(Number(row.id));
      message.success('删除成功');
      await gridApi.query();
    },
    title: '删除名录管理范围',
  });
}

function statusText(status?: string) {
  return status === 'DISABLED' ? '停用' : '启用';
}
</script>

<template>
  <Page auto-content-height>
    <UserModal title="选择成员" @confirm="handleUserConfirm" />
    <div class="srm-crud-page">
      <div class="srm-crud-page__header">
        <div class="srm-crud-page__title">供应商名录管理范围</div>
      </div>
      <div class="srm-crud-page__body">
        <Grid>
          <template #toolbar-tools>
            <TableAction
              :actions="[
                {
                  icon: ACTION_ICON.ADD,
                  label: '新增名录范围',
                  onClick: openCreate,
                  type: 'primary',
                },
              ]"
            />
          </template>
          <template #scopeCode="{ row }">
            <a class="srm-crud-link" @click="openDetail(row)">
              {{ row.scopeCode || '-' }}
            </a>
          </template>
          <template #scopeName="{ row }">
            <span class="srm-crud-cell">{{ row.scopeName || '-' }}</span>
          </template>
          <template #status="{ row }">
            <Tag :color="row.status === 'DISABLED' ? 'default' : 'success'" class="!m-0">
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
                  onClick: deleteScope.bind(null, row),
                  type: 'link',
                },
              ]"
            />
          </template>
        </Grid>
      </div>
    </div>

    <DetailModal>
      <div class="qms-ncr-detail">
        <div class="qms-ncr-toolbar">
          <div class="qms-ncr-toolbar__placeholder">
            <Button class="qms-ncr-toolbar-action" @click="closeDetail">
              <IconifyIcon icon="lucide:arrow-left" />
              返回
            </Button>
          </div>
          <div class="qms-ncr-title-panel">
            <div class="qms-ncr-title-panel__name">供应商名录管理范围</div>
            <div class="qms-ncr-title-panel__subtitle">
              <span class="qms-ncr-title-panel__subtitle-item">
                {{ form.scopeCode ? `编号 ${form.scopeCode}` : '新增' }}
              </span>
              <span class="qms-ncr-title-panel__subtitle-item">
                {{ pageTitle }}
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
              v-else
              class="qms-ncr-toolbar-action"
              :loading="saving"
              type="primary"
              @click="saveScope(true)"
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
                    <strong>{{ pageTitle }}</strong>
                  </div>
                </div>
                <div class="erp-form-grid">
                  <div class="erp-form-item">
                    <label class="erp-form-label">范围编号</label>
                    <div class="erp-form-value">
                      <span class="qms-exception-readonly-value">
                        {{ form.scopeCode || '保存后自动生成' }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">分组名称</label>
                    <div class="erp-form-value">
                      <Input
                        v-if="!isReadonly"
                        v-model:value="form.scopeName"
                        placeholder="请输入分组名称"
                      />
                      <span v-else class="qms-exception-readonly-value">
                        {{ form.scopeName || '-' }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">状态</label>
                    <div class="erp-form-value">
                      <Select
                        v-if="!isReadonly"
                        v-model:value="form.status"
                        :options="statusOptions"
                        placeholder="请选择状态"
                      />
                      <Tag v-else :color="form.status === 'DISABLED' ? 'default' : 'success'" class="!m-0">
                        {{ statusText(form.status) }}
                      </Tag>
                    </div>
                  </div>
                  <div class="erp-form-item">
                    <label class="erp-form-label">排序</label>
                    <div class="erp-form-value">
                      <InputNumber
                        v-if="!isReadonly"
                        v-model:value="form.sort"
                        class="w-full"
                        :min="0"
                        placeholder="请输入排序"
                      />
                      <span v-else class="qms-exception-readonly-value">
                        {{ form.sort ?? '-' }}
                      </span>
                    </div>
                  </div>
                  <div class="erp-form-item erp-form-item--full">
                    <label class="erp-form-label erp-form-label--tall">备注</label>
                    <div class="erp-form-value">
                      <Input.TextArea
                        v-if="!isReadonly"
                        v-model:value="form.remark"
                        :rows="3"
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
                    <strong>分组成员</strong>
                  </div>
                  <Space v-if="!isReadonly" :size="8">
                    <Button @click="openUserPicker">
                      <IconifyIcon icon="lucide:users-round" />
                      追加成员
                    </Button>
                  </Space>
                </div>
                <Table
                  :columns="[
                    { title: '成员', dataIndex: 'userName', width: 260 },
                    { title: '供应商权限', dataIndex: 'permissionLevel', width: 220 },
                    { title: '操作', dataIndex: 'actions', width: 90 },
                  ]"
                  :data-source="form.members || []"
                  :pagination="false"
                  row-key="userId"
                  size="small"
                >
                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.dataIndex === 'permissionLevel'">
                      <Select
                        v-if="!isReadonly"
                        v-model:value="record.permissionLevel"
                        :options="permissionOptions"
                      />
                      <Tag v-else :color="permissionColorMap[record.permissionLevel] || 'default'" class="!m-0">
                        {{ permissionLabelMap[record.permissionLevel] || record.permissionLevel || '-' }}
                      </Tag>
                    </template>
                    <template v-else-if="column.dataIndex === 'actions'">
                      <Button
                        v-if="!isReadonly"
                        danger
                        size="small"
                        type="link"
                        @click="removeMember(index)"
                      >
                        删除
                      </Button>
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

:deep(.vben-vxe-grid) {
  display: flex;
  height: 100%;
  flex-direction: column;
}
</style>
