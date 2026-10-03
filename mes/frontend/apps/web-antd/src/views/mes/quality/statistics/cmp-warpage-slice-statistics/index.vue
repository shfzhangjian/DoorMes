<script lang="ts" setup>
import type { TableColumnsType } from 'ant-design-vue';

import type { MesQmsCmpWarpageSliceStatApi } from '#/api/mes/quality/statistics/cmp-warpage-slice-statistics';

import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Pagination,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
} from 'ant-design-vue';
import dayjs from 'dayjs';

import {
  createCmpWarpageSliceStat,
  deleteCmpWarpageSliceStat,
  getCmpWarpageSliceStat,
  getCmpWarpageSliceStatPage,
  syncCmpWarpageSlices,
  syncCmpWarpageSliceStats,
  updateCmpWarpageSliceStat,
} from '#/api/mes/quality/statistics/cmp-warpage-slice-statistics';

defineOptions({ name: 'MesQmsCmpWarpageSliceStatistics' });

type RecordVO = MesQmsCmpWarpageSliceStatApi.RecordVO;
type DisplayRecord = RecordVO & { rowNo: number };

const resultOptions = [
  { label: '合格', value: 'OK' },
  { label: '不合格', value: 'NG' },
];

const queryResultOptions = [
  ...resultOptions,
  { label: '待判定', value: 'PENDING' },
];

const overrideOptions = [
  { label: '源值', value: false },
  { label: '人工值', value: true },
];

const loading = ref(false);
const importingSlices = ref(false);
const saving = ref(false);
const syncing = ref(false);
const syncingIds = ref<number[]>([]);
const modalOpen = ref(false);
const rows = ref<DisplayRecord[]>([]);
const total = ref(0);
const selectedRowKeys = ref<number[]>([]);
const editingSource = ref<RecordVO>();
const tableAreaRef = ref<HTMLElement>();
const tableScrollY = ref(260);
let tableResizeObserver: ResizeObserver | undefined;

const query = reactive({
  dateRange: undefined as [string, string] | undefined,
  inspectionResult: undefined as string | undefined,
  keyword: '',
  manualOverride: undefined as boolean | undefined,
  modelCode: '',
  pageNo: 1,
  pageSize: 20,
  parentBatchNo: '',
  segmentSliceNo: '',
});

const formState = reactive<MesQmsCmpWarpageSliceStatApi.SaveReqVO>({
  manualOverride: false,
  modelCode: '',
  parentBatchNo: '',
  productionSliceNo: '',
  recordDate: dayjs().format('YYYY-MM-DD'),
  remark: '',
  segmentSliceNo: '',
  warpageValueMm: undefined,
});

const editFormState = reactive<
  Partial<MesQmsCmpWarpageSliceStatApi.UpdateReqVO>
>({
  customerSliceNo: '',
  id: undefined,
  inspectionResult: undefined,
  warpageValueMm: undefined,
});

const isEditing = computed(() => editFormState.id !== undefined);

const modalTitle = computed(() =>
  isEditing.value ? '编辑CMP软垫翘曲片号' : '新增CMP软垫翘曲片号',
);

const rowSelection = computed(() => ({
  onChange: (keys: (number | string)[]) => {
    selectedRowKeys.value = keys.map(Number);
  },
  selectedRowKeys: selectedRowKeys.value,
}));

function mergeSpan(index: number, fields: (keyof DisplayRecord)[]) {
  const current = rows.value[index];
  if (!current) return 1;
  const sameAs = (row?: DisplayRecord) =>
    !!row && fields.every((field) => row[field] === current[field]);
  if (index > 0 && sameAs(rows.value[index - 1])) return 0;
  let span = 1;
  while (sameAs(rows.value[index + span])) span += 1;
  return span;
}

