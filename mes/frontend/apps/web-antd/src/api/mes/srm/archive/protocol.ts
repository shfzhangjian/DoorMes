export namespace SrmProtocolApi {
  export interface ProtocolRecord {
    id: string;
    supplierCode: string;
    supplierName: string;
    protocolType: string;   // 协议/证书类型
    protocolName: string;   // 协议/证书名称
    effectDate: string;     // 生效日期
    expiryDate: string;     // 失效日期
    daysLeft: number;       // 剩余天数 (动态计算)
    status: 'VALID' | 'WARNING' | 'EXPIRED'; // 状态
    tabType: 'valid' | 'warning' | 'expired'; // 用于前端 Tab 过滤
    attachment?: string;    // 附件名
  }
}

export async function getProtocolPage(params: any): Promise<{ items: SrmProtocolApi.ProtocolRecord[], total: number }> {
  return new Promise((resolve) => {
    setTimeout(() => {
      const today = new Date('2026-02-25').getTime(); // 模拟当前业务时间

      // 基础业务数据，贯通合格名录
      const rawData = [
        // 场景 1：正常生效中的协议与资质
        { id: '1', supplierCode: 'SUP-RM-001', supplierName: '江苏某某高分子材料有限公司', protocolType: 'AGREEMENT', protocolName: '2026年度采购框架协议', effectDate: '2026-01-01', expiryDate: '2026-12-31', attachment: '2026_Purchase_Agreement_Signed.pdf' },
        { id: '2', supplierCode: 'SUP-RM-001', supplierName: '江苏某某高分子材料有限公司', protocolType: 'QAA', protocolName: '质量保证协议 (QAA)', effectDate: '2025-05-12', expiryDate: '2028-05-11', attachment: 'QAA_Standard_v2.pdf' },
        { id: '3', supplierCode: 'SUP-RM-001', supplierName: '江苏某某高分子材料有限公司', protocolType: 'ISO9001', protocolName: 'ISO9001:2015 质量体系认证', effectDate: '2024-05-01', expiryDate: '2027-05-01', attachment: 'ISO9001_Cert.pdf' },

        // 场景 2：临期预警 (<30天) 的资质
        { id: '4', supplierCode: 'SUP-CH-022', supplierName: '上海某某化学试剂贸易行', protocolType: 'ROHS', protocolName: 'SGS RoHS 检测报告 (特种交联剂)', effectDate: '2025-02-25', expiryDate: '2026-03-05', attachment: 'SGS_RoHS_Report.pdf' },
        { id: '5', supplierCode: 'SUP-CH-022', supplierName: '上海某某化学试剂贸易行', protocolType: 'NDA', protocolName: '商业保密协议 (NDA)', effectDate: '2024-03-01', expiryDate: '2026-03-15', attachment: 'NDA_Signed.pdf' },

        // 场景 3：已过期拦截的资质
        { id: '6', supplierCode: 'SUP-PKG-005', supplierName: '无锡某某包装制品厂', protocolType: 'ISO9001', protocolName: 'ISO9001:2015 质量体系认证', effectDate: '2022-10-10', expiryDate: '2025-10-10', attachment: 'ISO9001_Old.pdf' },
        { id: '7', supplierCode: 'SUP-PKG-005', supplierName: '无锡某某包装制品厂', protocolType: 'ROHS', protocolName: '包装材料环保承诺书', effectDate: '2024-08-01', expiryDate: '2025-08-01', attachment: 'Env_Commitment.pdf' }
      ];

      // 动态计算剩余天数及状态
      const processedData: SrmProtocolApi.ProtocolRecord[] = rawData.map(item => {
        const expiry = new Date(item.expiryDate).getTime();
        const daysLeft = Math.ceil((expiry - today) / (1000 * 60 * 60 * 24));
        let status: 'VALID' | 'WARNING' | 'EXPIRED' = 'VALID';
        let tabType: 'valid' | 'warning' | 'expired' = 'valid';

        if (daysLeft < 0) {
          status = 'EXPIRED'; tabType = 'expired';
        } else if (daysLeft <= 30) {
          status = 'WARNING'; tabType = 'warning';
        }

        return { ...item, daysLeft, status, tabType };
      });

      // 前端查询过滤
      let filtered = processedData;
      if (params.tabType && params.tabType !== 'all') {
        filtered = filtered.filter(i => i.tabType === params.tabType);
      }
      if (params.supplierInfo) {
        filtered = filtered.filter(i => i.supplierName.includes(params.supplierInfo) || i.supplierCode.includes(params.supplierInfo));
      }
      if (params.protocolType) {
        filtered = filtered.filter(i => i.protocolType === params.protocolType);
      }

      resolve({ items: filtered, total: filtered.length });
    }, 400);
  });
}
