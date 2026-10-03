import type { VbenFormSchema } from '#/adapter/form';
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { MesHcFinishedGlueBoardMapApi } from '#/api/mes/hc/finishedglueboardmap';

export const mapStatusOptions = [
  { label: '启用', value: 'ENABLE' },
  { label: '停用', value: 'DISABLE' },
];

export const glueProcessOptions = [
  { label: '粘胶1', value: 'ADHESIVE1' },
  { label: '粘胶2', value: 'ADHESIVE2' },
];

const statusMap = Object.fromEntries(mapStatusOptions.map((item) => [item.value, item.label]));

export function useFormSchema(): VbenFormSchema[] {
  return [
    { fieldName: 'id', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'productModelId', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'productModelCode', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'sizeSpec', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    { fieldName: 'sizeName', component: 'Input', dependencies: { triggerFields: [''], show: () => false } },
    {
      fieldName: 'productModelName',
      label: '型号名称',
      component: 'Input',
      componentProps: { disabled: true, placeholder: '选择产品型号后带出' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'productSpec',
      label: '成品规格',
      component: 'Input',
      rules: 'required',
      componentProps: { placeholder: '请输入成品规格，如 775 / 740' },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'RadioGroup',
      defaultValue: 'ENABLE',
      componentProps: {
        options: mapStatusOptions,
        buttonStyle: 'solid',
        optionType: 'button',
      },
      formItemClass: 'col-span-1',
    },
    {
      fieldName: 'remark',
      label: '备注',
      component: 'Textarea',
      componentProps: { rows: 3, placeholder: '请输入备注' },
      formItemClass: 'col-span-2',
    },
  ];
}

export function useGridFormSchema(): VbenFormSchema[] {
  return [
    {
      fieldName: 'productModelCode',
      label: '产品型号',
      component: 'Input',
      componentProps: { placeholder: '请输入产品型号' },
    },
    {
      fieldName: 'productSpec',
      label: '成品规格',
      component: 'Input',
      componentProps: { placeholder: '请输入规格' },
    },
    {
      fieldName: 'glueBoardKeyword',
      label: '胶板关键字',
      component: 'Input',
      componentProps: { placeholder: '请输入胶板料号/型号' },
    },
    {
      fieldName: 'status',
      label: '状态',
      component: 'Select',
      componentProps: { options: mapStatusOptions, allowClear: true },
    },
  ];
}

export function useGridColumns(): VxeTableGridOptions<MesHcFinishedGlueBoardMapApi.FinishedGlueBoardMap>['columns'] {
  return [
    { type: 'checkbox', width: 48, fixed: 'left' },
    { field: 'productModelCode', title: '产品型号', minWidth: 150, fixed: 'left' },
    { field: 'productModelName', title: '型号名称', minWidth: 160 },
    { field: 'productSpec', title: '成品规格', width: 110, align: 'center' },
    { field: 'adhesive1Summary', title: '粘胶1胶板', minWidth: 240, showOverflow: 'tooltip' },
    { field: 'adhesive2Summary', title: '粘胶2胶板', minWidth: 280, showOverflow: 'tooltip' },
    {
      field: 'status',
      title: '状态',
      width: 90,
      align: 'center',
      formatter: ({ cellValue }) => statusMap[String(cellValue)] || cellValue || '-',
    },
    { field: 'remark', title: '备注', minWidth: 220, showOverflow: 'tooltip' },
    { title: '操作', width: 160, fixed: 'right', slots: { default: 'actions' } },
  ];
}
