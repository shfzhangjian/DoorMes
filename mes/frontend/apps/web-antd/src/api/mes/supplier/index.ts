import type { PageParam, PageResult } from '@vben/request';

import { requestClient } from '#/api/request';

export namespace MesSupplierApi {
  /** 供应商主数据信息 */
  export interface Supplier {
    id?: number; // 主键ID
    usingDepartment?: string; // 使用部门
    supplierCode?: string; // 供应商编码
    supplierName?: string; // 供应商名称
    contactPerson?: string; // 联系人
    contactPhone?: string; // 联系电话
    address?: string; // 供应商地址
    companyNature?: string; // 企业性质
    legalPerson?: string; // 法定代表人
    registeredCapital?: number; // 注册资本(万元)
    establishDate?: string; // 成立日期，yyyy-MM-dd
    originPlace?: string; // 产地
    originalFactoryInfo?: string; // 原厂信息
    mainProducts?: string; // 主营产品
    providedProduct?: string; // 提供/协作产品
    model?: string; // 型号
    materialCode?: string; // 物料代码
    applicableProduct?: string; // 适用产品
    importDate?: string; // 导入日期，yyyy-MM-dd
    materialCategory?: 'A' | 'B' | 'C' | 'D'; // 历史物料类别，保留接口兼容
    materialGrade?: 'A' | 'B' | 'C' | 'D'; // 物料等级
    paymentTerms?: string; // 结算付款条件
    deliveryMethod?: string; // 交货方式
    status?: 'ELIMINATED' | 'EXITED' | 'FROZEN' | 'PENDING' | 'QUALIFIED' | 'UNQUALIFIED'; // 供应商资源状态
    sourceSurveyId?: number; // 来源基本情况调查表ID
    sourceSurveyNo?: string; // 来源基本情况调查表编号
    registrarId?: number; // 登记人ID
    registrarName?: string; // 登记人
    initializationReason?: string; // 初始化说明
    scopeCode?: string; // 供应商名录管理范围编号
    scopeId?: number; // 供应商名录管理范围ID
    scopeName?: string; // 供应商名录管理范围名称
    shortName?: string; // 历史简称，保留接口兼容
    email?: string; // 历史电子邮箱，保留接口兼容
    level?: string; // 历史等级，保留接口兼容
    remark?: string; // 历史备注，保留接口兼容
    sort?: number; // 排序
    version?: number; // 乐观锁
    canEdit?: boolean; // 当前用户是否可编辑
    maskedFields?: string[]; // 当前用户被脱敏的字段
    viewPermission?: 'EDIT' | 'FULL' | 'MASKED'; // 当前用户查看权限
  }

  /** 供应商名录导入结果 */
  export interface ImportResp {
    totalRows?: number;
    skippedRows?: number;
    successCount?: number;
    failureCount?: number;
    messages?: string[];
    failures?: string[];
  }
}

/** 查询供应商主数据分页 */
export function getSupplierPage(params: PageParam) {
  return requestClient.get<PageResult<MesSupplierApi.Supplier>>(
    '/mes/supplier/page',
    { params },
  );
}

/** 查询供应商主数据详情 */
export function getSupplier(id: number) {
  return requestClient.get<MesSupplierApi.Supplier>(
    `/mes/supplier/get?id=${id}`,
  );
}

/** 新增供应商主数据 */
export function createSupplier(data: MesSupplierApi.Supplier) {
  return requestClient.post('/mes/supplier/create', data);
}

/** 修改供应商主数据 */
export function updateSupplier(data: MesSupplierApi.Supplier) {
  return requestClient.put('/mes/supplier/update', data);
}

/** 删除供应商主数据 */
export function deleteSupplier(id: number) {
  return requestClient.delete(`/mes/supplier/delete?id=${id}`);
}

/** 批量删除供应商主数据 */
export function deleteSupplierList(ids: number[]) {
  return requestClient.delete(`/mes/supplier/delete-list?ids=${ids.join(',')}`);
}

/** 导出供应商主数据 */
export function exportSupplier(params: any) {
  return requestClient.download('/mes/supplier/export-excel', { params });
}

/** 下载供应商名录导入模板 */
export function exportSupplierImportTemplate() {
  return requestClient.download('/mes/supplier/import-template');
}

/** 导入供应商名录 */
export function importSupplierExcel(file: File) {
  return requestClient.upload<MesSupplierApi.ImportResp>('/mes/supplier/import-excel', { file });
}
