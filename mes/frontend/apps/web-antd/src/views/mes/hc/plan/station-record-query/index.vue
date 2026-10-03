<script lang="ts" setup>
import type { MesHcStationRecordApi } from '#/api/mes/hc/stationrecord';

import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import {
  Button,
  DatePicker,
  Empty,
  Input,
  Modal,
  Radio,
  RadioGroup,
  Select,
  Switch,
  Tabs,
  Tag,
  Tooltip,
  message,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  getStationRecordDetail,
  getStationRecordPage,
  updateStationRecord,
} from '#/api/mes/hc/stationrecord';
import { useVbenVxeGrid } from '#/adapter/vxe-table';

import '../../package-fg/shared/cut-round-board.css';

defineOptions({ name: 'MesHcPlanStationRecordQuery' });

type StationRecord = MesHcStationRecordApi.Record;
type ScopeTabKey =
  | 'ALL'
  | 'EQUIPMENT_DAILY'
  | 'PLAN_OPERATION'
  | 'PROCESS_DETAIL';

const DATETIME_FORMAT = 'YYYY-MM-DD HH:mm:ss';

const recordScopeOptions = [
  { label: '计划工序', value: 'PLAN_OPERATION' },
  { label: '设备日记录', value: 'EQUIPMENT_DAILY' },
  { label: '过程明细', value: 'PROCESS_DETAIL' },
];

const recordScopeMeta: Record<string, { color: string; text: string }> = {
  EQUIPMENT_DAILY: { color: 'blue', text: '设备日记录' },
  PLAN_OPERATION: { color: 'green', text: '计划工序' },
  PROCESS_DETAIL: { color: 'purple', text: '过程明细' },
};

const resultStatusMeta: Record<string, { color: string; text: string }> = {
  FAIL: { color: 'red', text: '异常' },
  NG: { color: 'red', text: 'NG' },
  OK: { color: 'green', text: 'OK' },
  PASS: { color: 'green', text: '通过' },
};

const operationColorMap: Record<string, string> = {
  分切: 'blue',
  压槽: 'orange',
  开工前: 'cyan',
  粘胶1: 'purple',
  粘胶2: 'magenta',
  裁切: 'red',
};

const filters = reactive({
  confirmTimeEnd: undefined as string | undefined,
  confirmTimeStart: undefined as string | undefined,
  confirmUserName: '',
  createTimeEnd: undefined as string | undefined,
  createTimeStart: undefined as string | undefined,
  createUserName: '',
  equipmentCode: '',
  equipmentName: '',
  formName: '',
  keyword: '',
  operationName: '',
  planNo: '',
  recordScope: undefined as string | undefined,
});

