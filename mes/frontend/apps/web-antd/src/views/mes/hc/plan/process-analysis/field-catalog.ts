import type { MesHcProcessAnalysisApi } from '#/api/mes/hc/process-analysis';

type Field = MesHcProcessAnalysisApi.FieldDefinition;

const textOperators = [
  'EQ',
  'NE',
  'CONTAINS',
  'NOT_CONTAINS',
  'IN',
  'EMPTY',
  'NOT_EMPTY',
];
const numberOperators = [
  'EQ',
  'NE',
  'GT',
  'GTE',
  'LT',
  'LTE',
  'BETWEEN',
  'EMPTY',
  'NOT_EMPTY',
];
const dateOperators = ['EQ', 'GTE', 'LTE', 'BETWEEN', 'EMPTY', 'NOT_EMPTY'];

function dimension(
  code: string,
  label: string,
  category: string,
  dataType: Field['dataType'] = 'TEXT',
  drillable = true,
): Field {
  return {
    category,
    code,
    dataType,
    drillable,
    filterOperators:
      dataType === 'DATE' || dataType === 'DATETIME'
        ? dateOperators
        : dataType === 'BOOLEAN'
          ? ['EQ', 'NE', 'EMPTY', 'NOT_EMPTY']
          : textOperators,
    label,
    role: dataType === 'DATE' || dataType === 'DATETIME' ? 'TIME' : 'DIMENSION',
    sourcePath: code,
  };
}

function metric(
  code: string,
  label: string,
  distinctKeyField = 'stageGrainKey',
  defaultAggregation: Field['defaultAggregation'] = 'SUM_DISTINCT',
): Field {
  return {
    category: '指标',
    code,
    dataType: 'NUMBER',
    defaultAggregation,
    distinctKeyField,
    drillable: false,
    filterOperators: numberOperators,
    label,
    role: 'METRIC',
    sourcePath: code,
  };
}

function detail(
  code: string,
  label: string,
  dataType: Field['dataType'] = 'TEXT',
): Field {
  return {
    category: '来源明细',
    code,
    dataType,
    drillable: false,
    filterOperators: [],
    label,
    role: 'DETAIL',
    sourcePath: code,
  };
}

