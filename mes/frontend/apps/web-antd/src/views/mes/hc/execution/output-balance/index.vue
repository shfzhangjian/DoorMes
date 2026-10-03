<script lang="ts" setup>
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcProcessOutputBalanceApi } from '#/api/mes/hc/execution/output-balance';

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { buildSortingField } from '@vben/request';

import { Button, DatePicker, Input, message, Modal as AModal, Select, Switch, Tabs, TabPane, Tag } from 'ant-design-vue';
import dayjs from 'dayjs';

import { useVbenVxeGrid } from '#/adapter/vxe-table';
import { getProcessOutputBalancePage } from '#/api/mes/hc/execution/output-balance';
import { releasePlanInventoryLock } from '#/api/mes/hc/planorder';

import '../../package-fg/shared/cut-round-board.css';
import {
  BALANCE_STATUS_COLOR,
  BALANCE_STATUS_LABEL,
  SORTABLE_FIELD_OPTIONS,
  STAGE_COLOR,
  STAGE_OPTIONS,
  useGridColumns,
} from './data';

defineOptions({ name: 'MesHcExecutionOutputBalance' });

type QueryState = Omit<MesHcProcessOutputBalanceApi.PageReqVO, 'pageNo' | 'pageSize'>;
type SortOrder = 'asc' | 'desc';
type SortState = { field: string; label: string; order: SortOrder };

const activeTab = ref('ALL');
const advancedQueryVisible = ref(false);
const currentDateTime = ref(dayjs().format('YYYY-MM-DD HH:mm:ss'));
const newSortField = ref<string>();
const pageRows = ref<MesHcProcessOutputBalanceApi.Balance[]>([]);
const pageTotal = ref(0);
const sortConfigVisible = ref(false);
const sourceDetailVisible = ref(false);
const sourceDetailRow = ref<MesHcProcessOutputBalanceApi.Balance | null>(null);
const activeSortFields = ref<SortState[]>([]);
let currentTimer: ReturnType<typeof setInterval> | null = null;
let countRequestSeq = 0;

const queryParams = reactive<QueryState>({
  keyword: '',
  materialCode: '',
  modelCode: '',
  onlyRemaining: false,
  outputBatchNo: '',
  parentBatchNo: '',
  planNo: '',
  reportDateEnd: '',
  reportDateStart: '',
  sourceBatchNo: '',
  stageCode: '',
});

const advancedQuery = reactive<QueryState>({ ...queryParams });

const statusTabCounts = reactive({
  ALL: 0,
  AVAILABLE: 0,
  CONSUMED: 0,
  PARTIAL: 0,
});

const remainingRows = computed(() =>
  pageRows.value.filter((item) => Number(item.remainingQty || 0) > 0).length,
);

const activeQueryTags = computed(() => {
  const tags: Array<{ key: keyof QueryState; label: string; value: string }> = [];
  const addTag = (key: keyof QueryState, label: string, value?: string | boolean) => {
    if (value === undefined || value === null || value === '' || value === false) return;
    tags.push({ key, label, value: value === true ? '是' : String(value) });
  };
  addTag('planNo', '计划号', queryParams.planNo);
  addTag('outputBatchNo', '产出批号', queryParams.outputBatchNo);
  addTag('sourceBatchNo', '来源批号', queryParams.sourceBatchNo);
  addTag('parentBatchNo', '母批/上游', queryParams.parentBatchNo);
  addTag('materialCode', '物料编码', queryParams.materialCode);
  addTag('modelCode', '型号', queryParams.modelCode);
  addTag('stageCode', '工序', STAGE_OPTIONS.find((item) => item.value === queryParams.stageCode)?.label);
  addTag('reportDateStart', '确认日期起', queryParams.reportDateStart);
  addTag('reportDateEnd', '确认日期止', queryParams.reportDateEnd);
  addTag('onlyRemaining', '只看有剩余', queryParams.onlyRemaining);
  return tags;
});

