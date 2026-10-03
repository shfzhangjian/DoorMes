<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

import { h, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { Button, Checkbox, Input, message, Modal, Tag } from 'ant-design-vue';

import { ACTION_ICON, TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import {
  getExceptionPage,
  withdrawExceptionEvent,
} from '#/api/mes/quality/abnormal/exception';
import { DictTag } from '#/components/dict-tag';

import { QMS_EXCEPTION_DICT, useGridColumns, useGridFormSchema } from './data';
import DetailModalForm from './modules/detail-modal.vue';

defineOptions({ name: 'MesException' });

const mineScope = ref<string[]>([]);
const advancedSearchVisible = ref(false);
const route = useRoute();
const openedRouteKey = ref('');
const legacyRouteTabType = ref<string>();
const tabKeys = [
  'todo',
  'initiated',
  'processed',
  'monitor',
  'containmentOverdue',
];
const mineScopeOptions = [
  { label: '待我处理的', value: 'pendingMine' },
  { label: '我发现的', value: 'discoveredMine' },
  { label: '我参与的', value: 'participatedMine' },
];

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'qms-exception-vben-grid',
  gridClass: 'qms-exception-vxe-grid',
  formOptions: {
    schema: useGridFormSchema(false),
    collapsed: false,
    showCollapseButton: false,
  },
  gridOptions: {
    columns: useGridColumns(),
    height: '100%',
    keepSource: true,
    rowConfig: { keyField: 'id', isHover: true },
    toolbarConfig: { search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) =>
          await getExceptionPage({
            pageNo: page.currentPage,
            pageSize: page.pageSize,
            ...formValues,
            ...getLegacyRouteTabParams(),
            ...getMineScopeParams(),
          }),
      },
    },
  } as VxeTableGridOptions<any>,
});

function getLegacyRouteTabParams() {
  if (mineScope.value.length > 0 || !legacyRouteTabType.value) {
    return {};
  }
  return { tabType: legacyRouteTabType.value };
}

function getMineScopeParams() {
  return {
    discoveredMine: mineScope.value.includes('discoveredMine') || undefined,
    participatedMine: mineScope.value.includes('participatedMine') || undefined,
    pendingMine: mineScope.value.includes('pendingMine') || undefined,
  };
}

function handleMineScopeChange() {
  legacyRouteTabType.value = undefined;
  gridApi.query();
}

function handleView(row?: any) {
  detailModalApi.setData(row?.id ? { ...row } : { isNew: true }).open();
}

function handleWithdraw(row: any) {
  let reason = '';
  Modal.confirm({
    title: `撤回修改 ${row.exceptionNo || ''}`,
    width: 520,
    icon: h(IconifyIcon, {
      class: 'text-orange-500',
      icon: 'lucide:circle-alert',
    }),
    content: h('div', { class: 'space-y-3' }, [
      h(
        'div',
        { class: 'rounded bg-amber-50 p-3 text-sm text-amber-700' },
        '撤回后当前办理人的待办会取消，单据回到上一环节，可修改后重新提交。若下一环节已办理，系统会阻止撤回。',
      ),
      h(Input.TextArea, {
        maxlength: 500,
        placeholder: '请输入撤回原因',
        rows: 4,
        showCount: true,
        'onUpdate:value': (value: string) => {
          reason = value;
        },
      }),
    ]),
    okText: '确认撤回',
    cancelText: '取消',
    async onOk() {
      if (!reason.trim()) {
        message.warning('请输入撤回原因');
        throw new Error('withdraw reason required');
      }
      await withdrawExceptionEvent({ id: row.id, reason: reason.trim() });
      message.success('撤回成功');
      await gridApi.query();
    },
  });
}

function parseDictValues(value?: string) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