export const FALLBACK_FIELD_CATALOG: Field[] = [
  dimension('planNo', '计划号', '计划'),
  dimension('planStatus', '计划状态', '计划'),
  dimension('postProcessFlag', '是否后加工', '计划', 'BOOLEAN'),
  dimension('planNoTagText', '计划标签', '计划', 'TEXT', false),
  dimension('planDate', '计划日期', '计划', 'DATE'),
  dimension('productionStartDate', '生产开始日期', '计划', 'DATE'),
  dimension('productionEndDate', '生产结束日期', '计划', 'DATE'),
  dimension('materialCode', '生产料号', '物料批次'),
  dimension('materialName', '生产物料名称', '物料批次'),
  dimension('motherMaterialCode', '母料料号', '物料批次'),
  dimension('motherMaterialName', '母料名称', '物料批次'),
  dimension('motherModelCode', '母料型号', '物料批次'),
  dimension('motherModelName', '母料型号名称', '物料批次'),
  dimension('modelCode', '成品型号', '物料批次'),
  dimension('modelName', '成品型号名称', '物料批次'),
  dimension('actualModelCode', '实际型号', '物料批次'),
  dimension('sizeSpec', '尺寸规格', '物料批次'),
  dimension('sizeName', '尺寸名称', '物料批次'),
  dimension('actualSizeSpec', '实际尺寸', '物料批次'),
  dimension('targetUom', '计划单位', '物料批次', 'TEXT', false),
  dimension('batchNo', '主批号', '物料批次'),
  dimension('productionBatchNo', '生产批号', '物料批次'),
  dimension('parentProductionBatchNo', '父生产批号', '物料批次'),
  dimension('motherRollBatchNo', '母批号', '物料批次'),
  dimension('segmentBatchNo', '分段批号', '物料批次'),
  dimension('variationStartStageCode', '差异起始工序编码', '工序'),
  dimension('variationStartStageName', '差异起始工序', '工序'),
  dimension('stageCode', '工序编码', '工序'),
  dimension('stageName', '工序名称', '工序'),
  dimension('stageStatus', '工序状态', '工序'),
  dimension('sourceBatchNos', '工序来源批号', '工序', 'TEXT', false),
  dimension('outputBatchNos', '工序产出批号', '工序', 'TEXT', false),
  dimension('reportUnit', '报工单位', '工序', 'TEXT', false),
  dimension('pendingUnit', '未加工单位', '工序', 'TEXT', false),
  dimension('lengthUnit', '长度单位', '工序', 'TEXT', false),
  dimension('stageRemark', '工序备注', '工序', 'TEXT', false),
  metric('targetQty', '计划目标量', 'planMergeKey'),
  metric('netPlanQty', '净排产量', 'planMergeKey'),
  metric('totalDefectQty', '显示行累计损耗', 'pivotRowKey'),
  metric('inputQty', '投入量'),
  metric('reportQty', '报工量'),
  metric('doneQty', '完工量'),
  metric('pendingQty', '未加工量'),
  metric('defectQty', '不合格/损耗量'),
  metric('inspectionQty', '送检数量'),
  metric('inspectionNgQty', '检验NG数量'),
  metric('confirmedQty', '已确认数量'),
  metric('lengthQty', '折算长度'),
  metric('processLength', '二次磨皮长度'),
  metric('startPosition', '二次磨皮起位置', 'stageGrainKey', 'MAX'),
  metric('pieceCount', '片级记录数'),
  metric('pieceNgCount', '不良片数'),
  metric('inspectionRecordCount', '检验记录数'),
  metric('blackDotCount', '黑点', 'qualityGrainKey'),
  metric('blueDotCount', '蓝点', 'qualityGrainKey'),
  metric('yellowDotCount', '黄点', 'qualityGrainKey'),
  metric('redDotCount', '红点', 'qualityGrainKey'),
  metric('pinholeCount', '针孔', 'qualityGrainKey'),
  metric('stripeCount', '条纹', 'qualityGrainKey'),
  metric('wrinkleCount', '褶皱', 'qualityGrainKey'),
  metric('waveCount', '波浪纹', 'qualityGrainKey'),
  metric('otherCount', '其他', 'qualityGrainKey'),
  dimension('latestReportTime', '计划最后报工时间', '时间', 'DATETIME'),
  dimension('lastReportTime', '工序最后报工时间', '时间', 'DATETIME'),
  detail('pieceNo', '生产片号'),
  detail('pieceActualModelCode', '片实际型号'),
  detail('pieceActualSizeSpec', '片实际尺寸'),
  detail('pieceSourceBatchNo', '片来源批号'),
  detail('pieceOutputBatchNo', '片产出批号'),
  detail('pieceStatus', '片状态'),
  detail('coaFlag', 'COA标记', 'BOOLEAN'),
  detail('defectFlag', '片缺陷标记', 'BOOLEAN'),
  detail('reportConfirmed', '片报工已确认', 'BOOLEAN'),
  detail('pieceRemark', '片备注'),
  detail('pieceStageCode', '片工序编码'),
  detail('pieceLastReportTime', '片最后报工时间', 'DATETIME'),
  detail('inspectionId', '检验ID', 'NUMBER'),
  detail('inspectionNo', '检验单号'),
  detail('inspectionType', '检验类型'),
  detail('inspectionSourceType', '检验来源类型'),
  detail('inspectionStageCode', '检验工序编码'),
  detail('inspectionProductBatchNo', '检验产品批号'),
  detail('inspectionStatus', '检验状态'),
  detail('judgment', '检验判定'),
  detail('inspectionDefectSummary', '检验缺陷摘要'),
  detail('inspectionRemark', '检验备注'),
  detail('detailInspectionQty', '明细送检数量', 'NUMBER'),
  detail('detailInspectionNgQty', '明细检验NG数量', 'NUMBER'),
  detail('inspectionTime', '检验时间', 'DATETIME'),
];