const sortFieldLabelMap = computed(() =>
  SORTABLE_FIELD_OPTIONS.reduce<Record<string, string>>((map, item) => {
    map[item.value] = item.label;
    return map;
  }, {}),
);

const availableSortOptions = computed(() => {
  const selectedFields = new Set(activeSortFields.value.map((item) => item.field));
  return SORTABLE_FIELD_OPTIONS.filter((item) => !selectedFields.has(item.value));
});

function formatTabCount(count: number) {
  return count > 100 ? '99+' : String(count);
}

function formatSortOrder(order: SortOrder) {
  return order === 'asc' ? '升序' : '降序';
}

function getSortIndex(field?: string) {
  const index = activeSortFields.value.findIndex((item) => item.field === field);
  return index >= 0 ? index + 1 : undefined;
}

function normalizeSorts(sorts?: Array<{ field?: string; order?: string }>): SortState[] {
  if (!sorts || sorts.length === 0) {
    return [];
  }
  const fields = new Set<string>();
  return sorts.reduce<SortState[]>((list, item) => {
    const field = item.field;
    const order = item.order;
    if (!field || fields.has(field) || (order !== 'asc' && order !== 'desc')) {
      return list;
    }
    const label = sortFieldLabelMap.value[field];
    if (!label) {
      return list;
    }
    fields.add(field);
    list.push({ field, label, order });
    return list;
  }, []);
}

function buildQueryParams(): MesHcProcessOutputBalanceApi.PageReqVO {
  return {
    ...queryParams,
    balanceStatus: activeTab.value === 'ALL' ? undefined : activeTab.value,
  };
}

function buildCountQueryParams(balanceStatus?: string): MesHcProcessOutputBalanceApi.PageReqVO {
  return {
    ...queryParams,
    balanceStatus,
    pageNo: 1,
    pageSize: 1,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'process-output-balance-vben-grid',
  gridClass: 'process-output-balance-vxe-grid',
  gridOptions: {
    border: true,
    columns: useGridColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100] },
    proxyConfig: {
      ajax: {
        query: async ({ page, sorts }) => {
          const sortFields = normalizeSorts(sorts);
          activeSortFields.value = sortFields;
          const result = await getProcessOutputBalancePage({
            ...buildQueryParams(),
            ...buildSortingField(sortFields),
            pageNo: page.currentPage,
            pageSize: page.pageSize,
          });
          pageRows.value = result.list || [];
          pageTotal.value = result.total || 0;
          return result;
        },
      },
      sort: true,
    },
    rowConfig: {
      height: 46,
      isHover: true,
      keyField: 'ledgerId',
    },
    sortConfig: {
      chronological: true,
      multiple: true,
      remote: true,
    },
    toolbarConfig: { enabled: false },
  } as VxeTableGridOptions<MesHcProcessOutputBalanceApi.Balance>,
});

async function refreshTabCounts() {
  const requestSeq = ++countRequestSeq;
  const [all, available, partial, consumed] = await Promise.all([
    getProcessOutputBalancePage(buildCountQueryParams()),
    getProcessOutputBalancePage(buildCountQueryParams('AVAILABLE')),
    getProcessOutputBalancePage(buildCountQueryParams('PARTIAL')),
    getProcessOutputBalancePage(buildCountQueryParams('CONSUMED')),
  ]);
  if (requestSeq !== countRequestSeq) return;
  statusTabCounts.ALL = all.total || 0;
  statusTabCounts.AVAILABLE = available.total || 0;
  statusTabCounts.PARTIAL = partial.total || 0;
  statusTabCounts.CONSUMED = consumed.total || 0;
}

async function queryFirstPage() {
  await (gridApi.grid as any)?.setCurrentPage?.(1);
  await Promise.all([refreshTabCounts(), gridApi.query()]);
}

function applyQuery() {
  void queryFirstPage();
}

