<script lang="ts" setup>
import type { EchartsUIType } from '@vben/plugins/echarts';
import type { MesQmsEnvironmentBoardApi } from '#/api/mes/quality/environment-board';

import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';

import { useAccess } from '@vben/access';
import { Page } from '@vben/common-ui';
import { getDictOptions } from '@vben/hooks';
import { IconifyIcon } from '@vben/icons';
import { EchartsUI, useEcharts } from '@vben/plugins/echarts';
import { downloadFileFromBlobPart } from '@vben/utils';

import dayjs from 'dayjs';
import {
  Button,
  DatePicker,
  Input,
  InputNumber,
  Modal,
  RadioButton,
  RadioGroup,
  message,
} from 'ant-design-vue';

import {
  confirmEnvironmentRecord,
  correctEnvironmentRecord,
  exportEnvironmentRecords,
  getEnvironmentBoard,
  importEnvironmentRecords,
  saveEnvironmentRecord,
  saveEnvironmentStandard,
} from '#/api/mes/quality/environment-board';
import AuthModal from '#/views/mes/work-order-booking/modules/components/AuthModal.vue';

defineOptions({ name: 'MesQualityEnvironmentBoard' });

type BoardRecord = MesQmsEnvironmentBoardApi.Record;
type BoardStandard = MesQmsEnvironmentBoardApi.Standard;

interface BoardRow extends BoardRecord {
  humidityValueDraft?: number;
  remarkDraft?: string;
  temperatureValueDraft?: number;
}

interface AuthUser {
  empName?: string;
  empNo?: string;
  userId?: number;
  username?: string;
}

interface RecordEditPayload {
  correctionReason?: string;
  humidityValue: number;
  remark?: string;
  temperatureValue: number;
}

type AuthTarget =
  | {
      payload?: RecordEditPayload;
      row: BoardRow;
      type: 'confirm' | 'correct' | 'record';
    }
  | {
      type: 'import';
    }
  | undefined;

const DICT_WORKSHOP = 'mes_qms_environment_workshop';
const STATUS_NG = 'NG';
const STATUS_OK = 'OK';
const STATUS_MISSING = 'MISSING';
const RECORD_WAIT_RECORD = 'WAIT_RECORD';
const RECORD_WAIT_CONFIRM = 'WAIT_CONFIRM';
const RECORD_CONFIRMED = 'CONFIRMED';

const fallbackWorkshopOptions = [
  { label: '终检室', value: 'FINAL_INSPECTION_ROOM' },
  { label: '百级房', value: 'CLASS_100_ROOM' },
];

const query = reactive({
  recordMonth: dayjs().format('YYYY-MM'),
  workshopCode: fallbackWorkshopOptions[0].value,
});

const standardForm = reactive({
  humidityMax: 65,
  humidityMin: 45,
  remark: '',
  temperatureMax: 27,
  temperatureMin: 19,
});

const loading = ref(false);
const exportingExcel = ref(false);
const importingExcel = ref(false);
const savingStandard = ref(false);
const standardVisible = ref(false);
const editVisible = ref(false);
const editSaving = ref(false);
const confirmVisible = ref(false);
const confirmSaving = ref(false);
const authVisible = ref(false);
const authTarget = ref<AuthTarget>();
const boardRows = ref<BoardRow[]>([]);
const boardWorkshopName = ref(fallbackWorkshopOptions[0].label);
const importFileInputRef = ref<HTMLInputElement>();
const pendingImportFile = ref<File>();
const temperatureChartRef = ref<EchartsUIType>();
const humidityChartRef = ref<EchartsUIType>();
const { renderEcharts: renderTemperatureChart } = useEcharts(temperatureChartRef);
const { renderEcharts: renderHumidityChart } = useEcharts(humidityChartRef);
const route = useRoute();
const { hasAccessByCodes } = useAccess();

const editForm = reactive({
  correctionReason: '',
  humidityValue: undefined as number | undefined,
  recordDate: dayjs().format('YYYY-MM-DD'),
  remark: '',
  temperatureValue: undefined as number | undefined,
});

const confirmForm = reactive({
  recordDate: dayjs().format('YYYY-MM-DD'),
});

const workshopOptions = computed(() => {
  const dictOptions = getDictOptions(DICT_WORKSHOP, 'string') as Array<{
    label: string;
    value: string;
  }>;
  return dictOptions.length > 0 ? dictOptions : fallbackWorkshopOptions;
});

const currentWorkshopName = computed(() => {
  const option = workshopOptions.value.find(
    (item) => item.value === query.workshopCode,
  );
  return option?.label || query.workshopCode;
});

const todayRecord = computed(() => boardRows.value.find((row) => isTodayRow(row)));
const todayRecordAbnormal = computed(() => isAbnormalRecord(todayRecord.value));
const editTargetRow = computed(() => findRowByDate(editForm.recordDate));
const confirmTargetRow = computed(() => findRowByDate(confirmForm.recordDate));
const editDateIsPast = computed(() => dayjs(editForm.recordDate).isBefore(dayjs(), 'day'));

