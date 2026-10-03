<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { downloadFileFromBlobPart } from '@vben/utils';

import { Button, Checkbox, Input, message, Modal, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  exportNcrExcel,
  getProductNcrRestorePreview,
  getNcrPage,
  restoreProductNcrRecord,
  withdrawNcr,
} from '#/api/mes/quality/abnormal/ncr';
import { DictTag } from '#/components/dict-tag';

import { QMS_NCR_DICT, useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/detail-modal.vue';
import ClosedCorrectionForm from './modules/closed-correction-modal.vue';

defineOptions({ name: 'MesQualityNcr' });

const mineScope = ref<string[]>([]);
const advancedSearchVisible = ref(false);
const restorePermission = 'mes:qms-nc-record:restore';
const route = useRoute();
const openedRouteKey = ref('');
const mineScopeOptions = [
  { label: '待我处理的', value: 'pendingMine' },
  { label: '我发现的', value: 'discoveredMine' },
  { label: '我参与的', value: 'participatedMine' },
];

const [ClosedCorrectionModal, closedCorrectionApi] = useVbenModal({
  connectedComponent: ClosedCorrectionForm,
  destroyOnClose: true,
});

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-ncr-vben-grid',
  gridClass: 'qms-ncr-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(false),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns().map((column) =>
      column.slots?.default === 'actions' ? { ...column, width: 260 } : column,
    ),
    height: '100%',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getNcrPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            ...getMineScopeParams(),
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

const toolbarActions = computed(() => [
  {
    label: '新建',
    type: 'primary',
    icon: ACTION_ICON.ADD,
    onClick: () => handleView(),
  },
  {
    label: '导出',
    type: 'link',
    icon: ACTION_ICON.DOWNLOAD,
    onClick: handleExport,
  },
]);

function getMineScopeParams() {
  return {
    discoveredMine: mineScope.value.includes('discoveredMine') || undefined,
    participatedMine: mineScope.value.includes('participatedMine') || undefined,
    pendingMine: mineScope.value.includes('pendingMine') || undefined,
  };
}

function handleMineScopeChange() {
  gridApi.query();
}

function handleView(row?: any) {
  detailModalApi.setData(row?.id ? { ...row } : { isNew: true }).open();
}

function handleRestore(row: any) {
  const restoreForm = reactive<{
    confirmNcNo: string;
    loadingPreview: boolean;
    previewWarnings: string[];
    reason: string;
  }>({ confirmNcNo: '', loadingPreview: true, previewWarnings: [], reason: '' });
  const ncNo = String(row.ncNo || '').trim();
  void getProductNcrRestorePreview(Number(row.id))
    .then((preview) => {
      restoreForm.previewWarnings = preview.warnings || [];
    })
    .catch(() => {
      restoreForm.previewWarnings = [
        '关联单据核查失败；请确认后仍按强制还原执行，后台会删除当前 NCR 并尝试解除关联。',
      ];
    })
    .finally(() => {
      restoreForm.loadingPreview = false;
    });
  Modal.confirm({
    title: `还原产品处置单 ${ncNo}`,
    width: 600,
    okText: '确认还原',
    cancelText: '取消',
    okButtonProps: { danger: true },
    content: () =>
      h('div', { class: 'space-y-3 pt-2' }, [
        h(
          'div',
          { class: 'rounded bg-red-50 p-3 text-red-600' },
          '还原后将删除本产品 NCR、独立流程实例、会签、处置范围、工作台指令和报工门禁，并释放原产品异常来源重新生成 NCR；原检验和 NG 数据不会删除。',
        ),
        h(
          'div',
          { class: 'text-gray-500' },
          '若已关联异常事件、重新发起 NCR、已生效工作台指令/报工门禁或已办理 8D，系统会在下方提醒；确认还原后会解除关联并继续删除当前 NCR。',
        ),
        h(
          'div',
          {
            class: restoreForm.loadingPreview
              ? 'rounded bg-gray-50 p-3 text-gray-500'
              : restoreForm.previewWarnings.length > 0
                ? 'rounded bg-amber-50 p-3 text-amber-700'
                : 'rounded bg-green-50 p-3 text-green-700',
          },
          restoreForm.loadingPreview
            ? '正在核查关联单据变化...'
            : restoreForm.previewWarnings.length > 0
              ? [
                  h('div', { class: 'mb-2 font-medium' }, '已检测到以下关联变化，确认后会继续还原：'),
                  h(
                    'ul',
                    { class: 'list-disc pl-5' },
                    restoreForm.previewWarnings.map((item) => h('li', item)),
                  ),
                ]
              : '未检测到额外关联变化，可按当前确认信息继续还原。',
        ),
        h('div', { class: 'font-medium' }, `请输入单号 ${ncNo} 进行确认`),
        h(Input, {
          value: restoreForm.confirmNcNo,
          placeholder: ncNo,
          'onUpdate:value': (value: string) => {
            restoreForm.confirmNcNo = value;
          },
        }),
        h('div', { class: 'font-medium' }, '还原原因（必填）'),
        h(Input.TextArea, {
          value: restoreForm.reason,
          maxlength: 500,
          rows: 3,
          showCount: true,
          placeholder: '请填写误生成、测试复位等具体原因，系统将永久留存审计记录',
          'onUpdate:value': (value: string) => {
            restoreForm.reason = value;
          },
        }),
      ]),
    async onOk() {
      if (restoreForm.loadingPreview) {
        message.warning('请等待关联单据核查完成');
        throw new Error('Restore preview is loading');
      }
      if (restoreForm.confirmNcNo.trim() !== ncNo) {
        message.error('确认单号与当前 NCR 单号不一致');
        throw new Error('NCR confirmation mismatch');
      }
      if (!restoreForm.reason.trim()) {
        message.error('请填写还原原因');
        throw new Error('Restore reason is required');
      }
      await restoreProductNcrRecord({
        id: Number(row.id),
        confirmNcNo: restoreForm.confirmNcNo.trim(),
        reason: restoreForm.reason.trim(),
      });
      message.success(`${ncNo} 已还原，原产品异常来源可重新生成 NCR`);
      await gridApi.query();
    },
  });
}

