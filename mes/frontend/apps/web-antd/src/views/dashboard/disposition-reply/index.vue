<script lang="ts" setup>
import type { MesNcrApi } from '#/api/mes/quality/abnormal/ncr';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page, useVbenModal } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Empty,
  Input,
  Pagination,
  Radio,
  Space,
  Spin,
  Table,
  Tag,
  Tooltip,
} from 'ant-design-vue';

import { getNcrDispositionNotifyPage } from '#/api/mes/quality/abnormal/ncr';
import DetailModalForm from '#/views/mes/quality/abnormal/ncr/modules/detail-modal.vue';

defineOptions({ name: 'DashboardDispositionReply' });

type NotifyRow = MesNcrApi.DispositionNotifyWorkbench;

const pageNo = ref(1);
const pageSize = ref(20);
const total = ref(0);
const loading = ref(false);
const rows = ref<NotifyRow[]>([]);
const query = reactive<{
  lotNo?: string;
  ncNo?: string;
  notifyStatus?: 'PENDING' | 'REPLIED';
}>({
  notifyStatus: 'PENDING',
});

const [DetailModal, detailModalApi] = useVbenModal({
  connectedComponent: DetailModalForm,
  destroyOnClose: true,
});

const columns = [
  { key: 'document', title: '处置单', width: 210 },
  { key: 'source', title: '来源/批次', width: 220 },
  { key: 'material', title: '物料与不良', width: 280 },
  { key: 'notify', title: '通知回复', width: 240 },
  { key: 'node', title: '当前节点', width: 150 },
  { align: 'center', key: 'actions', title: '操作', width: 88 },
];

const statusOptions = [
  { label: '待回复', value: 'PENDING' },
  { label: '已回复', value: 'REPLIED' },
];

const tableScroll = computed(() => ({ x: 1188, y: 'calc(100vh - 316px)' }));

function buildParams() {
  return {
    lotNo: query.lotNo?.trim() || undefined,
    ncNo: query.ncNo?.trim() || undefined,
    notifyStatus: query.notifyStatus,
    pageNo: pageNo.value,
    pageSize: pageSize.value,
  };
}