async function applySortFields(sortFields: SortState[]) {
  const previousFields = new Set(activeSortFields.value.map((item) => item.field));
  const nextFields = new Set(sortFields.map((item) => item.field));
  const removedFields = [...previousFields].filter((field) => !nextFields.has(field));

  if (sortFields.length === 0) {
    await (gridApi.grid as any)?.clearSort?.();
  } else {
    for (const field of removedFields) {
      await (gridApi.grid as any)?.clearSort?.(field);
    }
    await (gridApi.grid as any)?.sort?.(sortFields.map(({ field, order }) => ({ field, order })));
  }
  activeSortFields.value = sortFields;
  await queryFirstPage();
}

function addSortField(field?: string) {
  if (!field || activeSortFields.value.some((item) => item.field === field)) {
    return;
  }
  const label = sortFieldLabelMap.value[field];
  if (!label) {
    return;
  }
  newSortField.value = undefined;
  void applySortFields([...activeSortFields.value, { field, label, order: 'asc' }]);
}

function moveSortField(index: number, offset: number) {
  const nextIndex = index + offset;
  if (nextIndex < 0 || nextIndex >= activeSortFields.value.length) {
    return;
  }
  const next = [...activeSortFields.value];
  const [item] = next.splice(index, 1);
  next.splice(nextIndex, 0, item);
  void applySortFields(next);
}

function toggleSortOrder(field: string) {
  const next = activeSortFields.value.map((item) => {
    const order: SortOrder = item.order === 'asc' ? 'desc' : 'asc';
    return item.field === field ? { ...item, order } : item;
  });
  void applySortFields(next);
}

function removeSortField(field: string) {
  void applySortFields(activeSortFields.value.filter((item) => item.field !== field));
}

function clearSortFields() {
  void applySortFields([]);
}

function resetQuery() {
  Object.assign(queryParams, {
    keyword: '',
    materialCode: '',
    modelCode: '',
    onlyRemaining: false,
    outputBatchNo: '',
    parentBatchNo: '',
    planNo: '',
    reportDateEnd: '',
    reportDateStart: '',
    sourceBatchNo: '',
    stageCode: '',
  });
  activeTab.value = 'ALL';
  clearSortFields();
}

function openAdvancedQuery() {
  Object.assign(advancedQuery, queryParams);
  advancedQueryVisible.value = true;
}

function applyAdvancedQuery() {
  Object.assign(queryParams, advancedQuery);
  advancedQueryVisible.value = false;
  applyQuery();
}

function removeQueryCondition(key: keyof QueryState) {
  (queryParams[key] as any) = key === 'onlyRemaining' ? false : '';
  applyQuery();
}

function openSourceDetail(row: MesHcProcessOutputBalanceApi.Balance) {
  sourceDetailRow.value = row;
  sourceDetailVisible.value = true;
}

function canReleaseSourceLock(row: MesHcProcessOutputBalanceApi.Balance) {
  return Boolean(
    row.sourcePlanLockId &&
      String(row.sourceLockStatus || 'ACTIVE').toUpperCase() === 'ACTIVE' &&
      Number(row.sourceLockRemainingQty || 0) > 0,
  );
}

function openReleaseSourceLock(row: MesHcProcessOutputBalanceApi.Balance) {
  if (!canReleaseSourceLock(row)) {
    message.warning('当前产出物没有可释放的锁定余量');
    return;
  }
  const releaseQty = Number(row.sourceLockRemainingQty || 0);
  AModal.confirm({
    content: `确认释放 ${row.sourceLockTargetPlanNo || row.planNo || '-'} 锁定的 ${releaseQty.toFixed(3)} ${row.uom || ''}？释放后可在生产计划明细的余料查询中重新挂接。`,
    okText: '释放',
    onOk: async () => {
      await releasePlanInventoryLock({
        lockId: row.sourcePlanLockId!,
        releaseQty,
        releaseReason: `工序产出物结存台账释放：${row.outputBatchNo || row.ledgerId}`,
      });
      message.success('锁定余量已释放');
      await queryFirstPage();
    },
    title: '释放锁定余量',
  });
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format('YYYY-MM-DD HH:mm:ss');
  }, 1000);
  void refreshTabCounts();
});

