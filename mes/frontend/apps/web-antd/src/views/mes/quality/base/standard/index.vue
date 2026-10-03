<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesQualityStandardApi } from '#/api/mes/quality/base/standard';

import { computed, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useAccess } from '@vben/access';
import { confirm, Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart, isEmpty } from '@vben/utils';
import {
  Button,
  Dropdown,
  Input,
  Menu,
  message,
  Modal,
  Popconfirm,
  RadioButton,
  RadioGroup,
  Tag,
} from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  auditStandard,
  deleteStandard,
  deleteStandardList,
  exportStandard,
  getStandardPage,
} from '#/api/mes/quality/base/standard';
import { $t } from '#/locales';

import {
  isUniversalIqcStandard,
  resolveStandardMenuContext,
  useGridColumns,
  useGridFormSchema,
} from './data';
import FormComponent from './modules/form.vue';

defineOptions({ name: 'MesQualityStandard' });

const [FormModal, formModalApi] = useVbenModal({
  connectedComponent: FormComponent,
  destroyOnClose: true,
});
type AuditResult = 'PASS' | 'REJECT';

const { hasAccessByCodes } = useAccess();
const AUDIT_RESULT_PASS: AuditResult = 'PASS';
const AUDIT_RESULT_REJECT: AuditResult = 'REJECT';
const AUDIT_STATUS_AUDITED = 20;
const route = useRoute();
const auditModalOpen = ref(false);
const auditSubmitting = ref(false);
const currentAuditRow = ref<MesQualityStandardApi.Standard>();
const auditForm = reactive<{
  auditResult: AuditResult;
  rejectReason: string;
}>({
  auditResult: AUDIT_RESULT_PASS,
  rejectReason: '',
});
const standardContext = computed(() => resolveStandardMenuContext(route));
const fixedApplyType = computed(() => standardContext.value?.applyType);
const tableTitle = computed(
  () => `${standardContext.value?.title || '检验标准定义'}列表`,
);
const standardFileName = computed(
  () => `${standardContext.value?.title || '检验标准定义'}.xls`,
);

function getPermission(
  action: 'audit' | 'create' | 'delete' | 'export' | 'list' | 'update',
) {
  return standardContext.value
    ? `${standardContext.value.permissionPrefix}:${action}`
    : `mes:quality-standard:${action}`;
}

function getAuditPermissions() {
  return [getPermission('audit')];
}

function hasActionPermission(
  action: 'audit' | 'create' | 'delete' | 'export' | 'list' | 'update',
) {
  return hasAccessByCodes([getPermission(action)]);
}

function hasMoreActionPermission() {
  return hasActionPermission('list') || hasActionPermission('delete');
}

function handleRefresh() {
  gridApi.query();
}

function openFormModal(
  type: 'copy' | 'create' | 'detail' | 'edit',
  row?: MesQualityStandardApi.Standard,
) {
  formModalApi
    .setData({
      type,
      id: row?.id,
      standardContext: standardContext.value,
    })
    .open();
}

function handleCreate() {
  openFormModal('create');
}
function handleEdit(row: MesQualityStandardApi.Standard) {
  openFormModal('edit', row);
}
function handleCopy(row: MesQualityStandardApi.Standard) {
  openFormModal('copy', row);
}
function handleDetail(row: MesQualityStandardApi.Standard) {
  openFormModal('detail', row);
}

async function handleDeleteBatch() {
  await confirm('确认要删除选中的检验标准吗？');
  const hideLoading = message.loading({ content: '删除中...', duration: 0 });
  try {
    await deleteStandardList(checkedIds.value, fixedApplyType.value);
    checkedIds.value = [];
    message.success('删除成功');
    handleRefresh();
  } finally {
    hideLoading();
  }
}

async function handleDelete(row: MesQualityStandardApi.Standard) {
  await deleteStandard(row.id!, fixedApplyType.value);
  message.success('删除成功');
  handleRefresh();
}

function isAudited(row: MesQualityStandardApi.Standard) {
  return row.auditStatus === AUDIT_STATUS_AUDITED;
}

function iqcScopeColor(row: MesQualityStandardApi.Standard) {
  return isUniversalIqcStandard(row, standardContext.value) ? 'purple' : 'blue';
}

function iqcScopeLabel(row: MesQualityStandardApi.Standard) {
  return isUniversalIqcStandard(row, standardContext.value)
    ? '通用标准'
    : '物料专用';
}

