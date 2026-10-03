<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { SrmTrialValidationApi } from '#/api/mes/srm/trial-validation';

import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import { Page, useVbenModal } from '@vben/common-ui';

import { Button, Space, Tag } from 'ant-design-vue';

import { TableAction, useVbenVxeGrid } from '#/adapter/vxe-table';
import { getTrialValidationPage } from '#/api/mes/srm/trial-validation';

import TrialValidationDetailModal from './modules/detail-modal.vue';

defineOptions({ name: 'SrmCertificationTrialValidation' });

const statusMetaMap: Record<string, { color: string; text: string }> = {
  ARCHIVE_CONFIRM: { color: 'warning', text: '确认归档' },
  ARCHIVED: { color: 'success', text: '已归档' },
  NOTICE_SENT: { color: 'blue', text: '发起试生产通知' },
  PRODUCTION_COMPLETE: { color: 'processing', text: '完成生产' },
  TRIAL_EXECUTION: { color: 'processing', text: '试生产执行' },
};

const route = useRoute();
const router = useRouter();
const openedEntryKey = ref('');

const [Grid, gridApi] = useVbenVxeGrid({
  formOptions: {
    collapsed: false,
    schema: [
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入跟踪单号' },
        fieldName: 'trialNo',
        label: '跟踪单号',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '供应商名称模糊搜索' },
        fieldName: 'supplierName',
        label: '供应商名称',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料编码' },
        fieldName: 'materialCode',
        label: '物料编码',
      },
      {
        component: 'Input',
        componentProps: { allowClear: true, placeholder: '请输入物料名称' },
        fieldName: 'materialName',
        label: '物料名称',
      },
      {
        component: 'Select',
        componentProps: {
          allowClear: true,
          options: Object.entries(statusMetaMap).map(([value, meta]) => ({
            label: meta.text,
            value,
          })),
          placeholder: '请选择当前状态',
        },
        fieldName: 'status',
        label: '当前状态',
      },
    ],
  },
  gridOptions: {
    columns: [
      { align: 'center', fixed: 'left', title: '序号', type: 'seq', width: 60 },
      {
        field: 'trialNo',
        fixed: 'left',
        minWidth: 190,
        slots: { default: 'trialNo' },
        title: '跟踪单号',
      },
      {
        field: 'sourceSampleEvaluationNo',
        minWidth: 190,
        title: '来源样品评价单',
      },
      { field: 'supplierCode', minWidth: 150, title: '供应商编码' },
      { field: 'supplierName', minWidth: 210, title: '供应商名称' },
      { field: 'materialCode', minWidth: 150, title: '物料编码' },
      { field: 'materialName', minWidth: 190, title: '物料名称' },
      { field: 'materialModel', minWidth: 160, title: '规格型号' },
      { field: 'materialBatchNo', minWidth: 150, title: '物料批号' },
      { align: 'right', field: 'quantity', minWidth: 110, title: '数量' },
      {
        align: 'center',
        field: 'status',
        minWidth: 150,
        slots: { default: 'status' },
        title: '当前状态',
      },
      { field: 'currentNodeName', minWidth: 140, title: '当前节点' },
      { field: 'initiatorUserName', minWidth: 120, title: '发起人' },
      { field: 'noticeTime', minWidth: 170, title: '下达时间' },
      {
        fixed: 'right',
        slots: { default: 'actions' },
        title: '操作',
        width: 126,
      },
    ],
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20 },
    rowConfig: { isHover: true, keyField: 'id' },
    toolbarConfig: { refresh: true, search: true },
    proxyConfig: {
      ajax: {
        query: async ({ page }, formValues) => {
          const result = await getTrialValidationPage({
            ...formValues,
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
  } as VxeTableGridOptions<SrmTrialValidationApi.TrialValidation>,
});

const [TrialValidationDetailModalHost, trialValidationDetailModalApi] =
  useVbenModal({
    connectedComponent: TrialValidationDetailModal,
    destroyOnClose: true,
  });

function openDetail(row: SrmTrialValidationApi.TrialValidation) {
  if (!row.id) {
    return;
  }
  trialValidationDetailModalApi.setData({ id: row.id }).open();
}

async function handleModalSuccess() {
  await gridApi.query();
}

function statusText(status?: string) {
  return statusMetaMap[String(status || '')]?.text || status || '-';
}

function statusColor(status?: string) {
  return statusMetaMap[String(status || '')]?.color || 'default';
}

async function openEntryDetail() {
  const rawBusinessKey = firstText(
    route.query.id,
    route.query.trialValidationId,
    route.query.businessKey,
    route.query.bizId,
  );
  const trialId = normalizeId(rawBusinessKey);
  const entryKey = trialId ? `id:${trialId}` : '';
  if (!entryKey || openedEntryKey.value === entryKey) {
    return;
  }
  openedEntryKey.value = entryKey;
  trialValidationDetailModalApi.setData({ id: trialId }).open();
}

function clearEntryQuery() {
  if (
    route.query.id ||
    route.query.trialValidationId ||
    route.query.businessKey ||
    route.query.bizId
  ) {
    void router.replace({ path: route.path, query: {} });
  }
}

function firstText(...values: unknown[]) {
  return values.map((item) => String(item || '').trim()).find(Boolean) || '';
}

function normalizeId(value: unknown) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const id = Number(value);
  return Number.isFinite(id) && id > 0 ? id : undefined;
}

onMounted(() => void openEntryDetail());
watch(
  () => route.query.id,
  () => void openEntryDetail(),
);
watch(
  () => route.query.trialValidationId,
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
</script>

<template>
  <Page auto-content-height>
    <TrialValidationDetailModalHost
      @close="clearEntryQuery"
      @success="handleModalSuccess"
    />
    <Grid>
      <template #toolbar-tools>
        <Space :size="8">
          <span class="srm-trial-list-title">试产验证跟踪</span>
        </Space>
      </template>
      <template #trialNo="{ row }">
        <Button type="link" class="srm-trial-link" @click="openDetail(row)">
          {{ row.trialNo || '-' }}
        </Button>
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
              icon: 'lucide:file-search',
              label: '查看',
              onClick: openDetail.bind(null, row),
              type: 'link',
            },
          ]"
        />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.srm-trial-list-title {
  display: inline-block;
  min-width: 180px;
  color: #10233d;
  font-size: 15px;
  font-weight: 700;
}

.srm-trial-link {
  padding: 0;
}
</style>
