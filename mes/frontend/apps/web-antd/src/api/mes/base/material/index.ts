import type { PageParam, PageResult } from '@vben/request';
import { requestClient } from '#/api/request';

export namespace baseMesMaterialApi {
  /** MES物料主数据信息 */
  export interface MesMaterial {
    id: number; // 主键ID
    code?: string; // 物料编码 (ERP码)
    name?: string; // 物料名称

    // 🔥 新增: 真实的物料类目树结构
    categoryId?: string;

    category?: string; // 物料性质 (原物料分类)
    materialSource?: string; // 物料来源

    // 🔥 新增: 极其重要的车间防呆追溯模式
    trackingMode?: 'BATCH' | 'SN' | 'NONE';

    // 🔥 新增：预置批次/条码生成规则绑定
    batchRuleId?: number; // 绑定的批次生成规则ID
    batchRuleName?: string; // 绑定的批次生成规则名称

    materialGrade?: string; // 材质牌号
    spec?: string; // 规格型号
    unit?: string; // 计量单位
    unitWeight?: number; // 单重 (kg)
    scrapRate?: number; // 理论废品率 (%)
    drawingUrl?: string; // 图纸/规范附件路径
    leadTime?: number; // 采购/生产提前期(天)
    supplierId?: number; // 默认供应商关联ID
    supplierName?: string; // 默认供应商关联名称
    remark?: string; // 备注
    status?: number; // 状态
    createTime?: string; // 创建时间
  }
}

/** 查询MES物料主数据分页 */
export function getMesMaterialPage(params: PageParam) {
  return requestClient.get<PageResult<baseMesMaterialApi.MesMaterial>>(
    '/mes/base/material/page',
    { params },
  );
}

/** 查询MES物料主数据详情 */
export function getMesMaterial(id: number) {
  return requestClient.get<baseMesMaterialApi.MesMaterial>(
    `/mes/base/material/get?id=${id}`,
  );
}

/** 新增MES物料主数据 */
export function createMesMaterial(data: baseMesMaterialApi.MesMaterial) {
  return requestClient.post('/mes/base/material/create', data);
}

/** 修改MES物料主数据 */
export function updateMesMaterial(data: baseMesMaterialApi.MesMaterial) {
  return requestClient.put('/mes/base/material/update', data);
}

/** 删除MES物料主数据 */
export function deleteMesMaterial(id: number) {
  return requestClient.delete(`/mes/base/material/delete?id=${id}`);
}

/** 批量删除MES物料主数据 */
export function deleteMesMaterialList(ids: number[]) {
  return requestClient.delete(
    `/mes/base/material/delete-list?ids=${ids.join(',')}`,
  );
}

/** 导出MES物料主数据 */
export function exportMesMaterial(params: any) {
  return requestClient.download('/mes/base/material/export-excel', { params });
}