async function handleAudit(row: MesQualityStandardApi.Standard) {
  if (!row.id || isAudited(row)) return;
  currentAuditRow.value = row;
  auditForm.auditResult = AUDIT_RESULT_PASS;
  auditForm.rejectReason = '';
  auditModalOpen.value = true;
}

function handleAuditCancel() {
  if (auditSubmitting.value) return;
  auditModalOpen.value = false;
}

async function handleAuditSubmit() {
  const row = currentAuditRow.value;
  if (!row?.id) return;
  const rejectReason = auditForm.rejectReason.trim();
  if (auditForm.auditResult === AUDIT_RESULT_REJECT && !rejectReason) {
    message.warning('驳回原因不能为空');
    return;
  }
  const hideLoading = message.loading({ content: '审核中...', duration: 0 });
  auditSubmitting.value = true;
  try {
    await auditStandard(
      {
        id: row.id,
        auditResult: auditForm.auditResult,
        rejectReason:
          auditForm.auditResult === AUDIT_RESULT_REJECT
            ? rejectReason
            : undefined,
      },
      fixedApplyType.value,
    );
    message.success(
      auditForm.auditResult === AUDIT_RESULT_PASS
        ? '审核通过'
        : '已驳回并恢复修改前数据',
    );
    auditModalOpen.value = false;
    handleRefresh();
  } finally {
    auditSubmitting.value = false;
    hideLoading();
  }
}

async function handleExport() {
  const data = await exportStandard(
    await gridApi.formApi.getValues(),
    fixedApplyType.value,
  );
  downloadFileFromBlobPart({ fileName: standardFileName.value, source: data });
}

const checkedIds = ref<number[]>([]);
function handleRowCheckboxChange({
  records,
}: {
  records: MesQualityStandardApi.Standard[];
}) {
  checkedIds.value = records.map((item) => item.id!);
}

const [BaseGrid, gridApi] = useVbenVxeGrid({
  formOptions: { schema: useGridFormSchema(standardContext.value) },
  gridOptions: {
    columns: useGridColumns(standardContext.value),
    height: 'auto',
    keepSource: true,
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getStandardPage(
            {
              pageNo: page.currentPage,
              pageSize: page.pageSize,
              ...formValues,
            },
            fixedApplyType.value,
          ),
      },
    },
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { refresh: true, search: true },
  } as VxeTableGridOptions<MesQualityStandardApi.Standard>,
  gridEvents: {
    checkboxAll: handleRowCheckboxChange,
    checkboxChange: handleRowCheckboxChange,
  },
});
</script>