const canCorrectHistory = computed(() =>
  hasAccessByCodes(['mes:qms-environment-board:correct']),
);

const dayCount = computed(() => boardRows.value.length || dayjs(`${query.recordMonth}-01`).daysInMonth());
const authActionName = computed(() => {
  if (!authTarget.value) {
    return '温湿度签核';
  }
  if (authTarget.value.type === 'import') {
    return '温湿度Excel导入';
  }
  if (authTarget.value.type === 'record') {
    return `${authTarget.value.row.day}日记录`;
  }
  if (authTarget.value.type === 'correct') {
    return `${authTarget.value.row.day}日历史修正`;
  }
  return `${authTarget.value.row.day}日确认`;
});

onMounted(async () => {
  applyRouteQuery();
  await loadBoard();
});

async function loadBoard() {
  loading.value = true;
  try {
    const data = await getEnvironmentBoard({
      recordMonth: query.recordMonth,
      workshopCode: query.workshopCode,
      workshopName: currentWorkshopName.value,
    });
    boardWorkshopName.value = data.workshopName || currentWorkshopName.value;
    syncStandard(data.standard);
    boardRows.value = (data.records || []).map(toBoardRow);
    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
}

async function handleSearch() {
  await loadBoard();
}

function applyRouteQuery() {
  const routeWorkshopCode = route.query.workshopCode;
  const routeRecordMonth = route.query.recordMonth;
  if (typeof routeWorkshopCode === 'string' && routeWorkshopCode) {
    query.workshopCode = routeWorkshopCode;
  }
  if (
    typeof routeRecordMonth === 'string' &&
    dayjs(`${routeRecordMonth}-01`).isValid()
  ) {
    query.recordMonth = routeRecordMonth;
  }
}

async function handleSaveStandard() {
  if (!isValidRange(standardForm.temperatureMin, standardForm.temperatureMax)) {
    message.warning('温度标准下限不能大于上限。');
    return;
  }
  if (!isValidRange(standardForm.humidityMin, standardForm.humidityMax)) {
    message.warning('湿度标准下限不能大于上限。');
    return;
  }
  savingStandard.value = true;
  try {
    const standard = await saveEnvironmentStandard({
      humidityMax: standardForm.humidityMax,
      humidityMin: standardForm.humidityMin,
      remark: standardForm.remark || undefined,
      temperatureMax: standardForm.temperatureMax,
      temperatureMin: standardForm.temperatureMin,
      workshopCode: query.workshopCode,
      workshopName: currentWorkshopName.value,
    });
    syncStandard(standard);
    standardVisible.value = false;
    await nextTick();
    renderCharts();
    message.success('标准范围已保存。');
  } finally {
    savingStandard.value = false;
  }
}

async function openTodayEditDialog() {
  editForm.recordDate = dayjs().format('YYYY-MM-DD');
  await ensureBoardMonth(editForm.recordDate);
  syncEditFormByDate();
  editVisible.value = true;
}

async function openTodayConfirmDialog() {
  confirmForm.recordDate = dayjs().format('YYYY-MM-DD');
  await ensureBoardMonth(confirmForm.recordDate);
  confirmVisible.value = true;
}

async function handleEditDateChange() {
  await ensureBoardMonth(editForm.recordDate);
  syncEditFormByDate();
}

async function handleConfirmDateChange() {
  await ensureBoardMonth(confirmForm.recordDate);
}

async function ensureBoardMonth(recordDate: string) {
  if (!dayjs(recordDate).isValid()) {
    return;
  }
  const nextMonth = dayjs(recordDate).format('YYYY-MM');
  if (query.recordMonth === nextMonth) {
    return;
  }
  query.recordMonth = nextMonth;
  await loadBoard();
}

function syncEditFormByDate() {
  const row = editTargetRow.value;
  editForm.temperatureValue = toNumber(row?.temperatureValueDraft);
  editForm.humidityValue = toNumber(row?.humidityValueDraft);
  editForm.remark = row?.remarkDraft || '';
  editForm.correctionReason = '';
}

function openEditAuth() {
  const row = editTargetRow.value;
  if (!row) {
    message.warning('未找到所选日期记录，请先切换到对应月份。');
    return;
  }
  if (isFutureRow(row)) {
    message.warning('未来日期不能记录。');
    return;
  }
  if (!isValidRangeValue(editForm.temperatureValue) || !isValidRangeValue(editForm.humidityValue)) {
    message.warning('请填写温度和湿度。');
    return;
  }
  if (isPastRow(row)) {
    if (!canCorrectHistory.value) {
      message.warning('历史日期修改需要修正权限。');
      return;
    }
    if (!editForm.correctionReason.trim()) {
      message.warning('请填写历史修正原因。');
      return;
    }
  }
  editSaving.value = true;
  authTarget.value = {
    payload: {
      correctionReason: editForm.correctionReason,
      humidityValue: Number(editForm.humidityValue),
      remark: editForm.remark || undefined,
      temperatureValue: Number(editForm.temperatureValue),
    },
    row,
    type: isPastRow(row) ? 'correct' : 'record',
  };
  authVisible.value = true;
}

function openConfirmDialogAuth() {
  const row = confirmTargetRow.value;
  if (!row) {
    message.warning('未找到所选日期记录，请先切换到对应月份。');
    return;
  }
  confirmSaving.value = true;
  if (!openConfirmAuth(row)) {
    confirmSaving.value = false;
  }
}

function openConfirmAuth(row: BoardRow) {
  if (isFutureRow(row)) {
    message.warning('未来日期不能确认。');
    return false;
  }
  if (isPastRow(row) && !canCorrectHistory.value) {
    message.warning('历史日期确认需要修正权限。');
    return false;
  }
  if (row.recordStatus === RECORD_CONFIRMED) {
    message.info('当天记录已确认。');
    return false;
  }
  if (!row.id || row.recordStatus === RECORD_WAIT_RECORD) {
    message.warning('请先保存当天记录后再确认。');
    return false;
  }
  if (hasUnsavedValue(row)) {
    message.warning('当前数值已修改，请先重新记录后再确认。');
    return false;
  }
  authTarget.value = { row, type: 'confirm' };
  authVisible.value = true;
  return true;
}

async function handleExportExcel() {
  exportingExcel.value = true;
  try {
    const data = await exportEnvironmentRecords({
      recordMonth: query.recordMonth,
      workshopCode: query.workshopCode,
      workshopName: currentWorkshopName.value,
    });
    downloadFileFromBlobPart({
      fileName: `温湿度记录_${currentWorkshopName.value}_${query.recordMonth}.xlsx`,
      source: data,
    });
  } finally {
    exportingExcel.value = false;
  }
}

function triggerImportExcel() {
  if (!dayjs(`${query.recordMonth}-01`).isSame(dayjs(), 'month') && !canCorrectHistory.value) {
    message.warning('导入历史月份记录需要修正权限。');
    return;
  }
  importFileInputRef.value?.click();
}

function handleImportFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) {
    return;
  }
  pendingImportFile.value = file;
  authTarget.value = { type: 'import' };
  authVisible.value = true;
}

