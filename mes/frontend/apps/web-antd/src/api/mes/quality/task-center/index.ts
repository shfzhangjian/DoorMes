import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesQualityTaskCenterApi {
  export type CheckType =
    | 'FAI'
    | 'FQC'
    | 'GLUE_BOARD_FAI'
    | 'IPQC'
    | 'IQC'
    | 'OQC';
  export type TaskType = 'ADDITIONAL' | 'LEGACY' | 'RECHECK';
  export type SourceMode = 'INSPECTION_RECORD' | 'MANUAL_BATCH';
  export type ExecutionMode = 'NATIVE' | 'TASK_OWNED';
  export type ObjectMode = 'BATCH' | 'EXECUTION_PIECE' | 'SOURCE_RECORD';
  export type SampleSelectionMode =
    | 'QUANTITY_ONLY'
    | 'SOURCE_ITEMS'
    | 'SPECIFIED_PIECE';
  export type DispatchStatus =
    | 'CANCELLED'
    | 'COMPLETED'
    | 'DISPATCHED'
    | 'DRAFT';

  export interface Task {
    id: number;
    taskNo: string;
    taskType: TaskType;
    sourceMode?: SourceMode;
    executionMode?: ExecutionMode;
    objectMode?: ObjectMode;
    sampleSelectionMode?: SampleSelectionMode;
    requiredSampleQty?: number;
    triggerSource?: string;
    inspectionScene?: string;
    checkType: CheckType;
    executionId?: number;
    executionNo?: string;
    executionRoute?: string;
    sourceExecutionId?: number;
    sourceExecutionNo?: string;
    rootExecutionId?: number;
    rootExecutionNo?: string;
    recheckGroupId?: number;
    currentRoundNo?: number;
    recheckCount?: number;
    materialCode?: string;
    materialName?: string;
    materialSpec?: string;
    productModel?: string;
    operationCode?: string;
    operationName?: string;
    lotNo?: string;
    receiptNo?: string;
    supplierName?: string;
    arrivalDate?: string;
    checkQty?: number;
    unit?: string;
    standardNo?: string;
    standardName?: string;
    standardVersion?: string;
    assigneeUserId?: number;
    assigneeUserName?: string;
    assigneeDeptId?: number;
    assigneeDeptName?: string;
    priority?: 'NORMAL' | 'URGENT';
    requiredFinishTime?: string;
    dispatchStatus: DispatchStatus;
    dispatchTime?: string;
    taskInstruction?: string;
    processInstanceId?: string;
    processStatus?: string;
    sourceStatus?: string;
    sourceJudgment?: string;
    itemCount?: number;
    abnormalCount?: number;
    effectiveStatus?: string;
    cancelReason?: string;
    createTime?: string;
  }

  export interface CandidateItem {
    id: number;
    sourceItemId?: number;
    sourceItemIds?: number[];
    executionItemId?: number;
    standardItemId?: number;
    nodeKey?: string;
    nodeType?: 'GROUP' | 'ITEM' | 'PIECE' | 'POSITION';
    selectable?: boolean;
    positionCode?: string;
    positionName?: string;
    sampleGroupNo?: number;
    pieceNo?: string;
    inspectionItem: string;
    standardDesc?: string;
    unit?: string;
    itemType?: string;
    inspectionMethod?: string;
    testTool?: string;
    templateParams?: string;
    sampleSize?: number;
    avgMinLimit?: number;
    avgMaxLimit?: number;
    stdMinLimit?: number;
    stdMaxLimit?: number;
    minValueLimit?: number;
    maxValueLimit?: number;
    averageValue?: number;
    standardDeviation?: number;
    defectCode?: string;
    defectName?: string;
    measuredValue?: number;
    resultValue?: number;
    qualitativeValue?: string;
    measuredData?: string;
    result?: string;
    currentResult?: string;
    sort?: number;
    pieceCount?: number;
    okPieceCount?: number;
    ngPieceCount?: number;
    pendingPieceCount?: number;
    children?: CandidateItem[];
  }

  export interface SourceRecord {
    id: number;
    executionNo: string;
    planNo?: string;
    workOrderNo?: string;
    operationCode?: string;
    operationName?: string;
    batchNo?: string;
    shippingNoticeNo?: string;
    receiptNo?: string;
    supplierName?: string;
    arrivalDate?: string;
    productionDate?: string;
    customerName?: string;
    productModel?: string;
    materialCode?: string;
    materialName?: string;
    materialId?: number;
    specification?: string;
    standardId?: number;
    standardNo?: string;
    standardName?: string;
    checkQty?: number;
    okQty?: number;
    ngQty?: number;
    unit?: string;
    status?: string;
    judgment?: string;
    abnormalSummary?: string;
    inspectorName?: string;
    inspectionTime?: string;
    auditorName?: string;
    auditTime?: string;
    remark?: string;
  }

  export interface Standard {
    id: number;
    standardNo?: string;
    standardName: string;
    version?: string;
    operationCode?: string;
    operationName?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    glueBoardModel?: string;
    auditorName?: string;
    auditTime?: string;
    itemCount?: number;
  }

  export interface WizardCreateReq {
    taskType: TaskType;
    checkType: CheckType;
    sourceMode?: SourceMode;
    objectMode?: ObjectMode;
    sampleSelectionMode?: SampleSelectionMode;
    requiredSampleQty?: number;
    samplePieceNos?: string[];
    sourceExecutionId?: number;
    sourceExecutionNo?: string;
    triggerSource?: string;
    inspectionScene?: string;
    rejectReason?: string;
    standardId?: number;
    selectedItemIds: number[];
    selectedScopes?: Array<{
      itemId: number;
      positions?: string[];
      scopeType: 'ITEM' | 'PIECE' | 'POSITION';
    }>;
    batchNo?: string;
    workOrderNo?: string;
    productModelId?: number;
    productModelCode?: string;
    productModelName?: string;
    materialId?: number;
    materialCode?: string;
    materialName?: string;
    specification?: string;
    operationId?: number;
    operationCode?: string;
    operationName?: string;
    checkQty?: number;
    unit?: string;
    receiptNo?: string;
    supplierName?: string;
    arrivalDate?: string;
    taskInstruction: string;
    assigneeUserId: number;
    assigneeUserName: string;
    assigneeDeptId?: number;
    assigneeDeptName?: string;
    priority?: string;
    requiredFinishTime?: string;
  }

  export interface PageReq extends PageParam {
    checkType?: CheckType;
    dispatchStatus?: DispatchStatus;
    keyword?: string;
    scope?: 'ALL' | 'MY_ASSIGNED' | 'MY_CREATED';
  }

  export interface TaskDetail {
    task: Task;
    items: CandidateItem[];
    samples: SampleResult[];
    rounds: TaskRound[];
    editable?: boolean;
    activeTaskKey?: string;
    activeTaskName?: string;
    actionable?: boolean;
  }

  export interface TaskRound {
    id?: number;
    taskId: number;
    roundNo: number;
    checkType: CheckType;
    inspectionScene?: string;
    recheckDetailId?: number;
    sourceExecutionId?: number;
    sourceExecutionNo?: string;
    executionId?: number;
    executionNo?: string;
    roundStatus: 'CONFIRMED' | 'EXECUTING' | 'RETURNED' | 'SUBMITTED';
    inspectionStatus?: string;
    judgment?: string;
    inspectorName?: string;
    inspectionTime?: string;
    returnReason?: string;
    returnedByName?: string;
    returnedTime?: string;
    confirmedByName?: string;
    confirmedTime?: string;
  }

  export interface SampleResult {
    id: number;
    taskItemId: number;
    roundNo: number;
    sampleSeq: number;
    pieceNo?: string;
    inspectionItem: string;
    standardDesc?: string;
    unit?: string;
    itemType?: string;
    inspectionMethod?: string;
    testTool?: string;
    measuredValue?: number;
    qualitativeValue?: string;
    result?: string;
    inspectorId?: number;
    inspectorName?: string;
    inspectionTime?: string;
  }

  export interface ResultSaveReq {
    id: number;
    items: Array<{
      taskItemId: number;
      sampleResultId?: number;
      sampleSeq?: number;
      pieceNo?: string;
      measuredValue?: number;
      qualitativeValue?: string;
      result: string;
    }>;
  }

  export interface TaskLog {
    id: number;
    actionType: string;
    actionDesc?: string;
    beforeStatus?: string;
    afterStatus?: string;
    operatorName?: string;
    actionTime?: string;
  }
}