const columns: TableColumnsType<DisplayRecord> = [
  { dataIndex: 'rowNo', fixed: 'left', title: '序号', width: 70 },
  {
    customCell: (_record, index) => ({
      rowSpan: mergeSpan(index ?? 0, ['recordDate']),
    }),
    dataIndex: 'recordDate',
    fixed: 'left',
    title: '日期',
    width: 110,
  },
  {
    customCell: (_record, index) => ({
      rowSpan: mergeSpan(index ?? 0, ['recordDate', 'modelCode']),
    }),
    dataIndex: 'modelCode',
    fixed: 'left',
    title: '型号',
    width: 125,
  },
  {
    customCell: (_record, index) => ({
      rowSpan: mergeSpan(index ?? 0, [
        'recordDate',
        'modelCode',
        'parentBatchNo',
        'segmentSliceNo',
      ]),
    }),
    dataIndex: 'batchNo',
    fixed: 'left',
    title: '批号',
    width: 220,
  },
  {
    dataIndex: 'productionSliceNo',
    fixed: 'left',
    title: '生产片号',
    width: 185,
  },
  {
    dataIndex: 'customerSliceNo',
    title: '发货片号',
    width: 170,
  },
  {
    align: 'right',
    dataIndex: 'warpageValueMm',
    title: '翘曲高度(mm)',
    width: 135,
  },
  { dataIndex: 'inspectionResult', title: '判定结果', width: 105 },
  { dataIndex: 'action', fixed: 'right', title: '操作', width: 190 },
];

onMounted(async () => {
  await loadPage();
  await nextTick();
  updateTableScrollHeight();
  if (tableAreaRef.value) {
    tableResizeObserver = new ResizeObserver(updateTableScrollHeight);
    tableResizeObserver.observe(tableAreaRef.value);
  }
});

onBeforeUnmount(() => tableResizeObserver?.disconnect());

function updateTableScrollHeight() {
  const areaHeight = tableAreaRef.value?.clientHeight || 0;
  tableScrollY.value = Math.max(160, areaHeight - 42);
}

async function loadPage() {
  loading.value = true;
  try {
    const result = await getCmpWarpageSliceStatPage(buildQueryParams());
    const offset = (query.pageNo - 1) * query.pageSize;
    rows.value = (result.list || []).map((row, index) => ({
      ...row,
      rowNo: offset + index + 1,
    }));
    total.value = Number(result.total || 0);
    selectedRowKeys.value = selectedRowKeys.value.filter((id) =>
      rows.value.some((row) => row.id === id),
    );
  } finally {
    loading.value = false;
  }
}

function buildQueryParams(): MesQmsCmpWarpageSliceStatApi.PageReqVO {
  return cleanParams({
    inspectionResult: query.inspectionResult,
    keyword: query.keyword.trim() || undefined,
    manualOverride: query.manualOverride,
    modelCode: query.modelCode.trim() || undefined,
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    parentBatchNo: query.parentBatchNo.trim() || undefined,
    recordDateEnd: query.dateRange?.[1],
    recordDateStart: query.dateRange?.[0],
    segmentSliceNo: query.segmentSliceNo.trim() || undefined,
  });
}

function buildSliceImportParams(): MesQmsCmpWarpageSliceStatApi.SliceImportReqVO {
  return cleanParams({
    keyword: query.keyword.trim() || undefined,
    modelCode: query.modelCode.trim() || undefined,
    parentBatchNo: query.parentBatchNo.trim() || undefined,
    recordDateEnd: query.dateRange?.[1],
    recordDateStart: query.dateRange?.[0],
    segmentSliceNo: query.segmentSliceNo.trim() || undefined,
  });
}

function handleQuery() {
  query.pageNo = 1;
  loadPage();
}

function handleReset() {
  query.dateRange = undefined;
  query.inspectionResult = undefined;
  query.keyword = '';
  query.manualOverride = undefined;
  query.modelCode = '';
  query.parentBatchNo = '';
  query.segmentSliceNo = '';
  query.pageNo = 1;
  loadPage();
}

function handlePageChange(pageNo: number, pageSize: number) {
  query.pageNo = pageNo;
  query.pageSize = pageSize;
  loadPage();
}