async function handleAuthSuccess(user: AuthUser) {
  const target = authTarget.value;
  if (!target) {
    return;
  }
  try {
    if (target.type === 'import') {
      await importWithAuth(user);
      authTarget.value = undefined;
      return;
    }
    if (target.type === 'record') {
      const payload = target.payload || {
        humidityValue: Number(target.row.humidityValueDraft),
        remark: target.row.remarkDraft || undefined,
        temperatureValue: Number(target.row.temperatureValueDraft),
      };
      const record = await saveEnvironmentRecord({
        humidityValue: payload.humidityValue,
        recordDate: target.row.recordDate,
        recorderId: user.userId,
        recorderName: user.empName || user.username || user.empNo || '',
        recorderUsername: user.username || user.empNo,
        remark: payload.remark,
        temperatureValue: payload.temperatureValue,
        workshopCode: query.workshopCode,
        workshopName: currentWorkshopName.value,
      });
      syncRecord(target.row, record);
      editVisible.value = false;
      message.success('温湿度记录已保存。');
    } else if (target.type === 'correct') {
      const payload = target.payload;
      if (!payload) {
        authTarget.value = undefined;
        message.warning('修正数据为空，请重新打开记录弹窗。');
        return;
      }
      const record = await correctEnvironmentRecord({
        correctionReason: payload.correctionReason || '',
        humidityValue: payload.humidityValue,
        recordDate: target.row.recordDate,
        recorderId: user.userId,
        recorderName: user.empName || user.username || user.empNo || '',
        recorderUsername: user.username || user.empNo,
        remark: payload.remark,
        temperatureValue: payload.temperatureValue,
        workshopCode: query.workshopCode,
        workshopName: currentWorkshopName.value,
      });
      syncRecord(target.row, record);
      editVisible.value = false;
      message.success('历史温湿度记录已修正。');
    } else {
      const record = await confirmEnvironmentRecord({
        confirmerId: user.userId,
        confirmerName: user.empName || user.username || user.empNo || '',
        confirmerUsername: user.username || user.empNo,
        recordDate: target.row.recordDate,
        workshopCode: query.workshopCode,
        workshopName: currentWorkshopName.value,
      });
      syncRecord(target.row, record);
      confirmVisible.value = false;
      message.success('温湿度记录已确认。');
    }
    authTarget.value = undefined;
    await nextTick();
    renderCharts();
  } finally {
    authVisible.value = false;
    editSaving.value = false;
    confirmSaving.value = false;
  }
}

async function importWithAuth(user: AuthUser) {
  if (!pendingImportFile.value) {
    message.warning('请选择要导入的 Excel 文件。');
    return;
  }
  importingExcel.value = true;
  try {
    const result = await importEnvironmentRecords({
      file: pendingImportFile.value,
      operatorId: user.userId,
      operatorName: user.empName || user.username || user.empNo || '',
      operatorUsername: user.username || user.empNo,
      recordMonth: query.recordMonth,
      workshopCode: query.workshopCode,
      workshopName: currentWorkshopName.value,
    });
    if (result.failureCount && result.failureCount > 0) {
      Modal.error({
        title: '导入校验未通过',
        content: (result.failures || []).slice(0, 8).join('\n') || '请检查Excel内容。',
      });
      return;
    }
    message.success((result.messages || [])[0] || '温湿度记录导入完成。');
    await loadBoard();
  } finally {
    importingExcel.value = false;
    pendingImportFile.value = undefined;
  }
}

