import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesBatchingTaskApi } from '#/api/mes/execution/batching-task';
import { getDictOptions } from '@vben/hooks';

export function useFormSchema(formType: string): VbenFormSchema[] {
  const isDetail = formType === 'detail';
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'status', component: 'Select', defaultValue: 10, dependencies: { triggerFields: [''], show: () => false } },

    // 严格排版：每行3个，状态字段隐藏，最后一行 remark 跨2列填满
    { fieldName: 'taskNo', label: '配料单号', rules: 'required', component: 'Input', componentProps: { placeholder: '系统自动生成', disabled: true } },
    { fieldName: 'workOrderNo', label: '生产工单', rules: 'required', component: 'Input', componentProps: { placeholder: '请选择关联工单', disabled: isDetail } },
    { fieldName: 'recipeName', label: '配方名称', rules: 'required', component: 'Input', componentProps: { placeholder: '选择工单带出', disabled: true } },

    { fieldName: 'productName', label: '产出产品', component: 'Input', componentProps: { disabled: true } },
    { fieldName: 'planQty', label: '计划配制量', rules: 'required', component: 'InputNumber', componentProps: { min: 0, class: 'w-full', disabled: isDetail } },
    { fieldName: 'unit', label: '单位', component: 'Input', componentProps: { disabled: true, placeholder: 'kg' } },

    { fieldName: 'operator', label: '作业人员', component: 'Input', componentProps: { disabled: isDetail } },
    { fieldName: 'remark', label: '任务备注', component: 'Textarea', componentProps: { placeholder: '请输入派工备注信息', rows: 1, disabled: isDetail } },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'taskNo', label: '配料单号', component: 'Input' },
    { fieldName: 'workOrderNo', label: '生产工单', component: 'Input' },
    { fieldName: 'status', label: '任务状态', component: 'Select', componentProps: { options: [{ label: '待配料', value: 10 }, { label: '配料中', value: 20 }, { label: '已完成', value: 30 }] } },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesBatchingTaskApi.Task>['columns'] {
  return [
    { type: 'checkbox', width: 40 },
    { field: 'taskNo', title: '配料任务号', minWidth: 160 },
    { field: 'workOrderNo', title: '生产工单号', minWidth: 150 },
    { field: 'recipeName', title: '应用配方', minWidth: 160 },
    { field: 'productName', title: '产品名称', minWidth: 150 },
    { field: 'planQty', title: '计划量', width: 100, formatter: ({ cellValue, row }) => `${cellValue} ${row.unit}` },
    {
      field: 'status', title: '状态', width: 100,
      formatter: ({ cellValue }) => {
        if (cellValue === 10) return '待配料';
        if (cellValue === 20) return '配料中';
        if (cellValue === 30) return '已完成';
        return '未知';
      }
    },
    { field: 'operator', title: '操作人', width: 100 },
    { field: 'startTime', title: '开始时间', width: 150 },
    { title: '操作', width: 200, fixed: 'right', slots: { default: 'actions' } },
  ];
}

export function useItemGridColumns(): VxeTableGridOptions<MesBatchingTaskApi.TaskItem>['columns'] {
  return [
    { field: 'sort', title: '顺序', width: 60, align: 'center', slots: { default: 'sort' } },
    { field: 'materialCode', title: '物料编码', width: 130 },
    { field: 'materialName', title: '物料名称', minWidth: 160, slots: { default: 'materialSlot' } },
    { field: 'standardQty', title: '标准用量', width: 100 },
    { field: 'tolerance', title: '允差', width: 80 },
    { field: 'barcode', title: '投料条码', minWidth: 150, slots: { default: 'barcode' } },
    { field: 'actualQty', title: '实称量', width: 110, slots: { default: 'actualQty' } },
    { field: 'unit', title: '单位', width: 60, align: 'center' },
    { field: 'result', title: '比对结果', width: 90, align: 'center', slots: { default: 'result' } },
    { title: '操作', width: 80, fixed: 'right', align: 'center', slots: { default: 'actions' } },
  ];
}
