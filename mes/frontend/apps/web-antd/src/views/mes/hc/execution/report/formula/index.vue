<script lang="ts" setup>
import dayjs from 'dayjs';

import {
  getFormulaReportTaskList,
  submitFormulaReport,
} from '#/api/mes/hc/execution/formula-report';

import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';

defineOptions({ name: 'MesExecutionFormulaReport' });

const formulaColumns = [
  { field: 'planNo', title: '计划号', width: 150, fixed: 'left' },
  {
    field: 'productionStartDate',
    title: '计划开始日期',
    width: 120,
    formatter: ({ row }: any) => row.productionStartDate || '-',
  },
  { field: 'motherModelCode', title: '产品型号', minWidth: 120 },
  {
    field: 'batchNo',
    title: '母批批号',
    minWidth: 170,
    formatter: ({ row }: any) => row.productionBatchNo || row.batchNo || '-',
  },
  { field: 'motherMaterialCode', title: '产品料号', minWidth: 140 },
  { field: 'modelCode', title: '产品型号', minWidth: 120, visible: false },
  { field: 'spec', title: '尺寸规格', minWidth: 100, visible: false },
  {
    field: 'productionDate',
    title: '生产日期',
    width: 120,
    formatter: ({ row }: any) => row.productionDate || '-',
  },
  { field: 'requirements', title: '执行要求', minWidth: 220, showOverflow: 'tooltip' },
  { field: 'process', title: '执行工序', width: 100 },
  { field: 'mixerEquipmentCode', title: '搅拌机台号', minWidth: 130, formatter: ({ row }: any) => row.mixerEquipmentCode || '-' },
  { field: 'foamingEquipmentCode', title: '脱泡机台号', minWidth: 130, formatter: ({ row }: any) => row.foamingEquipmentCode || '-' },
  {
    field: 'goodQty',
    title: '已报量',
    width: 120,
    align: 'right',
    formatter: ({ row }: any) => `${row.goodQty ?? 0} ${row.uom || ''}`,
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    slots: { default: 'statusSlot' },
  },
  { field: 'startTime', title: '开工时间', width: 160 },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } },
];

const scene = {
  title: '配料报工',
  compactHeader: true,
  consoleIcon: 'lucide:flask-conical',
  enablePager: true,
  refreshTaskListOnActivated: true,
  hideDeviceLoginAction: true,
  hideQuickCheckActions: true,
  hideBannerPersonnel: true,
  industrialConsole: true,
  batchNoLabel: '母批批号',
  keywordPlaceholder: '计划号 / 产品型号 / 产品料号 / 母批批号',
  processLabel: '配料',
  workstationName: '配料工位一线',
  deviceRole: '',
  deviceId: '',
  deviceName: '',
  columns: formulaColumns,
  fetchTasks: async ({ product, motherMaterial, motherModel, statusTab, taskKeyword }: any) => {
    return await getFormulaReportTaskList({
      productKeyword: product,
      motherMaterialKeyword: motherMaterial,
      motherModelKeyword: motherModel,
      taskKeyword,
      taskStatus: statusTab,
    });
  },
  submitReport: async ({
    batchNo,
    batchingNo,
    endTime,
    feedBatchNo,
    feedQty,
    filterBatchNo,
    inputWeight,
    batchingTankNo,
    defoamingTankNo,
    laborHours,
    foamingEquipmentCode,
    foamingEquipmentId,
    foamingEquipmentName,
    mixerEquipmentCode,
    mixerEquipmentId,
    mixerEquipmentName,
    planId,
    planOperationId,
    recipeCode,
    recipeName,
    recorderName,
    recorderTime,
    reportDate,
    remark,
    slurryTemperature,
    startTime,
    stirEndTime,
    stirStartTime,
    viscosity,
  }: any) => {
    const reportQty = Number(feedQty ?? 0);
    return await submitFormulaReport({
      batchNo,
      batchingNo,
      endTime,
      feedBatchNo,
      feedQty,
      filterBatchNo,
      inputWeight,
      batchingTankNo,
      defoamingTankNo,
      foamingEquipmentCode,
      foamingEquipmentId,
      foamingEquipmentName,
      goodQty: reportQty,
      laborHours,
      mixerEquipmentCode,
      mixerEquipmentId,
      mixerEquipmentName,
      planId,
      planOperationId,
      recipeCode,
      recipeName,
      recorderName,
      recorderTime,
      remark,
      reportDate: reportDate || dayjs(endTime || startTime || new Date()).format('YYYY-MM-DD'),
      reportType: 'END',
      scrapQty: 0,
      scrapReason: undefined,
      slurryTemperature,
      startTime,
      stirEndTime,
      stirStartTime,
      viscosity,
      defects: [],
    });
  },
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>