function syncStandard(standard?: BoardStandard) {
  if (!standard) {
    return;
  }
  standardForm.humidityMax = toNumber(standard.humidityMax) ?? standardForm.humidityMax;
  standardForm.humidityMin = toNumber(standard.humidityMin) ?? standardForm.humidityMin;
  standardForm.remark = standard.remark || '';
  standardForm.temperatureMax = toNumber(standard.temperatureMax) ?? standardForm.temperatureMax;
  standardForm.temperatureMin = toNumber(standard.temperatureMin) ?? standardForm.temperatureMin;
}

function syncRecord(row: BoardRow, record: BoardRecord) {
  Object.assign(row, toBoardRow(record));
}

function toBoardRow(record: BoardRecord): BoardRow {
  return {
    ...record,
    humidityValueDraft: toNumber(record.humidityValue),
    remarkDraft: record.remark || '',
    temperatureValueDraft: toNumber(record.temperatureValue),
  };
}

function renderCharts() {
  renderMetricChart('temperature');
  renderMetricChart('humidity');
}

function renderMetricChart(metric: 'humidity' | 'temperature') {
  const isTemperature = metric === 'temperature';
  const min = isTemperature ? standardForm.temperatureMin : standardForm.humidityMin;
  const max = isTemperature ? standardForm.temperatureMax : standardForm.humidityMax;
  const days = boardRows.value.map((row) => row.day);
  const values = boardRows.value.map((row) =>
    toNumber(isTemperature ? row.temperatureValueDraft : row.humidityValueDraft),
  );
  const axisRange = getAxisRange(values, min, max, isTemperature ? 2 : 5);
  const renderChart = isTemperature ? renderTemperatureChart : renderHumidityChart;
  const name = isTemperature ? '温度' : '湿度';
  const unit = isTemperature ? '℃' : '%RH';
  renderChart({
    animationDuration: 250,
    color: [isTemperature ? '#2563eb' : '#0f766e'],
    grid: {
      bottom: 34,
      containLabel: true,
      left: 44,
      right: 86,
      top: 44,
    },
    legend: {
      data: [name],
      right: 12,
      top: 6,
    },
    series: [
      {
        connectNulls: false,
        data: boardRows.value.map((row) => {
          const value = toNumber(
            isTemperature ? row.temperatureValueDraft : row.humidityValueDraft,
          );
          if (value === undefined) {
            return null;
          }
          const abnormal = isValueOut(value, min, max);
          return {
            itemStyle: { color: abnormal ? '#dc2626' : undefined },
            symbolSize: abnormal ? 10 : 7,
            value,
          };
        }),
        lineStyle: {
          color: isTemperature ? '#2563eb' : '#0f766e',
          width: 2,
        },
        name,
        smooth: true,
        symbol: 'circle',
        type: 'line',
        z: 3,
      },
      buildLimitLineSeries(min),
      buildLimitLineSeries(max),
    ],
    tooltip: {
      formatter(
        params: Array<{
          data?: number | { value?: number };
          marker?: string;
          name?: string;
          seriesName?: string;
        }>,
      ) {
        const item = params.find((param) => param.seriesName === name) || params[0];
        const data = item?.data;
        const value = typeof data === 'object' ? data?.value : data;
        return `${item?.marker || ''}${item?.name || '-'}日 ${name}: ${formatNumber(value)}${unit}`;
      },
      trigger: 'axis',
    },
    xAxis: {
      axisTick: { alignWithLabel: true },
      data: days,
      name: '日',
      type: 'category',
    },
    yAxis: {
      max: axisRange.max,
      min: axisRange.min,
      name: unit,
      type: 'value',
    },
  });
}

function buildLimitLineSeries(value: number) {
  return {
    clip: false,
    data: boardRows.value.map(() => value),
    emphasis: { disabled: true },
    endLabel: {
      color: '#dc2626',
      distance: 8,
      fontSize: 12,
      fontWeight: 700,
      formatter: formatNumber(value),
      show: true,
    },
    lineStyle: {
      color: '#dc2626',
      type: 'dashed',
      width: 1,
    },
    showSymbol: false,
    silent: true,
    symbol: 'none',
    tooltip: { show: false },
    type: 'line',
    z: 2,
  };
}

function getTemperatureStatus(row: BoardRow) {
  return judgeValue(
    row.temperatureValueDraft,
    standardForm.temperatureMin,
    standardForm.temperatureMax,
  );
}

function getHumidityStatus(row: BoardRow) {
  return judgeValue(
    row.humidityValueDraft,
    standardForm.humidityMin,
    standardForm.humidityMax,
  );
}