function resetForm() {
  editingSource.value = undefined;
  Object.assign(editFormState, {
    customerSliceNo: '',
    id: undefined,
    inspectionResult: undefined,
    warpageValueMm: undefined,
  });
  Object.assign(formState, {
    id: undefined,
    manualOverride: false,
    modelCode: '',
    parentBatchNo: '',
    productionSliceNo: '',
    recordDate: dayjs().format('YYYY-MM-DD'),
    remark: '',
    segmentSliceNo: '',
    warpageValueMm: undefined,
  });
}

function openCreate() {
  resetForm();
  modalOpen.value = true;
}

async function openEdit(row: DisplayRecord) {
  if (!row.id) return;
  resetForm();
  const detail = await getCmpWarpageSliceStat(row.id);
  editingSource.value = detail;
  Object.assign(editFormState, {
    customerSliceNo: detail.customerSliceNo || '',
    id: detail.id,
    inspectionResult: detail.inspectionResult,
    warpageValueMm: detail.warpageValueMm,
  });
  modalOpen.value = true;
}

function handleEditWarpageValueChange(value: null | number) {
  if (value === null || value === undefined) {
    editFormState.inspectionResult = undefined;
    return;
  }
  editFormState.inspectionResult = value <= 20 ? 'OK' : 'NG';
}

function handleWarpageValueChange(value: null | number) {
  if (value !== null && value !== undefined) {
    formState.manualOverride = true;
  }
}

function handleOverrideChange(checked: boolean) {
  formState.manualOverride = checked;
  if (!checked) {
    formState.warpageValueMm = parseSourceValue(
      editingSource.value?.sourceActualValue,
    );
  }
}

async function handleSave() {
  if (isEditing.value) {
    if (!editFormState.id) return;
    if (
      editFormState.warpageValueMm !== undefined &&
      editFormState.warpageValueMm !== null &&
      Number(editFormState.warpageValueMm) < 0
    ) {
      message.warning('翘曲高度不能小于0');
      return;
    }
    saving.value = true;
    try {
      const payload: MesQmsCmpWarpageSliceStatApi.UpdateReqVO = {
        customerSliceNo: editFormState.customerSliceNo?.trim() || undefined,
        id: editFormState.id,
        inspectionResult: editFormState.inspectionResult,
        warpageValueMm: editFormState.warpageValueMm,
      };
      await updateCmpWarpageSliceStat(payload);
      message.success('发货片号、翘曲高度和判定结果已更新');
      modalOpen.value = false;
      await loadPage();
    } finally {
      saving.value = false;
    }
    return;
  }

  const productionSliceNo = formState.productionSliceNo.trim();
  if (!productionSliceNo) {
    message.warning('请输入生产片号');
    return;
  }
  if (
    formState.warpageValueMm !== undefined &&
    formState.warpageValueMm !== null &&
    Number(formState.warpageValueMm) < 0
  ) {
    message.warning('翘曲高度不能小于0');
    return;
  }
  saving.value = true;
  try {
    const payload: MesQmsCmpWarpageSliceStatApi.SaveReqVO = cleanParams({
      ...formState,
      modelCode: formState.modelCode?.trim() || undefined,
      parentBatchNo: formState.parentBatchNo?.trim() || undefined,
      productionSliceNo,
      recordDate: formState.recordDate || undefined,
      remark: formState.remark?.trim() || undefined,
      segmentSliceNo: formState.segmentSliceNo?.trim() || undefined,
    });
    await createCmpWarpageSliceStat(payload);
    message.success('翘曲片号记录已新增，可点击同步抓取检验和发货数据');
    modalOpen.value = false;
    await loadPage();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(row: DisplayRecord) {
  if (!row.id) return;
  await deleteCmpWarpageSliceStat(row.id);
  message.success('翘曲片号记录已删除');
  await loadPage();
}

async function handleRowSync(row: DisplayRecord) {
  if (!row.id) return;
  syncingIds.value = [row.id];
  try {
    const result = await syncCmpWarpageSliceStats([row.id]);
    showSyncResult(result);
    await loadPage();
  } finally {
    syncingIds.value = [];
  }
}

async function handleBatchSync() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择需要同步的片号记录');
    return;
  }
  syncing.value = true;
  syncingIds.value = [...selectedRowKeys.value];
  try {
    const result = await syncCmpWarpageSliceStats(selectedRowKeys.value);
    showSyncResult(result);
    await loadPage();
  } finally {
    syncing.value = false;
    syncingIds.value = [];
  }
}

