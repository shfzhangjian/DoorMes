import type { MesHcProductionInstructionApi } from '#/api/mes/hc/production-instruction';

export interface ProductionInstructionContext {
  batchNo?: string;
  instructionType?: string;
  operationCode?: string;
  operationIds?: number[];
  operationName?: string;
  planId?: number;
  planNo?: string;
  planOperationId?: number;
  processCode?: string;
  processId?: number;
  processName?: string;
  productMaterialCode?: string;
  productModelCode?: string;
  productionBatchNo?: string;
  recipientIds?: number[];
  scopeType?: string;
  segmentBatchNo?: string;
}

export interface ProductionInstructionSegmentOption {
  label: string;
  value: string;
}

export function instructionContextBatchNo(context?: ProductionInstructionContext) {
  return context?.segmentBatchNo || context?.batchNo || context?.productionBatchNo || '';
}

export function hasInstructionContext(context?: ProductionInstructionContext) {
  return Boolean(
    context?.planOperationId ||
      context?.planId ||
      context?.planNo ||
      instructionContextBatchNo(context) ||
      context?.processCode ||
      context?.processName ||
      context?.operationCode ||
      context?.operationName,
  );
}

export function buildOperationInstructionQuery(
  context?: ProductionInstructionContext,
  includeConfirmed = true,
): MesHcProductionInstructionApi.OperationReq {
  return {
    batchNo: instructionContextBatchNo(context) || undefined,
    includeConfirmed,
    operationCode: context?.operationCode || undefined,
    operationName: context?.operationName || undefined,
    planId: context?.planId || undefined,
    planNo: context?.planNo || undefined,
    planOperationId: context?.planOperationId || undefined,
    processCode: context?.processCode || undefined,
    processName: context?.processName || undefined,
  };
}

export function buildInstructionContextSnapshot(
  context?: ProductionInstructionContext,
): Partial<MesHcProductionInstructionApi.Instruction> {
  return {
    batchNo: instructionContextBatchNo(context),
    instructionType: context?.instructionType,
    operationCode: context?.operationCode || context?.processCode || '',
    operationIds: context?.operationIds,
    operationName: context?.operationName || context?.processName || '',
    planId: context?.planId,
    planNo: context?.planNo || '',
    planOperationId: context?.planOperationId,
    beforeMaterialCode: context?.productMaterialCode || '',
    beforeModelCode: context?.productModelCode || '',
    processCode: context?.processCode || '',
    processId: context?.processId,
    processName: context?.processName || '',
    productionBatchNo: context?.productionBatchNo,
    scopeType: context?.scopeType,
    segmentBatchNo: context?.segmentBatchNo,
  };
}