onBeforeUnmount(() => {
  if (currentTimer) clearInterval(currentTimer);
});
</script>

<template>
  <Page auto-content-height>
    <div class="package-fg-console process-output-balance-console">
      <section class="prototype-banner process-output-balance-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:clipboard-list" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">工序产出物结存台账</h2>
            <Tag color="processing" class="console-title-tag">车间执行</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ statusTabCounts.ALL }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页有剩余</span>
              <span class="console-meta-value">{{ remainingRows }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">当前状态</span>
              <span class="console-meta-value">
                {{ activeTab === 'ALL' ? '全部' : BALANCE_STATUS_LABEL[activeTab] || activeTab }}
              </span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateTime.slice(0, 10) }}</div>
          <strong>{{ currentDateTime.slice(11) }}</strong>
        </div>
        <div class="console-action-group process-output-balance-action-group">
          <button class="action-tile" type="button" @click="applyQuery">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="resetQuery">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-output-balance-query-panel">
        <div class="process-output-balance-simple-query">
          <label class="process-output-balance-query-label">查询</label>
          <Select
            v-model:value="queryParams.stageCode"
            allow-clear
            :options="STAGE_OPTIONS"
            placeholder="工序"
            @change="applyQuery"
          />
          <Input
            v-model:value="queryParams.keyword"
            allow-clear
            placeholder="计划号 / 产出批号 / 来源批号 / 物料 / 型号 / 工序"
            @press-enter="applyQuery"
          />
          <Button type="primary" @click="applyQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Button @click="sortConfigVisible = true">
            <template #icon><IconifyIcon icon="lucide:list-ordered" /></template>
            排序
          </Button>
          <div class="process-output-balance-switch">
            <span>只看有剩余</span>
            <Switch v-model:checked="queryParams.onlyRemaining" checked-children="是" un-checked-children="否" @change="applyQuery" />
          </div>
        </div>
        <div v-if="activeQueryTags.length > 0" class="process-output-balance-query-tags">
          <span v-for="tag in activeQueryTags" :key="tag.key" class="process-output-balance-query-tag">
            <b>{{ tag.label }}</b>
            <span>{{ tag.value }}</span>
            <button type="button" @click="removeQueryCondition(tag.key)">x</button>
          </span>
        </div>
      </section>

      <div class="process-output-balance-body">
        <Tabs v-model:activeKey="activeTab" class="process-output-balance-tabs" @change="applyQuery">
          <TabPane key="ALL">
            <template #tab>
              <span class="status-tab-label">
                全部
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.ALL) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="AVAILABLE">
            <template #tab>
              <span class="status-tab-label">
                有结存
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.AVAILABLE) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="PARTIAL">
            <template #tab>
              <span class="status-tab-label">
                部分消耗
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.PARTIAL) }}</span>
              </span>
            </template>
          </TabPane>
          <TabPane key="CONSUMED">
            <template #tab>
              <span class="status-tab-label">
                已耗尽
                <span class="status-tab-badge">{{ formatTabCount(statusTabCounts.CONSUMED) }}</span>
              </span>
            </template>
          </TabPane>
        </Tabs>

        <div class="process-output-balance-grid-host">
          <Grid @cell-dblclick="({ row }) => openSourceDetail(row)">
            <template #sortableHeader="{ column }">
              <span class="process-output-balance-sort-header">
                <span>{{ column.title }}</span>
                <i v-if="getSortIndex(column.field)">{{ getSortIndex(column.field) }}</i>
              </span>
            </template>

            <template #stageCode="{ row }">
              <Tag :color="STAGE_COLOR[row.stageCode] || 'default'">
                {{ row.stageName || row.stageCode || '-' }}
              </Tag>
            </template>

            <template #balanceStatus="{ row }">
              <Tag :color="BALANCE_STATUS_COLOR[row.balanceStatus] || 'default'">
                {{ BALANCE_STATUS_LABEL[row.balanceStatus] || row.balanceStatus || '-' }}
              </Tag>
            </template>

            <template #actions="{ row }">
              <Button
                size="small"
                type="link"
                :disabled="!canReleaseSourceLock(row)"
                @click.stop="openReleaseSourceLock(row)"
              >
                释放
              </Button>
            </template>
          </Grid>
        </div>
      </div>

      <AModal
        v-model:open="advancedQueryVisible"
        title="多条件查询"
        width="760px"
        @ok="applyAdvancedQuery"
      >
        <div class="process-output-balance-advanced-body">
          <div class="process-output-balance-advanced-grid">
            <label class="process-output-balance-query-item">
              <span>计划号</span>
              <Input v-model:value="advancedQuery.planNo" allow-clear placeholder="请输入计划号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>产出批号</span>
              <Input v-model:value="advancedQuery.outputBatchNo" allow-clear placeholder="请输入产出物批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>来源批号</span>
              <Input v-model:value="advancedQuery.sourceBatchNo" allow-clear placeholder="请输入来源批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>母批/上游</span>
              <Input v-model:value="advancedQuery.parentBatchNo" allow-clear placeholder="请输入母批或上游批号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>工序</span>
              <Select
                v-model:value="advancedQuery.stageCode"
                allow-clear
                :options="STAGE_OPTIONS"
                placeholder="请选择工序"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>物料编码</span>
              <Input v-model:value="advancedQuery.materialCode" allow-clear placeholder="请输入物料编码" />
            </label>
            <label class="process-output-balance-query-item">
              <span>型号</span>
              <Input v-model:value="advancedQuery.modelCode" allow-clear placeholder="请输入型号" />
            </label>
            <label class="process-output-balance-query-item">
              <span>确认日期起</span>
              <DatePicker
                v-model:value="advancedQuery.reportDateStart"
                class="w-full"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="开始日期"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>确认日期止</span>
              <DatePicker
                v-model:value="advancedQuery.reportDateEnd"
                class="w-full"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="结束日期"
              />
            </label>
            <label class="process-output-balance-query-item">
              <span>只看有剩余</span>
              <Switch v-model:checked="advancedQuery.onlyRemaining" checked-children="是" un-checked-children="否" />
            </label>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="sortConfigVisible"
        title="排序设置"
        width="720px"
        :footer="null"
      >
        <div class="process-output-balance-sort-modal">
          <div class="process-output-balance-sort-add">
            <span>新增字段</span>
            <Select
              v-model:value="newSortField"
              allow-clear
              :options="availableSortOptions"
              placeholder="请选择排序字段"
              @change="addSortField"
            />
          </div>
          <div class="process-output-balance-sort-list">
            <div
              v-for="(sort, index) in activeSortFields"
              :key="sort.field"
              class="process-output-balance-sort-list-item"
            >
              <b>{{ index + 1 }}</b>
              <span>{{ sort.label }}</span>
              <Button size="small" @click="toggleSortOrder(sort.field)">
                {{ formatSortOrder(sort.order) }}
              </Button>
              <Button size="small" :disabled="index === 0" @click="moveSortField(index, -1)">
                <template #icon><IconifyIcon icon="lucide:chevron-up" /></template>
              </Button>
              <Button size="small" :disabled="index === activeSortFields.length - 1" @click="moveSortField(index, 1)">
                <template #icon><IconifyIcon icon="lucide:chevron-down" /></template>
              </Button>
              <Button size="small" danger @click="removeSortField(sort.field)">
                <template #icon><IconifyIcon icon="lucide:x" /></template>
              </Button>
            </div>
            <div v-if="activeSortFields.length === 0" class="process-output-balance-sort-empty">暂无排序字段</div>
          </div>
        </div>
      </AModal>

      <AModal
        v-model:open="sourceDetailVisible"
        title="来源信息"
        width="640px"
        :footer="null"
      >
        <div class="process-output-balance-source-detail">
          <div>
            <span>来源表</span>
            <b>{{ sourceDetailRow?.sourceTable || '-' }}</b>
          </div>
          <div>
            <span>来源ID</span>
            <b>{{ sourceDetailRow?.sourceId || '-' }}</b>
          </div>
          <div>
            <span>来源报工ID</span>
            <b>{{ sourceDetailRow?.sourceReportId || '-' }}</b>
          </div>
          <div>
            <span>锁定明细ID</span>
            <b>{{ sourceDetailRow?.sourcePlanLockId || '-' }}</b>
          </div>
          <div>
            <span>锁定状态</span>
            <b>{{ sourceDetailRow?.sourceLockStatus || '-' }}</b>
          </div>
          <div>
            <span>锁定计划</span>
            <b>{{ sourceDetailRow?.sourceLockTargetPlanNo || '-' }}</b>
          </div>
          <div>
            <span>锁定剩余</span>
            <b>{{ sourceDetailRow?.sourceLockRemainingQty ?? '-' }}</b>
          </div>
          <div>
            <span>台账行ID</span>
            <b>{{ sourceDetailRow?.ledgerId || '-' }}</b>
          </div>
          <div>
            <span>计划工序ID</span>
            <b>{{ sourceDetailRow?.planOperationId || '-' }}</b>
          </div>
          <div>
            <span>来源批号</span>
            <b>{{ sourceDetailRow?.sourceBatchNo || '-' }}</b>
          </div>
          <div>
            <span>母批/上游</span>
            <b>{{ sourceDetailRow?.parentBatchNo || '-' }}</b>
          </div>
        </div>
      </AModal>
    </div>
  </Page>