function handleWithdraw(row: any) {
  const withdrawForm = reactive({ reason: '' });
  const ncNo = String(row.ncNo || '').trim();
  Modal.confirm({
    title: `撤回修改 ${ncNo}`,
    width: 520,
    okText: '确认撤回',
    cancelText: '取消',
    content: () =>
      h('div', { class: 'space-y-3 pt-2' }, [
        h(
          'div',
          { class: 'rounded bg-amber-50 p-3 text-amber-700' },
          '撤回后当前办理人的待办会取消，单据回到上一环节，可修改后重新提交。若下一环节已办理，系统会阻止撤回。',
        ),
        h('div', { class: 'font-medium' }, '撤回原因（必填）'),
        h(Input.TextArea, {
          value: withdrawForm.reason,
          maxlength: 500,
          rows: 3,
          showCount: true,
          placeholder: '请填写本次撤回修改的原因，例如内容填错、办理人选错等',
          'onUpdate:value': (value: string) => {
            withdrawForm.reason = value;
          },
        }),
      ]),
    async onOk() {
      if (!withdrawForm.reason.trim()) {
        message.error('请填写撤回原因');
        throw new Error('Withdraw reason is required');
      }
      await withdrawNcr({
        id: Number(row.id),
        reason: withdrawForm.reason.trim(),
      });
      message.success(`${ncNo} 已撤回，可重新编辑提交`);
      await gridApi.query();
    },
  });
}

async function openRouteDetail() {
  const routeId = Number(route.query.id || route.query.ncrId);
  const routeTabType =
    typeof route.query.tabType === 'string' ? route.query.tabType : undefined;
  const ncNoParam = route.query.ncNo;
  const ncNo = Array.isArray(ncNoParam)
    ? ncNoParam[0]?.trim()
    : typeof ncNoParam === 'string'
      ? ncNoParam.trim()
      : '';
  const routeKey =
    Number.isFinite(routeId) && routeId > 0
      ? `id:${routeId}`
      : ncNo
        ? `no:${ncNo}`
        : '';
  if (!routeKey || openedRouteKey.value === routeKey) {
    return;
  }
  openedRouteKey.value = routeKey;
  if (Number.isFinite(routeId) && routeId > 0) {
    detailModalApi
      .setData({
        id: routeId,
        ...(routeTabType ? { tabType: routeTabType } : {}),
      })
      .open();
    return;
  }
  const page = await getNcrPage({
    pageNo: 1,
    pageSize: 1,
    ncNo,
    ...(routeTabType ? { tabType: routeTabType } : {}),
  });
  const row = page.list?.[0];
  if (row?.id) {
    detailModalApi
      .setData({
        id: row.id,
        ...(routeTabType ? { tabType: routeTabType } : {}),
      })
      .open();
    return;
  }
  await gridApi.formApi.setValues({ ncNo });
  await gridApi.query();
}

function toggleAdvancedSearch() {
  advancedSearchVisible.value = !advancedSearchVisible.value;
  gridApi.formApi.setState((prev) => ({
    ...prev,
    schema: useGridFormSchema(advancedSearchVisible.value),
  }));
  nextTick(() => gridApi.grid?.recalculate?.(true));
}

async function handleExport() {
  const data = await exportNcrExcel({
    ...(await gridApi.formApi.getValues()),
    ...getMineScopeParams(),
  });
  downloadFileFromBlobPart({ fileName: 'NCR不合格处置台账.xls', source: data });
}