async function loadData() {
  loading.value = true;
  try {
    const result = await getNcrDispositionNotifyPage(buildParams());
    rows.value = result.list || [];
    total.value = Number(result.total || 0);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  pageNo.value = 1;
  void loadData();
}

function handleReset() {
  query.lotNo = undefined;
  query.ncNo = undefined;
  query.notifyStatus = 'PENDING';
  pageNo.value = 1;
  void loadData();
}

function handlePageChange(nextPageNo: number, nextPageSize: number) {
  pageNo.value = nextPageNo;
  pageSize.value = nextPageSize;
  void loadData();
}

function openDisposition(row: NotifyRow) {
  if (!row.ncRecordId) {
    return;
  }
  detailModalApi
    .setData({
      id: row.ncRecordId,
      sourceType: row.sourceType,
      tabType: row.canReply ? 'todo' : undefined,
    })
    .open();
}

function handleDetailSuccess() {
  void loadData();
}

function showTotalText(value: number) {
  return `共 ${value} 条`;
}

function displaySourceType(row: NotifyRow) {
  if (row.sourceType === 'RAW_MATERIAL') {
    return '原材料';
  }
  return row.sourceTypeName || '产品';
}

function displayHappenPlace(row: NotifyRow) {
  return row.sourceType === 'RAW_MATERIAL'
    ? row.happenDeptName || '-'
    : row.processName || '-';
}

function displayNotifyStatus(row: NotifyRow) {
  return row.notifyStatus === 'REPLIED' ? '已回复' : '待回复';
}

function notifyStatusColor(row: NotifyRow) {
  return row.notifyStatus === 'REPLIED' ? 'success' : 'warning';
}

function nodeStatusColor(row: NotifyRow) {
  if (row.status === 'CLOSED') {
    return 'success';
  }
  if (row.status === 'RETURNED' || row.status === 'CANCELLED') {
    return 'error';
  }
  return 'processing';
}

function displayDisposition(row: NotifyRow) {
  const map: Record<string, string> = {
    CONCESSION: '特采放行',
    PICK: '挑选',
    RECUT: '改切',
    REWORK: '返工',
    SCRAP: '报废',
  };
  return row.dispositionType
    ? map[row.dispositionType] || row.dispositionType
    : '-';
}

function formatDefectQty(row: NotifyRow) {
  if (row.defectQty === undefined || row.defectQty === null || row.defectQty === '') {
    return '-';
  }
  return String(row.defectQty);
}

onMounted(() => {
  void loadData();
});
</script>

<template>
  <Page auto-content-height>
    <DetailModal @success="handleDetailSuccess" />
    <div class="disposition-reply-page">
      <section class="disposition-reply-toolbar">
        <div class="disposition-reply-title">
          <span class="disposition-reply-title__icon">
            <IconifyIcon icon="lucide:messages-square" />
          </span>
          <div>
            <h2>待回复处置单</h2>
            <p>处置执行分配通知</p>
          </div>
        </div>
        <div class="disposition-reply-filters">
          <Radio.Group
            v-model:value="query.notifyStatus"
            button-style="solid"
            class="disposition-reply-status-group"
            option-type="button"
            :options="statusOptions"
            @change="handleSearch"
          />
          <Input
            v-model:value="query.ncNo"
            allow-clear
            class="disposition-reply-filter disposition-reply-filter--keyword"
            placeholder="输入处置单号"
            @press-enter="handleSearch"
          />
          <Input
            v-model:value="query.lotNo"
            allow-clear
            class="disposition-reply-filter disposition-reply-filter--batch"
            placeholder="输入批次"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon>
              <IconifyIcon icon="lucide:search" />
            </template>
            查询
          </Button>
          <Button @click="handleReset">
            <template #icon>
              <IconifyIcon icon="lucide:rotate-ccw" />
            </template>
            重置
          </Button>
        </div>
      </section>

      <section class="disposition-reply-table">
        <Spin :spinning="loading" wrapper-class-name="disposition-reply-spin">
          <Table
            :columns="columns"
            :data-source="rows"
            :pagination="false"
            :scroll="tableScroll"
            row-key="id"
            size="middle"
          >
            <template #emptyText>
              <Empty description="暂无处置通知" />
            </template>

            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'document'">
                <div class="disposition-reply-doc">
                  <a @click="openDisposition(record)">
                    {{ record.ncNo || '-' }}
                  </a>
                  <span>{{ record.happenTime || '-' }}</span>
                  <Tag color="blue" class="!m-0">
                    {{ displaySourceType(record) }}
                  </Tag>
                </div>
              </template>

              <template v-else-if="column.key === 'source'">
                <div class="disposition-reply-cell">
                  <b>{{ record.sourceNo || '-' }}</b>
                  <span>{{ record.lotNo || '-' }}</span>
                  <small>{{ displayHappenPlace(record) }}</small>
                </div>
              </template>

              <template v-else-if="column.key === 'material'">
                <div class="disposition-reply-cell disposition-reply-cell--wide">
                  <b>{{ record.materialName || '-' }}</b>
                  <span>
                    {{ record.materialCode || '-' }} / {{ record.specification || '-' }}
                  </span>
                  <small>
                    {{ record.defectName || '-' }}，数量 {{ formatDefectQty(record) }}
                  </small>
                </div>
              </template>

              <template v-else-if="column.key === 'notify'">
                <div class="disposition-reply-cell">
                  <span class="disposition-reply-statusline">
                    <Tag :color="notifyStatusColor(record)" class="!m-0">
                      {{ displayNotifyStatus(record) }}
                    </Tag>
                    <b>{{ displayDisposition(record) }}</b>
                  </span>
                  <small>通知：{{ record.notifyTime || '-' }}</small>
                  <Tooltip v-if="record.replyConclusion" :title="record.replyConclusion">
                    <span class="disposition-reply-conclusion">
                      {{ record.replyConclusion }}
                    </span>
                  </Tooltip>
                  <small v-if="record.replyTime">回复：{{ record.replyTime }}</small>
                </div>
              </template>

              <template v-else-if="column.key === 'node'">
                <div class="disposition-reply-cell">
                  <Tag :color="nodeStatusColor(record)" class="!m-0">
                    {{ record.currentNodeName || '-' }}
                  </Tag>
                  <small>{{ record.currentHandlerUserName || '-' }}</small>
                </div>
              </template>

              <template v-else-if="column.key === 'actions'">
                <Space>
                  <Button type="link" size="small" @click="openDisposition(record)">
                    {{ record.canReply ? '回复' : '查看' }}
                  </Button>
                </Space>
              </template>
            </template>
          </Table>
        </Spin>
        <div class="disposition-reply-pagination">
          <Pagination
            :current="pageNo"
            :page-size="pageSize"
            :show-total="showTotalText"
            :total="total"
            show-size-changer
            @change="handlePageChange"
            @show-size-change="handlePageChange"
          />
        </div>
      </section>
    </div>
  </Page>
</template>

<style scoped>
.disposition-reply-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f7f8fa;
}