</template>

<style scoped>
.process-output-balance-console {
  grid-template-rows: max-content max-content minmax(0, 1fr);
}

.process-output-balance-banner {
  min-height: 78px;
  max-height: 90px;
}

.process-output-balance-action-group {
  flex-wrap: nowrap;
}

.process-output-balance-query-panel {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  min-height: 0;
  padding: 8px 10px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef3f8 0%, #e4ebf3 100%);
  border: 1px solid #8794a4;
}

.process-output-balance-simple-query {
  display: grid;
  grid-template-columns: 86px 170px minmax(260px, 1fr) 86px 106px 86px 150px;
  gap: 8px;
  align-items: stretch;
  min-width: 0;
}

.process-output-balance-query-label {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
  background: #dbe3ed;
  border: 1px solid #c6d3df;
}

.process-output-balance-simple-query :deep(.ant-input-affix-wrapper),
.process-output-balance-simple-query :deep(.ant-select-selector) {
  min-height: 34px;
  border-radius: 0;
}

.process-output-balance-simple-query :deep(.ant-btn) {
  height: 34px;
  font-weight: 800;
  border-radius: 0;
}

.process-output-balance-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-query-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 26px;
  padding: 0 0 1px 86px;
}

.process-output-balance-query-tag {
  display: inline-flex;
  align-items: center;
  max-width: 360px;
  min-height: 24px;
  overflow: hidden;
  color: #075985;
  font-size: 12px;
  font-weight: 700;
  background: #f8fafc;
  border: 1px solid #9fb6cd;
}