export function createQualityTaskByWizard(
  data: MesQualityTaskCenterApi.WizardCreateReq,
) {
  return requestClient.post<number>('/mes/quality/task-center/wizard-create', data);
}

export function getQualityTaskSourcePage(
  params: PageParam & {
    batchNo?: string;
    checkType: MesQualityTaskCenterApi.CheckType;
    operationName?: string;
  },
) {
  return requestClient.get<PageResult<MesQualityTaskCenterApi.SourceRecord>>(
    '/mes/quality/task-center/source-page',
    { params },
  );
}

export function getQualityTaskSourceItems(
  checkType: MesQualityTaskCenterApi.CheckType,
  executionId: number,
) {
  return requestClient.get<MesQualityTaskCenterApi.CandidateItem[]>(
    '/mes/quality/task-center/source-items',
    { params: { checkType, executionId } },
  );
}

export function getQualityTaskSourceItemTree(
  checkType: MesQualityTaskCenterApi.CheckType,
  executionId: number,
) {
  return requestClient.get<MesQualityTaskCenterApi.CandidateItem[]>(
    '/mes/quality/task-center/source-item-tree',
    { params: { checkType, executionId } },
  );
}

export function getQualityTaskStandardPage(
  params: PageParam & {
    checkType: MesQualityTaskCenterApi.CheckType;
    operationCode?: string;
    operationName?: string;
    productModel?: string;
    standardName?: string;
  },
) {
  return requestClient.get<PageResult<MesQualityTaskCenterApi.Standard>>(
    '/mes/quality/task-center/standard-page',
    { params },
  );
}

