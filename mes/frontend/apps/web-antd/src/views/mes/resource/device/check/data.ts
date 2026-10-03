import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesMaintPlanApi } from '#/api/mes/resource/device/check';
import { DICT_TYPE } from '@vben/constants';
import { getDictOptions } from '@vben/hooks';

// ================= 1. 维保计划 (Plan) Schema =================
export function usePlanFormSchema(formType: string): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'planNo', label: '计划编号', component: 'Input', componentProps: { placeholder: '自动生成或手工输入', disabled: isDetail } },
    { fieldName: 'deviceName', label: '关联设备', rules: 'required', component: 'Input', componentProps: { placeholder: '请选择关联设备 (如 1#真空搅拌机)', disabled: isDetail } },
    { fieldName: 'pmLevel', label: '维保级别', rules: 'required', component: 'Select', componentProps: { options: [{ label: '一级保养', value: '一级保养' }, { label: '二级保养', value: '二级保养' }, { label: '三级大修', value: '三级大修' }], disabled: isDetail } },
    { fieldName: 'cycleDays', label: '周期(天)', rules: 'required', component: 'InputNumber', componentProps: { min: 1, class: 'w-full', disabled: isDetail } },
    { fieldName: 'status', label: '状态', rules: 'required', component: 'RadioGroup', defaultValue: 1, componentProps: { options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }], buttonStyle: 'solid', optionType: 'button', disabled: isDetail } },
    { fieldName: 'content', label: '维保内容要求', rules: 'required', component: 'Textarea', componentProps: { rows: 3, placeholder: '请详细描述该级别维保需要执行的具体动作和标准', disabled: isDetail } },
  ];
}

export function usePlanGridColumns(): VxeTableGridOptions<MesMaintPlanApi.Plan>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'planNo', title: '计划编号', width: 140 },
    { field: 'deviceName', title: '关联设备', minWidth: 160 },
    { field: 'pmLevel', title: '维保级别', width: 100 },
    { field: 'cycleDays', title: '周期(天)', width: 100, formatter: ({ cellValue }) => `${cellValue} 天` },
    { field: 'content', title: '维保内容', minWidth: 250, showOverflow: 'tooltip' },
    { field: 'status', title: '状态', width: 80, cellRender: { name: 'CellDict', props: { type: DICT_TYPE.COMMON_STATUS } } },
    { title: '操作', width: 200, fixed: 'right', slots: { default: 'actions' } },
  ];
}

// ================= 2. 维保工单 (Order) Schema =================
export function useOrderFormSchema(formType: string): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'status', component: 'Input', defaultValue: 10, dependencies: { triggerFields: [''], show: () => false } },

    { fieldName: 'orderNo', label: '工单号', component: 'Input', componentProps: { placeholder: '自动生成', disabled: true } },
    { fieldName: 'planNo', label: '来源计划', component: 'Input', componentProps: { disabled: isDetail } },
    { fieldName: 'deviceName', label: '维保设备', component: 'Input', componentProps: { disabled: isDetail } },

    { fieldName: 'pmLevel', label: '维保级别', component: 'Input', componentProps: { disabled: isDetail } },
    { fieldName: 'operator', label: '执行人', component: 'Input', componentProps: { disabled: isDetail } },
    { fieldName: 'planDate', label: '计划日期', component: 'DatePicker', componentProps: { class: 'w-full', disabled: isDetail } },

    { fieldName: 'remark', label: '执行总结', component: 'Textarea', componentProps: { rows: 2, placeholder: '记录保养过程发现的问题及处理结果', disabled: isDetail } },
  ];
}

export function useOrderGridColumns(): VxeTableGridOptions<MesMaintPlanApi.Order>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'orderNo', title: '维保工单号', width: 160 },
    { field: 'planNo', title: '来源计划', width: 140 },
    { field: 'deviceName', title: '设备名称', minWidth: 150 },
    { field: 'pmLevel', title: '级别', width: 100 },
    { field: 'operator', title: '执行人', width: 100 },
    { field: 'status', title: '状态', width: 100, slots: { default: 'status' } },
    { field: 'planDate', title: '计划日期', width: 120 },
    { field: 'actualDate', title: '执行时间', width: 160 },
    { title: '操作', width: 200, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useOrderItemColumns(): VxeTableGridOptions<MesMaintPlanApi.OrderItem>['columns'] {
  return [
    { field: 'sort', title: '序号', width: 60, align: 'center', slots: { default: 'sort' } },
    { field: 'partName', title: '消耗备件名称', minWidth: 180, slots: { default: 'partSlot' } },
    { field: 'spec', title: '规格型号', minWidth: 120 },
    { field: 'quantity', title: '消耗数量', width: 100, slots: { default: 'quantity' } },
    { field: 'unit', title: '单位', width: 60, align: 'center' },
    { field: 'remark', title: '备注说明', minWidth: 150, slots: { default: 'remark' } },
    { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}