<template>
  <Page auto-content-height>
    <FormModal @success="handleRefresh" />
    <Modal
      v-model:open="auditModalOpen"
      cancel-text="取消"
      :confirm-loading="auditSubmitting"
      destroy-on-close
      :mask-closable="!auditSubmitting"
      :ok-text="
        auditForm.auditResult === AUDIT_RESULT_PASS ? '审核通过' : '确认驳回'
      "
      title="审核检验标准"
      @cancel="handleAuditCancel"
      @ok="handleAuditSubmit"
    >
      <div class="space-y-4 py-1">
        <RadioGroup v-model:value="auditForm.auditResult" button-style="solid">
          <RadioButton :value="AUDIT_RESULT_PASS">通过</RadioButton>
          <RadioButton :value="AUDIT_RESULT_REJECT">驳回</RadioButton>
        </RadioGroup>
        <div
          v-if="auditForm.auditResult === AUDIT_RESULT_PASS"
          class="rounded border border-green-100 bg-green-50 px-3 py-2 text-sm text-green-700"
        >
          审核通过后将记录当前审核人和审核时间。
        </div>
        <div v-else>
          <div class="mb-2 text-sm font-medium text-gray-700">驳回原因</div>
          <Input.TextArea
            v-model:value="auditForm.rejectReason"
            :maxlength="300"
            placeholder="请填写驳回原因，系统将恢复到本次修改前的数据"
            :rows="4"
            show-count
          />
        </div>
      </div>
    </Modal>
    <BaseGrid :table-title="tableTitle">
      <template #toolbar-tools>
        <TableAction
          class="ledger-toolbar-actions"
          :actions="[
            {
              label: '新增标准',
              type: 'primary',
              icon: ACTION_ICON.ADD,
              auth: [getPermission('create')],
              onClick: handleCreate,
            },
            {
              label: $t('ui.actionTitle.export'),
              type: 'primary',
              icon: ACTION_ICON.DOWNLOAD,
              auth: [getPermission('export')],
              onClick: handleExport,
            },
            {
              label: '批量删除',
              type: 'primary',
              danger: true,
              icon: ACTION_ICON.DELETE,
              auth: [getPermission('delete')],
              disabled: isEmpty(checkedIds),
              onClick: handleDeleteBatch,
            },
          ]"
        />
      </template>

      <template #applyType="{ row }">
        <Tag color="blue" v-if="row.applyType === 'IQC'">进料 (IQC)</Tag>
        <Tag color="geekblue" v-else-if="row.applyType === 'FAI'"
          >首件 (FAI)</Tag
        >
        <Tag color="orange" v-else-if="row.applyType === 'GLUE_BOARD_FAI'"
          >胶板 (GLUE_BOARD_FAI)</Tag
        >
        <Tag color="cyan" v-else-if="row.applyType === 'IPQC'">过程 (IPQC)</Tag>
        <Tag color="purple" v-else-if="row.applyType === 'FQC'">成品 (FQC)</Tag>
        <Tag color="green" v-else-if="row.applyType === 'OQC'">出货 (OQC)</Tag>
      </template>

      <template #status="{ row }">
        <Tag :color="row.status === 1 ? 'success' : 'error'">{{
          row.status === 1 ? '启用' : '停用'
        }}</Tag>
      </template>

      <template #auditStatus="{ row }">
        <Tag :color="isAudited(row) ? 'success' : 'default'">
          {{ isAudited(row) ? '已审核' : '未审核' }}
        </Tag>
      </template>

      <template #iqcScope="{ row }">
        <Tag :color="iqcScopeColor(row)" class="!m-0">
          {{ iqcScopeLabel(row) }}
        </Tag>
      </template>

      <template #actions="{ row }">
        <div class="quality-standard-row-actions">
          <TableAction
            :actions="[
              {
                label: $t('common.edit'),
                type: 'link',
                icon: ACTION_ICON.EDIT,
                auth: [getPermission('update')],
                onClick: handleEdit.bind(null, row),
              },
              {
                label: '复制',
                type: 'link',
                icon: ACTION_ICON.COPY,
                auth: [getPermission('create')],
                onClick: handleCopy.bind(null, row),
              },
              {
                label: '审核',
                type: 'link',
                icon: ACTION_ICON.AUDIT,
                auth: getAuditPermissions(),
                ifShow: !isAudited(row),
                tooltip: '审核该检验标准',
                onClick: handleAudit.bind(null, row),
              },
            ]"
          />
          <Dropdown
            v-if="hasMoreActionPermission()"
            :trigger="['hover']"
            placement="bottomRight"
          >
            <Button class="quality-standard-more-button" type="link">
              <span>更多</span>
              <IconifyIcon :icon="ACTION_ICON.MORE" />
            </Button>
            <template #overlay>
              <Menu class="quality-standard-more-menu">
                <Menu.Item
                  v-if="hasActionPermission('list')"
                  key="detail"
                  @click="handleDetail(row)"
                >
                  <div class="quality-standard-more-menu__item">
                    <IconifyIcon :icon="ACTION_ICON.VIEW" />
                    <span>详情</span>
                  </div>
                </Menu.Item>
                <Menu.Item v-if="hasActionPermission('delete')" key="delete">
                  <Popconfirm title="确认删除?" @confirm="handleDelete(row)">
                    <div
                      class="quality-standard-more-menu__item quality-standard-more-menu__item--danger"
                    >
                      <IconifyIcon :icon="ACTION_ICON.DELETE" />
                      <span>删除</span>
                    </div>
                  </Popconfirm>
                </Menu.Item>
              </Menu>
            </template>
          </Dropdown>
        </div>
      </template>
    </BaseGrid>
  </Page>
</template>

<style scoped>
.ledger-toolbar-actions {
  display: inline-flex;
  flex-wrap: wrap;
}

.ledger-toolbar-actions :deep(.ant-space) {
  column-gap: 12px !important;
  row-gap: 8px;
  flex-wrap: wrap;
}

.ledger-toolbar-actions :deep(.ant-space-item) {
  display: inline-flex;
}

.quality-standard-row-actions {
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
}

.quality-standard-more-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  white-space: nowrap;
}

.quality-standard-more-menu {
  min-width: 92px;
}

.quality-standard-more-menu__item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 20px;
  white-space: nowrap;
}

.quality-standard-more-menu__item--danger {
  color: var(--ant-color-error);
}
</style>
