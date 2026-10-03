<script lang="ts" setup>
import { getAdhesiveReportTaskList } from '#/api/mes/hc/execution/adhesive-report';

import WorkstationProcessReportPage from '../shared/WorkstationProcessReportPage.vue';
import AdhesiveReportTaskDetailModal from './modules/AdhesiveReportTaskDetailModal.vue';

defineOptions({ name: 'MesExecutionTapeReport' });

const normalizeTaskRows = (payload: any): any[] => {
  const source = payload?.data ?? payload;
  if (Array.isArray(source)) return source;
  if (Array.isArray(source?.list)) return source.list;
  if (Array.isArray(source?.rows)) return source.rows;
  if (Array.isArray(source?.records)) return source.records;
  if (Array.isArray(source?.items)) return source.items;
  return [];
};

const adhesiveColumns = [
  { field: 'planNo', title: '计划号', width: 150, fixed: 'left' },
  { field: 'sourceModeName', title: '来源类型', width: 110, formatter: ({ row }: any) => row.sourceModeName || '-' },
  { field: 'sourcePlanNo', title: '来源计划', width: 150, formatter: ({ row }: any) => row.sourcePlanNo || row.planNo || '-' },
  { field: 'sourceProductionBatchNo', title: '来源分段批次', minWidth: 150, formatter: ({ row }: any) => row.sourceProductionBatchNo || '-' },
  { field: 'sourceBatchNo', title: '来源母批', minWidth: 140, formatter: ({ row }: any) => row.sourceMotherBatchNo || row.sourceBatchNo || '-' },
  { field: 'motherMaterialCode', title: '母料料号', minWidth: 140, formatter: ({ row }: any) => row.motherMaterialCode || '-' },
  { field: 'motherModelCode', title: '母料型号', minWidth: 120, formatter: ({ row }: any) => row.motherModelCode || '-' },
  { field: 'modelCode', title: '产品型号', minWidth: 120, formatter: ({ row }: any) => row.modelCode || '-' },
  { field: 'materialCode', title: '产品料号', minWidth: 140, formatter: ({ row }: any) => row.materialCode || '-' },
  {
    field: 'productionDate',
    title: '生产日期',
    width: 120,
    formatter: ({ row }: any) => row.productionDate || (row.startTime ? String(row.startTime).slice(0, 10) : '-'),
  },
  {
    field: 'availableSourceLength',
    title: '可加工米数(m)',
    width: 130,
    align: 'right',
    formatter: ({ row }: any) => `${row.availableSourceLength ?? 0}`,
  },
  {
    field: 'confirmedSourceCount',
    title: '已确认来源',
    width: 110,
    align: 'right',
    formatter: ({ row }: any) => `${row.confirmedSourceCount ?? 0}`,
  },
  {
    field: 'requirements',
    title: '执行要求',
    minWidth: 220,
    showOverflow: 'tooltip',
    formatter: ({ row }: any) => row.requirements || '-',
  },
  { field: 'status', title: '状态', width: 100, align: 'center', slots: { default: 'statusSlot' } },
  { field: 'action', title: '操作', width: 120, fixed: 'right', slots: { default: 'actionSlot' } },
];

const scene = {
  title: '粘双面胶报工',
  compactHeader: true,
  enableProductionDateQuery: true,
  enablePager: true,
  enableScanEntry: true,
  scanPlanMinLength: 10,
  processLabel: '粘双面胶',
  queryProcess: '粘双面胶',
  edgeConsumableProcessCode: 'ADHESIVE1',
  detailComponent: AdhesiveReportTaskDetailModal,
  workstationName: '粘双面胶工位一线',
  deviceRole: '',
  deviceId: '',
  deviceName: '',
  columns: adhesiveColumns,
  fetchTasks: async ({ product, motherMaterial, motherModel, productionDate, statusTab, taskKeyword }: any) => {
    const rows = normalizeTaskRows(await getAdhesiveReportTaskList({
      motherMaterialKeyword: motherMaterial || undefined,
      motherModelKeyword: motherModel || undefined,
      productKeyword: product || undefined,
      productionDate: productionDate || undefined,
      taskKeyword: taskKeyword || undefined,
      taskStatus: statusTab || 'ALL',
    }));
    return rows.map((row: any) => ({
      ...row,
      process: '粘双面胶',
      materialCode: row.materialCode || '',
      modelCode: row.modelCode || '',
      motherMaterialCode: row.motherMaterialCode || '',
      motherModelCode: row.motherModelCode || '',
      requirements: row.requirements || '',
      sourceMode: row.sourceMode || '',
      sourceModeName: row.sourceModeName || '',
      sourcePlanNo: row.sourcePlanNo || '',
      sourceMotherBatchNo: row.sourceMotherBatchNo || '',
      sourceBatchNo: row.sourceBatchNo || '',
      sourceProductionBatchNo: row.sourceProductionBatchNo || '',
      sourceSegmentMarks: row.sourceSegmentMarks || '',
      sourceDetailIds: row.sourceDetailIds || '',
      inventoryLockIds: row.inventoryLockIds || '',
      availableSourceLength: row.availableSourceLength ?? 0,
      confirmedSourceCount: row.confirmedSourceCount ?? 0,
    }));
  },
};
</script>

<template>
  <WorkstationProcessReportPage :scene="scene" />
</template>