function getMetricCellClass(row: BoardRow, metric: 'humidity' | 'temperature') {
  const status = metric === 'temperature' ? getTemperatureStatus(row) : getHumidityStatus(row);
  return [
    'day-cell',
    {
      'day-cell--abnormal': status === STATUS_NG || isAbnormalRecord(row),
      'day-cell--today': isTodayRow(row),
    },
  ];
}

function getDayCellClass(row: BoardRow, extraClass?: string) {
  return [
    'day-cell',
    extraClass,
    {
      'day-cell--abnormal': isAbnormalRecord(row),
      'day-cell--today': isTodayRow(row),
    },
  ];
}

function getMetricValueClass(row: BoardRow | undefined, metric: 'humidity' | 'temperature') {
  const status = row ? (metric === 'temperature' ? getTemperatureStatus(row) : getHumidityStatus(row)) : STATUS_MISSING;
  return [
    'metric-value',
    {
      'metric-value--abnormal': status === STATUS_NG,
      'metric-value--missing': status === STATUS_MISSING,
    },
  ];
}

function isAbnormalRecord(row?: BoardRow) {
  return !!row && (getTemperatureStatus(row) === STATUS_NG || getHumidityStatus(row) === STATUS_NG);
}

function findRowByDate(recordDate?: string) {
  if (!recordDate || !dayjs(recordDate).isValid()) {
    return undefined;
  }
  const normalizedDate = dayjs(recordDate).format('YYYY-MM-DD');
  return boardRows.value.find((row) => row.recordDate === normalizedDate);
}

function judgeValue(value: number | undefined, min: number, max: number) {
  const numericValue = toNumber(value);
  if (numericValue === undefined) {
    return STATUS_MISSING;
  }
  return isValueOut(numericValue, min, max) ? STATUS_NG : STATUS_OK;
}

function hasUnsavedValue(row: BoardRow) {
  return (
    toNumber(row.temperatureValue) !== toNumber(row.temperatureValueDraft) ||
    toNumber(row.humidityValue) !== toNumber(row.humidityValueDraft)
  );
}

function isTodayRow(row: BoardRow) {
  return dayjs(row.recordDate).isSame(dayjs(), 'day');
}

function isPastRow(row: BoardRow) {
  return dayjs(row.recordDate).isBefore(dayjs(), 'day');
}

function isFutureRow(row: BoardRow) {
  return dayjs(row.recordDate).isAfter(dayjs(), 'day');
}

function isValueOut(value: number, min: number, max: number) {
  return value < min || value > max;
}

function isValidRange(min: number | undefined, max: number | undefined) {
  return min !== undefined && max !== undefined && Number(min) <= Number(max);
}

function isValidRangeValue(value: number | undefined) {
  return value !== undefined && Number.isFinite(Number(value));
}

function toNumber(value: number | string | undefined) {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  const numericValue = Number(value);
  return Number.isFinite(numericValue) ? numericValue : undefined;
}

function getAxisRange(
  values: Array<number | undefined>,
  standardMin: number,
  standardMax: number,
  padding: number,
) {
  const actualValues = values.filter((value): value is number => value !== undefined);
  const low = Math.min(standardMin, ...actualValues);
  const high = Math.max(standardMax, ...actualValues);
  return {
    max: Math.ceil(high + padding),
    min: Math.floor(low - padding),
  };
}

function formatNumber(value?: number) {
  if (value === undefined || value === null || Number.isNaN(Number(value))) {
    return '-';
  }
  return Number(value).toFixed(2).replace(/\.?0+$/, '');
}

function formatTime(time?: string) {
  if (!time) {
    return '-';
  }
  return dayjs(time).format('HH:mm');
}

function openRemarkDetail(row: BoardRow) {
  if (!row.remarkDraft) {
    return;
  }
  Modal.info({
    title: `${row.recordDate} 备注`,
    content: row.remarkDraft,
  });
}

function getRecordStatusText(row: BoardRow) {
  if (row.recordStatus === RECORD_CONFIRMED) {
    return '已确认';
  }
  if (row.recordStatus === RECORD_WAIT_CONFIRM) {
    return '待确认';
  }
  return '待记录';
}

function getStatusBadgeClass(row?: BoardRow) {
  const status = row?.recordStatus || RECORD_WAIT_RECORD;
  return [
    'status-badge',
    {
      'status-badge--confirmed': status === RECORD_CONFIRMED,
      'status-badge--pending': status === RECORD_WAIT_CONFIRM,
      'status-badge--waiting': status === RECORD_WAIT_RECORD,
    },
  ];
}
</script>

