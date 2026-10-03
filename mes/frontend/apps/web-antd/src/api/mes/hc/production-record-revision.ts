import { requestClient } from '#/api/request';

export interface ProductionRecordRevisionCreateReqVO {
  moduleCode: string;
  originalSnapshot: Record<string, any>;
  recordId: number;
  revisedData: Record<string, any>;
  reviseReason: string;
}

export async function createProductionRecordRevision(
  data: ProductionRecordRevisionCreateReqVO,
) {
  return requestClient.post<boolean>(
    '/mes/hc/production-record-revision/create',
    data,
  );
}