.process-output-balance-query-tag b {
  flex: 0 0 auto;
  padding: 0 7px;
  color: #334155;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-tag span {
  min-width: 0;
  padding: 0 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-query-tag button {
  flex: 0 0 24px;
  height: 24px;
  color: #64748b;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-left: 1px solid #c6d3df;
}

.process-output-balance-query-tag button:hover {
  color: #dc2626;
  background: #fee2e2;
}

.process-output-balance-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.process-output-balance-tabs {
  flex: 0 0 auto;
  min-height: 0;
  overflow: hidden;
}

.process-output-balance-tabs :deep(.ant-tabs-nav) {
  padding: 0 8px;
  margin: 0;
  background: #eef3f8;
  border: 1px solid #8794a4;
  border-bottom: 0;
}

.process-output-balance-tabs :deep(.ant-tabs-content-holder) {
  display: none;
}

.status-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 900;
}

.status-tab-badge {
  min-width: 22px;
  padding: 0 6px;
  color: #075985;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  background: #dbeafe;
  border: 1px solid #9fb6cd;
}

.process-output-balance-grid-host {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #8794a4;
}

.process-output-balance-grid-host :deep(.vxe-grid) {
  height: 100% !important;
  min-height: 0 !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper) {
  min-height: 0 !important;
  overflow: hidden !important;
}