async function handleSyncSlices() {
  importingSlices.value = true;
  try {
    const result = await syncCmpWarpageSlices(buildSliceImportParams());
    query.pageNo = 1;
    await loadPage();
    if (result.sourceCount === 0) {
      message.info('当前条件下没有可同步的裁切送检片号');
      return;
    }
    message.success(
      `片号同步完成：新增 ${result.importedCount} 条，已存在 ${result.existingCount} 条${
        result.duplicateSourceCount > 0
          ? `，合并重复送检 ${result.duplicateSourceCount} 条`
          : ''
      }`,
    );
  } finally {
    importingSlices.value = false;
  }
}

function showSyncResult(result: MesQmsCmpWarpageSliceStatApi.SyncResult) {
  const notes = [
    `成功抓取 ${result.syncedCount} 条`,
    result.sourceMissingCount > 0
      ? `未找到检验 ${result.sourceMissingCount} 条`
      : '',
    result.customerMissingCount > 0
      ? `无客户片号 ${result.customerMissingCount} 条`
      : '',
    result.manualProtectedCount > 0
      ? `保留人工值 ${result.manualProtectedCount} 条`
      : '',
    result.invalidValueCount > 0
      ? `源值无法解析 ${result.invalidValueCount} 条`
      : '',
  ].filter(Boolean);
  message.success(`同步完成：${notes.join('，')}`);
}

function isSyncing(id?: number) {
  return !!id && syncingIds.value.includes(id);
}

function resultMeta(value?: string) {
  if (value === 'OK') return { color: 'success', label: '合格' };
  if (value === 'NG') return { color: 'error', label: '不合格' };
  return { color: 'default', label: '待判定' };
}

function formatValue(value?: number) {
  if (value === undefined || value === null) return '-';
  return Number(value).toLocaleString('zh-CN', {
    maximumFractionDigits: 4,
  });
}

function parseSourceValue(value?: string) {
  if (!value) return undefined;
  const matched = value.replace(',', '.').match(/[-+]?\d+(?:\.\d+)?/);
  if (!matched) return undefined;
  let numberValue = Number(matched[0]);
  if (/cm|厘米/i.test(value)) numberValue *= 10;
  return Number.isFinite(numberValue) && numberValue >= 0
    ? numberValue
    : undefined;
}

function cleanParams<T extends Record<string, any>>(params: T) {
  return Object.fromEntries(
    Object.entries(params).filter(
      ([, value]) => value !== '' && value !== undefined && value !== null,
    ),
  ) as T;
}
</script>