async function openRouteDetail() {
  const routeId = Number(route.query.id || route.query.exceptionId);
  const exceptionNoParam = route.query.exceptionNo;
  const exceptionNo = Array.isArray(exceptionNoParam)
    ? exceptionNoParam[0]?.trim()
    : typeof exceptionNoParam === 'string'
      ? exceptionNoParam.trim()
      : '';
  const routeTabType =
    typeof route.query.tabType === 'string' && tabKeys.includes(route.query.tabType)
      ? route.query.tabType
      : undefined;
  if (legacyRouteTabType.value !== routeTabType) {
    legacyRouteTabType.value = routeTabType;
    await nextTick();
    gridApi.query();
  }
  const routeKey =
    Number.isFinite(routeId) && routeId > 0
      ? `id:${routeId}:${routeTabType || ''}`
      : exceptionNo
        ? `no:${exceptionNo}:${routeTabType || ''}`
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
  const page = await getExceptionPage({
    pageNo: 1,
    pageSize: 1,
    exceptionNo,
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
  await gridApi.formApi.setValues({ exceptionNo });
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

function getSlaStatus(row: any) {
  if (row.status === 'CLOSED' || row.status === 'CANCELLED') {
    return { color: 'success', text: '已结束' };
  }
  if (!row.containmentDeadline) {
    return { color: 'default', text: '-' };
  }
  const deadline = new Date(row.containmentDeadline).getTime();
  const diffHours = Math.floor((deadline - Date.now()) / 3_600_000);
  if (diffHours < 0) {
    return { color: 'error', text: `超期 ${Math.abs(diffHours)} 小时` };
  }
  if (diffHours < 12) {
    return { color: 'warning', text: `剩 ${diffHours} 小时` };
  }
  return { color: 'processing', text: `剩 ${diffHours} 小时` };
}

function buildRowActions(row: any) {
  const canHandle = row.canHandle === true || row.currentUserTaskTodo === true;
  const isTaskTodo = canHandle && row.currentUserTaskTodo === true;
  const actions: any[] = [
    {
      label:
        row.listActionName ||
        (isTaskTodo
          ? row.currentUserTaskTodoLabel || '处理子任务'
          : canHandle
            ? '办理'
            : '详情'),
      type: 'link' as const,
      icon: isTaskTodo
        ? 'lucide:upload-cloud'
        : canHandle
          ? ACTION_ICON.EDIT
          : ACTION_ICON.PREVIEW,
      onClick: () => handleView(row),
    },
  ];
  if (row.canWithdraw === true) {
    actions.push({
      label: '撤回修改',
      type: 'link' as const,
      danger: true,
      icon: 'lucide:rotate-ccw',
      onClick: () => handleWithdraw(row),
    });
  }
  return actions;
}

onMounted(() => void openRouteDetail());
watch(() => route.query.id, () => void openRouteDetail());
watch(() => route.query.exceptionId, () => void openRouteDetail());
watch(() => route.query.exceptionNo, () => void openRouteDetail());
watch(() => route.query.tabType, () => void openRouteDetail());
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="gridApi.query()" />
    <div class="qms-exception-page">
      <div class="qms-exception-content">
        <div class="qms-exception-grid-host">
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
                <TableAction
                  :actions="[
                    {
                      label: '提报异常事件',
                      type: 'primary',
                      icon: ACTION_ICON.ADD,
                      onClick: () => handleView(),
                    },
                  ]"
                />
              </div>
            </template>

            <template #exceptionNo="{ row }">
              <a
                class="font-mono font-bold text-blue-700 hover:underline"
                @click="handleView(row)"
              >
                {{ row.exceptionNo }}
              </a>
            </template>

            <template #exceptionType="{ row }">
              <div class="flex flex-wrap gap-1">
                <DictTag
                  v-for="value in parseDictValues(row.exceptionType)"
                  :key="value"
                  :type="QMS_EXCEPTION_DICT.type"
                  :value="value"
                />
                <span v-if="parseDictValues(row.exceptionType).length === 0">-</span>
              </div>
            </template>

            <template #exceptionLevel="{ row }">
              <DictTag
                :type="QMS_EXCEPTION_DICT.level"
                :value="row.exceptionLevel"
              />
            </template>

            <template #slaStatus="{ row }">
              <Tag
                :color="getSlaStatus(row).color"
                class="w-28 text-center font-mono"
              >
                <IconifyIcon
                  v-if="getSlaStatus(row).color === 'error'"
                  icon="lucide:alarm-clock-off"
                  class="mr-1 inline"
                />
                {{ getSlaStatus(row).text }}
              </Tag>
            </template>

            <template #currentNodeName="{ row }">
              <Tag color="processing" class="!m-0">
                {{ row.currentNodeName || '-' }}
              </Tag>
            </template>

            <template #currentUserTaskTodo="{ row }">
              <Tag v-if="row.currentUserTaskTodo" color="blue" class="!m-0">
                {{ row.currentUserTaskTodoLabel || '处理子任务' }}
                <span v-if="row.currentUserTaskTodoCount > 1">
                  ({{ row.currentUserTaskTodoCount }})
                </span>
              </Tag>
              <span v-else>-</span>
            </template>

            <template #status="{ row }">
              <DictTag :type="QMS_EXCEPTION_DICT.status" :value="row.status" />
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
.qms-exception-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #fff;
}

.qms-exception-content {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.qms-exception-grid-host {
  position: absolute;
  inset: 0;
  min-height: 0;
  overflow: hidden;
}

.qms-exception-grid-host :deep(.qms-exception-vben-grid) {
  height: 100% !important;
  max-height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.qms-exception-vxe-grid) {
  display: grid !important;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto auto !important;
  height: 100% !important;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.vxe-grid--table-wrapper) {
  grid-row: 4;
  min-height: 0 !important;
  overflow: hidden !important;
}

.qms-exception-grid-host :deep(.vxe-grid--pager-wrapper) {
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