<template>
  <Page auto-content-height>
    <div class="environment-board">
      <section class="environment-toolbar">
        <div class="toolbar-main">
          <div class="toolbar-title">
            <IconifyIcon icon="lucide:thermometer-sun" class="toolbar-title__icon" />
            <div>
              <div class="toolbar-title__text">温湿度录入看板</div>
              <div class="toolbar-title__sub">{{ boardWorkshopName }} / {{ query.recordMonth }}</div>
            </div>
          </div>
          <div class="toolbar-filters">
            <RadioGroup
              v-model:value="query.workshopCode"
              button-style="solid"
              class="workshop-radio"
              @change="() => handleSearch()"
            >
              <RadioButton
                v-for="item in workshopOptions"
                :key="item.value"
                :value="item.value"
              >
                {{ item.label }}
              </RadioButton>
            </RadioGroup>
            <DatePicker
              v-model:value="query.recordMonth"
              picker="month"
              value-format="YYYY-MM"
              :allow-clear="false"
              class="filter-control filter-control--month"
              @change="handleSearch"
            />
            <Button :loading="loading" @click="handleSearch">
              <template #icon>
                <IconifyIcon icon="lucide:search" />
              </template>
              查询
            </Button>
            <Button
              v-access:code="['mes:qms-environment-board:export']"
              :loading="exportingExcel"
              @click="handleExportExcel"
            >
              <template #icon>
                <IconifyIcon icon="lucide:file-down" />
              </template>
              导出
            </Button>
            <Button
              v-access:code="['mes:qms-environment-board:import']"
              :loading="importingExcel"
              @click="triggerImportExcel"
            >
              <template #icon>
                <IconifyIcon icon="lucide:file-up" />
              </template>
              导入
            </Button>
            <Button
              v-access:code="['mes:qms-environment-board:standard']"
              @click="standardVisible = true"
            >
              <template #icon>
                <IconifyIcon icon="lucide:sliders-horizontal" />
              </template>
              标准
            </Button>
            <input
              ref="importFileInputRef"
              accept=".xls,.xlsx"
              class="hidden-file-input"
              type="file"
              @change="handleImportFileChange"
            />
          </div>
        </div>
      </section>

      <section :class="['today-panel', { 'today-panel--abnormal': todayRecordAbnormal }]">
        <button class="today-panel__main" type="button" @click="openTodayEditDialog">
          <span class="today-panel__label">当天温湿度</span>
          <span class="today-panel__date">{{ dayjs().format('YYYY-MM-DD') }}</span>
          <span class="today-panel__metric">
            温度
            <strong :class="getMetricValueClass(todayRecord, 'temperature')">
              {{ formatNumber(todayRecord?.temperatureValueDraft) }}℃
            </strong>
          </span>
          <span class="today-panel__metric">
            湿度
            <strong :class="getMetricValueClass(todayRecord, 'humidity')">
              {{ formatNumber(todayRecord?.humidityValueDraft) }}%RH
            </strong>
          </span>
          <span :class="getStatusBadgeClass(todayRecord)">
            {{ todayRecord ? getRecordStatusText(todayRecord) : '待记录' }}
          </span>
        </button>
        <div class="today-panel__actions">
          <Button v-access:code="['mes:qms-environment-board:record']" type="primary" @click="openTodayEditDialog">
            记录
          </Button>
          <Button v-access:code="['mes:qms-environment-board:confirm']" @click="openTodayConfirmDialog">
            确认
          </Button>
        </div>
      </section>

      <section class="chart-grid">
        <div class="chart-panel">
          <div class="chart-panel__title">温度折线图</div>
          <EchartsUI ref="temperatureChartRef" class="chart-canvas" />
        </div>
        <div class="chart-panel">
          <div class="chart-panel__title">湿度折线图</div>
          <EchartsUI ref="humidityChartRef" class="chart-canvas" />
        </div>
      </section>

      <section class="entry-table-wrap">
        <div class="entry-table-scroll">
          <table class="entry-table">
            <thead>
              <tr>
                <th class="row-head">日期</th>
                <th
                  v-for="row in boardRows"
                  :key="`day-${row.day}`"
                  :class="['day-head', { 'day-head--today': isTodayRow(row) }]"
                >
                  {{ row.day }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th class="row-head">温度℃</th>
                <td
                  v-for="row in boardRows"
                  :key="`temperature-${row.day}`"
                  :class="getMetricCellClass(row, 'temperature')"
                >
                  <span :class="getMetricValueClass(row, 'temperature')">
                    {{ formatNumber(row.temperatureValueDraft) }}
                  </span>
                </td>
              </tr>
              <tr>
                <th class="row-head">湿度%RH</th>
                <td
                  v-for="row in boardRows"
                  :key="`humidity-${row.day}`"
                  :class="getMetricCellClass(row, 'humidity')"
                >
                  <span :class="getMetricValueClass(row, 'humidity')">
                    {{ formatNumber(row.humidityValueDraft) }}
                  </span>
                </td>
              </tr>
              <tr>
                <th class="row-head">记录</th>
                <td
                  v-for="row in boardRows"
                  :key="`record-${row.day}`"
                  :class="getDayCellClass(row, 'action-cell')"
                >
                  <div class="person-line">{{ row.recorderName || '-' }}</div>
                  <div class="time-line">{{ formatTime(row.recordTime) }}</div>
                </td>
              </tr>
              <tr>
                <th class="row-head">确认</th>
                <td
                  v-for="row in boardRows"
                  :key="`confirm-${row.day}`"
                  :class="getDayCellClass(row, 'action-cell')"
                >
                  <div class="person-line">{{ row.confirmerName || '-' }}</div>
                  <div class="time-line">{{ formatTime(row.confirmTime) }}</div>
                </td>
              </tr>
              <tr>
                <th class="row-head">备注</th>
                <td
                  v-for="row in boardRows"
                  :key="`remark-${row.day}`"
                  :class="getDayCellClass(row)"
                >
                  <button
                    v-if="row.remarkDraft"
                    class="remark-display remark-display--clickable"
                    type="button"
                    @click="openRemarkDetail(row)"
                  >
                    {{ row.remarkDraft }}
                  </button>
                  <span v-else class="remark-display">-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="table-footer">
          <span>日历天数：{{ dayCount }}</span>
          <span>未生产日可在备注中标记。</span>
        </div>
      </section>
    </div>

    <AuthModal
      v-model:visible="authVisible"
      auth-mode="username"
      :action-name="authActionName"
      :title="
        authTarget?.type === 'confirm'
          ? '温湿度确认签核'
          : authTarget?.type === 'correct'
            ? '温湿度历史修正签核'
            : authTarget?.type === 'import'
              ? '温湿度Excel导入签核'
              : '温湿度记录签核'
      "
      :workstation="currentWorkshopName"
      @success="handleAuthSuccess"
      @cancel="
        authTarget = undefined;
        editSaving = false;
        confirmSaving = false;
      "
    />
    <Modal
      v-model:open="editVisible"
      :confirm-loading="editSaving"
      title="温湿度记录"
      ok-text="认证保存"
      cancel-text="取消"
      @ok="openEditAuth"
    >
      <div class="daily-modal-form">
        <label>
          日期
          <DatePicker
            v-model:value="editForm.recordDate"
            value-format="YYYY-MM-DD"
            :allow-clear="false"
            class="daily-modal-form__date"
            @change="handleEditDateChange"
          />
        </label>
        <label>
          温度℃
          <InputNumber
            v-model:value="editForm.temperatureValue"
            :precision="2"
            :step="0.1"
            class="daily-modal-form__number"
          />
        </label>
        <label>
          湿度%RH
          <InputNumber
            v-model:value="editForm.humidityValue"
            :precision="2"
            :step="0.1"
            class="daily-modal-form__number"
          />
        </label>
        <label v-if="editDateIsPast" class="daily-modal-form__full">
          修正原因
          <Input v-model:value="editForm.correctionReason" placeholder="请填写历史修正原因" />
        </label>
        <label class="daily-modal-form__full">
          备注
          <Input v-model:value="editForm.remark" placeholder="备注" allow-clear />
        </label>
      </div>
    </Modal>
    <Modal
      v-model:open="confirmVisible"
      :confirm-loading="confirmSaving"
      title="温湿度确认"
      ok-text="认证确认"
      cancel-text="取消"
      @ok="openConfirmDialogAuth"
    >
      <div class="confirm-modal-form">
        <label>
          日期
          <DatePicker
            v-model:value="confirmForm.recordDate"
            value-format="YYYY-MM-DD"
            :allow-clear="false"
            class="daily-modal-form__date"
            @change="handleConfirmDateChange"
          />
        </label>
        <div :class="['confirm-preview', { 'confirm-preview--abnormal': isAbnormalRecord(confirmTargetRow) }]">
          <div>
            <span>温度</span>
            <strong :class="getMetricValueClass(confirmTargetRow, 'temperature')">
              {{ formatNumber(confirmTargetRow?.temperatureValueDraft) }}℃
            </strong>
          </div>
          <div>
            <span>湿度</span>
            <strong :class="getMetricValueClass(confirmTargetRow, 'humidity')">
              {{ formatNumber(confirmTargetRow?.humidityValueDraft) }}%RH
            </strong>
          </div>
          <span :class="getStatusBadgeClass(confirmTargetRow)">
            {{ confirmTargetRow ? getRecordStatusText(confirmTargetRow) : '待记录' }}
          </span>
        </div>
      </div>
    </Modal>
    <Modal
      v-model:open="standardVisible"
      :confirm-loading="savingStandard"
      title="温湿度标准设置"
      ok-text="保存标准"
      cancel-text="取消"
      @ok="handleSaveStandard"
    >
      <div class="standard-modal-form">
        <label>
          温度下限
          <InputNumber
            v-model:value="standardForm.temperatureMin"
            :precision="2"
            :step="0.1"
            class="standard-modal-form__number"
            @change="renderCharts"
          />
        </label>
        <label>
          温度上限
          <InputNumber
            v-model:value="standardForm.temperatureMax"
            :precision="2"
            :step="0.1"
            class="standard-modal-form__number"
            @change="renderCharts"
          />
        </label>
        <label>
          湿度下限
          <InputNumber
            v-model:value="standardForm.humidityMin"
            :precision="2"
            :step="0.1"
            class="standard-modal-form__number"
            @change="renderCharts"
          />
        </label>
        <label>
          湿度上限
          <InputNumber
            v-model:value="standardForm.humidityMax"
            :precision="2"
            :step="0.1"
            class="standard-modal-form__number"
            @change="renderCharts"
          />
        </label>
        <label class="standard-modal-form__full">
          标准备注
          <Input v-model:value="standardForm.remark" placeholder="标准备注" allow-clear />
        </label>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
.environment-board {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  gap: 12px;
}

.environment-toolbar,
.entry-table-wrap {
  border: 1px solid #e5e7eb;
  background: #fff;
}

.environment-toolbar {
  padding: 14px 16px 12px;
}

.toolbar-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-title {
  display: flex;
  min-width: 260px;
  align-items: center;
  gap: 10px;
}

.toolbar-title__icon {
  color: #0f766e;
  font-size: 30px;
}

.toolbar-title__text {
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
}

.toolbar-title__sub {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.toolbar-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.workshop-radio {
  white-space: nowrap;
}

.filter-control--month {
  width: 140px;
}

.hidden-file-input {
  display: none;
}

.today-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  gap: 12px;
  padding: 10px 14px;
}

.today-panel--abnormal {
  border-color: #fecaca;
  background: #fff1f2;
}

.today-panel__main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  align-items: center;
  border: 0;
  background: transparent;
  color: #0f172a;
  cursor: pointer;
  gap: 12px;
  padding: 0;
  text-align: left;
}