<template>
  <Page
    auto-content-height
    class="cmp-warpage-page"
    content-class="cmp-warpage-content"
  >
    <div class="cmp-warpage-shell">
      <section class="cmp-warpage-banner">
        <div class="cmp-warpage-banner__icon">
          <IconifyIcon icon="lucide:chart-no-axes-column-increasing" />
        </div>
        <div>
          <h2>CMP软垫翘曲片号统计</h2>
          <p>
            点击同步片号系统将同步读取裁切成品检验“Pad边缘翘曲”实际值、判定和发货客户片号
          </p>
        </div>
        <div class="cmp-warpage-banner__limit">
          <span>判定标准</span>
          <strong>≤ 20 mm 合格</strong>
        </div>
      </section>

      <div class="cmp-warpage-query-bar">
        <Space wrap>
          <Input
            v-model:value="query.keyword"
            allow-clear
            class="cmp-filter cmp-filter--keyword"
            placeholder="母批 / 分段 / 生产片号 / 客户片号"
            @press-enter="handleQuery"
          />
          <Input
            v-model:value="query.modelCode"
            allow-clear
            class="cmp-filter"
            placeholder="型号"
            @press-enter="handleQuery"
          />
          <Input
            v-model:value="query.parentBatchNo"
            allow-clear
            class="cmp-filter"
            placeholder="母批"
            @press-enter="handleQuery"
          />
          <Input
            v-model:value="query.segmentSliceNo"
            allow-clear
            class="cmp-filter"
            placeholder="分段片号"
            @press-enter="handleQuery"
          />
          <DatePicker.RangePicker
            v-model:value="query.dateRange"
            class="cmp-date-filter"
            value-format="YYYY-MM-DD"
          />
          <Select
            v-model:value="query.inspectionResult"
            allow-clear
            :options="queryResultOptions"
            class="cmp-filter"
            placeholder="判定结果"
          />
          <Select
            v-model:value="query.manualOverride"
            allow-clear
            :options="overrideOptions"
            class="cmp-filter"
            placeholder="值来源"
          />
          <Button type="primary" :loading="loading" @click="handleQuery">
            <template #icon><IconifyIcon icon="lucide:search" /></template>
            查询
          </Button>
        </Space>
      </div>

      <div class="cmp-warpage-action-toolbar">
        <Space :size="4">
          <Button size="small" @click="handleReset">
            <template #icon><IconifyIcon icon="lucide:rotate-ccw" /></template>
            重置
          </Button>
          <Button
            v-access:code="['mes:cmp-warpage-slice-stat:create']"
            size="small"
            type="primary"
            @click="openCreate"
          >
            <template #icon><IconifyIcon icon="lucide:plus" /></template>
            新增片号
          </Button>
          <Button
            v-access:code="['mes:cmp-warpage-slice-stat:sync']"
            :loading="importingSlices"
            size="small"
            @click="handleSyncSlices"
          >
            <template #icon>
              <IconifyIcon icon="lucide:database-zap" />
            </template>
            同步片号
          </Button>
          <Button
            v-access:code="['mes:cmp-warpage-slice-stat:sync']"
            :disabled="selectedRowKeys.length === 0"
            :loading="syncing"
            size="small"
            @click="handleBatchSync"
          >
            <template #icon><IconifyIcon icon="lucide:refresh-cw" /></template>
            同步所选检测值{{
              selectedRowKeys.length > 0 ? `(${selectedRowKeys.length})` : ''
            }}
          </Button>
        </Space>
        <span>先同步裁切送检片号，再同步检测值或人工填写</span>
      </div>

      <section class="cmp-warpage-panel">
        <div class="cmp-warpage-panel__head">
          <div>
            <IconifyIcon icon="lucide:table-2" />
            <span>CMP软垫翘曲片号统计明细</span>
          </div>
        </div>
        <div ref="tableAreaRef" class="cmp-warpage-table-area">
          <Table
            bordered
            class="cmp-warpage-table"
            :columns="columns"
            :data-source="rows"
            :loading="loading"
            :pagination="false"
            row-key="id"
            :row-selection="rowSelection"
            :scroll="{ x: 1360, y: tableScrollY }"
            :style="{
              '--cmp-table-body-height': `${tableScrollY}px`,
            }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'recordDate'">
                {{ record.recordDate || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'modelCode'">
                {{ record.modelCode || '-' }}
              </template>
              <template v-else-if="column.dataIndex === 'batchNo'">
                <div class="cmp-batch-cell">
                  <strong>{{
                    record.segmentSliceNo || record.parentBatchNo || '-'
                  }}</strong>
                  <small
                    v-if="
                      record.parentBatchNo &&
                      record.parentBatchNo !== record.segmentSliceNo
                    "
                  >
                    母批：{{ record.parentBatchNo }}
                  </small>
                </div>
              </template>
              <template v-else-if="column.dataIndex === 'customerSliceNo'">
                <span :class="{ 'cmp-cell-empty': !record.customerSliceNo }">
                  {{ record.customerSliceNo || '' }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'warpageValueMm'">
                <strong class="cmp-warpage-value">{{
                  formatValue(record.warpageValueMm)
                }}</strong>
              </template>
              <template v-else-if="column.dataIndex === 'inspectionResult'">
                <Tag :color="resultMeta(record.inspectionResult).color">
                  {{ resultMeta(record.inspectionResult).label }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <Button
                  v-access:code="['mes:cmp-warpage-slice-stat:sync']"
                  :loading="isSyncing(record.id)"
                  size="small"
                  type="link"
                  @click="handleRowSync(record)"
                >
                  同步值
                </Button>
                <Button
                  v-access:code="['mes:cmp-warpage-slice-stat:update']"
                  size="small"
                  type="link"
                  @click="openEdit(record)"
                >
                  {{ record.warpageValueMm == null ? '填写' : '编辑' }}
                </Button>
                <Popconfirm
                  title="确认删除这条翘曲片号镜像记录？"
                  @confirm="handleDelete(record)"
                >
                  <Button
                    v-access:code="['mes:cmp-warpage-slice-stat:delete']"
                    danger
                    size="small"
                    type="link"
                  >
                    删除
                  </Button>
                </Popconfirm>
              </template>
            </template>
          </Table>
        </div>
        <div class="cmp-warpage-pagination">
          <span>已选 {{ selectedRowKeys.length }} 条</span>
          <Pagination
            :current="query.pageNo"
            :page-size="query.pageSize"
            :page-size-options="['10', '20', '50', '100']"
            :show-total="(value: number) => `共 ${value} 条`"
            :total="total"
            show-quick-jumper
            show-size-changer
            size="small"
            @change="handlePageChange"
          />
        </div>
      </section>
    </div>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      cancel-text="取消"
      centered
      destroy-on-close
      ok-text="保存"
      :width="isEditing ? '560px' : '820px'"
      wrap-class-name="cmp-warpage-edit-modal"
      @ok="handleSave"
    >
      <template v-if="isEditing">
        <div class="cmp-warpage-edit-context">
          <span>生产片号</span>
          <strong>{{ editingSource?.productionSliceNo || '-' }}</strong>
        </div>
        <Form
          :label-col="{ flex: '132px' }"
          :model="editFormState"
          :wrapper-col="{ flex: 1 }"
          class="cmp-warpage-simple-form"
          label-align="right"
        >
          <Form.Item label="发货片号">
            <Input
              v-model:value="editFormState.customerSliceNo"
              allow-clear
              placeholder="请输入发货片号"
            />
          </Form.Item>
          <Form.Item label="翘曲高度(mm)">
            <InputNumber
              v-model:value="editFormState.warpageValueMm"
              class="cmp-warpage-form__control"
              :min="0"
              :precision="4"
              placeholder="请输入翘曲高度"
              @change="handleEditWarpageValueChange"
            />
          </Form.Item>
          <Form.Item label="判定结果">
            <Select
              v-model:value="editFormState.inspectionResult"
              allow-clear
              :options="resultOptions"
              placeholder="请选择判定结果"
            />
          </Form.Item>
        </Form>
      </template>
      <template v-else>
        <Alert
          class="cmp-warpage-form-alert"
          message="同步规则"
          show-icon
          type="info"
        >
          <template #description>
            生产片号用于抓取裁切FQC“Pad边缘翘曲”实际值，并匹配发货成品检验已对齐的客户片号。打开“人工覆盖”后，后续同步只刷新源数据快照，不覆盖当前翘曲高度。
          </template>
        </Alert>
        <Form
          :label-col="{ flex: '112px' }"
          :model="formState"
          :wrapper-col="{ flex: 1 }"
          class="cmp-warpage-form"
          label-align="right"
        >
          <div class="cmp-warpage-form__grid">
            <Form.Item label="日期">
              <DatePicker
                v-model:value="formState.recordDate"
                class="cmp-warpage-form__control"
                value-format="YYYY-MM-DD"
              />
            </Form.Item>
            <Form.Item label="型号">
              <Input
                v-model:value="formState.modelCode"
                allow-clear
                placeholder="可人工填写；同步后按裁切FQC刷新"
              />
            </Form.Item>
            <Form.Item label="母批">
              <Input
                v-model:value="formState.parentBatchNo"
                allow-clear
                placeholder="母批号"
              />
            </Form.Item>
            <Form.Item label="分段片号">
              <Input
                v-model:value="formState.segmentSliceNo"
                allow-clear
                placeholder="分段片号/内部编号"
              />
            </Form.Item>
            <Form.Item class="cmp-warpage-form__full" label="生产片号" required>
              <Input
                v-model:value="formState.productionSliceNo"
                allow-clear
                placeholder="请输入裁切成品生产片号"
              />
            </Form.Item>
            <Form.Item label="翘曲高度(mm)">
              <InputNumber
                v-model:value="formState.warpageValueMm"
                class="cmp-warpage-form__control"
                :min="0"
                :precision="4"
                placeholder="输入后自动标记人工覆盖"
                @change="handleWarpageValueChange"
              />
            </Form.Item>
            <Form.Item label="人工覆盖">
              <div class="cmp-warpage-override-control">
                <Switch
                  :checked="formState.manualOverride"
                  checked-children="保留人工值"
                  un-checked-children="采用源值"
                  @change="handleOverrideChange"
                />
                <small>新增后可同步源值，也可保留当前人工填写值</small>
              </div>
            </Form.Item>
            <Form.Item class="cmp-warpage-form__full" label="备注">
              <Input.TextArea
                v-model:value="formState.remark"
                :auto-size="{ minRows: 2, maxRows: 4 }"
                allow-clear
                placeholder="可填写人工调整原因或现场说明"
              />
            </Form.Item>
          </div>
        </Form>
      </template>
    </Modal>
  </Page>
</template>

<style scoped>
.cmp-warpage-page {
  height: 100%;
  background: #f4f7fb;
}

:global(.cmp-warpage-content) {
  overflow: hidden !important;
  padding: 0 !important;
}

.cmp-warpage-shell {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.cmp-warpage-banner {
  display: flex;
  min-height: 74px;
  align-items: center;
  gap: 14px;
  border-bottom: 1px solid #d6e1ed;
  background: linear-gradient(100deg, #f7faff 0%, #edf5fc 62%, #e5f1fa 100%);
  padding: 10px 18px;
  color: #203c56;
}

.cmp-warpage-banner__icon {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border: 1px solid #b9cde0;
  border-radius: 8px;
  background: #fff;
  color: #176ba4;
  font-size: 25px;
}

.cmp-warpage-banner h2 {
  margin: 0 0 3px;
  font-size: 19px;
  font-weight: 650;
}

.cmp-warpage-banner p {
  margin: 0;
  color: #687e93;
  font-size: 12px;
}

.cmp-warpage-banner__limit {
  margin-left: auto;
  border-left: 1px solid #c8d7e5;
  padding-left: 18px;
  text-align: right;
}

.cmp-warpage-banner__limit span,
.cmp-warpage-banner__limit strong {
  display: block;
}

.cmp-warpage-banner__limit span {
  color: #71869a;
  font-size: 12px;
}

.cmp-warpage-banner__limit strong {
  margin-top: 2px;
  color: #176ba4;
  font-size: 17px;
}

.cmp-warpage-query-bar {
  flex: 0 0 auto;
  border-bottom: 1px solid #dbe3ef;
  background: #fff;
  padding: 10px 14px;
}

.cmp-warpage-action-toolbar {
  display: flex;
  min-height: 42px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #d7e1ee;
  background: #f7f9fc;
  padding: 5px 14px;
}

.cmp-warpage-action-toolbar :deep(.ant-btn) {
  display: inline-flex;
  min-width: 88px;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.cmp-warpage-action-toolbar > span {
  color: #7a8797;
  font-size: 12px;
}

.cmp-filter {
  width: 132px;
}

.cmp-filter--keyword {
  width: 255px;
}

.cmp-date-filter {
  width: 238px;
}

.cmp-warpage-panel {
  display: flex;
  overflow: hidden;
  min-height: 0;
  flex: 1 1 0;
  flex-direction: column;
  margin: 10px;
  border: 1px solid #d7e1ee;
  background: #fff;
}

.cmp-warpage-panel__head {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4eaf2;
  padding: 0 12px;
}

.cmp-warpage-panel__head > div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1e3a56;
  font-weight: 600;
}

.cmp-warpage-panel__head small {
  color: #75859a;
}

.cmp-warpage-table-area {
  min-height: 0;
  flex: 1 1 0;
  overflow: hidden;
}

.cmp-warpage-table {
  height: 100%;
  min-height: 0;
}

.cmp-warpage-table :deep(.ant-spin-nested-loading),
.cmp-warpage-table :deep(.ant-spin-container),
.cmp-warpage-table :deep(.ant-table) {
  height: 100%;
}

.cmp-warpage-table :deep(.ant-table-body) {
  min-height: var(--cmp-table-body-height) !important;
}

.cmp-warpage-table :deep(.ant-table-placeholder > td) {
  height: var(--cmp-table-body-height) !important;
  border-bottom: 0 !important;
}

.cmp-warpage-table :deep(.ant-table-cell) {
  height: 42px;
  border-color: #dfe7f0 !important;
  padding: 0 8px !important;
  text-align: center;
  white-space: nowrap;
}

.cmp-warpage-table :deep(.ant-table-thead > tr > th) {
  background: #edf4fb;
  color: #35536e;
  font-weight: 650;
  text-align: center !important;
}

.cmp-warpage-table :deep(.ant-table-cell-fix-left),
.cmp-warpage-table :deep(.ant-table-cell-fix-right) {
  background: #fff;
}

.cmp-warpage-value {
  color: #0f5f91;
  font-variant-numeric: tabular-nums;
}

.cmp-batch-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  line-height: 1.2;
}

.cmp-batch-cell strong {
  color: #243f59;
  font-weight: 600;
}

.cmp-batch-cell small {
  color: #8190a2;
  font-size: 11px;
}

.cmp-cell-empty {
  display: inline-block;
  min-width: 36px;
  min-height: 18px;
}

.cmp-warpage-pagination {
  display: flex;
  min-height: 44px;
  flex: 0 0 44px;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #dfe7f0;
  background: #fff;
  padding: 5px 12px;
}

.cmp-warpage-pagination > span {
  color: #7a8797;
  font-size: 12px;
}

.cmp-warpage-form-alert {
  margin-bottom: 14px;
}

.cmp-warpage-edit-context {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #d9e5f2;
  border-radius: 4px;
  background: #f5f9fd;
  margin-bottom: 18px;
  padding: 10px 14px;
}

.cmp-warpage-edit-context span {
  color: #738399;
  font-size: 12px;
}

.cmp-warpage-edit-context strong {
  color: #24435f;
  font-weight: 600;
}

.cmp-warpage-simple-form :deep(.ant-form-item) {
  margin-bottom: 16px;
}

.cmp-warpage-simple-form :deep(.ant-form-item:last-child) {
  margin-bottom: 6px;
}

.cmp-warpage-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.cmp-warpage-form__grid :deep(.ant-form-item) {
  margin-bottom: 13px;
}

.cmp-warpage-form__full {
  grid-column: 1 / -1;
}

.cmp-warpage-form__control {
  width: 100%;
}

.cmp-warpage-override-control {
  display: flex;
  min-height: 32px;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;
}

.cmp-warpage-override-control small {
  color: #7a8797;
  line-height: 1.3;
}

:global(.cmp-warpage-edit-modal .ant-modal-content) {
  overflow: hidden;
  border: 1px solid #c9d5e5;
  border-radius: 4px;
  padding: 0;
}

:global(.cmp-warpage-edit-modal .ant-modal-header) {
  margin-bottom: 0;
  border-bottom: 1px solid #d9e2ef;
  background: #f7f9fc;
  padding: 14px 18px;
}

:global(.cmp-warpage-edit-modal .ant-modal-body) {
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: 16px 18px 8px;
}

:global(.cmp-warpage-edit-modal .ant-modal-footer) {
  margin-top: 0;
  border-top: 1px solid #d9e2ef;
  background: #f7f9fc;
  padding: 10px 18px;
}

@media (max-width: 900px) {
  .cmp-warpage-banner__limit {
    display: none;
  }

  .cmp-warpage-form__grid {
    grid-template-columns: 1fr;
  }

  .cmp-warpage-form__full {
    grid-column: auto;
  }

  .cmp-warpage-action-toolbar > span {
    display: none;
  }
}
</style>