const activeScopeTab = ref<ScopeTabKey>('ALL');
const advancedVisible = ref(false);
const currentDateTime = ref(dayjs().format(DATETIME_FORMAT));
const loading = ref(false);
const records = ref<StationRecord[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const saving = ref(false);
const editing = ref(false);
const editingRecord = ref<StationRecord | null>(null);
const onlyConfirmed = ref(false);
let currentTimer: ReturnType<typeof setInterval> | null = null;
let queryFirstPageChain: Promise<void> = Promise.resolve();
let queryFirstPageSeq = 0;

const modalTitle = computed(
  () => `${editing.value ? '填写' : '查看'}${editingRecord.value?.formName || '记录单'}`,
);

const isStartupCleaningMaintenanceRecord = computed(() => {
  const record = editingRecord.value;
  const text = [
    record?.formName,
    record?.triggerTimingName,
    record?.operationName,
  ]
    .filter(Boolean)
    .join(' ');
  return ['开机点检', '清洁点检', '清洁保养', '设备清洁点检', '保养点检'].some((keyword) =>
    text.includes(keyword),
  );
});

const stationRecordHeaderFields = computed(() => {
  const record = editingRecord.value;
  return [
    {
      field: 'equipment',
      label: '设备名',
      value: record?.equipmentName || record?.workCenterName || '-',
    },
    {
      field: 'planNo',
      label: '计划号',
      value: record?.planNo || '-',
    },
    {
      field: 'recorder',
      label: '记录人',
      value: record?.recordUserName || '-',
    },
    {
      field: 'confirmer',
      label: '确认人',
      value: record?.confirmUserName || '-',
    },
    {
      field: 'recordTime',
      label: '记录时间',
      value: formatDateTime(record?.recordTime),
    },
  ];
});

const pageConfirmedCount = computed(() => records.value.filter(isConfirmedRecord).length);

const scopeTabCounts = computed(() => {
  const counts: Record<ScopeTabKey, number> = {
    ALL: total.value,
    EQUIPMENT_DAILY: 0,
    PLAN_OPERATION: 0,
    PROCESS_DETAIL: 0,
  };
  records.value.forEach((row) => {
    const scope = String(row.recordScope || '') as ScopeTabKey;
    if (scope in counts) {
      counts[scope] += 1;
    }
  });
  return counts;
});

const activeQueryTags = computed(() => {
  const tags: Array<{ key: keyof typeof filters; label: string; value: string }> = [];
  const pushText = (key: keyof typeof filters, label: string, value?: string) => {
    const text = String(value || '').trim();
    if (text) tags.push({ key, label, value: text });
  };
  pushText('keyword', '关键词', filters.keyword);
  pushText('planNo', '计划号', filters.planNo);
  pushText('operationName', '工序', filters.operationName);
  pushText('formName', '表单名称', filters.formName);
  pushText('equipmentName', '设备名', filters.equipmentName);
  pushText('equipmentCode', '设备编号', filters.equipmentCode);
  pushText('createUserName', '创建人', filters.createUserName);
  pushText('confirmUserName', '确认人', filters.confirmUserName);
  if (filters.recordScope) {
    tags.push({
      key: 'recordScope',
      label: '记录类型',
      value: scopeMeta(filters.recordScope).text,
    });
  }
  if (filters.createTimeStart || filters.createTimeEnd) {
    tags.push({
      key: 'createTimeStart',
      label: '创建时间',
      value: `${formatText(filters.createTimeStart)} 至 ${formatText(filters.createTimeEnd)}`,
    });
  }
  if (filters.confirmTimeStart || filters.confirmTimeEnd) {
    tags.push({
      key: 'confirmTimeStart',
      label: '确认时间',
      value: `${formatText(filters.confirmTimeStart)} 至 ${formatText(filters.confirmTimeEnd)}`,
    });
  }
  return tags;
});

function trimToUndefined(value?: string) {
  const text = String(value || '').trim();
  return text || undefined;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = dayjs(value);
  return date.isValid() ? date.format(DATETIME_FORMAT) : String(value);
}

function formatText(value?: number | string) {
  if (value === null || value === undefined || value === '') return '-';
  return String(value);
}

function resultValue(row?: StationRecord) {
  return row?.resultStatus || row?.inspectionResult;
}

function isConfirmedRecord(row?: StationRecord) {
  return !!row?.confirmTime || !!row?.confirmUserName;
}

function statusMeta(status?: string) {
  const value = String(status || '').trim();
  if (!value) return { color: 'default', text: '-' };
  return resultStatusMeta[value] || { color: 'default', text: value };
}

function scopeMeta(scope?: string) {
  const value = String(scope || '').trim();
  if (!value) return { color: 'default', text: '-' };
  return recordScopeMeta[value] || { color: 'default', text: value };
}

function operationColor(operationName?: string) {
  const text = String(operationName || '').trim();
  const match = Object.keys(operationColorMap).find((key) => text.includes(key));
  return match ? operationColorMap[match] : 'processing';
}

function cloneRecord(record: StationRecord): StationRecord {
  return JSON.parse(JSON.stringify(record || {}));
}

function formatTabCount(count: number) {
  return count > 100 ? '99+' : String(count);
}

const recordGridColumns = [
  { fixed: 'left', title: '序号', type: 'seq', width: 60 },
  { field: 'planNo', title: '计划号', minWidth: 145, fixed: 'left', sortable: true, showOverflow: 'tooltip', slots: { default: 'planNo' } },
  { field: 'operationName', title: '工序', width: 110, fixed: 'left', sortable: true, align: 'center', slots: { default: 'operationName' } },
  { field: 'formName', title: '表单名称', minWidth: 230, fixed: 'left', sortable: true, showOverflow: 'tooltip', slots: { default: 'formName' } },
  { field: 'recordScope', title: '记录类型', width: 116, align: 'center', slots: { default: 'recordScope' } },
  { field: 'equipmentName', title: '设备名', minWidth: 170, showOverflow: 'tooltip', slots: { default: 'equipmentName' } },
  { field: 'equipmentCode', title: '设备编号', minWidth: 130, showOverflow: 'tooltip', slots: { default: 'equipmentCode' } },
  { field: 'createTime', title: '创建时间', width: 166, sortable: true, formatter: ({ row }: any) => formatDateTime(row.createTime) },
  { field: 'createUserName', title: '创建人', width: 116, formatter: ({ row }: any) => formatText(row.createUserName) },
  { field: 'confirmUserName', title: '确认人', width: 116, formatter: ({ row }: any) => formatText(row.confirmUserName) },
  { field: 'confirmTime', title: '确认时间', width: 166, sortable: true, formatter: ({ row }: any) => formatDateTime(row.confirmTime) },
  { field: 'resultStatus', title: '结果', width: 96, align: 'center', slots: { default: 'resultStatus' } },
  { field: 'action', title: '操作', width: 126, fixed: 'right', align: 'center', slots: { default: 'actions' } },
];

function buildQueryParams(): Omit<MesHcStationRecordApi.PageReqVO, 'pageNo' | 'pageSize'> {
  return {
    confirmTimeEnd: filters.confirmTimeEnd,
    confirmTimeStart: filters.confirmTimeStart,
    confirmUserName: trimToUndefined(filters.confirmUserName),
    createTimeEnd: filters.createTimeEnd,
    createTimeStart: filters.createTimeStart,
    createUserName: trimToUndefined(filters.createUserName),
    equipmentCode: trimToUndefined(filters.equipmentCode),
    equipmentName: trimToUndefined(filters.equipmentName),
    formName: trimToUndefined(filters.formName),
    keyword: trimToUndefined(filters.keyword),
    operationName: trimToUndefined(filters.operationName),
    planNo: trimToUndefined(filters.planNo),
    recordScope: filters.recordScope,
  };
}

const [Grid, gridApi] = useVbenVxeGrid({
  class: 'process-output-balance-vben-grid',
  gridClass: 'process-output-balance-vxe-grid',
  gridOptions: {
    border: true,
    columns: recordGridColumns,
    height: 'auto',
    keepSource: true,
    pagerConfig: { enabled: true, pageSize: 20, pageSizes: [20, 50, 100, 200] },
    proxyConfig: {
      ajax: {
        query: async ({ page }) => {
          loading.value = true;
          try {
            const pageSize = Number(page.pageSize || 20);
            const pageNo = Math.max(1, Number(page.currentPage || 1));
            const result = await getStationRecordPage({
              ...buildQueryParams(),
              pageNo,
              pageSize,
            });
            const resultRows = result?.list || [];
            records.value = onlyConfirmed.value
              ? resultRows.filter(isConfirmedRecord)
              : resultRows;
            total.value = Number(result?.total || 0);
            return {
              list: records.value,
              total: onlyConfirmed.value ? records.value.length : total.value,
            };
          } finally {
            loading.value = false;
          }
        },
      },
    },
    rowConfig: {
      keyField: 'id',
    },
    toolbarConfig: { enabled: false },
  },
});

function queryFirstPage() {
  const seq = ++queryFirstPageSeq;
  queryFirstPageChain = queryFirstPageChain
    .catch(() => undefined)
    .then(async () => {
      if (seq !== queryFirstPageSeq) return;
      await nextTick();
      await (gridApi.grid as any)?.setCurrentPage?.(1);
      await gridApi.query();
    });
  return queryFirstPageChain;
}

function refreshRecords() {
  void gridApi.query();
}

function handleSearch() {
  activeScopeTab.value = filters.recordScope
    ? (filters.recordScope as ScopeTabKey)
    : 'ALL';
  advancedVisible.value = false;
  void queryFirstPage();
}

function handleReset() {
  filters.confirmTimeEnd = undefined;
  filters.confirmTimeStart = undefined;
  filters.confirmUserName = '';
  filters.createTimeEnd = undefined;
  filters.createTimeStart = undefined;
  filters.createUserName = '';
  filters.equipmentCode = '';
  filters.equipmentName = '';
  filters.formName = '';
  filters.keyword = '';
  filters.operationName = '';
  filters.planNo = '';
  filters.recordScope = undefined;
  activeScopeTab.value = 'ALL';
  onlyConfirmed.value = false;
  void queryFirstPage();
}

function openSortConfig() {
  message.info('可点击表头进行排序');
}

function handleScopeTabChange(key: string) {
  activeScopeTab.value = key as ScopeTabKey;
  filters.recordScope = key === 'ALL' ? undefined : key;
  void queryFirstPage();
}

function openAdvancedQuery() {
  advancedVisible.value = true;
}

function removeQueryCondition(key: keyof typeof filters) {
  switch (key) {
    case 'confirmTimeStart': {
      filters.confirmTimeStart = undefined;
      filters.confirmTimeEnd = undefined;
      break;
    }
    case 'createTimeStart': {
      filters.createTimeStart = undefined;
      filters.createTimeEnd = undefined;
      break;
    }
    case 'recordScope': {
      filters.recordScope = undefined;
      activeScopeTab.value = 'ALL';
      break;
    }
    default: {
      filters[key] = '' as never;
      break;
    }
  }
  void queryFirstPage();
}

async function openDetail(row: StationRecord, editable = false) {
  if (!row.id) return;
  editing.value = editable;
  editingRecord.value = null;
  const detail = await getStationRecordDetail(row.id);
  editingRecord.value = cloneRecord({
    ...detail,
    items: detail.items || [],
  });
  detailVisible.value = true;
}

function closeDetail() {
  detailVisible.value = false;
  editing.value = false;
  editingRecord.value = null;
}

async function handleSave() {
  if (!editingRecord.value?.id) return;
  saving.value = true;
  try {
    await updateStationRecord({
      id: editingRecord.value.id,
      confirmRemark: editingRecord.value.confirmRemark,
      confirmTime: editingRecord.value.confirmTime,
      confirmUserName: editingRecord.value.confirmUserName,
      docStatus: editingRecord.value.docStatus,
      formRemark: editingRecord.value.formRemark,
      headerDataJson: editingRecord.value.headerDataJson,
      inspectionResult: editingRecord.value.inspectionResult,
      items: editingRecord.value.items || [],
      recordTime: editingRecord.value.recordTime,
      recordUserName: editingRecord.value.recordUserName,
      resultStatus: editingRecord.value.resultStatus,
    });
    message.success('保存成功');
    closeDetail();
    refreshRecords();
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  currentTimer = setInterval(() => {
    currentDateTime.value = dayjs().format(DATETIME_FORMAT);
  }, 1000);
  void queryFirstPage();
});

onBeforeUnmount(() => {
  if (currentTimer) {
    clearInterval(currentTimer);
    currentTimer = null;
  }
});
</script>

<template>
  <Page
    auto-content-height
    class="station-record-page"
  >
    <div class="package-fg-console process-output-balance-console station-record-report">
      <section class="prototype-banner process-output-balance-banner">
        <span class="console-main-icon">
          <IconifyIcon icon="lucide:warehouse" />
        </span>
        <div class="console-title-block">
          <div class="console-title-row">
            <h2 class="console-title-text">工位记录查询</h2>
            <Tag color="processing" class="console-title-tag">计划排程</Tag>
          </div>
          <div class="console-meta-row">
            <span class="console-meta-item">
              <span class="console-meta-label">总数</span>
              <span class="console-meta-value">{{ total }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">本页确认</span>
              <span class="console-meta-value">{{ pageConfirmedCount }}</span>
            </span>
            <span class="console-meta-item">
              <span class="console-meta-label">当前状态</span>
              <span class="console-meta-value">
                {{ activeScopeTab === 'ALL' ? '全部' : scopeMeta(activeScopeTab).text }}
              </span>
            </span>
          </div>
        </div>
        <div class="work-time-card">
          <div>{{ currentDateTime.slice(0, 10) }}</div>
          <strong>{{ currentDateTime.slice(11) }}</strong>
        </div>
        <div class="console-action-group process-output-balance-action-group">
          <button class="action-tile" type="button" @click="handleSearch">
            <IconifyIcon icon="lucide:search" />
            <span>查询</span>
          </button>
          <button class="action-tile" type="button" @click="handleReset">
            <IconifyIcon icon="lucide:rotate-ccw" />
            <span>重置</span>
          </button>
        </div>
      </section>

      <section class="package-fg-filter-bar process-output-balance-query-panel">
        <div class="process-output-balance-simple-query station-record-simple-query">
          <label class="process-output-balance-query-label">查询</label>
          <Select
            v-model:value="filters.recordScope"
            allow-clear
            :options="recordScopeOptions"
            placeholder="记录类型"
            @change="handleSearch"
          />
          <Input
            v-model:value="filters.keyword"
            allow-clear
            placeholder="计划号 / 工序 / 表单 / 设备名 / 设备编号 / 创建人 / 确认人"
            @press-enter="handleSearch"
          />
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
          <Button @click="openAdvancedQuery">
            <template #icon><IconifyIcon icon="lucide:sliders-horizontal" /></template>
            多条件查询
          </Button>
          <Button @click="openSortConfig">
            <template #icon><IconifyIcon icon="lucide:list-ordered" /></template>
            排序
          </Button>
          <div class="process-output-balance-switch">
            <span>只看确认</span>
            <Switch
              v-model:checked="onlyConfirmed"
              checked-children="是"
              un-checked-children="否"
              @change="handleSearch"
            />
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
        <Tabs
          v-model:active-key="activeScopeTab"
          class="process-output-balance-tabs station-record-tabs"
          @change="handleScopeTabChange"
        >
          <Tabs.TabPane key="ALL">
            <template #tab>
                <span class="status-tab-label">
                全部
                <span class="status-tab-badge">{{ formatTabCount(scopeTabCounts.ALL) }}</span>
              </span>
            </template>
          </Tabs.TabPane>
          <Tabs.TabPane key="PLAN_OPERATION">
            <template #tab>
              <span class="status-tab-label">
                计划工序
                <span class="status-tab-badge">{{ formatTabCount(scopeTabCounts.PLAN_OPERATION) }}</span>
              </span>
            </template>
          </Tabs.TabPane>
          <Tabs.TabPane key="EQUIPMENT_DAILY">
            <template #tab>
              <span class="status-tab-label">
                设备日记录
                <span class="status-tab-badge">{{ formatTabCount(scopeTabCounts.EQUIPMENT_DAILY) }}</span>
              </span>
            </template>
          </Tabs.TabPane>
          <Tabs.TabPane key="PROCESS_DETAIL">
            <template #tab>
              <span class="status-tab-label">
                过程明细
                <span class="status-tab-badge">{{ formatTabCount(scopeTabCounts.PROCESS_DETAIL) }}</span>
              </span>
            </template>
          </Tabs.TabPane>
        </Tabs>

        <div class="process-output-balance-grid-host station-record-grid-host">
          <Grid @cell-dblclick="({ row }) => openDetail(row, false)">
            <template #operationName="{ row }">
              <Tag :color="operationColor(row.operationName || row.operationCode)">
                {{ formatText(row.operationName || row.operationCode) }}
              </Tag>
            </template>

            <template #formName="{ row }">
              <button class="plan-link" type="button" @click.stop="openDetail(row, false)">
                {{ formatText(row.formName) }}
              </button>
            </template>

            <template #recordScope="{ row }">
              <Tag :color="scopeMeta(row.recordScope).color">
                {{ scopeMeta(row.recordScope).text }}
              </Tag>
            </template>

            <template #planNo="{ row }">
              <button class="qty-link" type="button" @click.stop="openDetail(row, false)">
                {{ formatText(row.planNo) }}
              </button>
            </template>

            <template #equipmentName="{ row }">
              <Tooltip :title="row.equipmentName || row.workCenterName">
                <span>{{ formatText(row.equipmentName || row.workCenterName) }}</span>
              </Tooltip>
            </template>

            <template #equipmentCode="{ row }">
              <Tooltip :title="row.equipmentCode || row.workCenterCode">
                <span>{{ formatText(row.equipmentCode || row.workCenterCode) }}</span>
              </Tooltip>
            </template>

            <template #resultStatus="{ row }">
              <Tag :color="statusMeta(resultValue(row)).color">
                {{ statusMeta(resultValue(row)).text }}
              </Tag>
            </template>

            <template #actions="{ row }">
              <div class="station-record-row-actions">
                <Button size="small" type="link" @click.stop="openDetail(row, false)">
                  查看
                </Button>
                <Button size="small" type="link" @click.stop="openDetail(row, true)">
                  编辑
                </Button>
              </div>
            </template>
          </Grid>
        </div>
      </div>
    </div>

    <Modal
      v-model:open="advancedVisible"
      :footer="null"
      destroy-on-close
      title="多条件查询"
      width="820px"
      wrap-class-name="station-record-advanced-modal"
      @cancel="advancedVisible = false"
    >
      <div class="station-record-advanced">
        <div class="station-record-advanced-grid">
          <label>计划号</label>
          <Input v-model:value="filters.planNo" allow-clear placeholder="计划号" />
          <label>工序</label>
          <Input v-model:value="filters.operationName" allow-clear placeholder="工序名称" />
          <label>表单名称</label>
          <Input v-model:value="filters.formName" allow-clear placeholder="表单名称" />
          <label>设备名</label>
          <Input v-model:value="filters.equipmentName" allow-clear placeholder="设备名" />
          <label>设备编号</label>
          <Input v-model:value="filters.equipmentCode" allow-clear placeholder="设备编号" />
          <label>创建人</label>
          <Input v-model:value="filters.createUserName" allow-clear placeholder="创建人" />
          <label>确认人</label>
          <Input v-model:value="filters.confirmUserName" allow-clear placeholder="确认人" />
          <label>记录类型</label>
          <Select
            v-model:value="filters.recordScope"
            allow-clear
            :options="recordScopeOptions"
            placeholder="全部"
          />
          <label>创建时间</label>
          <div class="station-record-date-range">
            <DatePicker
              v-model:value="filters.createTimeStart"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <span>至</span>
            <DatePicker
              v-model:value="filters.createTimeEnd"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </div>
          <label>确认时间</label>
          <div class="station-record-date-range">
            <DatePicker
              v-model:value="filters.confirmTimeStart"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
            <span>至</span>
            <DatePicker
              v-model:value="filters.confirmTimeEnd"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </div>
        </div>
        <div class="station-record-advanced-footer">
          <Button @click="handleReset">
            <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
            重置
          </Button>
          <Button type="primary" @click="handleSearch">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="detailVisible"
      :footer="null"
      :title="null"
      destroy-on-close
      width="100vw"
      wrap-class-name="hc-pass-work-modal station-record-pass-work-modal"
      @cancel="closeDetail"
    >
      <div class="pp-plan-modal station-record-plan-modal">
        <div class="pp-plan-toolbar">
          <div class="pp-plan-toolbar__title pass-work-dialog-title">
            <span class="pp-plan-toolbar__main">{{ modalTitle }}</span>
            <div class="toolbar-meta">
              <span>计划号：{{ formatText(editingRecord?.planNo) }}</span>
              <span>当前工序：{{ formatText(editingRecord?.operationName) }}</span>
              <span>执行时机：{{ formatText(editingRecord?.triggerTimingName) }}</span>
            </div>
          </div>
          <div class="pp-plan-toolbar__actions">
            <Button size="small" @click="closeDetail">关闭</Button>
            <Button v-if="editing" :loading="saving" size="small" type="primary" @click="handleSave">
              保存
            </Button>
          </div>
        </div>

        <div v-if="editingRecord" class="pp-plan-body">
          <fieldset class="pp-fieldset">
            <legend>表单信息</legend>
            <div class="pp-form-grid pass-work-form-grid station-record-head-grid">
              <div
                v-for="item in stationRecordHeaderFields"
                :key="item.field"
                class="head-item"
              >
                <span class="head-item__label">{{ item.label }}</span>
                <span class="head-item__value">{{ item.value || '-' }}</span>
              </div>
            </div>
          </fieldset>

          <div class="pp-panel pass-work-detail-panel">
            <div class="pp-panel__header">
              <span>明细项目</span>
            </div>
            <div class="pp-table-wrap pass-work-detail-wrap station-record-detail-table-wrap">
              <div class="pass-work-detail-table-body">
                <div v-if="isStartupCleaningMaintenanceRecord" class="daily-check-result-tip">
                  开机、清洁保养时遇到问题请记录在备注列说明情况。
                </div>
                <table class="pp-grid station-record-detail-table">
                  <thead>
                    <tr>
                      <th width="90">序号</th>
                      <th width="260">点检项目</th>
                      <th width="360">标准</th>
                      <th v-if="!isStartupCleaningMaintenanceRecord">实际/记录</th>
                      <th width="180">OK/NG</th>
                      <th width="260">备注</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="(item, index) in editingRecord.items || []"
                      :key="item.id || index"
                    >
                      <td align="center">{{ item.itemSeq || index + 1 }}</td>
                      <td>
                        {{ formatText(item.itemName) }}
                      </td>
                      <td>{{ formatText(item.standardText) }}</td>
                      <td v-if="!isStartupCleaningMaintenanceRecord">
                        <div v-if="editing" class="station-record-value-editor">
                          <Input
                            v-model:value="item.actualValue"
                            size="small"
                            :placeholder="item.dualLabel1 || '实际/记录'"
                          />
                        </div>
                        <template v-else>
                          <span>{{ formatText(item.actualValue) }}</span>
                        </template>
                      </td>
                      <td align="center">
                        <RadioGroup
                          v-if="editing"
                          v-model:value="item.resultFlag"
                          class="pp-radio-group"
                          size="small"
                        >
                          <Radio value="OK">OK</Radio>
                          <Radio value="NG">NG</Radio>
                        </RadioGroup>
                        <span v-else>{{ item.resultFlag || '-' }}</span>
                      </td>
                      <td>
                        <Input
                          v-if="editing"
                          v-model:value="item.abnormalRemark"
                          size="small"
                        />
                        <span v-else>{{ formatText(item.abnormalRemark) }}</span>
                      </td>
                    </tr>
                    <tr v-if="(editingRecord.items || []).length === 0">
                      <td :colspan="isStartupCleaningMaintenanceRecord ? 5 : 6" class="pp-empty-cell" align="center">暂无明细数据</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="station-record-detail-empty">
          <Empty description="暂无记录详情" />
        </div>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.station-record-page {
  --report-border: #d8dee8;
  --report-header: #f3f5f8;
  --report-muted: #667085;
  --report-soft: #f8fafc;
  --report-text: #1f2937;
}

.station-record-report {
  display: grid;
  grid-template-rows: max-content max-content minmax(0, 1fr);
  gap: 8px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.console-meta-row,
.console-action-group,
.station-record-row-actions,
.station-record-advanced-footer {
  display: flex;
  gap: 8px;
  align-items: center;
}

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

.process-output-balance-action-group .action-tile:disabled {
  cursor: not-allowed;
  opacity: 0.48;
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
  gap: 6px;
  align-items: center;
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

.station-record-tabs {
  padding: 0;
  background: transparent;
  border: 0;
}

.cell-sub {
  margin-top: 2px;
  overflow: hidden;
  color: #8a96a8;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-link,
.qty-link {
  padding: 0;
  color: #172033;
  font: inherit;
  font-weight: 400;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.plan-link:hover,
.qty-link:hover {
  color: #0958d9;
  text-decoration: underline;
}

.station-record-advanced {
  display: grid;
  gap: 12px;
}

.station-record-advanced-grid {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 86px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.station-record-advanced-grid label {
  color: #334155;
  font-size: 13px;
  font-weight: 700;
}

.station-record-date-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 6px;
  align-items: center;
}

.station-record-date-range :deep(.ant-picker) {
  width: 100%;
}

.station-record-advanced-footer {
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.pp-plan-modal {
  display: flex;
  flex-direction: column;
  height: 100vh;
  min-height: 0;
  background: #f5f7fa;
}

.pp-plan-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 10px 12px;
  background: #fff;
  border-bottom: 1px solid #d9d9d9;
}

.pp-plan-toolbar__title {
  display: flex;
  gap: 10px;
  align-items: baseline;
}

.pp-plan-toolbar__main {
  color: #111827;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
}

.pp-plan-toolbar__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.toolbar-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  color: #4b5563;
  font-size: 12px;
}

.pp-plan-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  padding: 8px;
  overflow: hidden;
}

.pp-fieldset {
  min-width: 0;
  margin: 0;
  padding: 8px 12px 10px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.pp-fieldset legend {
  padding: 0 6px;
  color: #1677ff;
  font-weight: 700;
  font-size: 12px;
}

.pp-form-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 12px;
}

.head-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  min-height: 0;
  border: 0;
}

.head-item__label {
  color: #4b5563;
  font-weight: 700;
}

.head-item__value {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: 32px;
  padding: 4px 11px;
  overflow-wrap: anywhere;
  color: #374151;
  font-weight: 400;
  line-height: 1.4;
  background: #fafafa;
  border: 1px solid #d9d9d9;
  border-radius: 0;
}

.pp-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.pp-panel__header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  height: 34px;
  padding: 0 10px;
  color: #1677ff;
  font-weight: 700;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
}

.pp-table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.pass-work-detail-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.pass-work-detail-table-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.daily-check-result-tip {
  padding: 6px 10px;
  margin-bottom: 8px;
  color: #7a4a00;
  font-size: 13px;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.pp-grid {
  width: 100%;
  min-width: 1260px;
  border-collapse: collapse;
  table-layout: fixed;
}

.pp-grid th,
.pp-grid td {
  min-height: 28px;
  padding: 3px 5px;
  color: #1f2937;
  font-size: 13px;
  vertical-align: middle;
  word-break: break-word;
  border: 1px solid #e5e7eb;
}

.pp-grid th {
  position: sticky;
  top: 0;
  z-index: 1;
  color: #374151;
  font-weight: 400;
  text-align: center;
  background: #f8fafc;
}

.pp-grid tbody tr:hover {
  background: #e6f4ff;
}

.station-record-detail-table :deep(.ant-input),
.station-record-detail-table :deep(.ant-select),
.station-record-value-editor :deep(.ant-input) {
  width: 100%;
}

.station-record-value-editor {
  display: grid;
  gap: 4px;
}

.pp-radio-group {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
}

.station-record-detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-picker),
:deep(.ant-select-selector),
:deep(.ant-btn) {
  border-radius: 0 !important;
}

@media (max-width: 960px) {
  .prototype-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .process-output-balance-simple-query {
    grid-template-columns: 1fr;
  }

  .process-output-balance-query-tags {
    padding-left: 0;
  }

  .station-record-advanced-grid,
  .pp-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<style>
.station-record-pass-work-modal.hc-pass-work-modal .ant-modal {
  top: 0;
  width: 100vw !important;
  max-width: 100vw !important;
  margin: 0 !important;
  padding: 0 !important;
}

.station-record-pass-work-modal.hc-pass-work-modal [class*='modal__header'],
.station-record-pass-work-modal.hc-pass-work-modal .ant-modal-header,
.station-record-pass-work-modal.hc-pass-work-modal [class*='modal__close'],
.station-record-pass-work-modal.hc-pass-work-modal .ant-modal-close {
  display: none !important;
}

.station-record-pass-work-modal.hc-pass-work-modal [class*='modal__body'],
.station-record-pass-work-modal.hc-pass-work-modal .ant-modal-body {
  height: 100vh !important;
  padding: 0 !important;
  overflow: hidden !important;
  background: #f5f7fa;
}

.station-record-pass-work-modal.hc-pass-work-modal [class*='modal__content'],
.station-record-pass-work-modal.hc-pass-work-modal .ant-modal-content {
  height: 100vh !important;
  padding: 0 !important;
  border-radius: 0 !important;
}

.station-record-advanced-modal .ant-modal-content,
.station-record-advanced-modal .ant-modal-header,
.station-record-advanced-modal .ant-modal-body {
  border-radius: 0 !important;
}
</style>
