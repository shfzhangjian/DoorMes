<script lang="ts" setup>
import {
  getRoughGrindingReportTaskList,
  submitRoughGrindingReport,
} from '#/api/mes/hc/execution/rough-grinding-report';

import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';
import RoughGrindingReportTaskDetailModal from './modules/RoughGrindingReportTaskDetailModal.vue';

defineOptions({ name: 'MesExecutionRoughGrindingReport' });

const normalizeTaskRows = (payload: any): any[] => {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.items)) return source.items;
  return [];
};

const roughColumns = [
  { field: 'planNo', title: '计划号', width: 150, fixed: 'left' },
  { field: 'motherMaterialCode', title: '母料料号', minWidth: 140, formatter: ({ row }: any) => row.motherMaterialCode || '-' },
  { field: 'motherModelCode', title: '母料型号', minWidth: 120, formatter: ({ row }: any) => row.motherModelCode || '-' },
  { field: 'modelCode', title: '成品型号', minWidth: 120, formatter: ({ row }: any) => row.modelCode || '-' },
  {
    field: 'productionStartDate',
    title: '生产日期',
    width: 120,
    formatter: ({ row }: any) => row.productionDate || (row.startTime ? String(row.startTime).slice(0, 10) : '-'),
  },
  {
    field: 'requirements',
    title: '执行要求',
    minWidth: 220,
    showOverflow: 'tooltip',
    formatter: ({ row }: any) => row.requirements || '-',
  },
  {
    field: 'motherLength',
    title: '母料米',
    width: 110,
    align: 'right',
    formatter: ({ row }: any) => `${row.motherLength ?? 0} ${row.uom || 'm'}`,
  },
  {
    field: 'remainingLength',
    title: '剩余米',
    width: 110,
    align: 'right',
    formatter: ({ row }: any) => `${row.remainingLength ?? 0} ${row.uom || 'm'}`,
  },
  {
    field: 'firstGrindingProcessLength',
    title: '1次磨皮加工米数(m)',
    minWidth: 150,
    align: 'right',
    formatter: ({ row }: any) => `${row.firstGrindingProcessLength ?? 0}`,
  },
  {
    field: 'secondGrindingProcessLength',
    title: '2次磨皮加工米数',
    width: 130,
    align: 'right',
    formatter: ({ row }: any) => `${row.secondGrindingProcessLength ?? 0}`,
  },
  {
    field: 'status',
    title: '状态',
    width: 100,
    align: 'center',
    slots: { default: 'statusSlot' },
  },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } },
];

const scene = {
  title: '磨皮报工',
  compactHeader: true,
  enableProductionDateQuery: true,
  enablePager: true,
  enableScanEntry: true,
  scanPlanMinLength: 10,
  processLabel: '磨皮',
  queryProcess: '粗磨',
  edgeConsumableProcessCode: 'ROUGH_GRINDING',
  detailComponent: RoughGrindingReportTaskDetailModal,
  workstationName: '磨皮工位一线',
  deviceRole: '',
  deviceId: '',
  deviceName: '',
  columns: roughColumns,
  fetchTasks: async ({ product, motherMaterial, motherModel, productionDate, statusTab, taskKeyword }: any) => {
    const rows = normalizeTaskRows(await getRoughGrindingReportTaskList({
      motherMaterialKeyword: motherMaterial || undefined,
      motherModelKeyword: motherModel || undefined,
      productKeyword: product || undefined,
      productionDate: productionDate || undefined,
      taskKeyword: taskKeyword || undefined,
      taskStatus: statusTab || 'ALL',
    }));
    return rows
      .map((row: any) => ({
        ...row,
        process: '磨皮',
        motherMaterialCode: row.motherMaterialCode || '',
        motherModelCode: row.motherModelCode || '',
        materialCode: row.materialCode || '',
        modelCode: row.modelCode || '',
        spec: row.spec || '',
        requirements: row.requirements || '',
        equipmentName: row.equipmentName || '',
        motherLength: row.motherLength ?? row.previousGoodQty ?? 0,
        remainingLength: row.remainingLength ?? 0,
        firstGrindingProcessLength: row.firstGrindingProcessLength ?? 0,
        secondGrindingProcessLength: row.secondGrindingProcessLength ?? 0,
      }))
      .filter((row: any) => {
        const matchMotherMaterial = !motherMaterial || String(row.motherMaterialCode || '').includes(motherMaterial);
        const matchMotherModel = !motherModel || String(row.motherModelCode || '').includes(motherModel);
        return matchMotherMaterial && matchMotherModel;
      });
  },
  submitReport: async ({
    goodQty,
    scrapQty,
    startTime,
    endTime,
    remark,
    recorderName,
    recorderTime,
    confirmerName,
    confirmerTime,
    reportDate,
    outputLength,
    inputLength,
    firstProcessLength,
    firstLossLength,
    firstOutputLength,
    firstNapSampleLength,
    secondProcessLength,
    secondLossLength,
    secondOutputLength,
    secondNapSampleLength,
    lastFirstSandpaperBatchNo,
    lastFirstSandpaperLife,
    lastFirstSandpaperLifeDays,
    lastSecondSandpaperBatchNo,
    lastSecondSandpaperLife,
    lastSecondSandpaperLifeDays,
    extraJson,
    equipmentCode,
    equipmentId,
    equipmentName,
    planId,
    planOperationId,
    batchNo,
  }: any) => {
    return await submitRoughGrindingReport({
      batchNo,
      confirmerName,
      confirmerTime,
      endTime,
      equipmentCode,
      equipmentId,
      equipmentName,
      extraJson,
      firstLossLength,
      firstNapSampleLength,
      firstOutputLength,
      firstProcessLength,
      inputLength,
      lastFirstSandpaperBatchNo,
      lastFirstSandpaperLife,
      lastFirstSandpaperLifeDays,
      lastSecondSandpaperBatchNo,
      lastSecondSandpaperLife,
      lastSecondSandpaperLifeDays,
      planId,
      planOperationId,
      recorderName,
      recorderTime,
      remark,
      reportDate,
      reportQty: goodQty ?? outputLength,
      secondLossLength,
      secondNapSampleLength,
      secondOutputLength,
      secondProcessLength,
      startTime,
    });
  },
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>