.process-output-balance-grid-host :deep(.vxe-grid--table-wrapper .vxe-table) {
  height: 100% !important;
  max-height: 100% !important;
}

.process-output-balance-sort-header {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 0;
  max-width: 100%;
  vertical-align: middle;
}

.process-output-balance-sort-header > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-sort-header i {
  display: inline-flex;
  flex: 0 0 14px;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  color: #075985;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  line-height: 14px;
  background: #dbeafe;
  border: 1px solid #60a5fa;
  border-radius: 999px;
}

.process-output-balance-advanced-body {
  display: grid;
  gap: 10px;
}

.process-output-balance-advanced-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.process-output-balance-query-item {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-query-item > span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-query-item :deep(.ant-input-affix-wrapper),
.process-output-balance-query-item :deep(.ant-select-selector),
.process-output-balance-query-item :deep(.ant-picker) {
  min-height: 34px;
  border: 0;
  border-radius: 0;
}

.process-output-balance-sort-modal {
  display: grid;
  gap: 10px;
}

.process-output-balance-sort-add {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  min-width: 0;
  border: 1px solid #c6d3df;
}

.process-output-balance-sort-add > span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 34px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-sort-add :deep(.ant-select-selector) {
  min-height: 34px;
  border: 0;
  border-radius: 0;
}

.process-output-balance-sort-list {
  display: grid;
  gap: 6px;
}

.process-output-balance-sort-list-item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 78px 34px 34px 34px;
  gap: 6px;
  align-items: center;
  min-width: 0;
  padding: 6px;
  background: #f8fafc;
  border: 1px solid #c6d3df;
}

.process-output-balance-sort-list-item b {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 28px;
  color: #075985;
  background: #dbeafe;
  border: 1px solid #9fb6cd;
}

.process-output-balance-sort-list-item span {
  min-width: 0;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.process-output-balance-sort-list-item :deep(.ant-btn) {
  border-radius: 0;
}

.process-output-balance-sort-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
  border: 1px dashed #c6d3df;
}

.process-output-balance-source-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #c6d3df;
}

.process-output-balance-source-detail > div {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  min-width: 0;
  border-bottom: 1px solid #c6d3df;
}

.process-output-balance-source-detail > div:last-child {
  border-bottom: 0;
}

.process-output-balance-source-detail span {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-height: 36px;
  padding: 0 10px;
  color: #334155;
  font-size: 12px;
  font-weight: 900;
  background: #e2e8f0;
  border-right: 1px solid #c6d3df;
}

.process-output-balance-source-detail b {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 36px;
  padding: 0 10px;
  overflow: hidden;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .process-output-balance-simple-query {
    grid-template-columns: 78px minmax(0, 1fr);
  }

  .process-output-balance-query-tags {
    padding-left: 0;
  }

  .process-output-balance-advanced-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .process-output-balance-sort-list-item {
    grid-template-columns: 32px minmax(0, 1fr) 72px 32px 32px 32px;
  }
}
</style>
