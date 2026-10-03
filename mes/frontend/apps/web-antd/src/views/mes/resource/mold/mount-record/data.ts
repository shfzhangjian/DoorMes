import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import { getRangePickerDefaultProps } from '#/utils';

// ==========================================
// 用于【执行上模 (Mount)】的表单
// ==========================================
export function useMountFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },

    // 1. 生产工单提取区
    { fieldName: 'workOrderNo', label: '扫码关联派工单', rules: 'required', component: 'Input', componentProps: { placeholder: '扫码自动解析工单' } },
    { fieldName: 'productName', label: '生产目标产品', component: 'Input', componentProps: { disabled: true, placeholder: '扫码带出' } },
    { fieldName: 'planQty', label: '工单计划产量', component: 'InputNumber', componentProps: { disabled: true, class: 'w-full', addonAfter: '件' } },

    // 2. 模具防呆区
    { fieldName: 'moldCode', label: '扫码选取模具', rules: 'required', component: 'Input', componentProps: { placeholder: '扫描模具二维码' }, help: '系统将拦截与工单BOM不匹配的模具' },
    { fieldName: 'moldName', label: '模具/工装名称', component: 'Input', componentProps: { disabled: true, placeholder: '扫码带出' } },
    { fieldName: 'outputPerCycle', label: '单次产出量(穴数)', component: 'InputNumber', componentProps: { disabled: true, class: 'w-full', addonAfter: '件/次' }, help: '该模具动作一次的产出数，用于后续寿命折算' },

    // 3. 设备与动作区
    { fieldName: 'deviceCode', label: '挂载目标设备', rules: 'required', component: 'Input', componentProps: { placeholder: '扫描设备二维码' } },
    { fieldName: 'deviceName', label: '设备名称', component: 'Input', componentProps: { disabled: true, placeholder: '扫设备自动带出' } },
    { fieldName: 'mountTime', label: '上模登记时间', rules: 'required', component: 'DatePicker', componentProps: { class: 'w-full', showTime: true } },
    { fieldName: 'mounter', label: '上模操作人员', rules: 'required', component: 'Input' },
    { fieldName: 'remark', label: '上模备注说明', component: 'Textarea', componentProps: { rows: 2, placeholder: '其他现场补充信息' } },
  ];
}

// ==========================================
// 用于【执行下模 (Teardown)】的表单
// ==========================================
export function useTeardownFormSchema(calculateFn: (qty: number) => void): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },

    // 1. 基础挂载信息 (只读回显)
    { fieldName: 'workOrderNo', label: '执行中派工单', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'moldName', label: '在机模具', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'deviceCode', label: '卸下所属设备', component: 'Input', componentProps: { disabled: true } },

    // 2. 寿命扣减计算引擎区 (核心字段化)
    { fieldName: 'currentLife', label: '当前已用总寿命', component: 'InputNumber', componentProps: { disabled: true, class: 'w-full', addonAfter: '次' } },
    { fieldName: 'outputPerCycle', label: '单次产出量(穴数)', component: 'InputNumber', componentProps: { disabled: true, class: 'w-full', addonAfter: '件/次' } },
    {
      fieldName: 'reportedQty', label: '工单实际报工总数', rules: 'required', component: 'InputNumber',
      componentProps: {
        min: 0, class: 'w-full', addonAfter: '件', placeholder: '填入实际产出数',
        onChange: (val: number) => calculateFn(val) // 💡 注入联动计算逻辑
      },
      help: '输入实际生产件数，系统将根据穴数自动折算寿命扣减值'
    },
    { fieldName: 'producedQty', label: '折算消耗模次', rules: 'required', component: 'InputNumber', componentProps: { min: 0, class: 'w-full', addonAfter: '次 (扣减量)' }, help: '算法 = 报工总数 ÷ 单次产出量(向上取整)' },

    // 3. 下模评估信息
    { fieldName: 'teardownTime', label: '下线剥离时间', rules: 'required', component: 'DatePicker', componentProps: { class: 'w-full', showTime: true } },
    { fieldName: 'teardowner', label: '下线操作人员', rules: 'required', component: 'Input' },
    {
      fieldName: 'afterStatus', label: '下线后状态评估', rules: 'required', component: 'RadioGroup',
      componentProps: {
        options: [{ label: '🟢 完好入库', value: 10 }, { label: '🟠 报修维保', value: 30 }, { label: '🔴 彻底报废', value: 40 }],
        buttonStyle: 'solid', optionType: 'button'
      },
      help: '必选：将直接重置模具台账状态。报修将触发维修工单。'
    },
    { fieldName: 'remark', label: '下线异常反馈', component: 'Textarea', componentProps: { rows: 2, placeholder: '若拉伤、卡死或尺寸超差，请详述...' } },
  ];
}

// ==========================================
// 列表查询与展示列 (完全恢复)
// ==========================================
export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'moldCode', label: '模具编号', component: 'Input' },
    { fieldName: 'deviceCode', label: '设备编号', component: 'Input' },
    { fieldName: 'recordStatus', label: '挂载状态', component: 'Select', componentProps: { options: [{ label: '在机生产中', value: 'MOUNTED' }, { label: '已下线剥离', value: 'TEARDOWN' }], allowClear: true } },
    { fieldName: 'mountTime', label: '上模时间范围', component: 'RangePicker', componentProps: { ...getRangePickerDefaultProps(), allowClear: true } },
  ];
}

export function useGridColumns(): VxeTableGridOptions['columns'] {
  return [
    { type: 'checkbox', width: 40, fixed: 'left' },
    { field: 'moldCode', title: '模具编号', minWidth: 140, fixed: 'left' },
    { field: 'moldName', title: '模具名称', minWidth: 180 },
    { field: 'deviceCode', title: '挂载设备', minWidth: 140 },
    { field: 'workOrderNo', title: '生产工单', minWidth: 150 },
    { field: 'mountTime', title: '上模时间', width: 160, align: 'center' },
    { field: 'mounter', title: '上模人', width: 100 },
    { field: 'recordStatus', title: '挂载状态', width: 120, align: 'center', slots: { default: 'recordStatus' } },
    { field: 'teardownTime', title: '下模时间', width: 160, align: 'center', formatter: ({ cellValue }) => cellValue || '-' },
    { title: '操作', width: 150, fixed: 'right', slots: { default: 'actions' } },
  ];
}