.today-panel__label {
  color: #0f172a;
  font-size: 15px;
  font-weight: 700;
}

.today-panel__date {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.today-panel__metric {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  color: #475569;
  font-size: 13px;
}

.today-panel__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.metric-value {
  color: #0f172a;
  font-weight: 700;
}

.metric-value--abnormal {
  color: #b91c1c;
}

.metric-value--missing {
  color: #94a3b8;
  font-weight: 500;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  line-height: 20px;
  min-width: 48px;
  padding: 0 6px;
}

.status-badge--confirmed {
  border-color: #86efac;
  background: #f0fdf4;
  color: #15803d;
}

.status-badge--pending {
  border-color: #93c5fd;
  background: #eff6ff;
  color: #1d4ed8;
}

.status-badge--waiting {
  border-color: #e2e8f0;
  background: #f8fafc;
  color: #64748b;
}

.chart-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.chart-panel {
  min-width: 0;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.chart-panel__title {
  padding: 9px 12px 0;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.chart-canvas {
  height: 300px;
}

.entry-table-wrap {
  min-height: 0;
}

.entry-table-scroll {
  overflow: auto;
}

.entry-table {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  table-layout: fixed;
}

.entry-table th,
.entry-table td {
  height: 44px;
  border-right: 1px solid #dbe3ee;
  border-bottom: 1px solid #dbe3ee;
  text-align: center;
  vertical-align: middle;
}

.entry-table thead th {
  position: sticky;
  z-index: 4;
  top: 0;
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

.row-head {
  position: sticky;
  left: 0;
  z-index: 6;
  width: 88px;
  min-width: 88px;
  background: #f8fafc;
  box-shadow: 1px 0 0 #dbe3ee;
  color: #334155;
  font-weight: 700;
}

.entry-table thead .row-head {
  z-index: 8;
}

.day-head,
.day-cell {
  width: 78px;
  min-width: 78px;
}

.day-cell {
  background: #fff;
}

.entry-table thead th.day-head--today {
  background: #dbeafe;
  color: #1d4ed8;
  box-shadow:
    inset 2px 0 0 #2563eb,
    inset -2px 0 0 #93c5fd;
}

.day-cell--today {
  background: #eff6ff;
  box-shadow:
    inset 2px 0 0 #2563eb,
    inset -2px 0 0 #bfdbfe;
}

.day-cell--abnormal {
  background: #fef2f2;
}

.day-cell--abnormal.day-cell--today {
  background: #fff1f2;
}

.action-cell {
  height: 78px;
  padding: 4px 2px;
}

.person-line {
  min-height: 18px;
  overflow: hidden;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-line {
  min-height: 16px;
  color: #64748b;
  font-size: 11px;
}

.remark-display {
  display: block;
  width: 68px;
  max-width: 68px;
  overflow: hidden;
  border: 0;
  margin: 0 auto;
  background: transparent;
  color: #475569;
  cursor: default;
  font-size: 12px;
  line-height: 18px;
  padding: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark-display--clickable {
  cursor: pointer;
}

.remark-display--clickable:hover {
  color: #2563eb;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 12px;
  color: #64748b;
  font-size: 12px;
}

.standard-modal-form {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.standard-modal-form label {
  display: grid;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.standard-modal-form__full {
  grid-column: 1 / -1;
}

.standard-modal-form__number {
  width: 100%;
}

.daily-modal-form,
.confirm-modal-form {
  display: grid;
  gap: 12px;
}

.daily-modal-form {
  grid-template-columns: 1fr;
}

.daily-modal-form label,
.confirm-modal-form label {
  display: grid;
  gap: 6px;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.daily-modal-form__full {
  grid-column: 1 / -1;
}

.daily-modal-form__date,
.daily-modal-form__number {
  width: 100%;
}

.confirm-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  gap: 12px;
  padding: 10px 12px;
}

.confirm-preview--abnormal {
  border-color: #fecaca;
  background: #fff1f2;
}

@media (max-width: 1180px) {
  .toolbar-main {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-filters {
    justify-content: flex-start;
  }

  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
