import type { PageParam, PageResult } from '@vben/request';

export namespace MesMoldLedgerApi {
  export interface Mold {
    id?: string | number;
    moldCode?: string;       // 工装/模具编号
    moldName?: string;       // 工装/模具名称
    moldType?: string;       // 类别 (注塑模/冲压模/治具/夹具)
    outputPerCycle?: number; // 【通用化升级】：单次产出量 (注塑即穴数，冲压即一冲几件，夹具即单次装夹数)
    location?: string;
    designLife?: number;
    currentLife?: number;
    warningRatio?: number;
    status?: number;
    remark?: string;
    createTime?: string;
    products?: MoldProduct[];
  }

  export interface MoldProduct {
    id?: string | number;
    moldId?: string | number;
    productCode?: string;
    productName?: string;
    spec?: string;
    isDefault?: boolean;
    remark?: string;
    sort?: number;
  }
}

// ================= Mock 数据池 =================
const mockMoldProducts: MesMoldLedgerApi.MoldProduct[] = [
  { id: 101, moldId: 1, productCode: 'P-IP15-CASE', productName: 'iPhone 15 标准版外壳', spec: 'PC+ABS', isDefault: true, sort: 1 },
  { id: 201, moldId: 2, productCode: 'P-CAR-HOOD', productName: '新能源汽车前机盖', spec: '铝合金 2mm', isDefault: true, sort: 1 },
  { id: 301, moldId: 3, productCode: 'P-PCB-TEST', productName: '主板测试总成', spec: '通用', isDefault: true, sort: 1 },
];

const mockMolds: MesMoldLedgerApi.Mold[] = [
  // 注塑场景 (一模4穴)
  { id: 1, moldCode: 'MD-INJ-001', moldName: 'IP15手机壳热流道注塑模', moldType: '注塑模', outputPerCycle: 4, location: '1#注塑机 (在机)', designLife: 500000, currentLife: 465000, warningRatio: 90, status: 20, remark: '高频使用模具', createTime: '2024-05-10 08:00:00' },
  // 冲压场景 (一冲1件)
  { id: 2, moldCode: 'MD-STM-002', moldName: '前机盖大型冲压模具', moldType: '冲压模', outputPerCycle: 1, location: '模具库-重型货架A区', designLife: 300000, currentLife: 120000, warningRatio: 85, status: 10, remark: '需天车吊装', createTime: '2024-08-15 10:00:00' },
  // 测试治具场景 (一次可装夹测试8块板子)
  { id: 3, moldCode: 'FX-TEST-001', moldName: 'PCB主板功能测试治具', moldType: '治具/夹具', outputPerCycle: 8, location: '测试线-A工位', designLife: 100000, currentLife: 15000, warningRatio: 90, status: 20, remark: '探针需定期更换', createTime: '2025-01-11 14:00:00' },
];

export function getMoldPage(params: PageParam) { return Promise.resolve({ list: mockMolds, total: mockMolds.length } as PageResult<MesMoldLedgerApi.Mold>); }
export function getMold(id: string | number) {
  const mold = mockMolds.find(i => i.id == id);
  const products = mockMoldProducts.filter(i => i.moldId == id);
  return Promise.resolve(JSON.parse(JSON.stringify({ ...mold, products })) as MesMoldLedgerApi.Mold);
}
export function createMold(data: MesMoldLedgerApi.Mold) { return Promise.resolve({ success: true, data }); }
export function updateMold(data: MesMoldLedgerApi.Mold) { return Promise.resolve({ success: true, data }); }
export function deleteMoldList(ids: (string | number)[]) { return Promise.resolve({ success: true }); }
export function exportMold(params: any) { return Promise.resolve(new Blob()); }

export function getMoldProductListById(moldId: string | number) {
  return Promise.resolve(JSON.parse(JSON.stringify(mockMoldProducts.filter(i => i.moldId == moldId))) as MesMoldLedgerApi.MoldProduct[]);
}
