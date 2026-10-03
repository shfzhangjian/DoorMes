import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';

// ... (保留原有的 useGridFormSchema 和 useGridColumns 不变) ...

export function useGridFormSchema(): VbenFormSchema[] {
  // ... 原有代码保持不变
  return [
    { fieldName: 'productCode', label: '物料编码/名称', component: 'Input', componentProps: { placeholder: '输入编码或名称模糊查询', allowClear: true } },
    { fieldName: 'location', label: '库位/货架', component: 'Input', componentProps: { placeholder: '输入库区或特定储位', allowClear: true } },
    { fieldName: 'batchNo', label: '物料批次号', component: 'Input', componentProps: { placeholder: '精确追溯批次', allowClear: true } },
    { fieldName: 'hasStock', label: '库存过滤', component: 'Select', defaultValue: 'Y', componentProps: { options: [{ label: '仅显示有库存的物料 (>0)', value: 'Y' }, { label: '包含零库存', value: 'ALL' }] } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  // ... 原有代码保持不变
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center', fixed: 'left' },
    { field: 'location', title: '物理库位', minWidth: 140, slots: { default: 'location' }, fixed: 'left' },
    { field: 'productCode', title: '物料编码', minWidth: 140, slots: { default: 'productCode' } },
    { field: 'productName', title: '物料名称', minWidth: 160 },
    { field: 'batchNo', title: '批次号', minWidth: 140, slots: { default: 'batchNo' } },
    { field: 'unit', title: '单位', minWidth: 80, align: 'center' },
    { field: 'totalQty', title: '账面总库存', minWidth: 120, align: 'right', slots: { default: 'totalQty' } },
    { field: 'lockedQty', title: '冻结/锁定数', minWidth: 120, align: 'right', slots: { default: 'lockedQty' } },
    { field: 'availableQty', title: '实际可用量', minWidth: 130, align: 'right', slots: { default: 'availableQty' } },
    { field: 'lastUpdate', title: '最后变动时间', minWidth: 160, align: 'center' },
    { title: '操作', width: 150, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}

// ==========================================
// 💡 新增：3. 弹窗流水台账的列定义
// ==========================================
export function useLedgerColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'seq', title: '序号', width: 60, align: 'center' },
    { field: 'trxTime', title: '发生时间', minWidth: 160, align: 'center' },
    { field: 'trxType', title: '事务类型', minWidth: 120, align: 'center', slots: { default: 'trxType' } },
    { field: 'trxNo', title: '台账凭证号', minWidth: 180, slots: { default: 'trxNo' } },
    { field: 'refOrderNo', title: '关联源单据', minWidth: 160 },
    { field: 'qty', title: '变动数量', minWidth: 100, align: 'right', slots: { default: 'qty' } },
    { field: 'balanceQty', title: '变动后结存', minWidth: 100, align: 'right', slots: { default: 'balanceQty' } },
    { field: 'operator', title: '操作人', minWidth: 100, align: 'center' },
  ];
}