export function getQualityTaskStandardItems(standardId: number) {
  return requestClient.get<MesQualityTaskCenterApi.CandidateItem[]>(
    '/mes/quality/task-center/standard-items',
    { params: { standardId } },
  );
}

export function cancelQualityTask(data: { id: number; reason: string }) {
  return requestClient.put<boolean>('/mes/quality/task-center/cancel', data);
}

export function getQualityTask(id: number) {
  return requestClient.get<MesQualityTaskCenterApi.Task>(
    '/mes/quality/task-center/get',
    { params: { id } },
  );
}

export function getQualityTaskDetail(id: number) {
  return requestClient.get<MesQualityTaskCenterApi.TaskDetail>(
    '/mes/quality/task-center/detail',
    { params: { id } },
  );
}

export function getQualityTaskPage(
  params: MesQualityTaskCenterApi.PageReq,
) {
  return requestClient.get<PageResult<MesQualityTaskCenterApi.Task>>(
    '/mes/quality/task-center/page',
    { params },
  );
}

export function getQualityTaskLogs(taskId: number) {
  return requestClient.get<MesQualityTaskCenterApi.TaskLog[]>(
    '/mes/quality/task-center/logs',
    { params: { taskId } },
  );
}

export function saveQualityTaskResult(
  data: MesQualityTaskCenterApi.ResultSaveReq,
) {
  return requestClient.put<boolean>('/mes/quality/task-center/result/save', data);
}

export function submitQualityTaskProcessNode(data: {
  id: number;
  reason?: string;
}) {
  return requestClient.put<boolean>('/mes/quality/task-center/process/submit', data);
}

export function returnQualityTaskForRecheck(data: {
  id: number;
  reason: string;
}) {
  return requestClient.put<boolean>('/mes/quality/task-center/process/recheck', data);
}
