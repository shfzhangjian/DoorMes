import type { PageParam, PageResult } from '@vben/request';
import type { Dayjs } from 'dayjs';

import { requestClient } from '#/api/request';

export namespace MesMockWorkOrderApi {
  /** 模拟生产工单表（用于AI大模型MCP调用测试）信息 */
  export interface MockWorkOrder {
    id: number; // 工单流水号（主键）
    orderNo?: string; // 生产工单号 (如: WO-20260225-001)
    productName?: string; // 产品名称
    quantity?: number; // 生产排产数量
    status?: string; // 工单状态（DRAFT:草稿, DOING:生产中, DONE:已完成）
    remark: string; // 备注（可存储由AI提取的自然语言原始诉求）
  }
}

/** 查询模拟生产工单表（用于AI大模型MCP调用测试）分页 */
export function getMockWorkOrderPage(params: PageParam) {
  return requestClient.get<PageResult<MesMockWorkOrderApi.MockWorkOrder>>(
    '/mes/mock-work-order/page',
    { params },
  );
}

/** 查询模拟生产工单表（用于AI大模型MCP调用测试）详情 */
export function getMockWorkOrder(id: number) {
  return requestClient.get<MesMockWorkOrderApi.MockWorkOrder>(
    `/mes/mock-work-order/get?id=${id}`,
  );
}

/** 新增模拟生产工单表（用于AI大模型MCP调用测试） */
export function createMockWorkOrder(data: MesMockWorkOrderApi.MockWorkOrder) {
  return requestClient.post('/mes/mock-work-order/create', data);
}

/** 修改模拟生产工单表（用于AI大模型MCP调用测试） */
export function updateMockWorkOrder(data: MesMockWorkOrderApi.MockWorkOrder) {
  return requestClient.put('/mes/mock-work-order/update', data);
}

/** 删除模拟生产工单表（用于AI大模型MCP调用测试） */
export function deleteMockWorkOrder(id: number) {
  return requestClient.delete(`/mes/mock-work-order/delete?id=${id}`);
}

/** 批量删除模拟生产工单表（用于AI大模型MCP调用测试） */
export function deleteMockWorkOrderList(ids: number[]) {
  return requestClient.delete(
    `/mes/mock-work-order/delete-list?ids=${ids.join(',')}`,
  );
}

/** 导出模拟生产工单表（用于AI大模型MCP调用测试） */
export function exportMockWorkOrder(params: any) {
  return requestClient.download('/mes/mock-work-order/export-excel', { params });
}