function buildRowActions(row: any) {
  const canHandle = row.canHandle === true;
  const actions: Array<Record<string, any>> = [
    {
      label: row.listActionName || (canHandle ? '办理' : '详情'),
      type: 'link' as const,
      icon: canHandle ? ACTION_ICON.EDIT : ACTION_ICON.PREVIEW,
      onClick: () => handleView(row),
    },
  ];
  if (row.sourceType !== 'RAW_MATERIAL' && row.sourceBizType && row.sourceId) {
    if (row.canWithdraw === true) {
      actions.push({
        label: '撤回修改',
        type: 'link' as const,
        danger: true,
        icon: ACTION_ICON.REFRESH,
        onClick: () => handleWithdraw(row),
      });
    }
    actions.push({
      label: '还原',
      type: 'link' as const,
      danger: true,
      icon: ACTION_ICON.REFRESH,
      auth: [restorePermission],
      onClick: () => handleRestore(row),
    });
  }
  if (row.status === 'CLOSED' && row.sourceType !== 'RAW_MATERIAL') {
    actions.push({
      label: '修改', type: 'link', icon: ACTION_ICON.EDIT,
      onClick: () => closedCorrectionApi.setData({ id: row.id }).open(),
    });
  }
  return actions;
}

function getNodeColor(row: any) {
  if (row.status === 'CLOSED') {
    return 'success';
  }
  if (row.status === 'RETURNED' || row.status === 'CANCELLED') {
    return 'error';
  }
  if (row.status === 'PENDING_STOCK_DISPOSE') {
    return 'warning';
  }
  return 'processing';
}

function isPendingDisposition(value?: string) {
  return value?.toUpperCase() === 'PENDING';
}

onMounted(() => void openRouteDetail());
watch(() => route.query.id, () => void openRouteDetail());
watch(() => route.query.ncrId, () => void openRouteDetail());
watch(() => route.query.ncNo, () => void openRouteDetail());
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />
    <ClosedCorrectionModal @success="gridApi.query()" />
    <div class="qms-ncr-page">
      <div class="qms-ncr-content">
        <div class="qms-ncr-grid-host">
          <Grid>
            <template #toolbar-actions>
              <div class="qms-mine-scope">
                <Checkbox.Group
                  v-model:value="mineScope"
                  :options="mineScopeOptions"
                  @change="handleMineScopeChange"
                />
              </div>
            </template>

            <template #toolbar-tools>
              <div class="flex items-center gap-2">
                <Button type="link" class="px-1" @click="toggleAdvancedSearch">
                  <IconifyIcon
                    :icon="
                      advancedSearchVisible
                        ? 'lucide:chevron-up'
                        : 'lucide:chevron-down'
                    "
                    class="mr-1"
                  />
                  {{ advancedSearchVisible ? '收起' : '展开' }}
                </Button>
                <TableAction :actions="toolbarActions" />
              </div>
            </template>

            <template #ncNo="{ row }">
              <a
                class="font-mono font-bold text-blue-700 hover:underline"
                @click="handleView(row)"
              >
                {{ row.ncNo }}
              </a>
            </template>

            <template #sourceType="{ row }">
              <Tag v-if="row.sourceTypeName" color="blue" class="!m-0">
                {{ row.sourceTypeName }}
              </Tag>
              <DictTag
                v-else
                :type="QMS_NCR_DICT.sourceType"
                :value="row.sourceType"
              />
            </template>

            <template #materialName="{ row }">
              <div class="leading-tight">
                <div class="font-bold text-slate-800">
                  {{ row.materialName || '-' }}
                </div>
                <div class="mt-1 text-xs text-slate-400">
                  {{ row.materialCode || '-' }} / {{ row.specification || '-' }}
                </div>
              </div>
            </template>

            <template #ncLevel="{ row }">
              <Tag v-if="row.ncLevelName" color="processing" class="!m-0">
                {{ row.ncLevelName }}
              </Tag>
              <DictTag v-else :type="QMS_NCR_DICT.level" :value="row.ncLevel" />
            </template>

            <template #currentNodeName="{ row }">
              <Tag :color="getNodeColor(row)" class="!m-0">
                {{ row.currentNodeName || '-' }}
              </Tag>
            </template>

            <template #status="{ row }">
              <DictTag :type="QMS_NCR_DICT.status" :value="row.status" />
            </template>

            <template #finalDisposition="{ row }">
              <span
                v-if="isPendingDisposition(row.finalDisposition || row.mrbDecision)"
              >
                待处置
              </span>
              <DictTag
                v-else
                :type="QMS_NCR_DICT.disposition"
                :value="row.finalDisposition || row.mrbDecision"
              />
            </template>

            <template #actions="{ row }">
              <TableAction :actions="buildRowActions(row)" />
            </template>
          </Grid>
        </div>
      </div>
    </div>
  </Page>
</template>

<style scoped>
.qms-ncr-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-ncr-content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.qms-ncr-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-ncr-grid-host :deep(.qms-ncr-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-ncr-grid-host :deep(.qms-ncr-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-ncr-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-ncr-grid-host :deep(.vxe-grid--pager-wrapper) {
  grid-row: 6;
  background: #fff;
}

.qms-mine-scope {
  display: flex;
  min-height: 32px;
  align-items: center;
}

.qms-mine-scope :deep(.ant-checkbox-group) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.qms-mine-scope :deep(.ant-checkbox-wrapper) {
  margin-inline-start: 0;
  color: #475569;
  font-weight: 500;
}
</style>