.disposition-reply-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.disposition-reply-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
}

.disposition-reply-title__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #0f766e;
  background: #ccfbf1;
  border-radius: 8px;
}

.disposition-reply-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  letter-spacing: 0;
}

.disposition-reply-title p {
  margin: 1px 0 0;
  font-size: 12px;
  color: #6b7280;
}

.disposition-reply-filters {
  flex: 1;
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.disposition-reply-status-group {
  flex: 0 0 auto;
}

.disposition-reply-filter--keyword {
  width: 200px;
}

.disposition-reply-filter--batch {
  width: 180px;
}

.disposition-reply-table {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  height: 100%;
  min-height: 0;
  padding: 10px;
  overflow: hidden;
}

.disposition-reply-spin,
.disposition-reply-spin :deep(.ant-spin-nested-loading),
.disposition-reply-spin :deep(.ant-spin-container) {
  height: 100%;
  min-height: 0;
}

.disposition-reply-table :deep(.ant-table-wrapper),
.disposition-reply-table :deep(.ant-spin-container),
.disposition-reply-table :deep(.ant-table),
.disposition-reply-table :deep(.ant-table-container),
.disposition-reply-table :deep(.ant-table-content) {
  height: 100%;
  min-height: 0;
}

.disposition-reply-table :deep(.ant-table) {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.disposition-reply-table :deep(.ant-table-thead > tr > th) {
  font-size: 12px;
  font-weight: 700;
  color: #374151;
  background: #f3f4f6;
}

.disposition-reply-table :deep(.ant-table-placeholder .ant-table-cell) {
  height: calc(100vh - 360px);
  padding: 0;
  border-bottom: 0;
}

.disposition-reply-table :deep(.ant-empty) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 380px);
}

.disposition-reply-doc,
.disposition-reply-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  line-height: 1.35;
}

.disposition-reply-doc a {
  overflow: hidden;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  color: #1d4ed8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.disposition-reply-doc span,
.disposition-reply-cell span,
.disposition-reply-cell small {
  overflow: hidden;
  color: #6b7280;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.disposition-reply-cell b {
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.disposition-reply-statusline {
  display: flex;
  align-items: center;
  gap: 6px;
}

.disposition-reply-conclusion {
  overflow: hidden;
  max-width: 230px;
  color: #374151;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.disposition-reply-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 10px 4px 0;
  background: #f7f8fa;
}

@media (max-width: 900px) {
  .disposition-reply-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .disposition-reply-filters {
    flex-wrap: wrap;
    width: 100%;
  }

  .disposition-reply-status-group,
  .disposition-reply-filter--keyword,
  .disposition-reply-filter--batch {
    flex: 1 1 180px;
    width: auto;
  }
}
</style>
